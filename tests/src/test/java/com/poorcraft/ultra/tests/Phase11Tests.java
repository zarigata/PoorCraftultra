package com.poorcraft.ultra.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.material.RenderState;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.poorcraft.ultra.voxel.BlockDefinition;
import com.poorcraft.ultra.voxel.BlockRegistry;
import com.poorcraft.ultra.voxel.BlockType;
import com.poorcraft.ultra.voxel.Chunk;
import com.poorcraft.ultra.voxel.ChunkPos;
import com.poorcraft.ultra.voxel.ChunkRenderer;
import com.poorcraft.ultra.voxel.ChunkStorage;
import com.poorcraft.ultra.voxel.GreedyMesher;
import com.poorcraft.ultra.voxel.TextureAtlas;
import java.lang.reflect.Field;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Phase 1.1 verification tests covering greedy meshing and chunk rendering.
 */
public class Phase11Tests {

    private static final Path ASSETS_DIR = Path.of("assets");
    private static final Path TEXTURES_DIR = ASSETS_DIR.resolve("textures");

    private AssetManager assetManager;
    private TextureAtlas textureAtlas;
    private BlockRegistry blockRegistry;

    @BeforeEach
    void setUp() {
        resetTextureAtlasSingleton();
        resetBlockRegistrySingleton();
        resetChunkRendererSingleton();

        assetManager = createAssetManager();
        assumeAssetsPresent();
        textureAtlas = TextureAtlas.load(assetManager);
        blockRegistry = BlockRegistry.load(textureAtlas);
    }

    @AfterEach
    void tearDown() {
        resetTextureAtlasSingleton();
        resetBlockRegistrySingleton();
        resetChunkRendererSingleton();
    }

    @Nested
    class GreedyMesherTests {

        @Test
        void emptyChunkProducesEmptyMesh() {
            ChunkStorage storage = new ChunkStorage();
            Chunk chunk = new Chunk(new ChunkPos(0, 0));
            storage.putChunk(chunk);

            Mesh mesh = GreedyMesher.generateMesh(chunk, storage, blockRegistry, textureAtlas);

            assertThat(mesh.getVertexCount()).isZero();
            assertThat(mesh.getTriangleCount()).isZero();
        }

        @Test
        void singleBlockGeneratesSixFaces() {
            ChunkStorage storage = new ChunkStorage();
            Chunk chunk = new Chunk(new ChunkPos(0, 0));
            chunk.setBlock(1, 1, 1, BlockType.STONE.getId());
            storage.putChunk(chunk);

            Mesh mesh = GreedyMesher.generateMesh(chunk, storage, blockRegistry, textureAtlas);

            assertThat(mesh.getTriangleCount()).isEqualTo(12);
            assertThat(mesh.getVertexCount()).isEqualTo(24);
        }

        @Test
        void flatTerrainMergesTopFaces() {
            ChunkStorage storage = new ChunkStorage();
            Chunk chunk = new Chunk(new ChunkPos(0, 0));
            for (int x = 0; x < Chunk.CHUNK_SIZE_X; x++) {
                for (int z = 0; z < Chunk.CHUNK_SIZE_Z; z++) {
                    chunk.setBlock(x, 0, z, BlockType.DIRT.getId());
                    chunk.setBlock(x, 1, z, BlockType.GRASS.getId());
                }
            }
            storage.putChunk(chunk);

            Mesh mesh = GreedyMesher.generateMesh(chunk, storage, blockRegistry, textureAtlas);
            int naiveTriangles = countNaiveTriangles(chunk, storage, blockRegistry);

            assertThat(mesh.getTriangleCount()).isLessThan(20);
            double reduction = 1.0 - (mesh.getTriangleCount() / (double) naiveTriangles);
            assertThat(reduction).isGreaterThanOrEqualTo(0.40);
        }

