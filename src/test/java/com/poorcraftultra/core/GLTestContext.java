package com.poorcraftultra.core;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Shared test utility for GLFW and OpenGL context lifecycle management.
 * This extension ensures GLFW is initialized once per test run and provides
 * a shared hidden window with OpenGL context for all tests.
 * 
 * Usage: Add @ExtendWith(GLTestContext.class) to test classes that need OpenGL.
 */
public class GLTestContext implements BeforeAllCallback, AfterAllCallback {
    private static boolean initialized = false;
    private static long sharedWindow = 0;
    private static final Object lock = new Object();

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        synchronized (lock) {
            if (!initialized) {
                // Initialize GLFW
                if (!glfwInit()) {
                    throw new RuntimeException("Failed to initialize GLFW for testing");
                }

                // Configure GLFW for OpenGL 3.3 core
                glfwDefaultWindowHints();
                glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
                glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
                glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
                glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
                glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

                // Create shared hidden window
                sharedWindow = glfwCreateWindow(800, 600, "Test Context Window", 0, 0);
                if (sharedWindow == 0) {
                    throw new RuntimeException("Failed to create shared test window");
                }

                glfwMakeContextCurrent(sharedWindow);
                org.lwjgl.opengl.GL.createCapabilities();

                initialized = true;
                System.out.println("GLTestContext: Shared OpenGL context initialized");
            } else {
                // Make sure the context is current for this test class
                glfwMakeContextCurrent(sharedWindow);
            }
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        // Note: We don't tear down here because other test classes may still need the context.
        // The context will be cleaned up when the JVM exits or can be manually cleaned
        // by calling cleanup() in a global test suite teardown if needed.
    }

    /**
     * Manually cleanup the shared context. Should only be called once all tests are complete.
     * This is typically not needed as the JVM will clean up on exit.
     */
    public static void cleanup() {
        synchronized (lock) {
            if (initialized) {
                if (sharedWindow != 0) {
                    glfwDestroyWindow(sharedWindow);
                    sharedWindow = 0;
                }
                glfwTerminate();
                initialized = false;
                System.out.println("GLTestContext: Shared OpenGL context destroyed");
            }
        }
    }

    /**
     * Gets the shared window handle.
     * @return The shared window handle
     */
    public static long getSharedWindow() {
        return sharedWindow;
    }
}
