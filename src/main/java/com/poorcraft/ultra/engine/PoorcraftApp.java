package com.poorcraft.ultra.engine;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.poorcraft.ultra.ui.MainMenuAppState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main jMonkeyEngine application class for Poorcraft Ultra.
 * 
 * Checkpoint v0.1: Window opens, FPS counter, ESC quits
 * Checkpoint v0.2: Main menu with animated background
 */
public class PoorcraftApp extends SimpleApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(PoorcraftApp.class);
    
    private final String checkpointVersion = "0.2";
    private final boolean smokeTestMode;
    
    private int frameCounter = 0;
    private static final int SMOKE_TEST_FRAMES = 60;
    
    /**
     * Constructor for normal mode.
     */
    public PoorcraftApp() {
        this(false);
    }
    
    /**
     * Constructor with smoke test mode.
     * 
     * @param smokeTestMode If true, app will auto-stop after 60 frames
     */
    public PoorcraftApp(boolean smokeTestMode) {
        super();
        this.smokeTestMode = smokeTestMode;
    }
    
    @Override
    public void simpleInitApp() {
        // Detach default stats (we'll add custom FPS counter for CP v0.1)
        setDisplayStatView(false);
        setDisplayFps(false);
        
        // Detach FlyCam (not needed for menu)
        flyCam.setEnabled(false);
        
        // Set up ESC key to quit
        inputManager.addMapping("Quit", new KeyTrigger(KeyInput.KEY_ESCAPE));
        inputManager.addListener(quitListener, "Quit");
        
        // CP v0.1: Attach FPS counter
        FpsCounterAppState fpsCounter = new FpsCounterAppState();
        stateManager.attach(fpsCounter);
        
        // CP v0.2: Attach main menu
        MainMenuAppState mainMenu = new MainMenuAppState();
        stateManager.attach(mainMenu);
        
        logger.info("PoorcraftApp initialized (CP v{})", checkpointVersion);
        
        if (smokeTestMode) {
            logger.info("Running in smoke test mode - will auto-stop after {} frames", SMOKE_TEST_FRAMES);
        }
    }
    
    private final ActionListener quitListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            if (name.equals("Quit") && !isPressed) {
                logger.info("ESC pressed - quitting application");
                stop();
            }
        }
    };
    
    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);
        
        // Smoke test: auto-stop after N frames
        if (smokeTestMode) {
            frameCounter++;
            if (frameCounter >= SMOKE_TEST_FRAMES) {
                logger.info("Smoke test completed successfully ({} frames)", frameCounter);
                stop();
            }
        }
    }
    
    @Override
    public void destroy() {
        logger.info("PoorcraftApp shutting down");
        super.destroy();
    }
}
