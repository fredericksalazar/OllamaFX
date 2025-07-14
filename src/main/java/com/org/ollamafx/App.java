// src/main/java/com/org/ollamafx/App.java
package com.org.ollamafx;

import com.org.ollamafx.controller.MainController;
import com.org.ollamafx.manager.ModelManager; // <-- AÑADE ESTE IMPORT
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 1. Crear la instancia ÚNICA de nuestro gestor de modelos.
        ModelManager modelManager = new ModelManager();
        // 2. Iniciar la carga de datos en segundo plano INMEDIATAMENTE.
        modelManager.loadAllModels();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/main_view.fxml"));
        Scene scene = new Scene(loader.load());

        // 3. Obtener el controlador principal que se acaba de crear.
        MainController mainController = loader.getController();
        // 4. "Inyectar" nuestro gestor de modelos en el controlador principal.
        mainController.initModelManager(modelManager);

        primaryStage.setTitle("OllamaFX");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}