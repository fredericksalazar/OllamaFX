package com.org.ollamafx.manager;

import com.org.ollamafx.model.LibraryCache;
import com.org.ollamafx.model.OllamaModel;
import com.org.ollamafx.util.Utils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ModelLibraryManager {

    private static ModelLibraryManager instance;
    private final OllamaManager ollamaManager = OllamaManager.getInstance();
    private final LibraryCacheManager cacheManager = LibraryCacheManager.getInstance();
    private LibraryCache currentLibrary;

    // Status tracking for UI progress
    private volatile double currentProgress = 0.0;
    private volatile String currentStatus = "";
    private final AtomicBoolean isCancelling = new AtomicBoolean(false);

    private ModelLibraryManager() {
        this.currentLibrary = cacheManager.loadCache();
        if (this.currentLibrary == null) {
            this.currentLibrary = new LibraryCache();
        }
    }

    public static synchronized ModelLibraryManager getInstance() {
        if (instance == null) {
            instance = new ModelLibraryManager();
        }
        return instance;
    }

    /**
     * Enum for update advice.
     */
    public enum UpdateStatus {
        UP_TO_DATE,
        OUTDATED_SOFT, // > 5 days
        OUTDATED_HARD // > 15 days or missing
    }

    public UpdateStatus getUpdateStatus() {
        if (currentLibrary == null || currentLibrary.getAllModels().isEmpty() || currentLibrary.getLastUpdated() == 0) {
            return UpdateStatus.OUTDATED_HARD;
        }

        long ageMs = System.currentTimeMillis() - currentLibrary.getLastUpdated();
        long days = ageMs / (1000 * 60 * 60 * 24);

        if (days > 15)
            return UpdateStatus.OUTDATED_HARD;
        if (days > 5)
            return UpdateStatus.OUTDATED_SOFT;

        return UpdateStatus.UP_TO_DATE;
    }

    /**
     * Checks if Ollama is installed by running 'ollama --version'.
     */
    public boolean isOllamaInstalled() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ollama", "--version");
            Process p = pb.start();
            int exitCode = p.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public LibraryCache getLibrary() {
        return currentLibrary;
    }

    public double getProgress() {
        return currentProgress;
    }

    public String getStatus() {
        return currentStatus;
    }

    public void cancelUpdate() {
        isCancelling.set(true);
    }

    /**
     * Crawler Logic:
     * 1. Scrape "Popular" list from Page 1 to N until empty.
     * 2. For every model found, scrape its DETAILS page to get Tags/Sizes.
     * 3. Classify every tag.
     * 4. Save to Disk.
     */
    /**
     * Lógica del Crawler (Bifásica):
     * Fase 1: Descubrimiento - Escanear todas las páginas para obtener el total de
     * modelos.
     * Fase 2: Procesamiento - Descargar detalles uno por uno con progreso exacto.
     */
    public void updateLibraryFull() throws Exception {
        isCancelling.set(false);
        currentProgress = -1.0;

        // --- FASE 1: DESCUBRIMIENTO ---
        currentStatus = "Descubriendo catálogo...";
        System.out.println("ModelLibraryManager: Iniciando Fase de Descubrimiento...");

        java.util.Set<String> uniqueModels = discoverAllModelNames();

        if (uniqueModels.isEmpty()) {
            System.err.println("ModelLibraryManager: No se encontraron modelos.");
            currentStatus = "Error de conexión.";
            return;
        }

        int total = uniqueModels.size();
        System.out.println("ModelLibraryManager: Descubrimiento Finalizado. Total: " + total);

        // --- FASE 2: PROCESAMIENTO ---
        List<OllamaModel> allFoundModels = new ArrayList<>();
        List<String> sortedList = new ArrayList<>(uniqueModels);
        java.util.Collections.sort(sortedList);

        int current = 0;

        for (String name : sortedList) {
            if (isCancelling.get()) {
                currentStatus = "Cancelado.";
                return;
            }

            // Format: "Procesando (X/Y): Name" - Parsed by SplashController
            currentStatus = String.format("Procesando (%d/%d): %s", (current + 1), total, name);

            try {
                // Fetch Details
                List<OllamaModel> tags = ollamaManager.scrapeModelDetails(name);

                // Classify
                for (OllamaModel tag : tags) {
                    ModelManager.getInstance().classifyModel(tag);
                }

                if (!tags.isEmpty()) {
                    ModelDetailsCacheManager.getInstance().saveDetails(name, tags);
                    // Use the first tag (usually latest/popular) as the card representation
                    allFoundModels.add(tags.get(0));
                }

            } catch (Exception e) {
                System.err.println("Error procesando modelo " + name + ": " + e.getMessage());
            }

            current++;
            currentProgress = (double) current / total;
        }

        // Finalize
        currentStatus = "Guardando librería...";
        currentLibrary.setAllModels(allFoundModels);
        currentLibrary.setPopularModels(allFoundModels);
        currentLibrary.setLastUpdated(System.currentTimeMillis());
        cacheManager.saveCache(currentLibrary);

        currentStatus = "Completado.";
        currentProgress = 1.0;
    }

    private java.util.Set<String> discoverAllModelNames() {
        java.util.Set<String> uniqueNames = new java.util.HashSet<>();
        int page = 1;
        boolean hasMore = true;
        int MAX_PAGES = 500;

        while (hasMore && !isCancelling.get() && page <= MAX_PAGES) {
            try {
                // Status for Phase 1
                currentStatus = "Escaneando página " + page + "...";

                List<String> names = scrapeModelNamesFromPage(page);

                if (names.isEmpty()) {
                    hasMore = false;
                } else {
                    int newFound = 0;
                    for (String n : names) {
                        if (uniqueNames.add(n))
                            newFound++;
                    }
                    if (newFound == 0)
                        hasMore = false; // End of list (duplicates met)
                    else
                        page++;
                }
                Thread.sleep(50);
            } catch (Exception e) {
                hasMore = false;
            }
        }
        return uniqueNames;
    }

    private List<String> scrapeModelNamesFromPage(int page) throws IOException {
        String url = "https://ollama.com/library?sort=popular&page=" + page;
        Document doc = Jsoup.connect(url).userAgent("OllamaFX/1.0").get();

        List<String> names = new ArrayList<>();
        // Select list items. Based on Ollama site structure:
        // usually <ul> with <li> <a> ...
        // Reusing logic from OllamaManager but simplified for just Names
        Elements links = doc.select("li a[href^='/library/']");

        for (Element link : links) {
            String href = link.attr("href");
            // href is like "/library/llama3"
            String name = href.replace("/library/", "");
            if (!name.isEmpty()) {
                names.add(name);
            }
        }
        return names;
    }
}
