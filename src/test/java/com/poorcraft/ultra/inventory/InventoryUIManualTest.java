package com.poorcraft.ultra.inventory;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.system.AppSettings;
import com.poorcraft.ultra.items.ItemRegistry;

/**
 * Manual test for inventory UI.
 * Press 'E' to open inventory, test shift-click, tooltips, crafting, etc.
 */
public class InventoryUIManualTest extends SimpleApplication implements ActionListener {
    
    private PlayerInventory inventory;
    private InventoryAppState inventoryState;
    private boolean inventoryOpen = false;
    
    public static void main(String[] args) {
        InventoryUIManualTest app = new InventoryUIManualTest();
        
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Inventory UI Test");
        settings.setResolution(1280, 720);
        settings.setVSync(true);
        settings.setFullscreen(false);
        
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }
    
    @Override
    public void simpleInitApp() {
        // Initialize registries
        ItemRegistry.getInstance();
        
        // Create player inventory with some test items
        inventory = new PlayerInventory();
        populateTestInventory();
        
        // Set up input
        inputManager.addMapping("ToggleInventory", new KeyTrigger(KeyInput.KEY_E));
        inputManager.addListener(this, "ToggleInventory");
        
        System.out.println("=== Inventory UI Manual Test ===");
        System.out.println("Press 'E' to open/close inventory");
        System.out.println("Test features:");
        System.out.println("  - Shift-click to quick transfer between hotbar and main inventory");
        System.out.println("  - Hover over items to see tooltips");
        System.out.println("  - Stack counts visible for stacks > 1");
        System.out.println("  - Use 2x2 crafting grid (try crafting sticks, crafting table)");
        System.out.println("  - Right-click to place/take single items");
    }
    
    private void populateTestInventory() {
        // Add some test items to inventory
        // Hotbar
        inventory.setStack(0, ItemStack.of(1000, 1)); // Wooden pickaxe
        inventory.setStack(1, ItemStack.of(1001, 1)); // Wooden axe
        inventory.setStack(2, ItemStack.of(1025, 16)); // Sticks
        inventory.setStack(3, ItemStack.of(1024, 32)); // Oak planks
        inventory.setStack(4, ItemStack.of(1084, 8)); // Torches
        
        // Main inventory
        inventory.setStack(9, ItemStack.of(1, 64)); // Dirt
        inventory.setStack(10, ItemStack.of(2, 64)); // Stone
        inventory.setStack(11, ItemStack.of(1024, 64)); // Oak planks
        inventory.setStack(12, ItemStack.of(1026, 16)); // Coal
        inventory.setStack(13, ItemStack.of(1027, 8)); // Iron ingot
        inventory.setStack(14, ItemStack.of(1028, 4)); // Gold ingot
        inventory.setStack(15, ItemStack.of(1029, 2)); // Diamond
        
        System.out.println("Test inventory populated with items");
    }
    
    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (name.equals("ToggleInventory") && isPressed) {
            toggleInventory();
        }
    }
    
    private void toggleInventory() {
        AppStateManager stateManager = getStateManager();
        
        if (inventoryOpen) {
            // Close inventory
            if (inventoryState != null) {
                stateManager.detach(inventoryState);
                inventoryState = null;
            }
            inventoryOpen = false;
            System.out.println("Inventory closed");
        } else {
            // Open inventory
            inventoryState = new InventoryAppState(inventory, null);
            stateManager.attach(inventoryState);
            inventoryOpen = true;
            System.out.println("Inventory opened");
        }
    }
    
    @Override
    public void simpleUpdate(float tpf) {
        // Nothing to update
    }
}
