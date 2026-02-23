package com.org.ollamafx.util;

import java.text.DecimalFormat;

public class Utils {

    public static Long parseSize(String sizeStr) {
        if (sizeStr == null || sizeStr.isEmpty() || sizeStr.equals("N/A")) {
            return -1L;
        }
        try {
            String[] parts = sizeStr.trim().split("\\s+");
            if (parts.length < 2)
                return 0L;

            double value = Double.parseDouble(parts[0]);
            String unit = parts[1].toUpperCase();

            long multiplier = 1;
            switch (unit) {
                case "KB":
                    multiplier = 1024;
                    break;
                case "MB":
                    multiplier = 1024 * 1024;
                    break;
                case "GB":
                    multiplier = 1024 * 1024 * 1024;
                    break;
                case "TB":
                    multiplier = 1024L * 1024 * 1024 * 1024;
                    break;
            }

            return (long) (value * multiplier);
        } catch (Exception e) {
            return 0L;
        }
    }

    public static String formatSize(long size) {
        if (size <= 0)
            return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static void showError(String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        // Add style if available
        javafx.scene.control.DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStyleClass().add("dialog");
        String userAgentStylesheet = javafx.application.Application.getUserAgentStylesheet();
        if (userAgentStylesheet != null && userAgentStylesheet.toLowerCase().contains("light")) {
            dialogPane.getStyleClass().add("light");
        }

        alert.showAndWait();
    }

    public static long parseDownloadCount(String downloadStr) {
        if (downloadStr == null || downloadStr.isEmpty() || downloadStr.equals("N/A")) {
            return 0;
        }

        String raw = downloadStr.toUpperCase().replace("DOWNLOADS", "").replace("PULLS", "").trim();
        double multiplier = 1;

        if (raw.endsWith("M")) {
            multiplier = 1_000_000;
            raw = raw.substring(0, raw.length() - 1);
        } else if (raw.endsWith("K")) {
            multiplier = 1_000;
            raw = raw.substring(0, raw.length() - 1);
        } else if (raw.endsWith("B")) { // Billion, unlikely but possible
            multiplier = 1_000_000_000;
            raw = raw.substring(0, raw.length() - 1);
        }

        try {
            return (long) (Double.parseDouble(raw) * multiplier);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Parses a relative date string (e.g. "2 days ago", "1 month ago") into a
     * timestamp for sorting.
     * Returns current time minus the offset.
     */
    public static long parseRelativeDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty() || dateStr.equals("N/A")) {
            return 0;
        }

        // Very basic parsing for sorting purposes
        long now = System.currentTimeMillis();
        long oneHour = 3600 * 1000L;
        long oneDay = 24 * oneHour;
        long oneMonth = 30 * oneDay;
        long oneYear = 365 * oneDay;

        String lower = dateStr.toLowerCase();
        try {
            String[] parts = lower.split("\\s+");
            if (parts.length < 2)
                return 0;

            long val = Long.parseLong(parts[0]);
            String unit = parts[1]; // hours, days, months

            if (unit.startsWith("second") || unit.startsWith("minute"))
                return now; // Very new
            if (unit.startsWith("hour"))
                return now - (val * oneHour);
            if (unit.startsWith("day"))
                return now - (val * oneDay);
            if (unit.startsWith("week"))
                return now - (val * 7 * oneDay); // approx
            if (unit.startsWith("month"))
                return now - (val * oneMonth);
            if (unit.startsWith("year"))
                return now - (val * oneYear);

            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Resuelve la ruta al ejecutable de Ollama.
     * En aplicaciones empaquetadas en macOS, el PATH no incluye /usr/local/bin o
     * /opt/homebrew/bin.
     */
    public static String getOllamaExecutable() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac") || os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            java.io.File homebrewPath = new java.io.File("/opt/homebrew/bin/ollama");
            if (homebrewPath.exists() && homebrewPath.canExecute()) {
                return homebrewPath.getAbsolutePath();
            }
            java.io.File usrLocalPath = new java.io.File("/usr/local/bin/ollama");
            if (usrLocalPath.exists() && usrLocalPath.canExecute()) {
                return usrLocalPath.getAbsolutePath();
            }
        }
        // Fallback al PATH del sistema (funciona en terminal o Windows)
        return "ollama";
    }
}
