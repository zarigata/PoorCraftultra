package com.poorcraftultra.integration;

import com.poorcraftultra.core.Window;
import com.poorcraftultra.input.InputManager;
import com.poorcraftultra.rendering.Camera;
import org.joml.Vector3f;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test verifying InputManager and Camera work together.
 */
public class InputCameraIntegrationTest {

    @BeforeAll
    static void initGLFW() {
        if (!GLFW.glfwInit()) {
            fail("Failed to initialize GLFW");
        }
    }

    @AfterAll
    static void terminateGLFW() {
        GLFW.glfwTerminate();
    }

    @Test
    void testInputManagerInitialization() {
        Window window = new Window();
        window.init();

        InputManager inputManager = new InputManager();
        assertDoesNotThrow(() -> inputManager.init(window));

        Camera camera = new Camera();
        assertDoesNotThrow(() -> inputManager.processInput(camera, 0.016f));

        assertDoesNotThrow(inputManager::cleanup);
        window.cleanup();
    }

    @Test
    void testCameraMovementIntegration() {
        Window window = new Window();
        window.init();

        InputManager inputManager = new InputManager();
        inputManager.init(window);

        Camera camera = new Camera();
        Vector3f initialPosition = new Vector3f(camera.getPosition());

        // Simulate pressing W key
        inputManager.getKeyboardInput().setKeyState(GLFW.GLFW_KEY_W, true);
        inputManager.processInput(camera, 0.016f);

        Vector3f newPosition = camera.getPosition();

        // Camera should have moved forward (decreased z)
        assertTrue(newPosition.z < initialPosition.z);
        assertEquals(initialPosition.x, newPosition.x);
        assertEquals(initialPosition.y, newPosition.y);

        inputManager.cleanup();
        window.cleanup();
    }

    @Test
    void testCameraRotationIntegration() {
        Window window = new Window();
        window.init();

        InputManager inputManager = new InputManager();
        inputManager.init(window);

        Camera camera = new Camera();
        float initialYaw = camera.getYaw();
        float initialPitch = camera.getPitch();

        // Simulate mouse movement
        inputManager.getMouseInput().setMouseOffsets(10.0, 5.0);
        inputManager.processInput(camera, 0.016f);

        // Camera should have rotated
        assertNotEquals(initialYaw, camera.getYaw());
        assertNotEquals(initialPitch, camera.getPitch());

        inputManager.cleanup();
        window.cleanup();
    }

    @Test
    void testDeltaTimeApplication() {
        Window window = new Window();
        window.init();

        InputManager inputManager = new InputManager();
        inputManager.init(window);

        Camera camera = new Camera();

        // Test with different delta times
        inputManager.processInput(camera, 0.016f); // 60 FPS
        inputManager.processInput(camera, 0.033f); // 30 FPS

        // Should not crash with different delta times
        assertTrue(true);

        inputManager.cleanup();
        window.cleanup();
    }
}
