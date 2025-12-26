package com.org.ollamafx.controller;

import atlantafx.base.controls.RingProgressIndicator;
import com.org.ollamafx.App;
import com.org.ollamafx.manager.ModelLibraryManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;

import java.io.InputStream;
import java.util.Properties;
import java.util.ResourceBundle;

public class SplashController {

    @FXML
    private Label titleLabel;
    @FXML
    private Label counterLabel;
    @FXML
    private RingProgressIndicator progressIndicator;
    @FXML
    private Label actionLabel;
    @FXML
    private Label modelLabel;
    @FXML
    private Label descriptionLabel;
    @FXML
    private Label descriptionSecondaryLabel;
    @FXML
    private Label modelsLabel;

    private final ModelLibraryManager libraryManager = ModelLibraryManager.getInstance();
    private ResourceBundle bundle;

    @FXML
    public void initialize() {
        bundle = App.getBundle();

        // Set version from app.properties
        String version = loadAppVersion();
        titleLabel.setText("OllamaFX v" + version);

        // Set i18n labels
        modelsLabel.setText(bundle.getString("splash.models"));
        descriptionLabel.setText(bundle.getString("splash.description"));
        descriptionSecondaryLabel.setText(bundle.getString("splash.description.secondary"));

        startInitialization();
    }

    private String loadAppVersion() {
        try (InputStream input = getClass().getResourceAsStream("/app.properties")) {
            if (input != null) {
                Properties props = new Properties();
                props.load(input);
                return props.getProperty("app.version", "0.0.0");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "0.0.0";
    }

    private void startInitialization() {
        Thread initThread = new Thread(() -> {
            try {
                Platform.runLater(() -> {
                    counterLabel.setText("...");
                    actionLabel.setText(bundle.getString("splash.verifying"));
                    modelLabel.setText("");
                });
                Thread.sleep(600);

                if (!libraryManager.isOllamaInstalled()) {
                    Platform.runLater(this::showOllamaMissingAlert);
                    return;
                }

                ModelLibraryManager.UpdateStatus status = libraryManager.getUpdateStatus();
                boolean needsUpdate = (status == ModelLibraryManager.UpdateStatus.OUTDATED_HARD);

                if (needsUpdate) {
                    Thread crawlerThread = new Thread(() -> {
                        try {
                            libraryManager.updateLibraryFull();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    crawlerThread.start();

                    while (crawlerThread.isAlive()) {
                        String statusRaw = libraryManager.getStatus();

                        Platform.runLater(() -> {
                            progressIndicator.setProgress(-1.0);

                            if (statusRaw.startsWith("Procesando (")) {
                                try {
                                    int closeParen = statusRaw.indexOf(')');
                                    if (closeParen > 0) {
                                        String counts = statusRaw.substring(statusRaw.indexOf('(') + 1, closeParen);
                                        String modelName = statusRaw.substring(closeParen + 2).trim();

                                        counterLabel.setText(counts.replace("/", " / "));
                                        actionLabel.setText(bundle.getString("splash.downloading"));
                                        modelLabel.setText(modelName);
                                    }
                                } catch (Exception e) {
                                    modelLabel.setText("...");
                                }
                            } else if (statusRaw.startsWith("Escaneando")) {
                                counterLabel.setText("...");
                                actionLabel.setText(bundle.getString("splash.discovering"));
                                modelLabel.setText(statusRaw.replace("Escaneando ", "").replace("...", ""));
                            } else {
                                actionLabel.setText("...");
                                modelLabel.setText(statusRaw);
                            }
                        });
                        Thread.sleep(100);
                    }
                } else {
                    Platform.runLater(() -> {
                        counterLabel.setText("✓");
                        actionLabel.setText(bundle.getString("splash.ready"));
                        modelLabel.setText("");
                        progressIndicator.setProgress(1.0);
                    });
                    Thread.sleep(800);
                }

                Platform.runLater(this::launchMainApp);

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showErrorAlert(e));
            }
        });
        initThread.setDaemon(true);
        initThread.start();
    }

    private void showOllamaMissingAlert() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error de Ollama");
        alert.setHeaderText("Ollama no está instalado o corriendo.");
        alert.setContentText("Por favor verifica que Ollama esté instalado y ejecutándose.");
        alert.showAndWait();
        Platform.exit();
    }

    private void showErrorAlert(Throwable e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error de Inicialización");
        alert.setHeaderText("Ocurrió un error al iniciar.");
        alert.setContentText(e != null ? e.getMessage() : "Error desconocido");
        alert.showAndWait();
        Platform.exit();
    }

    private void launchMainApp() {
        App.reloadUI();
    }
}
