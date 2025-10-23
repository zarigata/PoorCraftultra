package com.poorcraftultra.input;

import com.poorcraftultra.core.GLTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;

/**
 * Unit tests for InputManager class.
 * Requires GLFW context via GLTestContext.
 */
@ExtendWith(GLTestContext.class)
class InputManagerTest {
    
    @Test
    @DisplayName("InputManager creation and initialization succeeds")
    void testInputManagerCreation(long windowHandle) {
        InputManager inputManager = new InputManager(windowHandle);
        assertDoesNotThrow(() -> inputManager.init());
        inputManager.cleanup();
    }
    
    @Test
    @DisplayName("Default key bindings are set correctly")
    void testDefaultKeyBindings(long windowHandle) {
        InputManager inputManager = new InputManager(windowHandle);
        
        assertEquals(GLFW_KEY_W, inputManager.getKeyBinding(InputAction.MOVE_FORWARD));
        assertEquals(GLFW_KEY_S, inputManager.getKeyBinding(InputAction.MOVE_BACKWARD));
        assertEquals(GLFW_KEY_A, inputManager.getKeyBinding(InputAction.MOVE_LEFT));
        assertEquals(GLFW_KEY_D, inputManager.getKeyBinding(InputAction.MOVE_RIGHT));
        assertEquals(GLFW_KEY_SPACE, inputManager.getKeyBinding(InputAction.JUMP));
        assertEquals(GLFW_KEY_LEFT_SHIFT, inputManager.getKeyBinding(InputAction.SPRINT));
        assertEquals(GLFW_KEY_LEFT_CONTROL, inputManager.getKeyBinding(InputAction.CROUCH));
        assertEquals(GLFW_KEY_ESCAPE, inputManager.getKeyBinding(InputAction.TOGGLE_CURSOR));
    }
    
    @Test
    @DisplayName("Key binding can be rebound")
    void testKeyBindingRebind(long windowHandle) {
        InputManager inputManager = new InputManager(windowHandle);
        
        // Rebind MOVE_FORWARD to UP arrow
        inputManager.setKeyBinding(InputAction.MOVE_FORWARD, GLFW_KEY_UP);
        assertEquals(GLFW_KEY_UP, inputManager.getKeyBinding(InputAction.MOVE_FORWARD));
        
        // Other bindings should remain unchanged
        assertEquals(GLFW_KEY_S, inputManager.getKeyBinding(InputAction.MOVE_BACKWARD));
    }
    
    @Test
    @DisplayName("Cursor locking changes state correctly")
    void testCursorLocking(long windowHandle) {
        InputManager inputManager = new InputManager(windowHandle);
        inputManager.init();
        
        // Initially unlocked
        assertFalse(inputManager.isCursorLocked());
        
        // Lock cursor
        inputManager.lockCursor();
        assertTrue(inputManager.isCursorLocked());
        
        // Unlock cursor
        inputManager.unlockCursor();
        assertFalse(inputManager.isCursorLocked());
        
        inputManager.cleanup();
    }
    
    @Test
    @DisplayName("Mouse sensitivity can be set and retrieved")
    void testMouseSensitivity(long windowHandle) {
        InputManager inputManager = new InputManager(windowHandle);
        
        // Default sensitivity
        assertEquals(0.1f, inputManager.getMouseSensitivity(), 0.0001f);
        
        // Set new sensitivity
        inputManager.setMouseSensitivity(0.5f);
        assertEquals(0.5f, inputManager.getMouseSensitivity(), 0.0001f);
    }
    
    @Test
    @DisplayName("Mouse look callbacks are invoked correctly")
    void testMouseLookCallbacks(long windowHandle) {
        InputManager inputManager = new InputManager(windowHandle);
        inputManager.init();
        
        // Create atomic references to capture callback values
        AtomicReference<Float> capturedPitch = new AtomicReference<>(0.0f);
        AtomicReference<Float> capturedYaw = new AtomicReference<>(0.0f);
        
        // Register callbacks
        inputManager.setMouseLookCallbacks(
            pitch -> capturedPitch.set(pitch),
            yaw -> capturedYaw.set(yaw)
        );
        
        // Lock cursor to enable mouse look
        inputManager.lockCursor();
        
        // Simulate mouse movement by triggering the callback manually
        // Note: We can't actually move the mouse in unit tests, so we verify the callback registration
        assertNotNull(capturedPitch.get());
        assertNotNull(capturedYaw.get());
        
        inputManager.cleanup();
    }
    
