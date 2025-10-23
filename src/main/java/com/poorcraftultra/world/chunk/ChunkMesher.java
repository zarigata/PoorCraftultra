package com.poorcraftultra.world.chunk;

import org.joml.Vector3f;

/**
 * Implements the greedy meshing algorithm for chunk rendering optimization.
 * 
 * <p>Greedy meshing scans each axis and merges adjacent faces of the same block type
 * into larger quads, reducing vertex count by 90%+ compared to naive per-block meshing.
 * Uses face culling to skip faces between solid blocks.
 * 
 * <p>The algorithm works by:
 * <ol>
 *   <li>Scanning through each section in all 6 directions</li>
 *   <li>Creating a 2D mask for each slice perpendicular to the scan direction</li>
 *   <li>Merging adjacent visible faces of the same block type into larger quads</li>
 *   <li>Generating optimized vertex and index data</li>
 * </ol>
 */
public class ChunkMesher {
    private final ChunkManager chunkManager;
    
    /**
     * Represents the six cardinal directions for face culling and meshing.
     */
    public enum Direction {
        NORTH(0, 0, -1),
        SOUTH(0, 0, 1),
        EAST(1, 0, 0),
        WEST(-1, 0, 0),
        UP(0, 1, 0),
        DOWN(0, -1, 0);
        
        public final int dx, dy, dz;
        
