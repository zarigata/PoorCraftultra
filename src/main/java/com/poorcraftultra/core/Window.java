package com.poorcraftultra.core;

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Manages the GLFW window lifecycle and OpenGL context.
 */
public class Window {
    private long windowHandle;
    private final int width;
    private final int height;
    private final String title;
    private int fbWidth;
    private int fbHeight;
    private GLFWErrorCallback errorCallback;
    private GLFWFramebufferSizeCallback framebufferSizeCallback;

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
        errorCallback = GLFWErrorCallback.createPrint(System.err).set();

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

        // Get initial framebuffer size
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            glfwGetFramebufferSize(windowHandle, pWidth, pHeight);
            fbWidth = pWidth.get(0);
            fbHeight = pHeight.get(0);
        }

        // Set initial viewport
        glViewport(0, 0, fbWidth, fbHeight);

        // Setup framebuffer size callback
        framebufferSizeCallback = glfwSetFramebufferSizeCallback(windowHandle, (window, width, height) -> {
            fbWidth = width;
            fbHeight = height;
            glViewport(0, 0, width, height);
        });

        System.out.println("Window initialized: " + width + "x" + height + " (framebuffer: " + fbWidth + "x" + fbHeight + ")");
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
        // Free window-specific callbacks
        Callbacks.glfwFreeCallbacks(windowHandle);
        
        // Destroy the window
        glfwDestroyWindow(windowHandle);
        
        // Free the error callback
        if (errorCallback != null) {
            glfwSetErrorCallback(null);
            errorCallback.free();
        }
        
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

    /**
     * Gets the framebuffer width.
     *
     * @return The framebuffer width in pixels
     */
    public int getFramebufferWidth() {
        return fbWidth;
    }

    /**
     * Gets the framebuffer height.
     *
     * @return The framebuffer height in pixels
     */
    public int getFramebufferHeight() {
        return fbHeight;
    }
}
