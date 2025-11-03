package com.poorcraft.ultra.smelting;

import com.poorcraft.ultra.inventory.ItemStack;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Registry for smelting recipes.
 * CP v3.3: Smelting System
 */
public class SmeltingRecipeRegistry {
    private static final Logger LOGGER = Logger.getLogger(SmeltingRecipeRegistry.class.getName());
    private static SmeltingRecipeRegistry instance;
    
    private final Map<Integer, SmeltingRecipe> recipes = new HashMap<>();
    
    private SmeltingRecipeRegistry() {
        init();
    }
    
    public static SmeltingRecipeRegistry getInstance() {
        if (instance == null) {
            instance = new SmeltingRecipeRegistry();
        }
        return instance;
    }
    
    private void init() {
        LOGGER.info("Initializing SmeltingRecipeRegistry...");
        
        // Register basic smelting recipes
        register(new SmeltingRecipe("iron_ingot", 1071, ItemStack.of(1027, 1), 200, 0.7f));
        register(new SmeltingRecipe("gold_ingot", 1072, ItemStack.of(1028, 1), 200, 1.0f));
        register(new SmeltingRecipe("cooked_meat", 1052, ItemStack.of(1053, 1), 200, 0.35f));
        register(new SmeltingRecipe("glass", 1065, ItemStack.of(1080, 1), 200, 0.1f));
        
        LOGGER.info("SmeltingRecipeRegistry initialized: " + recipes.size() + " recipes");
    }
    
    public void register(SmeltingRecipe recipe) {
        recipes.put(recipe.getInput(), recipe);
        LOGGER.fine("Loaded smelting recipe: " + recipe.getName());
    }
    
    public SmeltingRecipe getRecipe(int inputItemId) {
        return recipes.get(inputItemId);
    }
    
    public Collection<SmeltingRecipe> getAllRecipes() {
        return recipes.values();
    }
}
