package com.org.ollamafx.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatFolder {
    private String id;
    private String name;
    private String color; // Hex code or preset name
    private List<String> chatIds;
    private boolean expanded;

    // No-arg constructor for Jackson
    public ChatFolder() {
        this.id = UUID.randomUUID().toString();
        this.chatIds = new ArrayList<>();
        this.expanded = true; // Default expanded
        this.color = "#8E8E93"; // Default Gray
    }

    public ChatFolder(String name, String color) {
        this();
        this.name = name;
        this.color = color;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public List<String> getChatIds() {
        return chatIds;
    }

    public void setChatIds(List<String> chatIds) {
        this.chatIds = chatIds;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    // Helpers
    public void addChatId(String chatId) {
        if (!chatIds.contains(chatId)) {
            chatIds.add(chatId);
        }
    }

    public void removeChatId(String chatId) {
        chatIds.remove(chatId);
    }
}
