package com.poorcraftultra.player;

import com.poorcraftultra.core.Camera;
import com.poorcraftultra.input.InputManager;

/**
 * Coordinates player movement with camera updates.
 * 
 * <p>This class integrates the Player entity with the Camera system,
 * ensuring that camera position and rotation stay synchronized with
 * the player's state. It acts as the bridge between input handling,
 * player physics, and camera rendering.
 * 
 * <p>Usage:
 * 1. Create PlayerController with Player and Camera instances
 * 2. Register handleMouseLook as InputManager callback
 * 3. Call update() each frame to sync player and camera
 */
public class PlayerController {
    private final Player player;
    private final Camera camera;
    
    /**
     * Creates a new player controller.
     * 
     * @param player Player entity to control
     * @param camera Camera to synchronize with player
     */
    public PlayerController(Player player, Camera camera) {
        this.player = player;
        this.camera = camera;
    }
    
    /**
     * Updates player physics and synchronizes camera.
     * 
     * @param deltaTime Time since last update in seconds
     * @param input Input manager for reading player actions
     */
    public void update(float deltaTime, InputManager input) {
        // Update player physics and movement
        player.update(deltaTime, input);
        
        // Sync camera to player
        camera.setPosition(player.getEyePosition());
        camera.setPitch(player.getPitch());
        camera.setYaw(player.getYaw());
    }
    
    /**
     * Handles mouse look input by updating player rotation.
     * This method should be registered as the InputManager mouse look callback.
     * 
     * @param deltaPitch Change in pitch (degrees)
     * @param deltaYaw Change in yaw (degrees)
     */
    public void handleMouseLook(float deltaPitch, float deltaYaw) {
        // Update player rotation
        float newPitch = player.getPitch() + deltaPitch;
        float newYaw = player.getYaw() + deltaYaw;
        
        // Clamp pitch to prevent gimbal lock
        newPitch = Math.max(-89.0f, Math.min(89.0f, newPitch));
        
        // Wrap yaw to [0, 360]
        newYaw = newYaw % 360.0f;
        if (newYaw < 0) {
            newYaw += 360.0f;
        }
        
        player.setPitch(newPitch);
        player.setYaw(newYaw);
    }
    
    /**
     * Gets the player entity.
     * 
     * @return Player instance
     */
    public Player getPlayer() {
        return player;
    }
    
    /**
     * Gets the camera.
     * 
     * @return Camera instance
     */
    public Camera getCamera() {
        return camera;
    }
}
