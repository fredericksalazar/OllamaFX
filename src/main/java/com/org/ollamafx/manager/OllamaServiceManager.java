package com.org.ollamafx.manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class OllamaServiceManager {

    private static OllamaServiceManager instance;
    private Process ollamaProcess;

    private OllamaServiceManager() {
    }

    public static OllamaServiceManager getInstance() {
        if (instance == null) {
            instance = new OllamaServiceManager();
        }
        return instance;
    }

    /**
     * Checks if Ollama CLI is installed and accessible in the system PATH.
     */
    public boolean isInstalled() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ollama", "--version");
            Process p = pb.start();
            boolean finished = p.waitFor(5, TimeUnit.SECONDS);
            return finished && p.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    /**
     * Checks if the Ollama service is currently running on the default port
     * (11434).
     */
    public boolean isRunning() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 11434), 500); // 500ms timeout
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Attempts to start the Ollama service.
     */
    public boolean startOllama() {
        if (isRunning())
            return true;

        System.out.println("OllamaServiceManager: Attempting to start Ollama...");
        try {
            ProcessBuilder pb = new ProcessBuilder("ollama", "serve");
            // Redirect output to inherit IO so we can see logs if run from terminal,
            // or maybe redirect to a specific log file in the future.
            // For now, let's keep it simple.
            pb.redirectErrorStream(true);

            this.ollamaProcess = pb.start();

            // Give it a moment to spin up
            int Retries = 10;
            while (Retries > 0) {
                Thread.sleep(1000);
                if (isRunning()) {
                    System.out.println("OllamaServiceManager: Ollama started successfully.");
                    return true;
                }
                Retries--;
            }
            return false; // Timed out
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Attempts to stop the locally managed Ollama process.
     * Note: This only works if WE started it. Stopping a system-wide service is
     * harder/requires admin.
     * However, the requirement is "Start/Stop" button.
     * If we didn't start it, we can't easily kill it without 'pkill' which might be
     * aggressive.
     * Let's try to kill the process we hold reference to first.
     */
    public void stopOllama() {
        if (this.ollamaProcess != null && this.ollamaProcess.isAlive()) {
            System.out.println("OllamaServiceManager: Stopping local Ollama process...");
            this.ollamaProcess.destroy(); // SIGTERM
        } else {
            // Try explicit kill if we want to be aggressive (User request: "botón para
            // iniciar o apagar")
            // This is useful if the user started it manually or via another instance.
            // On Mac/Linux:
            try {
                System.out.println("OllamaServiceManager: Attempting to kill system ollama process...");
                ProcessBuilder pb = new ProcessBuilder("pkill", "ollama");
                pb.start().waitFor();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