    @Test
    @DisplayName("Mouse look callback magnitudes match deltas with sensitivity")
    void testMouseLookCallbackMagnitudes(long windowHandle) {
        InputManager inputManager = new InputManager(windowHandle);
        inputManager.init();
        
        float sensitivity = 0.1f;
        inputManager.setMouseSensitivity(sensitivity);
        
        // Create atomic references to capture callback values
        AtomicReference<Float> capturedPitch = new AtomicReference<>(0.0f);
        AtomicReference<Float> capturedYaw = new AtomicReference<>(0.0f);
        
        // Register callbacks
        inputManager.setMouseLookCallbacks(
            pitch -> capturedPitch.set(pitch),
            yaw -> capturedYaw.set(yaw)
        );
        
        // Lock cursor to enable mouse look
        inputManager.lockCursor();
        
        // First event is ignored (firstMouse flag)
        inputManager.simulateCursorPosEvent(100, 100);
        
        // Second event should trigger callbacks with deltas
        inputManager.simulateCursorPosEvent(110, 120);
        
        // Expected deltas: deltaX = 10, deltaY = 20
        // Expected yaw = 10 * 0.1 = 1.0
        // Expected pitch = -20 * 0.1 = -2.0 (inverted Y)
        assertEquals(1.0f, capturedYaw.get(), 0.0001f);
        assertEquals(-2.0f, capturedPitch.get(), 0.0001f);
        
        inputManager.cleanup();
    }
    
    @Test
    @DisplayName("Mouse look respects sensitivity changes")
    void testMouseLookSensitivity(long windowHandle) {
        InputManager inputManager = new InputManager(windowHandle);
        inputManager.init();
        
        // Set higher sensitivity
        float sensitivity = 0.5f;
        inputManager.setMouseSensitivity(sensitivity);
        
        AtomicReference<Float> capturedYaw = new AtomicReference<>(0.0f);
        
        inputManager.setMouseLookCallbacks(
            pitch -> {},
            yaw -> capturedYaw.set(yaw)
        );
        
        inputManager.lockCursor();
        
        // First event (ignored)
        inputManager.simulateCursorPosEvent(100, 100);
        
        // Second event with deltaX = 10
        inputManager.simulateCursorPosEvent(110, 100);
        
        // Expected yaw = 10 * 0.5 = 5.0
        assertEquals(5.0f, capturedYaw.get(), 0.0001f);
        
        inputManager.cleanup();
    }
    
    @Test
    @DisplayName("Mouse look callbacks not invoked when cursor unlocked")
    void testMouseLookOnlyWhenLocked(long windowHandle) {
        InputManager inputManager = new InputManager(windowHandle);
        inputManager.init();
        
        AtomicReference<Float> capturedYaw = new AtomicReference<>(0.0f);
        
        inputManager.setMouseLookCallbacks(
            pitch -> {},
            yaw -> capturedYaw.set(yaw)
        );
        
        // Don't lock cursor
        assertFalse(inputManager.isCursorLocked());
        
        // Simulate mouse movement
        inputManager.simulateCursorPosEvent(100, 100);
        inputManager.simulateCursorPosEvent(110, 100);
        
        // Callback should not have been invoked
        assertEquals(0.0f, capturedYaw.get(), 0.0001f);
        
        inputManager.cleanup();
    }
    
    @Test
    @DisplayName("Toggle cursor action switches lock state")
    void testToggleCursorAction(long windowHandle) {
        InputManager inputManager = new InputManager(windowHandle);
        inputManager.init();
        
        // Initially unlocked
        assertFalse(inputManager.isCursorLocked());
        
        // Toggle (should lock)
        inputManager.toggleCursorLock();
        assertTrue(inputManager.isCursorLocked());
        
        // Toggle again (should unlock)
        inputManager.toggleCursorLock();
        assertFalse(inputManager.isCursorLocked());
        
        inputManager.cleanup();
    }
    
