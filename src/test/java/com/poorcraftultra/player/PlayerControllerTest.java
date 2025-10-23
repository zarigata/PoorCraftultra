package com.poorcraftultra.player;

import com.poorcraftultra.core.Camera;
import com.poorcraftultra.input.InputAction;
import com.poorcraftultra.input.InputManager;
import com.poorcraftultra.world.chunk.ChunkManager;
import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PlayerController class.
 * Tests integration between player and camera systems.
 */
class PlayerControllerTest {
    private static final float EPSILON = 0.0001f;
    
    /**
     * Creates a mock ChunkManager that returns air for all blocks.
     */
    private ChunkManager createEmptyWorld() {
        ChunkManager chunkManager = mock(ChunkManager.class);
        when(chunkManager.getBlock(anyInt(), anyInt(), anyInt())).thenReturn((byte) 0);
        return chunkManager;
    }
    
    /**
     * Creates a mock InputManager with no actions active.
     */
    private InputManager createIdleInput() {
        InputManager inputManager = mock(InputManager.class);
        when(inputManager.isActionActive(any(InputAction.class))).thenReturn(false);
        return inputManager;
    }
    
    @Test
    @DisplayName("PlayerController creation succeeds")
    void testControllerCreation() {
        ChunkManager chunkManager = createEmptyWorld();
        Player player = new Player(new Vector3f(0, 10, 0), chunkManager);
        Camera camera = new Camera(new Vector3f(0, 10, 0));
        
        assertDoesNotThrow(() -> new PlayerController(player, camera));
    }
    
    @Test
    @DisplayName("Camera position syncs to player eye position")
    void testCameraSyncToPlayer() {
        ChunkManager chunkManager = createEmptyWorld();
        Player player = new Player(new Vector3f(5, 10, 15), chunkManager);
        Camera camera = new Camera(new Vector3f(0, 0, 0));
        PlayerController controller = new PlayerController(player, camera);
        InputManager input = createIdleInput();
        
        // Update controller
        controller.update(0.016f, input);
        
        // Camera should be at player eye position
        Vector3f cameraPos = camera.getPosition();
        Vector3f expectedPos = player.getEyePosition();
        
        assertEquals(expectedPos.x, cameraPos.x, EPSILON);
        assertEquals(expectedPos.y, cameraPos.y, EPSILON);
        assertEquals(expectedPos.z, cameraPos.z, EPSILON);
    }
    
    @Test
    @DisplayName("Camera rotation syncs to player rotation")
    void testRotationSync() {
        ChunkManager chunkManager = createEmptyWorld();
        Player player = new Player(new Vector3f(0, 10, 0), chunkManager);
        Camera camera = new Camera(new Vector3f(0, 10, 0));
        PlayerController controller = new PlayerController(player, camera);
        InputManager input = createIdleInput();
        
        // Set player rotation
        player.setYaw(45.0f);
        player.setPitch(30.0f);
        
        // Update controller
        controller.update(0.016f, input);
        
        // Camera rotation should match player
        assertEquals(45.0f, camera.getYaw(), EPSILON);
        assertEquals(30.0f, camera.getPitch(), EPSILON);
    }
    
    @Test
    @DisplayName("Mouse look updates player rotation")
    void testMouseLookIntegration() {
        ChunkManager chunkManager = createEmptyWorld();
        Player player = new Player(new Vector3f(0, 10, 0), chunkManager);
        Camera camera = new Camera(new Vector3f(0, 10, 0));
        PlayerController controller = new PlayerController(player, camera);
        
        // Initial rotation
        assertEquals(0.0f, player.getPitch(), EPSILON);
        assertEquals(0.0f, player.getYaw(), EPSILON);
        
        // Handle mouse look
        controller.handleMouseLook(10.0f, 20.0f);
        
        // Player rotation should be updated
        assertEquals(10.0f, player.getPitch(), EPSILON);
        assertEquals(20.0f, player.getYaw(), EPSILON);
    }
    
