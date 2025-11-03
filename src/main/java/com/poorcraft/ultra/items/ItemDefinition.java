package com.poorcraft.ultra.items;

/**
 * Immutable data class for item properties.
 * CP v3.1: Item System
 */
public class ItemDefinition {
    private final int id;
    private final String name;
    private final String displayName;
    private final String icon;
    private final int maxStackSize;
    private final int durability;
    private final ToolType toolType;
    private final ToolTier toolTier;
    private final Short placeableBlock;
    private final int fuelBurnTime;
    private final int foodValue;
    
    private ItemDefinition(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.displayName = builder.displayName;
        this.icon = builder.icon;
        this.maxStackSize = builder.maxStackSize;
        this.durability = builder.durability;
        this.toolType = builder.toolType;
        this.toolTier = builder.toolTier;
        this.placeableBlock = builder.placeableBlock;
        this.fuelBurnTime = builder.fuelBurnTime;
        this.foodValue = builder.foodValue;
        
        validate();
    }
    
    private void validate() {
        // Special case for AIR (ID 0) - allow maxStackSize=0
        if (id == 0) {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("Item name cannot be empty");
            }
            if (icon == null || icon.isEmpty()) {
                throw new IllegalArgumentException("Item icon cannot be empty");
            }
            return;
        }
        
        if (id <= 0) {
            throw new IllegalArgumentException("Item ID must be positive: " + id);
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Item name cannot be empty");
        }
        if (icon == null || icon.isEmpty()) {
            throw new IllegalArgumentException("Item icon cannot be empty");
        }
        if (maxStackSize <= 0) {
            throw new IllegalArgumentException("Max stack size must be positive: " + maxStackSize);
        }
        if (isTool() && durability <= 0) {
            throw new IllegalArgumentException("Tools must have positive durability: " + name);
        }
        if (isTool() && maxStackSize != 1) {
            throw new IllegalArgumentException("Tools must have maxStackSize=1: " + name);
        }
    }
    
    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public String getIcon() { return icon; }
    public int getMaxStackSize() { return maxStackSize; }
    public int getDurability() { return durability; }
    public ToolType getToolType() { return toolType; }
    public ToolTier getToolTier() { return toolTier; }
    public Short getPlaceableBlock() { return placeableBlock; }
    public int getFuelBurnTime() { return fuelBurnTime; }
    public int getFoodValue() { return foodValue; }
    
    // Type checks
    public boolean isTool() { return toolType != ToolType.NONE; }
    public boolean isPlaceable() { return placeableBlock != null; }
    public boolean isFuel() { return fuelBurnTime > 0; }
    public boolean isFood() { return foodValue > 0; }
    
    // Builder
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private int id;
        private String name;
        private String displayName;
        private String icon;
        private int maxStackSize = 64;
        private int durability = 0;
        private ToolType toolType = ToolType.NONE;
        private ToolTier toolTier = ToolTier.NONE;
        private Short placeableBlock = null;
        private int fuelBurnTime = 0;
        private int foodValue = 0;
        
        public Builder id(int id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder displayName(String displayName) { this.displayName = displayName; return this; }
        public Builder icon(String icon) { this.icon = icon; return this; }
        public Builder maxStackSize(int maxStackSize) { this.maxStackSize = maxStackSize; return this; }
        public Builder durability(int durability) { this.durability = durability; return this; }
        public Builder toolType(ToolType toolType) { this.toolType = toolType; return this; }
        public Builder toolTier(ToolTier toolTier) { this.toolTier = toolTier; return this; }
        public Builder placeableBlock(short placeableBlock) { this.placeableBlock = placeableBlock; return this; }
        public Builder fuelBurnTime(int fuelBurnTime) { this.fuelBurnTime = fuelBurnTime; return this; }
        public Builder foodValue(int foodValue) { this.foodValue = foodValue; return this; }
        
        public ItemDefinition build() {
            return new ItemDefinition(this);
        }
    }
    
    @Override
    public String toString() {
        return "ItemDefinition{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                '}';
    }
}
