package com.poorcraft.ultra.inventory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Player inventory container (hotbar + main inventory).
 * CP v3.1: Inventory System
 */
public class PlayerInventory {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int TOTAL_SLOTS = 36; // 9 hotbar + 27 main
    
    private final ItemStack[] slots;
    private int selectedSlot;
    private final List<InventoryListener> listeners;
    
    public PlayerInventory() {
        this.slots = new ItemStack[TOTAL_SLOTS];
        Arrays.fill(slots, ItemStack.empty());
        this.selectedSlot = 0;
        this.listeners = new ArrayList<>();
    }
    
    @JsonCreator
    private PlayerInventory(
            @JsonProperty("slots") ItemStack[] slots,
            @JsonProperty("selectedSlot") int selectedSlot) {
        this.slots = slots != null ? slots : new ItemStack[TOTAL_SLOTS];
        if (this.slots.length != TOTAL_SLOTS) {
            throw new IllegalArgumentException("Invalid slot count: " + this.slots.length);
        }
        // Ensure no null slots
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (this.slots[i] == null) {
                this.slots[i] = ItemStack.empty();
            }
        }
        this.selectedSlot = selectedSlot;
        this.listeners = new ArrayList<>();
    }
    
    // Slot access
    public ItemStack getStack(int slot) {
        validateSlot(slot);
        return slots[slot];
    }
    
    public void setStack(int slot, ItemStack stack) {
        validateSlot(slot);
        ItemStack oldStack = slots[slot];
        slots[slot] = stack != null ? stack : ItemStack.empty();
        notifySlotChanged(slot, oldStack, slots[slot]);
    }
    
    public ItemStack getHotbarStack(int hotbarIndex) {
        if (hotbarIndex < 0 || hotbarIndex >= 9) {
            throw new IllegalArgumentException("Invalid hotbar index: " + hotbarIndex);
        }
        return getStack(hotbarIndex);
    }
    
    public ItemStack getMainStack(int mainIndex) {
        if (mainIndex < 0 || mainIndex >= 27) {
            throw new IllegalArgumentException("Invalid main inventory index: " + mainIndex);
        }
        return getStack(9 + mainIndex);
    }
    
    // Hotbar selection
    @JsonProperty("selectedSlot")
    public int getSelectedSlot() {
        return selectedSlot;
    }
    
    @JsonProperty("slots")
    public ItemStack[] getSlots() {
        return slots;
    }
    
    public void setSelectedSlot(int slot) {
        if (slot < 0 || slot >= 9) {
            throw new IllegalArgumentException("Invalid hotbar slot: " + slot);
        }
        int oldSlot = selectedSlot;
        selectedSlot = slot;
        notifySelectedSlotChanged(oldSlot, selectedSlot);
    }
    
    public ItemStack getSelectedStack() {
        return getStack(selectedSlot);
    }
    
    public void setSelectedStack(ItemStack stack) {
        setStack(selectedSlot, stack);
    }
    
    // Item operations
    public ItemStack addItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.empty();
        }
        
        ItemStack remaining = stack.copy();
        
        // First pass: try to merge with existing stacks
        for (int i = 0; i < TOTAL_SLOTS && !remaining.isEmpty(); i++) {
            ItemStack slotStack = slots[i];
            if (!slotStack.isEmpty() && slotStack.canMergeWith(remaining)) {
                int spaceLeft = slotStack.getMaxStackSize() - slotStack.getCount();
                if (spaceLeft > 0) {
                    int toAdd = Math.min(spaceLeft, remaining.getCount());
                    setStack(i, slotStack.grow(toAdd));
                    remaining = remaining.shrink(toAdd);
                }
            }
        }
        
        // Second pass: fill empty slots
        for (int i = 0; i < TOTAL_SLOTS && !remaining.isEmpty(); i++) {
            if (slots[i].isEmpty()) {
                int toAdd = Math.min(remaining.getMaxStackSize(), remaining.getCount());
                setStack(i, remaining.withCount(toAdd));
                remaining = remaining.shrink(toAdd);
            }
        }
        
        return remaining;
    }
    
    public int removeItem(int itemId, int count) {
        int removed = 0;
        for (int i = 0; i < TOTAL_SLOTS && removed < count; i++) {
            ItemStack slotStack = slots[i];
            if (!slotStack.isEmpty() && slotStack.getItemId() == itemId) {
                int toRemove = Math.min(slotStack.getCount(), count - removed);
                setStack(i, slotStack.shrink(toRemove));
                removed += toRemove;
            }
        }
        return removed;
    }
    
    public boolean hasItem(int itemId, int count) {
        int total = 0;
        for (ItemStack slotStack : slots) {
            if (!slotStack.isEmpty() && slotStack.getItemId() == itemId) {
                total += slotStack.getCount();
                if (total >= count) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public int findSlot(int itemId) {
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (!slots[i].isEmpty() && slots[i].getItemId() == itemId) {
                return i;
            }
        }
        return -1;
    }
    
    public int findEmptySlot() {
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (slots[i].isEmpty()) {
                return i;
            }
        }
        return -1;
    }
    
    // Stack operations
    public boolean canMerge(int slot1, int slot2) {
        validateSlot(slot1);
        validateSlot(slot2);
        ItemStack stack1 = slots[slot1];
        ItemStack stack2 = slots[slot2];
        
        if (stack1.isEmpty() || stack2.isEmpty()) {
            return true; // Can always move to/from empty slot
        }
        
        return stack1.canMergeWith(stack2) && 
               stack1.getCount() + stack2.getCount() <= stack1.getMaxStackSize();
    }
    
    public void mergeStacks(int fromSlot, int toSlot) {
        validateSlot(fromSlot);
        validateSlot(toSlot);
        
        ItemStack fromStack = slots[fromSlot];
        ItemStack toStack = slots[toSlot];
        
        if (fromStack.isEmpty()) {
            return;
        }
        
        if (toStack.isEmpty()) {
            // Simple move
            setStack(toSlot, fromStack);
            setStack(fromSlot, ItemStack.empty());
        } else if (fromStack.canMergeWith(toStack)) {
            // Merge stacks
            int spaceLeft = toStack.getMaxStackSize() - toStack.getCount();
            int toTransfer = Math.min(spaceLeft, fromStack.getCount());
            
            setStack(toSlot, toStack.grow(toTransfer));
            setStack(fromSlot, fromStack.shrink(toTransfer));
        }
    }
    
    public void swapStacks(int slot1, int slot2) {
        validateSlot(slot1);
        validateSlot(slot2);
        
        ItemStack temp = slots[slot1];
        slots[slot1] = slots[slot2];
        slots[slot2] = temp;
        
        notifySlotChanged(slot1, slots[slot2], slots[slot1]);
        notifySlotChanged(slot2, slots[slot1], slots[slot2]);
    }
    
    public ItemStack splitStack(int slot, int amount) {
        validateSlot(slot);
        ItemStack slotStack = slots[slot];
        
        if (slotStack.isEmpty() || amount <= 0 || amount > slotStack.getCount()) {
            return ItemStack.empty();
        }
        
        ItemStack split = slotStack.split(amount);
        setStack(slot, slotStack.shrink(amount));
        return split;
    }
    
    // Listeners
    public void addListener(InventoryListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(InventoryListener listener) {
        listeners.remove(listener);
    }
    
    private void notifySlotChanged(int slot, ItemStack oldStack, ItemStack newStack) {
        for (InventoryListener listener : listeners) {
            listener.onSlotChanged(slot, oldStack, newStack);
        }
    }
    
    private void notifySelectedSlotChanged(int oldSlot, int newSlot) {
        for (InventoryListener listener : listeners) {
            listener.onSelectedSlotChanged(oldSlot, newSlot);
        }
    }
    
    // Validation
    private void validateSlot(int slot) {
        if (slot < 0 || slot >= TOTAL_SLOTS) {
            throw new IllegalArgumentException("Invalid slot: " + slot);
        }
    }
    
    // Serialization
    public String toJson() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize PlayerInventory", e);
        }
    }
    
    public static PlayerInventory fromJson(String json) {
        try {
            return MAPPER.readValue(json, PlayerInventory.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize PlayerInventory", e);
        }
    }
}
