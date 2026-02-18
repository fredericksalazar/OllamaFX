package com.org.ollamafx.ui;

import com.org.ollamafx.manager.ChatCollectionManager;
import com.org.ollamafx.manager.ChatManager;
import com.org.ollamafx.model.ChatFolder;
import com.org.ollamafx.model.ChatNode;
import com.org.ollamafx.model.ChatSession;
import javafx.scene.control.*;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.geometry.Pos;

import java.util.Optional;

public class ChatTreeCell extends TreeCell<ChatNode> {

    private final ChatCollectionManager collectionManager = ChatCollectionManager.getInstance();
    private final ChatManager chatManager = ChatManager.getInstance();

    public ChatTreeCell() {
        setupDragAndDrop();
    }

    @Override
    protected void updateItem(ChatNode item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            setContextMenu(null);
            // Clear style classes if necessary
            getStyleClass().remove("drag-over");
        } else {
            // Explicitly set text and graphic to ensure visibility
            // Relying on super.updateItem() sometimes fails if specific properties aren't
            // bound
            setText(item.toString());
            // TreeItem might be null during intermediate states, but usually safe here if
            // item is not null
            if (getTreeItem() != null) {
                setGraphic(getTreeItem().getGraphic());
            }

            // Context Menus
            if (item.getType() == ChatNode.Type.FOLDER) {
                setContextMenu(createFolderContextMenu(item.getFolder()));
            } else if (item.getType() == ChatNode.Type.SMART_COLLECTION) {
                setContextMenu(createSmartCollectionContextMenu(item.getSmartCollection()));
            } else {
                setContextMenu(createChatContextMenu(item.getChat()));
            }
        }
    }
    // Remove renderFolder and renderChat methods as we rely on TreeItem graphics
    // now.

    private ContextMenu createSmartCollectionContextMenu(com.org.ollamafx.model.SmartCollection sc) {
        ContextMenu menu = new ContextMenu();
        // Localize later
        MenuItem editItem = new MenuItem("Edit Smart Collection");
        editItem.setOnAction(e -> {
            Optional<com.org.ollamafx.model.SmartCollection> result = com.org.ollamafx.ui.SmartCollectionDialog
                    .show(sc);

            result.ifPresent(updated -> {
                sc.setName(updated.getName());
                sc.setCriteria(updated.getCriteria());
                sc.setValue(updated.getValue());
                sc.setIcon(updated.getIcon());
                // save
                collectionManager.updateSmartCollection(sc);
            });
        });

        MenuItem deleteItem = new MenuItem("Delete Smart Collection");
        deleteItem.setStyle("-fx-text-fill: red;");
        deleteItem.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete Smart Collection");
            alert.setHeaderText("Delete '" + sc.getName() + "'?");
            alert.setContentText("This will only remove the collection view, not the chats.");
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    collectionManager.deleteSmartCollection(sc);
                }
            });
        });

        menu.getItems().addAll(editItem, new SeparatorMenuItem(), deleteItem);
        return menu;
    }

    private ContextMenu createFolderContextMenu(ChatFolder folder) {
        ContextMenu menu = new ContextMenu();
        java.util.ResourceBundle bundle = com.org.ollamafx.App.getBundle();

        MenuItem newChatHereItem = new MenuItem(bundle.getString("context.folder.newChat"));
        newChatHereItem.setGraphic(icon("M12 5v14M5 12h14", 14)); // Plus
        newChatHereItem.setOnAction(e -> {
            ChatSession newChat = chatManager.createChat("New Chat");
            collectionManager.moveChatToFolder(newChat, folder);
        });

        MenuItem renameItem = new MenuItem(bundle.getString("context.folder.rename"));
        renameItem.setGraphic(icon(
                "M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z",
                14)); // Pencil
        renameItem.setOnAction(e -> {
            FxDialog.showInputDialog(
                    getTreeView().getScene().getWindow(),
                    bundle.getString("dialog.folder.rename.title"),
                    folder.getName(),
                    folder.getName(),
                    bundle.getString("dialog.rename.confirm")).ifPresent(newName -> {
                        collectionManager.renameFolder(folder, newName);
                    });
        });

        // Color picker row
        CustomMenuItem colorItem = new CustomMenuItem();
        colorItem.setHideOnClick(false);

        HBox colorBox = new HBox(8);
        colorBox.setAlignment(Pos.CENTER);
        colorBox.setStyle("-fx-padding: 5 10 5 10;");

        String[] colors = { "#FF3B30", "#FF9500", "#FFCC00", "#4CD964", "#5AC8FA", "#007AFF", "#5856D6", "#8E8E93" };

        for (String colorHex : colors) {
            Circle dot = new Circle(7, Color.web(colorHex));
            dot.setStroke(Color.web("#000000", 0.2));
            dot.setStrokeWidth(1);
            dot.setOnMouseEntered(ev -> {
                dot.setScaleX(1.2);
                dot.setScaleY(1.2);
                dot.setCursor(javafx.scene.Cursor.HAND);
            });
            dot.setOnMouseExited(ev -> {
                dot.setScaleX(1.0);
                dot.setScaleY(1.0);
                dot.setCursor(javafx.scene.Cursor.DEFAULT);
            });
            dot.setOnMouseClicked(ev -> {
                collectionManager.setFolderColor(folder, colorHex);
                menu.hide();
            });
            colorBox.getChildren().add(dot);
        }
        colorItem.setContent(colorBox);

        MenuItem deleteItem = new MenuItem(bundle.getString("context.folder.delete"));
        deleteItem.setGraphic(
                icon("M3 6h18M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2", 14)); // Trash
        // Color negro nativo — sin override rojo
        deleteItem.setOnAction(e -> {
            boolean ok = FxDialog.showConfirmDialog(
                    getTreeView().getScene().getWindow(),
                    java.text.MessageFormat.format(bundle.getString("dialog.folder.delete.header"), folder.getName()),
                    bundle.getString("dialog.folder.delete.content"),
                    bundle.getString("dialog.delete.confirm"),
                    bundle.getString("button.cancel"));
            if (ok)
                collectionManager.deleteFolder(folder);
        });

        menu.getItems().addAll(newChatHereItem, renameItem, new SeparatorMenuItem(), colorItem, new SeparatorMenuItem(),
                deleteItem);
        return menu;
    }

    private ContextMenu createChatContextMenu(ChatSession chat) {
        ContextMenu menu = new ContextMenu();
        java.util.ResourceBundle bundle = com.org.ollamafx.App.getBundle();

        MenuItem renameItem = new MenuItem(bundle.getString("context.chat.rename"));
        renameItem.setGraphic(icon(
                "M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z",
                14)); // Pencil
        renameItem.setOnAction(e -> {
            FxDialog.showInputDialog(
                    getTreeView().getScene().getWindow(),
                    bundle.getString("dialog.rename.title"),
                    chat.getName(),
                    chat.getName(),
                    bundle.getString("dialog.rename.confirm")).ifPresent(newName -> {
                        chatManager.renameChat(chat, newName);
                        getTreeView().refresh();
                    });
        });

        // "Move to..." Submenu
        Menu moveMenu = new Menu(bundle.getString("context.chat.move"));
        moveMenu.setGraphic(icon("M5 12h14M12 5l7 7-7 7", 14)); // Arrow right

        MenuItem rootItem = new MenuItem(bundle.getString("context.chat.uncategorized"));
        rootItem.setGraphic(icon(
                "M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 0 0 1 1h3m10-11l2 2m-2-2v10a1 1 0 0 0-1 1h-3m-6 0a1 1 0 0 0 1-1v-4a1 1 0 0 0-1-1H9a1 1 0 0 0-1 1v4a1 1 0 0 0 1 1m-6 0h16",
                14)); // Home
        rootItem.setOnAction(e -> collectionManager.moveChatToFolder(chat, null));
        moveMenu.getItems().add(rootItem);
        moveMenu.getItems().add(new SeparatorMenuItem());

        for (ChatFolder f : collectionManager.getFolders()) {
            MenuItem folderItem = new MenuItem(f.getName());
            Circle dot = new Circle(6, Color.web(f.getColor()));
            folderItem.setGraphic(dot);
            folderItem.setOnAction(e -> collectionManager.moveChatToFolder(chat, f));
            moveMenu.getItems().add(folderItem);
        }

        MenuItem deleteItem = new MenuItem(bundle.getString("context.chat.delete"));
        deleteItem.setGraphic(
                icon("M3 6h18M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2", 14)); // Trash
        // Color negro nativo — sin override rojo
        deleteItem.setOnAction(e -> {
            boolean ok = FxDialog.showConfirmDialog(
                    getTreeView().getScene().getWindow(),
                    java.text.MessageFormat.format(bundle.getString("dialog.chat.delete.header"), chat.getName()),
                    bundle.getString("dialog.chat.delete.content"),
                    bundle.getString("dialog.delete.confirm"),
                    bundle.getString("button.cancel"));
            if (ok) {
                collectionManager.removeChatFromFolder(chat);
                chatManager.deleteChat(chat);
            }
        });

        menu.getItems().addAll(renameItem, moveMenu, new SeparatorMenuItem(), deleteItem);
        return menu;
    }

    /**
     * Crea un icono SVG minimalista (estilo Lucide/Feather) para los menu items.
     * El path SVG debe ser de un viewBox 24x24.
     */
    private javafx.scene.Node icon(String svgPath, double size) {
        SVGPath path = new SVGPath();
        path.setContent(svgPath);
        path.setStyle("-fx-fill: transparent; -fx-stroke: -color-fg-muted; -fx-stroke-width: 1.5;");
        // Escalar del viewBox 24x24 al tamaño deseado
        double scale = size / 24.0;
        path.setScaleX(scale);
        path.setScaleY(scale);
        // Contenedor para centrar correctamente
        javafx.scene.layout.StackPane container = new javafx.scene.layout.StackPane(path);
        container.setPrefSize(size, size);
        container.setMinSize(size, size);
        container.setMaxSize(size, size);
        return container;
    }

    // --- Drag & Drop Interface ---

    private void setupDragAndDrop() {
        // MOUSE CLICK (Toggle Folder Expansion)
        setOnMouseClicked((javafx.scene.input.MouseEvent event) -> {
            if (getItem() != null && getItem().getType() == ChatNode.Type.FOLDER) {
                if (event.getClickCount() == 1) { // Single click
                    TreeItem<ChatNode> treeItem = getTreeItem();
                    if (treeItem != null) {
                        treeItem.setExpanded(!treeItem.isExpanded());
                        event.consume();
                    }
                }
            }
        });

        // DRAG DETECTED (Start dragging a CHAT)
        setOnDragDetected(event -> {
            if (getItem() == null || getItem().getType() != ChatNode.Type.CHAT) {
                return;
            }

            javafx.scene.input.Dragboard db = startDragAndDrop(javafx.scene.input.TransferMode.MOVE);
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            // We store the Chat ID as string
            content.putString(getItem().getChat().getId().toString());
            db.setContent(content);

            // Set Drag View
            javafx.scene.image.WritableImage snapshot = this.snapshot(new javafx.scene.SnapshotParameters(), null);
            db.setDragView(snapshot);

            event.consume();
        });

        // DRAG OVER — acepta drops en FOLDER y en celdas sin item (para mover a
        // Uncategorized)
        setOnDragOver(event -> {
            if (event.getGestureSource() != this &&
                    event.getDragboard().hasString()) {

                ChatNode item = getItem();
                // Aceptar si es carpeta o si la celda no tiene item (zona vacía =
                // Uncategorized)
                boolean isFolder = item != null && item.getType() == ChatNode.Type.FOLDER;
                boolean isEmpty = item == null;

                if (isFolder || isEmpty) {
                    event.acceptTransferModes(javafx.scene.input.TransferMode.MOVE);
                    if (!getStyleClass().contains("drag-over")) {
                        getStyleClass().add("drag-over");
                    }
                }
            }
            event.consume();
        });

        // DRAG EXIT
        setOnDragExited(event -> {
            getStyleClass().remove("drag-over");
            event.consume();
        });

        // DRAG DROPPED
        setOnDragDropped(event -> {
            javafx.scene.input.Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                String chatId = db.getString();

                // Determinar carpeta destino: null = Uncategorized (raíz)
                ChatNode item = getItem();
                ChatFolder targetFolder = (item != null && item.getType() == ChatNode.Type.FOLDER)
                        ? item.getFolder()
                        : null; // ROOT o celda vacía → Uncategorized

                // Find chat by ID
                ChatSession chatToMove = chatManager.getChatSessions().stream()
                        .filter(c -> c.getId().toString().equals(chatId))
                        .findFirst()
                        .orElse(null);

                if (chatToMove != null) {
                    collectionManager.moveChatToFolder(chatToMove, targetFolder);
                    success = true;
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }
}
