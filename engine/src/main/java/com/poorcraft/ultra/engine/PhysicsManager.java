package com.poorcraft.ultra.engine;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.math.Vector3f;
import com.poorcraft.ultra.shared.Config;
import com.poorcraft.ultra.shared.Logger;

import java.util.Locale;
import java.util.Objects;

public final class PhysicsManager extends BaseAppState {

    private static final Logger logger = Logger.getLogger(PhysicsManager.class);
    private static PhysicsManager INSTANCE;

    private BulletAppState bulletAppState;
    private boolean initialized;

    @Override
    protected void initialize(Application app) {
        Objects.requireNonNull(app, "app must not be null");

        if (!(app instanceof SimpleApplication simpleApplication)) {
            throw new IllegalStateException("PhysicsManager requires a SimpleApplication context");
        }

        bulletAppState = new BulletAppState();

        configureFromConfig();

        simpleApplication.getStateManager().attach(bulletAppState);

        INSTANCE = this;
        initialized = true;

        logger.info("Physics manager initialized (Bullet physics active)");
    }

    @Override
    protected void cleanup(Application app) {
        if (bulletAppState != null && app instanceof SimpleApplication simpleApplication) {
            simpleApplication.getStateManager().detach(bulletAppState);
        }

        bulletAppState = null;
        initialized = false;
        INSTANCE = null;

        logger.info("Physics manager cleaned up");
    }

    @Override
    protected void onEnable() {
        if (bulletAppState != null) {
            bulletAppState.setEnabled(true);
        }
    }

    @Override
    protected void onDisable() {
        if (bulletAppState != null) {
            bulletAppState.setEnabled(false);
        }
    }

    public static PhysicsManager getInstance() {
        if (INSTANCE == null || !INSTANCE.initialized) {
            throw new IllegalStateException("PhysicsManager not initialized");
        }
        return INSTANCE;
    }

    public static boolean isInitializedManager() {
        return INSTANCE != null && INSTANCE.initialized;
    }

    public PhysicsSpace getPhysicsSpace() {
        if (bulletAppState == null) {
            throw new IllegalStateException("Physics space requested before initialization");
        }
        return bulletAppState.getPhysicsSpace();
    }

    public void setDebugEnabled(boolean enabled) {
        if (bulletAppState != null) {
            bulletAppState.setDebugEnabled(enabled);
        }
    }

    private void configureFromConfig() {
        Config config = Config.getInstance();

        String threadingValue = getString(config, "physics.threadingType", "PARALLEL");
        BulletAppState.ThreadingType threadingType = resolveThreadingType(threadingValue);
        bulletAppState.setThreadingType(threadingType);

        boolean debugDraw = getBoolean(config, "physics.debugDraw", false);
        bulletAppState.setDebugEnabled(debugDraw);

        float gravity = (float) getDouble(config, "physics.gravity", -9.81d);
        PhysicsSpace space = bulletAppState.getPhysicsSpace();
        space.setGravity(new Vector3f(0f, gravity, 0f));

        boolean enabled = getBoolean(config, "physics.enabled", true);
        bulletAppState.setEnabled(enabled);
    }

    private String getString(Config config, String path, String defaultValue) {
        return config.hasPath(path) ? config.getString(path) : defaultValue;
    }

    private boolean getBoolean(Config config, String path, boolean defaultValue) {
        return config.hasPath(path) ? config.getBoolean(path) : defaultValue;
    }

    private double getDouble(Config config, String path, double defaultValue) {
        return config.hasPath(path) ? config.getDouble(path) : defaultValue;
    }

    private BulletAppState.ThreadingType resolveThreadingType(String value) {
        if (value == null || value.isBlank()) {
            return BulletAppState.ThreadingType.PARALLEL;
        }
        try {
            return BulletAppState.ThreadingType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            logger.warn("Unknown physics threading type '{}', defaulting to PARALLEL", value);
            return BulletAppState.ThreadingType.PARALLEL;
        }
    }
}
