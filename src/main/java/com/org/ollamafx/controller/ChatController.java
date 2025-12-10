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
import javafx.collections.FXCollections;
import java.util.stream.Collectors;

public class ChatController {

    @FXML
    private javafx.scene.control.ComboBox<String> modelSelector;
    @FXML
    private Label statusLabel;
    @FXML
    private javafx.scene.layout.HBox headerBar; // New Header Bar reference
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private VBox messagesContainer;
    @FXML
    private TextArea inputField;
    @FXML
    private Button sendButton;

    // Adaptive UI Elements
    @FXML
    private javafx.scene.layout.VBox welcomeContainer;
    @FXML
    private javafx.scene.layout.VBox bottomInputContainer;
    @FXML
    private javafx.scene.layout.VBox inputCapsule;
    @FXML
    private javafx.scene.control.Label welcomeLabel;

    // Welcome Flow Elements
    @FXML
    private javafx.scene.layout.VBox setupContainer;
    @FXML
    private javafx.scene.control.ComboBox<String> initialModelSelector;

    @FXML
    private javafx.scene.control.Button settingsButton;
    @FXML
    private javafx.scene.layout.VBox settingsPane;
    // @FXML private TextArea systemPromptField; // Removed
    @FXML
    private javafx.scene.control.Slider tempSlider;
    @FXML
    private Label tempValueLabel;

    // private com.org.ollamafx.manager.ModelManager modelManager; // Removed unused
    // field

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
        // No converter needed if ComboBox holds String directly
        // modelSelector.setConverter(new javafx.util.StringConverter<>() {
        // @Override
        // public String toString(com.org.ollamafx.model.OllamaModel object) {
        // return object == null ? "" : object.getName() + ":" + object.getTag();
        // }

        // @Override
        // public com.org.ollamafx.model.OllamaModel fromString(String string) {
        // return null; // Not needed for read-only combo
        // }
        // });

