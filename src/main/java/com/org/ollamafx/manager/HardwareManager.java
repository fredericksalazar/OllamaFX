package com.org.ollamafx.manager;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

public class HardwareManager {

    private static final SystemInfo systemInfo = new SystemInfo();
    private static final HardwareAbstractionLayer hardware = systemInfo.getHardware();

    public static String getHardwareDetails() {
        StringBuilder sb = new StringBuilder();
        sb.append("Hardware Information:\n");
        sb.append("---------------------\n");

        // RAM
        GlobalMemory memory = hardware.getMemory();
        sb.append(String.format("RAM: %.2f GB Total / %.2f GB Available\n",
                bytesToGb(memory.getTotal()),
                bytesToGb(memory.getAvailable())));

        // CPU
        CentralProcessor processor = hardware.getProcessor();
        sb.append("CPU: ").append(processor.getProcessorIdentifier().getName()).append("\n");
        sb.append("Cores: ").append(processor.getPhysicalProcessorCount()).append(" Physical, ")
                .append(processor.getLogicalProcessorCount()).append(" Logical\n");

        // OS
        sb.append("OS: ").append(System.getProperty("os.name")).append(" ")
                .append(System.getProperty("os.version")).append("\n");

        return sb.toString();
    }

    private static double bytesToGb(long bytes) {
        return bytes / (1024.0 * 1024.0 * 1024.0);
    }
}
