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
 * Performance tests for chunk rendering (requires OpenGL context).
 */
@DisplayName("ChunkRender Performance Tests")
@Tag("performance")
@ExtendWith(GLTestContext.class)
public class ChunkRenderPerformanceTest {
    private ChunkManager chunkManager;
    private Shader shader;
    private ChunkRenderer chunkRenderer;

    @BeforeEach
    void setUp() {
        chunkManager = new ChunkManager();
        shader = new Shader("shaders/vertex.glsl", "shaders/fragment.glsl");
        shader.load();
        chunkRenderer = new ChunkRenderer(chunkManager, shader);
        chunkRenderer.init();
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
    @DisplayName("Render performance with 16 chunk distance")
    @Timeout(30)
    void testRenderPerformance16ChunkDistance() {
        // Load chunks in a 16 chunk radius (simplified to a grid for testing)
        int radius = 8; // 16 chunk diameter
        for (int cx = -radius; cx <= radius; cx++) {
            for (int cz = -radius; cz <= radius; cz++) {
                // Only load chunks within circular distance
                if (cx * cx + cz * cz <= radius * radius) {
                    ChunkPos pos = new ChunkPos(cx, 0, cz);
                    Chunk chunk = chunkManager.loadChunk(pos);

                    // Fill with realistic terrain
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            for (int y = 0; y < 32; y++) {
                                if (y < 16 || (x + z + y) % 3 == 0) {
                                    chunk.setBlock(x, y, z, (byte) 1);
                                }
                            }
                        }
                    }
                }
            }
        }

        System.out.println("Loaded chunks: " + chunkManager.getLoadedChunkCount());

        Matrix4f view = new Matrix4f().lookAt(
            new Vector3f(0, 50, 50),
            new Vector3f(0, 0, 0),
            new Vector3f(0, 1, 0)
        );
        Matrix4f projection = new Matrix4f().perspective(
            (float) Math.toRadians(45.0f),
            16.0f / 9.0f,
            0.1f,
            1000.0f
        );

        // Warm up
        for (int i = 0; i < 10; i++) {
            chunkRenderer.render(view, projection);
        }

        // Measure performance
        long startTime = System.nanoTime();
        int frames = 100;

