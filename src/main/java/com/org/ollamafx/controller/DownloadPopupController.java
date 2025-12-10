package com.org.ollamafx.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;

public class DownloadPopupController {

    @FXML
    private javafx.scene.layout.VBox rootPane;
    @FXML
    private Label titleLabel;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label percentageLabel;
    @FXML
    private Button cancelButton;

    private Task<Void> downloadTask;
    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void initialize() {
        // Draggable Window
        rootPane.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        rootPane.setOnMouseDragged(event -> {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
    }

    public void setDownloadTask(Task<Void> task) {
        this.downloadTask = task;

        // Bind UI elements to the task
        progressBar.progressProperty().bind(task.progressProperty());

        task.progressProperty().addListener((obs, oldVal, newVal) -> {
            double progress = newVal.doubleValue();
            int percentage = (int) (progress * 100);
            percentageLabel.setText(percentage + "%");
        });

        // Close window when task finishes (Succeeded, Failed, or Cancelled)
        task.stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED ||
                    newState == javafx.concurrent.Worker.State.FAILED ||
                    newState == javafx.concurrent.Worker.State.CANCELLED) {
                closeWindow();
            }
        });
    }

    public void setModelName(String modelName) {
        titleLabel.setText(com.org.ollamafx.App.getBundle().getString("download.title") + " " + modelName);
    }

    @FXML
    private void onCancel() {
        if (downloadTask != null && downloadTask.isRunning()) {
            downloadTask.cancel();
        }
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}
