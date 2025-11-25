package com.org.ollamafx.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class ChatController {

    @FXML
    private Label modelNameLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private VBox messagesContainer;
    @FXML
    private TextArea inputField;
    @FXML
    private Button sendButton;

    @FXML
    public void initialize() {
        // Handle Enter key to send message
        inputField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (event.isShiftDown()) {
                    // Allow new line with Shift+Enter
                    return;
                }
                event.consume(); // Prevent new line
                sendMessage();
            }
        });

        // Auto-scroll to bottom
        messagesContainer.heightProperty().addListener((observable, oldValue, newValue) -> {
            scrollPane.setVvalue(1.0);
        });
    }

    @FXML
    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty())
            return;

        // Add user message
        addMessage(text, true);
        inputField.clear();

        // Simulate AI response (Echo)
        statusLabel.setText("Thinking...");
        new Thread(() -> {
            try {
                Thread.sleep(1000); // Simulate network delay
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Platform.runLater(() -> {
                addMessage("Echo: " + text, false);
                statusLabel.setText("Ready");
            });
        }).start();
    }

    private void addMessage(String text, boolean isUser) {
        HBox bubbleContainer = new HBox();
        bubbleContainer.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.getStyleClass().add("chat-bubble");
        bubble.getStyleClass().add(isUser ? "chat-bubble-user" : "chat-bubble-assistant");

        // Ensure the label doesn't grow indefinitely
        bubble.setMaxWidth(Double.MAX_VALUE);

        // Add a container for the label to constrain its max width relative to the
        // scroll pane
        // actually, CSS max-width works better on the label if it's inside a container
        // that allows it.
        // But HBox should be fine.

        bubbleContainer.getChildren().add(bubble);
        messagesContainer.getChildren().add(bubbleContainer);
    }

    public void setModelName(String name) {
        modelNameLabel.setText(name);
    }
}
