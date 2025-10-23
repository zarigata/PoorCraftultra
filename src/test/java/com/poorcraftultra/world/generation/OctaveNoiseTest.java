package com.poorcraftultra.world.generation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Octave Noise Tests")
class OctaveNoiseTest {
    
    @Test
    @DisplayName("Single octave matches base noise")
    void testSingleOctave() {
        PerlinNoise perlin = new PerlinNoise(12345L);
        OctaveNoise octave = new OctaveNoise(perlin, 1, 1.0, 1.0, 2.0, 0.5);
        
        for (int i = 0; i < 10; i++) {
            double x = i * 0.1;
            double z = i * 0.2;
            assertEquals(perlin.noise2D(x, z), octave.sample2D(x, z), 1e-10);
        }
    }
    
    @Test
    @DisplayName("Multiple octaves add detail")
    void testMultipleOctaves() {
        PerlinNoise perlin = new PerlinNoise(12345L);
        OctaveNoise octave1 = new OctaveNoise(perlin, 1, 0.01, 1.0, 2.0, 0.5);
        OctaveNoise octave4 = new OctaveNoise(perlin, 4, 0.01, 1.0, 2.0, 0.5);
        
        // Sample over a range and check that 4-octave has more variation
        double sum1 = 0, sum4 = 0;
        for (int i = 0; i < 100; i++) {
            double x = i * 1.0;
            double z = i * 1.0;
            sum1 += Math.abs(octave1.sample2D(x, z) - octave1.sample2D(x + 1, z));
            sum4 += Math.abs(octave4.sample2D(x, z) - octave4.sample2D(x + 1, z));
        }
        
        assertTrue(sum4 > sum1, "More octaves should add more detail");
    }
    
    @Test
    @DisplayName("Output is normalized to [-1, 1]")
    void testOutputRange() {
        PerlinNoise perlin = new PerlinNoise(12345L);
        OctaveNoise octave = new OctaveNoise(perlin, 4, 0.01, 1.0, 2.0, 0.5);
        
        for (int i = 0; i < 1000; i++) {
            double x = i * 0.1;
            double z = i * 0.2;
            double value = octave.sample2D(x, z);
            assertTrue(value >= -1.0 && value <= 1.0, "Value out of range: " + value);
        }
    }
    
    @Test
    @DisplayName("Deterministic output")
    void testDeterminism() {
        PerlinNoise perlin = new PerlinNoise(12345L);
        OctaveNoise octave1 = new OctaveNoise(perlin, 4, 0.01, 1.0, 2.0, 0.5);
        OctaveNoise octave2 = new OctaveNoise(perlin, 4, 0.01, 1.0, 2.0, 0.5);
        
        for (int i = 0; i < 100; i++) {
            double x = i * 0.1;
            double z = i * 0.2;
            assertEquals(octave1.sample2D(x, z), octave2.sample2D(x, z), 1e-10);
        }
    }
    
    @Test
    @DisplayName("Frequency scaling affects output scale")
    void testFrequencyScaling() {
        PerlinNoise perlin = new PerlinNoise(12345L);
        OctaveNoise lowFreq = new OctaveNoise(perlin, 1, 0.01, 1.0, 2.0, 0.5);
        OctaveNoise highFreq = new OctaveNoise(perlin, 1, 0.1, 1.0, 2.0, 0.5);
        
        double lowVariation = 0, highVariation = 0;
        for (int i = 0; i < 100; i++) {
            lowVariation += Math.abs(lowFreq.sample2D(i, 0) - lowFreq.sample2D(i + 1, 0));
            highVariation += Math.abs(highFreq.sample2D(i, 0) - highFreq.sample2D(i + 1, 0));
        }
        
        assertTrue(highVariation > lowVariation, "Higher frequency should have more variation");
    }
    
