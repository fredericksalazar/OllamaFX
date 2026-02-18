package com.org.ollamafx.controller;

import com.org.ollamafx.App;
import com.org.ollamafx.manager.GitHubStatsService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;

import java.net.URL;
import java.util.Properties;
import java.util.ResourceBundle;
import java.io.InputStream;
import java.io.IOException;

public class AboutController implements Initializable {

    @FXML
    private Label versionLabel;
    @FXML
    private Label downloadsLabel;
    @FXML
    private ProgressIndicator downloadSpinner;

    // Buttons with icons set programmatically
    @FXML
    private Button paypalButton;
    @FXML
    private Button coffeeButton;
    @FXML
    private Button githubButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Load version
        String version = "Unknown";
        try (InputStream input = getClass().getResourceAsStream("/app.properties")) {
            if (input != null) {
                Properties prop = new Properties();
                prop.load(input);
                version = prop.getProperty("app.version", "Unknown");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        versionLabel.setText("Version " + version);

        // Set emoji icons as button graphics
        setButtonIcon(paypalButton, "💳");
        setButtonIcon(coffeeButton, "☕");
        setButtonIcon(githubButton, "🐙");

        // Load GitHub stats
        loadGitHubStats();
    }

    /**
     * Sets an emoji Label as the graphic (left icon) of a Button.
     */
    private void setButtonIcon(Button button, String emoji) {
        if (button == null)
            return;
        Label icon = new Label(emoji);
        icon.setStyle("-fx-font-size: 15px;");
        button.setGraphic(icon);
        button.setGraphicTextGap(8);
    }

    private void loadGitHubStats() {
        if (downloadsLabel != null && downloadSpinner != null) {
            downloadsLabel.setText("...");
            downloadSpinner.setVisible(true);
        }

        GitHubStatsService.getInstance().fetchTotalDownloads()
                .thenAccept(downloads -> {
                    Platform.runLater(() -> {
                        if (downloads > 0) {
                            downloadsLabel.setText(String.format("%,d", downloads));
                        } else {
                            downloadsLabel.setText("N/A");
                        }
                        if (downloadSpinner != null) {
                            downloadSpinner.setVisible(false);
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        downloadsLabel.setText("N/A");
                        if (downloadSpinner != null) {
                            downloadSpinner.setVisible(false);
                        }
                    });
                    return null;
                });
    }

    @FXML
    private void openGitHub() {
        openLink("https://github.com/fredericksalazar/OllamaFX");
    }

    @FXML
    private void openPayPal() {
        // Using standard donation link with email since paypal.me handle is uncertain
        openLink("https://www.paypal.com/donate/?business=fredefass01@gmail.com");
    }

    @FXML
    private void openBuyMeCoffee() {
        openLink("https://buymeacoffee.com/fredericksalazar");
    }

    @FXML
    private void openGitHubContribute() {
        openLink("https://github.com/fredericksalazar/OllamaFX/issues");
    }

    private void openLink(String url) {
        try {
            if (App.getAppHostServices() != null) {
                App.getAppHostServices().showDocument(url);
            } else {
                System.err.println("HostServices not available. URL: " + url);
            }
        } catch (Exception e) {
            System.err.println("Error opening URL: " + url);
            e.printStackTrace();
        }
    }
}
