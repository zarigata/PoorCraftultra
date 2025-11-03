package com.poorcraft.ultra.weather;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

/**
 * Day-night cycle and weather manager (CP v2.2).
 * Manages world time, sun/moon position, skybox, and weather states.
 */
public class WeatherManager extends BaseAppState {

    private long worldTime; // 0-24000 ticks = 1 day
    private float timeSpeed; // Time progression speed
    private DirectionalLight sun;
    private AmbientLight ambient;
    private Node rootNode;

    // Weather state (stub for Phase 6)
    public enum WeatherState {
        CLEAR, RAIN, STORM
    }
    private WeatherState weatherState;

    /**
     * Creates a weather manager with the given sun light.
     */
    public WeatherManager(DirectionalLight sun) {
        this.sun = sun;
        this.worldTime = 0; // Dawn
        this.timeSpeed = 1.0f; // Default speed
        this.weatherState = WeatherState.CLEAR;
    }

    @Override
    protected void initialize(Application app) {
        this.rootNode = ((com.jme3.app.SimpleApplication) app).getRootNode();
        
        // Create ambient light
        this.ambient = new AmbientLight(ColorRGBA.White.mult(0.3f));
        rootNode.addLight(ambient);
    }

    @Override
    protected void cleanup(Application app) {
        if (ambient != null) {
            rootNode.removeLight(ambient);
        }
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);
        
        // Advance time (20 ticks per second)
        worldTime += (long)(tpf * 20 * timeSpeed);
        
        // Wrap time (24000 ticks = 1 day)
        if (worldTime >= 24000) {
            worldTime -= 24000;
        }
        
        // Update sun position and lighting
        updateSunPosition();
        updateLighting();
    }

    /**
     * Updates sun direction based on world time.
     */
    private void updateSunPosition() {
        // Calculate angle (0-2π)
        double angle = (worldTime / 24000.0) * Math.PI * 2;
        
        // Sun direction (rotates around world)
        float x = (float) -Math.sin(angle);
        float y = (float) -Math.cos(angle);
        float z = -0.5f;
        
        Vector3f direction = new Vector3f(x, y, z).normalizeLocal();
        sun.setDirection(direction);
    }

    /**
     * Updates sun and ambient light intensity/color based on time of day.
     */
    private void updateLighting() {
        // Calculate day factor (0=night, 1=day)
        float dayFactor = getDayFactor();
        
        // Sun color: lerp between orange (sunrise/sunset) and white (noon)
        ColorRGBA sunColor;
        if (dayFactor < 0.2f || dayFactor > 0.8f) {
            // Sunrise/sunset: orange
            sunColor = new ColorRGBA(1.0f, 0.7f, 0.4f, 1.0f);
        } else {
            // Day: white
            sunColor = ColorRGBA.White;
        }
        
        // Sun intensity (never fully dark)
        float intensity = Math.max(0.2f, dayFactor);
        sun.setColor(sunColor.mult(intensity));
        
        // Ambient color: lerp between dark blue (night) and light gray (day)
        ColorRGBA nightColor = new ColorRGBA(0.1f, 0.1f, 0.2f, 1.0f);
        ColorRGBA dayColor = new ColorRGBA(0.7f, 0.7f, 0.7f, 1.0f);
        ColorRGBA ambientColor = nightColor.interpolateLocal(dayColor, dayFactor);
        ambient.setColor(ambientColor);
    }

    /**
     * Returns day factor (0=night, 1=day) based on world time.
     */
    private float getDayFactor() {
        if (worldTime < 6000) {
            // Dawn (0-6000): lerp 0 → 1
            return worldTime / 6000.0f;
        } else if (worldTime < 18000) {
            // Day (6000-18000): 1.0
            return 1.0f;
        } else {
            // Dusk (18000-24000): lerp 1 → 0
            return 1.0f - ((worldTime - 18000) / 6000.0f);
        }
    }

    // ===== Time Control =====

    /**
     * Sets world time (0-24000).
     */
    public void setWorldTime(long time) {
        this.worldTime = time % 24000;
    }

    /**
     * Returns current world time.
     */
    public long getWorldTime() {
        return worldTime;
    }

    /**
     * Sets time progression speed.
     */
    public void setTimeSpeed(float speed) {
        this.timeSpeed = speed;
    }

    /**
     * Returns true if it's night (12000-24000).
     */
    public boolean isNight() {
        return worldTime >= 12000;
    }

    /**
     * Returns true if it's day (0-12000).
     */
    public boolean isDay() {
        return worldTime < 12000;
    }

    // ===== Weather (stub for Phase 6) =====

    /**
     * Sets weather state.
     */
    public void setWeather(WeatherState state) {
        this.weatherState = state;
    }

    /**
     * Returns current weather state.
     */
    public WeatherState getWeather() {
        return weatherState;
    }
}
