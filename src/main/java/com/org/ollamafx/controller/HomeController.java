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

        // Trigger background load
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
        // Integrate with MainController's download or show popup
        // For now, simpler: trigger install via ModelManager (if supported) or just
        // print
        System.out.println("Install requested for: " + model.getName());
        // Ideally we open the DownloadPopup with this model pre-filled
        // But since we don't have direct access to MainController here easily without
        // injection or event bus,
        // we might leave this as a TODO or use a static helper if available.
        // Or better: Use the ModelManager to trigger logic if we add it there.
        // For this task, "Details" is sufficient or a simple console log as per plan
        // scope.
    }

    private void handleDetails(OllamaModel model) {
        System.out.println("Details requested for: " + model.getName());
        // Navigate to details view
    }

    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
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
