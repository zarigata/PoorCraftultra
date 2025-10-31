package com.poorcraft.ultra.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.poorcraft.ultra.voxel.BlockDefinition;
import com.poorcraft.ultra.voxel.BlockRegistry;
import com.poorcraft.ultra.voxel.BlockType;
import com.poorcraft.ultra.voxel.Chunk;
import com.poorcraft.ultra.voxel.ChunkPos;
import com.poorcraft.ultra.voxel.ChunkStorage;
import com.poorcraft.ultra.voxel.TextureAtlas;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Phase 1.0 verification tests.
 */
public class Phase10Tests {

    private static final Path ASSETS_DIR = Path.of("assets");
    private static final Path TEXTURES_DIR = ASSETS_DIR.resolve("textures");

    @BeforeEach
    void setUp() {
        resetTextureAtlasSingleton();
        resetBlockRegistrySingleton();
    }

    @AfterEach
    void tearDown() {
        resetTextureAtlasSingleton();
        resetBlockRegistrySingleton();
    }

    @Nested
    class BlockTypeTests {

        @Test
        void testEnumValues() {
            assertEquals(11, BlockType.values().length);
            assertEquals((short) 0, BlockType.AIR.getId());
            assertEquals((short) 10, BlockType.WATER.getId());

            assertThat(BlockType.AIR.getName()).isEqualTo("air");
            assertThat(BlockType.GRASS.getName()).isEqualTo("grass");
            assertThat(BlockType.LOG.getName()).isEqualTo("log");

            assertThat(BlockType.AIR.isSolid()).isFalse();
            assertThat(BlockType.AIR.isTransparent()).isTrue();
            assertThat(BlockType.STONE.isSolid()).isTrue();
            assertThat(BlockType.GLASS.isTransparent()).isTrue();
            assertThat(BlockType.WATER.isSolid()).isFalse();
        }

        @Test
        void testLookupById() {
            for (short id = 0; id <= 10; id++) {
                assertNotNull(BlockType.fromId(id), "BlockType.fromId should resolve id " + id);
            }
            assertThat(BlockType.fromId((short) -1)).isNull();
            assertThat(BlockType.fromId((short) 99)).isNull();
        }

        @Test
        void testLookupByName() {
            for (BlockType type : BlockType.values()) {
                assertThat(BlockType.fromName(type.getName())).isEqualTo(type);
                assertThat(BlockType.fromName(type.getName().toUpperCase(Locale.ROOT))).isEqualTo(type);
            }
            assertThat(BlockType.fromName(null)).isNull();
            assertThat(BlockType.fromName(" ")).isNull();
            assertThat(BlockType.fromName("unknown")).isNull();
        }
    }

    @Nested
    class BlockDefinitionTests {

        @Test
        void testAirDefinition() {
            BlockDefinition air = BlockDefinition.air();
            assertThat(air.getType()).isEqualTo(BlockType.AIR);
            assertThat(air.getAtlasIndex("all")).isEqualTo(-1);
            assertThat(air.hasMultipleFaces()).isFalse();
        }

        @Test
        void testUniformDefinition() {
            BlockDefinition stone = BlockDefinition.uniform(BlockType.STONE, 5);
            assertThat(stone.getAtlasIndex("all")).isEqualTo(5);
            assertThat(stone.getAtlasIndex("top")).isEqualTo(5);
            assertThat(stone.hasMultipleFaces()).isFalse();
        }

        @Test
        void testMultifaceDefinition() {
            BlockDefinition grass = BlockDefinition.multiface(BlockType.GRASS, 1, 2, 3);
            assertThat(grass.getAtlasIndex("top")).isEqualTo(1);
            assertThat(grass.getAtlasIndex("bottom")).isEqualTo(2);
            assertThat(grass.getAtlasIndex("side")).isEqualTo(3);
            assertThat(grass.hasMultipleFaces()).isTrue();
        }

        @Test
        void testInvalidFaceFallbacks() {
            BlockDefinition definition = BlockDefinition.multiface(BlockType.LOG, 10, 11, 12);
            assertThat(definition.getAtlasIndex("north")).isEqualTo(12);
            assertThat(definition.getAtlasIndex("unknown")).isEqualTo(12);
        }
    }

