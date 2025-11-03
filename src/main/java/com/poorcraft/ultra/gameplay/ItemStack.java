package com.poorcraft.ultra.gameplay;

import com.poorcraft.ultra.voxel.BlockType;

/**
 * Represents a stack of items in the inventory.
 */
public record ItemStack(BlockType type, int count) {

    public ItemStack {
        if (count < 0) {
            throw new IllegalArgumentException("Item count cannot be negative: " + count);
        }
        if (count > 64) {
            throw new IllegalArgumentException("Item count cannot exceed 64: " + count);
        }
        if (type == null) {
            throw new IllegalArgumentException("Item type cannot be null");
        }
    }

    public static ItemStack of(BlockType type, int count) {
        return new ItemStack(type, count);
    }

    public static ItemStack empty() {
        return new ItemStack(BlockType.AIR, 0);
    }

    public boolean isEmpty() {
        return type == BlockType.AIR || count == 0;
    }
}
