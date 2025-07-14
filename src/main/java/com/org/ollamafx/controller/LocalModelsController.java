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

    // Este método se llama DESPUÉS de initialize. Es el lugar perfecto para vincular los datos.
    public void setModelManager(ModelManager modelManager) {
        this.modelManager = modelManager;

        // 3. ¡La vinculación! La tabla ahora "observa" la lista del gestor.
        localModelsTable.setItems(this.modelManager.getLocalModels());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Configuramos las columnas para que coincidan con las propiedades de OllamaModel.
        modelNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        modelSizeColumn.setCellValueFactory(new PropertyValueFactory<>("size"));
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
        // Aquí irá tu futura lógica para borrar un modelo.
    }
}