package com.poorcraft.ultra.world;

import com.poorcraft.ultra.voxel.Chunk;
import com.poorcraft.ultra.voxel.ChunkManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public final class WorldSaveManager {

    private static final Logger logger = LoggerFactory.getLogger(WorldSaveManager.class);
    private static final Path DEFAULT_BASE_DIRECTORY = Path.of("data", "worlds");

    private String worldName;
    private Path worldRoot;
    private Path regionDir;
    private final AtomicBoolean savedOnShutdown = new AtomicBoolean(false);

    public void init(String worldName) {
        init(worldName, DEFAULT_BASE_DIRECTORY);
    }

    public void init(String worldName, Path baseDirectory) {
        this.worldName = worldName;
        this.worldRoot = baseDirectory.resolve(worldName);
        this.regionDir = worldRoot.resolve("region");
        savedOnShutdown.set(false);

        try {
            Files.createDirectories(regionDir);
            logger.info("WorldSaveManager initialized at {}", worldRoot.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to initialize world directories at {}", worldRoot.toAbsolutePath(), e);
            throw new IllegalStateException("Unable to create world directories", e);
        }
    }

    public void saveChunk(Chunk chunk) {
        if (chunk == null) {
            return;
        }

        if (!chunk.isModified()) {
            logger.debug("Skipping save for unmodified chunk ({}, {})", chunk.chunkX(), chunk.chunkZ());
            return;
        }

        byte[] data = chunk.getBlockData();
        long crc32 = ChecksumUtil.computeCRC32(data);
        int headerCrc32 = (int) (crc32 & 0xFFFFFFFFL);
        RegionFileHeader header = RegionFileHeader.create(chunk.chunkX(), chunk.chunkZ(), headerCrc32);
        Path filePath = getRegionFilePath(chunk.chunkX(), chunk.chunkZ());

        try {
            RegionFile.write(filePath, header, data);
            logger.debug("Saved chunk ({}, {}) to {}", chunk.chunkX(), chunk.chunkZ(), filePath);
            chunk.setModified(false);
        } catch (IOException e) {
            logger.error("Failed to save chunk ({}, {})", chunk.chunkX(), chunk.chunkZ(), e);
        }
    }

    public Chunk loadChunk(int chunkX, int chunkZ) {
        Path filePath = getRegionFilePath(chunkX, chunkZ);
        if (!Files.exists(filePath)) {
            return null;
        }

        try {
            RegionFile.RegionFileData data = RegionFile.read(filePath);
            RegionFileHeader header = data.header();

            if (header.chunkX() != chunkX || header.chunkZ() != chunkZ) {
                logger.warn("Region file coordinates mismatch for {}: header=({}, {}) expected=({}, {})",
                        filePath, header.chunkX(), header.chunkZ(), chunkX, chunkZ);
                return null;
            }

            Chunk chunk = new Chunk(chunkX, chunkZ);
            chunk.setBlockData(data.blockData());
            chunk.setDirty(false);
            chunk.setModified(false);
            logger.debug("Loaded chunk ({}, {}) from {}", chunkX, chunkZ, filePath);
            return chunk;
        } catch (RegionFile.RegionFileException e) {
            logger.warn("Failed to load chunk ({}, {}) due to region file error: {}", chunkX, chunkZ, e.getMessage());
        } catch (IOException e) {
            logger.error("I/O error loading chunk ({}, {})", chunkX, chunkZ, e);
        }

        return null;
    }

    public int saveAll(ChunkManager chunkManager) {
        if (chunkManager == null) {
            return 0;
        }

        final int[] count = {0};
        chunkManager.forEachChunk((coord, chunk) -> {
            if (chunk != null && chunk.isModified()) {
                saveChunk(chunk);
                count[0]++;
            }
        });
        if (count[0] > 0) {
            logger.info("Saved {} modified chunk(s) to {}", count[0], regionDir.toAbsolutePath());
        } else {
            logger.debug("No modified chunks to save in {}", regionDir.toAbsolutePath());
        }
        return count[0];
    }

    public Path getRegionFilePath(int chunkX, int chunkZ) {
        return regionDir.resolve(String.format("r.%d.%d.dat", chunkX, chunkZ));
    }

    public Path getRegionDir() {
        return regionDir;
    }

    public String getWorldName() {
        return worldName;
    }

    public void markSavedOnShutdown() {
        savedOnShutdown.set(true);
    }

    public boolean isSavedOnShutdown() {
        return savedOnShutdown.get();
    }
}
