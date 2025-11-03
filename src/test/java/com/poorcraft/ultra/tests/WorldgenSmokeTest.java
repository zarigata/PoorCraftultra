package com.poorcraft.ultra.tests;

import com.poorcraft.ultra.blocks.BlockRegistry;
import com.poorcraft.ultra.voxel.Chunk;
import com.poorcraft.ultra.voxel.LightEngine;
import com.poorcraft.ultra.world.WorldGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke tests for world generation (CP v2.2).
 * Tests seed determinism, cave density, and lighting propagation.
 */
@Tag("smoke")
public class WorldgenSmokeTest {
    
    @BeforeAll
    public static void setup() {
        // Initialize BlockRegistry
        BlockRegistry.getInstance();
    }
    
    @Test
    public void testSeedDeterminism() {
        long seed = 12345L;
        
        // Generate same chunk with two different generators using same seed
        WorldGenerator gen1 = new WorldGenerator(seed);
        WorldGenerator gen2 = new WorldGenerator(seed);
        
        Chunk chunk1 = gen1.generateChunk(0, 0);
        Chunk chunk2 = gen2.generateChunk(0, 0);
        
        // Verify blocks are identical
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    assertEquals(chunk1.getBlock(x, y, z), chunk2.getBlock(x, y, z),
                        String.format("Block mismatch at (%d, %d, %d)", x, y, z));
                }
            }
        }
    }
    
    @Test
    @DisplayName("CP v2: Cave density 20-40% underground (Y<8)")
    public void testCaveDensityBounds() {
        WorldGenerator generator = new WorldGenerator(42L);
        
        int undergroundAirBlocks = 0;
        int undergroundTotalBlocks = 0;
        int chunksToTest = 10;
        
        // Test multiple chunks along X axis
        for (int cx = 0; cx < chunksToTest; cx++) {
            Chunk chunk = generator.generateChunk(cx, 0);
            
            // Count only underground blocks (1 <= y < 8)
            for (int x = 0; x < 16; x++) {
                for (int y = 1; y < 8; y++) {
                    for (int z = 0; z < 16; z++) {
                        undergroundTotalBlocks++;
                        if (chunk.getBlock(x, y, z) == 0) {
                            undergroundAirBlocks++;
                        }
                    }
                }
            }
        }
        
        double airFraction = (double) undergroundAirBlocks / undergroundTotalBlocks;
        
        // Cave density should be 20-40% air underground
        assertTrue(airFraction >= 0.20, "Cave density too low: " + airFraction + " (expected >= 0.20)");
        assertTrue(airFraction <= 0.40, "Cave density too high: " + airFraction + " (expected <= 0.40)");
    }
    
    @Test
    @DisplayName("CP v2: Torch light falloff decreases by 1 per block")
    public void testTorchLightFalloff() {
        BlockRegistry registry = BlockRegistry.getInstance();
        
        // Create a flat chunk: solid floor at y=0, air above
        Chunk chunk = new Chunk(0, 0);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                chunk.setBlock(x, 0, z, (short) 1); // Stone floor
                for (int y = 1; y < 16; y++) {
                    chunk.setBlock(x, y, z, (short) 0); // Air
                }
            }
        }
        
        // Place torch at (8,1,8) - ID 24, emission 14
        chunk.setBlock(8, 1, 8, (short) 24);
        
        // Create minimal ChunkManager stub for LightEngine
        MinimalChunkManager chunkManager = new MinimalChunkManager(chunk);
        LightEngine lightEngine = new LightEngine(chunkManager, registry);
        
        // Initialize block light
        lightEngine.initializeBlockLight(chunk);
        
        // Process light propagation (drain queues)
        for (int i = 0; i < 100; i++) {
            lightEngine.update(0.016f);
        }
        
        // Verify light falloff along +X axis from torch
        int[] expectedLevels = {14, 13, 12, 11, 10, 9, 8, 7};
        for (int dist = 0; dist < expectedLevels.length; dist++) {
            int actualLevel = chunk.getBlockLight(8 + dist, 1, 8);
            assertEquals(expectedLevels[dist], actualLevel,
                String.format("Light level at distance %d should be %d but was %d",
                    dist, expectedLevels[dist], actualLevel));
        }
    }
    
    @Test
    @DisplayName("CP v2: Skylight propagates down open column and blocks reduce it")
    public void testSkylightPropagation() {
        BlockRegistry registry = BlockRegistry.getInstance();
        
        // Create chunk with air column at (8,*,8)
        Chunk chunk = new Chunk(0, 0);
        for (int y = 0; y < 16; y++) {
            chunk.setBlock(8, y, 8, (short) 0); // Air column
        }
        
        // Fill rest with stone to isolate the test
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (x != 8 || z != 8) {
                    for (int y = 0; y < 16; y++) {
                        chunk.setBlock(x, y, z, (short) 1); // Stone
                    }
                }
            }
        }
        
        // Create minimal ChunkManager stub
        MinimalChunkManager chunkManager = new MinimalChunkManager(chunk);
        LightEngine lightEngine = new LightEngine(chunkManager, registry);
        
        // Initialize skylight
        lightEngine.initializeSkylight(chunk);
        
        // Verify skylight is 15 down the entire air column
        for (int y = 0; y < 16; y++) {
            assertEquals(15, chunk.getSkyLight(8, y, 8),
                String.format("Skylight at y=%d should be 15 in open column", y));
        }
        
        // Place a solid block at (8,10,8)
        chunk.setBlock(8, 10, 8, (short) 1); // Stone
        
        // Remove skylight at that position
        lightEngine.removeSkylight(0, 0, 8, 10, 8);
        
        // Process removal propagation
        for (int i = 0; i < 100; i++) {
            lightEngine.update(0.016f);
        }
        
        // Verify skylight below the block is reduced
        int skylightBelow = chunk.getSkyLight(8, 9, 8);
        assertTrue(skylightBelow < 15,
            String.format("Skylight below solid block should be < 15, was %d", skylightBelow));
    }
    
    @Test
    public void testWorldGeneratorHeight() {
        WorldGenerator generator = new WorldGenerator(999L);
        
        // Generate a chunk and verify terrain height is reasonable
        Chunk chunk = generator.generateChunk(0, 0);
        
        boolean hasBlocks = false;
        boolean hasAir = false;
        
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 16; y++) {
                    short blockId = chunk.getBlock(x, y, z);
                    if (blockId != 0) {
                        hasBlocks = true;
                    } else {
                        hasAir = true;
                    }
                }
            }
        }
        
        // Chunk should have both blocks and air (not completely solid or empty)
        assertTrue(hasBlocks, "Chunk has no blocks");
        assertTrue(hasAir, "Chunk has no air");
    }
    
    @Test
    public void testOreGeneration() {
        WorldGenerator generator = new WorldGenerator(777L);
        
        int oreCount = 0;
        int chunksToTest = 5;
        
        // Test multiple chunks for ore presence
        for (int cx = 0; cx < chunksToTest; cx++) {
            Chunk chunk = generator.generateChunk(cx, 0);
            
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        short blockId = chunk.getBlock(x, y, z);
                        // Check for ore blocks (coal=12, iron=13, gold=14, diamond=15)
                        if (blockId >= 12 && blockId <= 15) {
                            oreCount++;
                        }
                    }
                }
            }
        }
        
        // Should have at least some ores across multiple chunks
        assertTrue(oreCount > 0, "No ores generated in " + chunksToTest + " chunks");
    }
    
    /**
     * Minimal ChunkManager implementation for testing LightEngine.
     * Returns a single chunk for all queries.
     */
    private static class MinimalChunkManager extends com.poorcraft.ultra.voxel.ChunkManager {
        private final Chunk chunk;
        
        MinimalChunkManager(Chunk chunk) {
            this.chunk = chunk;
        }
        
        @Override
        public Collection<Chunk> getAllChunks() {
            return Collections.singletonList(chunk);
        }
    }
}
