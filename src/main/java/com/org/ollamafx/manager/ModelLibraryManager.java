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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
     * Invalidates the in-memory cache, forcing OUTDATED_HARD status on next
     * getUpdateStatus() call.
     * Used when refreshing library from Settings.
     */
    public void invalidateCache() {
        this.currentLibrary = new LibraryCache();
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

        if (days > 10)
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
            ProcessBuilder pb = new ProcessBuilder(Utils.getOllamaExecutable(), "--version");
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

        Set<String> uniqueModels = discoverAllModelNames();

        if (uniqueModels.isEmpty()) {
            System.err.println("ModelLibraryManager: No se encontraron modelos.");
            currentStatus = "Error de conexión.";
            return;
        }

        int total = uniqueModels.size();

        // --- FASE 2: PROCESAMIENTO ---
        List<OllamaModel> allFoundModels = new ArrayList<>();
        List<String> sortedList = new ArrayList<>(uniqueModels);
        Collections.sort(sortedList);

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

        // Finalize - sort and categorize
        currentStatus = "Guardando librería...";

        // ALL models
        currentLibrary.setAllModels(allFoundModels);

        // POPULAR: Sort by pull count (most downloads first)
        List<OllamaModel> popular = new ArrayList<>(allFoundModels);
        popular.sort((a, b) -> {
            long countA = parsePullCount(a.getPullCount());
            long countB = parsePullCount(b.getPullCount());
            return Long.compare(countB, countA); // Descending
        });
        currentLibrary.setPopularModels(popular.size() > 20 ? popular.subList(0, 20) : popular);

        // NEW: Sort by lastUpdated date (newest first)
        List<OllamaModel> newest = new ArrayList<>(allFoundModels);
        newest.sort((a, b) -> {
            long dateA = parseRelativeDate(a.getLastUpdated());
            long dateB = parseRelativeDate(b.getLastUpdated());
            return Long.compare(dateB, dateA); // Descending (most recent first)
        });
        currentLibrary.setNewModels(newest.size() > 20 ? newest.subList(0, 20) : newest);

        currentLibrary.setLastUpdated(System.currentTimeMillis());
        cacheManager.saveCache(currentLibrary);

        currentStatus = "Completado.";
        currentProgress = 1.0;
    }

    /**
     * Parse pull count string like "1.2M", "500K", "1000" to long
     */
    private long parsePullCount(String pullCount) {
        if (pullCount == null || pullCount.isEmpty())
            return 0;
        try {
            String clean = pullCount.toUpperCase().trim();
            double value = Double.parseDouble(clean.replaceAll("[^0-9.]", ""));
            if (clean.contains("M")) {
                return (long) (value * 1_000_000);
            } else if (clean.contains("K")) {
                return (long) (value * 1_000);
            }
            return (long) value;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Parse relative date like "2 days ago", "1 month ago" to timestamp
     */
    private long parseRelativeDate(String relativeDate) {
        if (relativeDate == null || relativeDate.isEmpty())
            return 0;
        try {
            String lower = relativeDate.toLowerCase().trim();
            long now = System.currentTimeMillis();

            java.util.regex.Pattern p = java.util.regex.Pattern
                    .compile("(\\d+)\\s+(second|minute|hour|day|week|month|year)");
            java.util.regex.Matcher m = p.matcher(lower);

            if (m.find()) {
                int amount = Integer.parseInt(m.group(1));
                String unit = m.group(2);
                long ms = switch (unit) {
                    case "second" -> 1000L;
                    case "minute" -> 60 * 1000L;
                    case "hour" -> 60 * 60 * 1000L;
                    case "day" -> 24 * 60 * 60 * 1000L;
                    case "week" -> 7 * 24 * 60 * 60 * 1000L;
                    case "month" -> 30L * 24 * 60 * 60 * 1000L;
                    case "year" -> 365L * 24 * 60 * 60 * 1000L;
                    default -> 0L;
                };
                return now - (amount * ms);
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private Set<String> discoverAllModelNames() {
        Set<String> uniqueNames = new HashSet<>();
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
