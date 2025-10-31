package com.poorcraft.ultra.world;

import com.poorcraft.ultra.voxel.ChunkPos;

/**
 * Immutable region coordinate based on 32x32 chunk grids.
 */
public record RegionPos(int x, int z) {

    public static final int REGION_SIZE = 32;
    public static final int REGION_MASK = REGION_SIZE - 1;

    public static RegionPos fromChunkPos(ChunkPos chunkPos) {
        if (chunkPos == null) {
            throw new IllegalArgumentException("chunkPos must not be null");
        }
        return new RegionPos(chunkPos.x() >> 5, chunkPos.z() >> 5);
    }

    public static RegionPos fromChunkCoordinates(int chunkX, int chunkZ) {
        return new RegionPos(chunkX >> 5, chunkZ >> 5);
    }

    public String getFileName() {
        return "r." + x + '.' + z + ".mca";
    }

    public int getChunkOffset(ChunkPos chunkPos) {
        if (!containsChunk(chunkPos)) {
            throw new IllegalArgumentException("Chunk " + chunkPos + " is not part of region " + this);
        }
        int localX = chunkPos.x() & REGION_MASK;
        int localZ = chunkPos.z() & REGION_MASK;
        return localX + (localZ * REGION_SIZE);
    }

    public boolean containsChunk(ChunkPos chunkPos) {
        if (chunkPos == null) {
            return false;
        }
        return fromChunkPos(chunkPos).equals(this);
    }
}
