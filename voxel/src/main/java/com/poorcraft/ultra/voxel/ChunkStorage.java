package com.poorcraft.ultra.voxel;

import com.poorcraft.ultra.shared.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe storage manager for loaded chunks using packed long keys.
 */
public final class ChunkStorage {

    private static final Logger logger = Logger.getLogger(ChunkStorage.class);

    // Packed long keys keep memory overhead low and avoid object churn versus ChunkPos map entries.
    private final ConcurrentHashMap<Long, Chunk> chunks = new ConcurrentHashMap<>();

    public ChunkStorage() {
        logger.info("Chunk storage initialized");
    }

    public Chunk getChunk(ChunkPos pos) {
        Objects.requireNonNull(pos, "pos must not be null");
        return chunks.get(pos.asLong());
    }

    public Chunk getChunk(int chunkX, int chunkZ) {
        return getChunk(new ChunkPos(chunkX, chunkZ));
    }

    public void putChunk(Chunk chunk) {
        Objects.requireNonNull(chunk, "chunk must not be null");
        chunks.put(chunk.getPosition().asLong(), chunk);
        logger.info("Chunk loaded at ({}, {})", chunk.getPosition().x(), chunk.getPosition().z());
    }

    public Chunk removeChunk(ChunkPos pos) {
        Objects.requireNonNull(pos, "pos must not be null");
        Chunk removed = chunks.remove(pos.asLong());
        if (removed != null) {
            logger.info("Chunk unloaded at ({}, {})", pos.x(), pos.z());
        }
        return removed;
    }

    public boolean containsChunk(ChunkPos pos) {
        Objects.requireNonNull(pos, "pos must not be null");
        return chunks.containsKey(pos.asLong());
    }

    public int getLoadedChunkCount() {
        return chunks.size();
    }

    public Collection<Chunk> getAllChunks() {
        return Collections.unmodifiableCollection(chunks.values());
    }

    public void clear() {
        int removed = chunks.size();
        chunks.clear();
        if (removed > 0) {
            logger.info("All chunks unloaded ({} chunks removed)", removed);
        }
    }

    public short getBlock(int worldX, int worldY, int worldZ) {
        Chunk chunk = getChunk(ChunkPos.fromWorldCoordinates(worldX, worldZ));
        if (chunk == null) {
            return BlockType.AIR.getId();
        }
        int localX = localCoord(worldX);
        int localZ = localCoord(worldZ);
        return chunk.getBlock(localX, worldY, localZ);
    }

    public void setBlock(int worldX, int worldY, int worldZ, short blockId) {
        ChunkPos pos = ChunkPos.fromWorldCoordinates(worldX, worldZ);
        Chunk chunk = getChunk(pos);
        if (chunk == null) {
            logger.warn("Attempted to set block in unloaded chunk ({}, {})", pos.x(), pos.z());
            return;
        }
        int localX = localCoord(worldX);
        int localZ = localCoord(worldZ);
        chunk.setBlock(localX, worldY, localZ, blockId);
    }

    private static int localCoord(int worldCoord) {
        return worldCoord & 15;
    }
}
