package com.poorcraft.ultra.gameplay;

import com.poorcraft.ultra.voxel.BlockType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Maps block types to item drop rules used when the player breaks blocks.
 */
public class ItemDropTable {

    private final Map<BlockType, DropRule> drops = new EnumMap<>(BlockType.class);
    private final Random random = new Random();

    public void init() {
        registerConstant(BlockType.STONE, ItemStack.of(BlockType.STONE, 1));
        registerConstant(BlockType.DIRT, ItemStack.of(BlockType.DIRT, 1));
        register(BlockType.GRASS, (block, tool, rng) -> List.of(
            ItemStack.of(BlockType.DIRT, 1),
            ItemStack.of(BlockType.GRASS, 1)
        ));
        register(BlockType.GRASS_PLAINS, (block, tool, rng) -> List.of(
            ItemStack.of(BlockType.DIRT, 1),
            ItemStack.of(BlockType.GRASS_PLAINS, 1)
        ));
        register(BlockType.GRASS_FOREST, (block, tool, rng) -> List.of(
            ItemStack.of(BlockType.DIRT, 1),
            ItemStack.of(BlockType.GRASS_FOREST, 1)
        ));
        register(BlockType.GRASS_DESERT, (block, tool, rng) -> List.of(
            ItemStack.of(BlockType.DIRT, 1),
            ItemStack.of(BlockType.GRASS_DESERT, 1)
        ));

        registerConstant(BlockType.WOOD_OAK, ItemStack.of(BlockType.WOOD_OAK, 1));

        register(BlockType.LEAVES_OAK, (block, tool, rng) -> {
            if (rng.nextFloat() < 0.2f) {
                return List.of(ItemStack.of(BlockType.LEAVES_OAK, 1));
            }
            return Collections.emptyList();
        });

        registerConstant(BlockType.SAND, ItemStack.of(BlockType.SAND, 1));
        registerConstant(BlockType.GRAVEL, ItemStack.of(BlockType.GRAVEL, 1));
        registerConstant(BlockType.COAL_ORE, ItemStack.of(BlockType.COAL_ORE, 1));
        registerConstant(BlockType.IRON_ORE, ItemStack.of(BlockType.IRON_ORE, 1));
        registerConstant(BlockType.GOLD_ORE, ItemStack.of(BlockType.GOLD_ORE, 1));
        registerConstant(BlockType.PLANKS, ItemStack.of(BlockType.PLANKS, 1));
        registerConstant(BlockType.TORCH, ItemStack.of(BlockType.TORCH, 1));
        registerConstant(BlockType.CHEST, ItemStack.of(BlockType.CHEST, 1));
    }

    public List<ItemStack> getDrops(BlockType block, BlockType tool) {
        DropRule rule = drops.get(block);
        if (rule == null) {
            return Collections.emptyList();
        }
        List<ItemStack> generated = rule.generate(block, tool, random);
        if (generated == null || generated.isEmpty()) {
            return Collections.emptyList();
        }
        List<ItemStack> result = new ArrayList<>(generated.size());
        for (ItemStack stack : generated) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            result.add(stack);
        }
        return Collections.unmodifiableList(result);
    }

    public void register(BlockType block, DropRule rule) {
        drops.put(block, rule);
    }

    private void registerConstant(BlockType block, ItemStack... items) {
        drops.put(block, (b, tool, rng) -> {
            List<ItemStack> copy = new ArrayList<>(items.length);
            Collections.addAll(copy, items);
            return copy;
        });
    }

    @FunctionalInterface
    public interface DropRule {
        List<ItemStack> generate(BlockType block, BlockType tool, Random random);
    }
}
