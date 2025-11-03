package com.poorcraft.ultra.gameplay;

import com.poorcraft.ultra.voxel.BlockType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Defines a shapeless crafting recipe limited to the 2x2 crafting grid used in Phase 2.
 */
public record CraftingRecipe(String id, List<BlockType> inputs, BlockType output, int outputCount) {

    public CraftingRecipe {
        if (inputs == null || inputs.isEmpty()) {
            throw new IllegalArgumentException("Recipe inputs cannot be empty");
        }
        if (inputs.size() > 4) {
            throw new IllegalArgumentException("Recipe inputs cannot exceed 4 (2x2 grid)");
        }
        if (output == null || output == BlockType.AIR) {
            throw new IllegalArgumentException("Recipe output cannot be AIR");
        }
        if (outputCount < 1 || outputCount > 64) {
            throw new IllegalArgumentException("Recipe output count must be 1-64");
        }
    }

    public boolean matches(List<BlockType> providedInputs) {
        if (providedInputs == null || providedInputs.size() != inputs.size()) {
            return false;
        }
        List<BlockType> sortedInputs = new ArrayList<>(inputs);
        List<BlockType> sortedProvided = new ArrayList<>(providedInputs);
        sortedInputs.sort(Comparator.comparing(BlockType::id));
        sortedProvided.sort(Comparator.comparing(BlockType::id));
        return sortedInputs.equals(sortedProvided);
    }
}
