package com.poorcraft.ultra.tests;

import com.poorcraft.ultra.crafting.CraftingGrid;
import com.poorcraft.ultra.crafting.Recipe;
import com.poorcraft.ultra.crafting.RecipeBook;
import com.poorcraft.ultra.crafting.RecipeRegistry;
import com.poorcraft.ultra.crafting.RecipeType;
import com.poorcraft.ultra.inventory.ItemStack;
import com.poorcraft.ultra.inventory.PlayerInventory;
import com.poorcraft.ultra.items.ItemRegistry;
import org.junit.jupiter.api.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke tests for crafting system (CP v3.2).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CraftingSmokeTest {
    
    @BeforeAll
    static void initRegistries() {
        ItemRegistry.getInstance();
        RecipeRegistry.getInstance();
    }
    
    @Test
    @Tag("smoke")
    @Order(1)
    @DisplayName("CP v3.2: Recipe JSON roundtrip - serialize/deserialize match")
    @Timeout(5)
    void testRecipeJsonRoundtrip() {
        System.out.println("=== CP v3.2: Testing recipe JSON roundtrip ===");
        
        // Create recipe
        Recipe recipe = Recipe.builder()
                .shaped("wooden_pickaxe", 
                    new String[]{"AAA", " B ", " B "},
                    Map.of('A', "oak_planks", 'B', "stick"),
                    ItemStack.of(1000, 1))
                .build();
        
        // Serialize
        String json = recipe.toJson();
        assertNotNull(json, "JSON should not be null");
        assertTrue(json.contains("wooden_pickaxe"), "JSON should contain recipe name");
        
        // Deserialize
        Recipe loadedRecipe = Recipe.fromJson(json);
        
        // Verify fields match
        assertEquals("wooden_pickaxe", loadedRecipe.getName(), "Name should match");
        assertEquals(RecipeType.SHAPED, loadedRecipe.getType(), "Type should match");
        assertArrayEquals(new String[]{"AAA", " B ", " B "}, loadedRecipe.getPattern(), "Pattern should match");
        assertEquals(1000, loadedRecipe.getResult().getItemId(), "Result item ID should match");
        
        System.out.println("✓ Recipe roundtrip: serialize → deserialize (data preserved)");
    }
    
    @Test
    @Tag("smoke")
    @Order(2)
    @DisplayName("CP v3.2: Crafting grid evaluates recipe correctly")
    @Timeout(5)
    void testCraftingGridEvaluation() {
        System.out.println("=== CP v3.2: Testing crafting grid evaluation ===");
        
        CraftingGrid grid = new CraftingGrid(3, 3);
        
        // Place items in grid (wooden pickaxe pattern)
        grid.setSlot(0, ItemStack.of(1068, 1)); // oak planks
        grid.setSlot(1, ItemStack.of(1068, 1));
        grid.setSlot(2, ItemStack.of(1068, 1));
        grid.setSlot(4, ItemStack.of(1025, 1)); // stick
        grid.setSlot(7, ItemStack.of(1025, 1));
        
        // Evaluate recipe
        grid.evaluate();
        
        // Verify result
        ItemStack result = grid.getResultSlot();
        assertFalse(result.isEmpty(), "Result should not be empty");
        assertEquals(1000, result.getItemId(), "Result should be wooden pickaxe");
        assertEquals(1, result.getCount(), "Result count should be 1");
        
        System.out.println("✓ Crafting grid: pattern matched → wooden pickaxe");
    }
    
    @Test
    @Tag("smoke")
    @Order(3)
    @DisplayName("CP v3.2: Crafting consumes ingredients")
    @Timeout(5)
    void testCraftingConsumesIngredients() {
        System.out.println("=== CP v3.2: Testing ingredient consumption ===");
        
        CraftingGrid grid = new CraftingGrid(2, 2);
        
        // Place items for crafting table (2x2 planks)
        grid.setSlot(0, ItemStack.of(1068, 1)); // oak planks
        grid.setSlot(1, ItemStack.of(1068, 1));
        grid.setSlot(2, ItemStack.of(1068, 1));
        grid.setSlot(3, ItemStack.of(1068, 1));
        
        // Evaluate
        grid.evaluate();
        ItemStack result = grid.getResultSlot();
        assertEquals(1081, result.getItemId(), "Should craft crafting table");
        
        // Craft (consume ingredients)
        ItemStack crafted = grid.craft();
        assertEquals(1081, crafted.getItemId(), "Crafted item should be crafting table");
        
        // Verify ingredients consumed
        assertTrue(grid.getSlot(0).isEmpty(), "Slot 0 should be empty after crafting");
        assertTrue(grid.getSlot(1).isEmpty(), "Slot 1 should be empty after crafting");
        assertTrue(grid.getSlot(2).isEmpty(), "Slot 2 should be empty after crafting");
        assertTrue(grid.getSlot(3).isEmpty(), "Slot 3 should be empty after crafting");
        
        System.out.println("✓ Crafting: ingredients consumed (4 planks → 1 crafting table)");
    }
    
    @Test
    @Tag("smoke")
    @Order(4)
    @DisplayName("CP v3.2: Recipe discovery when ingredients obtained")
    @Timeout(5)
    void testRecipeDiscovery() {
        System.out.println("=== CP v3.2: Testing recipe discovery ===");
        
        RecipeBook recipeBook = new RecipeBook();
        PlayerInventory inventory = new PlayerInventory();
        
        // Initially no recipes discovered
        assertEquals(0, recipeBook.getDiscoveredRecipes().size(), "Should have 0 discovered recipes");
        
        // Add wooden planks to inventory
        inventory.addItem(ItemStack.of(1068, 10));
        
        // Check discovery
        recipeBook.checkDiscovery(inventory);
        
        // Should discover crafting table recipe (requires only wooden planks)
        assertTrue(recipeBook.isDiscovered("crafting_table"), "Should discover crafting table recipe");
        
        // Add stick to inventory
        inventory.addItem(ItemStack.of(1025, 5));
        
        // Check discovery again
        recipeBook.checkDiscovery(inventory);
        
        // Should discover wooden pickaxe recipe (requires planks + stick)
        assertTrue(recipeBook.isDiscovered("wooden_pickaxe"), "Should discover wooden pickaxe recipe");
        
        System.out.println("✓ Recipe discovery: auto-discovered when ingredients obtained");
    }
}
