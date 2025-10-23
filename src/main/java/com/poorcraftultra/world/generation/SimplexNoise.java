package com.poorcraftultra.world.generation;

import org.joml.Vector3f;
import java.util.Random;

/**
 * Simplex noise implementation - Ken Perlin's improved noise algorithm.
 * <p>
 * Simplex noise offers several advantages over classic Perlin noise:
 * <ul>
 *   <li><b>Faster</b>: Fewer gradient lookups (N+1 vs 2^N corners)</li>
 *   <li><b>Better visual quality</b>: No directional artifacts</li>
 *   <li><b>Scales better</b>: Complexity grows linearly with dimensions</li>
 * </ul>
 * <p>
 * Uses simplex grid (triangles in 2D, tetrahedra in 3D) instead of hypercube grid.
 * <b>Thread-safety</b>: Immutable after construction, safe for concurrent use.
 */
public class SimplexNoise implements NoiseGenerator {
    
    private final long seed;
    private final int[] permutation;
    private final Vector3f[] gradients;
    
    // Skewing and unskewing factors for 2D
    private static final double F2 = 0.5 * (Math.sqrt(3.0) - 1.0);
    private static final double G2 = (3.0 - Math.sqrt(3.0)) / 6.0;
    
    // Skewing and unskewing factors for 3D
    private static final double F3 = 1.0 / 3.0;
    private static final double G3 = 1.0 / 6.0;
    
    /**
     * Creates a new Simplex noise generator with the specified seed.
     * 
     * @param seed the seed for deterministic generation
     */
    public SimplexNoise(long seed) {
        this.seed = seed;
        this.permutation = new int[512];
        this.gradients = new Vector3f[12];
        
        // Initialize permutation table
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
        
        // Pre-compute gradient vectors (12 edges of a cube)
        gradients[0] = new Vector3f(1, 1, 0);
        gradients[1] = new Vector3f(-1, 1, 0);
        gradients[2] = new Vector3f(1, -1, 0);
        gradients[3] = new Vector3f(-1, -1, 0);
        gradients[4] = new Vector3f(1, 0, 1);
        gradients[5] = new Vector3f(-1, 0, 1);
        gradients[6] = new Vector3f(1, 0, -1);
        gradients[7] = new Vector3f(-1, 0, -1);
        gradients[8] = new Vector3f(0, 1, 1);
        gradients[9] = new Vector3f(0, -1, 1);
        gradients[10] = new Vector3f(0, 1, -1);
        gradients[11] = new Vector3f(0, -1, -1);
    }
    
    @Override
    public double noise2D(double x, double z) {
        // Skew input space to determine which simplex cell we're in
        double s = (x + z) * F2;
        int i = fastFloor(x + s);
        int j = fastFloor(z + s);
        
        // Unskew cell origin back to (x,z) space
        double t = (i + j) * G2;
        double X0 = i - t;
        double Z0 = j - t;
        double x0 = x - X0;
        double z0 = z - Z0;
        
        // Determine which simplex we're in (lower or upper triangle)
        int i1, j1;
        if (x0 > z0) {
            i1 = 1; j1 = 0; // Lower triangle
        } else {
            i1 = 0; j1 = 1; // Upper triangle
        }
        
        // Offsets for middle and last corners
        double x1 = x0 - i1 + G2;
        double z1 = z0 - j1 + G2;
        double x2 = x0 - 1.0 + 2.0 * G2;
        double z2 = z0 - 1.0 + 2.0 * G2;
        
        // Hash coordinates
        int ii = i & 255;
        int jj = j & 255;
        int gi0 = permutation[ii + permutation[jj]] % 12;
        int gi1 = permutation[ii + i1 + permutation[jj + j1]] % 12;
        int gi2 = permutation[ii + 1 + permutation[jj + 1]] % 12;
        
        // Calculate contributions from three corners
        double n0 = contribution2D(x0, z0, gi0);
        double n1 = contribution2D(x1, z1, gi1);
        double n2 = contribution2D(x2, z2, gi2);
        
        // Sum contributions and scale to [-1, 1]
        return 70.0 * (n0 + n1 + n2);
    }
    
