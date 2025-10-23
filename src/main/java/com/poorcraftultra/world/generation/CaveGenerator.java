package com.poorcraftultra.world.generation;

import com.poorcraftultra.world.chunk.Chunk;
import com.poorcraftultra.world.block.BlockRegistry;

/**
 * 3D cave carving generator using volumetric noise.
 * <p>
 * Creates cave systems by sampling 3D noise and removing blocks where
 * the noise value exceeds a threshold. This produces natural-looking
 * connected cave networks with branching tunnels.
 * <p>
 * Caves are carved AFTER terrain generation, cutting through existing
 * solid blocks. The height range can be configured to prevent caves
 * from appearing at the surface or bedrock level.
 */
public class CaveGenerator {
    
    private final OctaveNoise caveNoise;
    private final double caveThreshold;
    private final int minCaveHeight;
    private final int maxCaveHeight;
    
    /**
     * Creates a cave generator with custom parameters.
     * 
     * @param caveNoise the 3D noise generator for cave systems
     * @param caveThreshold noise threshold for carving (default 0.6)
     * @param minCaveHeight minimum Y for caves (default 1)
     * @param maxCaveHeight maximum Y for caves (default 120)
     */
    public CaveGenerator(OctaveNoise caveNoise, double caveThreshold, int minCaveHeight, int maxCaveHeight) {
        this.caveNoise = caveNoise;
        this.caveThreshold = caveThreshold;
        this.minCaveHeight = minCaveHeight;
        this.maxCaveHeight = maxCaveHeight;
    }
    
    /**
     * Determines if a block at the given coordinates should be carved into a cave.
     * 
     * @param worldX the world x-coordinate
     * @param worldY the world y-coordinate
     * @param worldZ the world z-coordinate
     * @return true if the block should be carved (become air)
     */
    public boolean shouldCarve(int worldX, int worldY, int worldZ) {
        if (worldY < minCaveHeight || worldY > maxCaveHeight) {
            return false;
        }
        
        double noise = caveNoise.sample3D(worldX, worldY, worldZ);
        return noise > caveThreshold;
    }
    
    /**
     * Carves caves into the specified chunk.
     * Removes solid blocks based on 3D noise to create cave systems.
     * 
     * @param chunk the chunk to carve caves in
     */
    public void carveCaves(Chunk chunk) {
        int chunkWorldX = chunk.getPosition().getX() * 16;
        int chunkWorldY = chunk.getPosition().getY() * 256;
        int chunkWorldZ = chunk.getPosition().getZ() * 16;
        
        // Cache air ID to avoid repeated registry lookups
        byte airId = BlockRegistry.getInstance().getBlock("air").getId();
        
        // Compute local Y range restricted to cave height bounds
        int localYStart = Math.max(0, minCaveHeight - chunkWorldY);
        int localYEnd = Math.min(255, maxCaveHeight - chunkWorldY);
        
        // Skip if chunk is entirely outside cave height range
        if (localYStart > 255 || localYEnd < 0) {
            return;
        }
        
        // Clamp to valid range
        localYStart = Math.max(0, localYStart);
        localYEnd = Math.min(255, localYEnd);
        
        // Reorder loops: Y-innermost for cache locality (matches section layout)
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = localYStart; y <= localYEnd; y++) {
                    int worldX = chunkWorldX + x;
                    int worldY = chunkWorldY + y;
                    int worldZ = chunkWorldZ + z;
                    
                    // Check for existing air first to avoid unnecessary noise sampling
                    byte currentBlock = chunk.getBlock(x, y, z);
                    if (currentBlock != airId) {
                        if (shouldCarve(worldX, worldY, worldZ)) {
                            chunk.setBlock(x, y, z, airId);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Creates a cave generator with default parameters.
     * 
     * @param seed the world seed (will be offset for cave generation)
     * @return CaveGenerator with default settings
     */
    public static CaveGenerator createDefault(long seed) {
        SimplexNoise simplex = new SimplexNoise(seed);
        OctaveNoise caveNoise = new OctaveNoise(simplex, 3, 0.05, 1.0, 2.0, 0.5);
        return new CaveGenerator(caveNoise, 0.6, 1, 120);
    }
    
    public double getCaveThreshold() {
        return caveThreshold;
    }
    
    public int getMinCaveHeight() {
        return minCaveHeight;
    }
    
    public int getMaxCaveHeight() {
        return maxCaveHeight;
    }
}
