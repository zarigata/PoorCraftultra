package com.poorcraft.ultra.ui;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import com.poorcraft.ultra.app.ClientConfig;
import com.poorcraft.ultra.app.ClientConfig.AudioConfig;
import com.poorcraft.ultra.app.ClientConfig.ControlsConfig;
import com.poorcraft.ultra.app.ClientConfig.GraphicsConfig;
import com.poorcraft.ultra.app.ClientConfig.WorldConfig;
import com.poorcraft.ultra.app.ConfigSaver;
import com.poorcraft.ultra.app.ServiceHub;
import com.simsilica.lemur.Axis;
import com.simsilica.lemur.style.BaseStyles;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Checkbox;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.DefaultRangedValueModel;
import com.simsilica.lemur.FillMode;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.Panel;
import com.simsilica.lemur.Slider;
import com.simsilica.lemur.TabbedPanel;
import com.simsilica.lemur.TextField;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.style.ElementId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.simsilica.lemur.core.VersionedReference;
import com.simsilica.lemur.text.DocumentModel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/**
 * Settings menu overlay with Graphics, Controls, and Audio tabs.
 */
public class SettingsMenuState extends BaseAppState {
    private static final Logger logger = LoggerFactory.getLogger(SettingsMenuState.class);

    private final ServiceHub serviceHub;

    private SimpleApplication application;
    private Node guiNode;

    private static final float SETTINGS_WIDTH = 720f;
    private static final float SETTINGS_HEIGHT = 520f;

    private Container settingsContainer;
    private TabbedPanel tabbedPanel;
    private Button applyButton;
    private Button cancelButton;

    private ClientConfig pendingConfig;
    private float pendingMouseSensitivity;
    private boolean pendingInvertMouseY;
    private Map<String, String> pendingKeybinds;

    private String pendingResolution;
    private String originalResolution;
    private String pendingWindowMode;
    private String originalWindowMode;
    private boolean pendingVsync;
    private int pendingFpsLimit;
    private Label graphicsRestartLabel;

    private float pendingMasterVolume;
    private float pendingMusicVolume;
    private float pendingEffectsVolume;
    private boolean pendingMute;

    private boolean graphicsRequiresRestart;

    private KeybindCaptureListener keybindCaptureListener;
    private Button activeRebindButton;

    private static final List<String> WINDOW_MODES = List.of("WINDOWED", "FULLSCREEN", "BORDERLESS");

    private float lastCameraWidth = -1f;
    private float lastCameraHeight = -1f;

    private VersionedReference<DocumentModel> resolutionDocRef;
    private VersionedReference<Double> fpsModelRef;
    private VersionedReference<Double> sensitivityModelRef;
    private VersionedReference<Double> masterVolumeRef;
    private VersionedReference<Double> musicVolumeRef;
    private VersionedReference<Double> effectsVolumeRef;

    private Label fpsValueLabel;
    private Label sensitivityValueLabel;
    private Label masterVolumeLabel;
    private Label musicVolumeLabel;
    private Label effectsVolumeLabel;

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

        initializeLemur(application);

        ClientConfig currentConfig = serviceHub.getOrNull(ClientConfig.class);
        if (currentConfig == null) {
            logger.error("ClientConfig not available in ServiceHub; cannot initialize settings menu");
            throw new IllegalStateException("ClientConfig service required for SettingsMenuState");
        }
        pendingConfig = cloneConfig(currentConfig);

        ControlsConfig controls = pendingConfig.controls();
        pendingMouseSensitivity = controls.mouseSensitivity();
        pendingInvertMouseY = controls.invertMouseY();
        pendingKeybinds = new LinkedHashMap<>(controls.keybinds());

        GraphicsConfig graphics = pendingConfig.graphics();
        pendingResolution = graphics.resolution();
        originalResolution = pendingResolution;
        pendingWindowMode = graphics.windowMode().toUpperCase(Locale.ROOT);
        originalWindowMode = pendingWindowMode;
        pendingVsync = graphics.vsync();
        pendingFpsLimit = graphics.fpsLimit();

