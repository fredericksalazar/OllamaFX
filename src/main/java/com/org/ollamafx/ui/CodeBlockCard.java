package com.org.ollamafx.ui;

import com.org.ollamafx.ui.markdown.SyntaxHighlighter;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

public class CodeBlockCard extends VBox {

    private final CodeArea codeArea;
    private final Label languageLabel;
    private final Button copyButton;
    private final SVGPath copyIcon;
    private String currentCode;
    private String currentLanguage;

    public CodeBlockCard(String code, String language) {
        getStyleClass().add("code-block-card");
        setMaxWidth(Double.MAX_VALUE);

        this.currentCode = code == null ? "" : code;
        this.currentLanguage = language == null ? "" : language;

        // --- Header ---
        HBox header = new HBox();
        header.getStyleClass().add("code-block-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(6, 12, 6, 12));

        this.languageLabel = new Label(displayLanguage(currentLanguage));
        this.languageLabel.getStyleClass().add("code-block-language");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        this.copyButton = new Button();
        this.copyButton.getStyleClass().add("code-block-copy-btn");
        this.copyIcon = new SVGPath();
        this.copyIcon.setContent(
                "M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z");
        this.copyIcon.getStyleClass().add("code-block-copy-icon");
        this.copyButton.setGraphic(this.copyIcon);
        this.copyButton.setOnAction(e -> copyToClipboard());

        header.getChildren().addAll(languageLabel, spacer, copyButton);

        // --- Code Area ---
        this.codeArea = new CodeArea();
        this.codeArea.getStyleClass().add("code-block-content");
        this.codeArea.setEditable(false);
        this.codeArea.setWrapText(false);
        this.codeArea.setFocusTraversable(false);
        this.codeArea.replaceText(currentCode);
        applyHighlight();
        adjustHeight();

        VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(this.codeArea);
        scrollPane.getStyleClass().add("code-block-scroll");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(header, scrollPane);
    }

    public void updateCode(String newCode) {
        updateCode(newCode, currentLanguage);
    }

    public void updateCode(String newCode, String newLanguage) {
        String safe = newCode == null ? "" : newCode;
        boolean codeChanged = !safe.equals(currentCode);
        boolean langChanged = newLanguage != null && !newLanguage.equals(currentLanguage);

        if (langChanged) {
            currentLanguage = newLanguage;
            languageLabel.setText(displayLanguage(currentLanguage));
        }
        if (codeChanged) {
            currentCode = safe;
            codeArea.replaceText(safe);
            applyHighlight();
            adjustHeight();
        } else if (langChanged) {
            applyHighlight();
        }
    }

    private void applyHighlight() {
        if (currentCode.isEmpty()) {
            return;
        }
        try {
            codeArea.setStyleSpans(0, SyntaxHighlighter.computeHighlight(currentCode, currentLanguage));
        } catch (Exception ignored) {
            // Highlighter never blocks rendering
        }
    }

    private void adjustHeight() {
        int rows = Math.max(1, currentCode.split("\n", -1).length);
        double height = Math.max(40, Math.min(rows * 18 + 16, 520));
        codeArea.setPrefHeight(height);
        codeArea.setMinHeight(40);
    }

    private static String displayLanguage(String lang) {
        if (lang == null || lang.isBlank()) {
            return "CODE";
        }
        return lang.trim().toUpperCase();
    }

    private void copyToClipboard() {
        ClipboardContent content = new ClipboardContent();
        content.putString(currentCode);
        Clipboard.getSystemClipboard().setContent(content);

        copyIcon.getStyleClass().add("code-block-copy-icon-success");
        Platform.runLater(() -> new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            Platform.runLater(() -> copyIcon.getStyleClass().remove("code-block-copy-icon-success"));
        }, "code-copy-feedback").start());
    }
}
