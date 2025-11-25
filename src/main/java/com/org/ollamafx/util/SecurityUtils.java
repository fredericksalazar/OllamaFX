package com.org.ollamafx.util;

import java.util.regex.Pattern;

public class SecurityUtils {

    // Allow alphanumeric characters, hyphens, underscores, colons, and periods.
    // This covers typical model names like "llama3:latest", "gemma:7b", etc.
    private static final Pattern MODEL_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-\\.:]+$");

    /**
     * Validates if the given model name is safe to use in shell commands.
     * 
     * @param modelName The model name to validate.
     * @return true if the model name is valid, false otherwise.
     */
    public static boolean isValidModelName(String modelName) {
        if (modelName == null || modelName.isEmpty()) {
            return false;
        }
        return MODEL_NAME_PATTERN.matcher(modelName).matches();
    }

    /**
     * Sanitizes a string to be safe for display or logging, removing potential
     * control characters.
     * 
     * @param input The input string.
     * @return The sanitized string.
     */
    public static String sanitizeForLog(String input) {
        if (input == null) {
            return "null";
        }
        // Replace newlines and other control characters to prevent log injection
        return input.replaceAll("[\\p{Cntrl}]", "");
    }
}
