package com.poorcraft.ultra.voxel;

import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.poorcraft.ultra.blocks.BlockFace;
import com.poorcraft.ultra.blocks.BlockRegistry;
import com.poorcraft.ultra.world.WorldGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.poorcraft.ultra.voxel.ChunkConstants.*;

/**
 * Central manager for chunk lifecycle.
 */
public class ChunkManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ChunkManager.class);
    
    private final Map<ChunkCoord, Chunk> chunks = new ConcurrentHashMap<>();
    private final Map<ChunkCoord, Geometry> chunkGeometries = new ConcurrentHashMap<>();
    private final Queue<ChunkCoord> dirtyChunks = new LinkedList<>();
    
    private GreedyMesher mesher;
    private ChunkRenderer renderer;
    private Node chunkNode;
    private WorldGenerator worldGenerator;
    private LightEngine lightEngine;
    private int updateBudget = MAX_CHUNK_UPDATES_PER_FRAME;
    
    /**
     * Initializes chunk manager subsystems.
     */
    public void init(AssetManager assetManager, Node rootNode) {
        init(assetManager, rootNode, 0L);
    }
    
    /**
     * Initializes chunk manager subsystems with world seed.
     */
    public void init(AssetManager assetManager, Node rootNode, long worldSeed) {
        logger.info("Initializing ChunkManager with seed {}...", worldSeed);
        
        BlockRegistry blockRegistry = BlockRegistry.getInstance();
        
        // Create texture atlas
        TextureAtlas textureAtlas = new TextureAtlas();
        textureAtlas.build(assetManager);
        
        // Create renderer
        renderer = new ChunkRenderer(assetManager, textureAtlas);
        renderer.init();
        
        // Create world generator
        worldGenerator = new WorldGenerator(worldSeed);
        
        // Create mesher with biome provider
        mesher = new GreedyMesher(blockRegistry, textureAtlas, worldGenerator.getBiomeProvider());
        
        // Create chunk node
        chunkNode = new Node("ChunkNode");
        rootNode.attachChild(chunkNode);
        
        // Create light engine
        lightEngine = new LightEngine(this, blockRegistry);
        
        logger.info("ChunkManager initialized");
    }
    
    /**
     * Loads or creates chunk.
     */
    public Chunk loadChunk(int chunkX, int chunkZ) {
        ChunkCoord coord = new ChunkCoord(chunkX, chunkZ);
        
        Chunk chunk = chunks.get(coord);
        if (chunk != null) {
            return chunk;
        }
        
        // Generate chunk using WorldGenerator
        if (worldGenerator != null) {
            chunk = worldGenerator.generateChunk(chunkX, chunkZ);
        } else {
            // Fallback to empty chunk if no generator
            chunk = new Chunk(chunkX, chunkZ);
            chunk.markDirty();
        }
        
        // Initialize lighting for new chunk
        if (lightEngine != null) {
            lightEngine.initializeSkylight(chunk);
            lightEngine.initializeBlockLight(chunk);
            chunk.markDirty();
        }
        
        addOrReplaceChunk(chunk);
        
        logger.debug("Loaded chunk ({}, {})", chunkX, chunkZ);
        return chunk;
    }
    
    /**
     * Adds or replaces a chunk in the world and triggers remeshing.
     */
    public void addOrReplaceChunk(Chunk chunk) {
        ChunkCoord coord = new ChunkCoord(chunk.getChunkX(), chunk.getChunkZ());
        
        // Store chunk
        chunks.put(coord, chunk);
        
        // Mark dirty and enqueue for remeshing
        chunk.markDirty();
        if (!dirtyChunks.contains(coord)) {
            dirtyChunks.offer(coord);
        }
        
        logger.debug("Added/replaced chunk ({}, {})", chunk.getChunkX(), chunk.getChunkZ());
    }
    
    /**
     * Unloads chunk.
     */
    public void unloadChunk(int chunkX, int chunkZ) {
        ChunkCoord coord = new ChunkCoord(chunkX, chunkZ);
        
        chunks.remove(coord);
        
        Geometry geometry = chunkGeometries.remove(coord);
        if (geometry != null) {
            chunkNode.detachChild(geometry);
        }
        
        logger.debug("Unloaded chunk ({}, {})", chunkX, chunkZ);
    }
    
    /**
     * Rebuilds mesh for chunk.
     */
    public void remeshChunk(ChunkCoord coord) {
        Chunk chunk = chunks.get(coord);
        if (chunk == null) {
            return;
        }
        
        // Get neighbor chunks
        Map<BlockFace, Chunk> neighbors = new HashMap<>();
        neighbors.put(BlockFace.NORTH, chunks.get(new ChunkCoord(coord.x, coord.z - 1)));
        neighbors.put(BlockFace.SOUTH, chunks.get(new ChunkCoord(coord.x, coord.z + 1)));
        neighbors.put(BlockFace.EAST, chunks.get(new ChunkCoord(coord.x + 1, coord.z)));
        neighbors.put(BlockFace.WEST, chunks.get(new ChunkCoord(coord.x - 1, coord.z)));
        
        // Generate mesh
        MeshData meshData = mesher.mesh(chunk, neighbors);
        
        // Create or update geometry
        Geometry geometry = chunkGeometries.get(coord);
        if (geometry == null) {
            geometry = renderer.createChunkGeometry(chunk, meshData);
            if (geometry != null) {
                chunkNode.attachChild(geometry);
                chunkGeometries.put(coord, geometry);
            }
        } else {
            boolean removed = renderer.updateChunkGeometry(geometry, meshData);
            if (removed) {
                // Remove from map when geometry becomes empty
                chunkGeometries.remove(coord);
            }
        }
        
        chunk.clearDirty();
        logger.debug("Remeshed chunk ({}, {})", coord.x, coord.z);
    }
    
    /**
     * Update loop - processes dirty chunks and lighting.
     */
    public void update(float tpf) {
        // Update lighting engine
        if (lightEngine != null) {
            lightEngine.update(tpf);
        }
        
        // Process dirty chunks
        int processed = 0;
        
        while (processed < updateBudget && !dirtyChunks.isEmpty()) {
            ChunkCoord coord = dirtyChunks.poll();
            if (coord != null) {
                remeshChunk(coord);
                processed++;
            }
        }
        
        if (!dirtyChunks.isEmpty()) {
            logger.debug("Mesh queue backlog: {} chunks", dirtyChunks.size());
        }
    }
    
    /**
     * Sets block in world coordinates.
     */
    public void setBlock(int worldX, int worldY, int worldZ, short blockId) {
        int chunkX = worldToChunk(worldX);
        int chunkZ = worldToChunk(worldZ);
        
        int localX = worldToBlock(worldX);
        int localY = worldY;
        int localZ = worldToBlock(worldZ);
        
        Chunk chunk = loadChunk(chunkX, chunkZ);
        short oldBlockId = chunk.getBlock(localX, localY, localZ);
        chunk.setBlock(localX, localY, localZ, blockId);
        
        // Update lighting when block changes
        if (lightEngine != null && oldBlockId != blockId) {
            BlockRegistry blockRegistry = BlockRegistry.getInstance();
            
            // Handle skylight changes
            boolean oldTransparent = oldBlockId == 0 || blockRegistry.getBlock(oldBlockId).isTransparent();
            boolean newTransparent = blockId == 0 || blockRegistry.getBlock(blockId).isTransparent();
            
            if (oldTransparent != newTransparent) {
                if (newTransparent) {
                    // Block removed or became transparent - propagate skylight
                    lightEngine.propagateSkylight(chunkX, chunkZ, localX, localY, localZ, chunk.getSkyLight(localX, localY, localZ));
                } else {
                    // Block placed or became opaque - remove skylight
                    lightEngine.removeSkylight(chunkX, chunkZ, localX, localY, localZ);
                }
            }
            
            // Handle block light changes
            int oldEmission = blockRegistry.getBlock(oldBlockId).getLightEmission();
            int newEmission = blockRegistry.getBlock(blockId).getLightEmission();
            
            if (oldEmission != newEmission) {
                if (newEmission > 0) {
                    // New light source - propagate
                    chunk.setBlockLight(localX, localY, localZ, newEmission);
                    lightEngine.propagateBlockLight(chunkX, chunkZ, localX, localY, localZ, newEmission);
                } else if (oldEmission > 0) {
                    // Light source removed
                    lightEngine.removeBlockLight(chunkX, chunkZ, localX, localY, localZ);
                }
            }
        }
        
        ChunkCoord coord = new ChunkCoord(chunkX, chunkZ);
        if (!dirtyChunks.contains(coord)) {
            dirtyChunks.offer(coord);
        }
        
        // Mark neighbor chunks dirty if on edge
        if (localX == 0) {
            markChunkDirty(chunkX - 1, chunkZ);
        } else if (localX == CHUNK_SIZE_X - 1) {
            markChunkDirty(chunkX + 1, chunkZ);
        }
        
        if (localZ == 0) {
            markChunkDirty(chunkX, chunkZ - 1);
        } else if (localZ == CHUNK_SIZE_Z - 1) {
            markChunkDirty(chunkX, chunkZ + 1);
        }
    }
    
    /**
     * Gets block from world coordinates.
     */
    public short getBlock(int worldX, int worldY, int worldZ) {
        int chunkX = worldToChunk(worldX);
        int chunkZ = worldToChunk(worldZ);
        
        Chunk chunk = chunks.get(new ChunkCoord(chunkX, chunkZ));
        if (chunk == null) {
            return 0;
        }
        
        int localX = worldToBlock(worldX);
        int localY = worldY;
        int localZ = worldToBlock(worldZ);
        
        return chunk.getBlockSafe(localX, localY, localZ);
    }
    
    private void markChunkDirty(int chunkX, int chunkZ) {
        ChunkCoord coord = new ChunkCoord(chunkX, chunkZ);
        Chunk chunk = chunks.get(coord);
        if (chunk != null && !dirtyChunks.contains(coord)) {
            chunk.markDirty();
            dirtyChunks.offer(coord);
        }
    }
    
    /**
     * Loads/unloads chunks based on player position.
     */
    public void updateChunksAroundPlayer(Vector3f playerPos) {
        int playerChunkX = worldToChunk(playerPos.x);
        int playerChunkZ = worldToChunk(playerPos.z);
        
        // Load chunks within radius
        for (int x = playerChunkX - CHUNK_LOAD_RADIUS; x <= playerChunkX + CHUNK_LOAD_RADIUS; x++) {
            for (int z = playerChunkZ - CHUNK_LOAD_RADIUS; z <= playerChunkZ + CHUNK_LOAD_RADIUS; z++) {
                loadChunk(x, z);
            }
        }
        
        // Unload chunks beyond radius
        List<ChunkCoord> toUnload = new ArrayList<>();
        for (ChunkCoord coord : chunks.keySet()) {
            int dx = Math.abs(coord.x - playerChunkX);
            int dz = Math.abs(coord.z - playerChunkZ);
            if (dx > CHUNK_UNLOAD_RADIUS || dz > CHUNK_UNLOAD_RADIUS) {
                toUnload.add(coord);
            }
        }
        
        for (ChunkCoord coord : toUnload) {
            unloadChunk(coord.x, coord.z);
        }
    }
    
    /**
     * Gets all loaded chunks.
     */
    public Collection<Chunk> getAllChunks() {
        return chunks.values();
    }
    
    /**
     * Clears all chunks.
     */
    public void clear() {
        for (ChunkCoord coord : new ArrayList<>(chunks.keySet())) {
            unloadChunk(coord.x, coord.z);
        }
        dirtyChunks.clear();
    }
    
    /**
     * Chunk coordinate record.
     */
    public static record ChunkCoord(int x, int z) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ChunkCoord)) return false;
            ChunkCoord that = (ChunkCoord) o;
            return x == that.x && z == that.z;
        }
        
        @Override
        public int hashCode() {
            return 31 * x + z;
        }
    }
}
