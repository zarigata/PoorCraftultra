package com.poorcraftultra.input;

import com.poorcraftultra.rendering.Camera;
import com.poorcraftultra.util.Constants;
import org.lwjgl.glfw.GLFW;

/**
 * Keyboard input handler.
 */
public class KeyboardInput {

    private long windowHandle;
    private boolean[] keys;

    public KeyboardInput(long windowHandle) {
        this.windowHandle = windowHandle;
        this.keys = new boolean[GLFW.GLFW_KEY_LAST + 1];

        // Set key callback
        GLFW.glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
            if (key >= 0 && key <= GLFW.GLFW_KEY_LAST) {
                if (action == GLFW.GLFW_PRESS) {
                    keys[key] = true;
                } else if (action == GLFW.GLFW_RELEASE) {
                    keys[key] = false;
                }
            }
        });
    }

    public boolean isKeyPressed(int key) {
        if (key >= 0 && key <= GLFW.GLFW_KEY_LAST) {
            return keys[key];
        }
        return false;
    }

    // Method for testing
    public void setKeyState(int key, boolean pressed) {
        if (key >= 0 && key <= GLFW.GLFW_KEY_LAST) {
            keys[key] = pressed;
        }
    }

    public void processInput(Camera camera, float deltaTime) {
        float speed = Constants.CAMERA_MOVE_SPEED * deltaTime;

        // Sprint modifier
        if (isKeyPressed(GLFW.GLFW_KEY_LEFT_SHIFT)) {
            speed *= Constants.CAMERA_SPRINT_MULTIPLIER;
        }

        // Movement
        if (isKeyPressed(GLFW.GLFW_KEY_W)) {
            camera.moveForward(speed);
        }
        if (isKeyPressed(GLFW.GLFW_KEY_S)) {
            camera.moveBackward(speed);
        }
        if (isKeyPressed(GLFW.GLFW_KEY_A)) {
            camera.moveLeft(speed);
        }
        if (isKeyPressed(GLFW.GLFW_KEY_D)) {
            camera.moveRight(speed);
        }

        // Close window
        if (isKeyPressed(GLFW.GLFW_KEY_ESCAPE)) {
            GLFW.glfwSetWindowShouldClose(windowHandle, true);
        }
    }
}
