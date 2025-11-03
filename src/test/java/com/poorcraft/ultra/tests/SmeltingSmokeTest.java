package com.poorcraft.ultra.tests;

import com.poorcraft.ultra.inventory.ItemStack;
import com.poorcraft.ultra.items.ItemRegistry;
import com.poorcraft.ultra.smelting.FuelRegistry;
import com.poorcraft.ultra.smelting.FurnaceBlockEntity;
import com.poorcraft.ultra.smelting.SmeltingRecipeRegistry;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke tests for smelting system (CP v3.3).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SmeltingSmokeTest {
    
    @BeforeAll
    static void initRegistries() {
        ItemRegistry.getInstance();
        FuelRegistry.getInstance();
        SmeltingRecipeRegistry.getInstance();
    }
    
    @Test
    @Tag("smoke")
    @Order(1)
    @DisplayName("CP v3.3: Fuel burn time math - coal burns for 1600 ticks")
    @Timeout(10)
    void testFuelBurnTimeMath() {
        System.out.println("=== CP v3.3: Testing fuel burn time ===");
        
        FurnaceBlockEntity furnace = new FurnaceBlockEntity(0, 0, 0);
        
        // Add coal to fuel slot
        furnace.setFuelSlot(ItemStack.of(1026, 1)); // coal
        
        // Add iron ore to input slot
        furnace.setInputSlot(ItemStack.of(1071, 1)); // iron ore
        
        // Tick until fuel consumed
        int tickCount = 0;
        while (furnace.isBurning() || furnace.getFuelSlot().getCount() > 0) {
            furnace.tick();
            tickCount++;
            if (tickCount > 2000) break; // Safety limit
        }
        
        // Verify coal burned for 1600 ticks
        assertEquals(1600, tickCount, "Coal should burn for exactly 1600 ticks");
        
        System.out.println("✓ Fuel burn time: coal = 1600 ticks (80 seconds)");
    }
    
    @Test
    @Tag("smoke")
    @Order(2)
    @DisplayName("CP v3.3: Smelting progress - iron ore smelts in 200 ticks")
    @Timeout(5)
    void testSmeltingProgress() {
        System.out.println("=== CP v3.3: Testing smelting progress ===");
        
        FurnaceBlockEntity furnace = new FurnaceBlockEntity(0, 0, 0);
        
        // Add coal and iron ore
        furnace.setFuelSlot(ItemStack.of(1026, 1)); // coal
        furnace.setInputSlot(ItemStack.of(1071, 1)); // iron ore
        
        // Tick 200 times
        for (int i = 0; i < 200; i++) {
            furnace.tick();
        }
        
        // Verify output
        ItemStack output = furnace.getOutputSlot();
        assertFalse(output.isEmpty(), "Output should not be empty");
        assertEquals(1027, output.getItemId(), "Output should be iron ingot");
        assertEquals(1, output.getCount(), "Output count should be 1");
        
        // Verify input consumed
        assertTrue(furnace.getInputSlot().isEmpty(), "Input should be consumed");
        
        System.out.println("✓ Smelting progress: iron ore → iron ingot (200 ticks)");
    }
    
    @Test
    @Tag("smoke")
    @Order(3)
    @DisplayName("CP v3.3: XP yield - smelt 10 iron ore yields 7.0 XP")
    @Timeout(10)
    void testXpYieldCalculation() {
        System.out.println("=== CP v3.3: Testing XP yield ===");
        
        FurnaceBlockEntity furnace = new FurnaceBlockEntity(0, 0, 0);
        
        // Add coal and 10 iron ore
        furnace.setFuelSlot(ItemStack.of(1026, 10)); // 10 coal (enough fuel)
        furnace.setInputSlot(ItemStack.of(1071, 10)); // 10 iron ore
        
        // Tick until all ore smelted
        int tickCount = 0;
        while (furnace.getInputSlot().getCount() > 0) {
            furnace.tick();
            tickCount++;
            if (tickCount > 5000) break; // Safety limit
        }
        
        // Verify XP accumulated (10 ore × 0.7 XP = 7.0 XP)
        float xp = furnace.getAccumulatedXp();
        assertEquals(7.0f, xp, 0.01f, "Should accumulate 7.0 XP from 10 iron ore");
        
        // Verify output count
        assertEquals(10, furnace.getOutputSlot().getCount(), "Should have 10 iron ingots");
        
        System.out.println("✓ XP yield: 10 iron ore → 7.0 XP (0.7 per smelt)");
    }
    
    @Test
    @Tag("smoke")
    @Order(4)
    @DisplayName("CP v3.3: Multiple fuel consumption - 5 coal smelts 40 ore")
    @Timeout(10)
    void testMultipleFuelConsumption() {
        System.out.println("=== CP v3.3: Testing multiple fuel consumption ===");
        
        FurnaceBlockEntity furnace = new FurnaceBlockEntity(0, 0, 0);
        
        // Add 5 coal and 40 iron ore
        furnace.setFuelSlot(ItemStack.of(1026, 5)); // 5 coal (1600 × 5 = 8000 ticks)
        furnace.setInputSlot(ItemStack.of(1071, 40)); // 40 iron ore (200 × 40 = 8000 ticks)
        
        // Tick until all ore smelted or fuel exhausted
        int tickCount = 0;
        while (furnace.getInputSlot().getCount() > 0 && tickCount < 10000) {
            furnace.tick();
            tickCount++;
        }
        
        // Verify all ore smelted
        assertEquals(0, furnace.getInputSlot().getCount(), "All ore should be smelted");
        assertEquals(40, furnace.getOutputSlot().getCount(), "Should have 40 iron ingots");
        
        // Verify fuel consumed
        assertEquals(0, furnace.getFuelSlot().getCount(), "All fuel should be consumed");
        
        System.out.println("✓ Multiple fuel: 5 coal → 40 iron ore smelted (8000 ticks)");
    }
    
    @Test
    @Tag("smoke")
    @Order(5)
    @DisplayName("CP v3.3: XP collection resets accumulator")
    @Timeout(5)
    void testXpCollection() {
        System.out.println("=== CP v3.3: Testing XP collection ===");
        
        FurnaceBlockEntity furnace = new FurnaceBlockEntity(0, 0, 0);
        
        // Smelt one iron ore
        furnace.setFuelSlot(ItemStack.of(1026, 1));
        furnace.setInputSlot(ItemStack.of(1071, 1));
        
        for (int i = 0; i < 200; i++) {
            furnace.tick();
        }
        
        // Verify XP accumulated
        float xp = furnace.getAccumulatedXp();
        assertEquals(0.7f, xp, 0.01f, "Should have 0.7 XP");
        
        // Collect XP
        float collectedXp = furnace.collectXp();
        assertEquals(0.7f, collectedXp, 0.01f, "Should collect 0.7 XP");
        
        // Verify accumulator reset
        assertEquals(0.0f, furnace.getAccumulatedXp(), 0.01f, "Accumulator should be reset");
        
        System.out.println("✓ XP collection: 0.7 XP collected, accumulator reset");
    }
}
