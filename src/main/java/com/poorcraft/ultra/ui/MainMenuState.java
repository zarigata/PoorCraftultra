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

        menuContainer.setLocalTranslation(
            scaledWidth / 2f - MENU_WIDTH / 2f,
            scaledHeight / 2f + MENU_HEIGHT / 2f,
            0f
        );
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
        if (menuContainer != null && menuContainer.getParent() == null) {
            guiNode.attachChild(menuContainer);
            guiNode.updateGeometricState();
        }
    }

    @Override
    protected void onDisable() {
        if (menuContainer != null) {
            menuContainer.removeFromParent();
        }
    }
}
