package com.poorcraft.ultra.voxel;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.poorcraft.ultra.shared.Logger;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton manager responsible for generating and rendering chunk geometries.
 */
public final class ChunkRenderer {

    private static final Logger logger = Logger.getLogger(ChunkRenderer.class);

    private static ChunkRenderer INSTANCE;

    private final SimpleApplication application;
    private final ChunkStorage storage;
    private final BlockRegistry registry;
    private final TextureAtlas atlas;

    private final Map<Long, Geometry> chunkGeometries = new ConcurrentHashMap<>();
    private final Node chunkRootNode;
    private final Material sharedMaterial;

    private volatile boolean wireframeEnabled;
    private volatile int renderDistance;
    private volatile ChunkPos lastCameraChunkPos;

    private ChunkRenderer(SimpleApplication application,
                          ChunkStorage storage,
                          BlockRegistry registry,
                          TextureAtlas atlas,
                          int renderDistance) {
        this.application = application;
        this.storage = storage;
        this.registry = registry;
        this.atlas = atlas;
        this.renderDistance = Math.max(0, renderDistance);

        this.chunkRootNode = new Node("ChunkRoot");
        this.sharedMaterial = BlockMaterial.create(application.getAssetManager(), atlas);

        application.getRootNode().attachChild(chunkRootNode);
        logger.info("Chunk renderer initialized (render distance: {} chunks)", this.renderDistance);
    }

    public static synchronized ChunkRenderer initialize(SimpleApplication application,
                                                        ChunkStorage storage,
                                                        BlockRegistry registry,
                                                        TextureAtlas atlas,
                                                        int renderDistance) {
        Objects.requireNonNull(application, "application must not be null");
        Objects.requireNonNull(storage, "storage must not be null");
        Objects.requireNonNull(registry, "registry must not be null");
        Objects.requireNonNull(atlas, "atlas must not be null");

        if (INSTANCE != null) {
            logger.warn("ChunkRenderer already initialized; returning existing instance");
            return INSTANCE;
        }

        INSTANCE = new ChunkRenderer(application, storage, registry, atlas, renderDistance);
        return INSTANCE;
    }

    public static ChunkRenderer getInstance() {
        ChunkRenderer snapshot = INSTANCE;
        if (snapshot == null) {
            throw new IllegalStateException("ChunkRenderer not initialized");
        }
        return snapshot;
    }

    public static boolean isInitialized() {
        return INSTANCE != null;
    }

    public Node getChunkRootNode() {
        return chunkRootNode;
    }

    public int getRenderedChunkCount() {
        return chunkGeometries.size();
    }

    public void renderChunk(Chunk chunk) {
        Objects.requireNonNull(chunk, "chunk must not be null");
        long key = chunk.getPosition().asLong();
        if (chunkGeometries.containsKey(key)) {
            return;
        }

        Mesh mesh = GreedyMesher.generateMesh(chunk, storage, registry, atlas);
        if (mesh.getVertexCount() == 0 || mesh.getTriangleCount() == 0) {
            chunk.clearDirty();
            logger.debug("Skipping render for empty chunk ({}, {})", chunk.getPosition().x(), chunk.getPosition().z());
            return;
        }

        if (lastCameraChunkPos != null && chebyshevDistance(lastCameraChunkPos, chunk.getPosition()) > renderDistance) {
            chunk.clearDirty();
            logger.debug("Skipping render for chunk ({}, {}) beyond render distance", chunk.getPosition().x(),
                    chunk.getPosition().z());
            return;
        }

        Geometry geometry = new Geometry("Chunk_" + chunk.getPosition().x() + '_' + chunk.getPosition().z(), mesh);
        geometry.setLocalTranslation(toChunkTranslation(chunk.getPosition()));
        geometry.setMaterial(sharedMaterial);
        geometry.getMaterial().getAdditionalRenderState().setWireframe(wireframeEnabled);

        chunkRootNode.attachChild(geometry);
        chunkGeometries.put(key, geometry);
        chunk.clearDirty();

        logger.info("Chunk rendered at ({}, {})", chunk.getPosition().x(), chunk.getPosition().z());
    }

