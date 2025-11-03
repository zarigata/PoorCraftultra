package com.poorcraft.ultra.crafting;

import com.poorcraft.ultra.inventory.ItemStack;

import java.util.*;
import java.util.logging.Logger;

/**
 * Central registry for crafting recipes (singleton pattern).
 * CP v3.2: Crafting System
 */
public class RecipeRegistry {
    private static final Logger LOGGER = Logger.getLogger(RecipeRegistry.class.getName());
    private static RecipeRegistry instance;
    
    private final Map<String, Recipe> recipes = new HashMap<>();
    private final Map<Integer, List<Recipe>> recipesByResult = new HashMap<>();
    
    private RecipeRegistry() {
        init();
    }
    
    public static RecipeRegistry getInstance() {
        if (instance == null) {
            instance = new RecipeRegistry();
        }
        return instance;
    }
    
    private void init() {
        LOGGER.info("Initializing RecipeRegistry...");
        
        // Load recipes from JSON resources
        loadRecipesFromResources();
        
        // Register programmatic fallback recipes
        registerBasicRecipes();
        
        LOGGER.info("RecipeRegistry initialized: " + recipes.size() + " recipes");
    }
    
    private void loadRecipesFromResources() {
        try {
            // Try to load recipes from classpath
            java.net.URL recipesUrl = getClass().getClassLoader().getResource("recipes");
            if (recipesUrl != null) {
                java.nio.file.Path recipesPath;
                if (recipesUrl.toURI().getScheme().equals("jar")) {
                    // Running from JAR
                    java.nio.file.FileSystem fs = java.nio.file.FileSystems.newFileSystem(recipesUrl.toURI(), java.util.Collections.emptyMap());
                    recipesPath = fs.getPath("/recipes");
                } else {
                    // Running from IDE/filesystem
                    recipesPath = java.nio.file.Paths.get(recipesUrl.toURI());
                }
                
                // Load all .json files
                java.nio.file.Files.walk(recipesPath)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> {
                        try {
                            String json = java.nio.file.Files.readString(path);
                            Recipe recipe = Recipe.fromJson(json);
                            register(recipe);
                            LOGGER.fine("Loaded recipe from resource: " + path.getFileName());
                        } catch (Exception e) {
                            LOGGER.warning("Failed to load recipe from " + path + ": " + e.getMessage());
                        }
                    });
            } else {
                LOGGER.info("No recipes directory found in resources, using programmatic recipes only");
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to load recipes from resources: " + e.getMessage());
        }
    }
    
    private void registerBasicRecipes() {
        // Stick recipe (2 wooden planks vertical)
        register(Recipe.builder()
                .shaped("stick", 
                    new String[]{"A", "A"}, 
                    Map.of('A', "oak_planks"),
                    ItemStack.of(1025, 4))
                .build());
        
        // Crafting table (2x2 wooden planks)
        register(Recipe.builder()
                .shaped("crafting_table",
                    new String[]{"AA", "AA"},
                    Map.of('A', "oak_planks"),
                    ItemStack.of(1081, 1))
                .build());
        
        // Torch (coal + stick)
        register(Recipe.builder()
                .shaped("torch",
                    new String[]{"A", "B"},
                    Map.of('A', "coal", 'B', "stick"),
                    ItemStack.of(1084, 4))
                .build());
        
        // Wooden pickaxe
        register(Recipe.builder()
                .shaped("wooden_pickaxe",
                    new String[]{"AAA", " B ", " B "},
                    Map.of('A', "oak_planks", 'B', "stick"),
                    ItemStack.of(1000, 1))
                .build());
        
        // Wooden axe
        register(Recipe.builder()
                .shaped("wooden_axe",
                    new String[]{"AA", "AB", " B"},
                    Map.of('A', "oak_planks", 'B', "stick"),
                    ItemStack.of(1001, 1))
                .build());
        
        // Wooden shovel
        register(Recipe.builder()
                .shaped("wooden_shovel",
                    new String[]{"A", "B", "B"},
                    Map.of('A', "oak_planks", 'B', "stick"),
                    ItemStack.of(1002, 1))
                .build());
        
        // Chest (8 wooden planks in square)
        register(Recipe.builder()
                .shaped("chest",
                    new String[]{"AAA", "A A", "AAA"},
                    Map.of('A', "oak_planks"),
                    ItemStack.of(1083, 1))
                .build());
        
        // Furnace (8 cobblestone in square)
        register(Recipe.builder()
                .shaped("furnace",
                    new String[]{"AAA", "A A", "AAA"},
                    Map.of('A', "cobblestone"),
                    ItemStack.of(1082, 1))
                .build());
    }
    
    public void register(Recipe recipe) {
        recipes.put(recipe.getName(), recipe);
        
        int resultItemId = recipe.getResult().getItemId();
        recipesByResult.computeIfAbsent(resultItemId, k -> new ArrayList<>()).add(recipe);
        
        LOGGER.fine("Loaded recipe: " + recipe.getName());
    }
    
    public Recipe getRecipe(String name) {
        return recipes.get(name);
    }
    
    public List<Recipe> getRecipesByResult(int itemId) {
        return recipesByResult.getOrDefault(itemId, Collections.emptyList());
    }
    
    public Collection<Recipe> getAllRecipes() {
        return recipes.values();
    }
    
    /**
     * Finds a recipe matching the given crafting grid.
     */
    public Recipe findMatchingRecipe(ItemStack[] grid, int width, int height) {
        for (Recipe recipe : recipes.values()) {
            if (recipe.matches(grid, width, height)) {
                return recipe;
            }
        }
        return null;
    }
}
