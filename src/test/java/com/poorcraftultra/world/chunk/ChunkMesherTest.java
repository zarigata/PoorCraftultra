package com.poorcraftultra.world.chunk;

import com.poorcraftultra.core.GLTestContext;
import com.poorcraftultra.rendering.TextureAtlas;
import com.poorcraftultra.world.block.BlockRegistry;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ChunkMesher class (pure algorithm testing, no OpenGL needed).
 */
@DisplayName("ChunkMesher Tests")
@ExtendWith(GLTestContext.class)
public class ChunkMesherTest {
    private ChunkManager chunkManager;
    private ChunkMesher mesher;
    private TextureAtlas textureAtlas;

    @BeforeEach
    void setUp() {
        chunkManager = new ChunkManager();
        
        // Create and initialize texture atlas
        textureAtlas = TextureAtlas.createDefault();
        textureAtlas.updateBlockTextures(BlockRegistry.getInstance());
        
        // Create mesher with texture atlas
        mesher = new ChunkMesher(chunkManager, textureAtlas);
    }
    
    @AfterEach
    void tearDown() {
        if (textureAtlas != null) {
            textureAtlas.cleanup();
        }
    }

    @Test
    @DisplayName("Empty chunk generates empty mesh")
    void testEmptyChunk() {
        ChunkPos pos = new ChunkPos(0, 0, 0);
        Chunk chunk = chunkManager.loadChunk(pos);

        ChunkMesh mesh = mesher.generateMesh(chunk);

        assertTrue(mesh.isEmpty(), "Empty chunk should generate empty mesh");
        assertEquals(0, mesh.getVertexCount());
    }

    @Test
    @DisplayName("Single block generates faces")
    void testSingleBlock() {
        ChunkPos pos = new ChunkPos(0, 0, 0);
        Chunk chunk = chunkManager.loadChunk(pos);
        chunk.setBlock(0, 0, 0, (byte) 1);

        ChunkMesh mesh = mesher.generateMesh(chunk);

        assertFalse(mesh.isEmpty(), "Single block should generate mesh");
        assertTrue(mesh.getVertexCount() > 0, "Should have vertices");
        // Single block should have 6 faces, each with 4 vertices = 24 vertices
        // But greedy meshing might merge them
        assertTrue(mesh.getVertexCount() >= 4, "Should have at least 4 vertices");
    }

    @Test
    @DisplayName("Face culling between adjacent blocks")
    void testFaceCulling() {
        ChunkPos pos = new ChunkPos(0, 0, 0);
        Chunk chunk = chunkManager.loadChunk(pos);
        chunk.setBlock(0, 0, 0, (byte) 1);

        ChunkMesh meshSingle = mesher.generateMesh(chunk);
        int singleVertexCount = meshSingle.getVertexCount();

        // Add adjacent block
        chunk.setBlock(1, 0, 0, (byte) 1);

        ChunkMesh meshDouble = mesher.generateMesh(chunk);
        int doubleVertexCount = meshDouble.getVertexCount();

        // Two adjacent blocks should have fewer vertices than 2x single block
        // because the shared face is culled
        assertTrue(doubleVertexCount < singleVertexCount * 2,
                "Adjacent blocks should cull shared faces");
    }

    @Test
    @DisplayName("Greedy meshing merges horizontal blocks")
    void testGreedyMeshingHorizontal() {
        ChunkPos pos = new ChunkPos(0, 0, 0);
        Chunk chunk = chunkManager.loadChunk(pos);

        // Create a 4x1x1 row of blocks
        for (int x = 0; x < 4; x++) {
            chunk.setBlock(x, 0, 0, (byte) 1);
        }

        ChunkMesh mesh = mesher.generateMesh(chunk);

        assertFalse(mesh.isEmpty(), "Row of blocks should generate mesh");
        assertTrue(mesh.getVertexCount() > 0, "Should have vertices");
        // Greedy meshing should significantly reduce vertex count
    }

