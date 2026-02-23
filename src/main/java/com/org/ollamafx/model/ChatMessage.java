package com.org.ollamafx.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

public class ChatMessage {
    private String role; // "user" or "assistant"
    private String content;
    private String timestamp;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> images; // Base64-encoded image data (null when text-only)

    public ChatMessage() {
        // Default constructor for Jackson
    }

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = LocalDateTime.now().toString();
    }

    public ChatMessage(String role, String content, List<String> images) {
        this(role, content);
        this.images = (images != null && !images.isEmpty()) ? images : null;
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

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public boolean hasImages() {
        return images != null && !images.isEmpty();
    }
}
