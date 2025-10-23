package com.poorcraftultra.world.chunk;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Chunk Tests")
class ChunkTest {

    @Test
    @DisplayName("Chunk creation initializes correctly")
    void testChunkCreation() {
        ChunkPos pos = new ChunkPos(0, 0, 0);
        Chunk chunk = new Chunk(pos);
        
        assertEquals(pos, chunk.getPosition());
        assertTrue(chunk.isEmpty());
    }

    @Test
    @DisplayName("Set and get block with local coordinates")
    void testSetAndGetBlockLocal() {
        Chunk chunk = new Chunk(new ChunkPos(0, 0, 0));
        
        chunk.setBlock(8, 64, 8, (byte) 10);
        
        assertEquals(10, chunk.getBlock(8, 64, 8));
    }

    @Test
    @DisplayName("Set and get block with world coordinates")
    void testSetAndGetBlockWorld() {
        Chunk chunk = new Chunk(new ChunkPos(2, 0, 3));
        
        // World (32, 64, 48) maps to local (0, 64, 0)
        chunk.setBlockWorld(32, 64, 48, (byte) 15);
        
        assertEquals(15, chunk.getBlockWorld(32, 64, 48));
        assertEquals(15, chunk.getBlock(0, 64, 0));
    }

    @Test
    @DisplayName("Multiple sections store blocks independently")
    void testMultipleSections() {
        Chunk chunk = new Chunk(new ChunkPos(0, 0, 0));
        
        chunk.setBlock(0, 0, 0, (byte) 1);     // Section 0
        chunk.setBlock(0, 64, 0, (byte) 2);    // Section 4
        chunk.setBlock(0, 128, 0, (byte) 3);   // Section 8
        chunk.setBlock(0, 255, 0, (byte) 4);   // Section 15
        
        assertEquals(1, chunk.getBlock(0, 0, 0));
        assertEquals(2, chunk.getBlock(0, 64, 0));
        assertEquals(3, chunk.getBlock(0, 128, 0));
        assertEquals(4, chunk.getBlock(0, 255, 0));
    }

    @Test
    @DisplayName("Section boundaries are handled correctly")
    void testSectionBoundaries() {
        Chunk chunk = new Chunk(new ChunkPos(0, 0, 0));
        
        chunk.setBlock(0, 15, 0, (byte) 1);  // Top of section 0
        chunk.setBlock(0, 16, 0, (byte) 2);  // Bottom of section 1
        chunk.setBlock(0, 31, 0, (byte) 3);  // Top of section 1
        chunk.setBlock(0, 32, 0, (byte) 4);  // Bottom of section 2
        
        assertEquals(1, chunk.getBlock(0, 15, 0));
        assertEquals(2, chunk.getBlock(0, 16, 0));
        assertEquals(3, chunk.getBlock(0, 31, 0));
        assertEquals(4, chunk.getBlock(0, 32, 0));
    }

    @Test
    @DisplayName("Negative world coordinates map correctly to local coordinates")
    void testNegativeWorldCoordinates() {
        Chunk chunk = new Chunk(new ChunkPos(-1, 0, -1));
        
        // World (-1, 64, -1) should map to local (15, 64, 15)
        chunk.setBlockWorld(-1, 64, -1, (byte) 99);
        
        assertEquals(99, chunk.getBlockWorld(-1, 64, -1));
        assertEquals(99, chunk.getBlock(15, 64, 15));
    }

    @Test
    @DisplayName("Bounds checking throws IllegalArgumentException")
    void testBoundsChecking() {
        Chunk chunk = new Chunk(new ChunkPos(0, 0, 0));
        
        assertThrows(IllegalArgumentException.class, () -> chunk.getBlock(-1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> chunk.getBlock(16, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> chunk.getBlock(0, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> chunk.getBlock(0, 256, 0));
        assertThrows(IllegalArgumentException.class, () -> chunk.getBlock(0, 0, -1));
        assertThrows(IllegalArgumentException.class, () -> chunk.getBlock(0, 0, 16));
    }

    @Test
    @DisplayName("Optimize frees empty sections")
    void testOptimize() {
        Chunk chunk = new Chunk(new ChunkPos(0, 0, 0));
        
        chunk.setBlock(0, 0, 0, (byte) 1);
        chunk.setBlock(0, 64, 0, (byte) 2);
        
        assertFalse(chunk.isEmpty());
        
        chunk.setBlock(0, 0, 0, (byte) 0);
        chunk.setBlock(0, 64, 0, (byte) 0);
        
        chunk.optimize();
        
        assertTrue(chunk.isEmpty());
    }

    @Test
    @DisplayName("Memory usage is minimal for empty chunks")
    void testMemoryUsage() {
        Chunk emptyChunk = new Chunk(new ChunkPos(0, 0, 0));
        assertTrue(emptyChunk.isEmpty());
        
        Chunk oneBlockChunk = new Chunk(new ChunkPos(0, 0, 0));
        oneBlockChunk.setBlock(0, 0, 0, (byte) 1);
        assertFalse(oneBlockChunk.isEmpty());
        assertNotNull(oneBlockChunk.getSection(0));
        assertNull(oneBlockChunk.getSection(1));
        
        Chunk multiSectionChunk = new Chunk(new ChunkPos(0, 0, 0));
        for (int i = 0; i < 16; i++) {
            multiSectionChunk.setBlock(0, i * 16, 0, (byte) 1);
        }
        for (int i = 0; i < 16; i++) {
            assertNotNull(multiSectionChunk.getSection(i));
        }
    }
}
