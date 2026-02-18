package com.org.ollamafx.model;

/**
 * Wrapper class for TreeView items.
 * Can represent either a Folder (Branch) or a ChatSession (Leaf).
 */
public class ChatNode {
    public enum Type {
        FOLDER,
        CHAT
    }

    private final Type type;
    private final ChatFolder folder;
    private final ChatSession chat;

    // Constructor for Folder Node
    public ChatNode(ChatFolder folder) {
        this.type = Type.FOLDER;
        this.folder = folder;
        this.chat = null;
    }

    // Constructor for Chat Node
    public ChatNode(ChatSession chat) {
        this.type = Type.CHAT;
        this.chat = chat;
        this.folder = null;
    }

    public Type getType() {
        return type;
    }

    public ChatFolder getFolder() {
        return folder;
    }

    public ChatSession getChat() {
        return chat;
    }

    @Override
    public String toString() {
        if (type == Type.FOLDER) {
            return folder != null ? folder.getName() : "Root";
        } else {
            return chat != null ? chat.getName() : "Unknown Chat";
        }
    }
}
