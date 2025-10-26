package com.poorcraftultra.rendering;

import org.junit.jupiter.api.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ShaderProgram class.
 * Requires OpenGL context for testing.
 */
@Tag("integration")
public class ShaderProgramTest {

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
    public void testShaderCompilation() {
        ShaderProgram shader = new ShaderProgram("/shaders/vertex.glsl", "/shaders/fragment.glsl");
        assertNotNull(shader, "Shader program should be created successfully");
        shader.cleanup();
    }

    @Test
    public void testShaderLinking() {
        ShaderProgram shader = new ShaderProgram("/shaders/vertex.glsl", "/shaders/fragment.glsl");
        // If we reach this point without exception, linking succeeded
        assertNotNull(shader);
        shader.cleanup();
    }

    @Test
    public void testUniformSetting() {
        ShaderProgram shader = new ShaderProgram("/shaders/vertex.glsl", "/shaders/fragment.glsl");

        shader.bind();

        // Test matrix uniform
        Matrix4f matrix = new Matrix4f().identity();
        assertDoesNotThrow(() -> shader.setUniform("uProjection", matrix));

        // Test vector uniform
        Vector3f vector = new Vector3f(1.0f, 2.0f, 3.0f);
        assertDoesNotThrow(() -> shader.setUniform("uView", vector));

        // Test float uniform
        assertDoesNotThrow(() -> shader.setUniform("testFloat", 1.5f));

        // Test int uniform
        assertDoesNotThrow(() -> shader.setUniform("testInt", 42));

        shader.unbind();

        shader.cleanup();
    }

    @Test
    public void testBindUnbind() {
        ShaderProgram shader = new ShaderProgram("/shaders/vertex.glsl", "/shaders/fragment.glsl");

        assertDoesNotThrow(() -> shader.bind());
        assertDoesNotThrow(() -> shader.unbind());

        shader.cleanup();
    }

    @Test
    public void testInvalidShaderCompilation() {
        assertThrows(RuntimeException.class, () ->
            new ShaderProgram("/shaders/bad_vertex.glsl", "/shaders/fragment.glsl"));
    }

    @Test
    public void testInvalidFragmentShaderCompilation() {
        assertThrows(RuntimeException.class, () ->
            new ShaderProgram("/shaders/vertex.glsl", "/shaders/bad_fragment.glsl"));
    }

    @Test
    public void testIncompatibleShaderLinking() {
        assertThrows(RuntimeException.class, () ->
            new ShaderProgram("/shaders/vertex.glsl", "/shaders/incompatible_fragment.glsl"));
    }

    @Test
    public void testUniformCacheBehavior() {
        ShaderProgram shader = new ShaderProgram("/shaders/vertex.glsl", "/shaders/fragment.glsl");

        shader.bind();

        // Set the same uniform multiple times
        for (int i = 0; i < 5; i++) {
            shader.setUniform("testFloat", 1.5f);
        }

        // Cache should only have one entry
        assertEquals(1, shader.getUniformCacheSize());

        shader.unbind();
        shader.cleanup();
    }
}
