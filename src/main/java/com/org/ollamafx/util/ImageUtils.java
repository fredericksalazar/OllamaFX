package com.org.ollamafx.util;

import javafx.scene.image.Image;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Set;

/**
 * Utility class for image conversion and validation for multimodal chat
 * support.
 */
public class ImageUtils {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10 MB

    /**
     * Converts a file to a Base64-encoded string (raw, no data: prefix).
     * Ollama API expects raw base64.
     */
    public static String toBase64(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Validates that the file is a supported image format and within size limits.
     */
    public static boolean isValidImageFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return false;
        }
        String ext = getExtension(file.getName());
        return SUPPORTED_EXTENSIONS.contains(ext) && file.length() <= MAX_FILE_SIZE;
    }

    /**
     * Checks if the file extension is a supported image format.
     */
    public static boolean isSupportedFormat(File file) {
        return SUPPORTED_EXTENSIONS.contains(getExtension(file.getName()));
    }

    /**
     * Checks if the file exceeds the maximum allowed size.
     */
    public static boolean isFileTooLarge(File file) {
        return file.length() > MAX_FILE_SIZE;
    }

    /**
     * Creates a thumbnail preview of the image at the given size.
     *
     * @param file The image file
     * @param size The target width and height (square)
     * @return A JavaFX Image scaled for preview
     */
    public static Image createThumbnail(File file, int size) {
        return new Image(file.toURI().toString(), size, size, true, true);
    }

    private static String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase();
    }
}
