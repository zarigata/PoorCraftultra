package com.poorcraft.ultra.crafting;

import com.poorcraft.ultra.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Crafting grid evaluator (2×2 or 3×3).
 * CP v3.2: Crafting System
 */
public class CraftingGrid {
    private final int width;
    private final int height;
    private final ItemStack[] slots;
    private ItemStack resultSlot;
    private Recipe matchedRecipe;
    private final List<CraftingListener> listeners;
    
    public CraftingGrid(int width, int height) {
        if (width < 2 || width > 3 || height < 2 || height > 3) {
            throw new IllegalArgumentException("Grid size must be 2x2 or 3x3");
        }
        this.width = width;
        this.height = height;
        this.slots = new ItemStack[width * height];
        Arrays.fill(slots, ItemStack.empty());
        this.resultSlot = ItemStack.empty();
        this.matchedRecipe = null;
        this.listeners = new ArrayList<>();
    }
    
    // Slot access
    public ItemStack getSlot(int index) {
        validateIndex(index);
        return slots[index];
    }
    
    public void setSlot(int index, ItemStack stack) {
        validateIndex(index);
        slots[index] = stack != null ? stack : ItemStack.empty();
        evaluate();
    }
    
    public ItemStack getResultSlot() {
        return resultSlot;
    }
    
    public void clear() {
        Arrays.fill(slots, ItemStack.empty());
        evaluate();
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    /**
     * Evaluates the current grid and updates the result slot.
     */
    public void evaluate() {
        RecipeRegistry registry = RecipeRegistry.getInstance();
        Recipe recipe = registry.findMatchingRecipe(slots, width, height);
        
        if (recipe != null) {
            matchedRecipe = recipe;
            resultSlot = recipe.getResult().copy();
        } else {
            matchedRecipe = null;
            resultSlot = ItemStack.empty();
        }
        
        notifyResultChanged();
    }
    
    /**
     * Crafts the current recipe, consuming ingredients and returning the result.
     */
    public ItemStack craft() {
        if (matchedRecipe == null) {
            return ItemStack.empty();
        }
        
        ItemStack result = resultSlot.copy();
        
        // Consume ingredients
        for (int i = 0; i < slots.length; i++) {
            if (!slots[i].isEmpty()) {
                slots[i] = slots[i].shrink(1);
            }
        }
        
        // Re-evaluate (may still match if multiple items)
        evaluate();
        
        return result;
    }
    
    public Recipe getMatchedRecipe() {
        return matchedRecipe;
    }
    
    // Listeners
    public void addListener(CraftingListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(CraftingListener listener) {
        listeners.remove(listener);
    }
    
    private void notifyResultChanged() {
        for (CraftingListener listener : listeners) {
            listener.onResultChanged(resultSlot);
        }
    }
    
    private void validateIndex(int index) {
        if (index < 0 || index >= slots.length) {
            throw new IllegalArgumentException("Invalid slot index: " + index);
        }
    }
}
