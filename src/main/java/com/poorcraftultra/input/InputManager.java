package com.poorcraftultra.input;

import org.lwjgl.glfw.Callbacks;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Manages input handling for the game using GLFW callbacks.
 * 
 * <p>This class bridges GLFW input events to game actions and provides mouse look integration.
 * It uses a callback-based architecture where:
 * - Key presses/releases update the set of active actions
 * - Mouse movement triggers registered callbacks for camera control
 * - Key bindings can be customized for rebindable controls
 * 
 * <p>Integration with game loop:
 * 1. Create InputManager with window handle
 * 2. Call init() to set up GLFW callbacks
 * 3. Call setMouseLookCallbacks() to register camera control
 * 4. Call lockCursor() to enable mouse look
 * 5. Query isActionActive() during game update
 * 6. Call cleanup() before shutdown
 */
public class InputManager {
    private final long windowHandle;
    private final Map<InputAction, Integer> keyBindings;
    private final Set<InputAction> activeActions;
    
    private boolean cursorLocked;
    private double lastMouseX;
    private double lastMouseY;
    private boolean firstMouse;
    
    private float mouseSensitivity;
    private Consumer<Float> pitchCallback;
    private Consumer<Float> yawCallback;
    
    /**
     * Creates an input manager for the specified window.
     * Initializes default key bindings.
     * 
     * @param windowHandle GLFW window handle
     */
    public InputManager(long windowHandle) {
        this.windowHandle = windowHandle;
        this.keyBindings = new EnumMap<>(InputAction.class);
        this.activeActions = EnumSet.noneOf(InputAction.class);
        this.cursorLocked = false;
        this.firstMouse = true;
        this.mouseSensitivity = 0.1f;
        
        // Initialize default key bindings
        keyBindings.put(InputAction.MOVE_FORWARD, GLFW_KEY_W);
        keyBindings.put(InputAction.MOVE_BACKWARD, GLFW_KEY_S);
        keyBindings.put(InputAction.MOVE_LEFT, GLFW_KEY_A);
        keyBindings.put(InputAction.MOVE_RIGHT, GLFW_KEY_D);
        keyBindings.put(InputAction.JUMP, GLFW_KEY_SPACE);
        keyBindings.put(InputAction.SPRINT, GLFW_KEY_LEFT_SHIFT);
        keyBindings.put(InputAction.CROUCH, GLFW_KEY_LEFT_CONTROL);
        keyBindings.put(InputAction.TOGGLE_CURSOR, GLFW_KEY_ESCAPE);
    }
    
