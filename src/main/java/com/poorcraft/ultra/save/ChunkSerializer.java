package com.poorcraft.ultra.save;

import com.poorcraft.ultra.voxel.Chunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.poorcraft.ultra.voxel.ChunkConstants.*;

/**
 * Binary serialization for chunks (CP v1.3).
 */
public class ChunkSerializer {
    
    private static final Logger logger = LoggerFactory.getLogger(ChunkSerializer.class);
    
    private final Path saveDirectory;
    private static final byte[] MAGIC = {'P', 'C', 'U', 'C'}; // Poorcraft Ultra Chunk
    private static final short VERSION = 2; // Version 2: Added lighting data
    
    public ChunkSerializer(Path saveDirectory) {
        this.saveDirectory = saveDirectory;
        try {
            Files.createDirectories(saveDirectory);
        } catch (IOException e) {
            logger.error("Failed to create save directory", e);
        }
        logger.info("ChunkSerializer initialized: {}", saveDirectory);
    }
    
    /**
     * Saves chunk to disk.
     */
    public void saveChunk(Chunk chunk) throws IOException {
        String filename = "chunk_" + chunk.getChunkX() + "_" + chunk.getChunkZ() + ".dat";
        Path filePath = saveDirectory.resolve(filename);
        
        // Create buffer: header(16) + blockData(4 + 8192) + skyLight(2048) + blockLight(2048) + checksum(32) = 12340 bytes
        ByteBuffer buffer = ByteBuffer.allocate(12340);
        
        // Write header
        buffer.put(MAGIC);
        buffer.putShort(VERSION);
        buffer.putInt(chunk.getChunkX());
        buffer.putInt(chunk.getChunkZ());
        buffer.putShort((short) 0); // Flags
        
        // Write block data
        buffer.putInt(CHUNK_VOLUME);
        for (int y = 0; y < CHUNK_SIZE_Y; y++) {
            for (int z = 0; z < CHUNK_SIZE_Z; z++) {
                for (int x = 0; x < CHUNK_SIZE_X; x++) {
                    buffer.putShort(chunk.getBlock(x, y, z));
                }
            }
        }
        
        // Write lighting data
        buffer.put(chunk.getSkyLightArray());
        buffer.put(chunk.getBlockLightArray());
        
        // Compute checksum
        byte[] data = new byte[12308]; // header + blockData + lighting
        buffer.position(0);
        buffer.get(data);
        byte[] checksum = computeChecksum(data);
        buffer.put(checksum);
        
        // Write to file
        Files.write(filePath, buffer.array());
        
        logger.debug("Saved chunk ({}, {}) v{}: {} bytes", chunk.getChunkX(), chunk.getChunkZ(), VERSION, buffer.capacity());
    }
    
    /**
     * Saves multiple chunks.
     */
    public void saveChunks(Collection<Chunk> chunks) throws IOException {
        for (Chunk chunk : chunks) {
            saveChunk(chunk);
        }
        logger.info("Saved {} chunks", chunks.size());
    }
    
    /**
     * Loads chunk from disk.
     */
    public Chunk loadChunk(int chunkX, int chunkZ) throws IOException {
        String filename = "chunk_" + chunkX + "_" + chunkZ + ".dat";
        Path filePath = saveDirectory.resolve(filename);
        
        if (!Files.exists(filePath)) {
            logger.debug("Chunk file not found: {}", filename);
            return null;
        }
        
        byte[] fileData = Files.readAllBytes(filePath);
        
        // Support both v1 (8244 bytes) and v2 (12340 bytes)
        if (fileData.length != 8244 && fileData.length != 12340) {
            throw new IOException("Invalid chunk file size: " + fileData.length);
        }
        
        ByteBuffer buffer = ByteBuffer.wrap(fileData);
        
        // Read and verify header
        byte[] magic = new byte[4];
        buffer.get(magic);
        if (!java.util.Arrays.equals(magic, MAGIC)) {
            throw new IOException("Invalid magic bytes");
        }
        
        short version = buffer.getShort();
        if (version != 1 && version != VERSION) {
            throw new IOException("Unsupported version: " + version);
        }
        
        int readChunkX = buffer.getInt();
        int readChunkZ = buffer.getInt();
        if (readChunkX != chunkX || readChunkZ != chunkZ) {
            throw new IOException("Chunk coordinates mismatch");
        }
        
        buffer.getShort(); // Flags
        
        // Read data
        int blockCount = buffer.getInt();
        if (blockCount != CHUNK_VOLUME) {
            throw new IOException("Invalid block count: " + blockCount);
        }
        
        Chunk chunk = new Chunk(chunkX, chunkZ);
        for (int y = 0; y < CHUNK_SIZE_Y; y++) {
            for (int z = 0; z < CHUNK_SIZE_Z; z++) {
                for (int x = 0; x < CHUNK_SIZE_X; x++) {
                    chunk.setBlock(x, y, z, buffer.getShort());
                }
            }
        }
        
        // Read lighting data (v2 only)
        if (version == 2) {
            byte[] skyLight = new byte[2048];
            byte[] blockLight = new byte[2048];
            buffer.get(skyLight);
            buffer.get(blockLight);
            chunk.setSkyLightArray(skyLight);
            chunk.setBlockLightArray(blockLight);
        } else {
            // v1: Initialize with default lighting
            logger.debug("Loading v1 chunk, initializing default lighting");
        }
        
        // Verify checksum
        byte[] expectedChecksum = new byte[32];
        buffer.get(expectedChecksum);
        
        int dataSize = version == 2 ? 12308 : 8212;
        byte[] data = new byte[dataSize];
        System.arraycopy(fileData, 0, data, 0, dataSize);
        byte[] actualChecksum = computeChecksum(data);
        
        if (!java.util.Arrays.equals(expectedChecksum, actualChecksum)) {
            throw new IOException("Checksum mismatch");
        }
        
        chunk.clearDirty();
        logger.debug("Loaded chunk ({}, {}): {} bytes", chunkX, chunkZ, fileData.length);
        return chunk;
    }
    
    /**
     * Loads multiple chunks.
     */
    public List<Chunk> loadChunks(Collection<ChunkCoord> coords) {
        List<Chunk> chunks = new ArrayList<>();
        for (ChunkCoord coord : coords) {
            try {
                Chunk chunk = loadChunk(coord.x(), coord.z());
                if (chunk != null) {
                    chunks.add(chunk);
                }
            } catch (IOException e) {
                logger.warn("Failed to load chunk ({}, {})", coord.x(), coord.z(), e);
            }
        }
        logger.info("Loaded {} chunks", chunks.size());
        return chunks;
    }
    
    private byte[] computeChecksum(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute checksum", e);
        }
    }
    
    public record ChunkCoord(int x, int z) {}
}
