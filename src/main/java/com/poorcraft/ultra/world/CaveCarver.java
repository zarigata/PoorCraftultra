package com.poorcraft.ultra.world;

import com.poorcraft.ultra.voxel.Chunk;
import com.poorcraft.ultra.world.noise.NoiseGenerator;

/**
 * Cave generation using 3D noise threshold (CP v2.1).
 * Carves caves and ravines into terrain using 3D noise.
 */
public class CaveCarver {

    private final long seed;
    private final NoiseGenerator caveNoise;
    private final double caveThreshold;
    private final int minCaveY;
    private final int maxCaveY;

    /**
     * Creates a cave carver with the given world seed.
     */
    public CaveCarver(long seed) {
        this.seed = seed;
        this.caveNoise = NoiseGenerator.forCaves(seed);
        this.caveThreshold = 0.6; // Higher = fewer caves
        this.minCaveY = 1; // Don't carve at bedrock level
        this.maxCaveY = 12; // Don't carve near surface
    }

    /**
     * Carves caves into the given chunk.
     * Uses 3D noise threshold to determine which blocks to remove.
     */
    public void carveCaves(Chunk chunk) {
        int chunkX = chunk.getChunkX();
        int chunkZ = chunk.getChunkZ();

        // For each block in chunk (16×16×16)
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minCaveY; y <= maxCaveY; y++) {
                    // Skip if already air
                    if (chunk.getBlock(x, y, z) == 0) {
                        continue;
                    }

                    // Get world coordinates
                    int worldX = chunkX * 16 + x;
                    int worldZ = chunkZ * 16 + z;

                    // Sample 3D noise
                    double noise = caveNoise.noise3D(worldX, y, worldZ);

                    // If noise exceeds threshold, carve cave (set to air)
                    if (noise > caveThreshold) {
                        chunk.setBlock(x, y, z, (short) 0); // Air
                    }
                }
            }
        }

        // Mark chunk as dirty
        chunk.markDirty();
    }
}
