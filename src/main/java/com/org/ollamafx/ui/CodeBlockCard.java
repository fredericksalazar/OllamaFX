package com.org.ollamafx.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;

public class CodeBlockCard extends VBox {

    private final TextArea codeArea;
    private final Label languageLabel;

    public CodeBlockCard(String code, String language) {
        this.getStyleClass().add("code-block-card");
        this.setMaxWidth(Double.MAX_VALUE);

        // --- Header ---
        HBox header = new HBox();
        header.getStyleClass().add("code-block-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(8, 12, 8, 12));

        this.languageLabel = new Label(language != null && !language.isEmpty() ? language.toUpperCase() : "CODE");
        this.languageLabel.getStyleClass().add("code-block-language");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button copyButton = new Button();
        copyButton.getStyleClass().add("code-block-copy-btn");

        SVGPath icon = new SVGPath();
        icon.setContent(
                "M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z");
        icon.setStyle("-fx-fill: -color-fg-muted; -fx-scale-x: 0.8; -fx-scale-y: 0.8;");
        copyButton.setGraphic(icon);

        copyButton.setOnAction(e -> copyToClipboard(code));

        header.getChildren().addAll(languageLabel, spacer, copyButton);

        // --- Code Area ---
        this.codeArea = new TextArea(code);
        this.codeArea.setEditable(false);
        this.codeArea.setWrapText(false);
        this.codeArea.getStyleClass().add("code-block-content");

        // Auto-height estimate
        int rows = code.split("\n").length;
        this.codeArea.setPrefHeight(Math.max(60, Math.min(rows * 20 + 20, 500)));
        this.codeArea.setMinHeight(60);

        VBox.setVgrow(codeArea, Priority.ALWAYS);

        this.getChildren().addAll(header, codeArea);
    }

    public void updateCode(String newCode) {
        if (!codeArea.getText().equals(newCode)) {
            codeArea.setText(newCode);
            // Re-estimate height
            int rows = newCode.split("\n").length;
            this.codeArea.setPrefHeight(Math.max(60, Math.min(rows * 20 + 20, 500)));
        }
    }

    private void copyToClipboard(String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
    }
}
