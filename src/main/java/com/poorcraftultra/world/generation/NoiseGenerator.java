package com.poorcraftultra.world.generation;

/**
 * Interface for noise generation algorithms used in procedural world generation.
 * <p>
 * Implementations must be:
 * <ul>
 *   <li><b>Deterministic</b>: Same seed + coordinates must always produce the same output</li>
 *   <li><b>Thread-safe</b>: Must support concurrent chunk generation from multiple threads</li>
 * </ul>
 * <p>
 * Noise values are normalized to the range [-1, 1] for consistent usage across different algorithms.
 * 
 * @see PerlinNoise
 * @see SimplexNoise
 */
public interface NoiseGenerator {
    
    /**
     * Generates 2D noise at the specified coordinates.
     * Used primarily for height map generation.
     * 
     * @param x the x-coordinate in world space
     * @param z the z-coordinate in world space
     * @return noise value in range [-1, 1]
     */
    double noise2D(double x, double z);
    
    /**
     * Generates 3D noise at the specified coordinates.
     * Used primarily for cave generation and volumetric features.
     * 
     * @param x the x-coordinate in world space
     * @param y the y-coordinate in world space
     * @param z the z-coordinate in world space
     * @return noise value in range [-1, 1]
     */
    double noise3D(double x, double y, double z);
    
    /**
     * Returns the seed used by this noise generator.
     * 
     * @return the seed value
     */
    long getSeed();
}
