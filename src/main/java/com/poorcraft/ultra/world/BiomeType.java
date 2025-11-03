package com.poorcraft.ultra.world;

/**
 * Enumerates available biome types for Phase 2 world generation.
 */
public enum BiomeType {
    PLAINS,
    FOREST,
    DESERT;

    public static BiomeType fromTemperatureAndMoisture(float temperature, float moisture) {
        if (temperature > 0.6f && moisture < 0.3f) {
            return DESERT;
        }
        if (temperature > 0.3f && moisture > 0.5f) {
            return FOREST;
        }
        return PLAINS;
    }
}
