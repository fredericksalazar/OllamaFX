package com.org.ollamafx.controller;

import com.org.ollamafx.manager.ChatManager;
import com.org.ollamafx.manager.ModelManager;
import com.org.ollamafx.model.ChatSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TreeView;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.BorderPane;

import com.org.ollamafx.model.ChatNode;
import com.org.ollamafx.model.ChatFolder;
import com.org.ollamafx.ui.ChatTreeCell;
import com.org.ollamafx.manager.ChatCollectionManager;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.material2.Material2AL;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Application;
import javafx.animation.FadeTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import com.org.ollamafx.manager.OllamaServiceManager;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.geometry.Pos;
import javafx.geometry.Insets;

public class MainController implements Initializable {

    @FXML
    private BorderPane mainBorderPane;
    @FXML
    private TreeView<ChatNode> chatTreeView;
    @FXML
    private javafx.scene.layout.StackPane centerContentPane;

    // Bottom Tool Buttons
    @FXML
    private javafx.scene.control.Button btnAvailable;
    @FXML
    private javafx.scene.control.Button btnLocal;
    @FXML
    private javafx.scene.control.Button btnSettings;
    @FXML
    private javafx.scene.control.Button btnAbout;
    @FXML
    private javafx.scene.control.Button btnHome;

    @FXML
    private HBox ollamaStatusBar;
    @FXML
    private Circle statusDot;
    @FXML
    private Circle pulseEmitter;
    @FXML
    private Label statusLabel;
    @FXML
    private ToggleButton btnControlOllama;

    private Timeline statusPollingTimeline;
    private FadeTransition pulseAnimation;

    private ChatManager chatManager;
    private ChatCollectionManager collectionManager;
    private ModelManager modelManager;

    public MainController() {
        this.chatManager = ChatManager.getInstance();
        this.collectionManager = ChatCollectionManager.getInstance();
    }

    public void initModelManager(ModelManager modelManager) {
        this.modelManager = modelManager;
        // If Ollama is already running but models failed to load (list empty), retry
        // now.
        if (OllamaServiceManager.getInstance().isRunning()) {
            if (modelManager.getLocalModels().isEmpty()) {
                modelManager.loadAllModels();
            }
        }
    }

    private Parent homeView;
    private HomeController homeController;

