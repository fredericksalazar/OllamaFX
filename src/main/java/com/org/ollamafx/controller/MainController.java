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
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Application; // <-- Added import

public class MainController implements Initializable {

    @FXML
    private BorderPane mainBorderPane;
    @FXML
    private ListView<ChatSession> chatListView;

    private ChatManager chatManager;
    private ModelManager modelManager;

    public MainController() {
        System.out.println("MainController instantiated!");
        this.chatManager = ChatManager.getInstance();
    }

    public void initModelManager(ModelManager modelManager) {
        this.modelManager = modelManager;
        showAvailableModels();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("MainController initialized!");

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
                    // Layout Container (HBox for better alignment)
                    javafx.scene.layout.HBox container = new javafx.scene.layout.HBox();
                    container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    container.setSpacing(10);

                    // Chat Name Label
                    String displayText = item.getName();
                    if (item.isPinned()) {
                        displayText = "📌 " + displayText;
                        getStyleClass().add("pinned-chat");
                    } else {
                        getStyleClass().remove("pinned-chat");
                    }
                    Label nameLabel = new Label(displayText);
                    nameLabel.setMaxWidth(Double.MAX_VALUE);
                    javafx.scene.layout.HBox.setHgrow(nameLabel, javafx.scene.layout.Priority.ALWAYS);

                    // Menu Button (SVG Icon)
                    javafx.scene.shape.SVGPath icon = new javafx.scene.shape.SVGPath();
                    icon.setContent(
                            "M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z");
                    icon.getStyleClass().add("chat-menu-icon");

                    javafx.scene.control.MenuButton menuButton = new javafx.scene.control.MenuButton();
                    menuButton.setGraphic(icon);
                    menuButton.getStyleClass().add("chat-menu-button");

                    // Menu Items
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

                    setText(null); // Clear default text
                    setGraphic(container);
                }
            }
        });

        chatListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadChatView(newVal);
            }
        });
    }

    /**
     * Carga la vista de modelos disponibles y le inyecta el gestor de modelos.
     */
    @FXML
    private void showAvailableModels() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/available_models_view.fxml"));
            Parent view = loader.load();
            AvailableModelsController controller = loader.getController();
            controller.setModelManager(this.modelManager); // Inyección de dependencia.
            mainBorderPane.setCenter(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Carga la vista de modelos locales y le inyecta el gestor de modelos.
     */
    @FXML
    private void showLocalModels() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/local_models_view.fxml"));
            Parent view = loader.load();
            LocalModelsController controller = loader.getController();
            controller.setModelManager(this.modelManager); // Inyección de dependencia.
            mainBorderPane.setCenter(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void createNewChat() {
        System.out.println("Creating new chat...");
        ChatSession newSession = chatManager.createChat("New Chat");
        chatListView.getSelectionModel().select(newSession);
        // loadChatView is triggered by listener
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

            mainBorderPane.setCenter(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void toggleTheme() {
        if (Application.getUserAgentStylesheet()
                .equals(new atlantafx.base.theme.PrimerDark().getUserAgentStylesheet())) {
            // Switch to Light
            Application.setUserAgentStylesheet(new atlantafx.base.theme.PrimerLight().getUserAgentStylesheet());
            mainBorderPane.getScene().getRoot().getStyleClass().add("light");
        } else {
            // Switch to Dark
            Application.setUserAgentStylesheet(new atlantafx.base.theme.PrimerDark().getUserAgentStylesheet());
            mainBorderPane.getScene().getRoot().getStyleClass().remove("light");
        }
    }
}