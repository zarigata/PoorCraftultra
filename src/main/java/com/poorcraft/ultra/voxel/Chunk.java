package com.poorcraft.ultra.voxel;

import static com.poorcraft.ultra.voxel.ChunkConstants.*;

/**
 * Chunk data storage (16×16×16 blocks).
 */
public class Chunk {
    
    private final int chunkX;
    private final int chunkZ;
    private final short[] blocks;
    private final byte[] skyLight;    // Packed 4-bit skylight values (0-15)
    private final byte[] blockLight;  // Packed 4-bit block light values (0-15)
    private boolean dirty;
    private boolean empty;
    private int nonAirCount;
    
    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.blocks = new short[CHUNK_VOLUME];
        this.skyLight = new byte[2048];  // 4096 blocks / 2 = 2048 bytes
        this.blockLight = new byte[2048];
        this.dirty = true;
        this.empty = true;
        this.nonAirCount = 0;
        
        // Initialize skylight to 15 (full brightness)
        java.util.Arrays.fill(skyLight, (byte) 0xFF);
    }
    
    public int getChunkX() {
        return chunkX;
    }
    
    public int getChunkZ() {
        return chunkZ;
    }
    
    /**
     * Returns block ID at local coordinates (0-15).
     */
    public short getBlock(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE_X || y < 0 || y >= CHUNK_SIZE_Y || z < 0 || z >= CHUNK_SIZE_Z) {
            throw new IndexOutOfBoundsException("Block coordinates out of bounds: (" + x + ", " + y + ", " + z + ")");
        }
        return blocks[toIndex(x, y, z)];
    }
    
    /**
     * Sets block and marks dirty.
     */
    public void setBlock(int x, int y, int z, short blockId) {
        if (x < 0 || x >= CHUNK_SIZE_X || y < 0 || y >= CHUNK_SIZE_Y || z < 0 || z >= CHUNK_SIZE_Z) {
            throw new IndexOutOfBoundsException("Block coordinates out of bounds: (" + x + ", " + y + ", " + z + ")");
        }
        
        int index = toIndex(x, y, z);
        short oldBlockId = blocks[index];
        blocks[index] = blockId;
        dirty = true;
        
        // Update nonAirCount efficiently
        if (oldBlockId == 0 && blockId != 0) {
            // Setting air to non-air
            nonAirCount++;
        } else if (oldBlockId != 0 && blockId == 0) {
            // Setting non-air to air
            nonAirCount--;
        }
        
        // Update empty flag
        empty = (nonAirCount == 0);
    }
    
    /**
     * Returns block with bounds checking (returns 0/air if out of bounds).
     */
    public short getBlockSafe(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE_X || y < 0 || y >= CHUNK_SIZE_Y || z < 0 || z >= CHUNK_SIZE_Z) {
            return 0;
        }
        return blocks[toIndex(x, y, z)];
    }
    
    /**
     * Converts XYZ to array index (YZX order).
     */
    private int toIndex(int x, int y, int z) {
        return y * CHUNK_SIZE_X * CHUNK_SIZE_Z + z * CHUNK_SIZE_X + x;
    }
    
    public boolean isEmpty() {
        return empty;
    }
    
    public boolean isDirty() {
        return dirty;
    }
    
    public void markDirty() {
        this.dirty = true;
    }
    
    public void clearDirty() {
        this.dirty = false;
    }
    
    /**
     * Fills entire chunk with one block type (for testing).
     */
    public void fill(short blockId) {
        for (int i = 0; i < blocks.length; i++) {
            blocks[i] = blockId;
        }
        dirty = true;
        empty = (blockId == 0);
        nonAirCount = (blockId == 0) ? 0 : CHUNK_VOLUME;
    }
    
    // ===== Lighting Methods =====
    
    /**
     * Returns skylight level (0-15) at local coordinates.
     */
    public int getSkyLight(int x, int y, int z) {
        int index = toIndex(x, y, z);
        int byteIndex = index / 2;
        int shift = (index % 2) * 4;
        return (skyLight[byteIndex] >> shift) & 0x0F;
    }
    
    /**
     * Sets skylight level (0-15) at local coordinates.
     */
    public void setSkyLight(int x, int y, int z, int level) {
        level = Math.max(0, Math.min(15, level));
        int index = toIndex(x, y, z);
        int byteIndex = index / 2;
        int shift = (index % 2) * 4;
        skyLight[byteIndex] = (byte)((skyLight[byteIndex] & ~(0x0F << shift)) | (level << shift));
    }
    
    /**
     * Returns block light level (0-15) at local coordinates.
     */
    public int getBlockLight(int x, int y, int z) {
        int index = toIndex(x, y, z);
        int byteIndex = index / 2;
        int shift = (index % 2) * 4;
        return (blockLight[byteIndex] >> shift) & 0x0F;
    }
    
    /**
     * Sets block light level (0-15) at local coordinates.
     */
    public void setBlockLight(int x, int y, int z, int level) {
        level = Math.max(0, Math.min(15, level));
        int index = toIndex(x, y, z);
        int byteIndex = index / 2;
        int shift = (index % 2) * 4;
        blockLight[byteIndex] = (byte)((blockLight[byteIndex] & ~(0x0F << shift)) | (level << shift));
    }
    
    /**
     * Returns combined light level (max of skylight and blocklight).
     */
    public int getCombinedLight(int x, int y, int z) {
        return Math.max(getSkyLight(x, y, z), getBlockLight(x, y, z));
    }
    
    /**
     * Returns raw skylight array for serialization.
     */
    public byte[] getSkyLightArray() {
        return skyLight;
    }
    
    /**
     * Sets skylight array from deserialization.
     */
    public void setSkyLightArray(byte[] data) {
        System.arraycopy(data, 0, skyLight, 0, Math.min(data.length, skyLight.length));
    }
    
    /**
     * Returns raw block light array for serialization.
     */
    public byte[] getBlockLightArray() {
        return blockLight;
    }
    
    /**
     * Sets block light array from deserialization.
     */
    public void setBlockLightArray(byte[] data) {
        System.arraycopy(data, 0, blockLight, 0, Math.min(data.length, blockLight.length));
    }
}
