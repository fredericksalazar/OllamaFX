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
    private final java.util.Set<String> installedModelsCache = new java.util.HashSet<>();

    public ModelManager() {
        // Keep cache in sync
        localModels.addListener((javafx.collections.ListChangeListener<OllamaModel>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (OllamaModel m : c.getAddedSubList()) {
                        installedModelsCache.add(m.getName().toLowerCase() + ":" + m.getTag().toLowerCase());
                    }
                }
                if (c.wasRemoved()) {
                    for (OllamaModel m : c.getRemoved()) {
                        installedModelsCache.remove(m.getName().toLowerCase() + ":" + m.getTag().toLowerCase());
                    }
                }
            }
        });
    }

    public ObservableList<OllamaModel> getLocalModels() {
        return localModels;
    }

    // ... (rest of getters)

    // ... (loadAllModels remains same)

    /**
     * Verifica si un modelo específico (nombre y tag) está instalado localmente.
     * Uses O(1) cache.
     */
    public boolean isModelInstalled(String name, String tag) {
        if (name == null || tag == null)
            return false;
        return installedModelsCache.contains(name.toLowerCase() + ":" + tag.toLowerCase());
    }

    // ... (deleteModel and other methods remain same... ensure to keep the existing
    // implementation just replacing what's needed)

    // I need to use ReplaceFileContent carefully.
    // I will replace the class start and constructor, and the isModelInstalled
    // method.
    // Wait, the tool requires contiguous blocks. I should do this in chunks or
    // replace the whole file content helper parts.

    // Let's replace the top part first to add the cache and constructor.

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
                final List<OllamaModel> fetchedAvailableModels = ollamaManager.getAvailableBaseModels(); // <-- LÍNEA
                                                                                                         // CORREGIDA
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

    public void deleteModel(String name, String tag) {
        Task<Void> deleteTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                ollamaManager.deleteModel(name, tag);
                return null;
            }
        };

        deleteTask.setOnSucceeded(e -> {
            System.out.println("Model deleted successfully: " + name + ":" + tag);
            refreshLocalModels();
        });

        deleteTask.setOnFailed(e -> {
            System.err.println("Failed to delete model: " + name + ":" + tag);
            if (deleteTask.getException() != null) {
                deleteTask.getException().printStackTrace();
            }
        });

        new Thread(deleteTask).start();
    }

    public void refreshLocalModels() {
        System.out.println("ModelManager: Refreshing local models...");
        Task<List<OllamaModel>> refreshTask = new Task<>() {
            @Override
            protected List<OllamaModel> call() throws Exception {
                return ollamaManager.getLocalModels();
            }
        };

        refreshTask.setOnSucceeded(e -> {
            List<OllamaModel> models = refreshTask.getValue();
            System.out.println("ModelManager: Local models refreshed. Count: " + models.size());
            for (OllamaModel m : models) {
                System.out.println(" - Found: " + m.getName() + ":" + m.getTag());
            }
            Platform.runLater(() -> localModels.setAll(models));
        });

        refreshTask.setOnFailed(e -> {
            System.err.println("ModelManager: Failed to refresh local models.");
            if (refreshTask.getException() != null) {
                refreshTask.getException().printStackTrace();
            }
        });

        new Thread(refreshTask).start();
    }

    public void addLocalModel(OllamaModel model) {
        Platform.runLater(() -> {
            System.out.println("ModelManager: Attempting to add model: " + model.getName() + ":" + model.getTag());
            // Check if it already exists to avoid duplicates
            boolean exists = localModels.stream()
                    .anyMatch(m -> m.getName().equalsIgnoreCase(model.getName())
                            && m.getTag().equalsIgnoreCase(model.getTag()));

            if (!exists) {
                localModels.add(model);
                System.out.println("ModelManager: Added new model directly. List size now: " + localModels.size());
            } else {
                System.out.println("ModelManager: Model already exists in list. Not adding.");
            }
        });
    }
}