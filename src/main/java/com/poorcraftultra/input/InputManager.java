package com.poorcraftultra.input;

import com.poorcraftultra.core.Window;
import com.poorcraftultra.rendering.Camera;

/**
 * Input manager that coordinates keyboard and mouse input.
 */
public class InputManager {

    private KeyboardInput keyboardInput;
    private MouseInput mouseInput;

    public void init(Window window) {
        keyboardInput = new KeyboardInput(window.getWindowHandle());
        mouseInput = new MouseInput(window.getWindowHandle());
    }

    public void processInput(Camera camera, float deltaTime) {
        mouseInput.processInput(camera, deltaTime);
        keyboardInput.processInput(camera, deltaTime);
    }

    public void cleanup() {
        // Cleanup if needed
    }

    public KeyboardInput getKeyboardInput() {
        return keyboardInput;
    }

    public MouseInput getMouseInput() {
        return mouseInput;
    }
}
