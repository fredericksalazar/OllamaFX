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

    public OllamaModel(String name, String description, String pullCount, String tag, String size, String lastUpdated) {
        this.name = new SimpleStringProperty(name);
        this.description = new SimpleStringProperty(description);
        this.pullCount = new SimpleStringProperty(pullCount);
        this.tag = new SimpleStringProperty(tag);
        this.size = new SimpleStringProperty(size);
        this.lastUpdated = new SimpleStringProperty(lastUpdated);
    }

    // Getters para las propiedades de JavaFX (esenciales para las TableView)
    public StringProperty nameProperty() { return name; }
    public StringProperty descriptionProperty() { return description; }
    public StringProperty pullCountProperty() { return pullCount; }
    public StringProperty tagProperty() { return tag; }
    public StringProperty sizeProperty() { return size; }
    public StringProperty lastUpdatedProperty() { return lastUpdated; }

    // Getters estándar opcionales
    public String getName() { return name.get(); }
    public String getTag() { return tag.get(); }
}