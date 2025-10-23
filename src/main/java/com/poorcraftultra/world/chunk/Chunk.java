package com.poorcraftultra.world.chunk;

/**
 * Represents a 16×16×256 column of blocks in the world.
 * A chunk is divided into 16 vertical sections, each 16×16×16 blocks.
 * 
 * Coordinate Systems:
 * - World coordinates: Global position in the world (can be negative)
 * - Chunk coordinates: Position of the chunk in the chunk grid
 * - Local coordinates: Position within a chunk (0-15 for X/Z, 0-255 for Y)
 * - Section-local coordinates: Position within a section (0-15 for all axes)
 * 
 * Sections are lazily allocated to minimize memory usage for empty chunks.
 */
public class Chunk {
    /** Horizontal dimension of a chunk (16 blocks) */
    public static final int CHUNK_SIZE_XZ = 16;
    
    /** Vertical dimension of a chunk (256 blocks) */
    public static final int CHUNK_HEIGHT = 256;
    
    /** Number of vertical sections in a chunk (16) */
    public static final int SECTION_COUNT = 16;

    private final ChunkPos position;
    private final ChunkSection[] sections;

    /**
     * Creates a new chunk at the specified position.
     * All sections are initially null (empty) for memory efficiency.
     *
     * @param position the chunk's position in chunk coordinates
     */
    public Chunk(ChunkPos position) {
        this.position = position;
        this.sections = new ChunkSection[SECTION_COUNT];
    }

    /**
     * @return the position of this chunk in chunk coordinates
     */
    public ChunkPos getPosition() {
        return position;
    }

    /**
     * Gets the block ID at the specified local coordinates.
     *
     * @param x local X coordinate (0-15)
     * @param y local Y coordinate (0-255)
     * @param z local Z coordinate (0-15)
     * @return the block ID (0 for air)
     * @throws IllegalArgumentException if coordinates are out of bounds
     */
    public byte getBlock(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE_XZ || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE_XZ) {
            throw new IllegalArgumentException(
                "Local coordinates out of bounds: (" + x + ", " + y + ", " + z + ")"
            );
        }
        
        int sectionIndex = y >> 4; // Divide by 16
        int localY = y & 15;       // Modulo 16
        
        ChunkSection section = sections[sectionIndex];
        if (section == null) {
            return 0; // Empty section = all air
        }
        
        return section.getBlock(x, localY, z);
    }

    /**
     * Sets the block ID at the specified local coordinates.
     *
     * @param x local X coordinate (0-15)
     * @param y local Y coordinate (0-255)
     * @param z local Z coordinate (0-15)
     * @param blockId the block ID to set
     * @throws IllegalArgumentException if coordinates are out of bounds
     */
    public void setBlock(int x, int y, int z, byte blockId) {
        if (x < 0 || x >= CHUNK_SIZE_XZ || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE_XZ) {
            throw new IllegalArgumentException(
                "Local coordinates out of bounds: (" + x + ", " + y + ", " + z + ")"
            );
        }
        
        int sectionIndex = y >> 4; // Divide by 16
        int localY = y & 15;       // Modulo 16
        
        ensureSection(sectionIndex);
        sections[sectionIndex].setBlock(x, localY, z, blockId);
    }

    /**
     * Gets the block ID at the specified world coordinates.
     * Converts world coordinates to local coordinates using floor modulo.
     *
     * @param worldX world X coordinate
     * @param worldY world Y coordinate
     * @param worldZ world Z coordinate
     * @return the block ID (0 for air)
     * @throws IllegalArgumentException if the world coordinates don't belong to this chunk
     */
    public byte getBlockWorld(int worldX, int worldY, int worldZ) {
        int localX = Math.floorMod(worldX, CHUNK_SIZE_XZ);
        int localY = Math.floorMod(worldY, CHUNK_HEIGHT);
        int localZ = Math.floorMod(worldZ, CHUNK_SIZE_XZ);
        
        return getBlock(localX, localY, localZ);
    }

    /**
     * Sets the block ID at the specified world coordinates.
     * Converts world coordinates to local coordinates using floor modulo.
     *
     * @param worldX world X coordinate
     * @param worldY world Y coordinate
     * @param worldZ world Z coordinate
     * @param blockId the block ID to set
     * @throws IllegalArgumentException if the world coordinates don't belong to this chunk
     */
    public void setBlockWorld(int worldX, int worldY, int worldZ, byte blockId) {
        int localX = Math.floorMod(worldX, CHUNK_SIZE_XZ);
        int localY = Math.floorMod(worldY, CHUNK_HEIGHT);
        int localZ = Math.floorMod(worldZ, CHUNK_SIZE_XZ);
        
        setBlock(localX, localY, localZ, blockId);
    }

    /**
     * Gets the section at the specified index.
     *
     * @param sectionIndex the section index (0-15, bottom to top)
     * @return the section, or null if empty
     * @throws IllegalArgumentException if index is out of bounds
     */
    public ChunkSection getSection(int sectionIndex) {
        if (sectionIndex < 0 || sectionIndex >= SECTION_COUNT) {
            throw new IllegalArgumentException("Section index out of bounds: " + sectionIndex);
        }
        return sections[sectionIndex];
    }

    /**
     * Ensures a section exists at the specified index, creating it if necessary.
     *
     * @param sectionIndex the section index (0-15, bottom to top)
     * @throws IllegalArgumentException if index is out of bounds
     */
    private void ensureSection(int sectionIndex) {
        if (sectionIndex < 0 || sectionIndex >= SECTION_COUNT) {
            throw new IllegalArgumentException("Section index out of bounds: " + sectionIndex);
        }
        
        if (sections[sectionIndex] == null) {
            sections[sectionIndex] = new ChunkSection();
        }
    }

    /**
     * @return true if all sections are empty (null)
     */
    public boolean isEmpty() {
        for (ChunkSection section : sections) {
            if (section != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Optimizes memory usage by freeing empty sections.
     * Should be called periodically or after bulk operations.
     */
    public void optimize() {
        for (int i = 0; i < SECTION_COUNT; i++) {
            if (sections[i] != null) {
                sections[i].optimize();
                if (sections[i].isEmpty()) {
                    sections[i] = null;
                }
            }
        }
    }
}