    public void updateChunk(Chunk chunk) {
        Objects.requireNonNull(chunk, "chunk must not be null");
        if (!chunk.isDirty()) {
            return;
        }
        long key = chunk.getPosition().asLong();
        Geometry geometry = chunkGeometries.get(key);
        if (geometry == null) {
            renderChunk(chunk);
            return;
        }

        Mesh mesh = GreedyMesher.generateMesh(chunk, storage, registry, atlas);
        if (mesh.getVertexCount() == 0 || mesh.getTriangleCount() == 0) {
            geometry.removeFromParent();
            chunkGeometries.remove(key);
            logger.info("Chunk became empty and was removed ({}, {})", chunk.getPosition().x(), chunk.getPosition().z());
        } else {
            geometry.setMesh(mesh);
            geometry.getMaterial().getAdditionalRenderState().setWireframe(wireframeEnabled);
            logger.info("Chunk updated at ({}, {})", chunk.getPosition().x(), chunk.getPosition().z());
        }
        chunk.clearDirty();
    }

    public void removeChunk(ChunkPos position) {
        Objects.requireNonNull(position, "position must not be null");
        Geometry geometry = chunkGeometries.remove(position.asLong());
        if (geometry != null) {
            geometry.removeFromParent();
            logger.info("Chunk unrendered at ({}, {})", position.x(), position.z());
        }
    }

    public void removeAllChunks() {
        chunkGeometries.values().forEach(Geometry::removeFromParent);
        int removed = chunkGeometries.size();
        chunkGeometries.clear();
        logger.info("All chunks unrendered ({} geometries removed)", removed);
    }

    public void updateCameraPosition(Camera camera) {
        Objects.requireNonNull(camera, "camera must not be null");
        ChunkPos current = ChunkPos.fromWorldCoordinates(camera.getLocation().x, camera.getLocation().z);
        if (!current.equals(lastCameraChunkPos)) {
            applyRenderDistance(current);
            lastCameraChunkPos = current;
        }
    }

    public void updateRenderDistance(Camera camera, int newRenderDistance) {
        this.renderDistance = Math.max(0, newRenderDistance);
        logger.info("Render distance updated to {} chunks", this.renderDistance);
        lastCameraChunkPos = null;
        updateCameraPosition(camera);
    }

    public void setWireframe(boolean enabled) {
        wireframeEnabled = enabled;
        chunkGeometries.values().stream()
                .map(Geometry::getMaterial)
                .map(Material::getAdditionalRenderState)
                .forEach(renderState -> renderState.setWireframe(enabled));
        logger.info("Wireframe mode: {}", enabled ? "enabled" : "disabled");
    }

    private Vector3f toChunkTranslation(ChunkPos position) {
        return new Vector3f(position.getWorldX(), 0f, position.getWorldZ());
    }

    private void applyRenderDistance(ChunkPos cameraChunk) {
        int maxDistance = renderDistance;

        chunkGeometries.entrySet().removeIf(entry -> {
            ChunkPos pos = ChunkPos.fromLong(entry.getKey());
            int distance = chebyshevDistance(cameraChunk, pos);
            if (distance > maxDistance) {
                entry.getValue().removeFromParent();
                logger.debug("Culled chunk ({}, {}) beyond render distance", pos.x(), pos.z());
                return true;
            }
            return false;
        });

        storage.getAllChunks().forEach(chunk -> {
            int distance = chebyshevDistance(cameraChunk, chunk.getPosition());
            if (distance <= maxDistance) {
                renderChunk(chunk);
            }
        });
    }

    private static int chebyshevDistance(ChunkPos a, ChunkPos b) {
        return Math.max(Math.abs(a.x() - b.x()), Math.abs(a.z() - b.z()));
    }
}
