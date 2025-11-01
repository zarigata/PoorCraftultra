package com.poorcraft.ultra.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Unit tests for ConfigLoader.
 */
class ConfigLoaderTest {
    
    @Test
    void testLoadDefaults() {
        // Load non-existent file should return defaults
        ClientConfig config = ConfigLoader.load("nonexistent.yaml");
        
        ClientConfig defaults = ClientConfig.defaults();
        assertEquals(defaults.displayWidth(), config.displayWidth());
        assertEquals(defaults.displayHeight(), config.displayHeight());
        assertEquals(defaults.fullscreen(), config.fullscreen());
        assertEquals(defaults.vsync(), config.vsync());
        assertEquals(defaults.fpsLimit(), config.fpsLimit());
        assertEquals(defaults.logLevel(), config.logLevel());
        assertEquals(defaults.worlds().baseDir(), config.worlds().baseDir());
    }
    
    @Test
    void testLoadValidYaml(@TempDir Path tempDir) throws IOException {
        // Create temp YAML file with custom values
        Path configFile = tempDir.resolve("test-config.yaml");
        String yaml = """
            displayWidth: 1920
            displayHeight: 1080
            fullscreen: true
            vsync: false
            fpsLimit: 144
            logLevel: DEBUG
            worlds:
              baseDir: custom/worlds
            """;
        Files.writeString(configFile, yaml);
        
        // Load config
        ClientConfig config = ConfigLoader.load(configFile.toString());
        
        // Verify custom values
        assertEquals(1920, config.displayWidth());
        assertEquals(1080, config.displayHeight());
        assertTrue(config.fullscreen());
        assertFalse(config.vsync());
        assertEquals(144, config.fpsLimit());
        assertEquals("DEBUG", config.logLevel());
        assertEquals("custom/worlds", config.worlds().baseDir());
    }
    
    @Test
    void testLoadInvalidYaml(@TempDir Path tempDir) throws IOException {
        // Create temp file with malformed YAML
        Path configFile = tempDir.resolve("invalid.yaml");
        String invalidYaml = "{ this is not valid yaml [[[";
        Files.writeString(configFile, invalidYaml);
        
        // Should return defaults and log error (not throw exception)
        ClientConfig config = ConfigLoader.load(configFile.toString());
        
        ClientConfig defaults = ClientConfig.defaults();
        assertEquals(defaults.displayWidth(), config.displayWidth());
        assertEquals(defaults.worlds().baseDir(), config.worlds().baseDir());
    }
}
