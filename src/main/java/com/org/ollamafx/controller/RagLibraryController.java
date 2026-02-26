package com.org.ollamafx.controller;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import org.kordamp.ikonli.javafx.FontIcon;

import com.org.ollamafx.App;
import com.org.ollamafx.manager.RagManager;
import com.org.ollamafx.model.RagCollection;
import com.org.ollamafx.model.RagDocumentItem;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Controller for the RAG Knowledge Base library panel.
 * Manages collections, document listing, drag-and-drop ingestion, and status display.
 */
public class RagLibraryController {

    private static final Logger LOGGER = Logger.getLogger(RagLibraryController.class.getName());

    @FXML private Button addFilesButton;
    @FXML private Button newCollectionButton;
    @FXML private HBox warningBanner;
    @FXML private Label warningLabel;
    @FXML private Label collectionNameLabel;
    @FXML private VBox dropZone;
    @FXML private ListView<RagDocumentItem> documentListView;
    @FXML private ListView<RagCollection> collectionListView;
    @FXML private Label statsLabel;

    private final RagManager ragManager = RagManager.getInstance();
    private ResourceBundle bundle;
    private RagCollection selectedCollection;
    private FilteredList<RagDocumentItem> filteredDocuments;

    @FXML
    public void initialize() {
        bundle = App.getBundle();

        // Initialize RAG engine
        ragManager.initialize();

        // Check embedding model
        if (!ragManager.isEmbeddingModelAvailable()) {
            warningBanner.setVisible(true);
            warningBanner.setManaged(true);
        }

        // Setup collections list
        collectionListView.setItems(ragManager.getCollections());
        collectionListView.setCellFactory(lv -> new CollectionCell());
        collectionListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> onCollectionSelected(newVal));

        // Setup filtered document list
        filteredDocuments = new FilteredList<>(ragManager.getDocuments(), p -> false);
        documentListView.setItems(filteredDocuments);
        documentListView.setCellFactory(lv -> new DocumentCell());

        // Listen for document changes to update empty state and stats
        ragManager.getDocuments().addListener((ListChangeListener<RagDocumentItem>) c -> {
            Platform.runLater(() -> {
                updateEmptyState();
                updateStats();
            });
        });

        // Setup drag and drop
        setupDragAndDrop();

        // Make drop zone look clickable
        dropZone.setStyle("-fx-cursor: hand;");

        // Select first collection if available
        if (!ragManager.getCollections().isEmpty()) {
            collectionListView.getSelectionModel().selectFirst();
        }

