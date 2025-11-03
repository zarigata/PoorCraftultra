package com.poorcraft.ultra.engine;

import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.math.ColorRGBA;
import com.jme3.system.AppSettings;
import com.poorcraft.ultra.app.ClientConfig;
import com.poorcraft.ultra.app.ServiceHub;
import java.util.Locale;
import com.poorcraft.ultra.player.BlockHighlighter;
import com.poorcraft.ultra.player.BlockPicker;
import com.poorcraft.ultra.player.PlayerController;
import com.poorcraft.ultra.player.PlayerInventory;
import com.poorcraft.ultra.ui.GameStateManager;
import com.poorcraft.ultra.ui.InputConfig;
import com.poorcraft.ultra.ui.UIScaleProcessor;
import com.poorcraft.ultra.voxel.BlockRegistry;
import com.poorcraft.ultra.voxel.ChunkDoctorService;
import com.poorcraft.ultra.voxel.ChunkManager;
import com.poorcraft.ultra.world.WorldGenerator;
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

    private GameStateManager gameStateManager;
    private InputConfig inputConfig;
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
        ClientConfig.GraphicsConfig graphicsConfig = config.graphics();

        int width = config.displayWidth();
        int height = config.displayHeight();
        boolean fullscreen = config.fullscreen();
        boolean borderless = false;
        boolean vsync = config.vsync();
        int fpsLimit = config.fpsLimit();

        if (graphicsConfig != null) {
            width = graphicsConfig.getWidth();
            height = graphicsConfig.getHeight();
            vsync = graphicsConfig.vsync();
            fpsLimit = graphicsConfig.fpsLimit();

            String windowMode = graphicsConfig.windowMode();
            if (windowMode != null) {
                switch (windowMode.toUpperCase(Locale.ROOT)) {
                    case "FULLSCREEN" -> {
                        fullscreen = true;
                        borderless = false;
                    }
                    case "BORDERLESS" -> {
                        fullscreen = true;
                        borderless = true;
                    }
                    default -> {
                        fullscreen = false;
                        borderless = false;
                    }
                }
            }
        }

        settings.setWidth(width);
        settings.setHeight(height);
        settings.setFullscreen(fullscreen);
        if (borderless) {
            settings.setUseJoysticks(settings.useJoysticks());
            settings.put("GraphicsWindowMode", "BORDERLESS");
        }
        settings.setVSync(vsync);
        settings.setFrameRate(fpsLimit);
        settings.setResizable(true);

        setSettings(settings);
        setShowSettings(false); // Skip settings dialog

        logger.info("Starting jME application with settings: {}x{} fullscreen={}", width, height, fullscreen);

        super.start();
    }
    
    @Override
    public void simpleInitApp() {
        logger.info("Initializing PoorcraftEngine...");

        if (inputManager.hasMapping(SimpleApplication.INPUT_MAPPING_EXIT)) {
            inputManager.deleteMapping(SimpleApplication.INPUT_MAPPING_EXIT);
            logger.info("Default escape-to-exit mapping removed");
        }

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
        logger.info("DebugOverlayAppState attached");

        // Phase 1.5: Initialize InputConfig service
        inputConfig = new InputConfig();
        inputConfig.init(inputManager, config.controls());
        inputConfig.setApplication(this);
        serviceHub.register(InputConfig.class, inputConfig);
        logger.info("InputConfig initialized");

        // CP 1.05: Initialize block registry and chunk manager
        BlockRegistry blockRegistry = new BlockRegistry();
        blockRegistry.init(assetManager);
        serviceHub.register(BlockRegistry.class, blockRegistry);

        WorldSaveManager worldSaveManager = new WorldSaveManager();
        Path baseWorldPath = Paths.get(config.worlds().baseDir());
        worldSaveManager.init("default", baseWorldPath);
        serviceHub.register(WorldSaveManager.class, worldSaveManager);
        logger.info("WorldSaveManager initialized for world: default at {}", baseWorldPath.toAbsolutePath());

        long persistedSeed = worldSaveManager.resolveOrPersistSeed(config.worlds().seed());
        WorldGenerator worldGenerator = new WorldGenerator();
        worldGenerator.init(persistedSeed);
        serviceHub.register(WorldGenerator.class, worldGenerator);
        logger.info("WorldGenerator initialized with seed: {}", persistedSeed);

        chunkManager = new ChunkManager();
        chunkManager.init(rootNode, assetManager, blockRegistry, worldSaveManager, worldGenerator);
        serviceHub.register(ChunkManager.class, chunkManager);
        logger.info("ChunkManager initialized - CP 1.05 OK");

        chunkDoctorService = new ChunkDoctorService();
        chunkDoctorService.init(chunkManager, rootNode, assetManager);
        serviceHub.register(ChunkDoctorService.class, chunkDoctorService);
        logger.info("ChunkDoctorService initialized");

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
        playerController.init(this, chunkManager, blockPicker, blockHighlighter, playerInventory, inputConfig);
        serviceHub.register(PlayerController.class, playerController);
        logger.info("PlayerController initialized - CP 1.2 OK");
        logger.info("PlayerInventory initialized - CP 1.3 OK");

        // Phase 1.5: Initialize GameStateManager and UI scale processor
        gameStateManager = new GameStateManager();
        gameStateManager.init(this, serviceHub);
        serviceHub.register(GameStateManager.class, gameStateManager);
        logger.info("GameStateManager initialized");

        UIScaleProcessor uiScaleProcessor = new UIScaleProcessor(guiNode, this);
        guiViewPort.addProcessor(uiScaleProcessor);
        logger.info("UI scale processor attached");

        // Start in main menu (not in-game)
        gameStateManager.enterMainMenu();
        logger.info("Main menu initialization requested - current state: {}", gameStateManager.getCurrentState());
        
        logger.info("PoorcraftEngine initialized - CP 0.1 OK");
    }
    
    @Override
    public void simpleUpdate(float tpf) {
        // Updates handled by InGameState when enabled
    }
    
    @Override
    public void destroy() {
        logger.info("PoorcraftEngine shutting down...");

        if (gameStateManager != null) {
            gameStateManager.exitToMainMenu();
        }

        try {
            if (serviceHub.has(WorldSaveManager.class)) {
                WorldSaveManager saveManager = serviceHub.get(WorldSaveManager.class);
                if (!saveManager.isSavedOnShutdown() && chunkManager != null) {
                    saveManager.saveAll(chunkManager);
                    saveManager.markSavedOnShutdown();
                }
            }
        } catch (Exception e) {
            logger.error("Failed to save chunks on shutdown", e);
        }

        super.destroy();
        logger.info("PoorcraftEngine shutdown complete");
    }
}
