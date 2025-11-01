package com.poorcraft.ultra.app;

import com.fasterxml.jackson.annotation.JsonProperty;

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
    @JsonProperty("worlds") WorldConfig worlds
) {

    public ClientConfig {
        if (worlds == null) {
            worlds = WorldConfig.defaults();
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
            WorldConfig.defaults()
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
}
