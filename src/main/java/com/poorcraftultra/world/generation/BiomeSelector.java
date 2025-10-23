package com.poorcraftultra.world.generation;

/**
 * Selects biomes for world coordinates using temperature and humidity noise maps.
 * Implements biome blending to create smooth transitions between different biomes.
 * <p>
 * The biome selection process:
 * <ol>
 *   <li>Sample temperature and humidity noise at world coordinates</li>
 *   <li>Map noise values from [-1, 1] to [0, 1]</li>
 *   <li>Select biome using {@link Biome#fromTemperatureHumidity(double, double)}</li>
 * </ol>
 * <p>
 * Biome blending uses distance-weighted interpolation:
 * <ol>
 *   <li>Sample biomes at center point and 4 neighboring points (N, S, E, W)</li>
 *   <li>Calculate height for each biome using its height modifiers</li>
 *   <li>Weight each height by inverse distance from center</li>
 *   <li>Return weighted average for smooth transitions</li>
 * </ol>
 * 
 * @see Biome
 * @see OctaveNoise
 */
public class BiomeSelector {
    private final OctaveNoise temperatureNoise;
    private final OctaveNoise humidityNoise;
    private final double biomeScale;
    private final boolean enableBlending;
    private final int blendRadius;
    
    /**
     * Constructs a BiomeSelector with specified parameters.
     *
     * @param temperatureNoise Noise generator for temperature values
     * @param humidityNoise Noise generator for humidity values
     * @param biomeScale Scale factor for biome regions (larger = bigger biomes)
     * @param enableBlending Whether to enable biome blending at boundaries
     * @param blendRadius Radius in blocks for blending calculations
     */
    public BiomeSelector(OctaveNoise temperatureNoise, OctaveNoise humidityNoise,
                        double biomeScale, boolean enableBlending, int blendRadius) {
        this.temperatureNoise = temperatureNoise;
        this.humidityNoise = humidityNoise;
        this.biomeScale = biomeScale;
        this.enableBlending = enableBlending;
        this.blendRadius = blendRadius;
    }
    
    /**
     * Constructs a BiomeSelector with default blending settings.
     *
     * @param seed World seed for noise generation
     */
    public BiomeSelector(long seed) {
        this(seed, 0.002, true, 8);
    }
    
    /**
     * Constructs a BiomeSelector with custom parameters.
     *
     * @param seed World seed for noise generation
     * @param biomeScale Scale factor for biome regions
     * @param enableBlending Whether to enable biome blending
     * @param blendRadius Radius in blocks for blending
     */
    public BiomeSelector(long seed, double biomeScale, boolean enableBlending, int blendRadius) {
        // Create temperature noise with seed offset
        PerlinNoise tempPerlin = new PerlinNoise(seed + 100);
        this.temperatureNoise = new OctaveNoise(tempPerlin, 4, 0.01, 1.0, 2.0, 0.5);
        
        // Create humidity noise with different seed offset
        PerlinNoise humidPerlin = new PerlinNoise(seed + 200);
        this.humidityNoise = new OctaveNoise(humidPerlin, 4, 0.01, 1.0, 2.0, 0.5);
        
        this.biomeScale = biomeScale;
        this.enableBlending = enableBlending;
        this.blendRadius = blendRadius;
    }
    
    /**
     * Gets the biome at the specified world coordinates.
     *
     * @param worldX World X coordinate
     * @param worldZ World Z coordinate
     * @return The biome at this location
     */
    public Biome getBiome(int worldX, int worldZ) {
        double temperature = getTemperature(worldX, worldZ);
        double humidity = getHumidity(worldX, worldZ);
        return Biome.fromTemperatureHumidity(temperature, humidity);
    }
    
    /**
     * Gets the temperature value at the specified world coordinates.
     * Maps noise from [-1, 1] to [0, 1].
     *
     * @param worldX World X coordinate
     * @param worldZ World Z coordinate
     * @return Temperature value (0.0 = cold, 1.0 = hot)
     */
    public double getTemperature(int worldX, int worldZ) {
        double noise = temperatureNoise.sample2D(worldX * biomeScale, worldZ * biomeScale);
        return (noise + 1.0) * 0.5; // Map [-1, 1] to [0, 1]
    }
    
    /**
     * Gets the humidity value at the specified world coordinates.
     * Maps noise from [-1, 1] to [0, 1].
     *
     * @param worldX World X coordinate
     * @param worldZ World Z coordinate
     * @return Humidity value (0.0 = dry, 1.0 = humid)
     */
    public double getHumidity(int worldX, int worldZ) {
        double noise = humidityNoise.sample2D(worldX * biomeScale, worldZ * biomeScale);
        return (noise + 1.0) * 0.5; // Map [-1, 1] to [0, 1]
    }
    
    /**
     * Calculates blended terrain height at the specified coordinates.
     * Uses distance-weighted interpolation of heights from neighboring biomes
     * to create smooth transitions at biome boundaries.
     *
     * @param worldX World X coordinate
     * @param worldZ World Z coordinate
     * @param baseHeight Base terrain height before biome modifiers
     * @param baseVariation Base height variation before biome modifiers
     * @return Blended height value
     */
    public int getBlendedHeight(int worldX, int worldZ, int baseHeight, int baseVariation) {
        if (!enableBlending) {
            // No blending - use center biome only
            Biome biome = getBiome(worldX, worldZ);
            return baseHeight + biome.getHeightOffset();
        }
        
        // Sample biomes at 5 points: center + 4 cardinal directions
        Biome centerBiome = getBiome(worldX, worldZ);
        Biome northBiome = getBiome(worldX, worldZ + blendRadius);
        Biome southBiome = getBiome(worldX, worldZ - blendRadius);
        Biome eastBiome = getBiome(worldX + blendRadius, worldZ);
        Biome westBiome = getBiome(worldX - blendRadius, worldZ);
        
        // Calculate heights for each biome
        int centerHeight = baseHeight + centerBiome.getHeightOffset();
        int northHeight = baseHeight + northBiome.getHeightOffset();
        int southHeight = baseHeight + southBiome.getHeightOffset();
        int eastHeight = baseHeight + eastBiome.getHeightOffset();
        int westHeight = baseHeight + westBiome.getHeightOffset();
        
        // Distance-weighted interpolation
        // Center has highest weight, neighbors weighted by inverse distance
        double centerWeight = 2.0;
        double neighborWeight = 1.0;
        
        double totalWeight = centerWeight + 4 * neighborWeight;
        double weightedSum = centerHeight * centerWeight +
                           northHeight * neighborWeight +
                           southHeight * neighborWeight +
                           eastHeight * neighborWeight +
                           westHeight * neighborWeight;
        
        return (int) Math.round(weightedSum / totalWeight);
    }
    
    /**
     * Creates a BiomeSelector with default parameters optimized for world generation.
     *
     * @param seed World seed
     * @return New BiomeSelector instance
     */
    public static BiomeSelector createDefault(long seed) {
        return new BiomeSelector(seed, 0.002, true, 8);
    }
}
