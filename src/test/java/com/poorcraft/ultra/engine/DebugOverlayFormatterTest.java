package com.poorcraft.ultra.engine;

import com.poorcraft.ultra.app.SystemInfo;
import com.poorcraft.ultra.tools.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DebugOverlayFormatter.
 * Tests text formatting without requiring a full jME application context.
 */
class DebugOverlayFormatterTest {
    
    private DebugOverlayFormatter formatter;
    private SystemInfo testSystemInfo;
    
    @BeforeEach
    void setUp() {
        formatter = new DebugOverlayFormatter();
        testSystemInfo = SystemInfo.detect();
    }
    
    @Test
    void testFormatContainsAllRequiredFields() {
        // Arrange
        int fps = 60;
        long usedMemoryMB = 256;
        long maxMemoryMB = 1024;
        
        // Act
        String result = formatter.format(fps, testSystemInfo, usedMemoryMB, maxMemoryMB, null, "");
        
        // Assert - verify all expected fields are present
        assertNotNull(result, "Formatted text should not be null");
        assertFalse(result.isEmpty(), "Formatted text should not be empty");
        
        assertTrue(result.contains("Poorcraft Ultra"), "Should contain game title");
        assertTrue(result.contains("Debug Overlay"), "Should contain overlay title");
        assertTrue(result.contains("FPS: 60"), "Should contain FPS value");
        assertTrue(result.contains("Java:"), "Should contain Java version label");
        assertTrue(result.contains("OS:"), "Should contain OS label");
        assertTrue(result.contains("Heap: 256/1024 MB"), "Should contain heap usage");
        assertTrue(result.contains("CPUs:"), "Should contain CPU count label");
        assertTrue(result.contains("F3:"), "Should contain F3 hotkey hint");
        assertTrue(result.contains("F9:"), "Should contain F9 hotkey hint");
        assertTrue(result.contains("F10:"), "Should contain F10 hotkey hint");
        assertTrue(result.contains("F11:"), "Should contain F11 hotkey hint");
        assertTrue(result.contains("ESC:"), "Should contain ESC hotkey hint");
    }
    
    @Test
    void testFormatUpdatesWithDifferentFPS() {
        // Arrange
        long usedMemoryMB = 128;
        long maxMemoryMB = 512;
        
        // Act
        String result30fps = formatter.format(30, testSystemInfo, usedMemoryMB, maxMemoryMB, null, "");
        String result120fps = formatter.format(120, testSystemInfo, usedMemoryMB, maxMemoryMB, null, "");
        
        // Assert
        assertTrue(result30fps.contains("FPS: 30"), "Should show 30 FPS");
        assertTrue(result120fps.contains("FPS: 120"), "Should show 120 FPS");
        assertNotEquals(result30fps, result120fps, "Different FPS should produce different output");
    }
    
    @Test
    void testFormatUpdatesWithDifferentMemory() {
        // Arrange
        int fps = 60;
        
        // Act
        String resultLowMem = formatter.format(fps, testSystemInfo, 100, 512, null, "");
        String resultHighMem = formatter.format(fps, testSystemInfo, 400, 1024, null, "");
        
        // Assert
        assertTrue(resultLowMem.contains("Heap: 100/512 MB"), "Should show low memory values");
        assertTrue(resultHighMem.contains("Heap: 400/1024 MB"), "Should show high memory values");
        assertNotEquals(resultLowMem, resultHighMem, "Different memory should produce different output");
    }
    
    @Test
    void testFormatIncludesSystemInfo() {
        // Arrange
        int fps = 60;
        long usedMemoryMB = 256;
        long maxMemoryMB = 1024;
        
        // Act
        String result = formatter.format(fps, testSystemInfo, usedMemoryMB, maxMemoryMB, null, "");
        
        // Assert - verify system info is included
        assertTrue(result.contains(testSystemInfo.javaVersion()), 
            "Should contain actual Java version");
        assertTrue(result.contains(testSystemInfo.osName()), 
            "Should contain actual OS name");
        assertTrue(result.contains(testSystemInfo.osVersion()), 
            "Should contain actual OS version");
        assertTrue(result.contains(String.valueOf(testSystemInfo.availableProcessors())), 
            "Should contain actual CPU count");
    }
    
