// src/main/java/com/org/ollamafx/App.java
package com.org.ollamafx;

import com.org.ollamafx.controller.MainController;
import com.org.ollamafx.manager.ChatManager;
import com.org.ollamafx.manager.ConfigManager;
import com.org.ollamafx.manager.ModelLibraryManager;
import com.org.ollamafx.manager.ModelManager;
import com.org.ollamafx.manager.OllamaServiceManager;
import com.org.ollamafx.util.Utils;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import atlantafx.base.theme.CupertinoDark;
import atlantafx.base.theme.CupertinoLight;

import java.awt.Taskbar;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class App extends Application {

    private static ExecutorService executorService;
    private static HostServices hostServices;

    public static ExecutorService getExecutorService() {
        return executorService;
    }

    public static HostServices getAppHostServices() {
        return hostServices;
    }

    @Override
    public void init() throws Exception {
        executorService = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
    }

    public static ResourceBundle getBundle() {
        String lang = ConfigManager.getInstance().getLanguage();
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        return ResourceBundle.getBundle("messages", locale);
    }

    private static Stage primaryStage;
    private static ModelManager modelManager;

    @Override
    public void start(Stage stage) throws IOException {
        // Global Exception Handler
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("CRITICAL UNCAUGHT EXCEPTION on thread " + thread.getName());
            throwable.printStackTrace();
            Platform.runLater(() -> {
                Utils.showError("Critical Error", "An error occurred: " + throwable.getMessage());
            });
        });

        primaryStage = stage;
        hostServices = getHostServices();

        ChatManager.getInstance().loadChats();

        // Apply saved theme
        String savedTheme = ConfigManager.getInstance().getTheme();
        if ("dark".equals(savedTheme)) {
            Application.setUserAgentStylesheet(new CupertinoDark().getUserAgentStylesheet());
        } else {
            Application.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());
        }

        modelManager = ModelManager.getInstance();

        // === DECISION: Splash Screen OR Main UI? ===
        ModelLibraryManager.UpdateStatus cacheStatus = ModelLibraryManager.getInstance().getUpdateStatus();

        boolean needsSplash = (cacheStatus == ModelLibraryManager.UpdateStatus.OUTDATED_HARD);

        if (needsSplash) {
            // Cache is missing or expired (> 10 days) -> Show Splash Screen
            loadSplashScreen();
        } else {
            // Cache is valid -> Go directly to Main UI
            loadMainUI();
        }

        // Icon
        try {
            stage.getIcons().add(new Image(App.class.getResourceAsStream("/icons/icon.png")));
        } catch (Exception e) {
            // Ignore
        }

        stage.show();
    }

    private void loadSplashScreen() throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/ui/splash_view.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(App.class.getResource("/css/ollama_active.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setTitle(getBundle().getString("app.title"));
    }

    private void loadMainUI() throws IOException {
        ResourceBundle.clearCache();

        FXMLLoader loader = new FXMLLoader(App.class.getResource("/ui/main_view.fxml"));
        loader.setResources(getBundle());
        Parent root = loader.load();

        String savedTheme = ConfigManager.getInstance().getTheme();
        root.getStyleClass().add(savedTheme);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(App.class.getResource("/css/ollama_active.css").toExternalForm());

        MainController controller = loader.getController();
        controller.initModelManager(modelManager);

        primaryStage.setScene(scene);
        primaryStage.setTitle(getBundle().getString("app.title"));
        primaryStage.setMaximized(true);
    }

    public static void reloadUI() {
        try {
            ResourceBundle.clearCache();

            FXMLLoader loader = new FXMLLoader(App.class.getResource("/ui/main_view.fxml"));
            loader.setResources(getBundle());
            Parent root = loader.load();
            root.getStyleClass().add("light");

            Scene scene = new Scene(root);
            scene.getStylesheets().add(App.class.getResource("/css/ollama_active.css").toExternalForm());

            MainController controller = loader.getController();
            controller.initModelManager(modelManager);

            primaryStage.setScene(scene);
            primaryStage.setTitle(getBundle().getString("app.title"));

            try {
                primaryStage.getIcons()
                        .add(new Image(App.class.getResourceAsStream("/icons/icon.png")));
                if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                    Taskbar.getTaskbar().setIconImage(
                            Toolkit.getDefaultToolkit().getImage(App.class.getResource("/icons/icon.png")));
                }
            } catch (Exception e) {
            }

            primaryStage.setMaximized(true);

            if (!primaryStage.isShowing()) {
                primaryStage.show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Fuerza la recarga iniciando desde el Splash Screen
     */
    public static void reloadFromSplash() {
        try {
            // Delete details cache file
            File detailsCache = new File(System.getProperty("user.home"),
                    ".ollamafx/details_cache.json");
            if (detailsCache.exists()) {
                detailsCache.delete();
            }

            // CRITICAL: Invalidate in-memory cache to force OUTDATED_HARD status
            ModelLibraryManager.getInstance().invalidateCache();

            FXMLLoader loader = new FXMLLoader(App.class.getResource("/ui/splash_view.fxml"));
            loader.setResources(getBundle());
            Parent root = loader.load();

            Scene scene = new Scene(root);
            scene.getStylesheets().add(App.class.getResource("/css/splash.css").toExternalForm());
            scene.getStylesheets().add(App.class.getResource("/css/ollama_active.css").toExternalForm());

            primaryStage.setScene(scene);
            primaryStage.setTitle(getBundle().getString("app.title"));
            primaryStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() throws Exception {
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }

        com.org.ollamafx.manager.RagManager.getInstance().shutdown();
        OllamaServiceManager.getInstance().stopOllama();
        ChatManager.getInstance().saveChats();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}