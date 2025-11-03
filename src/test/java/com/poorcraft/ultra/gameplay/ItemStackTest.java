package com.poorcraft.ultra.gameplay;

import com.poorcraft.ultra.voxel.BlockType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemStackTest {

    @Test
    void constructorRejectsNegativeCount() {
        assertThrows(IllegalArgumentException.class, () -> new ItemStack(BlockType.PLANKS, -1));
    }

    @Test
    void constructorRejectsCountOverSixtyFour() {
        assertThrows(IllegalArgumentException.class, () -> new ItemStack(BlockType.PLANKS, 65));
    }

    @Test
    void emptyFactoryProducesAirWithZeroCount() {
        ItemStack empty = ItemStack.empty();
        assertEquals(BlockType.AIR, empty.type());
        assertEquals(0, empty.count());
    }

    @Test
    void isEmptyMatchesTypeOrCount() {
        assertTrue(ItemStack.empty().isEmpty());
        assertTrue(new ItemStack(BlockType.AIR, 0).isEmpty());
        assertTrue(new ItemStack(BlockType.AIR, 5).isEmpty());
        assertTrue(new ItemStack(BlockType.PLANKS, 0).isEmpty());
        assertFalse(new ItemStack(BlockType.PLANKS, 1).isEmpty());
    }
}
