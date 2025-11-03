package com.poorcraft.ultra.gameplay;

import com.poorcraft.ultra.player.PlayerInventory;
import com.poorcraft.ultra.voxel.BlockType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CraftingManagerTest {

    private CraftingManager craftingManager;
    private PlayerInventory inventory;

    @BeforeEach
    void setUp() {
        craftingManager = new CraftingManager();
        craftingManager.init();

        inventory = new PlayerInventory();
        inventory.init();
    }

    @Test
    void findsChestRecipeWithFourPlanks() {
        List<BlockType> inputs = List.of(
            BlockType.PLANKS,
            BlockType.PLANKS,
            BlockType.PLANKS,
            BlockType.PLANKS
        );

        assertTrue(craftingManager.findRecipe(inputs).isPresent(), "Chest recipe should match four planks");
    }

    @Test
    void craftingConsumesInputsAndAddsOutput() {
        inventory.addBlock(BlockType.PLANKS, 16);

        List<BlockType> inputs = List.of(
            BlockType.PLANKS,
            BlockType.PLANKS,
            BlockType.PLANKS,
            BlockType.PLANKS
        );

        ItemStack result = craftingManager.craft(inputs, inventory);
        assertNotNull(result);
        assertEquals(BlockType.CHEST, result.type());
        assertEquals(1, result.count());
        assertEquals(16 - 4, inventory.getCount(BlockType.PLANKS));
        assertEquals(1, inventory.getCount(BlockType.CHEST));
    }

    @Test
    void craftingFailsWhenInventoryMissingItems() {
        inventory.addBlock(BlockType.PLANKS, 3);
        List<BlockType> inputs = List.of(
            BlockType.PLANKS,
            BlockType.PLANKS,
            BlockType.PLANKS,
            BlockType.PLANKS
        );

        assertNull(craftingManager.craft(inputs, inventory));
        assertEquals(3, inventory.getCount(BlockType.PLANKS), "Counts should remain unchanged on failure");
    }
}
