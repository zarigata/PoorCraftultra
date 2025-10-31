package com.poorcraft.ultra.gameplay;

import com.poorcraft.ultra.voxel.BlockType;

/**
 * Immutable representation of an inventory stack.
 */
public record ItemStack(BlockType blockType, int count) {

    private static final int MIN_STACK_SIZE = 1;
    private static final int MAX_STACK_SIZE = 64;

    public ItemStack {
        if (blockType == null) {
            throw new NullPointerException("blockType");
        }
        if (count < MIN_STACK_SIZE || count > MAX_STACK_SIZE) {
            throw new IllegalArgumentException(
                    "count must be within [" + MIN_STACK_SIZE + ", " + MAX_STACK_SIZE + "] but was " + count);
        }
    }

    public static ItemStack of(BlockType blockType) {
        return of(blockType, MIN_STACK_SIZE);
    }

    public static ItemStack of(BlockType blockType, int count) {
        return new ItemStack(blockType, count);
    }

    public ItemStack withCount(int newCount) {
        return new ItemStack(blockType, newCount);
    }

    public ItemStack increment(int amount) {
        if (amount <= 0) {
            return this;
        }
        int newCount = Math.min(count + amount, MAX_STACK_SIZE);
        return new ItemStack(blockType, newCount);
    }

    public ItemStack decrement(int amount) {
        if (amount <= 0) {
            return this;
        }
        int newCount = Math.max(count - amount, MIN_STACK_SIZE);
        return new ItemStack(blockType, newCount);
    }

    public boolean canStackWith(ItemStack other) {
        return other != null && blockType == other.blockType;
    }

    public boolean isFull() {
        return count >= MAX_STACK_SIZE;
    }

    public int getRemainingSpace() {
        return MAX_STACK_SIZE - count;
    }
}