    @Nested
    class BlockRegistryTests {

        @Test
        void testRegistryLoadAndContents() {
            assumeAssetsPresent();
            AssetManager manager = createAssetManager();
            TextureAtlas atlas = TextureAtlas.load(manager);
            BlockRegistry registry = BlockRegistry.load(atlas);

            assertTrue(BlockRegistry.isInitialized());
            assertEquals(11, registry.getBlockCount());

            for (BlockType type : BlockType.values()) {
                BlockDefinition definition = registry.getDefinition(type);
                assertNotNull(definition, "Definition missing for type " + type);
                assertEquals(type, definition.getType());
            }

            BlockDefinition logDefinition = registry.getDefinition(BlockType.LOG);
            assertNotNull(logDefinition, "Log block definition should be registered");

            Map<String, Map<String, Integer>> mapping = atlas.getMapping();
            assertThat(mapping).containsKey("log");
            Map<String, Integer> logFaces = mapping.get("log");
            assertThat(logFaces).containsKeys("top", "side");
            assertThat(logDefinition.getAtlasIndex("top")).isEqualTo(logFaces.get("top"));
            assertThat(logDefinition.getAtlasIndex("side")).isEqualTo(logFaces.get("side"));

            BlockDefinition fallbackById = registry.getDefinition((short) 99);
            assertThat(fallbackById.getType()).isEqualTo(BlockType.AIR);

            BlockDefinition fallbackByName = registry.getDefinition("missing");
            assertThat(fallbackByName.getType()).isEqualTo(BlockType.AIR);
        }

        @Test
        void testRegistryRequiresAtlas() {
            assertThatThrownBy(() -> BlockRegistry.getInstance())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not initialized");
        }

        @Test
        void testAtlasValidation() {
            assumeAssetsPresent();
            AssetManager manager = createAssetManager();
            TextureAtlas atlas = TextureAtlas.load(manager);
            BlockRegistry registry = BlockRegistry.load(atlas);

            for (BlockDefinition definition : registry.getDefinitionsById().values()) {
                if (definition.getType() == BlockType.AIR) {
                    continue;
                }
                int top = definition.getAtlasIndex("top");
                int bottom = definition.getAtlasIndex("bottom");
                int side = definition.getAtlasIndex("side");
                int all = definition.getAtlasIndex("all");

                assertWithinAtlasRange(top);
                assertWithinAtlasRange(bottom);
                assertWithinAtlasRange(side);
                assertWithinAtlasRange(all);
            }
        }

        private void assertWithinAtlasRange(int index) {
            if (index < 0) {
                return;
            }
            assertThat(index).isBetween(0, 63);
        }
    }

    @Nested
    class ChunkPosTests {

        @Test
        void testPackedKeyRoundTrip() {
            ChunkPos pos = new ChunkPos(5, -3);
            long packed = pos.asLong();
            ChunkPos unpacked = ChunkPos.fromLong(packed);
            assertThat(unpacked).isEqualTo(pos);
        }

        @Test
        void testWorldCoordinateConversion() {
            ChunkPos origin = ChunkPos.fromWorldCoordinates(0, 0);
            assertEquals(0, origin.x());
            assertEquals(0, origin.z());

            ChunkPos pos = ChunkPos.fromWorldCoordinates(25, -10);
            assertEquals(1, pos.x());
            assertEquals(-1, pos.z());
        }

        @Test
        void testNeighborLookup() {
            ChunkPos pos = new ChunkPos(0, 0);
            assertThat(pos.getNeighbor(1, 0)).isEqualTo(new ChunkPos(1, 0));
            assertThat(pos.getNeighbor(-1, -1)).isEqualTo(new ChunkPos(-1, -1));
        }
    }

    @Nested
    class ChunkTests {

        @Test
        void testEmptyChunkInitialization() {
            Chunk chunk = new Chunk(new ChunkPos(0, 0));
            assertEquals(Chunk.TOTAL_BLOCKS, chunk.getBlockCount());
            assertTrue(chunk.isDirty());
            for (int y = 0; y < Chunk.CHUNK_SIZE_Y; y += 64) {
                assertEquals(BlockType.AIR.getId(), chunk.getBlock(0, y, 0));
            }
        }

