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
    private javafx.scene.control.ComboBox<com.org.ollamafx.model.OllamaModel> modelSelector;
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

    private com.org.ollamafx.manager.ModelManager modelManager;

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

        // Configure ComboBox to display model names nicely
        modelSelector.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(com.org.ollamafx.model.OllamaModel object) {
                return object == null ? "" : object.getName() + ":" + object.getTag();
            }

            @Override
            public com.org.ollamafx.model.OllamaModel fromString(String string) {
                return null; // Not needed for read-only combo
            }
        });

        // Save model selection when changed
        modelSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (currentSession != null && newVal != null) {
                String modelName = newVal.getName() + ":" + newVal.getTag();
                currentSession.setModelName(modelName);
                com.org.ollamafx.manager.ChatManager.getInstance().saveChats();
            }
        });
    }

    public void setModelManager(com.org.ollamafx.manager.ModelManager modelManager) {
        this.modelManager = modelManager;
        if (modelManager != null) {
            modelSelector.setItems(modelManager.getLocalModels());
        }
    }

    private com.org.ollamafx.model.ChatSession currentSession;

    public void setChatSession(com.org.ollamafx.model.ChatSession session) {
        this.currentSession = session;
        messagesContainer.getChildren().clear();
        if (session != null) {
            // Restore model selection
            if (session.getModelName() != null && !session.getModelName().isEmpty()) {
                setModelName(session.getModelName());
            }

            for (com.org.ollamafx.model.ChatMessage msg : session.getMessages()) {
                addMessage(msg.getContent(), "user".equals(msg.getRole()));
            }
        }
    }

    @FXML
    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty())
            return;

        com.org.ollamafx.model.OllamaModel selectedModel = modelSelector.getValue();
        if (selectedModel == null) {
            addMessage("Error: No model selected. Please select a model from the dropdown.", false);
            statusLabel.setText("Ready");
            return;
        }

        String modelName = selectedModel.getName() + ":" + selectedModel.getTag();

        // Add user message
        addMessage(text, true);
        if (currentSession != null) {
            currentSession.addMessage(new com.org.ollamafx.model.ChatMessage("user", text));
            currentSession.setModelName(modelName); // Update model name
            com.org.ollamafx.manager.ChatManager.getInstance().saveChats(); // Save state
        }
        inputField.clear();

        // Call Ollama API
        statusLabel.setText("Thinking...");

        new Thread(() -> {
            try {
                // Create assistant message placeholder
                Platform.runLater(() -> {
                    addMessage("", false);
                    statusLabel.setText("Generating...");
                });

                StringBuilder responseBuilder = new StringBuilder();

                com.org.ollamafx.manager.OllamaManager.getInstance().askModelStream(modelName, text,
                        new io.github.ollama4j.models.generate.OllamaStreamHandler() {
                            @Override
                            public void accept(String messagePart) {
                                // System.out.println("Stream chunk: " + messagePart); // Debug
                                // ollama4j sends the accumulated text, not the delta.
                                // So we replace the builder content instead of appending.
                                responseBuilder.setLength(0);
                                responseBuilder.append(messagePart);

                                Platform.runLater(() -> {
                                    // Update the last message (assistant's bubble)
                                    if (!messagesContainer.getChildren().isEmpty()) {
                                        HBox lastContainer = (HBox) messagesContainer.getChildren()
                                                .get(messagesContainer.getChildren().size() - 1);
                                        Label bubble = (Label) lastContainer.getChildren().get(0);
                                        bubble.setText(messagePart);
                                    }
                                });
                            }
                        });

                // On complete (OllamaStreamHandler doesn't have onComplete, so we assume it
                // finishes when askModelStream returns?
                // Actually askModelStream might be blocking or async.
                // If blocking, we are good here.

                Platform.runLater(() -> {
                    String fullResponse = responseBuilder.toString();
                    if (currentSession != null) {
                        currentSession.addMessage(new com.org.ollamafx.model.ChatMessage("assistant", fullResponse));
                        com.org.ollamafx.manager.ChatManager.getInstance().saveChats(); // Save state
                    }
                    statusLabel.setText("Ready");
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    addMessage("Error: " + e.getMessage(), false);
                    statusLabel.setText("Error");
                });
            }
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

        bubbleContainer.getChildren().add(bubble);
        messagesContainer.getChildren().add(bubbleContainer);
    }

    public void setModelName(String name) {
        // Try to select the model in the combo box if it matches
        if (modelSelector.getItems() != null) {
            for (com.org.ollamafx.model.OllamaModel model : modelSelector.getItems()) {
                String fullName = model.getName() + ":" + model.getTag();
                if (fullName.equals(name) || model.getName().equals(name)) {
                    modelSelector.getSelectionModel().select(model);
                    return;
                }
            }
        }
    }
}
