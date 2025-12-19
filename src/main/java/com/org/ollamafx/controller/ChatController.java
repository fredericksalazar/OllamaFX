package com.org.ollamafx.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;

import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import com.org.ollamafx.App;
import com.org.ollamafx.manager.ChatManager;
import com.org.ollamafx.manager.ModelManager;
import com.org.ollamafx.manager.OllamaManager;
import com.org.ollamafx.model.ChatMessage;
import com.org.ollamafx.model.ChatSession;
import com.org.ollamafx.model.OllamaModel;
import com.org.ollamafx.ui.MarkdownOutput;

import io.github.ollama4j.models.generate.OllamaStreamHandler;

public class ChatController {

    private Future<?> currentGenerationTask;
    private boolean isGenerating = false;
    private final StringBuilder activeResponseBuffer = new StringBuilder();
    private long lastUiUpdate = 0;
    private static final long UI_UPDATE_INTERVAL_MS = 30; // ~30fps for text updates

    @FXML
    private ComboBox<String> modelSelector;
    @FXML
    private Label statusLabel;
    @FXML
    private HBox headerBar;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private VBox messagesContainer;
    @FXML
    private TextArea inputField;
    @FXML
    private Button sendButton;
    @FXML
    private Button cancelButton;

    // Adaptive UI Elements
    @FXML
    private VBox welcomeContainer;
    @FXML
    private VBox bottomInputContainer;
    @FXML
    private VBox inputCapsule;
    @FXML
    private Label welcomeLabel;

    // Welcome Flow Elements
    @FXML
    private VBox setupContainer;
    @FXML
    private ComboBox<String> initialModelSelector;

    @FXML
    private Button settingsButton;
    @FXML
    private VBox settingsPane;

    @FXML
    private Slider tempSlider;
    @FXML
    private Label tempValueLabel;

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

