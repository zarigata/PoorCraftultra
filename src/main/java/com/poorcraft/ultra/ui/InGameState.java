package com.poorcraft.ultra.ui;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;
import com.poorcraft.ultra.app.ServiceHub;
import com.poorcraft.ultra.gameplay.CraftingManager;
import com.poorcraft.ultra.gameplay.ItemDropTable;
import com.poorcraft.ultra.player.PlayerController;
import com.poorcraft.ultra.voxel.ChunkManager;
import java.util.concurrent.CompletableFuture;
import com.poorcraft.ultra.world.WorldSaveManager;
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
    private CraftingManager craftingManager;
    private ItemDropTable itemDropTable;
    private CraftingUIState craftingUIState;

    public InGameState(ServiceHub serviceHub) {
        this.serviceHub = serviceHub;
    }

    @Override
    protected void initialize(Application app) {
        application = (SimpleApplication) app;

        if (serviceHub.has(CraftingManager.class)) {
            craftingManager = serviceHub.get(CraftingManager.class);
        } else {
            craftingManager = new CraftingManager();
            craftingManager.init();
            serviceHub.register(CraftingManager.class, craftingManager);
            logger.info("CraftingManager initialized");
        }

        if (serviceHub.has(ItemDropTable.class)) {
            itemDropTable = serviceHub.get(ItemDropTable.class);
        } else {
            itemDropTable = new ItemDropTable();
            itemDropTable.init();
            serviceHub.register(ItemDropTable.class, itemDropTable);
            logger.info("ItemDropTable initialized");
        }

        chunkManager = requireService(ChunkManager.class);
        requireService(com.poorcraft.ultra.player.BlockPicker.class);
        requireService(com.poorcraft.ultra.player.BlockHighlighter.class);
        requireService(com.poorcraft.ultra.player.PlayerInventory.class);
        playerController = requireService(PlayerController.class);
        inputConfig = requireService(InputConfig.class);

        if (playerController != null) {
            playerController.setDropTable(itemDropTable);
        }

        craftingUIState = new CraftingUIState(serviceHub);

        logger.info("InGameState initialized with services: ChunkManager={}, PlayerController={}, InputConfig={}",
                chunkManager != null, playerController != null, inputConfig != null);
    }

    @Override
    protected void cleanup(Application app) {
        if (chunkManager != null && serviceHub.has(WorldSaveManager.class)) {
            WorldSaveManager worldSaveManager = serviceHub.get(WorldSaveManager.class);
            if (worldSaveManager != null && !worldSaveManager.isSavedOnShutdown()) {
                worldSaveManager.saveAll(chunkManager);
                worldSaveManager.markSavedOnShutdown();
            }
        }
        if (craftingUIState != null && getStateManager() != null) {
            getStateManager().detach(craftingUIState);
            craftingUIState = null;
        }
        logger.info("InGameState cleaned up");
    }

    @Override
    protected void onEnable() {
        if (chunkManager != null && chunkManager.getLoadedChunkCount() == 0) {
            chunkManager.loadChunks3x3(0, 0);
            logger.info("Loaded 3x3 chunk grid");
        }

        if (chunkManager != null) {
            logger.info("Loaded {} chunks for in-game state", chunkManager.getLoadedChunkCount());
        }

        if (application.getCamera().getLocation().y < 10f) {
            application.getCamera().setLocation(new Vector3f(8f, 70f, 8f));
            application.getCamera().lookAt(new Vector3f(8f, 64f, 8f), Vector3f.UNIT_Y);
        }

        logger.info("Camera positioned at: {}", application.getCamera().getLocation());

        if (inputConfig == null) {
            logger.error("InputConfig is null, cannot register pause action");
            return;
        }

        String pauseKeybind = inputConfig.getKeybindString("pause");
        if (pauseKeybind == null || pauseKeybind.isBlank()) {
            logger.info("Pause action lacked keybind; defaulting to ESCAPE");
            CompletableFuture<Boolean> future = inputConfig.rebindAction("pause", "ESCAPE");
            future.whenComplete((success, error) -> {
                if (error != null) {
                    logger.warn("Failed to rebind pause action to ESCAPE", error);
                }
                inputConfig.registerActionOnAppThread("pause", this::handlePauseAction);
                logger.info("Registered pause action with InputConfig");
            });
        } else {
            inputConfig.registerAction("pause", this::handlePauseAction);
            logger.info("Registered pause action with InputConfig");
        }

        if (playerController == null) {
            logger.error("PlayerController is null, cannot enable input");
            return;
        }

        playerController.enable();
        logger.info("PlayerController enabled - input registration requested");

        if (craftingUIState != null && getStateManager() != null && !getStateManager().hasState(craftingUIState)) {
            getStateManager().attach(craftingUIState);
            craftingUIState.setEnabled(false);
            logger.info("CraftingUIState attached (disabled by default)");
        }

        logger.info("InGameState enabled");
    }

    @Override
    protected void onDisable() {
        if (inputConfig != null) {
            inputConfig.unregisterAction("pause");
            logger.info("Unregistered pause action from InputConfig");
        }

        if (playerController != null) {
            playerController.disable();
            logger.info("PlayerController disabled - input unregistration requested");
        }

        if (craftingUIState != null && craftingUIState.isEnabled()) {
            craftingUIState.setEnabled(false);
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
