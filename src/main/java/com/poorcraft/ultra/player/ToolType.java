package com.poorcraft.ultra.player;

/**
 * Minimal tool categories used to determine block breaking speeds.
 */
public enum ToolType {
    NONE(1.0f),
    WOOD(1.2f),
    STONE(1.6f);

    private final float speedMultiplier;

    ToolType(float speedMultiplier) {
        this.speedMultiplier = speedMultiplier;
    }

    public float speedMultiplier() {
        return speedMultiplier;
    }
}
