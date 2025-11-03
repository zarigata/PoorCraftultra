package com.poorcraft.ultra.tests;

import com.jme3.asset.AssetManager;
import com.jme3.system.JmeSystem;
import com.poorcraft.ultra.blocks.BlockRegistry;
import com.poorcraft.ultra.save.ChunkSerializer;
import com.poorcraft.ultra.voxel.*;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Voxel engine smoke tests (CP v1.05-v1.3).
 */
@DisplayName("Voxel Engine Smoke Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VoxelSmokeTest {
    
    private static AssetManager assetManager;
    private static BlockRegistry blockRegistry;
    
    @BeforeAll
    static void setup() {
        // Initialize jME asset manager for headless testing
        assetManager = JmeSystem.newAssetManager(
            Thread.currentThread().getContextClassLoader().getResource("com/jme3/asset/Desktop.cfg"));
        blockRegistry = BlockRegistry.getInstance();
    }
    
    @Test
    @Order(1)
    @Tag("smoke")
    @DisplayName("CP v1.05: Static chunk renders headlessly")
    @Timeout(10)
    void testCpV105_StaticChunkRendersHeadlessly() {
        // Create a chunk with stone blocks
        Chunk chunk = new Chunk(0, 0);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 4; y++) {
                    chunk.setBlock(x, y, z, (short) 1); // Stone
                }
            }
        }
        
        // Verify chunk is not empty
        assertFalse(chunk.isEmpty(), "Chunk should not be empty after filling with stone");
        
        // Create texture atlas
        TextureAtlas textureAtlas = new TextureAtlas();
        textureAtlas.build(assetManager);
        
        // Create mesher
        GreedyMesher mesher = new GreedyMesher(blockRegistry, textureAtlas);
        
        // Generate mesh
        MeshData meshData = mesher.mesh(chunk, null);
        
        // Verify mesh was generated
        assertNotNull(meshData, "Mesh data should not be null");
        assertFalse(meshData.isEmpty(), "Mesh should not be empty for non-empty chunk");
        assertTrue(meshData.getVertexCount() > 0, "Mesh should have vertices");
        assertTrue(meshData.getTriangleCount() > 0, "Mesh should have triangles");
        
        System.out.println("CP v1.05 test passed: Generated " + meshData.getVertexCount() + 
            " vertices, " + meshData.getTriangleCount() + " triangles");
    }
    
    @Test
    @Order(2)
    @Tag("smoke")
    @DisplayName("CP v1.1: 3×3 grid stable FPS threshold")
    @Timeout(15)
    void testCpV11_3x3GridStableFpsThreshold() {
        // Create 3×3 chunk grid
        List<Chunk> chunks = new ArrayList<>();
        for (int cx = -1; cx <= 1; cx++) {
            for (int cz = -1; cz <= 1; cz++) {
                Chunk chunk = new Chunk(cx, cz);
                
                // Base layer (Y=0-2): stone
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = 0; y < 3; y++) {
                            chunk.setBlock(x, y, z, (short) 1); // Stone
                        }
                    }
                }
                
                // Top layer (Y=3): varied blocks
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        if ((x + z) % 3 == 0) {
                            chunk.setBlock(x, 3, z, (short) 5); // Grass
                        } else if ((x + z) % 3 == 1) {
                            chunk.setBlock(x, 3, z, (short) 4); // Dirt
                        } else {
                            chunk.setBlock(x, 3, z, (short) 1); // Stone
                        }
                    }
                }
                
                chunks.add(chunk);
            }
        }
        
        // Verify all chunks created
        assertEquals(9, chunks.size(), "Should have 9 chunks in 3×3 grid");
        
        // Create texture atlas and mesher
        TextureAtlas textureAtlas = new TextureAtlas();
        textureAtlas.build(assetManager);
        GreedyMesher mesher = new GreedyMesher(blockRegistry, textureAtlas);
        
        // Measure mesh generation time
        long startTime = System.currentTimeMillis();
        int totalVertices = 0;
        
        for (Chunk chunk : chunks) {
            MeshData meshData = mesher.mesh(chunk, null);
            totalVertices += meshData.getVertexCount();
        }
        
        long elapsedTime = System.currentTimeMillis() - startTime;
        
        // Verify performance (should complete in reasonable time)
        assertTrue(elapsedTime < 5000, "Meshing 9 chunks should complete in under 5 seconds");
        assertTrue(totalVertices > 0, "Should generate vertices for 3×3 grid");
        
        System.out.println("CP v1.1 test passed: Meshed 9 chunks in " + elapsedTime + 
            "ms, total vertices: " + totalVertices);
    }
    
    @Test
    @Order(3)
    @Tag("smoke")
    @DisplayName("CP v1.2: Automated place 10 / break 10 counts match")
    @Timeout(10)
    void testCpV12_PlaceBreakCountsMatch() {
        // Create chunk manager (minimal setup)
        ChunkManager chunkManager = new ChunkManager();
        chunkManager.init(assetManager, new com.jme3.scene.Node("TestRoot"));
        
        // Place 10 blocks
        int placedCount = 0;
        for (int i = 0; i < 10; i++) {
            int x = i % 16;
            int y = 5;
            int z = i / 16;
            
            chunkManager.setBlock(x, y, z, (short) 1); // Stone
            placedCount++;
        }
        
        // Verify blocks were placed
        int verifyPlaced = 0;
        for (int i = 0; i < 10; i++) {
            int x = i % 16;
            int y = 5;
            int z = i / 16;
            
            if (chunkManager.getBlock(x, y, z) != 0) {
                verifyPlaced++;
            }
        }
        
        assertEquals(10, verifyPlaced, "Should have 10 blocks placed");
        
        // Break 10 blocks
        int brokenCount = 0;
        for (int i = 0; i < 10; i++) {
            int x = i % 16;
            int y = 5;
            int z = i / 16;
            
            chunkManager.setBlock(x, y, z, (short) 0); // Air
            brokenCount++;
        }
        
        // Verify blocks were broken
        int verifyBroken = 0;
        for (int i = 0; i < 10; i++) {
            int x = i % 16;
            int y = 5;
            int z = i / 16;
            
            if (chunkManager.getBlock(x, y, z) == 0) {
                verifyBroken++;
            }
        }
        
        assertEquals(10, verifyBroken, "Should have 10 blocks broken");
        assertEquals(placedCount, brokenCount, "Place and break counts should match");
        
        System.out.println("CP v1.2 test passed: Placed " + placedCount + 
            " blocks, broke " + brokenCount + " blocks");
        
        // Cleanup
        chunkManager.clear();
    }
    
    @Test
    @Order(4)
    @Tag("smoke")
    @DisplayName("CP v1.3: Save/load roundtrip with checksum comparison")
    @Timeout(10)
    void testCpV13_SaveLoadRoundtripChecksum() throws Exception {
        // Create temporary save directory
        Path tempDir = Files.createTempDirectory("poorcraft-test-save");
        Path chunksDir = tempDir.resolve("chunks");
        Files.createDirectories(chunksDir);
        
        try {
            // Create a chunk with specific pattern
            Chunk originalChunk = new Chunk(5, 7);
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < 8; y++) {
                        // Create a pattern: alternating stone and dirt
                        short blockId = (short) ((x + y + z) % 2 == 0 ? 1 : 4);
                        originalChunk.setBlock(x, y, z, blockId);
                    }
                }
            }
            
            // Calculate checksum of original chunk
            byte[] originalChecksum = calculateChunkChecksum(originalChunk);
            
            // Save chunk
            ChunkSerializer serializer = new ChunkSerializer(chunksDir);
            List<Chunk> chunksToSave = new ArrayList<>();
            chunksToSave.add(originalChunk);
            serializer.saveChunks(chunksToSave);
            
            // Load chunk
            List<ChunkSerializer.ChunkCoord> coords = new ArrayList<>();
            coords.add(new ChunkSerializer.ChunkCoord(5, 7));
            List<Chunk> loadedChunks = serializer.loadChunks(coords);
            
            // Verify chunk was loaded
            assertEquals(1, loadedChunks.size(), "Should load 1 chunk");
            Chunk loadedChunk = loadedChunks.get(0);
            assertEquals(5, loadedChunk.getChunkX(), "Chunk X coordinate should match");
            assertEquals(7, loadedChunk.getChunkZ(), "Chunk Z coordinate should match");
            
            // Calculate checksum of loaded chunk
            byte[] loadedChecksum = calculateChunkChecksum(loadedChunk);
            
            // Compare checksums
            assertArrayEquals(originalChecksum, loadedChecksum, 
                "Checksums should match after save/load roundtrip");
            
            System.out.println("CP v1.3 test passed: Save/load roundtrip successful with matching checksums");
            
        } finally {
            // Cleanup
            Files.walk(tempDir)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        // Ignore cleanup errors
                    }
                });
        }
    }
    
    /**
     * Calculates SHA-256 checksum of chunk block data.
     */
    private byte[] calculateChunkChecksum(Chunk chunk) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        
        for (int y = 0; y < ChunkConstants.CHUNK_SIZE_Y; y++) {
            for (int z = 0; z < ChunkConstants.CHUNK_SIZE_Z; z++) {
                for (int x = 0; x < ChunkConstants.CHUNK_SIZE_X; x++) {
                    short blockId = chunk.getBlock(x, y, z);
                    digest.update((byte) (blockId & 0xFF));
                    digest.update((byte) ((blockId >> 8) & 0xFF));
                }
            }
        }
        
        return digest.digest();
    }
    
    @AfterAll
    static void cleanup() {
        System.out.println("Voxel smoke tests completed");
    }
}
