package com.poorcraft.ultra.voxel;

/**
 * Constants for voxel engine configuration.
 */
public final class ChunkConstants {
    
    // Chunk dimensions
    public static final int CHUNK_SIZE_X = 16;
    public static final int CHUNK_SIZE_Y = 16;
    public static final int CHUNK_SIZE_Z = 16;
    public static final int CHUNK_VOLUME = CHUNK_SIZE_X * CHUNK_SIZE_Y * CHUNK_SIZE_Z; // 4096
    
    // World/block conversion
    public static final float BLOCK_SIZE = 1.0f;
    
    // Rendering
    public static final int CHUNK_LOAD_RADIUS = 1;
    public static final int CHUNK_UNLOAD_RADIUS = 2;
    public static final int MAX_CHUNK_UPDATES_PER_FRAME = 2;
    
    // Atlas
    public static final int ATLAS_TILE_SIZE = 64;
    public static final int ATLAS_GRID_SIZE = 8;
    public static final int ATLAS_SIZE = ATLAS_GRID_SIZE * ATLAS_TILE_SIZE; // 512
    
    private ChunkConstants() {
        // Utility class
    }
    
    /**
     * Converts world coordinate to chunk coordinate (floor division).
     */
    public static int worldToChunk(float worldCoord) {
        return (int) Math.floor(worldCoord / (CHUNK_SIZE_X * BLOCK_SIZE));
    }
    
    /**
     * Converts world coordinate to block index within chunk (0-15).
     */
    public static int worldToBlock(float worldCoord) {
        int blockCoord = (int) Math.floor(worldCoord / BLOCK_SIZE);
        return Math.floorMod(blockCoord, CHUNK_SIZE_X);
    }
    
    /**
     * Converts chunk coordinate to world coordinate.
     */
    public static float chunkToWorld(int chunkCoord) {
        return chunkCoord * CHUNK_SIZE_X * BLOCK_SIZE;
    }
}