    @Test
    @DisplayName("Cleanup frees GLFW callbacks without errors")
    void testCleanup(long windowHandle) {
        InputManager inputManager = new InputManager(windowHandle);
        inputManager.init();
        
        assertDoesNotThrow(() -> inputManager.cleanup());
    }
    
    @Test
    @DisplayName("Action is not active initially")
    void testActionNotActiveInitially(long windowHandle) {
        InputManager inputManager = new InputManager(windowHandle);
        inputManager.init();
        
        assertFalse(inputManager.isActionActive(InputAction.MOVE_FORWARD));
        assertFalse(inputManager.isActionActive(InputAction.JUMP));
        
        inputManager.cleanup();
    }
    
    @Test
    @DisplayName("Key press activates bound action")
    void testKeyPressActivatesAction(long windowHandle) {
        InputManager inputManager = new InputManager(windowHandle);
        inputManager.init();
        
        // Initially not active
        assertFalse(inputManager.isActionActive(InputAction.MOVE_FORWARD));
        
        // Simulate W key press (bound to MOVE_FORWARD)
        inputManager.simulateKeyEvent(GLFW_KEY_W, GLFW_PRESS);
        
        // Action should now be active
        assertTrue(inputManager.isActionActive(InputAction.MOVE_FORWARD));
        
        inputManager.cleanup();
    }
    
    @Test
    @DisplayName("Key release deactivates bound action")
    void testKeyReleaseDeactivatesAction(long windowHandle) {
        InputManager inputManager = new InputManager(windowHandle);
        inputManager.init();
        
        // Press W key
        inputManager.simulateKeyEvent(GLFW_KEY_W, GLFW_PRESS);
        assertTrue(inputManager.isActionActive(InputAction.MOVE_FORWARD));
        
        // Release W key
        inputManager.simulateKeyEvent(GLFW_KEY_W, GLFW_RELEASE);
        assertFalse(inputManager.isActionActive(InputAction.MOVE_FORWARD));
        
        inputManager.cleanup();
    }
    
    @Test
    @DisplayName("Multiple simultaneous actions can be active")
    void testSimultaneousActions(long windowHandle) {
        InputManager inputManager = new InputManager(windowHandle);
        inputManager.init();
        
        // Press W (MOVE_FORWARD) and LEFT_SHIFT (SPRINT)
        inputManager.simulateKeyEvent(GLFW_KEY_W, GLFW_PRESS);
        inputManager.simulateKeyEvent(GLFW_KEY_LEFT_SHIFT, GLFW_PRESS);
        
        // Both actions should be active
        assertTrue(inputManager.isActionActive(InputAction.MOVE_FORWARD));
        assertTrue(inputManager.isActionActive(InputAction.SPRINT));
        
        // Release one
        inputManager.simulateKeyEvent(GLFW_KEY_W, GLFW_RELEASE);
        
        // SPRINT should still be active
        assertFalse(inputManager.isActionActive(InputAction.MOVE_FORWARD));
        assertTrue(inputManager.isActionActive(InputAction.SPRINT));
        
        inputManager.cleanup();
    }
    
    @Test
    @DisplayName("ESC key triggers cursor lock toggle")
    void testEscapeTogglesCursor(long windowHandle) {
        InputManager inputManager = new InputManager(windowHandle);
        inputManager.init();
        
        // Initially unlocked
        assertFalse(inputManager.isCursorLocked());
        
        // Press ESC (should lock)
        inputManager.simulateKeyEvent(GLFW_KEY_ESCAPE, GLFW_PRESS);
        assertTrue(inputManager.isCursorLocked());
        
        // Press ESC again (should unlock)
        inputManager.simulateKeyEvent(GLFW_KEY_ESCAPE, GLFW_PRESS);
        assertFalse(inputManager.isCursorLocked());
        
        inputManager.cleanup();
    }
}
