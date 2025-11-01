package com.poorcraft.ultra.voxel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ChunkMesherTest {

    private final ChunkMesher chunkMesher = new ChunkMesher();

    @Test
    void testEmptyChunkMesh() {
        Chunk chunk = new Chunk(0, 0);

        ChunkMesh mesh = chunkMesher.buildMesh(chunk, null);

        assertTrue(mesh.isEmpty(), "Empty chunk should produce an empty mesh");
        assertEquals(0, mesh.totalVertexCount(), "Empty mesh must have zero vertices");
        assertEquals(0, mesh.totalTriangleCount(), "Empty mesh must have zero triangles");
    }

    @Test
    void testSingleBlockMeshCounts() {
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlock(8, 64, 8, BlockType.STONE);

        ChunkMesh mesh = chunkMesher.buildMesh(chunk, null);

        assertEquals(24, mesh.totalVertexCount(), "Standalone block should contribute 6 quads (24 vertices)");
        assertEquals(12, mesh.totalTriangleCount(), "Standalone block should contribute 12 triangles");
        assertEquals(24, mesh.vertexCount(BlockType.STONE), "Stone vertex count should match total");
        assertEquals(12, mesh.triangleCount(BlockType.STONE), "Stone triangle count should match total");
    }

    @Test
    void testFaceCullingOnSolidCube() {
        Chunk chunk = new Chunk(0, 0);
        for (int x = 4; x < 6; x++) {
            for (int y = 64; y < 66; y++) {
                for (int z = 4; z < 6; z++) {
                    chunk.setBlock(x, y, z, BlockType.STONE);
                }
            }
        }

        ChunkMesh mesh = chunkMesher.buildMesh(chunk, null);

        assertTrue(mesh.totalVertexCount() > 0, "Solid cube should still render exterior faces");
        assertTrue(mesh.totalVertexCount() < 8 * 24, "Interior faces must be culled");
        assertTrue(mesh.totalTriangleCount() < 8 * 12, "Interior faces must be culled");
    }

    @Test
    void testGreedyReducesPlane() {
        Chunk chunk = new Chunk(0, 0);
        int y = 64;
        for (int x = 0; x < Chunk.SIZE_X; x++) {
            for (int z = 0; z < Chunk.SIZE_Z; z++) {
                chunk.setBlock(x, y, z, BlockType.STONE);
            }
        }

        ChunkMesh mesh = chunkMesher.buildMesh(chunk, null);

        int naiveVertexCount = Chunk.SIZE_X * Chunk.SIZE_Z * 2 * 4; // two faces per block, four vertices per quad
        int naiveTriangleCount = Chunk.SIZE_X * Chunk.SIZE_Z * 2 * 2; // two faces per block, two triangles per quad

        assertTrue(mesh.totalVertexCount() > 0, "Merged plane should still produce geometry");
        assertTrue(mesh.totalTriangleCount() > 0, "Merged plane should still produce geometry");
        assertTrue(mesh.totalVertexCount() <= naiveVertexCount / 64,
            () -> "Greedy merge should significantly reduce vertices; expected <= " + (naiveVertexCount / 64)
                + " but was " + mesh.totalVertexCount());
        assertTrue(mesh.totalTriangleCount() <= naiveTriangleCount / 64,
            () -> "Greedy merge should significantly reduce triangles; expected <= " + (naiveTriangleCount / 64)
                + " but was " + mesh.totalTriangleCount());
    }
}