        @Test
        void solidCuboidShellHasSignificantReduction() {
            ChunkStorage storage = new ChunkStorage();
            Chunk chunk = new Chunk(new ChunkPos(0, 0));

            int min = 2;
            int max = Chunk.CHUNK_SIZE_X - 3;
            int bottom = 2;
            int top = bottom + 5;

            for (int x = min; x <= max; x++) {
                for (int z = min; z <= max; z++) {
                    for (int y = bottom; y <= top; y++) {
                        boolean onShell = x == min || x == max || z == min || z == max || y == bottom || y == top;
                        if (onShell) {
                            chunk.setBlock(x, y, z, BlockType.STONE.getId());
                        }
                    }
                }
            }
            storage.putChunk(chunk);

            Mesh mesh = GreedyMesher.generateMesh(chunk, storage, blockRegistry, textureAtlas);
            int naiveTriangles = countNaiveTriangles(chunk, storage, blockRegistry);

            assertThat(mesh.getTriangleCount()).isGreaterThan(0);
            assertThat(mesh.getTriangleCount()).isLessThan(naiveTriangles);
            double reduction = 1.0 - (mesh.getTriangleCount() / (double) naiveTriangles);
            assertThat(reduction).isGreaterThanOrEqualTo(0.40);
        }

        @Test
        void crossChunkNeighborCullsSharedFaces() {
            Chunk chunkIsolated = new Chunk(new ChunkPos(0, 0));
            for (int z = 0; z < Chunk.CHUNK_SIZE_Z; z++) {
                for (int y = 0; y < 4; y++) {
                    chunkIsolated.setBlock(Chunk.CHUNK_SIZE_X - 1, y, z, BlockType.STONE.getId());
                }
            }

            ChunkStorage storageWithoutNeighbor = new ChunkStorage();
            storageWithoutNeighbor.putChunk(chunkIsolated);
            Mesh meshWithoutNeighbor = GreedyMesher.generateMesh(chunkIsolated, storageWithoutNeighbor, blockRegistry, textureAtlas);
            assertTrue(hasVertexWithCoordinate(meshWithoutNeighbor, 0, Chunk.CHUNK_SIZE_X));

            Chunk chunkMain = new Chunk(new ChunkPos(0, 0));
            Chunk chunkNeighbor = new Chunk(new ChunkPos(1, 0));
            for (int z = 0; z < Chunk.CHUNK_SIZE_Z; z++) {
                for (int y = 0; y < 4; y++) {
                    chunkMain.setBlock(Chunk.CHUNK_SIZE_X - 1, y, z, BlockType.STONE.getId());
                    chunkNeighbor.setBlock(0, y, z, BlockType.STONE.getId());
                }
            }

            ChunkStorage storageWithNeighbor = new ChunkStorage();
            storageWithNeighbor.putChunk(chunkMain);
            storageWithNeighbor.putChunk(chunkNeighbor);
            Mesh meshWithNeighbor = GreedyMesher.generateMesh(chunkMain, storageWithNeighbor, blockRegistry, textureAtlas);

            assertFalse(hasVertexWithCoordinate(meshWithNeighbor, 0, Chunk.CHUNK_SIZE_X));
            assertThat(meshWithNeighbor.getTriangleCount()).isLessThan(meshWithoutNeighbor.getTriangleCount());
        }

        @Test
        void transparentBlocksRetainBoundaryFaces() {
            int solidTriangles = trianglesForPair(BlockType.STONE, BlockType.STONE);
            int glassTriangles = trianglesForPair(BlockType.STONE, BlockType.GLASS);

            assertThat(glassTriangles).isGreaterThan(solidTriangles);
            assertThat(glassTriangles).isEqualTo(24);
        }

        @Test
        void grassBlockUsesDistinctAtlasIndices() {
            ChunkStorage storage = new ChunkStorage();
            Chunk chunk = new Chunk(new ChunkPos(0, 0));
            chunk.setBlock(4, 5, 6, BlockType.GRASS.getId());
            storage.putChunk(chunk);

            Mesh mesh = GreedyMesher.generateMesh(chunk, storage, blockRegistry, textureAtlas);

            Map<String, float[]> faceUVs = collectFaceUVs(mesh);
            BlockDefinition grass = blockRegistry.getDefinition(BlockType.GRASS);

            float[] topUV = textureAtlas.getUVCoordinates(grass.getAtlasIndex("top"));
            float[] bottomUV = textureAtlas.getUVCoordinates(grass.getAtlasIndex("bottom"));
            float[] sideUV = textureAtlas.getUVCoordinates(grass.getAtlasIndex("side"));

            assertThat(faceUVs).containsKeys("POS_Y", "NEG_Y", "POS_X", "NEG_X", "POS_Z", "NEG_Z");

            assertUVsMatch(faceUVs.get("POS_Y"), topUV);
            assertUVsMatch(faceUVs.get("NEG_Y"), bottomUV);
            assertUVsMatch(faceUVs.get("POS_X"), sideUV);
            assertUVsMatch(faceUVs.get("NEG_X"), sideUV);
            assertUVsMatch(faceUVs.get("POS_Z"), sideUV);
            assertUVsMatch(faceUVs.get("NEG_Z"), sideUV);
        }
    }

