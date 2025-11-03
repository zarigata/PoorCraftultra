package com.poorcraft.ultra.voxel;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.poorcraft.ultra.blocks.BlockDefinition;
import com.poorcraft.ultra.blocks.BlockFace;
import com.poorcraft.ultra.blocks.BlockRegistry;
import com.poorcraft.ultra.world.BiomeProvider;
import com.poorcraft.ultra.world.BiomeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.poorcraft.ultra.voxel.ChunkConstants.*;

/**
 * Greedy meshing algorithm implementation (based on 0fps.net algorithm).
 * Reference: https://0fps.net/2012/06/30/meshing-in-a-minecraft-game/
 */
public class GreedyMesher {
    
    private static final Logger logger = LoggerFactory.getLogger(GreedyMesher.class);
    
    private final BlockRegistry blockRegistry;
    private final TextureAtlas textureAtlas;
    private final BiomeProvider biomeProvider;
    
    // Biome tint color map
    private static final Map<BiomeType, ColorRGBA> BIOME_GRASS_TINTS = new HashMap<>();
    private static final Map<BiomeType, ColorRGBA> BIOME_LEAF_TINTS = new HashMap<>();
    
    static {
        // Grass tints per biome
        BIOME_GRASS_TINTS.put(BiomeType.PLAINS, new ColorRGBA(0.5f, 0.9f, 0.3f, 1.0f));      // Bright green
        BIOME_GRASS_TINTS.put(BiomeType.FOREST, new ColorRGBA(0.4f, 0.8f, 0.3f, 1.0f));      // Neutral green
        BIOME_GRASS_TINTS.put(BiomeType.DESERT, new ColorRGBA(0.7f, 0.7f, 0.4f, 1.0f));      // Pale yellow-green
        BIOME_GRASS_TINTS.put(BiomeType.MOUNTAINS, new ColorRGBA(0.4f, 0.7f, 0.5f, 1.0f));   // Bluish-green
        BIOME_GRASS_TINTS.put(BiomeType.TAIGA, new ColorRGBA(0.3f, 0.7f, 0.3f, 1.0f));       // Dark saturated green
        
        // Leaf tints per biome
        BIOME_LEAF_TINTS.put(BiomeType.PLAINS, new ColorRGBA(0.3f, 0.7f, 0.2f, 1.0f));       // Standard green
        BIOME_LEAF_TINTS.put(BiomeType.FOREST, new ColorRGBA(0.2f, 0.6f, 0.2f, 1.0f));       // Dark green
        BIOME_LEAF_TINTS.put(BiomeType.DESERT, new ColorRGBA(0.5f, 0.6f, 0.3f, 1.0f));       // Yellowish
        BIOME_LEAF_TINTS.put(BiomeType.MOUNTAINS, new ColorRGBA(0.3f, 0.6f, 0.4f, 1.0f));    // Cool green
        BIOME_LEAF_TINTS.put(BiomeType.TAIGA, new ColorRGBA(0.2f, 0.5f, 0.2f, 1.0f));        // Very dark green
    }
    
    public GreedyMesher(BlockRegistry blockRegistry, TextureAtlas textureAtlas, BiomeProvider biomeProvider) {
        this.blockRegistry = blockRegistry;
        this.textureAtlas = textureAtlas;
        this.biomeProvider = biomeProvider;
    }
    
    /**
     * Generates optimized mesh for chunk.
     */
    public MeshData mesh(Chunk chunk, Map<BlockFace, Chunk> neighbors) {
        MeshData meshData = new MeshData();
        
        if (chunk.isEmpty()) {
            return meshData;
        }
        
        // Process each axis
        for (int axis = 0; axis < 3; axis++) {
            // Compute slice count based on axis
            int sliceCount;
            switch (axis) {
                case 0: sliceCount = CHUNK_SIZE_X; break;
                case 1: sliceCount = CHUNK_SIZE_Y; break;
                case 2: sliceCount = CHUNK_SIZE_Z; break;
                default: throw new IllegalArgumentException("Invalid axis: " + axis);
            }
            
            // Process each slice perpendicular to axis
            for (int slice = 0; slice < sliceCount; slice++) {
                // Process both faces (positive and negative direction)
                processFace(chunk, neighbors, meshData, axis, slice, true);
                processFace(chunk, neighbors, meshData, axis, slice, false);
            }
        }
        
        logger.debug("Meshed chunk ({}, {}): {} vertices, {} triangles", 
            chunk.getChunkX(), chunk.getChunkZ(), 
            meshData.getVertexCount(), meshData.getTriangleCount());
        
        return meshData;
    }
    
