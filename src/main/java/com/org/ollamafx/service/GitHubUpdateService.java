package com.org.ollamafx.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service to manage GitHub API calls for finding new releases.
 */
public class GitHubUpdateService {

    private static final String REPO_OWNER = "fredericksalazar";
    private static final String REPO_NAME = "OllamaFX";
    private static final String API_URL = "https://api.github.com/repos/" + REPO_OWNER + "/" + REPO_NAME + "/releases/latest";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static class ReleaseInfo {
        public final String versionTag;
        public final String downloadUrl;
        public final String releaseNotes;

        public ReleaseInfo(String versionTag, String downloadUrl, String releaseNotes) {
            this.versionTag = versionTag;
            this.downloadUrl = downloadUrl;
            this.releaseNotes = releaseNotes;
        }
    }

    /**
     * Fetches the latest release asynchronously.
     */
    public CompletableFuture<ReleaseInfo> fetchLatestRelease() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Accept", "application/vnd.github.v3+json")
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("GitHub API returned status code: " + response.statusCode());
                    }
                    try {
                        JsonNode root = objectMapper.readTree(response.body());
                        String tag = root.path("tag_name").asText("");
                        String body = root.path("body").asText("");
                        
                        // Default fallback
                        String zipUrl = null;
                        
                        // Find the appropriate asset
                        JsonNode assets = root.path("assets");
                        if (assets.isArray()) {
                            for (JsonNode asset : assets) {
                                String name = asset.path("name").asText("").toLowerCase();
                                if (name.endsWith(".zip")) {
                                    zipUrl = asset.path("browser_download_url").asText("");
                                    break;
                                }
                            }
                        }

                        if (tag.isEmpty() || zipUrl == null || zipUrl.isEmpty()) {
                            throw new RuntimeException("Could not parse critical release info from JSON.");
                        }

                        return new ReleaseInfo(tag, zipUrl, body);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to parse GitHub JSON response", e);
                    }
                });
    }

    /**
     * Helper to compare semantic versions.
     * Returns true if remoteVersion is newer than currentVersion.
     * Handles v prefixes (e.g. v0.5.0 vs 0.4.0)
     */
    public boolean isUpdateAvailable(String currentVersion, String remoteVersion) {
        if (currentVersion == null || remoteVersion == null) return false;
        
        String cleanCurrent = currentVersion.replace("v", "").replace("V", "").trim();
        String cleanRemote = remoteVersion.replace("v", "").replace("V", "").trim();
        
        if (cleanCurrent.equalsIgnoreCase("Unknown") || cleanCurrent.isEmpty()) return false;
        if (cleanCurrent.equals(cleanRemote)) return false;

        String[] currentParts = cleanCurrent.split("\\.");
        String[] remoteParts = cleanRemote.split("\\.");

        int length = Math.max(currentParts.length, remoteParts.length);
        for (int i = 0; i < length; i++) {
            int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            int remotePart = i < remoteParts.length ? Integer.parseInt(remoteParts[i]) : 0;
            
            if (remotePart > currentPart) return true;
            if (remotePart < currentPart) return false;
        }

        return false;
    }
}
