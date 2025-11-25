package com.org.ollamafx.manager;

import com.org.ollamafx.model.OllamaModel;
import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.models.response.LibraryModel;
import io.github.ollama4j.models.response.Model;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OllamaManager {

    private static OllamaManager instance;
    private final OllamaAPI client;

    private OllamaManager() {
        this.client = new OllamaAPI();
    }

    public static synchronized OllamaManager getInstance() {
        if (instance == null) {
            instance = new OllamaManager();
        }
        return instance;
    }

    // --- MÉTODOS QUE YA FUNCIONAN Y NO NECESITAN CAMBIOS ---
    public List<OllamaModel> getLocalModels() {
        List<OllamaModel> localModelsList = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        try {
            List<Model> responseList = client.listModels();
            for (Model model : responseList) {
                String[] nameParts = model.getName().split(":", 2);
                String baseName = nameParts[0];
                String tag = (nameParts.length > 1) ? nameParts[1] : "latest";
                OffsetDateTime modifiedAt = model.getModifiedAt();
                String formattedDate = (modifiedAt != null) ? modifiedAt.format(formatter) : "N/A";
                OllamaModel localModel = new OllamaModel(baseName, "Installed locally", "N/A", tag,
                        formatSize(model.getSize()), formattedDate);
                localModelsList.add(localModel);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return localModelsList;
    }

    public List<OllamaModel> getAvailableBaseModels() {
        try {
            List<LibraryModel> baseModels = client.listModelsFromLibrary();
            return baseModels.stream().map(libraryModel -> new OllamaModel(libraryModel.getName(), "", "", "", "", ""))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // --- MÉTODO DE SCRAPING FINAL Y CORREGIDO ---

    /**
     * VERSIÓN FINAL con selectores de CSS ajustados al HTML real.
     * 
     * @param modelName El nombre del modelo a buscar (ej: "gemma3n").
     * @return Una lista de OllamaModel, donde cada uno representa un tag.
     * @throws IOException Si la conexión a la página web falla.
     */
    public List<OllamaModel> scrapeModelDetails(String modelName) throws IOException {
        List<OllamaModel> modelTags = new ArrayList<>();
        String url = "https://ollama.com/library/" + modelName;

        Document doc = Jsoup.connect(url).get();

        // Selector para la descripción principal del modelo
        String description = doc.selectFirst("#summary-content") != null ? doc.selectFirst("#summary-content").text()
                : "No description available.";

        // Selector para el contador de descargas (pulls)
        String pullCount = doc.selectFirst("span[x-test-pull-count]") != null
                ? doc.selectFirst("span[x-test-pull-count]").text() + " downloads"
                : "N/A";

        // Selector para la fecha de última actualización del modelo
        String lastUpdatedGlobal = doc.selectFirst("span[x-test-updated]") != null
                ? doc.selectFirst("span[x-test-updated]").text()
                : "N/A";

        // Selector para el contenedor de la lista de tags
        Element tagsContainer = doc.selectFirst("div.min-w-full.divide-y");
        if (tagsContainer == null) {
            return modelTags; // No se encontró el contenedor, devolvemos la lista vacía.
        }

        // Selector para cada fila de tag (solo las de escritorio, que están más
        // estructuradas)
        Elements tagRows = tagsContainer.select("div.hidden.sm\\:grid");

        for (Element row : tagRows) {
            // Extraemos los datos de cada columna de la fila
            // La estructura parece ser: Name | Size | Context | Input (según el usuario)
            // Vamos a intentar extraerlos por índice de columna si es posible, o por
            // selectores específicos.
            // Asumiremos que son elementos <p> o <div> dentro del grid.

            // El nombre suele estar en un <a>
            String fullTagName = row.select("a").first().text();

            // Los otros datos suelen estar en elementos hermanos.
            // En la web de Ollama, el layout es un grid.
            // Col 1: Name (span-2)
            // Col 2: Size
            // Col 3: Hash (a veces) -> Ahora parece que es Context?
            // Col 4: Update time

            // Basado en el request del usuario, parece que la web ha cambiado o él ve otra
            // cosa.
            // "Name, Size, Context, Input"

            // Vamos a intentar coger todos los textos de la fila y asignarlos.
            Elements columns = row.select("> *"); // Hijos directos del grid row

            // Fallback defaults
            String size = "N/A";
            String context = "Unknown";
            String input = "Text";

            // Intentamos parsear basado en la observación del usuario
            // Si hay suficientes columnas, las mapeamos.
            // Nota: El scraping es frágil.

            // Estrategia actual: Mantener lo que funcionaba y añadir placeholders o
            // intentar extraer si vemos patrones claros.
            // En la implementación anterior:
            // String size = row.select("p.col-span-2").get(0).text();

            // Vamos a intentar ser más robustos.
            // Buscamos elementos que parezcan el tamaño (contienen B, GB, MB).
            for (Element col : columns) {
                String text = col.text();
                if (text.matches(".*\\d+(\\.\\d+)?[GMK]B.*")) {
                    size = text;
                } else if (text.contains("K") && text.length() < 10) { // Posible context length (e.g. 128K)
                    context = text;
                } else if (text.equals("Text") || text.equals("Image") || text.equals("Multimodal")) {
                    input = text;
                }
            }

            // El nombre del tag es la parte después de los dos puntos
            String tagName = fullTagName.contains(":") ? fullTagName.split(":")[1] : fullTagName;

            modelTags.add(new OllamaModel(modelName, description, pullCount, tagName, size, lastUpdatedGlobal, context,
                    input));
        }

        return modelTags;
    }

    private String formatSize(long size) {
        if (size <= 0)
            return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public interface ProgressCallback {
        void onProgress(double progress, String status);
    }

    /**
     * Descarga un modelo desde Ollama.
     * Nota: La librería ollama4j tiene soporte limitado para callbacks de progreso
     * en algunas versiones.
     * Si no soporta callbacks granulares, simularemos o usaremos lo que haya.
     * 
     * @param modelName Nombre del modelo
     * @param tag       Tag del modelo
     * @param callback  Callback para actualizar la UI
     */
    public void pullModel(String modelName, String tag, ProgressCallback callback) throws Exception {
        String fullName = modelName + ":" + tag;

        // Usamos ProcessBuilder para ejecutar "ollama pull" y leer la salida
        ProcessBuilder builder = new ProcessBuilder("ollama", "pull", fullName);
        builder.redirectErrorStream(true); // Combinar stderr y stdout

        Process process = builder.start();

        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                // Parsear la línea para extraer progreso
                // Formatos típicos de Ollama:
                // "pulling manifest"
                // "pulling <hash>... 100%"
                // "verifying sha256 digest"
                // "success"

                String status = line;
                double progress = -1; // Indeterminado por defecto

                if (line.contains("%")) {
                    // Intentar extraer el porcentaje
                    try {
                        // Buscar el último número antes del %
                        int percentIndex = line.lastIndexOf('%');
                        // Retroceder para encontrar el inicio del número
                        int start = percentIndex - 1;
                        while (start >= 0 && (Character.isDigit(line.charAt(start)) || line.charAt(start) == '.')) {
                            start--;
                        }
                        String numStr = line.substring(start + 1, percentIndex).trim();
                        progress = Double.parseDouble(numStr);
                    } catch (Exception e) {
                        // Ignorar errores de parsing, mantener progreso actual o indeterminado
                    }
                } else if (line.contains("success")) {
                    progress = 100;
                }

                if (callback != null) {
                    callback.onProgress(progress, status);
                }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("Ollama pull failed with exit code: " + exitCode);
        }
    }

    public void deleteModel(String modelName, String tag) throws Exception {
        String fullName = modelName + ":" + tag;
        System.out.println("OllamaManager: Executing 'ollama rm " + fullName + "'");
        ProcessBuilder builder = new ProcessBuilder("ollama", "rm", fullName);
        builder.redirectErrorStream(true);
        Process process = builder.start();

        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Ollama rm output: " + line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("Ollama rm failed with exit code: " + exitCode);
        }
        System.out.println("OllamaManager: Model " + fullName + " deleted successfully.");
    }

    /**
     * Sends a prompt to the specified model and returns the response.
     * This is a synchronous blocking call.
     *
     * @param modelName The name of the model (e.g., "llama3:latest")
     * @param prompt    The user's message
     * @return The AI's response text
     * @throws Exception If the request fails
     */
    public String askModel(String modelName, String prompt) throws Exception {
        System.out.println("OllamaManager: Asking " + modelName + ": " + prompt);

        // Create a chat message object
        // We need to use the Chat API, not Generate, for better compatibility and
        // future history support.
        java.util.List<io.github.ollama4j.models.chat.OllamaChatMessage> messages = new java.util.ArrayList<>();
        messages.add(new io.github.ollama4j.models.chat.OllamaChatMessage(
                io.github.ollama4j.models.chat.OllamaChatMessageRole.USER, prompt));

        io.github.ollama4j.models.chat.OllamaChatRequest request = io.github.ollama4j.models.chat.OllamaChatRequestBuilder
                .getInstance(modelName).withMessages(messages).build();

        io.github.ollama4j.models.chat.OllamaChatResult result = client.chat(request);
        return result.getResponse();
    }
}