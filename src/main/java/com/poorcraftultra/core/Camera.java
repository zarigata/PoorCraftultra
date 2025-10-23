package com.poorcraftultra.core;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * First-person camera class for 3D rendering.
 * 
 * <p>Coordinate System: Y-up, right-handed coordinate system.
 * Rotation Conventions:
 * - Pitch: Vertical rotation in degrees, clamped to [-89, 89] to prevent gimbal lock
 * - Yaw: Horizontal rotation in degrees, wrapped to [0, 360]
 * - 0° yaw points along positive Z-axis
 * 
 * <p>This class is pure math with no dependencies on Player or Input systems,
 * making it reusable and testable.
 */
public class Camera {
    private static final Vector3f WORLD_UP = new Vector3f(0.0f, 1.0f, 0.0f);
    private static final float MIN_PITCH = -89.0f;
    private static final float MAX_PITCH = 89.0f;
    
    private final Vector3f position;
    private float pitch; // Vertical rotation in degrees
    private float yaw;   // Horizontal rotation in degrees
    
    private final Vector3f front;
    private final Vector3f right;
    private final Vector3f up;
    
    /**
     * Creates a new camera at the specified position.
     * 
     * @param position Initial camera position in world coordinates
     */
    public Camera(Vector3f position) {
        this.position = new Vector3f(position);
        this.pitch = 0.0f;
        this.yaw = 0.0f;
        this.front = new Vector3f();
        this.right = new Vector3f();
        this.up = new Vector3f();
        updateVectors();
    }
    
    /**
     * Recalculates the camera's direction vectors based on current pitch and yaw.
     * Called automatically when rotation changes.
     */
    private void updateVectors() {
        // Calculate front vector from pitch and yaw
        float pitchRad = (float) Math.toRadians(pitch);
        float yawRad = (float) Math.toRadians(yaw);
        
        front.x = (float) (Math.cos(pitchRad) * Math.sin(yawRad));
        front.y = (float) Math.sin(pitchRad);
        front.z = (float) (Math.cos(pitchRad) * Math.cos(yawRad));
        front.normalize();
        
        // Calculate right vector (perpendicular to front and world up)
        // WORLD_UP × front for right-handed system
        new Vector3f(WORLD_UP).cross(front, right);
        right.normalize();
        
        // Calculate up vector (perpendicular to right and front)
        // front × right for right-handed system
        new Vector3f(front).cross(right, up);
        up.normalize();
    }
    
    /**
     * Generates the view matrix for rendering.
     * 
     * @return View matrix transforming world space to camera space
     */
    public Matrix4f getViewMatrix() {
        Vector3f target = new Vector3f(position).add(front);
        return new Matrix4f().lookAt(position, target, up);
    }
    
    /**
     * Sets the camera position.
     * 
     * @param pos New position in world coordinates
     */
    public void setPosition(Vector3f pos) {
        this.position.set(pos);
    }
    
    /**
     * Sets the camera position.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     */
    public void setPosition(float x, float y, float z) {
        this.position.set(x, y, z);
    }
    
    /**
     * Gets the camera position.
     * 
     * @return Defensive copy of the position vector
     */
    public Vector3f getPosition() {
        return new Vector3f(position);
    }
    
    /**
     * Sets the vertical rotation angle.
     * 
     * @param pitch Pitch angle in degrees, will be clamped to [-89, 89]
     */
    public void setPitch(float pitch) {
        this.pitch = Math.max(MIN_PITCH, Math.min(MAX_PITCH, pitch));
        updateVectors();
    }
    
    /**
     * Sets the horizontal rotation angle.
     * 
     * @param yaw Yaw angle in degrees, will be wrapped to [0, 360]
     */
    public void setYaw(float yaw) {
        this.yaw = yaw % 360.0f;
        if (this.yaw < 0) {
            this.yaw += 360.0f;
        }
        updateVectors();
    }
    
    /**
     * Rotates the camera by the specified deltas.
     * 
     * @param deltaPitch Change in pitch (degrees)
     * @param deltaYaw Change in yaw (degrees)
     */
    public void rotate(float deltaPitch, float deltaYaw) {
        setPitch(this.pitch + deltaPitch);
        setYaw(this.yaw + deltaYaw);
    }
    
    /**
     * Gets the current pitch angle.
     * 
     * @return Pitch in degrees
     */
    public float getPitch() {
        return pitch;
    }
    
    /**
     * Gets the current yaw angle.
     * 
     * @return Yaw in degrees
     */
    public float getYaw() {
        return yaw;
    }
    
    /**
     * Gets the forward direction vector.
     * 
     * @return Defensive copy of the front vector
     */
    public Vector3f getFront() {
        return new Vector3f(front);
    }
    
    /**
     * Gets the right direction vector.
     * 
     * @return Defensive copy of the right vector
     */
    public Vector3f getRight() {
        return new Vector3f(right);
    }
    
    /**
     * Gets the up direction vector.
     * 
     * @return Defensive copy of the up vector
     */
    public Vector3f getUp() {
        return new Vector3f(up);
    }
}
