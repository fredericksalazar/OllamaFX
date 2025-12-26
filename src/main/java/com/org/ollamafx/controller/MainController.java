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
import javafx.scene.layout.BorderPane;

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
    private ListView<ChatSession> chatListView;
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
    private ModelManager modelManager;

    public MainController() {
        this.chatManager = ChatManager.getInstance();
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

        chatListView.setItems(chatManager.getChatSessions());

        // ... (rest of initialize)

        chatListView.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(ChatSession item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setContextMenu(null);
                } else {
                    HBox container = new HBox();
                    container.setAlignment(Pos.CENTER_LEFT);
                    container.setSpacing(10);
                    container.setPadding(new Insets(5, 0, 5, 15));

                    String displayText = item.getName();
                    if (item.isPinned()) {
                        displayText = "📌 " + displayText;
                        getStyleClass().add("pinned-chat");
                    } else {
                        getStyleClass().remove("pinned-chat");
                    }
                    Label nameLabel = new Label(displayText);
                    nameLabel.setMaxWidth(Double.MAX_VALUE);
                    HBox.setHgrow(nameLabel, Priority.ALWAYS);

                    SVGPath icon = new SVGPath();
                    icon.setContent(
                            "M6 10c0-1.1.9-2 2-2s2 .9 2 2-.9 2-2 2-2-.9-2-2zm6 0c0-1.1.9-2 2-2s2 .9 2 2-.9 2-2 2-2-.9-2-2zm6 0c0-1.1.9-2 2-2s2 .9 2 2-.9 2-2 2-2-.9-2-2z");
                    icon.setScaleX(0.85);
                    icon.setScaleY(0.85);

                    MenuButton menuButton = new MenuButton();
                    menuButton.setGraphic(icon);
                    menuButton.getStyleClass().addAll("flat", "button-icon");
                    menuButton.setStyle("-fx-mark-visible: false;");

                    ResourceBundle bundle = resources; // Use injected resources

                    MenuItem renameItem = new MenuItem(bundle.getString("context.rename"));
                    renameItem.setOnAction(e -> {
                        TextInputDialog dialog = new TextInputDialog(item.getName());
                        dialog.setTitle(bundle.getString("dialog.rename.title"));
                        dialog.setHeaderText(bundle.getString("dialog.rename.header"));
                        dialog.showAndWait().ifPresent(newName -> {
                            chatManager.renameChat(item, newName);
                            getListView().refresh();
                        });
                    });
                    MenuItem pinItem = new MenuItem(
                            item.isPinned() ? bundle.getString("context.unpin") : bundle.getString("context.pin"));
                    pinItem.setOnAction(e -> {
                        chatManager.togglePin(item);
                        getListView().refresh();
                    });
                    MenuItem deleteItem = new MenuItem(bundle.getString("context.delete"));
                    deleteItem.setStyle("-fx-text-fill: red;");
                    deleteItem.setOnAction(e -> chatManager.deleteChat(item));
                    menuButton.getItems().addAll(renameItem, pinItem, new SeparatorMenuItem(),
                            deleteItem);

                    container.getChildren().addAll(nameLabel, menuButton);
                    setText(null);
                    setGraphic(container);
                }
            }

        });

        chatListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                clearToolSelection(); // Unselect
                                      // bottom
                                      // buttons
                loadChatView(newVal);
            }
        });
    }

    // --- Helper to handle Active State Visuals ---

    public void openChat(ChatSession session) {
        if (session != null) {
            chatListView.getSelectionModel().select(session);
            // The listener on selection model calls loadChatView
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
            chatListView.getSelectionModel().clearSelection();
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

        chatListView.getSelectionModel().select(newSession);
        // Listener calls loadChatView -> which calls clearToolSelection
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