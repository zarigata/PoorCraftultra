package com.poorcraft.ultra.smelting;

import com.poorcraft.ultra.inventory.ItemStack;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Furnace block entity (stores furnace state).
 * CP v3.3: Smelting System
 */
public class FurnaceBlockEntity {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    private final int worldX;
    private final int worldY;
    private final int worldZ;
    
    private ItemStack inputSlot;
    private ItemStack fuelSlot;
    private ItemStack outputSlot;
    
    private int burnTime;
    private int burnTimeMax;
    private int smeltTime;
    private int smeltTimeMax;
    private SmeltingRecipe currentRecipe;
    private float accumulatedXp;
    
    public FurnaceBlockEntity(int worldX, int worldY, int worldZ) {
        this.worldX = worldX;
        this.worldY = worldY;
        this.worldZ = worldZ;
        this.inputSlot = ItemStack.empty();
        this.fuelSlot = ItemStack.empty();
        this.outputSlot = ItemStack.empty();
        this.burnTime = 0;
        this.burnTimeMax = 0;
        this.smeltTime = 0;
        this.smeltTimeMax = 200;
        this.currentRecipe = null;
        this.accumulatedXp = 0.0f;
    }
    
    @JsonCreator
    private FurnaceBlockEntity(
            @JsonProperty("worldX") int worldX,
            @JsonProperty("worldY") int worldY,
            @JsonProperty("worldZ") int worldZ,
            @JsonProperty("inputSlot") ItemStack inputSlot,
            @JsonProperty("fuelSlot") ItemStack fuelSlot,
            @JsonProperty("outputSlot") ItemStack outputSlot,
            @JsonProperty("burnTime") int burnTime,
            @JsonProperty("burnTimeMax") int burnTimeMax,
            @JsonProperty("smeltTime") int smeltTime,
            @JsonProperty("smeltTimeMax") int smeltTimeMax,
            @JsonProperty("accumulatedXp") float accumulatedXp) {
        this.worldX = worldX;
        this.worldY = worldY;
        this.worldZ = worldZ;
        this.inputSlot = inputSlot != null ? inputSlot : ItemStack.empty();
        this.fuelSlot = fuelSlot != null ? fuelSlot : ItemStack.empty();
        this.outputSlot = outputSlot != null ? outputSlot : ItemStack.empty();
        this.burnTime = burnTime;
        this.burnTimeMax = burnTimeMax;
        this.smeltTime = smeltTime;
        this.smeltTimeMax = smeltTimeMax;
        this.accumulatedXp = accumulatedXp;
        this.currentRecipe = null;
    }
    
    // Position getters
    public int getWorldX() { return worldX; }
    public int getWorldY() { return worldY; }
    public int getWorldZ() { return worldZ; }
    
    // Slot access
    public ItemStack getInputSlot() { return inputSlot; }
    public void setInputSlot(ItemStack stack) { 
        this.inputSlot = stack != null ? stack : ItemStack.empty();
    }
    
    public ItemStack getFuelSlot() { return fuelSlot; }
    public void setFuelSlot(ItemStack stack) { 
        this.fuelSlot = stack != null ? stack : ItemStack.empty();
    }
    
    public ItemStack getOutputSlot() { return outputSlot; }
    public void setOutputSlot(ItemStack stack) { 
        this.outputSlot = stack != null ? stack : ItemStack.empty();
    }
    
    /**
     * Ticks the furnace (called 20 times per second).
     */
    public void tick() {
        // Recipe matching
        if (currentRecipe == null || !currentRecipe.matches(inputSlot)) {
            currentRecipe = SmeltingRecipeRegistry.getInstance().getRecipe(inputSlot.getItemId());
            if (currentRecipe != null) {
                smeltTimeMax = currentRecipe.getSmeltTime();
            } else {
                smeltTime = 0;
            }
        }
        
        // Smelting progress
        if (currentRecipe != null && !inputSlot.isEmpty()) {
            // Try to consume fuel if needed
            if (burnTime == 0 && !fuelSlot.isEmpty()) {
                FuelRegistry fuelRegistry = FuelRegistry.getInstance();
                if (fuelRegistry.isFuel(fuelSlot.getItemId())) {
                    burnTime = fuelRegistry.getBurnTime(fuelSlot.getItemId());
                    burnTimeMax = burnTime;
                    fuelSlot = fuelSlot.shrink(1);
                }
            }
            
            // Smelt if burning
            if (burnTime > 0) {
                smeltTime++;
                
                if (smeltTime >= smeltTimeMax) {
                    // Complete smelting
                    ItemStack result = currentRecipe.getOutput().copy();
                    
                    // Add to output
                    if (outputSlot.isEmpty()) {
                        outputSlot = result;
                    } else if (outputSlot.canMergeWith(result)) {
                        outputSlot = outputSlot.grow(result.getCount());
                    } else {
                        // Output slot full or incompatible, can't complete
                        return;
                    }
                    
                    // Consume input
                    inputSlot = inputSlot.shrink(1);
                    
                    // Accumulate XP
                    accumulatedXp += currentRecipe.getXpYield();
                    
                    // Reset smelting
                    smeltTime = 0;
                    currentRecipe = null;
                }
            }
        } else {
            smeltTime = 0;
        }
        
        // Fuel consumption (after smelting to ensure exact burn time)
        if (burnTime > 0) {
            burnTime--;
        }
    }
    
    // State queries
    public int getBurnTime() {
        return burnTime;
    }
    
    public int getBurnTimeMax() {
        return burnTimeMax;
    }
    
    public int getSmeltTime() {
        return smeltTime;
    }
    
    public int getSmeltTimeMax() {
        return smeltTimeMax;
    }
    
    public boolean isBurning() {
        return burnTime > 0;
    }
    
    public boolean isSmelting() {
        return currentRecipe != null && burnTime > 0;
    }
    
    public float getBurnProgress() {
        return burnTimeMax > 0 ? (float) burnTime / burnTimeMax : 0.0f;
    }
    
    public float getSmeltProgress() {
        return smeltTimeMax > 0 ? (float) smeltTime / smeltTimeMax : 0.0f;
    }
    
    public float getAccumulatedXp() {
        return accumulatedXp;
    }
    
    public float collectXp() {
        float xp = accumulatedXp;
        accumulatedXp = 0.0f;
        return xp;
    }
    
    // Serialization
    public String toJson() {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize FurnaceBlockEntity", e);
        }
    }
    
    public static FurnaceBlockEntity fromJson(String json) {
        try {
            return MAPPER.readValue(json, FurnaceBlockEntity.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize FurnaceBlockEntity", e);
        }
    }
}
