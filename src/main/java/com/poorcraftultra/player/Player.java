package com.poorcraftultra.player;

import com.poorcraftultra.input.InputAction;
import com.poorcraftultra.input.InputManager;
import com.poorcraftultra.world.block.Block;
import com.poorcraftultra.world.block.BlockRegistry;
import com.poorcraftultra.world.chunk.ChunkManager;
import org.joml.Vector3f;

/**
 * Represents the player entity with physics, collision detection, and movement.
 * 
 * <p>Physics Model:
 * - Gravity: 32 blocks/second² (Minecraft-like)
 * - Movement: Frame-rate independent using delta time
 * - Collision: AABB swept tests against voxel grid
 * 
 * <p>Coordinate System:
 * - Position represents player's feet position
 * - Eye position is offset upward by eye height
 * - AABB is centered on X/Z, extends upward from feet
 */
public class Player {
    // Player dimensions
    public static final float PLAYER_WIDTH = 0.6f;
    public static final float PLAYER_HEIGHT = 1.8f;
    public static final float PLAYER_HEIGHT_CROUCHED = 1.5f;
    public static final float PLAYER_EYE_HEIGHT = 1.62f;
    public static final float PLAYER_EYE_HEIGHT_CROUCHED = 1.27f;
    
    // Movement constants
    public static final float WALK_SPEED = 4.317f;      // blocks/second
    public static final float SPRINT_SPEED = 5.612f;    // blocks/second
    public static final float CROUCH_SPEED = 1.295f;    // blocks/second
    public static final float JUMP_VELOCITY = 8.0f;     // blocks/second
    public static final float GRAVITY = 32.0f;          // blocks/second²
    
    private final Vector3f position;
    private final Vector3f velocity;
    private float yaw;
    private float pitch;
    
    private boolean onGround;
    private boolean crouching;
    private boolean sprinting;
    
    private final ChunkManager chunkManager;
    
    /**
     * Creates a new player at the specified position.
     * 
     * @param position Initial position (feet position)
     * @param chunkManager Chunk manager for collision queries
     */
    public Player(Vector3f position, ChunkManager chunkManager) {
        this.position = new Vector3f(position);
        this.velocity = new Vector3f(0, 0, 0);
        this.yaw = 0.0f;
        this.pitch = 0.0f;
        this.onGround = false;
        this.crouching = false;
        this.sprinting = false;
        this.chunkManager = chunkManager;
    }
    
    /**
     * Updates player state based on input and physics.
     * 
     * @param deltaTime Time since last update in seconds
     * @param input Input manager for reading player actions
     */
    public void update(float deltaTime, InputManager input) {
        // Calculate movement direction from input
        Vector3f moveDir = new Vector3f(0, 0, 0);
        
        if (input.isActionActive(InputAction.MOVE_FORWARD)) {
            moveDir.z -= 1.0f;
        }
        if (input.isActionActive(InputAction.MOVE_BACKWARD)) {
            moveDir.z += 1.0f;
        }
        if (input.isActionActive(InputAction.MOVE_LEFT)) {
            moveDir.x -= 1.0f;
        }
        if (input.isActionActive(InputAction.MOVE_RIGHT)) {
            moveDir.x += 1.0f;
        }
        
        // Normalize movement direction if moving diagonally
        if (moveDir.lengthSquared() > 0) {
            moveDir.normalize();
        }
        
        // Rotate movement direction by player yaw
        float yawRad = (float) Math.toRadians(yaw);
        float rotatedX = moveDir.x * (float) Math.cos(yawRad) - moveDir.z * (float) Math.sin(yawRad);
        float rotatedZ = moveDir.x * (float) Math.sin(yawRad) + moveDir.z * (float) Math.cos(yawRad);
        moveDir.x = rotatedX;
        moveDir.z = rotatedZ;
        
        // Update movement states
        crouching = input.isActionActive(InputAction.CROUCH);
        sprinting = input.isActionActive(InputAction.SPRINT) && !crouching;
        
        // Determine movement speed
        float speed = WALK_SPEED;
        if (sprinting) {
            speed = SPRINT_SPEED;
        } else if (crouching) {
            speed = CROUCH_SPEED;
        }
        
        // Apply movement to horizontal velocity
        velocity.x = moveDir.x * speed;
        velocity.z = moveDir.z * speed;
        
        // Handle jumping
        if (input.isActionActive(InputAction.JUMP) && onGround) {
            velocity.y = JUMP_VELOCITY;
        }
        
        // Apply gravity
        velocity.y -= GRAVITY * deltaTime;
        
        // Apply movement with collision detection
        move(deltaTime);
    }
    
    /**
     * Applies velocity to position with collision detection.
     * Uses swept AABB collision tests for each axis.
     * 
     * @param deltaTime Time step in seconds
     */
    private void move(float deltaTime) {
        Vector3f delta = new Vector3f(velocity).mul(deltaTime);
        
        // Move and collide on each axis separately
        // X axis
        position.x += delta.x;
        if (checkCollision()) {
            position.x -= delta.x;
            velocity.x = 0;
        }
        
        // Y axis
        position.y += delta.y;
        if (checkCollision()) {
            if (velocity.y < 0) {
                onGround = true;
            }
            position.y -= delta.y;
            velocity.y = 0;
        } else {
            onGround = false;
        }
        
        // Z axis
        position.z += delta.z;
        if (checkCollision()) {
            position.z -= delta.z;
            velocity.z = 0;
        }
    }
    
