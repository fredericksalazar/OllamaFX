package com.org.ollamafx.ui;

import atlantafx.base.theme.CupertinoLight;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.Optional;

/**
 * Reemplaza TextInputDialog con un diálogo minimalista alineado
 * con el lenguaje de diseño de OllamaFX / AtlantaFX CupertinoLight.
 */
public class FxDialog {

    /**
     * Muestra un diálogo de entrada de texto modal.
     *
     * @param owner        Ventana padre (para centrar el diálogo)
     * @param title        Título del diálogo (mostrado como label superior)
     * @param placeholder  Texto placeholder del campo de texto
     * @param initialValue Valor inicial del campo (puede ser vacío)
     * @param confirmLabel Texto del botón de confirmación (ej. "Crear",
     *                     "Renombrar")
     * @return Optional con el texto ingresado, o empty si se canceló
     */
    public static Optional<String> showInputDialog(
            Window owner,
            String title,
            String placeholder,
            String initialValue,
            String confirmLabel) {

        // --- Stage ---
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);
        dialog.initStyle(StageStyle.UNDECORATED); // Sin barra de título nativa
        dialog.setResizable(false);

        // --- Resultado ---
        final String[] result = { null };

        // --- UI ---
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dialog-title");

        TextField inputField = new TextField(initialValue != null ? initialValue : "");
        inputField.setPromptText(placeholder);
        inputField.getStyleClass().add("dialog-input");
        inputField.setPrefWidth(260);
        // Seleccionar todo el texto inicial para facilitar reemplazo
        if (initialValue != null && !initialValue.isEmpty()) {
            inputField.selectAll();
        }

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

        // --- Acciones ---
        confirmBtn.setOnAction(e -> {
            String text = inputField.getText().trim();
            if (!text.isEmpty()) {
                result[0] = text;
                dialog.close();
            }
        });

        cancelBtn.setOnAction(e -> dialog.close());

        // Enter confirma, Escape cancela
        content.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                dialog.close();
            }
        });

        // --- Scene ---
        Scene scene = new Scene(content);
        // Heredar el tema de AtlantaFX
        Application.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());
        // Aplicar nuestro CSS personalizado
        scene.getStylesheets().add(
                FxDialog.class.getResource("/css/ollama_active.css").toExternalForm());

        dialog.setScene(scene);

        // Foco automático en el campo de texto
        dialog.setOnShown(e -> {
            inputField.requestFocus();
            if (initialValue != null && !initialValue.isEmpty()) {
                inputField.selectAll();
            }
        });

        dialog.showAndWait();

        return Optional.ofNullable(result[0]);
    }
}
