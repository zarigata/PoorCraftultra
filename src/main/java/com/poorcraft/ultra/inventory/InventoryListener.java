package com.poorcraft.ultra.inventory;

/**
 * Listener interface for inventory changes.
 * CP v3.1: Inventory System
 */
public interface InventoryListener {
    /**
     * Called when a slot's contents change.
     */
    void onSlotChanged(int slot, ItemStack oldStack, ItemStack newStack);
    
    /**
     * Called when the selected hotbar slot changes.
     */
    void onSelectedSlotChanged(int oldSlot, int newSlot);
}
