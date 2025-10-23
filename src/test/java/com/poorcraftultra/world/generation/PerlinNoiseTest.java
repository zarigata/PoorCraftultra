package com.poorcraftultra.world.generation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Perlin Noise Tests")
class PerlinNoiseTest {
    
    @Test
    @DisplayName("Same seed produces deterministic output")
    void testDeterminism() {
        PerlinNoise noise1 = new PerlinNoise(12345L);
        PerlinNoise noise2 = new PerlinNoise(12345L);
        
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
        PerlinNoise noise1 = new PerlinNoise(12345L);
        PerlinNoise noise2 = new PerlinNoise(54321L);
        
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
        PerlinNoise noise = new PerlinNoise(12345L);
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
        PerlinNoise noise = new PerlinNoise(12345L);
        
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
        PerlinNoise noise = new PerlinNoise(12345L);
        
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
        PerlinNoise noise = new PerlinNoise(12345L);
        
        double value1 = noise.noise2D(10.0, 20.0);
        double value2 = noise.noise2D(10.01, 20.0);
        assertTrue(Math.abs(value1 - value2) < 0.1, "Adjacent 2D samples should be similar");
        
        double value3 = noise.noise3D(10.0, 15.0, 20.0);
        double value4 = noise.noise3D(10.01, 15.0, 20.0);
        assertTrue(Math.abs(value3 - value4) < 0.1, "Adjacent 3D samples should be similar");
    }
    
    @Test
    @DisplayName("Noise is not symmetric")
    void testSymmetry() {
        PerlinNoise noise = new PerlinNoise(12345L);
        
        double value1 = noise.noise2D(10.0, 20.0);
        double value2 = noise.noise2D(-10.0, -20.0);
        assertNotEquals(value1, value2, 1e-6, "Noise should not be symmetric");
        
        double value3 = noise.noise3D(10.0, 15.0, 20.0);
        double value4 = noise.noise3D(-10.0, -15.0, -20.0);
        assertNotEquals(value3, value4, 1e-6, "Noise should not be symmetric");
    }
    
    @Test
    @DisplayName("Noise works at zero coordinates")
    void testZeroCoordinates() {
        PerlinNoise noise = new PerlinNoise(12345L);
        
        double value2D = noise.noise2D(0, 0);
        assertTrue(value2D >= -1.0 && value2D <= 1.0, "Noise at origin should be valid");
        
        double value3D = noise.noise3D(0, 0, 0);
        assertTrue(value3D >= -1.0 && value3D <= 1.0, "Noise at origin should be valid");
    }
    
    @Test
    @DisplayName("Noise works with large coordinates")
    void testLargeCoordinates() {
        PerlinNoise noise = new PerlinNoise(12345L);
        
        double value2D = noise.noise2D(10000.0, 20000.0);
        assertTrue(value2D >= -1.0 && value2D <= 1.0, "Noise should work with large coordinates");
        assertFalse(Double.isNaN(value2D), "Noise should not be NaN");
        assertFalse(Double.isInfinite(value2D), "Noise should not be infinite");
        
        double value3D = noise.noise3D(10000.0, 15000.0, 20000.0);
        assertTrue(value3D >= -1.0 && value3D <= 1.0, "Noise should work with large coordinates");
        assertFalse(Double.isNaN(value3D), "Noise should not be NaN");
        assertFalse(Double.isInfinite(value3D), "Noise should not be infinite");
    }
    
    @Test
    @DisplayName("getSeed returns correct seed")
    void testGetSeed() {
        long seed = 12345L;
        PerlinNoise noise = new PerlinNoise(seed);
        assertEquals(seed, noise.getSeed());
    }
}
