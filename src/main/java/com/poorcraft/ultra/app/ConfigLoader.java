package com.poorcraft.ultra.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Configuration loader using Jackson YAML.
 * Loads ClientConfig from YAML files with fallback to defaults.
 * 
 * Loading order:
 * 1. Filesystem (config/client.yaml) - user overrides
 * 2. Classpath (embedded default) - bundled in JAR
 * 3. Hardcoded defaults - if all else fails
 */
public class ConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    
    /**
     * Loads configuration from the specified path.
     * Tries filesystem first (for user overrides), then classpath (embedded default).
     * Returns hardcoded defaults if file not found or parsing fails.
     * 
     * @param path Path to YAML config file (e.g., "config/client.yaml")
     * @return Loaded ClientConfig or defaults
     */
    public static ClientConfig load(String path) {
        // Try loading from file system first (user override)
        File file = new File(path);
        if (file.exists()) {
            try {
                logger.info("Loading config from filesystem (override): {}", path);
                return mapper.readValue(file, ClientConfig.class);
            } catch (IOException e) {
                logger.error("Failed to parse config file: {}", path, e);
            }
        }
        
        // Try loading from classpath (embedded default)
        try (InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream(path)) {
            if (is != null) {
                logger.info("Loading config from classpath (embedded default): {}", path);
                return mapper.readValue(is, ClientConfig.class);
            }
        } catch (IOException e) {
            logger.warn("Failed to load config from classpath: {}", path, e);
        }
        
        // Fall back to hardcoded defaults
        logger.warn("Config file not found in filesystem or classpath: {}, using hardcoded defaults", path);
        
        return ClientConfig.defaults();
    }
}
