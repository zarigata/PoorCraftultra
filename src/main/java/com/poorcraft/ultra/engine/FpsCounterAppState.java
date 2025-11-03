package com.poorcraft.ultra.engine;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.scene.Node;

/**
 * Custom AppState for FPS counter display (CP v0.1).
 * Displays FPS in top-left corner using jME BitmapText.
 */
public class FpsCounterAppState extends AbstractAppState {
    
    private Application app;
    private BitmapText fpsText;
    private Node guiNode;
    private BitmapFont guiFont;
    
    private int frameCounter = 0;
    private float lastFpsUpdate = 0f;
    private int currentFps = 0;
    
    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        
        this.app = app;
        this.guiNode = ((com.jme3.app.SimpleApplication) app).getGuiNode();
        
        // Load default font
        this.guiFont = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
        
        // Create FPS text
        fpsText = new BitmapText(guiFont);
        fpsText.setText("FPS: --");
        fpsText.setColor(com.jme3.math.ColorRGBA.White);
        
        // Position in top-left corner with padding
        float x = 10;
        float y = app.getContext().getSettings().getHeight() - 10;
        fpsText.setLocalTranslation(x, y, 0);
        
        // Attach to GUI node
        guiNode.attachChild(fpsText);
    }
    
    @Override
    public void update(float tpf) {
        super.update(tpf);
        
        frameCounter++;
        lastFpsUpdate += tpf;
        
        // Update FPS display once per second
        if (lastFpsUpdate >= 1.0f) {
            currentFps = frameCounter;
            fpsText.setText("FPS: " + currentFps);
            
            // Reset counters
            frameCounter = 0;
            lastFpsUpdate = 0f;
        }
    }
    
    @Override
    public void cleanup() {
        super.cleanup();
        
        // Detach from GUI node
        if (fpsText != null && guiNode != null) {
            guiNode.detachChild(fpsText);
        }
        
        // Null references
        fpsText = null;
        guiNode = null;
        guiFont = null;
        app = null;
    }
}