    private void processFace(Chunk chunk, Map<BlockFace, Chunk> neighbors, MeshData meshData, 
                             int axis, int slice, boolean positive) {
        // Determine mask dimensions based on axis
        int maskWidth, maskHeight;
        switch (axis) {
            case 0: // X-faces: Y×Z
                maskWidth = CHUNK_SIZE_Z;
                maskHeight = CHUNK_SIZE_Y;
                break;
            case 1: // Y-faces: Z×X
                maskWidth = CHUNK_SIZE_X;
                maskHeight = CHUNK_SIZE_Z;
                break;
            case 2: // Z-faces: X×Y
                maskWidth = CHUNK_SIZE_X;
                maskHeight = CHUNK_SIZE_Y;
                break;
            default:
                throw new IllegalArgumentException("Invalid axis: " + axis);
        }
        
        short[][] mask = buildMask(chunk, neighbors, axis, slice, positive, maskWidth, maskHeight);
        List<Quad> quads = greedyMerge(mask, maskWidth, maskHeight);
        
        for (Quad quad : quads) {
            generateQuad(meshData, chunk, quad, axis, slice, positive);
        }
    }
    
    private short[][] buildMask(Chunk chunk, Map<BlockFace, Chunk> neighbors, 
                                int axis, int slice, boolean positive, int maskWidth, int maskHeight) {
        short[][] mask = new short[maskHeight][maskWidth];
        
        for (int i = 0; i < maskHeight; i++) {
            for (int j = 0; j < maskWidth; j++) {
                int x, y, z;
                
                // Map i,j to actual coordinates based on axis
                switch (axis) {
                    case 0: // X axis
                        x = slice;
                        y = i;
                        z = j;
                        break;
                    case 1: // Y axis
                        x = j;
                        y = slice;
                        z = i;
                        break;
                    case 2: // Z axis
                        x = i;
                        y = j;
                        z = slice;
                        break;
                    default:
                        continue;
                }
                
                short blockId = chunk.getBlockSafe(x, y, z);
                if (blockId == 0) {
                    mask[i][j] = 0;
                    continue;
                }
                
                BlockDefinition block = blockRegistry.getBlock(blockId);
                if (!block.isSolid()) {
                    mask[i][j] = 0;
                    continue;
                }
                
                // Get neighbor block
                int nx = x, ny = y, nz = z;
                switch (axis) {
                    case 0: nx += positive ? 1 : -1; break;
                    case 1: ny += positive ? 1 : -1; break;
                    case 2: nz += positive ? 1 : -1; break;
                }
                
                short neighborId = getBlockWithNeighbors(chunk, neighbors, nx, ny, nz, axis, positive);
                
                // Face is visible if neighbor is air or transparent
                if (neighborId == 0 || blockRegistry.getBlock(neighborId).isTransparent()) {
                    mask[i][j] = blockId;
                } else {
                    mask[i][j] = 0;
                }
            }
        }
        
        return mask;
    }
    
    private short getBlockWithNeighbors(Chunk chunk, Map<BlockFace, Chunk> neighbors, 
                                        int x, int y, int z, int axis, boolean positive) {
        // Check if within chunk bounds
        if (x >= 0 && x < CHUNK_SIZE_X && y >= 0 && y < CHUNK_SIZE_Y && z >= 0 && z < CHUNK_SIZE_Z) {
            return chunk.getBlockSafe(x, y, z);
        }
        
        // Check neighbor chunks
        if (neighbors == null) {
            return 0; // Treat as air if no neighbors provided
        }
        
        BlockFace face = null;
        int localX = x, localY = y, localZ = z;
        
        if (x < 0) {
            face = BlockFace.WEST;
            localX = CHUNK_SIZE_X - 1;
        } else if (x >= CHUNK_SIZE_X) {
            face = BlockFace.EAST;
            localX = 0;
        } else if (z < 0) {
            face = BlockFace.NORTH;
            localZ = CHUNK_SIZE_Z - 1;
        } else if (z >= CHUNK_SIZE_Z) {
            face = BlockFace.SOUTH;
            localZ = 0;
        } else if (y < 0 || y >= CHUNK_SIZE_Y) {
            return 0; // Out of world bounds
        }
        
        if (face != null) {
            Chunk neighborChunk = neighbors.get(face);
            if (neighborChunk != null) {
                return neighborChunk.getBlockSafe(localX, localY, localZ);
            }
        }
        
        return 0;
    }
    
