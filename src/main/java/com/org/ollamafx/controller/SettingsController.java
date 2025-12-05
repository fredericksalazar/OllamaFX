package com.org.ollamafx.controller;

import com.org.ollamafx.manager.ConfigManager;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class SettingsController {

    @FXML
    private TextField hostTextField;

    @FXML
    private Button themeButton;

    @FXML
    private Label statusLabel;

    private final ConfigManager configManager = ConfigManager.getInstance();

    @FXML
    public void initialize() {
        hostTextField.setText(configManager.getOllamaHost());
        // Initial theme text update if needed, though we might not know current state
        // easily without checking CSS
    }

    @FXML
    private void saveSettings() {
        String newHost = hostTextField.getText();
        if (newHost != null && !newHost.trim().isEmpty()) {
            configManager.setOllamaHost(newHost.trim());
            statusLabel.setText("Settings saved! Restart might be required for connection changes.");
            statusLabel.setStyle("-fx-text-fill: green;");
        } else {
            statusLabel.setText("Invalid Host URL");
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void toggleTheme() {
        if (javafx.application.Application.getUserAgentStylesheet()
                .equals(new atlantafx.base.theme.PrimerDark().getUserAgentStylesheet())) {
            // Switch to Light
            javafx.application.Application
                    .setUserAgentStylesheet(new atlantafx.base.theme.PrimerLight().getUserAgentStylesheet());
            // We need to access the scene to update the style class for local variables
            if (themeButton.getScene() != null) {
                themeButton.getScene().getRoot().getStyleClass().add("light");
            }
        } else {
            // Switch to Dark
            javafx.application.Application
                    .setUserAgentStylesheet(new atlantafx.base.theme.PrimerDark().getUserAgentStylesheet());
            if (themeButton.getScene() != null) {
                themeButton.getScene().getRoot().getStyleClass().remove("light");
            }
        }
    }
}