        // Save model selection when changed
        modelSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (currentSession != null && newVal != null) {
                String modelName = newVal; // ComboBox now holds String directly
                currentSession.setModelName(modelName);
                com.org.ollamafx.manager.ChatManager.getInstance().saveChats();
            }
        });

        // Parameters listeners
        tempSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double val = Math.round(newVal.doubleValue() * 10.0) / 10.0;

            // Map value to Creativity Label
            String creativityText;
            if (val <= 0.3) {
                creativityText = com.org.ollamafx.App.getBundle().getString("chat.creativity.precise");
            } else if (val <= 0.7) {
                creativityText = com.org.ollamafx.App.getBundle().getString("chat.creativity.balanced");
            } else {
                creativityText = com.org.ollamafx.App.getBundle().getString("chat.creativity.imaginative");
            }

            tempValueLabel.setText(creativityText);
            // We assume there might be a secondary label for the number, but if not we just
            // show text
            // If tempNumericLabel existed we would update it too. For now let's just stick
            // to the text mapping requested.

            if (currentSession != null) {
                currentSession.setTemperature(val);
                com.org.ollamafx.manager.ChatManager.getInstance().saveChats();
            }
        });

        // System Prompt listener removed
        updateUIState(true); // Initial state is welcome screen
    }

    // Helper method for Adaptive UI
    private void updateUIState(boolean isNewChat) {
        if (isNewChat) {
            // WELCOME STATE
            // Header is gone permanently

            if (scrollPane != null)
                scrollPane.setVisible(false);
            if (welcomeContainer != null)
                welcomeContainer.setVisible(true);

            // Reset for Welcome Flow
            if (setupContainer != null) {
                setupContainer.setVisible(true);
                setupContainer.setManaged(true);
                animateWelcomeText(); // Trigger Typewriter
            }

            // Move Input Capsule to Welcome Container BUT HIDE IT initially
            if (welcomeContainer != null && inputCapsule != null
                    && !welcomeContainer.getChildren().contains(inputCapsule)) {
                if (bottomInputContainer != null)
                    bottomInputContainer.getChildren().remove(inputCapsule);
                welcomeContainer.getChildren().add(inputCapsule); // Add to end
            }

            if (inputCapsule != null) {
                inputCapsule.setVisible(false); // Hidden until model selected
                inputCapsule.setManaged(false); // Don't take space
            }
        } else {
            // ACTIVE CHAT STATE
            // Header is gone permanently

            if (scrollPane != null)
                scrollPane.setVisible(true);
            if (welcomeContainer != null)
                welcomeContainer.setVisible(false);

            // Move Input Capsule to Bottom Container
            if (bottomInputContainer != null && inputCapsule != null
                    && !bottomInputContainer.getChildren().contains(inputCapsule)) {
                if (welcomeContainer != null)
                    welcomeContainer.getChildren().remove(inputCapsule);
                bottomInputContainer.getChildren().add(inputCapsule);
            }

            // Ensure visible in Active Chat
            if (inputCapsule != null) {
                inputCapsule.setVisible(true);
                inputCapsule.setManaged(true);
            }
        }
    }

    // Suggestion chip methods removed as requested

    @FXML
    private void onInitialModelSelected() {
        String selected = initialModelSelector.getValue();
        if (selected != null) {
            // Sync to main selector
            modelSelector.setValue(selected);

            // Transition: Hide Setup, Show Input
            setupContainer.setVisible(false);
            setupContainer.setManaged(false);

            if (inputCapsule != null) {
                inputCapsule.setVisible(true);
                inputCapsule.setManaged(true);
                // Fade in effect could go here
            }
        }
    }

    private void animateWelcomeText() {
        String fullText = com.org.ollamafx.App.getBundle().getString("chat.welcome");
        welcomeLabel.setText("");

        final Integer[] i = { 0 };
        javafx.animation.Timeline timeline = new javafx.animation.Timeline();
        timeline.setCycleCount(1); // Run once works by adding keyframes

        for (int k = 0; k < fullText.length(); k++) {
            final int index = k;
            javafx.animation.KeyFrame keyFrame = new javafx.animation.KeyFrame(
                    javafx.util.Duration.millis(50 * (k + 1)), // 50ms per char
                    event -> welcomeLabel.setText(fullText.substring(0, index + 1)));
            timeline.getKeyFrames().add(keyFrame);
        }
        timeline.play();
    }

    public void setModelManager(com.org.ollamafx.manager.ModelManager modelManager) {
        // this.modelManager = modelManager; // Unused field
        if (modelManager != null) {
            // Convert OllamaModel list to String list for the ComboBox
            javafx.collections.ObservableList<String> models = modelManager.getLocalModels().stream()
                    .map(model -> model.getName() + ":" + model.getTag())
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));

            modelSelector.setItems(models);
            if (initialModelSelector != null) {
                initialModelSelector.setItems(models);
            }
        }
    }

    private com.org.ollamafx.model.ChatSession currentSession;

    public void setChatSession(com.org.ollamafx.model.ChatSession session) {
        this.currentSession = session;
        messagesContainer.getChildren().clear();

        if (session != null) {
            // Adaptive UI: Specific state based on message count
            boolean newChat = session.getMessages().isEmpty();
            updateUIState(newChat);

            // Restore model selection
            if (session.getModelName() != null && !session.getModelName().isEmpty()) {
                setModelName(session.getModelName());
            }

            // Restore parameters
            double temp = session.getTemperature();
            tempSlider.setValue(temp);

            String creativityText;
            if (temp <= 0.3)
                creativityText = com.org.ollamafx.App.getBundle().getString("chat.creativity.precise");
            else if (temp <= 0.7)
                creativityText = com.org.ollamafx.App.getBundle().getString("chat.creativity.balanced");
            else
                creativityText = com.org.ollamafx.App.getBundle().getString("chat.creativity.imaginative");

            tempValueLabel.setText(creativityText);
            // systemPromptField.setText(session.getSystemPrompt()); // Removed

            for (com.org.ollamafx.model.ChatMessage msg : session.getMessages()) {
                addMessage(msg.getContent(), "user".equals(msg.getRole()));
            }
        }
    }

    @FXML
    private void sendMessage() {
        // Logic to send message to Ollama
        String text = inputField.getText();
        if (text.isEmpty() || currentSession == null)
            return;

        // Transition to Active Chat State immediately on send
        updateUIState(false);

        String modelName = modelSelector.getValue();
        if (modelName == null) {
            addMessage("Error: " + com.org.ollamafx.App.getBundle().getString("chat.selectModel"), false); // Reuse or
                                                                                                           // add
                                                                                                           // specific
                                                                                                           // error
            if (statusLabel != null)
                statusLabel.setText(com.org.ollamafx.App.getBundle().getString("chat.status.ready"));
            return;
        }

        // Add user message
        addMessage(text, true);
        if (currentSession != null) {
            currentSession.addMessage(new com.org.ollamafx.model.ChatMessage("user", text));
            currentSession.setModelName(modelName); // Update
                                                    // model
                                                    // name
            com.org.ollamafx.manager.ChatManager.getInstance().saveChats(); // Save state
        }
        inputField.clear();

        // Call Ollama API
        if (statusLabel != null)
            statusLabel.setText(com.org.ollamafx.App.getBundle().getString("chat.status.thinking"));

        com.org.ollamafx.App.getExecutorService().submit(() -> {
            try {
                // Create assistant message placeholder
                Platform.runLater(() -> {
                    addMessage("", false);
                    if (statusLabel != null)
                        statusLabel.setText(com.org.ollamafx.App.getBundle().getString("chat.status.generating"));
                });

                StringBuilder responseBuilder = new StringBuilder();

                double temperature = tempSlider.getValue();
                String systemPrompt = ""; // System prompt disabled in UI

                com.org.ollamafx.manager.OllamaManager.getInstance().askModelStream(modelName, text, temperature,
                        systemPrompt,
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
                        com.org.ollamafx.manager.ChatManager.getInstance().saveChats(); // Save
                                                                                        // state
                    }
                    if (statusLabel != null)
                        statusLabel.setText(com.org.ollamafx.App.getBundle().getString("chat.status.ready"));
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    addMessage("Error: " + e.getMessage(), false);
                    if (statusLabel != null)
                        statusLabel.setText(com.org.ollamafx.App.getBundle().getString("chat.status.error"));
                });
            }
        });
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
            for (String modelName : modelSelector.getItems()) {
                if (modelName.equals(name) || modelName.startsWith(name + ":")) {
                    modelSelector.getSelectionModel().select(modelName);
                    return;
                }
            }
        }
    }

    @FXML
    private void toggleSettings() {
        boolean isVisible = settingsPane.isVisible();
        settingsPane.setVisible(!isVisible);
        settingsPane.setManaged(!isVisible);
    }
}
