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
import atlantafx.base.theme.CupertinoDark;
import atlantafx.base.theme.CupertinoLight;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Application;

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

    private ChatManager chatManager;
    private ModelManager modelManager;

    public MainController() {
        System.out.println("MainController instantiated!");
        this.chatManager = ChatManager.getInstance();
    }

    public void initModelManager(ModelManager modelManager) {
        this.modelManager = modelManager;
        // Default View? Maybe available models or empty
        // showAvailableModels();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("MainController initialized!");

        if (centerContentPane == null) {
            System.err.println("centerContentPane is NULL! Check FXML fx:id");
        }

        chatListView.setItems(chatManager.getChatSessions());

        chatListView.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(ChatSession item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setContextMenu(null);
                } else {
                    // Layout Container matches CSS pill style
                    javafx.scene.layout.HBox container = new javafx.scene.layout.HBox();
                    container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    container.setSpacing(10);
                    container.setPadding(new javafx.geometry.Insets(5, 0, 5, 15)); // Add left padding

                    String StringdisplayText = item.getName();
                    if (item.isPinned()) {
                        StringdisplayText = "📌 " + StringdisplayText;
                        getStyleClass().add("pinned-chat");
                    } else {
                        getStyleClass().remove("pinned-chat");
                    }
                    Label nameLabel = new Label(StringdisplayText);
                    nameLabel.setMaxWidth(Double.MAX_VALUE);
                    javafx.scene.layout.HBox.setHgrow(nameLabel, javafx.scene.layout.Priority.ALWAYS);

                    // Menu Button (Settings Gear Icon)
                    javafx.scene.shape.SVGPath icon = new javafx.scene.shape.SVGPath();
                    // Ellipsis Icon (Three Dots) - Horizontal
                    icon.setContent(
                            "M6 10c0-1.1.9-2 2-2s2 .9 2 2-.9 2-2 2-2-.9-2-2zm6 0c0-1.1.9-2 2-2s2 .9 2 2-.9 2-2 2-2-.9-2-2zm6 0c0-1.1.9-2 2-2s2 .9 2 2-.9 2-2 2-2-.9-2-2z");
                    icon.setScaleX(0.85); // Make it smaller/lighter
                    icon.setScaleY(0.85);

                    // We can use styleclass or inline style to ensure color adapts
                    // icon.setStyle("-fx-fill: -color-fg-default;"); // Native text color

                    javafx.scene.control.MenuButton menuButton = new javafx.scene.control.MenuButton();
                    menuButton.setGraphic(icon);
                    // Native AtlantaFX "Flat" and "Icon" styles
                    menuButton.getStyleClass().addAll("flat", "button-icon");
                    // Remove default arrow if user wants just the gear, but MenuButton usually
                    // needs arrow.
                    // "button-icon" often hides text. "flat" removes background.
                    // To remove the arrow, we can style the .arrow pane to 0 size in CSS or just
                    // accept it.
                    // For now, adhere to "MenuButton de tipo flat" which usually keeps arrow but is
                    // clean.
                    menuButton.setStyle("-fx-mark-visible: false;"); // Try to hide arrow via style if possible, or
                                                                     // leave standard.
                    // Actually, let's stick to standard FLAT.

                    javafx.scene.control.MenuItem renameItem = new javafx.scene.control.MenuItem("Rename");
                    renameItem.setOnAction(e -> {
                        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog(
                                item.getName());
                        dialog.setTitle("Rename Chat");
                        dialog.setHeaderText("Enter new name:");
                        dialog.showAndWait().ifPresent(newName -> {
                            chatManager.renameChat(item, newName);
                            getListView().refresh();
                        });
                    });
                    javafx.scene.control.MenuItem pinItem = new javafx.scene.control.MenuItem(
                            item.isPinned() ? "Unpin" : "Pin");
                    pinItem.setOnAction(e -> {
                        chatManager.togglePin(item);
                        getListView().refresh();
                    });
                    javafx.scene.control.MenuItem deleteItem = new javafx.scene.control.MenuItem("Delete");
                    deleteItem.setStyle("-fx-text-fill: red;");
                    deleteItem.setOnAction(e -> chatManager.deleteChat(item));
                    menuButton.getItems().addAll(renameItem, pinItem, new javafx.scene.control.SeparatorMenuItem(),
                            deleteItem);

                    container.getChildren().addAll(nameLabel, menuButton);
                    setText(null);
                    setGraphic(container);
                }
            }
        });

        chatListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                clearToolSelection(); // Unselect bottom buttons
                loadChatView(newVal);
            }
        });
    }

    // --- Helper to handle Active State Visuals ---
    private void setActiveTool(javafx.scene.control.Button activeButton) {
        // Clear active class from all tools
        if (btnAvailable != null)
            btnAvailable.getStyleClass().remove("selected");
        if (btnLocal != null)
            btnLocal.getStyleClass().remove("selected");
        if (btnSettings != null)
            btnSettings.getStyleClass().remove("selected");

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
    }

    /**
     * Carga la vista de modelos disponibles y le inyecta el gestor de modelos.
     */
    @FXML
    private void showAvailableModels() {
        setActiveTool(btnAvailable);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/available_models_view.fxml"));
            Parent view = loader.load();
            AvailableModelsController controller = loader.getController();
            controller.setModelManager(this.modelManager); // Inyección de dependencia.
            centerContentPane.getChildren().setAll(view); // Update StackPane content
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Carga la vista de modelos locales y le inyecta el gestor de modelos.
     */
    @FXML
    private void showLocalModels() {
        setActiveTool(btnLocal);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/local_models_view.fxml"));
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
            Parent view = loader.load();
            centerContentPane.getChildren().setAll(view); // Update StackPane content
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void createNewChat() {
        System.out.println("Creating new chat...");
        ChatSession newSession = chatManager.createChat("New Chat");
        chatListView.getSelectionModel().select(newSession);
        // Listener calls loadChatView -> which calls clearToolSelection
    }

    /**
     * Carga la vista de un chat específico.
     */
    private void loadChatView(ChatSession session) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/chat_view.fxml"));
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
}