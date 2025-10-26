package com.poorcraftultra.rendering;

import org.junit.jupiter.api.*;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Mesh class.
 * Requires OpenGL context for testing.
 */
@Tag("integration")
public class MeshTest {

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
    public void testMeshCreation() {
        float[] vertices = {
            -0.5f, -0.5f, 0.0f, 1.0f, 0.0f, 0.0f,
             0.5f, -0.5f, 0.0f, 0.0f, 1.0f, 0.0f,
             0.0f,  0.5f, 0.0f, 0.0f, 0.0f, 1.0f
        };
        int[] indices = {0, 1, 2};

        Mesh mesh = new Mesh(vertices, indices);
        assertNotNull(mesh, "Mesh should be created successfully");
        assertEquals(3, mesh.getVertexCount(), "Vertex count should match indices length");

        mesh.cleanup();
    }

    @Test
    public void testBindUnbind() {
        float[] vertices = {
            -0.5f, -0.5f, 0.0f, 1.0f, 0.0f, 0.0f,
             0.5f, -0.5f, 0.0f, 0.0f, 1.0f, 0.0f,
             0.0f,  0.5f, 0.0f, 0.0f, 0.0f, 1.0f
        };
        int[] indices = {0, 1, 2};

        Mesh mesh = new Mesh(vertices, indices);

        assertDoesNotThrow(() -> mesh.bind());
        assertDoesNotThrow(() -> mesh.unbind());

        mesh.cleanup();
    }

    @Test
    public void testCleanup() {
        float[] vertices = {
            -0.5f, -0.5f, 0.0f, 1.0f, 0.0f, 0.0f,
             0.5f, -0.5f, 0.0f, 0.0f, 1.0f, 0.0f,
             0.0f,  0.5f, 0.0f, 0.0f, 0.0f, 1.0f
        };
        int[] indices = {0, 1, 2};

        Mesh mesh = new Mesh(vertices, indices);
        assertDoesNotThrow(() -> mesh.cleanup());
    }
}
