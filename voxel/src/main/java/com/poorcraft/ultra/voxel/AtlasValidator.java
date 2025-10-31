package com.poorcraft.ultra.voxel;

import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.poorcraft.ultra.shared.Logger;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class AtlasValidator {

    private static final Logger logger = Logger.getLogger(AtlasValidator.class);

    private static final int EXPECTED_ATLAS_WIDTH = 512;
    private static final int EXPECTED_ATLAS_HEIGHT = 512;
    private static final int EXPECTED_TILE_SIZE = 64;
    private static final int ATLAS_GRID_SIZE = 8;
    private static final int MIN_ATLAS_INDEX = 0;
    private static final int MAX_ATLAS_INDEX = 63;

    private static final Set<String> VALID_FACES = Set.of(
            "all",
            "top",
            "bottom",
            "side",
            "north",
            "south",
            "east",
            "west"
    );

    private AtlasValidator() {
        throw new UnsupportedOperationException("Utility class, do not instantiate");
    }

    public static void validate(Texture2D atlasTexture, Map<String, Map<String, Integer>> atlasMapping) {
        validateAtlasDimensions(atlasTexture);
        validateTileSize();
        validateAtlasIndices(atlasMapping);
        validateBlockMapping(atlasMapping);

        int blockCount = atlasMapping.size();
        int indexCount = atlasMapping.values().stream().mapToInt(Map::size).sum();
        logger.info("Atlas validation passed: {}x{}, {} blocks, {} indices",
                EXPECTED_ATLAS_WIDTH, EXPECTED_ATLAS_HEIGHT, blockCount, indexCount);
    }

    public static void validateAtlasDimensions(Texture2D atlasTexture) {
        if (atlasTexture == null) {
            throw new IllegalArgumentException("Atlas texture must not be null");
        }

        Image image = atlasTexture.getImage();
        if (image == null) {
            throw new IllegalArgumentException("Atlas texture image data is missing");
        }

        int width = image.getWidth();
        int height = image.getHeight();

        if (width != EXPECTED_ATLAS_WIDTH) {
            throw new IllegalArgumentException("Atlas width must be " + EXPECTED_ATLAS_WIDTH + ", got: " + width);
        }
        if (height != EXPECTED_ATLAS_HEIGHT) {
            throw new IllegalArgumentException("Atlas height must be " + EXPECTED_ATLAS_HEIGHT + ", got: " + height);
        }

        logger.info("Atlas dimensions validated: {}x{}", width, height);
    }

    public static void validateTileSize() {
        int derivedTileSize = EXPECTED_ATLAS_WIDTH / ATLAS_GRID_SIZE;
        if (derivedTileSize != EXPECTED_TILE_SIZE) {
            throw new IllegalArgumentException("Tile size must be " + EXPECTED_TILE_SIZE + ", got: " + derivedTileSize);
        }
        logger.info("Tile size validated: {}x{}", EXPECTED_TILE_SIZE, EXPECTED_TILE_SIZE);
    }

    public static void validateAtlasIndices(Map<String, Map<String, Integer>> atlasMapping) {
        Objects.requireNonNull(atlasMapping, "Atlas mapping must not be null");
        if (atlasMapping.isEmpty()) {
            throw new IllegalArgumentException("Atlas mapping must not be empty");
        }

        Set<Integer> indices = new HashSet<>();
        for (Map.Entry<String, Map<String, Integer>> entry : atlasMapping.entrySet()) {
            String block = entry.getKey();
            Map<String, Integer> faces = entry.getValue();
            if (faces == null || faces.isEmpty()) {
                throw new IllegalArgumentException("Block " + block + " has no face mappings");
            }
            for (Map.Entry<String, Integer> faceEntry : faces.entrySet()) {
                Integer index = faceEntry.getValue();
                if (index == null) {
                    throw new IllegalArgumentException("Face " + faceEntry.getKey() + " for block " + block + " has null index");
                }
                if (index < MIN_ATLAS_INDEX || index > MAX_ATLAS_INDEX) {
                    throw new IllegalArgumentException("Atlas index out of range: " + index + " for block " + block);
                }
                if (!indices.add(index)) {
                    throw new IllegalArgumentException("Duplicate atlas index detected: " + index);
                }
            }
        }

        logger.info("Atlas indices validated: {} unique indices in range [{}-{}]",
                indices.size(), MIN_ATLAS_INDEX, MAX_ATLAS_INDEX);
    }

    public static void validateBlockMapping(Map<String, Map<String, Integer>> atlasMapping) {
        for (Map.Entry<String, Map<String, Integer>> entry : atlasMapping.entrySet()) {
            String block = entry.getKey();
            Map<String, Integer> faces = entry.getValue();
            if (faces == null || faces.isEmpty()) {
                throw new IllegalArgumentException("Block " + block + " has no face mappings");
            }

            for (String face : faces.keySet()) {
                if (!VALID_FACES.contains(face)) {
                    throw new IllegalArgumentException("Invalid face name: " + face + " for block " + block);
                }
            }
        }

        logger.info("Block mappings validated: {} blocks", atlasMapping.size());
    }
}
