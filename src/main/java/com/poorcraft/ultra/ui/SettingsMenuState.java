package com.poorcraft.ultra.ui;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.scene.Node;
import com.poorcraft.ultra.app.ClientConfig;
import com.poorcraft.ultra.app.ConfigSaver;
import com.poorcraft.ultra.app.ServiceHub;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.style.ElementId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Settings menu overlay with Graphics, Controls, and Audio tabs.
 * Stub implementation for Phase 1.5 - full UI to be expanded.
 */
public class SettingsMenuState extends BaseAppState {
    private static final Logger logger = LoggerFactory.getLogger(SettingsMenuState.class);

    private final ServiceHub serviceHub;

    private SimpleApplication application;
    private Node guiNode;

    private Container settingsContainer;
    private Button applyButton;
    private Button cancelButton;

    private ClientConfig pendingConfig;

    private final ActionListener cancelListener = (name, isPressed, tpf) -> {
        if (!isPressed && "CancelSettings".equals(name)) {
            closeSettings();
        }
    };

    public SettingsMenuState(ServiceHub serviceHub) {
        this.serviceHub = serviceHub;
    }

    @Override
    protected void initialize(Application app) {
        application = (SimpleApplication) app;
        guiNode = application.getGuiNode();

        pendingConfig = serviceHub.getConfig();
        buildMenu();
    }

    private void buildMenu() {
        settingsContainer = new Container(new ElementId("settingsMenu"));
        settingsContainer.addChild(new Label("Settings", new ElementId("title")));

        settingsContainer.addChild(new Label("Graphics, Controls, Audio tabs - stub for Phase 1.5"));
        settingsContainer.addChild(new Label("Full implementation requires additional Lemur widgets"));

        applyButton = settingsContainer.addChild(new Button("Apply"));
        applyButton.addClickCommands((Command<Button>) source -> onApply());

        cancelButton = settingsContainer.addChild(new Button("Cancel"));
        cancelButton.addClickCommands((Command<Button>) source -> closeSettings());

        layoutMenu();
    }

    private void layoutMenu() {
        float width = application.getCamera().getWidth();
        float height = application.getCamera().getHeight();
        settingsContainer.setLocalTranslation(width / 2f - 200f, height / 2f + 200f, 0f);
    }

    private void onApply() {
        ConfigSaver.saveAsync(pendingConfig, "config/client.yaml", success -> {
            if (success) {
                logger.info("Settings saved successfully");
            } else {
                logger.error("Failed to save settings");
            }
        });
        closeSettings();
    }

    private void closeSettings() {
        application.getStateManager().detach(this);
    }

    @Override
    protected void cleanup(Application app) {
        if (settingsContainer != null) {
            settingsContainer.removeFromParent();
        }
    }

    @Override
    protected void onEnable() {
        if (settingsContainer != null && settingsContainer.getParent() == null) {
            guiNode.attachChild(settingsContainer);
        }
        registerCancelMapping();
    }

    @Override
    protected void onDisable() {
        if (settingsContainer != null) {
            settingsContainer.removeFromParent();
        }
        unregisterCancelMapping();
    }

    private void registerCancelMapping() {
        var inputManager = application.getInputManager();
        if (!inputManager.hasMapping("CancelSettings")) {
            inputManager.addMapping("CancelSettings", new KeyTrigger(KeyInput.KEY_ESCAPE));
            inputManager.addListener(cancelListener, "CancelSettings");
        }
    }

    private void unregisterCancelMapping() {
        var inputManager = application.getInputManager();
        if (inputManager.hasMapping("CancelSettings")) {
            inputManager.deleteMapping("CancelSettings");
            inputManager.removeListener(cancelListener);
        }
    }
}
