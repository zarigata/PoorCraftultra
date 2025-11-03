package com.poorcraft.ultra.world;

import com.poorcraft.ultra.voxel.Chunk;

/**
 * Fills chunk with base terrain blocks based on heightmap and biome.
 */
public class ChunkPopulator {

    private final HeightmapGenerator heightmapGenerator;
    private final BiomeProvider biomeProvider;

    /**
     * Creates a chunk populator with the given heightmap generator and biome provider.
     */
    public ChunkPopulator(HeightmapGenerator heightmapGenerator, BiomeProvider biomeProvider) {
        this.heightmapGenerator = heightmapGenerator;
        this.biomeProvider = biomeProvider;
    }

    /**
     * Populates the given chunk with terrain blocks.
     * Fills stone/dirt/grass based on height and biome.
     */
    public void populate(Chunk chunk) {
        int chunkX = chunk.getChunkX();
        int chunkZ = chunk.getChunkZ();

        // For each XZ column in chunk (16×16)
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                // Get world coordinates
                int worldX = chunkX * 16 + x;
                int worldZ = chunkZ * 16 + z;

                // Get height and biome
                int height = heightmapGenerator.getHeight(worldX, worldZ);
                BiomeType biome = biomeProvider.getBiome(worldX, worldZ);

                // Fill column
                for (int y = 0; y <= height; y++) {
                    if (y < height - 2) {
                        // Bedrock/stone layer
                        chunk.setBlock(x, y, z, (short) 1); // Stone
                    } else if (y < height) {
                        // Filler layer (biome-specific)
                        chunk.setBlock(x, y, z, biome.getFillerBlock());
                    } else {
                        // Surface layer (biome-specific)
                        chunk.setBlock(x, y, z, biome.getSurfaceBlock());
                    }
                }
                
                // Y=height+1 to Y=15 remain air (default 0)
            }
        }

        // Mark chunk as dirty for meshing
        chunk.markDirty();
    }
}
