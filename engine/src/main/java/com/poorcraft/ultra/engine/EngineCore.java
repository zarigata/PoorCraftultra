package com.poorcraft.ultra.engine;

import com.jme3.app.SimpleApplication;
import com.poorcraft.ultra.shared.Config;
import com.poorcraft.ultra.shared.Logger;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;

public final class EngineCore extends SimpleApplication {

    private static final Logger logger = Logger.getLogger(EngineCore.class);

    private final Config config;
    private final CountDownLatch stopLatch = new CountDownLatch(1);

    public EngineCore(Config config) {
        this.config = Objects.requireNonNull(config, "config must not be null");
    }

    @Override
    public void simpleInitApp() {
        logger.info("Engine initialized");
        logger.banner("CP v0.1 OK – Poorcraft Ultra");
    }

    @Override
    public void simpleUpdate(float tpf) {
        // Phase 0.1: no per-frame logic yet.
    }

    @Override
    public void destroy() {
        super.destroy();
        stopLatch.countDown();
    }

    public void awaitStop() throws InterruptedException {
        stopLatch.await();
    }
}