        Direction(int dx, int dy, int dz) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
        }
    }
    
    /**
     * Creates a new chunk mesher.
     * 
     * @param chunkManager reference to chunk manager for neighbor access during face culling
     */
    public ChunkMesher(ChunkManager chunkManager) {
        this.chunkManager = chunkManager;
    }
    
    /**
     * Dynamic primitive float array with growth strategy.
     */
    private static class FloatBuffer {
        private float[] data;
        private int size;
        
        FloatBuffer(int initialCapacity) {
            this.data = new float[initialCapacity];
            this.size = 0;
        }
        
        void add(float value) {
            if (size >= data.length) {
                grow();
            }
            data[size++] = value;
        }
        
        private void grow() {
            int newCapacity = data.length * 2;
            float[] newData = new float[newCapacity];
            System.arraycopy(data, 0, newData, 0, size);
            data = newData;
        }
        
        float[] toArray() {
            float[] result = new float[size];
            System.arraycopy(data, 0, result, 0, size);
            return result;
        }
        
        int size() {
            return size;
        }
    }
    
    /**
     * Dynamic primitive int array with growth strategy.
     */
    private static class IntBuffer {
        private int[] data;
        private int size;
        
        IntBuffer(int initialCapacity) {
            this.data = new int[initialCapacity];
            this.size = 0;
        }
        
        void add(int value) {
            if (size >= data.length) {
                grow();
            }
            data[size++] = value;
        }
        
        private void grow() {
            int newCapacity = data.length * 2;
            int[] newData = new int[newCapacity];
            System.arraycopy(data, 0, newData, 0, size);
            data = newData;
        }
        
        int[] toArray() {
            int[] result = new int[size];
            System.arraycopy(data, 0, result, 0, size);
            return result;
        }
        
        int size() {
            return size;
        }
    }
    
    /**
     * Generates a complete mesh for the given chunk.
     * 
     * @param chunk the chunk to generate a mesh for
     * @return the generated chunk mesh
     */
    public ChunkMesh generateMesh(Chunk chunk) {
        // Pre-allocate with reasonable initial capacity
        FloatBuffer allVertices = new FloatBuffer(4096);
        IntBuffer allIndices = new IntBuffer(2048);
        
        // Iterate through all 16 sections
        for (int sectionIndex = 0; sectionIndex < 16; sectionIndex++) {
            ChunkSection section = chunk.getSection(sectionIndex);
            if (section == null || section.isEmpty()) {
                continue;
            }
            
            generateSectionMesh(chunk, sectionIndex, allVertices, allIndices);
        }
        
        return new ChunkMesh(allVertices.toArray(), allIndices.toArray());
    }
    
    /**
     * Generates mesh data for a single section.
     */
    private void generateSectionMesh(Chunk chunk, int sectionIndex, FloatBuffer vertices, IntBuffer indices) {
        // Generate meshes for all 6 directions
        for (Direction direction : Direction.values()) {
            greedyMesh(chunk, sectionIndex, direction, vertices, indices);
        }
    }
    
    /**
     * Implements greedy meshing algorithm for one face direction.
     * 
     * @param chunk the chunk being meshed
     * @param sectionIndex the section index (0-15)
     * @param direction the face direction to mesh
     * @param vertices output vertex buffer
     * @param indices output index buffer
     */
    private void greedyMesh(Chunk chunk, int sectionIndex, Direction direction, 
                           FloatBuffer vertices, IntBuffer indices) {
        int sectionY = sectionIndex * 16;
        
        // Determine axis and dimensions based on direction
        boolean[] mask = new boolean[16 * 16];
        byte[] blockTypes = new byte[16 * 16];
        
        // Scan through the section in the direction axis
        for (int d = 0; d < 16; d++) {
            // Clear mask
            for (int i = 0; i < mask.length; i++) {
                mask[i] = false;
                blockTypes[i] = 0;
            }
            
            // Build mask for this slice
            for (int u = 0; u < 16; u++) {
                for (int v = 0; v < 16; v++) {
                    int x, y, z;
                    
                    // Map (d, u, v) to (x, y, z) based on direction
                    switch (direction) {
                        case NORTH:
                        case SOUTH:
                            x = u; y = v; z = d;
                            break;
                        case EAST:
                        case WEST:
                            x = d; y = v; z = u;
                            break;
                        case UP:
                        case DOWN:
                            x = u; y = d; z = v;
                            break;
                        default:
                            continue;
                    }
                    
                    int worldY = sectionY + y;
                    
                    if (shouldRenderFace(chunk, x, worldY, z, direction)) {
                        mask[u + v * 16] = true;
                        blockTypes[u + v * 16] = chunk.getBlock(x, worldY, z);
                    }
                }
            }
            
            // Generate quads from mask using greedy meshing
            for (int v = 0; v < 16; v++) {
                for (int u = 0; u < 16; ) {
                    if (!mask[u + v * 16]) {
                        u++;
                        continue;
                    }
                    
                    byte blockType = blockTypes[u + v * 16];
                    
                    // Compute width
                    int width = 1;
                    while (u + width < 16 && mask[u + width + v * 16] && blockTypes[u + width + v * 16] == blockType) {
                        width++;
                    }
                    
                    // Compute height
                    int height = 1;
                    boolean done = false;
                    while (v + height < 16) {
                        for (int k = 0; k < width; k++) {
                            if (!mask[u + k + (v + height) * 16] || blockTypes[u + k + (v + height) * 16] != blockType) {
                                done = true;
                                break;
                            }
                        }
                        if (done) break;
                        height++;
                    }
                    
                    // Add quad
                    int x, y, z;
                    switch (direction) {
                        case NORTH:
                        case SOUTH:
                            x = u; y = v; z = d;
                            break;
                        case EAST:
                        case WEST:
                            x = d; y = v; z = u;
                            break;
                        case UP:
                        case DOWN:
                            x = u; y = d; z = v;
                            break;
                        default:
                            continue;
                    }
                    
                    int worldY = sectionY + y;
                    addQuad(chunk, x, worldY, z, width, height, direction, blockType, vertices, indices);
                    
                    // Clear mask
                    for (int h = 0; h < height; h++) {
                        for (int w = 0; w < width; w++) {
                            mask[u + w + (v + h) * 16] = false;
                        }
                    }
                    
                    u += width;
                }
            }
        }
    }
    
    /**
     * Determines if a face should be rendered (face culling logic).
     * 
     * @return true if the face is visible and should be rendered
     */
    private boolean shouldRenderFace(Chunk chunk, int x, int y, int z, Direction direction) {
        byte block = chunk.getBlock(x, y, z);
        if (block == 0) {
            return false; // Air blocks have no faces
        }
        
        // Calculate neighbor position
        int nx = x + direction.dx;
        int ny = y + direction.dy;
        int nz = z + direction.dz;
        
        byte neighbor;
        
        // Check if neighbor is within chunk bounds
        if (nx >= 0 && nx < 16 && ny >= 0 && ny < 256 && nz >= 0 && nz < 16) {
            neighbor = chunk.getBlock(nx, ny, nz);
        } else {
            // Neighbor is in adjacent chunk
            ChunkPos pos = chunk.getPosition();
            int chunkX = pos.getX();
            int chunkY = pos.getY();
            int chunkZ = pos.getZ();
            
            if (nx < 0) chunkX--;
            else if (nx >= 16) chunkX++;
            
            if (nz < 0) chunkZ--;
            else if (nz >= 16) chunkZ++;
            
            if (ny < 0 || ny >= 256) {
                return true; // Face at world boundary is visible
            }
            
            Chunk neighborChunk = chunkManager.getChunk(new ChunkPos(chunkX, chunkY, chunkZ));
            if (neighborChunk == null) {
                return true; // Neighbor chunk not loaded, render face
            }
            
            int localX = (nx + 16) % 16;
            int localZ = (nz + 16) % 16;
            neighbor = neighborChunk.getBlock(localX, ny, localZ);
        }
        
        return neighbor == 0; // Face is visible if neighbor is air
    }
    
    /**
     * Adds a quad to the mesh data.
     */
    private void addQuad(Chunk chunk, int x, int y, int z, int width, int height, 
                        Direction direction, byte blockType, FloatBuffer vertices, IntBuffer indices) {
        Vector3f color = getBlockColor(blockType, direction);
        
        // Calculate world position offset for the chunk
        float offsetX = chunk.getPosition().getX() * 16.0f;
        float offsetZ = chunk.getPosition().getZ() * 16.0f;
        
        // Define quad corners based on direction
        Vector3f[] corners = new Vector3f[4];
        
        switch (direction) {
            case NORTH: // -Z face
                corners[0] = new Vector3f(offsetX + x, y, offsetZ + z);
                corners[1] = new Vector3f(offsetX + x + width, y, offsetZ + z);
                corners[2] = new Vector3f(offsetX + x + width, y + height, offsetZ + z);
                corners[3] = new Vector3f(offsetX + x, y + height, offsetZ + z);
                break;
            case SOUTH: // +Z face
                corners[0] = new Vector3f(offsetX + x, y, offsetZ + z + 1);
                corners[1] = new Vector3f(offsetX + x, y + height, offsetZ + z + 1);
                corners[2] = new Vector3f(offsetX + x + width, y + height, offsetZ + z + 1);
                corners[3] = new Vector3f(offsetX + x + width, y, offsetZ + z + 1);
                break;
            case WEST: // -X face
                corners[0] = new Vector3f(offsetX + x, y, offsetZ + z);
                corners[1] = new Vector3f(offsetX + x, y + height, offsetZ + z);
                corners[2] = new Vector3f(offsetX + x, y + height, offsetZ + z + width);
                corners[3] = new Vector3f(offsetX + x, y, offsetZ + z + width);
                break;
            case EAST: // +X face
                corners[0] = new Vector3f(offsetX + x + 1, y, offsetZ + z);
                corners[1] = new Vector3f(offsetX + x + 1, y, offsetZ + z + width);
                corners[2] = new Vector3f(offsetX + x + 1, y + height, offsetZ + z + width);
                corners[3] = new Vector3f(offsetX + x + 1, y + height, offsetZ + z);
                break;
            case DOWN: // -Y face
                corners[0] = new Vector3f(offsetX + x, y, offsetZ + z);
                corners[1] = new Vector3f(offsetX + x, y, offsetZ + z + height);
                corners[2] = new Vector3f(offsetX + x + width, y, offsetZ + z + height);
                corners[3] = new Vector3f(offsetX + x + width, y, offsetZ + z);
                break;
            case UP: // +Y face
                corners[0] = new Vector3f(offsetX + x, y + 1, offsetZ + z);
                corners[1] = new Vector3f(offsetX + x + width, y + 1, offsetZ + z);
                corners[2] = new Vector3f(offsetX + x + width, y + 1, offsetZ + z + height);
                corners[3] = new Vector3f(offsetX + x, y + 1, offsetZ + z + height);
                break;
        }
        
        addQuadVertices(corners, color, vertices, indices);
    }
    
    /**
     * Adds quad vertices and indices to the mesh data.
     */
    private void addQuadVertices(Vector3f[] corners, Vector3f color, FloatBuffer vertices, IntBuffer indices) {
        int baseIndex = vertices.size() / 6; // Current vertex count
        
        // Add 4 vertices
        for (Vector3f corner : corners) {
            vertices.add(corner.x);
            vertices.add(corner.y);
            vertices.add(corner.z);
            vertices.add(color.x);
            vertices.add(color.y);
            vertices.add(color.z);
        }
        
        // Add 6 indices (2 triangles)
        indices.add(baseIndex);
        indices.add(baseIndex + 1);
        indices.add(baseIndex + 2);
        
        indices.add(baseIndex);
        indices.add(baseIndex + 2);
        indices.add(baseIndex + 3);
    }
    
    /**
     * Returns the color for a block face based on direction (simple per-face shading).
     * 
     * @param blockId the block type ID
     * @param face the face direction
     * @return RGB color vector
     */
    private Vector3f getBlockColor(byte blockId, Direction face) {
        // Simple per-face shading for now
        switch (face) {
            case UP:
                return new Vector3f(1.0f, 1.0f, 1.0f); // Brightest (top)
            case DOWN:
                return new Vector3f(0.5f, 0.5f, 0.5f); // Darkest (bottom)
            case NORTH:
                return new Vector3f(0.7f, 0.7f, 0.7f); // Medium
            case SOUTH:
                return new Vector3f(0.75f, 0.75f, 0.75f); // Medium-light
            case EAST:
                return new Vector3f(0.8f, 0.8f, 0.8f); // Light
            case WEST:
                return new Vector3f(0.85f, 0.85f, 0.85f); // Light
            default:
                return new Vector3f(0.7f, 0.7f, 0.7f);
        }
    }
}
