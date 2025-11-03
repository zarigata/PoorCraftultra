package com.poorcraft.ultra.world;

import com.poorcraft.ultra.voxel.BlockType;
import com.poorcraft.ultra.voxel.ChunkManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class SaveLoadIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void testSaveLoadCycle() throws IOException {
        Path baseDir = tempDir.resolve("worlds");

        WorldSaveManager saveManager = new WorldSaveManager();
        saveManager.init("default", baseDir);

        ChunkManager chunkManager = new ChunkManager();
        chunkManager.init(null, null, null, saveManager, null);
        chunkManager.loadChunks3x3(0, 0);

        Map<Coordinate, BlockType> testBlocks = placeTestBlocks(chunkManager);

        chunkManager.saveAll();

        Map<String, Integer> initialChecksums = computeChecksums(saveManager.getRegionDir());
        assertEquals(9, initialChecksums.size());
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                String fileName = String.format("r.%d.%d.dat", x, z);
                assertTrue(initialChecksums.containsKey(fileName));
            }
        }

        WorldSaveManager loadManager = new WorldSaveManager();
        loadManager.init("default", baseDir);

        ChunkManager chunkManagerReloaded = new ChunkManager();
        chunkManagerReloaded.init(null, null, null, loadManager, null);
        chunkManagerReloaded.loadChunks3x3(0, 0);

        verifyTestBlocks(chunkManagerReloaded, testBlocks);
        assertEquals(9, chunkManagerReloaded.getLoadedChunkCount());

        Map<String, Integer> reloadedChecksums = computeChecksums(loadManager.getRegionDir());
        assertEquals(initialChecksums, reloadedChecksums);
    }

    private static Map<Coordinate, BlockType> placeTestBlocks(ChunkManager chunkManager) {
        Map<Coordinate, BlockType> placements = new HashMap<>();
        placements.put(new Coordinate(0, 64, 0), BlockType.STONE);
        placements.put(new Coordinate(5, 65, 5), BlockType.GRASS);
        placements.put(new Coordinate(15, 70, 15), BlockType.DIRT);
        placements.put(new Coordinate(16, 64, 16), BlockType.WOOD_OAK);
        placements.put(new Coordinate(-1, 64, -1), BlockType.WOOD_OAK);
        placements.put(new Coordinate(-10, 66, 20), BlockType.STONE);
        placements.put(new Coordinate(20, 64, -10), BlockType.GRASS);
        placements.put(new Coordinate(25, 80, 25), BlockType.DIRT);
        placements.put(new Coordinate(-16, 64, -16), BlockType.WOOD_OAK);
        placements.put(new Coordinate(8, 90, -8), BlockType.WOOD_OAK);

        placements.forEach((coord, type) -> chunkManager.setBlock(coord.x, coord.y, coord.z, type));
        return placements;
    }

    private static void verifyTestBlocks(ChunkManager chunkManager, Map<Coordinate, BlockType> expected) {
        expected.forEach((coord, type) -> assertEquals(type, chunkManager.getBlock(coord.x, coord.y, coord.z),
                String.format("Mismatch at (%d,%d,%d)", coord.x, coord.y, coord.z)));
    }

    private static Map<String, Integer> computeChecksums(Path regionDir) throws IOException {
        try (var files = Files.list(regionDir)) {
            return files.filter(path -> path.getFileName().toString().endsWith(".dat"))
                    .collect(Collectors.toMap(path -> path.getFileName().toString(), SaveLoadIntegrationTest::readChecksum));
        }
    }

    private static int readChecksum(Path file) {
        try {
            return RegionFile.read(file).header().crc32();
        } catch (IOException e) {
            fail("Failed to read checksum for " + file + ": " + e.getMessage());
            return -1;
        }
    }

    private record Coordinate(int x, int y, int z) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Coordinate that)) return false;
            return x == that.x && y == that.y && z == that.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }
    }
}
