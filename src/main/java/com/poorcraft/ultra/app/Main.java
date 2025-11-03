package com.poorcraft.ultra.app;

import com.jme3.asset.plugins.FileLocator;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.poorcraft.ultra.engine.PoorcraftApp;
import com.poorcraft.ultra.tools.AssetValidator;
import com.poorcraft.ultra.tools.AssetValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

/**
 * Application entry point for Poorcraft Ultra.
 */
public class Main {
    
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    private static final String VERSION = "0.2.0-SNAPSHOT";
    private static final String TITLE = "Poorcraft Ultra v" + VERSION;
    
    public static void main(String[] args) {
        try {
            // Parse command-line arguments
            boolean headless = false;
            boolean smokeTest = false;
            Integer seed = null;
            
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--headless":
                        headless = true;
                        break;
                    case "--smoke-test":
                        smokeTest = true;
                        headless = true;
                        break;
                    case "--seed":
                        if (i + 1 < args.length) {
                            seed = Integer.parseInt(args[++i]);
                        }
                        break;
                    case "--version":
                        System.out.println(TITLE);
                        System.exit(0);
                        break;
                    case "--help":
                        printHelp();
                        System.exit(0);
                        break;
                }
            }
            
            // Log system information
            logger.info("Starting {}", TITLE);
            logger.info("Java Version: {}", System.getProperty("java.version"));
            logger.info("OS: {} {}", System.getProperty("os.name"), System.getProperty("os.version"));
            logger.info("Available Memory: {} MB", 
                Runtime.getRuntime().maxMemory() / (1024 * 1024));
            logger.info("CPU Cores: {}", Runtime.getRuntime().availableProcessors());
            
            // Run asset validation
            logger.info("Running asset validation...");
            try {
                AssetValidator.validateAll();
                logger.info("Assets OK");
            } catch (AssetValidationException e) {
                logger.error("Asset validation failed", e);
                if (!headless) {
                    showErrorDialog("Asset Validation Failed", e.getDetailedMessage());
                }
                System.exit(1);
            }
            
            // Create and configure jME application
            PoorcraftApp app = new PoorcraftApp(smokeTest);
            
            // Register file locator for generated assets directory
            app.getAssetManager().registerLocator("assets", FileLocator.class);
            
            AppSettings settings = new AppSettings(true);
            settings.setTitle(TITLE);
            settings.setResolution(1280, 720);
            settings.setVSync(true);
            settings.setFullscreen(false);
            settings.setSettingsDialogImage("Interface/splash.png");
            settings.setUseJoysticks(false);
            settings.setSamples(4); // MSAA
            
            // Skip settings dialog for automated tests
            if (smokeTest) {
                settings.setResolution(800, 600);
            }
            
            app.setSettings(settings);
            app.setShowSettings(false); // Don't show settings dialog
            
            // Start application
            JmeContext.Type contextType = headless ? JmeContext.Type.Headless : JmeContext.Type.Display;
            app.start(contextType);
            
        } catch (Exception e) {
            logger.error("Fatal error", e);
            showErrorDialog("Fatal Error", 
                "An unexpected error occurred:\n" + e.getMessage() + 
                "\n\nCheck logs for details.");
            System.exit(1);
        }
    }
    
    private static void printHelp() {
        System.out.println("Poorcraft Ultra - A Minecraft-class voxel game");
        System.out.println();
        System.out.println("Usage: poorcraft-ultra [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --headless       Run in headless mode (no window)");
        System.out.println("  --smoke-test     Run automated smoke test");
        System.out.println("  --seed <value>   Set world seed");
        System.out.println("  --version        Print version and exit");
        System.out.println("  --help           Print this help message");
    }
    
    private static void showErrorDialog(String title, String message) {
        try {
            JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            // If GUI fails, just log
            logger.error("Failed to show error dialog: {}", e.getMessage());
        }
    }
}