    @Nested
    class ChunkRendererTests {

        @Test
        void initializationCreatesRootNodeAndMaterial() {
            ChunkStorage storage = new ChunkStorage();

            Node rootNode = new Node("Root");
            SimpleApplicationAdapter application = new SimpleApplicationAdapter(assetManager, rootNode);

            ChunkRenderer renderer = ChunkRenderer.initialize(application, storage, blockRegistry, textureAtlas, 4);

            assertTrue(ChunkRenderer.isInitialized());
            assertThat(renderer.getChunkRootNode().getName()).isEqualTo("ChunkRoot");
            assertThat(rootNode.getChild("ChunkRoot")).isSameAs(renderer.getChunkRootNode());
        }

        @Test
        void renderChunkAttachesGeometryAndClearsDirtyFlag() {
            ChunkStorage storage = new ChunkStorage();
            Chunk chunk = new Chunk(new ChunkPos(0, 0));
            chunk.setBlock(0, 0, 0, BlockType.STONE.getId());
            storage.putChunk(chunk);

            Node rootNode = new Node("Root");
            SimpleApplicationAdapter application = new SimpleApplicationAdapter(assetManager, rootNode);
            ChunkRenderer renderer = ChunkRenderer.initialize(application, storage, blockRegistry, textureAtlas, 8);

            renderer.renderChunk(chunk);

            assertFalse(chunk.isDirty());
            assertThat(renderer.getRenderedChunkCount()).isEqualTo(1);
            assertThat(renderer.getChunkRootNode().getChildren()).hasSize(1);
        }

        @Test
        void updateChunkRefreshesMeshForDirtyChunk() {
            ChunkStorage storage = new ChunkStorage();
            Chunk chunk = new Chunk(new ChunkPos(0, 0));
            chunk.setBlock(0, 0, 0, BlockType.STONE.getId());
            storage.putChunk(chunk);

            Node rootNode = new Node("Root");
            SimpleApplicationAdapter application = new SimpleApplicationAdapter(assetManager, rootNode);
            ChunkRenderer renderer = ChunkRenderer.initialize(application, storage, blockRegistry, textureAtlas, 8);

            renderer.renderChunk(chunk);
            Geometry geometry = (Geometry) renderer.getChunkRootNode().getChild(0);
            Mesh initialMesh = geometry.getMesh();
            int initialTriangles = initialMesh.getTriangleCount();

            chunk.setBlock(1, 0, 0, BlockType.GLASS.getId());
            renderer.updateChunk(chunk);

            Mesh updatedMesh = geometry.getMesh();
            int updatedTriangles = updatedMesh.getTriangleCount();
            assertTrue(updatedTriangles >= initialTriangles);
            assertThat(updatedMesh).isNotSameAs(initialMesh);
            assertFalse(chunk.isDirty());
        }

        @Test
        void updateChunkDetachesEmptyGeometry() {
            ChunkStorage storage = new ChunkStorage();
            Chunk chunk = new Chunk(new ChunkPos(0, 0));
            chunk.setBlock(0, 0, 0, BlockType.STONE.getId());
            storage.putChunk(chunk);

            Node rootNode = new Node("Root");
            SimpleApplicationAdapter application = new SimpleApplicationAdapter(assetManager, rootNode);
            ChunkRenderer renderer = ChunkRenderer.initialize(application, storage, blockRegistry, textureAtlas, 8);

            renderer.renderChunk(chunk);
            chunk.setBlock(0, 0, 0, BlockType.AIR.getId());
            renderer.updateChunk(chunk);

            assertThat(renderer.getRenderedChunkCount()).isZero();
            assertThat(renderer.getChunkRootNode().getChildren()).isEmpty();
        }

