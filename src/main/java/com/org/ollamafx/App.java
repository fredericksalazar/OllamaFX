// src/main/java/com/org/ollamafx/App.java
package com.org.ollamafx;

import com.org.ollamafx.controller.MainController;
import com.org.ollamafx.manager.ModelManager; // <-- AÑADE ESTE IMPORT
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException; // Added for IOException
import com.org.ollamafx.manager.ChatManager;

public class App extends Application {

    private static java.util.concurrent.ExecutorService executorService;

    public static java.util.concurrent.ExecutorService getExecutorService() {
        return executorService;
    }

    @Override
    public void init() throws Exception {
        // Initialize the global executor service
        // CachedThreadPool is good for many short-lived tasks (like UI updates or small
        // requests)
        // but for heavy lifting like model downloads, we might want to limit
        // concurrency
        // elsewhere or here.
        // For a desktop app, CachedThreadPool is usually fine as we don't expect
        // massive concurrency.
        executorService = java.util.concurrent.Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true); // Ensure threads don't prevent app shutdown
            return t;
        });
    }

    public static java.util.ResourceBundle getBundle() {
        // Create a new Locale each time to ensure we get the correct one based on
        // current config/preference
        String lang = com.org.ollamafx.manager.ConfigManager.getInstance().getLanguage();

        java.util.Locale locale = new java.util.Locale(lang);
        java.util.Locale.setDefault(locale); // CRITICAL: Force system default to match preference to avoid fallback
                                             // issues

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
            // Optional: Show error dialog safely
            Platform.runLater(() -> {
                com.org.ollamafx.util.Utils.showError("Critical Error", "An occurred error: " + throwable.getMessage());
            });
        });

        primaryStage = stage;

        ChatManager.getInstance().loadChats();

        // Hardware Information Logging
        System.out.println(com.org.ollamafx.manager.HardwareManager.getHardwareDetails());

        // Set AtlantaFX theme
        Application.setUserAgentStylesheet(new atlantafx.base.theme.CupertinoLight().getUserAgentStylesheet());

        modelManager = ModelManager.getInstance();
        // Don't load all models here anymore, Splash/LibraryManager handles it.
        // modelManager.loadAllModels();

        // LOAD SPLASH SCREEN
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/ui/splash_view.fxml"));
        // Splash view might not need resources bundle if mostly hardcoded or unified,
        // but good practice:
        // loader.setResources(getBundle());
        javafx.scene.Parent root = loader.load();

        Scene scene = new Scene(root);
        // Add CSS if splash needs specific styling, or just reuse active
        scene.getStylesheets().add(App.class.getResource("/css/ollama_active.css").toExternalForm());

        stage.setScene(scene);
        stage.setTitle(getBundle().getString("app.title"));

        // Icon logic (reused)
        try {
            stage.getIcons().add(new javafx.scene.image.Image(App.class.getResourceAsStream("/icons/icon.png")));
        } catch (Exception e) {
        }

        stage.show();
    }

    public static void reloadUI() {
        try {
            // Clear ResourceBundle cache to ensure new language is loaded
            java.util.ResourceBundle.clearCache();

            FXMLLoader loader = new FXMLLoader(App.class.getResource("/ui/main_view.fxml"));
            loader.setResources(getBundle());
            javafx.scene.Parent root = loader.load();

            // Add custom style class
            root.getStyleClass().add("light");

            Scene scene = new Scene(root);
            scene.getStylesheets().add(App.class.getResource("/css/ollama_active.css").toExternalForm());

            // Inject ModelManager into MainController
            MainController controller = loader.getController();
            controller.initModelManager(modelManager);

            primaryStage.setScene(scene);
            primaryStage.setTitle(getBundle().getString("app.title"));

            // Set App Icon
            try {
                primaryStage.getIcons()
                        .add(new javafx.scene.image.Image(App.class.getResourceAsStream("/icons/icon.png")));
                // For Mac Dock Icon in dev mode (often requires Taskbar usage in AWT or
                // specific FX hooks, but Stage icon is the first step)
                if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                    java.awt.Taskbar.getTaskbar().setIconImage(
                            java.awt.Toolkit.getDefaultToolkit().getImage(App.class.getResource("/icons/icon.png")));
                }
            } catch (Exception e) {
                // Ignore icon load error
                System.out.println("Failed to load icon: " + e.getMessage());
            }

            primaryStage.setMaximized(true); // Ensure maximized on reload

            if (!primaryStage.isShowing()) {
                primaryStage.show();
            }
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

        // Ensure we stop any managed Ollama process
        com.org.ollamafx.manager.OllamaServiceManager.getInstance().stopOllama();

        ChatManager.getInstance().saveChats();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}