    private void preloadHomeView() {
        if (homeView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/home_view.fxml"));
                loader.setResources(com.org.ollamafx.App.getBundle());
                homeView = loader.load();
                homeController = loader.getController();
                // We inject modelManager later if initModelManager hasn't run yet,
                // but usually initModelManager runs after constructor/before UI show.
                // Safest to inject if modelManager is ready, otherwise showHome will do it.
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Eager load Home View
        preloadHomeView();

        // Ollama Checks
        checkOllamaInstallation();
        startStatusPolling();

        // Default to Home View
        Platform.runLater(this::showHome);

        // --- Chat Tree Setup ---
        setupChatTree();

        // Refresh tree initially
        refreshChatTree();

        // Listen for selection changes
        chatTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue().getType() == ChatNode.Type.CHAT) {
                clearToolSelection();
                loadChatView(newVal.getValue().getChat());
            }
        });

        // Listen for chat list changes to refresh tree
        // Weak listener or careful management needed? For now straightforward.
        chatManager.getChatSessions().addListener((javafx.collections.ListChangeListener<ChatSession>) c -> {
            refreshChatTree();
        });

        // Listen for folder changes (List add/remove)
        collectionManager.getFolders().addListener((javafx.collections.ListChangeListener<ChatFolder>) c -> {
            refreshChatTree();
        });

        // Listen for content updates (Color, content changes)
        collectionManager.addUpdateListener(() -> {
            Platform.runLater(this::refreshChatTree);
        });
    }

    private void setupChatTree() {
        chatTreeView.setCellFactory(tv -> new ChatTreeCell());
        chatTreeView.setShowRoot(false);
    }

    public void refreshChatTree() {
        // Simple approach: Rebuild.

        TreeItem<ChatNode> root = new TreeItem<>(new ChatNode((ChatFolder) null)); // Dummy Root
        root.setExpanded(true);

        // 1. Add Folders
        for (ChatFolder folder : collectionManager.getFolders()) {
            // Create initial icon based on state
            FontIcon folderIcon = createFolderIcon(folder);

            TreeItem<ChatNode> folderItem = new TreeItem<>(new ChatNode(folder), folderIcon);
            folderItem.setExpanded(folder.isExpanded());

            // Listener for expansion state persistence AND visual update
            folderItem.expandedProperty().addListener((obs, oldVal, newVal) -> {
                folder.setExpanded(newVal);
                // Update Icon
                folderItem.setGraphic(createFolderIcon(folder));
            });

            // Add Chats belonging to this folder
            for (String chatId : folder.getChatIds()) {
                ChatSession chat = findChatById(chatId);
                if (chat != null) {
                    FontIcon chatIcon = new FontIcon(Feather.MESSAGE_SQUARE);
                    chatIcon.getStyleClass().add("chat-icon");
                    folderItem.getChildren().add(new TreeItem<>(new ChatNode(chat), chatIcon));
                }
            }
            root.getChildren().add(folderItem);
        }

        // 2. Add Uncategorized Chats
        for (ChatSession chat : chatManager.getChatSessions()) {
            if (!collectionManager.isChatInFolder(chat)) {
                FontIcon chatIcon = new FontIcon(Feather.MESSAGE_SQUARE);
                chatIcon.getStyleClass().add("chat-icon");
                root.getChildren().add(new TreeItem<>(new ChatNode(chat), chatIcon));
            }
        }

        chatTreeView.setRoot(root);
    }

    private FontIcon createFolderIcon(ChatFolder folder) {
        // Closed = Solid Color (Material2AL.FOLDER)
        // Open = Outlined (Material2AL.FOLDER_OPEN) with Color border?
        // Actually Material2AL.FOLDER is usually solid/filled. Material2AL.FOLDER_OPEN
        // is usually outlined.
        // Let's verify Material2AL usage.

        // Note: We need to import Material2AL.

        org.kordamp.ikonli.Ikon iconCode;
        if (folder.isExpanded()) {
            iconCode = org.kordamp.ikonli.material2.Material2AL.FOLDER_OPEN;
        } else {
            iconCode = org.kordamp.ikonli.material2.Material2AL.FOLDER;
        }

        FontIcon icon = new FontIcon(iconCode);
        try {
            // Debug Logging
            // System.out.println("Applying Color to Folder '" + folder.getName() + "': " +
            // folder.getColor());
            icon.setIconColor(Color.web(folder.getColor()));
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid Color for folder " + folder.getName() + ": " + folder.getColor());
            icon.setIconColor(Color.GRAY);
        }
        // Increase size slightly for folders?
        // icon.setIconSize(16); // Default is usually fine
        return icon;
    }

    private ChatSession findChatById(String id) {
        return chatManager.getChatSessions().stream()
                .filter(c -> c.getId().toString().equals(id))
                .findFirst()
                .orElse(null);
    }

    @FXML
    public void createNewFolder() {
        TextInputDialog dialog = new TextInputDialog("New Folder");
        dialog.setTitle("Create Folder");
        dialog.setHeaderText("Enter folder name:");
        dialog.showAndWait().ifPresent(name -> {
            collectionManager.createFolder(name);
            // Listener will trigger refresh
        });
    }

    // --- Helper to handle Active State Visuals ---

    public void openChat(ChatSession session) {
        if (session != null) {
            // Find TreeItem for this session
            // Complex traversal needed or just logic?
            // For now, let's just create a temporary selection logic if needed
            // But usually openChat is called from Home or creation.
            // We need to expand folder if needed.
            // ... (Simple implementation: just focus if found)
        }
    }

    private void setActiveTool(javafx.scene.control.Button activeButton) {
        // Clear active class from all tools
        if (btnAvailable != null)
            btnAvailable.getStyleClass().remove("selected");
        if (btnLocal != null)
            btnLocal.getStyleClass().remove("selected");
        if (btnSettings != null)
            btnSettings.getStyleClass().remove("selected");
        if (btnAbout != null)
            btnAbout.getStyleClass().remove("selected");

        // Add to target
        if (activeButton != null) {
            activeButton.getStyleClass().add("selected");
            // Also clear chat selection so we don't look like we have 2 things active
            chatTreeView.getSelectionModel().clearSelection();
        }
    }

    // Clear tools when chat is selected
    private void clearToolSelection() {
        if (btnAvailable != null)
            btnAvailable.getStyleClass().remove("selected");
        if (btnLocal != null)
            btnLocal.getStyleClass().remove("selected");
        if (btnSettings != null)
            btnSettings.getStyleClass().remove("selected");
        if (btnAbout != null)
            btnAbout.getStyleClass().remove("selected");
    }

    /**
     * Carga la vista de modelos disponibles y le inyecta el gestor de modelos.
     */
    /**
     * Carga la vista de modelos disponibles y le inyecta el gestor de modelos.
     */
    @FXML
    public void showAvailableModels() {
        setActiveTool(btnAvailable);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/available_models_view.fxml"));
            loader.setResources(com.org.ollamafx.App.getBundle());
            Parent view = loader.load();
            AvailableModelsController controller = loader.getController();
            controller.setModelManager(this.modelManager); // Inyección de dependencia.
            centerContentPane.getChildren().setAll(view); // Update StackPane content
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showHome() {
        if (btnHome != null)
            setActiveTool(btnHome); // Ensure btnHome is defined in Controller

        if (homeView == null) {
            preloadHomeView();
        }

        if (homeController != null && this.modelManager != null) {
            homeController.setModelManager(this.modelManager);
            homeController.setMainController(this);
        }

        centerContentPane.getChildren().setAll(homeView);
    }

    /**
     * Carga la vista de modelos locales y le inyecta el gestor de modelos.
     */
    @FXML
    private void showLocalModels() {
        setActiveTool(btnLocal);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/local_models_view.fxml"));
            loader.setResources(com.org.ollamafx.App.getBundle());
            Parent view = loader.load();
            LocalModelsController controller = loader.getController();
            controller.setModelManager(this.modelManager); // Inyección de dependencia.
            centerContentPane.getChildren().setAll(view); // Update StackPane content
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void openSettings() {
        setActiveTool(btnSettings);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/settings_view.fxml"));
            loader.setResources(com.org.ollamafx.App.getBundle());
            Parent view = loader.load();
            centerContentPane.getChildren().setAll(view); // Update StackPane content
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void showAbout() {
        setActiveTool(btnAbout);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/about_view.fxml"));
            loader.setResources(com.org.ollamafx.App.getBundle());
            Parent view = loader.load();
            centerContentPane.getChildren().setAll(view); // Update StackPane content
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void createNewChat() {
        System.out.println("Creating new chat...");
        // Use a simple default name or fetch from bundle if desired.
        // For now, let's keep it simple or use a localized "Chat"
        ChatSession newSession = chatManager.createChat("Chat"); // Simplified

        // Select in tree?
        refreshChatTree();
        // Logic to select the new item in tree...
        // For now user can find it in Uncategorized.
    }

    /**
     * Carga la vista de un chat específico.
     */
    private void loadChatView(ChatSession session) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/chat_view.fxml"));
            loader.setResources(com.org.ollamafx.App.getBundle());
            Parent view = loader.load();

            ChatController controller = loader.getController();
            controller.setModelManager(this.modelManager); // Inject ModelManager
            controller.setChatSession(session); // Inject Session

            centerContentPane.getChildren().setAll(view); // Update StackPane content
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void toggleTheme() {
        if (Application.getUserAgentStylesheet()
                .equals(new atlantafx.base.theme.CupertinoDark().getUserAgentStylesheet())) {
            // Switch to Light
            Application.setUserAgentStylesheet(new atlantafx.base.theme.CupertinoLight().getUserAgentStylesheet());
            if (mainBorderPane.getScene() != null) {
                mainBorderPane.getScene().getRoot().getStyleClass().remove("dark");
                mainBorderPane.getScene().getRoot().getStyleClass().add("light");
            }
        } else {
            // Switch to Dark
            Application.setUserAgentStylesheet(new atlantafx.base.theme.CupertinoDark().getUserAgentStylesheet());
            if (mainBorderPane.getScene() != null) {
                mainBorderPane.getScene().getRoot().getStyleClass().remove("light");
                mainBorderPane.getScene().getRoot().getStyleClass().add("dark");
            }
        }
    }

    // --- OLLAMA STATUS LOGIC ---

    private void checkOllamaInstallation() {
        boolean installed = OllamaServiceManager.getInstance().isInstalled();
        if (!installed) {
            statusLabel.setText("Not Installed");
            statusLabel.setStyle("-fx-text-fill: -color-danger-fg;"); // Use error color

            // Reconfigure control button to be "Download"
            btnControlOllama.setText("Download"); // Assuming button supports text/icon change
            btnControlOllama.setSelected(false);
            btnControlOllama.setDisable(false);
            btnControlOllama.setTooltip(new javafx.scene.control.Tooltip("Download Ollama from ollama.com"));

            btnControlOllama.setOnAction(e -> {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI("https://ollama.com"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    // Fallback: copy to clipboard or show alert
                }
            });

            // Optionally disable other interactions or show a modal
        } else {
            // Reset to default behavior (managed by updateStatusUI)
            btnControlOllama.setText(null); // Clear text if icon only
        }
    }

    private void startStatusPolling() {
        // If not installed, don't poll or auto-start
        if (!OllamaServiceManager.getInstance().isInstalled()) {
            return;
        }

        statusPollingTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
            // Move blocking check to background thread
            new Thread(() -> {
                boolean running = OllamaServiceManager.getInstance().isRunning();
                Platform.runLater(() -> updateStatusUI(running));
            }).start();
        }));
        statusPollingTimeline.setCycleCount(Timeline.INDEFINITE);
        statusPollingTimeline.play();

        // Initial check immediately
        boolean running = OllamaServiceManager.getInstance().isRunning();
        if (!running) {
            // Auto-Start Scenario
            statusLabel.setText("Auto-starting...");
            new Thread(() -> {
                boolean started = OllamaServiceManager.getInstance().startOllama();
                Platform.runLater(() -> {
                    updateStatusUI(started);
                    if (started && modelManager != null) {
                        modelManager.loadAllModels();
                    }
                });
            }).start();
        } else {
            updateStatusUI(true);
        }
    }

    private void updateStatusUI(boolean running) {
        if (ollamaStatusBar == null || statusLabel == null || btnControlOllama == null)
            return;

        // Ensure Pulse Animation
        if (pulseAnimation == null && pulseEmitter != null) {
            pulseAnimation = new FadeTransition(Duration.seconds(1.5), pulseEmitter);
            pulseAnimation.setFromValue(0.8);
            pulseAnimation.setToValue(0.0);
            pulseAnimation.setCycleCount(FadeTransition.INDEFINITE);
        }

        if (running) {
            if (statusDot != null) {
                statusDot.getStyleClass().removeAll("status-dot-stopped");
                if (!statusDot.getStyleClass().contains("status-dot-running")) {
                    statusDot.getStyleClass().add("status-dot-running");
                }
            }
            if (pulseEmitter != null) {
                pulseEmitter.setFill(javafx.scene.paint.Color.rgb(0, 255, 128)); // Neon Green
                if (pulseAnimation.getStatus() != javafx.animation.Animation.Status.RUNNING) {
                    pulseAnimation.play();
                }
            }

            statusLabel.setText("Ollama Service");

            // Switch State
            btnControlOllama.setSelected(true);
            btnControlOllama.setTooltip(new javafx.scene.control.Tooltip("Stop Service"));
            btnControlOllama.setOnAction(this::toggleOllamaService);
        } else {
            if (statusDot != null) {
                statusDot.getStyleClass().removeAll("status-dot-running");
                if (!statusDot.getStyleClass().contains("status-dot-stopped")) {
                    statusDot.getStyleClass().add("status-dot-stopped");
                }
            }
            if (pulseEmitter != null) {
                pulseEmitter.setFill(javafx.scene.paint.Color.rgb(255, 59, 48)); // Neon Red
                // Pulse even when stopped? User image implies active pulse for "Operational".
                // Maybe stop pulse if stopped.
                pulseAnimation.stop();
                pulseEmitter.setOpacity(0.0);
            }

            statusLabel.setText("Ollama Service");

            // Switch State
            btnControlOllama.setSelected(false);
            btnControlOllama.setTooltip(new javafx.scene.control.Tooltip("Start Service"));
            btnControlOllama.setOnAction(this::toggleOllamaService);
        }
    }

    @FXML
    private void toggleOllamaService(javafx.event.ActionEvent event) {
        // Prevent double interactions while checking/toggling
        btnControlOllama.setDisable(true);
        statusLabel.setText("Checking...");

        new Thread(() -> {
            boolean isRunning = OllamaServiceManager.getInstance().isRunning();

            Platform.runLater(() -> {
                if (isRunning) {
                    // STOPPING
                    statusLabel.setText("Stopping...");
                    new Thread(() -> {
                        OllamaServiceManager.getInstance().stopOllama();
                        Platform.runLater(() -> {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                            }
                            // Re-enable button and update UI
                            // here inside
                            // thread? No, need
                            // to be careful.
                            // Actually, let's just trigger an updateStatusUI logic via poll or explicit
                            // check.
                            // updateStatusUI is checking running status? No, it takes a boolean.
                            // Let's do a quick check
                            new Thread(() -> {
                                boolean finalState = OllamaServiceManager.getInstance().isRunning();
                                Platform.runLater(() -> {
                                    updateStatusUI(finalState);
                                    btnControlOllama.setDisable(false);
                                });
                            }).start();
                        });
                    }).start();
                } else {
                    // STARTING
                    statusLabel.setText("Starting...");
                    new Thread(() -> {
                        boolean success = OllamaServiceManager.getInstance().startOllama();
                        Platform.runLater(() -> {
                            btnControlOllama.setDisable(false);
                            if (success) {
                                updateStatusUI(true);
                            } else {
                                statusLabel.setText("Start Failed");
                                updateStatusUI(false);
                            }
                        });
                    }).start();
                }
            });
        }).start();
    }
}