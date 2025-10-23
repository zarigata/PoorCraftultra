package com.poorcraftultra.core;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Handles OpenGL rendering operations.
 */
public class Renderer {
    private int vaoId;
    private int vboId;
    private int eboId;
    private Shader shader;
    private int indexCount;

    // Cube vertex data: position (x, y, z) and color (r, g, b)
    private static final float[] CUBE_VERTICES = {
            // Front face (red)
            -0.5f, -0.5f,  0.5f,  1.0f, 0.0f, 0.0f,
             0.5f, -0.5f,  0.5f,  1.0f, 0.0f, 0.0f,
             0.5f,  0.5f,  0.5f,  1.0f, 0.0f, 0.0f,
            -0.5f,  0.5f,  0.5f,  1.0f, 0.0f, 0.0f,

            // Back face (green)
            -0.5f, -0.5f, -0.5f,  0.0f, 1.0f, 0.0f,
             0.5f, -0.5f, -0.5f,  0.0f, 1.0f, 0.0f,
             0.5f,  0.5f, -0.5f,  0.0f, 1.0f, 0.0f,
            -0.5f,  0.5f, -0.5f,  0.0f, 1.0f, 0.0f,

            // Top face (blue)
            -0.5f,  0.5f, -0.5f,  0.0f, 0.0f, 1.0f,
            -0.5f,  0.5f,  0.5f,  0.0f, 0.0f, 1.0f,
             0.5f,  0.5f,  0.5f,  0.0f, 0.0f, 1.0f,
             0.5f,  0.5f, -0.5f,  0.0f, 0.0f, 1.0f,

            // Bottom face (yellow)
            -0.5f, -0.5f, -0.5f,  1.0f, 1.0f, 0.0f,
            -0.5f, -0.5f,  0.5f,  1.0f, 1.0f, 0.0f,
             0.5f, -0.5f,  0.5f,  1.0f, 1.0f, 0.0f,
             0.5f, -0.5f, -0.5f,  1.0f, 1.0f, 0.0f,

            // Right face (magenta)
             0.5f, -0.5f, -0.5f,  1.0f, 0.0f, 1.0f,
             0.5f,  0.5f, -0.5f,  1.0f, 0.0f, 1.0f,
             0.5f,  0.5f,  0.5f,  1.0f, 0.0f, 1.0f,
             0.5f, -0.5f,  0.5f,  1.0f, 0.0f, 1.0f,

            // Left face (cyan)
            -0.5f, -0.5f, -0.5f,  0.0f, 1.0f, 1.0f,
            -0.5f,  0.5f, -0.5f,  0.0f, 1.0f, 1.0f,
            -0.5f,  0.5f,  0.5f,  0.0f, 1.0f, 1.0f,
            -0.5f, -0.5f,  0.5f,  0.0f, 1.0f, 1.0f
    };

    // Cube indices (2 triangles per face)
    private static final int[] CUBE_INDICES = {
            // Front
            0, 1, 2, 2, 3, 0,
            // Back
            4, 5, 6, 6, 7, 4,
            // Top
            8, 9, 10, 10, 11, 8,
            // Bottom
            12, 13, 14, 14, 15, 12,
            // Right
            16, 17, 18, 18, 19, 16,
            // Left
            20, 21, 22, 22, 23, 20
    };

    private float rotation = 0.0f;

    /**
     * Creates a new Renderer instance.
     */
    public Renderer() {
        this.shader = new Shader("shaders/vertex.glsl", "shaders/fragment.glsl");
    }

    /**
     * Initializes the renderer and sets up OpenGL resources.
     */
    public void init() {
        // Load shader
        shader.load();

        // Enable depth testing
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);

        // Set clear color (sky blue)
        glClearColor(0.53f, 0.81f, 0.92f, 1.0f);

        // Create and bind VAO
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        // Create and bind VBO
        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);

        // Upload vertex data
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer vertexBuffer = stack.mallocFloat(CUBE_VERTICES.length);
            vertexBuffer.put(CUBE_VERTICES).flip();
            glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
        }

        // Configure vertex attributes
        // Position attribute (location = 0)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 6 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        // Color attribute (location = 1)
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        // Create and bind EBO
        eboId = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);

        // Upload index data
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer indexBuffer = stack.mallocInt(CUBE_INDICES.length);
            indexBuffer.put(CUBE_INDICES).flip();
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
        }

        indexCount = CUBE_INDICES.length;

        // Unbind VAO
        glBindVertexArray(0);

        System.out.println("Renderer initialized");
    }

    /**
     * Renders the scene.
     *
     * @param framebufferWidth  The framebuffer width for aspect ratio calculation
     * @param framebufferHeight The framebuffer height for aspect ratio calculation
     */
    public void render(int framebufferWidth, int framebufferHeight) {
        // Clear buffers
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Bind shader
        shader.use();

        // Update rotation
        rotation += 0.5f;
        if (rotation > 360.0f) {
            rotation -= 360.0f;
        }

        // Create transformation matrices
        Matrix4f model = new Matrix4f()
                .identity()
                .rotateY((float) Math.toRadians(rotation))
                .rotateX((float) Math.toRadians(rotation * 0.5f));

        Matrix4f view = new Matrix4f()
                .identity()
                .lookAt(new Vector3f(0.0f, 0.0f, 3.0f),
                        new Vector3f(0.0f, 0.0f, 0.0f),
                        new Vector3f(0.0f, 1.0f, 0.0f));

        float aspectRatio = (float) framebufferWidth / (float) framebufferHeight;
        Matrix4f projection = new Matrix4f()
                .identity()
                .perspective((float) Math.toRadians(45.0f), aspectRatio, 0.1f, 100.0f);

        // Set uniforms
        shader.setUniform("model", model);
        shader.setUniform("view", view);
        shader.setUniform("projection", projection);

        // Bind VAO and draw
        glBindVertexArray(vaoId);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);

        // Unbind shader
        shader.unbind();
    }

    /**
     * Cleans up renderer resources.
     */
    public void cleanup() {
        // Unbind state before deletion
        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        
        // Delete buffers with validity checks
        if (glIsBuffer(vboId)) {
            glDeleteBuffers(vboId);
        }
        vboId = 0;
        
        if (glIsBuffer(eboId)) {
            glDeleteBuffers(eboId);
        }
        eboId = 0;
        
        if (glIsVertexArray(vaoId)) {
            glDeleteVertexArrays(vaoId);
        }
        vaoId = 0;

        // Cleanup shader
        shader.cleanup();

        System.out.println("Renderer cleaned up");
    }

    /**
     * Gets the VAO ID (for testing purposes).
     *
     * @return The VAO ID
     */
    public int getVaoId() {
        return vaoId;
    }

    /**
     * Gets the VBO ID (for testing purposes).
     *
     * @return The VBO ID
     */
    public int getVboId() {
        return vboId;
    }

    /**
     * Gets the EBO ID (for testing purposes).
     *
     * @return The EBO ID
     */
    public int getEboId() {
        return eboId;
    }

    /**
     * Gets the shader instance (for testing purposes).
     *
     * @return The shader
     */
    public Shader getShader() {
        return shader;
    }
}
