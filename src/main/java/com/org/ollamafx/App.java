// src/main/java/com/org/ollamafx/App.java
package com.org.ollamafx;

import atlantafx.base.theme.PrimerLight;
import com.org.ollamafx.controller.MainController;
import com.org.ollamafx.manager.ModelManager; // <-- AÑADE ESTE IMPORT
import javafx.application.Application;
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

    @Override
    public void start(Stage primaryStage) throws IOException { // Changed Exception to IOException
        // Load chats
        ChatManager.getInstance().loadChats();

        // Load Fonts
        javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/Ubuntu-Regular.ttf"), 12);
        javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/Ubuntu-Bold.ttf"), 12);

        Application.setUserAgentStylesheet(new atlantafx.base.theme.PrimerLight().getUserAgentStylesheet());

        ModelManager modelManager = new ModelManager();
        modelManager.loadAllModels();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/main_view.fxml"));
        javafx.scene.Parent root = loader.load();
        root.getStyleClass().add("light"); // Force light mode CSS overrides
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/gnome.css").toExternalForm());

        MainController mainController = loader.getController();
        mainController.initModelManager(modelManager);

        primaryStage.setTitle("OllamaFX");
        primaryStage.setScene(scene);
        primaryStage.show();
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
        ChatManager.getInstance().saveChats();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}