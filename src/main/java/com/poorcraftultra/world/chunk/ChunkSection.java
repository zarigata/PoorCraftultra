package com.poorcraftultra.world.chunk;

/**
 * Represents a 16×16×16 section of blocks within a chunk.
 * Uses Y-major memory layout for optimal cache locality during vertical traversals
 * (lighting, meshing, etc.). The indexing formula is: (y << 8) | (z << 4) | x
 * 
 * This means blocks are stored in order: all Y values for x=0,z=0, then all Y values
 * for x=1,z=0, etc. This layout is cache-friendly for operations that traverse vertically.
 * 
 * Sections can be null (empty) to save memory when all blocks are air.
 */
public class ChunkSection {
    /** The dimension of a section (16 blocks) */
    public static final int SECTION_SIZE = 16;
    
    /** The total number of blocks in a section (4096) */
    public static final int SECTION_VOLUME = SECTION_SIZE * SECTION_SIZE * SECTION_SIZE;

    /** Block data storage. Null for empty sections (all air). */
    private byte[] blocks;

    /**
     * Creates a new empty chunk section (all air, no memory allocated).
     */
    public ChunkSection() {
        this.blocks = null;
    }

    /**
     * @return true if this section is empty (all air)
     */
    public boolean isEmpty() {
        return blocks == null;
    }

    /**
     * Ensures the block array is allocated. Called before setting blocks.
     * If already initialized, does nothing.
     */
    private void ensureInitialized() {
        if (blocks == null) {
            blocks = new byte[SECTION_VOLUME];
        }
    }

    /**
     * Gets the block ID at the specified local coordinates.
     *
     * @param x local X coordinate (0-15)
     * @param y local Y coordinate (0-15)
     * @param z local Z coordinate (0-15)
     * @return the block ID (0 for air)
     * @throws IllegalArgumentException if coordinates are out of bounds
     */
    public byte getBlock(int x, int y, int z) {
        if (x < 0 || x >= SECTION_SIZE || y < 0 || y >= SECTION_SIZE || z < 0 || z >= SECTION_SIZE) {
            throw new IllegalArgumentException(
                "Coordinates out of bounds: (" + x + ", " + y + ", " + z + ")"
            );
        }
        
        if (isEmpty()) {
            return 0; // Air
        }
        
        return blocks[index(x, y, z)];
    }

    /**
     * Sets the block ID at the specified local coordinates.
     *
     * @param x local X coordinate (0-15)
     * @param y local Y coordinate (0-15)
     * @param z local Z coordinate (0-15)
     * @param blockId the block ID to set
     * @throws IllegalArgumentException if coordinates are out of bounds
     */
    public void setBlock(int x, int y, int z, byte blockId) {
        if (x < 0 || x >= SECTION_SIZE || y < 0 || y >= SECTION_SIZE || z < 0 || z >= SECTION_SIZE) {
            throw new IllegalArgumentException(
                "Coordinates out of bounds: (" + x + ", " + y + ", " + z + ")"
            );
        }
        
        ensureInitialized();
        blocks[index(x, y, z)] = blockId;
    }

    /**
     * Calculates the array index for the given coordinates using Y-major ordering.
     * Formula: (y << 8) | (z << 4) | x
     * This provides optimal cache locality for vertical traversals.
     *
     * @param x local X coordinate (0-15)
     * @param y local Y coordinate (0-15)
     * @param z local Z coordinate (0-15)
     * @return the array index
     */
    private int index(int x, int y, int z) {
        return (y << 8) | (z << 4) | x;
    }

    /**
     * Counts the number of non-air blocks in this section.
     *
     * @return the count of non-air blocks
     */
    public int getBlockCount() {
        if (isEmpty()) {
            return 0;
        }
        
        int count = 0;
        for (byte block : blocks) {
            if (block != 0) {
                count++;
            }
        }
        return count;
    }

    /**
     * Optimizes memory usage by freeing the block array if all blocks are air.
     * Should be called periodically or after bulk operations that may empty a section.
     */
    public void optimize() {
        if (isEmpty()) {
            return;
        }
        
        for (byte block : blocks) {
            if (block != 0) {
                return; // Found a non-air block, keep the array
            }
        }
        
        // All blocks are air, free the memory
        blocks = null;
    }
}
