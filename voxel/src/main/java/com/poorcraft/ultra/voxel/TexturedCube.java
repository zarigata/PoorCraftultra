package com.poorcraft.ultra.voxel;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Box;
import com.jme3.util.BufferUtils;
import com.poorcraft.ultra.shared.Logger;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Label;

import java.nio.FloatBuffer;

public final class TexturedCube extends BaseAppState {

    private static final Logger logger = Logger.getLogger(TexturedCube.class);

    private static final float CUBE_SIZE = 1.0f;
    private static final float ROTATION_SPEED = FastMath.TWO_PI / 60.0f;
    private static final Vector3f CUBE_POSITION = new Vector3f(0f, 0f, -5f);
    private static final String LABEL_TEXT = "Poorcraft Ultra v0.4";

    private Geometry cubeGeometry;
    private Label versionLabel;
    private float rotationAngle;
    private TextureAtlas atlas;
    private int lastViewportWidth = -1;
    private int lastViewportHeight = -1;

    @Override
    protected void initialize(Application app) {
        ensureGuiGlobalsInitialized(app);
        SimpleApplication simpleApplication = (SimpleApplication) app;
        AssetManager assetManager = app.getAssetManager();

        try {
            atlas = TextureAtlas.load(assetManager);
        } catch (RuntimeException exception) {
            logger.error("Failed to load texture atlas for TexturedCube", exception);
            setEnabled(false);
            return;
        }

        Box cubeMesh = new Box(CUBE_SIZE / 2f, CUBE_SIZE / 2f, CUBE_SIZE / 2f);
        applyStoneUVs(cubeMesh);

        cubeGeometry = new Geometry("TexturedCube", cubeMesh);
        Material material = BlockMaterial.create(assetManager, atlas);
        cubeGeometry.setMaterial(material);
        cubeGeometry.setLocalTranslation(CUBE_POSITION);
        simpleApplication.getRootNode().attachChild(cubeGeometry);

        versionLabel = new Label(LABEL_TEXT);
        versionLabel.setFontSize(20f);
        versionLabel.setColor(new ColorRGBA(1f, 1f, 1f, 0.9f));
        centerLabel(simpleApplication);
        updateCachedViewportSize(simpleApplication);
        simpleApplication.getGuiNode().attachChild(versionLabel);

        rotationAngle = 0f;
        logger.info("Textured cube initialized (stone texture, 1 RPM rotation)");
    }

    @Override
    protected void cleanup(Application app) {
        if (cubeGeometry != null) {
            cubeGeometry.removeFromParent();
            cubeGeometry = null;
        }
        if (versionLabel != null) {
            versionLabel.removeFromParent();
            versionLabel = null;
        }
        atlas = null;
        logger.info("Textured cube cleaned up");
    }

    @Override
    protected void onEnable() {
        setCullHint(Spatial.CullHint.Never);
    }

    @Override
    protected void onDisable() {
        setCullHint(Spatial.CullHint.Always);
    }

    @Override
    public void update(float tpf) {
        if (cubeGeometry == null) {
            return;
        }

        SimpleApplication application = (SimpleApplication) getApplication();
        if (application != null && updateCachedViewportSize(application)) {
            centerLabel(application);
        }

        rotationAngle += ROTATION_SPEED * tpf;
        if (rotationAngle > FastMath.TWO_PI) {
            rotationAngle -= FastMath.TWO_PI;
        }

        Quaternion rotation = new Quaternion().fromAngleAxis(rotationAngle, Vector3f.UNIT_Y);
        cubeGeometry.setLocalRotation(rotation);
    }

    private void setCullHint(Spatial.CullHint hint) {
        if (cubeGeometry != null) {
            cubeGeometry.setCullHint(hint);
        }
        if (versionLabel != null) {
            versionLabel.setCullHint(hint);
        }
    }

    private void applyStoneUVs(Box box) {
        int stoneIndex = atlas.getAtlasIndex("stone");
        float[] uv = atlas.getUVCoordinates(stoneIndex);
        float u0 = uv[0];
        float v0 = uv[1];
        float u1 = uv[2];
        float v1 = uv[3];

        FloatBuffer original = box.getFloatBuffer(VertexBuffer.Type.TexCoord);
        if (original == null) {
            return;
        }

        // Preserve jME Box vertex ordering by scaling the default UVs into the atlas tile.
        FloatBuffer remapped = BufferUtils.createFloatBuffer(original.limit());
        original.rewind();
        float uRange = u1 - u0;
        float vRange = v1 - v0;
        while (original.hasRemaining()) {
            float baseU = original.get();
            float baseV = original.get();
            remapped.put(u0 + baseU * uRange).put(v0 + baseV * vRange);
        }
        remapped.flip();
        box.setBuffer(VertexBuffer.Type.TexCoord, 2, remapped);
    }

    private void centerLabel(SimpleApplication app) {
        if (versionLabel == null) {
            return;
        }
        float width = app.getCamera().getWidth();
        float textWidth = versionLabel.getPreferredSize().x;
        float x = (width - textWidth) / 2f;
        float y = 40f;
        versionLabel.setLocalTranslation(x, y, 0f);
    }

    private boolean updateCachedViewportSize(SimpleApplication app) {
        int width = app.getCamera().getWidth();
        int height = app.getCamera().getHeight();
        if (width != lastViewportWidth || height != lastViewportHeight) {
            lastViewportWidth = width;
            lastViewportHeight = height;
            return true;
        }
        return false;
    }

    private void ensureGuiGlobalsInitialized(Application app) {
        try {
            GuiGlobals.getInstance();
        } catch (IllegalStateException ignored) {
            GuiGlobals.initialize(app);
        }
    }
}
