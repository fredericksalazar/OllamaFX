package com.org.ollamafx.controller;

import com.org.ollamafx.App;
import com.org.ollamafx.manager.ModelManager;
import com.org.ollamafx.manager.OllamaManager;
import com.org.ollamafx.model.OllamaModel;
import atlantafx.base.theme.Styles;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ModelDetailController {

    @FXML
    private Label modelNameLabel;
    @FXML
    private Text modelDescriptionText;
    @FXML
    private VBox modelListContainer;

    private ModelManager modelManager;

    public void setModelManager(ModelManager modelManager) {
        this.modelManager = modelManager;
    }

    @FXML
    private Label parameterSizeLabel;

    @FXML
    public void initialize() {
        // Setup default placeholder or properties if needed.
    }

    private void showDownloadPopup(OllamaModel model) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/download_popup.fxml"));
            loader.setResources(App.getBundle());

            Parent root = loader.load();
            DownloadPopupController controller = loader.getController();
            controller.setModelName(model.getName() + ":" + model.getTag());

            String userAgentStylesheet = Application.getUserAgentStylesheet();
            if (userAgentStylesheet != null && userAgentStylesheet.toLowerCase().contains("light")) {
                root.getStyleClass().add("light");
            } else {
                // If not explicitly light, assume dark (or check for dark)
                // This ensures .root.dark selectors work inside the popup
                root.getStyleClass().add("dark");
            }

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setTitle(App.getBundle().getString("download.title.default"));

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);
            stage.setResizable(false);

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    updateMessage(App.getBundle().getString("download.status.process"));
                    updateProgress(0, 100);

                    OllamaManager.getInstance().pullModel(model.getName(), model.getTag(),
                            (progress, status) -> {
                                updateMessage(status);
                                if (progress >= 0) {
                                    updateProgress(progress, 100);
                                } else {
                                    updateProgress(-1, 100);
                                }
                            });

                    return null;
                }
            };

            task.setOnSucceeded(e -> {
                if (modelManager != null) {
                    String date = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                            .format(LocalDateTime.now());
                    OllamaModel newModel = new OllamaModel(
                            model.getName(), App.getBundle().getString("model.installed"), "N/A", model.getTag(),
                            model.sizeProperty().get(), date,
                            model.getContextLength(), model.getInputType());
                    modelManager.addLocalModel(newModel);
                }
            });

            task.setOnFailed(e -> {
                System.err.println("Download failed: " + task.getException().getMessage());
            });

            controller.setDownloadTask(task);
            App.getExecutorService().submit(task);
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private FlowPane badgesContainer;
    @FXML
    private Label pullCountLabel;
    @FXML
    private Label lastUpdatedLabel;
    @FXML
    private Label commandLabel;
    @FXML
    private Button copyButton;

    public void populateDetails(List<OllamaModel> modelTags) {
        if (modelTags == null || modelTags.isEmpty()) {
            modelNameLabel.setText(App.getBundle().getString("model.error"));
            modelDescriptionText.setText(App.getBundle().getString("model.error.details"));
            modelListContainer.getChildren().clear();
            return;
        }

        OllamaModel firstTag = modelTags.get(0);
        // Apply Apple Title Style
        modelNameLabel.getStyleClass().add("apple-title-large");
        modelNameLabel.setText(firstTag.getName());

        // Apply Body Style
        modelDescriptionText.getStyleClass().add("apple-text-body");
        modelDescriptionText.setText(firstTag.descriptionProperty().get());

        commandLabel.setText("ollama run " + firstTag.getName());

        copyButton.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(commandLabel.getText());
            clipboard.setContent(content);
            copyButton.getStyleClass().add("success");
            PauseTransition pause = new PauseTransition(
                    Duration.seconds(1));
            pause.setOnFinished(ev -> copyButton.getStyleClass().remove("success"));
            pause.play();
        });

        pullCountLabel.setText(firstTag.pullCountProperty().get());
        lastUpdatedLabel.setText(firstTag.lastUpdatedProperty().get());

        // Parse Parameter Size (e.g., 7b)
        String tag = firstTag.getTag();
        if (tag.contains(":")) {
            String[] parts = tag.split(":");
            if (parts.length > 1) {
                parameterSizeLabel.setText(parts[1].toUpperCase());
            } else {
                parameterSizeLabel.setText("-");
            }
        } else {
            parameterSizeLabel.setText("-");
        }

        badgesContainer.getChildren().clear();
        for (String badge : firstTag.getBadges()) {
            Label badgeLabel = new Label(badge);
            badgeLabel.getStyleClass().add("badge-chip");
            // Reuse existing badge colors for now, they look okay
            String lowerBadge = badge.toLowerCase();
            if (lowerBadge.contains("tool") || lowerBadge.contains("think") || lowerBadge.contains("vision")) {
                badgeLabel.getStyleClass().add("badge-purple");
            } else if (lowerBadge.contains("cloud") || lowerBadge.matches(".*\\d+b.*")) {
                badgeLabel.getStyleClass().add("badge-blue");
            } else if (lowerBadge.contains("code") || lowerBadge.contains("math")) {
                badgeLabel.getStyleClass().add("badge-green");
            } else {
                badgeLabel.getStyleClass().add("badge-blue");
            }
            badgesContainer.getChildren().add(badgeLabel);
        }

        // BUILD APPLE STYLE CARDS (Manual VBox)
        modelListContainer.getChildren().clear();
        modelListContainer.setSpacing(12); // Spacing between cards

        for (OllamaModel model : modelTags) {
            // Classify each tag
            if (modelManager != null) {
                modelManager.classifyModel(model);
            }

            // Main Card Container
            HBox card = new HBox();
            card.getStyleClass().add("apple-card-row");
            card.setAlignment(Pos.CENTER_LEFT);
            card.setSpacing(15);
            card.setPadding(new javafx.geometry.Insets(10, 15, 10, 15)); // Add padding for better look

            // TRAFFIC LIGHT INDICATOR (Label Badge)
            Label statusBadge = new Label();
            statusBadge.getStyleClass().add("badge-chip"); // Reusing chip style or custom

            OllamaModel.CompatibilityStatus status = model.getCompatibilityStatus();
            if (status == null)
                status = OllamaModel.CompatibilityStatus.CAUTION;

            switch (status) {
                case RECOMMENDED:
                    statusBadge.setText(App.getBundle().getString("status.recommended").split(":")[0]); // Use short
                                                                                                        // text
                    statusBadge.getStyleClass().add("success");
                    statusBadge
                            .setStyle("-fx-background-color: -color-success-subtle; -fx-text-fill: -color-success-fg;");
                    break;
                case CAUTION:
                    statusBadge.setText(App.getBundle().getString("status.caution").split(":")[0]);
                    statusBadge.getStyleClass().add("warning");
                    statusBadge
                            .setStyle("-fx-background-color: -color-warning-subtle; -fx-text-fill: -color-warning-fg;");
                    break;
                case INCOMPATIBLE:
                    statusBadge.setText(App.getBundle().getString("status.incompatible").split(":")[0]);
                    statusBadge.getStyleClass().add("danger");
                    statusBadge
                            .setStyle("-fx-background-color: -color-danger-subtle; -fx-text-fill: -color-danger-fg;");
                    break;
            }

            // Create tooltip for full description
            javafx.scene.control.Tooltip tip = new javafx.scene.control.Tooltip();
            if (status == OllamaModel.CompatibilityStatus.RECOMMENDED)
                tip.setText(App.getBundle().getString("status.recommended"));
            else if (status == OllamaModel.CompatibilityStatus.CAUTION)
                tip.setText(App.getBundle().getString("status.caution"));
            else
                tip.setText(App.getBundle().getString("status.incompatible"));
            javafx.scene.control.Tooltip.install(statusBadge, tip);

            // Left: Icon placeholder or just padding? Let's use a tech icon or just clean
            // text.
            // Let's stack Tag Name (Bold) and Size (Subtle)
            VBox infoBox = new VBox();
            infoBox.setAlignment(Pos.CENTER_LEFT);
            infoBox.setSpacing(4);

            Label nameLbl = new Label(model.getTag());
            nameLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: normal; -fx-text-fill: -color-fg-default;");

            Label sizeLbl = new Label(model.sizeProperty().get() + " • " + model.getInputType());
            sizeLbl.getStyleClass().add("apple-text-subtle");

            infoBox.getChildren().addAll(nameLbl, sizeLbl);

            // Spacer
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            // Right: Action Button
            Button actionBtn = new Button();

            boolean isInstalled = modelManager != null
                    && modelManager.isModelInstalled(model.getName(), model.getTag());

            if (isInstalled) {
                actionBtn.setText(App.getBundle().getString("model.action.uninstall"));
                actionBtn.getStyleClass().add(Styles.DANGER);
                actionBtn.setOnAction(ev -> {
                    if (modelManager != null) {
                        modelManager.deleteModel(model.getName(), model.getTag());
                        actionBtn.setDisable(true);
                    }
                });
            } else {
                actionBtn.setText(App.getBundle().getString("model.action.get")); // Apple Style "Get"
                // Disable if incompatible? No, let user override if they want (maybe they have
                // swap)
                // But visual feedback is enough.

                actionBtn.getStyleClass().add(Styles.SUCCESS);
                actionBtn.setOnAction(ev -> {
                    showDownloadPopup(model);
                });
            }

            card.getChildren().addAll(statusBadge, infoBox, spacer, actionBtn);
            modelListContainer.getChildren().add(card);
        }
    }
}