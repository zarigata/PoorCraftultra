package com.poorcraft.ultra.engine;

import com.jme3.system.AppSettings;

public class EngineConfig {

    private int width = 1280;
    private int height = 720;
    private boolean fullscreen = false;
    private boolean vsync = true;
    private int msaaSamples = 4;
    private int fov = 75;
    private boolean showFps = true;
    private boolean showStats = false;

    public int getWidth() {
        return width;
    }

    public EngineConfig setWidth(int width) {
        this.width = width;
        return this;
    }

    public int getHeight() {
        return height;
    }

    public EngineConfig setHeight(int height) {
        this.height = height;
        return this;
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public EngineConfig setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
        return this;
    }

    public boolean isVsync() {
        return vsync;
    }

    public EngineConfig setVsync(boolean vsync) {
        this.vsync = vsync;
        return this;
    }

    public int getMsaaSamples() {
        return msaaSamples;
    }

    public EngineConfig setMsaaSamples(int msaaSamples) {
        this.msaaSamples = msaaSamples;
        return this;
    }

    public int getFov() {
        return fov;
    }

    public EngineConfig setFov(int fov) {
        this.fov = fov;
        return this;
    }

    public boolean isShowFps() {
        return showFps;
    }

    public EngineConfig setShowFps(boolean showFps) {
        this.showFps = showFps;
        return this;
    }

    public boolean isShowStats() {
        return showStats;
    }

    public EngineConfig setShowStats(boolean showStats) {
        this.showStats = showStats;
        return this;
    }

    public AppSettings toAppSettings() {
        AppSettings settings = new AppSettings(true);
        settings.setWidth(width);
        settings.setHeight(height);
        settings.setFullscreen(fullscreen);
        settings.setVSync(vsync);
        settings.setSamples(msaaSamples);
        settings.setUseJoysticks(true);
        settings.setUseInput(true);
        settings.setGammaCorrection(true);
        settings.setTitle("Poorcraft Ultra");
        return settings;
    }
}
