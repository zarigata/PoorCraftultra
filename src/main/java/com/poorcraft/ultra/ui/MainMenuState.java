package com.poorcraft.ultra.ui;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.poorcraft.ultra.app.ServiceHub;
import com.simsilica.lemur.style.BaseStyles;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.style.ElementId;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main menu UI state implementing Lemur-based menu.
 */
public class MainMenuState extends BaseAppState {
    private static final Logger logger = LoggerFactory.getLogger(MainMenuState.class);

    private final ServiceHub serviceHub;

    private SimpleApplication application;
    private Node guiNode;

    private static final float MENU_WIDTH = 420f;
    private static final float MENU_HEIGHT = 280f;

    private Container menuContainer;
    private Button startButton;
    private Button settingsButton;
    private Button exitButton;
    private InputConfig inputConfig;

    private float lastCameraWidth = -1f;
    private float lastCameraHeight = -1f;

    public MainMenuState(ServiceHub serviceHub) {
        this.serviceHub = serviceHub;
    }

    @Override
    protected void initialize(Application app) {
        application = (SimpleApplication) app;
        guiNode = application.getGuiNode();

        initializeLemur(application);
        buildMenu();
        if (serviceHub.has(InputConfig.class)) {
            inputConfig = serviceHub.get(InputConfig.class);
        } else {
            logger.warn("InputConfig service missing; main menu hotkeys unavailable");
        }
        logger.info("MainMenuState initialized - Lemur GUI ready, menu built");
    }

    private void initializeLemur(SimpleApplication app) {
        if (GuiGlobals.getInstance() == null) {
            GuiGlobals.initialize(app);
            BaseStyles.loadGlassStyle();
            GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");
            logger.info("Lemur GUI initialised with glass style");
        }
    }

    private void buildMenu() {
        menuContainer = new Container();
        menuContainer.setPreferredSize(new Vector3f(MENU_WIDTH, MENU_HEIGHT, 0f));
        menuContainer.addChild(new Label("Poorcraft Ultra", new ElementId("title")));

        startButton = menuContainer.addChild(new Button("Start Game"));
        startButton.addClickCommands((Command<Button>) source -> serviceHub.get(GameStateManager.class).startGame());

        settingsButton = menuContainer.addChild(new Button("Settings"));
        settingsButton.addClickCommands((Command<Button>) source -> {
            SettingsMenuState settings = new SettingsMenuState(serviceHub);
            application.getStateManager().attach(settings);
        });

        exitButton = menuContainer.addChild(new Button("Exit"));
        exitButton.addClickCommands((Command<Button>) source -> application.stop());

        layoutMenu();
        logger.info("Main menu built with {} buttons at size {}x{}", 3, MENU_WIDTH, MENU_HEIGHT);
    }

    private void layoutMenu() {
        float width = application.getCamera().getWidth();
        float height = application.getCamera().getHeight();
        lastCameraWidth = width;
        lastCameraHeight = height;

        float scale = UIScaleProcessor.getCurrentScale();
        if (!Float.isFinite(scale) || scale <= 0f) {
            logger.warn("Invalid UI scale detected ({}); defaulting to 1.0", scale);
            scale = 1f;
        }

        if (scale < 0.1f || scale > 10f) {
            logger.warn("Unusual UI scale detected: {}. Clamping to 1.0", scale);
            scale = 1f;
        }

        float scaledWidth = width / scale;
        float scaledHeight = height / scale;

        Vector3f translation = new Vector3f(
            scaledWidth / 2f - MENU_WIDTH / 2f,
            scaledHeight / 2f + MENU_HEIGHT / 2f,
            0f
        );
        menuContainer.setLocalTranslation(translation);
        logger.debug("Menu positioned at ({}, {}) with scale={}, camera={}x{}",
                translation.x, translation.y, scale, width, height);
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);

        if (menuContainer == null) {
            return; // Menu not built yet, skip layout
        }

        float width = application.getCamera().getWidth();
        float height = application.getCamera().getHeight();
        if (width != lastCameraWidth || height != lastCameraHeight) {
            layoutMenu();
        }
    }

    @Override
    protected void cleanup(Application app) {
        if (menuContainer != null) {
            menuContainer.removeFromParent();
        }
    }

    @Override
    protected void onEnable() {
        if (menuContainer == null) {
            logger.error("Menu container is null, cannot attach main menu");
            return;
        }

        if (menuContainer.getParent() != guiNode) {
            guiNode.detachChild(menuContainer);
            guiNode.attachChild(menuContainer);
            guiNode.updateGeometricState();
        }

        if (menuContainer.getParent() != guiNode) {
            logger.error("Menu attachment failed - parent is {} instead of guiNode", menuContainer.getParent());
            return;
        }

        if (inputConfig != null) {
            String exitKeybind = inputConfig.getKeybindString("mainMenuExit");
            if (exitKeybind == null || exitKeybind.isBlank()) {
                logger.info("Main menu exit action lacked keybind; defaulting to ESCAPE");
                CompletableFuture<Boolean> future = inputConfig.rebindAction("mainMenuExit", "ESCAPE");
                future.whenComplete((success, error) -> {
                    if (error != null) {
                        logger.warn("Failed to rebind main menu exit to ESCAPE", error);
                    }
                    inputConfig.registerActionOnAppThread("mainMenuExit", this::handleExitAction);
                    logger.info("Registered main menu exit action with InputConfig");
                });
            } else {
                inputConfig.registerAction("mainMenuExit", this::handleExitAction);
                logger.info("Registered main menu exit action with InputConfig");
            }
        }

        logger.info("MainMenuState enabled - menu attached to guiNode at position: {}", menuContainer.getLocalTranslation());
    }

    @Override
    protected void onDisable() {
        if (menuContainer != null) {
            menuContainer.removeFromParent();
            logger.info("MainMenuState disabled - menu removed from guiNode");
        }

        if (inputConfig != null) {
            inputConfig.unregisterAction("mainMenuExit");
        }
    }

    private void handleExitAction(String name, boolean isPressed, float tpf) {
        if (!"mainMenuExit".equals(name) || isPressed) {
            return;
        }
        logger.info("ESC triggered from main menu - exiting application");
        application.stop();
    }
}
