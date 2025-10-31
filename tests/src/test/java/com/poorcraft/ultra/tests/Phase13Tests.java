package com.poorcraft.ultra.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.poorcraft.ultra.voxel.BlockType;
import com.poorcraft.ultra.voxel.Chunk;
import com.poorcraft.ultra.voxel.ChunkPos;
import com.poorcraft.ultra.voxel.ChunkStorage;
import com.poorcraft.ultra.world.ChunkSerializer;
import com.poorcraft.ultra.world.RegionFile;
import com.poorcraft.ultra.world.RegionPos;
import com.poorcraft.ultra.world.WorldSaveManager;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class Phase13Tests {

    private static final int UNCOMPRESSED_CHUNK_SIZE_BYTES = (Integer.BYTES * 2)
            + (Chunk.TOTAL_BLOCKS * Short.BYTES) + Integer.BYTES;

    @TempDir
    Path tempDir;

    private Path savesDirectory;

    @BeforeEach
    void setUp() {
        resetWorldSaveManager();
        ChunkSerializer.setCompressionLevel(3);
        savesDirectory = tempDir.resolve("saves-test");
    }

    @AfterEach
    void tearDown() {
        resetWorldSaveManager();
    }

    @Test
    void testRegionPosFromChunkPos() {
        assertThat(RegionPos.fromChunkPos(new ChunkPos(35, -10))).isEqualTo(new RegionPos(1, -1));
        assertThat(RegionPos.fromChunkPos(new ChunkPos(0, 0))).isEqualTo(new RegionPos(0, 0));
        assertThat(RegionPos.fromChunkPos(new ChunkPos(31, 31))).isEqualTo(new RegionPos(0, 0));
        assertThat(RegionPos.fromChunkPos(new ChunkPos(32, 32))).isEqualTo(new RegionPos(1, 1));
        assertThat(RegionPos.fromChunkPos(new ChunkPos(-1, -1))).isEqualTo(new RegionPos(-1, -1));
        assertThat(RegionPos.fromChunkPos(new ChunkPos(-32, -32))).isEqualTo(new RegionPos(-1, -1));
    }

    @Test
    void testRegionPosGetFileName() {
        assertThat(new RegionPos(0, 0).getFileName()).isEqualTo("r.0.0.mca");
        assertThat(new RegionPos(-1, 2).getFileName()).isEqualTo("r.-1.2.mca");
        assertThat(new RegionPos(5, -7).getFileName()).isEqualTo("r.5.-7.mca");
    }

    @Test
    void testRegionPosGetChunkOffset() {
        RegionPos region = new RegionPos(0, 0);
        assertThat(region.getChunkOffset(new ChunkPos(0, 0))).isEqualTo(0);
        assertThat(region.getChunkOffset(new ChunkPos(31, 0))).isEqualTo(31);
        assertThat(region.getChunkOffset(new ChunkPos(0, 31))).isEqualTo(992);
        assertThat(region.getChunkOffset(new ChunkPos(31, 31))).isEqualTo(1023);
        assertThatThrownBy(() -> region.getChunkOffset(new ChunkPos(32, 0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testChunkSerializerRoundTrip() {
        ChunkPos pos = new ChunkPos(5, 10);
        Chunk chunk = new Chunk(pos);
        chunk.setBlock(0, 0, 0, BlockType.STONE.getId());
        chunk.setBlock(15, 255, 15, BlockType.GRASS.getId());

        byte[] serialized = ChunkSerializer.serialize(chunk);
        assertThat(serialized).isNotNull();
        assertThat(serialized.length).isGreaterThan(0);

        Chunk loaded = ChunkSerializer.deserialize(serialized, pos);
        assertThat(loaded.getPosition()).isEqualTo(pos);
        assertThat(loaded.isDirty()).isFalse();
        assertThat(loaded.getBlock(0, 0, 0)).isEqualTo(BlockType.STONE.getId());
        assertThat(loaded.getBlock(15, 255, 15)).isEqualTo(BlockType.GRASS.getId());

        Random random = new Random(42);
        for (int i = 0; i < 100; i++) {
            int x = random.nextInt(Chunk.CHUNK_SIZE_X);
            int y = random.nextInt(Chunk.CHUNK_SIZE_Y);
            int z = random.nextInt(Chunk.CHUNK_SIZE_Z);
            assertThat(loaded.getBlock(x, y, z)).isEqualTo(chunk.getBlock(x, y, z));
        }
    }

    @Test
    void testChunkSerializerChecksum() {
        ChunkPos pos = new ChunkPos(2, 3);
        Chunk chunk = new Chunk(pos);
        chunk.fill(BlockType.STONE.getId());

        byte[] data = ChunkSerializer.serialize(chunk);
        byte[] corrupted = data.clone();
        corrupted[corrupted.length / 2] ^= 0x7F;

        assertThatThrownBy(() -> ChunkSerializer.deserialize(corrupted, pos))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to deserialize chunk");
    }

    @Test
    void testChunkSerializerCompression() {
        ChunkPos pos = new ChunkPos(1, 1);
        Chunk chunk = new Chunk(pos);
        chunk.fill(BlockType.STONE.getId());

        byte[] compressed = ChunkSerializer.serialize(chunk);
        double ratio = 1.0 - (compressed.length / (double) UNCOMPRESSED_CHUNK_SIZE_BYTES);
        assertThat(ratio * 100d).isGreaterThanOrEqualTo(50.0);
    }

    @Test
    void testRegionFileCreation() throws IOException {
        Path regionPath = savesDirectory.resolve("region").resolve("r.0.0.mca");
        Files.createDirectories(regionPath.getParent());

        try (RegionFile regionFile = new RegionFile(regionPath, new RegionPos(0, 0))) {
            assertThat(Files.exists(regionPath)).isTrue();
            assertThat(Files.size(regionPath)).isGreaterThanOrEqualTo(RegionFileTestUtil.HEADER_SIZE_BYTES);
        }
    }

    @Test
    void testRegionFileWriteRead() throws IOException {
        Path regionPath = savesDirectory.resolve("region").resolve("r.0.0.mca");
        Files.createDirectories(regionPath.getParent());
        ChunkPos pos = new ChunkPos(0, 0);
        Chunk chunk = new Chunk(pos);
        chunk.fill(BlockType.DIRT.getId());
        byte[] data = ChunkSerializer.serialize(chunk);

        try (RegionFile regionFile = new RegionFile(regionPath, RegionPos.fromChunkPos(pos))) {
            regionFile.writeChunk(pos, data);
            byte[] loaded = regionFile.readChunk(pos);
            assertThat(loaded).isNotNull();
            assertThat(loaded).isEqualTo(data);
        }
    }

    @Test
    void testRegionFileMultipleChunks() throws IOException {
        Path regionPath = savesDirectory.resolve("region").resolve("r.0.0.mca");
        Files.createDirectories(regionPath.getParent());
        Map<ChunkPos, byte[]> stored = new HashMap<>();

        try (RegionFile regionFile = new RegionFile(regionPath, new RegionPos(0, 0))) {
            for (int i = 0; i < 10; i++) {
                ChunkPos pos = new ChunkPos(i * 3 % 32, i * 5 % 32);
                Chunk chunk = new Chunk(pos);
                chunk.fill((short) (BlockType.STONE.getId() + i));
                byte[] serialized = ChunkSerializer.serialize(chunk);
                regionFile.writeChunk(pos, serialized);
                stored.put(pos, serialized);
            }

            for (Map.Entry<ChunkPos, byte[]> entry : stored.entrySet()) {
                byte[] loaded = regionFile.readChunk(entry.getKey());
                assertThat(loaded).isEqualTo(entry.getValue());
            }
        }
    }

    @Test
    void testWorldSaveManagerInitialization() {
        ChunkStorage storage = new ChunkStorage();
        WorldSaveManager manager = WorldSaveManager.initialize(storage, "TestWorld",
                savesDirectory, true, 300f, 3);

        assertThat(WorldSaveManager.isInitialized()).isTrue();
        assertThat(manager.getWorldDirectory()).isEqualTo(savesDirectory.resolve("TestWorld"));
        assertThat(Files.exists(manager.getRegionDirectory())).isTrue();
        manager.shutdown();
    }

    @Test
    void testWorldSaveManagerSaveLoad() {
        ChunkStorage storage = new ChunkStorage();
        Chunk chunk = new Chunk(new ChunkPos(0, 0));
        chunk.setBlock(1, 1, 1, BlockType.STONE.getId());
        storage.putChunk(chunk);

        WorldSaveManager manager = WorldSaveManager.initialize(storage, "TestWorld",
                savesDirectory, true, 300f, 3);
        manager.saveChunk(chunk);

        storage.clear();
        Chunk loaded = manager.loadChunk(chunk.getPosition());
        assertThat(loaded).isNotNull();
        assertThat(loaded.getBlock(1, 1, 1)).isEqualTo(BlockType.STONE.getId());
        manager.shutdown();
    }

    @Test
    void testWorldSaveManagerAutoSave() {
        ChunkStorage storage = new ChunkStorage();
        Chunk chunk = new Chunk(new ChunkPos(0, 0));
        chunk.setBlock(2, 64, 2, BlockType.GRASS.getId());
        storage.putChunk(chunk);

        WorldSaveManager manager = WorldSaveManager.initialize(storage, "TestWorld",
                savesDirectory, true, 300f, 3);

        for (int i = 0; i < 300; i++) {
            manager.update(1.0f);
        }

        Chunk loaded = manager.loadChunk(chunk.getPosition());
        assertThat(loaded).isNotNull();
        assertThat(loaded.getBlock(2, 64, 2)).isEqualTo(BlockType.GRASS.getId());
        assertThat(chunk.isDirty()).isFalse();
        manager.shutdown();
    }

    @Test
    void testWorldSaveManagerSaveAll() {
        ChunkStorage storage = new ChunkStorage();
        List<Chunk> chunks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Chunk chunk = new Chunk(new ChunkPos(i, i));
            chunk.setBlock(1, i + 10, 1, (short) (BlockType.STONE.getId() + i));
            if (i % 2 == 0) {
                chunk.clearDirty();
            }
            storage.putChunk(chunk);
            chunks.add(chunk);
        }

        WorldSaveManager manager = WorldSaveManager.initialize(storage, "TestWorld",
                savesDirectory, true, 300f, 3);
        manager.saveAll();

        for (Chunk chunk : chunks) {
            Chunk reloaded = manager.loadChunk(chunk.getPosition());
            assertThat(reloaded).isNotNull();
            assertThat(reloaded.getBlock(1, chunk.getPosition().x() + 10, 1))
                    .isEqualTo(chunk.getBlock(1, chunk.getPosition().x() + 10, 1));
        }
        manager.shutdown();
    }

    @Test
    void testWorldSaveManagerLoadOrCreate() {
        ChunkStorage storage = new ChunkStorage();
        WorldSaveManager manager = WorldSaveManager.initialize(storage, "TestWorld",
                savesDirectory, true, 300f, 3);

        ChunkPos pos = new ChunkPos(99, 99);
        Chunk created = manager.loadOrCreateChunk(pos);
        assertThat(created).isNotNull();
        assertThat(created.getBlock(0, 0, 0)).isEqualTo(BlockType.AIR.getId());

        created.setBlock(0, 0, 0, BlockType.DIRT.getId());
        manager.saveChunk(created);

        Chunk loaded = manager.loadOrCreateChunk(pos);
        assertThat(loaded.getBlock(0, 0, 0)).isEqualTo(BlockType.DIRT.getId());
        manager.shutdown();
    }

    @Test
    void testSaveLoadIntegration() {
        ChunkStorage storage = new ChunkStorage();
        Map<ChunkPos, Chunk> originalChunks = new HashMap<>();

        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                ChunkPos pos = new ChunkPos(x, z);
                Chunk chunk = new Chunk(pos);
                Random random = new Random(pos.asLong());
                for (int i = 0; i < 500; i++) {
                    int bx = random.nextInt(Chunk.CHUNK_SIZE_X);
                    int by = random.nextInt(Chunk.CHUNK_SIZE_Y);
                    int bz = random.nextInt(Chunk.CHUNK_SIZE_Z);
                    short blockId = (short) (BlockType.STONE.getId() + random.nextInt(4));
                    chunk.setBlock(bx, by, bz, blockId);
                }
                storage.putChunk(chunk);
                originalChunks.put(pos, chunk);
            }
        }

        WorldSaveManager manager = WorldSaveManager.initialize(storage, "TestWorld",
                savesDirectory, true, 300f, 3);
        manager.saveAll();
        manager.shutdown();
        resetWorldSaveManager();

        ChunkStorage newStorage = new ChunkStorage();
        WorldSaveManager reloadManager = WorldSaveManager.initialize(newStorage, "TestWorld",
                savesDirectory, true, 300f, 3);

        Random validationRandom = new Random(1234);
        for (Map.Entry<ChunkPos, Chunk> entry : originalChunks.entrySet()) {
            ChunkPos pos = entry.getKey();
            Chunk loaded = reloadManager.loadChunk(pos);
            assertThat(loaded).isNotNull();
            Chunk original = entry.getValue();
            for (int i = 0; i < 1000; i++) {
                int bx = validationRandom.nextInt(Chunk.CHUNK_SIZE_X);
                int by = validationRandom.nextInt(Chunk.CHUNK_SIZE_Y);
                int bz = validationRandom.nextInt(Chunk.CHUNK_SIZE_Z);
                assertThat(loaded.getBlock(bx, by, bz)).isEqualTo(original.getBlock(bx, by, bz));
            }
        }

        long totalSize = Files.walk(savesDirectory)
                .filter(Files::isRegularFile)
                .mapToLong(Phase13Tests::fileSizeUnchecked)
                .sum();
        assertThat(totalSize).isLessThan((long) originalChunks.size() * UNCOMPRESSED_CHUNK_SIZE_BYTES / 2);
        reloadManager.shutdown();
    }

    @Test
    void testWorldSaveManagerAutoSaveIntervalExposure() {
        ChunkStorage storage = new ChunkStorage();
        WorldSaveManager manager = WorldSaveManager.initialize(storage, "TestWorld",
                savesDirectory, true, 300f, 3);
        Duration interval = manager.getAutoSaveInterval();
        assertThat(interval).isEqualTo(Duration.ofMinutes(5));
        manager.shutdown();
    }

    private static void resetWorldSaveManager() {
        try {
            Field instanceField = WorldSaveManager.class.getDeclaredField("INSTANCE");
            instanceField.setAccessible(true);
            instanceField.set(null, null);

            Field initField = WorldSaveManager.class.getDeclaredField("INITIALIZING");
            initField.setAccessible(true);
            AtomicBoolean initializing = (AtomicBoolean) initField.get(null);
            initializing.set(false);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to reset WorldSaveManager", e);
        }
    }

    private static long fileSizeUnchecked(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class RegionFileTestUtil {
        private static final int HEADER_SIZE_BYTES = 4096 * 2;

        private RegionFileTestUtil() {
        }
    }
}
