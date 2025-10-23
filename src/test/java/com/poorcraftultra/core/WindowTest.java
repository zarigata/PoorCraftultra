package com.poorcraftultra.core;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;

/**
 * Unit tests for the Window class.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WindowTest {

    @BeforeAll
    public static void setupGLFW() {
        if (!glfwInit()) {
            throw new RuntimeException("Failed to initialize GLFW for testing");
        }
        System.out.println("GLFW initialized for Window tests");
    }

    @AfterAll
    public static void teardownGLFW() {
        glfwTerminate();
        System.out.println("GLFW terminated after Window tests");
    }

    @Test
    @Order(1)
    @DisplayName("Test window creation")
    public void testWindowCreation() {
        Window window = new Window(800, 600, "Test Window");
        window.init();

        assertNotEquals(0, window.getHandle(), "Window handle should not be null");
        assertEquals(800, window.getWidth(), "Window width should match");
        assertEquals(600, window.getHeight(), "Window height should match");

        window.destroy();
    }

    @Test
    @Order(2)
    @DisplayName("Test window should close")
    public void testWindowShouldClose() {
        Window window = new Window(800, 600, "Test Window");
        window.init();

        assertFalse(window.shouldClose(), "Window should not be marked for closing initially");

        glfwSetWindowShouldClose(window.getHandle(), true);
        assertTrue(window.shouldClose(), "Window should be marked for closing after setting flag");

        window.destroy();
    }

    @Test
    @Order(3)
    @DisplayName("Test window destroy")
    public void testWindowDestroy() {
        Window window = new Window(800, 600, "Test Window");
        window.init();

        assertDoesNotThrow(() -> {
            window.destroy();
        }, "Window destroy should not throw exceptions");
    }

    @Test
    @Order(4)
    @DisplayName("Test window swap buffers")
    public void testWindowSwapBuffers() {
        Window window = new Window(800, 600, "Test Window");
        window.init();

        assertDoesNotThrow(() -> {
            window.swapBuffers();
        }, "Swap buffers should not throw exceptions");

        window.destroy();
    }

    @Test
    @Order(5)
    @DisplayName("Test window getters")
    public void testWindowGetters() {
        int width = 1024;
        int height = 768;
        String title = "Getter Test Window";

        Window window = new Window(width, height, title);
        window.init();

        assertEquals(width, window.getWidth(), "Width getter should return correct value");
        assertEquals(height, window.getHeight(), "Height getter should return correct value");
        assertTrue(window.getHandle() > 0, "Handle should be a valid positive value");

        window.destroy();
    }
}
