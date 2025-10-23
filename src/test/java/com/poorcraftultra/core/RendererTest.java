package com.poorcraftultra.core;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Unit tests for the Renderer class.
 */
@ExtendWith(GLTestContext.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RendererTest {

    @Test
    @Order(1)
    @DisplayName("Test renderer initialization")
    public void testRendererInitialization() {
        Renderer renderer = new Renderer();

        assertDoesNotThrow(() -> {
            renderer.init();
        }, "Renderer initialization should not throw exceptions");

        assertTrue(renderer.getVaoId() > 0, "VAO ID should be valid");
        assertTrue(renderer.getVboId() > 0, "VBO ID should be valid");
        assertTrue(renderer.getEboId() > 0, "EBO ID should be valid");
        assertNotNull(renderer.getShader(), "Shader should be initialized");
        assertTrue(renderer.getShader().getProgramId() > 0, "Shader program should be loaded");

        int error = glGetError();
        assertEquals(GL_NO_ERROR, error, "There should be no OpenGL errors after initialization");

        renderer.cleanup();
    }

    @Test
    @Order(2)
    @DisplayName("Test renderer cleanup")
    public void testRendererCleanup() {
        Renderer renderer = new Renderer();
        renderer.init();

        int vaoId = renderer.getVaoId();
        int vboId = renderer.getVboId();
        int eboId = renderer.getEboId();

        assertTrue(glIsVertexArray(vaoId), "VAO should exist before cleanup");
        assertTrue(glIsBuffer(vboId), "VBO should exist before cleanup");
        assertTrue(glIsBuffer(eboId), "EBO should exist before cleanup");

        assertDoesNotThrow(() -> {
            renderer.cleanup();
        }, "Renderer cleanup should not throw exceptions");

        int error = glGetError();
        assertEquals(GL_NO_ERROR, error, "There should be no OpenGL errors after cleanup");

        // After cleanup, buffers should be deleted
        assertFalse(glIsVertexArray(vaoId), "VAO should be deleted after cleanup");
        assertFalse(glIsBuffer(vboId), "VBO should be deleted after cleanup");
        assertFalse(glIsBuffer(eboId), "EBO should be deleted after cleanup");
    }

    @Test
    @Order(3)
    @DisplayName("Test render without errors")
    public void testRenderWithoutErrors() {
        Renderer renderer = new Renderer();
        renderer.init();

        assertDoesNotThrow(() -> {
            renderer.render(800, 600);
        }, "Render method should not throw exceptions");

        int error = glGetError();
        assertEquals(GL_NO_ERROR, error, "There should be no OpenGL errors after rendering");

        renderer.cleanup();
    }

    @Test
    @Order(4)
    @DisplayName("Test multiple render calls")
    public void testMultipleRenderCalls() {
        Renderer renderer = new Renderer();
        renderer.init();

        assertDoesNotThrow(() -> {
            for (int i = 0; i < 10; i++) {
                renderer.render(800, 600);
            }
        }, "Multiple render calls should not throw exceptions");

        int error = glGetError();
        assertEquals(GL_NO_ERROR, error, "There should be no OpenGL errors after multiple renders");

        renderer.cleanup();
    }

    @Test
    @Order(5)
    @DisplayName("Test render with different aspect ratios")
    public void testRenderWithDifferentAspectRatios() {
        Renderer renderer = new Renderer();
        renderer.init();

        assertDoesNotThrow(() -> {
            renderer.render(1920, 1080); // 16:9
            renderer.render(1280, 720);  // 16:9
            renderer.render(800, 600);   // 4:3
            renderer.render(1024, 768);  // 4:3
        }, "Render with different aspect ratios should not throw exceptions");

        int error = glGetError();
        assertEquals(GL_NO_ERROR, error, "There should be no OpenGL errors");

        renderer.cleanup();
    }

    @Test
    @Order(6)
    @DisplayName("Test depth testing is enabled")
    public void testDepthTestingEnabled() {
        Renderer renderer = new Renderer();
        renderer.init();

        boolean depthTestEnabled = glIsEnabled(GL_DEPTH_TEST);
        assertTrue(depthTestEnabled, "Depth testing should be enabled after renderer init");

        renderer.cleanup();
    }
}
