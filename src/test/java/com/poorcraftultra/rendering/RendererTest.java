package com.poorcraftultra.rendering;

import org.junit.jupiter.api.*;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Renderer class.
 * Requires OpenGL context for testing.
 */
@Tag("integration")
public class RendererTest {

    private static long window;

    @BeforeAll
    public static void initGLFW() {
        assertTrue(GLFW.glfwInit(), "Failed to initialize GLFW");

        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        window = GLFW.glfwCreateWindow(800, 600, "Test", 0, 0);
        assertNotEquals(0, window, "Failed to create GLFW window");

        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();
    }

    @AfterAll
    public static void cleanupGLFW() {
        if (window != 0) {
            GLFW.glfwDestroyWindow(window);
        }
        GLFW.glfwTerminate();
    }

    @Test
    public void testRendererInit() {
        Renderer renderer = new Renderer();
        assertDoesNotThrow(() -> renderer.init());

        // Assert depth testing is enabled
        assertTrue(GL11.glIsEnabled(GL11.GL_DEPTH_TEST), "Depth testing should be enabled");

        // Assert clear color
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(4);
            GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, buffer);
            assertEquals(0.1f, buffer.get(0), 0.01f, "Clear color red should be 0.1");
            assertEquals(0.1f, buffer.get(1), 0.01f, "Clear color green should be 0.1");
            assertEquals(0.1f, buffer.get(2), 0.01f, "Clear color blue should be 0.1");
            assertEquals(1.0f, buffer.get(3), 0.01f, "Clear color alpha should be 1.0");
        }

        // Assert cube mesh index count
        assertEquals(36, renderer.getCubeIndexCount(), "Cube mesh should have 36 indices");

        renderer.cleanup();
    }

    @Test
    public void testCubeMeshCreation() {
        Renderer renderer = new Renderer();
        renderer.init();

        // Access private field via reflection or assume it's created
        // Since we can't access private fields easily, we test via render call
        Camera camera = new Camera();
        assertDoesNotThrow(() -> renderer.render(camera, 800f / 600f));

        renderer.cleanup();
    }

    @Test
    public void testRenderCall() {
        Renderer renderer = new Renderer();
        renderer.init();

        Camera camera = new Camera();
        assertDoesNotThrow(() -> renderer.render(camera, 800f / 600f));

        renderer.cleanup();
    }

    @Test
    public void testCleanup() {
        Renderer renderer = new Renderer();
        renderer.init();

        assertDoesNotThrow(() -> renderer.cleanup());
    }
}
