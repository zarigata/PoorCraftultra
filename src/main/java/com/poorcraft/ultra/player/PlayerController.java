package com.poorcraft.ultra.player;

import com.jme3.app.SimpleApplication;
import com.jme3.input.FlyByCamera;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.poorcraft.ultra.gameplay.ItemDropTable;
import com.poorcraft.ultra.gameplay.ItemStack;
import com.poorcraft.ultra.player.BlockPicker.BlockPickResult;
import com.poorcraft.ultra.ui.InputConfig;
import com.poorcraft.ultra.voxel.BlockType;
import com.poorcraft.ultra.voxel.Chunk;
import com.poorcraft.ultra.voxel.ChunkManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PlayerController implements AnalogListener, ActionListener {
    private static final Logger logger = LoggerFactory.getLogger(PlayerController.class);
    private static final float DEFAULT_MOVE_SPEED = 5f;
    private static final float SPRINT_MULTIPLIER = 1.8f;
    private static final float DEFAULT_MOUSE_SENSITIVITY = 1.5f;

    private static final String[] ANALOG_MAPPINGS = {
        "MouseX+",
        "MouseX-",
        "MouseY+",
        "MouseY-"
    };

    private static final String[] ACTION_MAPPINGS = {
        "moveForward",
        "moveBackward",
        "moveLeft",
        "moveRight",
        "sprint",
        "breakBlock",
        "placeBlock"
    };

    private SimpleApplication app;
    private ChunkManager chunkManager;
    private BlockPicker blockPicker;
    private BlockHighlighter blockHighlighter;
    private PlayerInventory inventory;
    private InputConfig inputConfig;
    private ItemDropTable dropTable;
    private boolean inputsEnabled;

    private float yaw;
    private float pitch;
    private float moveSpeed = DEFAULT_MOVE_SPEED;
    private boolean moveForward;
    private boolean moveBackward;
    private boolean moveLeft;
    private boolean moveRight;
    private boolean sprint;

    public void init(SimpleApplication app, ChunkManager chunkManager, BlockPicker blockPicker,
                     BlockHighlighter blockHighlighter, PlayerInventory inventory, InputConfig inputConfig) {
        this.app = app;
        this.chunkManager = chunkManager;
        this.blockPicker = blockPicker;
        this.blockHighlighter = blockHighlighter;
        this.inventory = inventory;
        this.inputConfig = inputConfig;

        disableFlyCam(app.getFlyByCamera());

        Vector3f camDir = app.getCamera().getDirection();
        yaw = FastMath.atan2(camDir.x, camDir.z);
        pitch = FastMath.asin(camDir.y);
        logger.info("PlayerController initialized with camera yaw={}, pitch={}", yaw, pitch);
    }

    public void setDropTable(ItemDropTable dropTable) {
        this.dropTable = dropTable;
    }

    private void disableFlyCam(FlyByCamera flyCam) {
        if (flyCam == null) {
            logger.warn("FlyByCamera unavailable; skipping disable step");
            return;
        }
        flyCam.setEnabled(false);
        logger.info("FlyCam disabled for PlayerController");
    }

    public synchronized void enable() {
        if (inputsEnabled) {
            logger.debug("PlayerController.enable() called but inputs already enabled, skipping");
            return;
        }
        if (inputConfig == null) {
            logger.error("PlayerController.enable() failed: InputConfig is null");
            return;
        }

        logger.info("PlayerController.enable() - registering {} analog and {} action mappings",
                ANALOG_MAPPINGS.length, ACTION_MAPPINGS.length);
        for (String analog : ANALOG_MAPPINGS) {
            inputConfig.registerAnalog(analog, this);
        }
        for (String action : ACTION_MAPPINGS) {
            inputConfig.registerAction(action, this);
        }
        inputsEnabled = true;
        logger.info("PlayerController.enable() - input registration complete, inputsEnabled=true");
    }

    public synchronized void disable() {
        if (!inputsEnabled) {
            logger.debug("PlayerController.disable() called but inputs not enabled, skipping");
            return;
        }
        if (inputConfig == null) {
            logger.error("PlayerController.disable() failed: InputConfig is null");
            return;
        }

        for (String analog : ANALOG_MAPPINGS) {
            inputConfig.unregisterAnalog(analog);
        }
        for (String action : ACTION_MAPPINGS) {
            inputConfig.unregisterAction(action);
        }
        inputsEnabled = false;
        logger.info("PlayerController.disable() - input unregistration complete, inputsEnabled=false");
    }

    public synchronized boolean isInputsEnabled() {
        return inputsEnabled;
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        logger.trace("onAnalog: name={}, value={}", name, value);
        float sensitivity = inputConfig != null ? inputConfig.getMouseSensitivity() : DEFAULT_MOUSE_SENSITIVITY;
        boolean inverted = inputConfig != null && inputConfig.isMouseYInverted();

        switch (name) {
            case "MouseX+":
                yaw -= value * sensitivity;
                break;
            case "MouseX-":
                yaw += value * sensitivity;
                break;
            case "MouseY+":
                pitch -= value * sensitivity * (inverted ? -1 : 1);
                break;
            case "MouseY-":
                pitch += value * sensitivity * (inverted ? -1 : 1);
                break;
            default:
                break;
        }

        pitch = FastMath.clamp(pitch, -FastMath.PI / 2 + 0.01f, FastMath.PI / 2 - 0.01f);
        Quaternion rotation = new Quaternion();
        rotation.fromAngles(pitch, yaw, 0f);
        app.getCamera().setRotation(rotation);
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        logger.trace("onAction: name={}, isPressed={}", name, isPressed);
        switch (name) {
            case "moveForward" -> moveForward = isPressed;
            case "moveBackward" -> moveBackward = isPressed;
            case "moveLeft" -> moveLeft = isPressed;
            case "moveRight" -> moveRight = isPressed;
            case "sprint" -> sprint = isPressed;
            case "breakBlock" -> {
                if (isPressed) {
                    beginBlockBreak();
                } else {
                    cancelBlockBreak(false);
                }
            }
            case "placeBlock" -> {
                if (!isPressed) {
                    handlePlaceBlock();
                }
            }
            default -> {
            }
        }
    }

    private ToolType resolveToolType(BlockType held) {
        if (held == null || held == BlockType.AIR) {
            return ToolType.NONE;
        }
        return switch (held) {
            case WOOD_OAK, PLANKS, CHEST -> ToolType.WOOD;
            case STONE, COAL_ORE, IRON_ORE, GOLD_ORE -> ToolType.STONE;
            default -> ToolType.NONE;
        };
    }

    public void update(float tpf) {
        Vector3f camDir = app.getCamera().getDirection().clone();
        Vector3f camLeft = app.getCamera().getLeft().clone();

        Vector3f movement = new Vector3f();
        if (moveForward) {
            movement.addLocal(camDir);
        }
        if (moveBackward) {
            movement.addLocal(camDir.negate());
        }
        if (moveLeft) {
            movement.addLocal(camLeft);
        }
        if (moveRight) {
            movement.addLocal(camLeft.negate());
        }
        movement.y = 0;

        if (movement.lengthSquared() > 0) {
            movement.normalizeLocal();
            float speed = moveSpeed * (sprint ? SPRINT_MULTIPLIER : 1f);
            movement.multLocal(speed * tpf);
            app.getCamera().setLocation(app.getCamera().getLocation().add(movement));
        }

        Vector3f cameraPos = app.getCamera().getLocation();
        Vector3f cameraDirection = app.getCamera().getDirection();
        blockPicker.updateCamera(cameraPos, cameraDirection);
        BlockPickResult result = blockPicker.pickBlock();
        blockHighlighter.update(result);

        updateBlockBreak(tpf);
    }

    private void beginBlockBreak() {
        BlockPickResult result = blockPicker.pickBlock();
        if (result == null) {
            cancelBlockBreak(true);
            return;
        }
        BlockType target = chunkManager.getBlock(result.blockX(), result.blockY(), result.blockZ());
        if (target == BlockType.AIR) {
            cancelBlockBreak(true);
            return;
        }

        BlockType heldTool = inventory.getSelectedBlock();
        ToolType toolType = resolveToolType(heldTool);
        ToolType requiredTool = target.requiredTool();
        float hardness = Math.max(0.1f, target.hardness());

        boolean toolUnderpowered = requiredTool != ToolType.NONE && toolType.ordinal() < requiredTool.ordinal();
        float effectiveSpeed;
        if (toolUnderpowered) {
            effectiveSpeed = Math.max(0.1f, requiredTool.speedMultiplier() * 0.2f);
        } else {
            effectiveSpeed = Math.max(0.1f, toolType.speedMultiplier());
        }
        float requiredTime = Math.max(0.1f, hardness / effectiveSpeed);

        breakingState = new BlockBreakingState(result.blockX(), result.blockY(), result.blockZ(), target,
            heldTool, requiredTime);
    }

    private void cancelBlockBreak(boolean immediate) {
        if (!immediate && breakingState != null && breakingState.progress >= breakingState.requiredTime) {
            completeBlockBreak();
        }
        breakingState = null;
    }

    private void updateBlockBreak(float tpf) {
        if (breakingState == null) {
            return;
        }
        BlockType current = chunkManager.getBlock(breakingState.x, breakingState.y, breakingState.z);
        if (current != breakingState.block) {
            breakingState = null;
            return;
        }
        breakingState.progress += tpf;
        if (breakingState.progress >= breakingState.requiredTime) {
            completeBlockBreak();
            breakingState = null;
        }
    }

    private void completeBlockBreak() {
        if (breakingState == null) {
            return;
        }
        chunkManager.setBlock(breakingState.x, breakingState.y, breakingState.z, BlockType.AIR);

        BlockType tool = breakingState.heldTool;
        if (dropTable != null) {
            List<ItemStack> drops = dropTable.getDrops(breakingState.block, tool == null ? BlockType.AIR : tool);
            if (drops.isEmpty()) {
                logger.info("Broke {} at ({}, {}, {}); no drops", breakingState.block.name(), breakingState.x,
                    breakingState.y, breakingState.z);
            } else {
                for (ItemStack drop : drops) {
                    inventory.addBlock(drop.type(), drop.count());
                    logger.info("Broke {} at ({}, {}, {}); dropped {} x{}", breakingState.block.name(),
                        breakingState.x, breakingState.y, breakingState.z, drop.type().name(), drop.count());
                }
            }
        } else {
            inventory.addBlock(breakingState.block, 1);
            logger.info("Broke {} at ({}, {}, {}); inventory: {}", breakingState.block.name(), breakingState.x,
                breakingState.y, breakingState.z, inventory.getCount(breakingState.block));
        }
    }

    private BlockBreakingState breakingState;

    private static final class BlockBreakingState {
        private final int x;
        private final int y;
        private final int z;
        private final BlockType block;
        private final BlockType heldTool;
        private final float requiredTime;
        private float progress;

        private BlockBreakingState(int x, int y, int z, BlockType block, BlockType heldTool, float requiredTime) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.block = block;
            this.heldTool = heldTool;
            this.requiredTime = requiredTime;
        }
    }

    private void handlePlaceBlock() {
        BlockPickResult result = blockPicker.pickBlock();
        if (result == null) {
            return;
        }

        BlockType selected = inventory.getSelectedBlock();
        if (selected == BlockType.AIR || !inventory.canPlace(selected)) {
            return;
        }

        int placeX = result.blockX() + result.face().offsetX();
        int placeY = result.blockY() + result.face().offsetY();
        int placeZ = result.blockZ() + result.face().offsetZ();

        if (placeY < 0 || placeY >= Chunk.SIZE_Y) {
            return;
        }

        if (chunkManager.getBlock(placeX, placeY, placeZ) != BlockType.AIR) {
            return;
        }

        chunkManager.setBlock(placeX, placeY, placeZ, selected);
        inventory.removeBlock(selected, 1);
        logger.info("Placed {} at ({}, {}, {}); inventory: {}", selected.name(), placeX, placeY, placeZ,
            inventory.getCount(selected));
    }
}
