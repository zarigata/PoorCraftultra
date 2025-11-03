package com.poorcraft.ultra.player;

import com.jme3.app.Application;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * First-person camera and movement controller.
 */
public class FirstPersonController implements ActionListener, AnalogListener {
    
    private static final Logger logger = LoggerFactory.getLogger(FirstPersonController.class);
    
    private final Application app;
    private final Camera camera;
    private final InputManager inputManager;
    private final Node playerNode;
    private BetterCharacterControl characterControl;
    
    private float yaw = 0;
    private float pitch = 0;
    private boolean moveForward = false;
    private boolean moveBackward = false;
    private boolean moveLeft = false;
    private boolean moveRight = false;
    
    private float mouseSensitivity = 0.003f;
    private float moveSpeed = 4.0f;
    private float eyeHeight = 1.6f;
    private boolean inputEnabled = true;
    
    public FirstPersonController(Application app) {
        this.app = app;
        this.camera = app.getCamera();
        this.inputManager = app.getInputManager();
        this.playerNode = new Node("PlayerNode");
    }
    
    /**
     * Initializes controller.
     */
    public void init(BulletAppState bulletAppState, Vector3f spawnPos) {
        logger.info("Initializing FirstPersonController...");
        
        // Create character control
        characterControl = new BetterCharacterControl(0.4f, 1.8f, 80f);
        characterControl.setGravity(new Vector3f(0, -20, 0));
        characterControl.setJumpForce(new Vector3f(0, 300, 0));
        
        playerNode.addControl(characterControl);
        bulletAppState.getPhysicsSpace().add(characterControl);
        
        // Warp to spawn position
        characterControl.warp(spawnPos);
        
        // Disable FlyCam
        if (app.getFlyByCamera() != null) {
            app.getFlyByCamera().setEnabled(false);
        }
        
        // Hide cursor
        inputManager.setCursorVisible(false);
        
        // Set up input mappings
        setupInputMappings();
        
        logger.info("FirstPersonController initialized at ({}, {}, {})", 
            spawnPos.x, spawnPos.y, spawnPos.z);
    }
    
    private void setupInputMappings() {
        // Movement keys
        inputManager.addMapping("MoveForward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("MoveBackward", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("MoveLeft", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("MoveRight", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
        
        // Mouse look
        inputManager.addMapping("LookLeft", new MouseAxisTrigger(MouseInput.AXIS_X, true));
        inputManager.addMapping("LookRight", new MouseAxisTrigger(MouseInput.AXIS_X, false));
        inputManager.addMapping("LookUp", new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        inputManager.addMapping("LookDown", new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        
        // Mouse buttons (for interaction)
        inputManager.addMapping("BreakBlock", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("PlaceBlock", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        
        // Add listeners
        inputManager.addListener(this, "MoveForward", "MoveBackward", "MoveLeft", "MoveRight", "Jump",
                                      "BreakBlock", "PlaceBlock");
        inputManager.addListener(this, "LookLeft", "LookRight", "LookUp", "LookDown");
    }
    
    /**
     * Update loop.
     */
    public void update(float tpf) {
        if (!inputEnabled) {
            return;
        }
        
        // Update camera rotation
        Quaternion rotation = new Quaternion();
        rotation.fromAngles(pitch, yaw, 0);
        camera.setRotation(rotation);
        
        // Build walk direction from input
        Vector3f forward = camera.getDirection().clone();
        forward.y = 0;
        forward.normalizeLocal();
        
        Vector3f left = camera.getLeft().clone();
        left.y = 0;
        left.normalizeLocal();
        
        Vector3f walkDir = new Vector3f();
        if (moveForward) {
            walkDir.addLocal(forward);
        }
        if (moveBackward) {
            walkDir.subtractLocal(forward);
        }
        if (moveLeft) {
            walkDir.addLocal(left);
        }
        if (moveRight) {
            walkDir.subtractLocal(left);
        }
        
        if (walkDir.lengthSquared() > 0) {
            walkDir.normalizeLocal();
            walkDir.multLocal(moveSpeed);
        }
        
        characterControl.setWalkDirection(walkDir);
        characterControl.setViewDirection(forward);
        
        // Update camera position
        Vector3f physicsLoc = characterControl.getPhysicsLocation();
        camera.setLocation(physicsLoc.add(0, eyeHeight, 0));
    }
    
    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (!inputEnabled) {
            return;
        }
        
        switch (name) {
            case "MoveForward":
                moveForward = isPressed;
                break;
            case "MoveBackward":
                moveBackward = isPressed;
                break;
            case "MoveLeft":
                moveLeft = isPressed;
                break;
            case "MoveRight":
                moveRight = isPressed;
                break;
            case "Jump":
                if (isPressed) {
                    characterControl.jump();
                }
                break;
            // BreakBlock and PlaceBlock handled by BlockInteraction
        }
    }
    
    @Override
    public void onAnalog(String name, float value, float tpf) {
        if (!inputEnabled) {
            return;
        }
        
        switch (name) {
            case "LookLeft":
                yaw += value * mouseSensitivity;
                break;
            case "LookRight":
                yaw -= value * mouseSensitivity;
                break;
            case "LookUp":
                pitch -= value * mouseSensitivity;
                pitch = Math.max(pitch, -1.55f); // Clamp to -89 degrees
                break;
            case "LookDown":
                pitch += value * mouseSensitivity;
                pitch = Math.min(pitch, 1.55f); // Clamp to +89 degrees
                break;
        }
    }
    
    public Vector3f getPosition() {
        return characterControl.getPhysicsLocation();
    }
    
    public Camera getCamera() {
        return camera;
    }
    
    public Vector3f getViewDirection() {
        return camera.getDirection();
    }
    
    public void setPosition(Vector3f pos) {
        characterControl.warp(pos);
    }
    
    public void setMouseSensitivity(float sensitivity) {
        this.mouseSensitivity = sensitivity;
    }
    
    public void setMoveSpeed(float speed) {
        this.moveSpeed = speed;
    }
    
    /**
     * Enables or disables input handling (for UI overlays).
     */
    public void setInputEnabled(boolean enabled) {
        this.inputEnabled = enabled;
        if (!enabled) {
            // Clear movement flags when disabling
            moveForward = false;
            moveBackward = false;
            moveLeft = false;
            moveRight = false;
        }
        logger.debug("FirstPersonController input {}", enabled ? "enabled" : "disabled");
    }
    
    public boolean isInputEnabled() {
        return inputEnabled;
    }
    
    /**
     * Cleanup.
     */
    public void cleanup() {
        inputManager.removeListener(this);
        inputManager.deleteTrigger("MoveForward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.deleteTrigger("MoveBackward", new KeyTrigger(KeyInput.KEY_S));
        inputManager.deleteTrigger("MoveLeft", new KeyTrigger(KeyInput.KEY_A));
        inputManager.deleteTrigger("MoveRight", new KeyTrigger(KeyInput.KEY_D));
        inputManager.deleteTrigger("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
        
        // Re-enable FlyCam
        if (app.getFlyByCamera() != null) {
            app.getFlyByCamera().setEnabled(true);
        }
        
        // Show cursor
        inputManager.setCursorVisible(true);
    }
}
