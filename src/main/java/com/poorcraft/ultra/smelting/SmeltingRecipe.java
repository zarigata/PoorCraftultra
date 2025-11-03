package com.poorcraft.ultra.smelting;

import com.poorcraft.ultra.inventory.ItemStack;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Smelting recipe data class.
 * CP v3.3: Smelting System
 */
public class SmeltingRecipe {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    private final String name;
    private final int input;
    private final ItemStack output;
    private final int smeltTime;
    private final float xpYield;
    
    @JsonCreator
    public SmeltingRecipe(
            @JsonProperty("name") String name,
            @JsonProperty("input") int input,
            @JsonProperty("output") ItemStack output,
            @JsonProperty("smeltTime") int smeltTime,
            @JsonProperty("xpYield") float xpYield) {
        this.name = name;
        this.input = input;
        this.output = output;
        this.smeltTime = smeltTime > 0 ? smeltTime : 200;  // Default 200 ticks
        this.xpYield = xpYield;
    }
    
    // Getters
    public String getName() { return name; }
    public int getInput() { return input; }
    public ItemStack getOutput() { return output; }
    public int getSmeltTime() { return smeltTime; }
    public float getXpYield() { return xpYield; }
    
    /**
     * Returns true if the input stack matches this recipe.
     */
    public boolean matches(ItemStack inputStack) {
        return !inputStack.isEmpty() && inputStack.getItemId() == input;
    }
    
    // Serialization
    public String toJson() {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize SmeltingRecipe", e);
        }
    }
    
    public static SmeltingRecipe fromJson(String json) {
        try {
            return MAPPER.readValue(json, SmeltingRecipe.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize SmeltingRecipe", e);
        }
    }
    
    @Override
    public String toString() {
        return "SmeltingRecipe{name='" + name + "', input=" + input + ", output=" + output + "}";
    }
}
