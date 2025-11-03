package com.poorcraft.ultra.tests;

import com.poorcraft.ultra.inventory.ItemStack;
import com.poorcraft.ultra.inventory.PlayerInventory;
import com.poorcraft.ultra.items.ItemRegistry;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke tests for inventory system (CP v3.1).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class InventorySmokeTest {
    
    @BeforeAll
    static void initRegistry() {
        ItemRegistry.getInstance();
    }
    
    @Test
    @Tag("smoke")
    @Order(1)
    @DisplayName("CP v3.1: Stack rules enforced - no duplication on merge")
    @Timeout(5)
    void testStackRulesEnforced() {
        System.out.println("=== CP v3.1: Testing stack merge rules ===");
        
        PlayerInventory inventory = new PlayerInventory();
        
        // Add 32 stone to slot 0
        ItemStack stack1 = ItemStack.of(1060, 32); // stone item
        inventory.setStack(0, stack1);
        
        // Add 32 stone to slot 1
        ItemStack stack2 = ItemStack.of(1060, 32);
        inventory.setStack(1, stack2);
        
        // Merge slot 1 into slot 0 (should fill to 64)
        inventory.mergeStacks(1, 0);
        
        // Verify counts
        assertEquals(64, inventory.getStack(0).getCount(), "Slot 0 should have 64 items");
        assertEquals(0, inventory.getStack(1).getCount(), "Slot 1 should be empty");
        
        // Total items should be 64 (no duplication)
        int totalItems = 0;
        for (int i = 0; i < 36; i++) {
            totalItems += inventory.getStack(i).getCount();
        }
        assertEquals(64, totalItems, "Total items should be 64 (no duplication)");
        
        System.out.println("✓ Stack merge rules enforced: 32 + 32 = 64 (no duplication)");
    }
    
    @Test
    @Tag("smoke")
    @Order(2)
    @DisplayName("CP v3.1: Split stack - no item loss")
    @Timeout(5)
    void testSplitStackNoLoss() {
        System.out.println("=== CP v3.1: Testing stack split ===");
        
        PlayerInventory inventory = new PlayerInventory();
        
        // Add 64 stone to slot 0
        ItemStack stack = ItemStack.of(1060, 64);
        inventory.setStack(0, stack);
        
        // Split 32 items
        ItemStack splitStack = inventory.splitStack(0, 32);
        
        // Verify counts
        assertEquals(32, inventory.getStack(0).getCount(), "Original stack should have 32 items");
        assertEquals(32, splitStack.getCount(), "Split stack should have 32 items");
        
        // Total should be 64
        assertEquals(64, inventory.getStack(0).getCount() + splitStack.getCount(), 
            "Total items should be 64 (no loss)");
        
        System.out.println("✓ Stack split: 64 → 32 + 32 (no item loss)");
    }
    
    @Test
    @Tag("smoke")
    @Order(3)
    @DisplayName("CP v3.1: Tool durability decreases on use")
    @Timeout(5)
    void testToolDurabilityTracking() {
        System.out.println("=== CP v3.1: Testing tool durability ===");
        
        // Create wooden pickaxe (durability 59)
        ItemStack pickaxe = ItemStack.of(1000, 1, 59);
        
        // Damage tool 10 times
        for (int i = 0; i < 10; i++) {
            pickaxe = pickaxe.damageTool(1);
        }
        
        // Verify durability
        assertEquals(49, pickaxe.getDurability(), "Durability should be 49 after 10 uses");
        
        // Damage until broken
        for (int i = 0; i < 49; i++) {
            pickaxe = pickaxe.damageTool(1);
        }
        
        // Verify tool is broken (durability 0)
        assertEquals(0, pickaxe.getDurability(), "Tool should be broken (durability 0)");
        
        System.out.println("✓ Tool durability: 59 → 49 (after 10 uses) → 0 (broken)");
    }
    
    @Test
    @Tag("smoke")
    @Order(4)
    @DisplayName("CP v3.1: Inventory save/load roundtrip")
    @Timeout(5)
    void testInventoryPersistenceRoundtrip() {
        System.out.println("=== CP v3.1: Testing inventory persistence ===");
        
        PlayerInventory inventory = new PlayerInventory();
        
        // Fill inventory with items
        inventory.setStack(0, ItemStack.of(1060, 32)); // stone
        inventory.setStack(1, ItemStack.of(1000, 1, 50)); // wooden pickaxe
        inventory.setStack(5, ItemStack.of(1026, 16)); // coal
        inventory.setSelectedSlot(2);
        
        // Serialize
        String json = inventory.toJson();
        assertNotNull(json, "JSON should not be null");
        assertTrue(json.contains("1060"), "JSON should contain stone item ID");
        
        // Deserialize
        PlayerInventory loadedInventory = PlayerInventory.fromJson(json);
        
        // Verify slots match
        assertEquals(32, loadedInventory.getStack(0).getCount(), "Slot 0 count should match");
        assertEquals(1060, loadedInventory.getStack(0).getItemId(), "Slot 0 item ID should match");
        assertEquals(50, loadedInventory.getStack(1).getDurability(), "Slot 1 durability should match");
        assertEquals(2, loadedInventory.getSelectedSlot(), "Selected slot should match");
        
        System.out.println("✓ Inventory persistence: serialize → deserialize (data preserved)");
    }
}
