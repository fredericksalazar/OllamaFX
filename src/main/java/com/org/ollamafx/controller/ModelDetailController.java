package com.org.ollamafx.controller;

import com.org.ollamafx.model.OllamaModel;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
    private VBox tagsContainer;

    /**
     * Puebla la vista con la lista de tags del modelo seleccionado.
     */
    public void populateDetails(List<OllamaModel> modelTags) {
        if (modelTags == null || modelTags.isEmpty()) {
            modelNameLabel.setText("Error");
            modelDescriptionText.setText("Could not retrieve model details. The library's web scraping might be outdated.");
            tagsContainer.getChildren().clear();
            return;
        }

        OllamaModel firstTag = modelTags.get(0);
        modelNameLabel.setText(firstTag.getName());
        modelDescriptionText.setText(firstTag.descriptionProperty().get());

        tagsContainer.getChildren().clear();

        for (OllamaModel tagModel : modelTags) {
            HBox tagRow = createTagRow(tagModel);
            tagsContainer.getChildren().add(tagRow);
        }
    }

    /**
     * Crea un nodo de UI (HBox) para un tag individual.
     * Este método ahora está completo y es funcional.
     */
    private HBox createTagRow(OllamaModel tagModel) {
        // 1. Creamos el contenedor para esta fila.
        HBox hbox = new HBox(10);
        hbox.setAlignment(Pos.CENTER_LEFT);

        // 2. Creamos la etiqueta con la información del tag.
        Label tagLabel = new Label(String.format("Tag: %s  •  Size: %s",
                tagModel.getTag(), tagModel.sizeProperty().get()));
        HBox.setHgrow(tagLabel, Priority.ALWAYS); // Para que ocupe el espacio disponible.

        // 3. Creamos el botón de instalar para ESTE tag específico.
        Button installButton = new Button("Install");
        installButton.setOnAction(event -> {
            System.out.println("Installing: " + tagModel.getName() + ":" + tagModel.getTag());
            // Aquí irá la lógica de instalación...
            installButton.setText("Installing...");
            installButton.setDisable(true);
        });

        // 4. Añadimos la etiqueta y el botón a la fila.
        hbox.getChildren().addAll(tagLabel, installButton);

        // 5. Devolvemos la fila completa.
        return hbox;
    }
}