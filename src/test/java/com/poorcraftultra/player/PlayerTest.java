package com.poorcraftultra.player;

import com.poorcraftultra.input.InputAction;
import com.poorcraftultra.input.InputManager;
import com.poorcraftultra.world.chunk.ChunkManager;
import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Player class.
 * Uses mocked ChunkManager and InputManager to test physics and collision logic.
 */
class PlayerTest {
    private static final float EPSILON = 0.0001f;
    
    /**
     * Creates a mock ChunkManager that returns air (0) for all blocks.
     */
    private ChunkManager createEmptyWorld() {
        ChunkManager chunkManager = mock(ChunkManager.class);
        when(chunkManager.getBlock(anyInt(), anyInt(), anyInt())).thenReturn((byte) 0);
        return chunkManager;
    }
    
    /**
     * Creates a mock ChunkManager with a solid block at the specified position.
     */
    private ChunkManager createWorldWithBlock(int x, int y, int z) {
        ChunkManager chunkManager = mock(ChunkManager.class);
        when(chunkManager.getBlock(anyInt(), anyInt(), anyInt())).thenReturn((byte) 0);
        when(chunkManager.getBlock(x, y, z)).thenReturn((byte) 1);
        return chunkManager;
    }
    
    /**
     * Creates a mock ChunkManager with a solid floor at y=9.
     */
    private ChunkManager createWorldWithFloor() {
        ChunkManager chunkManager = mock(ChunkManager.class);
        when(chunkManager.getBlock(anyInt(), anyInt(), anyInt())).thenAnswer(invocation -> {
            int y = invocation.getArgument(1);
            return y == 9 ? (byte) 1 : (byte) 0;
        });
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
    
    /**
     * Creates a mock InputManager with specified action active.
     */
    private InputManager createInputWithAction(InputAction action) {
        InputManager inputManager = mock(InputManager.class);
        when(inputManager.isActionActive(action)).thenReturn(true);
        when(inputManager.isActionActive(any(InputAction.class))).thenAnswer(invocation -> {
            InputAction arg = invocation.getArgument(0);
            return arg == action;
        });
        return inputManager;
    }
    
    @Test
    @DisplayName("Player creation initializes position and state correctly")
    void testPlayerCreation() {
        ChunkManager chunkManager = createEmptyWorld();
        Vector3f pos = new Vector3f(0, 10, 0);
        Player player = new Player(pos, chunkManager);
        
        Vector3f playerPos = player.getPosition();
        assertEquals(0, playerPos.x, EPSILON);
        assertEquals(10, playerPos.y, EPSILON);
        assertEquals(0, playerPos.z, EPSILON);
        
        Vector3f velocity = player.getVelocity();
        assertEquals(0, velocity.x, EPSILON);
        assertEquals(0, velocity.y, EPSILON);
        assertEquals(0, velocity.z, EPSILON);
        
        assertFalse(player.isOnGround());
    }
    
    @Test
    @DisplayName("Player AABB has correct dimensions")
    void testPlayerAABB() {
        ChunkManager chunkManager = createEmptyWorld();
        Player player = new Player(new Vector3f(0, 10, 0), chunkManager);
        
        // Standing AABB
        Vector3f[] aabb = player.getAABB();
        Vector3f min = aabb[0];
        Vector3f max = aabb[1];
        
        float width = max.x - min.x;
        float height = max.y - min.y;
        
        assertEquals(Player.PLAYER_WIDTH, width, EPSILON);
        assertEquals(Player.PLAYER_HEIGHT, height, EPSILON);
        
        // Crouched AABB
        player.setCrouching(true);
        aabb = player.getAABB();
        min = aabb[0];
        max = aabb[1];
        height = max.y - min.y;
        
        assertEquals(Player.PLAYER_HEIGHT_CROUCHED, height, EPSILON);
    }
    
    @Test
    @DisplayName("Player eye position is offset correctly")
    void testPlayerEyePosition() {
        ChunkManager chunkManager = createEmptyWorld();
        Player player = new Player(new Vector3f(0, 10, 0), chunkManager);
        
        // Standing eye position
        Vector3f eyePos = player.getEyePosition();
        assertEquals(0, eyePos.x, EPSILON);
        assertEquals(10 + Player.PLAYER_EYE_HEIGHT, eyePos.y, EPSILON);
        assertEquals(0, eyePos.z, EPSILON);
        
        // Crouched eye position
        player.setCrouching(true);
        eyePos = player.getEyePosition();
        assertEquals(10 + Player.PLAYER_EYE_HEIGHT_CROUCHED, eyePos.y, EPSILON);
    }
    
    @Test
    @DisplayName("Gravity is applied to player velocity")
    void testGravityApplication() {
        ChunkManager chunkManager = createEmptyWorld();
        Player player = new Player(new Vector3f(0, 10, 0), chunkManager);
        InputManager input = createIdleInput();
        
        float initialY = player.getPosition().y;
        
        // Update several times
        for (int i = 0; i < 10; i++) {
            player.update(0.1f, input);
        }
        
        // Player should have fallen
        float finalY = player.getPosition().y;
        assertTrue(finalY < initialY, "Player should fall due to gravity");
        
        // Velocity should be negative (falling)
        assertTrue(player.getVelocity().y < 0, "Velocity should be negative when falling");
    }
    
    @Test
    @DisplayName("Player stops on ground collision")
    void testGroundCollision() {
        ChunkManager chunkManager = createWorldWithFloor();
        Player player = new Player(new Vector3f(0, 10, 0), chunkManager);
        InputManager input = createIdleInput();
        
        // Update until player hits ground
        for (int i = 0; i < 100; i++) {
            player.update(0.016f, input);
            if (player.isOnGround()) {
                break;
            }
        }
        
        // Player should be on ground
        assertTrue(player.isOnGround(), "Player should be on ground");
        
        // Player should be at y=10 (standing on block at y=9)
        float playerY = player.getPosition().y;
        assertEquals(10, playerY, 0.1f);
        
        // Vertical velocity should be zero
        assertEquals(0, player.getVelocity().y, EPSILON);
    }
    
    @Test
    @DisplayName("Player cannot move through walls")
    void testWallCollision() {
        // Create world with wall at x=1
        ChunkManager chunkManager = mock(ChunkManager.class);
        when(chunkManager.getBlock(anyInt(), anyInt(), anyInt())).thenAnswer(invocation -> {
            int x = invocation.getArgument(0);
            int y = invocation.getArgument(1);
            // Floor at y=9, wall at x=1
            if (y == 9) return (byte) 1;
            if (x == 1 && y >= 10 && y <= 12) return (byte) 1;
            return (byte) 0;
        });
        
        Player player = new Player(new Vector3f(0, 10, 0), chunkManager);
        InputManager input = createInputWithAction(InputAction.MOVE_FORWARD);
        player.setYaw(90.0f); // Face positive X direction
        
        // Let player settle on ground first
        InputManager idleInput = createIdleInput();
        for (int i = 0; i < 50; i++) {
            player.update(0.016f, idleInput);
        }
        
        float initialX = player.getPosition().x;
        
        // Try to move into wall
        for (int i = 0; i < 50; i++) {
            player.update(0.016f, input);
        }
        
        // Player should not have moved significantly into the wall
        float finalX = player.getPosition().x;
        assertTrue(finalX < 1.0f, "Player should not move through wall");
    }
    
    @Test
    @DisplayName("Player can jump when on ground")
    void testJump() {
        ChunkManager chunkManager = createWorldWithFloor();
        Player player = new Player(new Vector3f(0, 10, 0), chunkManager);
        InputManager idleInput = createIdleInput();
        
        // Let player settle on ground
        for (int i = 0; i < 100; i++) {
            player.update(0.016f, idleInput);
            if (player.isOnGround()) {
                break;
            }
        }
        
        assertTrue(player.isOnGround(), "Player should be on ground before jump");
        
        // Jump
        InputManager jumpInput = createInputWithAction(InputAction.JUMP);
        player.update(0.016f, jumpInput);
        
        // Velocity should be positive (jumping up)
        assertEquals(Player.JUMP_VELOCITY, player.getVelocity().y, EPSILON);
        
        // After one more frame, should not be on ground
        player.update(0.016f, idleInput);
        assertFalse(player.isOnGround(), "Player should be airborne after jump");
    }
    
    @Test
    @DisplayName("Player movement speed varies with state")
    void testMovementSpeed() {
        ChunkManager chunkManager = createWorldWithFloor();
        
        // Test walk speed
        Player walkPlayer = new Player(new Vector3f(0, 10, 0), chunkManager);
        InputManager walkInput = createInputWithAction(InputAction.MOVE_FORWARD);
        
        // Let settle on ground
        for (int i = 0; i < 100; i++) {
            walkPlayer.update(0.016f, createIdleInput());
            if (walkPlayer.isOnGround()) break;
        }
        
        float walkStartZ = walkPlayer.getPosition().z;
        walkPlayer.update(1.0f, walkInput);
        float walkEndZ = walkPlayer.getPosition().z;
        float walkDistance = Math.abs(walkEndZ - walkStartZ);
        
        // Should move approximately WALK_SPEED blocks
        assertEquals(Player.WALK_SPEED, walkDistance, 1.0f);
        
        // Test sprint speed
        Player sprintPlayer = new Player(new Vector3f(5, 10, 0), chunkManager);
        InputManager sprintInput = mock(InputManager.class);
        when(sprintInput.isActionActive(InputAction.MOVE_FORWARD)).thenReturn(true);
        when(sprintInput.isActionActive(InputAction.SPRINT)).thenReturn(true);
        when(sprintInput.isActionActive(any(InputAction.class))).thenAnswer(invocation -> {
            InputAction action = invocation.getArgument(0);
            return action == InputAction.MOVE_FORWARD || action == InputAction.SPRINT;
        });
        
        // Let settle on ground
        for (int i = 0; i < 100; i++) {
            sprintPlayer.update(0.016f, createIdleInput());
            if (sprintPlayer.isOnGround()) break;
        }
        
        float sprintStartZ = sprintPlayer.getPosition().z;
        sprintPlayer.update(1.0f, sprintInput);
        float sprintEndZ = sprintPlayer.getPosition().z;
        float sprintDistance = Math.abs(sprintEndZ - sprintStartZ);
        
        // Sprint should be faster than walk
        assertTrue(sprintDistance > walkDistance, "Sprint should be faster than walk");
    }
    
    @Test
    @DisplayName("Crouching changes player height")
    void testCrouchingHeightChange() {
        ChunkManager chunkManager = createEmptyWorld();
        Player player = new Player(new Vector3f(0, 10, 0), chunkManager);
        
        // Standing height
        Vector3f[] standingAABB = player.getAABB();
        float standingHeight = standingAABB[1].y - standingAABB[0].y;
        
        // Crouch
        player.setCrouching(true);
        Vector3f[] crouchedAABB = player.getAABB();
        float crouchedHeight = crouchedAABB[1].y - crouchedAABB[0].y;
        
        // Crouched should be shorter
        assertTrue(crouchedHeight < standingHeight, "Crouched height should be less than standing");
        assertEquals(Player.PLAYER_HEIGHT_CROUCHED, crouchedHeight, EPSILON);
    }
    
    @Test
    @DisplayName("Player rotation setters and getters work correctly")
    void testRotation() {
        ChunkManager chunkManager = createEmptyWorld();
        Player player = new Player(new Vector3f(0, 10, 0), chunkManager);
        
        player.setYaw(45.0f);
        player.setPitch(30.0f);
        
        assertEquals(45.0f, player.getYaw(), EPSILON);
        assertEquals(30.0f, player.getPitch(), EPSILON);
    }
    
    @Test
    @DisplayName("Player can move freely in empty world")
    void testNoCollisionWithAir() {
        ChunkManager chunkManager = createEmptyWorld();
        Player player = new Player(new Vector3f(0, 10, 0), chunkManager);
        InputManager input = createInputWithAction(InputAction.MOVE_FORWARD);
        
        float initialZ = player.getPosition().z;
        
        // Move forward
        for (int i = 0; i < 10; i++) {
            player.update(0.1f, input);
        }
        
        float finalZ = player.getPosition().z;
        
        // Player should have moved (no collision with air)
        assertNotEquals(initialZ, finalZ, EPSILON);
    }
    
    @Test
    @DisplayName("Player can stand up when there is sufficient headroom")
    void testStandUpWithHeadroom() {
        ChunkManager chunkManager = createEmptyWorld();
        Player player = new Player(new Vector3f(0, 10, 0), chunkManager);
        
        // Crouch
        player.setCrouching(true);
        assertTrue(player.isCrouching(), "Player should be crouching");
        
        // Stand up (should succeed with no blocks above)
        player.setCrouching(false);
        assertFalse(player.isCrouching(), "Player should be standing");
    }
    
    @Test
    @DisplayName("Player cannot stand up when there is insufficient headroom")
    void testStandUpWithoutHeadroom() {
        // Create world with low ceiling
        ChunkManager chunkManager = mock(ChunkManager.class);
        when(chunkManager.getBlock(anyInt(), anyInt(), anyInt())).thenAnswer(invocation -> {
            int y = invocation.getArgument(1);
            // Floor at y=9, ceiling at y=11 (only 2 blocks high)
            if (y == 9 || y == 11) return (byte) 1;
            return (byte) 0;
        });
        
        Player player = new Player(new Vector3f(0, 10, 0), chunkManager);
        
        // Crouch
        player.setCrouching(true);
        assertTrue(player.isCrouching(), "Player should be crouching");
        
        // Try to stand up (should fail due to ceiling)
        player.setCrouching(false);
        assertTrue(player.isCrouching(), "Player should remain crouching due to low ceiling");
    }
    
    @Test
    @DisplayName("Player can stand up when ceiling is exactly at standing height")
    void testStandUpAtExactHeight() {
        // Create world with ceiling at exactly standing height
        ChunkManager chunkManager = mock(ChunkManager.class);
        when(chunkManager.getBlock(anyInt(), anyInt(), anyInt())).thenAnswer(invocation -> {
            int y = invocation.getArgument(1);
            // Floor at y=9, ceiling at y=12 (exactly 1.8 blocks high from y=10)
            // Player height is 1.8, so ceiling at 10+1.8=11.8, block at y=12 should not collide
            if (y == 9) return (byte) 1;
            if (y >= 12) return (byte) 1;
            return (byte) 0;
        });
        
        Player player = new Player(new Vector3f(0, 10, 0), chunkManager);
        
        // Crouch
        player.setCrouching(true);
        assertTrue(player.isCrouching(), "Player should be crouching");
        
        // Stand up (should succeed - ceiling is just high enough)
        player.setCrouching(false);
        assertFalse(player.isCrouching(), "Player should be able to stand with exact headroom");
    }
}