        AudioConfig audio = pendingConfig.audio();
        pendingMasterVolume = audio.masterVolume();
        pendingMusicVolume = audio.musicVolume();
        pendingEffectsVolume = audio.effectsVolume();
        pendingMute = audio.mute();

        graphicsRequiresRestart = false;
        buildMenu();
    }

    private void buildMenu() {
        settingsContainer = new Container(new SpringGridLayout(), new ElementId("settingsMenu"));
        settingsContainer.setPreferredSize(new Vector3f(SETTINGS_WIDTH, SETTINGS_HEIGHT, 0f));
        settingsContainer.addChild(new Label("Settings", new ElementId("title")));

        tabbedPanel = settingsContainer.addChild(new TabbedPanel(new ElementId("settingsTabs"), "glass"));
        tabbedPanel.addTab("Graphics", buildGraphicsTab());
        tabbedPanel.addTab("Controls", buildControlsTab());
        tabbedPanel.addTab("Audio", buildAudioTab());

        Container buttonRow = settingsContainer.addChild(new Container(new SpringGridLayout(Axis.X, Axis.Y, FillMode.Even, FillMode.None)));
        applyButton = buttonRow.addChild(new Button("Apply"));
        applyButton.addClickCommands((Command<Button>) source -> onApply());

        cancelButton = buttonRow.addChild(new Button("Cancel"));
        cancelButton.addClickCommands((Command<Button>) source -> closeSettings());

        layoutMenu();
    }

    private Panel buildGraphicsTab() {
        Container container = new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.Last, FillMode.None));

        TextField resolutionField = new TextField(pendingResolution);
        resolutionDocRef = resolutionField.getDocumentModel().createReference();
        container.addChild(createRow("Resolution", resolutionField));

        Button windowModeButton = new Button(formatWindowModeDisplay(pendingWindowMode));
        windowModeButton.addClickCommands((Command<Button>) source -> {
            int index = WINDOW_MODES.indexOf(pendingWindowMode);
            if (index < 0) {
                index = 0;
            }
            index = (index + 1) % WINDOW_MODES.size();
            pendingWindowMode = WINDOW_MODES.get(index);
            windowModeButton.setText(formatWindowModeDisplay(pendingWindowMode));
            recalculateGraphicsRestartRequirement();
        });
        container.addChild(createRow("Window Mode", windowModeButton));

        Checkbox vsyncCheckbox = new Checkbox("Enable");
        vsyncCheckbox.setChecked(pendingVsync);
        vsyncCheckbox.addClickCommands((Command<Button>) source -> pendingVsync = vsyncCheckbox.isChecked());
        container.addChild(createRow("VSync", vsyncCheckbox));

        DefaultRangedValueModel fpsModel = new DefaultRangedValueModel(30, 240, pendingFpsLimit);
        Slider fpsSlider = new Slider(fpsModel);
        fpsSlider.setDelta(5);
        Label fpsValue = new Label(Integer.toString(pendingFpsLimit));
        fpsModelRef = fpsModel.createReference();
        fpsValueLabel = fpsValue; // Store reference for update() method
        Container fpsRow = new Container(new SpringGridLayout(Axis.X, Axis.Y, FillMode.None, FillMode.None));
        fpsRow.addChild(fpsSlider);
        fpsRow.addChild(fpsValue);
        container.addChild(createRow("FPS Limit", fpsRow));

        graphicsRestartLabel = container.addChild(new Label(""));
        updateGraphicsRestartLabel();

        return container;
    }

    private Panel buildControlsTab() {
        Container container = new Container();

        DefaultRangedValueModel sensitivityModel = new DefaultRangedValueModel(0.1, 5.0, pendingMouseSensitivity);
        Slider sensitivitySlider = new Slider(sensitivityModel);
        sensitivitySlider.setDelta(0.05f);
        Label sensitivityValue = new Label(formatFloat(pendingMouseSensitivity));
        sensitivityModelRef = sensitivityModel.createReference();
        sensitivityValueLabel = sensitivityValue; // Store reference
        Container sensitivityRow = new Container(new SpringGridLayout(Axis.X, Axis.Y, FillMode.None, FillMode.None));
        sensitivityRow.addChild(sensitivitySlider);
        sensitivityRow.addChild(sensitivityValue);
        container.addChild(createRow("Mouse Sensitivity", sensitivityRow));

        Checkbox invertMouse = new Checkbox("Invert");
        invertMouse.setChecked(pendingInvertMouseY);
        invertMouse.addClickCommands((Command<Button>) source -> pendingInvertMouseY = invertMouse.isChecked());
        container.addChild(createRow("Mouse Y", invertMouse));

        container.addChild(new Label("Keybinds"));
        container.addChild(buildKeybindsList());

        return container;
    }

    private Container buildKeybindsList() {
        Container listContainer = new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.Last, FillMode.None));
        listContainer.addChild(new Label("Action"));
        listContainer.addChild(new Label("Binding"));
        listContainer.addChild(new Label(""));

        pendingKeybinds.forEach((action, binding) -> {
            listContainer.addChild(new Label(action));
            Label bindingLabel = new Label(tokenToDisplay(binding));
            listContainer.addChild(bindingLabel);

            Button rebindButton = new Button("Rebind");
            rebindButton.addClickCommands((Command<Button>) source -> startKeybindCapture(action, bindingLabel, rebindButton));
            listContainer.addChild(rebindButton);
        });

        return listContainer;
    }

    private Panel buildAudioTab() {
        Container container = new Container();

        DefaultRangedValueModel masterModel = new DefaultRangedValueModel(0f, 1f, pendingMasterVolume);
        Slider masterSlider = new Slider(masterModel);
        masterSlider.setDelta(0.05f);
        Label masterValue = new Label(formatPercentage(pendingMasterVolume));
        masterVolumeRef = masterModel.createReference();
        masterVolumeLabel = masterValue; // Store reference
        Container masterRow = new Container(new SpringGridLayout(Axis.X, Axis.Y, FillMode.None, FillMode.None));
        masterRow.addChild(masterSlider);
        masterRow.addChild(masterValue);
        container.addChild(createRow("Master Volume", masterRow));

        DefaultRangedValueModel musicModel = new DefaultRangedValueModel(0f, 1f, pendingMusicVolume);
        Slider musicSlider = new Slider(musicModel);
        musicSlider.setDelta(0.05f);
        Label musicValue = new Label(formatPercentage(pendingMusicVolume));
        musicVolumeRef = musicModel.createReference();
        musicVolumeLabel = musicValue; // Store reference
        Container musicRow = new Container(new SpringGridLayout(Axis.X, Axis.Y, FillMode.None, FillMode.None));
        musicRow.addChild(musicSlider);
        musicRow.addChild(musicValue);
        container.addChild(createRow("Music Volume", musicRow));

        DefaultRangedValueModel effectsModel = new DefaultRangedValueModel(0f, 1f, pendingEffectsVolume);
        Slider effectsSlider = new Slider(effectsModel);
        effectsSlider.setDelta(0.05f);
        Label effectsValue = new Label(formatPercentage(pendingEffectsVolume));
        effectsVolumeRef = effectsModel.createReference();
        effectsVolumeLabel = effectsValue; // Store reference
        Container effectsRow = new Container(new SpringGridLayout(Axis.X, Axis.Y, FillMode.None, FillMode.None));
        effectsRow.addChild(effectsSlider);
        effectsRow.addChild(effectsValue);
        container.addChild(createRow("Effects Volume", effectsRow));

        Checkbox muteCheckbox = new Checkbox("Mute");
        muteCheckbox.setChecked(pendingMute);
        muteCheckbox.addClickCommands((Command<Button>) source -> pendingMute = muteCheckbox.isChecked());
        container.addChild(createRow("Mute", muteCheckbox));

        return container;
    }

    private Container createRow(String labelText, Panel control) {
        Container row = new Container(new SpringGridLayout(Axis.X, Axis.Y, FillMode.Last, FillMode.None));
        row.addChild(new Label(labelText));
        row.addChild(control);
        return row;
    }

    private void layoutMenu() {
        float width = application.getCamera().getWidth();
        float height = application.getCamera().getHeight();

        float scale = UIScaleProcessor.getCurrentScale();
        if (!Float.isFinite(scale) || scale <= 0f) {
            scale = 1f;
        }

        float scaledWidth = width / scale;
        float scaledHeight = height / scale;

        settingsContainer.setLocalTranslation(
            scaledWidth / 2f - SETTINGS_WIDTH / 2f,
            scaledHeight / 2f + SETTINGS_HEIGHT / 2f,
            0f
        );
        lastCameraWidth = width;
        lastCameraHeight = height;
    }

    private void onApply() {
        cancelKeybindCapture();

        ControlsConfig newControls = new ControlsConfig(
            pendingMouseSensitivity,
            pendingInvertMouseY,
            new LinkedHashMap<>(pendingKeybinds)
        );

        GraphicsConfig newGraphics = new GraphicsConfig(
            pendingWindowMode,
            pendingResolution,
            pendingVsync,
            pendingFpsLimit
        );

        AudioConfig newAudio = new AudioConfig(
            pendingMasterVolume,
            pendingMusicVolume,
            pendingEffectsVolume,
            pendingMute
        );

        Resolution resolution = parseResolution(pendingResolution, pendingConfig.displayWidth(), pendingConfig.displayHeight());
        boolean fullscreen = isFullscreenMode(pendingWindowMode);

        pendingConfig = new ClientConfig(
            resolution.width(),
            resolution.height(),
            fullscreen,
            pendingVsync,
            pendingFpsLimit,
            pendingConfig.logLevel(),
            pendingConfig.loadMultiChunk(),
            cloneWorldConfig(pendingConfig.worlds()),
            newControls,
            newGraphics,
            newAudio
        );

        serviceHub.register(ClientConfig.class, pendingConfig);

        InputConfig inputConfigService = serviceHub.getOrNull(InputConfig.class);
        if (inputConfigService != null) {
            try {
                inputConfigService.applyConfig(newControls);
                logger.info("Applied new controls configuration to InputConfig");
            } catch (Exception ex) {
                logger.error("Failed to apply controls configuration", ex);
                logger.warn("Controls changes will take effect on next restart if runtime apply fails");
            }
        } else {
            logger.warn("InputConfig service not available; controls changes will apply on next restart");
        }

        if (graphicsRequiresRestart) {
            logger.info("Graphics configuration saved; restart required for resolution/window mode changes");
        } else {
            applyGraphicsSettings(newGraphics);
        }

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
        cancelKeybindCapture();
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
        cancelKeybindCapture();
        unregisterCancelMapping();
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);
        float width = application.getCamera().getWidth();
        float height = application.getCamera().getHeight();
        if (Math.abs(width - lastCameraWidth) > 0.1f || Math.abs(height - lastCameraHeight) > 0.1f) {
            layoutMenu();
        }

        // Poll VersionedReferences for model changes (Lemur pattern)
        if (resolutionDocRef != null && resolutionDocRef.update()) {
            pendingResolution = resolutionDocRef.get().getText().trim();
            recalculateGraphicsRestartRequirement();
        }

        if (fpsModelRef != null && fpsModelRef.update() && fpsValueLabel != null) {
            pendingFpsLimit = Math.round(fpsModelRef.get().floatValue());
            fpsValueLabel.setText(Integer.toString(pendingFpsLimit));
        }

        if (sensitivityModelRef != null && sensitivityModelRef.update() && sensitivityValueLabel != null) {
            pendingMouseSensitivity = sensitivityModelRef.get().floatValue();
            sensitivityValueLabel.setText(formatFloat(pendingMouseSensitivity));
        }

        if (masterVolumeRef != null && masterVolumeRef.update() && masterVolumeLabel != null) {
            pendingMasterVolume = masterVolumeRef.get().floatValue();
            masterVolumeLabel.setText(formatPercentage(pendingMasterVolume));
        }

        if (musicVolumeRef != null && musicVolumeRef.update() && musicVolumeLabel != null) {
            pendingMusicVolume = musicVolumeRef.get().floatValue();
            musicVolumeLabel.setText(formatPercentage(pendingMusicVolume));
        }

        if (effectsVolumeRef != null && effectsVolumeRef.update() && effectsVolumeLabel != null) {
            pendingEffectsVolume = effectsVolumeRef.get().floatValue();
            effectsVolumeLabel.setText(formatPercentage(pendingEffectsVolume));
        }
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

    private void initializeLemur(SimpleApplication app) {
        if (GuiGlobals.getInstance() == null) {
            GuiGlobals.initialize(app);
            BaseStyles.loadGlassStyle();
            GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");
        }
    }

    private void startKeybindCapture(String action, Label bindingLabel, Button rebindButton) {
        cancelKeybindCapture();
        activeRebindButton = rebindButton;
        if (applyButton != null) {
            applyButton.setEnabled(false);
        }
        if (cancelButton != null) {
            cancelButton.setEnabled(false);
        }
        if (rebindButton != null) {
            rebindButton.setEnabled(false);
        }
        bindingLabel.setText("Press a key or mouse button...");
        keybindCaptureListener = new KeybindCaptureListener(action, bindingLabel);
        application.getInputManager().addRawInputListener(keybindCaptureListener);
    }

    private void cancelKeybindCapture() {
        if (keybindCaptureListener != null) {
            application.getInputManager().removeRawInputListener(keybindCaptureListener);
            keybindCaptureListener.restorePrevious();
            keybindCaptureListener = null;
        }
        if (activeRebindButton != null) {
            activeRebindButton.setEnabled(true);
            activeRebindButton = null;
        }
        if (applyButton != null) {
            applyButton.setEnabled(true);
        }
        if (cancelButton != null) {
            cancelButton.setEnabled(true);
        }
    }

    private void finishKeybindCapture() {
        if (keybindCaptureListener != null) {
            application.getInputManager().removeRawInputListener(keybindCaptureListener);
            keybindCaptureListener = null;
        }
        if (activeRebindButton != null) {
            activeRebindButton.setEnabled(true);
            activeRebindButton = null;
        }
        if (applyButton != null) {
            applyButton.setEnabled(true);
        }
        if (cancelButton != null) {
            cancelButton.setEnabled(true);
        }
    }

    private void recalculateGraphicsRestartRequirement() {
        graphicsRequiresRestart = !pendingResolution.equalsIgnoreCase(originalResolution)
            || !pendingWindowMode.equalsIgnoreCase(originalWindowMode);
        updateGraphicsRestartLabel();
    }

    private void updateGraphicsRestartLabel() {
        if (graphicsRestartLabel == null) {
            return;
        }
        graphicsRestartLabel.setText(graphicsRequiresRestart
            ? "Resolution/window mode changes apply after restart"
            : "");
    }

    private void applyGraphicsSettings(GraphicsConfig graphicsConfig) {
        try {
            var context = application.getContext();
            if (context == null) {
                return;
            }
            AppSettings settings = context.getSettings();
            if (settings == null) {
                return;
            }
            settings.setVSync(graphicsConfig.vsync());
            settings.setFrameRate(graphicsConfig.fpsLimit());
            context.setSettings(settings);
            logger.info("Applied graphics settings (vsync={}, fpsLimit={})", graphicsConfig.vsync(), graphicsConfig.fpsLimit());
        } catch (Exception ex) {
            logger.warn("Failed to apply graphics settings immediately", ex);
            graphicsRequiresRestart = true;
            updateGraphicsRestartLabel();
        }
    }

    private static boolean isFullscreenMode(String windowMode) {
        return "FULLSCREEN".equalsIgnoreCase(windowMode) || "BORDERLESS".equalsIgnoreCase(windowMode);
    }

    private static String formatWindowModeDisplay(String mode) {
        return switch (mode.toUpperCase(Locale.ROOT)) {
            case "FULLSCREEN" -> "Fullscreen";
            case "BORDERLESS" -> "Borderless";
            default -> "Windowed";
        };
    }

    private static String formatFloat(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String formatPercentage(float value) {
        return Math.round(value * 100f) + "%";
    }

    private Resolution parseResolution(String text, int fallbackWidth, int fallbackHeight) {
        if (text == null || text.isBlank()) {
            return new Resolution(fallbackWidth, fallbackHeight);
        }
        String[] parts = text.toLowerCase(Locale.ROOT).split("x");
        if (parts.length < 2) {
            logger.warn("Invalid resolution '{}', using fallback", text);
            return new Resolution(fallbackWidth, fallbackHeight);
        }
        try {
            int width = Integer.parseInt(parts[0].trim());
            int height = Integer.parseInt(parts[1].trim());
            if (width <= 0 || height <= 0) {
                throw new NumberFormatException("Resolution dimensions must be positive");
            }
            return new Resolution(width, height);
        } catch (NumberFormatException ex) {
            logger.warn("Failed to parse resolution '{}': {}", text, ex.getMessage());
            return new Resolution(fallbackWidth, fallbackHeight);
        }
    }

    private ClientConfig cloneConfig(ClientConfig source) {
        return new ClientConfig(
            source.displayWidth(),
            source.displayHeight(),
            source.fullscreen(),
            source.vsync(),
            source.fpsLimit(),
            source.logLevel(),
            source.loadMultiChunk(),
            cloneWorldConfig(source.worlds()),
            cloneControlsConfig(source.controls()),
            cloneGraphicsConfig(source.graphics()),
            cloneAudioConfig(source.audio())
        );
    }

    private WorldConfig cloneWorldConfig(WorldConfig worldConfig) {
        return worldConfig != null ? new WorldConfig(worldConfig.baseDir(), worldConfig.seed()) : null;
    }

    private ControlsConfig cloneControlsConfig(ControlsConfig controlsConfig) {
        return controlsConfig != null
            ? new ControlsConfig(controlsConfig.mouseSensitivity(), controlsConfig.invertMouseY(), new LinkedHashMap<>(controlsConfig.keybinds()))
            : ClientConfig.ControlsConfig.defaults();
    }

    private GraphicsConfig cloneGraphicsConfig(GraphicsConfig graphicsConfig) {
        return graphicsConfig != null
            ? new GraphicsConfig(graphicsConfig.windowMode(), graphicsConfig.resolution(), graphicsConfig.vsync(), graphicsConfig.fpsLimit())
            : ClientConfig.GraphicsConfig.defaults();
    }

    private AudioConfig cloneAudioConfig(AudioConfig audioConfig) {
        return audioConfig != null
            ? new AudioConfig(audioConfig.masterVolume(), audioConfig.musicVolume(), audioConfig.effectsVolume(), audioConfig.mute())
            : ClientConfig.AudioConfig.defaults();
    }

    private String keyCodeToToken(int keyCode) {
        if (keyCode >= KeyInput.KEY_A && keyCode <= KeyInput.KEY_Z) {
            return String.valueOf((char) ('A' + (keyCode - KeyInput.KEY_A)));
        }
        if (keyCode >= KeyInput.KEY_1 && keyCode <= KeyInput.KEY_9) {
            return String.valueOf((char) ('1' + (keyCode - KeyInput.KEY_1)));
        }
        if (keyCode == KeyInput.KEY_0) {
            return "0";
        }
        return switch (keyCode) {
            case KeyInput.KEY_SPACE -> "SPACE";
            case KeyInput.KEY_RETURN -> "ENTER";
            case KeyInput.KEY_NUMPADENTER -> "NUMPADENTER";
            case KeyInput.KEY_ESCAPE -> "ESCAPE";
            case KeyInput.KEY_BACK -> "BACKSPACE";
            case KeyInput.KEY_TAB -> "TAB";
            case KeyInput.KEY_DELETE -> "DELETE";
            case KeyInput.KEY_INSERT -> "INSERT";
            case KeyInput.KEY_HOME -> "HOME";
            case KeyInput.KEY_END -> "END";
            case KeyInput.KEY_PGUP -> "PAGEUP";
            case KeyInput.KEY_PGDN -> "PAGEDOWN";
            case KeyInput.KEY_LSHIFT -> "LSHIFT";
            case KeyInput.KEY_RSHIFT -> "RSHIFT";
            case KeyInput.KEY_LCONTROL -> "LCTRL";
            case KeyInput.KEY_RCONTROL -> "RCTRL";
            case KeyInput.KEY_LMENU -> "LALT";
            case KeyInput.KEY_RMENU -> "RALT";
            case KeyInput.KEY_UP -> "UP";
            case KeyInput.KEY_DOWN -> "DOWN";
            case KeyInput.KEY_LEFT -> "LEFT";
            case KeyInput.KEY_RIGHT -> "RIGHT";
            case KeyInput.KEY_MINUS -> "MINUS";
            case KeyInput.KEY_EQUALS -> "EQUALS";
            case KeyInput.KEY_LBRACKET -> "LBRACKET";
            case KeyInput.KEY_RBRACKET -> "RBRACKET";
            case KeyInput.KEY_SEMICOLON -> "SEMICOLON";
            case KeyInput.KEY_APOSTROPHE -> "APOSTROPHE";
            case KeyInput.KEY_GRAVE -> "GRAVE";
            case KeyInput.KEY_COMMA -> "COMMA";
            case KeyInput.KEY_PERIOD -> "PERIOD";
            case KeyInput.KEY_SLASH -> "SLASH";
            case KeyInput.KEY_BACKSLASH -> "BACKSLASH";
            case KeyInput.KEY_F1 -> "F1";
            case KeyInput.KEY_F2 -> "F2";
            case KeyInput.KEY_F3 -> "F3";
            case KeyInput.KEY_F4 -> "F4";
            case KeyInput.KEY_F5 -> "F5";
            case KeyInput.KEY_F6 -> "F6";
            case KeyInput.KEY_F7 -> "F7";
            case KeyInput.KEY_F8 -> "F8";
            case KeyInput.KEY_F9 -> "F9";
            case KeyInput.KEY_F10 -> "F10";
            case KeyInput.KEY_F11 -> "F11";
            case KeyInput.KEY_F12 -> "F12";
            default -> null;
        };
    }

    private static String mouseButtonToToken(int buttonIndex) {
        return switch (buttonIndex) {
            case MouseInput.BUTTON_LEFT -> "MOUSE_LEFT";
            case MouseInput.BUTTON_RIGHT -> "MOUSE_RIGHT";
            case MouseInput.BUTTON_MIDDLE -> "MOUSE_MIDDLE";
            case MouseInput.BUTTON_LEFT + 3 -> "MOUSE_BUTTON3";
            case MouseInput.BUTTON_LEFT + 4 -> "MOUSE_BUTTON4";
            case MouseInput.BUTTON_LEFT + 5 -> "MOUSE_BUTTON5";
            default -> null;
        };
    }

    private String tokenToDisplay(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }
        if (token.startsWith("MOUSE_")) {
            return switch (token) {
                case "MOUSE_LEFT" -> "Left Mouse";
                case "MOUSE_RIGHT" -> "Right Mouse";
                case "MOUSE_MIDDLE" -> "Middle Mouse";
                case "MOUSE_BUTTON3" -> "Mouse Button 3";
                case "MOUSE_BUTTON4" -> "Mouse Button 4";
                case "MOUSE_BUTTON5" -> "Mouse Button 5";
                default -> token.replace('_', ' ');
            };
        }
        return switch (token) {
            case "LSHIFT" -> "Left Shift";
            case "RSHIFT" -> "Right Shift";
            case "LCTRL" -> "Left Ctrl";
            case "RCTRL" -> "Right Ctrl";
            case "LALT" -> "Left Alt";
            case "RALT" -> "Right Alt";
            case "ESCAPE" -> "Escape";
            case "ENTER" -> "Enter";
            case "NUMPADENTER" -> "Numpad Enter";
            case "BACKSPACE" -> "Backspace";
            case "PAGEUP" -> "Page Up";
            case "PAGEDOWN" -> "Page Down";
            case "SEMICOLON" -> ";";
            case "APOSTROPHE" -> "'";
            case "GRAVE" -> "`";
            case "COMMA" -> ",";
            case "PERIOD" -> ".";
            case "SLASH" -> "/";
            case "BACKSLASH" -> "\\";
            case "MINUS" -> "-";
            case "EQUALS" -> "=";
            case "LBRACKET" -> "[";
            case "RBRACKET" -> "]";
            default -> normaliseTokenDisplay(token);
        };
    }

    private static String normaliseTokenDisplay(String token) {
        if (token.length() == 1) {
            return token.toUpperCase(Locale.ROOT);
        }
        String[] parts = token.toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(parts[i].charAt(0)))
                .append(parts[i].substring(1));
        }
        return builder.toString();
    }

    private record Resolution(int width, int height) {
    }

    private final class KeybindCaptureListener implements RawInputListener {
        private final String action;
        private final Label bindingLabel;
        private final String previousToken;

        private KeybindCaptureListener(String action, Label bindingLabel) {
            this.action = action;
            this.bindingLabel = bindingLabel;
            this.previousToken = pendingKeybinds.getOrDefault(action, "");
        }

        private void restorePrevious() {
            String token = pendingKeybinds.getOrDefault(action, previousToken);
            bindingLabel.setText(tokenToDisplay(token));
        }

        @Override
        public void onKeyEvent(KeyInputEvent evt) {
            if (!evt.isPressed()) {
                return;
            }
            String token = keyCodeToToken(evt.getKeyCode());
            if (token != null) {
                pendingKeybinds.put(action, token);
                bindingLabel.setText(tokenToDisplay(token));
                finishKeybindCapture();
            } else {
                logger.warn("Unsupported key code {} for action {}", evt.getKeyCode(), action);
                restorePrevious();
                finishKeybindCapture();
            }
            evt.setConsumed();
        }

        @Override
        public void onMouseButtonEvent(MouseButtonEvent evt) {
            if (!evt.isPressed()) {
                return;
            }
            String token = mouseButtonToToken(evt.getButtonIndex());
            if (token != null) {
                pendingKeybinds.put(action, token);
                bindingLabel.setText(tokenToDisplay(token));
                finishKeybindCapture();
            } else {
                logger.warn("Unsupported mouse button {} for action {}", evt.getButtonIndex(), action);
                restorePrevious();
                finishKeybindCapture();
            }
            evt.setConsumed();
        }

        @Override
        public void onMouseMotionEvent(MouseMotionEvent evt) {
            // No-op
        }

        @Override
        public void onJoyAxisEvent(JoyAxisEvent evt) {
            // Not supported for keybind capture
        }

        @Override
        public void onJoyButtonEvent(JoyButtonEvent evt) {
            // Not supported for keybind capture
        }

        @Override
        public void onTouchEvent(TouchEvent evt) {
            // Not used
        }

        @Override
        public void beginInput() {
            // No-op
        }

        @Override
        public void endInput() {
            // No-op
        }
    }
}
