package com.poorcraft.ultra.world;

import com.poorcraft.ultra.voxel.Chunk;

import java.util.Random;

/**
 * Places surface features: trees, ores, structures (CP v2.1).
 * Adds decorative features to generated terrain.
 */
public class FeaturePopulator {

    private final long seed;
    private final BiomeProvider biomeProvider;
    private final Random random;

    /**
     * Creates a feature populator with the given seed and biome provider.
     */
    public FeaturePopulator(long seed, BiomeProvider biomeProvider) {
        this.seed = seed;
        this.biomeProvider = biomeProvider;
        this.random = new Random();
    }

    /**
     * Populates the given chunk with features (trees, ores, structures).
     */
    public void populate(Chunk chunk, HeightmapGenerator heightmap) {
        int chunkX = chunk.getChunkX();
        int chunkZ = chunk.getChunkZ();

        // Seed chunk-specific random
        random.setSeed(seed ^ (chunkX * 341873128712L + chunkZ * 132897987541L));

        // Place features
        placeTrees(chunk, heightmap);
        placeOres(chunk);
        placeStructures(chunk, heightmap);

        // Mark chunk as dirty
        chunk.markDirty();
    }

    /**
     * Places trees based on biome density.
     */
    private void placeTrees(Chunk chunk, HeightmapGenerator heightmap) {
        int chunkX = chunk.getChunkX();
        int chunkZ = chunk.getChunkZ();

        // Get biome for chunk center
        int centerX = chunkX * 16 + 8;
        int centerZ = chunkZ * 16 + 8;
        BiomeType biome = biomeProvider.getBiome(centerX, centerZ);

        // Roll for tree placement
        if (random.nextFloat() < biome.getTreeDensity()) {
            // Choose random position in chunk
            int x = random.nextInt(16);
            int z = random.nextInt(16);

            // Get surface height
            int worldX = chunkX * 16 + x;
            int worldZ = chunkZ * 16 + z;
            int height = heightmap.getHeight(worldX, worldZ);

            // Check if surface block is grass or dirt (valid for tree)
            short surfaceBlock = chunk.getBlock(x, height, z);
            if (surfaceBlock == 5 || surfaceBlock == 4) { // Grass or dirt
                placeTree(chunk, x, height + 1, z, biome.getTreeType());
            }
        }
    }

