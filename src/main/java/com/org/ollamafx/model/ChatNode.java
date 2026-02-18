package com.org.ollamafx.model;

/**
 * Wrapper class for TreeView items.
 * Can represent either a Folder (Branch) or a ChatSession (Leaf).
 */
public class ChatNode {
    public enum Type {
        FOLDER,
        SMART_COLLECTION,
        CHAT
    }

    private final Type type;
    private final ChatFolder folder;
    private final SmartCollection smartCollection;
    private final ChatSession chat;

    // Constructor for Folder Node
    public ChatNode(ChatFolder folder) {
        this.type = Type.FOLDER;
        this.folder = folder;
        this.chat = null;
        this.smartCollection = null;
    }

    // Constructor for Smart Collection Node
    public ChatNode(SmartCollection smartCollection) {
        this.type = Type.SMART_COLLECTION;
        this.smartCollection = smartCollection;
        this.folder = null;
        this.chat = null;
    }

    // Constructor for Chat Node
    public ChatNode(ChatSession chat) {
        this.type = Type.CHAT;
        this.chat = chat;
        this.folder = null;
        this.smartCollection = null;
    }

    public Type getType() {
        return type;
    }

    public ChatFolder getFolder() {
        return folder;
    }

    public SmartCollection getSmartCollection() {
        return smartCollection;
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
