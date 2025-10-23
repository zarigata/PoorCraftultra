package com.poorcraftultra.world.generation;

import org.joml.Vector3f;
import java.util.Random;

/**
 * Classic Perlin noise implementation for procedural terrain generation.
 * <p>
 * Perlin noise is a gradient noise function that produces smooth, continuous random values.
 * This implementation uses:
 * <ul>
 *   <li>Seed-based permutation table for deterministic output</li>
 *   <li>Pre-computed gradient vectors for performance</li>
 *   <li>Improved fade curve (6t^5 - 15t^4 + 10t^3) for smooth interpolation</li>
 * </ul>
 * <p>
 * <b>Performance</b>: O(1) per sample with constant-time gradient lookups.
 * <b>Thread-safety</b>: Immutable after construction, safe for concurrent use.
 * 
 * @see SimplexNoise for a faster alternative with better visual properties
 */
public class PerlinNoise implements NoiseGenerator {
    
    private final long seed;
    private final int[] permutation;
    private final Vector3f[] gradients;
    
    /**
     * Creates a new Perlin noise generator with the specified seed.
     * 
     * @param seed the seed for deterministic generation
     */
    public PerlinNoise(long seed) {
        this.seed = seed;
        this.permutation = new int[512];
        this.gradients = new Vector3f[256];
        
        // Initialize permutation table with seed-based random
        Random random = new Random(seed);
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) {
            p[i] = i;
        }
        
        // Fisher-Yates shuffle
        for (int i = 255; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = p[i];
            p[i] = p[j];
            p[j] = temp;
        }
        
        // Duplicate for wrapping
        for (int i = 0; i < 512; i++) {
            permutation[i] = p[i & 255];
        }
        
        // Pre-compute gradient vectors
        for (int i = 0; i < 256; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double z = random.nextDouble() * 2 - 1;
            double r = Math.sqrt(1 - z * z);
            gradients[i] = new Vector3f(
                (float)(r * Math.cos(angle)),
                (float)(r * Math.sin(angle)),
                (float)z
            );
        }
    }
    
    @Override
    public double noise2D(double x, double z) {
        // Find unit grid cell containing point
        int X = (int)Math.floor(x) & 255;
        int Z = (int)Math.floor(z) & 255;
        
        // Get relative position within cell
        x -= Math.floor(x);
        z -= Math.floor(z);
        
        // Compute fade curves
        double u = fade(x);
        double w = fade(z);
        
        // Hash coordinates of 4 corners
        int aa = permutation[permutation[X] + Z];
        int ab = permutation[permutation[X] + Z + 1];
        int ba = permutation[permutation[X + 1] + Z];
        int bb = permutation[permutation[X + 1] + Z + 1];
        
        // Compute gradients and dot products
        double g1 = grad2D(aa, x, z);
        double g2 = grad2D(ba, x - 1, z);
        double g3 = grad2D(ab, x, z - 1);
        double g4 = grad2D(bb, x - 1, z - 1);
        
        // Bilinear interpolation
        double x1 = lerp(u, g1, g2);
        double x2 = lerp(u, g3, g4);
        return lerp(w, x1, x2);
    }
    
    @Override
    public double noise3D(double x, double y, double z) {
        // Find unit cube containing point
        int X = (int)Math.floor(x) & 255;
        int Y = (int)Math.floor(y) & 255;
        int Z = (int)Math.floor(z) & 255;
        
        // Get relative position within cube
        x -= Math.floor(x);
        y -= Math.floor(y);
        z -= Math.floor(z);
        
        // Compute fade curves
        double u = fade(x);
        double v = fade(y);
        double w = fade(z);
        
        // Hash coordinates of 8 corners
        int aaa = permutation[permutation[permutation[X] + Y] + Z];
        int aba = permutation[permutation[permutation[X] + Y + 1] + Z];
        int aab = permutation[permutation[permutation[X] + Y] + Z + 1];
        int abb = permutation[permutation[permutation[X] + Y + 1] + Z + 1];
        int baa = permutation[permutation[permutation[X + 1] + Y] + Z];
        int bba = permutation[permutation[permutation[X + 1] + Y + 1] + Z];
        int bab = permutation[permutation[permutation[X + 1] + Y] + Z + 1];
        int bbb = permutation[permutation[permutation[X + 1] + Y + 1] + Z + 1];
        
        // Compute gradients and dot products
        double g1 = grad(aaa, x, y, z);
        double g2 = grad(baa, x - 1, y, z);
        double g3 = grad(aba, x, y - 1, z);
        double g4 = grad(bba, x - 1, y - 1, z);
        double g5 = grad(aab, x, y, z - 1);
        double g6 = grad(bab, x - 1, y, z - 1);
        double g7 = grad(abb, x, y - 1, z - 1);
        double g8 = grad(bbb, x - 1, y - 1, z - 1);
        
        // Trilinear interpolation
        double x1 = lerp(u, g1, g2);
        double x2 = lerp(u, g3, g4);
        double x3 = lerp(u, g5, g6);
        double x4 = lerp(u, g7, g8);
        double y1 = lerp(v, x1, x2);
        double y2 = lerp(v, x3, x4);
        return lerp(w, y1, y2);
    }
    
    @Override
    public long getSeed() {
        return seed;
    }
    
    /**
     * Improved fade curve: 6t^5 - 15t^4 + 10t^3
     * Provides smooth interpolation with zero first and second derivatives at boundaries.
     */
    private double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }
    
    /**
     * Linear interpolation between two values.
     */
    private double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }
    
    /**
     * Computes gradient dot product for 2D noise.
     */
    private double grad2D(int hash, double x, double z) {
        Vector3f g = gradients[hash & 255];
        return g.x * x + g.z * z;
    }
    
    /**
     * Computes gradient dot product for 3D noise.
     */
    private double grad(int hash, double x, double y, double z) {
        Vector3f g = gradients[hash & 255];
        return g.x * x + g.y * y + g.z * z;
    }
}
