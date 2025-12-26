package com.org.ollamafx.manager;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.ollamafx.model.ModelDetailsCache;
import java.io.File;
import java.io.IOException;

public class ModelDetailsCacheManager {

    private static final String CACHE_FILE_NAME = "details_cache.json";
    // Cache expiry for details can be longer, e.g., 3 days
    private static final long CACHE_EXPIRY_MS = 3L * 24 * 60 * 60 * 1000;

    private final File cacheFile;
    private final ObjectMapper mapper;

    private ModelDetailsCache memoryCache;

    // Singleton instance
    private static ModelDetailsCacheManager instance;

    private ModelDetailsCacheManager() {
        String userHome = System.getProperty("user.home");
        File appDir = new File(userHome, ".ollamafx");
        if (!appDir.exists()) {
            appDir.mkdirs();
        }
        this.cacheFile = new File(appDir, CACHE_FILE_NAME);

        // Configure ObjectMapper to only use getters, not fields
        // This prevents serialization of JavaFX StringProperty fields
        this.mapper = new ObjectMapper();
        this.mapper.setVisibility(
                this.mapper.getSerializationConfig()
                        .getDefaultVisibilityChecker()
                        .withFieldVisibility(JsonAutoDetect.Visibility.NONE)
                        .withGetterVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                        .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                        .withCreatorVisibility(JsonAutoDetect.Visibility.ANY));

        this.memoryCache = loadCacheFromDisk();
        if (this.memoryCache == null) {
            this.memoryCache = new ModelDetailsCache();
        }
    }

    public static synchronized ModelDetailsCacheManager getInstance() {
        if (instance == null) {
            instance = new ModelDetailsCacheManager();
        }
        return instance;
    }

    private synchronized ModelDetailsCache loadCacheFromDisk() {
        if (!cacheFile.exists()) {
            return new ModelDetailsCache();
        }
        try {
            return mapper.readValue(cacheFile, ModelDetailsCache.class);
        } catch (IOException e) {
            System.err.println("ModelDetailsCacheManager: Cache corrupto, eliminando archivo: " + e.getMessage());
            // DELETE corrupt file so fresh cache can be created
            cacheFile.delete();
            return new ModelDetailsCache();
        }
    }

    public synchronized void saveCache() {
        try {
            mapper.writeValue(cacheFile, memoryCache);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public com.org.ollamafx.model.ModelDetailsEntry getDetails(String modelName) {
        return memoryCache.getEntry(modelName);
    }

    public void saveDetails(String modelName, java.util.List<com.org.ollamafx.model.OllamaModel> tags) {
        com.org.ollamafx.model.ModelDetailsEntry entry = new com.org.ollamafx.model.ModelDetailsEntry(tags,
                System.currentTimeMillis());
        memoryCache.addEntry(modelName, entry);
        saveCache(); // Persist immediately (or could debounce)
    }

    public boolean isEntryValid(com.org.ollamafx.model.ModelDetailsEntry entry) {
        if (entry == null)
            return false;
        long age = System.currentTimeMillis() - entry.getLastUpdated();
        return age < CACHE_EXPIRY_MS;
    }
}
