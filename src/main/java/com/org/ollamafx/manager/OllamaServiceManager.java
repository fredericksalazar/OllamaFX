package com.org.ollamafx.manager;

import java.io.IOException;
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
     * (11434). Uses 127.0.0.1 to avoid DNS issues.
     */
    public boolean isRunning() {
        try (Socket socket = new Socket()) {
            // Use 127.0.0.1 instead of localhost for robustness
            socket.connect(new InetSocketAddress("127.0.0.1", 11434), 1000); // 1000ms timeout
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Attempts to start the Ollama service.
     */
    public boolean startOllama() {
        if (isRunning()) {
            return true;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder("ollama", "serve");
            pb.redirectErrorStream(true);

            // Inherit IO might be useful for debug but can clutter app logs.
            // pb.inheritIO();

            this.ollamaProcess = pb.start();

            // Give it a moment to spin up
            int Retries = 10;
            while (Retries > 0) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }

                if (isRunning()) {
                    return true;
                }
                Retries--;
            }
            return false; // Timed out
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Attempts to stop the locally managed Ollama process.
     */
    public void stopOllama() {
        if (this.ollamaProcess != null && this.ollamaProcess.isAlive()) {
            this.ollamaProcess.destroy(); // SIGTERM
        } else {
            // No process to stop
        }
    }
}
