package com.poorcraftultra.world.generation;

import com.poorcraftultra.world.chunk.Chunk;
import com.poorcraftultra.world.block.BlockRegistry;

/**
 * Height map-based terrain generator.
 * <p>
 * Uses 2D noise to generate terrain height at each XZ column, then fills
 * the column vertically with appropriate blocks:
 * <ul>
 *   <li><b>Stone</b>: Deep underground (Y < height - 4)</li>
 *   <li><b>Dirt</b>: Subsurface layer (height - 4 <= Y < height - 1)</li>
 *   <li><b>Grass</b>: Surface block (Y == height - 1)</li>
 *   <li><b>Air</b>: Above terrain (Y >= height)</li>
 * </ul>
 * <p>
 * Terrain height is determined by sampling multi-octave noise and mapping
 * the result [-1, 1] to [baseHeight - variation, baseHeight + variation].
 * <p>
 * Phase 7 adds optional biome support. When a BiomeSelector is provided,
 * terrain uses biome-specific blocks and height modifiers. Backward compatible
 * with Phase 6 by making BiomeSelector optional (null = no biomes).
 */
public class TerrainGenerator {
    
    private final OctaveNoise heightNoise;
    private final int baseHeight;
    private final int heightVariation;
    private final int seaLevel;
    private final BiomeSelector biomeSelector;
    
    /**
     * Creates a terrain generator with custom parameters.
     * 
     * @param heightNoise the noise generator for height maps
     * @param baseHeight base terrain level (default 64)
     * @param heightVariation maximum height variation (default 32)
     * @param seaLevel water level for future phases (default 62)
     */
    public TerrainGenerator(OctaveNoise heightNoise, int baseHeight, int heightVariation, int seaLevel) {
        this(heightNoise, baseHeight, heightVariation, seaLevel, null);
    }
    
    /**
     * Creates a terrain generator with biome support.
     * 
     * @param heightNoise the noise generator for height maps
     * @param baseHeight base terrain level (default 64)
     * @param heightVariation maximum height variation (default 32)
     * @param seaLevel water level for future phases (default 62)
     * @param biomeSelector optional biome selector (null for no biomes)
     */
    public TerrainGenerator(OctaveNoise heightNoise, int baseHeight, int heightVariation, int seaLevel, BiomeSelector biomeSelector) {
        this.heightNoise = heightNoise;
        this.baseHeight = baseHeight;
        this.heightVariation = heightVariation;
        this.seaLevel = seaLevel;
        this.biomeSelector = biomeSelector;
    }
    
    /**
     * Gets the terrain height at the specified world coordinates.
     * Uses biome-specific height modifiers if BiomeSelector is present.
     * 
     * @param worldX the world x-coordinate
     * @param worldZ the world z-coordinate
     * @return terrain height (Y coordinate)
     */
    public int getHeight(int worldX, int worldZ) {
        double noise = heightNoise.sample2D(worldX, worldZ);
        int basicHeight = (int)(baseHeight + noise * heightVariation);
        
        if (biomeSelector != null) {
            return biomeSelector.getBlendedHeight(worldX, worldZ, basicHeight, heightVariation);
        }
        
        return basicHeight;
    }
    
    /**
     * Generates terrain for the specified chunk.
     * Fills the chunk with stone, dirt, grass, and air based on height map.
     * Uses biome-specific blocks if BiomeSelector is present.
     * 
     * @param chunk the chunk to generate terrain in
     */
    public void generateTerrain(Chunk chunk) {
        int chunkWorldX = chunk.getPosition().getX() * 16;
        int chunkWorldY = chunk.getPosition().getY() * 256;
        int chunkWorldZ = chunk.getPosition().getZ() * 16;
        
        // Cache default block IDs (used when no biome selector)
        byte defaultStoneId = BlockRegistry.getInstance().getBlock("stone").getId();
        byte defaultDirtId = BlockRegistry.getInstance().getBlock("dirt").getId();
        byte defaultGrassId = BlockRegistry.getInstance().getBlock("grass").getId();
        
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkWorldX + x;
                int worldZ = chunkWorldZ + z;
                int height = getHeight(worldX, worldZ);
                
                // Get biome-specific blocks if biome selector is present
                byte surfaceBlock, subsurfaceBlock, stoneBlock;
                if (biomeSelector != null) {
                    Biome biome = biomeSelector.getBiome(worldX, worldZ);
                    surfaceBlock = biome.getSurfaceBlock();
                    subsurfaceBlock = biome.getSubsurfaceBlock();
                    stoneBlock = biome.getStoneBlock();
                } else {
                    surfaceBlock = defaultGrassId;
                    subsurfaceBlock = defaultDirtId;
                    stoneBlock = defaultStoneId;
                }
                
                // Compute local Y range that needs to be filled (skip air above surface)
                // Only iterate Y from max(0, 0 - chunkWorldY) up to min(255, height - 1 - chunkWorldY)
                int localYStart = Math.max(0, 0 - chunkWorldY);
                int localYEnd = Math.min(255, height - 1 - chunkWorldY);
                
                // Skip this column if it's entirely above the terrain
                if (localYEnd < 0 || localYStart > 255) {
                    continue;
                }
                
                // Clamp to valid range
                localYStart = Math.max(0, localYStart);
                localYEnd = Math.min(255, localYEnd);
                
                for (int y = localYStart; y <= localYEnd; y++) {
                    int worldY = chunkWorldY + y;
                    
                    byte blockId;
                    if (worldY < height - 4) {
                        blockId = stoneBlock;
                    } else if (worldY < height - 1) {
                        blockId = subsurfaceBlock;
                    } else {
                        blockId = surfaceBlock;
                    }
                    
                    chunk.setBlock(x, y, z, blockId);
                }
            }
        }
    }
    
    /**
     * Creates a terrain generator with default parameters (no biomes).
     * 
     * @param seed the world seed
     * @return TerrainGenerator with default settings (Phase 6 compatible)
     */
    public static TerrainGenerator createDefault(long seed) {
        PerlinNoise perlin = new PerlinNoise(seed);
        OctaveNoise heightNoise = OctaveNoise.createDefault(perlin);
        return new TerrainGenerator(heightNoise, 64, 32, 62, null);
    }
    
    /**
     * Creates a terrain generator with biome support (Phase 7).
     * 
     * @param seed the world seed
     * @return TerrainGenerator with biome-aware generation
     */
    public static TerrainGenerator createWithBiomes(long seed) {
        PerlinNoise perlin = new PerlinNoise(seed);
        OctaveNoise heightNoise = OctaveNoise.createDefault(perlin);
        BiomeSelector biomeSelector = BiomeSelector.createDefault(seed + 300);
        return new TerrainGenerator(heightNoise, 64, 32, 62, biomeSelector);
    }
    
    public int getBaseHeight() {
        return baseHeight;
    }
    
    public int getHeightVariation() {
        return heightVariation;
    }
    
    public int getSeaLevel() {
        return seaLevel;
    }
    
    public BiomeSelector getBiomeSelector() {
        return biomeSelector;
    }
}
