package com.org.ollamafx.manager;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

public class HardwareManager {

    private static final SystemInfo systemInfo = new SystemInfo();
    private static final HardwareAbstractionLayer hardware = systemInfo.getHardware();

    public static String getRamDetails() {
        GlobalMemory memory = hardware.getMemory();
        return String.format("%.2f GB Total / %.2f GB Available",
                bytesToGb(memory.getTotal()),
                bytesToGb(memory.getAvailable()));
    }

    public static String getCpuDetails() {
        CentralProcessor processor = hardware.getProcessor();
        return processor.getProcessorIdentifier().getName();
    }

    public static String getOsDetails() {
        return System.getProperty("os.name") + " " + System.getProperty("os.version");
    }

    public static String getHardwareDetails() {
        StringBuilder sb = new StringBuilder();
        sb.append("Hardware Information:\n");
        sb.append("---------------------\n");

        // RAM
        sb.append("RAM: ").append(getRamDetails()).append("\n");

        // CPU
        sb.append("CPU: ").append(getCpuDetails()).append("\n");
        CentralProcessor processor = hardware.getProcessor();
        sb.append("Cores: ").append(processor.getPhysicalProcessorCount()).append(" Physical, ")
                .append(processor.getLogicalProcessorCount()).append(" Logical\n");

        // OS
        sb.append("OS: ").append(getOsDetails()).append("\n");

        return sb.toString();
    }

    private static double bytesToGb(long bytes) {
        return bytes / (1024.0 * 1024.0 * 1024.0);
    }
}
