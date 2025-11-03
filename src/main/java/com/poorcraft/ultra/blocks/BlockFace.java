package com.poorcraft.ultra.blocks;

import com.jme3.math.Vector3f;

/**
 * Enum for block face directions.
 */
public enum BlockFace {
    NORTH(new Vector3f(0, 0, -1)),
    SOUTH(new Vector3f(0, 0, 1)),
    EAST(new Vector3f(1, 0, 0)),
    WEST(new Vector3f(-1, 0, 0)),
    UP(new Vector3f(0, 1, 0)),
    DOWN(new Vector3f(0, -1, 0));
    
    private final Vector3f normal;
    private BlockFace opposite;
    
    static {
        NORTH.opposite = SOUTH;
        SOUTH.opposite = NORTH;
        EAST.opposite = WEST;
        WEST.opposite = EAST;
        UP.opposite = DOWN;
        DOWN.opposite = UP;
    }
    
    BlockFace(Vector3f normal) {
        this.normal = normal;
    }
    
    public Vector3f getNormal() {
        return normal.clone();
    }
    
    public BlockFace getOpposite() {
        return opposite;
    }
    
    /**
     * Finds face from normal vector (for ray-picking).
     */
    public static BlockFace fromNormal(Vector3f normal) {
        for (BlockFace face : values()) {
            if (face.normal.equals(normal)) {
                return face;
            }
        }
        // Find closest match
        float maxDot = -1;
        BlockFace closest = NORTH;
        for (BlockFace face : values()) {
            float dot = face.normal.dot(normal);
            if (dot > maxDot) {
                maxDot = dot;
                closest = face;
            }
        }
        return closest;
    }
}
