package com.org.ollamafx.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import atlantafx.base.controls.ProgressSliderSkin;
import atlantafx.base.controls.RingProgressIndicator;

import java.io.File;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Base64;
import java.io.ByteArrayInputStream;
import javafx.beans.binding.Bindings;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.animation.PauseTransition;
import com.org.ollamafx.util.Utils;
import com.org.ollamafx.service.MarkdownService;

import com.org.ollamafx.App;
import com.org.ollamafx.manager.ChatManager;
import com.org.ollamafx.manager.ModelManager;
import com.org.ollamafx.manager.OllamaManager;
import com.org.ollamafx.model.ChatMessage;
import com.org.ollamafx.model.ChatSession;
import com.org.ollamafx.model.OllamaModel;
import com.org.ollamafx.ui.ImagePreviewStrip;
import com.org.ollamafx.ui.MarkdownOutput;
import com.org.ollamafx.util.ImageUtils;

import io.github.ollama4j.models.generate.OllamaStreamHandler;

public class ChatController {

    private Future<?> currentGenerationTask;
    private boolean isGenerating = false;
    private final StringBuilder activeResponseBuffer = new StringBuilder();
    private long lastUiUpdate = 0;
    private static final long UI_UPDATE_INTERVAL_MS = 30; // ~30fps for text updates

    // Multimodal: Image preview strip
    private ImagePreviewStrip imagePreviewStrip;
    private Label visionWarningLabel;

    @FXML
    private ComboBox<String> modelSelector;
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
    private Button cancelButton;
    @FXML
    private Button attachButton;
    @FXML
    private Button exportMdButton;

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

    private static final Logger LOGGER = Logger.getLogger(ChatController.class.getName());

    @FXML
    public void initialize() {
        setupInputField();
        setupListeners();
        setupMultimedia();

        updateUIState(true); // Initial state is welcome screen
    }

