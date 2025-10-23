package com.poorcraftultra.world.generation;

/**
 * Represents the different biomes in the world, each with unique environmental properties
 * and terrain characteristics.
 * <p>
 * Each biome is defined by:
 * <ul>
 *   <li><b>Temperature and Humidity</b>: Used for biome selection via noise maps (0.0-1.0 range)</li>
 *   <li><b>Height Modifiers</b>: Offset and variation that affect terrain elevation</li>
 *   <li><b>Block Palette</b>: Surface, subsurface, and stone blocks specific to the biome</li>
 * </ul>
 * <p>
 * Biome selection uses Euclidean distance in temperature-humidity space to find the closest match.
 * Height generation applies biome-specific offsets and variations to create distinct terrain profiles.
 * 
 * @see BiomeSelector
 */
public enum Biome {
    /**
     * Temperate grasslands with moderate elevation and gentle rolling hills.
     * Default biome for balanced temperature and humidity.
     */
    PLAINS(
        "Plains",
        0.5,  // temperature: temperate
        0.5,  // humidity: moderate
        0,    // heightOffset: sea level
        20,   // heightVariation: gentle hills
        (byte) 2,  // surfaceBlock: grass
        (byte) 3,  // subsurfaceBlock: dirt
        (byte) 1   // stoneBlock: stone
    ),
    
    /**
     * Hot, arid desert with lower elevation and sandy terrain.
     * Features sand surface and sandstone underground.
     */
    DESERT(
        "Desert",
        0.9,  // temperature: hot
        0.1,  // humidity: dry
        -5,   // heightOffset: slightly lower
        15,   // heightVariation: flat with dunes
        (byte) 4,  // surfaceBlock: sand
        (byte) 4,  // subsurfaceBlock: sand
        (byte) 8   // stoneBlock: sandstone
    ),
    
    /**
     * Cold, snowy biome with slightly elevated terrain.
     * Features snow-covered surface and ice formations.
     */
    SNOW(
        "Snow",
        0.1,  // temperature: cold
        0.5,  // humidity: moderate
        5,    // heightOffset: slightly elevated
        25,   // heightVariation: rolling hills
        (byte) 6,  // surfaceBlock: snow_block
        (byte) 3,  // subsurfaceBlock: dirt
        (byte) 1   // stoneBlock: stone
    ),
    
    /**
     * Hot, humid jungle with varied elevation and dense vegetation.
     * Features vibrant jungle grass and diverse terrain.
     */
    JUNGLE(
        "Jungle",
        0.9,  // temperature: hot
        0.9,  // humidity: humid
        0,    // heightOffset: sea level
        30,   // heightVariation: varied terrain
        (byte) 7,  // surfaceBlock: jungle_grass
        (byte) 3,  // subsurfaceBlock: dirt
        (byte) 1   // stoneBlock: stone
    ),
    
    /**
     * Cool, mountainous biome with high elevation and dramatic peaks.
     * Features exposed stone and the highest terrain variation.
     */
    MOUNTAINS(
        "Mountains",
        0.3,  // temperature: cool
        0.4,  // humidity: moderate-low
        20,   // heightOffset: high elevation
        40,   // heightVariation: dramatic peaks
        (byte) 1,  // surfaceBlock: stone
        (byte) 1,  // subsurfaceBlock: stone
        (byte) 1   // stoneBlock: stone
    );
    
    private final String name;
    private final double baseTemperature;
    private final double baseHumidity;
    private final int heightOffset;
    private final int heightVariation;
    private final byte surfaceBlock;
    private final byte subsurfaceBlock;
    private final byte stoneBlock;
    
    /**
     * Constructs a biome with all properties.
     *
     * @param name Display name of the biome
     * @param baseTemperature Temperature value (0.0 = cold, 1.0 = hot)
     * @param baseHumidity Humidity value (0.0 = dry, 1.0 = humid)
     * @param heightOffset Base height adjustment in blocks
     * @param heightVariation Maximum height variation from base
     * @param surfaceBlock Block ID for the top layer
     * @param subsurfaceBlock Block ID for layers below surface
     * @param stoneBlock Block ID for deep underground
     */
    Biome(String name, double baseTemperature, double baseHumidity, 
          int heightOffset, int heightVariation,
          byte surfaceBlock, byte subsurfaceBlock, byte stoneBlock) {
        this.name = name;
        this.baseTemperature = baseTemperature;
        this.baseHumidity = baseHumidity;
        this.heightOffset = heightOffset;
        this.heightVariation = heightVariation;
        this.surfaceBlock = surfaceBlock;
        this.subsurfaceBlock = subsurfaceBlock;
        this.stoneBlock = stoneBlock;
    }
    
    /**
     * @return Display name of this biome
     */
    public String getName() {
        return name;
    }
    
    /**
     * @return Base temperature (0.0-1.0)
     */
    public double getBaseTemperature() {
        return baseTemperature;
    }
    
    /**
     * @return Base humidity (0.0-1.0)
     */
    public double getBaseHumidity() {
        return baseHumidity;
    }
    
    /**
     * @return Height offset applied to base terrain elevation
     */
    public int getHeightOffset() {
        return heightOffset;
    }
    
    /**
     * @return Maximum height variation from base elevation
     */
    public int getHeightVariation() {
        return heightVariation;
    }
    
    /**
     * @return Block ID for the surface layer (topmost block)
     */
    public byte getSurfaceBlock() {
        return surfaceBlock;
    }
    
    /**
     * @return Block ID for subsurface layers (below surface)
     */
    public byte getSubsurfaceBlock() {
        return subsurfaceBlock;
    }
    
    /**
     * @return Block ID for deep stone layers
     */
    public byte getStoneBlock() {
        return stoneBlock;
    }
    
    /**
     * Selects the biome that best matches the given temperature and humidity values.
     * Uses Euclidean distance in temperature-humidity space to find the closest biome.
     *
     * @param temperature Temperature value (0.0-1.0)
     * @param humidity Humidity value (0.0-1.0)
     * @return The biome with the closest temperature/humidity match
     */
    public static Biome fromTemperatureHumidity(double temperature, double humidity) {
        Biome closest = PLAINS;
        double minDistance = Double.MAX_VALUE;
        
        for (Biome biome : values()) {
            double tempDiff = temperature - biome.baseTemperature;
            double humidDiff = humidity - biome.baseHumidity;
            double distance = Math.sqrt(tempDiff * tempDiff + humidDiff * humidDiff);
            
            if (distance < minDistance) {
                minDistance = distance;
                closest = biome;
            }
        }
        
        return closest;
    }
}
