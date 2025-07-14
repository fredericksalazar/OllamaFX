package com.org.ollamafx.controller;

import com.org.ollamafx.manager.ModelManager;
import com.org.ollamafx.manager.OllamaManager;
import com.org.ollamafx.model.OllamaModel;

import io.github.ollama4j.models.response.LibraryModelDetail;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
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
    private final OllamaManager ollamaManager = OllamaManager.getInstance();

    public void setModelManager(ModelManager modelManager) {
        this.modelManager = modelManager;
        modelListView.setItems(this.modelManager.getAvailableModels());

        modelListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(OllamaModel item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        modelListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                displayModelDetails(newSelection);
            }
        });
    }

    private void displayModelDetails(OllamaModel selectedModel) {
        ProgressIndicator progressIndicator = new ProgressIndicator();
        detailViewContainer.getChildren().setAll(progressIndicator);

        // La tarea ahora espera una Lista de OllamaModel
        Task<List<OllamaModel>> loadDetailsTask = new Task<>() {
            @Override
            protected List<OllamaModel> call() throws Exception {
                // LLAMAMOS AL NUEVO MÉTODO DE SCRAPING
                return ollamaManager.scrapeModelDetails(selectedModel.getName());
            }
        };

        loadDetailsTask.setOnSucceeded(event -> {
            List<OllamaModel> details = loadDetailsTask.getValue();
            Platform.runLater(() -> showDetailsInView(details)); // Pasamos la lista completa
        });

        // ... (onFailed se queda igual)
        new Thread(loadDetailsTask).start();
    }

    // El método ahora recibe una lista de modelos (tags)
    private void showDetailsInView(List<OllamaModel> modelTags) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/model_detail_view.fxml"));
            Parent detailView = loader.load();

            ModelDetailController controller = loader.getController();
            controller.populateDetails(modelTags); // Le pasamos la lista

            detailViewContainer.getChildren().setAll(detailView);
        } catch (IOException e) {
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
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        titleLabel.setTextFill(Color.RED);

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);

        errorBox.getChildren().addAll(titleLabel, messageLabel);
        detailViewContainer.getChildren().setAll(errorBox);
    }
}