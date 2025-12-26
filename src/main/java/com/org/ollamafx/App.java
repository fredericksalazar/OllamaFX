// src/main/java/com/org/ollamafx/App.java
package com.org.ollamafx;

import com.org.ollamafx.controller.MainController;
import com.org.ollamafx.manager.ModelLibraryManager;
import com.org.ollamafx.manager.ModelManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import com.org.ollamafx.manager.ChatManager;

public class App extends Application {

    private static java.util.concurrent.ExecutorService executorService;

    public static java.util.concurrent.ExecutorService getExecutorService() {
        return executorService;
    }

    @Override
    public void init() throws Exception {
        executorService = java.util.concurrent.Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
    }

    public static java.util.ResourceBundle getBundle() {
        String lang = com.org.ollamafx.manager.ConfigManager.getInstance().getLanguage();
        java.util.Locale locale = new java.util.Locale(lang);
        java.util.Locale.setDefault(locale);
        return java.util.ResourceBundle.getBundle("messages", locale);
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
                com.org.ollamafx.util.Utils.showError("Critical Error", "An error occurred: " + throwable.getMessage());
            });
        });

        primaryStage = stage;

        ChatManager.getInstance().loadChats();
        System.out.println(com.org.ollamafx.manager.HardwareManager.getHardwareDetails());
        Application.setUserAgentStylesheet(new atlantafx.base.theme.CupertinoLight().getUserAgentStylesheet());

        modelManager = ModelManager.getInstance();

        // === DECISION: Splash Screen OR Main UI? ===
        ModelLibraryManager.UpdateStatus cacheStatus = ModelLibraryManager.getInstance().getUpdateStatus();

        boolean needsSplash = (cacheStatus == ModelLibraryManager.UpdateStatus.OUTDATED_HARD);

        System.out.println("App: Cache status = " + cacheStatus + ", needsSplash = " + needsSplash);

        if (needsSplash) {
            // Cache is missing or expired (> 10 days) -> Show Splash Screen
            loadSplashScreen();
        } else {
            // Cache is valid -> Go directly to Main UI
            loadMainUI();
        }

        // Icon
        try {
            stage.getIcons().add(new javafx.scene.image.Image(App.class.getResourceAsStream("/icons/icon.png")));
        } catch (Exception e) {
            // Ignore
        }

        stage.show();
    }

    private void loadSplashScreen() throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/ui/splash_view.fxml"));
        javafx.scene.Parent root = loader.load();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(App.class.getResource("/css/ollama_active.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setTitle(getBundle().getString("app.title"));
    }

    private void loadMainUI() throws IOException {
        java.util.ResourceBundle.clearCache();

        FXMLLoader loader = new FXMLLoader(App.class.getResource("/ui/main_view.fxml"));
        loader.setResources(getBundle());
        javafx.scene.Parent root = loader.load();
        root.getStyleClass().add("light");

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
            java.util.ResourceBundle.clearCache();

            FXMLLoader loader = new FXMLLoader(App.class.getResource("/ui/main_view.fxml"));
            loader.setResources(getBundle());
            javafx.scene.Parent root = loader.load();
            root.getStyleClass().add("light");

            Scene scene = new Scene(root);
            scene.getStylesheets().add(App.class.getResource("/css/ollama_active.css").toExternalForm());

            MainController controller = loader.getController();
            controller.initModelManager(modelManager);

            primaryStage.setScene(scene);
            primaryStage.setTitle(getBundle().getString("app.title"));

            try {
                primaryStage.getIcons()
                        .add(new javafx.scene.image.Image(App.class.getResourceAsStream("/icons/icon.png")));
                if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                    java.awt.Taskbar.getTaskbar().setIconImage(
                            java.awt.Toolkit.getDefaultToolkit().getImage(App.class.getResource("/icons/icon.png")));
                }
            } catch (Exception e) {
                System.out.println("Failed to load icon: " + e.getMessage());
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
            java.io.File detailsCache = new java.io.File(System.getProperty("user.home"),
                    ".ollamafx/details_cache.json");
            if (detailsCache.exists()) {
                detailsCache.delete();
                System.out.println("App: Deleted details_cache.json for refresh");
            }

            // CRITICAL: Invalidate in-memory cache to force OUTDATED_HARD status
            com.org.ollamafx.manager.ModelLibraryManager.getInstance().invalidateCache();

            FXMLLoader loader = new FXMLLoader(App.class.getResource("/ui/splash_view.fxml"));
            loader.setResources(getBundle());
            javafx.scene.Parent root = loader.load();

            Scene scene = new Scene(root);
            scene.getStylesheets().add(App.class.getResource("/css/splash.css").toExternalForm());
            scene.getStylesheets().add(App.class.getResource("/css/ollama_active.css").toExternalForm());

            primaryStage.setScene(scene);
            primaryStage.setTitle(getBundle().getString("app.title"));
            primaryStage.show();

            System.out.println("App: Loaded Splash Screen for library refresh");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() throws Exception {
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(800, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }

        com.org.ollamafx.manager.OllamaServiceManager.getInstance().stopOllama();
        ChatManager.getInstance().saveChats();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}