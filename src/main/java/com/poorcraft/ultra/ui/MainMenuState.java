package com.poorcraft.ultra.ui;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.scene.Node;
import com.poorcraft.ultra.app.ServiceHub;
import com.simsilica.lemur.BaseStyles;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Label;
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

    private Container menuContainer;
    private Button startButton;
    private Button settingsButton;
    private Button exitButton;

    private final ActionListener escListener = (name, isPressed, tpf) -> {
        if (!isPressed && "MenuExit".equals(name)) {
            application.stop();
        }
    };

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
        menuContainer = new Container(new ElementId("mainMenu"));
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
        menuContainer.setLocalTranslation(width / 2f - 150f, height / 2f + 150f, 0f);
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
        }
        registerEscMapping();
        application.getInputManager().setCursorVisible(true);
    }

    @Override
    protected void onDisable() {
        if (menuContainer != null) {
            menuContainer.removeFromParent();
        }
        unregisterEscMapping();
    }

    private void registerEscMapping() {
        var inputManager = application.getInputManager();
        if (!inputManager.hasMapping("MenuExit")) {
            inputManager.addMapping("MenuExit", new KeyTrigger(KeyInput.KEY_ESCAPE));
            inputManager.addListener(escListener, "MenuExit");
        }
    }

    private void unregisterEscMapping() {
        var inputManager = application.getInputManager();
        if (inputManager.hasMapping("MenuExit")) {
            inputManager.deleteMapping("MenuExit");
            inputManager.removeListener(escListener);
        }
    }
}
