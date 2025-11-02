package com.poorcraft.ultra.ui;

import com.jme3.input.InputManager;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.InputListener;
import com.jme3.input.controls.Trigger;
import com.poorcraft.ultra.app.ClientConfig;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InputConfigTest {

    private InputConfig inputConfig;
    private RecordingInputManager inputManager;

    @BeforeEach
    void setUp() {
        var keyInput = new TestInputDevices.StubKeyInput();
        var mouseInput = new TestInputDevices.StubMouseInput();
        inputManager = new RecordingInputManager(mouseInput, keyInput);
        inputConfig = new InputConfig();
        inputConfig.init(inputManager, null);
    }

    @Test
    void initializationDefaults() {
        assertEquals(1.5f, inputConfig.getMouseSensitivity(), 1e-6);
        assertFalse(inputConfig.isMouseYInverted());

        var binds = inputConfig.getControlsConfig().keybinds();
        assertEquals("W", binds.get("moveForward"));
        assertEquals("LSHIFT", binds.get("sprint"));
    }

    @Test
    void rebindActionUpdatesMappingsAndSnapshot() {
        inputConfig.registerAction("moveForward", (name, pressed, tpf) -> {});

        assertTrue(inputManager.addedMappings.containsKey("moveForward"));
        assertEquals("W", inputConfig.getControlsConfig().keybinds().get("moveForward"));

        var result = inputConfig.rebindAction("moveForward", "E");
        assertTrue(result.join());

        assertEquals("E", inputConfig.getControlsConfig().keybinds().get("moveForward"));
        assertEquals("E", inputConfig.getKeybindString("moveForward"));

        Trigger[] recorded = inputManager.addedMappings.get("moveForward");
        assertNotNull(recorded);
        assertEquals(1, recorded.length);
        assertTrue(inputManager.removedMappings.contains("moveForward"));
    }

    @Test
    void mouseSensitivityChangeIsPersisted() {
        inputConfig.setMouseSensitivity(2.5f);

        assertEquals(2.5f, inputConfig.getMouseSensitivity(), 1e-6);
        assertEquals(2.5f, inputConfig.getControlsConfig().mouseSensitivity(), 1e-6);
    }

    @Test
    void invertYToggleUpdatesSnapshot() {
        inputConfig.setMouseYInverted(true);

        assertTrue(inputConfig.isMouseYInverted());
        assertTrue(inputConfig.getControlsConfig().invertMouseY());

        inputConfig.setMouseYInverted(false);
        assertFalse(inputConfig.isMouseYInverted());
        assertFalse(inputConfig.getControlsConfig().invertMouseY());
    }

    @Test
    void applyConfigOverwritesExistingValues() {
        Map<String, String> overrides = new HashMap<>();
        overrides.put("moveForward", "UP");
        overrides.put("breakBlock", "MOUSE_LEFT");
        ClientConfig.ControlsConfig newConfig = new ClientConfig.ControlsConfig(0.75f, true, overrides);

        inputConfig.applyConfig(newConfig);

        assertEquals(0.75f, inputConfig.getMouseSensitivity(), 1e-6);
        assertTrue(inputConfig.isMouseYInverted());
        assertEquals("UP", inputConfig.getControlsConfig().keybinds().get("moveForward"));
        assertEquals("MOUSE_LEFT", inputConfig.getControlsConfig().keybinds().get("breakBlock"));
    }

    @Test
    void parseKeyStringsCoversKeyboardAndMouseTokens() {
        inputConfig.registerAction("breakBlock", (ActionListener) (n, p, t) -> {});

        assertTrue(inputConfig.rebindAction("breakBlock", "MOUSE_LEFT").join());
        assertEquals("Left Mouse", inputConfig.getKeybindString("breakBlock"));

        assertTrue(inputConfig.rebindAction("breakBlock", "LSHIFT").join());
        String actual = inputConfig.getKeybindString("breakBlock");
        System.out.println("Actual value for LSHIFT: " + actual);
        assertEquals("LShift", actual);

        assertTrue(inputConfig.rebindAction("breakBlock", "F2").join());
        assertEquals("F2", inputConfig.getKeybindString("breakBlock"));
    }

    private static final class RecordingInputManager extends InputManager {
        private final Map<String, Trigger[]> addedMappings = new HashMap<>();
        private final Set<String> removedMappings = new HashSet<>();
        private final Set<ActionListener> registeredActionListeners = new HashSet<>();
        private final Set<AnalogListener> registeredAnalogListeners = new HashSet<>();

        RecordingInputManager(TestInputDevices.StubMouseInput mouse, TestInputDevices.StubKeyInput keys) {
            super(mouse, keys, null, null);
        }

        @Override
        public void addMapping(String mappingName, Trigger... triggers) {
            Objects.requireNonNull(mappingName, "mappingName");
            super.addMapping(mappingName, triggers);
            addedMappings.put(mappingName, triggers);
        }

        @Override
        public void deleteMapping(String mappingName) {
            super.deleteMapping(mappingName);
            removedMappings.add(mappingName);
        }

        @Override
        public void addListener(InputListener listener, String... mappingNames) {
            super.addListener(listener, mappingNames);
            if (listener instanceof ActionListener actionListener) {
                registeredActionListeners.add(actionListener);
            }
            if (listener instanceof AnalogListener analogListener) {
                registeredAnalogListeners.add(analogListener);
            }
        }

        @Override
        public void removeListener(InputListener listener) {
            super.removeListener(listener);
            if (listener instanceof ActionListener actionListener) {
                registeredActionListeners.remove(actionListener);
            }
            if (listener instanceof AnalogListener analogListener) {
                registeredAnalogListeners.remove(analogListener);
            }
        }
    }
}
