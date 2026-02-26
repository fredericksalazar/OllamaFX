package com.org.ollamafx.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ChatSession {
    private UUID id; // Not final for Jackson
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty modelName = new SimpleStringProperty();
    private final BooleanProperty pinned = new SimpleBooleanProperty();
    private LocalDateTime creationDate; // Not final for Jackson
    private List<ChatMessage> messages = new ArrayList<>();

    public ChatSession() {
        // Default constructor for Jackson
        this.id = UUID.randomUUID();
        this.creationDate = LocalDateTime.now();
    }

    public ChatSession(String name) {
        this.id = UUID.randomUUID();
        this.name.set(name);
        this.pinned.set(false);
        this.creationDate = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @JsonProperty("name")
    public String getName() {
        return name.get();
    }

    @JsonIgnore
    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    @JsonProperty("modelName")
    public String getModelName() {
        return modelName.get();
    }

    @JsonIgnore
    public StringProperty modelNameProperty() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName.set(modelName);
    }

    @JsonProperty("pinned")
    public boolean isPinned() {
        return pinned.get();
    }

    @JsonIgnore
    public BooleanProperty pinnedProperty() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned.set(pinned);
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    @JsonProperty("messages")
    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }

    public void addMessage(ChatMessage message) {
        this.messages.add(message);
    }

    private double temperature = 0.7;
    private String systemPrompt = "";

    @JsonProperty("temperature")
    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    @JsonProperty("systemPrompt")
    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    private int seed = -1; // -1 for random
    private int numCtx = 4096;
    private int topK = 40;
    private double topP = 0.9;
    private List<String> ragCollectionIds = new ArrayList<>();

    @JsonProperty("seed")
    public int getSeed() {
        return seed;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }

    @JsonProperty("numCtx")
    public int getNumCtx() {
        return numCtx;
    }

    public void setNumCtx(int numCtx) {
        this.numCtx = numCtx;
    }

    @JsonProperty("topK")
    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    @JsonProperty("topP")
    public double getTopP() {
        return topP;
    }

    public void setTopP(double topP) {
        this.topP = topP;
    }

    @JsonProperty("ragCollectionIds")
    public List<String> getRagCollectionIds() {
        return ragCollectionIds;
    }

    public void setRagCollectionIds(List<String> ragCollectionIds) {
        this.ragCollectionIds = ragCollectionIds != null ? ragCollectionIds : new ArrayList<>();
    }

    @Override
    public String toString() {
        return getName();
    }
}