        updateStats();
    }

    private void onCollectionSelected(RagCollection collection) {
        selectedCollection = collection;
        if (collection != null) {
            collectionNameLabel.setText(collection.getName());
            filteredDocuments.setPredicate(doc ->
                    collection.getId().equals(doc.getCollectionId()));
        } else {
            collectionNameLabel.setText("");
            filteredDocuments.setPredicate(p -> false);
        }
        updateEmptyState();
    }

    @FXML
    private void onNewCollection() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(bundle.getString("rag.newCollection"));
        dialog.setHeaderText(null);
        dialog.setContentText(bundle.getString("rag.collectionName"));
        dialog.initOwner(newCollectionButton.getScene().getWindow());

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                RagCollection created = ragManager.createCollection(name.trim());
                collectionListView.getSelectionModel().select(created);
            }
        });
    }

    @FXML
    private void onAddFiles() {
        if (selectedCollection == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(bundle.getString("rag.addFiles"));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Documents", "*.txt", "*.md", "*.pdf"),
                new FileChooser.ExtensionFilter("Text Files", "*.txt", "*.md"),
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );

        List<File> files = fileChooser.showOpenMultipleDialog(
                addFilesButton.getScene().getWindow());

        if (files != null) {
            for (File file : files) {
                addDocumentFile(file);
            }
        }
    }

    @FXML
    private void onDropZoneClicked(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {
            onAddFiles();
        }
    }

    private void addDocumentFile(File file) {
        if (!RagManager.isSupportedFile(file)) return;
        if (selectedCollection == null) return;
        if (ragManager.isDocumentIndexed(file.getName())) return;

        RagDocumentItem item = new RagDocumentItem(
                file.getName(), file.getAbsolutePath(), selectedCollection.getId());
        ragManager.getDocuments().add(item);

        // Index asynchronously
        Task<Void> task = ragManager.indexDocument(file, item);
        task.setOnSucceeded(e -> Platform.runLater(this::updateStats));
        task.setOnFailed(e -> Platform.runLater(this::updateStats));
    }

    private void setupDragAndDrop() {
        dropZone.setOnDragOver(this::handleDragOver);
        dropZone.setOnDragDropped(this::handleDragDropped);
        dropZone.setOnDragExited(e -> dropZone.getStyleClass().remove("rag-drop-zone-active"));

        documentListView.setOnDragOver(this::handleDragOver);
        documentListView.setOnDragDropped(this::handleDragDropped);
    }

    private void handleDragOver(DragEvent event) {
        if (selectedCollection == null) return;
        if (event.getDragboard().hasFiles()) {
            boolean hasSupported = event.getDragboard().getFiles().stream()
                    .anyMatch(RagManager::isSupportedFile);
            if (hasSupported) {
                event.acceptTransferModes(TransferMode.COPY);
                if (!dropZone.getStyleClass().contains("rag-drop-zone-active")) {
                    dropZone.getStyleClass().add("rag-drop-zone-active");
                }
            }
        }
        event.consume();
    }

    private void handleDragDropped(DragEvent event) {
        dropZone.getStyleClass().remove("rag-drop-zone-active");
        boolean success = false;
        if (event.getDragboard().hasFiles() && selectedCollection != null) {
            for (File file : event.getDragboard().getFiles()) {
                if (RagManager.isSupportedFile(file)) {
                    addDocumentFile(file);
                    success = true;
                }
            }
        }
        event.setDropCompleted(success);
        event.consume();
    }

    private void updateEmptyState() {
        boolean empty = filteredDocuments.isEmpty();
        dropZone.setVisible(empty);
        dropZone.setManaged(empty);
        documentListView.setVisible(!empty);
        documentListView.setManaged(!empty);
    }

    private void updateStats() {
        long readyCount = ragManager.getReadyDocumentCount();
        int total = ragManager.getDocuments().size();
        if (total > 0) {
            statsLabel.setText(readyCount + "/" + total + " " +
                    bundle.getString("rag.status.ready").toLowerCase());
        } else {
            statsLabel.setText("");
        }
    }

    // ========== Collection Cell ==========

    private class CollectionCell extends ListCell<RagCollection> {

        private final HBox container;
        private final FontIcon folderIcon;
        private final Label nameLabel;
        private final Label countLabel;

        CollectionCell() {
            container = new HBox(8);
            container.setAlignment(Pos.CENTER_LEFT);
            container.setPadding(new Insets(8, 12, 8, 16));
            container.getStyleClass().add("rag-collection-cell");

            folderIcon = new FontIcon("fth-folder");
            folderIcon.setIconSize(15);
            folderIcon.getStyleClass().add("font-icon");

            nameLabel = new Label();
            nameLabel.getStyleClass().add("rag-collection-name");
            nameLabel.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(nameLabel, Priority.ALWAYS);

            countLabel = new Label();
            countLabel.getStyleClass().add("rag-collection-count");

            container.getChildren().addAll(folderIcon, nameLabel, countLabel);

            // Right-click context menu
            setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.SECONDARY && getItem() != null) {
                    showContextMenu(event.getScreenX(), event.getScreenY());
                }
            });
        }

        @Override
        protected void updateItem(RagCollection item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }

            nameLabel.setText(item.getName());

            // Count documents in this collection
            long count = ragManager.getDocuments().stream()
                    .filter(d -> item.getId().equals(d.getCollectionId()))
                    .count();
            countLabel.setText(String.valueOf(count));

            setGraphic(container);
        }

        private void showContextMenu(double screenX, double screenY) {
            RagCollection item = getItem();
            if (item == null) return;

            ContextMenu contextMenu = new ContextMenu();

            MenuItem renameItem = new MenuItem(bundle.getString("rag.renameCollection"));
            renameItem.setOnAction(e -> {
                TextInputDialog dialog = new TextInputDialog(item.getName());
                dialog.setTitle(bundle.getString("rag.renameCollection"));
                dialog.setHeaderText(null);
                dialog.setContentText(bundle.getString("rag.collectionName"));

                dialog.showAndWait().ifPresent(newName -> {
                    if (!newName.trim().isEmpty()) {
                        ragManager.renameCollection(item.getId(), newName.trim());
                        collectionListView.refresh();
                        if (selectedCollection != null && selectedCollection.getId().equals(item.getId())) {
                            collectionNameLabel.setText(newName.trim());
                        }
                    }
                });
            });

            MenuItem deleteItem = new MenuItem(bundle.getString("rag.deleteCollection"));
            deleteItem.setOnAction(e -> {
                ragManager.deleteCollection(item.getId());
                if (selectedCollection != null && selectedCollection.getId().equals(item.getId())) {
                    selectedCollection = null;
                    collectionNameLabel.setText("");
                }
                if (!ragManager.getCollections().isEmpty()) {
                    collectionListView.getSelectionModel().selectFirst();
                }
                updateStats();
            });

            contextMenu.getItems().addAll(renameItem, deleteItem);
            contextMenu.show(this, screenX, screenY);
        }
    }

    // ========== Document Cell ==========

    private class DocumentCell extends ListCell<RagDocumentItem> {

        private final HBox container;
        private final FontIcon fileIcon;
        private final Label nameLabel;
        private final Label statusLabel;
        private final ProgressBar progressBar;
        private final Button deleteButton;

        DocumentCell() {
            container = new HBox(10);
            container.setAlignment(Pos.CENTER_LEFT);
            container.getStyleClass().add("rag-doc-cell");

            fileIcon = new FontIcon();
            fileIcon.setIconSize(16);
            fileIcon.getStyleClass().add("font-icon");

            nameLabel = new Label();
            nameLabel.getStyleClass().add("rag-doc-name");
            nameLabel.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(nameLabel, Priority.ALWAYS);

            statusLabel = new Label();
            statusLabel.getStyleClass().add("rag-doc-status");

            progressBar = new ProgressBar();
            progressBar.setPrefWidth(60);
            progressBar.setPrefHeight(4);
            progressBar.setVisible(false);
            progressBar.setManaged(false);

            deleteButton = new Button();
            FontIcon trashIcon = new FontIcon("fth-trash-2");
            trashIcon.setIconSize(14);
            trashIcon.getStyleClass().add("font-icon");
            deleteButton.setGraphic(trashIcon);
            deleteButton.getStyleClass().add("button-icon-small");
            deleteButton.setTooltip(new Tooltip(App.getBundle().getString("button.delete")));
            deleteButton.setVisible(false);
            deleteButton.setOnAction(e -> {
                RagDocumentItem item = getItem();
                if (item != null) {
                    ragManager.deleteDocument(item.getFileName());
                    updateStats();
                    collectionListView.refresh();
                }
            });

            container.getChildren().addAll(fileIcon, nameLabel, statusLabel, progressBar, deleteButton);

            container.setOnMouseEntered(e -> deleteButton.setVisible(true));
            container.setOnMouseExited(e -> deleteButton.setVisible(false));
        }

        @Override
        protected void updateItem(RagDocumentItem item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setGraphic(null);
                return;
            }

            String name = item.getFileName().toLowerCase();
            fileIcon.setIconLiteral(getIconForFile(name));

            // Apply color style for PDF
            fileIcon.getStyleClass().removeAll("rag-icon-pdf", "rag-icon-text", "rag-icon-code", "rag-icon-data");
            if (name.endsWith(".pdf")) {
                fileIcon.getStyleClass().add("rag-icon-pdf");
            } else if (name.endsWith(".md") || name.endsWith(".txt")) {
                fileIcon.getStyleClass().add("rag-icon-text");
            } else if (name.endsWith(".json") || name.endsWith(".csv") || name.endsWith(".xml")) {
                fileIcon.getStyleClass().add("rag-icon-data");
            } else {
                fileIcon.getStyleClass().add("rag-icon-code");
            }

            nameLabel.setText(item.getFileName());

            updateStatus(item.getStatus());
            item.statusProperty().addListener((obs, oldVal, newVal) -> {
                Platform.runLater(() -> updateStatus(newVal));
            });

            boolean indexing = item.getStatus() == RagDocumentItem.Status.INDEXING;
            progressBar.setVisible(indexing);
            progressBar.setManaged(indexing);
            if (indexing) {
                progressBar.progressProperty().bind(item.progressProperty());
            }

            setGraphic(container);
        }

        private void updateStatus(RagDocumentItem.Status status) {
            statusLabel.getStyleClass().removeAll("rag-doc-status-ready", "rag-doc-status-error");

            switch (status) {
                case PENDING:
                    statusLabel.setText(bundle.getString("rag.status.pending"));
                    progressBar.setVisible(false);
                    progressBar.setManaged(false);
                    break;
                case INDEXING:
                    statusLabel.setText(bundle.getString("rag.status.indexing"));
                    progressBar.setVisible(true);
                    progressBar.setManaged(true);
                    break;
                case READY:
                    statusLabel.setText(bundle.getString("rag.status.ready"));
                    statusLabel.getStyleClass().add("rag-doc-status-ready");
                    progressBar.setVisible(false);
                    progressBar.setManaged(false);
                    break;
                case ERROR:
                    statusLabel.setText(bundle.getString("rag.status.error"));
                    statusLabel.getStyleClass().add("rag-doc-status-error");
                    progressBar.setVisible(false);
                    progressBar.setManaged(false);
                    break;
            }
        }
    }

    /**
     * Returns the appropriate Feather icon literal based on file extension.
     */
    private static String getIconForFile(String fileName) {
        if (fileName.endsWith(".pdf")) {
            return "fth-file";           // generic file icon for PDF
        } else if (fileName.endsWith(".md")) {
            return "fth-book-open";      // book icon for markdown
        } else if (fileName.endsWith(".txt")) {
            return "fth-file-text";      // text file icon
        } else if (fileName.endsWith(".json") || fileName.endsWith(".csv") || fileName.endsWith(".xml")) {
            return "fth-database";       // data icon
        } else if (fileName.endsWith(".java") || fileName.endsWith(".py") || fileName.endsWith(".js")) {
            return "fth-code";           // code icon
        } else {
            return "fth-file-text";      // default
        }
    }
}
