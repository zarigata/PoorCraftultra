package com.poorcraft.ultra.save;

import com.poorcraft.ultra.inventory.PlayerInventory;
import com.poorcraft.ultra.crafting.RecipeBook;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Player data storage class (for save/load).
 * CP v3.1: Inventory System
 */
public class PlayerData {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    private PlayerInventory inventory;
    private int selectedSlot;
    private RecipeBook recipeBook;
    private float xp;
    private float health;
    private float hunger;
    
    public PlayerData() {
        this.inventory = new PlayerInventory();
        this.selectedSlot = 0;
        this.recipeBook = new RecipeBook();
        this.xp = 0.0f;
        this.health = 20.0f;
        this.hunger = 20.0f;
    }
    
    @JsonCreator
    public PlayerData(
            @JsonProperty("inventory") PlayerInventory inventory,
            @JsonProperty("selectedSlot") int selectedSlot,
            @JsonProperty("recipeBook") RecipeBook recipeBook,
            @JsonProperty("xp") float xp,
            @JsonProperty("health") float health,
            @JsonProperty("hunger") float hunger) {
        this.inventory = inventory != null ? inventory : new PlayerInventory();
        this.selectedSlot = selectedSlot;
        this.recipeBook = recipeBook != null ? recipeBook : new RecipeBook();
        this.xp = xp;
        this.health = health;
        this.hunger = hunger;
    }
    
    public PlayerData(PlayerInventory inventory) {
        this();
        this.inventory = inventory;
        this.selectedSlot = inventory.getSelectedSlot();
    }
    
    // Getters and setters
    public PlayerInventory getInventory() {
        return inventory;
    }
    
    public void setInventory(PlayerInventory inventory) {
        this.inventory = inventory;
    }
    
    public int getSelectedSlot() {
        return selectedSlot;
    }
    
    public void setSelectedSlot(int selectedSlot) {
        this.selectedSlot = selectedSlot;
    }
    
    public RecipeBook getRecipeBook() {
        return recipeBook;
    }
    
    public void setRecipeBook(RecipeBook recipeBook) {
        this.recipeBook = recipeBook;
    }
    
    public float getXp() {
        return xp;
    }
    
    public void setXp(float xp) {
        this.xp = xp;
    }
    
    public float getHealth() {
        return health;
    }
    
    public void setHealth(float health) {
        this.health = health;
    }
    
    public float getHunger() {
        return hunger;
    }
    
    public void setHunger(float hunger) {
        this.hunger = hunger;
    }
    
    // Serialization
    public String toJson() {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize PlayerData", e);
        }
    }
    
    public static PlayerData fromJson(String json) {
        try {
            return MAPPER.readValue(json, PlayerData.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize PlayerData", e);
        }
    }
}
