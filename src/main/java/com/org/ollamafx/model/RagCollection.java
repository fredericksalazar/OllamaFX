package com.org.ollamafx.model;

import java.util.UUID;

/**
 * Represents a collection/folder in the RAG Knowledge Base.
 * Documents are organized into collections for topic-based retrieval.
 */
public class RagCollection {

    private String id;
    private String name;

    public RagCollection() {
        this.id = UUID.randomUUID().toString();
        this.name = "";
    }

    public RagCollection(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
    }

    public RagCollection(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RagCollection that = (RagCollection) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
