package com.org.ollamafx.ui;

import com.org.ollamafx.model.OllamaModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ModelCard extends VBox {

    private final OllamaModel model;
    private final Runnable onInstall;
    private final Runnable onDetails;

    public ModelCard(OllamaModel model, boolean isInstalled, Runnable onInstall, Runnable onDetails) {
        this.model = model;
        this.onInstall = onInstall;
        this.onDetails = onDetails;

        initUI(isInstalled);
    }

    private void initUI(boolean isInstalled) {
        this.getStyleClass().add("model-card");
        this.setPrefWidth(220);
        this.setPrefHeight(280);
        this.setSpacing(10);
        this.setPadding(new Insets(20));

        // ICON (Placeholder or derived from name)
        VBox iconContainer = new VBox();
        iconContainer.setAlignment(Pos.CENTER);
        iconContainer.getStyleClass().add("model-icon-container");
        Label iconLabel = new Label(model.getName().substring(0, 1).toUpperCase());
        iconLabel.getStyleClass().add("model-icon-text");
        iconContainer.getChildren().add(iconLabel);

        // TITLE
        Label title = new Label(model.getName());
        title.getStyleClass().add("model-card-title");
        title.setWrapText(true);

        // BADGES
        HBox badgesBox = new HBox(5);
        badgesBox.setAlignment(Pos.CENTER_LEFT);
        // Show first 2 badges max
        if (model.getBadges() != null) {
            model.getBadges().stream().limit(2).forEach(b -> {
                Label badge = new Label(b);
                badge.getStyleClass().add("model-badge-small");
                badgesBox.getChildren().add(badge);
            });
        }

        // PARAM SIZE (if not in badges, try to guess or show from size)
        if (badgesBox.getChildren().isEmpty() && !model.getSize().equals("N/A")) {
            Label sizeBadge = new Label(model.getSize());
            sizeBadge.getStyleClass().add("model-badge-small");
            badgesBox.getChildren().add(sizeBadge);
        }

        // DESCRIPTION
        Label desc = new Label(model.getDescription());
        desc.getStyleClass().add("model-card-desc");
        desc.setWrapText(true);
        desc.setMaxHeight(60); // Limit height
        VBox.setVgrow(desc, Priority.ALWAYS); // Grow to fill space

        // STATS
        Label stats = new Label("⬇ " + model.getPullCount());
        stats.getStyleClass().add("model-card-stats");

        // ACTION BUTTON
        Button actionBtn = new Button();
        actionBtn.setMaxWidth(Double.MAX_VALUE);
        if (isInstalled) {
            actionBtn.setText(com.org.ollamafx.App.getBundle().getString("model.action.uninstall"));
            actionBtn.getStyleClass().addAll("button", "danger", "outlined");
            actionBtn.setOnAction(e -> onDetails.run()); // This is now uninstall action
        } else {
            actionBtn.setText(com.org.ollamafx.App.getBundle().getString("model.action.get"));
            actionBtn.getStyleClass().addAll("button", "accent", "pill");
            actionBtn.setOnAction(e -> onInstall.run());
        }

        this.getChildren().addAll(iconContainer, title, badgesBox, desc, stats, actionBtn);
    }
}
