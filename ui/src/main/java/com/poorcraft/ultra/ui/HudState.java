package com.poorcraft.ultra.ui;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;
import com.poorcraft.ultra.shared.Logger;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Label;

public final class HudState extends BaseAppState {

    private static final Logger logger = Logger.getLogger(HudState.class);
    private static final float FPS_UPDATE_INTERVAL = 0.5f;

    private Label fpsLabel;
    private Label positionLabel;
    private Camera cam;
    private float fpsTimer;
    private float lastWidth;
    private float lastHeight;

    @Override
    protected void initialize(Application app) {
        ensureGuiGlobalsInitialized(app);

        cam = app.getCamera();
        SimpleApplication simpleApp = (SimpleApplication) app;

        fpsLabel = new Label("FPS: --");
        fpsLabel.setFontSize(16f);
        fpsLabel.setColor(new ColorRGBA(1f, 1f, 1f, 0.9f));
        fpsLabel.setLocalTranslation(10f, cam.getHeight() - 10f, 0f);
        simpleApp.getGuiNode().attachChild(fpsLabel);

        positionLabel = new Label("X: 0.0 Y: 0.0 Z: 0.0");
        positionLabel.setFontSize(16f);
        positionLabel.setColor(new ColorRGBA(1f, 1f, 1f, 0.9f));
        alignPositionLabel();
        simpleApp.getGuiNode().attachChild(positionLabel);

        lastWidth = cam.getWidth();
        lastHeight = cam.getHeight();

        logger.info("HUD initialized");
    }

    @Override
    protected void cleanup(Application app) {
        if (fpsLabel != null) {
            fpsLabel.removeFromParent();
        }
        if (positionLabel != null) {
            positionLabel.removeFromParent();
        }
        logger.info("HUD cleaned up");
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
        fpsTimer += tpf;
        if (fpsTimer >= FPS_UPDATE_INTERVAL) {
            int fps = (int) getApplication().getTimer().getFrameRate();
            fpsLabel.setText("FPS: " + fps);
            fpsTimer = 0f;
        }

        Vector3f pos = cam.getLocation();
        positionLabel.setText(String.format("X: %.1f Y: %.1f Z: %.1f", pos.x, pos.y, pos.z));
        alignPositionLabel();

        float currentWidth = cam.getWidth();
        float currentHeight = cam.getHeight();
        if (currentWidth != lastWidth || currentHeight != lastHeight) {
            repositionLabels();
            lastWidth = currentWidth;
            lastHeight = currentHeight;
        }
    }

    private void repositionLabels() {
        fpsLabel.setLocalTranslation(10f, cam.getHeight() - 10f, 0f);
        alignPositionLabel();
    }

    private void alignPositionLabel() {
        float padding = 10f;
        float x = cam.getWidth() - positionLabel.getPreferredSize().x - padding;
        float y = cam.getHeight() - padding;
        positionLabel.setLocalTranslation(x, y, 0f);
    }

    private void ensureGuiGlobalsInitialized(Application app) {
        try {
            GuiGlobals.getInstance();
        } catch (IllegalStateException ignored) {
            GuiGlobals.initialize(app);
        }
    }

    private void setCullHint(Spatial.CullHint hint) {
        if (fpsLabel != null) {
            fpsLabel.setCullHint(hint);
        }
        if (positionLabel != null) {
            positionLabel.setCullHint(hint);
        }
    }
}
