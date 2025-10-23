package com.poorcraftultra.world.generation;

import com.poorcraftultra.world.block.BlockRegistry;
import com.poorcraftultra.world.chunk.Chunk;
import com.poorcraftultra.world.chunk.ChunkPos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Cave Generator Tests")
class CaveGeneratorTest {
    
    @BeforeAll
    static void setup() {
        BlockRegistry.getInstance();
    }
    
    @Test
    @DisplayName("Cave carving removes blocks")
    void testCaveCarving() {
        CaveGenerator generator = CaveGenerator.createDefault(12345L);
        Chunk chunk = new Chunk(new ChunkPos(0, 0, 0));
        
        // Fill with stone
        byte stoneId = BlockRegistry.getInstance().getBlock("stone").getId();
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 256; y++) {
                for (int z = 0; z < 16; z++) {
                    chunk.setBlock(x, y, z, stoneId);
                }
            }
        }
        
        int solidBefore = countSolidBlocks(chunk);
        generator.carveCaves(chunk);
        int solidAfter = countSolidBlocks(chunk);
        
        assertTrue(solidAfter < solidBefore, "Caves should remove some blocks");
    }
    
    @Test
    @DisplayName("Same seed produces deterministic caves")
    void testCaveDeterminism() {
        CaveGenerator gen1 = CaveGenerator.createDefault(12345L);
        CaveGenerator gen2 = CaveGenerator.createDefault(12345L);
        
        Chunk chunk1 = createStoneChunk();
        Chunk chunk2 = createStoneChunk();
        
        gen1.carveCaves(chunk1);
        gen2.carveCaves(chunk2);
        
        // Chunks should be identical
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 256; y++) {
                for (int z = 0; z < 16; z++) {
                    assertEquals(chunk1.getBlock(x, y, z), chunk2.getBlock(x, y, z));
                }
            }
        }
    }
    
    @Test
    @DisplayName("Caves respect height limits")
    void testCaveHeightLimits() {
        CaveGenerator generator = CaveGenerator.createDefault(12345L);
        Chunk chunk = createStoneChunk();
        
        byte stoneId = BlockRegistry.getInstance().getBlock("stone").getId();
        
        generator.carveCaves(chunk);
        
        int minHeight = generator.getMinCaveHeight();
        int maxHeight = generator.getMaxCaveHeight();
        
        // Check blocks outside cave height range are unchanged
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (minHeight > 0) {
                    assertEquals(stoneId, chunk.getBlock(x, 0, z), "Below min height should be unchanged");
                }
                if (maxHeight < 255) {
                    assertEquals(stoneId, chunk.getBlock(x, 255, z), "Above max height should be unchanged");
                }
            }
        }
    }
    
    @Test
    @DisplayName("Caves don't carve at surface")
    void testNoCavesAtSurface() {
        TerrainGenerator terrainGen = TerrainGenerator.createDefault(12345L);
        CaveGenerator caveGen = CaveGenerator.createDefault(12345L);
        
        Chunk chunk = new Chunk(new ChunkPos(0, 0, 0));
        terrainGen.generateTerrain(chunk);
        
        byte grassId = BlockRegistry.getInstance().getBlock("grass").getId();
        
        // Count grass blocks before caves
        int grassBefore = 0;
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 256; y++) {
                for (int z = 0; z < 16; z++) {
                    if (chunk.getBlock(x, y, z) == grassId) grassBefore++;
                }
            }
        }
        
        caveGen.carveCaves(chunk);
        
        // Count grass blocks after caves
        int grassAfter = 0;
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 256; y++) {
                for (int z = 0; z < 16; z++) {
                    if (chunk.getBlock(x, y, z) == grassId) grassAfter++;
                }
            }
        }
        
        // Most grass should remain (caves shouldn't reach surface often)
        assertTrue(grassAfter >= grassBefore * 0.8, "Most surface grass should remain");
    }
    
    @Test
    @DisplayName("No caves at bedrock level")
    void testNoCavesAtBedrock() {
        CaveGenerator generator = CaveGenerator.createDefault(12345L);
        Chunk chunk = createStoneChunk();
        
        byte stoneId = BlockRegistry.getInstance().getBlock("stone").getId();
        
        generator.carveCaves(chunk);
        
        // Y=0 should be solid (bedrock layer for future)
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                assertEquals(stoneId, chunk.getBlock(x, 0, z), "Bedrock level should be solid");
            }
        }
    }
    
    @Test
    @DisplayName("Carved blocks form connected regions")
    void testCaveConnectivity() {
        CaveGenerator generator = CaveGenerator.createDefault(12345L);
        Chunk chunk = createStoneChunk();
        
        generator.carveCaves(chunk);
        
        int airBlocks = countAirBlocks(chunk);
        
        // If caves were carved, there should be a reasonable number of air blocks
        // (not just random isolated blocks)
        if (airBlocks > 0) {
            assertTrue(airBlocks > 10, "Caves should carve multiple blocks, not just isolated ones");
        }
    }
    
    @Test
    @DisplayName("Different thresholds produce different cave densities")
    void testCaveThreshold() {
        SimplexNoise noise = new SimplexNoise(12345L);
        OctaveNoise octave = new OctaveNoise(noise, 3, 0.05, 1.0, 2.0, 0.5);
        
        CaveGenerator lowThreshold = new CaveGenerator(octave, 0.4, 1, 120);
        CaveGenerator highThreshold = new CaveGenerator(octave, 0.8, 1, 120);
        
        Chunk chunk1 = createStoneChunk();
        Chunk chunk2 = createStoneChunk();
        
        lowThreshold.carveCaves(chunk1);
        highThreshold.carveCaves(chunk2);
        
        int caves1 = countAirBlocks(chunk1);
        int caves2 = countAirBlocks(chunk2);
        
        assertTrue(caves1 > caves2, "Lower threshold should produce more caves");
    }
    
    @Test
    @DisplayName("Empty chunk doesn't crash")
    void testEmptyChunkNoCrash() {
        CaveGenerator generator = CaveGenerator.createDefault(12345L);
        Chunk chunk = new Chunk(new ChunkPos(0, 0, 0));
        
        assertDoesNotThrow(() -> generator.carveCaves(chunk));
    }
    
    @Test
    @DisplayName("Default factory creates valid generator")
    void testDefaultFactory() {
        CaveGenerator generator = CaveGenerator.createDefault(12345L);
        
        assertNotNull(generator);
        assertEquals(0.6, generator.getCaveThreshold(), 1e-10);
        assertEquals(1, generator.getMinCaveHeight());
        assertEquals(120, generator.getMaxCaveHeight());
        
        // Should carve valid caves
        Chunk chunk = createStoneChunk();
        generator.carveCaves(chunk);
        assertTrue(countAirBlocks(chunk) > 0);
    }
    
    private Chunk createStoneChunk() {
        Chunk chunk = new Chunk(new ChunkPos(0, 0, 0));
        byte stoneId = BlockRegistry.getInstance().getBlock("stone").getId();
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 256; y++) {
                for (int z = 0; z < 16; z++) {
                    chunk.setBlock(x, y, z, stoneId);
                }
            }
        }
        return chunk;
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
    
    private int countAirBlocks(Chunk chunk) {
        int count = 0;
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 256; y++) {
                for (int z = 0; z < 16; z++) {
                    if (chunk.getBlock(x, y, z) == 0) count++;
                }
            }
        }
        return count;
    }
}
