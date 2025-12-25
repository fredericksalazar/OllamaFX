package com.org.ollamafx.manager;

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
        this.mapper = new ObjectMapper();
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
            System.err.println("LibraryCacheManager: Failed to load cache. It might be corrupted.");
            e.printStackTrace();
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
}
