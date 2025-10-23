package com.poorcraftultra.world.generation;

import com.poorcraftultra.world.chunk.Chunk;

/**
 * Main world generator orchestrating the procedural generation pipeline.
 * <p>
 * Generation occurs in phases:
 * <ol>
 *   <li><b>Terrain generation</b>: Height map-based terrain with stone/dirt/grass layers</li>
 *   <li><b>Cave carving</b>: 3D noise-based cave systems (optional)</li>
 *   <li><b>Optimization</b>: Free empty chunk sections to save memory</li>
 * </ol>
 * <p>
 * All generation is deterministic - the same seed produces identical worlds
 * across all platforms and runs. Different seeds are used for terrain and
 * caves to ensure independent noise patterns.
 * <p>
 * <b>Phase 6</b>: Basic terrain and caves (createDefault).
 * <b>Phase 7</b>: Biome-specific generation (createWithBiomes).
 */
public class WorldGenerator {
    
    private final long seed;
    private final TerrainGenerator terrainGenerator;
    private final CaveGenerator caveGenerator;
    private boolean generateCaves;
    
    /**
     * Creates a world generator with default terrain and cave generators.
     * 
     * @param seed the world seed for deterministic generation
     */
    public WorldGenerator(long seed) {
        this.seed = seed;
        this.terrainGenerator = TerrainGenerator.createDefault(seed);
        this.caveGenerator = CaveGenerator.createDefault(seed + 1);
        this.generateCaves = true;
    }
    
    /**
     * Creates a world generator with custom generators.
     * 
     * @param seed the world seed
     * @param terrainGenerator custom terrain generator
     * @param caveGenerator custom cave generator
     */
    public WorldGenerator(long seed, TerrainGenerator terrainGenerator, CaveGenerator caveGenerator) {
        this.seed = seed;
        this.terrainGenerator = terrainGenerator;
        this.caveGenerator = caveGenerator;
        this.generateCaves = true;
    }
    
    /**
     * Generates a complete chunk with terrain and caves.
     * 
     * @param chunk the chunk to generate
     */
    public void generateChunk(Chunk chunk) {
        terrainGenerator.generateTerrain(chunk);
        
        if (generateCaves) {
            caveGenerator.carveCaves(chunk);
        }
        
        chunk.optimize();
    }
    
    /**
     * Creates a world generator with default settings (no biomes).
     * 
     * @param seed the world seed
     * @return WorldGenerator with default terrain and cave generators (Phase 6)
     */
    public static WorldGenerator createDefault(long seed) {
        return new WorldGenerator(seed);
    }
    
    /**
     * Creates a world generator with biome support (Phase 7).
     * 
     * @param seed the world seed
     * @return WorldGenerator with biome-aware terrain generation
     */
    public static WorldGenerator createWithBiomes(long seed) {
        TerrainGenerator terrainGenerator = TerrainGenerator.createWithBiomes(seed);
        CaveGenerator caveGenerator = CaveGenerator.createDefault(seed + 1);
        return new WorldGenerator(seed, terrainGenerator, caveGenerator);
    }
    
    public long getSeed() {
        return seed;
    }
    
    public boolean isGenerateCaves() {
        return generateCaves;
    }
    
    public void setGenerateCaves(boolean generateCaves) {
        this.generateCaves = generateCaves;
    }
    
    public TerrainGenerator getTerrainGenerator() {
        return terrainGenerator;
    }
    
    public CaveGenerator getCaveGenerator() {
        return caveGenerator;
    }
    
    /**
     * Gets the BiomeSelector from the terrain generator.
     * 
     * @return BiomeSelector if using biome generation, null otherwise
     */
    public BiomeSelector getBiomeSelector() {
        return terrainGenerator.getBiomeSelector();
    }
}
