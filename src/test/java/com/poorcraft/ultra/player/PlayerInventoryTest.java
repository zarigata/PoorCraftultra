package com.poorcraft.ultra.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.poorcraft.ultra.voxel.BlockType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlayerInventoryTest {

    private PlayerInventory inventory;

    @BeforeEach
    void setUp() {
        inventory = new PlayerInventory();
        inventory.init();
    }

    @Test
    void testInitialization() {
        assertEquals(BlockType.STONE, inventory.getSelectedBlock(), "Slot 0 should be STONE by default");
        assertEquals(64, inventory.getCount(BlockType.STONE));
        assertEquals(64, inventory.getCount(BlockType.DIRT));
        assertEquals(64, inventory.getCount(BlockType.GRASS));
        assertEquals(64, inventory.getCount(BlockType.WOOD_OAK));
        assertEquals(64, inventory.getCount(BlockType.LEAVES_OAK));
        assertEquals(0, inventory.getCount(BlockType.AIR));
    }

    @Test
    void testAddRemove() {
        inventory.addBlock(BlockType.STONE, 10);
        assertEquals(74, inventory.getCount(BlockType.STONE));

        assertTrue(inventory.removeBlock(BlockType.STONE, 5));
        assertEquals(69, inventory.getCount(BlockType.STONE));

        assertFalse(inventory.removeBlock(BlockType.STONE, 100), "Removing more than available should fail");
        assertEquals(69, inventory.getCount(BlockType.STONE), "Failed removal must not change count");
    }

    @Test
    void testSelectSlotBounds() {
        inventory.selectSlot(0);
        assertEquals(BlockType.STONE, inventory.getSelectedBlock());

        inventory.selectSlot(8);
        assertEquals(BlockType.AIR, inventory.getSelectedBlock());

        assertThrows(IllegalArgumentException.class, () -> inventory.selectSlot(-1));
        assertThrows(IllegalArgumentException.class, () -> inventory.selectSlot(9));
    }

    @Test
    void testCanPlace() {
        assertTrue(inventory.canPlace(BlockType.STONE));

        assertTrue(inventory.removeBlock(BlockType.STONE, 64));
        assertFalse(inventory.canPlace(BlockType.STONE));
    }

    @Test
    void testPlaceBreakCycle() {
        int initial = inventory.getCount(BlockType.STONE);

        assertTrue(inventory.removeBlock(BlockType.STONE, 1));
        assertEquals(initial - 1, inventory.getCount(BlockType.STONE));

        inventory.addBlock(BlockType.STONE, 1);
        assertEquals(initial, inventory.getCount(BlockType.STONE));
    }
}
