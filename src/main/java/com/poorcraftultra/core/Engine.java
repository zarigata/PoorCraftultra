package com.poorcraftultra.core;

import com.poorcraftultra.input.InputManager;
import com.poorcraftultra.rendering.Camera;
import com.poorcraftultra.rendering.Renderer;
import org.lwjgl.glfw.GLFW;

/**
 * Core game engine that orchestrates the game loop.
 */
public class Engine {

    private Window window;
    private InputManager inputManager;
    private Camera camera;
    private Renderer renderer;

    private double lastTime;

    public void run() {
        init();

        while (!window.shouldClose()) {
            double currentTime = GLFW.glfwGetTime();
            float deltaTime = (float) (currentTime - lastTime);
            lastTime = currentTime;

            inputManager.processInput(camera, deltaTime);
            update(deltaTime);
            renderer.render(camera, (float) window.getWidth() / window.getHeight());
            window.update();
        }
    }

    private void init() {
        if (!GLFW.glfwInit()) {
            throw new RuntimeException("Failed to initialize GLFW");
        }

        window = new Window();
        window.init();

        inputManager = new InputManager();
        inputManager.init(window);

        camera = new Camera();

        renderer = new Renderer();
        renderer.init();

        lastTime = GLFW.glfwGetTime();
    }

    private void update(float deltaTime) {
        camera.updateVectors();
    }

    public void cleanup() {
        if (renderer != null) {
            renderer.cleanup();
        }
        if (window != null) {
            window.cleanup();
        }
        GLFW.glfwTerminate();
    }
}
