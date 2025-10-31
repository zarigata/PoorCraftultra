package com.poorcraft.ultra.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;
import com.poorcraft.ultra.voxel.AtlasValidator;
import com.poorcraft.ultra.voxel.BlockMaterial;
import com.poorcraft.ultra.voxel.TextureAtlas;
import com.poorcraft.ultra.voxel.TexturedCube;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Phase 0.4 verification tests.
 */
public class Phase04Tests {

    private static final Path ASSETS_DIR = Path.of("assets");
    private static final Path TEXTURES_DIR = ASSETS_DIR.resolve("textures");

    @AfterEach
    void tearDown() {
        resetTextureAtlasSingleton();
    }

    @Test
    void testTextureAtlasLoading() {
        assumeAssetsPresent();
        AssetManager assetManager = createAssetManager();

        TextureAtlas atlas = TextureAtlas.load(assetManager);

        assertTrue(TextureAtlas.isInitialized(), "TextureAtlas should be initialized after load");
        assertNotNull(atlas.getTexture(), "Atlas texture must be available");
        assertFalse(atlas.getAllBlockNames().isEmpty(), "Atlas mapping must contain at least one block");
    }

    @Test
    void testAtlasValidatorDimensions() {
        assumeAssetsPresent();
        AssetManager assetManager = createAssetManager();
        TextureAtlas atlas = TextureAtlas.load(assetManager);

        AtlasValidator.validateAtlasDimensions(atlas.getTexture());

        Image invalidImage = new Image(Image.Format.RGBA8, 256, 256,
                BufferUtils.createByteBuffer(256 * 256 * 4));
        Texture2D invalidTexture = new Texture2D(invalidImage);
        assertThrows(IllegalArgumentException.class,
                () -> AtlasValidator.validateAtlasDimensions(invalidTexture));
    }

    @Test
    void testAtlasValidatorIndices() {
        Map<String, Map<String, Integer>> validMapping = Map.of(
                "stone", Map.of("all", 0),
                "dirt", Map.of("all", 1)
        );
        AtlasValidator.validateAtlasIndices(validMapping);

        Map<String, Map<String, Integer>> invalidIndexMapping = Map.of(
                "stone", Map.of("all", 64)
        );
        assertThatThrownBy(() -> AtlasValidator.validateAtlasIndices(invalidIndexMapping))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("out of range");

        Map<String, Map<String, Integer>> duplicateMapping = Map.of(
                "stone", Map.of("all", 0),
                "dirt", Map.of("all", 0)
        );
        assertThatThrownBy(() -> AtlasValidator.validateAtlasIndices(duplicateMapping))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate");
    }

    @Test
    void testAtlasValidatorTileSize() {
        AtlasValidator.validateTileSize();
    }

    @Test
    void testBlockMaterialCreation() {
        assumeAssetsPresent();
        AssetManager assetManager = createAssetManager();
        TextureAtlas atlas = TextureAtlas.load(assetManager);

        Material material = BlockMaterial.create(assetManager, atlas);

        assertNotNull(material, "Material must be created");
        Texture colorMap = material.getTextureParam("ColorMap").getTextureValue();
        assertNotNull(colorMap, "ColorMap texture must be set");
        assertThat(colorMap.getMagFilter()).isEqualTo(Texture.MagFilter.Nearest);
        assertThat(colorMap.getMinFilter()).isEqualTo(Texture.MinFilter.Trilinear);
    }

    @Test
    void testTextureAtlasGetIndex() {
        assumeAssetsPresent();
        AssetManager assetManager = createAssetManager();
        TextureAtlas atlas = TextureAtlas.load(assetManager);

        int stoneIndex = atlas.getAtlasIndex("stone");
        assertThat(stoneIndex).isBetween(0, 63);

        int grassTop = atlas.getAtlasIndex("grass", "top");
        int grassSide = atlas.getAtlasIndex("grass", "side");
        assertThat(grassTop).isBetween(0, 63);
        assertThat(grassSide).isBetween(0, 63);
        assertThat(grassTop).isNotEqualTo(grassSide);

        int fallbackIndex = atlas.getAtlasIndex("nonexistent");
        assertEquals(0, fallbackIndex, "Missing block should fallback to index 0 (stone)");
    }

