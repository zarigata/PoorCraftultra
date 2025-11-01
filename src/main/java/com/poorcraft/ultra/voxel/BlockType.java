package com.poorcraft.ultra.voxel;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Phase 1 block definitions. Each block uses a single texture without rotation or transparency.
 */
public enum BlockType {
    AIR((byte) 0, "air", false, null),
    STONE((byte) 1, "stone", true, "stone_granite"),
    DIRT((byte) 2, "dirt", true, "dirt"),
    GRASS((byte) 3, "grass", true, "grass_top"),
    WOOD_OAK((byte) 4, "wood_oak", true, "wood_oak"),
    LEAVES_OAK((byte) 5, "leaves_oak", true, "leaves_oak");

    private static final Map<Byte, BlockType> TYPES_BY_ID = Arrays.stream(values())
        .collect(Collectors.toUnmodifiableMap(BlockType::id, Function.identity()));

    private final byte id;
    private final String name;
    private final boolean opaque;
    private final String textureName;

    BlockType(byte id, String name, boolean opaque, String textureName) {
        this.id = id;
        this.name = name;
        this.opaque = opaque;
        this.textureName = textureName;
    }

    public byte id() {
        return id;
    }

    public String displayName() {
        return name;
    }

    public boolean opaque() {
        return opaque;
    }

    public String textureName() {
        return textureName;
    }

    public static BlockType fromId(byte id) {
        return TYPES_BY_ID.getOrDefault(id, AIR);
    }
}
