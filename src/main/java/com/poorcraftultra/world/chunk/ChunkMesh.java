package com.poorcraftultra.world.chunk;

import static org.lwjgl.opengl.GL30.*;

/**
 * Manages mesh data and GPU resources for a single chunk.
 * 
 * <p>Vertex format: Interleaved position (XYZ), color (RGB), and texture coordinates (UV), 8 floats per vertex.
 * Each vertex contains:
 * <ul>
 *   <li>Position: vec3 (x, y, z) - world space coordinates</li>
 *   <li>Color: vec3 (r, g, b) - RGB color values [0.0, 1.0] for lighting/shading</li>
 *   <li>TexCoord: vec2 (u, v) - texture atlas UV coordinates [0.0, 1.0]</li>
 * </ul>
 * 
 * <p>This class encapsulates all OpenGL state (VAO, VBO, EBO) for one chunk's geometry,
 * following the same pattern as the existing Renderer class but designed for reuse
 * across many chunks.
 */
public class ChunkMesh {
    private final float[] vertices;
    private final int[] indices;
    private final int vertexCount;
    private final int indexCount;
    
    private int vaoId;
    private int vboId;
    private int eboId;
    private boolean uploaded;
    
    /**
     * Creates a new chunk mesh with the given vertex and index data.
     * 
     * @param vertices vertex data (position XYZ + color RGB + texCoord UV, 8 floats per vertex)
     * @param indices triangle indices
     */
    public ChunkMesh(float[] vertices, int[] indices) {
        this.vertices = vertices;
        this.indices = indices;
        this.vertexCount = vertices.length / 8; // 8 floats per vertex
        this.indexCount = indices.length;
        this.uploaded = false;
    }
    
    /**
     * Uploads the mesh data to the GPU.
     * Generates VAO, VBO, and EBO, and configures vertex attributes.
     */
    public void upload() {
        if (uploaded) {
            cleanup(); // Cleanup old buffers if re-uploading
        }
        
        // Generate buffers
        vaoId = glGenVertexArrays();
        vboId = glGenBuffers();
        eboId = glGenBuffers();
        
        // Bind VAO
        glBindVertexArray(vaoId);
        
        // Upload vertex data
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
        
        // Upload index data
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
        
        // Configure vertex attributes
        int stride = 8 * Float.BYTES;
        
        // Location 0: position (vec3)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        
        // Location 1: color (vec3)
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        
        // Location 2: texCoord (vec2)
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6 * Float.BYTES);
        glEnableVertexAttribArray(2);
        
        // Unbind (unbind array buffer before VAO, leave EBO bound to VAO)
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        
        uploaded = true;
    }
    
    /**
     * Renders the mesh using glDrawElements.
     */
    public void render() {
        if (!uploaded) {
            return;
        }
        
        glBindVertexArray(vaoId);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }
    
    /**
     * Deletes GPU resources (VAO, VBO, EBO).
     */
    public void cleanup() {
        if (uploaded) {
            glDeleteVertexArrays(vaoId);
            glDeleteBuffers(vboId);
            glDeleteBuffers(eboId);
            vaoId = 0;
            vboId = 0;
            eboId = 0;
            uploaded = false;
        }
    }
    
    /**
     * @return true if the mesh has no vertices
     */
    public boolean isEmpty() {
        return vertexCount == 0;
    }
    
    /**
     * @return the number of vertices in the mesh
     */
    public int getVertexCount() {
        return vertexCount;
    }
    
    /**
     * @return the number of indices in the mesh
     */
    public int getIndexCount() {
        return indexCount;
    }
}