        @Test
        void removeChunkDetachesGeometry() {
            ChunkStorage storage = new ChunkStorage();
            Chunk chunk = new Chunk(new ChunkPos(0, 0));
            chunk.setBlock(0, 0, 0, BlockType.STONE.getId());
            storage.putChunk(chunk);

            Node rootNode = new Node("Root");
            SimpleApplicationAdapter application = new SimpleApplicationAdapter(assetManager, rootNode);
            ChunkRenderer renderer = ChunkRenderer.initialize(application, storage, blockRegistry, textureAtlas, 8);

            renderer.renderChunk(chunk);
            renderer.removeChunk(chunk.getPosition());

            assertThat(renderer.getRenderedChunkCount()).isZero();
            assertThat(renderer.getChunkRootNode().getChildren()).isEmpty();
        }

        @Test
        void setWireframeTogglesRenderState() {
            ChunkStorage storage = new ChunkStorage();

            Chunk chunk = new Chunk(new ChunkPos(0, 0));
            chunk.setBlock(0, 0, 0, BlockType.STONE.getId());
            storage.putChunk(chunk);

            Node rootNode = new Node("Root");
            SimpleApplicationAdapter application = new SimpleApplicationAdapter(assetManager, rootNode);
            ChunkRenderer renderer = ChunkRenderer.initialize(application, storage, blockRegistry, textureAtlas, 8);

            renderer.renderChunk(chunk);
            renderer.setWireframe(true);

            RenderState state = ((Geometry) renderer.getChunkRootNode().getChild(0)).getMaterial().getAdditionalRenderState();
            assertTrue(state.isWireframe());

            renderer.setWireframe(false);
            assertFalse(state.isWireframe());
        }

        @Test
        void updateRenderDistanceRespectsChebyshevBound() {
            ChunkStorage storage = new ChunkStorage();

            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    Chunk chunk = new Chunk(new ChunkPos(x, z));
                    chunk.setBlock(0, 0, 0, BlockType.STONE.getId());
                    storage.putChunk(chunk);
                }
            }

            Node rootNode = new Node("Root");
            SimpleApplicationAdapter application = new SimpleApplicationAdapter(assetManager, rootNode);
            ChunkRenderer renderer = ChunkRenderer.initialize(application, storage, blockRegistry, textureAtlas, 8);

            storage.getAllChunks().forEach(renderer::renderChunk);
            assertThat(renderer.getRenderedChunkCount()).isGreaterThan(9);

            Camera camera = new Camera(800, 600);
            camera.setLocation(new Vector3f(0.5f, 80f, 0.5f));
            renderer.updateRenderDistance(camera, 1);

            int allowed = (int) Math.pow((2 * 1 + 1), 2);
            assertThat(renderer.getRenderedChunkCount()).isLessThanOrEqualTo(allowed);
            for (Spatial spatial : renderer.getChunkRootNode().getChildren()) {
                ChunkPos pos = extractChunkPos(spatial.getName());
                int distance = Math.max(Math.abs(pos.x()), Math.abs(pos.z()));
                assertThat(distance).isLessThanOrEqualTo(1);
            }

            camera.setLocation(new Vector3f(Chunk.CHUNK_SIZE_X * 2 + 0.5f, 80f, 0.5f));
            renderer.updateCameraPosition(camera);

