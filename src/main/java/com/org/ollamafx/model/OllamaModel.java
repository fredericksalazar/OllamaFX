// src/main/java/com/org/ollamafx/model/OllamaModel.java
package com.org.ollamafx.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Ignore any unknown properties and specifically the internal StringProperty fields
@JsonIgnoreProperties(ignoreUnknown = true)
public class OllamaModel {
    private final StringProperty name;
    private final StringProperty description;
    private final StringProperty pullCount;
    private final StringProperty tag;
    private final StringProperty size;
    private final StringProperty lastUpdated;

    // Status Enum
    public enum CompatibilityStatus {
        RECOMMENDED, // Green
        CAUTION, // Orange
        INCOMPATIBLE // Red
    }

    private final javafx.beans.property.ObjectProperty<CompatibilityStatus> compatibilityStatus = new javafx.beans.property.SimpleObjectProperty<>(
            CompatibilityStatus.CAUTION);

    private final StringProperty contextLength;
    private final StringProperty inputType;
    private final java.util.List<String> badges;
    private final StringProperty readmeContent;

    @com.fasterxml.jackson.annotation.JsonCreator
    public OllamaModel(
            @com.fasterxml.jackson.annotation.JsonProperty("name") String name,
            @com.fasterxml.jackson.annotation.JsonProperty("description") String description,
            @com.fasterxml.jackson.annotation.JsonProperty("pull_count") String pullCount,
            @com.fasterxml.jackson.annotation.JsonProperty("tag") String tag,
            @com.fasterxml.jackson.annotation.JsonProperty("size") String size,
            @com.fasterxml.jackson.annotation.JsonProperty("last_updated") String lastUpdated,
            @com.fasterxml.jackson.annotation.JsonProperty("context_length") String contextLength,
            @com.fasterxml.jackson.annotation.JsonProperty("input_type") String inputType,
            @com.fasterxml.jackson.annotation.JsonProperty("badges") java.util.List<String> badges,
            @com.fasterxml.jackson.annotation.JsonProperty("readme_content") String readmeContent) {
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
    @com.fasterxml.jackson.annotation.JsonIgnore
    public StringProperty nameProperty() {
        return name;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public StringProperty descriptionProperty() {
        return description;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public StringProperty pullCountProperty() {
        return pullCount;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public StringProperty tagProperty() {
        return tag;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public StringProperty sizeProperty() {
        return size;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public StringProperty lastUpdatedProperty() {
        return lastUpdated;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public StringProperty contextLengthProperty() {
        return contextLength;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public StringProperty inputTypeProperty() {
        return inputType;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
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

    @com.fasterxml.jackson.annotation.JsonProperty("context_length")
    public String getContextLength() {
        return contextLength.get();
    }

    @com.fasterxml.jackson.annotation.JsonProperty("input_type")
    public String getInputType() {
        return inputType.get();
    }

    public java.util.List<String> getBadges() {
        return badges;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("readme_content")
    public String getReadmeContent() {
        return readmeContent.get();
    }

    public String getDescription() {
        return description.get();
    }

    @com.fasterxml.jackson.annotation.JsonProperty("pull_count")
    public String getPullCount() {
        return pullCount.get();
    }

    public String getSize() {
        return size.get();
    }

    @com.fasterxml.jackson.annotation.JsonProperty("last_updated")
    public String getLastUpdated() {
        return lastUpdated.get();
    }

    // ...

    @com.fasterxml.jackson.annotation.JsonIgnore
    public javafx.beans.property.ObjectProperty<CompatibilityStatus> compatibilityStatusProperty() {
        return compatibilityStatus;
    }

    public CompatibilityStatus getCompatibilityStatus() {
        return compatibilityStatus.get();
    }

    public void setCompatibilityStatus(CompatibilityStatus status) {
        this.compatibilityStatus.set(status);
    }
}