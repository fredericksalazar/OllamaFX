package com.org.ollamafx.manager;

import com.org.ollamafx.model.OllamaModel;
import com.org.ollamafx.util.SecurityUtils;
import com.org.ollamafx.util.Utils;

import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.models.response.LibraryModel;
import io.github.ollama4j.models.response.Model;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.time.Duration;
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
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;
import io.github.ollama4j.models.chat.OllamaChatMessage;
import io.github.ollama4j.models.chat.OllamaChatMessageRole;
import io.github.ollama4j.models.chat.OllamaChatRequest;
import io.github.ollama4j.models.chat.OllamaChatRequestBuilder;
import io.github.ollama4j.models.chat.OllamaChatResult;
import io.github.ollama4j.models.generate.OllamaStreamHandler;

public class OllamaManager {

    private static OllamaManager instance;
    private OllamaAPI client;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private volatile InputStream activeStream; // To support forceful cancellation

    private OllamaManager() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper();
        updateClient();
    }

    public void updateClient() {
        String host = ConfigManager.getInstance().getOllamaHost();
        if (host == null || host.isEmpty()) {
            host = "http://127.0.0.1:11434";
        }
        this.client = new OllamaAPI(host);
        this.client.setRequestTimeoutSeconds(60);
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
                        Utils.formatSize(model.getSize()), formattedDate);
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
        if (sort != null && !sort.isEmpty() && !SecurityUtils.isValidModelName(sort)) {
            return new ArrayList<>();
        }
        List<OllamaModel> models = new ArrayList<>();
        String url = "https://ollama.com/library" + (sort != null && !sort.isEmpty() ? "?sort=" + sort : "");

        try {
            Document doc = Jsoup.connect(url).get();
            Elements items = doc.select("li a.group");

            if (items.isEmpty()) {
                items = doc.select("ul li a[href^='/library/']");
            }

            for (Element item : items) {
                String href = item.attr("href");
                String name = href.replace("/library/", "");

                // Extract title/name

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
                        "Unknown", "Text", badges, "", null);
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
        if (!SecurityUtils.isValidModelName(modelName)) {
            throw new IllegalArgumentException("Invalid model name.");
        }
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
                    "Unknown", "Text", badges, readmeContent, null));
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

            modelTags.add(new OllamaModel(modelName, description, pullCount,
                    fullTagName.contains(":") ? fullTagName.split(":")[1] : fullTagName,
                    size, lastUpdatedGlobal, context, input, badges, readmeContent, null));
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
        if (!SecurityUtils.isValidModelName(modelName) ||
                !SecurityUtils.isValidModelName(tag)) {
            throw new IllegalArgumentException("Invalid model name or tag.");
        }
        String fullName = modelName + ":" + tag;

        // Usamos ProcessBuilder para ejecutar "ollama pull" y leer la salida
        ProcessBuilder builder = new ProcessBuilder(Utils.getOllamaExecutable(), "pull",
                fullName);
        builder.redirectErrorStream(true); // Combinar stderr y stdout

        Process process = builder.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {

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
        if (!SecurityUtils.isValidModelName(modelName) ||
                !SecurityUtils.isValidModelName(tag)) {
            throw new IllegalArgumentException("Invalid model name or tag.");
        }
        String fullName = modelName + ":" + tag;
        ProcessBuilder builder = new ProcessBuilder(Utils.getOllamaExecutable(), "rm", fullName);
        builder.redirectErrorStream(true);
        Process process = builder.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            while (reader.readLine() != null) {
                // Drain the buffer
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("Ollama rm failed with exit code: " + exitCode);
        }
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
    /**
     * @deprecated Use {@link #askModelStream} instead. This synchronous method is
     *             no longer called.
     */
    @Deprecated
    public String askModel(String modelName, String prompt) throws Exception {

        // Create a chat message object
        // We need to use the Chat API, not Generate, for better compatibility and
        // future history support.
        List<OllamaChatMessage> messages = new ArrayList<>();
        messages.add(new OllamaChatMessage(OllamaChatMessageRole.USER, prompt));

        OllamaChatRequest request = OllamaChatRequestBuilder
                .getInstance(modelName).withMessages(messages).build();

        OllamaChatResult result = client.chat(request);
        return result.getResponse();
    }

    public void askModelStream(String modelName, String prompt, List<String> images,
            Map<String, Object> requestOptions, String systemPrompt,
            OllamaStreamHandler handler)
            throws Exception {

        // Build Payload manually
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", modelName);
        payload.put("stream", true);

        List<Map<String, Object>> messages = new ArrayList<>();

        // Some vision models don't support system prompts alongside images.
        // Only include system prompt when there are no images.
        boolean hasImages = images != null && !images.isEmpty();
        if (!hasImages && systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            Map<String, Object> sysMsg = new HashMap<>();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
            messages.add(sysMsg);
        }

        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        // Ensure content is never empty — use a default for image-only messages
        String messageContent = (prompt != null && !prompt.trim().isEmpty())
                ? prompt
                : (hasImages ? "What is in this image?" : prompt);
        userMsg.put("content", messageContent);
        if (hasImages) {
            userMsg.put("images", images);
        }
        messages.add(userMsg);

        payload.put("messages", messages);

        // Options: Merge any defaults if needed, but here we assume requestOptions is
        // complete or null
        if (requestOptions != null) {
            payload.put("options", requestOptions);
        } else {
            // Fallback default
            Map<String, Object> defaultOptions = new HashMap<>();
            defaultOptions.put("temperature", 0.7);
            payload.put("options", defaultOptions);
        }

        String jsonBody = mapper.writeValueAsString(payload);
        // Debug: log payload (truncate images for readability)
        if (hasImages) {
            System.out.println("[OllamaFX] Sending multimodal request to model: " + modelName
                    + ", images count: " + images.size());
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ConfigManager.getInstance().getOllamaHost() + "/api/chat"))
                .header("Content-Type", "application/json")
                .timeout(java.time.Duration.ofSeconds(ConfigManager.getInstance().getApiTimeout()))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<InputStream> response;
        try {
            // Use send (blocking) but handle interruption gracefully
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (java.net.http.HttpTimeoutException e) {
            System.err.println("[OllamaFX] API Timeout: " + e.getMessage());
            int timeoutVal = ConfigManager.getInstance().getApiTimeout();
            String lang = ConfigManager.getInstance().getLanguage();
            java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("messages",
                    new java.util.Locale(lang));
            String errorMsg = bundle.getString("error.timeout").replace("{0}", String.valueOf(timeoutVal));
            throw new Exception(errorMsg);
        }

        if (response.statusCode() != 200) {
            // Read error body for diagnostics
            String errorBody = "";
            try (var errorReader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                errorBody = errorReader.lines().collect(Collectors.joining("\n"));
            } catch (Exception ignored) {
            }

            // Try to parse JSON to get a clean error string for the UI
            String displayError = errorBody;
            try {
                com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(errorBody);
                if (node.has("error")) {
                    displayError = node.get("error").asText();
                }
            } catch (Exception e) {
                // Fallback to raw errorBody if not JSON
            }

            System.err.println("[OllamaFX] API Error " + response.statusCode() + ": " + errorBody);
            throw new Exception(displayError);
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
                    throw re;
                } catch (Exception e) {
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