package com.org.ollamafx.controller;

import com.org.ollamafx.manager.ModelManager;
import com.org.ollamafx.model.OllamaModel;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.util.List;

public class AvailableModelsController {

    @FXML
    private StackPane detailViewContainer;
    @FXML
    private ListView<OllamaModel> modelListView;

    private ModelManager modelManager;

    // Debounce for selection to prevent UI flooding
    private javafx.animation.PauseTransition selectionDebounce;

    public void setModelManager(ModelManager modelManager) {
        this.modelManager = modelManager;
        modelListView.setItems(this.modelManager.getAvailableModels());

        modelListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(OllamaModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getName());
                    setGraphic(null);
                }
            }
        });

        // Initialize debounce (100ms to prevent rapid clicks while maintaining
        // responsiveness)
        selectionDebounce = new javafx.animation.PauseTransition(javafx.util.Duration.millis(100));
        selectionDebounce.setOnFinished(event -> {
            System.out.println("DEBUG: Debounce finished. Getting selected item...");
            OllamaModel selected = modelListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                System.out
                        .println("DEBUG: Selected item found: " + selected.getName() + ". Triggering details display.");
                displayModelDetails(selected);
            } else {
                System.out.println("DEBUG: Debounce finished but no selection found.");
            }
        });

        modelListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            System.out.println(
                    "DEBUG: Selection changed. New: " + (newSelection != null ? newSelection.getName() : "null"));
            if (newSelection != null) {
                // Showing a temporary loading state or just waiting?
                // Using debounce to wait for user to stop scrolling/clicking
                System.out.println("DEBUG: Starting debounce timer...");
                selectionDebounce.playFromStart();
            } else {
                System.out.println("DEBUG: Selection cleared.");
                detailViewContainer.getChildren().clear();
            }
        });
    }

    private void displayModelDetails(OllamaModel selectedModel) {
        System.out.println("DEBUG: displayModelDetails called for " + selectedModel.getName());

        // Get all tags for this model from the in-memory details cache
        // This is instant since cache is already loaded in memory
        try {
            if (modelManager == null) {
                showErrorInView("Error", "ModelManager is null");
                return;
            }

            // Get all tags from cache - this uses in-memory ModelDetailsCacheManager
            List<OllamaModel> details = modelManager.getModelDetails(selectedModel.getName());
            System.out.println("DEBUG: Got " + details.size() + " tags from cache for " + selectedModel.getName());

            // Show immediately
            showDetailsInView(details);

        } catch (Exception e) {
            System.err.println("DEBUG: Error loading details: " + e.getMessage());
            e.printStackTrace();
            showErrorInView("Error Loading Details", "Failed to load model details.");
        }
    }

    // El método ahora recibe una lista de modelos (tags)
    private void showDetailsInView(List<OllamaModel> modelTags) {
        System.out.println("DEBUG: showDetailsInView method entered.");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/model_detail_view.fxml"));
            Parent detailView = loader.load();
            System.out.println("DEBUG: FXML loaded.");

            ModelDetailController controller = loader.getController();
            controller.setModelManager(this.modelManager); // Inyectamos el manager
            System.out.println("DEBUG: Controller retrieved and manager set. Populating details...");
            controller.populateDetails(modelTags); // Le pasamos la lista
            System.out.println("DEBUG: populateDetails returned.");

            detailViewContainer.getChildren().setAll(detailView);
            System.out.println("DEBUG: detailViewContainer updated.");
        } catch (IOException e) {
            System.err.println("DEBUG: Error in showDetailsInView:");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("DEBUG: Unexpected error in showDetailsInView:");
            e.printStackTrace();
        }
    }

    /**
     * Muestra un panel con un mensaje de error.
     */
    private void showErrorInView(String title, String message) {
        VBox errorBox = new VBox(10);
        errorBox.setAlignment(Pos.CENTER);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: normal;");
        titleLabel.setTextFill(Color.RED);

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);

        errorBox.getChildren().addAll(titleLabel, messageLabel);
        detailViewContainer.getChildren().setAll(errorBox);
    }

    @FXML
    private void sortByNameAsc() {
        modelListView.getItems().sort((m1, m2) -> m1.getName().compareToIgnoreCase(m2.getName()));
    }

    @FXML
    private void sortByNameDesc() {
        modelListView.getItems().sort((m1, m2) -> m2.getName().compareToIgnoreCase(m1.getName()));
    }
}