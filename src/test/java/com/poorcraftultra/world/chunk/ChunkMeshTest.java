package com.poorcraftultra.world.chunk;

import com.poorcraftultra.core.GLTestContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Tests for ChunkMesh class (requires OpenGL context).
 */
@DisplayName("ChunkMesh Tests")
@ExtendWith(GLTestContext.class)
public class ChunkMeshTest {

    @Test
    @DisplayName("Mesh creation with sample data")
    void testMeshCreation() {
        // Vertex format: position (XYZ) + color (RGB) + texCoord (UV) = 8 floats
        float[] vertices = {
            0, 0, 0, 1, 0, 0, 0, 0,
            1, 0, 0, 1, 0, 0, 1, 0,
            1, 1, 0, 1, 0, 0, 1, 1,
            0, 1, 0, 1, 0, 0, 0, 1
        };
        int[] indices = {0, 1, 2, 2, 3, 0};

        ChunkMesh mesh = new ChunkMesh(vertices, indices);

        assertEquals(4, mesh.getVertexCount());
        assertEquals(6, mesh.getIndexCount());
        assertFalse(mesh.isEmpty());
    }

    @Test
    @DisplayName("Empty mesh creation")
    void testEmptyMesh() {
        float[] vertices = {};
        int[] indices = {};

        ChunkMesh mesh = new ChunkMesh(vertices, indices);

        assertEquals(0, mesh.getVertexCount());
        assertEquals(0, mesh.getIndexCount());
        assertTrue(mesh.isEmpty());
    }

    @Test
    @DisplayName("Mesh upload to GPU")
    void testMeshUpload() {
        float[] vertices = {
            0, 0, 0, 1, 0, 0, 0, 0,
            1, 0, 0, 1, 0, 0, 1, 0,
            1, 1, 0, 1, 0, 0, 1, 1
        };
        int[] indices = {0, 1, 2};

        ChunkMesh mesh = new ChunkMesh(vertices, indices);
        mesh.upload();

        // Verify no OpenGL errors
        int error = glGetError();
        assertEquals(GL_NO_ERROR, error, "OpenGL error after upload: " + error);

        mesh.cleanup();
    }

    @Test
    @DisplayName("Mesh rendering")
    void testMeshRender() {
        float[] vertices = {
            0, 0, 0, 1, 0, 0, 0, 0,
            1, 0, 0, 1, 0, 0, 1, 0,
            1, 1, 0, 1, 0, 0, 1, 1
        };
        int[] indices = {0, 1, 2};

        ChunkMesh mesh = new ChunkMesh(vertices, indices);
        mesh.upload();
        mesh.render();

        // Verify no OpenGL errors
        int error = glGetError();
        assertEquals(GL_NO_ERROR, error, "OpenGL error after render: " + error);

        mesh.cleanup();
    }

    @Test
    @DisplayName("Mesh cleanup")
    void testMeshCleanup() {
        float[] vertices = {
            0, 0, 0, 1, 0, 0, 0, 0,
            1, 0, 0, 1, 0, 0, 1, 0,
            1, 1, 0, 1, 0, 0, 1, 1
        };
        int[] indices = {0, 1, 2};

        ChunkMesh mesh = new ChunkMesh(vertices, indices);
        mesh.upload();
        mesh.cleanup();

        // Verify no OpenGL errors
        int error = glGetError();
        assertEquals(GL_NO_ERROR, error, "OpenGL error after cleanup: " + error);
    }

    @Test
    @DisplayName("Double upload handling")
    void testDoubleUpload() {
        float[] vertices = {
            0, 0, 0, 1, 0, 0, 0, 0,
            1, 0, 0, 1, 0, 0, 1, 0,
            1, 1, 0, 1, 0, 0, 1, 1
        };
        int[] indices = {0, 1, 2};

        ChunkMesh mesh = new ChunkMesh(vertices, indices);
        mesh.upload();
        mesh.upload(); // Should cleanup old buffers first

        // Verify no OpenGL errors
        int error = glGetError();
        assertEquals(GL_NO_ERROR, error, "OpenGL error after double upload: " + error);

        mesh.cleanup();
    }
}
