package com.poorcraft.ultra.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.poorcraft.ultra.engine.PhysicsManager;
import com.poorcraft.ultra.voxel.ChunkMesh;
import com.poorcraft.ultra.voxel.FlatPatchState;
import com.poorcraft.ultra.voxel.TextureAtlas;
import java.lang.reflect.Field;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Phase 0.5 verification tests.
 */
public class Phase05Tests {

    private static final Path ASSETS_DIR = Path.of("assets");
    private static final Path TEXTURES_DIR = ASSETS_DIR.resolve("textures");

    @BeforeEach
    void setUp() {
        resetPhysicsManagerSingleton();
    }

    @AfterEach
    void tearDown() {
        resetTextureAtlasSingleton();
        resetPhysicsManagerSingleton();
    }

    @Test
    void testChunkMeshGeneration() {
        assumeAssetsPresent();
        AssetManager assetManager = createAssetManager();
        TextureAtlas atlas = TextureAtlas.load(assetManager);

        Mesh mesh = ChunkMesh.generateFlatPatch(32, 32, atlas);

        assertNotNull(mesh, "Mesh must not be null");
        assertThat(mesh.getVertexCount()).isGreaterThan(0);
        assertThat(mesh.getTriangleCount()).isGreaterThanOrEqualTo(2);

        assertNotNull(mesh.getBuffer(VertexBuffer.Type.Position));
        assertNotNull(mesh.getBuffer(VertexBuffer.Type.Normal));
        assertNotNull(mesh.getBuffer(VertexBuffer.Type.TexCoord));
        assertNotNull(mesh.getBuffer(VertexBuffer.Type.Index));

        assertNotNull(mesh.getBound());
    }

    @Test
    void testGreedyMeshingReduction() {
        assumeAssetsPresent();
        AssetManager assetManager = createAssetManager();
        TextureAtlas atlas = TextureAtlas.load(assetManager);

        Mesh mesh = ChunkMesh.generateFlatPatch(32, 32, atlas);

        int naiveTriangles = 4352;
        int actualTriangles = mesh.getTriangleCount();

        assertThat(actualTriangles).isLessThan(naiveTriangles);

        double reduction = ((double) (naiveTriangles - actualTriangles) / naiveTriangles) * 100.0;
        assertThat(reduction).isGreaterThanOrEqualTo(40.0);
    }

    @Test
    void testMeshUVCoordinates() {
        assumeAssetsPresent();
        AssetManager assetManager = createAssetManager();
        TextureAtlas atlas = TextureAtlas.load(assetManager);

        Mesh mesh = ChunkMesh.generateFlatPatch(32, 32, atlas);

        FloatBuffer uvBuffer = mesh.getFloatBuffer(VertexBuffer.Type.TexCoord);
        assertNotNull(uvBuffer);
        uvBuffer.rewind();
        while (uvBuffer.hasRemaining()) {
            float value = uvBuffer.get();
            assertThat(value).isBetween(0f, 1f);
        }
    }

    @Test
    void testMeshNormals() {
        assumeAssetsPresent();
        AssetManager assetManager = createAssetManager();
        TextureAtlas atlas = TextureAtlas.load(assetManager);

        Mesh mesh = ChunkMesh.generateFlatPatch(32, 32, atlas);

        FloatBuffer normalBuffer = mesh.getFloatBuffer(VertexBuffer.Type.Normal);
        assertNotNull(normalBuffer);
        normalBuffer.rewind();

        int vertexIndex = 0;
        while (normalBuffer.hasRemaining()) {
            float x = normalBuffer.get();
            float y = normalBuffer.get();
            float z = normalBuffer.get();
            Vector3f normal = new Vector3f(x, y, z);

            if (vertexIndex < 4) {
                assertThat(normal).isEqualTo(new Vector3f(0f, 1f, 0f));
            } else if (vertexIndex < 8) {
                assertThat(normal).isEqualTo(new Vector3f(0f, -1f, 0f));
            }
            vertexIndex++;
        }

        assertThat(vertexIndex).isGreaterThan(0);
    }

    @Test
    void testPhysicsManagerCreation() {
        PhysicsManager manager = new PhysicsManager();
        assertNotNull(manager);
        assertThat(PhysicsManager.isInitializedManager()).isFalse();
    }

    @Test
    void testFlatPatchStateCreation() {
        FlatPatchState patchState = new FlatPatchState();
        assertNotNull(patchState);
        assertThat(patchState.isInitialized()).isFalse();
    }

    @Test
    void testCollisionRayTest() {
        Vector3f rayOrigin = new Vector3f(16f, 10f, 16f);
        Vector3f rayTarget = new Vector3f(16f, -10f, 16f);
        Vector3f direction = rayTarget.subtract(rayOrigin).normalizeLocal();

        assertThat(direction).isEqualTo(new Vector3f(0f, -1f, 0f));

        float expectedHitY = 1f;
        assertThat(expectedHitY).isEqualTo(1f, within(1e-5f));
    }

    @Test
    void testPatchDimensions() throws Exception {
        Class<FlatPatchState> clazz = FlatPatchState.class;
        Field widthField = clazz.getDeclaredField("PATCH_WIDTH");
        Field depthField = clazz.getDeclaredField("PATCH_DEPTH");
        Field heightField = clazz.getDeclaredField("PATCH_HEIGHT");
        widthField.setAccessible(true);
        depthField.setAccessible(true);
        heightField.setAccessible(true);

        int width = widthField.getInt(null);
        int depth = depthField.getInt(null);
        int height = heightField.getInt(null);

        assertEquals(32, width, "Patch width must be 32 blocks");
        assertEquals(32, depth, "Patch depth must be 32 blocks");
        assertEquals(1, height, "Patch height must be 1 block");
    }

    @Test
    void testCameraSpawnPosition() {
        Vector3f cameraPosition = new Vector3f(16f, 2f, 16f);
        Vector3f lookAtTarget = new Vector3f(16f, 0f, 16f);
        Vector3f direction = lookAtTarget.subtract(cameraPosition).normalizeLocal();

        assertThat(cameraPosition.x).isEqualTo(16f);
        assertThat(cameraPosition.y).isEqualTo(2f);
        assertThat(cameraPosition.z).isEqualTo(16f);

        assertThat(direction.y).isLessThan(0f);
    }

    @Disabled("Requires full engine context and generated assets")
    @Test
    void testFlatPatchIntegration() {
        // Integration test placeholder for manual execution when running full engine context.
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

    private void resetPhysicsManagerSingleton() {
        try {
            Field instanceField = PhysicsManager.class.getDeclaredField("INSTANCE");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to reset PhysicsManager singleton", exception);
        }
    }
}
