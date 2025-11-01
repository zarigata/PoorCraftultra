package com.poorcraft.ultra.engine;

import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.system.AppSettings;
import com.poorcraft.ultra.app.ClientConfig;
import com.poorcraft.ultra.app.ServiceHub;
import com.poorcraft.ultra.player.BlockHighlighter;
import com.poorcraft.ultra.player.BlockPicker;
import com.poorcraft.ultra.player.PlayerController;
import com.poorcraft.ultra.player.PlayerInventory;
import com.poorcraft.ultra.voxel.BlockRegistry;
import com.poorcraft.ultra.voxel.ChunkDoctorService;
import com.poorcraft.ultra.voxel.ChunkManager;
import com.poorcraft.ultra.world.WorldSaveManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Main jMonkeyEngine application class.
 * Extends SimpleApplication to provide game loop and rendering.
 */
public class PoorcraftEngine extends SimpleApplication {
    private static final Logger logger = LoggerFactory.getLogger(PoorcraftEngine.class);
    private final ServiceHub serviceHub;
    private final ClientConfig config;

    private ChunkManager chunkManager;
    private ChunkDoctorService chunkDoctorService;
    private BlockPicker blockPicker;
    private BlockHighlighter blockHighlighter;
    private PlayerController playerController;
    private PlayerInventory playerInventory;
    
    public PoorcraftEngine(ServiceHub serviceHub) {
        super(new StatsAppState()); // Enable FPS counter
        this.serviceHub = serviceHub;
        this.config = serviceHub.getConfig();
    }
    
    public ServiceHub getServiceHub() {
        return serviceHub;
    }
    
    @Override
    public void start() {
        // Configure display settings from ClientConfig
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Poorcraft Ultra");
        settings.setWidth(config.displayWidth());
        settings.setHeight(config.displayHeight());
        settings.setFullscreen(config.fullscreen());
        settings.setVSync(config.vsync());
        settings.setFrameRate(config.fpsLimit());
        settings.setResizable(true);
        
        setSettings(settings);
        setShowSettings(false); // Skip settings dialog
        
        super.start();
    }
    
    @Override
    public void simpleInitApp() {
        logger.info("Initializing PoorcraftEngine...");
        
        // Phase 0A: Register external assets directory
        Path assetsRoot = Paths.get("assets").toAbsolutePath();
        if (Files.exists(assetsRoot)) {
            assetManager.registerLocator(assetsRoot.toString(), FileLocator.class);
            logger.info("Registered assets directory: {}", assetsRoot);
        } else {
            logger.warn("Assets directory not found: {}", assetsRoot);
        }
        
        // Set solid background color
        viewPort.setBackgroundColor(ColorRGBA.DarkGray);
        
        // Attach debug overlay AppState with ServiceHub access
        DebugOverlayAppState debugOverlay = new DebugOverlayAppState(serviceHub);
        stateManager.attach(debugOverlay);

        // CP 1.05: Initialize block registry and chunk manager
        BlockRegistry blockRegistry = new BlockRegistry();
        blockRegistry.init(assetManager);
        serviceHub.register(BlockRegistry.class, blockRegistry);

        WorldSaveManager worldSaveManager = new WorldSaveManager();
        Path baseWorldPath = Paths.get(config.worlds().baseDir());
        worldSaveManager.init("default", baseWorldPath);
        serviceHub.register(WorldSaveManager.class, worldSaveManager);
        logger.info("WorldSaveManager initialized for world: default at {}", baseWorldPath.toAbsolutePath());

        chunkManager = new ChunkManager();
        chunkManager.init(rootNode, assetManager, blockRegistry, worldSaveManager);
        serviceHub.register(ChunkManager.class, chunkManager);
        logger.info("ChunkManager initialized - CP 1.05 OK");

        // CP 1.05: Load initial chunk
        chunkManager.loadChunk(0, 0);

        // CP 1.1: Load additional chunks based on configuration and setup ChunkDoctor
        if (config.loadMultiChunk()) {
            chunkManager.loadChunks3x3(0, 0);
            logger.info("3x3 chunk grid loaded - CP 1.1 OK");
        } else {
            logger.info("Single chunk mode enabled - CP 1.05 verification");
        }

        chunkDoctorService = new ChunkDoctorService();
        chunkDoctorService.init(chunkManager, rootNode, assetManager);
        serviceHub.register(ChunkDoctorService.class, chunkDoctorService);

        // CP 1.3: Initialize player inventory
        playerInventory = new PlayerInventory();
        playerInventory.init();
        serviceHub.register(PlayerInventory.class, playerInventory);

        // CP 1.2: Initialize block picker, highlighter, and player controller
        blockPicker = new BlockPicker();
        blockPicker.init(chunkManager);
        serviceHub.register(BlockPicker.class, blockPicker);

        blockHighlighter = new BlockHighlighter();
        blockHighlighter.init(rootNode, assetManager);
        serviceHub.register(BlockHighlighter.class, blockHighlighter);

        playerController = new PlayerController();
        playerController.init(this, chunkManager, blockPicker, blockHighlighter, playerInventory);
        serviceHub.register(PlayerController.class, playerController);

        // Position camera at terrain center
        cam.setLocation(new Vector3f(8f, 70f, 8f));
        cam.lookAt(new Vector3f(8f, 64f, 8f), Vector3f.UNIT_Y);
        logger.info("PlayerController initialized - CP 1.2 OK");
        logger.info("PlayerInventory initialized - CP 1.3 OK");
        
        // Register ESC key to exit
        inputManager.addMapping("Exit", new KeyTrigger(KeyInput.KEY_ESCAPE));
        inputManager.addListener(exitListener, "Exit");
        
        logger.info("PoorcraftEngine initialized - CP 0.1 OK");
    }
    
    private final ActionListener exitListener = (name, isPressed, tpf) -> {
        if (name.equals("Exit") && !isPressed) {
            logger.info("ESC pressed - shutting down");
            stop();
        }
    };
    
    @Override
    public void simpleUpdate(float tpf) {
        if (chunkManager != null) {
            chunkManager.update(tpf);
        }

        if (playerController != null) {
            playerController.update(tpf);
        }
    }
    
    @Override
    public void destroy() {
        logger.info("PoorcraftEngine shutting down...");

        if (chunkManager != null) {
            try {
                chunkManager.saveAll();
                if (serviceHub.has(WorldSaveManager.class)) {
                    serviceHub.get(WorldSaveManager.class).markSavedOnShutdown();
                }
            } catch (Exception e) {
                logger.error("Failed to save chunks on shutdown", e);
            }
        }

        super.destroy();
        logger.info("PoorcraftEngine shutdown complete");
    }
}
