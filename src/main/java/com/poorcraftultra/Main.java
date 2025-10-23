package com.poorcraftultra;

import com.poorcraftultra.core.Renderer;
import com.poorcraftultra.core.Window;
import org.lwjgl.glfw.GLFW;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Main entry point for PoorCraftUltra.
 */
public class Main {
    private static final int WINDOW_WIDTH = 1280;
    private static final int WINDOW_HEIGHT = 720;
    private static final String WINDOW_TITLE = "PoorCraftUltra - Phase 1";

    public static void main(String[] args) {
        System.out.println("Starting PoorCraftUltra...");

        Window window = null;
        Renderer renderer = null;

        try {
            // Initialize GLFW
            if (!glfwInit()) {
                throw new RuntimeException("Failed to initialize GLFW");
            }
            System.out.println("GLFW initialized");

            // Create and initialize window
            window = new Window(WINDOW_WIDTH, WINDOW_HEIGHT, WINDOW_TITLE);
            window.init();

            // Create and initialize renderer
            renderer = new Renderer();
            renderer.init();

            // Main game loop
            System.out.println("Entering main loop...");
            while (!window.shouldClose()) {
                // Poll for window events
                glfwPollEvents();

                // Render
                renderer.render(window.getWidth(), window.getHeight());

                // Swap buffers
                window.swapBuffers();
            }

            System.out.println("Exiting main loop...");

        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);

        } finally {
            // Cleanup
            if (renderer != null) {
                renderer.cleanup();
            }

            if (window != null) {
                window.destroy();
            }

            glfwTerminate();
            System.out.println("GLFW terminated");
            System.out.println("PoorCraftUltra shutdown complete");
        }
    }
}
