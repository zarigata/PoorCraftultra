package com.poorcraftultra.integration;

import com.poorcraftultra.core.Window;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Window class.
 */
public class WindowIntegrationTest {

    @BeforeAll
    static void initGLFW() {
        if (!GLFW.glfwInit()) {
            fail("Failed to initialize GLFW");
        }
    }

    @AfterAll
    static void terminateGLFW() {
        GLFW.glfwTerminate();
    }

    @Test
    void testWindowCreation() {
        Window window = new Window();
        assertDoesNotThrow(window::init);

        assertNotEquals(0L, window.getWindowHandle());
        assertEquals(1280, window.getWidth());
        assertEquals(720, window.getHeight());
        assertFalse(window.shouldClose());

        // Test update
        assertDoesNotThrow(window::update);

        // Test cleanup
        assertDoesNotThrow(window::cleanup);
    }

    @Test
    void testWindowHandleValidity() {
        Window window = new Window();
        window.init();

        long handle = window.getWindowHandle();
        assertNotEquals(0L, handle);
        assertFalse(GLFW.glfwWindowShouldClose(handle));

        window.cleanup();
    }

    @Test
    void testMultipleUpdates() {
        Window window = new Window();
        window.init();

        // Perform multiple updates
        for (int i = 0; i < 10; i++) {
            assertDoesNotThrow(window::update);
            assertFalse(window.shouldClose());
        }

        window.cleanup();
    }
}
