package com.poorcraft.ultra.items;

/**
 * Enum for tool tiers (material quality) - determines mining speed and durability.
 * CP v3.1: Item System
 */
public enum ToolTier {
    WOOD(1, 2.0f, 59, 2.0f),
    STONE(2, 4.0f, 131, 3.0f),
    IRON(3, 6.0f, 250, 4.0f),
    GOLD(4, 12.0f, 32, 2.0f),      // Fast but fragile
    DIAMOND(5, 8.0f, 1561, 5.0f),
    NONE(0, 1.0f, 0, 1.0f);         // Not a tool
    
    private final int level;
    private final float miningSpeed;
    private final int durability;
    private final float attackDamage;
    
    ToolTier(int level, float miningSpeed, int durability, float attackDamage) {
        this.level = level;
        this.miningSpeed = miningSpeed;
        this.durability = durability;
        this.attackDamage = attackDamage;
    }
    
    /**
     * Returns the tier level (1-5, 0 for NONE).
     */
    public int getLevel() {
        return level;
    }
    
    /**
     * Returns the mining speed multiplier.
     */
    public float getMiningSpeed() {
        return miningSpeed;
    }
    
    /**
     * Returns the max durability (uses before breaking).
     */
    public int getDurability() {
        return durability;
    }
    
    /**
     * Returns the base attack damage (Phase 5).
     */
    public float getAttackDamage() {
        return attackDamage;
    }
    
    /**
     * Returns true if this tier can mine blocks requiring the given tier.
     */
    public boolean canMine(ToolTier requiredTier) {
        return this.level >= requiredTier.level;
    }
}
