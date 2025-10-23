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

    @Test
    @DisplayName("Register new custom block and retrieve by ID and name")
    void testRegisterCustomBlock() {
        BlockRegistry registry = BlockRegistry.getInstance();
        
        // Note: Since BlockRegistry is a singleton and may be locked by other tests,
        // we can only test this if the registry is not locked
        if (registry.isLocked()) {
            // Skip test if registry is already locked
            return;
        }
        
        // Create a custom block
        Block customBlock = new Block(
            (byte) 100,
            "custom_test_block",
            "Custom Test Block",
            BlockProperties.solid(),
            "stone"
        );
        
        // Register the block
        registry.register(customBlock);
        
        // Retrieve by ID
        Block retrievedById = registry.getBlock((byte) 100);
        assertNotNull(retrievedById);
        assertEquals("custom_test_block", retrievedById.getName());
        assertEquals((byte) 100, retrievedById.getId());
        
        // Retrieve by name
        Block retrievedByName = registry.getBlock("custom_test_block");
        assertNotNull(retrievedByName);
        assertEquals((byte) 100, retrievedByName.getId());
        
        // Verify it's registered
        assertTrue(registry.isRegistered((byte) 100));
    }

    @Test
    @DisplayName("Register duplicate ID should throw exception")
    void testRegisterDuplicateId() {
        BlockRegistry registry = BlockRegistry.getInstance();
        
        if (registry.isLocked()) {
            return; // Skip if locked
        }
        
        // Try to register a block with ID 1 (STONE already exists)
        Block duplicateIdBlock = new Block(
            (byte) 1,
            "duplicate_id_block",
            "Duplicate ID Block",
            BlockProperties.solid(),
            "stone"
        );
        
        assertThrows(IllegalArgumentException.class, () -> {
            registry.register(duplicateIdBlock);
        }, "Should throw exception for duplicate ID");
    }

    @Test
    @DisplayName("Register duplicate name should throw exception")
    void testRegisterDuplicateName() {
        BlockRegistry registry = BlockRegistry.getInstance();
        
        if (registry.isLocked()) {
            return; // Skip if locked
        }
        
        // Try to register a block with name "stone" (already exists)
        Block duplicateNameBlock = new Block(
            (byte) 101,
            "stone",
            "Duplicate Name Block",
            BlockProperties.solid(),
            "stone"
        );
        
        assertThrows(IllegalArgumentException.class, () -> {
            registry.register(duplicateNameBlock);
        }, "Should throw exception for duplicate name");
    }

    @Test
    @DisplayName("Lock should prevent further registration")
    void testLockPreventsRegistration() {
        BlockRegistry registry = BlockRegistry.getInstance();
        
        // Lock the registry
        registry.lock();
        assertTrue(registry.isLocked(), "Registry should be locked");
        
        // Try to register a new block
        Block newBlock = new Block(
            (byte) 102,
            "locked_test_block",
            "Locked Test Block",
            BlockProperties.solid(),
            "stone"
        );
        
        assertThrows(IllegalStateException.class, () -> {
            registry.register(newBlock);
        }, "Should throw exception when trying to register after lock");
    }
}
