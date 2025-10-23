package com.poorcraftultra.core;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Camera class.
 * Tests pure math operations without requiring OpenGL context.
 */
class CameraTest {
    private static final float EPSILON = 0.0001f;
    
    /**
     * Compares two floats with epsilon tolerance.
     */
    private void assertFloatEquals(float expected, float actual) {
        assertEquals(expected, actual, EPSILON);
    }
    
    /**
     * Compares two Vector3f with epsilon tolerance.
     */
    private void assertVector3fEquals(Vector3f expected, Vector3f actual) {
        assertFloatEquals(expected.x, actual.x);
        assertFloatEquals(expected.y, actual.y);
        assertFloatEquals(expected.z, actual.z);
    }
    
    @Test
    @DisplayName("Camera creation initializes position and rotation correctly")
    void testCameraCreation() {
        Vector3f pos = new Vector3f(0, 0, 0);
        Camera camera = new Camera(pos);
        
        assertVector3fEquals(pos, camera.getPosition());
        assertFloatEquals(0.0f, camera.getPitch());
        assertFloatEquals(0.0f, camera.getYaw());
    }
    
    @Test
    @DisplayName("Camera view matrix transforms world to view space correctly")
    void testCameraViewMatrix() {
        // Camera at (0, 0, 5) looking at origin
        Camera camera = new Camera(new Vector3f(0, 0, 5));
        camera.setYaw(180.0f); // Look toward negative Z (origin) - 180° from positive Z
        
        Matrix4f view = camera.getViewMatrix();
        
        // Transform origin (0,0,0) to view space
        Vector4f origin = new Vector4f(0, 0, 0, 1);
        Vector4f transformed = view.transform(origin);
        
        // Origin should be in front of camera (negative Z in view space)
        assertTrue(transformed.z < 0, "Origin should be in front of camera");
    }
    
    @Test
    @DisplayName("Camera rotation updates direction vectors correctly")
    void testCameraRotation() {
        Camera camera = new Camera(new Vector3f(0, 0, 0));
        
        // Set pitch=30, yaw=45
        camera.setPitch(30.0f);
        camera.setYaw(45.0f);
        
        Vector3f front = camera.getFront();
        
        // Calculate expected front vector
        float pitchRad = (float) Math.toRadians(30.0f);
        float yawRad = (float) Math.toRadians(45.0f);
        float expectedX = (float) (Math.cos(pitchRad) * Math.sin(yawRad));
        float expectedY = (float) Math.sin(pitchRad);
        float expectedZ = (float) (Math.cos(pitchRad) * Math.cos(yawRad));
        
        assertFloatEquals(expectedX, front.x);
        assertFloatEquals(expectedY, front.y);
        assertFloatEquals(expectedZ, front.z);
    }
    
    @Test
    @DisplayName("Pitch is clamped to [-89, 89] degrees")
    void testPitchClamping() {
        Camera camera = new Camera(new Vector3f(0, 0, 0));
        
        // Test upper bound
        camera.setPitch(100.0f);
        assertFloatEquals(89.0f, camera.getPitch());
        
        // Test lower bound
        camera.setPitch(-100.0f);
        assertFloatEquals(-89.0f, camera.getPitch());
        
        // Test within bounds
        camera.setPitch(45.0f);
        assertFloatEquals(45.0f, camera.getPitch());
    }
    
    @Test
    @DisplayName("Yaw wraps to [0, 360] degrees")
    void testYawWrapping() {
        Camera camera = new Camera(new Vector3f(0, 0, 0));
        
        // Test wrapping above 360
        camera.setYaw(370.0f);
        assertFloatEquals(10.0f, camera.getYaw());
        
        // Test wrapping below 0
        camera.setYaw(-10.0f);
        assertFloatEquals(350.0f, camera.getYaw());
        
        // Test within bounds
        camera.setYaw(180.0f);
        assertFloatEquals(180.0f, camera.getYaw());
    }
    
    @Test
    @DisplayName("Rotate method updates pitch and yaw with clamping/wrapping")
    void testRotateMethod() {
        Camera camera = new Camera(new Vector3f(0, 0, 0));
        camera.setPitch(0.0f);
        camera.setYaw(0.0f);
        
        // Rotate by deltas
        camera.rotate(10.0f, 20.0f);
        assertFloatEquals(10.0f, camera.getPitch());
        assertFloatEquals(20.0f, camera.getYaw());
        
        // Test clamping
        camera.rotate(100.0f, 0.0f);
        assertFloatEquals(89.0f, camera.getPitch());
        
        // Test wrapping
        camera.setYaw(350.0f);
        camera.rotate(0.0f, 20.0f);
        assertFloatEquals(10.0f, camera.getYaw());
    }
    
    @Test
    @DisplayName("Direction vectors are orthogonal and normalized")
    void testDirectionVectors() {
        Camera camera = new Camera(new Vector3f(0, 0, 0));
        
        // Test various rotations
        float[][] rotations = {
            {0.0f, 0.0f},
            {30.0f, 45.0f},
            {-45.0f, 90.0f},
            {60.0f, 180.0f}
        };
        
        for (float[] rotation : rotations) {
            camera.setPitch(rotation[0]);
            camera.setYaw(rotation[1]);
            
            Vector3f front = camera.getFront();
            Vector3f right = camera.getRight();
            Vector3f up = camera.getUp();
            
            // Check normalization (length = 1)
            assertFloatEquals(1.0f, front.length());
            assertFloatEquals(1.0f, right.length());
            assertFloatEquals(1.0f, up.length());
            
            // Check orthogonality (dot products = 0)
            assertFloatEquals(0.0f, front.dot(right));
            assertFloatEquals(0.0f, front.dot(up));
            assertFloatEquals(0.0f, right.dot(up));
        }
    }
    
