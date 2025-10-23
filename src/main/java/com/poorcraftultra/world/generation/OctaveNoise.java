package com.poorcraftultra.world.generation;

/**
 * Multi-octave noise generator that layers multiple frequencies of noise.
 * <p>
 * Octave layering creates natural-looking terrain by combining:
 * <ul>
 *   <li><b>Low octaves</b>: Large-scale features (mountains, valleys)</li>
 *   <li><b>High octaves</b>: Fine details (bumps, texture)</li>
 * </ul>
 * <p>
 * Each octave has:
 * <ul>
 *   <li><b>Frequency</b>: Multiplied by lacunarity each octave (controls detail scale)</li>
 *   <li><b>Amplitude</b>: Multiplied by persistence each octave (controls detail strength)</li>
 * </ul>
 * <p>
 * Output is normalized to [-1, 1] by dividing by the sum of all amplitudes.
 * 
 * @see NoiseGenerator
 */
public class OctaveNoise {
    
    private final NoiseGenerator baseNoise;
    private final int octaves;
    private final double frequency;
    private final double amplitude;
    private final double lacunarity;
    private final double persistence;
    private final double normalizationFactor;
    
    /**
     * Creates a multi-octave noise generator.
     * 
     * @param baseNoise the underlying noise generator
     * @param octaves number of octaves to layer (typically 4-8)
     * @param frequency base frequency (typically 0.01-0.1)
     * @param amplitude base amplitude (typically 1.0)
     * @param lacunarity frequency multiplier per octave (typically 2.0)
     * @param persistence amplitude multiplier per octave (typically 0.5)
     */
    public OctaveNoise(NoiseGenerator baseNoise, int octaves, double frequency, 
                       double amplitude, double lacunarity, double persistence) {
        this.baseNoise = baseNoise;
        this.octaves = octaves;
        this.frequency = frequency;
        this.amplitude = amplitude;
        this.lacunarity = lacunarity;
        this.persistence = persistence;
        
        // Pre-compute normalization factor (sum of geometric series)
        double sum = 0;
        double amp = amplitude;
        for (int i = 0; i < octaves; i++) {
            sum += amp;
            amp *= persistence;
        }
        this.normalizationFactor = sum;
    }
    
    /**
     * Samples 2D noise with multiple octaves.
     * 
     * @param x the x-coordinate
     * @param z the z-coordinate
     * @return noise value in range [-1, 1]
     */
    public double sample2D(double x, double z) {
        double result = 0;
        double currentFreq = frequency;
        double currentAmp = amplitude;
        
        for (int i = 0; i < octaves; i++) {
            result += baseNoise.noise2D(x * currentFreq, z * currentFreq) * currentAmp;
            currentFreq *= lacunarity;
            currentAmp *= persistence;
        }
        
        return result / normalizationFactor;
    }
    
    /**
     * Samples 3D noise with multiple octaves.
     * 
     * @param x the x-coordinate
     * @param y the y-coordinate
     * @param z the z-coordinate
     * @return noise value in range [-1, 1]
     */
    public double sample3D(double x, double y, double z) {
        double result = 0;
        double currentFreq = frequency;
        double currentAmp = amplitude;
        
        for (int i = 0; i < octaves; i++) {
            result += baseNoise.noise3D(x * currentFreq, y * currentFreq, z * currentFreq) * currentAmp;
            currentFreq *= lacunarity;
            currentAmp *= persistence;
        }
        
        return result / normalizationFactor;
    }
    
    /**
     * Creates an OctaveNoise with sensible default parameters.
     * 
     * @param baseNoise the underlying noise generator
     * @return OctaveNoise with 4 octaves, frequency=0.01, amplitude=1.0, lacunarity=2.0, persistence=0.5
     */
    public static OctaveNoise createDefault(NoiseGenerator baseNoise) {
        return new OctaveNoise(baseNoise, 4, 0.01, 1.0, 2.0, 0.5);
    }
    
    /**
     * Creates an OctaveNoise with sensible default parameters and a seed-based noise generator.
     * 
     * @param seed the seed for deterministic generation
     * @param useSimplex true to use SimplexNoise, false to use PerlinNoise
     * @return OctaveNoise with 4 octaves, frequency=0.01, amplitude=1.0, lacunarity=2.0, persistence=0.5
     */
    public static OctaveNoise createDefault(long seed, boolean useSimplex) {
        NoiseGenerator baseNoise = useSimplex ? new SimplexNoise(seed) : new PerlinNoise(seed);
        return createDefault(baseNoise);
    }
    
    public NoiseGenerator getBaseNoise() {
        return baseNoise;
    }
    
    public int getOctaves() {
        return octaves;
    }
    
    public double getFrequency() {
        return frequency;
    }
    
    public double getAmplitude() {
        return amplitude;
    }
    
    public double getLacunarity() {
        return lacunarity;
    }
    
    public double getPersistence() {
        return persistence;
    }
}