        // Save model selection when changed
        modelSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (currentSession != null && newVal != null) {
                String modelName = newVal;
                currentSession.setModelName(modelName);
                ChatManager.getInstance().saveChats();
            }
        });

        // Parameters listeners
        tempSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double val = Math.round(newVal.doubleValue() * 10.0) / 10.0;

            // Map value to Creativity Label
            String creativityText;
            if (val <= 0.3) {
                creativityText = App.getBundle().getString("chat.creativity.precise");
            } else if (val <= 0.7) {
                creativityText = App.getBundle().getString("chat.creativity.balanced");
            } else {
                creativityText = App.getBundle().getString("chat.creativity.imaginative");
            }

            tempValueLabel.setText(creativityText);

            if (currentSession != null) {
                currentSession.setTemperature(val);
                ChatManager.getInstance().saveChats();
            }
        });

        updateUIState(true); // Initial state is welcome screen
    }

    // Helper method for Adaptive UI
    private void updateUIState(boolean isNewChat) {
        if (isNewChat) {
            // WELCOME STATE
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
            }
        }
    }

    private void animateWelcomeText() {
        String fullText = App.getBundle().getString("chat.welcome");
        welcomeLabel.setText("");

        Timeline timeline = new Timeline();
        timeline.setCycleCount(1); // Run once works by adding keyframes

        for (int k = 0; k < fullText.length(); k++) {
            final int index = k;
            KeyFrame keyFrame = new KeyFrame(
                    Duration.millis(50 * (k + 1)), // 50ms per char
                    event -> welcomeLabel.setText(fullText.substring(0, index + 1)));
            timeline.getKeyFrames().add(keyFrame);
        }
        timeline.play();
    }

    public void setModelManager(ModelManager modelManager) {
        if (modelManager != null) {
            // Initial population
            updateModelList(modelManager.getLocalModels());

            // Listener for future updates
            modelManager.getLocalModels().addListener(
                    (ListChangeListener.Change<? extends OllamaModel> c) -> {
                        updateModelList(modelManager.getLocalModels());
                    });
        }
    }

    private void updateModelList(List<OllamaModel> models) {
        Platform.runLater(() -> {
            ObservableList<String> modelNames = models.stream()
                    .map(model -> model.getName() + ":" + model.getTag())
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));

            String currentSelection = modelSelector.getValue();

            modelSelector.setItems(modelNames);
            if (initialModelSelector != null) {
                initialModelSelector.setItems(modelNames);
            }

            modelListLoaded = true;

            // Priority: Pending Selection (from loading chat) > Current Selection
            // (preserve)
            if (pendingModelSelection != null) {
                setModelName(pendingModelSelection);
                pendingModelSelection = null; // Clear after applying
            } else if (currentSelection != null && modelNames.contains(currentSelection)) {
                // Restore previous selection if still valid and no pending overwrite
                modelSelector.setValue(currentSelection);
                if (initialModelSelector != null)
                    initialModelSelector.setValue(currentSelection);
            }
        });
    }

    private ChatSession currentSession;

    public void setChatSession(ChatSession session) {
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
                creativityText = App.getBundle().getString("chat.creativity.precise");
            else if (temp <= 0.7)
                creativityText = App.getBundle().getString("chat.creativity.balanced");
            else
                creativityText = App.getBundle().getString("chat.creativity.imaginative");

            tempValueLabel.setText(creativityText);

            for (ChatMessage msg : session.getMessages()) {
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

        if (isGenerating) {
            cancelGeneration();
            return;
        }

        String modelName = modelSelector.getValue();
        if (modelName == null) {
            addMessage("Error: " + App.getBundle().getString("chat.selectModel"), false);
            if (statusLabel != null)
                statusLabel.setText(App.getBundle().getString("chat.status.ready"));
            return;
        }

        // Add user message
        addMessage(text, true);
        // Capture session and model to ensure thread safety across tab switches
        final ChatSession targetSession = currentSession;
        final String targetModel = modelName;

        // Add user message to session synchronously
        if (targetSession != null) {
            targetSession.addMessage(new ChatMessage("user", text));
            targetSession.setModelName(targetModel);
            ChatManager.getInstance().saveChats();
        }

        inputField.clear();
        setGeneratingState(true);

        if (statusLabel != null)
            statusLabel.setText(App.getBundle().getString("chat.status.thinking"));

        activeResponseBuffer.setLength(0);

        // Add Assistant Placeholder message to SESSION Synchronously
        ChatMessage assistantMsg = new ChatMessage("assistant", "");
        if (targetSession != null) {
            targetSession.addMessage(assistantMsg);
            ChatManager.getInstance().saveChats(); // Persist placeholder
        }

        // Add Assistant Placeholder to UI Synchronously
        addMessage("", false);
        if (statusLabel != null)
            statusLabel.setText(App.getBundle().getString("chat.status.generating"));

        currentGenerationTask = App.getExecutorService().submit(() -> {
            try {
                StringBuilder responseBuilder = new StringBuilder();
                double temperature = tempSlider.getValue();
                String systemPrompt = "";

                OllamaManager.getInstance().askModelStream(targetModel, text, temperature,
                        systemPrompt,
                        new OllamaStreamHandler() {
                            @Override
                            public void accept(String messagePart) {
                                if (Thread.currentThread().isInterrupted()) {
                                    throw new RuntimeException("Cancelled by user");
                                }

                                // FIX: Ollama4j gives full text accumulator
                                responseBuilder.setLength(0);
                                responseBuilder.append(messagePart);
                                String properFullText = responseBuilder.toString();

                                // Update Session Data (Always safe to do, even if in background)
                                assistantMsg.setContent(properFullText);

                                long now = System.currentTimeMillis();
                                if (now - lastUiUpdate > UI_UPDATE_INTERVAL_MS) {
                                    Platform.runLater(() -> {
                                        // Guard: Only update UI if we are still looking at this session
                                        if (currentSession == targetSession) {
                                            updateLastMessage(properFullText);
                                        }
                                    });
                                    lastUiUpdate = now;
                                }
                            }
                        });

                Platform.runLater(() -> {
                    String fullResponse = responseBuilder.toString();

                    // Final UI Update (Guarded)
                    if (currentSession == targetSession) {
                        updateLastMessage(fullResponse);
                    }

                    // Final Data Update (Always)
                    assistantMsg.setContent(fullResponse);

                    if (targetSession != null) {
                        ChatManager.getInstance().saveChats();
                    }
                    setGeneratingState(false);
                });

            } catch (Exception e) {
                if (e instanceof InterruptedException || Thread.interrupted()
                        || "Cancelled by user".equals(e.getMessage())) {
                    Platform.runLater(() -> setGeneratingState(false));
                    return;
                }
                e.printStackTrace();
                Platform.runLater(() -> {
                    // Only show error if active
                    if (currentSession == targetSession) {
                        addMessage("Error: " + e.getMessage(), false);
                    }
                    setGeneratingState(false);
                });
            }
        });
    }

    private void updateLastMessage(String text) {
        if (!messagesContainer.getChildren().isEmpty()) {
            Node lastNode = messagesContainer.getChildren().get(messagesContainer.getChildren().size() - 1);

            if (lastNode instanceof HBox) {
                HBox container = (HBox) lastNode;
                if (!container.getChildren().isEmpty()) {
                    Node content = container.getChildren().get(0);

                    // Case 1: Direct MarkdownOutput
                    if (content instanceof MarkdownOutput) {
                        ((MarkdownOutput) content).updateContent(text);
                    }
                    // Case 2: Wrapped in VBox (New structure with Copy Button)
                    else if (content instanceof VBox) {
                        VBox wrapper = (VBox) content;
                        for (Node child : wrapper.getChildren()) {
                            if (child instanceof MarkdownOutput) {
                                ((MarkdownOutput) child).updateContent(text);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private void setGeneratingState(boolean generating) {
        this.isGenerating = generating;
        if (generating) {
            sendButton.setVisible(false);
            sendButton.setManaged(false);

            cancelButton.setVisible(true);
            cancelButton.setManaged(true);

            if (statusLabel != null)
                statusLabel.setText(App.getBundle().getString("chat.status.generating"));
        } else {
            cancelButton.setVisible(false);
            cancelButton.setManaged(false);

            sendButton.setVisible(true);
            sendButton.setManaged(true);

            if (statusLabel != null)
                statusLabel.setText(App.getBundle().getString("chat.status.ready"));
        }
    }

    @FXML
    private void onCancelClicked() {
        cancelGeneration();
    }

    private void cancelGeneration() {
        // Force close the stream at the network level
        OllamaManager.getInstance().cancelCurrentRequest();

        if (currentGenerationTask != null) {
            currentGenerationTask.cancel(true); // Interrupt thread as well
        }
        setGeneratingState(false);
    }

    private void addMessage(String text, boolean isUser) {
        if (isUser) {
            HBox bubbleContainer = new HBox();
            bubbleContainer.setAlignment(Pos.CENTER_RIGHT);

            Label bubble = new Label(text);
            bubble.setWrapText(true);
            bubble.getStyleClass().add("chat-bubble");
            bubble.getStyleClass().add("chat-bubble-user");
            // Ensure the label doesn't grow indefinitely
            bubble.setMaxWidth(600);

            bubbleContainer.getChildren().add(bubble);
            messagesContainer.getChildren().add(bubbleContainer);
        } else {
            // Assistant Message
            HBox container = new HBox();
            container.setAlignment(Pos.CENTER);
            container.setPadding(new Insets(10, 20, 10, 20));

            // Wrapper for Markdown + Footer actions
            VBox contentWrapper = new VBox();
            contentWrapper.setStyle("-fx-background-color: transparent;");
            HBox.setHgrow(contentWrapper, Priority.ALWAYS);
            contentWrapper.setMaxWidth(800); // Strict max width

            try {
                MarkdownOutput markdownOutput = new MarkdownOutput();
                // We let the wrapper constrain the width, but MarkdownOutput needs to fill it
                markdownOutput.setMaxWidth(Double.MAX_VALUE);

                // Set Initial Content
                markdownOutput.setMarkdown(text);

                // --- Copy Button Footer ---
                HBox footer = new HBox();
                footer.setAlignment(Pos.CENTER_RIGHT);
                footer.setPadding(new Insets(5, 0, 0, 0));

                Button copyBtn = new Button();
                copyBtn.getStyleClass().add("chat-copy-button");

                SVGPath copyIcon = new SVGPath();
                copyIcon.setContent(
                        "M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z");
                copyIcon.setStyle("-fx-fill: -color-fg-muted; -fx-scale-x: 0.9; -fx-scale-y: 0.9;");
                copyIcon.getStyleClass().add("icon");

                copyBtn.setGraphic(copyIcon);
                copyBtn.setTooltip(new Tooltip("Copy full response"));

                copyBtn.setOnAction(e -> {
                    ClipboardContent content = new ClipboardContent();
                    content.putString(markdownOutput.getMarkdown());
                    Clipboard.getSystemClipboard().setContent(content);

                    // Visual Feedback (Green Tick)
                    copyIcon.setStyle("-fx-fill: -color-success-fg; -fx-scale-x: 0.9; -fx-scale-y: 0.9;");
                    javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                            Duration.seconds(1));
                    pause.setOnFinished(
                            ev -> copyIcon.setStyle("-fx-fill: -color-fg-muted; -fx-scale-x: 0.9; -fx-scale-y: 0.9;"));
                    pause.play();
                });

                footer.getChildren().add(copyBtn);
                contentWrapper.getChildren().addAll(markdownOutput, footer);

                container.getChildren().add(contentWrapper);
            } catch (Throwable t) {
                t.printStackTrace();
                Label errorLabel = new Label("Error loading Markdown Component: " + t.getMessage());
                errorLabel.setStyle("-fx-text-fill: red;");
                container.getChildren().add(errorLabel);
            }
            messagesContainer.getChildren().add(container);
        }
    }

    private boolean modelListLoaded = false;
    private String pendingModelSelection = null;

    public void setModelName(String name) {
        if (!modelListLoaded || modelSelector.getItems() == null || modelSelector.getItems().isEmpty()) {
            this.pendingModelSelection = name;
            return;
        }

        for (String modelName : modelSelector.getItems()) {
            if (modelName.equals(name) || modelName.startsWith(name + ":")) {
                modelSelector.getSelectionModel().select(modelName);
                break;
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
