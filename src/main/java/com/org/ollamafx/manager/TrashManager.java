package com.org.ollamafx.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.org.ollamafx.model.ChatFolder;
import com.org.ollamafx.model.ChatSession;
import com.org.ollamafx.model.TrashItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestiona la papelera de reciclaje de OllamaFX.
 * Los items eliminados se mueven aquí antes de ser borrados físicamente.
 * Persiste en ~/.OllamaFX/trash.json
 */
public class TrashManager {

    private static TrashManager instance;

    private final ObservableList<TrashItem> trashItems;
    private final File storageFile;
    private final File chatStorageDir;
    private final ObjectMapper objectMapper;
    private final List<Runnable> updateListeners = new ArrayList<>();

    private TrashManager() {
        trashItems = FXCollections.observableArrayList();

        String userHome = System.getProperty("user.home");
        File storageDir = new File(userHome, ".OllamaFX");
        storageDir.mkdirs();

        storageFile = new File(storageDir, "trash.json");
        chatStorageDir = new File(storageDir, "chats");

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        load();
        cleanOldItems(); // Limpiar items antiguos al inicio
    }

    public static synchronized TrashManager getInstance() {
        if (instance == null) {
            instance = new TrashManager();
        }
        return instance;
    }

    public ObservableList<TrashItem> getTrashItems() {
        return trashItems;
    }

