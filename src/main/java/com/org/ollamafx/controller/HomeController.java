package com.org.ollamafx.controller;

import com.org.ollamafx.manager.ModelManager;
import com.org.ollamafx.model.OllamaModel;
import com.org.ollamafx.ui.ModelCard;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import java.net.URL;
import java.util.ResourceBundle;

public class HomeController implements Initializable {

    @FXML
    private HBox recommendedContainer;
    @FXML
    private HBox popularContainer;
    @FXML
    private HBox newContainer;
    @FXML
    private Button btnExplore;

    private ModelManager modelManager;

    public void setModelManager(ModelManager modelManager) {
        this.modelManager = modelManager;
        setupListeners();

        // Populate with existing data (from cache/memory) immediately
        if (!modelManager.getPopularModels().isEmpty()) {
            updateCarousel(popularContainer, modelManager.getPopularModels());
        }
        if (!modelManager.getNewModels().isEmpty()) {
            updateCarousel(newContainer, modelManager.getNewModels());
        }
        if (!modelManager.getRecommendedModels().isEmpty()) {
            updateCarousel(recommendedContainer, modelManager.getRecommendedModels());
        }

        // Trigger background load (refresh)
        modelManager.loadLibraryModels();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initial setup if needed
    }

    private void setupListeners() {
        modelManager.getPopularModels().addListener((ListChangeListener<OllamaModel>) c -> {
            Platform.runLater(() -> updateCarousel(popularContainer, c.getList()));
        });

        modelManager.getNewModels().addListener((ListChangeListener<OllamaModel>) c -> {
            Platform.runLater(() -> updateCarousel(newContainer, c.getList()));
        });

        modelManager.getRecommendedModels().addListener((ListChangeListener<OllamaModel>) c -> {
            Platform.runLater(() -> updateCarousel(recommendedContainer, c.getList()));
        });
    }

    private void updateCarousel(HBox container, java.util.List<? extends OllamaModel> models) {
        container.getChildren().clear();
        for (OllamaModel model : models) {
            boolean isInstalled = modelManager.isModelInstalled(model.getName(), model.getTag());
            ModelCard card = new ModelCard(model, isInstalled,
                    () -> handleInstall(model),
                    () -> handleDetails(model));
            container.getChildren().add(card);
        }
    }

    private void handleInstall(OllamaModel model) {
        System.out.println("Install requested for: " + model.getName());
        showDownloadPopup(model);
    }

    private void handleDetails(OllamaModel model) {
        System.out.println("Details requested for: " + model.getName());
        // For now, simpler navigation: go to available models view and select it?
        // Or open Details Popup?
        // Let's rely on "View More" for full navigation, or maybe simple log for now
        // as per instructions "3 - ... ningun boton ejecuta ningun proceso de descarga"
        // -> fix that.
        // User didn't prioritize details navigation, but download.
    }

    private void showDownloadPopup(OllamaModel model) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/ui/download_popup.fxml"));
            loader.setResources(com.org.ollamafx.App.getBundle());

            javafx.scene.Parent root = loader.load();
            com.org.ollamafx.controller.DownloadPopupController controller = loader.getController();
            controller.setModelName(model.getName() + ":" + model.getTag());

            String userAgentStylesheet = javafx.application.Application.getUserAgentStylesheet();
            if (userAgentStylesheet != null && userAgentStylesheet.toLowerCase().contains("light")) {
                root.getStyleClass().add("light");
            } else {
                root.getStyleClass().add("dark");
            }

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            stage.setTitle(com.org.ollamafx.App.getBundle().getString("download.title.default"));

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.setScene(scene);
            stage.setResizable(false);

            javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
                @Override
                protected Void call() throws Exception {
                    updateMessage(com.org.ollamafx.App.getBundle().getString("download.status.process"));
                    updateProgress(0, 100);

                    com.org.ollamafx.manager.OllamaManager.getInstance().pullModel(model.getName(), model.getTag(),
                            (progress, status) -> {
                                updateMessage(status);
                                if (progress >= 0) {
                                    updateProgress(progress, 100);
                                } else {
                                    updateProgress(-1, 100);
                                }
                            });

                    return null;
                }
            };

            task.setOnSucceeded(e -> {
                System.out.println("Download complete!");
                if (modelManager != null) {
                    String date = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                            .format(java.time.LocalDateTime.now());
                    OllamaModel newModel = new OllamaModel(
                            model.getName(), com.org.ollamafx.App.getBundle().getString("model.installed"), "N/A",
                            model.getTag(),
                            model.getSize(), date,
                            "N/A", "N/A"); // simplified
                    modelManager.addLocalModel(newModel);

                    // Refresh home to update button state (Get -> Details)
                    // loadLibraryModels might overlap, but quick valid check runs in ModelCard
                    // update
                    // We might need to force refresh ui
                    modelManager.loadLibraryModels();
                }
            });

            task.setOnFailed(e -> {
                System.err.println("Download failed: " + task.getException().getMessage());
            });

            controller.setDownloadTask(task);
            com.org.ollamafx.App.getExecutorService().submit(task);
            stage.showAndWait();

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void createNewChat() {
        if (mainController != null) {
            mainController.createNewChat();
        } else {
            System.err.println("MainController not valid in HomeController");
        }
    }

    @FXML
    private void loadFile() {
        System.out.println("Load File action triggered!");
        // TODO: Implement file loading logic (Document Manager?)
    }

    @FXML
    private HBox recentChatsContainer;

    private void updateRecentChats() {
        if (recentChatsContainer == null)
            return;

        recentChatsContainer.getChildren().clear();

        var sessions = com.org.ollamafx.manager.ChatManager.getInstance().getChatSessions();
        // Take top 10 most recent
        var recent = sessions.stream().limit(10).toList();

        if (recent.isEmpty()) {
            javafx.scene.control.Label emptyLbl = new javafx.scene.control.Label("No recent chats.");
            emptyLbl.getStyleClass().add("apple-text-subtle");
            recentChatsContainer.getChildren().add(emptyLbl);
        } else {
            for (var session : recent) {
                // Simple Card for Chat
                HBox card = new HBox();
                card.getStyleClass().add("apple-card-row");
                card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                card.setSpacing(15);
                card.setPadding(new javafx.geometry.Insets(15));
                card.setStyle("-fx-min-width: 200px; -fx-cursor: hand;");

                javafx.scene.layout.VBox info = new javafx.scene.layout.VBox();
                info.setSpacing(5);

                javafx.scene.control.Label name = new javafx.scene.control.Label(session.getName());
                name.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-fg-default;");

                javafx.scene.control.Label model = new javafx.scene.control.Label(session.getModelName());
                model.getStyleClass().add("apple-text-subtle");

                info.getChildren().addAll(name, model);
                card.getChildren().add(info);

                // On click -> open chat
                // We need MainController to load chat view by session
                // MainController needs a public method `loadChat(Session)`?
                // It has `loadChatView(Session)` but it's private.
                // Let's rely on selection model in MainController!
                card.setOnMouseClicked(e -> {
                    // Select in list view, main controller listener will handle it
                    // But we don't have access to list view directly.
                    // But if we select it in ChatManager? No, ChatManager logic is data.
                    // We need MainController to expose `openChat(session)`
                    // OR we rely on MainController being injected.
                    // Let's add openChat to MainController as well.
                    if (mainController != null) {
                        mainController.openChat(session);
                    }
                });

                recentChatsContainer.getChildren().add(card);
            }
        }
    }

    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        // Also update recent chats when controller is set (so we have reference for
        // navigation)
        // But better to update when view is shown or data changes.
        // Listen to ChatManager changes?
        com.org.ollamafx.manager.ChatManager.getInstance().getChatSessions()
                .addListener((ListChangeListener<com.org.ollamafx.model.ChatSession>) c -> {
                    Platform.runLater(this::updateRecentChats);
                });
        updateRecentChats();
    }

    @FXML
    private void scrollToPopular() {
        // Scroll to popular section
        if (popularContainer.getParent() != null && popularContainer.getParent().getParent() instanceof ScrollPane) {
            popularContainer.requestFocus();
        }
    }

    @FXML
    private void viewMore() {
        if (mainController != null) {
            mainController.showAvailableModels();
        } else {
            System.err.println("MainController not valid in HomeController");
        }
    }
}
