package com.org.ollamafx.controller;

import com.org.ollamafx.manager.OllamaManager;
import javafx.application.HostServices;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;

public class MainController {

    private HostServices hostServices;

    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    @FXML
    public void initialize() {
        OllamaManager manager = OllamaManager.getInstance();
        if (!manager.isOllamaInstalled()) {
            showOllamaNotInstalled();
            openOllamaWebsite();
        } else {
            showOllamaInstalled(manager.getOllamaVersion());
        }
    }

    private void showOllamaNotInstalled() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ollama no instalado");
        alert.setHeaderText("Ollama no se encuentra en tu sistema.");
        alert.setContentText("Por favor, instala Ollama desde su sitio oficial.");
        alert.showAndWait();
    }

    private void openOllamaWebsite() {
        if (hostServices != null) {
            hostServices.showDocument("https://ollama.com");
        }
    }

    private void showOllamaInstalled(String version) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Ollama instalado");
        alert.setHeaderText("¡Ollama está instalado!");
        alert.setContentText("Versión detectada: " + version);
        alert.showAndWait();
    }
}

