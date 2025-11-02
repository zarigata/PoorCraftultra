package com.poorcraft.ultra.ui;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;
import com.poorcraft.ultra.app.ServiceHub;
import com.poorcraft.ultra.player.PlayerController;
import com.poorcraft.ultra.voxel.ChunkManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In-game state managing gameplay, player controller, and chunk updates.
 */
public class InGameState extends BaseAppState {
    private static final Logger logger = LoggerFactory.getLogger(InGameState.class);

    private final ServiceHub serviceHub;

    private SimpleApplication application;
    private ChunkManager chunkManager;
    private PlayerController playerController;
    private InputConfig inputConfig;

    public InGameState(ServiceHub serviceHub) {
        this.serviceHub = serviceHub;
    }

    @Override
    protected void initialize(Application app) {
        application = (SimpleApplication) app;

        chunkManager = requireService(ChunkManager.class);
        requireService(com.poorcraft.ultra.player.BlockPicker.class);
        requireService(com.poorcraft.ultra.player.BlockHighlighter.class);
        requireService(com.poorcraft.ultra.player.PlayerInventory.class);
        playerController = requireService(PlayerController.class);
        inputConfig = requireService(InputConfig.class);

        logger.info("InGameState initialized");
    }

    @Override
    protected void cleanup(Application app) {
        if (chunkManager != null) {
            chunkManager.saveAll();
        }
        logger.info("InGameState cleaned up");
    }

    @Override
    protected void onEnable() {
        if (chunkManager != null && chunkManager.getLoadedChunkCount() == 0) {
            chunkManager.loadChunks3x3(0, 0);
            logger.info("Loaded 3x3 chunk grid");
        }

        if (application.getCamera().getLocation().y < 10f) {
            application.getCamera().setLocation(new Vector3f(8f, 70f, 8f));
            application.getCamera().lookAt(new Vector3f(8f, 64f, 8f), Vector3f.UNIT_Y);
        }

        if (inputConfig != null) {
            inputConfig.registerAction("pause", this::handlePauseAction);
        }

        if (playerController != null) {
            playerController.enable();
        }

        logger.info("InGameState enabled");
    }

    @Override
    protected void onDisable() {
        if (inputConfig != null) {
            inputConfig.unregisterAction("pause");
        }

        if (playerController != null) {
            playerController.disable();
        }

        logger.info("InGameState disabled (paused)");
    }

    private void handlePauseAction(String name, boolean isPressed, float tpf) {
        if (!"pause".equals(name) || isPressed) {
            return;
        }
        GameStateManager gsm = serviceHub.get(GameStateManager.class);
        gsm.pauseGame();
    }

    private <T> T requireService(Class<T> type) {
        T service = serviceHub.get(type);
        if (service == null) {
            throw new IllegalStateException(type.getSimpleName() + " service is not registered");
        }
        return service;
    }

    @Override
    public void update(float tpf) {
        if (!isEnabled()) {
            return;
        }

        if (chunkManager != null) {
            chunkManager.update(tpf);
        }

        if (playerController != null) {
            playerController.update(tpf);
        }
    }
}
