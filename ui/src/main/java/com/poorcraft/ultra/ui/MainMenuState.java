package com.poorcraft.ultra.ui;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.poorcraft.ultra.engine.api.InputControllable;
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
    private float lastCameraWidth;
    private float lastCameraHeight;

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
        trackCameraSize(simpleApp);
        simpleApp.getGuiNode().attachChild(menuContainer);
        setEnabled(false);
        logger.info("Main menu initialized (disabled by default)");
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
            centerMenu(simpleApplication);
            trackCameraSize(simpleApplication);
            simpleApplication.getInputManager().setCursorVisible(true);
        }

        InputControllable cameraController = resolveCameraController();
        if (cameraController != null) {
            cameraController.setInputEnabled(false);
        }

        logger.info("Main menu enabled");
    }

    @Override
    protected void onDisable() {
        if (menuContainer != null) {
            menuContainer.setCullHint(Spatial.CullHint.Always);
        }

        Application application = getApplication();
        if (application != null) {
            application.getInputManager().setCursorVisible(false);
        }

        InputControllable cameraController = resolveCameraController();
        if (cameraController != null) {
            cameraController.setInputEnabled(true);
        }

        logger.info("Main menu disabled");
    }

    @Override
    public void update(float tpf) {
        Application application = getApplication();
        if (!(application instanceof SimpleApplication simpleApplication)) {
            return;
        }

        float currentWidth = simpleApplication.getCamera().getWidth();
        float currentHeight = simpleApplication.getCamera().getHeight();
        if (currentWidth != lastCameraWidth || currentHeight != lastCameraHeight) {
            centerMenu(simpleApplication);
            trackCameraSize(simpleApplication);
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

    private void trackCameraSize(SimpleApplication app) {
        lastCameraWidth = app.getCamera().getWidth();
        lastCameraHeight = app.getCamera().getHeight();
    }

    private InputControllable resolveCameraController() {
        var stateManager = getStateManager();
        if (stateManager == null) {
            return null;
        }

        try {
            Class<?> clazz = Class.forName("com.poorcraft.ultra.engine.CameraController");
            if (!BaseAppState.class.isAssignableFrom(clazz)) {
                return null;
            }
            @SuppressWarnings("unchecked")
            Class<? extends BaseAppState> stateClass = (Class<? extends BaseAppState>) clazz;
            var state = stateManager.getState(stateClass);
            if (state instanceof InputControllable controllable) {
                return controllable;
            }
        } catch (ClassNotFoundException ignored) {
            // Camera controller not available; likely detached or module missing.
        }
        return null;
    }
}
