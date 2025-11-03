package com.poorcraft.ultra.world;

import com.poorcraft.ultra.world.noise.NoiseGenerator;

/**
 * Terrain heightmap generator using noise.
 * Generates base terrain height for any world coordinate.
 */
public class HeightmapGenerator {

    private final long seed;
    private final NoiseGenerator baseNoise;
    private final NoiseGenerator detailNoise;
    private final BiomeProvider biomeProvider;

    /**
     * Creates a heightmap generator with the given seed and biome provider.
     */
    public HeightmapGenerator(long seed, BiomeProvider biomeProvider) {
        this.seed = seed;
        this.biomeProvider = biomeProvider;
        // Base terrain shape (large scale)
        this.baseNoise = NoiseGenerator.forHeightmap(seed);
        // Fine detail (small scale)
        this.detailNoise = new NoiseGenerator(seed + 500, 2, 0.3, 2.0, 0.05);
    }

    /**
     * Returns terrain height (Y coordinate 0-15) for the given world coordinates.
     */
    public int getHeight(int worldX, int worldZ) {
        // Get biome for this location
        BiomeType biome = biomeProvider.getBiome(worldX, worldZ);
        
        // Sample base noise (0-1)
        double baseHeight = baseNoise.noise2D(worldX, worldZ);
        
        // Sample detail noise (0-1)
        double detail = detailNoise.noise2D(worldX, worldZ);
        
        // Combine: base height + biome variation + detail
        double height = biome.getBaseHeight() 
                      + (baseHeight * biome.getHeightVariation()) 
                      + (detail * 2 - 1); // Detail in range [-1, 1]
        
        // Clamp to [0, 15]
        return Math.max(0, Math.min(15, (int) height));
    }

    /**
     * Returns floating-point height for smooth interpolation.
     * Used for biome blending at chunk edges.
     */
    public double getHeightSmooth(double x, double z) {
        BiomeType biome = biomeProvider.getBiomeAt(x, z);
        double baseHeight = baseNoise.noise2D(x, z);
        double detail = detailNoise.noise2D(x, z);
        
        double height = biome.getBaseHeight() 
                      + (baseHeight * biome.getHeightVariation()) 
                      + (detail * 2 - 1);
        
        return Math.max(0.0, Math.min(15.0, height));
    }
}
