package com.poorcraftultra.world.chunk;

import java.util.Objects;

/**
 * Immutable value class representing chunk coordinates in the world.
 * Chunks are positioned on a grid, with each chunk covering a 16×16×16 block region.
 * This class serves as the key in the ChunkManager's HashMap and ensures correct
 * coordinate handling for negative positions using floor division.
 */
public class ChunkPos {
    private final int x;
    private final int y;
    private final int z;

    /**
     * Creates a new chunk position.
     *
     * @param x the chunk X coordinate
     * @param y the chunk Y coordinate
     * @param z the chunk Z coordinate
     */
    public ChunkPos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Converts world coordinates to chunk coordinates.
     * Uses floor division to correctly handle negative coordinates.
     *
     * @param worldX the world X coordinate
     * @param worldY the world Y coordinate
     * @param worldZ the world Z coordinate
     * @return a ChunkPos representing the chunk containing the world position
     */
    public static ChunkPos fromWorldPos(int worldX, int worldY, int worldZ) {
        return new ChunkPos(
            Math.floorDiv(worldX, 16),
            Math.floorDiv(worldY, 256),
            Math.floorDiv(worldZ, 16)
        );
    }

    /**
     * @return the chunk X coordinate
     */
    public int getX() {
        return x;
    }

    /**
     * @return the chunk Y coordinate
     */
    public int getY() {
        return y;
    }

    /**
     * @return the chunk Z coordinate
     */
    public int getZ() {
        return z;
    }

    /**
     * Converts this chunk position to the minimum world coordinates it contains.
     *
     * @return an array [worldX, worldY, worldZ] representing the minimum corner
     */
    public int[] toWorldPos() {
        return new int[] { x * 16, y * 256, z * 16 };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChunkPos chunkPos = (ChunkPos) o;
        return x == chunkPos.x && y == chunkPos.y && z == chunkPos.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() {
        return "ChunkPos[" + x + ", " + y + ", " + z + "]";
    }
}
