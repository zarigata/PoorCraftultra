package com.poorcraft.ultra.voxel;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture;
import java.util.EnumMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads and caches per-block materials for rendering.
 */
public class BlockRegistry {
    private static final Logger logger = LoggerFactory.getLogger(BlockRegistry.class);

    private final Map<BlockType, Material> materialCache = new EnumMap<>(BlockType.class);

    public void init(AssetManager assetManager) {
        for (BlockType type : BlockType.values()) {
            if (type == BlockType.AIR) {
                continue;
            }

            try {
                Texture texture = assetManager.loadTexture("blocks/" + type.textureName() + ".png");
                texture.setWrap(Texture.WrapMode.Repeat);

                Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
                material.setTexture("ColorMap", texture);
                if (type == BlockType.LEAVES_OAK || type == BlockType.TORCH) {
                    material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
                    material.setTransparent(true);
                }
                materialCache.put(type, material);
                logger.debug("Loaded material for block type {}", type.name());
            } catch (RuntimeException ex) {
                logger.warn("Failed to load texture for block type {} (using fallback)", type.name(), ex);
                Material fallback = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
                fallback.setColor("Color", ColorRGBA.Magenta);
                materialCache.put(type, fallback);
            }
        }
    }

    public Material getMaterial(BlockType type) {
        if (type == BlockType.AIR) {
            return null;
        }
        return materialCache.get(type);
    }

    public BlockType getBlockType(byte id) {
        return BlockType.fromId(id);
    }
}
