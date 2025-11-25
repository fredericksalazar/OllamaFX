package com.org.ollamafx.controller;

import com.org.ollamafx.manager.ModelManager;
import com.org.ollamafx.model.OllamaModel;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import java.util.List;

public class ModelDetailController {

    @FXML
    private Label modelNameLabel;
    @FXML
    private Text modelDescriptionText;
    @FXML
    private TableView<OllamaModel> tagsTable;
    @FXML
    private TableColumn<OllamaModel, String> tagNameColumn;
    @FXML
    private TableColumn<OllamaModel, String> tagSizeColumn;
    @FXML
    private TableColumn<OllamaModel, String> tagContextColumn;
    @FXML
    private TableColumn<OllamaModel, String> tagInputColumn;
    @FXML
    private TableColumn<OllamaModel, Void> tagActionColumn;

    private ModelManager modelManager;

    public void setModelManager(ModelManager modelManager) {
        this.modelManager = modelManager;

        // Listen for changes in local models to refresh the table automatically
        if (this.modelManager != null) {
            this.modelManager.getLocalModels()
                    .addListener((javafx.collections.ListChangeListener.Change<? extends OllamaModel> c) -> {
                        tagsTable.refresh();
                    });
        }
    }

    @FXML
    public void initialize() {
        tagNameColumn.setCellValueFactory(cellData -> cellData.getValue().tagProperty());

        tagSizeColumn.setCellValueFactory(cellData -> cellData.getValue().sizeProperty());
        tagSizeColumn.setComparator((size1, size2) -> {
            return parseSize(size1).compareTo(parseSize(size2));
        });

        tagContextColumn.setCellValueFactory(cellData -> cellData.getValue().contextLengthProperty());
        tagInputColumn.setCellValueFactory(cellData -> cellData.getValue().inputTypeProperty());

        // Configurar la columna de acción con un botón
        tagActionColumn.setCellFactory(param -> new javafx.scene.control.TableCell<>() {
            private final Button btn = new Button();

            {
                // Centrar el botón en la celda
                setAlignment(Pos.CENTER);

                btn.setOnAction(event -> {
                    OllamaModel model = getTableView().getItems().get(getIndex());
                    boolean isInstalled = modelManager != null
                            && modelManager.isModelInstalled(model.getName(), model.getTag());

                    if (isInstalled) {
                        // Lógica de desinstalación real
                        System.out.println("Uninstalling: " + model.getName() + ":" + model.getTag());
                        btn.setDisable(true); // Disable while deleting
                        if (modelManager != null) {
                            modelManager.deleteModel(model.getName(), model.getTag());
                            // Table refresh is now handled by the listener on localModels
                        }
                    } else {
                        // Lógica de instalación con Popup
                        showDownloadPopup(model);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    OllamaModel model = getTableView().getItems().get(getIndex());
                    boolean isInstalled = modelManager != null
                            && modelManager.isModelInstalled(model.getName(), model.getTag());

                    if (isInstalled) {
                        btn.setText("Uninstall");
                        btn.getStyleClass().removeAll("success");
                        btn.getStyleClass().add("danger");
                    } else {
                        btn.setText("Install");
                        btn.getStyleClass().removeAll("danger");
                        btn.getStyleClass().add("success");
                    }
                    setGraphic(btn);
                }
            }
        });
    }

    /**
     * Muestra el popup de descarga e inicia la tarea.
     */
    private void showDownloadPopup(OllamaModel model) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/download_popup.fxml"));
            javafx.scene.Parent root = loader.load();
            DownloadPopupController controller = loader.getController();
            controller.setModelName(model.getName() + ":" + model.getTag());

            // Fix: Apply theme to popup
            String userAgentStylesheet = javafx.application.Application.getUserAgentStylesheet();
            if (userAgentStylesheet != null && userAgentStylesheet.toLowerCase().contains("light")) {
                root.getStyleClass().add("light");
            }

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT); // Transparent window
            stage.setTitle("Downloading Model");

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT); // Transparent scene background
            stage.setScene(scene);
            stage.setResizable(false);

            // Crear la tarea de descarga
            javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
                @Override
                protected Void call() throws Exception {
                    updateMessage("Starting download process...");
                    updateProgress(0, 100);

                    com.org.ollamafx.manager.OllamaManager.getInstance().pullModel(model.getName(), model.getTag(),
                            (progress, status) -> {
                                updateMessage(status);
                                if (progress >= 0) {
                                    updateProgress(progress, 100);
                                } else {
                                    updateProgress(-1, 100); // Indeterminado
                                }
                            });

                    return null;
                }
            };

            task.setOnSucceeded(e -> {
                System.out.println("Download complete!");
                if (modelManager != null) {
                    // Create the new model object with the info we have
                    // We might not have exact size/date yet, but we can update those later or use
                    // placeholders
                    // For now, use "Installed" and current date or "Just now"
                    String date = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                            .format(java.time.LocalDateTime.now());
                    OllamaModel newModel = new OllamaModel(
                            model.getName(),
                            "Installed locally",
                            "N/A",
                            model.getTag(),
                            model.sizeProperty().get(), // Use the size from the available model info
                            date,
                            model.getContextLength(),
                            model.getInputType());

                    modelManager.addLocalModel(newModel);

                    // Optional: Trigger a background refresh to get exact metadata eventually
                    // modelManager.refreshLocalModels();
                }
            });

            task.setOnFailed(e -> {
                System.err.println("Download failed: " + task.getException().getMessage());
                // Mostrar error
            });

            controller.setDownloadTask(task);

            // Iniciar la tarea en un hilo
            new Thread(task).start();

            stage.showAndWait();

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Puebla la vista con la lista de tags del modelo seleccionado.
     */
    public void populateDetails(List<OllamaModel> modelTags) {
        if (modelTags == null || modelTags.isEmpty()) {
            modelNameLabel.setText("Error");
            modelDescriptionText
                    .setText("Could not retrieve model details. The library's web scraping might be outdated.");
            tagsTable.getItems().clear();
            return;
        }

        OllamaModel firstTag = modelTags.get(0);
        modelNameLabel.setText(firstTag.getName());
        modelDescriptionText.setText(firstTag.descriptionProperty().get());

        tagsTable.setItems(javafx.collections.FXCollections.observableArrayList(modelTags));
    }

    private Long parseSize(String sizeStr) {
        if (sizeStr == null || sizeStr.isEmpty() || sizeStr.equals("N/A")) {
            return -1L;
        }
        try {
            String[] parts = sizeStr.trim().split("\\s+");
            if (parts.length < 2)
                return 0L;

            double value = Double.parseDouble(parts[0]);
            String unit = parts[1].toUpperCase();

            long multiplier = 1;
            switch (unit) {
                case "KB":
                    multiplier = 1024;
                    break;
                case "MB":
                    multiplier = 1024 * 1024;
                    break;
                case "GB":
                    multiplier = 1024 * 1024 * 1024;
                    break;
                case "TB":
                    multiplier = 1024L * 1024 * 1024 * 1024;
                    break;
            }

            return (long) (value * multiplier);
        } catch (Exception e) {
            return 0L;
        }
    }
}