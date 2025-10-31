package com.poorcraft.ultra.voxel;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

/**
 * Represents a 16x256x16 block chunk using a packed short array for storage.
 */
public final class Chunk {

    public static final int CHUNK_SIZE_X = 16;
    public static final int CHUNK_SIZE_Y = 256;
    public static final int CHUNK_SIZE_Z = 16;
    public static final int TOTAL_BLOCKS = CHUNK_SIZE_X * CHUNK_SIZE_Y * CHUNK_SIZE_Z;

    private final ChunkPos position;
    private final short[] blocks;
    private volatile boolean dirty;

    public Chunk(ChunkPos position) {
        this.position = Objects.requireNonNull(position, "position must not be null");
        this.blocks = new short[TOTAL_BLOCKS];
        Arrays.fill(blocks, BlockType.AIR.getId());
        this.dirty = true;
    }

    public ChunkPos getPosition() {
        return position;
    }

    public short getBlock(int x, int y, int z) {
        int index = toIndexValidated(x, y, z);
        return blocks[index];
    }

    public short setBlock(int x, int y, int z, short blockId) {
        int index = toIndexValidated(x, y, z);
        short previous = blocks[index];
        if (previous != blockId) {
            blocks[index] = blockId;
            dirty = true;
        }
        return previous;
    }

    public void fill(short blockId) {
        Arrays.fill(blocks, blockId);
        dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public void clearDirty() {
        this.dirty = false;
    }

    public int getBlockCount() {
        return blocks.length;
    }

    short[] getBlocksArray() {
        return blocks;
    }

    public void copyBlocksTo(ByteBuffer target) {
        Objects.requireNonNull(target, "target must not be null");
        int required = blocks.length * Short.BYTES;
        if (target.remaining() < required) {
            throw new IllegalArgumentException("ByteBuffer does not have enough remaining space: required " + required);
        }
        for (short block : blocks) {
            target.putShort(block);
        }
    }

    /**
     * Replaces the chunk contents with the provided block array.
     *
     * @param data block data to copy into this chunk
     * @throws IllegalArgumentException if the array length does not match the chunk size
     */
    public void setBlocksArray(short[] data) {
        Objects.requireNonNull(data, "data must not be null");
        if (data.length != blocks.length) {
            throw new IllegalArgumentException(
                    "Expected block array of length " + blocks.length + " but got " + data.length);
        }
        System.arraycopy(data, 0, blocks, 0, blocks.length);
    }

    private static int toIndexValidated(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE_X
                || y < 0 || y >= CHUNK_SIZE_Y
                || z < 0 || z >= CHUNK_SIZE_Z) {
            throw new IndexOutOfBoundsException("Chunk coordinates out of bounds: (" + x + ", " + y + ", " + z + ")");
        }
        return x + (z << 4) + (y << 8);
    }
}
