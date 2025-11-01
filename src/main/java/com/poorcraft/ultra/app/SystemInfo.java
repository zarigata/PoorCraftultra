package com.poorcraft.ultra.app;

/**
 * System information detection utility.
 * Captures OS, Java version, CPU count, and memory information.
 */
public record SystemInfo(
    String osName,
    String osVersion,
    String osArch,
    String javaVersion,
    String javaVendor,
    int availableProcessors,
    long maxMemoryMB
) {
    /**
     * Detects current system information.
     */
    public static SystemInfo detect() {
        Runtime runtime = Runtime.getRuntime();
        
        return new SystemInfo(
            System.getProperty("os.name"),
            System.getProperty("os.version"),
            System.getProperty("os.arch"),
            System.getProperty("java.version"),
            System.getProperty("java.vendor"),
            runtime.availableProcessors(),
            runtime.maxMemory() / (1024 * 1024)
        );
    }
    
    @Override
    public String toString() {
        return String.format(
            "OS: %s %s (%s), Java: %s (%s), CPUs: %d, Max Heap: %d MB",
            osName, osVersion, osArch, javaVersion, javaVendor, 
            availableProcessors, maxMemoryMB
        );
    }
}
