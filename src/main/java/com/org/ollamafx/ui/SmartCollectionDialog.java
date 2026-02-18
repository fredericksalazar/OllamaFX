package com.org.ollamafx.ui;

import com.org.ollamafx.model.SmartCollection;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;
import java.util.Optional;

public class SmartCollectionDialog extends Dialog<SmartCollection> {

    public SmartCollectionDialog(SmartCollection existing) {
        setTitle(existing == null ? "New Smart Collection" : "Edit Smart Collection");
        setHeaderText("Define your smart collection criteria.");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("Collection Name");
        if (existing != null)
            nameField.setText(existing.getName());

        ComboBox<SmartCollection.Criteria> criteriaBox = new ComboBox<>();
        criteriaBox.getItems().setAll(SmartCollection.Criteria.values());
        criteriaBox.setValue(existing != null ? existing.getCriteria() : SmartCollection.Criteria.KEYWORD);

        TextField valueField = new TextField();
        valueField.setPromptText("Value (e.g. 'project', '7')");
        if (existing != null)
            valueField.setText(existing.getValue());

        ComboBox<String> iconBox = new ComboBox<>();
        iconBox.getItems().addAll("activity", "clock", "tag", "cpu", "star");
        iconBox.setValue(existing != null && existing.getIcon() != null ? existing.getIcon() : "activity");
        // Simple string rendering for now

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Criteria:"), 0, 1);
        grid.add(criteriaBox, 1, 1);
        grid.add(new Label("Value:"), 0, 2);
        grid.add(valueField, 1, 2);
        grid.add(new Label("Icon:"), 0, 3);
        grid.add(iconBox, 1, 3);

        // Value hint logic
        Label hintLabel = new Label("Filter by chat name substring.");
        grid.add(hintLabel, 1, 4);

        criteriaBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == SmartCollection.Criteria.DATE) {
                valueField.setPromptText("Days (e.g. 7)");
                hintLabel.setText("Chats from the last N days.");
                iconBox.setValue("clock");
            } else if (newVal == SmartCollection.Criteria.MODEL) {
                valueField.setPromptText("Model Name (e.g. llama3)");
                hintLabel.setText("Chats using this model.");
                iconBox.setValue("cpu");
            } else {
                valueField.setPromptText("Keyword");
                hintLabel.setText("Filter by chat name substring.");
                iconBox.setValue("tag");
            }
        });

        getDialogPane().setContent(grid);

        Platform.runLater(nameField::requestFocus);

        setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new SmartCollection(
                        nameField.getText(),
                        criteriaBox.getValue(),
                        valueField.getText(),
                        iconBox.getValue());
            }
            return null;
        });
    }

    public static Optional<SmartCollection> show(SmartCollection existing) {
        SmartCollectionDialog dialog = new SmartCollectionDialog(existing);
        return dialog.showAndWait();
    }
}
