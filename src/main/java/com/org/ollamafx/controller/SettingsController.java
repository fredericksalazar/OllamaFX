package com.org.ollamafx.controller;

import com.org.ollamafx.manager.ConfigManager;
import com.org.ollamafx.manager.HardwareManager;
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
    private javafx.scene.control.ComboBox<String> languageComboBox;

    @FXML
    private Label statusLabel;

    @FXML
    private Label ramLabel;

    @FXML
    private Label vramLabel;

    @FXML
    private Label cpuLabel;

    @FXML
    private Label osLabel;

    private final ConfigManager configManager = ConfigManager.getInstance();

    @FXML
    public void initialize() {
        hostTextField.setText(configManager.getOllamaHost());

        // Populate Hardware Info
        ramLabel.setText(HardwareManager.getRamDetails());
        if (vramLabel != null)
            vramLabel.setText(HardwareManager.getVramDetails());
        cpuLabel.setText(HardwareManager.getCpuDetails());
        osLabel.setText(HardwareManager.getOsDetails());

        // Language Setup
        languageComboBox.getItems().addAll("English", "Español");
        String currentLang = configManager.getLanguage();
        if ("es".equals(currentLang)) {
            languageComboBox.getSelectionModel().select("Español");
        } else {
            languageComboBox.getSelectionModel().select("English");
        }

        languageComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                if (newVal.equals("Español")) {
                    configManager.setLanguage("es");
                } else {
                    configManager.setLanguage("en");
                }
                // Hot reload the UI
                com.org.ollamafx.App.reloadUI();
            }
        });
    }

    @FXML
    private void saveSettings() {
        String newHost = hostTextField.getText();
        if (newHost != null && !newHost.trim().isEmpty()) {
            configManager.setOllamaHost(newHost.trim());
            statusLabel.setText(com.org.ollamafx.App.getBundle().getString("settings.status.saved"));
            statusLabel.setStyle("-fx-text-fill: green;");
        } else {
            statusLabel.setText(com.org.ollamafx.App.getBundle().getString("settings.status.invalid"));
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void showHardwareLogic() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/ui/hardware_explanation_popup.fxml"));
            loader.setResources(com.org.ollamafx.App.getBundle());
            javafx.scene.Parent root = loader.load();

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle(com.org.ollamafx.App.getBundle().getString("settings.hardware.popup.title"));
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void toggleTheme() {
        if (javafx.application.Application.getUserAgentStylesheet()
                .equals(new atlantafx.base.theme.CupertinoDark().getUserAgentStylesheet())) {
            // Switch to Light
            javafx.application.Application
                    .setUserAgentStylesheet(new atlantafx.base.theme.CupertinoLight().getUserAgentStylesheet());
            if (themeButton.getScene() != null) {
                themeButton.getScene().getRoot().getStyleClass().remove("dark");
                themeButton.getScene().getRoot().getStyleClass().add("light");
            }
        } else {
            // Switch to Dark
            javafx.application.Application
                    .setUserAgentStylesheet(new atlantafx.base.theme.CupertinoDark().getUserAgentStylesheet());
            if (themeButton.getScene() != null) {
                themeButton.getScene().getRoot().getStyleClass().remove("light");
                themeButton.getScene().getRoot().getStyleClass().add("dark");
            }
        }
    }
}