    private List<Quad> greedyMerge(short[][] mask, int maskWidth, int maskHeight) {
        List<Quad> quads = new ArrayList<>();
        
        for (int i = 0; i < maskHeight; i++) {
            for (int j = 0; j < maskWidth; j++) {
                if (mask[i][j] == 0) {
                    continue;
                }
                
                short blockId = mask[i][j];
                
                // Expand horizontally (j direction)
                int width = 1;
                while (j + width < maskWidth && mask[i][j + width] == blockId) {
                    width++;
                }
                
                // Expand vertically (i direction)
                int height = 1;
                boolean canExpand = true;
                while (i + height < maskHeight && canExpand) {
                    for (int k = 0; k < width; k++) {
                        if (mask[i + height][j + k] != blockId) {
                            canExpand = false;
                            break;
                        }
                    }
                    if (canExpand) {
                        height++;
                    }
                }
                
                // Create quad
                quads.add(new Quad(j, i, width, height, blockId));
                
                // Mark merged cells as processed
                for (int h = 0; h < height; h++) {
                    for (int w = 0; w < width; w++) {
                        mask[i + h][j + w] = 0;
                    }
                }
            }
        }
        
        return quads;
    }
    
    private void generateQuad(MeshData meshData, Chunk chunk, Quad quad, 
                              int axis, int slice, boolean positive) {
        BlockDefinition block = blockRegistry.getBlock(quad.blockId);
        
        // Determine face direction
        BlockFace face = getFaceForAxis(axis, positive);
        
        // Get texture
        String textureName = block.getTexture(face);
        float[] uvs = textureAtlas.getUVs(textureName);
        
        // Scale UVs for tiling (if quad is larger than 1x1)
        float[] scaledUVs = scaleUVs(uvs, quad.width, quad.height);
        
        // Compute vertex positions
        Vector3f[] vertices = computeVertices(quad, axis, slice, positive);
        
        // Get normal
        Vector3f normal = face.getNormal();
        
        // Sample lighting at vertices
        ColorRGBA[] colors = sampleVertexLighting(chunk, quad, axis, slice, positive);
        
        // Add quad to mesh with vertex colors
        meshData.addQuad(vertices[0], vertices[1], vertices[2], vertices[3], normal, scaledUVs,
                        colors[0], colors[1], colors[2], colors[3]);
    }
    
    /**
     * Samples lighting at the four vertices of a quad.
     * Applies lighting, ambient occlusion, and biome tinting for grass/leaves.
     */
    private ColorRGBA[] sampleVertexLighting(Chunk chunk, Quad quad, int axis, int slice, boolean positive) {
        ColorRGBA[] colors = new ColorRGBA[4];
        
        // Get vertex positions in local chunk coordinates
        int[][] vertexCoords = getVertexCoords(quad, axis, slice, positive);
        
        // Determine face direction for AO sampling
        BlockFace face = getFaceForAxis(axis, positive);
        
        // Check if block needs biome tinting (grass=5, oak_leaves=19, birch_leaves=20)
        boolean needsBiomeTint = (quad.blockId == 5 || quad.blockId == 19 || quad.blockId == 20);
        ColorRGBA biomeTint = needsBiomeTint ? getBiomeTintForQuad(chunk, quad, axis, slice, positive) : ColorRGBA.White;
        
        for (int i = 0; i < 4; i++) {
            int x = vertexCoords[i][0];
            int y = vertexCoords[i][1];
            int z = vertexCoords[i][2];
            
            // Clamp to chunk bounds
            x = Math.max(0, Math.min(15, x));
            y = Math.max(0, Math.min(15, y));
            z = Math.max(0, Math.min(15, z));
            
            // Sample combined light (0-15)
            int lightLevel = chunk.getCombinedLight(x, y, z);
            
            // Convert to color (0-15 -> 0.0-1.0)
            float brightness = lightLevel / 15.0f;
            
            // Compute ambient occlusion for this vertex
            float ao = computeAOForVertex(chunk, face, x, y, z, i);
            
            // Apply AO to brightness
            brightness *= ao;
            
            // Apply minimum ambient light (reduced to let AO show)
            brightness = Math.max(0.05f, brightness);
            
            // Apply biome tint
            colors[i] = new ColorRGBA(
                brightness * biomeTint.r,
                brightness * biomeTint.g,
                brightness * biomeTint.b,
                1.0f
            );
        }
        
        return colors;
    }
    
