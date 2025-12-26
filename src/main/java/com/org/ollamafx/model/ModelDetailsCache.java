package com.org.ollamafx.model;

import java.util.HashMap;
import java.util.Map;

public class ModelDetailsCache {
    private Map<String, ModelDetailsEntry> cache = new HashMap<>();

    public Map<String, ModelDetailsEntry> getCache() {
        return cache;
    }

    public void setCache(Map<String, ModelDetailsEntry> cache) {
        this.cache = cache;
    }

    public void addEntry(String modelName, ModelDetailsEntry entry) {
        this.cache.put(modelName, entry);
    }

    public ModelDetailsEntry getEntry(String modelName) {
        return this.cache.get(modelName);
    }
}
