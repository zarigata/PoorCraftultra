package com.poorcraftultra.world.chunk;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ChunkPos Tests")
class ChunkPosTest {

    @Test
    @DisplayName("Constructor and getters work correctly")
    void testConstructorAndGetters() {
        ChunkPos pos = new ChunkPos(1, 2, 3);
        
        assertEquals(1, pos.getX());
        assertEquals(2, pos.getY());
        assertEquals(3, pos.getZ());
    }

    @Test
    @DisplayName("Equals and hashCode work correctly")
    void testEqualsAndHashCode() {
        ChunkPos pos1 = new ChunkPos(5, 10, 15);
        ChunkPos pos2 = new ChunkPos(5, 10, 15);
        ChunkPos pos3 = new ChunkPos(5, 10, 16);
        
        // Same values should be equal
        assertEquals(pos1, pos2);
        assertEquals(pos1.hashCode(), pos2.hashCode());
        
        // Different values should not be equal
        assertNotEquals(pos1, pos3);
    }

    @Test
    @DisplayName("fromWorldPos converts positive coordinates correctly")
    void testFromWorldPosPositive() {
        // Origin
        ChunkPos pos = ChunkPos.fromWorldPos(0, 0, 0);
        assertEquals(new ChunkPos(0, 0, 0), pos);
        
        // Within first chunk
        pos = ChunkPos.fromWorldPos(15, 15, 15);
        assertEquals(new ChunkPos(0, 0, 0), pos);
        
        // Exactly at chunk boundary
        pos = ChunkPos.fromWorldPos(16, 0, 0);
        assertEquals(new ChunkPos(1, 0, 0), pos);
        
        // Multiple chunks away
        pos = ChunkPos.fromWorldPos(31, 31, 31);
        assertEquals(new ChunkPos(1, 0, 1), pos);
    }

    @Test
    @DisplayName("fromWorldPos converts negative coordinates correctly using floor division")
    void testFromWorldPosNegative() {
        // Just below origin
        ChunkPos pos = ChunkPos.fromWorldPos(-1, -1, -1);
        assertEquals(new ChunkPos(-1, -1, -1), pos);
        
        // Exactly at negative chunk boundary
        pos = ChunkPos.fromWorldPos(-16, -16, -16);
        assertEquals(new ChunkPos(-1, -1, -1), pos);
        
        // Beyond negative chunk boundary
        pos = ChunkPos.fromWorldPos(-17, -17, -17);
        assertEquals(new ChunkPos(-2, -1, -2), pos);
    }

    @Test
    @DisplayName("toWorldPos converts chunk coordinates to world coordinates")
    void testToWorldPos() {
        // Positive chunk
        ChunkPos pos = new ChunkPos(2, 3, 4);
        int[] worldPos = pos.toWorldPos();
        assertArrayEquals(new int[]{32, 768, 64}, worldPos);
        
        // Negative chunk
        pos = new ChunkPos(-1, -1, -1);
        worldPos = pos.toWorldPos();
        assertArrayEquals(new int[]{-16, -256, -16}, worldPos);
        
        // Origin
        pos = new ChunkPos(0, 0, 0);
        worldPos = pos.toWorldPos();
        assertArrayEquals(new int[]{0, 0, 0}, worldPos);
    }

    @Test
    @DisplayName("ChunkPos works correctly as HashMap key")
    void testHashMapKey() {
        HashMap<ChunkPos, String> map = new HashMap<>();
        
        ChunkPos pos1 = new ChunkPos(1, 2, 3);
        ChunkPos pos2 = new ChunkPos(4, 5, 6);
        ChunkPos pos3 = new ChunkPos(1, 2, 3); // Same as pos1
        
        map.put(pos1, "First");
        map.put(pos2, "Second");
        
        // Should retrieve using equal key
        assertEquals("First", map.get(pos3));
        assertEquals("Second", map.get(pos2));
        
        // Should have 2 entries (pos1 and pos3 are the same key)
        assertEquals(2, map.size());
    }
}
