package com.poorcraft.ultra.world;

import com.poorcraft.ultra.world.noise.FastNoiseLite;

/**
 * Determines cave locations using 3D Perlin noise.
 */
public final class CaveCarver {
    private static final int NEAR_SURFACE_BUFFER = 2;
    private final FastNoiseLite noise = new FastNoiseLite();

    public void init(long seed) {
        noise.SetSeed((int) (seed + 2000));
        noise.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
        noise.SetFrequency(0.05f);
    }

    public boolean shouldCarve(int worldX, int worldY, int worldZ, int surfaceHeight) {
        if (worldY >= surfaceHeight) {
            return false;
        }
        if (worldY > surfaceHeight - NEAR_SURFACE_BUFFER) {
            return false;
        }
        if (worldY < 10) {
            return false;
        }
        return isCave(worldX, worldY, worldZ);
    }

    private boolean isCave(int worldX, int worldY, int worldZ) {
        float value = noise.GetNoise(worldX, worldY, worldZ);
        return value > 0.3f;
    }
}
