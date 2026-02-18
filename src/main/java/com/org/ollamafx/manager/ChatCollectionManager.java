package com.org.ollamafx.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.org.ollamafx.model.ChatFolder;
import com.org.ollamafx.model.ChatSession;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleLongProperty;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ChatCollectionManager {
    private static ChatCollectionManager instance;
    private final ObservableList<ChatFolder> folders;
    private final Map<String, ChatFolder> chatFolderMap; // Fast lookup: ChatID -> Folder

    private final File storageFile;
    private final ObjectMapper objectMapper;

    private ChatCollectionManager() {
        folders = FXCollections.observableArrayList();
        chatFolderMap = new HashMap<>();

        String userHome = System.getProperty("user.home");
        File storageDir = new File(userHome, ".OllamaFX");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        storageFile = new File(storageDir, "collections.json");

        objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        loadCollections();
    }

    public static ChatCollectionManager getInstance() {
        if (instance == null) {
            instance = new ChatCollectionManager();
        }
        return instance;
    }

    public ObservableList<ChatFolder> getFolders() {
        return folders;
    }

    // --- UI Update Signal (Observer Pattern) ---
    private final List<Runnable> updateListeners = new ArrayList<>();

    public void addUpdateListener(Runnable listener) {
        updateListeners.add(listener);
    }

    private void notifyUpdate() {
        for (Runnable listener : updateListeners) {
            listener.run();
        }
    }

    public ChatFolder createFolder(String name) {
        ChatFolder folder = new ChatFolder(name, "#8E8E93");
        folders.add(folder);
        saveCollections();
        notifyUpdate();
        return folder;
    }

    public void deleteFolder(ChatFolder folder) {
        for (String chatId : folder.getChatIds()) {
            chatFolderMap.remove(chatId);
        }
        folders.remove(folder);
        saveCollections();
        notifyUpdate();
    }

    public void renameFolder(ChatFolder folder, String newName) {
        folder.setName(newName);
        saveCollections();
        notifyUpdate();
    }

    public void setFolderColor(ChatFolder folder, String colorHex) {
        folder.setColor(colorHex);
        saveCollections();
        notifyUpdate();
    }

    public void moveChatToFolder(ChatSession chat, ChatFolder targetFolder) {
        if (chat == null)
            return;
        String chatId = chat.getId().toString();

        ChatFolder currentFolder = chatFolderMap.get(chatId);
        if (currentFolder != null) {
            currentFolder.removeChatId(chatId);
            if (currentFolder == targetFolder) {
                return;
            }
        }

        if (targetFolder != null) {
            targetFolder.addChatId(chatId);
            chatFolderMap.put(chatId, targetFolder);
        } else {
            chatFolderMap.remove(chatId);
        }

        saveCollections();
        notifyUpdate();
    }

    public void removeChatFromFolder(ChatSession chat) {
        moveChatToFolder(chat, null);
    }

    public ChatFolder getFolderForChat(ChatSession chat) {
        if (chat == null)
            return null;
        return chatFolderMap.get(chat.getId().toString());
    }

    public boolean isChatInFolder(ChatSession chat) {
        return getFolderForChat(chat) != null;
    }

    // --- Persistence ---

    private void saveCollections() {
        try {
            // We save the list of folders.
            // The map is derived data, so we don't save it directly.
            objectMapper.writeValue(storageFile, new ArrayList<>(folders));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadCollections() {
        folders.clear();
        chatFolderMap.clear();

        if (storageFile.exists()) {
            try {
                List<ChatFolder> loadedFolders = objectMapper.readValue(storageFile,
                        new TypeReference<List<ChatFolder>>() {
                        });
                folders.addAll(loadedFolders);

                // Rebuild lookup map
                for (ChatFolder folder : folders) {
                    for (String chatId : folder.getChatIds()) {
                        chatFolderMap.put(chatId, folder);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Failed to load collections.json");
            }
        }
    }
}
