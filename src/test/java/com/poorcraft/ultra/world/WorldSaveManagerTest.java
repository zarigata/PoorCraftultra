package com.poorcraft.ultra.world;

import com.poorcraft.ultra.voxel.BlockType;
import com.poorcraft.ultra.voxel.Chunk;
import com.poorcraft.ultra.voxel.ChunkManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class WorldSaveManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void testSaveAndLoadChunk() {
        WorldSaveManager manager = createManager();
        Chunk chunk = createChunk(0, 0);
        chunk.setBlock(5, 64, 7, BlockType.STONE);
        chunk.setBlock(1, 70, 3, BlockType.GRASS);

        manager.saveChunk(chunk);

        Chunk loaded = manager.loadChunk(0, 0);
        assertNotNull(loaded);
        assertEquals(BlockType.STONE, loaded.getBlock(5, 64, 7));
        assertEquals(BlockType.GRASS, loaded.getBlock(1, 70, 3));
    }

    @Test
    void testLoadNonexistentChunk() {
        WorldSaveManager manager = createManager();
        assertNull(manager.loadChunk(5, -2));
    }

    @Test
    void testSaveAllChunksCreatesFiles() throws IOException {
        WorldSaveManager manager = createManager();
        ChunkManager chunkManager = new ChunkManager();
        chunkManager.init(null, null, null, manager);
        chunkManager.loadChunks3x3(0, 0);

        manager.saveAll(chunkManager);

        Path regionDir = manager.getRegionDir();
        try (var files = Files.list(regionDir)) {
            List<Path> regionFiles = files.filter(path -> path.getFileName().toString().endsWith(".dat"))
                    .collect(Collectors.toList());
            assertEquals(9, regionFiles.size());
            for (Path file : regionFiles) {
                assertEquals(65_568, Files.size(file));
            }
        }
    }

    @Test
    void testLoadCorruptChunkReturnsNull() throws IOException {
        WorldSaveManager manager = createManager();
        Chunk chunk = createChunk(1, 1);
        chunk.setBlock(2, 65, 2, BlockType.DIRT);
        manager.saveChunk(chunk);

        Path file = manager.getRegionFilePath(1, 1);
        byte[] bytes = Files.readAllBytes(file);
        bytes[RegionFileHeader.HEADER_SIZE_BYTES + 100] ^= 0xFF;
        Files.write(file, bytes);

        Chunk loaded = manager.loadChunk(1, 1);
        assertNull(loaded);
    }

    @Test
    void testSaveChunkCreatesDirectories() {
        WorldSaveManager manager = new WorldSaveManager();
        Path baseDir = tempDir.resolve("custom/worlds");
        manager.init("demo", baseDir);

        Chunk chunk = createChunk(-1, 0);
        manager.saveChunk(chunk);

        assertTrue(Files.exists(manager.getRegionDir()));
        assertTrue(Files.isDirectory(manager.getRegionDir()));
        assertTrue(Files.exists(manager.getRegionFilePath(-1, 0)));
    }

    @Test
    void testChecksumChangesWhenDataChanges() throws IOException {
        WorldSaveManager manager = createManager();
        Chunk chunk = createChunk(0, 0);
        chunk.setBlock(0, 64, 0, BlockType.STONE);
        manager.saveChunk(chunk);

        int firstChecksum = RegionFile.read(manager.getRegionFilePath(0, 0)).header().crc32();

        chunk.setBlock(1, 65, 1, BlockType.GRASS);
        manager.saveChunk(chunk);
        int secondChecksum = RegionFile.read(manager.getRegionFilePath(0, 0)).header().crc32();

        assertNotEquals(firstChecksum, secondChecksum);
    }

    private WorldSaveManager createManager() {
        WorldSaveManager manager = new WorldSaveManager();
        manager.init("default", tempDir.resolve("worlds"));
        return manager;
    }

    private static Chunk createChunk(int x, int z) {
        return new Chunk(x, z);
    }
}
