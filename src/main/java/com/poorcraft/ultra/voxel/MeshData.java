package com.poorcraft.ultra.voxel;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Data class for mesh geometry (output of greedy mesher).
 */
public class MeshData {
    
    private final List<Float> positions = new ArrayList<>();
    private final List<Float> normals = new ArrayList<>();
    private final List<Float> texCoords = new ArrayList<>();
    private final List<Float> colors = new ArrayList<>();
    private final List<Integer> indices = new ArrayList<>();
    
    /**
     * Adds quad (2 triangles) to mesh with default white color.
     */
    public void addQuad(Vector3f v0, Vector3f v1, Vector3f v2, Vector3f v3, Vector3f normal, float[] uvs) {
        ColorRGBA white = ColorRGBA.White;
        addQuad(v0, v1, v2, v3, normal, uvs, white, white, white, white);
    }
    
    /**
     * Adds quad (2 triangles) to mesh with per-vertex colors.
     */
    public void addQuad(Vector3f v0, Vector3f v1, Vector3f v2, Vector3f v3, Vector3f normal, float[] uvs,
                        ColorRGBA c0, ColorRGBA c1, ColorRGBA c2, ColorRGBA c3) {
        int base = getVertexCount();
        
        // Add 4 vertices
        addVertex(v0, normal, uvs[0], uvs[1], c0);
        addVertex(v1, normal, uvs[2], uvs[3], c1);
        addVertex(v2, normal, uvs[4], uvs[5], c2);
        addVertex(v3, normal, uvs[6], uvs[7], c3);
        
        // Add 6 indices for 2 triangles
        indices.add(base);
        indices.add(base + 1);
        indices.add(base + 2);
        
        indices.add(base);
        indices.add(base + 2);
        indices.add(base + 3);
    }
    
    private void addVertex(Vector3f pos, Vector3f normal, float u, float v, ColorRGBA color) {
        positions.add(pos.x);
        positions.add(pos.y);
        positions.add(pos.z);
        
        normals.add(normal.x);
        normals.add(normal.y);
        normals.add(normal.z);
        
        texCoords.add(u);
        texCoords.add(v);
        
        colors.add(color.r);
        colors.add(color.g);
        colors.add(color.b);
        colors.add(color.a);
    }
    
    public void clear() {
        positions.clear();
        normals.clear();
        texCoords.clear();
        colors.clear();
        indices.clear();
    }
    
    public boolean isEmpty() {
        return positions.isEmpty();
    }
    
    public int getVertexCount() {
        return positions.size() / 3;
    }
    
    public int getTriangleCount() {
        return indices.size() / 3;
    }
    
    /**
     * Converts to jME Mesh.
     */
    public Mesh toJmeMesh() {
        Mesh mesh = new Mesh();
        
        // Convert lists to arrays
        float[] posArray = toFloatArray(positions);
        float[] normArray = toFloatArray(normals);
        float[] texArray = toFloatArray(texCoords);
        float[] colorArray = toFloatArray(colors);
        int[] indArray = toIntArray(indices);
        
        // Create buffers
        FloatBuffer posBuf = BufferUtils.createFloatBuffer(posArray);
        FloatBuffer normBuf = BufferUtils.createFloatBuffer(normArray);
        FloatBuffer texBuf = BufferUtils.createFloatBuffer(texArray);
        FloatBuffer colorBuf = BufferUtils.createFloatBuffer(colorArray);
        IntBuffer indBuf = BufferUtils.createIntBuffer(indArray);
        
        // Set buffers
        mesh.setBuffer(VertexBuffer.Type.Position, 3, posBuf);
        mesh.setBuffer(VertexBuffer.Type.Normal, 3, normBuf);
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, texBuf);
        mesh.setBuffer(VertexBuffer.Type.Color, 4, colorBuf);
        mesh.setBuffer(VertexBuffer.Type.Index, 1, indBuf);
        
        mesh.updateBound();
        mesh.setDynamic();
        
        return mesh;
    }
    
    private float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
    
    private int[] toIntArray(List<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
}
