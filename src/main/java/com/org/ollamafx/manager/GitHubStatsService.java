package com.org.ollamafx.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Service to fetch GitHub repository statistics asynchronously.
 * Uses GitHub API to retrieve download counts from releases.
 */
public class GitHubStatsService {

    private static GitHubStatsService instance;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private static final String RELEASES_API_URL = "https://api.github.com/repos/fredericksalazar/OllamaFX/releases";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private GitHubStatsService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public static synchronized GitHubStatsService getInstance() {
        if (instance == null) {
            instance = new GitHubStatsService();
        }
        return instance;
    }

    /**
     * Fetches total download count from all assets of the latest release.
     * 
     * @return CompletableFuture with total downloads, or -1 if error occurs
     */
    public CompletableFuture<Integer> fetchTotalDownloads() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(RELEASES_API_URL))
                        .timeout(REQUEST_TIMEOUT)
                        .header("Accept", "application/vnd.github.v3+json")
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    System.err.println("GitHub API returned status: " + response.statusCode());
                    return -1;
                }

                return parseDownloadCount(response.body());

            } catch (Exception e) {
                System.err.println("Error fetching GitHub stats: " + e.getMessage());
                return -1;
            }
        });
    }

    /**
     * Parses the JSON response to extract total download count.
     * Sums download_count from all assets in the latest (first) release.
     * 
     * @param jsonResponse JSON string from GitHub API
     * @return total download count, or -1 if parsing fails
     */
    private int parseDownloadCount(String jsonResponse) {
        try {
            JsonNode releases = objectMapper.readTree(jsonResponse);

            if (releases.isArray() && releases.size() > 0) {
                // Get the latest release (first in the array)
                JsonNode latestRelease = releases.get(0);
                JsonNode assets = latestRelease.get("assets");

                if (assets != null && assets.isArray()) {
                    int totalDownloads = 0;

                    for (JsonNode asset : assets) {
                        JsonNode downloadCount = asset.get("download_count");
                        if (downloadCount != null) {
                            totalDownloads += downloadCount.asInt();
                        }
                    }

                    return totalDownloads;
                }
            }

            return -1;

        } catch (Exception e) {
            System.err.println("Error parsing GitHub response: " + e.getMessage());
            return -1;
        }
    }
}
