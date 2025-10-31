package com.poorcraft.ultra.voxel;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Enumeration of base block types defined for Phase 1.0.
 */
public enum BlockType {
    AIR(0, "air", false, true, 0),
    STONE(1, "stone", true, false, 0),
    DIRT(2, "dirt", true, false, 0),
    GRASS(3, "grass", true, false, 0),
    PLANKS(4, "planks", true, false, 0),
    LOG(5, "log", true, false, 0),
    LEAVES(6, "leaves", true, true, 0),
    SAND(7, "sand", true, false, 0),
    GRAVEL(8, "gravel", true, false, 0),
    GLASS(9, "glass", true, true, 0),
    WATER(10, "water", false, true, 0);

    private static final BlockType[] BY_ID;
    private static final Map<String, BlockType> BY_NAME;

    static {
        int maxId = 0;
        for (BlockType type : values()) {
            maxId = Math.max(maxId, type.id);
        }
        BY_ID = new BlockType[maxId + 1];
        BY_NAME = new HashMap<>();
        for (BlockType type : values()) {
            BY_ID[type.id] = type;
            BY_NAME.put(type.name.toLowerCase(Locale.ROOT), type);
        }
    }

    private final short id;
    private final String name;
    private final boolean solid;
    private final boolean transparent;
    private final int lightLevel;

    BlockType(int id, String name, boolean solid, boolean transparent, int lightLevel) {
        if (id < Short.MIN_VALUE || id > Short.MAX_VALUE) {
            throw new IllegalArgumentException("Block ID out of short range: " + id);
        }
        this.id = (short) id;
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.solid = solid;
        this.transparent = transparent;
        this.lightLevel = lightLevel;
    }

    public short getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isSolid() {
        return solid;
    }

    public boolean isTransparent() {
        return transparent;
    }

    public int getLightLevel() {
        return lightLevel;
    }

    public static BlockType fromId(short id) {
        if (id < 0 || id >= BY_ID.length) {
            return null;
        }
        return BY_ID[id];
    }

    public static BlockType fromName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return BY_NAME.get(name.toLowerCase(Locale.ROOT));
    }
}
