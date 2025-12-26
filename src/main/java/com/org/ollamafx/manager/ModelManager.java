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
     * Carga modelos ÚNICAMENTE desde el caché local.
     * La descarga de datos frescos se maneja en el Splash Screen.
     */
    public void loadLibraryModels() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Load from ModelLibraryManager cache (updated by SplashScreen)
                com.org.ollamafx.model.LibraryCache cache = ModelLibraryManager.getInstance().getLibrary();

                if (cache != null && cache.getAllModels() != null && !cache.getAllModels().isEmpty()) {
                    System.out.println("ModelManager: Loading from cache. Models: " + cache.getAllModels().size());

                    // Sort POPULAR by pull count (most downloads first)
                    List<OllamaModel> sortedPopular = new java.util.ArrayList<>(cache.getAllModels());
                    sortedPopular.sort((a, b) -> {
                        long countA = parsePullCountValue(a.getPullCount());
                        long countB = parsePullCountValue(b.getPullCount());
                        return Long.compare(countB, countA);
                    });
                    List<OllamaModel> top20Popular = sortedPopular.size() > 20
                            ? sortedPopular.subList(0, 20)
                            : sortedPopular;

                    // Sort NEW by lastUpdated date (newest first)
                    List<OllamaModel> sortedNew = new java.util.ArrayList<>(cache.getAllModels());
                    sortedNew.sort((a, b) -> {
                        long dateA = parseRelativeDateValue(a.getLastUpdated());
                        long dateB = parseRelativeDateValue(b.getLastUpdated());
                        return Long.compare(dateB, dateA);
                    });
                    List<OllamaModel> top20New = sortedNew.size() > 20
                            ? sortedNew.subList(0, 20)
                            : sortedNew;

                    Platform.runLater(() -> {
                        popularModels.setAll(top20Popular);
                        newModels.setAll(top20New);
                        availableModels.setAll(cache.getAllModels());
                    });

                    // Generate recommendations
                    ModelManager.this.generateRecommendations(top20Popular, top20New);
                } else {
                    System.out.println("ModelManager: No valid cache found. Data should be loaded via Splash Screen.");
                }

                return null;
            }
        };
        new Thread(task).start();
    }

    private long parsePullCountValue(String pullCount) {
        if (pullCount == null || pullCount.isEmpty())
            return 0;
        try {
            String clean = pullCount.toUpperCase().trim();
            double value = Double.parseDouble(clean.replaceAll("[^0-9.]", ""));
            if (clean.contains("M"))
                return (long) (value * 1_000_000);
            if (clean.contains("K"))
                return (long) (value * 1_000);
            return (long) value;
        } catch (Exception e) {
            return 0;
        }
    }

    private long parseRelativeDateValue(String relativeDate) {
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

    // refreshLibraryCache() removed - all data refresh now handled by
    // SplashController via ModelLibraryManager

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
        // Get hardware stats ONCE for performance
        HardwareManager.HardwareStats stats = HardwareManager.getStats();
        long osOverhead = 4L * 1024 * 1024 * 1024;
        long safeRamLimit = stats.totalRamBytes - osOverhead;
        long vramLimit = stats.isUnifiedMemory ? safeRamLimit : stats.totalVramBytes;

        System.out.println("ModelManager: Generating recommendations...");

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

            // INLINE classification to avoid repeated getStats() calls
            long modelSizeBytes = parseModelSizeBytes(m.getSize());

            // Fallback: Estimate from parameters if size is unknown
            if (modelSizeBytes == 0) {
                double billionsParams = extractParameterCount(m);
                if (billionsParams > 0) {
                    modelSizeBytes = (long) (billionsParams * 0.65 * 1024 * 1024 * 1024);
                }
            }

            // Determine status inline
            if (modelSizeBytes > 0 && modelSizeBytes <= vramLimit) {
                m.setCompatibilityStatus(OllamaModel.CompatibilityStatus.RECOMMENDED);
                recs.add(m);
            } else if (modelSizeBytes > 0 && modelSizeBytes <= (safeRamLimit * 0.9)) {
                m.setCompatibilityStatus(OllamaModel.CompatibilityStatus.CAUTION);
            } else if (modelSizeBytes > 0) {
                m.setCompatibilityStatus(OllamaModel.CompatibilityStatus.INCOMPATIBLE);
            } else {
                m.setCompatibilityStatus(OllamaModel.CompatibilityStatus.CAUTION);
            }

            if (recs.size() >= 12)
                break;
        }

        System.out.println("ModelManager: Generated " + recs.size() + " recommendations");
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
     * Gets details (tags) for a model from LOCAL CACHE ONLY.
     * Classification is already persisted in cache from initial download.
     * No scraping, no re-classification - just return cached data.
     */
    public List<OllamaModel> getModelDetails(String modelName) throws Exception {
        // Check Cache ONLY - no expiry check, just presence
        com.org.ollamafx.model.ModelDetailsEntry entry = ModelDetailsCacheManager.getInstance().getDetails(modelName);

        if (entry != null && entry.getTags() != null && !entry.getTags().isEmpty()) {
            System.out.println("ModelManager: Cache HIT for " + modelName + " (" + entry.getTags().size() + " tags)");
            // Return directly - classification is already persisted
            return entry.getTags();
        }

        // If not in cache, return basic info (no scraping)
        System.out.println("ModelManager: Cache MISS for " + modelName + ". Returning basic info.");
        List<OllamaModel> basicInfo = new java.util.ArrayList<>();
        OllamaModel basic = new OllamaModel(modelName, "Detalles no disponibles en caché local", "", "latest", "", "");
        basicInfo.add(basic);
        return basicInfo;
    }

    /**
     * Classify multiple models with a single HardwareManager.getStats() call.
     * Much faster than calling classifyModel() individually.
     */
    private void classifyModelsBatch(List<OllamaModel> models) {
        // Get hardware stats ONCE
        HardwareManager.HardwareStats stats = HardwareManager.getStats();
        long osOverhead = 4L * 1024 * 1024 * 1024;
        long safeRamLimit = stats.totalRamBytes - osOverhead;
        long vramLimit = stats.isUnifiedMemory ? safeRamLimit : stats.totalVramBytes;

        for (OllamaModel model : models) {
            long modelSizeBytes = parseModelSizeBytes(model.getSize());

            // Fallback: Estimate from parameters if size is unknown
            if (modelSizeBytes == 0) {
                double billionsParams = extractParameterCount(model);
                if (billionsParams > 0) {
                    modelSizeBytes = (long) (billionsParams * 0.65 * 1024 * 1024 * 1024);
                }
            }

            // Classify based on size
            if (modelSizeBytes == 0) {
                model.setCompatibilityStatus(OllamaModel.CompatibilityStatus.CAUTION);
            } else if (modelSizeBytes <= vramLimit) {
                model.setCompatibilityStatus(OllamaModel.CompatibilityStatus.RECOMMENDED);
            } else if (modelSizeBytes <= (safeRamLimit * 0.9)) {
                model.setCompatibilityStatus(OllamaModel.CompatibilityStatus.CAUTION);
            } else {
                model.setCompatibilityStatus(OllamaModel.CompatibilityStatus.INCOMPATIBLE);
            }
        }
    }
}