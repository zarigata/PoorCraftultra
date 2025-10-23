package com.poorcraftultra.rendering;

import com.poorcraftultra.core.GLTestContext;
import com.poorcraftultra.world.block.BlockRegistry;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Tests for TextureAtlas.
 * Note: Full OpenGL tests require GLTestContext which is complex to set up.
 * These are basic tests for the atlas structure.
 */
@ExtendWith(GLTestContext.class)
class TextureAtlasTest {
    private TextureAtlas atlas;

    @AfterEach
    void tearDown() {
        if (atlas != null) {
            atlas.cleanup();
            atlas = null;
        }
    }

    @Test
    @DisplayName("Atlas creation should initialize correctly")
    void testAtlasCreation() {
        atlas = new TextureAtlas(512, 512, 32);
        
        assertNotNull(atlas);
        assertEquals(-1, atlas.getTextureId(), "Texture ID should be -1 before loading");
    }

    @Test
    @DisplayName("Atlas position should calculate UV coordinates correctly")
    void testAtlasPositionUVCalculation() {
        // Create an atlas position for tile at (1, 2) in a 512x512 atlas with 32px tiles
        TextureAtlas.AtlasPosition pos = new TextureAtlas.AtlasPosition(1, 2, 0.0625f, 0.125f, 0.125f, 0.1875f);
        
        assertEquals(1, pos.x);
        assertEquals(2, pos.y);
        assertTrue(pos.u0 >= 0.0f && pos.u0 <= 1.0f);
        assertTrue(pos.v0 >= 0.0f && pos.v0 <= 1.0f);
        assertTrue(pos.u1 >= 0.0f && pos.u1 <= 1.0f);
        assertTrue(pos.v1 >= 0.0f && pos.v1 <= 1.0f);
        assertTrue(pos.u1 > pos.u0, "u1 should be greater than u0");
        assertTrue(pos.v1 > pos.v0, "v1 should be greater than v0");
    }

    @Test
    @DisplayName("Atlas position toString should work")
    void testAtlasPositionToString() {
        TextureAtlas.AtlasPosition pos = new TextureAtlas.AtlasPosition(1, 2, 0.0625f, 0.125f, 0.125f, 0.1875f);
        
        String str = pos.toString();
        assertTrue(str.contains("1"));
        assertTrue(str.contains("2"));
    }

    @Test
    @DisplayName("Create default atlas and verify texture loading")
    void testCreateDefaultAtlas() {
        // Create default atlas with GL context
        atlas = TextureAtlas.createDefault();
        
        assertNotNull(atlas);
        assertTrue(atlas.getTextureId() > 0, "Texture ID should be valid after loading");
        
        // Verify no GL errors occurred during creation
        int error = glGetError();
        assertEquals(GL_NO_ERROR, error, "No OpenGL errors should occur during atlas creation");
    }

    @Test
    @DisplayName("Get UV coordinates for known textures")
    void testGetUVCoordinates() {
        atlas = TextureAtlas.createDefault();
        
        // Test known textures from the default atlas
        TextureAtlas.AtlasPosition stonePos = atlas.getUVCoordinates("stone");
        assertNotNull(stonePos, "Stone texture should be in atlas");
        assertTrue(stonePos.u0 >= 0.0f && stonePos.u0 < 1.0f);
        assertTrue(stonePos.v0 >= 0.0f && stonePos.v0 < 1.0f);
        assertTrue(stonePos.u1 > stonePos.u0);
        assertTrue(stonePos.v1 > stonePos.v0);
        
        TextureAtlas.AtlasPosition grassPos = atlas.getUVCoordinates("grass_top");
        assertNotNull(grassPos, "Grass top texture should be in atlas");
        
        TextureAtlas.AtlasPosition dirtPos = atlas.getUVCoordinates("dirt");
        assertNotNull(dirtPos, "Dirt texture should be in atlas");
        
        // Test that unknown texture throws exception
        assertThrows(IllegalArgumentException.class, () -> {
            atlas.getUVCoordinates("nonexistent_texture");
        }, "Should throw exception for unknown texture");
    }

    @Test
    @DisplayName("Bind and unbind atlas")
    void testBindUnbind() {
        atlas = TextureAtlas.createDefault();
        
        // Bind the atlas
        atlas.bind();
        int error = glGetError();
        assertEquals(GL_NO_ERROR, error, "No OpenGL errors should occur during bind");
        
        // Unbind the atlas
        atlas.unbind();
        error = glGetError();
        assertEquals(GL_NO_ERROR, error, "No OpenGL errors should occur during unbind");
    }

    @Test
    @DisplayName("Update block textures with atlas coordinates")
    void testUpdateBlockTextures() {
        atlas = TextureAtlas.createDefault();
        BlockRegistry registry = BlockRegistry.getInstance();
        
        // Update block textures with atlas coordinates
        atlas.updateBlockTextures(registry);
        
        // Verify no GL errors
        int error = glGetError();
        assertEquals(GL_NO_ERROR, error, "No OpenGL errors should occur during block texture update");
        
        // This test mainly verifies the method runs without errors
        // Actual coordinate verification would require inspecting block internals
    }

    @Test
    @DisplayName("Cleanup releases GL resources")
    void testCleanup() {
        atlas = TextureAtlas.createDefault();
        int textureId = atlas.getTextureId();
        assertTrue(textureId > 0, "Texture should be created");
        
        // Cleanup
        atlas.cleanup();
        
        // After cleanup, texture ID should be -1
        assertEquals(-1, atlas.getTextureId(), "Texture ID should be -1 after cleanup");
        
        // Verify no GL errors
        int error = glGetError();
        assertEquals(GL_NO_ERROR, error, "No OpenGL errors should occur during cleanup");
    }
}