    @Test
    void testFormatHandlesZeroFPS() {
        // Arrange
        int fps = 0;
        long usedMemoryMB = 256;
        long maxMemoryMB = 1024;
        
        // Act
        String result = formatter.format(fps, testSystemInfo, usedMemoryMB, maxMemoryMB, null, "");
        
        // Assert
        assertTrue(result.contains("FPS: 0"), "Should handle zero FPS gracefully");
        assertFalse(result.isEmpty(), "Should still produce valid output");
    }
    
    @Test
    void testFormatHandlesLargeMemoryValues() {
        // Arrange
        int fps = 60;
        long usedMemoryMB = 8192; // 8 GB
        long maxMemoryMB = 16384; // 16 GB
        
        // Act
        String result = formatter.format(fps, testSystemInfo, usedMemoryMB, maxMemoryMB, null, "");
        
        // Assert
        assertTrue(result.contains("Heap: 8192/16384 MB"), 
            "Should handle large memory values correctly");
    }
    
    @Test
    void testFormatIsConsistent() {
        // Arrange
        int fps = 60;
        long usedMemoryMB = 256;
        long maxMemoryMB = 1024;
        
        // Act - call format multiple times with same inputs
        String result1 = formatter.format(fps, testSystemInfo, usedMemoryMB, maxMemoryMB, null, "");
        String result2 = formatter.format(fps, testSystemInfo, usedMemoryMB, maxMemoryMB, null, "");
        
        // Assert
        assertEquals(result1, result2, 
            "Formatter should produce consistent output for same inputs");
    }
    
    @Test
    void testFormatWithAssetValidation() {
        // Arrange
        int fps = 60;
        long usedMemoryMB = 256;
        long maxMemoryMB = 1024;
        ValidationResult validResult = ValidationResult.success();
        
        // Act
        String result = formatter.format(fps, testSystemInfo, usedMemoryMB, maxMemoryMB, validResult, "");
        
        // Assert
        assertTrue(result.contains("Assets: OK"), "Should show asset validation success");
    }
    
    @Test
    void testFormatWithAssetValidationFailure() {
        // Arrange
        int fps = 60;
        long usedMemoryMB = 256;
        long maxMemoryMB = 1024;
        ValidationResult invalidResult = ValidationResult.failure(List.of("Missing blocks/wood.png"));
        
        // Act
        String result = formatter.format(fps, testSystemInfo, usedMemoryMB, maxMemoryMB, invalidResult, "");
        
        // Assert
        assertTrue(result.contains("Assets: MISSING"), "Should show asset validation failure");
        assertTrue(result.contains("1 errors"), "Should show error count");
    }
    
    @Test
    void testFormatWithoutAssetValidation() {
        // Arrange
        int fps = 60;
        long usedMemoryMB = 256;
        long maxMemoryMB = 1024;
        
        // Act
        String result = formatter.format(fps, testSystemInfo, usedMemoryMB, maxMemoryMB, null, "");
        
        // Assert
        assertFalse(result.contains("Assets:"), "Should not show asset status when validation is null");
    }

    @Test
    void testFormatWithChunkStats() {
        int fps = 60;
        long usedMemoryMB = 256;
        long maxMemoryMB = 1024;
        String chunkStats = "\nChunks: 9 loaded | Vertices: 1024 (avg 114/chunk) | Triangles: 512";

        String result = formatter.format(fps, testSystemInfo, usedMemoryMB, maxMemoryMB, null, chunkStats);

        assertTrue(result.contains("Chunks: 9 loaded"), "Should include chunk count");
        assertTrue(result.contains("Vertices: 1024"), "Should include vertex count");
        assertTrue(result.contains("Triangles: 512"), "Should include triangle count");
    }
}
