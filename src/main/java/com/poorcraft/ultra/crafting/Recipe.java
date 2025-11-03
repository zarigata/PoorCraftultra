package com.poorcraft.ultra.crafting;

import com.poorcraft.ultra.inventory.ItemStack;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

/**
 * Recipe data class.
 * CP v3.2: Crafting System
 */
public class Recipe {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    private final String name;
    private final RecipeType type;
    private final String[] pattern;
    private final Map<Character, String> key;
    private final List<String> ingredients;
    private final ItemStack result;
    private final int width;
    private final int height;
    
    @JsonCreator
    private Recipe(
            @JsonProperty("name") String name,
            @JsonProperty("type") RecipeType type,
            @JsonProperty("pattern") String[] pattern,
            @JsonProperty("key") Map<Character, String> key,
            @JsonProperty("ingredients") List<String> ingredients,
            @JsonProperty("result") ItemStack result) {
        this.name = name;
        this.type = type;
        this.pattern = pattern;
        this.key = key;
        this.ingredients = ingredients;
        this.result = result;
        
        if (type == RecipeType.SHAPED && pattern != null) {
            this.height = pattern.length;
            this.width = pattern.length > 0 ? pattern[0].length() : 0;
        } else {
            this.width = 0;
            this.height = 0;
        }
    }
    
    // Getters
    public String getName() { return name; }
    public RecipeType getType() { return type; }
    public String[] getPattern() { return pattern; }
    public Map<Character, String> getKey() { return key; }
    public List<String> getIngredients() { return ingredients; }
    public ItemStack getResult() { return result; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    
    public boolean isShaped() { return type == RecipeType.SHAPED; }
    public boolean isShapeless() { return type == RecipeType.SHAPELESS; }
    
    /**
     * Checks if the given grid matches this recipe.
     */
    public boolean matches(ItemStack[] grid, int gridWidth, int gridHeight) {
        if (type == RecipeType.SHAPED) {
            return matchesShaped(grid, gridWidth, gridHeight);
        } else {
            return matchesShapeless(grid);
        }
    }
    
    private boolean matchesShaped(ItemStack[] grid, int gridWidth, int gridHeight) {
        // Recipe must fit in grid
        if (width > gridWidth || height > gridHeight) {
            return false;
        }
        
        // Try all possible positions in the grid
        for (int offsetY = 0; offsetY <= gridHeight - height; offsetY++) {
            for (int offsetX = 0; offsetX <= gridWidth - width; offsetX++) {
                if (matchesShapedAt(grid, gridWidth, offsetX, offsetY)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private boolean matchesShapedAt(ItemStack[] grid, int gridWidth, int offsetX, int offsetY) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                char patternChar = pattern[y].charAt(x);
                int gridIndex = (offsetY + y) * gridWidth + (offsetX + x);
                ItemStack gridStack = grid[gridIndex];
                
                if (patternChar == ' ') {
                    // Empty slot required
                    if (!gridStack.isEmpty()) {
                        return false;
                    }
                } else {
                    // Item required
                    String requiredItemName = key.get(patternChar);
                    if (requiredItemName == null) {
                        return false;
                    }
                    
                    if (gridStack.isEmpty()) {
                        return false;
                    }
                    
                    String gridItemName = gridStack.getItem().getName();
                    if (!gridItemName.equals(requiredItemName)) {
                        return false;
                    }
                }
            }
        }
        
        // Check that all other grid slots are empty
        for (int i = 0; i < grid.length; i++) {
            int gridX = i % gridWidth;
            int gridY = i / gridWidth;
            
            boolean inRecipeArea = gridX >= offsetX && gridX < offsetX + width &&
                                   gridY >= offsetY && gridY < offsetY + height;
            
            if (!inRecipeArea && !grid[i].isEmpty()) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean matchesShapeless(ItemStack[] grid) {
        Map<String, Integer> required = new HashMap<>();
        for (String ingredient : ingredients) {
            required.put(ingredient, required.getOrDefault(ingredient, 0) + 1);
        }
        
        Map<String, Integer> provided = new HashMap<>();
        for (ItemStack stack : grid) {
            if (!stack.isEmpty()) {
                String itemName = stack.getItem().getName();
                provided.put(itemName, provided.getOrDefault(itemName, 0) + 1);
            }
        }
        
        return required.equals(provided);
    }
    
    // Builder
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String name;
        private RecipeType type;
        private String[] pattern;
        private Map<Character, String> key;
        private List<String> ingredients;
        private ItemStack result;
        
        public Builder shaped(String name, String[] pattern, Map<Character, String> key, ItemStack result) {
            this.name = name;
            this.type = RecipeType.SHAPED;
            this.pattern = pattern;
            this.key = key;
            this.result = result;
            return this;
        }
        
        public Builder shapeless(String name, List<String> ingredients, ItemStack result) {
            this.name = name;
            this.type = RecipeType.SHAPELESS;
            this.ingredients = ingredients;
            this.result = result;
            return this;
        }
        
        public Recipe build() {
            return new Recipe(name, type, pattern, key, ingredients, result);
        }
    }
    
    // Serialization
    public String toJson() {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize Recipe", e);
        }
    }
    
    public static Recipe fromJson(String json) {
        try {
            return MAPPER.readValue(json, Recipe.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize Recipe", e);
        }
    }
    
    @Override
    public String toString() {
        return "Recipe{name='" + name + "', type=" + type + ", result=" + result + "}";
    }
}
