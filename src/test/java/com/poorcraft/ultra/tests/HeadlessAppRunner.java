package com.poorcraft.ultra.tests;

import com.jme3.app.Application;
import com.jme3.app.state.AppState;
import com.jme3.system.JmeContext;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test utility for running jME apps in headless mode.
 */
public class HeadlessAppRunner {
    
    public static boolean runHeadless(Application app, int maxFrames) {
        return runHeadlessWithTimeout(app, maxFrames, 30);
    }
    
    public static boolean runHeadlessWithTimeout(Application app, int maxFrames, int timeoutSeconds) {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);
        AtomicReference<Exception> error = new AtomicReference<>();
        
        FrameCounterAppState frameCounter = new FrameCounterAppState(maxFrames, () -> {
            success.set(true);
            app.stop();
        });
        
        Thread appThread = new Thread(() -> {
            try {
                app.getStateManager().attach(frameCounter);
                app.start(JmeContext.Type.Headless);
                started.countDown();
            } catch (Exception e) {
                error.set(e);
            } finally {
                finished.countDown();
            }
        });
        
        appThread.setName("HeadlessAppRunner-Thread");
        appThread.start();
        
        try {
            if (!started.await(5, TimeUnit.SECONDS)) {
                app.stop();
                return false;
            }
            
            if (!finished.await(timeoutSeconds, TimeUnit.SECONDS)) {
                app.stop();
                appThread.interrupt();
                return false;
            }
            
            appThread.join(1000);
            
            if (error.get() != null) {
                throw new RuntimeException("App crashed", error.get());
            }
            
            return success.get();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    public static boolean waitForState(Application app, Class<? extends AppState> stateClass, int maxFrames) {
        for (int i = 0; i < maxFrames; i++) {
            AppState state = app.getStateManager().getState(stateClass);
            if (state != null && state.isInitialized() && state.isEnabled()) {
                return true;
            }
            
            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
    
    public static <T> T enqueueAndWait(Application app, Callable<T> task) {
        AtomicReference<T> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        app.enqueue(() -> {
            try {
                result.set(task.call());
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                latch.countDown();
            }
            return null;
        });
        
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return result.get();
    }
    
    private static class FrameCounterAppState extends com.jme3.app.state.AbstractAppState {
        private final int maxFrames;
        private final Runnable onComplete;
        private int frameCount = 0;
        
        public FrameCounterAppState(int maxFrames, Runnable onComplete) {
            this.maxFrames = maxFrames;
            this.onComplete = onComplete;
        }
        
        @Override
        public void update(float tpf) {
            super.update(tpf);
            frameCount++;
            if (frameCount >= maxFrames) {
                onComplete.run();
            }
        }
    }
}
