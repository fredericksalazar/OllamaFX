package com.org.ollamafx.ui;

import com.org.ollamafx.manager.TrashManager;
import com.org.ollamafx.model.TrashItem;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ResourceBundle;
import com.org.ollamafx.App;
import javafx.scene.Node;

/**
 * Vista de la Papelera de Reciclaje.
 * Muestra los chats y carpetas eliminados con opciones de restaurar o eliminar
 * permanentemente.
 */
public class TrashView extends VBox {

    private final TrashManager trashManager = TrashManager.getInstance();
    private final ListView<TrashItem> listView = new ListView<>();
    private ResourceBundle bundle;

    public TrashView() {
        bundle = App.getBundle();
        buildUI();
        bindData();
    }

    private void buildUI() {
        setSpacing(0);
        getStyleClass().add("trash-view");

        // ── Header ──────────────────────────────────────────────────────────
        // Título e Ícono
        HBox titleBox = new HBox(8);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        SVGPath trashIcon = new SVGPath();
        trashIcon.setContent(
                "M3 6h18M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2");
        trashIcon.setStyle("-fx-fill: transparent; -fx-stroke: -color-fg-default; -fx-stroke-width: 1.5;");

        double iconSize = 20.0;
        double scale = iconSize / 24.0;
        trashIcon.setScaleX(scale);
        trashIcon.setScaleY(scale);

        StackPane iconContainer = new StackPane(trashIcon);
        iconContainer.setPrefSize(iconSize, iconSize);
        iconContainer.setMinSize(iconSize, iconSize);
        iconContainer.setMaxSize(iconSize, iconSize);

        Label title = new Label(bundle.getString("sidebar.trash"));
        title.getStyleClass().add("trash-title");
        titleBox.getChildren().addAll(iconContainer, title);

        // Subtítulo (aviso de 30 días)
        Label subtitle = new Label(bundle.getString("trash.header.subtitle"));
        subtitle.getStyleClass().add("trash-subtitle");
        subtitle.setWrapText(true);

        VBox headerText = new VBox(4, titleBox, subtitle);
        HBox.setHgrow(headerText, Priority.ALWAYS);

        Button emptyBtn = new Button(bundle.getString("trash.empty"));
        emptyBtn.getStyleClass().addAll("button", "danger");
        emptyBtn.setOnAction(e -> confirmEmptyTrash());

        HBox header = new HBox(headerText, emptyBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 20, 16, 20));
        header.getStyleClass().add("trash-header");

        // ── List ─────────────────────────────────────────────────────────────
        listView.setCellFactory(lv -> new TrashCell());
        listView.getStyleClass().add("trash-list");
        VBox.setVgrow(listView, Priority.ALWAYS);

        // ── Empty state ───────────────────────────────────────────────────────
        Label emptyLabel = new Label("🗑 " + bundle.getString("trash.emptyState"));
        emptyLabel.getStyleClass().add("trash-empty-label");
        listView.setPlaceholder(emptyLabel);

