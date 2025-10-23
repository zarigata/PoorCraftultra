package com.poorcraftultra.world.chunk;

import com.poorcraftultra.core.GLTestContext;
import com.poorcraftultra.core.Shader;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Tests for ChunkRenderer class (requires OpenGL context).
 */
@DisplayName("ChunkRenderer Tests")
@ExtendWith(GLTestContext.class)
public class ChunkRendererTest {
    private ChunkManager chunkManager;
    private Shader shader;
    private ChunkRenderer chunkRenderer;

    @BeforeEach
    void setUp() {
        chunkManager = new ChunkManager();
        shader = new Shader("shaders/vertex.glsl", "shaders/fragment.glsl");
        shader.load();
        chunkRenderer = new ChunkRenderer(chunkManager, shader);
    }

    @AfterEach
    void tearDown() {
        if (chunkRenderer != null) {
            chunkRenderer.cleanup();
        }
        if (shader != null) {
            shader.cleanup();
        }
    }

    @Test
    @DisplayName("Renderer initialization")
    void testRendererInitialization() {
        assertDoesNotThrow(() -> chunkRenderer.init());

        int error = glGetError();
        assertEquals(GL_NO_ERROR, error, "OpenGL error after init: " + error);
    }

    @Test
    @DisplayName("Render empty world")
    void testRenderEmptyWorld() {
        chunkRenderer.init();

        Matrix4f view = createTestView();
        Matrix4f projection = createTestProjection();

        assertDoesNotThrow(() -> chunkRenderer.render(view, projection));
        assertEquals(0, chunkRenderer.getRenderedChunkCount());

        int error = glGetError();
        assertEquals(GL_NO_ERROR, error, "OpenGL error after render: " + error);
    }

    @Test
    @DisplayName("Render single chunk")
    void testRenderSingleChunk() {
        chunkRenderer.init();

        ChunkPos pos = new ChunkPos(0, 0, 0);
        Chunk chunk = chunkManager.loadChunk(pos);
        chunk.setBlock(0, 0, 0, (byte) 1);

        Matrix4f view = createTestView();
        Matrix4f projection = createTestProjection();

        chunkRenderer.render(view, projection);

        assertEquals(1, chunkRenderer.getRenderedChunkCount());

        int error = glGetError();
        assertEquals(GL_NO_ERROR, error, "OpenGL error after render: " + error);
    }

    @Test
    @DisplayName("Render multiple chunks")
    void testRenderMultipleChunks() {
        chunkRenderer.init();

        // Load 10 chunks with blocks
        for (int i = 0; i < 10; i++) {
            ChunkPos pos = new ChunkPos(i, 0, 0);
            Chunk chunk = chunkManager.loadChunk(pos);
            chunk.setBlock(0, 0, 0, (byte) 1);
        }

        Matrix4f view = createTestView();
        Matrix4f projection = createTestProjection();

        chunkRenderer.render(view, projection);

        assertTrue(chunkRenderer.getRenderedChunkCount() > 0);

        int error = glGetError();
        assertEquals(GL_NO_ERROR, error, "OpenGL error after render: " + error);
    }

    @Test
    @DisplayName("Mesh caching")
    void testMeshCaching() {
        chunkRenderer.init();

        ChunkPos pos = new ChunkPos(0, 0, 0);
        Chunk chunk = chunkManager.loadChunk(pos);
        chunk.setBlock(0, 0, 0, (byte) 1);

        Matrix4f view = createTestView();
        Matrix4f projection = createTestProjection();

        // First render - mesh should be generated
        chunkRenderer.render(view, projection);
        int firstRenderCount = chunkRenderer.getRenderedChunkCount();

        // Second render - mesh should be cached
        chunkRenderer.render(view, projection);
        int secondRenderCount = chunkRenderer.getRenderedChunkCount();

        assertEquals(firstRenderCount, secondRenderCount);

        int error = glGetError();
        assertEquals(GL_NO_ERROR, error, "OpenGL error: " + error);
    }

