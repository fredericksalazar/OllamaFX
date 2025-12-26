package com.org.ollamafx.manager;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.ollamafx.model.LibraryCache;
import java.io.File;
import java.io.IOException;

public class LibraryCacheManager {

    private static final String CACHE_FILE_NAME = "library_cache.json";
    private static final long CACHE_EXPIRY_MS = 24 * 60 * 60 * 1000; // 24 hours

    private final File cacheFile;
    private final ObjectMapper mapper;

    // Singleton instance
    private static LibraryCacheManager instance;

    private LibraryCacheManager() {
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
    }

    public static synchronized LibraryCacheManager getInstance() {
        if (instance == null) {
            instance = new LibraryCacheManager();
        }
        return instance;
    }

    public synchronized void saveCache(LibraryCache cache) {
        try {
            cache.setLastUpdated(System.currentTimeMillis());
            mapper.writeValue(cacheFile, cache);
            System.out.println("LibraryCacheManager: Cache saved to " + cacheFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("LibraryCacheManager: Failed to save cache.");
            e.printStackTrace();
        }
    }

    public synchronized LibraryCache loadCache() {
        if (!cacheFile.exists()) {
            System.out.println("LibraryCacheManager: No cache file found.");
            return null;
        }

        try {
            return mapper.readValue(cacheFile, LibraryCache.class);
        } catch (IOException e) {
            System.err.println("LibraryCacheManager: Cache corrupto, eliminando archivo: " + e.getMessage());
            // DELETE corrupt file so fresh cache can be created
            cacheFile.delete();
            return null;
        }
    }

    public boolean isCacheValid(LibraryCache cache) {
        if (cache == null)
            return false;
        long age = System.currentTimeMillis() - cache.getLastUpdated();
        boolean valid = age < CACHE_EXPIRY_MS;
        System.out.println("LibraryCacheManager: Cache age is " + (age / 1000 / 60) + " minutes. Valid: " + valid);
        return valid;
    }

    // === METADATA METHODS FOR SETTINGS ===

    public String getCacheFilePath() {
        return cacheFile.getAbsolutePath();
    }

    public String getCacheFileName() {
        return CACHE_FILE_NAME;
    }

    public long getCacheFileSizeKB() {
        if (cacheFile.exists()) {
            return cacheFile.length() / 1024;
        }
        return 0;
    }

    public long getLastUpdatedTimestamp() {
        LibraryCache cache = loadCache();
        if (cache != null) {
            return cache.getLastUpdated();
        }
        return 0;
    }

    public int getDaysSinceUpdate() {
        long lastUpdated = getLastUpdatedTimestamp();
        if (lastUpdated == 0)
            return -1; // No cache
        long ageMs = System.currentTimeMillis() - lastUpdated;
        return (int) (ageMs / (1000 * 60 * 60 * 24));
    }

    public boolean cacheExists() {
        return cacheFile.exists();
    }
}
