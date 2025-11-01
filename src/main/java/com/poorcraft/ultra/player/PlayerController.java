package com.poorcraft.ultra.player;

import com.jme3.app.SimpleApplication;
import com.jme3.input.FlyByCamera;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.poorcraft.ultra.player.BlockPicker.BlockPickResult;
import com.poorcraft.ultra.voxel.BlockType;
import com.poorcraft.ultra.voxel.Chunk;
import com.poorcraft.ultra.voxel.ChunkManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerController implements AnalogListener, ActionListener {
    private static final Logger logger = LoggerFactory.getLogger(PlayerController.class);
    private static final float DEFAULT_MOVE_SPEED = 5f;
    private static final float SPRINT_MULTIPLIER = 1.8f;
    private static final float DEFAULT_MOUSE_SENSITIVITY = 1.5f;

    private SimpleApplication app;
    private InputManager inputManager;
    private ChunkManager chunkManager;
    private BlockPicker blockPicker;
    private BlockHighlighter blockHighlighter;
    private PlayerInventory inventory;

    private float yaw;
    private float pitch;
    private float moveSpeed = DEFAULT_MOVE_SPEED;
    private float mouseSensitivity = DEFAULT_MOUSE_SENSITIVITY;
    private boolean moveForward;
    private boolean moveBackward;
    private boolean moveLeft;
    private boolean moveRight;
    private boolean sprint;

    public void init(SimpleApplication app, ChunkManager chunkManager, BlockPicker blockPicker,
                     BlockHighlighter blockHighlighter, PlayerInventory inventory) {
        this.app = app;
        this.chunkManager = chunkManager;
        this.blockPicker = blockPicker;
        this.blockHighlighter = blockHighlighter;
        this.inventory = inventory;
        this.inputManager = app.getInputManager();

        disableFlyCam(app.getFlyByCamera());
        inputManager.setCursorVisible(false);

        registerInputs();
        Vector3f camDir = app.getCamera().getDirection();
        yaw = FastMath.atan2(camDir.x, camDir.z);
        pitch = FastMath.asin(camDir.y);
    }

    private void disableFlyCam(FlyByCamera flyCam) {
        if (flyCam == null) {
            logger.warn("FlyByCamera unavailable; skipping disable step");
            return;
        }
        flyCam.setEnabled(false);
    }

    private void registerInputs() {
        inputManager.addMapping("MouseX+", new MouseAxisTrigger(MouseInput.AXIS_X, true));
        inputManager.addMapping("MouseX-", new MouseAxisTrigger(MouseInput.AXIS_X, false));
        inputManager.addMapping("MouseY+", new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        inputManager.addMapping("MouseY-", new MouseAxisTrigger(MouseInput.AXIS_Y, false));

        inputManager.addMapping("MoveForward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("MoveBackward", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("MoveLeft", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("MoveRight", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Sprint", new KeyTrigger(KeyInput.KEY_LSHIFT));
        inputManager.addMapping("BreakBlock", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("PlaceBlock", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));

        inputManager.addListener(this, "MouseX+", "MouseX-", "MouseY+", "MouseY-");
        inputManager.addListener(this, "MoveForward", "MoveBackward", "MoveLeft", "MoveRight", "Sprint");
        inputManager.addListener(this, "BreakBlock", "PlaceBlock");
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        switch (name) {
            case "MouseX+":
                yaw -= value * mouseSensitivity;
                break;
            case "MouseX-":
                yaw += value * mouseSensitivity;
                break;
            case "MouseY+":
                pitch += value * mouseSensitivity;
                break;
            case "MouseY-":
                pitch -= value * mouseSensitivity;
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
        switch (name) {
            case "MoveForward" -> moveForward = isPressed;
            case "MoveBackward" -> moveBackward = isPressed;
            case "MoveLeft" -> moveLeft = isPressed;
            case "MoveRight" -> moveRight = isPressed;
            case "Sprint" -> sprint = isPressed;
            case "BreakBlock" -> {
                if (!isPressed) {
                    handleBreakBlock();
                }
            }
            case "PlaceBlock" -> {
                if (!isPressed) {
                    handlePlaceBlock();
                }
            }
            default -> {
            }
        }
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
    }

    private void handleBreakBlock() {
        BlockPickResult result = blockPicker.pickBlock();
        if (result == null) {
            return;
        }
        BlockType target = chunkManager.getBlock(result.blockX(), result.blockY(), result.blockZ());
        if (target == BlockType.AIR) {
            return;
        }
        chunkManager.setBlock(result.blockX(), result.blockY(), result.blockZ(), BlockType.AIR);
        inventory.addBlock(target, 1);
        logger.info("Broke {} at ({}, {}, {}); inventory: {}", target.name(), result.blockX(),
            result.blockY(), result.blockZ(), inventory.getCount(target));
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
