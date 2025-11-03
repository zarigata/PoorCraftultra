package com.poorcraft.ultra.world.noise;

/**
 * Wrapper utility for noise generation with octaves/FBM (Fractal Brownian Motion).
 * Provides convenient methods for multi-octave noise using OpenSimplex2.
 */
public class NoiseGenerator {

    private final long seed;
    private final int octaves;
    private final double persistence;
    private final double lacunarity;
    private final double scale;

    /**
     * Creates a noise generator with custom parameters.
     *
     * @param seed World seed
     * @param octaves Number of octaves (default 4)
     * @param persistence Amplitude multiplier per octave (default 0.5)
     * @param lacunarity Frequency multiplier per octave (default 2.0)
     * @param scale Base frequency scale (default 1.0)
     */
    public NoiseGenerator(long seed, int octaves, double persistence, double lacunarity, double scale) {
        this.seed = seed;
        this.octaves = octaves;
        this.persistence = persistence;
        this.lacunarity = lacunarity;
        this.scale = scale;
    }

    /**
     * Returns 2D FBM noise in range [0, 1].
     */
    public double noise2D(double x, double z) {
        double total = 0.0;
        double amplitude = 1.0;
        double frequency = scale;
        double maxValue = 0.0;

        for (int i = 0; i < octaves; i++) {
            total += OpenSimplex2.noise2(seed + i, x * frequency, z * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= lacunarity;
        }

        // Normalize to [0, 1]
        return (total / maxValue + 1.0) * 0.5;
    }

    /**
     * Returns 3D FBM noise in range [0, 1].
     * Uses noise3_ImproveXZ for terrain-optimized 3D noise.
     */
    public double noise3D(double x, double y, double z) {
        double total = 0.0;
        double amplitude = 1.0;
        double frequency = scale;
        double maxValue = 0.0;

        for (int i = 0; i < octaves; i++) {
            total += OpenSimplex2.noise3_ImproveXZ(seed + i, x * frequency, y * frequency, z * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= lacunarity;
        }

        // Normalize to [0, 1]
        return (total / maxValue + 1.0) * 0.5;
    }

    /**
     * Returns raw 2D noise in range [-1, 1] (no normalization).
     */
    public double noise2DRaw(double x, double z) {
        double total = 0.0;
        double amplitude = 1.0;
        double frequency = scale;

        for (int i = 0; i < octaves; i++) {
            total += OpenSimplex2.noise2(seed + i, x * frequency, z * frequency) * amplitude;
            amplitude *= persistence;
            frequency *= lacunarity;
        }

        return total;
    }

    /**
     * Returns raw 3D noise in range [-1, 1] (no normalization).
     */
    public double noise3DRaw(double x, double y, double z) {
        double total = 0.0;
        double amplitude = 1.0;
        double frequency = scale;

        for (int i = 0; i < octaves; i++) {
            total += OpenSimplex2.noise3_ImproveXZ(seed + i, x * frequency, y * frequency, z * frequency) * amplitude;
            amplitude *= persistence;
            frequency *= lacunarity;
        }

        return total;
    }

    /**
     * Factory method: Returns NoiseGenerator configured for terrain heightmaps.
     * 4 octaves, scale 0.01 for large-scale terrain features.
     */
    public static NoiseGenerator forHeightmap(long seed) {
        return new NoiseGenerator(seed, 4, 0.5, 2.0, 0.01);
    }

    /**
     * Factory method: Returns NoiseGenerator for biome distribution.
     * 3 octaves, scale 0.005 for very large-scale biome regions.
     */
    public static NoiseGenerator forBiomes(long seed) {
        return new NoiseGenerator(seed, 3, 0.5, 2.0, 0.005);
    }

    /**
     * Factory method: Returns NoiseGenerator for cave carving.
     * 2 octaves, scale 0.05 for medium-scale cave systems.
     */
    public static NoiseGenerator forCaves(long seed) {
        return new NoiseGenerator(seed, 2, 0.5, 2.0, 0.05);
    }
}
