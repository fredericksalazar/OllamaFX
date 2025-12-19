package com.org.ollamafx.controller;

import com.org.ollamafx.manager.ModelManager;
import com.org.ollamafx.model.OllamaModel;
import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import com.org.ollamafx.App;
import java.text.MessageFormat;

import java.net.URL;
import java.util.ResourceBundle;

public class LocalModelsController implements Initializable {

    @FXML
    private VBox modelListContainer;

    // We might want to keep a reference to title or other header elements if
    // needed,
    // but the FXML update will handle the structural containers.

    private ModelManager modelManager;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Init logic if needed, currently empty as we wait for modelManager
    }

    public void setModelManager(ModelManager modelManager) {
        this.modelManager = modelManager;

        // Populate initial list using a safe copy to avoid thread issues
        populateLocalModels();

        // Listen for changes
        this.modelManager.getLocalModels().addListener((ListChangeListener<OllamaModel>) c -> {
            Platform.runLater(this::populateLocalModels);
        });
    }

    private void populateLocalModels() {
        if (modelListContainer == null)
            return;
        modelListContainer.getChildren().clear();
        modelListContainer.setSpacing(12);

        if (modelManager == null || modelManager.getLocalModels().isEmpty()) {
            Label placeholder = new Label(App.getBundle().getString("local.empty"));
            placeholder.setStyle("-fx-text-fill: -color-fg-subtle; -fx-font-size: 14px;");
            modelListContainer.getChildren().add(placeholder);
            return;
        }

        for (OllamaModel model : modelManager.getLocalModels()) {
            HBox card = new HBox();
            card.getStyleClass().add("apple-card-row");
            card.setAlignment(Pos.CENTER_LEFT);
            card.setSpacing(15);

            // Hide the icon, just use text layout like the screenshot
            // Or keep a small one? The screenshot has NO icon on the left row.
            // Let's remove the icon region to match the screenshot exactly.

            // Info Box
            VBox infoBox = new VBox();
            infoBox.setAlignment(Pos.CENTER_LEFT);
            infoBox.setSpacing(4);

            // Title: "gemma3:latest" or just "latest" vs "gemma3"?
            // In a global list, we need the full identifier.
            Label nameLbl = new Label(model.getName() + ":" + model.getTag());
            nameLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-fg-default;");

            String details = String.format("%s • Modified: %s",
                    model.sizeProperty().get(),
                    model.lastUpdatedProperty().get());

            Label detailsLbl = new Label(details);
            detailsLbl.getStyleClass().add("apple-text-subtle");

            infoBox.getChildren().addAll(nameLbl, detailsLbl);

            // Spacer
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            // Delete Action (Text Button, Apple Destructive Style)
            Button deleteBtn = new Button(App.getBundle().getString("model.action.uninstall"));
            deleteBtn.getStyleClass().add(Styles.DANGER);

            deleteBtn.setOnAction(e -> confirmAndDelete(model));

            card.getChildren().addAll(infoBox, spacer, deleteBtn);
            modelListContainer.getChildren().add(card);
        }
    }

    private void confirmAndDelete(OllamaModel model) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle(App.getBundle().getString("local.uninstall.title"));
        String header = MessageFormat.format(App.getBundle().getString("local.uninstall.header"),
                model.getName() + ":" + model.getTag());
        alert.setHeaderText(header);
        alert.setContentText(App.getBundle().getString("local.uninstall.content"));

        // Apply styling to dialog if we had a Utils method for it, or just rely on
        // default for now.
        // Ideally use Utils.showConfirmation(...) if it existed, but we'll stick to
        // standard for speed unless requested.

        alert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                modelManager.deleteModel(model.getName(), model.getTag());
            }
        });
    }

    // Helper not strictly needed if we use the model's formatted size,
    // but kept just in case we need parsing later. The model.sizeProperty() usually
    // sends formatted string.
}