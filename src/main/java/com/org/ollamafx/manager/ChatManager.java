package com.org.ollamafx.manager;

import com.org.ollamafx.model.ChatSession;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;

import java.util.Comparator;

public class ChatManager {
    private static ChatManager instance;
    private final ObservableList<ChatSession> chatSessions;
    private final SortedList<ChatSession> sortedSessions;

    private ChatManager() {
        chatSessions = FXCollections.observableArrayList();

        // Sort by Pinned (descending) then by Creation Date (descending)
        sortedSessions = new SortedList<>(chatSessions, (c1, c2) -> {
            if (c1.isPinned() != c2.isPinned()) {
                return c1.isPinned() ? -1 : 1; // Pinned first
            }
            return c2.getCreationDate().compareTo(c1.getCreationDate()); // Newest first
        });
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
        // Listen for property changes to re-sort
        session.pinnedProperty().addListener((obs, oldVal, newVal) -> {
            // Trigger sort update (SortedList usually handles this if configured,
            // but sometimes needs a nudge or we rely on the list update)
            // For simple SortedList over ObservableList, property updates might not trigger
            // resort
            // unless using an extractor. For now, we'll remove and add to force resort if
            // needed,
            // or better, use an extractor in the ObservableList constructor.
            // Let's keep it simple: The UI binding usually handles the view update,
            // but the sort order needs the comparator to re-evaluate.
            // A common trick is to modify the list or use an extractor.
            // We will use an extractor approach in a refactor if this doesn't auto-update.
            // Actually, let's just re-set the comparator to force re-sort or use an
            // extractor.
            chatSessions.sort((c1, c2) -> 0); // Dummy sort to trigger events? No.
            // Let's try the extractor pattern in the constructor next time.
            // For now, we will just let it be, and if it doesn't sort on pin click, we'll
            // fix it.
            // Actually, let's fix it now by removing and re-adding the item to the source
            // list (hacky but works)
            // or better:
            int index = chatSessions.indexOf(session);
            chatSessions.set(index, session);
        });
        chatSessions.add(session);
        return session;
    }

    public void deleteChat(ChatSession session) {
        chatSessions.remove(session);
    }

    public void renameChat(ChatSession session, String newName) {
        session.setName(newName);
    }

    public void togglePin(ChatSession session) {
        session.setPinned(!session.isPinned());
    }
}
