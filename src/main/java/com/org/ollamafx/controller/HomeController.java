package com.org.ollamafx.controller;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;

import org.kordamp.ikonli.javafx.FontIcon;

import com.org.ollamafx.App;
import com.org.ollamafx.manager.ChatManager;
import com.org.ollamafx.manager.ConfigManager;
import com.org.ollamafx.manager.ModelManager;
import com.org.ollamafx.manager.OllamaManager;
import com.org.ollamafx.model.ChatSession;
import com.org.ollamafx.model.OllamaModel;
import com.org.ollamafx.service.GitHubUpdateService;
import com.org.ollamafx.service.UpdateManagerService;
import com.org.ollamafx.ui.ModelCard;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
public class HomeController implements Initializable {

    @FXML
    private HBox recommendedContainer;
    @FXML
    private HBox popularContainer;
    @FXML
    private HBox newContainer;
    @FXML
    private Button btnExplore;
    @FXML
    private Button themeToggleButton;
    @FXML
    private FontIcon themeToggleIcon;

    private MainController mainController;

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
        applyCurrentThemeIcon();
        
        // Trigger update checking on startup
        Platform.runLater(() -> {
            checkPendingUpdates();
            checkForUpdates();
        });
    }

    private void applyCurrentThemeIcon() {
        if (themeToggleIcon != null) {
            String currentTheme = ConfigManager.getInstance().getTheme();
            if ("light".equals(currentTheme)) {
                themeToggleIcon.setIconLiteral("fas-moon");
            } else {
                themeToggleIcon.setIconLiteral("fas-sun");
            }
        }
    }

    @FXML
    private void toggleTheme() {
        if (mainController != null) {
            mainController.toggleTheme();
            applyCurrentThemeIcon();
        }
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

    private void updateCarousel(HBox container, List<? extends OllamaModel> models) {
        container.getChildren().clear();

        if (models.isEmpty()) {
            Label emptyLbl = new Label(
                    App.getBundle().getString("home.noModels"));
            emptyLbl.getStyleClass().add("apple-text-subtle");
            emptyLbl.setPadding(new Insets(20));
            container.getChildren().add(emptyLbl);
            return;
        }

        // Limit to top 20 models to reduce CPU/memory usage
        int limit = Math.min(models.size(), 20);

        for (int i = 0; i < limit; i++) {
            OllamaModel model = models.get(i);
            boolean isInstalled = modelManager.isModelInstalled(model.getName(), model.getTag());
            ModelCard card = new ModelCard(model, isInstalled,
                    () -> handleInstall(model),
                    () -> handleUninstall(model));
            container.getChildren().add(card);
        }
    }

    private void handleInstall(OllamaModel model) {
        showDownloadPopup(model);
    }

    private void handleUninstall(OllamaModel model) {
        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION);
        alert.setTitle(App.getBundle().getString("local.uninstall.title"));
        alert.setHeaderText(MessageFormat
                .format(App.getBundle().getString("local.uninstall.header"), model.getName()));
        alert.setContentText(App.getBundle().getString("local.uninstall.content"));

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                modelManager.deleteModel(model.getName(), model.getTag());
            }
        });
    }

    private void showDownloadPopup(OllamaModel model) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ui/download_popup.fxml"));
            loader.setResources(App.getBundle());

            Parent root = loader.load();
            DownloadPopupController controller = loader.getController();
            controller.setModelName(model.getName() + ":" + model.getTag());

            String userAgentStylesheet = Application.getUserAgentStylesheet();
            if (userAgentStylesheet != null && userAgentStylesheet.toLowerCase().contains("light")) {
                root.getStyleClass().add("light");
            } else {
                root.getStyleClass().add("dark");
            }

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setTitle(App.getBundle().getString("download.title.default"));

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);
            stage.setResizable(false);

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    updateMessage(App.getBundle().getString("download.status.process"));
                    updateProgress(0, 100);

                    OllamaManager.getInstance().pullModel(model.getName(), model.getTag(),
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
                if (modelManager != null) {
                    String date = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                            .format(LocalDateTime.now());
                    OllamaModel newModel = new OllamaModel(
                            model.getName(), App.getBundle().getString("model.installed"), "N/A",
                            model.getTag(),
                            model.getSize(), date,
                            "N/A", "N/A"); // simplified
                    modelManager.addLocalModel(newModel);

                    modelManager.loadLibraryModels();
                }
            });

            task.setOnFailed(e -> {
                System.err.println("Download failed: " + task.getException().getMessage());
            });

            controller.setDownloadTask(task);
            App.getExecutorService().submit(task);
            stage.showAndWait();

        } catch (IOException e) {
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
    private void createNewFolder() {
        if (mainController != null) {
            mainController.createNewFolder();
        }
    }

    @FXML
    private void loadFile() {
        // TODO: Implement file loading logic (Document Manager?)
    }

    @FXML
    public void openPayPal() {
        String url = "https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=fredefass01@gmail.com&item_name=OllamaFX+Support&amount=10.00&currency_code=USD";
        if (App.getAppHostServices() != null) {
            App.getAppHostServices().showDocument(url);
        }
    }

    @FXML
    public void openBuyMeACoffee() {
        String url = "https://www.buymeacoffee.com/fredericksalazar";
        if (App.getAppHostServices() != null) {
            App.getAppHostServices().showDocument(url);
        }
    }

    @FXML
    private HBox recentChatsContainer;

    private void updateRecentChats() {
        if (recentChatsContainer == null)
            return;

        recentChatsContainer.getChildren().clear();

        var sessions = ChatManager.getInstance().getChatSessions();
        var recent = sessions.stream().limit(10).toList();

        if (recent.isEmpty()) {
            Label emptyLbl = new Label("No recent chats.");
            emptyLbl.getStyleClass().add("apple-text-subtle");
            recentChatsContainer.getChildren().add(emptyLbl);
        } else {
            for (var session : recent) {
                HBox card = new HBox();
                card.getStyleClass().add("apple-card-row");
                card.setAlignment(Pos.CENTER_LEFT);
                card.setSpacing(15);
                card.setPadding(new Insets(15));
                card.setStyle("-fx-min-width: 200px; -fx-cursor: hand;");

                VBox info = new VBox();
                info.setSpacing(5);

                Label name = new Label(session.getName());
                name.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-fg-default;");

                Label model = new Label(session.getModelName());
                model.getStyleClass().add("apple-text-subtle");

                info.getChildren().addAll(name, model);
                card.getChildren().add(info);

                card.setOnMouseClicked(e -> {
                    if (mainController != null) {
                        mainController.openChat(session);
                    }
                });

                recentChatsContainer.getChildren().add(card);
            }
        }
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        ChatManager.getInstance().getChatSessions()
                .addListener((ListChangeListener<ChatSession>) c -> {
                    Platform.runLater(this::updateRecentChats);
                });
        updateRecentChats();
    }

    @FXML
    private void scrollToPopular() {
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

    private void checkForUpdates() {
        GitHubUpdateService updateService = new GitHubUpdateService();
        updateService.fetchLatestRelease().thenAccept(releaseInfo -> {
            String currentVersion = getAppVersion();
            if (updateService.isUpdateAvailable(currentVersion, releaseInfo.versionTag)) {
                Platform.runLater(() -> showUpdateDialog(releaseInfo));
            }
        }).exceptionally(ex -> {
            System.err.println("Failed to check for updates: " + ex.getMessage());
            return null;
        });
    }

    private String getAppVersion() {
        try (InputStream input = getClass().getResourceAsStream("/app.properties")) {
            if (input != null) {
                Properties prop = new Properties();
                prop.load(input);
                return prop.getProperty("app.version", "Unknown");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Unknown";
    }

    private void showUpdateDialog(GitHubUpdateService.ReleaseInfo releaseInfo) {
        Alert updateAlert = new Alert(Alert.AlertType.CONFIRMATION);
        updateAlert.setTitle(App.getBundle().getString("update.available.title"));
        updateAlert.setHeaderText(MessageFormat.format(App.getBundle().getString("update.available.header"), releaseInfo.versionTag));
        updateAlert.setContentText(MessageFormat.format(App.getBundle().getString("update.available.content"), releaseInfo.versionTag, releaseInfo.releaseNotes));

        ButtonType btnDownload = new ButtonType(App.getBundle().getString("update.button.download"), ButtonBar.ButtonData.OK_DONE);
        ButtonType btnLater = new ButtonType(App.getBundle().getString("update.button.later"), ButtonBar.ButtonData.CANCEL_CLOSE);
        
        updateAlert.getButtonTypes().setAll(btnDownload, btnLater);

        updateAlert.showAndWait().ifPresent(type -> {
            if (type == btnDownload) {
                performUpdate(releaseInfo.downloadUrl);
            }
        });
    }

    private void performUpdate(String zipUrl) {
        UpdateManagerService updateManager = new UpdateManagerService();
        
        // Create a Progress Dialog
        Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
        progressAlert.setTitle(App.getBundle().getString("update.progress.title"));
        progressAlert.setHeaderText(App.getBundle().getString("update.progress.header"));
        
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        
        VBox content = new VBox(10, new Label("Downloading..."), progressBar);
        progressAlert.getDialogPane().setContent(content);
        progressAlert.getButtonTypes().clear(); // Remove OK button while downloading
        
        progressAlert.show();

        updateManager.downloadUpdate(zipUrl, progress -> {
            Platform.runLater(() -> progressBar.setProgress(progress));
        }).thenRun(() -> {
            Platform.runLater(() -> {
                progressAlert.setHeaderText(App.getBundle().getString("update.progress.ready"));
                content.getChildren().set(0, new Label(App.getBundle().getString("update.progress.readyContent")));
                progressBar.setProgress(1.0);
                
                ButtonType btnRestart = new ButtonType("Restart", ButtonBar.ButtonData.OK_DONE);
                progressAlert.getButtonTypes().setAll(btnRestart);
                
                progressAlert.setOnHidden(evt -> {
                    try {
                        updateManager.extractUpdate();
                        updateManager.backupCurrentVersion();
                        updateManager.applyAndRestart();
                    } catch (IOException e) {
                        e.printStackTrace();
                        showErrorDialog();
                    }
                });
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                progressAlert.close();
                showErrorDialog();
            });
            return null;
        });
    }
    
    private void showErrorDialog() {
        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
        errorAlert.setTitle(App.getBundle().getString("update.error.title"));
        errorAlert.setHeaderText(null);
        errorAlert.setContentText(App.getBundle().getString("update.error.content"));
        errorAlert.showAndWait();
    }

    private void checkPendingUpdates() {
        System.out.println("[HomeController] checkPendingUpdates() invoked.");
        UpdateManagerService updateManager = new UpdateManagerService();
        java.nio.file.Path newJarPath = java.nio.file.Paths.get(updateManager.getNewDir(), "lib", "OllamaFX.jar");
        java.nio.file.Path currentJarPath = java.nio.file.Paths.get(updateManager.getAppRoot(), "lib", "OllamaFX.jar");

        System.out.println("[HomeController] Looking for new JAR at: " + newJarPath.toAbsolutePath());
        System.out.println("[HomeController] Current JAR is at: " + currentJarPath.toAbsolutePath());

        try {
            if (java.nio.file.Files.exists(newJarPath) && java.nio.file.Files.exists(currentJarPath)) {
                System.out.println("[HomeController] Both JARs exist.");
                long newSize = java.nio.file.Files.size(newJarPath);
                long currentSize = java.nio.file.Files.size(currentJarPath);
                System.out.println("[HomeController] New JAR size: " + newSize + ", Current JAR size: " + currentSize);

                if (newSize == currentSize) {
                    // Update was already applied or is identical, clean up
                    System.out.println("[HomeController] Sizes are identical. Cleaning up previously applied update files.");
                    updateManager.deleteFolder(java.nio.file.Paths.get(updateManager.getNewDir()));
                    java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(updateManager.getTempZip()));
                } else {
                    // Pending update detected
                    System.out.println("[HomeController] Sizes differ. Pending update detected! Prompting user.");
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle(App.getBundle().getString("update.pending.title"));
                    alert.setHeaderText(App.getBundle().getString("update.pending.header"));
                    alert.setContentText(App.getBundle().getString("update.pending.content"));

                    ButtonType btnApply = new ButtonType(App.getBundle().getString("update.button.apply"), ButtonBar.ButtonData.OK_DONE);
                    ButtonType btnLater = new ButtonType(App.getBundle().getString("update.button.later"), ButtonBar.ButtonData.CANCEL_CLOSE);

                    alert.getButtonTypes().setAll(btnApply, btnLater);

                    alert.showAndWait().ifPresent(type -> {
                        if (type == btnApply) {
                            System.out.println("[HomeController] User selected 'Apply Update'. Triggering applyAndRestart()");
                            try {
                                updateManager.backupCurrentVersion();
                                updateManager.applyAndRestart();
                            } catch (Exception e) {
                                e.printStackTrace();
                                showErrorDialog();
                            }
                        } else {
                            // User clicked later, clean up so we don't ask again until they redownload
                            System.out.println("[HomeController] User selected 'Later'. Cleaning up update/new directory.");
                            try {
                                updateManager.deleteFolder(java.nio.file.Paths.get(updateManager.getNewDir()));
                                java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(updateManager.getTempZip()));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            } else if (java.nio.file.Files.exists(java.nio.file.Paths.get(updateManager.getNewDir()))) {
                 // Incomplete download or bad extraction, clean up
                 System.out.println("[HomeController] Incomplete download or bad extraction. Cleaning up update/new directory.");
                 updateManager.deleteFolder(java.nio.file.Paths.get(updateManager.getNewDir()));
                 java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(updateManager.getTempZip()));
            } else {
                 System.out.println("[HomeController] No pending update detected.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to check for pending updates: " + e.getMessage());
        }
    }
}
