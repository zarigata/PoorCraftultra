package com.poorcraft.ultra.voxel;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.renderer.queue.RenderQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.poorcraft.ultra.voxel.ChunkConstants.*;

/**
 * Converts MeshData to jME Geometry.
 */
public class ChunkRenderer {
    
    private static final Logger logger = LoggerFactory.getLogger(ChunkRenderer.class);
    
    private final AssetManager assetManager;
    private final TextureAtlas textureAtlas;
    private Material chunkMaterial;
    
    public ChunkRenderer(AssetManager assetManager, TextureAtlas textureAtlas) {
        this.assetManager = assetManager;
        this.textureAtlas = textureAtlas;
    }
    
    /**
     * Initializes shared material.
     */
    public void init() {
        chunkMaterial = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        chunkMaterial.setTexture("DiffuseMap", textureAtlas.getAtlasTexture());
        chunkMaterial.setBoolean("UseMaterialColors", false);
        logger.info("ChunkRenderer initialized");
    }
    
    /**
     * Creates jME Geometry from chunk mesh data.
     */
    public Geometry createChunkGeometry(Chunk chunk, MeshData meshData) {
        if (meshData.isEmpty()) {
            return null;
        }
        
        Mesh mesh = meshData.toJmeMesh();
        
        String name = "Chunk_" + chunk.getChunkX() + "_" + chunk.getChunkZ();
        Geometry geometry = new Geometry(name, mesh);
        geometry.setMaterial(chunkMaterial);
        
        // Set world position
        float worldX = chunk.getChunkX() * CHUNK_SIZE_X * BLOCK_SIZE;
        float worldZ = chunk.getChunkZ() * CHUNK_SIZE_Z * BLOCK_SIZE;
        geometry.setLocalTranslation(worldX, 0, worldZ);
        
        // Enable frustum culling
        geometry.setCullHint(Spatial.CullHint.Dynamic);
        
        // Enable shadows (for future lighting)
        geometry.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        
        // Store chunk reference
        geometry.setUserData("chunk", chunk);
        
        logger.debug("Created geometry for chunk ({}, {}): {} vertices", 
            chunk.getChunkX(), chunk.getChunkZ(), meshData.getVertexCount());
        
        return geometry;
    }
    
    /**
     * Updates existing geometry with new mesh data.
     * Returns true if geometry was removed (became empty).
     */
    public boolean updateChunkGeometry(Geometry geometry, MeshData meshData) {
        if (meshData.isEmpty()) {
            // Remove geometry if mesh is now empty
            if (geometry.getParent() != null) {
                geometry.removeFromParent();
            }
            return true; // Indicate removal
        }
        
        Mesh newMesh = meshData.toJmeMesh();
        geometry.setMesh(newMesh);
        geometry.updateModelBound();
        
        logger.debug("Updated geometry: {} vertices", meshData.getVertexCount());
        return false; // Not removed
    }
}
