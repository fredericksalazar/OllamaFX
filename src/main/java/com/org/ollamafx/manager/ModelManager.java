package com.org.ollamafx.manager;

import com.org.ollamafx.model.OllamaModel;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import java.util.List;

public class ModelManager {

    private static ModelManager instance;
    private final ObservableList<OllamaModel> localModels = FXCollections.observableArrayList();
    private final ObservableList<OllamaModel> availableModels = FXCollections.observableArrayList();
    private final OllamaManager ollamaManager = OllamaManager.getInstance();
    private final java.util.Set<String> installedModelsCache = new java.util.HashSet<>();

    private final ObservableList<OllamaModel> popularModels = FXCollections.observableArrayList();
    private final ObservableList<OllamaModel> newModels = FXCollections.observableArrayList();
    private final ObservableList<OllamaModel> recommendedModels = FXCollections.observableArrayList();

    private ModelManager() {
        // Keep cache in sync
        localModels.addListener((javafx.collections.ListChangeListener<OllamaModel>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (OllamaModel m : c.getAddedSubList()) {
                        installedModelsCache.add(m.getName().toLowerCase() + ":" + m.getTag().toLowerCase());
                    }
                }
                if (c.wasRemoved()) {
                    for (OllamaModel m : c.getRemoved()) {
                        installedModelsCache.remove(m.getName().toLowerCase() + ":" + m.getTag().toLowerCase());
                    }
                }
            }
        });
    }

    public static synchronized ModelManager getInstance() {
        if (instance == null) {
            instance = new ModelManager();
        }
        return instance;
    }

    public ObservableList<OllamaModel> getLocalModels() {
        return localModels;
    }

    public ObservableList<OllamaModel> getAvailableModels() {
        return availableModels;
    }

    public ObservableList<OllamaModel> getPopularModels() {
        return popularModels;
    }

    public ObservableList<OllamaModel> getNewModels() {
        return newModels;
    }

    public ObservableList<OllamaModel> getRecommendedModels() {
        return recommendedModels;
    }

    /**
     * Verifica si un modelo específico (nombre y tag) está instalado localmente.
     * Uses O(1) cache.
     */
    public boolean isModelInstalled(String name, String tag) {
        if (name == null || tag == null)
            return false;
        return installedModelsCache.contains(name.toLowerCase() + ":" + tag.toLowerCase());
    }

    /**
     * Carga modelos desde el caché local o inicia una actualización en background
     * si es necesario.
     */
    public void loadLibraryModels() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // 1. Load from cache immediately
                com.org.ollamafx.model.LibraryCache cache = LibraryCacheManager.getInstance().loadCache();

                if (cache != null) {
                    System.out.println("ModelManager: Loaded data from local cache.");
                    Platform.runLater(() -> {
                        if (cache.getPopularModels() != null)
                            popularModels.setAll(cache.getPopularModels());
                        if (cache.getNewModels() != null)
                            newModels.setAll(cache.getNewModels());
                        if (cache.getAllModels() != null)
                            availableModels.setAll(cache.getAllModels());
                    });

                    // Generate recommendations immediately from cached data
                    ModelManager.this.generateRecommendations(cache.getPopularModels(), cache.getNewModels());
                } else {
                    System.out.println("ModelManager: No local cache found.");
                }

                // 2. Check if update is needed (expired or missing)
                if (!LibraryCacheManager.getInstance().isCacheValid(cache)) {
                    System.out.println("ModelManager: Cache invalid or missing. Starting background refresh...");
                    ModelManager.this.refreshLibraryCache();
                }

                return null;
            }
        };
        new Thread(task).start();
    }

    private void refreshLibraryCache() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Fetch all data
                System.out.println("ModelManager: Scraping fresh data...");
                List<OllamaModel> popular = ollamaManager.getLibraryModels("popular");
                List<OllamaModel> newest = ollamaManager.getLibraryModels("newest");

                // SORTING & LIMITING LOGIC
                // Popular: Sort by downloads descending
                popular.sort((m1, m2) -> {
                    long d1 = com.org.ollamafx.util.Utils.parseDownloadCount(m1.getPullCount());
                    long d2 = com.org.ollamafx.util.Utils.parseDownloadCount(m2.getPullCount());
                    return Long.compare(d2, d1); // Descending
                });

                // Newest: Sort by date descending (using scraped relative date)
                newest.sort((m1, m2) -> {
                    long t1 = com.org.ollamafx.util.Utils.parseRelativeDate(m1.getLastUpdated());
                    long t2 = com.org.ollamafx.util.Utils.parseRelativeDate(m2.getLastUpdated());
                    return Long.compare(t2, t1); // Descending
                });

                // Helper to limit list
                List<OllamaModel> topPopular = popular.size() > 10 ? new java.util.ArrayList<>(popular.subList(0, 10))
                        : popular;
                List<OllamaModel> topNewest = newest.size() > 10 ? new java.util.ArrayList<>(newest.subList(0, 10))
                        : newest;

                // Available base models (names only, but maybe we can enrich them later)
                List<OllamaModel> available = ollamaManager.getAvailableBaseModels();

                // CLASSIFY ALL MODELS
                List<OllamaModel> allToClassify = new java.util.ArrayList<>();
                allToClassify.addAll(topPopular);
                allToClassify.addAll(topNewest);
                // allToClassify.addAll(available); // Available might not have size yet, skip
                // for now or fetch details

                for (OllamaModel m : allToClassify) {
                    classifyModel(m);
                }

                // Update UI
                Platform.runLater(() -> {
                    popularModels.setAll(topPopular);
                    newModels.setAll(topNewest);
                    availableModels.setAll(available);
                });

                // Save to cache
                com.org.ollamafx.model.LibraryCache newCache = new com.org.ollamafx.model.LibraryCache();
                newCache.setPopularModels(topPopular);
                newCache.setNewModels(topNewest);
                newCache.setAllModels(available);

                LibraryCacheManager.getInstance().saveCache(newCache);

                // Regenerate recommendations with fresh data
                ModelManager.this.generateRecommendations(topPopular, topNewest);

                return null;
            }
        };
        new Thread(task).start();
    }

    // Keep this for ABI compatibility if needed, but direct it to the new logic
    public void loadAllModels() {
        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Carga de modelos locales (Real-time check)
                System.out.println("ModelManager: Iniciando carga de modelos locales...");
                final List<OllamaModel> fetchedLocalModels = ollamaManager.getLocalModels();
                Platform.runLater(() -> localModels.setAll(fetchedLocalModels));

                // Load library models (Cached/Scraped)
                loadLibraryModels();

                return null;
            }
        };
        new Thread(loadTask).start();
    }

    // --- CLASSIFICATION LOGIC ---
    public void classifyModel(OllamaModel model) {
        HardwareManager.HardwareStats stats = HardwareManager.getStats();
        long osOverhead = 4L * 1024 * 1024 * 1024;
        long safeRamLimit = stats.totalRamBytes - osOverhead;
        long vramLimit = stats.isUnifiedMemory ? safeRamLimit : stats.totalVramBytes;

        long modelSizeBytes = parseModelSizeBytes(model.getSize());

        // Fallback: Estimate from parameters if size is unknown
        if (modelSizeBytes == 0) {
            double billionsParams = extractParameterCount(model);
            if (billionsParams > 0) {
                // Rule of thumb: ~0.6-0.7 GB per billion parameters (Q4 quantization)
                // We use 0.65 to be slightly optimistic but realistic
                modelSizeBytes = (long) (billionsParams * 0.65 * 1024 * 1024 * 1024);
            }
        }

        // Still 0? We can't know. Default to CAUTION (Standard).
        if (modelSizeBytes == 0) {
            model.setCompatibilityStatus(OllamaModel.CompatibilityStatus.CAUTION);
            return;
        }

        if (modelSizeBytes <= vramLimit) {
            // Fits in VRAM -> FAST (Green)
            model.setCompatibilityStatus(OllamaModel.CompatibilityStatus.RECOMMENDED);
        } else if (modelSizeBytes <= (safeRamLimit * 0.9)) {
            // Fits in RAM (with 90% usage cap) -> SLOW (Orange)
            model.setCompatibilityStatus(OllamaModel.CompatibilityStatus.CAUTION);
        } else {
            // Dangerous -> RED
            model.setCompatibilityStatus(OllamaModel.CompatibilityStatus.INCOMPATIBLE);
        }
    }

    /**
     * Extracts parameter count (in billions) from model badges, tags, or name.
     * e.g., "llama3:8b" -> 8.0
     * e.g., Badge "7B" -> 7.0
     * Returns 0.0 if not found.
     */
    private double extractParameterCount(OllamaModel model) {
        // 1. Check Badges first (usually most accurate from library)
        if (model.getBadges() != null) {
            for (String badge : model.getBadges()) {
                double val = parseBillionString(badge);
                if (val > 0)
                    return val;
            }
        }

        // 2. Check Tag (e.g. "8b", "70b-instruct")
        double valTag = parseBillionString(model.getTag());
        if (valTag > 0)
            return valTag;

        // 3. Check Name (e.g. "gemma:2b")
        // Sometimes name includes tag like "gemma:2b"
        if (model.getName().contains(":")) {
            String[] parts = model.getName().split(":");
            if (parts.length > 1) {
                double valNameTag = parseBillionString(parts[1]);
                if (valNameTag > 0)
                    return valNameTag;
            }
        }

        return 0.0;
    }

    private double parseBillionString(String text) {
        if (text == null)
            return 0;
        String t = text.toLowerCase().trim();
        // Regex to find number followed by 'b' e.g. "8b", "1.5b", "70b"
        // Avoid matching "byte" or words, strictly digits then 'b' at word boundary or
        // end
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d+(\\.\\d+)?)(b)");
        java.util.regex.Matcher m = p.matcher(t);
        if (m.find()) {
            try {
                return Double.parseDouble(m.group(1));
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    private void generateRecommendations(List<OllamaModel> popular, List<OllamaModel> newest) {
        HardwareManager.HardwareStats stats = HardwareManager.getStats();

        // Safety Overhead for OS (e.g., 4GB reserved)
        long osOverhead = 4L * 1024 * 1024 * 1024;

        long safeRamLimit = stats.totalRamBytes - osOverhead;
        // On Apple Silicon, safeRamLimit acts as VRAM too.
        // On PC, we have distinct VRAM.
        long vramLimit = stats.isUnifiedMemory ? safeRamLimit : stats.totalVramBytes;

        System.out.println("ModelManager: Generating recommendations based on Hardware:");
        System.out.println(" - Total RAM: " + String.format("%.2f GB", stats.getTotalRamGB()));
        System.out
                .println(" - Effective VRAM: " + String.format("%.2f GB", vramLimit / (1024.0 * 1024.0 * 1024.0)));
        System.out.println(" - Safe RAM Limit (OS reserved): "
                + String.format("%.2f GB", safeRamLimit / (1024.0 * 1024.0 * 1024.0)));

        List<OllamaModel> recs = new java.util.ArrayList<>();

        // Pool Source: Popular + Newest
        java.util.Set<String> seen = new java.util.HashSet<>();
        List<OllamaModel> pool = new java.util.ArrayList<>();
        if (popular != null)
            pool.addAll(popular);
        if (newest != null)
            pool.addAll(newest);

        for (OllamaModel m : pool) {
            if (seen.contains(m.getName()))
                continue;
            seen.add(m.getName());

            long modelSizeBytes = parseModelSizeBytes(m.getSize());

            // LOGIC TABLE OF EQUIVALENCES
            // 1. Must fit in System RAM (absolute hard limit for running at all)
            if (modelSizeBytes > safeRamLimit) {
                continue; // Skip, too big for this machine
            }

            // 2. Recommendation Tier
            // We recommend it if it fits in VRAM (Fast) OR if it is a smaller model that
            // runs reasonably on CPU.
            // For the "Recommended" list, let's be strict: Models that will provide a GOOD
            // experience.
            // - Fits in VRAM (Green tier) -> DEFINITELY RECOMMEND
            // - Fits in RAM but < 50% of Total RAM (to ensure decent CPU performance) ->
            // OKAY

            boolean highPerformance = modelSizeBytes <= vramLimit;
            boolean standardPerformance = modelSizeBytes <= (safeRamLimit * 0.8); // 80%
                                                                                  // of
                                                                                  // max
                                                                                  // safe
                                                                                  // ram

            if (highPerformance || standardPerformance) {
                recs.add(m);
            }

            if (recs.size() >= 12)
                break; // Limit to desktop grid size
        }

        Platform.runLater(() -> recommendedModels.setAll(recs));
    }

    private long parseModelSizeBytes(String sizeStr) {
        if (sizeStr == null || sizeStr.isEmpty() || sizeStr.equalsIgnoreCase("N/A"))
            return 0;
        try {
            String lower = sizeStr.toLowerCase().trim();
            double value = Double.parseDouble(lower.replaceAll("[^0-9.]", ""));
            long multiplier = 1;
            if (lower.contains("gb") || lower.contains("g")) {
                multiplier = 1024L * 1024 * 1024;
            } else if (lower.contains("mb") || lower.contains("m")) {
                multiplier = 1024L * 1024;
            } else if (lower.contains("kb") || lower.contains("k")) {
                multiplier = 1024L;
            }
            return (long) (value * multiplier);
        } catch (NumberFormatException e) {
            // Valid case for "Unknown" or unexpected formats
            return 0;
        }
    }

    public void deleteModel(String name, String tag) {
        Task<Void> deleteTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                ollamaManager.deleteModel(name, tag);
                return null;
            }
        };

        deleteTask.setOnSucceeded(e -> {
            System.out.println("Model deleted successfully: " + name + ":" + tag);
            refreshLocalModels();
        });

        deleteTask.setOnFailed(e -> {
            System.err.println("Failed to delete model: " + name + ":" + tag);
            if (deleteTask.getException() != null) {
                deleteTask.getException().printStackTrace();
            }
        });

        new Thread(deleteTask).start();
    }

    public void refreshLocalModels() {
        System.out.println("ModelManager: Refreshing local models...");
        Task<List<OllamaModel>> refreshTask = new Task<>() {
            @Override
            protected List<OllamaModel> call() throws Exception {
                return ollamaManager.getLocalModels();
            }
        };

        refreshTask.setOnSucceeded(e -> {
            List<OllamaModel> models = refreshTask.getValue();
            System.out.println("ModelManager: Local models refreshed. Count: " + models.size());
            for (OllamaModel m : models) {
                System.out.println(" - Found: " + m.getName() + ":" + m.getTag());
                classifyModel(m); // Classify installed models too
            }
            Platform.runLater(() -> localModels.setAll(models));
        });

        refreshTask.setOnFailed(e -> {
            System.err.println("ModelManager: Failed to refresh local models.");
            if (refreshTask.getException() != null) {
                refreshTask.getException().printStackTrace();
            }
        });

        new Thread(refreshTask).start();
    }

    public void addLocalModel(OllamaModel model) {
        Platform.runLater(() -> {
            System.out.println("ModelManager: Attempting to add model: " + model.getName() + ":" + model.getTag());
            // Check if it already exists to avoid duplicates
            boolean exists = localModels.stream()
                    .anyMatch(m -> m.getName().equalsIgnoreCase(model.getName())
                            && m.getTag().equalsIgnoreCase(model.getTag()));

            if (!exists) {
                localModels.add(model);
                System.out.println("ModelManager: Added new model directly. List size now: " + localModels.size());
            } else {
                System.out.println("ModelManager: Model already exists in list. Not adding.");
            }
        });
    }

    /**
     * Gets details (tags) for a model.
     * Uses Cache first. If missing/expired, scrapes, classifies, and caches.
     */
    public List<OllamaModel> getModelDetails(String modelName) throws Exception {
        // 1. Check Cache
        com.org.ollamafx.model.ModelDetailsEntry entry = ModelDetailsCacheManager.getInstance().getDetails(modelName);
        if (ModelDetailsCacheManager.getInstance().isEntryValid(entry)) {
            System.out.println("ModelManager: Cache HIT for " + modelName);
            List<OllamaModel> cached = entry.getTags();
            // RE-Apply Classification (Computing is cheap, ensures hardware changes are
            // respected)
            for (OllamaModel m : cached) {
                classifyModel(m);
            }
            return cached;
        }

        // 2. Scrape if miss
        System.out.println("ModelManager: Cache MISS for " + modelName + ". Scraping...");
        List<OllamaModel> freshTags = ollamaManager.scrapeModelDetails(modelName);

        // 3. Classify
        for (OllamaModel m : freshTags) {
            classifyModel(m);
        }

        // 4. Cache
        ModelDetailsCacheManager.getInstance().saveDetails(modelName, freshTags);

        return freshTags;
    }
}