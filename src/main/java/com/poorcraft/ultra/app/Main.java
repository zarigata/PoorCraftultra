package com.poorcraft.ultra.app;

import com.poorcraft.ultra.engine.PoorcraftEngine;
import com.poorcraft.ultra.voxel.ChunkManager;
import com.poorcraft.ultra.world.WorldSaveManager;
import com.poorcraft.ultra.tools.AssetValidator;
import com.poorcraft.ultra.tools.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Application entry point.
 * Initializes logging, loads configuration, and starts the game engine.
 */
public class Main {
    // Static initializer runs before any logger access
    // This MUST complete before any SLF4J logger is created
    private static final String configuredLogLevel;
    static {
        configuredLogLevel = LoggingConfig.setup();
    }
    
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    public static void main(String[] args) {
        try {
            // Log the configured level (now that logger is initialized)
            logger.info("Logging initialized at level: {}", configuredLogLevel);
            
            // Log startup banner
            logger.info("=================================================");
            logger.info("  Poorcraft Ultra v0.1 - Initializing...");
            logger.info("=================================================");
            
            // Detect and log system information
            SystemInfo systemInfo = SystemInfo.detect();
            logger.info("System Info: {}", systemInfo);
            
            // Load configuration
            ClientConfig config = ConfigLoader.load("config/client.yaml");
            logger.info("Configuration loaded: {}x{}, vsync={}, fps={}", 
                config.displayWidth(), config.displayHeight(), 
                config.vsync(), config.fpsLimit());
            
            // Initialize service hub (DI container)
            ServiceHub serviceHub = new ServiceHub(config);
            logger.info("ServiceHub initialized");

            // Phase 1.35: Register shutdown hook for saving chunks on abnormal exit
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutdown hook triggered - evaluating world save state...");
                try {
                    WorldSaveManager worldSaveManager = serviceHub.has(WorldSaveManager.class)
                            ? serviceHub.get(WorldSaveManager.class)
                            : null;

                    if (worldSaveManager != null && worldSaveManager.isSavedOnShutdown()) {
                        logger.info("World already saved by primary shutdown sequence; skipping hook save");
                        return;
                    }

                    if (serviceHub.has(ChunkManager.class)) {
                        ChunkManager chunkManager = serviceHub.get(ChunkManager.class);
                        chunkManager.saveAll();
                        if (worldSaveManager != null) {
                            worldSaveManager.markSavedOnShutdown();
                        }
                        logger.info("World saved successfully via shutdown hook");
                    } else {
                        logger.debug("ChunkManager not available during shutdown hook");
                    }
                } catch (Exception e) {
                    logger.error("Failed to save world in shutdown hook", e);
                }
            }, "Poorcraft-Shutdown-Hook"));
            logger.info("Shutdown hook registered");

            // Phase 0A: Validate assets before starting engine
            logger.info("Validating assets...");
            AssetValidator assetValidator = new AssetValidator();
            Path assetsRoot = Paths.get("assets").toAbsolutePath();
            ValidationResult validationResult = assetValidator.validate(assetsRoot);
            
            if (!validationResult.valid()) {
                logger.error("Asset validation failed:");
                validationResult.errors().forEach(err -> logger.error("  - {}", err));
                logger.error("Run './scripts/dev/gen-assets.sh' (Unix) or 'scripts\\dev\\gen-assets.bat' (Windows) to generate assets.");
                System.exit(1);
            }
            
            if (!validationResult.warnings().isEmpty()) {
                logger.warn("Asset validation warnings:");
                validationResult.warnings().forEach(warn -> logger.warn("  - {}", warn));
            }
            
            logger.info("Assets validated: {} blocks, {} skins, {} items",
                countAssetsByCategory(assetsRoot, "blocks"),
                countAssetsByCategory(assetsRoot, "skins"),
                countAssetsByCategory(assetsRoot, "items"));
            
            // Register validation result in ServiceHub for UI access
            serviceHub.register(ValidationResult.class, validationResult);
            
            // Create and start engine via service hub
            logger.info("Starting PoorcraftEngine...");
            PoorcraftEngine engine = new PoorcraftEngine(serviceHub);
            engine.start();
            
        } catch (Exception e) {
            logger.error("Fatal error during startup", e);
            System.exit(1);
        }
    }
    
    /**
     * Count assets by category from manifest.
     * Gracefully returns 0 if manifest missing or invalid.
     */
    private static int countAssetsByCategory(Path assetsRoot, String category) {
        try {
            Path manifestPath = assetsRoot.resolve("manifest.json");
            if (!Files.exists(manifestPath)) {
                return 0;
            }
            
            String manifestJson = Files.readString(manifestPath);
            com.poorcraft.ultra.tools.AssetManifest manifest = 
                com.poorcraft.ultra.tools.AssetManifest.fromJson(manifestJson);
            
            return (int) manifest.assets().stream()
                .filter(entry -> entry.category().equals(category))
                .count();
        } catch (Exception e) {
            logger.debug("Failed to count assets for category {}: {}", category, e.getMessage());
            return 0;
        }
    }
}
