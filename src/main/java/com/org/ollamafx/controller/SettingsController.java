package com.org.ollamafx.controller;

import com.org.ollamafx.App;
import com.org.ollamafx.manager.ConfigManager;
import com.org.ollamafx.manager.HardwareManager;
import com.org.ollamafx.manager.LibraryCacheManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;

public class SettingsController {

    @FXML
    private TextField hostTextField;
    @FXML
    private Button themeButton;
    @FXML
    private ComboBox<String> languageComboBox;
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

    // Library Cache Labels
    @FXML
    private Label cacheFileLabel;
    @FXML
    private Label cacheLocationLabel;
    @FXML
    private Label cacheSizeLabel;
    @FXML
    private Label cacheLastUpdateLabel;
    @FXML
    private Label cacheDaysLabel;
    @FXML
    private Button refreshLibraryButton;

    private final ConfigManager configManager = ConfigManager.getInstance();
    private final LibraryCacheManager cacheManager = LibraryCacheManager.getInstance();
    private ResourceBundle bundle;

    @FXML
    public void initialize() {
        bundle = App.getBundle();

        hostTextField.setText(configManager.getOllamaHost());

        // Populate Hardware Info
        ramLabel.setText(HardwareManager.getRamDetails());
        if (vramLabel != null)
            vramLabel.setText(HardwareManager.getVramDetails());
        cpuLabel.setText(HardwareManager.getCpuDetails());
        osLabel.setText(HardwareManager.getOsDetails());

        // Populate Library Cache Info
        populateCacheInfo();

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
                App.reloadUI();
            }
        });
    }

    private void populateCacheInfo() {
        if (cacheManager.cacheExists()) {
            cacheFileLabel.setText(cacheManager.getCacheFileName());
            cacheLocationLabel.setText(cacheManager.getCacheFilePath());
            cacheSizeLabel.setText(cacheManager.getCacheFileSizeKB() + " KB");

            long lastUpdated = cacheManager.getLastUpdatedTimestamp();
            if (lastUpdated > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                cacheLastUpdateLabel.setText(sdf.format(new Date(lastUpdated)));

                int days = cacheManager.getDaysSinceUpdate();
                cacheDaysLabel.setText(days + " " + (days == 1 ? "día" : "días"));
            } else {
                cacheLastUpdateLabel.setText(bundle.getString("settings.library.never"));
                cacheDaysLabel.setText("-");
            }
        } else {
            String noCache = bundle.getString("settings.library.nocache");
            cacheFileLabel.setText(noCache);
            cacheLocationLabel.setText("-");
            cacheSizeLabel.setText("-");
            cacheLastUpdateLabel.setText(bundle.getString("settings.library.never"));
            cacheDaysLabel.setText("-");
        }
    }

    @FXML
    private void saveSettings() {
        String newHost = hostTextField.getText();
        if (newHost != null && !newHost.trim().isEmpty()) {
            configManager.setOllamaHost(newHost.trim());
            statusLabel.setText(bundle.getString("settings.status.saved"));
            statusLabel.setStyle("-fx-text-fill: green;");
        } else {
            statusLabel.setText(bundle.getString("settings.status.invalid"));
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void refreshLibrary() {
        // Delete cache to force redownload
        try {
            java.io.File cacheFile = new java.io.File(cacheManager.getCacheFilePath());
            if (cacheFile.exists()) {
                cacheFile.delete();
                System.out.println("SettingsController: Cache file deleted for refresh.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Reload from Splash Screen to trigger download
        App.reloadFromSplash();
    }

    @FXML
    private void showHardwareLogic() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/ui/hardware_explanation_popup.fxml"));
            loader.setResources(bundle);
            javafx.scene.Parent root = loader.load();

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle(bundle.getString("settings.hardware.popup.title"));
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
            javafx.application.Application
                    .setUserAgentStylesheet(new atlantafx.base.theme.CupertinoLight().getUserAgentStylesheet());
            if (themeButton.getScene() != null) {
                themeButton.getScene().getRoot().getStyleClass().remove("dark");
                themeButton.getScene().getRoot().getStyleClass().add("light");
            }
        } else {
            javafx.application.Application
                    .setUserAgentStylesheet(new atlantafx.base.theme.CupertinoDark().getUserAgentStylesheet());
            if (themeButton.getScene() != null) {
                themeButton.getScene().getRoot().getStyleClass().remove("light");
                themeButton.getScene().getRoot().getStyleClass().add("dark");
            }
        }
    }
}