    /**
     * Places a tree at the given position.
     */
    private void placeTree(Chunk chunk, int x, int y, int z, String treeType) {
        // Check bounds - don't place if tree extends outside chunk
        if (x < 2 || x > 13 || z < 2 || z > 13 || y > 10) {
            return; // Defer to neighbor chunk
        }

        if ("oak".equals(treeType)) {
            // Oak tree: 4-5 blocks tall log, 3×3×2 leaves on top
            int height = 4 + random.nextInt(2);
            
            // Place log
            for (int dy = 0; dy < height; dy++) {
                if (y + dy < 16) {
                    chunk.setBlock(x, y + dy, z, (short) 11); // Wood log
                }
            }
            
            // Place leaves (3×3×2)
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dy = 0; dy < 2; dy++) {
                        int leafY = y + height + dy;
                        if (leafY < 16) {
                            chunk.setBlock(x + dx, leafY, z + dz, (short) 19); // Leaves
                        }
                    }
                }
            }
        } else if ("spruce".equals(treeType)) {
            // Spruce tree: 6-7 blocks tall log, 5×5×3 leaves in cone shape
            int height = 6 + random.nextInt(2);
            
            // Place log
            for (int dy = 0; dy < height; dy++) {
                if (y + dy < 16) {
                    chunk.setBlock(x, y + dy, z, (short) 11); // Wood log
                }
            }
            
            // Place leaves in cone shape (simplified)
            for (int dy = 0; dy < 3; dy++) {
                int leafY = y + height + dy;
                int radius = 2 - dy; // Cone shape
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (leafY < 16 && Math.abs(dx) + Math.abs(dz) <= radius) {
                            chunk.setBlock(x + dx, leafY, z + dz, (short) 19); // Leaves
                        }
                    }
                }
            }
        }
    }

    /**
     * Places ore veins in the chunk.
     */
    private void placeOres(Chunk chunk) {
        // Coal ore (ID 12): 1-3 veins per chunk, Y=1-12
        placeOreVeins(chunk, (short) 12, 1 + random.nextInt(3), 1, 12, 3 + random.nextInt(6));

        // Iron ore (ID 13): 1-2 veins per chunk, Y=1-10
        placeOreVeins(chunk, (short) 13, 1 + random.nextInt(2), 1, 10, 3 + random.nextInt(6));

        // Gold ore (ID 14): 0-1 veins per chunk, Y=1-8
        if (random.nextFloat() < 0.5f) {
            placeOreVeins(chunk, (short) 14, 1, 1, 8, 2 + random.nextInt(4));
        }

        // Diamond ore (ID 15): 0-1 veins per chunk, Y=1-6
        if (random.nextFloat() < 0.3f) {
            placeOreVeins(chunk, (short) 15, 1, 1, 6, 2 + random.nextInt(3));
        }
    }

    /**
     * Places ore veins of the given type.
     */
    private void placeOreVeins(Chunk chunk, short oreId, int veinCount, int minY, int maxY, int veinSize) {
        for (int i = 0; i < veinCount; i++) {
            // Choose random position
            int x = random.nextInt(16);
            int y = minY + random.nextInt(maxY - minY + 1);
            int z = random.nextInt(16);

            // Place vein (random walk from center)
            placeOreCluster(chunk, x, y, z, oreId, veinSize);
        }
    }

    /**
     * Places an ore cluster using random walk.
     */
    private void placeOreCluster(Chunk chunk, int startX, int startY, int startZ, short oreId, int size) {
        int x = startX;
        int y = startY;
        int z = startZ;

        for (int i = 0; i < size; i++) {
            // Only replace stone blocks
            if (x >= 0 && x < 16 && y >= 0 && y < 16 && z >= 0 && z < 16) {
                if (chunk.getBlock(x, y, z) == 1) { // Stone
                    chunk.setBlock(x, y, z, oreId);
                }
            }

            // Random walk
            x += random.nextInt(3) - 1; // -1, 0, or 1
            y += random.nextInt(3) - 1;
            z += random.nextInt(3) - 1;
        }
    }

    /**
     * Places structures (simple huts).
     */
    private void placeStructures(Chunk chunk, HeightmapGenerator heightmap) {
        // Low probability: 2% chance per chunk
        if (random.nextFloat() < 0.02f) {
            int chunkX = chunk.getChunkX();
            int chunkZ = chunk.getChunkZ();

            // Choose random position
            int x = 4 + random.nextInt(8); // Keep away from edges
            int z = 4 + random.nextInt(8);

            // Get surface height
            int worldX = chunkX * 16 + x;
            int worldZ = chunkZ * 16 + z;
            int height = heightmap.getHeight(worldX, worldZ);

            // Check if area is relatively flat
            if (isFlatArea(chunk, heightmap, x, z, height)) {
                placeSimpleHut(chunk, x, height + 1, z);
            }
        }
    }

    /**
     * Checks if the area is flat enough for a structure.
     */
    private boolean isFlatArea(Chunk chunk, HeightmapGenerator heightmap, int x, int z, int baseHeight) {
        int chunkX = chunk.getChunkX();
        int chunkZ = chunk.getChunkZ();

        // Check 5×5 area
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int worldX = chunkX * 16 + x + dx;
                int worldZ = chunkZ * 16 + z + dz;
                int height = heightmap.getHeight(worldX, worldZ);
                
                // Height variation must be < 2
                if (Math.abs(height - baseHeight) >= 2) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Places a simple hut structure (5×5×4 wood planks + door).
     */
    private void placeSimpleHut(Chunk chunk, int x, int y, int z) {
        // Check bounds
        if (x < 2 || x > 13 || z < 2 || z > 13 || y > 11) {
            return;
        }

        // Place floor (5×5 oak planks)
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (y - 1 >= 0) {
                    chunk.setBlock(x + dx, y - 1, z + dz, (short) 8); // Oak planks
                }
            }
        }

        // Place walls (hollow 5×5×4)
        for (int dy = 0; dy < 4; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    // Only place on edges
                    if (dx == -2 || dx == 2 || dz == -2 || dz == 2) {
                        if (y + dy < 16) {
                            // Leave door opening on one side
                            if (!(dy < 2 && dx == 0 && dz == -2)) {
                                chunk.setBlock(x + dx, y + dy, z + dz, (short) 8); // Oak planks
                            }
                        }
                    }
                }
            }
        }

        // Place roof (5×5 oak planks)
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (y + 4 < 16) {
                    chunk.setBlock(x + dx, y + 4, z + dz, (short) 8); // Oak planks
                }
            }
        }
    }
}
