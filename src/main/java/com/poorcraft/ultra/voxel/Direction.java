package com.poorcraft.ultra.voxel;

import com.jme3.math.Vector3f;

public enum Direction {
    UP(0, 1, 0, new Vector3f(0f, 1f, 0f)),
    DOWN(0, -1, 0, new Vector3f(0f, -1f, 0f)),
    NORTH(0, 0, -1, new Vector3f(0f, 0f, -1f)),
    SOUTH(0, 0, 1, new Vector3f(0f, 0f, 1f)),
    EAST(1, 0, 0, new Vector3f(1f, 0f, 0f)),
    WEST(-1, 0, 0, new Vector3f(-1f, 0f, 0f));

    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;
    private final Vector3f normal;

    Direction(int offsetX, int offsetY, int offsetZ, Vector3f normal) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.normal = normal;
    }

    public int offsetX() {
        return offsetX;
    }

    public int offsetY() {
        return offsetY;
    }

    public int offsetZ() {
        return offsetZ;
    }

    public Vector3f normal() {
        return normal;
    }
}
