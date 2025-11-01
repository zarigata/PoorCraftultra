package com.poorcraft.ultra.app;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Client configuration data class.
 * Loaded from config/client.yaml with fallback to defaults.
 */
public record ClientConfig(
    @JsonProperty("displayWidth") int displayWidth,
    @JsonProperty("displayHeight") int displayHeight,
    @JsonProperty("fullscreen") boolean fullscreen,
    @JsonProperty("vsync") boolean vsync,
    @JsonProperty("fpsLimit") int fpsLimit,
    @JsonProperty("logLevel") String logLevel,
    @JsonProperty("loadMultiChunk") boolean loadMultiChunk,
    @JsonProperty("worlds") WorldConfig worlds,
    @JsonProperty("controls") ControlsConfig controls,
    @JsonProperty("graphics") GraphicsConfig graphics
) {

    public ClientConfig {
        if (worlds == null) {
            worlds = WorldConfig.defaults();
        }
        if (controls == null) {
            controls = ControlsConfig.defaults();
        }
        if (graphics == null) {
            graphics = GraphicsConfig.defaults();
        }
    }
    /**
     * Returns default configuration values.
     */
    public static ClientConfig defaults() {
        return new ClientConfig(
            1280,    // displayWidth
            720,     // displayHeight
            false,   // fullscreen
            true,    // vsync
            60,      // fpsLimit
            "INFO",  // logLevel
            true,     // loadMultiChunk
            WorldConfig.defaults(),
            ControlsConfig.defaults(),
            GraphicsConfig.defaults()
        );
    }

    public record WorldConfig(@JsonProperty("baseDir") String baseDir) {
        private static final String DEFAULT_BASE_DIR = "data/worlds";

        public WorldConfig {
            if (baseDir == null || baseDir.isBlank()) {
                baseDir = DEFAULT_BASE_DIR;
            }
        }

        public static WorldConfig defaults() {
            return new WorldConfig(DEFAULT_BASE_DIR);
        }
    }

    public record ControlsConfig(
        @JsonProperty("mouseSensitivity") float mouseSensitivity,
        @JsonProperty("invertMouseY") boolean invertMouseY,
        @JsonProperty("keybinds") Map<String, String> keybinds
    ) {

        public ControlsConfig {
            if (keybinds == null) {
                keybinds = defaultKeybinds();
            } else {
                keybinds = new HashMap<>(keybinds);
            }
        }

        public static ControlsConfig defaults() {
            return new ControlsConfig(1.5f, false, defaultKeybinds());
        }

        private static Map<String, String> defaultKeybinds() {
            Map<String, String> binds = new HashMap<>();
            binds.put("moveForward", "W");
            binds.put("moveBackward", "S");
            binds.put("moveLeft", "A");
            binds.put("moveRight", "D");
            binds.put("sprint", "LSHIFT");
            binds.put("breakBlock", "MOUSE_LEFT");
            binds.put("placeBlock", "MOUSE_RIGHT");
            binds.put("pause", "ESCAPE");
            return binds;
        }
    }

    public record GraphicsConfig(
        @JsonProperty("windowMode") String windowMode,
        @JsonProperty("resolution") String resolution,
        @JsonProperty("vsync") boolean vsync,
        @JsonProperty("fpsLimit") int fpsLimit
    ) {

        public GraphicsConfig {
            if (windowMode == null || windowMode.isBlank()) {
                windowMode = "WINDOWED";
            }
            if (resolution == null || resolution.isBlank()) {
                resolution = "1280x720";
            }
        }

        public static GraphicsConfig defaults() {
            return new GraphicsConfig("WINDOWED", "1280x720", true, 60);
        }

        public int getWidth() {
            String[] parts = resolution.split("x");
            return parts.length >= 1 ? Integer.parseInt(parts[0].trim()) : 1280;
        }

        public int getHeight() {
            String[] parts = resolution.split("x");
            return parts.length >= 2 ? Integer.parseInt(parts[1].trim()) : 720;
        }
    }
}