    /**
     * Checks if the player's AABB collides with any solid blocks.
     * 
     * @return true if collision detected
     */
    private boolean checkCollision() {
        return checkCollision(getAABB());
    }
    
    /**
     * Checks if a custom AABB collides with any solid blocks.
     * 
     * @param aabb Array of two vectors: [min, max]
     * @return true if collision detected
     */
    private boolean checkCollision(Vector3f[] aabb) {
        Vector3f min = aabb[0];
        Vector3f max = aabb[1];
        
        // Calculate block coordinates the AABB spans
        // Use epsilon on max to prevent counting blocks only touched at boundary
        int minX = (int) Math.floor(min.x);
        int minY = (int) Math.floor(min.y);
        int minZ = (int) Math.floor(min.z);
        int maxX = (int) Math.floor(max.x - 1e-6f);
        int maxY = (int) Math.floor(max.y - 1e-6f);
        int maxZ = (int) Math.floor(max.z - 1e-6f);
        
        // Check all blocks in the AABB range
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    byte blockId = chunkManager.getBlock(x, y, z);
                    Block block = BlockRegistry.getInstance().getBlock(blockId);
                    // Only solid blocks cause collision (transparent blocks like glass are solid but don't block light)
                    if (block.isSolid()) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Gets the player's axis-aligned bounding box.
     * 
     * @return Array of two vectors: [min, max]
     */
    public Vector3f[] getAABB() {
        float halfWidth = PLAYER_WIDTH / 2.0f;
        float height = crouching ? PLAYER_HEIGHT_CROUCHED : PLAYER_HEIGHT;
        
        Vector3f min = new Vector3f(
            position.x - halfWidth,
            position.y,
            position.z - halfWidth
        );
        
        Vector3f max = new Vector3f(
            position.x + halfWidth,
            position.y + height,
            position.z + halfWidth
        );
        
        return new Vector3f[] { min, max };
    }
    
    /**
     * Gets the player's eye position for camera placement.
     * 
     * @return Eye position in world coordinates
     */
    public Vector3f getEyePosition() {
        float eyeHeight = crouching ? PLAYER_EYE_HEIGHT_CROUCHED : PLAYER_EYE_HEIGHT;
        return new Vector3f(position.x, position.y + eyeHeight, position.z);
    }
    
    /**
     * Sets the player position.
     * 
     * @param pos New position
     */
    public void setPosition(Vector3f pos) {
        this.position.set(pos);
    }
    
    /**
     * Gets the player position.
     * 
     * @return Defensive copy of position
     */
    public Vector3f getPosition() {
        return new Vector3f(position);
    }
    
    /**
     * Gets the player velocity.
     * 
     * @return Defensive copy of velocity
     */
    public Vector3f getVelocity() {
        return new Vector3f(velocity);
    }
    
    /**
     * Gets the player's yaw rotation.
     * 
     * @return Yaw in degrees
     */
    public float getYaw() {
        return yaw;
    }
    
    /**
     * Sets the player's yaw rotation.
     * 
     * @param yaw Yaw in degrees
     */
    public void setYaw(float yaw) {
        this.yaw = yaw;
    }
    
    /**
     * Gets the player's pitch rotation.
     * 
     * @return Pitch in degrees
     */
    public float getPitch() {
        return pitch;
    }
    
    /**
     * Sets the player's pitch rotation.
     * 
     * @param pitch Pitch in degrees
     */
    public void setPitch(float pitch) {
        this.pitch = pitch;
    }
    
    /**
     * Checks if the player is on the ground.
     * 
     * @return true if on ground
     */
    public boolean isOnGround() {
        return onGround;
    }
    
    /**
     * Checks if the player is crouching.
     * 
     * @return true if crouching
     */
    public boolean isCrouching() {
        return crouching;
    }
    
    /**
     * Checks if the player is sprinting.
     * 
     * @return true if sprinting
     */
    public boolean isSprinting() {
        return sprinting;
    }
    
    /**
     * Sets the crouching state.
     * Validates headroom when attempting to stand up.
     * 
     * @param crouching true to crouch
     */
    public void setCrouching(boolean crouching) {
        // When standing up, check if there's enough space
        if (!crouching && this.crouching) {
            // Calculate standing AABB
            float halfWidth = PLAYER_WIDTH / 2.0f;
            Vector3f min = new Vector3f(
                position.x - halfWidth,
                position.y,
                position.z - halfWidth
            );
            Vector3f max = new Vector3f(
                position.x + halfWidth,
                position.y + PLAYER_HEIGHT,
                position.z + halfWidth
            );
            Vector3f[] standingAABB = new Vector3f[] { min, max };
            
            // Only allow standing if no collision
            if (checkCollision(standingAABB)) {
                return; // Keep crouching, insufficient headroom
            }
        }
        this.crouching = crouching;
    }
    
    /**
     * Sets the sprinting state.
     * 
     * @param sprinting true to sprint
     */
    public void setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
    }
}
