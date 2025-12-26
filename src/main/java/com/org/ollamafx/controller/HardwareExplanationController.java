package com.org.ollamafx.controller;

import com.org.ollamafx.manager.HardwareManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class HardwareExplanationController {

    @FXML
    private Label totalRamLabel;
    @FXML
    private Label vramLabel;
    @FXML
    private Label safeLimitLabel;

    @FXML
    public void initialize() {
        HardwareManager.HardwareStats stats = HardwareManager.getStats();

        totalRamLabel.setText(String.format("%.2f GB", stats.getTotalRamGB()));

        if (stats.isUnifiedMemory) {
            vramLabel.setText("Unified");
        } else {
            vramLabel.setText(String.format("%.2f GB", stats.getVramGB()));
        }

        double safeLimitGb = (stats.totalRamBytes - 4L * 1024 * 1024 * 1024) / (1024.0 * 1024.0 * 1024.0);
        safeLimitLabel.setText(String.format("%.2f GB", safeLimitGb));
    }

    @FXML
    private void close() {
        Stage stage = (Stage) totalRamLabel.getScene().getWindow();
        stage.close();
    }
}
