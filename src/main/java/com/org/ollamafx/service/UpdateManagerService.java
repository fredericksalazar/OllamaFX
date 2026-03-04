package com.org.ollamafx.service;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UpdateManagerService {

    private final String appRoot;
    private final String updateDir;
    private final String newDir;
    private final String oldDir;
    private final String tempZip;
    private final HttpClient httpClient;

    public UpdateManagerService() {
        this.appRoot = determineAppRoot();
        this.updateDir = appRoot + File.separator + "update";
        this.newDir = updateDir + File.separator + "new";
        this.oldDir = updateDir + File.separator + "old";
        this.tempZip = updateDir + File.separator + "update.zip";
        
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }

    private String determineAppRoot() {
        try {
            // Find where this current class's JAR is located
            URI jarUri = UpdateManagerService.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            File jarFile = new File(jarUri);
            
            // Expected structure is <Root>/lib/OllamaFX.jar
            // If we are running from a JAR in a lib folder, the root is two levels up.
            if (jarFile.getName().endsWith(".jar") && jarFile.getParentFile().getName().equals("lib")) {
                return jarFile.getParentFile().getParentFile().getAbsolutePath();
            } else if (jarFile.getName().endsWith(".jar")) { // Fallback if no lib folder
                 return jarFile.getParentFile().getAbsolutePath();
            }
            // Fallback for IDE development
            return new File(".").getAbsolutePath();
        } catch (Exception e) {
            System.err.println("Could not determine APP_ROOT dynamically. Falling back to working directory.");
            return new File(".").getAbsolutePath();
        }
    }

    /**
     * Step 1: Downloads the ZIP file from GitHub with progress tracking.
     */
    public CompletableFuture<Void> downloadUpdate(String zipUrl, Consumer<Double> progressCallback) {
        return CompletableFuture.runAsync(() -> {
            try {
                prepareDirectories();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(zipUrl))
                        .GET()
                        .build();

                // Get content length for progress calculation
                HttpResponse<Void> headResponse = httpClient.send(
                        HttpRequest.newBuilder(request.uri()).method("HEAD", HttpRequest.BodyPublishers.noBody()).build(), 
                        HttpResponse.BodyHandlers.discarding());
                        
                long contentLength = headResponse.headers().firstValueAsLong("Content-Length").orElse(-1L);

                HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

                try (InputStream inputStream = response.body();
                     OutputStream outputStream = new FileOutputStream(tempZip)) {

                    byte[] buffer = new byte[8192];
                    long totalBytesRead = 0;
                    int bytesRead;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;

                        if (contentLength > 0 && progressCallback != null) {
                            double progress = (double) totalBytesRead / contentLength;
                            progressCallback.accept(progress);
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to download update zip from " + zipUrl, e);
            }
        });
    }

    /**
     * Step 2: Unzips the downloaded file into the `update/new/` directory.
     * Note: The GitHub distribution ZIP contains a root folder (e.g., `OllamaFX-0.6.0-Dist`).
     * We need to extract the *contents* of that root folder directly into `update/new/`.
     */
    public void extractUpdate() throws IOException {
        System.out.println("[UpdateManager] Starting update extraction...");
        Path zipFilePath = Paths.get(tempZip);
        Path targetDir = Paths.get(newDir);
        System.out.println("[UpdateManager] Zip Path: " + zipFilePath);
        System.out.println("[UpdateManager] Target Dir: " + targetDir);

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath.toFile()))) {
            ZipEntry zipEntry = zis.getNextEntry();
            
            String rootFolderName = null;

            while (zipEntry != null) {
                String entryName = zipEntry.getName();
                
                // Identify the root folder name inside the zip to strip it out later
                if (rootFolderName == null) {
                    int firstSlash = entryName.indexOf('/');
                    if (firstSlash != -1) {
                        String potentialRoot = entryName.substring(0, firstSlash + 1);
                        if (potentialRoot.toLowerCase().contains("ollamafx")) {
                            rootFolderName = potentialRoot;
                            System.out.println("[UpdateManager] Identified root wrapper folder: " + rootFolderName);
                        } else {
                            rootFolderName = "";
                            System.out.println("[UpdateManager] No expected root wrapper folder found.");
                        }
                    } else {
                        rootFolderName = "";
                        System.out.println("[UpdateManager] Zip start with file. No root folder assumed.");
                    }
                }

                // Strip the root folder from the extraction path
                String strippedName = rootFolderName.isEmpty() ? entryName : entryName.replaceFirst("^" + rootFolderName, "");
                
                if (!strippedName.isEmpty()) {
                    System.out.println("[UpdateManager] Extracting: " + strippedName);
                    File newFile = new File(targetDir.toFile(), strippedName);

                    if (zipEntry.isDirectory()) {
                        newFile.mkdirs();
                    } else {
                        newFile.getParentFile().mkdirs();
                        try (FileOutputStream fos = new FileOutputStream(newFile)) {
                            byte[] buffer = new byte[8192];
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }
                    }
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        }
        System.out.println("[UpdateManager] Extraction completed.");
    }

    /**
     * Step 3: Backs up the current running environment to `update/old/`
     */
    public void backupCurrentVersion() throws IOException {
        Path sourceDir = Paths.get(appRoot);
        Path backupDir = Paths.get(oldDir);

        // Files to back up natively in our distribution structure
        String[] filesToBackup = {
            "OllamaFX", "OllamaFX.bat", "OllamaFX.command", "OllamaFX.sh"
        };

        for (String file : filesToBackup) {
            Path sourceFile = sourceDir.resolve(file);
            if (Files.exists(sourceFile)) {
                Files.copy(sourceFile, backupDir.resolve(file), StandardCopyOption.REPLACE_EXISTING);
            }
        }

        // Backup the lib folder containing the JAR
        Path sourceLib = sourceDir.resolve("lib");
        if (Files.exists(sourceLib)) {
            copyFolder(sourceLib, backupDir.resolve("lib"));
        }
    }

    /**
     * Step 4: Triggers the external script to perform the hot swap and restarts the app.
     */
    public void applyAndRestart() throws IOException {
        System.out.println("[UpdateManager] Starting applyAndRestart()...");
        String os = System.getProperty("os.name").toLowerCase();
        System.out.println("[UpdateManager] OS detected: " + os);

        ProcessBuilder builder = new ProcessBuilder();
        File uDir = new File(updateDir);

        if (!uDir.exists()) {
            System.err.println("[UpdateManager] updateDir does not exist!");
            throw new IOException("Update directory not found at " + updateDir + ", cannot apply update.");
        }

        if (os.contains("win")) {
            builder.command("cmd.exe", "/c", "start", "\"\"", "apply_update.bat");
        } else if (os.contains("mac")) {
            System.out.println("[UpdateManager] Setting command to: /bin/bash apply_update.command");
            builder.command("/bin/bash", "apply_update.command");
        } else {
            System.out.println("[UpdateManager] Setting command to: /bin/bash apply_update.sh");
            builder.command("/bin/bash", "apply_update.sh");
        }

        builder.directory(uDir);
        
        // Redirect output to a temp file, which helps avoid SIGPIPE if script prints after JVM exits
        File logFile = new File(appRoot, "update_script_debug.log");
        builder.redirectErrorStream(true);
        builder.redirectOutput(ProcessBuilder.Redirect.to(logFile));

        System.out.println("[UpdateManager] Launching script process in directory: " + uDir.getAbsolutePath());
        System.out.println("[UpdateManager] Log output will be in: " + logFile.getAbsolutePath());
        
        builder.start();

        System.out.println("[UpdateManager] Script started. Calling System.exit(0)...");
        // System exit allows the OS script to take over
        System.exit(0);
    }

    public String getAppRoot() { return appRoot; }
    public String getNewDir() { return newDir; }
    public String getTempZip() { return tempZip; }

    // --- Utility Methods ---

    private void prepareDirectories() throws IOException {
        Path newDirPath = Paths.get(newDir);
        Path oldDirPath = Paths.get(oldDir);

        deleteFolder(newDirPath);
        deleteFolder(oldDirPath);

        Files.createDirectories(newDirPath);
        Files.createDirectories(oldDirPath);
    }

    private void copyFolder(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public void deleteFolder(Path folder) throws IOException {
        if (Files.exists(folder)) {
            Files.walkFileTree(folder, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}