    @Override
    public double noise3D(double x, double y, double z) {
        // Skew input space
        double s = (x + y + z) * F3;
        int i = fastFloor(x + s);
        int j = fastFloor(y + s);
        int k = fastFloor(z + s);
        
        // Unskew cell origin
        double t = (i + j + k) * G3;
        double X0 = i - t;
        double Y0 = j - t;
        double Z0 = k - t;
        double x0 = x - X0;
        double y0 = y - Y0;
        double z0 = z - Z0;
        
        // Determine which simplex we're in
        int i1, j1, k1, i2, j2, k2;
        if (x0 >= y0) {
            if (y0 >= z0) {
                i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 1; k2 = 0;
            } else if (x0 >= z0) {
                i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 0; k2 = 1;
            } else {
                i1 = 0; j1 = 0; k1 = 1; i2 = 1; j2 = 0; k2 = 1;
            }
        } else {
            if (y0 < z0) {
                i1 = 0; j1 = 0; k1 = 1; i2 = 0; j2 = 1; k2 = 1;
            } else if (x0 < z0) {
                i1 = 0; j1 = 1; k1 = 0; i2 = 0; j2 = 1; k2 = 1;
            } else {
                i1 = 0; j1 = 1; k1 = 0; i2 = 1; j2 = 1; k2 = 0;
            }
        }
        
        // Offsets for corners
        double x1 = x0 - i1 + G3;
        double y1 = y0 - j1 + G3;
        double z1 = z0 - k1 + G3;
        double x2 = x0 - i2 + 2.0 * G3;
        double y2 = y0 - j2 + 2.0 * G3;
        double z2 = z0 - k2 + 2.0 * G3;
        double x3 = x0 - 1.0 + 3.0 * G3;
        double y3 = y0 - 1.0 + 3.0 * G3;
        double z3 = z0 - 1.0 + 3.0 * G3;
        
        // Hash coordinates
        int ii = i & 255;
        int jj = j & 255;
        int kk = k & 255;
        int gi0 = permutation[ii + permutation[jj + permutation[kk]]] % 12;
        int gi1 = permutation[ii + i1 + permutation[jj + j1 + permutation[kk + k1]]] % 12;
        int gi2 = permutation[ii + i2 + permutation[jj + j2 + permutation[kk + k2]]] % 12;
        int gi3 = permutation[ii + 1 + permutation[jj + 1 + permutation[kk + 1]]] % 12;
        
        // Calculate contributions
        double n0 = contribution3D(x0, y0, z0, gi0);
        double n1 = contribution3D(x1, y1, z1, gi1);
        double n2 = contribution3D(x2, y2, z2, gi2);
        double n3 = contribution3D(x3, y3, z3, gi3);
        
        // Sum and scale to [-1, 1]
        return 32.0 * (n0 + n1 + n2 + n3);
    }
    
    @Override
    public long getSeed() {
        return seed;
    }
    
    /**
     * Fast floor function for integer conversion.
     */
    private int fastFloor(double x) {
        int xi = (int)x;
        return x < xi ? xi - 1 : xi;
    }
    
    /**
     * Calculates contribution from a corner in 2D.
     * Uses radial attenuation: max(0, r^2 - d^2)^4
     */
    private double contribution2D(double x, double z, int gi) {
        double t = 0.5 - x * x - z * z;
        if (t < 0) return 0.0;
        t *= t;
        Vector3f g = gradients[gi];
        return t * t * (g.x * x + g.z * z);
    }
    
    /**
     * Calculates contribution from a corner in 3D.
     */
    private double contribution3D(double x, double y, double z, int gi) {
        double t = 0.6 - x * x - y * y - z * z;
        if (t < 0) return 0.0;
        t *= t;
        Vector3f g = gradients[gi];
        return t * t * (g.x * x + g.y * y + g.z * z);
    }
}
