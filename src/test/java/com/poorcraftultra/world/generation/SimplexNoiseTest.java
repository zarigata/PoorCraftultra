package com.poorcraftultra.world.generation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Simplex Noise Tests")
class SimplexNoiseTest {
    
    @Test
    @DisplayName("Same seed produces deterministic output")
    void testDeterminism() {
        SimplexNoise noise1 = new SimplexNoise(12345L);
        SimplexNoise noise2 = new SimplexNoise(12345L);
        
        for (int i = 0; i < 100; i++) {
            double x = i * 0.1;
            double z = i * 0.2;
            assertEquals(noise1.noise2D(x, z), noise2.noise2D(x, z), 1e-10);
            
            double y = i * 0.15;
            assertEquals(noise1.noise3D(x, y, z), noise2.noise3D(x, y, z), 1e-10);
        }
    }
    
    @Test
    @DisplayName("Different seeds produce different output")
    void testDifferentSeeds() {
        SimplexNoise noise1 = new SimplexNoise(12345L);
        SimplexNoise noise2 = new SimplexNoise(54321L);
        
        int differentCount = 0;
        for (int i = 0; i < 100; i++) {
            double x = i * 0.1;
            double z = i * 0.2;
            if (Math.abs(noise1.noise2D(x, z) - noise2.noise2D(x, z)) > 1e-6) {
                differentCount++;
            }
        }
        
        assertTrue(differentCount > 90, "Different seeds should produce mostly different values");
    }
    
    @Test
    @DisplayName("Output is in range [-1, 1]")
    void testOutputRange() {
        SimplexNoise noise = new SimplexNoise(12345L);
        Random random = new Random(67890L);
        
        for (int i = 0; i < 1000; i++) {
            double x = random.nextDouble() * 1000 - 500;
            double z = random.nextDouble() * 1000 - 500;
            double value2D = noise.noise2D(x, z);
            assertTrue(value2D >= -1.0 && value2D <= 1.0, 
                "2D noise value " + value2D + " out of range at (" + x + ", " + z + ")");
            
            double y = random.nextDouble() * 1000 - 500;
            double value3D = noise.noise3D(x, y, z);
            assertTrue(value3D >= -1.0 && value3D <= 1.0,
                "3D noise value " + value3D + " out of range at (" + x + ", " + y + ", " + z + ")");
        }
    }
    
    @Test
    @DisplayName("2D noise produces reasonable values")
    void test2DNoise() {
        SimplexNoise noise = new SimplexNoise(12345L);
        
        double sum = 0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        
        for (int i = 0; i < 100; i++) {
            double value = noise.noise2D(i * 0.1, i * 0.2);
            sum += value;
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        
        double avg = sum / 100;
        assertNotEquals(0.0, min, "Noise should not be all zeros");
        assertNotEquals(0.0, max, "Noise should not be all zeros");
        assertTrue(max - min > 0.5, "Noise should have reasonable variation");
        assertTrue(Math.abs(avg) < 0.5, "Average should be near zero");
    }
    
    @Test
    @DisplayName("3D noise produces reasonable values")
    void test3DNoise() {
        SimplexNoise noise = new SimplexNoise(12345L);
        
        double sum = 0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        
        for (int i = 0; i < 100; i++) {
            double value = noise.noise3D(i * 0.1, i * 0.15, i * 0.2);
            sum += value;
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        
        double avg = sum / 100;
        assertNotEquals(0.0, min, "Noise should not be all zeros");
        assertNotEquals(0.0, max, "Noise should not be all zeros");
        assertTrue(max - min > 0.5, "Noise should have reasonable variation");
        assertTrue(Math.abs(avg) < 0.5, "Average should be near zero");
    }
    
    @Test
    @DisplayName("Noise is continuous (adjacent samples are similar)")
    void testContinuity() {
        SimplexNoise noise = new SimplexNoise(12345L);
        
        double value1 = noise.noise2D(10.0, 20.0);
        double value2 = noise.noise2D(10.01, 20.0);
        assertTrue(Math.abs(value1 - value2) < 0.1, "Adjacent 2D samples should be similar");
        
        double value3 = noise.noise3D(10.0, 15.0, 20.0);
        double value4 = noise.noise3D(10.01, 15.0, 20.0);
        assertTrue(Math.abs(value3 - value4) < 0.1, "Adjacent 3D samples should be similar");
    }
    
    @Test
    @DisplayName("Simplex performance vs Perlin (3D)")
    void testPerformanceVsPerlin() {
        SimplexNoise simplex = new SimplexNoise(12345L);
        PerlinNoise perlin = new PerlinNoise(12345L);
        
        // Warm up
        for (int i = 0; i < 1000; i++) {
            simplex.noise3D(i * 0.1, i * 0.15, i * 0.2);
            perlin.noise3D(i * 0.1, i * 0.15, i * 0.2);
        }
        
        // Measure Simplex
        long simplexStart = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            simplex.noise3D(i * 0.1, i * 0.15, i * 0.2);
        }
        long simplexTime = System.nanoTime() - simplexStart;
        
        // Measure Perlin
        long perlinStart = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            perlin.noise3D(i * 0.1, i * 0.15, i * 0.2);
        }
        long perlinTime = System.nanoTime() - perlinStart;
        
        System.out.println("Simplex 3D: " + (simplexTime / 1_000_000.0) + " ms");
        System.out.println("Perlin 3D: " + (perlinTime / 1_000_000.0) + " ms");
        System.out.println("Speedup: " + (perlinTime / (double)simplexTime) + "x");
        
        // Simplex should be faster or at least comparable
        assertTrue(simplexTime < perlinTime * 1.5, "Simplex should be reasonably fast compared to Perlin");
    }
    
    @Test
    @DisplayName("Visual quality check (no directional artifacts)")
    void testVisualQuality() {
        SimplexNoise noise = new SimplexNoise(12345L);
        
        // Sample in different directions and check variance
        double[] xDirection = new double[10];
        double[] zDirection = new double[10];
        double[] diagonalDirection = new double[10];
        
        for (int i = 0; i < 10; i++) {
            xDirection[i] = noise.noise2D(i * 1.0, 5.0);
            zDirection[i] = noise.noise2D(5.0, i * 1.0);
            diagonalDirection[i] = noise.noise2D(i * 1.0, i * 1.0);
        }
        
        double xVariance = calculateVariance(xDirection);
        double zVariance = calculateVariance(zDirection);
        double diagVariance = calculateVariance(diagonalDirection);
        
        // Variances should be similar (no strong directional bias)
        assertTrue(Math.abs(xVariance - zVariance) / Math.max(xVariance, zVariance) < 0.5,
            "X and Z directions should have similar variance");
        assertTrue(Math.abs(xVariance - diagVariance) / Math.max(xVariance, diagVariance) < 0.5,
            "Diagonal should have similar variance to axes");
    }
    
    private double calculateVariance(double[] values) {
        double mean = 0;
        for (double v : values) mean += v;
        mean /= values.length;
        
        double variance = 0;
        for (double v : values) {
            double diff = v - mean;
            variance += diff * diff;
        }
        return variance / values.length;
    }
    
    @Test
    @DisplayName("getSeed returns correct seed")
    void testGetSeed() {
        long seed = 12345L;
        SimplexNoise noise = new SimplexNoise(seed);
        assertEquals(seed, noise.getSeed());
    }
}
