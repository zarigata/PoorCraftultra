++ Added
package com.poorcraft.ultra.engine;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;

import java.util.Objects;

public final class EngineMaterials {

    private EngineMaterials() {
    }

    public static Material createBasicMaterial(AssetManager assetManager, ColorRGBA color) {
        Objects.requireNonNull(assetManager, "assetManager");
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", color != null ? color : ColorRGBA.White);
        return material;
    }
}

package com.poorcraft.ultra.engine;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;

public final class EngineMaterials {

    private EngineMaterials() {
    }

    public static Material createBasicMaterial(AssetManager assetManager, ColorRGBA color) {
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", color);
        return material;
    }
}
