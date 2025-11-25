package com.org.ollamafx.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.time.LocalDateTime;
import java.util.UUID;

public class ChatSession {
    private final UUID id;
    private final StringProperty name;
    private final BooleanProperty pinned;
    private final LocalDateTime creationDate;

    public ChatSession(String name) {
        this.id = UUID.randomUUID();
        this.name = new SimpleStringProperty(name);
        this.pinned = new SimpleBooleanProperty(false);
        this.creationDate = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public boolean isPinned() {
        return pinned.get();
    }

    public BooleanProperty pinnedProperty() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned.set(pinned);
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    @Override
    public String toString() {
        return getName();
    }
}
