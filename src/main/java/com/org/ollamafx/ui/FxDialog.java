package com.org.ollamafx.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.Optional;

/**
 * Reemplaza TextInputDialog y Alert con diálogos minimalistas alineados
 * con el lenguaje de diseño de OllamaFX / AtlantaFX CupertinoLight.
 */
public class FxDialog {

    /**
     * Muestra un diálogo de entrada de texto modal.
     */
    public static Optional<String> showInputDialog(
            Window owner,
            String title,
            String placeholder,
            String initialValue,
            String confirmLabel) {

        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setResizable(false);

        final String[] result = { null };

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dialog-title");

        TextField inputField = new TextField(initialValue != null ? initialValue : "");
        inputField.setPromptText(placeholder);
        inputField.getStyleClass().add("dialog-input");
        inputField.setPrefWidth(260);

        Button confirmBtn = new Button(confirmLabel);
        confirmBtn.getStyleClass().addAll("button", "accent");
        confirmBtn.setDefaultButton(true);
        confirmBtn.setPrefWidth(90);

        Button cancelBtn = new Button("Cancelar");
        cancelBtn.getStyleClass().add("button");
        cancelBtn.setCancelButton(true);
        cancelBtn.setPrefWidth(90);

        HBox buttons = new HBox(8, cancelBtn, confirmBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox content = new VBox(12, titleLabel, inputField, buttons);
        content.setPadding(new Insets(20, 24, 20, 24));
        content.getStyleClass().add("fx-dialog-pane");
        content.setPrefWidth(320);

        confirmBtn.setOnAction(e -> {
            String text = inputField.getText().trim();
            if (!text.isEmpty()) {
                result[0] = text;
                dialog.close();
            }
        });

        cancelBtn.setOnAction(e -> dialog.close());

        content.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE)
                dialog.close();
        });

        Scene scene = new Scene(content);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialog.initStyle(StageStyle.TRANSPARENT);

        String currentTheme = com.org.ollamafx.manager.ConfigManager.getInstance().getTheme();
        if ("dark".equalsIgnoreCase(currentTheme)) {
            content.getStyleClass().add("dark");
        } else {
            content.getStyleClass().add("light");
        }
        scene.getStylesheets().add(
                FxDialog.class.getResource("/css/ollama_active.css").toExternalForm());

        dialog.setScene(scene);
        dialog.setOnShown(e -> {
            inputField.requestFocus();
            if (initialValue != null && !initialValue.isEmpty()) {
                inputField.selectAll();
            }
        });

        dialog.showAndWait();
        return Optional.ofNullable(result[0]);
    }

    /**
     * Muestra un diálogo de confirmación modal (sin campo de texto).
     *
     * @param owner        Ventana padre
     * @param title        Título principal del diálogo
     * @param body         Mensaje descriptivo (puede ser null)
     * @param confirmLabel Texto del botón de confirmación (ej. "Eliminar")
     * @param cancelLabel  Texto del botón de cancelación (ej. "Cancelar")
     * @return true si el usuario confirmó, false si canceló
     */
    public static boolean showConfirmDialog(
            Window owner,
            String title,
            String body,
            String confirmLabel,
            String cancelLabel) {

        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setResizable(false);

        final boolean[] confirmed = { false };

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dialog-title");

        VBox content = new VBox(12);
        content.setPadding(new Insets(20, 24, 20, 24));
        content.getStyleClass().add("fx-dialog-pane");
        content.setPrefWidth(320);
        content.getChildren().add(titleLabel);

        if (body != null && !body.isBlank()) {
            Label bodyLabel = new Label(body);
            bodyLabel.getStyleClass().add("dialog-body");
            bodyLabel.setWrapText(true);
            bodyLabel.setMaxWidth(272);
            content.getChildren().add(bodyLabel);
        }

        Button confirmBtn = new Button(confirmLabel);
        confirmBtn.getStyleClass().addAll("button", "danger");
        confirmBtn.setDefaultButton(true);
        confirmBtn.setPrefWidth(90);

        Button cancelBtn = new Button(cancelLabel);
        cancelBtn.getStyleClass().add("button");
        cancelBtn.setCancelButton(true);
        cancelBtn.setPrefWidth(90);

        HBox buttons = new HBox(8, cancelBtn, confirmBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        content.getChildren().add(buttons);

        confirmBtn.setOnAction(e -> {
            confirmed[0] = true;
            dialog.close();
        });
        cancelBtn.setOnAction(e -> dialog.close());

        content.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE)
                dialog.close();
        });

        Scene scene = new Scene(content);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialog.initStyle(StageStyle.TRANSPARENT);

        String currentTheme = com.org.ollamafx.manager.ConfigManager.getInstance().getTheme();
        if ("dark".equalsIgnoreCase(currentTheme)) {
            content.getStyleClass().add("dark");
        } else {
            content.getStyleClass().add("light");
        }
        scene.getStylesheets().add(
                FxDialog.class.getResource("/css/ollama_active.css").toExternalForm());

        dialog.setScene(scene);
        // Foco en Cancelar por seguridad (acción destructiva)
        dialog.setOnShown(e -> cancelBtn.requestFocus());
        dialog.showAndWait();

        return confirmed[0];
    }
}