    /**
     * Computes ambient occlusion factor for a vertex.
     * Samples 3 neighbors (2 edges + 1 corner) and returns AO multiplier.
     * 
     * @param chunk The chunk containing the block
     * @param face The face direction
     * @param vx Vertex X coordinate (clamped to chunk bounds)
     * @param vy Vertex Y coordinate (clamped to chunk bounds)
     * @param vz Vertex Z coordinate (clamped to chunk bounds)
     * @param cornerIndex Corner index (0-3) in quad winding order
     * @return AO factor (0.45-1.0)
     */
    private float computeAOForVertex(Chunk chunk, BlockFace face, int vx, int vy, int vz, int cornerIndex) {
        // Get the 3 sampling offsets for this face and corner
        int[][] offsets = getAOSamplingOffsets(face, cornerIndex);
        
        // Count solid neighbors
        int solidCount = 0;
        for (int[] offset : offsets) {
            int nx = vx + offset[0];
            int ny = vy + offset[1];
            int nz = vz + offset[2];
            
            // Sample block (returns 0/air if out of bounds)
            short blockId = chunk.getBlockSafe(nx, ny, nz);
            if (blockId != 0 && blockRegistry.getBlock(blockId).isSolid()) {
                solidCount++;
            }
        }
        
        // Convert solid count to AO factor
        // 0 solid = 1.0 (full brightness)
        // 1 solid = 0.8
        // 2 solid = 0.6
        // 3 solid = 0.45 (darkest)
        switch (solidCount) {
            case 0: return 1.0f;
            case 1: return 0.8f;
            case 2: return 0.6f;
            case 3: return 0.45f;
            default: return 1.0f;
        }
    }
    
