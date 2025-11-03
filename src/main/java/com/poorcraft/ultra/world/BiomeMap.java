package com.poorcraft.ultra.world;

import com.poorcraft.ultra.world.noise.FastNoiseLite;

/**
 * Computes biome assignments using temperature and moisture noise fields.
 */
public final class BiomeMap {
    private final FastNoiseLite temperatureNoise = new FastNoiseLite();
    private final FastNoiseLite moistureNoise = new FastNoiseLite();

    public void init(long seed) {
        temperatureNoise.SetSeed((int) seed);
        temperatureNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
        temperatureNoise.SetFrequency(0.005f);

        moistureNoise.SetSeed((int) (seed + 1000));
        moistureNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
        moistureNoise.SetFrequency(0.005f);
    }

    public BiomeType getBiome(int worldX, int worldZ) {
        float temperature = normalize(temperatureNoise.GetNoise(worldX, worldZ));
        float moisture = normalize(moistureNoise.GetNoise(worldX, worldZ));
        return BiomeType.fromTemperatureAndMoisture(temperature, moisture);
    }

    public BiomeDefinition getBiomeDefinition(int worldX, int worldZ) {
        return BiomeDefinition.forType(getBiome(worldX, worldZ));
    }

    private static float normalize(float value) {
        return (value + 1.0f) * 0.5f;
    }
}
