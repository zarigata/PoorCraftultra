package com.poorcraft.ultra.world;

import com.poorcraft.ultra.world.noise.NoiseGenerator;

/**
 * Biome distribution provider using temperature/moisture noise.
 * Determines biome type for any world coordinate using 2D noise maps.
 */
public class BiomeProvider {

    private final long seed;
    private final NoiseGenerator temperatureNoise;
    private final NoiseGenerator moistureNoise;

    /**
     * Creates a biome provider with the given world seed.
     */
    public BiomeProvider(long seed) {
        this.seed = seed;
        this.temperatureNoise = NoiseGenerator.forBiomes(seed);
        this.moistureNoise = NoiseGenerator.forBiomes(seed + 1000); // Offset seed for independence
    }

    /**
     * Returns the biome type for the given world coordinates.
     * Uses temperature and moisture noise to determine biome.
     */
    public BiomeType getBiome(int worldX, int worldZ) {
        return getBiomeAt(worldX, worldZ);
    }

    /**
     * Returns the biome type for the given floating-point coordinates.
     * Useful for smooth biome transitions.
     */
    public BiomeType getBiomeAt(double x, double z) {
        // Sample temperature and moisture (0-1 range)
        double temp = temperatureNoise.noise2D(x, z);
        double moisture = moistureNoise.noise2D(x, z);

        // Biome selection logic based on temperature and moisture
        if (temp > 0.7 && moisture < 0.3) {
            return BiomeType.DESERT;
        }
        if (temp < 0.3 && moisture < 0.4) {
            return BiomeType.MOUNTAINS;
        }
        if (temp < 0.4 && moisture > 0.5) {
            return BiomeType.TAIGA;
        }
        if (moisture > 0.6) {
            return BiomeType.FOREST;
        }
        
        return BiomeType.PLAINS; // Default
    }
}
