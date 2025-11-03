package com.poorcraft.ultra.world;

import com.poorcraft.ultra.voxel.Chunk;
import java.util.logging.Logger;

/**
 * Main world generation orchestrator.
 * Coordinates all worldgen subsystems to generate chunks.
 */
public class WorldGenerator {

    private static final Logger LOGGER = Logger.getLogger(WorldGenerator.class.getName());

    private final long seed;
    private final BiomeProvider biomeProvider;
    private final HeightmapGenerator heightmapGenerator;
    private final ChunkPopulator chunkPopulator;
    private final CaveCarver caveCarver;
    private final FeaturePopulator featurePopulator;

    /**
     * Creates a world generator with the given seed.
     */
    public WorldGenerator(long seed) {
        this.seed = seed;
        
        // Initialize all subsystems with seed
        this.biomeProvider = new BiomeProvider(seed);
        this.heightmapGenerator = new HeightmapGenerator(seed, biomeProvider);
        this.chunkPopulator = new ChunkPopulator(heightmapGenerator, biomeProvider);
        this.caveCarver = new CaveCarver(seed);
        this.featurePopulator = new FeaturePopulator(seed, biomeProvider);
        
        LOGGER.info("WorldGenerator initialized with seed: " + seed);
    }

    /**
     * Generates a fully populated chunk at the given coordinates.
     * 
     * @param chunkX Chunk X coordinate
     * @param chunkZ Chunk Z coordinate
     * @return Fully generated chunk
     */
    public Chunk generateChunk(int chunkX, int chunkZ) {
        // Create empty chunk
        Chunk chunk = new Chunk(chunkX, chunkZ);
        
        // Populate base terrain
        chunkPopulator.populate(chunk);
        
        // Carve caves (CP v2.1)
        caveCarver.carveCaves(chunk);
        
        // Place features (CP v2.1)
        featurePopulator.populate(chunk, heightmapGenerator);
        
        // Mark chunk dirty for meshing
        chunk.markDirty();
        
        return chunk;
    }

    /**
     * Returns the world seed.
     */
    public long getSeed() {
        return seed;
    }

    /**
     * Returns the biome type at the given world coordinates.
     */
    public BiomeType getBiomeAt(int worldX, int worldZ) {
        return biomeProvider.getBiome(worldX, worldZ);
    }

    /**
     * Returns the terrain height at the given world coordinates.
     */
    public int getHeightAt(int worldX, int worldZ) {
        return heightmapGenerator.getHeight(worldX, worldZ);
    }
    
    /**
     * Returns the biome provider for biome-based tinting.
     */
    public BiomeProvider getBiomeProvider() {
        return biomeProvider;
    }
}
