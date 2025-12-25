// src/main/java/com/org/ollamafx/model/OllamaModel.java
package com.org.ollamafx.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class OllamaModel {
    private final StringProperty name;
    private final StringProperty description;
    private final StringProperty pullCount;
    private final StringProperty tag;
    private final StringProperty size;
    private final StringProperty lastUpdated;

    private final StringProperty contextLength;
    private final StringProperty inputType;
    private final java.util.List<String> badges;
    private final StringProperty readmeContent;

    public OllamaModel(String name, String description, String pullCount, String tag, String size, String lastUpdated,
            String contextLength, String inputType, java.util.List<String> badges, String readmeContent) {
        this.name = new SimpleStringProperty(name);
        this.description = new SimpleStringProperty(description);
        this.pullCount = new SimpleStringProperty(pullCount);
        this.tag = new SimpleStringProperty(tag);
        this.size = new SimpleStringProperty(size);
        this.lastUpdated = new SimpleStringProperty(lastUpdated);
        this.contextLength = new SimpleStringProperty(contextLength);
        this.inputType = new SimpleStringProperty(inputType);
        this.badges = badges != null ? badges : new java.util.ArrayList<>();
        this.readmeContent = new SimpleStringProperty(readmeContent != null ? readmeContent : "");
    }

    public OllamaModel(String name, String description, String pullCount, String tag, String size, String lastUpdated,
            String contextLength, String inputType) {
        this(name, description, pullCount, tag, size, lastUpdated, contextLength, inputType,
                new java.util.ArrayList<>(), "");
    }

    public OllamaModel(String name, String description, String pullCount, String tag, String size, String lastUpdated) {
        this(name, description, pullCount, tag, size, lastUpdated, "Unknown", "Text", new java.util.ArrayList<>(), "");
    }

    // Getters para las propiedades de JavaFX (esenciales para las TableView)
    public StringProperty nameProperty() {
        return name;
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public StringProperty pullCountProperty() {
        return pullCount;
    }

    public StringProperty tagProperty() {
        return tag;
    }

    public StringProperty sizeProperty() {
        return size;
    }

    public StringProperty lastUpdatedProperty() {
        return lastUpdated;
    }

    public StringProperty contextLengthProperty() {
        return contextLength;
    }

    public StringProperty inputTypeProperty() {
        return inputType;
    }

    public StringProperty readmeContentProperty() {
        return readmeContent;
    }

    // Getters estándar opcionales
    public String getName() {
        return name.get();
    }

    public String getTag() {
        return tag.get();
    }

    public String getContextLength() {
        return contextLength.get();
    }

    public String getInputType() {
        return inputType.get();
    }

    public java.util.List<String> getBadges() {
        return badges;
    }

    public String getReadmeContent() {
        return readmeContent.get();
    }

    public String getDescription() {
        return description.get();
    }

    public String getPullCount() {
        return pullCount.get();
    }

    public String getSize() {
        return size.get();
    }

    public String getLastUpdated() {
        return lastUpdated.get();
    }
}