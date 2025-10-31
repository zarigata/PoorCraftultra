package com.poorcraft.ultra.voxel;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;

public final class BlockMaterial {

    private BlockMaterial() {
        throw new UnsupportedOperationException("Factory class, do not instantiate");
    }

    public static Material create(AssetManager assetManager, TextureAtlas atlas) {
        if (assetManager == null) {
            throw new IllegalArgumentException("assetManager must not be null");
        }
        if (atlas == null) {
            throw new IllegalArgumentException("atlas must not be null");
        }
        return createInternal(assetManager, atlas.getTexture());
    }

    public static Material create(AssetManager assetManager, Texture2D texture) {
        if (assetManager == null) {
            throw new IllegalArgumentException("assetManager must not be null");
        }
        if (texture == null) {
            throw new IllegalArgumentException("texture must not be null");
        }
        return createInternal(assetManager, texture);
    }

    public static Material createWithColor(AssetManager assetManager, TextureAtlas atlas, com.jme3.math.ColorRGBA color) {
        if (atlas == null) {
            throw new IllegalArgumentException("atlas must not be null");
        }
        Material material = create(assetManager, atlas);
        material.setBoolean("UseMaterialColors", true);
        material.setColor("Color", color);
        return material;
    }

    private static Material createInternal(AssetManager assetManager, Texture2D texture) {
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setTexture("ColorMap", texture);

        texture.setMagFilter(Texture.MagFilter.Nearest);
        texture.setMinFilter(Texture.MinFilter.Trilinear);

        return material;
    }
}
