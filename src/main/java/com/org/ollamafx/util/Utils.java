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
}
