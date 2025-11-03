package com.poorcraft.ultra.crafting;

import com.poorcraft.ultra.inventory.ItemStack;

/**
 * Listener interface for crafting grid changes.
 * CP v3.2: Crafting System
 */
public interface CraftingListener {
    /**
     * Called when the crafting result changes.
     */
    void onResultChanged(ItemStack newResult);
}
