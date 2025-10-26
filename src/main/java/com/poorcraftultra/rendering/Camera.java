package com.poorcraftultra.rendering;

import com.poorcraftultra.util.Constants;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * First-person camera class using JOML for math operations.
 */
public class Camera {

    private Vector3f position;
    private float yaw;
    private float pitch;

    private Vector3f front;
    private Vector3f up;
    private Vector3f right;

    public Camera() {
        position = new Vector3f(0.0f, 0.0f, 0.0f);
        yaw = 0.0f;
        pitch = 0.0f;
        front = new Vector3f(0.0f, 0.0f, -1.0f);
        up = new Vector3f(0.0f, 1.0f, 0.0f);
        right = new Vector3f(1.0f, 0.0f, 0.0f);
        updateVectors();
    }

    public void updateVectors() {
        // Calculate front vector
        front.x = (float) Math.sin(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch));
        front.y = (float) Math.sin(Math.toRadians(pitch));
        front.z = (float) -Math.cos(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch));
        front.normalize();

        // Calculate right and up vectors
        right = new Vector3f(front).cross(new Vector3f(0.0f, 1.0f, 0.0f)).normalize();
        up = new Vector3f(right).cross(front).normalize();
    }

    public Matrix4f getViewMatrix() {
        Matrix4f view = new Matrix4f();
        view.lookAt(position, new Vector3f(position).add(front), up);
        return view;
    }

    public Matrix4f getProjectionMatrix(float fov, float aspect, float near, float far) {
        Matrix4f projection = new Matrix4f();
        projection.perspective((float) Math.toRadians(fov), aspect, near, far);
        return projection;
    }

    public Matrix4f getProjectionMatrix(float aspect) {
        return getProjectionMatrix(Constants.FOV, aspect, Constants.Z_NEAR, Constants.Z_FAR);
    }

    // Movement methods
    public void moveForward(float distance) {
        position.add(new Vector3f(front).mul(distance));
    }

    public void moveBackward(float distance) {
        position.sub(new Vector3f(front).mul(distance));
    }

    public void moveLeft(float distance) {
        position.sub(new Vector3f(right).mul(distance));
    }

    public void moveRight(float distance) {
        position.add(new Vector3f(right).mul(distance));
    }

    public void rotate(float yawDelta, float pitchDelta) {
        yaw += yawDelta;
        pitch += pitchDelta;

        // Clamp pitch to prevent camera flipping
        if (pitch > 89.0f) {
            pitch = 89.0f;
        }
        if (pitch < -89.0f) {
            pitch = -89.0f;
        }

        updateVectors();
    }

    // Getters and setters
    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(Vector3f position) {
        this.position = position;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public Vector3f getFront() {
        return front;
    }

    public Vector3f getUp() {
        return up;
    }

    public Vector3f getRight() {
        return right;
    }
}
