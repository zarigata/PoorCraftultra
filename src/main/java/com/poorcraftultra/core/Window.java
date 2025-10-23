package com.poorcraftultra.core;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Manages the GLFW window lifecycle and OpenGL context.
 */
public class Window {
    private long windowHandle;
    private final int width;
    private final int height;
    private final String title;

    /**
     * Creates a new Window instance.
     *
     * @param width  The width of the window in pixels
     * @param height The height of the window in pixels
     * @param title  The title of the window
     */
    public Window(int width, int height, String title) {
        this.width = width;
        this.height = height;
        this.title = title;
    }

    /**
     * Initializes the window and OpenGL context.
     * Must be called before any OpenGL operations.
     *
     * @throws RuntimeException if window creation fails
     */
    public void init() {
        // Setup error callback
        GLFWErrorCallback.createPrint(System.err).set();

        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        // Create window
        windowHandle = glfwCreateWindow(width, height, title, NULL, NULL);
        if (windowHandle == NULL) {
            throw new RuntimeException("Failed to create GLFW window");
        }

        // Make the OpenGL context current
        glfwMakeContextCurrent(windowHandle);

        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(windowHandle);

        // Create OpenGL capabilities
        GL.createCapabilities();

        System.out.println("Window initialized: " + width + "x" + height);
    }

    /**
     * Checks if the window should close.
     *
     * @return true if the window should close, false otherwise
     */
    public boolean shouldClose() {
        return glfwWindowShouldClose(windowHandle);
    }

    /**
     * Swaps the front and back buffers.
     * Should be called at the end of each frame.
     */
    public void swapBuffers() {
        glfwSwapBuffers(windowHandle);
    }

    /**
     * Destroys the window and frees resources.
     */
    public void destroy() {
        glfwDestroyWindow(windowHandle);
        System.out.println("Window destroyed");
    }

    /**
     * Gets the window width.
     *
     * @return The width in pixels
     */
    public int getWidth() {
        return width;
    }

    /**
     * Gets the window height.
     *
     * @return The height in pixels
     */
    public int getHeight() {
        return height;
    }

    /**
     * Gets the GLFW window handle.
     *
     * @return The window handle
     */
    public long getHandle() {
        return windowHandle;
    }
}
