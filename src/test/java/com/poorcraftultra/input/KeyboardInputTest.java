package com.poorcraftultra.input;

import com.poorcraftultra.rendering.Camera;
import com.poorcraftultra.util.Constants;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for KeyboardInput class.
 */
public class KeyboardInputTest {

    private KeyboardInput keyboardInput;
    private Camera camera;
    private long mockWindowHandle;

    @BeforeEach
    void setUp() {
        // Mock window handle
        mockWindowHandle = 12345L;
        keyboardInput = new KeyboardInput(mockWindowHandle);
        camera = new Camera();
    }

    @Test
    void testInitialKeyState() {
        assertFalse(keyboardInput.isKeyPressed(GLFW.GLFW_KEY_W));
        assertFalse(keyboardInput.isKeyPressed(GLFW.GLFW_KEY_S));
        assertFalse(keyboardInput.isKeyPressed(GLFW.GLFW_KEY_A));
        assertFalse(keyboardInput.isKeyPressed(GLFW.GLFW_KEY_D));
    }

    @Test
    void testKeyPressDetection() {
        keyboardInput.setKeyState(GLFW.GLFW_KEY_W, true);
        assertTrue(keyboardInput.isKeyPressed(GLFW.GLFW_KEY_W));

        keyboardInput.setKeyState(GLFW.GLFW_KEY_W, false);
        assertFalse(keyboardInput.isKeyPressed(GLFW.GLFW_KEY_W));
    }

    @Test
    void testCameraMovementForward() {
        Vector3f initialPos = new Vector3f(camera.getPosition());
        keyboardInput.setKeyState(GLFW.GLFW_KEY_W, true);
        keyboardInput.processInput(camera, 1.0f);
        Vector3f newPos = camera.getPosition();

        // Moving forward should decrease z
        assertEquals(initialPos.x, newPos.x, 0.001f);
        assertEquals(initialPos.y, newPos.y, 0.001f);
        assertTrue(newPos.z < initialPos.z);
    }

    @Test
    void testCameraMovementBackward() {
        Vector3f initialPos = new Vector3f(camera.getPosition());
        keyboardInput.setKeyState(GLFW.GLFW_KEY_S, true);
        keyboardInput.processInput(camera, 1.0f);
        Vector3f newPos = camera.getPosition();

        // Moving backward should increase z
        assertEquals(initialPos.x, newPos.x, 0.001f);
        assertEquals(initialPos.y, newPos.y, 0.001f);
        assertTrue(newPos.z > initialPos.z);
    }

    @Test
    void testCameraMovementLeft() {
        Vector3f initialPos = new Vector3f(camera.getPosition());
        keyboardInput.setKeyState(GLFW.GLFW_KEY_A, true);
        keyboardInput.processInput(camera, 1.0f);
        Vector3f newPos = camera.getPosition();

        // Moving left should decrease x
        assertTrue(newPos.x < initialPos.x);
        assertEquals(initialPos.y, newPos.y, 0.001f);
        assertEquals(initialPos.z, newPos.z, 0.001f);
    }

    @Test
    void testCameraMovementRight() {
        Vector3f initialPos = new Vector3f(camera.getPosition());
        keyboardInput.setKeyState(GLFW.GLFW_KEY_D, true);
        keyboardInput.processInput(camera, 1.0f);
        Vector3f newPos = camera.getPosition();

        // Moving right should increase x
        assertTrue(newPos.x > initialPos.x);
        assertEquals(initialPos.y, newPos.y, 0.001f);
        assertEquals(initialPos.z, newPos.z, 0.001f);
    }

    @Test
    void testSprintModifier() {
        Vector3f initialPos = new Vector3f(camera.getPosition());
        keyboardInput.setKeyState(GLFW.GLFW_KEY_W, true);
        keyboardInput.setKeyState(GLFW.GLFW_KEY_LEFT_SHIFT, true);
        keyboardInput.processInput(camera, 1.0f);
        Vector3f newPos = camera.getPosition();

        // Sprinting forward should move more than normal speed
        float sprintMove = Constants.CAMERA_MOVE_SPEED * Constants.CAMERA_SPRINT_MULTIPLIER;
        assertEquals(initialPos.x, newPos.x, 0.001f);
        assertEquals(initialPos.y, newPos.y, 0.001f);
        assertEquals(newPos.z, initialPos.z - sprintMove, 0.001f);
    }

    @Test
    void testDeltaTimeApplication() {
        Vector3f pos1 = new Vector3f(camera.getPosition());
        keyboardInput.setKeyState(GLFW.GLFW_KEY_W, true);
        keyboardInput.processInput(camera, 1.0f);
        Vector3f pos2 = camera.getPosition();

        // Reset camera
        camera.setPosition(new Vector3f(0, 0, 0));

        Vector3f pos3 = new Vector3f(camera.getPosition());
        keyboardInput.processInput(camera, 0.5f);
        Vector3f pos4 = camera.getPosition();

        // Movement with deltaTime 0.5 should be half of deltaTime 1.0
        float move1 = pos1.z - pos2.z;
        float move2 = pos3.z - pos4.z;
        assertEquals(move1 / 2, move2, 0.001f);
    }

    @Test
    void testInvalidKeyRange() {
        assertFalse(keyboardInput.isKeyPressed(-1));
        assertFalse(keyboardInput.isKeyPressed(GLFW.GLFW_KEY_LAST + 1));
    }
}
