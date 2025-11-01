package com.poorcraft.ultra.voxel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Chunk voxel container storing block ids in a flattened byte array.
 */
public class Chunk {
    public static final int SIZE_X = 16;
    public static final int SIZE_Z = 16;
    public static final int SIZE_Y = 256;
    public static final int VOLUME = SIZE_X * SIZE_Z * SIZE_Y;

    private final int chunkX;
    private final int chunkZ;
    private final byte[] blocks;
    private final List<BlockType> palette;
    private boolean dirty;
    private boolean modified;

    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.blocks = new byte[VOLUME];
        Arrays.fill(this.blocks, BlockType.AIR.id());
        this.palette = new ArrayList<>();
        this.dirty = true;
        this.modified = true;
    }

    public int chunkX() {
        return chunkX;
    }

    public int chunkZ() {
        return chunkZ;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public void setBlock(int x, int y, int z, BlockType type) {
        validateBounds(x, y, z);
        blocks[toIndex(x, y, z)] = type.id();
        dirty = true;
        modified = true;
    }

    public BlockType getBlock(int x, int y, int z) {
        if (!inBounds(x, y, z)) {
            return BlockType.AIR;
        }
        return BlockType.fromId(blocks[toIndex(x, y, z)]);
    }

    public byte getBlockId(int x, int y, int z) {
        if (!inBounds(x, y, z)) {
            return BlockType.AIR.id();
        }
        return blocks[toIndex(x, y, z)];
    }

    public void fill(BlockType type) {
        Arrays.fill(blocks, type.id());
        dirty = true;
        modified = true;
    }

    public void fillCheckerboard() {
        Arrays.fill(blocks, BlockType.AIR.id());
        int y = 64;
        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                BlockType type = ((x + z) & 1) == 0 ? BlockType.STONE : BlockType.DIRT;
                blocks[toIndex(x, y, z)] = type.id();
            }
        }
        dirty = true;
        modified = true;
    }

    public static int toIndex(int x, int y, int z) {
        return x + (z << 4) + (y << 8);
    }

    public static int[] fromIndex(int index) {
        int x = index & 0xF;
        int z = (index >> 4) & 0xF;
        int y = (index >> 8) & 0xFF;
        return new int[] {x, y, z};
    }

    /**
     * Phase 1.35: Return a defensive copy of raw block data for serialization.
     */
    public byte[] getBlockData() {
        return Arrays.copyOf(blocks, blocks.length);
    }

    /**
     * Phase 1.35: Replace block data from deserialized byte array.
     *
     * @param data serialized block data array
     */
    public void setBlockData(byte[] data) {
        if (data.length != VOLUME) {
            throw new IllegalArgumentException(
                    String.format("Invalid block data length: expected %d, got %d", VOLUME, data.length));
        }
        System.arraycopy(data, 0, blocks, 0, VOLUME);
        dirty = true;
        modified = true;
    }

    private static boolean inBounds(int x, int y, int z) {
        return x >= 0 && x < SIZE_X && y >= 0 && y < SIZE_Y && z >= 0 && z < SIZE_Z;
    }

    private static void validateBounds(int x, int y, int z) {
        if (!inBounds(x, y, z)) {
            throw new IllegalArgumentException(
                String.format("Block coordinates out of bounds: (%d,%d,%d)", x, y, z));
        }
    }
}
