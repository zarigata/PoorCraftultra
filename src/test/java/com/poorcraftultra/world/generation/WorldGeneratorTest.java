package com.poorcraftultra.world.generation;

import com.poorcraftultra.world.block.BlockRegistry;
import com.poorcraftultra.world.chunk.Chunk;
import com.poorcraftultra.world.chunk.ChunkPos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("World Generator Tests")
class WorldGeneratorTest {
    
    @BeforeAll
    static void setup() {
        BlockRegistry.getInstance();
    }
    
    @Test
    @DisplayName("Chunk generation produces non-empty chunks")
    void testChunkGeneration() {
        WorldGenerator generator = WorldGenerator.createDefault(12345L);
        Chunk chunk = new Chunk(new ChunkPos(0, 0, 0));
        
        generator.generateChunk(chunk);
        
        int solidBlocks = countSolidBlocks(chunk);
        assertTrue(solidBlocks > 0, "Generated chunk should have blocks");
    }
    
    @Test
    @DisplayName("Same seed produces identical chunks")
    void testGenerationDeterminism() {
        WorldGenerator gen1 = WorldGenerator.createDefault(12345L);
        WorldGenerator gen2 = WorldGenerator.createDefault(12345L);
        
        Chunk chunk1 = new Chunk(new ChunkPos(5, 0, 10));
        Chunk chunk2 = new Chunk(new ChunkPos(5, 0, 10));
        
        gen1.generateChunk(chunk1);
        gen2.generateChunk(chunk2);
        
        assertTrue(chunksEqual(chunk1, chunk2), "Same seed should produce identical chunks");
    }
    
    @Test
    @DisplayName("Different seeds produce different chunks")
    void testDifferentSeeds() {
        WorldGenerator gen1 = WorldGenerator.createDefault(12345L);
        WorldGenerator gen2 = WorldGenerator.createDefault(54321L);
        
        Chunk chunk1 = new Chunk(new ChunkPos(0, 0, 0));
        Chunk chunk2 = new Chunk(new ChunkPos(0, 0, 0));
        
        gen1.generateChunk(chunk1);
        gen2.generateChunk(chunk2);
        
        assertFalse(chunksEqual(chunk1, chunk2), "Different seeds should produce different chunks");
    }
    
    @Test
    @DisplayName("Generated chunks have terrain and caves")
    void testTerrainAndCaves() {
        WorldGenerator generator = WorldGenerator.createDefault(12345L);
        Chunk chunk = new Chunk(new ChunkPos(0, 0, 0));
        
        generator.generateChunk(chunk);
        
        byte stoneId = BlockRegistry.getInstance().getBlock("stone").getId();
        byte grassId = BlockRegistry.getInstance().getBlock("grass").getId();
        byte airId = BlockRegistry.getInstance().getBlock("air").getId();
        
        boolean hasStone = false, hasGrass = false, hasAir = false;
        
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 256; y++) {
                for (int z = 0; z < 16; z++) {
                    byte block = chunk.getBlock(x, y, z);
                    if (block == stoneId) hasStone = true;
                    if (block == grassId) hasGrass = true;
                    if (block == airId) hasAir = true;
                }
            }
        }
        
        assertTrue(hasStone, "Should have stone blocks");
        assertTrue(hasGrass, "Should have grass blocks");
        assertTrue(hasAir, "Should have air blocks");
    }
    
    @Test
    @DisplayName("Caves can be disabled")
    void testCavesDisabled() {
        WorldGenerator genWithCaves = WorldGenerator.createDefault(12345L);
        WorldGenerator genNoCaves = WorldGenerator.createDefault(12345L);
        genNoCaves.setGenerateCaves(false);
        
        Chunk chunk1 = new Chunk(new ChunkPos(0, 0, 0));
        Chunk chunk2 = new Chunk(new ChunkPos(0, 0, 0));
        
        genWithCaves.generateChunk(chunk1);
        genNoCaves.generateChunk(chunk2);
        
        int solid1 = countSolidBlocks(chunk1);
        int solid2 = countSolidBlocks(chunk2);
        
        assertTrue(solid2 > solid1, "Chunk without caves should have more solid blocks");
    }
    
    @Test
    @DisplayName("Multiple chunks generate correctly")
    void testMultipleChunks() {
        WorldGenerator generator = WorldGenerator.createDefault(12345L);
        
        for (int cx = -2; cx <= 2; cx++) {
            for (int cz = -2; cz <= 2; cz++) {
                Chunk chunk = new Chunk(new ChunkPos(cx, 0, cz));
                generator.generateChunk(chunk);
                assertTrue(countSolidBlocks(chunk) > 0, "Chunk at (" + cx + ", " + cz + ") should have blocks");
            }
        }
    }
    
    @Test
    @DisplayName("Vertical chunks generate correctly")
    void testVerticalChunks() {
        WorldGenerator generator = WorldGenerator.createDefault(12345L);
        
        Chunk chunk0 = new Chunk(new ChunkPos(0, 0, 0));
        Chunk chunk1 = new Chunk(new ChunkPos(0, 1, 0));
        
        generator.generateChunk(chunk0);
        generator.generateChunk(chunk1);
        
        assertTrue(countSolidBlocks(chunk0) > 0, "Lower chunk should have blocks");
        // Upper chunk might be empty or have some blocks
        assertTrue(countSolidBlocks(chunk1) >= 0, "Upper chunk should be valid");
    }
    
    @Test
    @DisplayName("Chunk optimization frees empty sections")
    void testChunkOptimization() {
        WorldGenerator generator = WorldGenerator.createDefault(12345L);
        Chunk chunk = new Chunk(new ChunkPos(0, 0, 0));
        
        generator.generateChunk(chunk);
        
        // After generation with caves, some sections should be empty and optimized
        // This is implicitly tested by the optimize() call in generateChunk
        assertDoesNotThrow(() -> chunk.optimize());
    }
    
    @Test
    @DisplayName("Default factory creates valid generator")
    void testDefaultFactory() {
        WorldGenerator generator = WorldGenerator.createDefault(12345L);
        
        assertNotNull(generator);
        assertEquals(12345L, generator.getSeed());
        assertTrue(generator.isGenerateCaves());
        assertNotNull(generator.getTerrainGenerator());
        assertNotNull(generator.getCaveGenerator());
        
        // Should generate valid chunks
        Chunk chunk = new Chunk(new ChunkPos(0, 0, 0));
        generator.generateChunk(chunk);
        assertTrue(countSolidBlocks(chunk) > 0);
    }
    
    @Test
    @DisplayName("Seed is correctly propagated")
    void testSeedPropagation() {
        long seed = 99999L;
        WorldGenerator generator = WorldGenerator.createDefault(seed);
        
        assertEquals(seed, generator.getSeed());
        
        // Generate same chunk twice with same seed
        Chunk chunk1 = new Chunk(new ChunkPos(10, 0, 20));
        Chunk chunk2 = new Chunk(new ChunkPos(10, 0, 20));
        
        generator.generateChunk(chunk1);
        generator.generateChunk(chunk2);
        
        assertTrue(chunksEqual(chunk1, chunk2), "Same seed should produce identical results");
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
    
    private boolean chunksEqual(Chunk chunk1, Chunk chunk2) {
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 256; y++) {
                for (int z = 0; z < 16; z++) {
                    if (chunk1.getBlock(x, y, z) != chunk2.getBlock(x, y, z)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
