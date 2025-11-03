package com.poorcraft.ultra.inventory;

import com.poorcraft.ultra.items.ItemDefinition;
import com.poorcraft.ultra.items.ItemRegistry;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Immutable data class for item stack (item + count + metadata).
 * CP v3.1: Inventory System
 */
public class ItemStack {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    private final int itemId;
    private final int count;
    private final int durability;
    private final Map<String, Object> nbt;
    
    @JsonCreator
    private ItemStack(
            @JsonProperty("itemId") int itemId,
            @JsonProperty("count") int count,
            @JsonProperty("durability") int durability,
            @JsonProperty("nbt") Map<String, Object> nbt) {
        this.itemId = itemId;
        this.count = count;
        this.durability = durability;
        this.nbt = nbt != null ? new HashMap<>(nbt) : new HashMap<>();
        
        if (itemId > 0 && count > 0) {
            validate();
        }
    }
    
    private void validate() {
        ItemDefinition item = getItem();
        if (item.getDurability() > 0 && durability > item.getDurability()) {
            throw new IllegalArgumentException("Durability exceeds max: " + durability + " > " + item.getDurability());
        }
    }
    
    // Static factory methods
    public static ItemStack of(int itemId, int count) {
        ItemDefinition item = ItemRegistry.getInstance().getItem(itemId);
        int durability = item.getDurability() > 0 ? item.getDurability() : -1;
        return new ItemStack(itemId, count, durability, null);
    }
    
    public static ItemStack of(int itemId, int count, int durability) {
        return new ItemStack(itemId, count, durability, null);
    }
    
    public static ItemStack empty() {
        return new ItemStack(0, 0, -1, null);
    }
    
    // Getters
    public int getItemId() { return itemId; }
    public int getCount() { return count; }
    public int getDurability() { return durability; }
    public Map<String, Object> getNbt() { return new HashMap<>(nbt); }
    
    public boolean isEmpty() { return itemId == 0 || count == 0; }
    
    public ItemDefinition getItem() {
        return ItemRegistry.getInstance().getItem(itemId);
    }
    
    public int getMaxStackSize() {
        return getItem().getMaxStackSize();
    }
    
    public boolean isFull() {
        return count >= getMaxStackSize();
    }
    
    public boolean canMergeWith(ItemStack other) {
        if (other == null || other.isEmpty() || this.isEmpty()) {
            return false;
        }
        if (this.itemId != other.itemId) {
            return false;
        }
        // Tools with different durability can't merge
        if (getItem().isTool() && this.durability != other.durability) {
            return false;
        }
        return true;
    }
    
    // Immutable operations (return new instances)
    public ItemStack withCount(int newCount) {
        if (newCount == count) return this;
        return new ItemStack(itemId, newCount, durability, nbt);
    }
    
    public ItemStack withDurability(int newDurability) {
        if (newDurability == durability) return this;
        return new ItemStack(itemId, count, newDurability, nbt);
    }
    
    public ItemStack split(int amount) {
        if (amount <= 0 || amount > count) {
            throw new IllegalArgumentException("Invalid split amount: " + amount);
        }
        return new ItemStack(itemId, amount, durability, nbt);
    }
    
    public ItemStack grow(int amount) {
        return withCount(count + amount);
    }
    
    public ItemStack shrink(int amount) {
        int newCount = Math.max(0, count - amount);
        return withCount(newCount);
    }
    
    public ItemStack damageTool(int amount) {
        if (!getItem().isTool()) {
            return this;
        }
        int newDurability = Math.max(0, durability - amount);
        return withDurability(newDurability);
    }
    
    public ItemStack copy() {
        return new ItemStack(itemId, count, durability, nbt);
    }
    
    // Serialization
    public String toJson() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize ItemStack", e);
        }
    }
    
    public static ItemStack fromJson(String json) {
        try {
            return MAPPER.readValue(json, ItemStack.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize ItemStack", e);
        }
    }
    
    @Override
    public String toString() {
        return "ItemStack{" +
                "item=" + getItem().getName() +
                ", count=" + count +
                (durability >= 0 ? ", durability=" + durability : "") +
                '}';
    }
}
