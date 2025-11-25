package com.org.ollamafx.controller;

import com.org.ollamafx.manager.ModelManager;
import com.org.ollamafx.model.OllamaModel;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import java.net.URL;
import java.util.ResourceBundle;

public class LocalModelsController implements Initializable {

    @FXML
    private TableView<OllamaModel> localModelsTable;
    @FXML
    private TableColumn<OllamaModel, String> modelNameColumn;
    @FXML
    private TableColumn<OllamaModel, String> modelSizeColumn;
    @FXML
    private TableColumn<OllamaModel, String> modelLastModifiedColumn;
    @FXML
    private TableColumn<OllamaModel, String> modelDigestColumn; // Asumiremos que esta columna ahora mostrará el 'tag'.
    @FXML
    private Button deleteButton;

    private ModelManager modelManager;

    // Este método se llama DESPUÉS de initialize. Es el lugar perfecto para
    // vincular los datos.
    public void setModelManager(ModelManager modelManager) {
        this.modelManager = modelManager;

        // 3. ¡La vinculación! La tabla ahora "observa" la lista del gestor.
        localModelsTable.setItems(this.modelManager.getLocalModels());

        // Debug: Listen for changes in the table's items
        localModelsTable.getItems()
                .addListener((javafx.collections.ListChangeListener.Change<? extends OllamaModel> c) -> {
                    while (c.next()) {
                        if (c.wasAdded()) {
                            System.out.println("LocalModelsController: Items added to table: " + c.getAddedSize());
                        }
                    }
                });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Configuramos las columnas para que coincidan con las propiedades de
        // OllamaModel.
        modelNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        modelSizeColumn.setCellValueFactory(new PropertyValueFactory<>("size"));
        modelSizeColumn.setComparator((size1, size2) -> {
            return parseSize(size1).compareTo(parseSize(size2));
        });
        modelLastModifiedColumn.setCellValueFactory(new PropertyValueFactory<>("lastUpdated"));
        modelDigestColumn.setCellValueFactory(new PropertyValueFactory<>("tag")); // Apuntamos al 'tag'.

        // 2. La lógica de la UI (como deshabilitar botones) se queda aquí.
        deleteButton.setDisable(true);
        localModelsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            deleteButton.setDisable(newSelection == null);
        });
    }

    @FXML
    private void deleteSelectedModel() {
        OllamaModel selectedModel = localModelsTable.getSelectionModel().getSelectedItem();
        if (selectedModel != null) {
            System.out.println("Delete button clicked for: " + selectedModel.getName() + ":" + selectedModel.getTag());

            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete Model");
            alert.setHeaderText("Are you sure you want to delete this model?");
            alert.setContentText("Model: " + selectedModel.getName() + ":" + selectedModel.getTag());

            java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
                System.out.println("User confirmed deletion.");
                if (modelManager != null) {
                    modelManager.deleteModel(selectedModel.getName(), selectedModel.getTag());
                }
            } else {
                System.out.println("Deletion cancelled by user.");
            }
        }
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