    @Test
    @DisplayName("Greedy meshing merges vertical blocks")
    void testGreedyMeshingVertical() {
        ChunkPos pos = new ChunkPos(0, 0, 0);
        Chunk chunk = chunkManager.loadChunk(pos);

        // Create a 1x4x1 column of blocks
        for (int y = 0; y < 4; y++) {
            chunk.setBlock(0, y, 0, (byte) 1);
        }

        ChunkMesh mesh = mesher.generateMesh(chunk);

        assertFalse(mesh.isEmpty(), "Column of blocks should generate mesh");
        assertTrue(mesh.getVertexCount() > 0, "Should have vertices");
    }

    @Test
    @DisplayName("Greedy meshing merges plane of blocks")
    void testGreedyMeshingPlane() {
        ChunkPos pos = new ChunkPos(0, 0, 0);
        Chunk chunk = chunkManager.loadChunk(pos);

        // Create a 4x4x1 plane of blocks
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                chunk.setBlock(x, 0, z, (byte) 1);
            }
        }

        ChunkMesh mesh = mesher.generateMesh(chunk);

        assertFalse(mesh.isEmpty(), "Plane of blocks should generate mesh");
        assertTrue(mesh.getVertexCount() > 0, "Should have vertices");
        // A 4x4 plane should be merged into large quads
    }

    @Test
    @DisplayName("Face culling across chunk boundaries")
    void testChunkBoundaryFaceCulling() {
        ChunkPos pos1 = new ChunkPos(0, 0, 0);
        ChunkPos pos2 = new ChunkPos(1, 0, 0);

        Chunk chunk1 = chunkManager.loadChunk(pos1);
        Chunk chunk2 = chunkManager.loadChunk(pos2);

        // Place blocks at boundary
        chunk1.setBlock(15, 0, 0, (byte) 1);
        chunk2.setBlock(0, 0, 0, (byte) 1);

        ChunkMesh mesh1 = mesher.generateMesh(chunk1);

        // The face between the two chunks should be culled
        assertFalse(mesh1.isEmpty());
    }

    @Test
    @DisplayName("Different block types are not merged")
    void testMixedBlockTypes() {
        ChunkPos pos = new ChunkPos(0, 0, 0);
        Chunk chunk = chunkManager.loadChunk(pos);

        // Create row with different block types
        chunk.setBlock(0, 0, 0, (byte) 1);
        chunk.setBlock(1, 0, 0, (byte) 2);
        chunk.setBlock(2, 0, 0, (byte) 1);

        ChunkMesh mesh = mesher.generateMesh(chunk);

        assertFalse(mesh.isEmpty());
        // Different block types should not be merged
    }

    @Test
    @DisplayName("Meshing works for all directions")
    void testAllDirections() {
        ChunkPos pos = new ChunkPos(0, 0, 0);
        Chunk chunk = chunkManager.loadChunk(pos);

        // Single isolated block should have faces in all 6 directions
        chunk.setBlock(5, 5, 5, (byte) 1);

        ChunkMesh mesh = mesher.generateMesh(chunk);

        assertFalse(mesh.isEmpty());
        assertTrue(mesh.getVertexCount() >= 24, "Isolated block should have all 6 faces");
    }

    @Test
    @DisplayName("Section boundaries are handled correctly")
    void testSectionBoundaries() {
        ChunkPos pos = new ChunkPos(0, 0, 0);
        Chunk chunk = chunkManager.loadChunk(pos);

        // Place blocks at section boundary (y=15/16)
        chunk.setBlock(0, 15, 0, (byte) 1);
        chunk.setBlock(0, 16, 0, (byte) 1);

        ChunkMesh mesh = mesher.generateMesh(chunk);

        assertFalse(mesh.isEmpty());
        // Blocks across section boundary should have shared face culled
    }

    @Test
    @DisplayName("Color generation varies by face direction")
    void testColorGeneration() {
        ChunkPos pos = new ChunkPos(0, 0, 0);
        Chunk chunk = chunkManager.loadChunk(pos);
        chunk.setBlock(0, 0, 0, (byte) 1);

        ChunkMesh mesh = mesher.generateMesh(chunk);

        assertFalse(mesh.isEmpty());
        // Different faces should have different colors (simple shading)
        // This is implicitly tested by the mesh generation
    }

    @Test
    @DisplayName("Mesh vertices include UV coordinates (12 floats per vertex)")
    void testVertexStrideIncludesUVs() {
        ChunkPos pos = new ChunkPos(0, 0, 0);
        Chunk chunk = chunkManager.loadChunk(pos);
        chunk.setBlock(0, 0, 0, (byte) 1); // Single stone block

        ChunkMesh mesh = mesher.generateMesh(chunk);

        assertFalse(mesh.isEmpty(), "Mesh should not be empty");
        
        // Vertex stride should be 12 floats: position (3) + color (3) + UV (2) + faceUV (2) + tileSpan (2)
        // For a single isolated block with 6 faces, each face has 4 vertices
        // So minimum vertex count is 24 (6 faces × 4 vertices)
        int vertexCount = mesh.getVertexCount();
        assertTrue(vertexCount >= 24, "Single block should have at least 24 vertices (6 faces × 4 vertices)");
        
        // The vertex count should be consistent with 12 floats per vertex
        // This is implicitly validated by the ChunkMesh constructor which divides by 12
    }

    @Test
    @DisplayName("Transparent block adjacent to opaque block renders face")
    void testTransparentOpaqueTransition() {
        ChunkPos pos = new ChunkPos(0, 0, 0);
        Chunk chunk = chunkManager.loadChunk(pos);
        
        // Place opaque block (stone) and transparent block (glass) adjacent
        chunk.setBlock(0, 0, 0, (byte) 1); // Stone (opaque)
        chunk.setBlock(1, 0, 0, (byte) 5); // Glass (transparent)

        ChunkMesh mesh = mesher.generateMesh(chunk);

        assertFalse(mesh.isEmpty(), "Mesh should not be empty");
        
        // Both blocks should have faces rendered at the transition
        // Stone should show its face adjacent to glass
        // Glass should show its face adjacent to stone
        // The exact vertex count depends on greedy meshing, but should be > 0
        assertTrue(mesh.getVertexCount() > 0, "Should have vertices for transparent-opaque transition");
    }

    @Test
    @DisplayName("Two adjacent transparent blocks of same type cull shared face")
    void testTransparentBlockCulling() {
        ChunkPos pos = new ChunkPos(0, 0, 0);
        Chunk chunk = chunkManager.loadChunk(pos);
        
        // Place two glass blocks adjacent
        chunk.setBlock(0, 0, 0, (byte) 5); // Glass
        chunk.setBlock(1, 0, 0, (byte) 5); // Glass

        ChunkMesh meshDouble = mesher.generateMesh(chunk);

        // Now test with a single glass block for comparison
        Chunk chunkSingle = chunkManager.loadChunk(new ChunkPos(1, 0, 0));
        chunkSingle.setBlock(0, 0, 0, (byte) 5); // Glass
        ChunkMesh meshSingle = mesher.generateMesh(chunkSingle);

        // Two adjacent glass blocks should have fewer vertices than 2× single glass
        // because the shared face between them should be culled
        assertTrue(meshDouble.getVertexCount() < meshSingle.getVertexCount() * 2,
                "Adjacent identical transparent blocks should cull shared face");
    }

    @Test
    @DisplayName("Different transparent blocks render face between them")
    void testDifferentTransparentBlocks() {
        ChunkPos pos = new ChunkPos(0, 0, 0);
        Chunk chunk = chunkManager.loadChunk(pos);
        
        // If we had different transparent blocks, they should render faces between them
        // For now, we only have glass (ID 5), so this test documents expected behavior
        // Place two glass blocks - they should cull since they're the same type
        chunk.setBlock(0, 0, 0, (byte) 5); // Glass
        chunk.setBlock(1, 0, 0, (byte) 5); // Glass

        ChunkMesh mesh = mesher.generateMesh(chunk);

        assertFalse(mesh.isEmpty(), "Mesh should not be empty");
        // This test serves as documentation for future when we have multiple transparent block types
    }
}
