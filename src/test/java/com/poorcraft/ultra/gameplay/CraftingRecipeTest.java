package com.poorcraft.ultra.gameplay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poorcraft.ultra.voxel.BlockType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CraftingRecipeTest {

    @Test
    void matchesIgnoresOrder() {
        CraftingRecipe recipe = new CraftingRecipe(
            "test",
            List.of(BlockType.WOOD_OAK, BlockType.PLANKS),
            BlockType.CHEST,
            1
        );

        assertTrue(recipe.matches(List.of(BlockType.PLANKS, BlockType.WOOD_OAK)));
    }

    @Test
    void matchesFailsWhenMissingIngredient() {
        CraftingRecipe recipe = new CraftingRecipe(
            "test",
            List.of(BlockType.WOOD_OAK, BlockType.PLANKS),
            BlockType.CHEST,
            1
        );

        assertFalse(recipe.matches(List.of(BlockType.WOOD_OAK)));
    }

    @Test
    void serializesAndDeserializesWithJackson() throws IOException {
        CraftingRecipe recipe = new CraftingRecipe(
            "serialize",
            List.of(BlockType.PLANKS, BlockType.PLANKS),
            BlockType.TORCH,
            4
        );

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(recipe);
        CraftingRecipe roundTrip = mapper.readValue(json, CraftingRecipe.class);

        assertEquals(recipe, roundTrip);
    }

    @Test
    void constructorRejectsMoreThanFourInputs() {
        assertThrows(IllegalArgumentException.class, () -> new CraftingRecipe(
            "tooMany",
            List.of(
                BlockType.WOOD_OAK,
                BlockType.WOOD_OAK,
                BlockType.WOOD_OAK,
                BlockType.WOOD_OAK,
                BlockType.WOOD_OAK
            ),
            BlockType.CHEST,
            1
        ));
    }
}
