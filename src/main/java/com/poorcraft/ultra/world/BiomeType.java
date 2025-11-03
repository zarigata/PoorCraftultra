package com.poorcraft.ultra.world;

/**
 * Enum for biome types with their properties.
 * Each biome defines surface/filler blocks, height characteristics, and vegetation density.
 */
public enum BiomeType {
    
    PLAINS(
        (short) 5,  // grass
        (short) 4,  // dirt
        6,          // base height
        3,          // height variation
        0.1f,       // tree density
        "oak"       // tree type
    ),
    
    FOREST(
        (short) 5,  // grass
        (short) 4,  // dirt
        7,          // base height
        2,          // height variation
        0.4f,       // tree density
        "oak"       // tree type
    ),
    
    DESERT(
        (short) 6,  // sand
        (short) 6,  // sand
        5,          // base height
        2,          // height variation
        0.0f,       // tree density
        "none"      // tree type
    ),
    
    MOUNTAINS(
        (short) 1,  // stone
        (short) 1,  // stone
        10,         // base height
        6,          // height variation
        0.05f,      // tree density
        "spruce"    // tree type
    ),
    
    TAIGA(
        (short) 5,  // grass
        (short) 4,  // dirt
        6,          // base height
        3,          // height variation
        0.3f,       // tree density
        "spruce"    // tree type
    );

    private final short surfaceBlock;
    private final short fillerBlock;
    private final int baseHeight;
    private final int heightVariation;
    private final float treeDensity;
    private final String treeType;

    BiomeType(short surfaceBlock, short fillerBlock, int baseHeight, int heightVariation, 
              float treeDensity, String treeType) {
        this.surfaceBlock = surfaceBlock;
        this.fillerBlock = fillerBlock;
        this.baseHeight = baseHeight;
        this.heightVariation = heightVariation;
        this.treeDensity = treeDensity;
        this.treeType = treeType;
    }

    public short getSurfaceBlock() {
        return surfaceBlock;
    }

    public short getFillerBlock() {
        return fillerBlock;
    }

    public int getBaseHeight() {
        return baseHeight;
    }

    public int getHeightVariation() {
        return heightVariation;
    }

    public float getTreeDensity() {
        return treeDensity;
    }

    public String getTreeType() {
        return treeType;
    }
}
