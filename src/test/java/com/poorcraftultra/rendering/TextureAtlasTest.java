package com.poorcraftultra.rendering;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TextureAtlas.
 * Note: Full OpenGL tests require GLTestContext which is complex to set up.
 * These are basic tests for the atlas structure.
 */
class TextureAtlasTest {

    @Test
    @DisplayName("Atlas creation should initialize correctly")
    void testAtlasCreation() {
        TextureAtlas atlas = new TextureAtlas(512, 512, 32);
        
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
}
