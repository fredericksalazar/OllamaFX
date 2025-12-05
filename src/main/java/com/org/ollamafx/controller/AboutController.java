package com.org.ollamafx.controller;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import java.net.URL;
import java.util.Properties;
import java.util.ResourceBundle;
import java.io.InputStream;

public class AboutController implements Initializable {

    @FXML
    private Label versionLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
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
    }

    @FXML
    private void openGitHub() {
        openLink("https://github.com/fredericksalazar/OllamaFX"); // Replace with actual repo if different
    }

    private void openLink(String url) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        } else {
            // Fallback for non-desktop environments (though JavaFX implies desktop)
            System.err.println("Desktop browse not supported. URL: " + url);
            // On Linux/Mac sometimes ProcessBuilder is more reliable if Desktop fails,
            // but Desktop is standard.
        }
    }
}
