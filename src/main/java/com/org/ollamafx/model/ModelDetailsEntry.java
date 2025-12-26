package com.org.ollamafx.model;

import java.util.List;

public class ModelDetailsEntry {
    private long lastUpdated;
    private List<OllamaModel> tags;

    public ModelDetailsEntry() {
    }

    public ModelDetailsEntry(List<OllamaModel> tags, long lastUpdated) {
        this.tags = tags;
        this.lastUpdated = lastUpdated;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public List<OllamaModel> getTags() {
        return tags;
    }

    public void setTags(List<OllamaModel> tags) {
        this.tags = tags;
    }
}
