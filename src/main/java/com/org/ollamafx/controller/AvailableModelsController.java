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

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AvailableModelsController {

    @FXML
    private ListView<OllamaModel> modelListView;
    @FXML
    private javafx.scene.control.TextField searchField;
    @FXML
    private StackPane detailViewContainer;
    @FXML
    private Label resultsLabel;

    // Compatibility filters
    @FXML
    private ToggleButton filterRecommended;
    @FXML
    private ToggleButton filterStandard;
    @FXML
    private ToggleButton filterNotRecommended;

    // Size filters
    @FXML
    private ToggleButton filterSmall;
    @FXML
    private ToggleButton filterMedium;
    @FXML
    private ToggleButton filterLarge;
    @FXML
    private ToggleButton filterXLarge;

    // Capability filters
    @FXML
    private ToggleButton filterVision;
    @FXML
    private ToggleButton filterTools;
    @FXML
    private ToggleButton filterCode;

    private FilteredList<OllamaModel> filteredModels;
    private ModelManager modelManager;
    private javafx.animation.PauseTransition selectionDebounce;

    private static final Pattern SIZE_PATTERN = Pattern.compile("(\\d+\\.?\\d*)b", Pattern.CASE_INSENSITIVE);

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

        // All toggle filters use OR logic within category, AND between categories
        filterRecommended.selectedProperty().addListener((obs, old, sel) -> applyFilters());
        filterStandard.selectedProperty().addListener((obs, old, sel) -> applyFilters());
        filterNotRecommended.selectedProperty().addListener((obs, old, sel) -> applyFilters());

        filterSmall.selectedProperty().addListener((obs, old, sel) -> applyFilters());
        filterMedium.selectedProperty().addListener((obs, old, sel) -> applyFilters());
        filterLarge.selectedProperty().addListener((obs, old, sel) -> applyFilters());
        filterXLarge.selectedProperty().addListener((obs, old, sel) -> applyFilters());

        filterVision.selectedProperty().addListener((obs, old, sel) -> applyFilters());
        filterTools.selectedProperty().addListener((obs, old, sel) -> applyFilters());
        filterCode.selectedProperty().addListener((obs, old, sel) -> applyFilters());
    }

    private void applyFilters() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();

        // Check which categories have active filters
        boolean hasCompatFilter = filterRecommended.isSelected() || filterStandard.isSelected()
                || filterNotRecommended.isSelected();
        boolean hasSizeFilter = filterSmall.isSelected() || filterMedium.isSelected() || filterLarge.isSelected()
                || filterXLarge.isSelected();
        boolean hasCapabilityFilter = filterVision.isSelected() || filterTools.isSelected() || filterCode.isSelected();

        filteredModels.setPredicate(model -> {
            // Search by name - always applies
            if (!searchText.isEmpty() && !model.getName().toLowerCase().contains(searchText)) {
                return false;
            }

            // Compatibility filter (OR within category)
            if (hasCompatFilter) {
                modelManager.classifyModel(model);
                OllamaModel.CompatibilityStatus status = model.getCompatibilityStatus();
                boolean matchesCompat = false;
                if (filterRecommended.isSelected() && status == OllamaModel.CompatibilityStatus.RECOMMENDED)
                    matchesCompat = true;
                if (filterStandard.isSelected() && status == OllamaModel.CompatibilityStatus.CAUTION)
                    matchesCompat = true;
                if (filterNotRecommended.isSelected() && status == OllamaModel.CompatibilityStatus.INCOMPATIBLE)
                    matchesCompat = true;
                if (!matchesCompat)
                    return false;
            }

            // Size filter (OR within category)
            if (hasSizeFilter) {
                Double size = extractModelSize(model.getName());
                boolean matchesSize = false;

                // If size can't be detected, include in ≤3B (most are small models/embeddings)
                // Also include in all size categories if user selected multiple
                if (size == null) {
                    // Assume models without size indicator are small (embeddings, etc.)
                    if (filterSmall.isSelected())
                        matchesSize = true;
                } else {
                    if (filterSmall.isSelected() && size <= 3)
                        matchesSize = true;
                    if (filterMedium.isSelected() && size > 3 && size <= 10)
                        matchesSize = true;
                    if (filterLarge.isSelected() && size > 10 && size < 70)
                        matchesSize = true;
                    if (filterXLarge.isSelected() && size >= 70)
                        matchesSize = true;
                }
                if (!matchesSize)
                    return false;
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

    private Double extractModelSize(String modelName) {
        Matcher matcher = SIZE_PATTERN.matcher(modelName.toLowerCase());
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private void updateResultsLabel() {
        int count = filteredModels.size();
        resultsLabel.setText(count + " " + (count == 1 ? "modelo" : "modelos"));
    }

    private void setupEmptyState() {
        Label placeholder = new Label();
        placeholder.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
                () -> {
                    String search = searchField.getText();
                    boolean hasAnyFilter = filterRecommended.isSelected() || filterStandard.isSelected() ||
                            filterNotRecommended.isSelected() || filterSmall.isSelected() || filterMedium.isSelected()
                            ||
                            filterLarge.isSelected() || filterXLarge.isSelected() || filterVision.isSelected() ||
                            filterTools.isSelected() || filterCode.isSelected();

                    if ((search != null && !search.isEmpty()) || hasAnyFilter) {
                        return com.org.ollamafx.App.getBundle().getString("available.empty.no_match");
                    }
                    return com.org.ollamafx.App.getBundle().getString("available.empty.connecting");
                },
                searchField.textProperty(),
                filterRecommended.selectedProperty(),
                filterStandard.selectedProperty(),
                filterNotRecommended.selectedProperty(),
                filterSmall.selectedProperty(),
                filterMedium.selectedProperty(),
                filterLarge.selectedProperty(),
                filterXLarge.selectedProperty(),
                filterVision.selectedProperty(),
                filterTools.selectedProperty(),
                filterCode.selectedProperty()));
        placeholder.setStyle("-fx-text-fill: -color-fg-muted;");
        modelListView.setPlaceholder(placeholder);
    }

    // Simple, clean list cell - just the model name
    private void setupSimpleListCell() {
        modelListView.setCellFactory(param -> new javafx.scene.control.ListCell<>() {
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
        selectionDebounce = new javafx.animation.PauseTransition(javafx.util.Duration.millis(100));
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