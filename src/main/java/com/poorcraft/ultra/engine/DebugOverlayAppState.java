package com.poorcraft.ultra.engine;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.poorcraft.ultra.app.ServiceHub;
import com.poorcraft.ultra.app.SystemInfo;
import com.poorcraft.ultra.tools.ValidationResult;
import com.poorcraft.ultra.voxel.ChunkDoctorService;
import com.poorcraft.ultra.voxel.ChunkDoctorService.ChunkDoctorStats;
import com.poorcraft.ultra.voxel.ChunkManager;
import com.poorcraft.ultra.world.WorldGenerator;
import com.poorcraft.ultra.world.WorldSaveManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Debug overlay AppState (F3-style HUD).
 * Displays FPS, Java version, OS, heap usage, and provides debug hotkeys.
 */
public class DebugOverlayAppState extends AbstractAppState {
    private static final Logger logger = LoggerFactory.getLogger(DebugOverlayAppState.class);
    
    private SimpleApplication app;
    private BitmapText overlayText;
    private boolean visible = false;
    private SystemInfo systemInfo;
    private DebugOverlayFormatter formatter;
    private String currentText = ""; // Track current text for testing
    private ServiceHub serviceHub; // Phase 0A: Access to ValidationResult
    
    public DebugOverlayAppState(ServiceHub serviceHub) {
        this.serviceHub = serviceHub;
    }
    
    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = (SimpleApplication) app;
        this.systemInfo = SystemInfo.detect();
        this.formatter = new DebugOverlayFormatter();
        
        // Create overlay text
        BitmapFont font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
        overlayText = new BitmapText(font, false);
        overlayText.setSize(font.getCharSet().getRenderedSize());
        overlayText.setColor(com.jme3.math.ColorRGBA.White);
        overlayText.setLocalTranslation(10, app.getCamera().getHeight() - 10, 0);
        overlayText.setText(""); // Initialize with empty text
        
        // Register hotkeys
        app.getInputManager().addMapping("ToggleDebug", new KeyTrigger(KeyInput.KEY_F3));
        app.getInputManager().addMapping("ReloadAssets", new KeyTrigger(KeyInput.KEY_F9));
        app.getInputManager().addMapping("RebuildMeshes", new KeyTrigger(KeyInput.KEY_F10));
        app.getInputManager().addMapping("ShowChunkBounds", new KeyTrigger(KeyInput.KEY_F11));
        
        app.getInputManager().addListener(actionListener, 
            "ToggleDebug", "ReloadAssets", "RebuildMeshes", "ShowChunkBounds");
        
        logger.info("DebugOverlayAppState initialized");
    }
    
    private final ActionListener actionListener = (name, isPressed, tpf) -> {
        if (!isPressed) {
            switch (name) {
                case "ToggleDebug":
                    toggleOverlay();
                    break;
                case "ReloadAssets":
                    logger.info("F9: Reload assets (stub)");
                    break;
                case "RebuildMeshes":
                    if (serviceHub != null && serviceHub.has(ChunkManager.class)) {
                        serviceHub.get(ChunkManager.class).rebuildAllMeshes();
                        logger.info("F10: Rebuilding all chunk meshes");
                    } else {
                        logger.info("F10: Rebuild meshes (ChunkManager not initialized)");
                    }
                    break;
                case "ShowChunkBounds":
                    if (serviceHub != null && serviceHub.has(ChunkDoctorService.class)) {
                        serviceHub.get(ChunkDoctorService.class).toggle();
                        logger.info("F11: Toggled chunk bounds visualization");
                    } else {
                        logger.info("F11: Show chunk bounds (ChunkDoctorService not initialized)");
                    }
                    break;
            }
        }
    };
    
    private void toggleOverlay() {
        visible = !visible;
        if (visible) {
            updateOverlayText(); // Update text immediately when toggling on
            app.getGuiNode().attachChild(overlayText);
            logger.info("Debug overlay enabled - CP 0.2 OK");
        } else {
            overlayText.removeFromParent();
            logger.info("Debug overlay disabled");
        }
    }
    
    /**
     * Package-private toggle method for testing.
     * Allows tests to toggle overlay without relying on InputManager.
     */
    void toggleForTest() {
        toggleOverlay();
    }
    
    @Override
    public void update(float tpf) {
        if (visible && overlayText != null) {
            updateOverlayText();
        }
    }
    
    private void updateOverlayText() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMemoryMB = runtime.maxMemory() / (1024 * 1024);
        
        int fps = (int) app.getTimer().getFrameRate();
        
        // Phase 0A: Retrieve ValidationResult from ServiceHub if available
        ValidationResult assetValidation = null;
        if (serviceHub != null && serviceHub.has(ValidationResult.class)) {
            assetValidation = serviceHub.get(ValidationResult.class);
        }
        
        String chunkStats = "";
        long worldSeed = 0L;
        if (serviceHub != null && serviceHub.has(ChunkManager.class)) {
            ChunkManager chunkManager = serviceHub.get(ChunkManager.class);
            int loadedChunks = chunkManager.getLoadedChunkCount();
            chunkStats = String.format("\nChunks: %d loaded", loadedChunks);
            if (serviceHub.has(ChunkDoctorService.class)) {
                ChunkDoctorService doctor = serviceHub.get(ChunkDoctorService.class);
                ChunkDoctorStats stats = doctor.getStats();
                chunkStats += String.format(" | Vertices: %d (avg %.0f/chunk) | Triangles: %d",
                    stats.totalVertices(), stats.avgVerticesPerChunk(), stats.totalTriangles());
            }
            if (chunkManager.getWorldGenerator() != null) {
                worldSeed = chunkManager.getWorldGenerator().getSeed();
            }
        } else if (serviceHub != null) {
            if (serviceHub.has(WorldGenerator.class)) {
                worldSeed = serviceHub.get(WorldGenerator.class).getSeed();
            }
            if (worldSeed == 0L && serviceHub.has(WorldSaveManager.class)) {
                long persistedSeed = serviceHub.get(WorldSaveManager.class).getPersistedSeed();
                if (persistedSeed != 0L) {
                    worldSeed = persistedSeed;
                }
            }
        }

        currentText = formatter.format(fps, systemInfo, usedMemoryMB, maxMemoryMB, assetValidation, chunkStats, worldSeed);
        overlayText.setText(currentText);
    }
    
    /**
     * Package-private getter for current text, used in testing.
     * @return The current overlay text
     */
    String getCurrentText() {
        return currentText;
    }
    
    /**
     * Package-private method to check if overlay is visible, used in testing.
     * @return true if overlay is visible
     */
    boolean isOverlayVisible() {
        return visible && overlayText != null && overlayText.getParent() != null;
    }
    
    @Override
    public void cleanup() {
        super.cleanup();
        if (overlayText != null) {
            overlayText.removeFromParent();
        }
        
        // Unregister input mappings
        app.getInputManager().deleteMapping("ToggleDebug");
        app.getInputManager().deleteMapping("ReloadAssets");
        app.getInputManager().deleteMapping("RebuildMeshes");
        app.getInputManager().deleteMapping("ShowChunkBounds");
        
        logger.info("DebugOverlayAppState cleaned up");
    }
}
