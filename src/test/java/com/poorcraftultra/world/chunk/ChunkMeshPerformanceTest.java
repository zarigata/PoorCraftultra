package com.poorcraftultra.world.chunk;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for chunk meshing (no OpenGL needed).
 */
@DisplayName("ChunkMesh Performance Tests")
@Tag("performance")
public class ChunkMeshPerformanceTest {
    private ChunkManager chunkManager;
    private ChunkMesher mesher;

    @BeforeEach
    void setUp() {
        chunkManager = new ChunkManager();
        mesher = new ChunkMesher(chunkManager);
    }

    @Test
    @DisplayName("Mesh generation performance")
    @Timeout(10)
    void testMeshGenerationPerformance() {
        // Create a fully populated chunk
        ChunkPos pos = new ChunkPos(0, 0, 0);
        Chunk chunk = chunkManager.loadChunk(pos);

        // Fill all sections with a realistic pattern
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 128; y++) {
                    // Create layers with some variation
                    if (y < 64 || (y < 96 && (x + z) % 2 == 0)) {
                        chunk.setBlock(x, y, z, (byte) 1);
                    }
                }
            }
        }

        // Warm up JVM
        for (int i = 0; i < 10; i++) {
            mesher.generateMesh(chunk);
        }

        // Measure performance
        long startTime = System.nanoTime();
        int iterations = 100;

        for (int i = 0; i < iterations; i++) {
            mesher.generateMesh(chunk);
        }

        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        double averageTimeMs = (totalTime / iterations) / 1_000_000.0;

        System.out.println("Average mesh generation time: " + averageTimeMs + " ms");

        // Assert performance target: < 5ms per mesh
        assertTrue(averageTimeMs < 5.0, 
                  "Mesh generation should take less than 5ms, took: " + averageTimeMs + "ms");
    }

    @Test
    @DisplayName("Greedy meshing efficiency")
    @Timeout(10)
    void testGreedyMeshingEfficiency() {
        ChunkPos pos = new ChunkPos(0, 0, 0);
        Chunk chunk = chunkManager.loadChunk(pos);

        // Create large flat areas (best case for greedy meshing)
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 32; y++) {
                    chunk.setBlock(x, y, z, (byte) 1);
                }
            }
        }

        ChunkMesh mesh = mesher.generateMesh(chunk);

        // Calculate vertex count
        int vertexCount = mesh.getVertexCount();

        // Naive meshing would create 6 faces per block * 4 vertices per face
        // = 24 vertices per block * (16*16*32) blocks = 196,608 vertices
        int naiveVertexCount = 24 * 16 * 16 * 32;

        // Greedy meshing should reduce by at least 90%
        double reduction = 1.0 - ((double) vertexCount / naiveVertexCount);
        System.out.println("Vertex reduction: " + (reduction * 100) + "%");
        System.out.println("Greedy vertices: " + vertexCount + ", Naive: " + naiveVertexCount);

        assertTrue(reduction >= 0.90, 
                  "Greedy meshing should reduce vertices by at least 90%, reduction: " + (reduction * 100) + "%");
    }

    @Test
    @DisplayName("Worst case performance")
    @Timeout(10)
    void testWorstCasePerformance() {
        ChunkPos pos = new ChunkPos(0, 0, 0);
        Chunk chunk = chunkManager.loadChunk(pos);

        // Checkerboard pattern (worst case for greedy meshing)
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 64; y++) {
                    if ((x + y + z) % 2 == 0) {
                        chunk.setBlock(x, y, z, (byte) 1);
                    }
                }
            }
        }

        long startTime = System.nanoTime();
        mesher.generateMesh(chunk);
        long endTime = System.nanoTime();

        double timeMs = (endTime - startTime) / 1_000_000.0;
        System.out.println("Worst case mesh generation time: " + timeMs + " ms");

        // Even worst case should complete in reasonable time
        assertTrue(timeMs < 10.0, 
                  "Worst case should take less than 10ms, took: " + timeMs + "ms");
    }

    @Test
    @DisplayName("Memory usage estimation")
    @Timeout(10)
    void testMemoryUsage() {
        long totalVertexBytes = 0;
        long totalIndexBytes = 0;

        // Generate meshes for 100 chunks
        for (int i = 0; i < 100; i++) {
            ChunkPos pos = new ChunkPos(i % 10, 0, i / 10);
            Chunk chunk = chunkManager.loadChunk(pos);

            // Fill with realistic terrain
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < 32; y++) {
                        if (y < 16 || (x + z) % 3 == 0) {
                            chunk.setBlock(x, y, z, (byte) 1);
                        }
                    }
                }
            }

            ChunkMesh mesh = mesher.generateMesh(chunk);
            totalVertexBytes += mesh.getVertexCount() * 6 * Float.BYTES;
            totalIndexBytes += mesh.getIndexCount() * Integer.BYTES;
        }

        long totalBytes = totalVertexBytes + totalIndexBytes;
        double totalMB = totalBytes / (1024.0 * 1024.0);

        System.out.println("Memory usage for 100 chunks: " + totalMB + " MB");
        System.out.println("Vertex data: " + (totalVertexBytes / 1024.0 / 1024.0) + " MB");
        System.out.println("Index data: " + (totalIndexBytes / 1024.0 / 1024.0) + " MB");

        // Should use less than 10MB for 100 chunks
        assertTrue(totalMB < 10.0, 
                  "100 chunks should use less than 10MB, used: " + totalMB + "MB");
    }

    @Test
    @DisplayName("Empty chunk performance")
    @Timeout(5)
    void testEmptyChunkPerformance() {
        ChunkPos pos = new ChunkPos(0, 0, 0);
        Chunk chunk = chunkManager.loadChunk(pos);

        long startTime = System.nanoTime();
        int iterations = 1000;

        for (int i = 0; i < iterations; i++) {
            mesher.generateMesh(chunk);
        }

        long endTime = System.nanoTime();
        double averageTimeMs = ((endTime - startTime) / iterations) / 1_000_000.0;

        System.out.println("Empty chunk mesh generation time: " + averageTimeMs + " ms");

        // Empty chunks should be very fast
        assertTrue(averageTimeMs < 0.1, 
                  "Empty chunk should take less than 0.1ms, took: " + averageTimeMs + "ms");
    }
}
