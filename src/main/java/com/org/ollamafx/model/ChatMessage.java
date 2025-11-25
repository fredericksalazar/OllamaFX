package com.org.ollamafx.model;

import java.time.LocalDateTime;

public class ChatMessage {
    private String role; // "user" or "assistant"
    private String content;
    private String timestamp;

    public ChatMessage() {
        // Default constructor for Jackson
    }

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = LocalDateTime.now().toString();
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