    @Test
    @DisplayName("Camera looking at origin from (10,10,10) transforms correctly")
    void testLookAtOrigin() {
        Camera camera = new Camera(new Vector3f(10, 10, 10));
        
        // Calculate yaw and pitch to look at origin
        // Direction from camera to origin: (-10, -10, -10)
        Vector3f dir = new Vector3f(-10, -10, -10).normalize();
        
        // Calculate yaw: atan2(x, z)
        float yaw = (float) Math.toDegrees(Math.atan2(dir.x, dir.z));
        if (yaw < 0) yaw += 360;
        
        // Calculate pitch: asin(y)
        float pitch = (float) Math.toDegrees(Math.asin(dir.y));
        
        camera.setYaw(yaw);
        camera.setPitch(pitch);
        
        Matrix4f view = camera.getViewMatrix();
        
        // Transform origin to view space
        Vector4f origin = new Vector4f(0, 0, 0, 1);
        Vector4f transformed = view.transform(origin);
        
        // Origin should be in front of camera (negative Z)
        assertTrue(transformed.z < 0, "Origin should be in front of camera");
    }
    
    @Test
    @DisplayName("Two cameras with same state produce identical view matrices")
    void testViewMatrixConsistency() {
        Vector3f pos = new Vector3f(5, 10, 15);
        float pitch = 30.0f;
        float yaw = 45.0f;
        
        Camera camera1 = new Camera(pos);
        camera1.setPitch(pitch);
        camera1.setYaw(yaw);
        
        Camera camera2 = new Camera(pos);
        camera2.setPitch(pitch);
        camera2.setYaw(yaw);
        
        Matrix4f view1 = camera1.getViewMatrix();
        Matrix4f view2 = camera2.getViewMatrix();
        
        // Compare all matrix elements
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                assertFloatEquals(view1.get(i, j), view2.get(i, j));
            }
        }
    }
    
    @Test
    @DisplayName("Camera position setters work correctly")
    void testPositionSetters() {
        Camera camera = new Camera(new Vector3f(0, 0, 0));
        
        // Test Vector3f setter
        Vector3f newPos = new Vector3f(10, 20, 30);
        camera.setPosition(newPos);
        assertVector3fEquals(newPos, camera.getPosition());
        
        // Test individual component setter
        camera.setPosition(5, 15, 25);
        assertVector3fEquals(new Vector3f(5, 15, 25), camera.getPosition());
    }
    
    @Test
    @DisplayName("Camera returns defensive copies of vectors")
    void testDefensiveCopies() {
        Camera camera = new Camera(new Vector3f(0, 0, 0));
        camera.setPitch(30.0f);
        camera.setYaw(45.0f);
        
        // Get vectors
        Vector3f pos1 = camera.getPosition();
        Vector3f front1 = camera.getFront();
        Vector3f right1 = camera.getRight();
        Vector3f up1 = camera.getUp();
        
        // Modify returned vectors
        pos1.set(100, 100, 100);
        front1.set(100, 100, 100);
        right1.set(100, 100, 100);
        up1.set(100, 100, 100);
        
        // Get vectors again - should be unchanged
        Vector3f pos2 = camera.getPosition();
        Vector3f front2 = camera.getFront();
        Vector3f right2 = camera.getRight();
        Vector3f up2 = camera.getUp();
        
        assertVector3fEquals(new Vector3f(0, 0, 0), pos2);
        assertFalse(front2.equals(new Vector3f(100, 100, 100)));
        assertFalse(right2.equals(new Vector3f(100, 100, 100)));
        assertFalse(up2.equals(new Vector3f(100, 100, 100)));
    }
    
    @Test
    @DisplayName("Camera basis is right-handed with correct handedness")
    void testRightHandedBasis() {
        Camera camera = new Camera(new Vector3f(0, 0, 0));
        
        // Test yaw=0: front should be +Z, right should be +X, up should be +Y
        camera.setPitch(0.0f);
        camera.setYaw(0.0f);
        
        Vector3f front = camera.getFront();
        Vector3f right = camera.getRight();
        Vector3f up = camera.getUp();
        
        // At yaw=0, pitch=0: front=(0,0,1), right=(1,0,0), up=(0,1,0)
        assertFloatEquals(0.0f, front.x);
        assertFloatEquals(0.0f, front.y);
        assertFloatEquals(1.0f, front.z);
        
        assertFloatEquals(1.0f, right.x);
        assertFloatEquals(0.0f, right.y);
        assertFloatEquals(0.0f, right.z);
        
        assertFloatEquals(0.0f, up.x);
        assertFloatEquals(1.0f, up.y);
        assertFloatEquals(0.0f, up.z);
        
        // Test yaw=90: front should be +X, right should be -Z, up should be +Y
        camera.setYaw(90.0f);
        
        front = camera.getFront();
        right = camera.getRight();
        up = camera.getUp();
        
        // At yaw=90, pitch=0: front=(1,0,0), right=(0,0,-1), up=(0,1,0)
        assertFloatEquals(1.0f, front.x);
        assertFloatEquals(0.0f, front.y);
        assertFloatEquals(0.0f, front.z);
        
        assertFloatEquals(0.0f, right.x);
        assertFloatEquals(0.0f, right.y);
        assertFloatEquals(-1.0f, right.z);
        
        assertFloatEquals(0.0f, up.x);
        assertFloatEquals(1.0f, up.y);
        assertFloatEquals(0.0f, up.z);
        
        // Verify right-handedness: front × right = up
        Vector3f crossProduct = new Vector3f(front).cross(right);
        assertVector3fEquals(up, crossProduct);
    }
}
