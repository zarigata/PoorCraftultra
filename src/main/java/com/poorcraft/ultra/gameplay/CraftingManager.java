package com.poorcraft.ultra.gameplay;

import com.poorcraft.ultra.player.PlayerInventory;
import com.poorcraft.ultra.voxel.BlockType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Registers and evaluates shapeless crafting recipes.
 */
public class CraftingManager {

    private final Map<String, CraftingRecipe> recipes = new LinkedHashMap<>();

    public void init() {
        registerRecipe(new CraftingRecipe(
            "planks",
            Collections.singletonList(BlockType.WOOD_OAK),
            BlockType.PLANKS,
            4
        ));

        registerRecipe(new CraftingRecipe(
            "torch",
            List.of(BlockType.COAL_ORE, BlockType.WOOD_OAK),
            BlockType.TORCH,
            4
        ));

        registerRecipe(new CraftingRecipe(
            "chest",
            List.of(
                BlockType.PLANKS,
                BlockType.PLANKS,
                BlockType.PLANKS,
                BlockType.PLANKS
            ),
            BlockType.CHEST,
            1
        ));
    }

    public void registerRecipe(CraftingRecipe recipe) {
        Objects.requireNonNull(recipe, "recipe");
        recipes.put(recipe.id(), recipe);
    }

    public Optional<CraftingRecipe> findRecipe(List<BlockType> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return Optional.empty();
        }
        List<BlockType> filtered = inputs.stream()
            .filter(type -> type != null && type != BlockType.AIR)
            .collect(Collectors.toList());
        if (filtered.isEmpty()) {
            return Optional.empty();
        }
        for (CraftingRecipe recipe : recipes.values()) {
            if (recipe.matches(filtered)) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    public ItemStack craft(List<BlockType> inputs, PlayerInventory inventory) {
        if (inventory == null) {
            return null;
        }
        Optional<CraftingRecipe> match = findRecipe(inputs);
        if (match.isEmpty()) {
            return null;
        }
        CraftingRecipe recipe = match.get();

        EnumMap<BlockType, Integer> required = new EnumMap<>(BlockType.class);
        for (BlockType type : recipe.inputs()) {
            if (type == null || type == BlockType.AIR) {
                continue;
            }
            required.merge(type, 1, Integer::sum);
        }

        for (Map.Entry<BlockType, Integer> entry : required.entrySet()) {
            int available = inventory.getCount(entry.getKey());
            if (available < entry.getValue()) {
                return null;
            }
        }

        List<Map.Entry<BlockType, Integer>> appliedRemovals = new ArrayList<>();
        for (Map.Entry<BlockType, Integer> entry : required.entrySet()) {
            boolean removed = inventory.removeBlock(entry.getKey(), entry.getValue());
            if (!removed) {
                for (Map.Entry<BlockType, Integer> applied : appliedRemovals) {
                    inventory.addBlock(applied.getKey(), applied.getValue());
                }
                return null;
            }
            appliedRemovals.add(Map.entry(entry.getKey(), entry.getValue()));
        }

        inventory.addBlock(recipe.output(), recipe.outputCount());
        return ItemStack.of(recipe.output(), recipe.outputCount());
    }

    public List<CraftingRecipe> getRecipes() {
        return List.copyOf(recipes.values());
    }
}
