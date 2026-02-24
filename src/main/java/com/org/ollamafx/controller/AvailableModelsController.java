package com.org.ollamafx.controller;

import com.org.ollamafx.manager.ModelManager;
import com.org.ollamafx.model.OllamaModel;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.TextField;
import javafx.scene.control.ListCell;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javafx.beans.binding.Bindings;
import com.org.ollamafx.App;

import java.io.IOException;
import java.util.List;

public class AvailableModelsController {

    @FXML
    private ListView<OllamaModel> modelListView;
    @FXML
    private TextField searchField;
    @FXML
    private StackPane detailViewContainer;
    @FXML
    private Label resultsLabel;

    // Compatibility filters
    @FXML
    private ToggleButton filterRecommended;

    // Capability filters
    @FXML
    private ToggleButton filterVision;
    @FXML
    private ToggleButton filterTools;
    @FXML
    private ToggleButton filterCode;

    private FilteredList<OllamaModel> filteredModels;
    private ModelManager modelManager;
    private PauseTransition selectionDebounce;

    public void setModelManager(ModelManager modelManager) {
        this.modelManager = modelManager;

        filteredModels = new FilteredList<>(modelManager.getAvailableModels(), p -> true);
        modelListView.setItems(filteredModels);

        setupFilters();
        setupSimpleListCell();
        setupSelectionLogic();
        setupEmptyState();
        updateResultsLabel();

        filteredModels.predicateProperty().addListener((obs, oldP, newP) -> updateResultsLabel());
    }

    private void setupFilters() {
        // Search filter
        searchField.textProperty().addListener((obs, old, newVal) -> applyFilters());

        // Toggle filters
        filterRecommended.selectedProperty().addListener((obs, old, sel) -> applyFilters());
        filterVision.selectedProperty().addListener((obs, old, sel) -> applyFilters());
        filterTools.selectedProperty().addListener((obs, old, sel) -> applyFilters());
        filterCode.selectedProperty().addListener((obs, old, sel) -> applyFilters());
    }

    private void applyFilters() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();

        boolean hasRecommendedFilter = filterRecommended.isSelected();
        boolean hasCapabilityFilter = filterVision.isSelected() || filterTools.isSelected() || filterCode.isSelected();

        filteredModels.setPredicate(model -> {
            // Search by name - always applies
            if (!searchText.isEmpty() && !model.getName().toLowerCase().contains(searchText)) {
                return false;
            }

            // Recommended filter
            if (hasRecommendedFilter) {
                modelManager.classifyModel(model);
                if (model.getCompatibilityStatus() != OllamaModel.CompatibilityStatus.RECOMMENDED) {
                    return false;
                }
            }

            // Capability filter (OR within category)
            if (hasCapabilityFilter) {
                List<String> badges = model.getBadges();
                String nameLower = model.getName().toLowerCase();
                boolean matchesCap = false;
                if (filterVision.isSelected() && badges.stream().anyMatch(b -> b.toLowerCase().contains("vision")))
                    matchesCap = true;
                if (filterTools.isSelected() && badges.stream().anyMatch(b -> b.toLowerCase().contains("tool")))
                    matchesCap = true;
                if (filterCode.isSelected() && (badges.stream().anyMatch(b -> b.toLowerCase().contains("code")) ||
                        nameLower.contains("code") || nameLower.contains("coder") || nameLower.contains("starcoder") ||
                        nameLower.contains("codellama") || nameLower.contains("deepseek-coder")))
                    matchesCap = true;
                if (!matchesCap)
                    return false;
            }

            return true;
        });
    }

    private void updateResultsLabel() {
        int count = filteredModels.size();
        resultsLabel.setText(count + " " + (count == 1 ? "modelo" : "modelos"));
    }

    private void setupEmptyState() {
        Label placeholder = new Label();
        placeholder.textProperty().bind(Bindings.createStringBinding(
                () -> {
                    String search = searchField.getText();
                    boolean hasAnyFilter = filterRecommended.isSelected() || filterVision.isSelected() ||
                            filterTools.isSelected() || filterCode.isSelected();

                    if ((search != null && !search.isEmpty()) || hasAnyFilter) {
                        return App.getBundle().getString("available.empty.no_match");
                    }
                    return App.getBundle().getString("available.empty.connecting");
                },
                searchField.textProperty(),
                filterRecommended.selectedProperty(),
                filterVision.selectedProperty(),
                filterTools.selectedProperty(),
                filterCode.selectedProperty()));
        placeholder.setStyle("-fx-text-fill: -color-fg-muted;");
        modelListView.setPlaceholder(placeholder);
    }

    // Simple, clean list cell - just the model name
    private void setupSimpleListCell() {
        modelListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(OllamaModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getName());
                    setGraphic(null);
                }
            }
        });
    }

    private void setupSelectionLogic() {
        selectionDebounce = new PauseTransition(Duration.millis(100));
        selectionDebounce.setOnFinished(event -> {
            OllamaModel selected = modelListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                displayModelDetails(selected);
            }
        });

        modelListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectionDebounce.playFromStart();
            } else {
                detailViewContainer.getChildren().clear();
            }
        });
    }

    private void displayModelDetails(OllamaModel selectedModel) {
        try {
            if (modelManager == null) {
                showErrorInView("Error", "ModelManager is null");
                return;
            }
            List<OllamaModel> details = modelManager.getModelDetails(selectedModel.getName());
            showDetailsInView(details);
        } catch (Exception e) {
            showErrorInView("Error", "Failed to load model details.");
        }
    }

    private void showDetailsInView(List<OllamaModel> modelTags) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/model_detail_view.fxml"));
            Parent detailView = loader.load();
            ModelDetailController controller = loader.getController();
            controller.setModelManager(this.modelManager);
            controller.populateDetails(modelTags);
            detailViewContainer.getChildren().setAll(detailView);
        } catch (IOException e) {
            // Silently handle
        }
    }

    private void showErrorInView(String title, String message) {
        VBox errorBox = new VBox(10);
        errorBox.setAlignment(Pos.CENTER);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px;");
        titleLabel.setTextFill(Color.RED);
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        errorBox.getChildren().addAll(titleLabel, messageLabel);
        detailViewContainer.getChildren().setAll(errorBox);
    }

    @FXML
    private void sortByNameAsc() {
        modelListView.getItems().sort((m1, m2) -> m1.getName().compareToIgnoreCase(m2.getName()));
    }

    @FXML
    private void sortByNameDesc() {
        modelListView.getItems().sort((m1, m2) -> m2.getName().compareToIgnoreCase(m1.getName()));
    }
}