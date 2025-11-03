package com.poorcraft.ultra.smelting;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Registry for furnace fuel items.
 * CP v3.3: Smelting System
 */
public class FuelRegistry {
    private static final Logger LOGGER = Logger.getLogger(FuelRegistry.class.getName());
    private static FuelRegistry instance;
    
    private final Map<Integer, Integer> fuelBurnTimes = new HashMap<>();
    
    private FuelRegistry() {
        init();
    }
    
    public static FuelRegistry getInstance() {
        if (instance == null) {
            instance = new FuelRegistry();
        }
        return instance;
    }
    
    private void init() {
        LOGGER.info("Initializing FuelRegistry...");
        
        // Register fuels
        register(1026, 1600);  // coal - 1600 ticks (80 seconds)
        register(1025, 100);   // stick - 100 ticks (5 seconds)
        register(1067, 300);   // oak_log - 300 ticks (15 seconds)
        register(1068, 300);   // oak_planks - 300 ticks (15 seconds)
        register(1081, 300);   // crafting_table - 300 ticks (15 seconds)
        register(1083, 300);   // chest - 300 ticks (15 seconds)
        
        LOGGER.info("FuelRegistry initialized: " + fuelBurnTimes.size() + " fuels");
    }
    
    public void register(int itemId, int burnTime) {
        fuelBurnTimes.put(itemId, burnTime);
        LOGGER.fine("Registered fuel: item ID " + itemId + " (" + burnTime + " ticks)");
    }
    
    public boolean isFuel(int itemId) {
        return fuelBurnTimes.containsKey(itemId);
    }
    
    public int getBurnTime(int itemId) {
        return fuelBurnTimes.getOrDefault(itemId, 0);
    }
}