    /**
     * Returns the 3 sampling offsets (2 edges + 1 corner) for AO calculation.
     * The offsets are relative to the vertex position and depend on the face and corner.
     */
    private int[][] getAOSamplingOffsets(BlockFace face, int cornerIndex) {
        // For each face, define the sampling pattern for each of the 4 corners
        // Pattern: [edge1, edge2, corner]
        switch (face) {
            case UP: // +Y face
                switch (cornerIndex) {
                    case 0: return new int[][]{{-1,0,0}, {0,0,-1}, {-1,0,-1}}; // back-left
                    case 1: return new int[][]{{1,0,0}, {0,0,-1}, {1,0,-1}};   // back-right
                    case 2: return new int[][]{{1,0,0}, {0,0,1}, {1,0,1}};     // front-right
                    case 3: return new int[][]{{-1,0,0}, {0,0,1}, {-1,0,1}};   // front-left
                }
                break;
            case DOWN: // -Y face
                switch (cornerIndex) {
                    case 0: return new int[][]{{-1,0,0}, {0,0,1}, {-1,0,1}};   // front-left
                    case 1: return new int[][]{{1,0,0}, {0,0,1}, {1,0,1}};     // front-right
                    case 2: return new int[][]{{1,0,0}, {0,0,-1}, {1,0,-1}};   // back-right
                    case 3: return new int[][]{{-1,0,0}, {0,0,-1}, {-1,0,-1}}; // back-left
                }
                break;
            case NORTH: // -Z face
                switch (cornerIndex) {
                    case 0: return new int[][]{{-1,0,0}, {0,-1,0}, {-1,-1,0}}; // bottom-left
                    case 1: return new int[][]{{1,0,0}, {0,-1,0}, {1,-1,0}};   // bottom-right
                    case 2: return new int[][]{{1,0,0}, {0,1,0}, {1,1,0}};     // top-right
                    case 3: return new int[][]{{-1,0,0}, {0,1,0}, {-1,1,0}};   // top-left
                }
                break;
            case SOUTH: // +Z face
                switch (cornerIndex) {
                    case 0: return new int[][]{{-1,0,0}, {0,-1,0}, {-1,-1,0}}; // bottom-left
                    case 1: return new int[][]{{1,0,0}, {0,-1,0}, {1,-1,0}};   // bottom-right
                    case 2: return new int[][]{{1,0,0}, {0,1,0}, {1,1,0}};     // top-right
                    case 3: return new int[][]{{-1,0,0}, {0,1,0}, {-1,1,0}};   // top-left
                }
                break;
            case EAST: // +X face
                switch (cornerIndex) {
                    case 0: return new int[][]{{0,-1,0}, {0,0,-1}, {0,-1,-1}}; // bottom-back
                    case 1: return new int[][]{{0,-1,0}, {0,0,1}, {0,-1,1}};   // bottom-front
                    case 2: return new int[][]{{0,1,0}, {0,0,1}, {0,1,1}};     // top-front
                    case 3: return new int[][]{{0,1,0}, {0,0,-1}, {0,1,-1}};   // top-back
                }
                break;
            case WEST: // -X face
                switch (cornerIndex) {
                    case 0: return new int[][]{{0,-1,0}, {0,0,1}, {0,-1,1}};   // bottom-front
                    case 1: return new int[][]{{0,-1,0}, {0,0,-1}, {0,-1,-1}}; // bottom-back
                    case 2: return new int[][]{{0,1,0}, {0,0,-1}, {0,1,-1}};   // top-back
                    case 3: return new int[][]{{0,1,0}, {0,0,1}, {0,1,1}};     // top-front
                }
                break;
        }
        
        // Fallback: no occlusion
        return new int[][]{{0,0,0}, {0,0,0}, {0,0,0}};
    }
    
    /**
     * Gets biome tint color for grass/leaves based on quad's world position.
     * Computes representative column position for the quad to avoid color seams.
     */
    private ColorRGBA getBiomeTintForQuad(Chunk chunk, Quad quad, int axis, int slice, boolean positive) {
        if (biomeProvider == null) {
            // Fallback if no biome provider
            if (quad.blockId == 5) {
                return new ColorRGBA(0.5f, 0.9f, 0.3f, 1.0f); // Grass
            } else if (quad.blockId == 19 || quad.blockId == 20) {
                return new ColorRGBA(0.3f, 0.7f, 0.2f, 1.0f); // Leaves
            }
            return ColorRGBA.White;
        }
        
        // Compute representative local coordinates for the quad's center
        int localX, localZ;
        
        switch (axis) {
            case 0: // X faces (axis=0)
                localX = slice + (positive ? 1 : 0);
                localZ = quad.x + quad.width / 2;
                break;
            case 1: // Y faces (axis=1)
                localX = quad.x + quad.width / 2;
                localZ = quad.y + quad.height / 2;
                break;
            case 2: // Z faces (axis=2)
                localX = quad.x + quad.width / 2;
                localZ = slice + (positive ? 1 : 0);
                break;
            default:
                localX = 8;
                localZ = 8;
        }
        
        // Clamp to chunk bounds and convert to world coordinates
        localX = Math.max(0, Math.min(15, localX));
        localZ = Math.max(0, Math.min(15, localZ));
        int worldX = chunk.getChunkX() * 16 + localX;
        int worldZ = chunk.getChunkZ() * 16 + localZ;
        
        // Query biome at this world position
        BiomeType biome = biomeProvider.getBiome(worldX, worldZ);
        
        // Return biome-specific tint
        if (quad.blockId == 5) {
            // Grass block
            return BIOME_GRASS_TINTS.getOrDefault(biome, new ColorRGBA(0.5f, 0.9f, 0.3f, 1.0f));
        } else if (quad.blockId == 19 || quad.blockId == 20) {
            // Leaves
            return BIOME_LEAF_TINTS.getOrDefault(biome, new ColorRGBA(0.3f, 0.7f, 0.2f, 1.0f));
        }
        
        return ColorRGBA.White;
    }
    
