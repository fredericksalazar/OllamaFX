package com.org.ollamafx.controller;

import com.org.ollamafx.manager.ModelManager;
import com.org.ollamafx.model.OllamaModel;
import javafx.fxml.FXML;
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
    }

    @FXML
    public void initialize() {
        tagNameColumn.setCellValueFactory(cellData -> cellData.getValue().tagProperty());
        tagSizeColumn.setCellValueFactory(cellData -> cellData.getValue().sizeProperty());
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
                        System.out.println("Uninstalling: " + model.getName() + ":" + model.getTag());
                        btn.setText("Uninstalling...");
                        btn.setDisable(true);
                        // Lógica de desinstalación aquí
                    } else {
                        System.out.println("Installing: " + model.getName() + ":" + model.getTag());
                        btn.setText("Installing...");
                        btn.setDisable(true);
                        // Lógica de instalación aquí
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
}