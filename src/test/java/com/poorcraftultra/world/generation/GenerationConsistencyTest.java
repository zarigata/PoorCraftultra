package com.poorcraftultra.world.generation;

import com.poorcraftultra.world.block.BlockRegistry;
import com.poorcraftultra.world.chunk.Chunk;
import com.poorcraftultra.world.chunk.ChunkPos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Generation Consistency Tests")
class GenerationConsistencyTest {
    
    @BeforeAll
    static void setup() {
        BlockRegistry.getInstance();
    }
    
    @Test
    @DisplayName("Same chunk with same seed is always identical")
    void testSeedConsistency() {
        WorldGenerator generator = WorldGenerator.createDefault(12345L);
        ChunkPos pos = new ChunkPos(5, 0, 10);
        
        byte[][] snapshots = new byte[10][16 * 256 * 16];
        
        for (int i = 0; i < 10; i++) {
            Chunk chunk = new Chunk(pos);
            generator.generateChunk(chunk);
            snapshots[i] = serializeChunk(chunk);
        }
        
        // All snapshots should be identical
        for (int i = 1; i < 10; i++) {
            assertArrayEquals(snapshots[0], snapshots[i], "Generation " + i + " differs from first generation");
        }
    }
    
    @Test
    @DisplayName("Generation is independent of execution order")
    void testChunkIndependence() {
        WorldGenerator generator = WorldGenerator.createDefault(12345L);
        
        // Generate in sequential order
        Chunk chunk1 = new Chunk(new ChunkPos(0, 0, 0));
        Chunk chunk2 = new Chunk(new ChunkPos(1, 0, 0));
        Chunk chunk3 = new Chunk(new ChunkPos(2, 0, 0));
        generator.generateChunk(chunk1);
        generator.generateChunk(chunk2);
        generator.generateChunk(chunk3);
        byte[] seq1 = serializeChunk(chunk1);
        byte[] seq2 = serializeChunk(chunk2);
        byte[] seq3 = serializeChunk(chunk3);
        
        // Generate in random order
        Chunk chunkA = new Chunk(new ChunkPos(2, 0, 0));
        Chunk chunkB = new Chunk(new ChunkPos(0, 0, 0));
        Chunk chunkC = new Chunk(new ChunkPos(1, 0, 0));
        generator.generateChunk(chunkA);
        generator.generateChunk(chunkB);
        generator.generateChunk(chunkC);
        byte[] rand1 = serializeChunk(chunkB);
        byte[] rand2 = serializeChunk(chunkC);
        byte[] rand3 = serializeChunk(chunkA);
        
        assertArrayEquals(seq1, rand1, "Chunk (0,0,0) should be identical regardless of order");
        assertArrayEquals(seq2, rand2, "Chunk (1,0,0) should be identical regardless of order");
        assertArrayEquals(seq3, rand3, "Chunk (2,0,0) should be identical regardless of order");
    }
    
    @Test
    @DisplayName("Long-term consistency over many regenerations")
    void testLongTermConsistency() {
        WorldGenerator generator = WorldGenerator.createDefault(12345L);
        ChunkPos pos = new ChunkPos(7, 0, 13);
        
        Chunk original = new Chunk(pos);
        generator.generateChunk(original);
        byte[] originalData = serializeChunk(original);
        
        // Regenerate 1000 times
        for (int i = 0; i < 1000; i++) {
            Chunk chunk = new Chunk(pos);
            generator.generateChunk(chunk);
            byte[] data = serializeChunk(chunk);
            assertArrayEquals(originalData, data, "Generation " + i + " differs from original");
        }
    }
    
