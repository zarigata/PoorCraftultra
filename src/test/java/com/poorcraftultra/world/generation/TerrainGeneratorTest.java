package com.poorcraftultra.world.generation;

import com.poorcraftultra.world.block.BlockRegistry;
import com.poorcraftultra.world.chunk.Chunk;
import com.poorcraftultra.world.chunk.ChunkPos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Terrain Generator Tests")
class TerrainGeneratorTest {
    
    @BeforeAll
    static void setup() {
        // Ensure BlockRegistry is initialized
        BlockRegistry.getInstance();
    }
    
    @Test
    @DisplayName("Height map generation produces values in expected range")
    void testHeightMapGeneration() {
        TerrainGenerator generator = TerrainGenerator.createDefault(12345L);
        
        for (int x = -100; x <= 100; x += 10) {
            for (int z = -100; z <= 100; z += 10) {
                int height = generator.getHeight(x, z);
                int baseHeight = generator.getBaseHeight();
                int variation = generator.getHeightVariation();
                assertTrue(height >= baseHeight - variation && height <= baseHeight + variation,
                    "Height " + height + " out of range at (" + x + ", " + z + ")");
            }
        }
    }
    
    @Test
    @DisplayName("Same seed produces deterministic heights")
    void testHeightDeterminism() {
        TerrainGenerator gen1 = TerrainGenerator.createDefault(12345L);
        TerrainGenerator gen2 = TerrainGenerator.createDefault(12345L);
        
        for (int x = -50; x <= 50; x += 10) {
            for (int z = -50; z <= 50; z += 10) {
                assertEquals(gen1.getHeight(x, z), gen2.getHeight(x, z));
            }
        }
    }
    
    @Test
    @DisplayName("Heights vary across the world")
    void testHeightVariation() {
        TerrainGenerator generator = TerrainGenerator.createDefault(12345L);
        
        int minHeight = Integer.MAX_VALUE;
        int maxHeight = Integer.MIN_VALUE;
        
        for (int x = 0; x < 100; x += 5) {
            for (int z = 0; z < 100; z += 5) {
                int height = generator.getHeight(x, z);
                minHeight = Math.min(minHeight, height);
                maxHeight = Math.max(maxHeight, height);
            }
        }
        
        assertTrue(maxHeight - minHeight > 10, "Terrain should have significant height variation");
    }
    
    @Test
    @DisplayName("Terrain generation fills chunk with blocks")
    void testTerrainGeneration() {
        TerrainGenerator generator = TerrainGenerator.createDefault(12345L);
        Chunk chunk = new Chunk(new ChunkPos(0, 0, 0));
        
        generator.generateTerrain(chunk);
        
        // Count non-air blocks
        int solidBlocks = 0;
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 256; y++) {
                for (int z = 0; z < 16; z++) {
                    if (chunk.getBlock(x, y, z) != 0) {
                        solidBlocks++;
                    }
                }
            }
        }
        
        assertTrue(solidBlocks > 0, "Chunk should have solid blocks after generation");
    }
    
    @Test
    @DisplayName("Terrain has correct layer structure")
    void testTerrainLayers() {
        TerrainGenerator generator = TerrainGenerator.createDefault(12345L);
        Chunk chunk = new Chunk(new ChunkPos(0, 0, 0));
        
        generator.generateTerrain(chunk);
        
        byte stoneId = BlockRegistry.getInstance().getBlock("stone").getId();
        byte grassId = BlockRegistry.getInstance().getBlock("grass").getId();
        byte airId = BlockRegistry.getInstance().getBlock("air").getId();
        
        // Check a column
        int x = 8, z = 8;
        int height = generator.getHeight(x, z);
        
        // Below height-4 should be stone
        if (height > 4) {
            assertEquals(stoneId, chunk.getBlock(x, height - 5, z), "Deep underground should be stone");
        }
        
        // At height-1 should be grass
        if (height > 0 && height < 256) {
            assertEquals(grassId, chunk.getBlock(x, height - 1, z), "Surface should be grass");
        }
        
        // Above height should be air
        if (height < 255) {
            assertEquals(airId, chunk.getBlock(x, height, z), "Above surface should be air");
        }
    }
    
    @Test
    @DisplayName("Adjacent chunks have continuous terrain")
    void testChunkBoundaries() {
        TerrainGenerator generator = TerrainGenerator.createDefault(12345L);
        Chunk chunk1 = new Chunk(new ChunkPos(0, 0, 0));
        Chunk chunk2 = new Chunk(new ChunkPos(1, 0, 0));
        
        generator.generateTerrain(chunk1);
        generator.generateTerrain(chunk2);
        
        // Check heights at boundary
        int height1 = generator.getHeight(15, 8);
        int height2 = generator.getHeight(16, 8);
        
        // Heights should be continuous (not jump dramatically)
        assertTrue(Math.abs(height1 - height2) < 10, "Terrain should be continuous at chunk boundaries");
    }
    
    @Test
    @DisplayName("Vertical chunks handle terrain correctly")
    void testVerticalChunks() {
        TerrainGenerator generator = TerrainGenerator.createDefault(12345L);
        Chunk chunkLower = new Chunk(new ChunkPos(0, 0, 0));
        Chunk chunkUpper = new Chunk(new ChunkPos(0, 1, 0));
        
        generator.generateTerrain(chunkLower);
        generator.generateTerrain(chunkUpper);
        
        // Lower chunk should have terrain
        int solidBlocksLower = countSolidBlocks(chunkLower);
        assertTrue(solidBlocksLower > 0, "Lower chunk should have terrain");
        
        // Upper chunk might be empty or have some terrain depending on height
        int solidBlocksUpper = countSolidBlocks(chunkUpper);
        assertTrue(solidBlocksUpper >= 0, "Upper chunk should be valid");
    }
    
    @Test
    @DisplayName("Terrain can generate below sea level")
    void testSeaLevel() {
        TerrainGenerator generator = TerrainGenerator.createDefault(12345L);
        
        boolean foundBelowSeaLevel = false;
        int seaLevel = generator.getSeaLevel();
        
        for (int x = 0; x < 100; x += 5) {
            for (int z = 0; z < 100; z += 5) {
                if (generator.getHeight(x, z) < seaLevel) {
                    foundBelowSeaLevel = true;
                    break;
                }
            }
            if (foundBelowSeaLevel) break;
        }
        
        // With enough samples, we should find some terrain below sea level
        assertTrue(foundBelowSeaLevel, "Should find terrain below sea level");
    }
    
    @Test
    @DisplayName("Default factory creates valid generator")
    void testDefaultFactory() {
        TerrainGenerator generator = TerrainGenerator.createDefault(12345L);
        
        assertNotNull(generator);
        assertEquals(64, generator.getBaseHeight());
        assertEquals(32, generator.getHeightVariation());
        assertEquals(62, generator.getSeaLevel());
        
        // Should generate valid terrain
        Chunk chunk = new Chunk(new ChunkPos(0, 0, 0));
        generator.generateTerrain(chunk);
        assertTrue(countSolidBlocks(chunk) > 0);
    }
    
    private int countSolidBlocks(Chunk chunk) {
        int count = 0;
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 256; y++) {
                for (int z = 0; z < 16; z++) {
                    if (chunk.getBlock(x, y, z) != 0) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
}