    /**
     * Initializes GLFW callbacks for keyboard and mouse input.
     * Must be called before input can be processed.
     */
    public void init() {
        // Set up key callback
        glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) {
                // Check if this key matches any action binding
                for (Map.Entry<InputAction, Integer> entry : keyBindings.entrySet()) {
                    if (entry.getValue() == key) {
                        InputAction inputAction = entry.getKey();
                        
                        // Handle toggle cursor specially
                        if (inputAction == InputAction.TOGGLE_CURSOR) {
                            toggleCursorLock();
                        } else {
                            activeActions.add(inputAction);
                        }
                        break;
                    }
                }
            } else if (action == GLFW_RELEASE) {
                // Remove action from active set
                for (Map.Entry<InputAction, Integer> entry : keyBindings.entrySet()) {
                    if (entry.getValue() == key) {
                        activeActions.remove(entry.getKey());
                        break;
                    }
                }
            }
        });
        
        // Set up cursor position callback
        glfwSetCursorPosCallback(windowHandle, (window, xpos, ypos) -> {
            if (cursorLocked) {
                if (firstMouse) {
                    lastMouseX = xpos;
                    lastMouseY = ypos;
                    firstMouse = false;
                    return;
                }
                
                double deltaX = xpos - lastMouseX;
                double deltaY = ypos - lastMouseY;
                lastMouseX = xpos;
                lastMouseY = ypos;
                
                // Apply sensitivity and invoke callbacks
                if (yawCallback != null) {
                    yawCallback.accept((float) (deltaX * mouseSensitivity));
                }
                if (pitchCallback != null) {
                    // Invert Y axis for natural mouse look
                    pitchCallback.accept((float) (-deltaY * mouseSensitivity));
                }
            }
        });
    }
    
    /**
     * Locks the cursor to the window and hides it.
     * Enables mouse look mode.
     */
    public void lockCursor() {
        glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        cursorLocked = true;
        firstMouse = true;
    }
    
    /**
     * Unlocks the cursor and makes it visible.
     * Disables mouse look mode.
     */
    public void unlockCursor() {
        glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        cursorLocked = false;
    }
    
    /**
     * Toggles cursor lock state.
     */
    public void toggleCursorLock() {
        if (cursorLocked) {
            unlockCursor();
        } else {
            lockCursor();
        }
    }
    
    /**
     * Checks if an action is currently active (key pressed).
     * 
     * @param action Action to check
     * @return true if the action is active
     */
    public boolean isActionActive(InputAction action) {
        return activeActions.contains(action);
    }
    
    /**
     * Sets the key binding for an action.
     * 
     * @param action Action to rebind
     * @param keyCode GLFW key code
     */
    public void setKeyBinding(InputAction action, int keyCode) {
        keyBindings.put(action, keyCode);
    }
    
    /**
     * Gets the current key binding for an action.
     * 
     * @param action Action to query
     * @return GLFW key code, or null if not bound
     */
    public Integer getKeyBinding(InputAction action) {
        return keyBindings.get(action);
    }
    
    /**
     * Sets the mouse sensitivity multiplier.
     * 
     * @param sensitivity Sensitivity value (default 0.1)
     */
    public void setMouseSensitivity(float sensitivity) {
        this.mouseSensitivity = sensitivity;
    }
    
    /**
     * Gets the current mouse sensitivity.
     * 
     * @return Sensitivity value
     */
    public float getMouseSensitivity() {
        return mouseSensitivity;
    }
    
    /**
     * Registers callbacks for mouse look.
     * 
     * @param pitchCallback Callback for pitch changes (accepts delta in degrees)
     * @param yawCallback Callback for yaw changes (accepts delta in degrees)
     */
    public void setMouseLookCallbacks(Consumer<Float> pitchCallback, Consumer<Float> yawCallback) {
        this.pitchCallback = pitchCallback;
        this.yawCallback = yawCallback;
    }
    
    /**
     * Checks if the cursor is currently locked.
     * 
     * @return true if cursor is locked
     */
    public boolean isCursorLocked() {
        return cursorLocked;
    }
    
    /**
     * Cleans up GLFW callbacks.
     * Should be called before window destruction.
     */
    public void cleanup() {
        Callbacks.glfwFreeCallbacks(windowHandle);
    }
    
    /**
     * Package-private method for testing: simulates a key event.
     * 
     * @param key GLFW key code
     * @param action GLFW action (GLFW_PRESS or GLFW_RELEASE)
     */
    void simulateKeyEvent(int key, int action) {
        if (action == GLFW_PRESS) {
            // Check if this key matches any action binding
            for (Map.Entry<InputAction, Integer> entry : keyBindings.entrySet()) {
                if (entry.getValue() == key) {
                    InputAction inputAction = entry.getKey();
                    
                    // Handle toggle cursor specially
                    if (inputAction == InputAction.TOGGLE_CURSOR) {
                        toggleCursorLock();
                    } else {
                        activeActions.add(inputAction);
                    }
                    break;
                }
            }
        } else if (action == GLFW_RELEASE) {
            // Remove action from active set
            for (Map.Entry<InputAction, Integer> entry : keyBindings.entrySet()) {
                if (entry.getValue() == key) {
                    activeActions.remove(entry.getKey());
                    break;
                }
            }
        }
    }
    
    /**
     * Package-private method for testing: simulates a cursor position event.
     * 
     * @param xpos Cursor X position
     * @param ypos Cursor Y position
     */
    void simulateCursorPosEvent(double xpos, double ypos) {
        if (cursorLocked) {
            if (firstMouse) {
                lastMouseX = xpos;
                lastMouseY = ypos;
                firstMouse = false;
                return;
            }
            
            double deltaX = xpos - lastMouseX;
            double deltaY = ypos - lastMouseY;
            lastMouseX = xpos;
            lastMouseY = ypos;
            
            // Apply sensitivity and invoke callbacks
            if (yawCallback != null) {
                yawCallback.accept((float) (deltaX * mouseSensitivity));
            }
            if (pitchCallback != null) {
                // Invert Y axis for natural mouse look
                pitchCallback.accept((float) (-deltaY * mouseSensitivity));
            }
        }
    }
    
    /**
     * Package-private method for testing: resets the first mouse flag.
     * Allows testing to control when the first mouse event is ignored.
     */
    void resetFirstMouse() {
        firstMouse = true;
    }
}
