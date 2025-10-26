package com.poorcraftultra.rendering;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Camera class.
 */
public class CameraTest {

    private Camera camera;

    @BeforeEach
    void setUp() {
        camera = new Camera();
    }

    @Test
    void testInitialPosition() {
        Vector3f position = camera.getPosition();
        assertEquals(0.0f, position.x);
        assertEquals(0.0f, position.y);
        assertEquals(0.0f, position.z);
    }

    @Test
    void testInitialRotation() {
        assertEquals(0.0f, camera.getYaw());
        assertEquals(0.0f, camera.getPitch());
    }

    @Test
    void testMoveForward() {
        Vector3f initialPos = new Vector3f(camera.getPosition());
        camera.moveForward(1.0f);
        Vector3f newPos = camera.getPosition();

        // Since front is (0,0,-1), moving forward should decrease z
        assertEquals(initialPos.x, newPos.x, 0.001f);
        assertEquals(initialPos.y, newPos.y, 0.001f);
        assertTrue(newPos.z < initialPos.z);
    }

    @Test
    void testMoveBackward() {
        Vector3f initialPos = new Vector3f(camera.getPosition());
        camera.moveBackward(1.0f);
        Vector3f newPos = camera.getPosition();

        // Moving backward should increase z
        assertEquals(initialPos.x, newPos.x, 0.001f);
        assertEquals(initialPos.y, newPos.y, 0.001f);
        assertTrue(newPos.z > initialPos.z);
    }

    @Test
    void testMoveLeft() {
        Vector3f initialPos = new Vector3f(camera.getPosition());
        camera.moveLeft(1.0f);
        Vector3f newPos = camera.getPosition();

        // Moving left should decrease x
        assertTrue(newPos.x < initialPos.x);
        assertEquals(initialPos.y, newPos.y, 0.001f);
        assertEquals(initialPos.z, newPos.z, 0.001f);
    }

    @Test
    void testMoveRight() {
        Vector3f initialPos = new Vector3f(camera.getPosition());
        camera.moveRight(1.0f);
        Vector3f newPos = camera.getPosition();

        // Moving right should increase x
        assertTrue(newPos.x > initialPos.x);
        assertEquals(initialPos.y, newPos.y, 0.001f);
        assertEquals(initialPos.z, newPos.z, 0.001f);
    }

    @Test
    void testRotateYaw() {
        camera.rotate(45.0f, 0.0f);
        assertEquals(45.0f, camera.getYaw());
        assertEquals(0.0f, camera.getPitch());
    }

    @Test
    void testRotatePitch() {
        camera.rotate(0.0f, 30.0f);
        assertEquals(0.0f, camera.getYaw());
        assertEquals(30.0f, camera.getPitch());
    }

    @Test
    void testPitchClamping() {
        camera.rotate(0.0f, 100.0f); // Try to go beyond 89
        assertEquals(89.0f, camera.getPitch());

        camera.rotate(0.0f, -200.0f); // Try to go below -89
        assertEquals(-89.0f, camera.getPitch());
    }

    @Test
    void testUpdateVectors() {
        camera.rotate(90.0f, 0.0f); // Face east
        camera.updateVectors();

        Vector3f front = camera.getFront();
        assertEquals(1.0f, front.x, 0.001f); // Should face positive X
        assertEquals(0.0f, front.y, 0.001f);
        assertEquals(0.0f, front.z, 0.001f);
    }

    @Test
    void testViewMatrix() {
        Matrix4f viewMatrix = camera.getViewMatrix();
        assertNotNull(viewMatrix);

        // View matrix should be identity for camera at origin facing -Z
        // But with JOML's lookAt, it will be transformed
        assertTrue(viewMatrix.determinant() != 0); // Should be invertible
    }

    @Test
    void testProjectionMatrix() {
        Matrix4f projMatrix = camera.getProjectionMatrix(1.6f); // 16:10 aspect ratio
        assertNotNull(projMatrix);

        // Check that it's a valid projection matrix
        assertTrue(projMatrix.determinant() != 0);
    }

    @Test
    void testProjectionMatrixWithParams() {
        Matrix4f projMatrix = camera.getProjectionMatrix(90.0f, 1.6f, 0.1f, 100.0f);
        assertNotNull(projMatrix);

        // Test with wide FOV
        Matrix4f wideProj = camera.getProjectionMatrix(120.0f, 1.6f, 0.1f, 100.0f);
        assertNotNull(wideProj);
    }
}
