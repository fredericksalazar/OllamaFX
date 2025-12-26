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
    private ListView<OllamaModel> modelListView;
    @FXML
    private javafx.scene.control.TextField searchField;
    @FXML
    private StackPane detailViewContainer;

    private ModelManager modelManager;

    // Debounce for selection to prevent UI flooding
    private javafx.animation.PauseTransition selectionDebounce;

    public void setModelManager(ModelManager modelManager) {
        this.modelManager = modelManager;

        // Use FilteredList for search
        javafx.collections.transformation.FilteredList<OllamaModel> filteredModels = new javafx.collections.transformation.FilteredList<>(
                this.modelManager.getAvailableModels(), p -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredModels.setPredicate(model -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return model.getName().toLowerCase().contains(lowerCaseFilter);
            });
        });

        modelListView.setItems(filteredModels);

        // Set placeholder for empty list (Empty State)
        Label placeholder = new Label();
        placeholder.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
                () -> {
                    String search = searchField.getText();
                    if (search != null && !search.isEmpty()) {
                        return "No models found matching \"" + search + "\"";
                    }
                    return "No models available";
                },
                searchField.textProperty()));
        placeholder.setStyle("-fx-text-fill: -color-fg-muted;");
        modelListView.setPlaceholder(placeholder);

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
            OllamaModel selected = modelListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                displayModelDetails(selected);
            }
        });

        modelListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectionDebounce.playFromStart();
            } else {
                detailViewContainer.getChildren().clear();
            }
        });
    }

    private void displayModelDetails(OllamaModel selectedModel) {
        try {
            if (modelManager == null) {
                showErrorInView("Error", "ModelManager is null");
                return;
            }

            List<OllamaModel> details = modelManager.getModelDetails(selectedModel.getName());
            showDetailsInView(details);

        } catch (Exception e) {
            showErrorInView("Error Loading Details", "Failed to load model details.");
        }
    }

    private void showDetailsInView(List<OllamaModel> modelTags) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/model_detail_view.fxml"));
            Parent detailView = loader.load();

            ModelDetailController controller = loader.getController();
            controller.setModelManager(this.modelManager);
            controller.populateDetails(modelTags);

            detailViewContainer.getChildren().setAll(detailView);
        } catch (IOException e) {
            // Log error or show in UI if critical
        } catch (Exception e) {
            // Log error or show in UI if critical
        }
    }

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