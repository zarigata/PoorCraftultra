package com.poorcraft.ultra.voxel;

import java.util.Objects;

/**
 * Immutable block metadata describing the textures used for each face.
 */
public final class BlockDefinition {

    private final BlockType type;
    private final int topIndex;
    private final int bottomIndex;
    private final int sideIndex;
    private final int allIndex;

    private BlockDefinition(BlockType type, int topIndex, int bottomIndex, int sideIndex, int allIndex) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.topIndex = topIndex;
        this.bottomIndex = bottomIndex;
        this.sideIndex = sideIndex;
        this.allIndex = allIndex;

        if (type != BlockType.AIR) {
            boolean hasAtlasIndex = topIndex >= 0 || bottomIndex >= 0 || sideIndex >= 0 || allIndex >= 0;
            if (!hasAtlasIndex) {
                throw new IllegalArgumentException("Non-air block definitions require at least one atlas index");
            }
        }
    }

    public static BlockDefinition air() {
        return new BlockDefinition(BlockType.AIR, -1, -1, -1, -1);
    }

    public static BlockDefinition uniform(BlockType type, int atlasIndex) {
        return new BlockDefinition(type, -1, -1, -1, atlasIndex);
    }

    public static BlockDefinition multiface(BlockType type, int topIndex, int bottomIndex, int sideIndex) {
        return new BlockDefinition(type, topIndex, bottomIndex, sideIndex, -1);
    }

    public BlockType getType() {
        return type;
    }

    public short getId() {
        return type.getId();
    }

    public String getName() {
        return type.getName();
    }

    public boolean isSolid() {
        return type.isSolid();
    }

    public boolean isTransparent() {
        return type.isTransparent();
    }

    public int getLightLevel() {
        return type.getLightLevel();
    }

    public boolean hasMultipleFaces() {
        int available = 0;
        if (topIndex >= 0) {
            available++;
        }
        if (bottomIndex >= 0) {
            available++;
        }
        if (sideIndex >= 0) {
            available++;
        }
        if (allIndex >= 0) {
            available++;
        }
        return available > 1;
    }

    public int getAtlasIndex(String faceName) {
        if (faceName == null || faceName.isBlank()) {
            return resolveFallbackIndex();
        }

        return switch (faceName) {
            case "top" -> resolveFace(topIndex);
            case "bottom" -> resolveFace(bottomIndex);
            case "side", "north", "south", "east", "west" -> resolveFace(sideIndex);
            case "all" -> resolveFace(allIndex);
            default -> resolveFallbackIndex();
        };
    }

    private int resolveFace(int candidate) {
        return candidate >= 0 ? candidate : resolveFallbackIndex();
    }

    private int resolveFallbackIndex() {
        if (allIndex >= 0) {
            return allIndex;
        }
        if (sideIndex >= 0) {
            return sideIndex;
        }
        if (topIndex >= 0) {
            return topIndex;
        }
        if (bottomIndex >= 0) {
            return bottomIndex;
        }
        return -1;
    }
}
