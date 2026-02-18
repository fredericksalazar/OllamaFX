package com.org.ollamafx.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa un item en la papelera de reciclaje.
 * Puede ser un ChatSession o un ChatFolder eliminado.
 */
public class TrashItem {

    public enum ItemType {
        CHAT, FOLDER
    }

    private String id;
    private ItemType type;
    private String deletedAt; // ISO-8601 timestamp
    private ChatSession chat; // populated if type == CHAT
    private ChatFolder folder; // populated if type == FOLDER
    private String originalFolderId; // carpeta de origen del chat (para restaurar)

    // No-arg constructor for Jackson
    public TrashItem() {
        this.id = UUID.randomUUID().toString();
        this.deletedAt = LocalDateTime.now().toString();
    }

    public static TrashItem forChat(ChatSession chat, String originalFolderId) {
        TrashItem item = new TrashItem();
        item.type = ItemType.CHAT;
        item.chat = chat;
        item.originalFolderId = originalFolderId;
        return item;
    }

    public static TrashItem forFolder(ChatFolder folder) {
        TrashItem item = new TrashItem();
        item.type = ItemType.FOLDER;
        item.folder = folder;
        return item;
    }

    // --- Getters & Setters (Jackson) ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ItemType getType() {
        return type;
    }

    public void setType(ItemType type) {
        this.type = type;
    }

    public String getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(String deletedAt) {
        this.deletedAt = deletedAt;
    }

    public ChatSession getChat() {
        return chat;
    }

    public void setChat(ChatSession chat) {
        this.chat = chat;
    }

    public ChatFolder getFolder() {
        return folder;
    }

    public void setFolder(ChatFolder folder) {
        this.folder = folder;
    }

    public String getOriginalFolderId() {
        return originalFolderId;
    }

    public void setOriginalFolderId(String originalFolderId) {
        this.originalFolderId = originalFolderId;
    }

    /** Nombre para mostrar en la UI */
    public String getDisplayName() {
        if (type == ItemType.CHAT && chat != null)
            return chat.getName();
        if (type == ItemType.FOLDER && folder != null)
            return folder.getName();
        return "Unknown";
    }
}
