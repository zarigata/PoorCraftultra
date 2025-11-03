package com.poorcraft.ultra.world;

import com.poorcraft.ultra.world.noise.FastNoiseLite;

/**
 * Generates terrain heights using OpenSimplex noise.
 */
public final class TerrainGenerator {
    private final FastNoiseLite noise = new FastNoiseLite();

    public void init(long seed) {
        noise.SetSeed((int) seed);
        noise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
        noise.SetFrequency(0.01f);
    }

    public int getHeight(int worldX, int worldZ, BiomeDefinition biome) {
        float normalized = sample(worldX, worldZ);
        float heightRange = biome.maxHeight();
        float height = biome.minHeight() + normalized * heightRange;
        return (int) Math.floor(height);
    }

    public float getHeightSmooth(float worldX, float worldZ, BiomeDefinition biome) {
        float normalized = sample(worldX, worldZ);
        float heightRange = biome.maxHeight();
        return biome.minHeight() + normalized * heightRange;
    }

    private float sample(float worldX, float worldZ) {
        float value = noise.GetNoise(worldX, worldZ);
        return (value + 1.0f) * 0.5f;
    }
}
