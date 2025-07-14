package com.org.ollamafx.manager;

import com.org.ollamafx.model.OllamaModel;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import java.util.List;

public class ModelManager {

    private final ObservableList<OllamaModel> localModels = FXCollections.observableArrayList();
    private final ObservableList<OllamaModel> availableModels = FXCollections.observableArrayList();
    private final OllamaManager ollamaManager = OllamaManager.getInstance();

    public ObservableList<OllamaModel> getLocalModels() {
        return localModels;
    }

    public ObservableList<OllamaModel> getAvailableModels() {
        return availableModels;
    }

    /**
     * Inicia la carga de todos los modelos en un hilo de fondo (background thread).
     */
    public void loadAllModels() {
        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Carga de modelos locales
                System.out.println("ModelManager: Iniciando carga de modelos locales...");
                final List<OllamaModel> fetchedLocalModels = ollamaManager.getLocalModels();
                Platform.runLater(() -> localModels.setAll(fetchedLocalModels));
                System.out.println("ModelManager: Carga de modelos locales finalizada.");

                // Carga de modelos disponibles (la versión rápida)
                System.out.println("ModelManager: Iniciando carga de modelos base disponibles...");
                final List<OllamaModel> fetchedAvailableModels = ollamaManager.getAvailableBaseModels(); // <-- LÍNEA CORREGIDA
                Platform.runLater(() -> availableModels.setAll(fetchedAvailableModels));
                System.out.println("ModelManager: Carga de modelos base disponibles finalizada.");

                return null;
            }
        };

        loadTask.setOnFailed(workerStateEvent -> {
            System.err.println("La tarea de carga de modelos ha fallado.");
            if (loadTask.getException() != null) {
                loadTask.getException().printStackTrace();
            }
        });

        new Thread(loadTask).start();
    }
}