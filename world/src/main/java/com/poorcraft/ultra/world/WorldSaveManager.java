package com.poorcraft.ultra.world;

import com.poorcraft.ultra.shared.Config;
import com.poorcraft.ultra.shared.Logger;
import com.poorcraft.ultra.voxel.Chunk;
import com.poorcraft.ultra.voxel.ChunkPos;
import com.poorcraft.ultra.voxel.ChunkStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Coordinates saving and loading of chunks to disk using {@link RegionFile}s.
 */
public final class WorldSaveManager {

    private static final Logger logger = Logger.getLogger(WorldSaveManager.class);

    private static final float DEFAULT_AUTO_SAVE_INTERVAL_SECONDS = 300f;
    private static final String DEFAULT_WORLD_NAME = "TestWorld";
    private static final String DEFAULT_SAVE_DIRECTORY = "saves";
    private static final int MILLIS_PER_SECOND = 1000;

    private static final AtomicBoolean INITIALIZING = new AtomicBoolean(false);
    private static final int MAX_ACTIVE_REGION_RADIUS = 4;

    private static volatile WorldSaveManager INSTANCE;

    private final Path worldDirectory;
    private final Path regionDirectory;
    private final Map<RegionPos, RegionFile> openRegions = new ConcurrentHashMap<>();
    private final Map<RegionPos, Long> regionAccessTimes = new ConcurrentHashMap<>();
    private final ChunkStorage chunkStorage;
    private volatile RegionPos trackedPlayerRegion;

    private final boolean autoSaveEnabled;
    private final float autoSaveInterval;

    private float autoSaveTimer;
    private boolean initialized;

