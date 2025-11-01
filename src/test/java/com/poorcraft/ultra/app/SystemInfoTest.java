package com.poorcraft.ultra.app;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SystemInfo.
 */
class SystemInfoTest {
    
    @Test
    void testDetect() {
        SystemInfo info = SystemInfo.detect();
        
        // Verify non-null fields
        assertNotNull(info.osName(), "OS name should not be null");
        assertNotNull(info.osVersion(), "OS version should not be null");
        assertNotNull(info.osArch(), "OS architecture should not be null");
        assertNotNull(info.javaVersion(), "Java version should not be null");
        assertNotNull(info.javaVendor(), "Java vendor should not be null");
        
        // Verify positive values
        assertTrue(info.availableProcessors() > 0, "Available processors should be > 0");
        assertTrue(info.maxMemoryMB() > 0, "Max memory should be > 0 MB");
    }
    
    @Test
    void testToString() {
        SystemInfo info = SystemInfo.detect();
        String str = info.toString();
        
        // Verify toString contains expected keywords
        assertTrue(str.contains("OS:"), "toString should contain 'OS:'");
        assertTrue(str.contains("Java:"), "toString should contain 'Java:'");
        assertTrue(str.contains("CPUs:"), "toString should contain 'CPUs:'");
        assertTrue(str.contains("Max Heap:"), "toString should contain 'Max Heap:'");
        assertTrue(str.contains("MB"), "toString should contain 'MB'");
    }
}
