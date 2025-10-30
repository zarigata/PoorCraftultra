package com.poorcraft.ultra.app;

import com.poorcraft.ultra.shared.Config;
import com.poorcraft.ultra.shared.Logger;
import com.poorcraft.ultra.shared.NativeLoader;
import com.poorcraft.ultra.shared.PlatformInfo;

import java.nio.file.Paths;

public final class Bootstrap {

    private Bootstrap() {
    }

    public static void main(String[] args) {
        System.out.println("============================");
        System.out.println("   POORCRAFT ULTRA v0.0.1   ");
        System.out.println("============================");
        Logger logger = Logger.getLogger(Bootstrap.class);
        try {
            Config config = Config.load();
            logger.info("Platform: {} {}", PlatformInfo.getOS(), PlatformInfo.getArch());
            logger.info("Java: {} ({})", System.getProperty("java.version"), System.getProperty("java.vendor"));
            logger.info("Configuration loaded from: {}", config.getRawConfig().origin().description());
            logger.info("Window title: {}", config.getString("window.title"));
            logger.info("Render distance: {} chunks", config.getInt("render.distance"));
            NativeLoader.extractNatives(Paths.get("natives"));
            logger.banner("CP v0.0 OK – Poorcraft Ultra – Java 17 – " + PlatformInfo.getOS() + " " + PlatformInfo.getArch());
            System.exit(0);
        } catch (Exception e) {
            logger.error("Bootstrap failed", e);
            System.exit(1);
        }
    }
}
