package com.org.ollamafx.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.time.LocalDateTime;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

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

    @Override
    public String toString() {
        return getName();
    }
}
