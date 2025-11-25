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
// import javafx.scene.layout.HBox; // Unused
// import javafx.scene.layout.Priority; // Unused
// import javafx.scene.layout.VBox; // Unused
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
    private Label parameterSizeLabel;

    @FXML
    public void initialize() {
        tagNameColumn.setCellValueFactory(cellData -> cellData.getValue().tagProperty());

        tagSizeColumn.setCellValueFactory(cellData -> cellData.getValue().sizeProperty());
        tagSizeColumn.setComparator((size1, size2) -> {
            return com.org.ollamafx.util.Utils.parseSize(size1).compareTo(com.org.ollamafx.util.Utils.parseSize(size2));
        });

        tagContextColumn.setCellValueFactory(cellData -> cellData.getValue().contextLengthProperty());
        tagInputColumn.setCellValueFactory(cellData -> cellData.getValue().inputTypeProperty());

        // Configurar la columna de acción con un botón de icono
        tagActionColumn.setCellFactory(param -> new javafx.scene.control.TableCell<>() {
            private final Button btn = new Button();
            private final javafx.scene.layout.Region icon = new javafx.scene.layout.Region();

            {
                setAlignment(Pos.CENTER);
                btn.getStyleClass().add("icon-button");
                btn.setGraphic(icon);
                icon.getStyleClass().add("icon-region");

                btn.setOnAction(event -> {
                    OllamaModel model = getTableView().getItems().get(getIndex());
                    boolean isInstalled = modelManager != null
                            && modelManager.isModelInstalled(model.getName(), model.getTag());

                    if (isInstalled) {
                        // Uninstall
                        btn.setDisable(true);
                        if (modelManager != null) {
                            modelManager.deleteModel(model.getName(), model.getTag());
                        }
                    } else {
                        // Install
                        ModelDetailController.this.showDownloadPopup(model);
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
                        // Trash Icon
                        icon.getStyleClass().removeAll("icon-download");
                        icon.getStyleClass().add("icon-trash");
                        btn.getStyleClass().add("danger");
                        javafx.scene.control.Tooltip.install(btn, new javafx.scene.control.Tooltip("Uninstall"));
                    } else {
                        // Download Icon
                        icon.getStyleClass().removeAll("icon-trash");
                        icon.getStyleClass().add("icon-download");
                        btn.getStyleClass().removeAll("danger");
                        javafx.scene.control.Tooltip.install(btn, new javafx.scene.control.Tooltip("Install"));
                    }
                    setGraphic(btn);
                    btn.setDisable(false);
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
                    String date = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                            .format(java.time.LocalDateTime.now());
                    OllamaModel newModel = new OllamaModel(
                            model.getName(),
                            "Installed locally",
                            "N/A",
                            model.getTag(),
                            model.sizeProperty().get(),
                            date,
                            model.getContextLength(),
                            model.getInputType());

                    modelManager.addLocalModel(newModel);
                }
            });

            task.setOnFailed(e -> {
                System.err.println("Download failed: " + task.getException().getMessage());
            });

            controller.setDownloadTask(task);

            // Iniciar la tarea en un hilo
            new Thread(task).start();

            stage.showAndWait();

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private javafx.scene.layout.FlowPane badgesContainer;
    @FXML
    private Label pullCountLabel;
    @FXML
    private Label lastUpdatedLabel;
    @FXML
    private Label commandLabel;
    @FXML
    private Button copyButton;

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

        // Set Command Text
        commandLabel.setText("ollama run " + firstTag.getName());

        // Setup Copy Button
        copyButton.setOnAction(e -> {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(commandLabel.getText());
            clipboard.setContent(content);

            // Visual feedback (optional)
            copyButton.getStyleClass().add("success");
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                    javafx.util.Duration.seconds(1));
            pause.setOnFinished(ev -> copyButton.getStyleClass().remove("success"));
            pause.play();
        });

        // Populate Metrics
        pullCountLabel.setText(firstTag.pullCountProperty().get());
        lastUpdatedLabel.setText(firstTag.lastUpdatedProperty().get());

        // Extract Parameter Size from Tag (e.g., "llama3:8b" -> "8B")
        // This is a heuristic.
        String tag = firstTag.getTag();
        if (tag.contains(":")) {
            String[] parts = tag.split(":");
            if (parts.length > 1) {
                parameterSizeLabel.setText(parts[1].toUpperCase());
            } else {
                parameterSizeLabel.setText("-");
            }
        } else {
            parameterSizeLabel.setText("-");
        }

        // Populate Badges
        badgesContainer.getChildren().clear();
        for (String badge : firstTag.getBadges()) {
            Label badgeLabel = new Label(badge);
            badgeLabel.getStyleClass().add("badge-chip");

            String lowerBadge = badge.toLowerCase();
            if (lowerBadge.contains("tool") || lowerBadge.contains("think") || lowerBadge.contains("vision")) {
                badgeLabel.getStyleClass().add("badge-purple");
            } else if (lowerBadge.contains("cloud") || lowerBadge.matches(".*\\d+b.*")) {
                badgeLabel.getStyleClass().add("badge-blue");
            } else if (lowerBadge.contains("code") || lowerBadge.contains("math")) {
                badgeLabel.getStyleClass().add("badge-green");
            } else {
                badgeLabel.getStyleClass().add("badge-blue"); // Default
            }

            badgesContainer.getChildren().add(badgeLabel);
        }

        tagsTable.setItems(javafx.collections.FXCollections.observableArrayList(modelTags));
    }

}