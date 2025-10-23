package com.poorcraftultra.world.chunk;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ChunkSection Tests")
class ChunkSectionTest {

    @Test
    @DisplayName("Empty section returns air and isEmpty() is true")
    void testEmptySection() {
        ChunkSection section = new ChunkSection();
        
        assertTrue(section.isEmpty());
        assertEquals(0, section.getBlock(0, 0, 0));
        assertEquals(0, section.getBlock(15, 15, 15));
    }

    @Test
    @DisplayName("Set and get block works correctly")
    void testSetAndGetBlock() {
        ChunkSection section = new ChunkSection();
        
        section.setBlock(5, 10, 7, (byte) 42);
        
        assertEquals(42, section.getBlock(5, 10, 7));
        assertFalse(section.isEmpty());
    }

    @Test
    @DisplayName("Bounds checking throws IllegalArgumentException")
    void testBoundsChecking() {
        ChunkSection section = new ChunkSection();
        
        // Out of bounds get
        assertThrows(IllegalArgumentException.class, () -> section.getBlock(16, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> section.getBlock(0, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> section.getBlock(0, 0, 20));
        
        // Out of bounds set
        assertThrows(IllegalArgumentException.class, () -> section.setBlock(-1, 0, 0, (byte) 1));
        assertThrows(IllegalArgumentException.class, () -> section.setBlock(0, 16, 0, (byte) 1));
        assertThrows(IllegalArgumentException.class, () -> section.setBlock(0, 0, -5, (byte) 1));
    }

    @Test
    @DisplayName("Y-major indexing stores blocks correctly")
    void testYMajorIndexing() {
        ChunkSection section = new ChunkSection();
        
        section.setBlock(0, 0, 0, (byte) 1);
        section.setBlock(1, 0, 0, (byte) 2);
        section.setBlock(0, 1, 0, (byte) 3);
        section.setBlock(0, 0, 1, (byte) 4);
        
        assertEquals(1, section.getBlock(0, 0, 0));
        assertEquals(2, section.getBlock(1, 0, 0));
        assertEquals(3, section.getBlock(0, 1, 0));
        assertEquals(4, section.getBlock(0, 0, 1));
    }

    @Test
    @DisplayName("All corner blocks can be set and retrieved")
    void testAllCorners() {
        ChunkSection section = new ChunkSection();
        
        section.setBlock(0, 0, 0, (byte) 1);
        section.setBlock(15, 0, 0, (byte) 2);
        section.setBlock(0, 15, 0, (byte) 3);
        section.setBlock(0, 0, 15, (byte) 4);
        section.setBlock(15, 15, 0, (byte) 5);
        section.setBlock(15, 0, 15, (byte) 6);
        section.setBlock(0, 15, 15, (byte) 7);
        section.setBlock(15, 15, 15, (byte) 8);
        
        assertEquals(1, section.getBlock(0, 0, 0));
        assertEquals(2, section.getBlock(15, 0, 0));
        assertEquals(3, section.getBlock(0, 15, 0));
        assertEquals(4, section.getBlock(0, 0, 15));
        assertEquals(5, section.getBlock(15, 15, 0));
        assertEquals(6, section.getBlock(15, 0, 15));
        assertEquals(7, section.getBlock(0, 15, 15));
        assertEquals(8, section.getBlock(15, 15, 15));
    }

    @Test
    @DisplayName("Block count tracks non-air blocks correctly")
    void testBlockCount() {
        ChunkSection section = new ChunkSection();
        
        assertEquals(0, section.getBlockCount());
        
        section.setBlock(0, 0, 0, (byte) 1);
        section.setBlock(1, 0, 0, (byte) 2);
        section.setBlock(2, 0, 0, (byte) 3);
        section.setBlock(3, 0, 0, (byte) 4);
        section.setBlock(4, 0, 0, (byte) 5);
        
        assertEquals(5, section.getBlockCount());
        
        section.setBlock(1, 0, 0, (byte) 0);
        
        assertEquals(4, section.getBlockCount());
    }

    @Test
    @DisplayName("Optimize frees memory when all blocks are air")
    void testOptimize() {
        ChunkSection section = new ChunkSection();
        
        section.setBlock(5, 5, 5, (byte) 10);
        section.setBlock(10, 10, 10, (byte) 20);
        
        assertFalse(section.isEmpty());
        
        section.setBlock(5, 5, 5, (byte) 0);
        section.setBlock(10, 10, 10, (byte) 0);
        
        section.optimize();
        
        assertTrue(section.isEmpty());
    }

    @Test
    @DisplayName("Memory footprint is minimal for empty sections")
    void testMemoryFootprint() {
        ChunkSection emptySection = new ChunkSection();
        assertTrue(emptySection.isEmpty());
        
        ChunkSection filledSection = new ChunkSection();
        filledSection.setBlock(0, 0, 0, (byte) 1);
        assertFalse(filledSection.isEmpty());
    }
}
