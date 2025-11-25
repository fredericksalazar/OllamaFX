// src/main/java/com/org/ollamafx/App.java
package com.org.ollamafx;

import atlantafx.base.theme.PrimerDark;
import com.org.ollamafx.controller.MainController;
import com.org.ollamafx.manager.ModelManager; // <-- AÑADE ESTE IMPORT
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

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

    public static void main(String[] args) {
        launch(args);
    }
}