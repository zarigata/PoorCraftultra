package com.poorcraft.ultra.save;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jme3.math.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * World metadata storage class.
 * Stores world-level data (seed, spawn, gamerules) for save/load.
 */
public class WorldMetadata {

    private long seed;
    private float spawnX;
    private float spawnY;
    private float spawnZ;
    private long worldTime;
    private String gamemode;
    private String difficulty;
    private Map<String, String> gamerules;

    /**
     * Default constructor for deserialization.
     */
    public WorldMetadata() {
        this.gamerules = new HashMap<>();
    }

    /**
     * Creates metadata with seed and spawn position.
     */
    public WorldMetadata(long seed, Vector3f spawn) {
        this.seed = seed;
        this.spawnX = spawn.x;
        this.spawnY = spawn.y;
        this.spawnZ = spawn.z;
        this.worldTime = 0;
        this.gamemode = "survival";
        this.difficulty = "normal";
        this.gamerules = new HashMap<>();
        this.gamerules.put("doDaylightCycle", "true");
        this.gamerules.put("doMobSpawning", "true");
    }

    // ===== Getters and Setters =====

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public float getSpawnX() {
        return spawnX;
    }

    public void setSpawnX(float spawnX) {
        this.spawnX = spawnX;
    }

    public float getSpawnY() {
        return spawnY;
    }

    public void setSpawnY(float spawnY) {
        this.spawnY = spawnY;
    }

    public float getSpawnZ() {
        return spawnZ;
    }

    public void setSpawnZ(float spawnZ) {
        this.spawnZ = spawnZ;
    }

    public long getWorldTime() {
        return worldTime;
    }

    public void setWorldTime(long worldTime) {
        this.worldTime = worldTime;
    }

    public String getGamemode() {
        return gamemode;
    }

    public void setGamemode(String gamemode) {
        this.gamemode = gamemode;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public Map<String, String> getGamerules() {
        return gamerules;
    }

    public void setGamerules(Map<String, String> gamerules) {
        this.gamerules = gamerules;
    }

    /**
     * Returns spawn position as Vector3f.
     */
    public Vector3f getSpawnPosition() {
        return new Vector3f(spawnX, spawnY, spawnZ);
    }

    // ===== Serialization =====

    /**
     * Serializes to JSON string.
     */
    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize WorldMetadata", e);
        }
    }

    /**
     * Deserializes from JSON string.
     */
    public static WorldMetadata fromJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, WorldMetadata.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize WorldMetadata", e);
        }
    }
}
