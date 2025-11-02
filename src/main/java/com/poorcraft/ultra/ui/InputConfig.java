package com.poorcraft.ultra.ui;

import com.jme3.app.SimpleApplication;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.controls.Trigger;
import com.poorcraft.ultra.app.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Manages configurable input mappings, mouse sensitivity, and inversion.
 * Designed for Phase 1.5 UI/input overhaul.
 */
public class InputConfig {
    private static final Logger logger = LoggerFactory.getLogger(InputConfig.class);

    private SimpleApplication application;
    private InputManager inputManager;
    private ClientConfig.ControlsConfig controlsConfig;

    private final Map<String, String> actionMappings = new HashMap<>();
    private final Map<String, ActionListener> actionListeners = new HashMap<>();
    private final Map<String, AnalogListener> analogListeners = new HashMap<>();

    private float mouseSensitivity = 1.5f;
    private boolean invertMouseY = false;

    /**
     * Initialize the service from the loaded configuration.
     */
    public synchronized void init(InputManager manager, ClientConfig.ControlsConfig controls) {
        this.inputManager = Objects.requireNonNull(manager, "inputManager");
        applyConfig(controls != null ? controls : ClientConfig.ControlsConfig.defaults());
    }

    /**
     * Provides the application reference for enqueue operations.
     */
    public synchronized void setApplication(SimpleApplication app) {
        this.application = app;
    }

    /**
     * Registers an action listener for the given action name using current bindings.
     */
    public synchronized void registerAction(String actionName, ActionListener listener) {
        Objects.requireNonNull(actionName, "actionName");
        Objects.requireNonNull(listener, "listener");

        actionListeners.put(actionName, listener);
        if (!actionMappings.containsKey(actionName) && controlsConfig != null) {
            String binding = controlsConfig.keybinds().get(actionName);
            if (binding != null) {
                actionMappings.put(actionName, binding);
            } else {
                logger.warn("No binding configured for action '{}' when registering listener", actionName);
            }
        }
        applyActionMapping(actionName);
    }

    /**
     * Unregisters a previously registered action listener and mapping.
     */
    public synchronized void unregisterAction(String actionName) {
        if (inputManager != null && inputManager.hasMapping(actionName)) {
            inputManager.deleteMapping(actionName);
        }
        ActionListener listener = actionListeners.remove(actionName);
        if (listener != null && inputManager != null) {
            inputManager.removeListener(listener);
        }
    }

    /**
     * Registers an analog listener (e.g., mouse axis) for the given action name.
     */
    public synchronized void registerAnalog(String actionName, AnalogListener listener) {
        Objects.requireNonNull(actionName, "actionName");
        Objects.requireNonNull(listener, "listener");

        analogListeners.put(actionName, listener);
        applyAnalogMapping(actionName);
    }

    /**
     * Unregisters a previously registered analog listener and mapping.
     */
    public synchronized void unregisterAnalog(String actionName) {
        if (inputManager != null && inputManager.hasMapping(actionName)) {
            inputManager.deleteMapping(actionName);
        }
        AnalogListener listener = analogListeners.remove(actionName);
        if (listener != null && inputManager != null) {
            inputManager.removeListener(listener);
        }
    }

    /**
     * Rebinds an action to the given key or mouse token, returning the result asynchronously.
     */
    public CompletableFuture<Boolean> rebindAction(String actionName, String newKeyOrButton) {
        Objects.requireNonNull(actionName, "actionName");
        Objects.requireNonNull(newKeyOrButton, "newKeyOrButton");

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Runnable task = () -> {
            boolean success;
            synchronized (InputConfig.this) {
                success = applyRebind(actionName, newKeyOrButton);
            }
            future.complete(success);
        };

        runOnAppThread(task, future);
        return future;
    }

    /**
     * @return current mouse sensitivity scalar
     */
    public synchronized float getMouseSensitivity() {
        return mouseSensitivity;
    }