    public boolean isChatInTrash(String chatId) {
        for (TrashItem item : trashItems) {
            if (item.getType() == TrashItem.ItemType.CHAT && item.getChat() != null) {
                if (item.getChat().getId().toString().equals(chatId)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void addUpdateListener(Runnable listener) {
        updateListeners.add(listener);
    }

    private void notifyUpdate() {
        for (Runnable l : updateListeners)
            l.run();
    }

    // ─── Mover a papelera ────────────────────────────────────────────────────

    /**
     * Mueve un chat a la papelera. No borra el archivo físico.
     * Remueve el chat de ChatManager y de su carpeta en CollectionManager.
     */
    public void trashChat(ChatSession chat) {
        ChatCollectionManager cm = ChatCollectionManager.getInstance();
        ChatFolder originalFolder = cm.getFolderForChat(chat);
        String originalFolderId = originalFolder != null ? originalFolder.getId() : null;

        // Remover de la colección (sin borrar archivo)
        cm.removeChatFromFolder(chat);
        ChatManager.getInstance().removeChatFromList(chat);

        TrashItem item = TrashItem.forChat(chat, originalFolderId);
        trashItems.add(0, item); // más reciente primero
        save();
        notifyUpdate();
    }

    /**
     * Mueve una carpeta y todos sus chats a la papelera.
     */
    public void trashFolder(ChatFolder folder) {
        ChatCollectionManager cm = ChatCollectionManager.getInstance();
        ChatManager chatManager = ChatManager.getInstance();

        // Mover cada chat de la carpeta a la papelera individualmente
        List<String> chatIds = new ArrayList<>(folder.getChatIds());
        for (String chatId : chatIds) {
            ChatSession chat = chatManager.getChatSessions().stream()
                    .filter(c -> c.getId().toString().equals(chatId))
                    .findFirst().orElse(null);
            if (chat != null) {
                cm.removeChatFromFolder(chat);
                chatManager.removeChatFromList(chat);
                TrashItem chatItem = TrashItem.forChat(chat, folder.getId());
                trashItems.add(0, chatItem);
            }
        }

        // Mover la carpeta a la papelera
        cm.removeFolderFromList(folder);
        TrashItem folderItem = TrashItem.forFolder(folder);
        trashItems.add(0, folderItem);

        save();
        notifyUpdate();
    }

    // ─── Restaurar ───────────────────────────────────────────────────────────

    /**
     * Restaura un chat desde la papelera.
     * Lo devuelve a su carpeta original si aún existe, o a Uncategorized.
     */
    public void restoreChat(TrashItem item) {
        if (item.getType() != TrashItem.ItemType.CHAT || item.getChat() == null)
            return;

        ChatSession chat = item.getChat();
        ChatManager.getInstance().restoreChatToList(chat);

        // Intentar restaurar a la carpeta original
        if (item.getOriginalFolderId() != null) {
            ChatFolder originalFolder = ChatCollectionManager.getInstance().getFolders().stream()
                    .filter(f -> f.getId().equals(item.getOriginalFolderId()))
                    .findFirst().orElse(null);
            if (originalFolder != null) {
                ChatCollectionManager.getInstance().addChatToFolder(chat, originalFolder);
            }
        }

        trashItems.remove(item);
        save();
        notifyUpdate();
    }

    /**
     * Restaura una carpeta desde la papelera.
     * Re-agrega la carpeta. Sus chats (si también están en papelera) se restauran
     * automáticamente.
     */
    public void restoreFolder(TrashItem item) {
        if (item.getType() != TrashItem.ItemType.FOLDER || item.getFolder() == null)
            return;

        ChatFolder folder = item.getFolder();
        ChatCollectionManager.getInstance().restoreFolderToList(folder);

        // Restaurar chats que pertenecían a esta carpeta y aún están en la papelera
        List<TrashItem> chatsToRestore = new ArrayList<>();
        for (TrashItem ti : trashItems) {
            if (ti.getType() == TrashItem.ItemType.CHAT
                    && folder.getId().equals(ti.getOriginalFolderId())) {
                chatsToRestore.add(ti);
            }
        }
        for (TrashItem chatItem : chatsToRestore) {
            ChatSession chat = chatItem.getChat();
            ChatManager.getInstance().restoreChatToList(chat);
            ChatCollectionManager.getInstance().addChatToFolder(chat, folder);
            trashItems.remove(chatItem);
        }

        trashItems.remove(item);
        save();
        notifyUpdate();
    }

    // ─── Eliminar permanentemente ─────────────────────────────────────────────

    /** Elimina un chat permanentemente (borra el archivo físico). */
    public void permanentlyDeleteChat(TrashItem item) {
        if (item.getType() != TrashItem.ItemType.CHAT || item.getChat() == null)
            return;
        File file = new File(chatStorageDir, item.getChat().getId().toString() + ".json");
        if (file.exists())
            file.delete();
        trashItems.remove(item);
        save();
        notifyUpdate();
    }

    /** Elimina una carpeta permanentemente. */
    public void permanentlyDeleteFolder(TrashItem item) {
        if (item.getType() != TrashItem.ItemType.FOLDER)
            return;
        trashItems.remove(item);
        save();
        notifyUpdate();
    }

    /** Vacía toda la papelera permanentemente. */
    public void emptyTrash() {
        // Borrar archivos físicos de todos los chats en papelera
        for (TrashItem item : new ArrayList<>(trashItems)) {
            if (item.getType() == TrashItem.ItemType.CHAT && item.getChat() != null) {
                File file = new File(chatStorageDir, item.getChat().getId().toString() + ".json");
                if (file.exists())
                    file.delete();
            }
        }
        trashItems.clear();
        save();
        notifyUpdate();
    }

    // ─── Persistencia ────────────────────────────────────────────────────────

    private void save() {
        try {
            objectMapper.writeValue(storageFile, new ArrayList<>(trashItems));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void load() {
        trashItems.clear();
        if (!storageFile.exists())
            return;
        try {
            List<TrashItem> loaded = objectMapper.readValue(storageFile,
                    new TypeReference<List<TrashItem>>() {
                    });
            trashItems.addAll(loaded);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Elimina automáticamente items que tengan más de 30 días en la papelera.
     */
    private void cleanOldItems() {
        List<TrashItem> toDelete = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (TrashItem item : trashItems) {
            try {
                LocalDateTime deletedAt = LocalDateTime.parse(item.getDeletedAt());
                long days = ChronoUnit.DAYS.between(deletedAt, now);
                if (days >= 30) {
                    toDelete.add(item);
                }
            } catch (Exception e) {
                // Si falla el parseo de fecha, ignorar o eliminar si es muy antiguo/inválido?
                // Mejor dejarlo por seguridad.
                e.printStackTrace();
            }
        }

        // Eliminar permanentemente
        for (TrashItem item : toDelete) {
            if (item.getType() == TrashItem.ItemType.CHAT) {
                permanentlyDeleteChat(item);
            } else {
                permanentlyDeleteFolder(item);
            }
        }
    }
}
