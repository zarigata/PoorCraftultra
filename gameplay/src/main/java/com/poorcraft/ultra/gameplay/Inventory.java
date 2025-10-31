package com.poorcraft.ultra.gameplay;

import com.poorcraft.ultra.shared.Logger;
import com.poorcraft.ultra.voxel.BlockType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Inventory {

    public static final int HOTBAR_SIZE = 9;
    public static final int MAIN_SIZE = 27;
    public static final int TOTAL_SIZE = HOTBAR_SIZE + MAIN_SIZE;
    public static final int MAX_STACK_SIZE = 64;

    private static final Logger logger = Logger.getLogger(Inventory.class);

    private final ItemStack[] slots = new ItemStack[TOTAL_SIZE];
    private final List<InventoryListener> listeners = new ArrayList<>();

    private int selectedSlot;

    public Inventory() {
        selectedSlot = 0;
        logger.info("Inventory initialized (36 slots)");
    }

    public int getSelectedSlot() {
        return selectedSlot;
    }

    public void setSelectedSlot(int slot) {
        if (slot < 0 || slot >= HOTBAR_SIZE) {
            throw new IllegalArgumentException("slot must be within hotbar range [0, 8]");
        }
        if (selectedSlot != slot) {
            selectedSlot = slot;
            notifyListeners();
        }
    }

    public ItemStack getSelectedItem() {
        return slots[selectedSlot];
    }

    public ItemStack getHotbarItem(int slot) {
        validateHotbarSlot(slot);
        return slots[slot];
    }

    public void setHotbarItem(int slot, ItemStack stack) {
        validateHotbarSlot(slot);
        setSlot(slot, stack);
    }

    public boolean addItem(BlockType blockType, int count) {
        Objects.requireNonNull(blockType, "blockType");
        if (count <= 0) {
            return false;
        }

        int remaining = count;
        boolean mutated = false;

        for (int i = 0; i < TOTAL_SIZE && remaining > 0; i++) {
            ItemStack stack = slots[i];
            if (stack != null && stack.blockType() == blockType && !stack.isFull()) {
                int space = stack.getRemainingSpace();
                int toAdd = Math.min(space, remaining);
                slots[i] = stack.increment(toAdd);
                remaining -= toAdd;
                mutated = true;
            }
        }

        for (int i = 0; i < TOTAL_SIZE && remaining > 0; i++) {
            if (slots[i] == null) {
                int toAdd = Math.min(MAX_STACK_SIZE, remaining);
                slots[i] = ItemStack.of(blockType, toAdd);
                remaining -= toAdd;
                mutated = true;
            }
        }

        boolean success = remaining == 0;
        if (mutated) {
            notifyListeners();
        }
        return success;
    }

    public boolean removeFromSlot(int slotIndex, int count) {
        validateSlot(slotIndex);
        if (count <= 0) {
            return false;
        }

        ItemStack stack = slots[slotIndex];
        if (stack == null || stack.count() < count) {
            return false;
        }

        int newCount = stack.count() - count;
        if (newCount == 0) {
            slots[slotIndex] = null;
        } else {
            slots[slotIndex] = stack.withCount(newCount);
        }

        notifyListeners();
        return true;
    }

    public boolean removeItem(BlockType blockType, int count) {
        Objects.requireNonNull(blockType, "blockType");
        if (count <= 0) {
            return false;
        }
        if (!hasItem(blockType, count)) {
            return false;
        }

        int remaining = count;
        for (int i = 0; i < TOTAL_SIZE && remaining > 0; i++) {
            ItemStack stack = slots[i];
            if (stack != null && stack.blockType() == blockType) {
                if (stack.count() <= remaining) {
                    remaining -= stack.count();
                    slots[i] = null;
                } else {
                    slots[i] = stack.decrement(remaining);
                    remaining = 0;
                }
            }
        }

        notifyListeners();
        return true;
    }

    public boolean hasItem(BlockType blockType, int count) {
        return countItem(blockType) >= count;
    }

    public int countItem(BlockType blockType) {
        Objects.requireNonNull(blockType, "blockType");
        int total = 0;
        for (ItemStack stack : slots) {
            if (stack != null && stack.blockType() == blockType) {
                total += stack.count();
            }
        }
        return total;
    }

    public ItemStack getSlot(int index) {
        validateSlot(index);
        return slots[index];
    }

    public void setSlot(int index, ItemStack stack) {
        validateSlot(index);
        slots[index] = stack;
        notifyListeners();
    }

    public boolean isSlotEmpty(int index) {
        validateSlot(index);
        return slots[index] == null;
    }

    public int getEmptySlotCount() {
        int empty = 0;
        for (ItemStack slot : slots) {
            if (slot == null) {
                empty++;
            }
        }
        return empty;
    }

    public void addListener(InventoryListener listener) {
        listeners.add(listener);
    }

    public void removeListener(InventoryListener listener) {
        listeners.remove(listener);
    }

    public List<InventoryListener> getListeners() {
        return Collections.unmodifiableList(listeners);
    }

    private void notifyListeners() {
        if (listeners.isEmpty()) {
            return;
        }
        for (InventoryListener listener : List.copyOf(listeners)) {
            listener.onInventoryChanged(this);
        }
    }

    private void validateSlot(int index) {
        if (index < 0 || index >= TOTAL_SIZE) {
            throw new IllegalArgumentException("slot must be within range [0, 35]");
        }
    }

    private void validateHotbarSlot(int slot) {
        if (slot < 0 || slot >= HOTBAR_SIZE) {
            throw new IllegalArgumentException("hotbar slot must be within range [0, 8]");
        }
    }
}
