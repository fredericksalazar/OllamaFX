package com.org.ollamafx.controller;

import com.org.ollamafx.manager.ModelManager;
import com.org.ollamafx.manager.OllamaManager;
import com.org.ollamafx.model.OllamaModel;

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

    // Debounce for selection to prevent UI flooding
    private javafx.animation.PauseTransition selectionDebounce;
    private Task<List<OllamaModel>> currentTask;

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

        // Initialize debounce
        selectionDebounce = new javafx.animation.PauseTransition(javafx.util.Duration.millis(250));
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

        // Cancel previous task if running
        if (currentTask != null && CodeBlock.isRunning(currentTask)) {
            System.out.println("DEBUG: Cancelling previous running task.");
            currentTask.cancel();
        }

        ProgressIndicator progressIndicator = new ProgressIndicator();
        detailViewContainer.getChildren().setAll(progressIndicator);
        System.out.println("DEBUG: ProgressIndicator set.");

        // La tarea ahora espera una Lista de OllamaModel
        Task<List<OllamaModel>> loadDetailsTask = new Task<>() {
            @Override
            protected List<OllamaModel> call() throws Exception {
                System.out.println("DEBUG: Background Task started for " + selectedModel.getName());
                } catch (Exception e) {
                    System.err.println("DEBUG: Error in background task: " + e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
            }
        };

    currentTask=loadDetailsTask; // Track it

    loadDetailsTask.setOnSucceeded(event->

    {
        System.out.println("DEBUG: Task succeeded. Updating UI...");
        List<OllamaModel> details = loadDetailsTask.getValue();
        Platform.runLater(() -> {
            System.out.println("DEBUG: Calling showDetailsInView on UI thread.");
            showDetailsInView(details);
        });
    });

    loadDetailsTask.setOnFailed(event->
    {
        System.err.println("DEBUG: Task failed event.");
        if (loadDetailsTask.getException() != null) {
            loadDetailsTask.getException().printStackTrace();
        }
        Platform.runLater(() -> showErrorInView("Error Loading Details", "Failed to load model details."));
    });

    // ... (onFailed se queda igual)
    new Thread(loadDetailsTask).start();System.out.println("DEBUG: Task thread started.");
}

// Helper to check task state implicitly
private static class CodeBlock {
    static boolean isRunning(Task<?> task) {
        return task.getState() == javafx.concurrent.Worker.State.RUNNING
                || task.getState() == javafx.concurrent.Worker.State.SCHEDULED;
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