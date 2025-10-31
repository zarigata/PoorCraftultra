package com.poorcraft.ultra.voxel;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.poorcraft.ultra.shared.Config;
import com.poorcraft.ultra.shared.Logger;
import com.poorcraft.ultra.world.RegionPos;
import com.poorcraft.ultra.world.WorldSaveManager;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Label;

import java.util.Objects;

/**
 * App state that builds and renders an 8x8 chunk superchunk for Phase 1.1 verification.
 */
public final class SuperchunkTestState extends BaseAppState {

    private static final Logger logger = Logger.getLogger(SuperchunkTestState.class);

    private static final int SUPERCHUNK_SIZE = 8;
    private static final int CAMERA_SPAWN = SUPERCHUNK_SIZE * Chunk.CHUNK_SIZE_X / 2;

    private ChunkStorage storage;
    private ChunkRenderer renderer;
    private Label versionLabel;
    private int renderDistance;

    private int cachedViewportWidth = -1;
    private int cachedViewportHeight = -1;
    private boolean attemptedDiskLoad;

    @Override
    protected void initialize(Application app) {
        ensureGuiGlobalsInitialized(app);

        SimpleApplication simpleApplication = (SimpleApplication) app;
        TextureAtlas atlas = TextureAtlas.getInstance();
        BlockRegistry registry = BlockRegistry.getInstance();

        storage = new ChunkStorage();
        Config config = Config.getInstance();
        renderDistance = config.hasPath("render.distance") ? config.getInt("render.distance") : 8;

        renderer = ChunkRenderer.initialize(simpleApplication, storage, registry, atlas, renderDistance);

        int loadedFromDisk = loadSavedChunks();
        generateSuperchunk();
        if (loadedFromDisk > 0) {
            logger.info("Loaded {} chunks from disk", loadedFromDisk);
            attemptedDiskLoad = true;
        }

        renderer.updateRenderDistance(simpleApplication.getCamera(), renderDistance);

        simpleApplication.getCamera().setLocation(new Vector3f(CAMERA_SPAWN, 10f, CAMERA_SPAWN));
        simpleApplication.getCamera().lookAt(new Vector3f(CAMERA_SPAWN, 0f, CAMERA_SPAWN), Vector3f.UNIT_Y);

        versionLabel = new Label("Poorcraft Ultra v1.3");
        versionLabel.setFontSize(20f);
        versionLabel.setColor(new ColorRGBA(1f, 1f, 1f, 0.9f));
        centerLabel(simpleApplication);
        cachedViewportWidth = simpleApplication.getCamera().getWidth();
        cachedViewportHeight = simpleApplication.getCamera().getHeight();
        simpleApplication.getGuiNode().attachChild(versionLabel);

        logger.info("Superchunk test initialized ({}x{} chunks = {} total)", SUPERCHUNK_SIZE, SUPERCHUNK_SIZE,
                SUPERCHUNK_SIZE * SUPERCHUNK_SIZE);
    }

    @Override
    protected void cleanup(Application app) {
        if (versionLabel != null) {
            versionLabel.removeFromParent();
            versionLabel = null;
        }

        if (renderer != null) {
            renderer.removeAllChunks();
        }

        if (storage != null) {
            storage.clear();
            storage = null;
        }

        logger.info("Superchunk test cleaned up");
    }

    @Override
    protected void onEnable() {
        setCullHints(Spatial.CullHint.Inherit, Spatial.CullHint.Never);
    }

    @Override
    protected void onDisable() {
        setCullHints(Spatial.CullHint.Always, Spatial.CullHint.Always);
    }

    @Override
    public void update(float tpf) {
        SimpleApplication application = (SimpleApplication) getApplication();
        if (application == null) {
            return;
        }

        renderer.updateCameraPosition(application.getCamera());

        if (WorldSaveManager.isInitialized()) {
            Vector3f camLocation = application.getCamera().getLocation();
            ChunkPos cameraChunk = ChunkPos.fromWorldCoordinates(camLocation.x, camLocation.z);
            WorldSaveManager.getInstance().updatePlayerRegion(RegionPos.fromChunkPos(cameraChunk));
        }

        if (updateCachedViewportSize(application)) {
            centerLabel(application);
        }

        if (!attemptedDiskLoad && WorldSaveManager.isInitialized()) {
            int loaded = loadSavedChunks();
            if (loaded > 0) {
                logger.info("Loaded {} chunks from disk", loaded);
            }
            attemptedDiskLoad = true;
        }

        storage.getAllChunks().forEach(chunk -> {
            if (chunk.isDirty()) {
                renderer.updateChunk(chunk);
            }
        });
    }

    public ChunkStorage getChunkStorage() {
        return storage;
    }

    private void generateSuperchunk() {
        Objects.requireNonNull(renderer, "renderer must not be null");
        for (int chunkX = 0; chunkX < SUPERCHUNK_SIZE; chunkX++) {
            for (int chunkZ = 0; chunkZ < SUPERCHUNK_SIZE; chunkZ++) {
                ChunkPos pos = new ChunkPos(chunkX, chunkZ);
                if (storage.containsChunk(pos)) {
                    continue;
                }
                Chunk chunk = new Chunk(pos);

                for (int x = 0; x < Chunk.CHUNK_SIZE_X; x++) {
                    for (int z = 0; z < Chunk.CHUNK_SIZE_Z; z++) {
                        chunk.setBlock(x, 0, z, BlockType.DIRT.getId());
                        chunk.setBlock(x, 1, z, BlockType.GRASS.getId());
                    }
                }

                storage.putChunk(chunk);
                renderer.renderChunk(chunk);
            }
        }
    }

    private void setCullHints(Spatial.CullHint chunkHint, Spatial.CullHint guiHint) {
        if (renderer != null) {
            renderer.getChunkRootNode().setCullHint(chunkHint);
        }
        if (versionLabel != null) {
            versionLabel.setCullHint(guiHint);
        }
    }

    private void centerLabel(SimpleApplication app) {
        if (versionLabel == null) {
            return;
        }
        float width = app.getCamera().getWidth();
        float textWidth = versionLabel.getPreferredSize().x;
        float x = (width - textWidth) / 2f;
        float y = 40f;
        versionLabel.setLocalTranslation(x, y, 0f);
    }

    private boolean updateCachedViewportSize(SimpleApplication app) {
        int width = app.getCamera().getWidth();
        int height = app.getCamera().getHeight();
        if (width != cachedViewportWidth || height != cachedViewportHeight) {
            cachedViewportWidth = width;
            cachedViewportHeight = height;
            return true;
        }
        return false;
    }

    private void ensureGuiGlobalsInitialized(Application app) {
        try {
            GuiGlobals.getInstance();
        } catch (IllegalStateException ignored) {
            GuiGlobals.initialize(app);
        }
    }

    private int loadSavedChunks() {
        if (!WorldSaveManager.isInitialized()) {
            logger.info("World save manager not initialized; generating fresh superchunk");
            return 0;
        }

        WorldSaveManager saveManager = WorldSaveManager.getInstance();
        int loaded = 0;
        for (int chunkX = 0; chunkX < SUPERCHUNK_SIZE; chunkX++) {
            for (int chunkZ = 0; chunkZ < SUPERCHUNK_SIZE; chunkZ++) {
                ChunkPos pos = new ChunkPos(chunkX, chunkZ);
                Chunk loadedChunk = saveManager.loadChunk(pos);
                if (loadedChunk == null) {
                    continue;
                }
                storage.putChunk(loadedChunk);
                renderer.renderChunk(loadedChunk);
                loaded++;
            }
        }
        return loaded;
    }
}