    /**
     * Updates mouse sensitivity and persists it in the internal config snapshot.
     */
    public synchronized void setMouseSensitivity(float sensitivity) {
        mouseSensitivity = sensitivity;
        updateControlsSnapshot();
    }

    /**
     * @return whether mouse Y-axis is inverted
     */
    public synchronized boolean isMouseYInverted() {
        return invertMouseY;
    }

    /**
     * Toggles mouse Y-axis inversion and rebuilds axis mappings.
     */
    public synchronized void setMouseYInverted(boolean inverted) {
        invertMouseY = inverted;
        updateControlsSnapshot();
        rebuildAnalogMappings();
    }

    /**
     * Human readable key string for UI display.
     */
    public synchronized String getKeybindString(String actionName) {
        String binding = actionMappings.get(actionName);
        if (binding == null) {
            return "";
        }

        if (binding.startsWith("MOUSE_")) {
            return mouseBindingToLabel(binding);
        }

        Integer keyCode = parseKeyCode(binding);
        return keyCode != null ? keyCodeToString(keyCode) : binding;
    }

    /**
     * Applies a new full controls configuration, rebuilding all mappings.
     */
    public synchronized void applyConfig(ClientConfig.ControlsConfig newConfig) {
        controlsConfig = newConfig != null ? newConfig : ClientConfig.ControlsConfig.defaults();
        mouseSensitivity = controlsConfig.mouseSensitivity();
        invertMouseY = controlsConfig.invertMouseY();

        actionMappings.clear();
        actionMappings.putAll(controlsConfig.keybinds());

        rebuildActionMappings();
        rebuildAnalogMappings();
    }

    /**
     * Current controls snapshot (mutable map returned by record).
     */
    public synchronized ClientConfig.ControlsConfig getControlsConfig() {
        return controlsConfig;
    }

    /**
     * Removes all listeners and mappings managed by this service.
     */
    public synchronized void clearAll() {
        if (inputManager != null) {
            for (String mapping : actionListeners.keySet()) {
                if (inputManager.hasMapping(mapping)) {
                    inputManager.deleteMapping(mapping);
                }
            }
            for (String mapping : analogListeners.keySet()) {
                if (inputManager.hasMapping(mapping)) {
                    inputManager.deleteMapping(mapping);
                }
            }

            actionListeners.values().forEach(inputManager::removeListener);
            analogListeners.values().forEach(inputManager::removeListener);
        }

        actionListeners.clear();
        analogListeners.clear();
    }

    // --- Internal helpers -------------------------------------------------

