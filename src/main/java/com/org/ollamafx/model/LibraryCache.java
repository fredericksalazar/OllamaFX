package com.org.ollamafx.model;

import java.util.List;
import java.util.ArrayList;

public class LibraryCache {
    private long lastUpdated;
    private List<OllamaModel> allModels; // For "Available Models" view
    private List<OllamaModel> popularModels; // For Home screen
    private List<OllamaModel> newModels; // For Home screen

    // Default constructor for Jackson
    public LibraryCache() {
        this.allModels = new ArrayList<>();
        this.popularModels = new ArrayList<>();
        this.newModels = new ArrayList<>();
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public List<OllamaModel> getAllModels() {
        return allModels;
    }

    public void setAllModels(List<OllamaModel> allModels) {
        this.allModels = allModels;
    }

    public List<OllamaModel> getPopularModels() {
        return popularModels;
    }

    public void setPopularModels(List<OllamaModel> popularModels) {
        this.popularModels = popularModels;
    }

    public List<OllamaModel> getNewModels() {
        return newModels;
    }

    public void setNewModels(List<OllamaModel> newModels) {
        this.newModels = newModels;
    }
}
