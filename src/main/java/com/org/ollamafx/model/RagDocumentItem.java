package com.org.ollamafx.model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * JavaFX model representing a document in the RAG Knowledge Base.
 * Observable properties allow direct binding to UI list cells.
 */
public class RagDocumentItem {

    public enum Status {
        PENDING, INDEXING, READY, ERROR
    }

    private final StringProperty fileName;
    private final StringProperty filePath;
    private final StringProperty collectionId;
    private final ObjectProperty<Status> status;
    private final DoubleProperty progress;
    private final StringProperty errorMessage;

    public RagDocumentItem(String fileName, String filePath) {
        this(fileName, filePath, "");
    }

    public RagDocumentItem(String fileName, String filePath, String collectionId) {
        this.fileName = new SimpleStringProperty(fileName);
        this.filePath = new SimpleStringProperty(filePath);
        this.collectionId = new SimpleStringProperty(collectionId);
        this.status = new SimpleObjectProperty<>(Status.PENDING);
        this.progress = new SimpleDoubleProperty(0.0);
        this.errorMessage = new SimpleStringProperty("");
    }

    // --- fileName ---
    public String getFileName() { return fileName.get(); }
    public StringProperty fileNameProperty() { return fileName; }

    // --- filePath ---
    public String getFilePath() { return filePath.get(); }
    public StringProperty filePathProperty() { return filePath; }

    // --- collectionId ---
    public String getCollectionId() { return collectionId.get(); }
    public void setCollectionId(String collectionId) { this.collectionId.set(collectionId); }
    public StringProperty collectionIdProperty() { return collectionId; }

    // --- status ---
    public Status getStatus() { return status.get(); }
    public void setStatus(Status status) { this.status.set(status); }
    public ObjectProperty<Status> statusProperty() { return status; }

    // --- progress ---
    public double getProgress() { return progress.get(); }
    public void setProgress(double progress) { this.progress.set(progress); }
    public DoubleProperty progressProperty() { return progress; }

    // --- errorMessage ---
    public String getErrorMessage() { return errorMessage.get(); }
    public void setErrorMessage(String msg) { this.errorMessage.set(msg); }
    public StringProperty errorMessageProperty() { return errorMessage; }
}
