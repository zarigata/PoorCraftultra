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
        System.out.println("   POORCRAFT ULTRA v0.1   ");
        System.out.println("============================");
    }

    private static AppSettings buildSettings(Config config) {
        AppSettings settings = new AppSettings(true);
        settings.setTitle(config.getString("window.title"));
        settings.setResolution(config.getInt("window.width"), config.getInt("window.height"));
        settings.setFullscreen(config.getBoolean("window.fullscreen"));
        settings.setVSync(config.getBoolean("window.vsync"));
        settings.setResizable(config.getBoolean("window.resizable"));
        return settings;
    }
}
