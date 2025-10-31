package com.poorcraft.ultra.player;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.controls.ActionListener;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.poorcraft.ultra.engine.api.InputMappings;
import com.poorcraft.ultra.gameplay.Inventory;
import com.poorcraft.ultra.gameplay.ItemStack;
import com.poorcraft.ultra.shared.Config;
import com.poorcraft.ultra.shared.Logger;
import com.poorcraft.ultra.ui.MainMenuState;
import com.poorcraft.ultra.voxel.BlockDefinition;
import com.poorcraft.ultra.voxel.BlockRegistry;
import com.poorcraft.ultra.voxel.BlockType;
import com.poorcraft.ultra.voxel.ChunkRenderer;
import com.poorcraft.ultra.voxel.ChunkStorage;
import com.poorcraft.ultra.voxel.SuperchunkTestState;
import java.util.Objects;
import java.util.Optional;

public final class PlayerInteractionState extends BaseAppState implements ActionListener {

    private static final Logger logger = Logger.getLogger(PlayerInteractionState.class);

    private final Inventory inventory;

    private ChunkStorage chunkStorage;
    private Node chunkRootNode;
    private float reachDistance = BlockPicker.DEFAULT_REACH_DISTANCE;

    public PlayerInteractionState(Inventory inventory) {
        this.inventory = Objects.requireNonNull(inventory, "inventory");
    }

    @Override
    protected void initialize(Application app) {
        ChunkRenderer renderer = ChunkRenderer.getInstance();
        chunkRootNode = renderer.getChunkRootNode();

        SuperchunkTestState superchunk = getStateManager().getState(SuperchunkTestState.class);
        if (superchunk == null) {
            throw new IllegalStateException("SuperchunkTestState is required for player interaction");
        }
        chunkStorage = superchunk.getChunkStorage();
        if (chunkStorage == null) {
            throw new IllegalStateException("ChunkStorage is not available");
        }

        Config config = Config.getInstance();
        if (config.hasPath("gameplay.reachDistance")) {
            reachDistance = config.getFloat("gameplay.reachDistance");
        }

        logger.info("Player interaction initialized (reach distance: {} blocks)", reachDistance);
    }

    @Override
    protected void cleanup(Application app) {
        chunkStorage = null;
        chunkRootNode = null;
        logger.info("Player interaction cleaned up");
    }

    @Override
    protected void onEnable() {
        // No-op
    }

    @Override
    protected void onDisable() {
        // No-op
    }

    @Override
    public void update(float tpf) {
        // Event-driven
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (!isPressed) {
            return;
        }

        MainMenuState menuState = getStateManager().getState(MainMenuState.class);
        if (menuState != null && menuState.isEnabled()) {
            return;
        }

        switch (name) {
            case InputMappings.BREAK_BLOCK -> handleBreakBlock();
            case InputMappings.PLACE_BLOCK -> handlePlaceBlock();
            case InputMappings.SELECT_SLOT_1,
                    InputMappings.SELECT_SLOT_2,
                    InputMappings.SELECT_SLOT_3,
                    InputMappings.SELECT_SLOT_4,
                    InputMappings.SELECT_SLOT_5,
                    InputMappings.SELECT_SLOT_6,
                    InputMappings.SELECT_SLOT_7,
                    InputMappings.SELECT_SLOT_8,
                    InputMappings.SELECT_SLOT_9 -> handleSelectSlot(name);
            default -> {
            }
        }
    }

    private void handleBreakBlock() {
        Optional<BlockHitResult> hitResult = BlockPicker.raycast(getApplication().getCamera(), chunkRootNode, reachDistance);
        if (hitResult.isEmpty()) {
            return;
        }

        BlockHitResult hit = hitResult.get();
        int x = hit.getBlockX();
        int y = hit.getBlockY();
        int z = hit.getBlockZ();

        short blockId = chunkStorage.getBlock(x, y, z);
        if (blockId == BlockType.AIR.getId()) {
            return;
        }

        BlockDefinition definition = BlockRegistry.getInstance().getDefinition(blockId);
        BlockType type = definition.getType();

        boolean added = inventory.addItem(type, 1);
        if (!added) {
            logger.warn("Inventory full, dropped block at ({}, {}, {})", x, y, z);
        }

        chunkStorage.setBlock(x, y, z, BlockType.AIR.getId());

        logger.info("Block broken at ({}, {}, {}), added to inventory: {}", x, y, z, added);
    }

    private void handlePlaceBlock() {
        Optional<BlockHitResult> hitResult = BlockPicker.raycast(getApplication().getCamera(), chunkRootNode, reachDistance);
        if (hitResult.isEmpty()) {
            return;
        }

        BlockHitResult hit = hitResult.get();
        int x = hit.getPlacementX();
        int y = hit.getPlacementY();
        int z = hit.getPlacementZ();

        if (chunkStorage.getBlock(x, y, z) != BlockType.AIR.getId()) {
            return;
        }

        ItemStack selectedItem = inventory.getSelectedItem();
        if (selectedItem == null) {
            return;
        }

        BlockType type = selectedItem.blockType();
        int selectedSlot = inventory.getSelectedSlot();
        ItemStack slotStack = inventory.getSlot(selectedSlot);
        if (slotStack == null || slotStack.blockType() != type) {
            logger.warn("Selected slot {} does not match block type {} for placement", selectedSlot, type);
            return;
        }

        if (!inventory.removeFromSlot(selectedSlot, 1)) {
            logger.warn("Insufficient items in slot {} to place block {}", selectedSlot, type);
            return;
        }

        chunkStorage.setBlock(x, y, z, type.getId());

        logger.info("Block placed at ({}, {}, {}), type: {}", x, y, z, type);
    }

    private void handleSelectSlot(String actionName) {
        int slotIndex = actionName.charAt(actionName.length() - 1) - '1';
        inventory.setSelectedSlot(slotIndex);
        logger.info("Hotbar slot selected: {}", slotIndex + 1);
    }
}
