package com.poorcraftultra.core;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;

public final class Game {
    private static final int TARGET_FPS = 60;
    private static final double TARGET_FRAME_TIME = 1.0d / TARGET_FPS;

    private final Window window;
    private boolean running;

    public Game() {
        this.window = new Window("PoorCraftUltra", 1280, 720);
    }

    public void run() {
        try {
            init();
            long lastTime = System.nanoTime();

            while (running) {
                long frameStart = System.nanoTime();
                double deltaTime = (frameStart - lastTime) / 1_000_000_000.0d;
                lastTime = frameStart;

                update(deltaTime);
                render();

                long frameEnd = System.nanoTime();
                double frameDuration = (frameEnd - frameStart) / 1_000_000_000.0d;
                double sleepTime = TARGET_FRAME_TIME - frameDuration;
                if (sleepTime > 0.0d) {
                    sleepForDuration(sleepTime);
                }
            }
        } finally {
            cleanup();
        }
    }

    private void init() {
        window.init();
        glClearColor(0.392f, 0.584f, 0.929f, 1.0f);
        running = true;
        System.out.println("Game initialized.\\n/\\_/\\");
    }

    private void update(double deltaTime) {
        if (window.shouldClose()) {
            running = false;
        }
    }

    private void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        window.swapBuffers();
    }

    private void cleanup() {
        window.cleanup();
        running = false;
        System.out.println("Game cleanup complete.\\n(>'-')>");
    }

    private void sleepForDuration(double durationSeconds) {
        long nanos = (long) (durationSeconds * 1_000_000_000L);
        long millis = nanos / 1_000_000L;
        int nanoRemainder = (int) (nanos % 1_000_000L);
        try {
            Thread.sleep(millis, nanoRemainder);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }
}
