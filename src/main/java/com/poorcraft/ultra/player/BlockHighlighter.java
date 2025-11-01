package com.poorcraft.ultra.player;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.debug.WireBox;

public class BlockHighlighter {
    private Geometry geometry;
    private boolean visible;

    public void init(Node rootNode, AssetManager assetManager) {
        WireBox wireBox = new WireBox(0.5f, 0.5f, 0.5f);
        geometry = new Geometry("block-highlight", wireBox);
        geometry.setCullHint(Geometry.CullHint.Always);

        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.getAdditionalRenderState().setWireframe(true);
        material.setColor("Color", ColorRGBA.White);
        geometry.setMaterial(material);

        rootNode.attachChild(geometry);
    }

    public void update(BlockPicker.BlockPickResult result) {
        if (result == null) {
            hide();
            return;
        }
        show();
        geometry.setLocalTranslation(new Vector3f(result.blockX() + 0.5f,
            result.blockY() + 0.5f, result.blockZ() + 0.5f));
    }

    private void show() {
        if (!visible) {
            geometry.setCullHint(Geometry.CullHint.Never);
            visible = true;
        }
    }

    private void hide() {
        if (visible) {
            geometry.setCullHint(Geometry.CullHint.Always);
            visible = false;
        }
    }
}
