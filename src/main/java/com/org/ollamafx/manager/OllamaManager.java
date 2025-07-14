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
                OllamaModel localModel = new OllamaModel(baseName, "Installed locally", "N/A", tag, formatSize(model.getSize()), formattedDate);
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
            return baseModels.stream().map(libraryModel -> new OllamaModel(libraryModel.getName(), "", "", "", "", "")).collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // --- MÉTODO DE SCRAPING FINAL Y CORREGIDO ---

    /**
     * VERSIÓN FINAL con selectores de CSS ajustados al HTML real.
     * @param modelName El nombre del modelo a buscar (ej: "gemma3n").
     * @return Una lista de OllamaModel, donde cada uno representa un tag.
     * @throws IOException Si la conexión a la página web falla.
     */
    public List<OllamaModel> scrapeModelDetails(String modelName) throws IOException {
        List<OllamaModel> modelTags = new ArrayList<>();
        String url = "https://ollama.com/library/" + modelName;

        Document doc = Jsoup.connect(url).get();

        // Selector para la descripción principal del modelo
        String description = doc.selectFirst("#summary-content") != null ? doc.selectFirst("#summary-content").text() : "No description available.";

        // Selector para el contador de descargas (pulls)
        String pullCount = doc.selectFirst("span[x-test-pull-count]") != null ? doc.selectFirst("span[x-test-pull-count]").text() + " downloads" : "N/A";

        // Selector para la fecha de última actualización del modelo
        String lastUpdatedGlobal = doc.selectFirst("span[x-test-updated]") != null ? doc.selectFirst("span[x-test-updated]").text() : "N/A";

        // Selector para el contenedor de la lista de tags
        Element tagsContainer = doc.selectFirst("div.min-w-full.divide-y");
        if (tagsContainer == null) {
            return modelTags; // No se encontró el contenedor, devolvemos la lista vacía.
        }

        // Selector para cada fila de tag (solo las de escritorio, que están más estructuradas)
        Elements tagRows = tagsContainer.select("div.hidden.sm\\:grid");

        for (Element row : tagRows) {
            // Extraemos los datos de cada columna de la fila
            String fullTagName = row.select("a").first().text();
            String size = row.select("p.col-span-2").get(0).text();

            // El nombre del tag es la parte después de los dos puntos
            String tagName = fullTagName.contains(":") ? fullTagName.split(":")[1] : fullTagName;

            modelTags.add(new OllamaModel(modelName, description, pullCount, tagName, size, lastUpdatedGlobal));
        }

        return modelTags;
    }

    private String formatSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}