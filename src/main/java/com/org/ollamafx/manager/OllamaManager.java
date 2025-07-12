package com.org.ollamafx.manager;


import io.github.ollama4j.OllamaAPI;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class OllamaManager {

    private static OllamaManager instance;
    private OllamaAPI client;

    private OllamaManager(){
        this.client = new OllamaAPI();
    }

    public boolean isOllamaInstalled() {
        try {
            System.out.println("Verifying that ollama is installed ...");
            Process process = new ProcessBuilder("ollama", "--version")
                                    .redirectErrorStream(true)
                                    .start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    public String getOllamaVersion() {
        StringBuilder output = new StringBuilder();
        try {
            Process process = new ProcessBuilder("ollama", "--version")
                    .redirectErrorStream(true)
                    .start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            process.waitFor();
            return output.toString().trim();
        } catch (IOException | InterruptedException e) {
            return null;
        }
    }

    public static OllamaManager getInstance() {
        if (instance == null) {
            instance = new OllamaManager();
        }
        return instance;
    }
}
