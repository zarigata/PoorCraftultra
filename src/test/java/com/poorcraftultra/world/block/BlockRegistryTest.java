package com.poorcraftultra.world.block;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BlockRegistry.
 * Note: Since BlockRegistry is a singleton, tests may affect each other.
 */
class BlockRegistryTest {

    @Test
    @DisplayName("Default blocks should be registered")
    void testDefaultBlocksRegistered() {
        BlockRegistry registry = BlockRegistry.getInstance();
        
        assertNotNull(registry.getBlock((byte) 0), "AIR should be registered");
        assertNotNull(registry.getBlock((byte) 1), "STONE should be registered");
        assertNotNull(registry.getBlock((byte) 2), "GRASS should be registered");
        assertNotNull(registry.getBlock((byte) 3), "DIRT should be registered");
        assertNotNull(registry.getBlock((byte) 4), "SAND should be registered");
        assertNotNull(registry.getBlock((byte) 5), "GLASS should be registered");
        
        assertEquals("air", registry.getBlock((byte) 0).getName());
        assertEquals("stone", registry.getBlock((byte) 1).getName());
        assertEquals("grass", registry.getBlock((byte) 2).getName());
    }

    @Test
    @DisplayName("Get block by ID should work")
    void testGetBlockById() {
        BlockRegistry registry = BlockRegistry.getInstance();
        
        Block stone = registry.getBlock((byte) 1);
        assertNotNull(stone);
        assertEquals("stone", stone.getName());
        
        // Invalid ID should return AIR
        Block invalid = registry.getBlock((byte) 100);
        assertEquals("air", invalid.getName());
    }

    @Test
    @DisplayName("Get block by name should work")
    void testGetBlockByName() {
        BlockRegistry registry = BlockRegistry.getInstance();
        
        Block stone = registry.getBlock("stone");
        assertNotNull(stone);
        assertEquals((byte) 1, stone.getId());
        
        // Invalid name should return null
        Block invalid = registry.getBlock("nonexistent");
        assertNull(invalid);
    }

    @Test
    @DisplayName("isRegistered should work correctly")
    void testIsRegistered() {
        BlockRegistry registry = BlockRegistry.getInstance();
        
        assertTrue(registry.isRegistered((byte) 0), "AIR should be registered");
        assertTrue(registry.isRegistered((byte) 1), "STONE should be registered");
        assertFalse(registry.isRegistered((byte) 100), "ID 100 should not be registered");
    }

    @Test
    @DisplayName("getAllBlocks should return all registered blocks")
    void testGetAllBlocks() {
        BlockRegistry registry = BlockRegistry.getInstance();
        
        var blocks = registry.getAllBlocks();
        assertTrue(blocks.size() >= 6, "Should have at least 6 default blocks");
        
        // Collection should be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> {
            blocks.clear();
        });
    }
}
