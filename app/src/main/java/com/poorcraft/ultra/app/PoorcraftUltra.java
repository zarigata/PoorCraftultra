package com.poorcraft.ultra.app;

import com.jme3.system.AppSettings;
import com.poorcraft.ultra.engine.EngineCore;
import com.poorcraft.ultra.shared.Config;
import com.poorcraft.ultra.shared.Logger;
import com.poorcraft.ultra.shared.NativeLoader;
import com.poorcraft.ultra.shared.PlatformInfo;
import com.poorcraft.ultra.ui.MainMenuState;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class PoorcraftUltra {

    private static final Logger logger = Logger.getLogger(PoorcraftUltra.class);

    private PoorcraftUltra() {
    }

    public static void main(String[] args) {
        printBanner();
        try {
            logger.info("Platform: {} {}", PlatformInfo.getOS(), PlatformInfo.getArch());
            logger.info("Java: {} ({})", System.getProperty("java.version"), System.getProperty("java.vendor"));

            Config config = Config.load();
            logger.info("Configuration loaded from: {}", config.getRawConfig().origin().description());

            Path nativesDir = Paths.get("natives");
            NativeLoader.extractNatives(nativesDir);
            logger.info("Native libraries extracted to: {}", nativesDir.toAbsolutePath());

            EngineCore engine = new EngineCore(config);
            AppSettings settings = buildSettings(config);
            engine.setSettings(settings);
            engine.setShowSettings(false);

            MainMenuState mainMenuState = new MainMenuState();
            engine.getStateManager().attach(mainMenuState);

            logger.info("Launching engine...");
            engine.start();
            engine.awaitStop();
            logger.info("Application stopped");
            System.exit(0);
        } catch (Exception exception) {
            logger.error("Application failed to start", exception);
            System.exit(1);
        }
    }

    private static void printBanner() {
        System.out.println("============================");
        System.out.println("   POORCRAFT ULTRA v1.3   ");
        System.out.println("============================");
    }

    private static AppSettings buildSettings(Config config) {
        AppSettings settings = new AppSettings(true);
        String title = config.hasPath("window.title") ? config.getString("window.title") : "Poorcraft Ultra";
        int width = config.hasPath("window.width") ? config.getInt("window.width") : 1280;
        int height = config.hasPath("window.height") ? config.getInt("window.height") : 720;
        boolean fullscreen = config.hasPath("window.fullscreen") ? config.getBoolean("window.fullscreen") : false;
        boolean vsync = config.hasPath("window.vsync") ? config.getBoolean("window.vsync") : true;
        boolean resizable = config.hasPath("window.resizable") ? config.getBoolean("window.resizable") : true;

        settings.setTitle(title != null && !title.isBlank() ? title : "Poorcraft Ultra");
        settings.setResolution(width > 0 ? width : 1280, height > 0 ? height : 720);
        settings.setFullscreen(fullscreen);
        settings.setVSync(vsync);
        settings.setResizable(resizable);
        return settings;
    }
}