    @Test
    @DisplayName("Boundary consistency - regenerating neighbors doesn't affect chunk")
    void testBoundaryConsistency() {
        WorldGenerator generator = WorldGenerator.createDefault(12345L);
        ChunkPos center = new ChunkPos(5, 0, 5);
        
        // Generate center chunk
        Chunk centerChunk = new Chunk(center);
        generator.generateChunk(centerChunk);
        byte[] centerData = serializeChunk(centerChunk);
        
        // Generate all neighbors
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                Chunk neighbor = new Chunk(new ChunkPos(center.getX() + dx, 0, center.getZ() + dz));
                generator.generateChunk(neighbor);
            }
        }
        
        // Regenerate center chunk
        Chunk centerChunk2 = new Chunk(center);
        generator.generateChunk(centerChunk2);
        byte[] centerData2 = serializeChunk(centerChunk2);
        
        assertArrayEquals(centerData, centerData2, "Center chunk should be unchanged after generating neighbors");
    }
    
    @Test
    @DisplayName("Various seed values work correctly")
    void testSeedRange() {
        long[] seeds = {0L, -1L, 12345L, -99999L, Long.MAX_VALUE, Long.MIN_VALUE};
        
        for (long seed : seeds) {
            WorldGenerator generator = WorldGenerator.createDefault(seed);
            Chunk chunk = new Chunk(new ChunkPos(0, 0, 0));
            
            assertDoesNotThrow(() -> generator.generateChunk(chunk), 
                "Seed " + seed + " should work without errors");
            
            int solidBlocks = countSolidBlocks(chunk);
            assertTrue(solidBlocks > 0, "Seed " + seed + " should generate terrain");
        }
    }
    
    @Test
    @DisplayName("Extreme coordinates work correctly")
    void testCoordinateRange() {
        WorldGenerator generator = WorldGenerator.createDefault(12345L);
        
        ChunkPos[] positions = {
            new ChunkPos(0, 0, 0),
            new ChunkPos(1000, 0, 1000),
            new ChunkPos(-1000, 0, -1000),
            new ChunkPos(10000, 0, -10000),
            new ChunkPos(-5000, 0, 5000)
        };
        
        for (ChunkPos pos : positions) {
            Chunk chunk = new Chunk(pos);
            assertDoesNotThrow(() -> generator.generateChunk(chunk),
                "Position " + pos + " should work without errors");
            
            int solidBlocks = countSolidBlocks(chunk);
            assertTrue(solidBlocks > 0, "Position " + pos + " should generate terrain");
        }
    }
    
    @Test
    @DisplayName("Vertical consistency across Y chunks")
    void testVerticalConsistency() {
        WorldGenerator generator = WorldGenerator.createDefault(12345L);
        TerrainGenerator terrainGen = generator.getTerrainGenerator();
        
        // Check that terrain height is consistent across vertical chunk boundaries
        int worldX = 8, worldZ = 8;
        int height = terrainGen.getHeight(worldX, worldZ);
        
        // Generate chunks at Y=0 and Y=1
        Chunk chunk0 = new Chunk(new ChunkPos(0, 0, 0));
        Chunk chunk1 = new Chunk(new ChunkPos(0, 1, 0));
        generator.generateChunk(chunk0);
        generator.generateChunk(chunk1);
        
        // Check continuity at boundary (Y=255/256)
        byte block255 = chunk0.getBlock(worldX, 255, worldZ);
        byte block256 = chunk1.getBlock(worldX, 0, worldZ);
        
        // Both should be air if height < 256, or both solid if height >= 256
        boolean bothAir = (block255 == 0 && block256 == 0);
        boolean bothSolid = (block255 != 0 && block256 != 0);
        assertTrue(bothAir || bothSolid, "Vertical boundary should be consistent");
    }
    
    @Test
    @DisplayName("No use of non-deterministic randomness")
    void testNoRandomness() {
        // This test verifies that generation doesn't use Math.random() or System.currentTimeMillis()
        // by checking that multiple generators with same seed produce same results
        
        WorldGenerator gen1 = WorldGenerator.createDefault(12345L);
        WorldGenerator gen2 = WorldGenerator.createDefault(12345L);
        
        // Generate at different times
        Chunk chunk1 = new Chunk(new ChunkPos(0, 0, 0));
        gen1.generateChunk(chunk1);
        
        try {
            Thread.sleep(100); // Wait a bit
        } catch (InterruptedException e) {
            // Ignore
        }
        
        Chunk chunk2 = new Chunk(new ChunkPos(0, 0, 0));
        gen2.generateChunk(chunk2);
        
        assertArrayEquals(serializeChunk(chunk1), serializeChunk(chunk2),
            "Generation should be deterministic regardless of time");
    }
    
    private byte[] serializeChunk(Chunk chunk) {
        byte[] data = new byte[16 * 256 * 16];
        int index = 0;
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 256; y++) {
                for (int z = 0; z < 16; z++) {
                    data[index++] = chunk.getBlock(x, y, z);
                }
            }
        }
        return data;
    }
    
    private int countSolidBlocks(Chunk chunk) {
        int count = 0;
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 256; y++) {
                for (int z = 0; z < 16; z++) {
                    if (chunk.getBlock(x, y, z) != 0) count++;
                }
            }
        }
        return count;
    }
}
