package com.poorcraft.ultra.ui;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.scene.Node;
import com.poorcraft.ultra.app.ServiceHub;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
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

    private Container pauseContainer;
    private Button resumeButton;
    private Button settingsButton;
    private Button saveExitButton;

    private final ActionListener resumeListener = (name, isPressed, tpf) -> {
        if (!isPressed && "Resume".equals(name)) {
            serviceHub.get(GameStateManager.class).resumeGame();
        }
    };

    public PauseMenuState(ServiceHub serviceHub) {
        this.serviceHub = serviceHub;
    }

    @Override
    protected void initialize(Application app) {
        application = (SimpleApplication) app;
        guiNode = application.getGuiNode();

        buildMenu();
    }

    private void buildMenu() {
        pauseContainer = new Container(new ElementId("pauseMenu"));
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
        pauseContainer.setLocalTranslation(width / 2f - 150f, height / 2f + 150f, 0f);
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
        registerResumeMapping();
        application.getInputManager().setCursorVisible(true);
    }

    @Override
    protected void onDisable() {
        if (pauseContainer != null) {
            pauseContainer.removeFromParent();
        }
        unregisterResumeMapping();
    }

    private void registerResumeMapping() {
        var inputManager = application.getInputManager();
        if (!inputManager.hasMapping("Resume")) {
            inputManager.addMapping("Resume", new KeyTrigger(KeyInput.KEY_ESCAPE));
            inputManager.addListener(resumeListener, "Resume");
        }
    }

    private void unregisterResumeMapping() {
        var inputManager = application.getInputManager();
        if (inputManager.hasMapping("Resume")) {
            inputManager.deleteMapping("Resume");
            inputManager.removeListener(resumeListener);
        }
    }
}
