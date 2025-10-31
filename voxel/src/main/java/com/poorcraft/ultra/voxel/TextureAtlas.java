package com.poorcraft.ultra.voxel;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.poorcraft.ultra.shared.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public final class TextureAtlas {

    private static final Logger logger = Logger.getLogger(TextureAtlas.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String ATLAS_PATH = "textures/blocks_atlas.png";
    private static final String MAPPING_PATH = "assets/textures/blocks_atlas.json";
    private static final int EXPECTED_ATLAS_SIZE = 512;
    private static final int EXPECTED_TILE_SIZE = 64;
    private static final int ATLAS_GRID_SIZE = 8;

    private static TextureAtlas INSTANCE;

    private final Texture2D atlasTexture;
    private final Map<String, Map<String, Integer>> atlasMapping;
    private final boolean initialized;

    private TextureAtlas(Texture2D atlasTexture, Map<String, Map<String, Integer>> atlasMapping) {
        this.atlasTexture = atlasTexture;
        this.atlasMapping = atlasMapping;
        this.initialized = true;
    }

    public static TextureAtlas getInstance() {
        if (INSTANCE == null || !INSTANCE.initialized) {
            throw new IllegalStateException("TextureAtlas not initialized. Call load() first.");
        }
        return INSTANCE;
    }

    public static synchronized TextureAtlas load(AssetManager assetManager) {
        Objects.requireNonNull(assetManager, "assetManager must not be null");

        if (INSTANCE != null && INSTANCE.initialized) {
            logger.warn("Texture atlas already initialized; returning existing instance");
            return INSTANCE;
        }

        try {
            assetManager.registerLocator("assets", FileLocator.class);

            Texture2D texture = (Texture2D) assetManager.loadTexture(ATLAS_PATH);
            if (texture == null) {
                throw new RuntimeException("Atlas texture not found at: " + ATLAS_PATH + ". Run :tools:generateAssets first.");
            }

            texture.setMagFilter(Texture.MagFilter.Nearest);
            texture.setMinFilter(Texture.MinFilter.Trilinear);
            texture.setWrap(Texture.WrapMode.Repeat);

            Image image = texture.getImage();
            if (image == null) {
                throw new RuntimeException("Atlas texture image data is missing for: " + ATLAS_PATH);
            }

            if (image.getWidth() != EXPECTED_ATLAS_SIZE || image.getHeight() != EXPECTED_ATLAS_SIZE) {
                logger.warn("Atlas texture dimensions do not match expected size ({}x{}); actual: {}x{}",
                        EXPECTED_ATLAS_SIZE, EXPECTED_ATLAS_SIZE, image.getWidth(), image.getHeight());
            }

            String jsonContent;
            try {
                jsonContent = Files.readString(Path.of(MAPPING_PATH));
            } catch (IOException ioException) {
                throw new RuntimeException("Failed to load texture atlas mapping from " + MAPPING_PATH, ioException);
            }

            Map<String, Map<String, Integer>> rawMapping;
            try {
                rawMapping = OBJECT_MAPPER.readValue(jsonContent, new TypeReference<>() {
                });
            } catch (IOException ioException) {
                throw new RuntimeException("Failed to parse atlas mapping JSON", ioException);
            }

            AtlasValidator.validate(texture, rawMapping);

            Map<String, Map<String, Integer>> immutableMapping = rawMapping.entrySet().stream()
                    .collect(Collectors.toUnmodifiableMap(
                            Map.Entry::getKey,
                            entry -> Collections.unmodifiableMap(new HashMap<>(entry.getValue()))
                    ));

            INSTANCE = new TextureAtlas(texture, immutableMapping);

            int blockCount = immutableMapping.size();
            int faceCount = immutableMapping.values().stream().mapToInt(Map::size).sum();
            logger.info("Texture atlas loaded successfully ({} blocks, {} total faces)", blockCount, faceCount);
            return INSTANCE;
        } catch (RuntimeException exception) {
            logger.error("Failed to load texture atlas", exception);
            throw exception;
        }
    }

    public static boolean isInitialized() {
        return INSTANCE != null && INSTANCE.initialized;
    }

    public Texture2D getTexture() {
        return atlasTexture;
    }

    public int getAtlasIndex(String blockName) {
        return getAtlasIndex(blockName, "all");
    }

    public int getAtlasIndex(String blockName, String faceName) {
        if (blockName == null || blockName.isBlank()) {
            logger.warn("Requested atlas index for null or blank block; defaulting to stone (index 0)");
            return 0;
        }

        Map<String, Integer> faces = atlasMapping.get(blockName);
        if (faces == null || faces.isEmpty()) {
            logger.warn("Block '{}' not found in atlas mapping; defaulting to stone (index 0)", blockName);
            return 0;
        }

        if (faceName != null && !faceName.isBlank()) {
            Integer index = faces.get(faceName);
            if (index != null) {
                return index;
            }
        }

        Integer allIndex = faces.get("all");
        if (allIndex != null) {
            return allIndex;
        }

        if (!faces.isEmpty()) {
            return faces.values().iterator().next();
        }

        logger.warn("Block '{}' has no usable face mappings; defaulting to stone (index 0)", blockName);
        return 0;
    }

    public float[] getUVCoordinates(int atlasIndex) {
        int clampedIndex = Math.max(0, Math.min(atlasIndex, ATLAS_GRID_SIZE * ATLAS_GRID_SIZE - 1));
        int column = clampedIndex % ATLAS_GRID_SIZE;
        int row = clampedIndex / ATLAS_GRID_SIZE;

        float tileSize = 1f / ATLAS_GRID_SIZE;
        float u0 = column * tileSize;
        float v1 = 1f - row * tileSize;
        float u1 = u0 + tileSize;
        float v0 = v1 - tileSize;
        return new float[]{u0, v0, u1, v1};
    }

    public Set<String> getAllBlockNames() {
        return Collections.unmodifiableSet(new TreeSet<>(atlasMapping.keySet()));
    }

    public Map<String, Map<String, Integer>> getMapping() {
        return atlasMapping;
    }

    public static int getExpectedAtlasSize() {
        return EXPECTED_ATLAS_SIZE;
    }

    public static int getExpectedTileSize() {
        return EXPECTED_TILE_SIZE;
    }

    public static int getAtlasGridSize() {
        return ATLAS_GRID_SIZE;
    }
}