            assertThat(renderer.getRenderedChunkCount()).isLessThanOrEqualTo(allowed);
            assertThat(renderer.getChunkRootNode().getChild("Chunk_0_0")).isNull();
            assertThat(renderer.getChunkRootNode().getChild("Chunk_2_0")).isNotNull();
        }

        @Disabled("Requires live jME render context to validate frustum culling. Run manually within application harness.")
        @Test
        void frustumCullingRequiresLiveContext() {
            // Manual validation: launch the game, enable chunk renderer debug logs, move camera to ensure
            // chunks outside the frustum are reported as culled while visible chunks remain rendered.
        }
    }

    private AssetManager createAssetManager() {
        DesktopAssetManager manager = new DesktopAssetManager();
        manager.registerLocator("assets", FileLocator.class);
        return manager;
    }

    private void assumeAssetsPresent() {
        Assumptions.assumeTrue(Files.exists(TEXTURES_DIR.resolve("blocks_atlas.png")),
                "blocks_atlas.png missing – run :tools:generateAssets");
        Assumptions.assumeTrue(Files.exists(TEXTURES_DIR.resolve("blocks_atlas.json")),
                "blocks_atlas.json missing – run :tools:generateAssets");
    }

    private int trianglesForPair(BlockType primary, BlockType neighbor) {
        ChunkStorage storage = new ChunkStorage();
        Chunk chunk = new Chunk(new ChunkPos(0, 0));
        chunk.setBlock(0, 0, 0, primary.getId());
        chunk.setBlock(1, 0, 0, neighbor.getId());
        storage.putChunk(chunk);
        Mesh mesh = GreedyMesher.generateMesh(chunk, storage, blockRegistry, textureAtlas);
        return mesh.getTriangleCount();
    }

    private Map<String, float[]> collectFaceUVs(Mesh mesh) {
        Map<String, float[]> faceUVs = new HashMap<>();
        FloatBuffer normalBuffer = mesh.getFloatBuffer(VertexBuffer.Type.Normal).duplicate();
        FloatBuffer uvBuffer = mesh.getFloatBuffer(VertexBuffer.Type.TexCoord).duplicate();
        int vertexCount = mesh.getVertexCount();

        for (int i = 0; i < vertexCount; i += 4) {
            float nx = normalBuffer.get(i * 3);
            float ny = normalBuffer.get(i * 3 + 1);
            float nz = normalBuffer.get(i * 3 + 2);
            String orientation = orientationFromNormal(nx, ny, nz);

            float minU = Float.MAX_VALUE;
            float minV = Float.MAX_VALUE;
            float maxU = -Float.MAX_VALUE;
            float maxV = -Float.MAX_VALUE;
            for (int j = 0; j < 4; j++) {
                float u = uvBuffer.get((i + j) * 2);
                float v = uvBuffer.get((i + j) * 2 + 1);
                minU = Math.min(minU, u);
                minV = Math.min(minV, v);
                maxU = Math.max(maxU, u);
                maxV = Math.max(maxV, v);
            }
            faceUVs.putIfAbsent(orientation, new float[]{minU, minV, maxU, maxV});
        }
        return faceUVs;
    }

    private String orientationFromNormal(float nx, float ny, float nz) {
        float epsilon = 1e-5f;
        if (Math.abs(nx - 1f) < epsilon) {
            return "POS_X";
        }
        if (Math.abs(nx + 1f) < epsilon) {
            return "NEG_X";
        }
        if (Math.abs(ny - 1f) < epsilon) {
            return "POS_Y";
        }
        if (Math.abs(ny + 1f) < epsilon) {
            return "NEG_Y";
        }
        if (Math.abs(nz - 1f) < epsilon) {
            return "POS_Z";
        }
        if (Math.abs(nz + 1f) < epsilon) {
            return "NEG_Z";
        }
        throw new IllegalStateException("Unexpected normal vector: (" + nx + ", " + ny + ", " + nz + ")");
    }

    private boolean hasVertexWithCoordinate(Mesh mesh, int axis, float value) {
        FloatBuffer positionBuffer = mesh.getFloatBuffer(VertexBuffer.Type.Position).duplicate();
        float epsilon = 1e-5f;
        for (int i = 0; i < mesh.getVertexCount(); i++) {
            float coordinate = positionBuffer.get(i * 3 + axis);
            if (Math.abs(coordinate - value) < epsilon) {
                return true;
            }
        }
        return false;
    }

    private int countNaiveTriangles(Chunk chunk, ChunkStorage storage, BlockRegistry registry) {
        int[] dimensions = {Chunk.CHUNK_SIZE_X, Chunk.CHUNK_SIZE_Y, Chunk.CHUNK_SIZE_Z};
        int[] coordinate = new int[3];
        int[] offset = new int[3];
        int chunkOriginX = chunk.getPosition().getWorldX();
        int chunkOriginZ = chunk.getPosition().getWorldZ();
        int triangles = 0;

        for (int axis = 0; axis < 3; axis++) {
            int axisU = (axis + 1) % 3;
            int axisV = (axis + 2) % 3;
            int sizeU = dimensions[axisU];
            int sizeV = dimensions[axisV];

            for (int i = 0; i < 3; i++) {
                coordinate[i] = 0;
                offset[i] = 0;
            }
            offset[axis] = 1;

            for (coordinate[axis] = -1; coordinate[axis] < dimensions[axis]; coordinate[axis]++) {
                for (coordinate[axisV] = 0; coordinate[axisV] < sizeV; coordinate[axisV]++) {
                    for (coordinate[axisU] = 0; coordinate[axisU] < sizeU; coordinate[axisU]++) {
                        short blockA = sampleBlock(chunk, storage, chunkOriginX, chunkOriginZ,
                                coordinate[0], coordinate[1], coordinate[2]);
                        short blockB = sampleBlock(chunk, storage, chunkOriginX, chunkOriginZ,
                                coordinate[0] + offset[0], coordinate[1] + offset[1], coordinate[2] + offset[2]);

                        BlockDefinition defA = registry.getDefinition(blockA);
                        BlockDefinition defB = registry.getDefinition(blockB);
                        if (shouldRenderFace(defA, defB)) {
                            triangles += 2;
                        } else if (shouldRenderFace(defB, defA)) {
                            triangles += 2;
                        }
                    }
                }
            }
        }
        return triangles;
    }

    private short sampleBlock(Chunk chunk,
                              ChunkStorage storage,
                              int chunkOriginX,
                              int chunkOriginZ,
                              int x,
                              int y,
                              int z) {
        if (y < 0 || y >= Chunk.CHUNK_SIZE_Y) {
            return BlockType.AIR.getId();
        }
        if (x >= 0 && x < Chunk.CHUNK_SIZE_X && z >= 0 && z < Chunk.CHUNK_SIZE_Z) {
            return chunk.getBlock(x, y, z);
        }
        int worldX = chunkOriginX + x;
        int worldZ = chunkOriginZ + z;
        return storage.getBlock(worldX, y, worldZ);
    }

    private boolean shouldRenderFace(BlockDefinition definition, BlockDefinition neighborDefinition) {
        if (definition == null || definition.getType() == BlockType.AIR) {
            return false;
        }
        if (neighborDefinition == null) {
            return true;
        }
        BlockType neighborType = neighborDefinition.getType();
        if (neighborType == BlockType.AIR) {
            return true;
        }
        if (!neighborDefinition.isSolid()) {
            return true;
        }
        if (definition.isTransparent() && neighborDefinition.isTransparent()) {
            return definition.getId() != neighborDefinition.getId();
        }
        if (definition.isTransparent() && !neighborDefinition.isTransparent()) {
            return true;
        }
        if (!definition.isTransparent() && neighborDefinition.isTransparent()) {
            return true;
        }
        return false;
    }

    private ChunkPos extractChunkPos(String geometryName) {
        String[] parts = geometryName.replace("Chunk_", "").split("_");
        int x = Integer.parseInt(parts[0]);
        int z = Integer.parseInt(parts[1]);
        return new ChunkPos(x, z);
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

    private void resetBlockRegistrySingleton() {
        try {
            Field instanceField = BlockRegistry.class.getDeclaredField("INSTANCE");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to reset BlockRegistry singleton", exception);
        }
    }

    private void resetChunkRendererSingleton() {
        try {
            Field instanceField = ChunkRenderer.class.getDeclaredField("INSTANCE");
            instanceField.setAccessible(true);
            ChunkRenderer renderer = (ChunkRenderer) instanceField.get(null);
            if (renderer != null) {
                renderer.removeAllChunks();
            }
            instanceField.set(null, null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to reset ChunkRenderer singleton", exception);
        }
    }

    private void assertUVsMatch(float[] actual, float[] expected) {
        assertThat(actual)
                .isNotNull()
                .hasSize(4)
                .containsExactly(expected[0], expected[1], expected[2], expected[3]);
    }

    private static final class SimpleApplicationAdapter extends SimpleApplication {

        private final AssetManager assetManager;
        private final Node rootNode;

        private SimpleApplicationAdapter(AssetManager assetManager, Node rootNode) {
            this.assetManager = assetManager;
            this.rootNode = rootNode;
        }

        @Override
        public void simpleInitApp() {
            // Not required for headless tests.
        }

        @Override
        public AssetManager getAssetManager() {
            return assetManager;
        }

        @Override
        public Node getRootNode() {
            return rootNode;
        }
    }
}
