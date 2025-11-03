package com.poorcraft.ultra.gameplay;

import com.poorcraft.ultra.voxel.BlockType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemDropTableTest {

    private ItemDropTable dropTable;

    @BeforeEach
    void setUp() {
        dropTable = new ItemDropTable();
        dropTable.init();
    }

    @Test
    void returnsConfiguredConstantDrops() {
        List<ItemStack> drops = dropTable.getDrops(BlockType.STONE, BlockType.AIR);
        assertEquals(1, drops.size());
        ItemStack stack = drops.get(0);
        assertEquals(BlockType.STONE, stack.type());
        assertEquals(1, stack.count());
    }

    @Test
    void returnsEmptyListForUnknownBlock() {
        List<ItemStack> drops = dropTable.getDrops(BlockType.AIR, BlockType.AIR);
        assertTrue(drops.isEmpty());
    }
}