        for (int i = 0; i < frames; i++) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            chunkRenderer.render(view, projection);
        }

        long endTime = System.nanoTime();
        double averageFrameTimeMs = ((endTime - startTime) / frames) / 1_000_000.0;
        double fps = 1000.0 / averageFrameTimeMs;

        System.out.println("Average frame time: " + averageFrameTimeMs + " ms");
        System.out.println("FPS: " + fps);
        System.out.println("Rendered chunks: " + chunkRenderer.getRenderedChunkCount());
        System.out.println("Culled chunks: " + chunkRenderer.getCulledChunkCount());

        // Target: 60 FPS = 16.67ms per frame
        assertTrue(averageFrameTimeMs < 16.67, 
                  "Should achieve 60 FPS, frame time: " + averageFrameTimeMs + "ms");

        int error = glGetError();
        assertEquals(GL_NO_ERROR, error, "OpenGL error: " + error);
    }

    @Test
    @DisplayName("Frustum culling effectiveness")
    @Timeout(10)
    void testFrustumCullingEffectiveness() {
        // Load chunks in all directions
        for (int cx = -10; cx <= 10; cx++) {
            for (int cz = -10; cz <= 10; cz++) {
                ChunkPos pos = new ChunkPos(cx, 0, cz);
                Chunk chunk = chunkManager.loadChunk(pos);
                chunk.setBlock(0, 0, 0, (byte) 1);
            }
        }

        // Camera looking in one direction
        Matrix4f view = new Matrix4f().lookAt(
            new Vector3f(0, 50, 0),
            new Vector3f(100, 0, 0),
            new Vector3f(0, 1, 0)
        );
        Matrix4f projection = new Matrix4f().perspective(
            (float) Math.toRadians(45.0f),
            1.0f,
            0.1f,
            1000.0f
        );

        chunkRenderer.render(view, projection);

        int totalChunks = chunkManager.getLoadedChunkCount();
        int renderedChunks = chunkRenderer.getRenderedChunkCount();
        int culledChunks = chunkRenderer.getCulledChunkCount();

        double cullingRatio = (double) culledChunks / totalChunks;

        System.out.println("Total chunks: " + totalChunks);
        System.out.println("Rendered chunks: " + renderedChunks);
        System.out.println("Culled chunks: " + culledChunks);
        System.out.println("Culling ratio: " + (cullingRatio * 100) + "%");

        // At least 50% should be culled with directional camera
        assertTrue(cullingRatio >= 0.5, 
                  "At least 50% of chunks should be culled, ratio: " + (cullingRatio * 100) + "%");

        int error = glGetError();
        assertEquals(GL_NO_ERROR, error, "OpenGL error: " + error);
    }

    @Test
    @DisplayName("Mesh cache effectiveness")
    @Timeout(10)
    void testMeshCacheEffectiveness() {
        // Load chunks
        for (int i = 0; i < 25; i++) {
            ChunkPos pos = new ChunkPos(i % 5, 0, i / 5);
            Chunk chunk = chunkManager.loadChunk(pos);

            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < 16; y++) {
                        chunk.setBlock(x, y, z, (byte) 1);
                    }
                }
            }
        }

        Matrix4f view = new Matrix4f().lookAt(
            new Vector3f(40, 30, 40),
            new Vector3f(40, 0, 40),
            new Vector3f(0, 1, 0)
        );
        Matrix4f projection = new Matrix4f().perspective(
            (float) Math.toRadians(45.0f),
            1.0f,
            0.1f,
            1000.0f
        );

        // First render - meshes generated
        long startTime1 = System.nanoTime();
        chunkRenderer.render(view, projection);
        long endTime1 = System.nanoTime();
        double firstRenderTime = (endTime1 - startTime1) / 1_000_000.0;

        // Subsequent renders - meshes cached
        long startTime2 = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            chunkRenderer.render(view, projection);
        }
        long endTime2 = System.nanoTime();
        double averageCachedTime = ((endTime2 - startTime2) / 100) / 1_000_000.0;

        System.out.println("First render (with meshing): " + firstRenderTime + " ms");
        System.out.println("Average cached render: " + averageCachedTime + " ms");

        // Cached rendering should be significantly faster
        double speedup = firstRenderTime / averageCachedTime;
        System.out.println("Speedup: " + speedup + "x");

        assertTrue(speedup >= 10.0, 
                  "Cached rendering should be at least 10x faster, speedup: " + speedup + "x");

        int error = glGetError();
        assertEquals(GL_NO_ERROR, error, "OpenGL error: " + error);
    }

    @Test
    @DisplayName("Scalability test")
    @Timeout(30)
    void testScalability() {
        int[] chunkCounts = {10, 50, 100};
        Matrix4f view = new Matrix4f().lookAt(
            new Vector3f(0, 50, 50),
            new Vector3f(0, 0, 0),
            new Vector3f(0, 1, 0)
        );
        Matrix4f projection = new Matrix4f().perspective(
            (float) Math.toRadians(45.0f),
            1.0f,
            0.1f,
            1000.0f
        );

        System.out.println("\nScalability Test:");
        System.out.println("Chunks | Frame Time (ms) | FPS");
        System.out.println("-------|-----------------|-----");

        for (int targetCount : chunkCounts) {
            // Clear previous chunks
            chunkManager.unloadAllChunks();
            chunkRenderer.invalidateAllMeshes();

            // Load chunks
            int loaded = 0;
            for (int cx = 0; cx < 20 && loaded < targetCount; cx++) {
                for (int cz = 0; cz < 20 && loaded < targetCount; cz++) {
                    ChunkPos pos = new ChunkPos(cx, 0, cz);
                    Chunk chunk = chunkManager.loadChunk(pos);
                    chunk.setBlock(0, 0, 0, (byte) 1);
                    loaded++;
                }
            }

            // Warm up
            for (int i = 0; i < 5; i++) {
                chunkRenderer.render(view, projection);
            }

            // Measure
            long startTime = System.nanoTime();
            int frames = 50;
            for (int i = 0; i < frames; i++) {
                chunkRenderer.render(view, projection);
            }
            long endTime = System.nanoTime();

            double avgFrameTime = ((endTime - startTime) / frames) / 1_000_000.0;
            double fps = 1000.0 / avgFrameTime;

            System.out.printf("%6d | %15.2f | %4.1f%n", loaded, avgFrameTime, fps);
        }

        int error = glGetError();
        assertEquals(GL_NO_ERROR, error, "OpenGL error: " + error);
    }
}
