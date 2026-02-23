package com.org.ollamafx.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.org.ollamafx.model.ChatFolder;
import com.org.ollamafx.model.ChatSession;
import com.org.ollamafx.model.SmartCollection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

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
    private final Map<String, ChatFolder> chatFolderMap;
    private final File storageFile;
    private final ObjectMapper objectMapper;
    private final List<Runnable> updateListeners = new ArrayList<>();

    private final ObservableList<SmartCollection> smartCollections;
    private final File smartStorageFile;

    private ChatCollectionManager() {
        folders = FXCollections.observableArrayList();
        smartCollections = FXCollections.observableArrayList(); // Initialize
        chatFolderMap = new HashMap<>();

        String userHome = System.getProperty("user.home");
        File storageDir = new File(userHome, ".OllamaFX");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        storageFile = new File(storageDir, "collections.json");
        smartStorageFile = new File(storageDir, "smart_collections.json"); // New file

        objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        loadCollections();
        loadSmartCollections(); // Load smart collections
    }

    public static synchronized ChatCollectionManager getInstance() {
        if (instance == null) {
            instance = new ChatCollectionManager();
        }
        return instance;
    }

    public ObservableList<ChatFolder> getFolders() {
        return folders;
    }

    /**
     * Devuelve la carpeta que contiene el chat, o null si está en Uncategorized.
     */
    public ChatFolder getFolderForChat(ChatSession chat) {
        if (chat == null)
            return null;
        return chatFolderMap.get(chat.getId().toString());
    }

    public ObservableList<SmartCollection> getSmartCollections() {
        return smartCollections;
    }

    // ... existing listener logic ...
    public void addUpdateListener(Runnable listener) {
        updateListeners.add(listener);
    }

    private void notifyUpdate() {
        for (Runnable listener : updateListeners) {
            listener.run();
        }
    }

    // --- Folder Management ---

    public ChatFolder createFolder(String name) {
        ChatFolder folder = new ChatFolder(name, "#8E8E93");
        folders.add(folder);
        saveCollections();
        notifyUpdate();
        return folder;
    }

    public void deleteFolder(ChatFolder folder) {
        // Delegar a TrashManager — no eliminar directamente
        TrashManager.getInstance().trashFolder(folder);
    }

    /**
     * Remueve la carpeta de la lista activa sin pasar por TrashManager (usado
     * internamente).
     */
    public void removeFolderFromList(ChatFolder folder) {
        folders.remove(folder);
        // Limpiar el mapa de chats
        for (String chatId : folder.getChatIds()) {
            chatFolderMap.remove(chatId);
        }
        saveCollections();
        notifyUpdate();
    }

    /** Restaura una carpeta a la lista activa (usado por TrashManager). */
    public void restoreFolderToList(ChatFolder folder) {
        if (!folders.contains(folder)) {
            folders.add(folder);
            saveCollections();
            notifyUpdate();
        }
    }

    public void addChatToFolder(ChatSession chat, ChatFolder folder) {
        if (folder != null && chat != null) {
            String chatId = chat.getId().toString();
            // Remove from any existing folder first
            ChatFolder existing = chatFolderMap.get(chatId);
            if (existing != null) {
                existing.removeChat(chatId);
            }

            folder.addChat(chatId);
            chatFolderMap.put(chatId, folder);
            saveCollections();
            notifyUpdate();
        }
    }

    public void removeChatFromFolder(ChatSession chat, ChatFolder folder) {
        if (folder != null && chat != null) {
            String chatId = chat.getId().toString();
            folder.removeChat(chatId);
            chatFolderMap.remove(chatId);
            saveCollections();
            notifyUpdate();
        }
    }

    public void removeChatFromFolder(ChatSession chat) {
        if (chat != null) {
            ChatFolder folder = getFolderForChat(chat);
            if (folder != null) {
                removeChatFromFolder(chat, folder);
            }
        }
    }

    public void moveChatToFolder(ChatSession chat, ChatFolder targetFolder) {
        if (chat == null)
            return;
        if (targetFolder == null) {
            // Mover a "Uncategorized" = quitar de cualquier carpeta actual
            removeChatFromFolder(chat);
        } else {
            addChatToFolder(chat, targetFolder);
        }
    }

    public boolean isChatInFolder(ChatSession chat) {
        return chat != null && chatFolderMap.containsKey(chat.getId().toString());
    }

    public void renameFolder(ChatFolder folder, String newName) {
        if (folder != null) {
            folder.setName(newName);
            saveCollections();
            notifyUpdate();
        }
    }

    public void setFolderColor(ChatFolder folder, String color) {
        if (folder != null) {
            folder.setColor(color);
            saveCollections();
            notifyUpdate();
        }
    }

    // --- Smart Collection Management ---

    public SmartCollection createSmartCollection(String name, SmartCollection.Criteria criteria, String value,
            String icon) {
        SmartCollection sc = new SmartCollection(name, criteria, value, icon);
        smartCollections.add(sc);
        saveSmartCollections();
        notifyUpdate();
        return sc;
    }

    public void deleteSmartCollection(SmartCollection sc) {
        smartCollections.remove(sc);
        saveSmartCollections();
        notifyUpdate();
    }

    public void updateSmartCollection(SmartCollection sc) {
        saveSmartCollections();
        notifyUpdate();
    }

    public List<ChatSession> getChatsForSmartCollection(SmartCollection sc) {
        List<ChatSession> allChats = ChatManager.getInstance().getChatSessions();
        List<ChatSession> filtered = new ArrayList<>();

        if (sc == null || sc.getCriteria() == null)
            return filtered;

        switch (sc.getCriteria()) {
            case KEYWORD:
                String keyword = sc.getValue().toLowerCase();
                for (ChatSession chat : allChats) {
                    // Search in title and maybe content? For now just title (name)
                    if (chat.getName() != null && chat.getName().toLowerCase().contains(keyword)) {
                        filtered.add(chat);
                    }
                }
                break;
            case MODEL:
                String model = sc.getValue();
                for (ChatSession chat : allChats) {
                    if (chat.getModelName() != null && chat.getModelName().equalsIgnoreCase(model)) {
                        filtered.add(chat);
                    }
                }
                break;
            case DATE:
                // Value might be "Today", "Last 7 Days", etc.
                // Parsing this requires some logic.
                // For simplified implementation, let's assume value is "7" for days.
                try {
                    int days = Integer.parseInt(sc.getValue());
                    java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusDays(days);
                    // ChatSession needs a creation date or last modified date.
                    // Assuming ChatSession has access to file timestamp or similar.
                    // If ChatSession structure doesn't support it, we might need to skip or infer.
                    // For now, let's skip implementation or add TODO.
                } catch (NumberFormatException e) {
                    // handle specific strings like "Today"
                }
                break;
        }
        return filtered;
    }

    // ... existing folder logic ...

    // --- Persistence ---

    private void saveCollections() {
        try {
            objectMapper.writeValue(storageFile, new ArrayList<>(folders));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveSmartCollections() {
        try {
            objectMapper.writeValue(smartStorageFile, new ArrayList<>(smartCollections));
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
            }
        }

        // Crear carpeta "General" si no existe ninguna carpeta
        boolean hasGeneral = folders.stream()
                .anyMatch(f -> "General".equalsIgnoreCase(f.getName()));
        if (folders.isEmpty() || !hasGeneral) {
            ChatFolder general = new ChatFolder("General", "#8E8E93");
            folders.add(0, general);
            saveCollections();
        }
    }

    private void loadSmartCollections() {
        smartCollections.clear();
        if (smartStorageFile.exists()) {
            try {
                List<SmartCollection> loaded = objectMapper.readValue(smartStorageFile,
                        new TypeReference<List<SmartCollection>>() {
                        });
                smartCollections.addAll(loaded);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
