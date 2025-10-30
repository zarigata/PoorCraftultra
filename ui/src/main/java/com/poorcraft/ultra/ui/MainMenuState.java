package com.poorcraft.ultra.ui;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.InputManager;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.poorcraft.ultra.shared.Logger;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.component.SpringGridLayout.Axis;
import com.simsilica.lemur.component.SpringGridLayout.FillMode;
import com.simsilica.lemur.style.BaseStyles;

public final class MainMenuState extends BaseAppState {

    private static final Logger logger = Logger.getLogger(MainMenuState.class);

    private static final float BUTTON_WIDTH = 200f;
    private static final float BUTTON_HEIGHT = 50f;

    private Container menuContainer;
    private Button exitButton;

    @Override
    protected void initialize(Application app) {
        ensureGuiGlobalsInitialized(app);

        menuContainer = new Container();
        menuContainer.setLayout(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.Even));
        exitButton = menuContainer.addChild(new Button("Exit"));
        exitButton.setPreferredSize(new Vector3f(BUTTON_WIDTH, BUTTON_HEIGHT, 0f));
        exitButton.addClickCommands(source -> handleExit());

        SimpleApplication simpleApp = (SimpleApplication) app;
        centerMenu(simpleApp);
        simpleApp.getGuiNode().attachChild(menuContainer);
        logger.info("Main menu initialized");
    }

    @Override
    protected void cleanup(Application app) {
        if (menuContainer != null) {
            menuContainer.removeFromParent();
        }
        logger.info("Main menu cleaned up");
    }

    @Override
    protected void onEnable() {
        if (menuContainer != null) {
            menuContainer.setCullHint(Spatial.CullHint.Never);
        }

        Application application = getApplication();
        if (application instanceof SimpleApplication simpleApplication) {
            InputManager inputManager = simpleApplication.getInputManager();
            if (inputManager != null) {
                inputManager.setCursorVisible(true);
            }
        }
    }

    @Override
    protected void onDisable() {
        if (menuContainer != null) {
            menuContainer.setCullHint(Spatial.CullHint.Always);
        }
    }

    private void handleExit() {
        logger.info("Exit button clicked, shutting down...");
        getApplication().stop();
    }

    private void ensureGuiGlobalsInitialized(Application app) {
        try {
            GuiGlobals.getInstance();
        } catch (IllegalStateException ignored) {
            GuiGlobals.initialize(app);
        }
        BaseStyles.loadGlassStyle();
        GuiGlobals.getInstance().getStyles().setDefaultStyle(BaseStyles.GLASS);
        GuiGlobals.getInstance().getInputMapper().activateGroup("default");
    }

    private void centerMenu(SimpleApplication app) {
        float width = app.getCamera().getWidth();
        float height = app.getCamera().getHeight();
        float x = (width - BUTTON_WIDTH) / 2f;
        float y = (height + BUTTON_HEIGHT) / 2f;
        menuContainer.setLocalTranslation(x, y, 0f);
    }
}
