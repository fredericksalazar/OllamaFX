package com.org.ollamafx.controller;

import com.org.ollamafx.manager.ChatManager;
import com.org.ollamafx.manager.ModelManager;
import com.org.ollamafx.manager.OllamaServiceManager;
import com.org.ollamafx.manager.ChatCollectionManager;
import com.org.ollamafx.model.ChatSession;
import com.org.ollamafx.model.ChatNode;
import com.org.ollamafx.model.ChatFolder;
import com.org.ollamafx.model.SmartCollection;
import com.org.ollamafx.ui.ChatTreeCell;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.FadeTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ContextMenu;
import com.org.ollamafx.ui.FxDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

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

        // --- Sidebar button icons (Lucide/Feather stroke style) ---
        btnHome.setGraphic(sidebarIcon("M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z", 16));
        btnAvailable.setGraphic(sidebarIcon("M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z", 16));
        btnLocal.setGraphic(sidebarIcon(
                "M21 16.5c0 .38-.21.71-.53.88l-7.9 4.44c-.16.12-.36.18-.57.18-.21 0-.41-.06-.57-.18l-7.9-4.44A.991.991 0 0 1 3 16.5v-9c0-.38.21-.71.53-.88l7.9-4.44c.16-.12.36-.18.57-.18.21 0 .41.06.57.18l7.9 4.44c.32.17.53.5.53.88v9zM12 4.15L6.04 7.5 12 10.85l5.96-3.35L12 4.15zM5 15.91l6 3.38v-6.71L5 9.21v6.7zM13 19.29l6-3.38v-6.7l-6 3.38v6.7z",
                16));
        btnSettings.setGraphic(sidebarIcon(
                "M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z",
                16));
        btnAbout.setGraphic(sidebarIcon(
                "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z",
                16));
    }

    /**
     * Crea un icono SVG estilo Lucide/Feather (stroke delgado, fill transparente)
     * para los botones del sidebar. Mismo estilo que los menu items de
     * ChatTreeCell.
     */
    private javafx.scene.Node sidebarIcon(String svgPath, double size) {
        SVGPath path = new SVGPath();
        path.setContent(svgPath);
        path.setStyle("-fx-fill: transparent; -fx-stroke: -color-fg-muted; -fx-stroke-width: 1.5;");
        double scale = size / 24.0;
        path.setScaleX(scale);
        path.setScaleY(scale);
        StackPane container = new StackPane(path);
        container.setPrefSize(size, size);
        container.setMinSize(size, size);
        container.setMaxSize(size, size);
        return container;
    }

    private void setupChatTree() {
        chatTreeView.setCellFactory(tv -> new ChatTreeCell());
        chatTreeView.setShowRoot(false);

        // Context Menu for empty space / root
        ContextMenu rootMenu = new ContextMenu();
        MenuItem smartColItem = new MenuItem("New Smart Collection");
        smartColItem.setOnAction(e -> {
            Optional<SmartCollection> result = com.org.ollamafx.ui.SmartCollectionDialog.show(null);
            result.ifPresent(sc -> {
                collectionManager.createSmartCollection(sc.getName(), sc.getCriteria(), sc.getValue(), sc.getIcon());
            });
        });
        rootMenu.getItems().add(smartColItem);
        chatTreeView.setContextMenu(rootMenu);
    }

    public void refreshChatTree() {
        // Simple approach: Rebuild.

        TreeItem<ChatNode> root = new TreeItem<>(new ChatNode((ChatFolder) null)); // Dummy Root
        root.setExpanded(true);

        // 0. Add Smart Collections
        for (SmartCollection sc : collectionManager.getSmartCollections()) {
            javafx.scene.Node scIcon = createSmartCollectionIcon(sc);

            TreeItem<ChatNode> scItem = new TreeItem<>(new ChatNode(sc), scIcon);
            scItem.setExpanded(sc.isExpanded());

            scItem.expandedProperty().addListener((obs, oldVal, newVal) -> {
                sc.setExpanded(newVal);
                collectionManager.updateSmartCollection(sc);
            });

            // Dynamic Filtering
            List<ChatSession> filteredChats = collectionManager.getChatsForSmartCollection(sc);
            for (ChatSession chat : filteredChats) {
                FontIcon chatIcon = new FontIcon(Feather.MESSAGE_SQUARE);
                chatIcon.getStyleClass().add("chat-icon");
                // Use a distinct node type or just ChatNode? ChatNode(ChatSession) works,
                // but we might want to know it belongs to a SmartCollection context?
                // For now, simple ChatNode.
                scItem.getChildren().add(new TreeItem<>(new ChatNode(chat), chatIcon));
            }

            // Optional: Add count to name? Done by cell factory if we updated toString or
            // cell renderer.
            // For now relies on ChatNode.toString().

            root.getChildren().add(scItem);
        }

        // 1. Add Folders
        for (ChatFolder folder : collectionManager.getFolders()) {
            // Create initial icon based on state
            javafx.scene.Node folderIcon = createFolderIcon(folder);

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

    private javafx.scene.Node createSmartCollectionIcon(SmartCollection sc) {
        Feather icon = Feather.ACTIVITY; // Default
        if (sc.getIcon() != null) {
            try {
                // Determine icon based on internal mapping or criteria
                // sc.getIcon() returns string name like "clock"
                switch (sc.getIcon().toLowerCase()) {
                    case "clock":
                        icon = Feather.CLOCK;
                        break;
                    case "tag":
                        icon = Feather.TAG;
                        break;
                    case "cpu":
                        icon = Feather.CPU;
                        break;
                    case "star":
                        icon = Feather.STAR;
                        break;
                    default:
                        icon = Feather.ACTIVITY;
                }
            } catch (Exception e) {
                // ignore
            }
        } else {
            // Fallback based on criteria
            if (sc.getCriteria() != null) {
                switch (sc.getCriteria()) {
                    case DATE:
                        icon = Feather.CLOCK;
                        break;
                    case KEYWORD:
                        icon = Feather.TAG;
                        break;
                    case MODEL:
                        icon = Feather.CPU;
                        break;
                }
            }
        }

        FontIcon fontIcon = new FontIcon(icon);
        fontIcon.setIconSize(16);
        fontIcon.setIconColor(Color.web("#007AFF")); // Apple Blue by default for smart collections
        return fontIcon;
    }

    private javafx.scene.Node createFolderIcon(ChatFolder folder) {
        // SVG Path for a standard folder icon (Material Design style)
        String folderPathContent = "M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z";

        SVGPath svgPath = new SVGPath();
        svgPath.setContent(folderPathContent);

        String colorHex = folder.getColor();
        if (colorHex == null || colorHex.isEmpty()) {
            colorHex = "#8E8E93"; // Default Gray
        }

        try {
            svgPath.setFill(Color.web(colorHex));
        } catch (IllegalArgumentException e) {
            svgPath.setFill(Color.GRAY);
        }

        // Scale it down to icon size (approx 16x16 or 20x20)
        // The original path is on a 24x24 grid.
        svgPath.setScaleX(0.75);
        svgPath.setScaleY(0.75);

        // Wrap in a StackPane to ensure proper sizing/alignment in the TreeCell
        StackPane iconContainer = new StackPane(svgPath);
        iconContainer.setPrefSize(20, 20);
        iconContainer.setMaxSize(20, 20);

        return iconContainer;
    }

    private ChatSession findChatById(String id) {
        return chatManager.getChatSessions().stream()
                .filter(c -> c.getId().toString().equals(id))
                .findFirst()
                .orElse(null);
    }

    @FXML
    private MenuButton btnAdd; // Finder-style add menu

    @FXML
    public void createNewFolder() {
        java.util.ResourceBundle bundle = com.org.ollamafx.App.getBundle();
        javafx.stage.Window owner = chatTreeView.getScene().getWindow();
        FxDialog.showInputDialog(
                owner,
                bundle.getString("dialog.folder.create.title"),
                bundle.getString("dialog.folder.create.placeholder"),
                "",
                bundle.getString("dialog.folder.create.confirm")).ifPresent(name -> {
                    if (!name.trim().isEmpty()) {
                        collectionManager.createFolder(name.trim());
                    }
                });
    }

    @FXML
    public void handleNewChat() {
        // Create a new empty chat in the root (Uncategorized)
        ChatSession newChat = chatManager.createChat("New Chat");
        // Select it immediately
        // Logic to select in tree...
        // We rely on refresh causing the tree to rebuild.
        // Ideally we should select the new item.
        // For now, let's just ensure it's created.
    }

    // Listener will trigger refresh

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

        // 1. Capture selection BEFORE creating chat (as creation triggers refresh via
        // listener)
        ChatFolder targetFolder = null;
        TreeItem<ChatNode> selectedItem = chatTreeView.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem.getValue() != null) {
            ChatNode node = selectedItem.getValue();
            if (node.getType() == ChatNode.Type.FOLDER) {
                targetFolder = node.getFolder();
            } else if (node.getType() == ChatNode.Type.CHAT) {
                // If a chat is selected, check if it belongs to a folder
                ChatSession selectedChat = node.getChat();
                for (ChatFolder folder : collectionManager.getFolders()) {
                    if (folder.getChatIds().contains(selectedChat.getId().toString())) {
                        targetFolder = folder;
                        break;
                    }
                }
            }
        }

        // 2. Create the chat (This triggers listener -> refreshChatTree)
        // Use a simple default name or fetch from bundle if desired.
        java.util.ResourceBundle bundle = com.org.ollamafx.App.getBundle();
        String defaultName = "New Chat";
        if (bundle.containsKey("sidebar.newChat")) {
            defaultName = bundle.getString("sidebar.newChat").trim().replace("+", "").trim();
        }

        ChatSession newSession = chatManager.createChat(defaultName);

        // 3. Move to folder if one was selected
        if (targetFolder != null) {
            collectionManager.moveChatToFolder(newSession, targetFolder);
            // Moving might trigger another refresh via collection listener, ensuring UI is
            // sync
        }

        // 4. Select the new item in the tree after refresh
        // We need to wait for the refresh to complete.
        // Since refresh is on the JavaFX thread (from listener), we can queue selection
        // using Platform.runLater to run AFTER the current event processing.
        Platform.runLater(() -> {
            selectChatInTree(newSession);
            loadChatView(newSession);
        });
    }

    private void selectChatInTree(ChatSession session) {
        if (chatTreeView.getRoot() == null)
            return;

        // Helper to recursively find and select
        findAndSelect(chatTreeView.getRoot(), session);
    }

    private boolean findAndSelect(TreeItem<ChatNode> item, ChatSession session) {
        if (item.getValue() != null && item.getValue().getType() == ChatNode.Type.CHAT) {
            if (item.getValue().getChat().getId().equals(session.getId())) {
                chatTreeView.getSelectionModel().select(item);
                chatTreeView.scrollTo(chatTreeView.getRow(item));
                return true;
            }
        }

        for (TreeItem<ChatNode> child : item.getChildren()) {
            if (findAndSelect(child, session))
                return true;
        }
        return false;
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