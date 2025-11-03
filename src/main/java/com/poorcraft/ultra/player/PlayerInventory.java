package com.poorcraft.ultra.player;

import com.poorcraft.ultra.voxel.BlockType;
import java.util.EnumMap;
import java.util.Map;

public class PlayerInventory {
    private static final int HOTBAR_SIZE = 9;

    private final BlockType[] hotbar = new BlockType[HOTBAR_SIZE];
    private final Map<BlockType, Integer> counts = new EnumMap<>(BlockType.class);
    private int selectedSlot;

    public void init() {
        for (BlockType type : BlockType.values()) {
            counts.put(type, 0);
        }

        hotbar[0] = BlockType.STONE;
        hotbar[1] = BlockType.DIRT;
        hotbar[2] = BlockType.GRASS;
        hotbar[3] = BlockType.WOOD_OAK;
        hotbar[4] = BlockType.LEAVES_OAK;
        for (int i = 5; i < HOTBAR_SIZE; i++) {
            hotbar[i] = BlockType.AIR;
        }

        for (int i = 0; i < HOTBAR_SIZE; i++) {
            if (hotbar[i] != BlockType.AIR) {
                counts.put(hotbar[i], 64);
            }
        }
        selectedSlot = 0;
    }

    public BlockType getSelectedBlock() {
        return hotbar[selectedSlot];
    }

    public void selectSlot(int slot) {
        if (slot < 0 || slot >= HOTBAR_SIZE) {
            throw new IllegalArgumentException("Hotbar slot out of range: " + slot);
        }
        selectedSlot = slot;
    }

    public void addBlock(BlockType type, int amount) {
        counts.merge(type, amount, (current, added) -> {
            int next = current + added;
            if (next > 64) {
                return 64;
            }
            return Math.max(next, 0);
        });
    }

    public boolean removeBlock(BlockType type, int amount) {
        int current = counts.getOrDefault(type, 0);
        if (current < amount) {
            return false;
        }
        counts.put(type, current - amount);
        return true;
    }

    public boolean canPlace(BlockType type) {
        return counts.getOrDefault(type, 0) > 0;
    }

    public int getCount(BlockType type) {
        return counts.getOrDefault(type, 0);
    }

    @Override
    public String toString() {
        return "PlayerInventory{" +
            "selectedSlot=" + selectedSlot +
            ", selectedBlock=" + getSelectedBlock() +
            ", counts=" + counts +
            '}';
    }
}
