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
    private static final String META_FILE_NAME = "world.meta";
    private static final String GENERATOR_VERSION_FILE = "generator_version.txt";
    private static final String CURRENT_GENERATOR_VERSION = "2.0-biomes-caves";

    private String worldName;
    private Path worldRoot;
    private Path regionDir;
    private WorldMetadata metadata;
    private boolean regenerationRecommended;
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
            metadata = WorldMetadata.read(worldRoot.resolve(META_FILE_NAME));
            String version = getWorldVersion();
            regenerationRecommended = version != null && !CURRENT_GENERATOR_VERSION.equals(version);

            if (version == null) {
                setWorldVersion(CURRENT_GENERATOR_VERSION);
                logger.info("New world created with generator version: {}", CURRENT_GENERATOR_VERSION);
            } else if (!CURRENT_GENERATOR_VERSION.equals(version)) {
                logger.warn("World generator version mismatch!");
                logger.warn("  Saved world version: {}", version);
                logger.warn("  Current generator version: {}", CURRENT_GENERATOR_VERSION);
                logger.warn("  Existing chunks will load from disk (may show old terrain).");
                logger.warn("  To regenerate world with new terrain:");
                logger.warn("    1. Exit game");
                logger.warn("    2. Delete or rename: {}", worldRoot.toAbsolutePath());
                logger.warn("    3. Restart game");
            } else {
                logger.info("World generator version: {} (current)", version);
            }
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
            setWorldVersion(CURRENT_GENERATOR_VERSION);
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

    public String getWorldVersion() {
        if (worldRoot == null) {
            return null;
        }

        Path versionFile = worldRoot.resolve(GENERATOR_VERSION_FILE);
        if (!Files.exists(versionFile)) {
            return null;
        }

        try {
            String value = Files.readString(versionFile).trim();
            return value.isEmpty() ? null : value;
        } catch (IOException e) {
            logger.warn("Failed to read world generator version from {}", versionFile, e);
            return null;
        }
    }

    public boolean isRegenerationRecommended() {
        return regenerationRecommended;
    }

    public long resolveOrPersistSeed(long configuredSeed) {
        long seedToUse = configuredSeed;
        if (metadata != null && metadata.seed() != 0L) {
            return metadata.seed();
        }
        if (configuredSeed == 0L) {
            seedToUse = System.currentTimeMillis();
        }
        metadata = new WorldMetadata(seedToUse);
        try {
            metadata.write(worldRoot.resolve(META_FILE_NAME));
        } catch (IOException e) {
            logger.warn("Failed to write world metadata for {}", worldRoot, e);
        }
        return seedToUse;
    }

    public long getPersistedSeed() {
        return metadata != null ? metadata.seed() : 0L;
    }

    private void setWorldVersion(String version) {
        if (worldRoot == null || version == null || version.isBlank()) {
            return;
        }

        Path versionFile = worldRoot.resolve(GENERATOR_VERSION_FILE);
        try {
            Files.writeString(versionFile, version + System.lineSeparator());
            regenerationRecommended = !CURRENT_GENERATOR_VERSION.equals(version);
        } catch (IOException e) {
            logger.warn("Failed to write world generator version to {}", versionFile, e);
        }
    }

    private record WorldMetadata(long seed) {
        private static final String SEED_KEY = "seed";

        static WorldMetadata read(Path file) {
            if (!Files.exists(file)) {
                return null;
            }
            try {
                String line = Files.readString(file).trim();
                if (line.isEmpty()) {
                    return null;
                }
                String[] parts = line.split("=");
                if (parts.length == 2 && parts[0].trim().equalsIgnoreCase(SEED_KEY)) {
                    return new WorldMetadata(Long.parseLong(parts[1].trim()));
                }
            } catch (IOException | NumberFormatException e) {
                LoggerFactory.getLogger(WorldMetadata.class).warn("Failed to read world metadata from {}", file, e);
            }
            return null;
        }

        void write(Path file) throws IOException {
            Files.writeString(file, SEED_KEY + "=" + seed + System.lineSeparator());
        }
    }
}