    @Test
    @DisplayName("Mouse look clamps pitch correctly")
    void testMouseLookPitchClamping() {
        ChunkManager chunkManager = createEmptyWorld();
        Player player = new Player(new Vector3f(0, 10, 0), chunkManager);
        Camera camera = new Camera(new Vector3f(0, 10, 0));
        PlayerController controller = new PlayerController(player, camera);
        
        // Try to set pitch beyond limits
        controller.handleMouseLook(100.0f, 0.0f);
        
        // Pitch should be clamped
        assertEquals(89.0f, player.getPitch(), EPSILON);
        
        // Try negative
        controller.handleMouseLook(-200.0f, 0.0f);
        assertEquals(-89.0f, player.getPitch(), EPSILON);
    }
    
    @Test
    @DisplayName("Mouse look wraps yaw correctly")
    void testMouseLookYawWrapping() {
        ChunkManager chunkManager = createEmptyWorld();
        Player player = new Player(new Vector3f(0, 10, 0), chunkManager);
        Camera camera = new Camera(new Vector3f(0, 10, 0));
        PlayerController controller = new PlayerController(player, camera);
        
        // Set yaw to 350
        player.setYaw(350.0f);
        
        // Add 20 degrees (should wrap to 10)
        controller.handleMouseLook(0.0f, 20.0f);
        
        assertEquals(10.0f, player.getYaw(), EPSILON);
    }
    
    @Test
    @DisplayName("Update propagates to player")
    void testUpdatePropagation() {
        ChunkManager chunkManager = createEmptyWorld();
        Player player = new Player(new Vector3f(0, 10, 0), chunkManager);
        Camera camera = new Camera(new Vector3f(0, 10, 0));
        PlayerController controller = new PlayerController(player, camera);
        InputManager input = createIdleInput();
        
        float initialY = player.getPosition().y;
        
        // Update multiple times (gravity should affect player)
        for (int i = 0; i < 10; i++) {
            controller.update(0.1f, input);
        }
        
        // Player should have moved (fallen due to gravity)
        float finalY = player.getPosition().y;
        assertNotEquals(initialY, finalY, EPSILON);
    }
    
    @Test
    @DisplayName("Camera continuously tracks player movement")
    void testContinuousUpdate() {
        ChunkManager chunkManager = createEmptyWorld();
        Player player = new Player(new Vector3f(0, 10, 0), chunkManager);
        Camera camera = new Camera(new Vector3f(0, 10, 0));
        PlayerController controller = new PlayerController(player, camera);
        InputManager input = createIdleInput();
        
        // Update multiple times
        for (int i = 0; i < 5; i++) {
            controller.update(0.1f, input);
            
            // Camera should always match player eye position
            Vector3f cameraPos = camera.getPosition();
            Vector3f playerEyePos = player.getEyePosition();
            
            assertEquals(playerEyePos.x, cameraPos.x, EPSILON);
            assertEquals(playerEyePos.y, cameraPos.y, EPSILON);
            assertEquals(playerEyePos.z, cameraPos.z, EPSILON);
        }
    }
    
    @Test
    @DisplayName("Getters return correct instances")
    void testGetters() {
        ChunkManager chunkManager = createEmptyWorld();
        Player player = new Player(new Vector3f(0, 10, 0), chunkManager);
        Camera camera = new Camera(new Vector3f(0, 10, 0));
        PlayerController controller = new PlayerController(player, camera);
        
        assertSame(player, controller.getPlayer());
        assertSame(camera, controller.getCamera());
    }
    
    @Test
    @DisplayName("Multiple mouse look calls accumulate rotation")
    void testAccumulatedMouseLook() {
        ChunkManager chunkManager = createEmptyWorld();
        Player player = new Player(new Vector3f(0, 10, 0), chunkManager);
        Camera camera = new Camera(new Vector3f(0, 10, 0));
        PlayerController controller = new PlayerController(player, camera);
        
        // Apply mouse look multiple times
        controller.handleMouseLook(5.0f, 10.0f);
        controller.handleMouseLook(5.0f, 10.0f);
        controller.handleMouseLook(5.0f, 10.0f);
        
        // Rotation should accumulate
        assertEquals(15.0f, player.getPitch(), EPSILON);
        assertEquals(30.0f, player.getYaw(), EPSILON);
    }
}
