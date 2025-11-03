package com.poorcraft.ultra.save;

import com.jme3.math.Vector3f;
import com.poorcraft.ultra.voxel.Chunk;
import com.poorcraft.ultra.voxel.ChunkManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * High-level save/load manager (CP v1.3).
 */
public class SaveManager {
    
    private static final Logger logger = LoggerFactory.getLogger(SaveManager.class);
    
    private final String worldName;
    private final Path saveDirectory;
    private final ChunkSerializer chunkSerializer;
    private final ChunkManager chunkManager;
    
    public SaveManager(String worldName, ChunkManager chunkManager) {
        this.worldName = worldName;
        this.chunkManager = chunkManager;
        this.saveDirectory = Paths.get("saves", worldName);
        
        try {
            Files.createDirectories(saveDirectory);
            Files.createDirectories(saveDirectory.resolve("chunks"));
            Files.createDirectories(saveDirectory.resolve("player"));
            Files.createDirectories(saveDirectory.resolve("world"));
        } catch (IOException e) {
            logger.error("Failed to create save directories", e);
        }
        
        this.chunkSerializer = new ChunkSerializer(saveDirectory.resolve("chunks"));
        
        logger.info("SaveManager initialized for world: {}", worldName);
    }
    
    /**
     * Saves entire world including player data (CP v3.1).
     */
    public void saveWorld(PlayerData playerData) {
        try {
            Collection<Chunk> chunks = chunkManager.getAllChunks();
            chunkSerializer.saveChunks(chunks);
            if (playerData != null) {
                savePlayerData(playerData);
            }
            logger.info("Saved world: {} ({} chunks)", worldName, chunks.size());
        } catch (IOException e) {
            logger.error("Failed to save world: {}", worldName, e);
        }
    }
    
    /**
     * Saves chunks near player.
     */
    public void saveChunksInRadius(Vector3f center, int radius) {
        try {
            Collection<Chunk> allChunks = chunkManager.getAllChunks();
            List<Chunk> chunksToSave = new ArrayList<>();
            
            int centerChunkX = (int) Math.floor(center.x / 16);
            int centerChunkZ = (int) Math.floor(center.z / 16);
            
            for (Chunk chunk : allChunks) {
                int dx = Math.abs(chunk.getChunkX() - centerChunkX);
                int dz = Math.abs(chunk.getChunkZ() - centerChunkZ);
                if (dx <= radius && dz <= radius) {
                    chunksToSave.add(chunk);
                }
            }
            
            chunkSerializer.saveChunks(chunksToSave);
            logger.info("Saved {} chunks around ({}, {})", chunksToSave.size(), centerChunkX, centerChunkZ);
        } catch (IOException e) {
            logger.error("Failed to save chunks in radius", e);
        }
    }
    
    /**
     * Saves world metadata.
     */
    public void saveWorldMetadata(WorldMetadata metadata) {
        try {
            Path metadataPath = saveDirectory.resolve("world").resolve("metadata.json");
            String json = metadata.toJson();
            Files.writeString(metadataPath, json, StandardCharsets.UTF_8);
            logger.info("Saved world metadata for: {}", worldName);
        } catch (IOException e) {
            logger.error("Failed to save world metadata", e);
        }
    }
    
    /**
     * Loads world metadata.
     */
    public WorldMetadata loadWorldMetadata() {
        try {
            Path metadataPath = saveDirectory.resolve("world").resolve("metadata.json");
            if (Files.exists(metadataPath)) {
                String json = Files.readString(metadataPath, StandardCharsets.UTF_8);
                WorldMetadata metadata = WorldMetadata.fromJson(json);
                logger.info("Loaded world metadata for: {} (seed: {})", worldName, metadata.getSeed());
                return metadata;
            }
        } catch (IOException e) {
            logger.error("Failed to load world metadata", e);
        }
        return null;
    }
    
    /**
     * Saves player data (CP v3.1).
     */
    public void savePlayerData(PlayerData playerData) {
        try {
            Path playerDataPath = saveDirectory.resolve("player").resolve("playerdata.json");
            String json = playerData.toJson();
            Files.writeString(playerDataPath, json, StandardCharsets.UTF_8);
            logger.info("Saved player data");
        } catch (IOException e) {
            logger.error("Failed to save player data", e);
        }
    }
    
    /**
     * Loads player data (CP v3.1).
     */
    public PlayerData loadPlayerData() {
        try {
            Path playerDataPath = saveDirectory.resolve("player").resolve("playerdata.json");
            if (Files.exists(playerDataPath)) {
                String json = Files.readString(playerDataPath, StandardCharsets.UTF_8);
                PlayerData playerData = PlayerData.fromJson(json);
                logger.info("Loaded player data");
                return playerData;
            }
        } catch (IOException e) {
            logger.error("Failed to load player data", e);
        }
        logger.info("No player data found, creating new");
        return new PlayerData();
    }
    
    /**
     * Loads world data and returns spawn position.
     */
    public Vector3f loadWorld() {
        WorldMetadata metadata = loadWorldMetadata();
        if (metadata != null) {
            logger.info("Loaded world: {} (seed: {})", worldName, metadata.getSeed());
            return metadata.getSpawnPosition();
        }
        logger.info("No metadata found for world: {}, using default spawn", worldName);
        return new Vector3f(8, 80, 8);
    }
    
    /**
     * Loads chunks near player.
     */
    public void loadChunksInRadius(Vector3f center, int radius) {
        int centerChunkX = (int) Math.floor(center.x / 16);
        int centerChunkZ = (int) Math.floor(center.z / 16);
        
        List<ChunkSerializer.ChunkCoord> coords = new ArrayList<>();
        for (int x = centerChunkX - radius; x <= centerChunkX + radius; x++) {
            for (int z = centerChunkZ - radius; z <= centerChunkZ + radius; z++) {
                coords.add(new ChunkSerializer.ChunkCoord(x, z));
            }
        }
        
        List<Chunk> chunks = chunkSerializer.loadChunks(coords);
        
        // Add loaded chunks to chunk manager
        for (Chunk chunk : chunks) {
            chunkManager.addOrReplaceChunk(chunk);
            logger.debug("Loaded chunk ({}, {}) from save", chunk.getChunkX(), chunk.getChunkZ());
        }
        
        logger.info("Loaded {} chunks around ({}, {})", chunks.size(), centerChunkX, centerChunkZ);
    }
}
