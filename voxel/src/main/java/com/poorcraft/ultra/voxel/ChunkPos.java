package com.poorcraft.ultra.voxel;

/**
 * Immutable chunk coordinate pair with helpers for packed-key conversions.
 */
public record ChunkPos(int x, int z) {

    private static final int CHUNK_SIZE = 16;

    public static ChunkPos fromLong(long packedKey) {
        int chunkX = (int) (packedKey >> 32);
        int chunkZ = (int) packedKey;
        return new ChunkPos(chunkX, chunkZ);
    }

    public static ChunkPos fromWorldCoordinates(float worldX, float worldZ) {
        int chunkX = (int) Math.floor(worldX / (float) CHUNK_SIZE);
        int chunkZ = (int) Math.floor(worldZ / (float) CHUNK_SIZE);
        return new ChunkPos(chunkX, chunkZ);
    }

    public static ChunkPos fromWorldCoordinates(int worldX, int worldZ) {
        int chunkX = Math.floorDiv(worldX, CHUNK_SIZE);
        int chunkZ = Math.floorDiv(worldZ, CHUNK_SIZE);
        return new ChunkPos(chunkX, chunkZ);
    }

    public long asLong() {
        return ((long) x << 32) | ((long) z & 0xFFFFFFFFL);
    }

    public ChunkPos getNeighbor(int dx, int dz) {
        return new ChunkPos(x + dx, z + dz);
    }

    public int getWorldX() {
        return x * CHUNK_SIZE;
    }

    public int getWorldZ() {
        return z * CHUNK_SIZE;
    }
}