    private void setupInputField() {
        inputField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (event.isShiftDown()) {
                    return;
                }
                event.consume();
                sendMessage();
            } else if (event.isShortcutDown() && event.getCode() == KeyCode.V) {
                handleClipboardPaste(event);
            }
        });
    }

    private void handleClipboardPaste(KeyEvent event) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (clipboard.hasFiles()) {
            boolean handledImagePaste = false;
            for (File file : clipboard.getFiles()) {
                if (ImageUtils.isValidImageFile(file)) {
                    imagePreviewStrip.addImage(file);
                    handledImagePaste = true;
                }
            }
            if (handledImagePaste) {
                event.consume();
            }
        }
    }

    private void setupListeners() {
        messagesContainer.heightProperty().addListener((observable, oldValue, newValue) -> {
            scrollPane.setVvalue(1.0);
        });

        modelSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (currentSession != null && newVal != null) {
                currentSession.setModelName(newVal);
                ChatManager.getInstance().saveChats();
            }
            updateVisionWarning();
        });

        tempSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateCreativityLabel(newVal.doubleValue());
            if (currentSession != null) {
                currentSession.setTemperature(newVal.doubleValue());
                ChatManager.getInstance().saveChats();
            }
        });

        systemPromptField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (currentSession != null) {
                currentSession.setSystemPrompt(newVal);
                ChatManager.getInstance().saveChats();
            }
        });
    }

    private void setupMultimedia() {
        initializeAdvancedParameters();
        initializeImagePreviewStrip();
        initializeDragAndDrop();
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

        // Clear any pending images when switching chats
        if (imagePreviewStrip != null) {
            imagePreviewStrip.clearImages();
        }

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

            // Cleanup any empty assistant messages (from errors or cancellations) before
            // rendering
            session.getMessages().removeIf(msg -> "assistant".equals(msg.getRole())
                    && (msg.getContent() == null || msg.getContent().isEmpty()));

            for (ChatMessage msg : session.getMessages()) {
                addMessage(msg.getContent(), "user".equals(msg.getRole()), msg.hasImages() ? msg.getImages() : null);
            }
        }
    }

    @FXML
    private void sendMessage() {
        String text = inputField.getText();
        if ((text.isEmpty() && !imagePreviewStrip.hasImages()) || currentSession == null) {
            return;
        }

        updateUIState(false);

        if (isGenerating) {
            cancelGeneration();
            return;
        }

        String modelName = modelSelector.getValue();
        if (modelName == null) {
            addMessage("Error: " + App.getBundle().getString("chat.selectModel"), false, null);
            if (statusLabel != null) {
                statusLabel.setText(App.getBundle().getString("chat.status.ready"));
            }
            return;
        }

        // Capture images and prepare session
        List<String> images = imagePreviewStrip.hasImages() ? imagePreviewStrip.getBase64Images() : null;
        prepareSessionForMessage(text, modelName, images);

        // UI Reset for generation
        inputField.clear();
        imagePreviewStrip.clearImages();
        setGeneratingState(true);
        updateStatusLabelForGeneration(images);

        activeResponseBuffer.setLength(0);

        // Create Assistant Placeholder
        ChatMessage assistantMsg = createAssistantPlaceholder();

        // Start Generation Task
        handleGenerationTask(modelName, text, images, assistantMsg);
    }

    private void prepareSessionForMessage(String text, String modelName, List<String> images) {
        addMessage(text, true, images);
        if (currentSession != null) {
            currentSession.addMessage(new ChatMessage("user", text, images));
            currentSession.setModelName(modelName);
            ChatManager.getInstance().saveChats();
        }
    }

    private void updateStatusLabelForGeneration(List<String> images) {
        if (statusLabel != null) {
            if (images != null && !images.isEmpty()) {
                statusLabel.setText(App.getBundle().getString("chat.status.analyzingImage"));
            } else {
                statusLabel.setText(App.getBundle().getString("chat.status.thinking"));
            }
        }
    }

    private ChatMessage createAssistantPlaceholder() {
        ChatMessage assistantMsg = new ChatMessage("assistant", "");
        if (currentSession != null) {
            currentSession.addMessage(assistantMsg);
            ChatManager.getInstance().saveChats();
        }
        addMessage("", false, null);
        if (statusLabel != null) {
            statusLabel.setText(App.getBundle().getString("chat.status.generating"));
        }
        return assistantMsg;
    }

    private void handleGenerationTask(String modelName, String text, List<String> images, ChatMessage assistantMsg) {
        final ChatSession targetSession = currentSession;

        currentGenerationTask = App.getExecutorService().submit(() -> {
            try {
                StringBuilder responseBuilder = new StringBuilder();
                Map<String, Object> options = collectGenerationOptions();

                String systemPrompt = targetSession != null ? targetSession.getSystemPrompt()
                        : systemPromptField.getText();

                OllamaManager.getInstance().askModelStream(modelName, text, images, options,
                        systemPrompt,
                        new OllamaStreamHandler() {
                            @Override
                            public void accept(String messagePart) {
                                if (Thread.currentThread().isInterrupted()) {
                                    throw new RuntimeException("Cancelled by user");
                                }

                                responseBuilder.setLength(0);
                                responseBuilder.append(messagePart);
                                String properFullText = responseBuilder.toString();

                                assistantMsg.setContent(properFullText);

                                long now = System.currentTimeMillis();
                                if (now - lastUiUpdate > UI_UPDATE_INTERVAL_MS) {
                                    Platform.runLater(() -> {
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
                    if (currentSession == targetSession) {
                        updateLastMessage(fullResponse);
                    }
                    assistantMsg.setContent(fullResponse);
                    if (targetSession != null) {
                        ChatManager.getInstance().saveChats();
                    }
                    setGeneratingState(false);
                });

            } catch (Exception e) {
                handleGenerationError(e, assistantMsg, targetSession);
            }
        });
    }

    private Map<String, Object> collectGenerationOptions() {
        Map<String, Object> options = new HashMap<>();
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
            LOGGER.log(Level.WARNING, "Invalid seed format: {0}", seedField.getText());
        }
        return options;
    }

    private void handleGenerationError(Exception e, ChatMessage assistantMsg, ChatSession targetSession) {
        if (e instanceof InterruptedException || Thread.interrupted()
                || "Cancelled by user".equals(e.getMessage())) {
            Platform.runLater(() -> setGeneratingState(false));
            return;
        }

        LOGGER.log(Level.SEVERE, "Generation error", e);
        Platform.runLater(() -> {
            String errorMsg = "⚡ Error: " + e.getMessage();
            assistantMsg.setContent(errorMsg);
            if (targetSession != null) {
                ChatManager.getInstance().saveChats();
            }

            if (currentSession == targetSession) {
                updateLastMessage(errorMsg);
            }
            setGeneratingState(false);
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
                        if (!wrapper.getChildren().isEmpty()) {
                            Node firstChild = wrapper.getChildren().get(0);
                            if (firstChild instanceof RingProgressIndicator) {
                                // First token received: Replace ring indicator with MarkdownOutput + Footer
                                wrapper.getChildren().clear();
                                setupAssistantContent(wrapper, text);
                            } else {
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

        // Remove thinking indicator if it's still there
        Platform.runLater(this::cleanupThinkingIndicator);

        // Remove empty assistant message from history if cancelled before any tokens
        // arrived
        if (currentSession != null) {
            List<ChatMessage> msgs = currentSession.getMessages();
            if (!msgs.isEmpty()) {
                ChatMessage last = msgs.get(msgs.size() - 1);
                if ("assistant".equals(last.getRole()) && (last.getContent() == null || last.getContent().isEmpty())) {
                    msgs.remove(msgs.size() - 1);
                    ChatManager.getInstance().saveChats();
                }
            }
        }

        setGeneratingState(false);
    }

    private void cleanupThinkingIndicator() {
        if (!messagesContainer.getChildren().isEmpty()) {
            Node lastNode = messagesContainer.getChildren().get(messagesContainer.getChildren().size() - 1);
            if (lastNode instanceof HBox) {
                HBox container = (HBox) lastNode;
                if (!container.getChildren().isEmpty() && container.getChildren().get(0) instanceof VBox) {
                    VBox wrapper = (VBox) container.getChildren().get(0);
                    if (!wrapper.getChildren().isEmpty()
                            && wrapper.getChildren().get(0) instanceof RingProgressIndicator) {
                        // Remove the whole message container if it's just a placeholder ring
                        messagesContainer.getChildren().remove(lastNode);
                    }
                }
            }
        }
    }

    private void addMessage(String text, boolean isUser, List<String> images) {
        if (isUser) {
            addUserMessage(text, images);
        } else {
            addAssistantMessage(text);
        }
    }

    private void addUserMessage(String text, List<String> images) {
        VBox userBubbleWrapper = new VBox(4);
        userBubbleWrapper.setAlignment(Pos.CENTER_RIGHT);

        if (images != null && !images.isEmpty()) {
            userBubbleWrapper.getChildren().add(createThumbnailRow(images));
        }

        Label bubble = new Label(text.isEmpty() ? App.getBundle().getString("chat.image.marker") : text);
        bubble.setWrapText(true);
        bubble.getStyleClass().addAll("chat-bubble", "chat-bubble-user");
        bubble.maxWidthProperty()
                .bind(Bindings.min(600.0, messagesContainer.widthProperty().subtract(60)));

        userBubbleWrapper.getChildren().add(bubble);

        HBox bubbleContainer = new HBox();
        bubbleContainer.setAlignment(Pos.CENTER_RIGHT);
        bubbleContainer.getChildren().add(userBubbleWrapper);
        messagesContainer.getChildren().add(bubbleContainer);
    }

    private HBox createThumbnailRow(List<String> images) {
        HBox thumbnailRow = new HBox(6);
        thumbnailRow.setAlignment(Pos.CENTER_RIGHT);
        thumbnailRow.setPadding(new Insets(0, 0, 4, 0));
        for (String base64 : images) {
            try {
                byte[] bytes = Base64.getDecoder().decode(base64);
                Image img = new Image(new ByteArrayInputStream(bytes), 80,
                        80, true, true);
                ImageView iv = new ImageView(img);
                iv.setFitWidth(80);
                iv.setFitHeight(80);
                iv.setPreserveRatio(true);
                Rectangle clip = new Rectangle(80, 80);
                clip.setArcWidth(12);
                clip.setArcHeight(12);
                iv.setClip(clip);
                iv.getStyleClass().add("chat-bubble-image");
                thumbnailRow.getChildren().add(iv);
            } catch (Exception ignored) {
            }
        }
        return thumbnailRow;
    }

    private void addAssistantMessage(String text) {
        HBox container = new HBox();
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(10, 20, 10, 20));

        VBox contentWrapper = new VBox();
        contentWrapper.setStyle("-fx-background-color: transparent;");
        HBox.setHgrow(contentWrapper, Priority.ALWAYS);
        contentWrapper.maxWidthProperty()
                .bind(Bindings.min(800.0, messagesContainer.widthProperty().subtract(100)));

        if (text.isEmpty()) {
            RingProgressIndicator ring = new RingProgressIndicator();
            ring.setProgress(-1);
            contentWrapper.getChildren().add(ring);
        } else {
            setupAssistantContent(contentWrapper, text);
        }

        container.getChildren().add(contentWrapper);
        messagesContainer.getChildren().add(container);
    }

    private void setupAssistantContent(VBox contentWrapper, String text) {
        MarkdownOutput markdownOutput = new MarkdownOutput();
        markdownOutput.setMaxWidth(Double.MAX_VALUE);
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
            PauseTransition pause = new PauseTransition(
                    Duration.seconds(1));
            pause.setOnFinished(
                    ev -> copyIcon.setStyle("-fx-fill: -color-fg-muted; -fx-scale-x: 0.9; -fx-scale-y: 0.9;"));
            pause.play();
        });

        footer.getChildren().add(copyBtn);
        contentWrapper.getChildren().addAll(markdownOutput, footer);
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

    // --- Multimodal: Drag & Drop and Image Preview ---

    private void initializeImagePreviewStrip() {
        imagePreviewStrip = new ImagePreviewStrip();

        visionWarningLabel = new Label();
        visionWarningLabel.getStyleClass().add("vision-warning-label");
        visionWarningLabel.setVisible(false);
        visionWarningLabel.setManaged(false);

        // Listen for image changes to toggle warning
        imagePreviewStrip.emptyProperty().addListener((obs, wasEmpty, isEmpty) -> {
            updateVisionWarning();
        });

        // Inject strip + warning into inputCapsule (between TextArea and toolbar)
        if (inputCapsule != null && inputCapsule.getChildren().size() >= 2) {
            // Index 0 = TextArea, Index 1 = bottom HBox toolbar
            inputCapsule.getChildren().add(1, visionWarningLabel);
            inputCapsule.getChildren().add(1, imagePreviewStrip);
        }
    }

    @FXML
    private void onExportMdClicked() {
        if (currentSession == null || currentSession.getMessages().isEmpty()) {
            Utils.showError("Export Error", "There are no messages to export.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Chat to Markdown");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Markdown Files", "*.md"));

        // Suggest a filename
        String safeName = currentSession.getName().replaceAll("[\\\\/:*?\"<>|]", "_");
        fileChooser.setInitialFileName(safeName + ".md");

        File file = fileChooser.showSaveDialog(inputField.getScene().getWindow());
        if (file != null) {
            try {
                MarkdownService.exportChatToMarkdown(currentSession, file);

                // Show info using standard Alert
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Successful");
                alert.setHeaderText(null);
                alert.setContentText("Chat exported to " + file.getName());
                alert.showAndWait();

            } catch (IOException e) {
                e.printStackTrace();
                Utils.showError("Export Error", "Failed to export chat: " + e.getMessage());
            }
        }
    }

    private void initializeDragAndDrop() {
        // Drag over handler for inputField
        inputField.setOnDragOver(this::handleDragOver);
        inputField.setOnDragDropped(this::handleDragDropped);
        inputField.setOnDragExited(e -> inputField.getStyleClass().remove("drag-overlay"));

        // Also allow drops on the messages container
        messagesContainer.setOnDragOver(this::handleDragOver);
        messagesContainer.setOnDragDropped(this::handleDragDropped);
    }

    private void handleDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            boolean hasImages = event.getDragboard().getFiles().stream()
                    .anyMatch(ImageUtils::isSupportedFormat);
            if (hasImages) {
                event.acceptTransferModes(TransferMode.COPY);
                if (!inputField.getStyleClass().contains("drag-overlay")) {
                    inputField.getStyleClass().add("drag-overlay");
                }
            }
        }
        event.consume();
    }

    private void handleDragDropped(DragEvent event) {
        inputField.getStyleClass().remove("drag-overlay");
        boolean success = false;
        if (event.getDragboard().hasFiles()) {
            for (File file : event.getDragboard().getFiles()) {
                String error = imagePreviewStrip.addImage(file);
                if (error != null) {
                    // Show error as status label briefly
                    if (statusLabel != null) {
                        String msg = App.getBundle().getString(error);
                        statusLabel.setText(msg);
                        // Reset after 3 seconds
                        PauseTransition reset = new PauseTransition(
                                Duration.seconds(3));
                        reset.setOnFinished(ev -> statusLabel.setText(App.getBundle().getString("chat.status.ready")));
                        reset.play();
                    }
                } else {
                    success = true;
                }
            }
        }
        event.setDropCompleted(success);
        event.consume();
    }

    @FXML
    private void onAttachClicked() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(App.getBundle().getString("chat.attachTitle"));

        // Add filters for valid image types
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg",
                "*.jpeg", "*.webp");
        fileChooser.getExtensionFilters().add(extFilter);

        // Allow multiple file selection
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(attachButton.getScene().getWindow());

        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            for (File file : selectedFiles) {
                if (ImageUtils.isValidImageFile(file)) {
                    imagePreviewStrip.addImage(file);
                }
            }
        }
    }

    private void updateVisionWarning() {
        if (visionWarningLabel == null || imagePreviewStrip == null)
            return;

        if (!imagePreviewStrip.hasImages()) {
            visionWarningLabel.setVisible(false);
            visionWarningLabel.setManaged(false);
            return;
        }

        // Check if current model supports vision
        String selectedModel = modelSelector.getValue();
        boolean isVisionModel = false;
        if (selectedModel != null) {
            // Check badges from the model manager's local models
            isVisionModel = ModelManager.getInstance().getLocalModels().stream()
                    .filter(m -> (m.getName() + ":" + m.getTag()).equals(selectedModel))
                    .findFirst()
                    .map(m -> m.getBadges().stream()
                            .anyMatch(b -> b.toLowerCase().contains("vision")))
                    .orElse(false);
        }

        if (!isVisionModel) {
            visionWarningLabel.setText(App.getBundle().getString("chat.image.visionWarning"));
            visionWarningLabel.setVisible(true);
            visionWarningLabel.setManaged(true);
        } else {
            visionWarningLabel.setVisible(false);
            visionWarningLabel.setManaged(false);
        }
    }

}
