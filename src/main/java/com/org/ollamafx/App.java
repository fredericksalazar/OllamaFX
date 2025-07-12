// App.java
package com.org.ollamafx;

import com.org.ollamafx.controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        URL fxmlLocation = getClass().getResource("/ui/main_view.fxml");

        System.out.println("Intentando cargar FXML desde: " + fxmlLocation);

        if (fxmlLocation == null) {
            throw new IllegalStateException("Recurso FXML no encontrado. Verifique la ruta y el empaquetado.");
        }

        FXMLLoader loader = new FXMLLoader(fxmlLocation); // Usar la URL resuelta
        Scene scene = new Scene(loader.load());

        MainController controller = loader.getController();
        controller.setHostServices(getHostServices());

        primaryStage.setTitle("OllamaFX");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}