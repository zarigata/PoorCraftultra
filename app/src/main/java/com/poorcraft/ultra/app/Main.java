package com.poorcraft.ultra.app;

import com.jme3.system.AppSettings;
import com.poorcraft.ultra.engine.EngineConfig;
import com.poorcraft.ultra.engine.PoorcraftApp;
import com.poorcraft.ultra.shared.Constants;
import com.poorcraft.ultra.shared.config.Config;
import com.poorcraft.ultra.shared.config.Config.Graphics;
import com.poorcraft.ultra.shared.config.Config.Resolution;
import com.poorcraft.ultra.shared.util.Logger;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class Main {

    private static final org.slf4j.Logger LOG = Logger.getLogger(Main.class);

    private Main() {
    }

    public static void main(String[] args) {
        int exitCode = 0;
        final PoorcraftApp[] applicationHolder = new PoorcraftApp[1];
        try {
            boolean devMode = detectDevMode();
            configureLogging(devMode);

            if (devMode) {
                LOG.info("Developer mode enabled");
            }

            PlatformCheck.validateJavaVersion();
            PlatformCheck.logEnvironment();

            Config config = loadConfiguration();
            EngineConfig engineConfig = fromConfig(config);

            PoorcraftApp application = new PoorcraftApp(config, devMode);
            applicationHolder[0] = application;
            AppSettings settings = engineConfig.toAppSettings();
            settings.setTitle(Constants.GAME_NAME);
            application.setSettings(settings);
            application.setShowSettings(false);
            application.setDisplayFps(engineConfig.isShowFps());
            application.setDisplayStatView(engineConfig.isShowStats());

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                PoorcraftApp running = applicationHolder[0];
                if (running != null) {
                    running.stop();
                }
            }, "poorcraft-shutdown"));

            LOG.info("Starting {} {}", Constants.GAME_NAME, Constants.VERSION);
            application.start(true);
            LOG.info("PHASE 0 OK – Poorcraft Ultra");
        } catch (Exception ex) {
            exitCode = 1;
            LOG.error("Failed to start {}", Constants.GAME_NAME, ex);
            PoorcraftApp running = applicationHolder[0];
            if (running != null) {
                running.stop(true);
            }
        }

        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    private static boolean detectDevMode() {
        String property = System.getProperty("dev.mode");
        if (property == null || property.isBlank()) {
            property = System.getenv("DEV_MODE");
        }
        if (property == null) {
            return false;
        }
        return property.equalsIgnoreCase("true") || property.equalsIgnoreCase("1") || property.equalsIgnoreCase("yes");
    }

    private static Config loadConfiguration() {
        Path defaults = Paths.get("config", "defaults.yml");
        Path yamlOverrides = Paths.get("config", "user.yml");
        Path jsonOverrides = Paths.get("config", "user.json");
        return ConfigLoader.load(defaults, yamlOverrides, jsonOverrides);
    }

    private static void configureLogging(boolean devMode) {
        System.setProperty("dev.mode", Boolean.toString(devMode));
        File logDir = Paths.get("logs").toFile();
        if (!logDir.exists() && !logDir.mkdirs()) {
            LOG.warn("Unable to create logs directory at {}", logDir.getAbsolutePath());
        }
        System.setProperty("LOG_DIR", logDir.getAbsolutePath());
    }

    private static EngineConfig fromConfig(Config config) {
        Graphics graphics = config.getGraphics();
        Resolution resolution = graphics.getResolution();
        EngineConfig engineConfig = new EngineConfig()
            .setWidth(resolution.getWidth())
            .setHeight(resolution.getHeight())
            .setFullscreen(graphics.isFullscreen())
            .setVsync(graphics.isVsync())
            .setMsaaSamples(graphics.getMsaa())
            .setFov(graphics.getFov());
        engineConfig.setShowFps(true);
        engineConfig.setShowStats(false);
        return engineConfig;
    }
}
