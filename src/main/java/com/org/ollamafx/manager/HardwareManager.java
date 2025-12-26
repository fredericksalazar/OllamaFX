package com.org.ollamafx.manager;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import java.util.List;

public class HardwareManager {

    private static final SystemInfo systemInfo = new SystemInfo();
    private static final HardwareAbstractionLayer hardware = systemInfo.getHardware();

    public static String getRamDetails() {
        GlobalMemory memory = hardware.getMemory();
        return String.format("%.2f GB Total / %.2f GB Available",
                bytesToGb(memory.getTotal()),
                bytesToGb(memory.getAvailable()));
    }

    public static String getVramDetails() {
        double vramGb = bytesToGb(getTotalVideoMemory());
        if (isAppleSilicon()) {
            return "Unified Memory (Uses RAM)";
        }
        return String.format("%.2f GB Dedicated", vramGb);
    }

    public static String getCpuDetails() {
        CentralProcessor processor = hardware.getProcessor();
        return processor.getProcessorIdentifier().getName();
    }

    public static String getOsDetails() {
        return System.getProperty("os.name") + " " + System.getProperty("os.version");
    }

    public static long getTotalVideoMemory() {
        long totalVram = 0;
        try {
            List<oshi.hardware.GraphicsCard> gpus = hardware.getGraphicsCards();
            for (oshi.hardware.GraphicsCard gpu : gpus) {
                totalVram += gpu.getVRam();
            }
        } catch (Exception e) {
            System.err.println("Error detecting VRAM: " + e.getMessage());
        }
        return totalVram;
    }

    public static boolean isAppleSilicon() {
        String vendor = hardware.getProcessor().getProcessorIdentifier().getVendor().toLowerCase();
        String name = hardware.getProcessor().getProcessorIdentifier().getName().toLowerCase();
        return vendor.contains("apple") || name.contains("apple");
    }

    public static HardwareStats getStats() {
        long totalRam = hardware.getMemory().getTotal();
        long availableRam = hardware.getMemory().getAvailable();
        long totalVram = getTotalVideoMemory();
        boolean isAppleShortCut = isAppleSilicon();

        // On Apple Silicon, Unified Memory means VRAM ~= RAM
        if (isAppleShortCut) {
            totalVram = totalRam;
        }

        return new HardwareStats(totalRam, availableRam, totalVram, isAppleShortCut);
    }

    public static class HardwareStats {
        public final long totalRamBytes;
        public final long availableRamBytes;
        public final long totalVramBytes;
        public final boolean isUnifiedMemory;

        public HardwareStats(long totalRamBytes, long availableRamBytes, long totalVramBytes, boolean isUnifiedMemory) {
            this.totalRamBytes = totalRamBytes;
            this.availableRamBytes = availableRamBytes;
            this.totalVramBytes = totalVramBytes;
            this.isUnifiedMemory = isUnifiedMemory;
        }

        public double getTotalRamGB() {
            return totalRamBytes / (1024.0 * 1024.0 * 1024.0);
        }

        public double getVramGB() {
            return totalVramBytes / (1024.0 * 1024.0 * 1024.0);
        }
    }

    public static String getHardwareDetails() {
        StringBuilder sb = new StringBuilder();
        sb.append("Hardware Information:\n");
        sb.append("---------------------\n");

        // RAM
        sb.append("RAM: ").append(getRamDetails()).append("\n");

        // VRAM
        sb.append("VRAM: ").append(String.format("%.2f GB", bytesToGb(getTotalVideoMemory()))).append("\n");

        // CPU
        sb.append("CPU: ").append(getCpuDetails()).append("\n");
        CentralProcessor processor = hardware.getProcessor();
        sb.append("Cores: ").append(processor.getPhysicalProcessorCount()).append(" Physical, ")
                .append(processor.getLogicalProcessorCount()).append(" Logical\n");

        // OS
        sb.append("OS: ").append(getOsDetails()).append("\n");

        if (isAppleSilicon()) {
            sb.append("Architecture: Apple Silicon (Unified Memory Detected)\n");
        }

        return sb.toString();
    }

    private static double bytesToGb(long bytes) {
        return bytes / (1024.0 * 1024.0 * 1024.0);
    }
}
