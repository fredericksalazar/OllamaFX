package com.org.ollamafx.controller;

import com.org.ollamafx.manager.ModelManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    // --- Componentes FXML ---
    @FXML
    private BorderPane mainBorderPane;
    @FXML
    private ListView<String> chatListView;

    // --- Campos de la clase ---
    private ObservableList<String> chatItems;
    private ModelManager modelManager; // Referencia al gestor de estado central.

    public MainController() {
        System.out.println("MainController instantiated!");
    }

    /**
     * Método para recibir el gestor desde App.java y cargar la vista inicial.
     * Se ejecuta DESPUÉS de initialize().
     */
    public void initModelManager(ModelManager modelManager) {
        this.modelManager = modelManager;
        // Una vez que tenemos el gestor, es seguro cargar la vista por defecto.
        showAvailableModels();
    }

    /**
     * Se ejecuta automáticamente después de que el FXML es cargado,
     * pero ANTES de que initModelManager sea llamado.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("MainController initialized!");

        chatItems = FXCollections.observableArrayList();
        chatListView.setItems(chatItems);

        // La carga de la vista inicial se movió a initModelManager() para evitar errores.

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

    /**
     * Lógica para crear un nuevo chat.
     */
    @FXML
    private void createNewChat() {
        System.out.println("Creating new chat...");
        String newChatName = "Chat " + (chatItems.size() + 1);
        chatItems.add(newChatName);
        chatListView.getSelectionModel().select(newChatName);
        loadChatView(newChatName);
    }

    /**
     * Carga la vista de un chat específico.
     */
    private void loadChatView(String chatName) {
        try {
            System.out.println("Loading Chat View for: " + chatName);
            StackPane placeholder = new StackPane(new Label("Chat con: " + chatName));
            mainBorderPane.setCenter(placeholder);
        } catch (Exception e) {
            System.err.println("Error loading chat view: " + e.getMessage());
            e.printStackTrace();
        }
    }
}