    @Test
    @DisplayName("Amplitude scaling affects magnitude")
    void testAmplitudeScaling() {
        PerlinNoise perlin = new PerlinNoise(12345L);
        OctaveNoise lowAmp = new OctaveNoise(perlin, 1, 0.01, 0.5, 2.0, 0.5);
        OctaveNoise highAmp = new OctaveNoise(perlin, 1, 0.01, 2.0, 2.0, 0.5);
        
        double lowSum = 0, highSum = 0;
        for (int i = 0; i < 100; i++) {
            lowSum += Math.abs(lowAmp.sample2D(i * 0.1, i * 0.2));
            highSum += Math.abs(highAmp.sample2D(i * 0.1, i * 0.2));
        }
        
        assertTrue(highSum > lowSum, "Higher amplitude should produce larger values");
    }
    
    @Test
    @DisplayName("Lacunarity affects frequency progression")
    void testLacunarity() {
        PerlinNoise perlin = new PerlinNoise(12345L);
        OctaveNoise lowLac = new OctaveNoise(perlin, 4, 0.01, 1.0, 1.5, 0.5);
        OctaveNoise highLac = new OctaveNoise(perlin, 4, 0.01, 1.0, 3.0, 0.5);
        
        // Different lacunarity should produce different results
        double diff = 0;
        for (int i = 0; i < 100; i++) {
            diff += Math.abs(lowLac.sample2D(i * 0.1, i * 0.2) - highLac.sample2D(i * 0.1, i * 0.2));
        }
        
        assertTrue(diff > 10.0, "Different lacunarity should produce different results");
    }
    
    @Test
    @DisplayName("Persistence affects amplitude progression")
    void testPersistence() {
        PerlinNoise perlin = new PerlinNoise(12345L);
        OctaveNoise lowPers = new OctaveNoise(perlin, 4, 0.01, 1.0, 2.0, 0.3);
        OctaveNoise highPers = new OctaveNoise(perlin, 4, 0.01, 1.0, 2.0, 0.7);
        
        // Different persistence should produce different results
        double diff = 0;
        for (int i = 0; i < 100; i++) {
            diff += Math.abs(lowPers.sample2D(i * 0.1, i * 0.2) - highPers.sample2D(i * 0.1, i * 0.2));
        }
        
        assertTrue(diff > 5.0, "Different persistence should produce different results");
    }
    
    @Test
    @DisplayName("Default factory produces valid OctaveNoise")
    void testDefaultFactory() {
        PerlinNoise perlin = new PerlinNoise(12345L);
        OctaveNoise octave = OctaveNoise.createDefault(perlin);
        
        assertNotNull(octave);
        assertEquals(4, octave.getOctaves());
        assertEquals(0.01, octave.getFrequency(), 1e-10);
        assertEquals(1.0, octave.getAmplitude(), 1e-10);
        assertEquals(2.0, octave.getLacunarity(), 1e-10);
        assertEquals(0.5, octave.getPersistence(), 1e-10);
        
        // Should produce valid output
        double value = octave.sample2D(10.0, 20.0);
        assertTrue(value >= -1.0 && value <= 1.0);
    }
    
    @Test
    @DisplayName("Each octave adds detail")
    void testOctaveContribution() {
        PerlinNoise perlin = new PerlinNoise(12345L);
        
        double[] variations = new double[4];
        for (int octaves = 1; octaves <= 4; octaves++) {
            OctaveNoise noise = new OctaveNoise(perlin, octaves, 0.01, 1.0, 2.0, 0.5);
            double variation = 0;
            for (int i = 0; i < 100; i++) {
                variation += Math.abs(noise.sample2D(i, 0) - noise.sample2D(i + 1, 0));
            }
            variations[octaves - 1] = variation;
        }
        
        // Each additional octave should increase variation
        for (int i = 1; i < 4; i++) {
            assertTrue(variations[i] > variations[i - 1], 
                "Octave " + (i + 1) + " should have more variation than octave " + i);
        }
    }
}
