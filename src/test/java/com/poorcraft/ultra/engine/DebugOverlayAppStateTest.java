package com.poorcraft.ultra.engine;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke test for DebugOverlayAppState.
 * Tests overlay toggle and text update functionality using headless context.
 * 
 * The DebugOverlayFormatter has comprehensive unit tests in DebugOverlayFormatterTest.
 */
class DebugOverlayAppStateTest {
    
    @Test
    void testOverlayToggleAndTextUpdate() throws Exception {
        CountDownLatch testCompleteLatch = new CountDownLatch(1);
        AtomicBoolean initiallyHidden = new AtomicBoolean(false);
        AtomicBoolean toggleOnSuccess = new AtomicBoolean(false);
        AtomicBoolean textContentValid = new AtomicBoolean(false);
        AtomicBoolean toggleOffSuccess = new AtomicBoolean(false);
        AtomicReference<Exception> error = new AtomicReference<>();
        
        // Create minimal headless application for testing
        SimpleApplication app = new SimpleApplication() {
            private DebugOverlayAppState overlayState;
            private int testPhase = 0;
            
            @Override
            public void simpleInitApp() {
                try {
                    // Attach debug overlay
                    overlayState = new DebugOverlayAppState(null);
                    stateManager.attach(overlayState);
                    
                    // Test 1: Verify overlay is initially hidden
                    initiallyHidden.set(!overlayState.isOverlayVisible());
                    
                } catch (Exception e) {
                    error.set(e);
                    stop();
                }
            }
            
            @Override
            public void simpleUpdate(float tpf) {
                try {
                    if (testPhase == 0) {
                        // Phase 0: Toggle overlay on
                        overlayState.toggleForTest();
                        testPhase = 1;
                    } else if (testPhase == 1) {
                        // Phase 1: Wait one frame for text to update
                        testPhase = 2;
                    } else if (testPhase == 2) {
                        // Phase 2: Check text content after update cycle
                        if (overlayState.isOverlayVisible()) {
                            toggleOnSuccess.set(true);
                            // Use getCurrentText() instead of BitmapText.getText() due to headless mode limitations
                            String content = overlayState.getCurrentText();
                            if (content != null && !content.isEmpty() &&
                                content.contains("FPS:") &&
                                content.contains("Java:")) {
                                textContentValid.set(true);
                            }
                        }
                        testPhase = 3;
                    } else if (testPhase == 3) {
                        // Phase 3: Toggle overlay off
                        overlayState.toggleForTest();
                        testPhase = 4;
                    } else if (testPhase == 4) {
                        // Phase 4: Wait one frame for removal
                        testPhase = 5;
                    } else if (testPhase == 5) {
                        // Phase 5: Verify removal
                        toggleOffSuccess.set(!overlayState.isOverlayVisible());
                        testCompleteLatch.countDown();
                        stop();
                    }
                } catch (Exception e) {
                    error.set(e);
                    testCompleteLatch.countDown();
                    stop();
                }
            }
        };
        
        // Configure for headless rendering
        AppSettings settings = new AppSettings(true);
        settings.setAudioRenderer(null);
        settings.setWidth(640);
        settings.setHeight(480);
        app.setSettings(settings);
        app.setShowSettings(false);
        
        // Start in headless mode
        app.start(JmeContext.Type.Headless);
        
        // Wait for test to complete with timeout
        assertTrue(testCompleteLatch.await(10, TimeUnit.SECONDS), 
            "Test timed out");
        
        // Check for errors
        if (error.get() != null) {
            throw error.get();
        }
        
        // Verify test results
        assertTrue(initiallyHidden.get(), 
            "Overlay should be hidden initially");
        assertTrue(toggleOnSuccess.get(), 
            "Overlay should be visible after toggle on");
        assertTrue(textContentValid.get(), 
            "Overlay text should contain FPS and Java info");
        assertTrue(toggleOffSuccess.get(), 
            "Overlay should be hidden after toggle off");
    }
    
    @Test
    void testOverlayUpdateCycle() throws Exception {
        CountDownLatch updateLatch = new CountDownLatch(1);
        AtomicBoolean updateSuccess = new AtomicBoolean(false);
        AtomicReference<Exception> error = new AtomicReference<>();
        
        SimpleApplication app = new SimpleApplication() {
            private DebugOverlayAppState overlayState;
            private int frameCount = 0;
            private boolean toggled = false;
            
            @Override
            public void simpleInitApp() {
                try {
                    overlayState = new DebugOverlayAppState(null);
                    stateManager.attach(overlayState);
                } catch (Exception e) {
                    error.set(e);
                    stop();
                }
            }
            
            @Override
            public void simpleUpdate(float tpf) {
                try {
                    frameCount++;
                    
                    // Toggle on first frame
                    if (frameCount == 1) {
                        overlayState.toggleForTest();
                        toggled = true;
                    }
                    
                    // After a few frames, verify text is being updated
                    if (frameCount >= 3 && frameCount <= 10 && toggled) {
                        boolean found = false;
                        for (int i = 0; i < guiNode.getChildren().size(); i++) {
                            if (guiNode.getChild(i) instanceof BitmapText) {
                                BitmapText text = (BitmapText) guiNode.getChild(i);
                                String content = text.getText();
                                
                                // Verify dynamic content (FPS should be present)
                                if (content != null && !content.isEmpty() && content.contains("FPS:")) {
                                    found = true;
                                    updateSuccess.set(true);
                                    updateLatch.countDown();
                                    stop();
                                    return;
                                }
                            }
                        }
                    }
                    
                    // Timeout after 10 frames
                    if (frameCount > 10) {
                        updateLatch.countDown();
                        stop();
                    }
                } catch (Exception e) {
                    error.set(e);
                    updateLatch.countDown();
                    stop();
                }
            }
        };
        
        AppSettings settings = new AppSettings(true);
        settings.setAudioRenderer(null);
        settings.setWidth(640);
        settings.setHeight(480);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start(JmeContext.Type.Headless);
        
        assertTrue(updateLatch.await(10, TimeUnit.SECONDS), 
            "Update cycle test timed out");
        
        if (error.get() != null) {
            throw error.get();
        }
        
        assertTrue(updateSuccess.get(), 
            "Overlay should update text content each frame when visible");
    }
}
