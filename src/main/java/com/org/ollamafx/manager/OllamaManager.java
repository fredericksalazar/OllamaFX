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
// import java.text.DecimalFormat; // Unused
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.HashMap;

public class OllamaManager {

    private static OllamaManager instance;
    private final OllamaAPI client;
    private volatile java.io.InputStream activeStream; // To support forceful cancellation

    private OllamaManager() {
        String host = ConfigManager.getInstance().getOllamaHost();
        // Ensure host is valid or default if empty
        if (host == null || host.isEmpty()) {
            host = "http://localhost:11434";
        }
        this.client = new OllamaAPI(host);
        this.client.setRequestTimeoutSeconds(60); // Set a reasonable timeout
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
                        com.org.ollamafx.util.Utils.formatSize(model.getSize()), formattedDate);
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

    // --- SCRAPING LIBRARY LISTS ---

    public List<OllamaModel> getLibraryModels(String sort) {
        List<OllamaModel> models = new ArrayList<>();
        String url = "https://ollama.com/library" + (sort != null && !sort.isEmpty() ? "?sort=" + sort : "");

        try {
            Document doc = Jsoup.connect(url).get();
            Elements items = doc.select("li a.group"); // Selector based on observation or standard list item structure

            // If the structure is different, let's use a more generic selector for the list
            // items
            // Based on view_content_chunk, it seems to be a list of links.
            // Let's suspect "li" or "div" containers. Re-checking chunk 20-30...
            // Actually, I'll use a robust selector strategy below.

            if (items.isEmpty()) {
                // Fallback: try different selector if the first one fails, or just elements
                // with specific classes
                items = doc.select("ul li a[href^='/library/']");
            }

            for (Element item : items) {
                String href = item.attr("href");
                String name = href.replace("/library/", "");

                // Extract title/name
                Element titleEl = item.selectFirst("h2");
                String displayName = (titleEl != null) ? titleEl.text() : name;

                // Extract description
                Element descEl = item.selectFirst("p");
                String description = (descEl != null) ? descEl.text() : "";

                // Extract pull count and badges
                String pullCount = "N/A";
                String lastUpdated = "N/A";
                List<String> badges = new ArrayList<>();

                Elements spans = item.select("span");
                for (Element span : spans) {
                    String text = span.text();
                    if (text.contains("Provides") || text.contains("param") || text.contains("B")) { // Likely a badge
                                                                                                     // like "7B" or
                                                                                                     // "Text"
                        badges.add(text);
                    }
                    if (text.matches(".*[kKmMbB]$") || text.contains("pulls")) { // Likely pull count
                        pullCount = text.replace("pulls", "").trim();
                    }
                    if (text.contains("ago")) {
                        lastUpdated = text;
                    }
                }

                // Create a model object. Using name as base. Tag we can assume "latest" or
                // leave generic.
                OllamaModel model = new OllamaModel(name, description, pullCount, "latest", "N/A", lastUpdated,
                        "Unknown", "Text", badges, "");
                models.add(model);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return models;
    }

    // --- MÉTODO DE SCRAPING FINAL Y CORREGIDO (DETALLES) ---

    /**
     * VERSIÓN FINAL con selectores de CSS ajustados al HTML real.
     * 
     * @param modelName El nombre del modelo a buscar (ej: "gemma3n").
     * @return Una lista de OllamaModel, donde cada uno representa un tag.
     * @throws IOException Si la conexión a la página web falla.
     */
    public List<OllamaModel> scrapeModelDetails(String modelName) throws IOException {
        String url = "https://ollama.com/library/" + modelName;

        Document doc = Jsoup.connect(url).get();

        String description = doc.selectFirst("#summary-content") != null ? doc.selectFirst("#summary-content").text()
                : "No description available.";

        String pullCount = doc.selectFirst("span[x-test-pull-count]") != null
                ? doc.selectFirst("span[x-test-pull-count]").text() + " downloads"
                : "N/A";

        String lastUpdatedGlobal = doc.selectFirst("span[x-test-updated]") != null
                ? doc.selectFirst("span[x-test-updated]").text()
                : "N/A";

        List<String> badges = extractBadges(doc);
        String readmeContent = extractReadme(doc);

        return extractTags(doc, modelName, description, pullCount, lastUpdatedGlobal, badges, readmeContent);
    }

    private List<String> extractBadges(Document doc) {
        List<String> badges = new ArrayList<>();
        Element badgesContainer = doc.selectFirst("div.flex.flex-wrap.space-x-2");

        if (badgesContainer != null) {
            Elements badgeElements = badgesContainer.select("span");
            for (Element badge : badgeElements) {
                String badgeText = badge.text().trim();
                if (!badgeText.isEmpty() && !badges.contains(badgeText)) {
                    badges.add(badgeText);
                }
            }
        }
        return badges;
    }

    private String extractReadme(Document doc) {
        Element readmeElement = doc.selectFirst("#readme");
        if (readmeElement == null) {
            readmeElement = doc.selectFirst(".prose");
        }
        return (readmeElement != null) ? readmeElement.html() : "No README available.";
    }

    private List<OllamaModel> extractTags(Document doc, String modelName, String description, String pullCount,
            String lastUpdatedGlobal, List<String> badges, String readmeContent) {
        List<OllamaModel> modelTags = new ArrayList<>();
        Element tagsContainer = doc.selectFirst("div.min-w-full.divide-y");

        if (tagsContainer == null) {
            modelTags.add(new OllamaModel(modelName, description, pullCount, "latest", "N/A", lastUpdatedGlobal,
                    "Unknown", "Text", badges, readmeContent));
            return modelTags;
        }

        Elements tagRows = tagsContainer.select("div.hidden.sm\\:grid");

        for (Element row : tagRows) {
            String fullTagName = row.select("a").first().text();
            Elements columns = row.select("> *");

            String size = "N/A";
            String context = "Unknown";
            String input = "Text";

            for (Element col : columns) {
                String text = col.text();
                if (text.matches(".*\\d+(\\.\\d+)?[GMK]B.*")) {
                    size = text;
                } else if (text.contains("K") && text.length() < 10) {
                    context = text;
                } else if (text.equals("Text") || text.equals("Image") || text.equals("Multimodal")) {
                    input = text;
                }
            }

            String tagName = fullTagName.contains(":") ? fullTagName.split(":")[1] : fullTagName;

            modelTags.add(new OllamaModel(modelName, description, pullCount, tagName, size, lastUpdatedGlobal, context,
                    input, badges, readmeContent));
        }
        return modelTags;
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
        if (!com.org.ollamafx.util.SecurityUtils.isValidModelName(modelName) ||
                !com.org.ollamafx.util.SecurityUtils.isValidModelName(tag)) {
            throw new IllegalArgumentException("Invalid model name or tag.");
        }
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
        if (!com.org.ollamafx.util.SecurityUtils.isValidModelName(modelName) ||
                !com.org.ollamafx.util.SecurityUtils.isValidModelName(tag)) {
            throw new IllegalArgumentException("Invalid model name or tag.");
        }
        String fullName = modelName + ":" + tag;
        System.out.println("OllamaManager: Executing 'ollama rm "
                + com.org.ollamafx.util.SecurityUtils.sanitizeForLog(fullName) + "'");
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

    public void askModelStream(String modelName, String prompt, double temperature, String systemPrompt,
            io.github.ollama4j.models.generate.OllamaStreamHandler handler)
            throws Exception {

        // Build Payload manually
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", modelName);
        payload.put("stream", true);

        List<Map<String, String>> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            Map<String, String> sysMsg = new HashMap<>();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
            messages.add(sysMsg);
        }
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.add(userMsg);

        payload.put("messages", messages);

        Map<String, Object> options = new HashMap<>();
        options.put("temperature", temperature);
        payload.put("options", options);

        ObjectMapper mapper = new ObjectMapper();
        String jsonBody = mapper.writeValueAsString(payload);

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ConfigManager.getInstance().getOllamaHost() + "/api/chat"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        // Use send (blocking) but handle interruption gracefully
        HttpResponse<java.io.InputStream> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new Exception("Ollama API Error: " + response.statusCode());
        }

        this.activeStream = response.body(); // Capture stream

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(activeStream, StandardCharsets.UTF_8))) {
            String line;
            StringBuilder fullContent = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                if (Thread.currentThread().isInterrupted()) {
                    return; // Stop reading
                }

                try {
                    JsonNode node = mapper.readTree(line);
                    if (node.has("message")) {
                        JsonNode msgNode = node.get("message");
                        if (msgNode.has("content")) {
                            String content = msgNode.get("content").asText();
                            // If I send DELTAS now, the UI will just flash single characters.
                            // So I MUST ACCUMULATE here before calling handler.accept

                            fullContent.append(content);
                            handler.accept(fullContent.toString());
                        }
                    }
                    if (node.has("done") && node.get("done").asBoolean()) {
                        break;
                    }
                } catch (RuntimeException re) {
                    // Re-throw (e.g., Cancelled by user)
                    System.out.println(
                            "OllamaManager: RuntimeException caught in loop (likely cancel): " + re.getMessage());
                    throw re;
                } catch (Exception e) {
                    System.out.println("OllamaManager: Parsing error ignored: " + e.getMessage());
                }
            }
        } finally {
            this.activeStream = null; // Clean up
        }
    }

    public void cancelCurrentRequest() {
        if (activeStream != null) {
            try {
                activeStream.close(); // This will throw IOException in the read loop
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}