package com.poorcraftultra.world.block;

import org.joml.Vector3i;

/**
 * Enum representing the six faces of a block.
 * <p>
 * The coordinate system is Y-up, right-handed:
 * <ul>
 *   <li>+X is East</li>
 *   <li>-X is West</li>
 *   <li>+Y is Up (Top)</li>
 *   <li>-Y is Down (Bottom)</li>
 *   <li>+Z is South</li>
 *   <li>-Z is North</li>
 * </ul>
 * <p>
 * Each face has a direction offset and can be queried for its opposite face.
 */
public enum BlockFace {
    TOP(0, 1, 0, "Top"),
    BOTTOM(0, -1, 0, "Bottom"),
    NORTH(0, 0, -1, "North"),
    SOUTH(0, 0, 1, "South"),
    EAST(1, 0, 0, "East"),
    WEST(-1, 0, 0, "West");

    private final int dx;
    private final int dy;
    private final int dz;
    private final String displayName;

    /**
     * Creates a BlockFace with the specified direction offset and display name.
     *
     * @param dx          X direction offset
     * @param dy          Y direction offset
     * @param dz          Z direction offset
     * @param displayName human-readable name
     */
    BlockFace(int dx, int dy, int dz, String displayName) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.displayName = displayName;
    }

    /**
     * @return the X direction offset
     */
    public int getDx() {
        return dx;
    }

    /**
     * @return the Y direction offset
     */
    public int getDy() {
        return dy;
    }

    /**
     * @return the Z direction offset
     */
    public int getDz() {
        return dz;
    }

    /**
     * @return the human-readable display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the opposite face.
     * <p>
     * Mappings: TOP↔BOTTOM, NORTH↔SOUTH, EAST↔WEST
     *
     * @return the opposite face
     */
    public BlockFace getOpposite() {
        return switch (this) {
            case TOP -> BOTTOM;
            case BOTTOM -> TOP;
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case EAST -> WEST;
            case WEST -> EAST;
        };
    }

    /**
     * Returns the direction offset as a Vector3i.
     *
     * @return a new Vector3i with the direction offset
     */
    public Vector3i getOffset() {
        return new Vector3i(dx, dy, dz);
    }

    /**
     * Converts a direction offset to a BlockFace.
     *
     * @param dx X direction offset
     * @param dy Y direction offset
     * @param dz Z direction offset
     * @return the corresponding BlockFace, or null if no match
     */
    public static BlockFace fromDirection(int dx, int dy, int dz) {
        for (BlockFace face : values()) {
            if (face.dx == dx && face.dy == dy && face.dz == dz) {
                return face;
            }
        }
        return null;
    }
}