    private WorldSaveManager(ChunkStorage storage, String worldName, Path baseDirectory,
                             boolean autoSaveEnabled, float autoSaveInterval, int compressionLevel) {
        this.chunkStorage = Objects.requireNonNull(storage, "storage must not be null");
        this.autoSaveEnabled = autoSaveEnabled;
        this.autoSaveInterval = autoSaveInterval;
        this.autoSaveTimer = 0f;

        this.worldDirectory = baseDirectory.resolve(worldName);
        this.regionDirectory = worldDirectory.resolve("region");
        try {
            Files.createDirectories(regionDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create world directories at " + regionDirectory, e);
        }
        ChunkSerializer.setCompressionLevel(compressionLevel);
        this.trackedPlayerRegion = new RegionPos(0, 0);
        this.initialized = true;
        logger.info("World save manager initialized (world: {}, path: {})", worldName, worldDirectory);
    }

    public static synchronized WorldSaveManager initialize(ChunkStorage storage) {
        return initialize(storage, null);
    }

    public static synchronized WorldSaveManager initialize(ChunkStorage storage, String worldName) {
        if (INSTANCE != null && INSTANCE.initialized) {
            logger.warn("WorldSaveManager already initialized");
            return INSTANCE;
        }

        if (!INITIALIZING.compareAndSet(false, true)) {
            throw new IllegalStateException("WorldSaveManager initialization already in progress");
        }

        try {
            Config config = Config.getInstance();
            String configuredName = null;
            String saveDir = DEFAULT_SAVE_DIRECTORY;
            boolean autoSaveEnabled = true;
            float autoSaveInterval = DEFAULT_AUTO_SAVE_INTERVAL_SECONDS;
            int compressionLevel = ChunkSerializer.getCompressionLevel();

            if (config.hasPath("world")) {
                com.typesafe.config.Config worldConfig = config.getConfig("world");
                if (worldConfig.hasPath("name")) {
                    configuredName = worldConfig.getString("name");
                }
                if (worldConfig.hasPath("saveDirectory")) {
                    saveDir = worldConfig.getString("saveDirectory");
                }
                if (worldConfig.hasPath("autoSave")) {
                    autoSaveEnabled = worldConfig.getBoolean("autoSave");
                }
                if (worldConfig.hasPath("autoSaveInterval")) {
                    autoSaveInterval = (float) worldConfig.getDouble("autoSaveInterval");
                }
                if (worldConfig.hasPath("compressionLevel")) {
                    compressionLevel = worldConfig.getInt("compressionLevel");
                }
            }

            String resolvedWorldName = worldName != null ? worldName
                    : (configuredName != null ? configuredName : DEFAULT_WORLD_NAME);

            Path baseDirectory = Paths.get(saveDir);
            INSTANCE = new WorldSaveManager(storage, resolvedWorldName, baseDirectory,
                    autoSaveEnabled, autoSaveInterval, compressionLevel);
            return INSTANCE;
        } finally {
            INITIALIZING.set(false);
        }
    }

    public static synchronized WorldSaveManager initialize(ChunkStorage storage,
                                                            String worldName,
                                                            Path baseDirectory,
                                                            boolean autoSave,
                                                            float autoSaveInterval,
                                                            int compressionLevel) {
        if (INSTANCE != null && INSTANCE.initialized) {
            logger.warn("WorldSaveManager already initialized");
            return INSTANCE;
        }
        if (baseDirectory == null) {
            throw new IllegalArgumentException("baseDirectory must not be null");
        }
        INSTANCE = new WorldSaveManager(storage,
                worldName != null ? worldName : DEFAULT_WORLD_NAME,
                baseDirectory,
                autoSave,
                autoSaveInterval,
                compressionLevel);
        return INSTANCE;
    }

    public static WorldSaveManager getInstance() {
        WorldSaveManager instance = INSTANCE;
        if (instance == null || !instance.initialized) {
            throw new IllegalStateException("WorldSaveManager not initialized");
        }
        return instance;
    }

    public static boolean isInitialized() {
        WorldSaveManager instance = INSTANCE;
        return instance != null && instance.initialized;
    }

    public void update(float tpfSeconds) {
        runAutoSave(tpfSeconds);
        sweepUnusedRegions();
    }

    public int saveAllDirtyChunks() {
        Collection<Chunk> chunks = chunkStorage.getAllChunks();
        int savedCount = 0;
        for (Chunk chunk : chunks) {
            if (!chunk.isDirty()) {
                continue;
            }
            try {
                saveChunk(chunk);
                savedCount++;
            } catch (RuntimeException e) {
                logger.error("Failed to save dirty chunk at ({}, {})", chunk.getPosition().x(),
                        chunk.getPosition().z(), e);
            }
        }
        return savedCount;
    }

    public void saveAll() {
        long start = System.currentTimeMillis();
        Collection<Chunk> chunks = chunkStorage.getAllChunks();
        int saved = 0;
        for (Chunk chunk : chunks) {
            try {
                saveChunk(chunk);
                saved++;
            } catch (RuntimeException e) {
                logger.error("Failed to save chunk at ({}, {})", chunk.getPosition().x(),
                        chunk.getPosition().z(), e);
            }
        }
        long duration = System.currentTimeMillis() - start;
        logger.info("World saved: {} chunks in {} ms", saved, duration);
    }

    public void saveChunk(Chunk chunk) {
        Objects.requireNonNull(chunk, "chunk must not be null");
        ChunkPos position = chunk.getPosition();
        RegionPos regionPos = RegionPos.fromChunkPos(position);
        RegionFile regionFile = getOrOpenRegion(regionPos);
        byte[] data = ChunkSerializer.serialize(chunk);
        try {
            regionFile.writeChunk(position, data);
            chunk.clearDirty();
            logger.debug("Chunk saved at ({}, {})", position.x(), position.z());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write chunk at " + position, e);
        }
    }

    public Chunk loadChunk(ChunkPos position) {
        Objects.requireNonNull(position, "position must not be null");
        RegionPos regionPos = RegionPos.fromChunkPos(position);
        RegionFile regionFile = getOrOpenRegion(regionPos);
        try {
            byte[] data = regionFile.readChunk(position);
            if (data == null) {
                return null;
            }
            Chunk chunk = ChunkSerializer.deserialize(data, position);
            logger.debug("Chunk loaded from disk at ({}, {})", position.x(), position.z());
            return chunk;
        } catch (IOException e) {
            logger.error("Failed to read chunk at ({}, {})", position.x(), position.z(), e);
            return null;
        }
    }

    public Chunk loadOrCreateChunk(ChunkPos position) {
        Chunk loaded = loadChunk(position);
        return loaded != null ? loaded : new Chunk(position);
    }

    public void shutdown() {
        logger.info("World save manager shutting down...");
        saveAll();
        closeAllRegions();
        initialized = false;
        logger.info("World save manager shut down");
    }

    private RegionFile getOrOpenRegion(RegionPos regionPos) {
        RegionFile region = openRegions.computeIfAbsent(regionPos, key -> {
            Path regionPath = regionDirectory.resolve(key.getFileName());
            try {
                return new RegionFile(regionPath, key);
            } catch (IOException e) {
                throw new RuntimeException("Failed to open region file " + regionPath, e);
            }
        });
        regionAccessTimes.put(regionPos, System.currentTimeMillis());
        return region;
    }

    public void closeRegion(RegionPos regionPos) {
        Objects.requireNonNull(regionPos, "regionPos must not be null");
        RegionFile regionFile = openRegions.remove(regionPos);
        regionAccessTimes.remove(regionPos);
        if (regionFile == null) {
            return;
        }
        try {
            regionFile.close();
            logger.debug("Region {} closed", regionPos);
        } catch (IOException e) {
            logger.error("Failed to close region {}", regionPos, e);
        }
    }

    private void closeAllRegions() {
        int closed = 0;
        for (RegionFile regionFile : openRegions.values()) {
            try {
                regionFile.close();
                closed++;
            } catch (IOException e) {
                logger.error("Failed to close region file {}", regionFile, e);
            }
        }
        openRegions.clear();
        regionAccessTimes.clear();
        logger.info("All region files closed ({} regions)", closed);
    }

    private void runAutoSave(float tpfSeconds) {
        if (!autoSaveEnabled) {
            return;
        }
        autoSaveTimer += tpfSeconds;
        if (autoSaveTimer < autoSaveInterval) {
            return;
        }

        autoSaveTimer = 0f;
        logger.info("Auto-save triggered ({} seconds elapsed)", (int) autoSaveInterval);
        long start = System.currentTimeMillis();
        int saved = saveAllDirtyChunks();
        long duration = System.currentTimeMillis() - start;
        if (saved > 0) {
            logger.info("Auto-save: {} chunks saved in {} ms", saved, duration);
        } else {
            logger.info("Auto-save: no dirty chunks to save ({} ms)", duration);
        }
    }

    private void sweepUnusedRegions() {
        if (openRegions.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        long maxIdleMillis = Duration.ofMinutes(1).toMillis();
        RegionPos playerRegion = trackedPlayerRegion;

        List<RegionPos> regionsToClose = new ArrayList<>();
        openRegions.keySet().forEach(regionPos -> {
            Long lastAccess = regionAccessTimes.get(regionPos);
            boolean idle = lastAccess == null || now - lastAccess > maxIdleMillis;
            boolean distant = playerRegion != null && distance(playerRegion, regionPos) > MAX_ACTIVE_REGION_RADIUS;
            if (idle || distant) {
                regionsToClose.add(regionPos);
            }
        });

        regionsToClose.forEach(this::closeRegion);
    }

    public Path getWorldDirectory() {
        return worldDirectory;
    }

    public Path getRegionDirectory() {
        return regionDirectory;
    }

    public Duration getAutoSaveInterval() {
        return Duration.ofMillis(Math.round(autoSaveInterval * MILLIS_PER_SECOND));
    }

    public void updatePlayerRegion(RegionPos regionPos) {
        this.trackedPlayerRegion = Objects.requireNonNull(regionPos, "regionPos must not be null");
    }

    private static int distance(RegionPos a, RegionPos b) {
        return Math.max(Math.abs(a.x() - b.x()), Math.abs(a.z() - b.z()));
    }
}
