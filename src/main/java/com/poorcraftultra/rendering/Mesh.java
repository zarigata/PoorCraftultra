package com.poorcraftultra.rendering;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Mesh manages vertex data and OpenGL buffer objects.
 * Handles VAO, VBO, and EBO for rendering 3D geometry.
 */
public class Mesh {

    private final int vaoId;
    private final int vboId;
    private final int eboId;
    private final int vertexCount;

    /**
     * Creates a mesh from vertex and index data.
     * @param vertices Interleaved vertex data (position + color: x, y, z, r, g, b per vertex)
     * @param indices Triangle indices for indexed drawing
     */
    public Mesh(float[] vertices, int[] indices) {
        vertexCount = indices.length;

        vaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoId);

        // Create VBO
        vboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer vertexBuffer = stack.mallocFloat(vertices.length);
            vertexBuffer.put(vertices).flip();
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STATIC_DRAW);
        }

        // Create EBO
        eboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eboId);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer indexBuffer = stack.mallocInt(indices.length);
            indexBuffer.put(indices).flip();
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL15.GL_STATIC_DRAW);
        }

        // Set vertex attribute pointers
        // Position attribute (location 0)
        GL20.glVertexAttribPointer(0, 3, GL20.GL_FLOAT, false, 6 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        // Color attribute (location 1)
        GL20.glVertexAttribPointer(1, 3, GL20.GL_FLOAT, false, 6 * Float.BYTES, 3L * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);

        // Unbind VAO
        GL30.glBindVertexArray(0);
    }

    /**
     * Binds this mesh for rendering.
     */
    public void bind() {
        GL30.glBindVertexArray(vaoId);
    }

    /**
     * Unbinds the current mesh.
     */
    public void unbind() {
        GL30.glBindVertexArray(0);
    }

    /**
     * Gets the number of vertices in this mesh.
     * @return Vertex count
     */
    public int getVertexCount() {
        return vertexCount;
    }

    /**
     * Cleans up OpenGL resources.
     */
    public void cleanup() {
        GL15.glDeleteBuffers(vboId);
        GL15.glDeleteBuffers(eboId);
        GL30.glDeleteVertexArrays(vaoId);
    }
}
