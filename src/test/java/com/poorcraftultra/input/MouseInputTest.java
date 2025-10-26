package com.poorcraftultra.input;

import com.poorcraftultra.rendering.Camera;
import com.poorcraftultra.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MouseInput class.
 */
public class MouseInputTest {

    private MouseInput mouseInput;
    private Camera camera;
    private long mockWindowHandle;

    @BeforeEach
    void setUp() {
        mockWindowHandle = 12345L;
        mouseInput = new MouseInput(mockWindowHandle);
        camera = new Camera();
    }

    @Test
    void testInitialMouseState() {
        // Test initial offsets
        assertEquals(0.0, mouseInput.getXOffset());
        assertEquals(0.0, mouseInput.getYOffset());
    }

    @Test
    void testFirstFrameSkip() {
        // Process input on first frame should not affect camera
        float initialYaw = camera.getYaw();
        float initialPitch = camera.getPitch();

        mouseInput.processInput(camera, 0.016f);

        assertEquals(initialYaw, camera.getYaw());
        assertEquals(initialPitch, camera.getPitch());
    }

    @Test
    void testMouseDeltaCalculation() {
        mouseInput.setMouseOffsets(10.0, 5.0);
        assertEquals(10.0, mouseInput.getXOffset());
        assertEquals(5.0, mouseInput.getYOffset());
    }

    @Test
    void testCameraRotation() {
        float initialYaw = camera.getYaw();
        float initialPitch = camera.getPitch();

        mouseInput.setMouseOffsets(10.0, 5.0);
        mouseInput.processInput(camera, 1.0f);

        // Yaw and pitch should have changed
        assertNotEquals(initialYaw, camera.getYaw());
        assertNotEquals(initialPitch, camera.getPitch());
    }

    @Test
    void testPitchClamping() {
        // Set large negative yoffset to try to go below -89
        mouseInput.setMouseOffsets(0.0, -1000.0);
        mouseInput.processInput(camera, 1.0f);
        assertEquals(-89.0f, camera.getPitch());

        // Reset camera
        camera.setPitch(0.0f);

        // Set large positive yoffset to try to go above 89
        mouseInput.setMouseOffsets(0.0, 1000.0);
        mouseInput.processInput(camera, 1.0f);
        assertEquals(89.0f, camera.getPitch());
    }

    @Test
    void testMouseSensitivity() {
        float initialYaw = camera.getYaw();
        float initialPitch = camera.getPitch();

        mouseInput.setMouseOffsets(10.0, 5.0);
        mouseInput.processInput(camera, 1.0f);

        float expectedYawChange = initialYaw + 10.0f * Constants.MOUSE_SENSITIVITY;
        float expectedPitchChange = initialPitch + 5.0f * Constants.MOUSE_SENSITIVITY;

        assertEquals(expectedYawChange, camera.getYaw(), 0.001f);
        assertEquals(expectedPitchChange, camera.getPitch(), 0.001f);
    }

    @Test
    void testOffsetReset() {
        mouseInput.setMouseOffsets(10.0, 5.0);
        mouseInput.processInput(camera, 1.0f);

        // Offsets should be reset to 0 after processing
        assertEquals(0.0, mouseInput.getXOffset());
        assertEquals(0.0, mouseInput.getYOffset());
    }
}
