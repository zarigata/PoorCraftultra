package com.poorcraft.ultra.crafting;

import com.poorcraft.ultra.inventory.PlayerInventory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Recipe book manager (tracks discovered recipes).
 * CP v3.2: Crafting System
 */
public class RecipeBook {
    private static final Logger LOGGER = Logger.getLogger(RecipeBook.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    private Set<String> discoveredRecipes;
    private Set<String> pinnedRecipes;
    
    public RecipeBook() {
        this.discoveredRecipes = new HashSet<>();
        this.pinnedRecipes = new HashSet<>();
    }
    
    @JsonCreator
    public RecipeBook(
            @JsonProperty("discovered") Set<String> discoveredRecipes,
            @JsonProperty("pinned") Set<String> pinnedRecipes) {
        this.discoveredRecipes = discoveredRecipes != null ? discoveredRecipes : new HashSet<>();
        this.pinnedRecipes = pinnedRecipes != null ? pinnedRecipes : new HashSet<>();
    }
    
    /**
     * Marks a recipe as discovered.
     */
    public void discoverRecipe(String recipeName) {
        if (discoveredRecipes.add(recipeName)) {
            LOGGER.info("Discovered recipe: " + recipeName);
        }
    }
    
    public boolean isDiscovered(String recipeName) {
        return discoveredRecipes.contains(recipeName);
    }
    
    public Set<String> getDiscoveredRecipes() {
        return new HashSet<>(discoveredRecipes);
    }
    
    /**
     * Pins a recipe for quick access.
     */
    public void pinRecipe(String recipeName) {
        pinnedRecipes.add(recipeName);
    }
    
    public void unpinRecipe(String recipeName) {
        pinnedRecipes.remove(recipeName);
    }
    
    public boolean isPinned(String recipeName) {
        return pinnedRecipes.contains(recipeName);
    }
    
    public Set<String> getPinnedRecipes() {
        return new HashSet<>(pinnedRecipes);
    }
    
    /**
     * Checks if player has ingredients for new recipes and auto-discovers them.
     */
    public void checkDiscovery(PlayerInventory inventory) {
        RecipeRegistry registry = RecipeRegistry.getInstance();
        
        for (Recipe recipe : registry.getAllRecipes()) {
            if (isDiscovered(recipe.getName())) {
                continue;
            }
            
            // Check if player has all ingredients
            boolean hasAllIngredients = true;
            
            if (recipe.isShaped()) {
                // Check shaped recipe ingredients
                Set<String> requiredItems = new HashSet<>(recipe.getKey().values());
                for (String itemName : requiredItems) {
                    if (!hasItemByName(inventory, itemName)) {
                        hasAllIngredients = false;
                        break;
                    }
                }
            } else {
                // Check shapeless recipe ingredients
                for (String itemName : recipe.getIngredients()) {
                    if (!hasItemByName(inventory, itemName)) {
                        hasAllIngredients = false;
                        break;
                    }
                }
            }
            
            if (hasAllIngredients) {
                discoverRecipe(recipe.getName());
            }
        }
    }
    
    private boolean hasItemByName(PlayerInventory inventory, String itemName) {
        for (int i = 0; i < 36; i++) {
            if (!inventory.getStack(i).isEmpty() && 
                inventory.getStack(i).getItem().getName().equals(itemName)) {
                return true;
            }
        }
        return false;
    }
    
    // Serialization
    public String toJson() {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize RecipeBook", e);
        }
    }
    
    public static RecipeBook fromJson(String json) {
        try {
            return MAPPER.readValue(json, RecipeBook.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize RecipeBook", e);
        }
    }
}