    @Test
    void testTextureAtlasUVCoordinates() {
        assumeAssetsPresent();
        AssetManager assetManager = createAssetManager();
        TextureAtlas atlas = TextureAtlas.load(assetManager);

        float[] stoneUv = atlas.getUVCoordinates(0);
        assertThat(stoneUv).hasSize(4);
        assertThat(stoneUv[0]).isCloseTo(0f, within(1e-6f));
        assertThat(stoneUv[1]).isCloseTo(0.875f, within(1e-6f));
        assertThat(stoneUv[2]).isCloseTo(0.125f, within(1e-6f));
        assertThat(stoneUv[3]).isCloseTo(1.0f, within(1e-6f));

        float[] topRight = atlas.getUVCoordinates(7);
        assertThat(topRight[0]).isCloseTo(0.875f, within(1e-6f));
        assertThat(topRight[1]).isCloseTo(0.875f, within(1e-6f));
        assertThat(topRight[2]).isCloseTo(1.0f, within(1e-6f));
        assertThat(topRight[3]).isCloseTo(1.0f, within(1e-6f));

        float[] bottomLeft = atlas.getUVCoordinates(56);
        assertThat(bottomLeft[0]).isCloseTo(0f, within(1e-6f));
        assertThat(bottomLeft[1]).isCloseTo(0f, within(1e-6f));
        assertThat(bottomLeft[2]).isCloseTo(0.125f, within(1e-6f));
        assertThat(bottomLeft[3]).isCloseTo(0.125f, within(1e-6f));

        float[] bottomRight = atlas.getUVCoordinates(63);
        assertThat(bottomRight[0]).isCloseTo(0.875f, within(1e-6f));
        assertThat(bottomRight[1]).isCloseTo(0f, within(1e-6f));
        assertThat(bottomRight[2]).isCloseTo(1.0f, within(1e-6f));
        assertThat(bottomRight[3]).isCloseTo(0.125f, within(1e-6f));
    }

    @Test
    void testTexturedCubeStateCreation() {
        TexturedCube cube = new TexturedCube();
        assertNotNull(cube, "Cube state must be instantiated");
        assertFalse(cube.isInitialized(), "Cube state should not be initialized before attach");
    }

    @Test
    void testCubeRotationLogic() {
        float angle = 0f;
        float tpf = 1f / 60f;
        for (int i = 0; i < 60 * 60; i++) {
            angle += (FastMath.TWO_PI / 60f) * tpf;
            if (angle > FastMath.TWO_PI) {
                angle -= FastMath.TWO_PI;
            }
        }
        assertEquals(FastMath.TWO_PI, angle, 0.01f);
    }

    @Test
    void testAtlasValidatorBlockMapping() {
        Map<String, Map<String, Integer>> validMapping = Map.of(
                "stone", Map.of("all", 0),
                "grass", Map.of("top", 1, "side", 2)
        );
        AtlasValidator.validateBlockMapping(validMapping);

        Map<String, Map<String, Integer>> invalidFaceMapping = Map.of(
                "stone", Map.of("invalid", 0)
        );
        assertThatThrownBy(() -> AtlasValidator.validateBlockMapping(invalidFaceMapping))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid face name");

        Map<String, Map<String, Integer>> emptyFaceMapping = Map.of(
                "stone", Map.of()
        );
        assertThatThrownBy(() -> AtlasValidator.validateBlockMapping(emptyFaceMapping))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no face mappings");
    }

    @Disabled("Requires generated assets and full engine context")
    @Test
    void testTexturedCubeIntegration() {
        // This integration test is intentionally disabled. Enable manually when running with full engine context.
    }

    private void assumeAssetsPresent() {
        Assumptions.assumeTrue(Files.exists(TEXTURES_DIR.resolve("blocks_atlas.png")),
                "blocks_atlas.png missing – run :tools:assets:generate");
        Assumptions.assumeTrue(Files.exists(TEXTURES_DIR.resolve("blocks_atlas.json")),
                "blocks_atlas.json missing – run :tools:assets:generate");
    }

    private AssetManager createAssetManager() {
        DesktopAssetManager manager = new DesktopAssetManager();
        manager.registerLocator("assets", FileLocator.class);
        return manager;
    }

    private void resetTextureAtlasSingleton() {
        try {
            Field instanceField = TextureAtlas.class.getDeclaredField("INSTANCE");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to reset TextureAtlas singleton", exception);
        }
    }
}
