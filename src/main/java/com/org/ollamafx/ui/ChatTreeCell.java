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

        MenuItem renameItem = new MenuItem(bundle.getString("context.folder.rename"));
        renameItem.setOnAction(e -> {
            FxDialog.showInputDialog(
                    getTreeView().getScene().getWindow(),
                    bundle.getString("dialog.folder.rename.title"),
                    folder.getName(),
                    folder.getName(),
                    bundle.getString("dialog.rename.confirm")).ifPresent(newName -> {
                        collectionManager.renameFolder(folder, newName);
                        // Refresh handled by listener
                    });
        });

        MenuItem newChatHereItem = new MenuItem(bundle.getString("context.folder.newChat"));
        newChatHereItem.setOnAction(e -> {
            ChatSession newChat = chatManager.createChat("New Chat");
            collectionManager.moveChatToFolder(newChat, folder);
            // Refresh handled by listener
        });

        // macOS-style Color Picker Row
        CustomMenuItem colorItem = new CustomMenuItem();
        colorItem.setHideOnClick(false); // We handle hiding manually

        HBox colorBox = new HBox(8); // Spacing between dots
        colorBox.setAlignment(Pos.CENTER);
        colorBox.setStyle("-fx-padding: 5 10 5 10;"); // Add padding

        String[] colors = { "#FF3B30", "#FF9500", "#FFCC00", "#4CD964", "#5AC8FA", "#007AFF", "#5856D6", "#8E8E93" };
        String[] colorNames = { "Red", "Orange", "Yellow", "Green", "Teal", "Blue", "Purple", "Gray" }; // For tooltip
                                                                                                        // if needed

        for (int i = 0; i < colors.length; i++) {
            final String colorHex = colors[i];
            Circle dot = new Circle(7, Color.web(colorHex));
            dot.setStroke(Color.web("#000000", 0.2)); // Subtle stroke definition
            dot.setStrokeWidth(1);

            // Interaction
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
                menu.hide(); // Close menu after selection
            });
            // Add checkmark if currently selected?
            // Ideally yes, but for now simple action.

            colorBox.getChildren().add(dot);
        }
        colorItem.setContent(colorBox);

        MenuItem deleteItem = new MenuItem(bundle.getString("context.folder.delete"));
        deleteItem.setStyle("-fx-text-fill: red;");
        deleteItem.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(bundle.getString("dialog.folder.delete.title"));
            alert.setHeaderText(
                    java.text.MessageFormat.format(bundle.getString("dialog.folder.delete.header"), folder.getName()));
            alert.setContentText(bundle.getString("dialog.folder.delete.content"));
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    collectionManager.deleteFolder(folder);
                }
            });
        });

        menu.getItems().addAll(newChatHereItem, renameItem, new SeparatorMenuItem(), colorItem, new SeparatorMenuItem(),
                deleteItem);
        return menu;
    }

    private ContextMenu createChatContextMenu(ChatSession chat) {
        ContextMenu menu = new ContextMenu();
        java.util.ResourceBundle bundle = com.org.ollamafx.App.getBundle();

        MenuItem renameItem = new MenuItem(bundle.getString("context.chat.rename"));
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

        MenuItem deleteItem = new MenuItem(bundle.getString("context.chat.delete"));
        deleteItem.setStyle("-fx-text-fill: red;");
        deleteItem.setOnAction(e -> {
            collectionManager.removeChatFromFolder(chat); // Cleanup folder ref
            chatManager.deleteChat(chat);
            // Controller needs to refresh tree
        });

        // "Move to..." Submenu
        Menu moveMenu = new Menu(bundle.getString("context.chat.move"));

        // Option to move to Root (Uncategorized)
        MenuItem rootItem = new MenuItem(bundle.getString("context.chat.uncategorized"));
        rootItem.setOnAction(e -> {
            collectionManager.moveChatToFolder(chat, null);
        });
        moveMenu.getItems().add(rootItem);
        moveMenu.getItems().add(new SeparatorMenuItem());

        for (ChatFolder f : collectionManager.getFolders()) {
            MenuItem folderItem = new MenuItem(f.getName());
            Circle dot = new Circle(6, Color.web(f.getColor()));
            folderItem.setGraphic(dot);
            folderItem.setOnAction(e -> {
                collectionManager.moveChatToFolder(chat, f);
            });
            moveMenu.getItems().add(folderItem);
        }

        menu.getItems().addAll(renameItem, moveMenu, new SeparatorMenuItem(), deleteItem);
        return menu;
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

        // DRAG OVER (Hovering over a FOLDER)
        setOnDragOver(event -> {
            if (event.getGestureSource() != this &&
                    event.getDragboard().hasString()) {

                // Only allow drop if we are a FOLDER
                // For now, let's allow dropping on FOLDER items.
                if (getItem() != null && getItem().getType() == ChatNode.Type.FOLDER) {
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
                ChatFolder targetFolder = getItem().getFolder();

                // Find chat by ID (inefficient but safe)
                ChatSession chatToMove = chatManager.getChatSessions().stream()
                        .filter(c -> c.getId().toString().equals(chatId))
                        .findFirst()
                        .orElse(null);

                if (chatToMove != null && targetFolder != null) {
                    collectionManager.moveChatToFolder(chatToMove, targetFolder);
                    // Force refresh of the tree
                    success = true;
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }
}
