package com.org.ollamafx.manager;

import java.util.prefs.Preferences;

public class ConfigManager {

    private static ConfigManager instance;
    private final Preferences prefs;

    private static final String KEY_OLLAMA_HOST = "ollama_host";
    private static final String KEY_THEME = "app_theme";
    private static final String DEFAULT_HOST = "http://localhost:11434";
    private static final String DEFAULT_THEME = "dark"; // dark or light

    private ConfigManager() {
        prefs = Preferences.userNodeForPackage(ConfigManager.class);
    }

    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    public String getOllamaHost() {
        return prefs.get(KEY_OLLAMA_HOST, DEFAULT_HOST);
    }

    public void setOllamaHost(String host) {
        prefs.put(KEY_OLLAMA_HOST, host);
    }

    public String getTheme() {
        return prefs.get(KEY_THEME, DEFAULT_THEME);
    }

    public void setTheme(String theme) {
        prefs.put(KEY_THEME, theme);
    }
}
