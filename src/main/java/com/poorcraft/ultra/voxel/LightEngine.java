package com.poorcraft.ultra.voxel;

import com.poorcraft.ultra.blocks.BlockRegistry;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Lighting propagation engine using BFS flood-fill (CP v2.2).
 * Calculates and propagates skylight and block light through chunks.
 * 
 * Note: This is a simplified initial implementation. Full BFS propagation
 * with queues and neighbor chunk handling will be implemented in integration phase.
 */
public class LightEngine {

    private final ChunkManager chunkManager;
    private final BlockRegistry blockRegistry;
    private final int updateBudget;
    
    // BFS queues for light propagation
    private final Queue<LightNode> skylightAddQueue = new LinkedList<>();
    private final Queue<LightNode> skylightRemoveQueue = new LinkedList<>();
    private final Queue<LightNode> blockLightAddQueue = new LinkedList<>();
    private final Queue<LightNode> blockLightRemoveQueue = new LinkedList<>();
    
    private static class LightNode {
        final int chunkX, chunkZ;
        final int x, y, z;
        final int level;
        
        LightNode(int chunkX, int chunkZ, int x, int y, int z, int level) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.x = x;
            this.y = y;
            this.z = z;
            this.level = level;
        }
    }

    /**
     * Creates a light engine with the given chunk manager and block registry.
     */
    public LightEngine(ChunkManager chunkManager, BlockRegistry blockRegistry) {
        this.chunkManager = chunkManager;
        this.blockRegistry = blockRegistry;
        this.updateBudget = 1000; // Max light updates per frame
    }

    /**
     * Initializes skylight for a newly generated chunk.
     * Sets skylight to 15 for all blocks exposed to sky, propagates downward.
     */
    public void initializeSkylight(Chunk chunk) {
        // For each XZ column
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                // Find highest non-air block
                int topY = 15;
                for (int y = 15; y >= 0; y--) {
                    if (chunk.getBlock(x, y, z) != 0) {
                        topY = y;
                        break;
                    }
                }

                // Set skylight to 15 above topY, propagate down
                for (int y = 15; y > topY; y--) {
                    chunk.setSkyLight(x, y, z, 15);
                }

                // Propagate downward with falloff (simplified)
                int lightLevel = 15;
                for (int y = topY; y >= 0; y--) {
                    short blockId = chunk.getBlock(x, y, z);
                    if (blockId == 0) {
                        // Air: keep level 15 (skylight special rule)
                        chunk.setSkyLight(x, y, z, 15);
                    } else if (isTransparent(blockId)) {
                        // Transparent: reduce by 1
                        lightLevel = Math.max(0, lightLevel - 1);
                        chunk.setSkyLight(x, y, z, lightLevel);
                    } else {
                        // Solid: block light
                        chunk.setSkyLight(x, y, z, 0);
                        lightLevel = 0;
                    }
                }
            }
        }
    }

    /**
     * Initializes block light for emissive blocks in the chunk.
     */
    public void initializeBlockLight(Chunk chunk) {
        // Scan chunk for emissive blocks
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    short blockId = chunk.getBlock(x, y, z);
                    int emission = getBlockLightEmission(blockId);
                    
                    if (emission > 0) {
                        chunk.setBlockLight(x, y, z, emission);
                        // Queue for BFS propagation
                        blockLightAddQueue.offer(new LightNode(chunk.getChunkX(), chunk.getChunkZ(), x, y, z, emission));
                    }
                }
            }
        }
    }


    /**
     * Updates lighting (called every frame).
     * Processes BFS queues with budget.
     */
    public void update(float tpf) {
        int processed = 0;
        
        // Process skylight removal
        while (processed < updateBudget && !skylightRemoveQueue.isEmpty()) {
            LightNode node = skylightRemoveQueue.poll();
            processSkylightRemoval(node);
            processed++;
        }
        
        // Process skylight addition
        while (processed < updateBudget && !skylightAddQueue.isEmpty()) {
            LightNode node = skylightAddQueue.poll();
            processSkylightAddition(node);
            processed++;
        }
        
        // Process block light removal
        while (processed < updateBudget && !blockLightRemoveQueue.isEmpty()) {
            LightNode node = blockLightRemoveQueue.poll();
            processBlockLightRemoval(node);
            processed++;
        }
        
        // Process block light addition
        while (processed < updateBudget && !blockLightAddQueue.isEmpty()) {
            LightNode node = blockLightAddQueue.poll();
            processBlockLightAddition(node);
            processed++;
        }
    }

    /**
     * Propagates skylight from a source position.
     */
    public void propagateSkylight(int chunkX, int chunkZ, int x, int y, int z, int level) {
        skylightAddQueue.offer(new LightNode(chunkX, chunkZ, x, y, z, level));
    }

    /**
     * Removes skylight when block is placed.
     */
    public void removeSkylight(int chunkX, int chunkZ, int x, int y, int z) {
        Chunk chunk = chunkManager.getAllChunks().stream()
            .filter(c -> c.getChunkX() == chunkX && c.getChunkZ() == chunkZ)
            .findFirst().orElse(null);
        if (chunk != null) {
            int level = chunk.getSkyLight(x, y, z);
            if (level > 0) {
                skylightRemoveQueue.offer(new LightNode(chunkX, chunkZ, x, y, z, level));
                chunk.setSkyLight(x, y, z, 0);
            }
        }
    }

    /**
     * Propagates block light from a source position.
     */
    public void propagateBlockLight(int chunkX, int chunkZ, int x, int y, int z, int level) {
        blockLightAddQueue.offer(new LightNode(chunkX, chunkZ, x, y, z, level));
    }

    /**
     * Removes block light when source is removed.
     */
    public void removeBlockLight(int chunkX, int chunkZ, int x, int y, int z) {
        Chunk chunk = chunkManager.getAllChunks().stream()
            .filter(c -> c.getChunkX() == chunkX && c.getChunkZ() == chunkZ)
            .findFirst().orElse(null);
        if (chunk != null) {
            int level = chunk.getBlockLight(x, y, z);
            if (level > 0) {
                blockLightRemoveQueue.offer(new LightNode(chunkX, chunkZ, x, y, z, level));
                chunk.setBlockLight(x, y, z, 0);
            }
        }
    }
    
    private void processSkylightAddition(LightNode node) {
        Chunk chunk = getChunk(node.chunkX, node.chunkZ);
        if (chunk == null) return;
        
        int currentLevel = chunk.getSkyLight(node.x, node.y, node.z);
        if (node.level <= currentLevel) return;
        
        chunk.setSkyLight(node.x, node.y, node.z, node.level);
        chunk.markDirty();
        
        // Propagate to neighbors
        propagateToNeighbors(node, true);
    }
    
    private void processSkylightRemoval(LightNode node) {
        // Check all neighbors
        int[][] offsets = {{1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}, {0,0,1}, {0,0,-1}};
        
        for (int[] offset : offsets) {
            int nx = node.x + offset[0];
            int ny = node.y + offset[1];
            int nz = node.z + offset[2];
            
            int nChunkX = node.chunkX;
            int nChunkZ = node.chunkZ;
            
            // Handle chunk boundaries
            if (nx < 0) { nChunkX--; nx = 15; }
            else if (nx >= 16) { nChunkX++; nx = 0; }
            if (nz < 0) { nChunkZ--; nz = 15; }
            else if (nz >= 16) { nChunkZ++; nz = 0; }
            if (ny < 0 || ny >= 16) continue;
            
            Chunk neighborChunk = getChunk(nChunkX, nChunkZ);
            if (neighborChunk == null) continue;
            
            int neighborLevel = neighborChunk.getSkyLight(nx, ny, nz);
            if (neighborLevel > 0 && neighborLevel < node.level) {
                skylightRemoveQueue.offer(new LightNode(nChunkX, nChunkZ, nx, ny, nz, neighborLevel));
                neighborChunk.setSkyLight(nx, ny, nz, 0);
            } else if (neighborLevel >= node.level) {
                skylightAddQueue.offer(new LightNode(nChunkX, nChunkZ, nx, ny, nz, neighborLevel));
            }
        }
    }
    
    private void processBlockLightAddition(LightNode node) {
        Chunk chunk = getChunk(node.chunkX, node.chunkZ);
        if (chunk == null) return;
        
        int currentLevel = chunk.getBlockLight(node.x, node.y, node.z);
        if (node.level <= currentLevel) return;
        
        chunk.setBlockLight(node.x, node.y, node.z, node.level);
        chunk.markDirty();
        
        // Propagate to neighbors
        propagateToNeighbors(node, false);
    }
    
    private void processBlockLightRemoval(LightNode node) {
        // Check all neighbors
        int[][] offsets = {{1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}, {0,0,1}, {0,0,-1}};
        
        for (int[] offset : offsets) {
            int nx = node.x + offset[0];
            int ny = node.y + offset[1];
            int nz = node.z + offset[2];
            
            int nChunkX = node.chunkX;
            int nChunkZ = node.chunkZ;
            
            // Handle chunk boundaries
            if (nx < 0) { nChunkX--; nx = 15; }
            else if (nx >= 16) { nChunkX++; nx = 0; }
            if (nz < 0) { nChunkZ--; nz = 15; }
            else if (nz >= 16) { nChunkZ++; nz = 0; }
            if (ny < 0 || ny >= 16) continue;
            
            Chunk neighborChunk = getChunk(nChunkX, nChunkZ);
            if (neighborChunk == null) continue;
            
            int neighborLevel = neighborChunk.getBlockLight(nx, ny, nz);
            if (neighborLevel > 0 && neighborLevel < node.level) {
                blockLightRemoveQueue.offer(new LightNode(nChunkX, nChunkZ, nx, ny, nz, neighborLevel));
                neighborChunk.setBlockLight(nx, ny, nz, 0);
            } else if (neighborLevel >= node.level) {
                blockLightAddQueue.offer(new LightNode(nChunkX, nChunkZ, nx, ny, nz, neighborLevel));
            }
        }
    }
    
    private void propagateToNeighbors(LightNode node, boolean isSkylight) {
        if (node.level <= 1) return;
        
        int[][] offsets = {{1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}, {0,0,1}, {0,0,-1}};
        
        for (int[] offset : offsets) {
            int nx = node.x + offset[0];
            int ny = node.y + offset[1];
            int nz = node.z + offset[2];
            
            int nChunkX = node.chunkX;
            int nChunkZ = node.chunkZ;
            
            // Handle chunk boundaries
            if (nx < 0) { nChunkX--; nx = 15; }
            else if (nx >= 16) { nChunkX++; nx = 0; }
            if (nz < 0) { nChunkZ--; nz = 15; }
            else if (nz >= 16) { nChunkZ++; nz = 0; }
            if (ny < 0 || ny >= 16) continue;
            
            Chunk neighborChunk = getChunk(nChunkX, nChunkZ);
            if (neighborChunk == null) continue;
            
            short blockId = neighborChunk.getBlockSafe(nx, ny, nz);
            if (!isTransparent(blockId)) continue;
            
            int newLevel = node.level - 1;
            int currentLevel = isSkylight ? neighborChunk.getSkyLight(nx, ny, nz) : neighborChunk.getBlockLight(nx, ny, nz);
            
            if (newLevel > currentLevel) {
                if (isSkylight) {
                    skylightAddQueue.offer(new LightNode(nChunkX, nChunkZ, nx, ny, nz, newLevel));
                } else {
                    blockLightAddQueue.offer(new LightNode(nChunkX, nChunkZ, nx, ny, nz, newLevel));
                }
            }
        }
    }
    
    private Chunk getChunk(int chunkX, int chunkZ) {
        return chunkManager.getAllChunks().stream()
            .filter(c -> c.getChunkX() == chunkX && c.getChunkZ() == chunkZ)
            .findFirst().orElse(null);
    }

    /**
     * Checks if a block allows light propagation.
     */
    private boolean isTransparent(short blockId) {
        if (blockId == 0) return true; // Air always transparent
        return blockRegistry.getBlock(blockId).isTransparent();
    }

    /**
     * Returns light emission level for a block (0-15).
     */
    private int getBlockLightEmission(short blockId) {
        return blockRegistry.getBlock(blockId).getLightEmission();
    }
}
