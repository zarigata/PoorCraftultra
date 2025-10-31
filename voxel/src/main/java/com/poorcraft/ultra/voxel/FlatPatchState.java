package com.poorcraft.ultra.voxel;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsRayTestResult;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.poorcraft.ultra.shared.Logger;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Label;

import java.util.ArrayList;
import java.util.List;

public final class FlatPatchState extends BaseAppState {

    private static final Logger logger = Logger.getLogger(FlatPatchState.class);

    private static final int PATCH_WIDTH = 32;
    private static final int PATCH_DEPTH = 32;
    private static final int PATCH_HEIGHT = 1;
    private static final float PATCH_Y_POSITION = 0f;

    private static final Vector3f RAY_START = new Vector3f(16f, 10f, 16f);
    private static final Vector3f RAY_END = new Vector3f(16f, -10f, 16f);

    private Geometry patchGeometry;
    private RigidBodyControl physicsControl;
    private PhysicsSpace physicsSpace;
    private TextureAtlas atlas;
    private Label versionLabel;
    private Vector3f lastRayHitLocation;

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
            logger.error("Failed to load texture atlas for FlatPatchState", exception);
            setEnabled(false);
            return;
        }

        Mesh patchMesh = ChunkMesh.generateFlatPatch(PATCH_WIDTH, PATCH_DEPTH, atlas);
        patchGeometry = new Geometry("FlatPatch", patchMesh);
        patchGeometry.setMaterial(BlockMaterial.create(assetManager, atlas));
        patchGeometry.setLocalTranslation(0f, PATCH_Y_POSITION, 0f);
        simpleApplication.getRootNode().attachChild(patchGeometry);

        physicsSpace = resolvePhysicsSpace(simpleApplication);
        CollisionShape shape = CollisionShapeFactory.createMeshShape(patchGeometry);
        physicsControl = new RigidBodyControl(shape, 0f);
        patchGeometry.addControl(physicsControl);
        physicsSpace.add(physicsControl);

        versionLabel = new Label("Poorcraft Ultra v1.0");
        versionLabel.setFontSize(20f);
        versionLabel.setColor(new ColorRGBA(1f, 1f, 1f, 0.9f));
        centerLabel(simpleApplication);
        updateCachedViewportSize(simpleApplication);
        simpleApplication.getGuiNode().attachChild(versionLabel);

        verifyCollision(physicsSpace);

        logger.info("Flat patch initialized ({}x{}x{}, greedy meshed, physics enabled)",
                PATCH_WIDTH, PATCH_HEIGHT, PATCH_DEPTH);
    }

    @Override
    protected void cleanup(Application app) {
        if (physicsControl != null && physicsSpace != null) {
            physicsSpace.remove(physicsControl);
            physicsControl = null;
        }

        if (patchGeometry != null) {
            patchGeometry.removeFromParent();
            patchGeometry = null;
        }

        if (versionLabel != null) {
            versionLabel.removeFromParent();
            versionLabel = null;
        }

        atlas = null;
        physicsSpace = null;
        logger.info("Flat patch cleaned up");
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
        SimpleApplication application = (SimpleApplication) getApplication();
        if (application != null && updateCachedViewportSize(application)) {
            centerLabel(application);
        }
    }

    private void setCullHint(Spatial.CullHint hint) {
        if (patchGeometry != null) {
            patchGeometry.setCullHint(hint);
        }
        if (versionLabel != null) {
            versionLabel.setCullHint(hint);
        }
    }

    private void verifyCollision(PhysicsSpace space) {
        List<PhysicsRayTestResult> results = new ArrayList<>();
        space.rayTest(RAY_START, RAY_END, results);
        if (results.isEmpty()) {
            logger.warn("Collision verification failed: ray did not hit patch");
            lastRayHitLocation = null;
            return;
        }
        PhysicsRayTestResult closestResult = null;
        float closestFraction = Float.POSITIVE_INFINITY;
        for (PhysicsRayTestResult result : results) {
            float fraction = result.getHitFraction();
            if (fraction < closestFraction) {
                closestFraction = fraction;
                closestResult = result;
            }
        }

        if (closestResult == null || !Float.isFinite(closestFraction)) {
            logger.warn("Collision verification: ray returned invalid results");
            lastRayHitLocation = null;
            return;
        }

        Vector3f direction = RAY_END.subtract(RAY_START);
        lastRayHitLocation = RAY_START.add(direction.mult(closestFraction));
        logger.info("Collision verification: ray hit patch at {} (fraction {} between Y={} and Y={}, shape={})",
                lastRayHitLocation, closestFraction, RAY_START.y, RAY_END.y, closestResult.getCollisionObject());
    }

    public Vector3f getLastRayHitLocation() {
        return lastRayHitLocation == null ? null : lastRayHitLocation.clone();
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

    private PhysicsSpace resolvePhysicsSpace(SimpleApplication app) {
        BulletAppState bulletState = app.getStateManager().getState(BulletAppState.class);
        if (bulletState == null) {
            throw new IllegalStateException("BulletAppState not attached; physics unavailable");
        }
        return bulletState.getPhysicsSpace();
    }

    private void ensureGuiGlobalsInitialized(Application app) {
        try {
            GuiGlobals.getInstance();
        } catch (IllegalStateException ignored) {
            GuiGlobals.initialize(app);
        }
    }
}
