package com.poorcraftultra.world.chunk;

import com.poorcraftultra.rendering.TextureAtlas;
import com.poorcraftultra.world.block.Block;
import com.poorcraftultra.world.block.BlockFace;
import com.poorcraftultra.world.block.BlockRegistry;
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
    private final TextureAtlas textureAtlas;
    
    /**
     * Creates a new chunk mesher.
     * 
     * @param chunkManager reference to chunk manager for neighbor access during face culling
     * @param textureAtlas reference to texture atlas for UV coordinate lookup
     */
    public ChunkMesher(ChunkManager chunkManager, TextureAtlas textureAtlas) {
        this.chunkManager = chunkManager;
        this.textureAtlas = textureAtlas;
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
        // Pre-allocate with reasonable initial capacity (10 floats per vertex now)
        FloatBuffer allVertices = new FloatBuffer(7680);
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
        for (BlockFace direction : BlockFace.values()) {
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
    private void greedyMesh(Chunk chunk, int sectionIndex, BlockFace direction, 
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
                        case TOP:
                        case BOTTOM:
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
                        case TOP:
                        case BOTTOM:
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
    private boolean shouldRenderFace(Chunk chunk, int x, int y, int z, BlockFace direction) {
        byte blockId = chunk.getBlock(x, y, z);
        Block block = BlockRegistry.getInstance().getBlock(blockId);
        if (!block.isSolid()) {
            return false; // Non-solid blocks (like air) have no faces
        }
        
        // Calculate neighbor position
        int nx = x + direction.getDx();
        int ny = y + direction.getDy();
        int nz = z + direction.getDz();
        
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
        
        Block neighborBlock = BlockRegistry.getInstance().getBlock(neighbor);
        
        // Face culling logic:
        // 1. Always render if neighbor is non-solid (air, etc.)
        // 2. Cull if neighbor is solid and opaque (same material)
        // 3. Render when transitioning between opaque and transparent
        // 4. Cull between two identical transparent blocks (e.g., glass-to-glass)
        
        if (!neighborBlock.isSolid()) {
            return true; // Neighbor is air or non-solid, render face
        }
        
        // Both blocks are solid at this point
        if (block.isTransparent() && neighborBlock.isTransparent()) {
            // Both transparent: only cull if they're the same block type
            return blockId != neighbor;
        }
        
        if (block.isTransparent() || neighborBlock.isTransparent()) {
            // One transparent, one opaque: always render the transition
            return true;
        }
        
        // Both opaque and solid: cull the face
        return false;
    }
    
    /**
     * Adds a quad to the mesh data.
     */
    private void addQuad(Chunk chunk, int x, int y, int z, int width, int height, 
                        BlockFace direction, byte blockType, FloatBuffer vertices, IntBuffer indices) {
        Vector3f color = getBlockColor(blockType, direction);
        
        // Get texture UV coordinates
        Block block = BlockRegistry.getInstance().getBlock(blockType);
        Block.TextureReference texRef = block.getTextureReference(direction);
        TextureAtlas.AtlasPosition atlasPos = textureAtlas.getUVCoordinates(texRef.textureName);
        
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
            case BOTTOM: // -Y face
                corners[0] = new Vector3f(offsetX + x, y, offsetZ + z);
                corners[1] = new Vector3f(offsetX + x, y, offsetZ + z + height);
                corners[2] = new Vector3f(offsetX + x + width, y, offsetZ + z + height);
                corners[3] = new Vector3f(offsetX + x + width, y, offsetZ + z);
                break;
            case TOP: // +Y face
                corners[0] = new Vector3f(offsetX + x, y + 1, offsetZ + z);
                corners[1] = new Vector3f(offsetX + x + width, y + 1, offsetZ + z);
                corners[2] = new Vector3f(offsetX + x + width, y + 1, offsetZ + z + height);
                corners[3] = new Vector3f(offsetX + x, y + 1, offsetZ + z + height);
                break;
        }
        
        // Calculate base UV coordinates (tile bounds in atlas)
        float[] uvs = calculateUVsForQuad(atlasPos, width, height, direction);
        
        // Calculate face-local UV coordinates for tiling in shader
        // These represent coordinates in block units (0..width, 0..height)
        float[] faceUVs = new float[8];
        switch (direction) {
            case NORTH:
            case SOUTH:
            case EAST:
            case WEST:
            case TOP:
            case BOTTOM:
                // All faces: corners map to (0,0), (width,0), (width,height), (0,height)
                faceUVs[0] = 0;      faceUVs[1] = 0;       // Corner 0
                faceUVs[2] = width;  faceUVs[3] = 0;       // Corner 1
                faceUVs[4] = width;  faceUVs[5] = height;  // Corner 2
                faceUVs[6] = 0;      faceUVs[7] = height;  // Corner 3
                break;
        }
        
        // Calculate tile span for shader
        float tileSpanU = atlasPos.u1 - atlasPos.u0;
        float tileSpanV = atlasPos.v1 - atlasPos.v0;
        
        addQuadVertices(corners, color, uvs, faceUVs, tileSpanU, tileSpanV, vertices, indices);
    }
    
    /**
     * Calculates UV coordinates for a quad.
     * Returns the base tile UV rectangle without scaling, to prevent bleeding.
     * Tiling is handled in the shader using faceUV coordinates.
     * 
     * @param atlasPos the atlas position for the base texture tile
     * @param width the width of the quad in blocks (unused, kept for signature compatibility)
     * @param height the height of the quad in blocks (unused, kept for signature compatibility)
     * @param direction the face direction (unused, kept for signature compatibility)
     * @return array of 8 UV coordinates (4 vertices × 2 coordinates) representing the base tile
     */
    private float[] calculateUVsForQuad(TextureAtlas.AtlasPosition atlasPos, int width, int height, BlockFace direction) {
        // Return the base tile's UV rectangle without any scaling
        // This ensures we never sample outside the tile bounds
        // Tiling is handled in the fragment shader using faceUV and fract()
        return new float[] {
            atlasPos.u0, atlasPos.v0,  // Corner 0
            atlasPos.u1, atlasPos.v0,  // Corner 1
            atlasPos.u1, atlasPos.v1,  // Corner 2
            atlasPos.u0, atlasPos.v1   // Corner 3
        };
    }
    
    /**
     * Adds quad vertices and indices to the mesh data.
     * Vertex format: position (XYZ), color (RGB), texCoord (UV), faceUV (XY), tileSpan (UV)
     * Total: 12 floats per vertex
     */
    private void addQuadVertices(Vector3f[] corners, Vector3f color, float[] uvs, float[] faceUVs,
                                float tileSpanU, float tileSpanV, FloatBuffer vertices, IntBuffer indices) {
        int baseIndex = vertices.size() / 12; // Current vertex count (12 floats per vertex)
        
        // Add 4 vertices
        for (int i = 0; i < corners.length; i++) {
            Vector3f corner = corners[i];
            vertices.add(corner.x);           // Position X
            vertices.add(corner.y);           // Position Y
            vertices.add(corner.z);           // Position Z
            vertices.add(color.x);            // Color R
            vertices.add(color.y);            // Color G
            vertices.add(color.z);            // Color B
            vertices.add(uvs[i * 2]);         // Base atlas U (u0)
            vertices.add(uvs[i * 2 + 1]);     // Base atlas V (v0)
            vertices.add(faceUVs[i * 2]);     // Face-local U (in block units)
            vertices.add(faceUVs[i * 2 + 1]); // Face-local V (in block units)
            vertices.add(tileSpanU);          // Tile span U (u1-u0)
            vertices.add(tileSpanV);          // Tile span V (v1-v0)
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
    private Vector3f getBlockColor(byte blockId, BlockFace face) {
        // Simple per-face shading for lighting (multiplied with texture color in shader)
        switch (face) {
            case TOP:
                return new Vector3f(1.0f, 1.0f, 1.0f); // Brightest (top)
            case BOTTOM:
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
