package com.poorcraftultra.core;

import com.poorcraftultra.util.Constants;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * Window management class using GLFW.
 */
public class Window {

    private long windowHandle;
    private int width;
    private int height;

    public void init() {
        // Set GLFW window hints
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        // Create window
        windowHandle = glfwCreateWindow(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT,
                                        Constants.WINDOW_TITLE, 0L, 0L);
        if (windowHandle == 0L) {
            throw new RuntimeException("Failed to create GLFW window");
        }

        // Make OpenGL context current
        glfwMakeContextCurrent(windowHandle);

        // Enable VSync
        glfwSwapInterval(1);

        // Center window on screen
        centerWindow();

        // Store dimensions
        this.width = Constants.WINDOW_WIDTH;
        this.height = Constants.WINDOW_HEIGHT;

        // Set up framebuffer size callback for window resizing
        glfwSetFramebufferSizeCallback(windowHandle, (window, width, height) -> {
            this.width = width;
            this.height = height;
            GL11.glViewport(0, 0, width, height);
        });

        // Create OpenGL capabilities
        GL.createCapabilities();

        // Set initial viewport
        GL11.glViewport(0, 0, width, height);
    }

    private void centerWindow() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);

            glfwGetWindowSize(windowHandle, pWidth, pHeight);

            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            glfwSetWindowPos(
                windowHandle,
                (vidmode.width() - pWidth.get(0)) / 2,
                (vidmode.height() - pHeight.get(0)) / 2
            );
        }
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(windowHandle);
    }

    public void update() {
        glfwSwapBuffers(windowHandle);
        glfwPollEvents();
    }

    public void cleanup() {
        glfwFreeCallbacks(windowHandle);
        glfwDestroyWindow(windowHandle);
    }

    public long getWindowHandle() {
        return windowHandle;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
