package com.poorcraftultra.core;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

/**
 * Unit tests for the Shader class.
 */
@ExtendWith(GLTestContext.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ShaderTest {

    @Test
    @Order(1)
    @DisplayName("Test shader compilation with valid shaders")
    public void testShaderCompilation() {
        Shader shader = new Shader("shaders/vertex.glsl", "shaders/fragment.glsl");

        assertDoesNotThrow(() -> {
            shader.load();
        }, "Shader loading should not throw exceptions with valid shaders");

        assertTrue(shader.getProgramId() > 0, "Shader program ID should be valid");

        int error = glGetError();
        assertEquals(GL_NO_ERROR, error, "There should be no OpenGL errors after shader compilation");

        shader.cleanup();
    }

    @Test
    @Order(2)
    @DisplayName("Test shader compilation failure with invalid shader")
    public void testShaderCompilationFailure() {
        // Create a shader with invalid GLSL source path
        Shader shader = new Shader("invalid/path/vertex.glsl", "invalid/path/fragment.glsl");

        assertThrows(RuntimeException.class, () -> {
            shader.load();
        }, "Shader loading should throw exception with invalid shader paths");
    }

    @Test
    @Order(3)
    @DisplayName("Test shader binding")
    public void testShaderBinding() {
        Shader shader = new Shader("shaders/vertex.glsl", "shaders/fragment.glsl");
        shader.load();

        assertDoesNotThrow(() -> {
            shader.bind();
        }, "Shader bind should not throw exceptions");

        int currentProgram = glGetInteger(GL_CURRENT_PROGRAM);
        assertEquals(shader.getProgramId(), currentProgram, "Current program should match shader program ID");

        assertDoesNotThrow(() -> {
            shader.unbind();
        }, "Shader unbind should not throw exceptions");

        currentProgram = glGetInteger(GL_CURRENT_PROGRAM);
        assertEquals(0, currentProgram, "Current program should be 0 after unbind");

        shader.cleanup();
    }

    @Test
    @Order(4)
    @DisplayName("Test uniform location")
    public void testUniformLocation() {
        Shader shader = new Shader("shaders/vertex.glsl", "shaders/fragment.glsl");
        shader.load();

        int modelLocation = shader.getUniformLocation("model");
        int viewLocation = shader.getUniformLocation("view");
        int projectionLocation = shader.getUniformLocation("projection");

        assertTrue(modelLocation >= 0, "Model uniform location should be valid");
        assertTrue(viewLocation >= 0, "View uniform location should be valid");
        assertTrue(projectionLocation >= 0, "Projection uniform location should be valid");

        int invalidLocation = shader.getUniformLocation("nonExistentUniform");
        assertEquals(-1, invalidLocation, "Non-existent uniform should return -1");

        shader.cleanup();
    }

    @Test
    @Order(5)
    @DisplayName("Test shader cleanup")
    public void testShaderCleanup() {
        Shader shader = new Shader("shaders/vertex.glsl", "shaders/fragment.glsl");
        shader.load();

        int programId = shader.getProgramId();
        assertTrue(programId > 0, "Program ID should be valid before cleanup");

        assertDoesNotThrow(() -> {
            shader.cleanup();
        }, "Shader cleanup should not throw exceptions");

        // After cleanup, the program should be deleted
        assertFalse(glIsProgram(programId), "Program should be deleted after cleanup");
    }
}
