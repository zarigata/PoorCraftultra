package com.poorcraft.ultra.world;

import com.poorcraft.ultra.voxel.BlockType;

/**
 * Describes the block composition and parameters for a biome.
 */
public record BiomeDefinition(
    BiomeType type,
    BlockType surfaceBlock,
    BlockType subsurfaceBlock,
    BlockType stoneBlock,
    float treeChance,
    int minHeight,
    int maxHeight
) {
    public static BiomeDefinition forType(BiomeType type) {
        return switch (type) {
            case PLAINS -> new BiomeDefinition(
                BiomeType.PLAINS,
                BlockType.GRASS_PLAINS,
                BlockType.DIRT,
                BlockType.STONE,
                0.01f,
                60,
                10
            );
            case FOREST -> new BiomeDefinition(
                BiomeType.FOREST,
                BlockType.GRASS_FOREST,
                BlockType.DIRT,
                BlockType.STONE,
                0.1f,
                65,
                20
            );
            case DESERT -> new BiomeDefinition(
                BiomeType.DESERT,
                BlockType.SAND,
                BlockType.SAND,
                BlockType.STONE,
                0.0f,
                58,
                5
            );
        };
    }
}