        @Test
        void testSetGetBlockRoundTrip() {
            Chunk chunk = new Chunk(new ChunkPos(0, 0));
            short previous = chunk.setBlock(5, 100, 7, BlockType.STONE.getId());
            assertEquals(BlockType.AIR.getId(), previous);
            assertEquals(BlockType.STONE.getId(), chunk.getBlock(5, 100, 7));
            assertTrue(chunk.isDirty());

            chunk.clearDirty();
            chunk.setBlock(5, 100, 7, BlockType.STONE.getId());
            assertTrue(!chunk.isDirty(), "Chunk should remain clean when value unchanged");
        }

        @Test
        void testFill() {
            Chunk chunk = new Chunk(new ChunkPos(0, 0));
            chunk.fill(BlockType.DIRT.getId());
            assertEquals(BlockType.DIRT.getId(), chunk.getBlock(1, 0, 1));
            assertTrue(chunk.isDirty());
        }

        @Test
        void testBoundsChecking() {
            Chunk chunk = new Chunk(new ChunkPos(0, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> chunk.getBlock(-1, 0, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> chunk.getBlock(16, 0, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> chunk.getBlock(0, -1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> chunk.getBlock(0, 256, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> chunk.getBlock(0, 0, -1));
            assertThrows(IndexOutOfBoundsException.class, () -> chunk.getBlock(0, 0, 16));
        }
    }

    @Nested
    class ChunkStorageTests {

        @Test
        void testPutGetRemoveChunk() {
            ChunkStorage storage = new ChunkStorage();
            Chunk chunk = new Chunk(new ChunkPos(1, 2));
            storage.putChunk(chunk);
            assertThat(storage.containsChunk(chunk.getPosition())).isTrue();
            assertThat(storage.getLoadedChunkCount()).isEqualTo(1);
            assertThat(storage.getChunk(1, 2)).isSameAs(chunk);

            Chunk removed = storage.removeChunk(chunk.getPosition());
            assertThat(removed).isSameAs(chunk);
            assertThat(storage.containsChunk(chunk.getPosition())).isFalse();
        }

        @Test
        void testWorldCoordinateAccess() {
            ChunkStorage storage = new ChunkStorage();
            Chunk chunk = new Chunk(new ChunkPos(0, 0));
            storage.putChunk(chunk);

            storage.setBlock(5, 100, 7, BlockType.GRASS.getId());
            assertThat(storage.getBlock(5, 100, 7)).isEqualTo(BlockType.GRASS.getId());
            assertThat(storage.getBlock(32, 0, 32)).isEqualTo(BlockType.AIR.getId());
        }

        @Test
        void testConcurrency() throws InterruptedException {
            ChunkStorage storage = new ChunkStorage();
            int threadCount = 8;
            int chunksPerThread = 16;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int t = 0; t < threadCount; t++) {
                final int threadIndex = t;
                executor.submit(() -> {
                    try {
                        for (int i = 0; i < chunksPerThread; i++) {
                            int chunkX = threadIndex * chunksPerThread + i;
                            Chunk chunk = new Chunk(new ChunkPos(chunkX, threadIndex));
                            storage.putChunk(chunk);
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(10, TimeUnit.SECONDS);
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);

            assertThat(storage.getLoadedChunkCount()).isEqualTo(threadCount * chunksPerThread);
        }
    }

    private void assumeAssetsPresent() {
        Assumptions.assumeTrue(Files.exists(TEXTURES_DIR.resolve("blocks_atlas.png")),
                "blocks_atlas.png missing – run :tools:generateAssets");
        Assumptions.assumeTrue(Files.exists(TEXTURES_DIR.resolve("blocks_atlas.json")),
                "blocks_atlas.json missing – run :tools:generateAssets");
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

    private void resetBlockRegistrySingleton() {
        try {
            Field instanceField = BlockRegistry.class.getDeclaredField("INSTANCE");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to reset BlockRegistry singleton", exception);
        }
    }
}
