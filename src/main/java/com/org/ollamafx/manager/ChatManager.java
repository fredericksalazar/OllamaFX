package com.org.ollamafx.manager;

import com.org.ollamafx.model.ChatSession;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;

import java.util.Comparator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;

public class ChatManager {
    private static ChatManager instance;
    private final ObservableList<ChatSession> chatSessions;
    private final SortedList<ChatSession> sortedSessions;

    private final File storageDir;
    private final ObjectMapper objectMapper;

    private ChatManager() {
        chatSessions = FXCollections.observableArrayList();

        // Sort by Pinned (descending) then by Creation Date (descending)
        sortedSessions = new SortedList<>(chatSessions, (c1, c2) -> {
            if (c1.isPinned() != c2.isPinned()) {
                return c1.isPinned() ? -1 : 1; // Pinned first
            }
            return c2.getCreationDate().compareTo(c1.getCreationDate()); // Newest first
        });

        // Setup storage
        String userHome = System.getProperty("user.home");
        storageDir = new File(userHome, ".OllamaFX/chats");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public static ChatManager getInstance() {
        if (instance == null) {
            instance = new ChatManager();
        }
        return instance;
    }

    public ObservableList<ChatSession> getChatSessions() {
        return sortedSessions;
    }

    public ChatSession createChat(String name) {
        ChatSession session = new ChatSession(name);
        setupSessionListeners(session);
        chatSessions.add(session);
        saveChat(session); // Save immediately
        return session;
    }

    private void setupSessionListeners(ChatSession session) {
        session.pinnedProperty().addListener((obs, oldVal, newVal) -> {
            int index = chatSessions.indexOf(session);
            if (index >= 0) {
                chatSessions.set(index, session); // Force re-sort
            }
            saveChat(session); // Save on pin change
        });
        session.nameProperty().addListener((obs, oldVal, newVal) -> saveChat(session)); // Save on rename
        session.modelNameProperty().addListener((obs, oldVal, newVal) -> saveChat(session)); // Save on model change
    }

    public void deleteChat(ChatSession session) {
        chatSessions.remove(session);
        File file = new File(storageDir, session.getId().toString() + ".json");
        if (file.exists()) {
            file.delete();
        }
    }

    public void renameChat(ChatSession session, String newName) {
        session.setName(newName);
        // Listener handles save
    }

    public void togglePin(ChatSession session) {
        session.setPinned(!session.isPinned());
        // Listener handles save
    }

    public void saveChats() {
        for (ChatSession session : chatSessions) {
            saveChat(session);
        }
    }

    private void saveChat(ChatSession session) {
        try {
            File file = new File(storageDir, session.getId().toString() + ".json");
            objectMapper.writeValue(file, session);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadChats() {
        chatSessions.clear();
        File[] files = storageDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                try {
                    ChatSession session = objectMapper.readValue(file, ChatSession.class);
                    setupSessionListeners(session);
                    chatSessions.add(session);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("Failed to load chat: " + file.getName());
                }
            }
        }
    }
}
