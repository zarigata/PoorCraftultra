package com.poorcraft.ultra.voxel;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import com.poorcraft.ultra.world.WorldSaveManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class ChunkManager {
    private static final Logger logger = LoggerFactory.getLogger(ChunkManager.class);
    private final Map<ChunkCoord, Chunk> chunks = new HashMap<>();
    private final Map<ChunkCoord, Node> chunkNodes = new HashMap<>();
    private final Map<ChunkCoord, ChunkStats> chunkStats = new HashMap<>();

    private final ChunkMesher mesher = new ChunkMesher();

    private Node rootNode;
    private AssetManager assetManager;
    private BlockRegistry blockRegistry;
    private WorldSaveManager worldSaveManager;

    public void init(Node rootNode, AssetManager assetManager, BlockRegistry registry, WorldSaveManager worldSaveManager) {
        this.rootNode = rootNode;
        this.assetManager = assetManager;
        this.blockRegistry = registry;
        this.worldSaveManager = worldSaveManager;
    }

    public void loadChunk(int chunkX, int chunkZ) {
        ChunkCoord coord = new ChunkCoord(chunkX, chunkZ);
        if (chunks.containsKey(coord)) {
            return;
        }

        Chunk chunk = null;
        if (worldSaveManager != null) {
            chunk = worldSaveManager.loadChunk(chunkX, chunkZ);
        }

        if (chunk == null) {
            chunk = new Chunk(chunkX, chunkZ);
            chunk.fillCheckerboard();
            logger.debug("Generated new chunk ({}, {})", chunkX, chunkZ);
        } else {
            logger.debug("Loaded chunk ({}, {}) from disk", chunkX, chunkZ);
        }
        chunks.put(coord, chunk);

        rebuildMesh(coord, chunk);
    }

    public void loadChunks3x3(int centerX, int centerZ) {
        for (int x = centerX - 1; x <= centerX + 1; x++) {
            for (int z = centerZ - 1; z <= centerZ + 1; z++) {
                loadChunk(x, z);
            }
        }
    }

    public void rebuildAllMeshes() {
        chunks.forEach((coord, chunk) -> rebuildMesh(coord, chunk));
    }

    public void update(float tpf) {
        chunks.forEach((coord, chunk) -> {
            if (chunk.isDirty()) {
                chunk.setDirty(false);
                rebuildMesh(coord, chunk);
            }
        });
    }

    public void setBlock(int worldX, int worldY, int worldZ, BlockType type) {
        ChunkCoord coord = worldToChunk(worldX, worldZ);
        Chunk chunk = chunks.computeIfAbsent(coord, c -> new Chunk(c.chunkX(), c.chunkZ()));
        if (worldY < 0 || worldY >= Chunk.SIZE_Y) {
            return;
        }

        int localX = worldToLocal(worldX);
        int localZ = worldToLocal(worldZ);
        chunk.setBlock(localX, worldY, localZ, type);
        chunk.setDirty(true);
    }

    /**
     * Save a single chunk to disk.
     */
    public void saveChunk(int chunkX, int chunkZ) {
        if (worldSaveManager == null) {
            return;
        }
        Chunk chunk = chunks.get(new ChunkCoord(chunkX, chunkZ));
        if (chunk != null) {
            worldSaveManager.saveChunk(chunk);
        }
    }

    /**
     * Save all loaded chunks to disk.
     */
    public void saveAll() {
        if (worldSaveManager == null) {
            return;
        }
        logger.info("Saving {} loaded chunk(s); persisting modified chunks only...", chunks.size());
        int saved = worldSaveManager.saveAll(this);
        if (saved > 0) {
            logger.info("Saved {} modified chunk(s)", saved);
        } else {
            logger.info("No modified chunks required saving");
        }
    }

    public Map<ChunkCoord, Chunk> getChunks() {
        return Map.copyOf(chunks);
    }

    public BlockType getBlock(int worldX, int worldY, int worldZ) {
        ChunkCoord coord = worldToChunk(worldX, worldZ);
        Chunk chunk = chunks.get(coord);
        if (chunk == null) {
            return BlockType.AIR;
        }
        int localX = worldToLocal(worldX);
        int localZ = worldToLocal(worldZ);
        return chunk.getBlock(localX, worldY, localZ);
    }

    public int getLoadedChunkCount() {
        return chunks.size();
    }

    public ChunkStats getChunkStats(ChunkCoord coord) {
        return chunkStats.getOrDefault(coord, ChunkStats.EMPTY);
    }

    public void forEachChunk(Consumer<Map.Entry<ChunkCoord, Chunk>> consumer) {
        chunks.entrySet().forEach(consumer);
    }

    public void forEachChunk(java.util.function.BiConsumer<ChunkCoord, Chunk> consumer) {
        chunks.forEach((coord, chunk) -> consumer.accept(coord, chunk));
    }

    private void rebuildMesh(ChunkCoord coord, Chunk chunk) {
        if (rootNode == null || assetManager == null || blockRegistry == null) {
            return;
        }

        ChunkMesh chunkMesh = mesher.buildMesh(chunk, (worldX, worldY, worldZ) -> getBlock(worldX, worldY, worldZ));

        Node chunkNode = chunkNodes.computeIfAbsent(coord, c -> {
            Node node = new Node("chunk-" + coord.chunkX() + "." + coord.chunkZ());
            rootNode.attachChild(node);
            node.setLocalTranslation(coord.chunkX() * Chunk.SIZE_X, 0f, coord.chunkZ() * Chunk.SIZE_Z);
            return node;
        });
        chunkNode.detachAllChildren();

        if (chunkMesh.isEmpty()) {
            chunkStats.put(coord, ChunkStats.EMPTY);
            return;
        }

        int totalVertices = 0;
        int totalTriangles = 0;

        for (BlockType type : chunkMesh.blockTypes()) {
            float[] positions = chunkMesh.positions(type);
            float[] normals = chunkMesh.normals(type);
            float[] texCoords = chunkMesh.texCoords(type);
            int[] indices = chunkMesh.indices(type);

            if (positions == null || positions.length == 0) {
                continue;
            }

            Mesh mesh = new Mesh();
            mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(positions));
            mesh.setBuffer(VertexBuffer.Type.Normal, 3, BufferUtils.createFloatBuffer(normals));
            mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(texCoords));
            mesh.setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createIntBuffer(indices));
            mesh.updateBound();

            Material material = blockRegistry.getMaterial(type);
            if (material == null) {
                continue;
            }

            Geometry geometry = new Geometry("chunk-geom-" + type.name().toLowerCase(), mesh);
            geometry.setMaterial(material);
            chunkNode.attachChild(geometry);

            totalVertices += chunkMesh.vertexCount(type);
            totalTriangles += chunkMesh.triangleCount(type);
        }

        chunkStats.put(coord, new ChunkStats(totalVertices, totalTriangles));
    }

    private static ChunkCoord worldToChunk(int worldX, int worldZ) {
        int chunkX = (int) Math.floor((double) worldX / Chunk.SIZE_X);
        int chunkZ = (int) Math.floor((double) worldZ / Chunk.SIZE_Z);
        return new ChunkCoord(chunkX, chunkZ);
    }

    private static int worldToLocal(int coord) {
        int value = coord % Chunk.SIZE_X;
        if (value < 0) {
            value += Chunk.SIZE_X;
        }
        return value;
    }

    public record ChunkCoord(int chunkX, int chunkZ) {
    }

    public record ChunkStats(int vertices, int triangles) {
        public static final ChunkStats EMPTY = new ChunkStats(0, 0);
    }
}
