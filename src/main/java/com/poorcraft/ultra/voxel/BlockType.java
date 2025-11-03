package com.poorcraft.ultra.voxel;

import com.poorcraft.ultra.player.ToolType;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Phase 2 block definitions with biome variants and crafting materials.
 */
public enum BlockType {
    AIR((byte) 0, "air", false, null, 0f, ToolType.NONE),
    STONE((byte) 1, "stone", true, "stone_granite", 2.5f, ToolType.NONE),
    DIRT((byte) 2, "dirt", true, "dirt", 0.6f, ToolType.NONE),
    GRASS((byte) 3, "grass", true, "grass_top", 0.6f, ToolType.NONE),
    WOOD_OAK((byte) 4, "wood_oak", true, "wood_oak", 1.5f, ToolType.WOOD),
    LEAVES_OAK((byte) 5, "leaves_oak", true, "leaves_oak", 0.2f, ToolType.NONE),
    SAND((byte) 6, "sand", true, "sand", 0.5f, ToolType.NONE),
    GRAVEL((byte) 7, "gravel", true, "gravel", 0.8f, ToolType.NONE),
    COAL_ORE((byte) 8, "coal_ore", true, "ore_coal", 3.0f, ToolType.STONE),
    IRON_ORE((byte) 9, "iron_ore", true, "ore_iron", 4.0f, ToolType.STONE),
    GOLD_ORE((byte) 10, "gold_ore", true, "ore_gold", 4.0f, ToolType.STONE),
    PLANKS((byte) 11, "planks", true, "planks_oak", 1.2f, ToolType.WOOD),
    TORCH((byte) 12, "torch", false, "torch", 0.1f, ToolType.NONE),
    CHEST((byte) 13, "chest", true, "chest", 2.5f, ToolType.WOOD),
    GRASS_PLAINS((byte) 14, "grass_plains", true, "grass_plains", 0.6f, ToolType.NONE),
    GRASS_FOREST((byte) 15, "grass_forest", true, "grass_forest", 0.6f, ToolType.NONE),
    GRASS_DESERT((byte) 16, "grass_desert", true, "grass_desert", 0.6f, ToolType.NONE);

    private static final Map<Byte, BlockType> TYPES_BY_ID = Arrays.stream(values())
        .collect(Collectors.toUnmodifiableMap(BlockType::id, Function.identity()));

    private final byte id;
    private final String name;
    private final boolean opaque;
    private final String textureName;
    private final float hardness;
    private final ToolType requiredTool;

    BlockType(byte id, String name, boolean opaque, String textureName, float hardness, ToolType requiredTool) {
        this.id = id;
        this.name = name;
        this.opaque = opaque;
        this.textureName = textureName;
        this.hardness = hardness;
        this.requiredTool = requiredTool;
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

    public float hardness() {
        return hardness;
    }

    public ToolType requiredTool() {
        return requiredTool;
    }

    public static BlockType fromId(byte id) {
        return TYPES_BY_ID.getOrDefault(id, AIR);
    }
}
