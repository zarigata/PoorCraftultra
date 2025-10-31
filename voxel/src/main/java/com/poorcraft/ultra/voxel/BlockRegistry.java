package com.poorcraft.ultra.voxel;

import com.poorcraft.ultra.shared.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Singleton registry providing validated access to block definitions.
 */
public final class BlockRegistry {

    private static final Logger logger = Logger.getLogger(BlockRegistry.class);
    private static volatile BlockRegistry INSTANCE;

    private final Map<Short, BlockDefinition> byId;
    private final Map<String, BlockDefinition> byName;
    private final int blockCount;
    private final boolean initialized;

    private BlockRegistry(Map<Short, BlockDefinition> byId, Map<String, BlockDefinition> byName) {
        this.byId = Collections.unmodifiableMap(byId);
        this.byName = Collections.unmodifiableMap(byName);
        this.blockCount = byId.size();
        this.initialized = true;
    }

    public static BlockRegistry getInstance() {
        BlockRegistry snapshot = INSTANCE;
        if (snapshot == null || !snapshot.initialized) {
            throw new IllegalStateException("BlockRegistry not initialized. Call load() first.");
        }
        return snapshot;
    }

    public static synchronized BlockRegistry load(TextureAtlas atlas) {
        Objects.requireNonNull(atlas, "atlas must not be null");

        if (!TextureAtlas.isInitialized()) {
            throw new IllegalStateException("TextureAtlas must be initialized before BlockRegistry.load()");
        }

        if (INSTANCE != null && INSTANCE.initialized) {
            logger.warn("BlockRegistry already initialized; returning existing instance");
            return INSTANCE;
        }

        List<BlockDefinition> definitions = buildDefinitions(atlas);
        validateDefinitions(definitions, atlas);

        Map<Short, BlockDefinition> byId = new HashMap<>();
        Map<String, BlockDefinition> byName = new HashMap<>();
        for (BlockDefinition definition : definitions) {
            byId.put(definition.getId(), definition);
            byName.put(definition.getName().toLowerCase(Locale.ROOT), definition);
        }

        INSTANCE = new BlockRegistry(byId, byName);
        logger.info("Block registry loaded successfully ({} blocks)", INSTANCE.blockCount);
        return INSTANCE;
    }

    private static List<BlockDefinition> buildDefinitions(TextureAtlas atlas) {
        List<BlockDefinition> definitions = new ArrayList<>();

        definitions.add(BlockDefinition.air());
        definitions.add(BlockDefinition.uniform(BlockType.STONE, requireAtlasIndex(atlas, "stone", "all")));
        definitions.add(BlockDefinition.uniform(BlockType.DIRT, requireAtlasIndex(atlas, "dirt", "all")));
        definitions.add(BlockDefinition.multiface(BlockType.GRASS,
                requireAtlasIndex(atlas, "grass", "top"),
                requireAtlasIndex(atlas, "grass", "bottom"),
                requireAtlasIndex(atlas, "grass", "side")));
        definitions.add(BlockDefinition.uniform(BlockType.PLANKS, requireAtlasIndex(atlas, "planks", "all")));
        definitions.add(BlockDefinition.multiface(BlockType.LOG,
                requireAtlasIndex(atlas, "log", "top"),
                requireAtlasIndex(atlas, "log", "top"),
                requireAtlasIndex(atlas, "log", "side")));
        definitions.add(BlockDefinition.uniform(BlockType.LEAVES, requireAtlasIndex(atlas, "leaves", "all")));
        definitions.add(BlockDefinition.uniform(BlockType.SAND, requireAtlasIndex(atlas, "sand", "all")));
        definitions.add(BlockDefinition.uniform(BlockType.GRAVEL, requireAtlasIndex(atlas, "gravel", "all")));
        definitions.add(BlockDefinition.uniform(BlockType.GLASS, requireAtlasIndex(atlas, "glass", "all")));
        definitions.add(BlockDefinition.uniform(BlockType.WATER, requireAtlasIndex(atlas, "water", "all")));

        return definitions;
    }

    private static int requireAtlasIndex(TextureAtlas atlas, String blockName, String face) {
        Map<String, Map<String, Integer>> mapping = atlas.getMapping();
        Map<String, Integer> faces = mapping.get(blockName);
        if (faces == null || faces.isEmpty()) {
            throw new IllegalArgumentException("Atlas mapping missing block: " + blockName);
        }
        Integer index;
        if (face == null || face.isBlank()) {
            index = faces.get("all");
        } else {
            index = faces.get(face);
            if (index == null) {
                index = faces.get("all");
            }
        }
        if (index == null) {
            throw new IllegalArgumentException("Atlas mapping missing face '" + face + "' for block: " + blockName);
        }
        return index;
    }

    private static void validateDefinitions(List<BlockDefinition> definitions, TextureAtlas atlas) {
        int minIndex = 0;
        int maxIndex = TextureAtlas.getAtlasGridSize() * TextureAtlas.getAtlasGridSize() - 1;
        Map<String, Map<String, Integer>> mapping = atlas.getMapping();

        for (BlockDefinition definition : definitions) {
            if (definition.getType() == BlockType.AIR) {
                continue;
            }

            Map<String, Integer> faces = mapping.get(definition.getName());
            if (faces == null || faces.isEmpty()) {
                throw new IllegalArgumentException("Block \"" + definition.getName() + "\" lacks atlas faces");
            }

            int[] indices = collectIndices(definition);
            boolean hasValidIndex = false;
            for (int index : indices) {
                if (index >= 0) {
                    hasValidIndex = true;
                    if (index < minIndex || index > maxIndex) {
                        throw new IllegalArgumentException("Atlas index out of range for block \"" + definition.getName() + "\": " + index);
                    }
                }
            }
            if (!hasValidIndex) {
                throw new IllegalArgumentException("Block \"" + definition.getName() + "\" has no valid atlas indices");
            }
        }

        logger.info("Atlas references validated for {} blocks", definitions.size());
    }

    private static int[] collectIndices(BlockDefinition definition) {
        return new int[]{
                definition.getAtlasIndex("top"),
                definition.getAtlasIndex("bottom"),
                definition.getAtlasIndex("side"),
                definition.getAtlasIndex("all")
        };
    }

    public static boolean isInitialized() {
        BlockRegistry snapshot = INSTANCE;
        return snapshot != null && snapshot.initialized;
    }

    public BlockDefinition getDefinition(short id) {
        BlockDefinition definition = byId.get(id);
        if (definition == null) {
            return byId.get((short) 0);
        }
        return definition;
    }

    public BlockDefinition getDefinition(String name) {
        if (name == null || name.isBlank()) {
            return byId.get((short) 0);
        }
        BlockDefinition definition = byName.get(name.toLowerCase(Locale.ROOT));
        if (definition == null) {
            return byId.get((short) 0);
        }
        return definition;
    }

    public BlockDefinition getDefinition(BlockType type) {
        Objects.requireNonNull(type, "type must not be null");
        return getDefinition(type.getId());
    }

    public int getBlockCount() {
        return blockCount;
    }

    public Map<Short, BlockDefinition> getDefinitionsById() {
        return byId;
    }

    public Map<String, BlockDefinition> getDefinitionsByName() {
        return byName;
    }
}
