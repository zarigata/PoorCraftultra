package com.poorcraft.ultra.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Persists {@link ClientConfig} instances to disk using Jackson YAML.
 * Mirrors {@link ConfigLoader} behaviour so runtime changes can be saved.
 */
public final class ConfigSaver {
    private static final Logger logger = LoggerFactory.getLogger(ConfigSaver.class);
    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    private ConfigSaver() {
        // Utility class
    }

    /**
     * Saves the provided configuration to the given path.
     *
     * @param config configuration instance to persist
     * @param path   target filesystem path (e.g., "config/client.yaml")
     * @return {@code true} if save succeeded, {@code false} otherwise
     */
    public static boolean save(ClientConfig config, String path) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(path, "path");

        File target = new File(path);
        try {
            File parent = target.getParentFile();
            if (parent != null && !parent.exists()) {
                Files.createDirectories(parent.toPath());
            }

            mapper.writeValue(target, config);
            logger.info("Saved client config to {}", target.getAbsolutePath());
            return true;
        } catch (IOException | java.nio.file.InvalidPathException e) {
            logger.error("Failed to save client config to {}", path, e);
            return false;
        }
    }

    /**
     * Saves the provided configuration asynchronously.
     *
     * @param config   configuration instance to persist
     * @param path     target filesystem path
     * @param callback optional callback invoked with the save result
     */
    public static void saveAsync(ClientConfig config, String path, Consumer<Boolean> callback) {
        CompletableFuture
            .supplyAsync(() -> save(config, path))
            .whenComplete((success, throwable) -> {
                boolean result = success != null && success;
                if (throwable != null) {
                    logger.error("Async config save failed for {}", path, throwable);
                    result = false;
                }
                if (callback != null) {
                    callback.accept(result);
                }
            });
    }
}
