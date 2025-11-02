package com.poorcraft.ultra.ui;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;
import com.jme3.input.controls.ActionListener;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pause menu overlay displayed when game is paused.
 */
public class PauseMenuState extends BaseAppState {
    private static final Logger logger = LoggerFactory.getLogger(PauseMenuState.class);

    private final ServiceHub serviceHub;

    private SimpleApplication application;
    private Node guiNode;

    private static final float MENU_WIDTH = 420f;
    private static final float MENU_HEIGHT = 240f;

    private Container pauseContainer;
    private Button resumeButton;
    private Button settingsButton;
    private Button saveExitButton;

    private float lastCameraWidth = -1f;
    private float lastCameraHeight = -1f;

    private InputConfig inputConfig;
    private final ActionListener pauseToggleListener;

    public PauseMenuState(ServiceHub serviceHub) {
        this.serviceHub = serviceHub;
        this.pauseToggleListener = this::handlePauseToggle;
    }

    @Override
    protected void initialize(Application app) {
        application = (SimpleApplication) app;
        guiNode = application.getGuiNode();

        initializeLemur(application);
        buildMenu();

        if (serviceHub.has(InputConfig.class)) {
            inputConfig = serviceHub.get(InputConfig.class);
        }
    }

    private void initializeLemur(SimpleApplication app) {
        if (GuiGlobals.getInstance() == null) {
            GuiGlobals.initialize(app);
            BaseStyles.loadGlassStyle();
            GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");
            logger.info("Lemur GUI initialised with glass style (pause menu)");
        }
    }

    private void buildMenu() {
        pauseContainer = new Container(new SpringGridLayout(), new ElementId("pauseMenu"));
        pauseContainer.setPreferredSize(new Vector3f(MENU_WIDTH, MENU_HEIGHT, 0f));
        pauseContainer.addChild(new Label("Paused", new ElementId("title")));

        resumeButton = pauseContainer.addChild(new Button("Resume"));
        resumeButton.addClickCommands((Command<Button>) source -> serviceHub.get(GameStateManager.class).resumeGame());

        settingsButton = pauseContainer.addChild(new Button("Settings"));
        settingsButton.addClickCommands((Command<Button>) source -> {
            SettingsMenuState settings = new SettingsMenuState(serviceHub);
            application.getStateManager().attach(settings);
        });

        saveExitButton = pauseContainer.addChild(new Button("Save & Exit to Menu"));
        saveExitButton.addClickCommands((Command<Button>) source -> serviceHub.get(GameStateManager.class).exitToMainMenu());

        layoutMenu();
    }

    private void layoutMenu() {
        float width = application.getCamera().getWidth();
        float height = application.getCamera().getHeight();
        lastCameraWidth = width;
        lastCameraHeight = height;

        float scale = UIScaleProcessor.getCurrentScale();
        if (!Float.isFinite(scale) || scale <= 0f) {
            scale = 1f;
        }

        float scaledWidth = width / scale;
        float scaledHeight = height / scale;

        pauseContainer.setLocalTranslation(
            scaledWidth / 2f - MENU_WIDTH / 2f,
            scaledHeight / 2f + MENU_HEIGHT / 2f,
            0f
        );
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);

        float width = application.getCamera().getWidth();
        float height = application.getCamera().getHeight();
        if (width != lastCameraWidth || height != lastCameraHeight) {
            layoutMenu();
        }
    }

    @Override
    protected void cleanup(Application app) {
        if (pauseContainer != null) {
            pauseContainer.removeFromParent();
        }
    }

    @Override
    protected void onEnable() {
        if (pauseContainer != null && pauseContainer.getParent() == null) {
            guiNode.attachChild(pauseContainer);
        }
        if (inputConfig != null) {
            inputConfig.registerAction("pause", pauseToggleListener);
        }
    }

    @Override
    protected void onDisable() {
        if (pauseContainer != null) {
            pauseContainer.removeFromParent();
        }
        if (inputConfig != null) {
            inputConfig.unregisterAction("pause");
        }
    }

    private void handlePauseToggle(String name, boolean isPressed, float tpf) {
        if (!"pause".equals(name) || isPressed) {
            return;
        }
        serviceHub.get(GameStateManager.class).resumeGame();
    }
}
