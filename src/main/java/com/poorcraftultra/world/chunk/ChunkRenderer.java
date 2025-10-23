package com.poorcraftultra.world.chunk;

import com.poorcraftultra.core.Frustum;
import com.poorcraftultra.core.Shader;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages rendering of multiple chunks with frustum culling and mesh caching.
 * 
 * <p>This class provides the main interface between the game loop and chunk rendering.
 * It handles:
 * <ul>
 *   <li>Mesh generation and caching (meshes are generated once and reused)</li>
 *   <li>Frustum culling (skips chunks outside camera view)</li>
 *   <li>Rendering pipeline coordination</li>
 *   <li>Performance statistics tracking</li>
 * </ul>
 * 
 * <p>Meshes are cached until chunks are modified, at which point they can be
 * invalidated and regenerated.
 */
public class ChunkRenderer {
    private final ChunkManager chunkManager;
    private final ChunkMesher mesher;
    private final Map<ChunkPos, ChunkMesh> meshCache;
    private final Shader shader;
    private final Frustum frustum;
    
    private int renderedChunkCount;
    private int culledChunkCount;
    
    /**
     * Creates a new chunk renderer.
     * 
     * @param chunkManager reference to the chunk manager
     * @param shader the shader program to use for rendering
     */
    public ChunkRenderer(ChunkManager chunkManager, Shader shader) {
        this.chunkManager = chunkManager;
        this.shader = shader;
        this.mesher = new ChunkMesher(chunkManager);
        this.meshCache = new HashMap<>();
        this.frustum = new Frustum();
        this.renderedChunkCount = 0;
        this.culledChunkCount = 0;
    }
    
    /**
     * Initializes the renderer (reserved for future use).
     */
    public void init() {
        // Reserved for future initialization
    }
    
    /**
     * Renders all visible chunks.
     * 
     * @param view the view matrix
     * @param projection the projection matrix
     */
    public void render(Matrix4f view, Matrix4f projection) {
        // Combine view and projection
        Matrix4f viewProjection = new Matrix4f(projection).mul(view);
        
        // Update frustum
        frustum.update(viewProjection);
        
        // Bind shader
        shader.use();
        
        // Set view and projection uniforms
        shader.setUniform("view", view);
        shader.setUniform("projection", projection);
        
        // Reset statistics
        renderedChunkCount = 0;
        culledChunkCount = 0;
        
        // Iterate through all loaded chunks
        for (Chunk chunk : chunkManager.getLoadedChunks()) {
            ChunkPos pos = chunk.getPosition();
            
            // Frustum culling
            if (!frustum.isChunkVisible(pos)) {
                culledChunkCount++;
                continue;
            }
            
            // Get or generate mesh
            ChunkMesh mesh = getOrCreateMesh(chunk);
            
            // Skip empty meshes
            if (mesh.isEmpty()) {
                continue;
            }
            
            // Set model matrix (translation to chunk world position)
            Matrix4f model = new Matrix4f().identity();
            // Chunk position is already baked into mesh vertices, so model is identity
            shader.setUniform("model", model);
            
            // Render mesh
            mesh.render();
            renderedChunkCount++;
        }
        
        // Unbind shader
        shader.unbind();
    }
    
    /**
     * Gets a mesh from cache or generates it if not present.
     * 
     * @param chunk the chunk to get/generate mesh for
     * @return the chunk mesh
     */
    private ChunkMesh getOrCreateMesh(Chunk chunk) {
        ChunkPos pos = chunk.getPosition();
        
        // Check cache
        ChunkMesh mesh = meshCache.get(pos);
        if (mesh != null) {
            return mesh;
        }
        
        // Generate new mesh
        mesh = mesher.generateMesh(chunk);
        
        // Only upload non-empty meshes to GPU
        if (!mesh.isEmpty()) {
            mesh.upload();
        }
        
        // Store in cache
        meshCache.put(pos, mesh);
        
        return mesh;
    }
    
    /**
     * Invalidates the mesh for a specific chunk position.
     * Used when a chunk is modified and needs to be re-meshed.
     * 
     * @param pos the chunk position
     */
    public void invalidateMesh(ChunkPos pos) {
        ChunkMesh mesh = meshCache.remove(pos);
        if (mesh != null) {
            mesh.cleanup();
        }
    }
    
    /**
     * Invalidates all cached meshes.
     */
    public void invalidateAllMeshes() {
        for (ChunkMesh mesh : meshCache.values()) {
            mesh.cleanup();
        }
        meshCache.clear();
    }
    
    /**
     * Cleans up all GPU resources.
     */
    public void cleanup() {
        invalidateAllMeshes();
    }
    
    /**
     * @return the number of chunks rendered in the last frame
     */
    public int getRenderedChunkCount() {
        return renderedChunkCount;
    }
    
    /**
     * @return the number of chunks culled in the last frame
     */
    public int getCulledChunkCount() {
        return culledChunkCount;
    }
}
