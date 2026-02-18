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
            } else {
                setContextMenu(createChatContextMenu(item.getChat()));
            }
        }
    }
    // Remove renderFolder and renderChat methods as we rely on TreeItem graphics
    // now.

    private ContextMenu createFolderContextMenu(ChatFolder folder) {
        ContextMenu menu = new ContextMenu();

        MenuItem renameItem = new MenuItem("Rename Folder");
        renameItem.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog(folder.getName());
            dialog.setTitle("Rename Folder");
            dialog.setHeaderText("Enter new name:");
            dialog.showAndWait().ifPresent(newName -> {
                collectionManager.renameFolder(folder, newName);
                // Refresh handled by listener
            });
        });

        MenuItem newChatHereItem = new MenuItem("New Chat in Folder");
        newChatHereItem.setOnAction(e -> {
            ChatSession newChat = chatManager.createChat("New Chat");
            collectionManager.moveChatToFolder(newChat, folder);
            // Refresh handled by listener
        });

        Menu colorMenu = new Menu("Color Tag");
        String[] colors = { "#FF3B30", "#FF9500", "#FFCC00", "#4CD964", "#5AC8FA", "#007AFF", "#5856D6", "#8E8E93" };
        String[] names = { "Red", "Orange", "Yellow", "Green", "Teal", "Blue", "Purple", "Gray" };

        for (int i = 0; i < colors.length; i++) {
            String color = colors[i];
            String name = names[i];
            MenuItem colorItem = new MenuItem(name);
            Circle dot = new Circle(6, Color.web(color));
            colorItem.setGraphic(dot);
            colorItem.setOnAction(e -> {
                collectionManager.setFolderColor(folder, color);
                // Refresh handled by listener in Controller
            });
            colorMenu.getItems().add(colorItem);
        }

        MenuItem deleteItem = new MenuItem("Delete Folder");
        deleteItem.setStyle("-fx-text-fill: red;");
        deleteItem.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete Folder");
            alert.setHeaderText("Delete '" + folder.getName() + "'?");
            alert.setContentText("Chats inside will be moved to Uncategorized.");
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    collectionManager.deleteFolder(folder);
                }
            });
        });

        menu.getItems().addAll(renameItem, colorMenu, new SeparatorMenuItem(), deleteItem);
        return menu;
    }

    private ContextMenu createChatContextMenu(ChatSession chat) {
        ContextMenu menu = new ContextMenu();

        MenuItem renameItem = new MenuItem("Rename Chat");
        renameItem.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog(chat.getName());
            dialog.setTitle("Rename Chat");
            dialog.showAndWait().ifPresent(newName -> {
                chatManager.renameChat(chat, newName);
                getTreeView().refresh();
            });
        });

        MenuItem deleteItem = new MenuItem("Delete Chat");
        deleteItem.setStyle("-fx-text-fill: red;");
        deleteItem.setOnAction(e -> {
            collectionManager.removeChatFromFolder(chat); // Cleanup folder ref
            chatManager.deleteChat(chat);
            // Controller needs to refresh tree
        });

        // "Move to..." Submenu
        Menu moveMenu = new Menu("Move to...");

        // Option to move to Root (Uncategorized)
        MenuItem rootItem = new MenuItem("Uncategorized");
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