    private void runOnAppThread(Runnable task, CompletableFuture<Boolean> future) {
        if (application != null) {
            application.enqueue(() -> {
                try {
                    task.run();
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
                return null;
            });
        } else {
            try {
                task.run();
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        }
    }

    private boolean applyRebind(String actionName, String newKeyOrButton) {
        if (inputManager == null) {
            logger.warn("InputManager not initialised; cannot rebind action {}", actionName);
            return false;
        }

        Trigger trigger = buildTriggerForBinding(newKeyOrButton);
        if (trigger == null) {
            logger.warn("Unrecognised keybinding '{}' for action {}", newKeyOrButton, actionName);
            return false;
        }

        if (inputManager.hasMapping(actionName)) {
            inputManager.deleteMapping(actionName);
        }

        inputManager.addMapping(actionName, trigger);
        ActionListener listener = actionListeners.get(actionName);
        if (listener != null) {
            inputManager.removeListener(listener);
            inputManager.addListener(listener, actionName);
        }

        actionMappings.put(actionName, normaliseBinding(newKeyOrButton));
        updateControlsSnapshot();
        logger.info("Rebound action {} to {}", actionName, newKeyOrButton);
        return true;
    }

    private void rebuildActionMappings() {
        if (inputManager == null) {
            return;
        }

        for (Map.Entry<String, ActionListener> entry : actionListeners.entrySet()) {
            applyActionMapping(entry.getKey());
        }
    }

    private void rebuildAnalogMappings() {
        if (inputManager == null) {
            return;
        }

        for (String name : analogListeners.keySet()) {
            applyAnalogMapping(name);
        }
    }

    private void applyActionMapping(String actionName) {
        if (inputManager == null) {
            return;
        }

        String binding = actionMappings.get(actionName);
        if (binding == null) {
            logger.warn("No keybind defined for action {}", actionName);
            return;
        }

        Trigger trigger = buildTriggerForBinding(binding);
        if (trigger == null) {
            logger.warn("Unable to build trigger for action {} binding {}", actionName, binding);
            return;
        }

        if (inputManager.hasMapping(actionName)) {
            inputManager.deleteMapping(actionName);
        }

        inputManager.addMapping(actionName, trigger);
        ActionListener listener = actionListeners.get(actionName);
        if (listener != null) {
            inputManager.removeListener(listener);
            inputManager.addListener(listener, actionName);
        }
    }

    private void applyAnalogMapping(String actionName) {
        if (inputManager == null) {
            return;
        }

        Trigger trigger = switch (actionName) {
            case "MouseX+" -> new MouseAxisTrigger(MouseInput.AXIS_X, false);
            case "MouseX-" -> new MouseAxisTrigger(MouseInput.AXIS_X, true);
            case "MouseY+" -> new MouseAxisTrigger(MouseInput.AXIS_Y, false);
            case "MouseY-" -> new MouseAxisTrigger(MouseInput.AXIS_Y, true);
            default -> null;
        };

        if (trigger == null) {
            logger.warn("Unknown analog mapping: {}", actionName);
            return;
        }

        if (inputManager.hasMapping(actionName)) {
            inputManager.deleteMapping(actionName);
        }

        inputManager.addMapping(actionName, trigger);
        AnalogListener listener = analogListeners.get(actionName);
        if (listener != null) {
            inputManager.removeListener(listener);
            inputManager.addListener(listener, actionName);
        }
    }

    private Trigger buildTriggerForBinding(String binding) {
        String normalised = normaliseBinding(binding);
        if (normalised.startsWith("MOUSE_")) {
            Integer button = parseMouseButton(normalised);
            return button != null ? new MouseButtonTrigger(button) : null;
        }

        Integer keyCode = parseKeyCode(normalised);
        return keyCode != null ? new KeyTrigger(keyCode) : null;
    }

    private String normaliseBinding(String binding) {
        return binding.trim().toUpperCase(Locale.ROOT);
    }

    private Integer parseMouseButton(String token) {
        return switch (token) {
            case "MOUSE_LEFT" -> MouseInput.BUTTON_LEFT;
            case "MOUSE_RIGHT" -> MouseInput.BUTTON_RIGHT;
            case "MOUSE_MIDDLE" -> MouseInput.BUTTON_MIDDLE;
            case "MOUSE_BUTTON3" -> MouseInput.BUTTON_LEFT + 3;
            case "MOUSE_BUTTON4" -> MouseInput.BUTTON_LEFT + 4;
            case "MOUSE_BUTTON5" -> MouseInput.BUTTON_LEFT + 5;
            default -> null;
        };
    }

    private Integer parseKeyCode(String token) {
        String upper = token.toUpperCase(Locale.ROOT);
        if (upper.length() == 1) {
            char ch = upper.charAt(0);
            if (ch >= 'A' && ch <= 'Z') {
                return KeyInput.KEY_A + (ch - 'A');
            }
            if (ch >= '0' && ch <= '9') {
                return (ch == '0') ? KeyInput.KEY_0 : KeyInput.KEY_1 + (ch - '1');
            }
        }

        return switch (upper) {
            case "SPACE" -> KeyInput.KEY_SPACE;
            case "ENTER", "RETURN" -> KeyInput.KEY_RETURN;
            case "ESCAPE", "ESC" -> KeyInput.KEY_ESCAPE;
            case "TAB" -> KeyInput.KEY_TAB;
            case "BACKSPACE" -> KeyInput.KEY_BACK;
            case "DELETE", "DEL" -> KeyInput.KEY_DELETE;
            case "INSERT", "INS" -> KeyInput.KEY_INSERT;
            case "HOME" -> KeyInput.KEY_HOME;
            case "END" -> KeyInput.KEY_END;
            case "PAGEUP", "PGUP" -> KeyInput.KEY_PGUP;
            case "PAGEDOWN", "PGDN" -> KeyInput.KEY_PGDN;
            case "LSHIFT", "SHIFT" -> KeyInput.KEY_LSHIFT;
            case "RSHIFT" -> KeyInput.KEY_RSHIFT;
            case "LCTRL", "CTRL" -> KeyInput.KEY_LCONTROL;
            case "RCTRL" -> KeyInput.KEY_RCONTROL;
            case "ALT", "LALT" -> KeyInput.KEY_LMENU;
            case "RALT" -> KeyInput.KEY_RMENU;
            case "UP" -> KeyInput.KEY_UP;
            case "DOWN" -> KeyInput.KEY_DOWN;
            case "LEFT" -> KeyInput.KEY_LEFT;
            case "RIGHT" -> KeyInput.KEY_RIGHT;
            case "MINUS", "-" -> KeyInput.KEY_MINUS;
            case "EQUALS", "=" -> KeyInput.KEY_EQUALS;
            case "LBRACKET", "[" -> KeyInput.KEY_LBRACKET;
            case "RBRACKET", "]" -> KeyInput.KEY_RBRACKET;
            case "SEMICOLON", ";" -> KeyInput.KEY_SEMICOLON;
            case "APOSTROPHE", "'", "QUOTE" -> KeyInput.KEY_APOSTROPHE;
            case "GRAVE", "`", "BACKTICK" -> KeyInput.KEY_GRAVE;
            case "COMMA", "," -> KeyInput.KEY_COMMA;
            case "PERIOD", ".", "DOT" -> KeyInput.KEY_PERIOD;
            case "SLASH", "/" -> KeyInput.KEY_SLASH;
            case "BACKSLASH", "\\" -> KeyInput.KEY_BACKSLASH;
            case "NUMPAD0" -> KeyInput.KEY_NUMPAD0;
            case "NUMPAD1" -> KeyInput.KEY_NUMPAD1;
            case "NUMPAD2" -> KeyInput.KEY_NUMPAD2;
            case "NUMPAD3" -> KeyInput.KEY_NUMPAD3;
            case "NUMPAD4" -> KeyInput.KEY_NUMPAD4;
            case "NUMPAD5" -> KeyInput.KEY_NUMPAD5;
            case "NUMPAD6" -> KeyInput.KEY_NUMPAD6;
            case "NUMPAD7" -> KeyInput.KEY_NUMPAD7;
            case "NUMPAD8" -> KeyInput.KEY_NUMPAD8;
            case "NUMPAD9" -> KeyInput.KEY_NUMPAD9;
            case "NUMPADENTER" -> KeyInput.KEY_NUMPADENTER;
            case "NUMPADADD" -> KeyInput.KEY_ADD;
            case "NUMPADSUBTRACT" -> KeyInput.KEY_SUBTRACT;
            case "NUMPADMULTIPLY" -> KeyInput.KEY_MULTIPLY;
            case "NUMPADDIVIDE" -> KeyInput.KEY_DIVIDE;
            case "NUMPADDECIMAL", "NUMPADPERIOD" -> KeyInput.KEY_DECIMAL;
            case "F1" -> KeyInput.KEY_F1;
            case "F2" -> KeyInput.KEY_F2;
            case "F3" -> KeyInput.KEY_F3;
            case "F4" -> KeyInput.KEY_F4;
            case "F5" -> KeyInput.KEY_F5;
            case "F6" -> KeyInput.KEY_F6;
            case "F7" -> KeyInput.KEY_F7;
            case "F8" -> KeyInput.KEY_F8;
            case "F9" -> KeyInput.KEY_F9;
            case "F10" -> KeyInput.KEY_F10;
            case "F11" -> KeyInput.KEY_F11;
            case "F12" -> KeyInput.KEY_F12;
            default -> null;
        };
    }

    private String mouseBindingToLabel(String binding) {
        return switch (binding) {
            case "MOUSE_LEFT" -> "Left Mouse";
            case "MOUSE_RIGHT" -> "Right Mouse";
            case "MOUSE_MIDDLE" -> "Middle Mouse";
            default -> binding;
        };
    }

    private String keyCodeToString(int keyCode) {
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
            case KeyInput.KEY_SPACE -> "Space";
            case KeyInput.KEY_RETURN -> "Enter";
            case KeyInput.KEY_NUMPADENTER -> "Numpad Enter";
            case KeyInput.KEY_ESCAPE -> "Escape";
            case KeyInput.KEY_BACK -> "Backspace";
            case KeyInput.KEY_TAB -> "Tab";
            case KeyInput.KEY_DELETE -> "Delete";
            case KeyInput.KEY_INSERT -> "Insert";
            case KeyInput.KEY_HOME -> "Home";
            case KeyInput.KEY_END -> "End";
            case KeyInput.KEY_PGUP -> "Page Up";
            case KeyInput.KEY_PGDN -> "Page Down";
            case KeyInput.KEY_LSHIFT -> "LShift";
            case KeyInput.KEY_RSHIFT -> "RShift";
            case KeyInput.KEY_LCONTROL -> "LCtrl";
            case KeyInput.KEY_RCONTROL -> "RCtrl";
            case KeyInput.KEY_LMENU -> "LAlt";
            case KeyInput.KEY_RMENU -> "RAlt";
            case KeyInput.KEY_UP -> "Up";
            case KeyInput.KEY_DOWN -> "Down";
            case KeyInput.KEY_LEFT -> "Left";
            case KeyInput.KEY_RIGHT -> "Right";
            case KeyInput.KEY_MINUS -> "-";
            case KeyInput.KEY_EQUALS -> "=";
            case KeyInput.KEY_LBRACKET -> "[";
            case KeyInput.KEY_RBRACKET -> "]";
            case KeyInput.KEY_SEMICOLON -> ";";
            case KeyInput.KEY_APOSTROPHE -> "'";
            case KeyInput.KEY_GRAVE -> "`";
            case KeyInput.KEY_COMMA -> ",";
            case KeyInput.KEY_PERIOD -> ".";
            case KeyInput.KEY_SLASH -> "/";
            case KeyInput.KEY_BACKSLASH -> "\\";
            case KeyInput.KEY_NUMPAD0 -> "Numpad 0";
            case KeyInput.KEY_NUMPAD1 -> "Numpad 1";
            case KeyInput.KEY_NUMPAD2 -> "Numpad 2";
            case KeyInput.KEY_NUMPAD3 -> "Numpad 3";
            case KeyInput.KEY_NUMPAD4 -> "Numpad 4";
            case KeyInput.KEY_NUMPAD5 -> "Numpad 5";
            case KeyInput.KEY_NUMPAD6 -> "Numpad 6";
            case KeyInput.KEY_NUMPAD7 -> "Numpad 7";
            case KeyInput.KEY_NUMPAD8 -> "Numpad 8";
            case KeyInput.KEY_NUMPAD9 -> "Numpad 9";
            case KeyInput.KEY_ADD -> "Numpad +";
            case KeyInput.KEY_SUBTRACT -> "Numpad -";
            case KeyInput.KEY_MULTIPLY -> "Numpad *";
            case KeyInput.KEY_DIVIDE -> "Numpad /";
            case KeyInput.KEY_DECIMAL -> "Numpad .";
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
            default -> "KeyCode " + keyCode;
        };
    }

    private void updateControlsSnapshot() {
        controlsConfig = new ClientConfig.ControlsConfig(
            mouseSensitivity,
            invertMouseY,
            new HashMap<>(actionMappings)
        );
    }
}