    /**
     * Gets the local chunk coordinates for the four vertices of a quad.
     */
    private int[][] getVertexCoords(Quad quad, int axis, int slice, boolean positive) {
        int[][] coords = new int[4][3];
        int offset = positive ? 1 : 0;
        
        switch (axis) {
            case 0: // X axis
                coords[0] = new int[]{slice + offset, quad.y, quad.x};
                coords[1] = new int[]{slice + offset, quad.y, quad.x + quad.width};
                coords[2] = new int[]{slice + offset, quad.y + quad.height, quad.x + quad.width};
                coords[3] = new int[]{slice + offset, quad.y + quad.height, quad.x};
                break;
            case 1: // Y axis
                coords[0] = new int[]{quad.x, slice + offset, quad.y};
                coords[1] = new int[]{quad.x + quad.width, slice + offset, quad.y};
                coords[2] = new int[]{quad.x + quad.width, slice + offset, quad.y + quad.height};
                coords[3] = new int[]{quad.x, slice + offset, quad.y + quad.height};
                break;
            case 2: // Z axis
                coords[0] = new int[]{quad.x, quad.y, slice + offset};
                coords[1] = new int[]{quad.x + quad.width, quad.y, slice + offset};
                coords[2] = new int[]{quad.x + quad.width, quad.y + quad.height, slice + offset};
                coords[3] = new int[]{quad.x, quad.y + quad.height, slice + offset};
                break;
        }
        
        return coords;
    }
    
    private BlockFace getFaceForAxis(int axis, boolean positive) {
        switch (axis) {
            case 0: return positive ? BlockFace.EAST : BlockFace.WEST;
            case 1: return positive ? BlockFace.UP : BlockFace.DOWN;
            case 2: return positive ? BlockFace.SOUTH : BlockFace.NORTH;
            default: return BlockFace.NORTH;
        }
    }
    
    private float[] scaleUVs(float[] uvs, int width, int height) {
        // For now, don't tile - just use base UVs
        // Future: scale UVs based on quad size for texture tiling
        return uvs;
    }
    
    private Vector3f[] computeVertices(Quad quad, int axis, int slice, boolean positive) {
        Vector3f[] vertices = new Vector3f[4];
        float offset = positive ? 1.0f : 0.0f;
        
        switch (axis) {
            case 0: // X axis
                vertices[0] = new Vector3f(slice + offset, quad.y, quad.x);
                vertices[1] = new Vector3f(slice + offset, quad.y, quad.x + quad.width);
                vertices[2] = new Vector3f(slice + offset, quad.y + quad.height, quad.x + quad.width);
                vertices[3] = new Vector3f(slice + offset, quad.y + quad.height, quad.x);
                break;
            case 1: // Y axis
                vertices[0] = new Vector3f(quad.x, slice + offset, quad.y);
                vertices[1] = new Vector3f(quad.x + quad.width, slice + offset, quad.y);
                vertices[2] = new Vector3f(quad.x + quad.width, slice + offset, quad.y + quad.height);
                vertices[3] = new Vector3f(quad.x, slice + offset, quad.y + quad.height);
                break;
            case 2: // Z axis
                vertices[0] = new Vector3f(quad.x, quad.y, slice + offset);
                vertices[1] = new Vector3f(quad.x + quad.width, quad.y, slice + offset);
                vertices[2] = new Vector3f(quad.x + quad.width, quad.y + quad.height, slice + offset);
                vertices[3] = new Vector3f(quad.x, quad.y + quad.height, slice + offset);
                break;
        }
        
        // Reverse winding order for negative faces
        if (!positive) {
            Vector3f temp = vertices[1];
            vertices[1] = vertices[3];
            vertices[3] = temp;
        }
        
        return vertices;
    }
    
    /**
     * Quad data structure for greedy meshing.
     */
    private static class Quad {
        final int x, y, width, height;
        final short blockId;
        
        Quad(int x, int y, int width, int height, short blockId) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.blockId = blockId;
        }
    }
}
