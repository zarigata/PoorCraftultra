package com.poorcraftultra.world.chunk;

import com.poorcraftultra.world.generation.WorldGenerator;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages loading, unloading, and accessing chunks in the world.
 * Provides a sparse world representation where only loaded chunks consume memory.
 * 
 * The world is infinite (within integer bounds) and chunks are loaded on-demand.
 * Chunks can be accessed by chunk position or world coordinates.
 * 
 * If a WorldGenerator is configured, chunks are procedurally generated when loaded.
 * Otherwise, empty chunks are created (backward compatible with existing tests).
 */
public class ChunkManager {
    private final Map<ChunkPos, Chunk> loadedChunks;
    private WorldGenerator worldGenerator;

    /**
     * Creates a new chunk manager with no loaded chunks and no world generator.
     * Chunks will be created empty.
     */
    public ChunkManager() {
        this.loadedChunks = new HashMap<>();
        this.worldGenerator = null;
    }

    /**
     * Creates a new chunk manager with the specified world generator.
     * Chunks will be procedurally generated when loaded.
     * 
     * @param worldGenerator the world generator for procedural generation (can be null)
     */
    public ChunkManager(WorldGenerator worldGenerator) {
        this.loadedChunks = new HashMap<>();
        this.worldGenerator = worldGenerator;
    }

    /**
     * Loads a chunk at the specified position.
     * If the chunk is already loaded, returns the existing chunk.
     * If a WorldGenerator is configured, the chunk will be procedurally generated.
     *
     * @param pos the chunk position
     * @return the loaded chunk
     */
    public Chunk loadChunk(ChunkPos pos) {
        return loadedChunks.computeIfAbsent(pos, p -> {
            Chunk chunk = new Chunk(p);
            if (worldGenerator != null) {
                worldGenerator.generateChunk(chunk);
            }
            return chunk;
        });
    }

    /**
     * Unloads the chunk at the specified position.
     *
     * @param pos the chunk position
     * @return the unloaded chunk, or null if it wasn't loaded
     */
    public Chunk unloadChunk(ChunkPos pos) {
        return loadedChunks.remove(pos);
    }

    /**
     * Gets the chunk at the specified position.
     *
     * @param pos the chunk position
     * @return the chunk, or null if not loaded
     */
    public Chunk getChunk(ChunkPos pos) {
        return loadedChunks.get(pos);
    }

    /**
     * Gets the chunk containing the specified world coordinates.
     *
     * @param worldX world X coordinate
     * @param worldY world Y coordinate
     * @param worldZ world Z coordinate
     * @return the chunk, or null if not loaded
     */
    public Chunk getChunkAt(int worldX, int worldY, int worldZ) {
        ChunkPos pos = ChunkPos.fromWorldPos(worldX, worldY, worldZ);
        return getChunk(pos);
    }

    /**
     * Checks if a chunk is loaded at the specified position.
     *
     * @param pos the chunk position
     * @return true if the chunk is loaded
     */
    public boolean isChunkLoaded(ChunkPos pos) {
        return loadedChunks.containsKey(pos);
    }

    /**
     * @return the number of currently loaded chunks
     */
    public int getLoadedChunkCount() {
        return loadedChunks.size();
    }

    /**
     * @return an unmodifiable collection of all loaded chunks
     */
    public Collection<Chunk> getLoadedChunks() {
        return Collections.unmodifiableCollection(loadedChunks.values());
    }

    /**
     * Unloads all chunks.
     */
    public void unloadAllChunks() {
        loadedChunks.clear();
    }

    /**
     * Gets the block ID at the specified world coordinates.
     * Returns 0 (air) if the chunk is not loaded.
     *
     * @param worldX world X coordinate
     * @param worldY world Y coordinate
     * @param worldZ world Z coordinate
     * @return the block ID (0 for air or unloaded)
     */
    public byte getBlock(int worldX, int worldY, int worldZ) {
        Chunk chunk = getChunkAt(worldX, worldY, worldZ);
        if (chunk == null) {
            return 0; // Unloaded chunks are treated as air
        }
        
        return chunk.getBlockWorld(worldX, worldY, worldZ);
    }

    /**
     * Sets the block ID at the specified world coordinates.
     * Loads the chunk if it's not already loaded.
     *
     * @param worldX world X coordinate
     * @param worldY world Y coordinate
     * @param worldZ world Z coordinate
     * @param blockId the block ID to set
     */
    public void setBlock(int worldX, int worldY, int worldZ, byte blockId) {
        ChunkPos pos = ChunkPos.fromWorldPos(worldX, worldY, worldZ);
        Chunk chunk = loadChunk(pos);
        chunk.setBlockWorld(worldX, worldY, worldZ, blockId);
    }

    /**
     * Gets the neighboring chunk at the specified offset.
     *
     * @param pos the base chunk position
     * @param dx X offset in chunks
     * @param dy Y offset in chunks
     * @param dz Z offset in chunks
     * @return the neighbor chunk, or null if not loaded
     */
    public Chunk getNeighbor(ChunkPos pos, int dx, int dy, int dz) {
        ChunkPos neighborPos = new ChunkPos(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
        return getChunk(neighborPos);
    }

    /**
     * Gets all face-adjacent neighbors (6 directions) that are currently loaded.
     *
     * @param pos the chunk position
     * @return a list of loaded neighbor chunks (may be empty, max 6 elements)
     */
    public List<Chunk> getNeighbors(ChunkPos pos) {
        List<Chunk> neighbors = new ArrayList<>();
        
        // Check all 6 face-adjacent positions
        Chunk neighbor;
        
        neighbor = getNeighbor(pos, 1, 0, 0);   // +X
        if (neighbor != null) neighbors.add(neighbor);
        
        neighbor = getNeighbor(pos, -1, 0, 0);  // -X
        if (neighbor != null) neighbors.add(neighbor);
        
        neighbor = getNeighbor(pos, 0, 1, 0);   // +Y
        if (neighbor != null) neighbors.add(neighbor);
        
        neighbor = getNeighbor(pos, 0, -1, 0);  // -Y
        if (neighbor != null) neighbors.add(neighbor);
        
        neighbor = getNeighbor(pos, 0, 0, 1);   // +Z
        if (neighbor != null) neighbors.add(neighbor);
        
        neighbor = getNeighbor(pos, 0, 0, -1);  // -Z
        if (neighbor != null) neighbors.add(neighbor);
        
        return neighbors;
    }

    /**
     * Sets the world generator for procedural chunk generation.
     * 
     * @param generator the world generator (can be null to disable generation)
     */
    public void setWorldGenerator(WorldGenerator generator) {
        this.worldGenerator = generator;
    }

    /**
     * Gets the current world generator.
     * 
     * @return the world generator, or null if not set
     */
    public WorldGenerator getWorldGenerator() {
        return worldGenerator;
    }
}
