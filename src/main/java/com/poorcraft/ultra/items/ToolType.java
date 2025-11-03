package com.poorcraft.ultra.items;

import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

/**
 * Enum for tool types - determines mining effectiveness against blocks.
 * CP v3.1: Item System
 */
public enum ToolType {
    PICKAXE(1.5f, Set.of(
        (short) 1,   // stone
        (short) 2,   // cobblestone
        (short) 3,   // stone_bricks
        (short) 12,  // coal_ore
        (short) 13,  // iron_ore
        (short) 14,  // gold_ore
        (short) 15,  // diamond_ore
        (short) 16,  // redstone_ore
        (short) 17,  // emerald_ore
        (short) 22   // furnace
    )),
    
    AXE(1.5f, Set.of(
        (short) 8,   // oak_log
        (short) 9,   // oak_planks
        (short) 10,  // oak_leaves
        (short) 11,  // oak_wood
        (short) 21,  // crafting_table
        (short) 23   // chest
    )),
    
    SHOVEL(1.5f, Set.of(
        (short) 4,   // dirt
        (short) 5,   // grass
        (short) 6,   // sand
        (short) 7    // gravel
    )),
    
    HOE(1.0f, Set.of()),  // Used for tilling, not mining
    
    SWORD(1.5f, Set.of()), // Used for combat, not mining
    
    NONE(1.0f, Set.of());  // Not a tool
    
    private final float damageMultiplier;
    private final Set<Short> effectiveBlocks;
    
    ToolType(float damageMultiplier, Set<Short> effectiveBlocks) {
        this.damageMultiplier = damageMultiplier;
        this.effectiveBlocks = Collections.unmodifiableSet(new HashSet<>(effectiveBlocks));
    }
    
    /**
     * Returns true if this tool is effective against the given block.
     */
    public boolean isEffectiveAgainst(short blockId) {
        return effectiveBlocks.contains(blockId);
    }
    
    /**
     * Returns the set of block IDs this tool is effective against.
     */
    public Set<Short> getEffectiveBlocks() {
        return effectiveBlocks;
    }
    
    /**
     * Returns the damage multiplier for combat (Phase 5).
     */
    public float getDamageMultiplier() {
        return damageMultiplier;
    }
}
