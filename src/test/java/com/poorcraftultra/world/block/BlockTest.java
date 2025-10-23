package com.poorcraftultra.world.block;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Block class.
 */
class BlockTest {

    @Test
    @DisplayName("Block creation should work correctly")
    void testBlockCreation() {
        Block block = new Block((byte) 1, "stone", "Stone", BlockProperties.solid(), "stone");
        
        assertEquals((byte) 1, block.getId());
        assertEquals("stone", block.getName());
        assertEquals("Stone", block.getDisplayName());
        assertTrue(block.isSolid());
    }

    @Test
    @DisplayName("Block with same texture on all faces should work")
    void testBlockWithSameTextureAllFaces() {
        Block block = new Block((byte) 1, "stone", "Stone", BlockProperties.solid(), "stone");
        
        for (BlockFace face : BlockFace.values()) {
            Block.TextureReference texRef = block.getTextureReference(face);
            assertEquals("stone", texRef.textureName);
        }
    }

    @Test
    @DisplayName("Block with different face textures should work")
    void testBlockWithDifferentFaceTextures() {
        String[] textures = {"grass_top", "dirt", "grass_side", "grass_side", "grass_side", "grass_side"};
        Block block = new Block((byte) 2, "grass", "Grass Block", BlockProperties.solid(), textures);
        
        assertEquals("grass_top", block.getTextureReference(BlockFace.TOP).textureName);
        assertEquals("dirt", block.getTextureReference(BlockFace.BOTTOM).textureName);
        assertEquals("grass_side", block.getTextureReference(BlockFace.NORTH).textureName);
    }

    @Test
    @DisplayName("Block property delegation should work")
    void testBlockPropertyDelegation() {
        Block solid = new Block((byte) 1, "stone", "Stone", BlockProperties.solid(), "stone");
        Block transparent = new Block((byte) 5, "glass", "Glass", BlockProperties.transparent(), "glass");
        
        assertTrue(solid.isSolid());
        assertFalse(solid.isTransparent());
        
        assertTrue(transparent.isSolid());
        assertTrue(transparent.isTransparent());
    }

    @Test
    @DisplayName("Block equality should be based on ID")
    void testBlockEquality() {
        Block block1 = new Block((byte) 1, "stone", "Stone", BlockProperties.solid(), "stone");
        Block block2 = new Block((byte) 1, "stone2", "Stone 2", BlockProperties.solid(), "stone2");
        Block block3 = new Block((byte) 2, "grass", "Grass", BlockProperties.solid(), "grass");
        
        assertEquals(block1, block2, "Blocks with same ID should be equal");
        assertNotEquals(block1, block3, "Blocks with different IDs should not be equal");
    }

    @Test
    @DisplayName("Atlas coordinates setting should work")
    void testAtlasCoordinatesSetting() {
        Block block = new Block((byte) 1, "stone", "Stone", BlockProperties.solid(), "stone");
        
        block.setAtlasCoordinates(BlockFace.TOP, 2, 3);
        
        Block.TextureReference texRef = block.getTextureReference(BlockFace.TOP);
        assertEquals(2, texRef.atlasX);
        assertEquals(3, texRef.atlasY);
    }

    @Test
    @DisplayName("Block toString should include ID and name")
    void testBlockToString() {
        Block block = new Block((byte) 1, "stone", "Stone", BlockProperties.solid(), "stone");
        
        String str = block.toString();
        assertTrue(str.contains("1"));
        assertTrue(str.contains("stone"));
    }
}
