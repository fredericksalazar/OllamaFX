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
import javafx.scene.control.TextField;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import atlantafx.base.controls.ProgressSliderSkin;

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
    private Slider tempSlider;
    @FXML
    private Label tempValueLabel;
    @FXML
    private TextArea systemPromptField;

    // Advanced Params
    @FXML
    private ComboBox<String> presetSelector;
    @FXML
    private Slider ctxSlider;
    @FXML
    private Label ctxValueLabel;
    @FXML
    private Slider topKSlider;
    @FXML
    private Label topKValueLabel;
    @FXML
    private Slider topPSlider;
    @FXML
    private Label topPValueLabel;
    @FXML
    private TextField seedField;

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

        // Creativity (Temperature)
        tempSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateCreativityLabel(newVal.doubleValue());
            // Save logic if needed directly or on release

            if (currentSession != null) {
                currentSession.setTemperature(newVal.doubleValue());
                ChatManager.getInstance().saveChats();
            }
        });

        // Initialize color for default

        // System Prompt Listener
        systemPromptField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (currentSession != null) {
                currentSession.setSystemPrompt(newVal);
                // We might want to save immediately or distinct logic.
                // Saving immediately ensures persistence.
                ChatManager.getInstance().saveChats();
            }
        });

        // Initialize Advanced Parameters
        initializeAdvancedParameters();

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

            // Default: Show Sidebar for New Chat
            if (rightSidebar != null && !rightSidebar.isVisible()) {
                rightSidebar.setVisible(true);
                rightSidebar.setManaged(true);
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

            // Restore Parameters
            double temp = session.getTemperature();
            tempSlider.setValue(temp);
            updateCreativityLabel(temp);

            ctxSlider.setValue(session.getNumCtx());
            topKSlider.setValue(session.getTopK());
            topPSlider.setValue(session.getTopP());

            seedField.setText(session.getSeed() == -1 ? "" : String.valueOf(session.getSeed()));

            // Restore System Prompt
            systemPromptField.setText(session.getSystemPrompt() != null ? session.getSystemPrompt() : "");

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

                // Collect Options
                java.util.Map<String, Object> options = new java.util.HashMap<>();
                options.put("temperature", tempSlider.getValue());
                options.put("num_ctx", (int) ctxSlider.getValue());
                options.put("top_k", (int) topKSlider.getValue());
                options.put("top_p", topPSlider.getValue());

                try {
                    String seedText = seedField.getText().trim();
                    if (!seedText.isEmpty()) {
                        options.put("seed", Integer.parseInt(seedText));
                    }
                } catch (NumberFormatException e) {
                    // Ignore invalid seed
                }

                // Use the session's prompt or the field's current value (should be synced)
                String systemPrompt = targetSession != null ? targetSession.getSystemPrompt()
                        : systemPromptField.getText();

                OllamaManager.getInstance().askModelStream(targetModel, text, options,
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

    private void updateCreativityLabel(double val) {
        String label = "";
        // Colors: Precise (Blue), Balanced (Green), Creative (Orange/Red)
        if (val < 0.3) {
            label = App.getBundle().getString("chat.creativity.precise");
        } else if (val < 0.7) {
            label = App.getBundle().getString("chat.creativity.balanced");
        } else {
            label = App.getBundle().getString("chat.creativity.imaginative");
        }
        tempValueLabel.setText(label);
    }

    private void initializeAdvancedParameters() {
        // Presets
        ObservableList<String> presets = FXCollections.observableArrayList(
                App.getBundle().getString("chat.preset.default"),
                App.getBundle().getString("chat.preset.writer"), // Creative
                App.getBundle().getString("chat.preset.precise"), // Dev
                App.getBundle().getString("chat.preset.lawyer"),
                App.getBundle().getString("chat.preset.doctor"),
                App.getBundle().getString("chat.preset.student"));
        presetSelector.setItems(presets);

        presetSelector.setOnAction(e -> applyPreset());

        // Context Window
        ctxSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int val = newVal.intValue();
            ctxValueLabel.setText(String.valueOf(val));
            // Just use a static accent for context or maybe based on size?
            // Let's keep context blue/neutral or maybe subtle scale.
            // For now, let's just colorize Temp/TopP as they are "vibe" params.
            if (currentSession != null) {
                currentSession.setNumCtx(val);
                ChatManager.getInstance().saveChats();
            }
        });

        // Top-K
        topKSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int val = newVal.intValue();
            topKValueLabel.setText(String.valueOf(val));
            // Maybe Green for low K (narrow)?
            // updateSliderColor(topKSlider, val, 1, 100);
            if (currentSession != null) {
                currentSession.setTopK(val);
                ChatManager.getInstance().saveChats();
            }
        });

        // Top-P
        topPSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double val = Math.round(newVal.doubleValue() * 100.0) / 100.0;
            topPValueLabel.setText(String.format("%.2f", val));

            if (currentSession != null) {
                currentSession.setTopP(val);
                ChatManager.getInstance().saveChats();
            }
        });

        // Init colors

        // Seed (Numeric only)
        seedField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("-?\\d*")) {
                seedField.setText(newValue.replaceAll("[^-?\\d]", ""));
            }
            // Save logic
            if (currentSession != null) {
                try {
                    int seed = newValue.isEmpty() || newValue.equals("-") ? -1 : Integer.parseInt(newValue);
                    currentSession.setSeed(seed);
                    ChatManager.getInstance().saveChats();
                } catch (NumberFormatException ignored) {
                }
            }
        });

        // Apply AtlantaFX ProgressSliderSkin
        tempSlider.setSkin(new ProgressSliderSkin(tempSlider));
        topPSlider.setSkin(new ProgressSliderSkin(topPSlider));
        topKSlider.setSkin(new ProgressSliderSkin(topKSlider));
        ctxSlider.setSkin(new ProgressSliderSkin(ctxSlider));
    }

    private void applyPreset() {
        String selected = presetSelector.getValue();
        if (selected == null)
            return;

        // Reset Styles
        rightSidebar.getStyleClass().removeAll("theme-creative", "theme-precise", "theme-professional",
                "theme-academic", "theme-medical");

        if (selected.equals(App.getBundle().getString("chat.preset.writer"))) {
            // Creative Writer
            tempSlider.setValue(0.9);
            topPSlider.setValue(0.95);
            topKSlider.setValue(50);
            ctxSlider.setValue(8192); // More context for stories
            rightSidebar.getStyleClass().add("theme-creative"); // Green
        } else if (selected.equals(App.getBundle().getString("chat.preset.precise"))) {
            // Developer
            tempSlider.setValue(0.2);
            topPSlider.setValue(0.3);
            topKSlider.setValue(20);
            ctxSlider.setValue(16384); // High context for codebases
            rightSidebar.getStyleClass().add("theme-precise"); // Blue
        } else if (selected.equals(App.getBundle().getString("chat.preset.lawyer"))) {
            // Lawyer
            tempSlider.setValue(0.3); // Low creativity, high accuracy
            topPSlider.setValue(0.4);
            ctxSlider.setValue(32768); // Max context for legal docs
            rightSidebar.getStyleClass().add("theme-professional"); // Purple
        } else if (selected.equals(App.getBundle().getString("chat.preset.doctor"))) {
            // Doctor
            tempSlider.setValue(0.1); // Extremely factual
            topPSlider.setValue(0.2);
            ctxSlider.setValue(16384);
            rightSidebar.getStyleClass().add("theme-medical"); // Red
        } else if (selected.equals(App.getBundle().getString("chat.preset.student"))) {
            // Student
            tempSlider.setValue(0.6); // Balanced, slightly creative
            topPSlider.setValue(0.8);
            ctxSlider.setValue(4096);
            rightSidebar.getStyleClass().add("theme-academic"); // Orange
        } else {
            // Default
            tempSlider.setValue(0.7);
            topPSlider.setValue(0.9);
            topKSlider.setValue(40);
            ctxSlider.setValue(4096);
            // No theme class implies default border
        }

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

            // Dynamic Max Width: Min(600, Container Width - Padding)
            bubble.maxWidthProperty().bind(
                    javafx.beans.binding.Bindings.min(
                            600.0,
                            messagesContainer.widthProperty().subtract(60) // Safety padding
                    ));

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

            // Dynamic Max Width: Min(800, Container Width - Padding)
            // Container Padding (20+20) + MessageContainer Padding (20+20) = 80. Use 100
            // for safety.
            contentWrapper.maxWidthProperty().bind(
                    javafx.beans.binding.Bindings.min(
                            800.0,
                            messagesContainer.widthProperty().subtract(100)));

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

    @FXML
    private void toggleSidebar() {
        if (rightSidebar != null) {
            boolean isVisible = rightSidebar.isVisible();
            rightSidebar.setVisible(!isVisible);
            rightSidebar.setManaged(!isVisible);
        }
    }

    @FXML
    private VBox rightSidebar;
    @FXML
    private Button sidebarToggleButton;

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

    // Default Sidebar state logic moved to setChatSession and initialize

}
