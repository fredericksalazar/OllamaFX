// src/main/java/com/org/ollamafx/model/OllamaModel.java
package com.org.ollamafx.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

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

    private final ObjectProperty<CompatibilityStatus> compatibilityStatus = new SimpleObjectProperty<>(
            CompatibilityStatus.CAUTION);

    private final StringProperty contextLength;
    private final StringProperty inputType;
    private final List<String> badges;
    private final StringProperty readmeContent;

    @JsonCreator
    public OllamaModel(
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("pull_count") String pullCount,
            @JsonProperty("tag") String tag,
            @JsonProperty("size") String size,
            @JsonProperty("last_updated") String lastUpdated,
            @JsonProperty("context_length") String contextLength,
            @JsonProperty("input_type") String inputType,
            @JsonProperty("badges") List<String> badges,
            @JsonProperty("readme_content") String readmeContent,
            @JsonProperty("compatibility_status") CompatibilityStatus status) {
        this.name = new SimpleStringProperty(name);
        this.description = new SimpleStringProperty(description);
        this.pullCount = new SimpleStringProperty(pullCount);
        this.tag = new SimpleStringProperty(tag);
        this.size = new SimpleStringProperty(size);
        this.lastUpdated = new SimpleStringProperty(lastUpdated);
        this.contextLength = new SimpleStringProperty(contextLength);
        this.inputType = new SimpleStringProperty(inputType);
        this.badges = badges != null ? badges : new ArrayList<>();
        this.readmeContent = new SimpleStringProperty(readmeContent != null ? readmeContent : "");
        if (status != null) {
            this.compatibilityStatus.set(status);
        }
    }

    public OllamaModel(String name, String description, String pullCount, String tag, String size, String lastUpdated,
            String contextLength, String inputType) {
        this(name, description, pullCount, tag, size, lastUpdated, contextLength, inputType,
                new ArrayList<>(), "", null);
    }

    public OllamaModel(String name, String description, String pullCount, String tag, String size, String lastUpdated) {
        this(name, description, pullCount, tag, size, lastUpdated, "Unknown", "Text", new ArrayList<>(), "",
                null);
    }

    // Getters para las propiedades de JavaFX (esenciales para las TableView)
    @JsonIgnore
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

    @JsonProperty("context_length")
    public String getContextLength() {
        return contextLength.get();
    }

    @JsonProperty("input_type")
    public String getInputType() {
        return inputType.get();
    }

    public List<String> getBadges() {
        return badges;
    }

    @JsonProperty("readme_content")
    public String getReadmeContent() {
        return readmeContent.get();
    }

    public String getDescription() {
        return description.get();
    }

    @JsonProperty("pull_count")
    public String getPullCount() {
        return pullCount.get();
    }

    public String getSize() {
        return size.get();
    }

    @JsonProperty("last_updated")
    public String getLastUpdated() {
        return lastUpdated.get();
    }

    // ...

    @JsonIgnore
    public ObjectProperty<CompatibilityStatus> compatibilityStatusProperty() {
        return compatibilityStatus;
    }

    @JsonProperty("compatibility_status")
    public CompatibilityStatus getCompatibilityStatus() {
        return compatibilityStatus.get();
    }

    public void setCompatibilityStatus(CompatibilityStatus status) {
        this.compatibilityStatus.set(status);
    }
}