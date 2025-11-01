package com.poorcraft.ultra.ui;

import com.jme3.post.SceneProcessor;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.texture.FrameBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SceneProcessor that scales the GUI node proportionally on window resize.
 * Reference resolution: 1280x720
 */
public class UIScaleProcessor implements SceneProcessor {
    private static final Logger logger = LoggerFactory.getLogger(UIScaleProcessor.class);

    private static final float REFERENCE_WIDTH = 1280f;
    private static final float REFERENCE_HEIGHT = 720f;

    private final Node guiNode;
    private boolean initialized = false;

    public UIScaleProcessor(Node guiNode) {
        this.guiNode = guiNode;
    }

    @Override
    public void initialize(RenderManager rm, ViewPort vp) {
        initialized = true;
        int w = vp.getCamera().getWidth();
        int h = vp.getCamera().getHeight();
        applyScale(w, h);
        logger.info("UIScaleProcessor initialized at {}x{}", w, h);
    }

    @Override
    public void reshape(ViewPort vp, int w, int h) {
        applyScale(w, h);
        logger.debug("UI scaled to {}x{}", w, h);
    }

    private void applyScale(int width, int height) {
        if (guiNode == null) {
            return;
        }

        float scaleX = width / REFERENCE_WIDTH;
        float scaleY = height / REFERENCE_HEIGHT;

        guiNode.setLocalScale(scaleX, scaleY, 1f);
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void preFrame(float tpf) {
        // Not needed
    }

    @Override
    public void postQueue(RenderQueue rq) {
        // Not needed
    }

    @Override
    public void postFrame(FrameBuffer out) {
        // Not needed
    }

    @Override
    public void cleanup() {
        initialized = false;
    }
}