    @Test
    @DisplayName("Frustum culling")
    void testFrustumCulling() {
        chunkRenderer.init();

        // Load chunks both near and far
        for (int i = -5; i <= 5; i++) {
            for (int j = -5; j <= 5; j++) {
                ChunkPos pos = new ChunkPos(i, 0, j);
                Chunk chunk = chunkManager.loadChunk(pos);
                chunk.setBlock(0, 0, 0, (byte) 1);
            }
        }

        // Camera looking at origin from distance
        Matrix4f view = new Matrix4f().lookAt(
            new Vector3f(0, 50, 50),
            new Vector3f(0, 0, 0),
            new Vector3f(0, 1, 0)
        );
        Matrix4f projection = createTestProjection();

        chunkRenderer.render(view, projection);

        // Some chunks should be culled
        int totalChunks = chunkManager.getLoadedChunkCount();
        int renderedChunks = chunkRenderer.getRenderedChunkCount();
        int culledChunks = chunkRenderer.getCulledChunkCount();

        assertTrue(culledChunks >= 0, "Should have culling statistics");
        assertEquals(totalChunks, renderedChunks + culledChunks, 
                    "Rendered + culled should equal total");

        int error = glGetError();
        assertEquals(GL_NO_ERROR, error, "OpenGL error: " + error);
    }

    @Test
    @DisplayName("Mesh invalidation")
    void testMeshInvalidation() {
        chunkRenderer.init();

        ChunkPos pos = new ChunkPos(0, 0, 0);
        Chunk chunk = chunkManager.loadChunk(pos);
        chunk.setBlock(0, 0, 0, (byte) 1);

        Matrix4f view = createTestView();
        Matrix4f projection = createTestProjection();

        // Render to cache mesh
        chunkRenderer.render(view, projection);

        // Invalidate mesh
        chunkRenderer.invalidateMesh(pos);

        // Render again - should regenerate mesh
        assertDoesNotThrow(() -> chunkRenderer.render(view, projection));

        int error = glGetError();
        assertEquals(GL_NO_ERROR, error, "OpenGL error: " + error);
    }

    @Test
    @DisplayName("Cleanup")
    void testCleanup() {
        chunkRenderer.init();

        ChunkPos pos = new ChunkPos(0, 0, 0);
        Chunk chunk = chunkManager.loadChunk(pos);
        chunk.setBlock(0, 0, 0, (byte) 1);

        Matrix4f view = createTestView();
        Matrix4f projection = createTestProjection();

        chunkRenderer.render(view, projection);
        chunkRenderer.cleanup();

        int error = glGetError();
        assertEquals(GL_NO_ERROR, error, "OpenGL error after cleanup: " + error);
    }

    @Test
    @DisplayName("Statistics accuracy")
    void testStatistics() {
        chunkRenderer.init();

        // Load 5 chunks
        for (int i = 0; i < 5; i++) {
            ChunkPos pos = new ChunkPos(i, 0, 0);
            Chunk chunk = chunkManager.loadChunk(pos);
            chunk.setBlock(0, 0, 0, (byte) 1);
        }

        Matrix4f view = createTestView();
        Matrix4f projection = createTestProjection();

        chunkRenderer.render(view, projection);

        int rendered = chunkRenderer.getRenderedChunkCount();
        int culled = chunkRenderer.getCulledChunkCount();

        assertTrue(rendered >= 0, "Rendered count should be non-negative");
        assertTrue(culled >= 0, "Culled count should be non-negative");
        assertEquals(5, rendered + culled, "Total should equal loaded chunks");
    }

    private Matrix4f createTestView() {
        return new Matrix4f().lookAt(
            new Vector3f(0, 10, 20),
            new Vector3f(0, 0, 0),
            new Vector3f(0, 1, 0)
        );
    }

    private Matrix4f createTestProjection() {
        return new Matrix4f().perspective(
            (float) Math.toRadians(45.0f),
            1.0f,
            0.1f,
            1000.0f
        );
    }
}
