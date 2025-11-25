package com.org.ollamafx.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;

public class DownloadPopupController {

    @FXML
    private Label titleLabel;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label percentageLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Button cancelButton;

    private Task<Void> downloadTask;

    public void setDownloadTask(Task<Void> task) {
        this.downloadTask = task;

        // Bind UI elements to the task
        progressBar.progressProperty().bind(task.progressProperty());
        statusLabel.textProperty().bind(task.messageProperty());

        task.progressProperty().addListener((obs, oldVal, newVal) -> {
            int percentage = (int) (newVal.doubleValue() * 100);
            percentageLabel.setText(percentage + "%");
        });

        // Close window when task succeeds or fails
        task.setOnSucceeded(e -> closeWindow());
        task.setOnFailed(e -> closeWindow());
        task.setOnCancelled(e -> closeWindow());
    }

    public void setModelName(String modelName) {
        titleLabel.setText("Downloading " + modelName + "...");
    }

    @FXML
    private void onCancel() {
        if (downloadTask != null && downloadTask.isRunning()) {
            downloadTask.cancel();
            statusLabel.setText("Cancelling...");
        }
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}