        getChildren().addAll(header, listView);
    }

    private void bindData() {
        listView.setItems(trashManager.getTrashItems());
        trashManager.getTrashItems().addListener((ListChangeListener<TrashItem>) c -> {
            listView.refresh();
        });
    }

    private void confirmEmptyTrash() {
        if (trashManager.getTrashItems().isEmpty())
            return;
        boolean ok = FxDialog.showConfirmDialog(
                getScene() != null ? getScene().getWindow() : null,
                bundle.getString("trash.confirmEmpty"),
                bundle.getString("trash.confirmEmptyContent"),
                bundle.getString("trash.deleteForever"),
                bundle.getString("button.cancel"));
        if (ok)
            trashManager.emptyTrash();
    }

    // ─── Cell ─────────────────────────────────────────────────────────────────

    private class TrashCell extends ListCell<TrashItem> {

        private final HBox root = new HBox(10);
        private final StackPane iconBox = new StackPane();
        private final VBox textBox = new VBox(2);
        private final Label nameLabel = new Label();
        private final Label dateLabel = new Label();
        private final HBox actions = new HBox(6);
        private final Button restoreBtn = new Button(bundle.getString("trash.restore"));
        private final Button deleteBtn = new Button(bundle.getString("trash.deleteForever"));

        TrashCell() {
            iconBox.setPrefSize(28, 28);
            iconBox.setMinSize(28, 28);

            nameLabel.getStyleClass().add("trash-item-name");
            dateLabel.getStyleClass().add("trash-item-date");
            textBox.getChildren().addAll(nameLabel, dateLabel);
            HBox.setHgrow(textBox, Priority.ALWAYS);

            restoreBtn.getStyleClass().addAll("button", "flat");
            deleteBtn.getStyleClass().addAll("button", "flat", "danger-text");

            restoreBtn.setOnAction(e -> {
                TrashItem item = getItem();
                if (item == null)
                    return;
                if (item.getType() == TrashItem.ItemType.CHAT) {
                    trashManager.restoreChat(item);
                } else {
                    trashManager.restoreFolder(item);
                }
            });

            deleteBtn.setOnAction(e -> {
                TrashItem item = getItem();
                if (item == null)
                    return;
                boolean ok = FxDialog.showConfirmDialog(
                        getScene() != null ? getScene().getWindow() : null,
                        bundle.getString("trash.confirmDelete"),
                        bundle.getString("trash.confirmDeleteContent"),
                        bundle.getString("trash.deleteForever"),
                        bundle.getString("button.cancel"));
                if (!ok)
                    return;
                if (item.getType() == TrashItem.ItemType.CHAT) {
                    trashManager.permanentlyDeleteChat(item);
                } else {
                    trashManager.permanentlyDeleteFolder(item);
                }
            });

            actions.getChildren().addAll(restoreBtn, deleteBtn);
            actions.setAlignment(Pos.CENTER_RIGHT);

            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(8, 12, 8, 12));
            root.getChildren().addAll(iconBox, textBox, actions);
        }

        @Override
        protected void updateItem(TrashItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }

            // Ícono según tipo
            iconBox.getChildren().clear();
            if (item.getType() == TrashItem.ItemType.FOLDER) {
                iconBox.getChildren().add(svgIcon(
                        "M10 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z",
                        14, "#F5A623"));
            } else {
                iconBox.getChildren().add(svgIcon(
                        "M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z",
                        14, "-color-fg-muted"));
            }

            nameLabel.setText(item.getDisplayName());

            // Formatear fecha y días restantes
            try {
                LocalDateTime deletedDate = LocalDateTime.parse(item.getDeletedAt());
                long daysInTrash = ChronoUnit.DAYS.between(deletedDate, LocalDateTime.now());
                long daysLeft = 30 - daysInTrash;

                String dateText = deletedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
                String remainingText;

                if (daysLeft <= 0) {
                    remainingText = bundle.getString("trash.item.deletingToday");
                } else {
                    remainingText = String.format(bundle.getString("trash.item.daysLeft"), daysLeft);
                }

                dateLabel.setText(dateText + " • " + remainingText);

                // Style warning if close to deletion
                if (daysLeft <= 3) {
                    dateLabel.setStyle("-fx-text-fill: -color-danger-fg;");
                } else {
                    dateLabel.setStyle("");
                }

            } catch (Exception ex) {
                dateLabel.setText(item.getDeletedAt());
            }

            setGraphic(root);
        }

        private Node svgIcon(String path, double size, String color) {
            SVGPath svg = new SVGPath();
            svg.setContent(path);
            svg.setStyle("-fx-fill: " + color + "; -fx-stroke: transparent;");
            double scale = size / 24.0;
            svg.setScaleX(scale);
            svg.setScaleY(scale);
            StackPane sp = new StackPane(svg);
            sp.setPrefSize(size, size);
            sp.setMinSize(size, size);
            sp.setMaxSize(size, size);
            return sp;
        }
    }
}
