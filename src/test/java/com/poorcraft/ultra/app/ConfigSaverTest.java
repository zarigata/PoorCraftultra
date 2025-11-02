package com.poorcraft.ultra.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConfigSaver.
 */
class ConfigSaverTest {

    @Test
    void testSaveConfig(@TempDir Path tempDir) {
        ClientConfig config = ClientConfig.defaults();
        Path configPath = tempDir.resolve("test-config.yaml");

        boolean success = ConfigSaver.save(config, configPath.toString());

        assertTrue(success, "Config save should succeed");
        assertTrue(configPath.toFile().exists(), "Config file should exist");
    }

    @Test
    void testSaveCreatesDirectories(@TempDir Path tempDir) {
        ClientConfig config = ClientConfig.defaults();
        Path nestedPath = tempDir.resolve("nested/dir/config.yaml");

        boolean success = ConfigSaver.save(config, nestedPath.toString());

        assertTrue(success, "Config save should succeed with nested dirs");
        assertTrue(nestedPath.toFile().exists(), "Config file should exist in nested path");
    }

    @Test
    void testSaveInvalidPath() {
        ClientConfig config = ClientConfig.defaults();
        // Use a path with invalid characters that will fail on any OS
        String invalidPath = "C:\\invalid<>path\\config.yaml";

        boolean success = ConfigSaver.save(config, invalidPath);

        assertFalse(success, "Config save should fail for invalid path");
    }

    @Test
    void testRoundTrip(@TempDir Path tempDir) {
        ClientConfig original = new ClientConfig(
            1920, 1080, true, false, 144, "DEBUG", false,
            ClientConfig.WorldConfig.defaults(),
            new ClientConfig.ControlsConfig(2.0f, true, ClientConfig.ControlsConfig.defaults().keybinds()),
            ClientConfig.GraphicsConfig.defaults(),
            ClientConfig.AudioConfig.defaults()
        );
        Path configPath = tempDir.resolve("roundtrip.yaml");

        ConfigSaver.save(original, configPath.toString());
        ClientConfig loaded = ConfigLoader.load(configPath.toString());

        // Debug: check if loaded is null and print values
        assertNotNull(loaded, "Loaded config should not be null");
        System.out.println("Original displayWidth: " + original.displayWidth());
        System.out.println("Loaded displayWidth: " + loaded.displayWidth());
        System.out.println("File exists: " + configPath.toFile().exists());

        assertEquals(original.displayWidth(), loaded.displayWidth());
        assertEquals(original.displayHeight(), loaded.displayHeight());
        assertEquals(original.controls().mouseSensitivity(), loaded.controls().mouseSensitivity());
        assertEquals(original.controls().invertMouseY(), loaded.controls().invertMouseY());
    }
}
