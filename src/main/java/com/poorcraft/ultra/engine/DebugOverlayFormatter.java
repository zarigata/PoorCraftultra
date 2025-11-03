package com.poorcraft.ultra.engine;

import com.poorcraft.ultra.app.SystemInfo;
import com.poorcraft.ultra.tools.ValidationResult;

/**
 * Formats debug overlay text for the F3 HUD.
 * Separated from DebugOverlayAppState for testability.
 */
public class DebugOverlayFormatter {
    
    /**
     * Formats the debug overlay text with current system metrics.
     * 
     * @param fps Current frames per second
     * @param systemInfo System information (Java, OS, CPUs)
     * @param usedMemoryMB Used heap memory in megabytes
     * @param maxMemoryMB Maximum heap memory in megabytes
     * @param assetValidation Asset validation result (nullable, Phase 0A)
     * @return Formatted overlay text
     */
    public String format(int fps, SystemInfo systemInfo, long usedMemoryMB, long maxMemoryMB,
                        ValidationResult assetValidation, String chunkStats, long worldSeed) {
        // Build asset status line
        String assetStatus = "";
        if (assetValidation != null) {
            if (assetValidation.valid()) {
                assetStatus = "\nAssets: OK";
            } else {
                assetStatus = String.format("\nAssets: MISSING (%d errors)", 
                                          assetValidation.errors().size());
            }
        }
        
        String chunkLine = (chunkStats != null && !chunkStats.isEmpty()) ? chunkStats : "";
        String seedLine = worldSeed != 0L ? String.format("\nWorld Seed: %d", worldSeed) : "";

        return String.format(
            "Poorcraft Ultra v0.1 - Debug Overlay\n" +
            "FPS: %d | Java: %s | OS: %s %s\n" +
            "Heap: %d/%d MB | CPUs: %d%s%s%s\n" +
            "\n" +
            "F3: Toggle overlay | F9: Reload assets | F10: Rebuild meshes | F11: Chunk bounds\n" +
            "ESC: Exit",
            fps,
            systemInfo.javaVersion(),
            systemInfo.osName(),
            systemInfo.osVersion(),
            usedMemoryMB,
            maxMemoryMB,
            systemInfo.availableProcessors(),
            assetStatus,
            chunkLine,
            seedLine
        );
    }
}
