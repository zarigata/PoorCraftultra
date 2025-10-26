package com.poorcraftultra.rendering;

import com.poorcraftultra.util.Constants;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/**
 * Renderer orchestrates the rendering pipeline.
 * Manages shaders, meshes, and draw calls.
 */
public class Renderer {

    private ShaderProgram shaderProgram;
    private Mesh cubeMesh;

    /**
     * Initializes the renderer.
     */
    public void init() {
        shaderProgram = new ShaderProgram(Constants.VERTEX_SHADER_PATH, Constants.FRAGMENT_SHADER_PATH);
        cubeMesh = createCubeMesh();

        // Enable depth testing
        GL11.glEnable(GL11.GL_DEPTH_TEST);

        // Set clear color
        GL11.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
    }

    /**
     * Creates a test cube mesh.
     * @return Cube mesh
     */
    private Mesh createCubeMesh() {
        // Cube vertices (position + color: x, y, z, r, g, b)
        float[] vertices = {
            // Front face (red)
            -0.5f, -0.5f,  0.5f, 1.0f, 0.0f, 0.0f,
             0.5f, -0.5f,  0.5f, 1.0f, 0.0f, 0.0f,
             0.5f,  0.5f,  0.5f, 1.0f, 0.0f, 0.0f,
            -0.5f,  0.5f,  0.5f, 1.0f, 0.0f, 0.0f,

            // Back face (green)
            -0.5f, -0.5f, -0.5f, 0.0f, 1.0f, 0.0f,
            -0.5f,  0.5f, -0.5f, 0.0f, 1.0f, 0.0f,
             0.5f,  0.5f, -0.5f, 0.0f, 1.0f, 0.0f,
             0.5f, -0.5f, -0.5f, 0.0f, 1.0f, 0.0f,

            // Left face (blue)
            -0.5f, -0.5f, -0.5f, 0.0f, 0.0f, 1.0f,
            -0.5f, -0.5f,  0.5f, 0.0f, 0.0f, 1.0f,
            -0.5f,  0.5f,  0.5f, 0.0f, 0.0f, 1.0f,
            -0.5f,  0.5f, -0.5f, 0.0f, 0.0f, 1.0f,

            // Right face (yellow)
             0.5f, -0.5f, -0.5f, 1.0f, 1.0f, 0.0f,
             0.5f,  0.5f, -0.5f, 1.0f, 1.0f, 0.0f,
             0.5f,  0.5f,  0.5f, 1.0f, 1.0f, 0.0f,
             0.5f, -0.5f,  0.5f, 1.0f, 1.0f, 0.0f,

            // Top face (cyan)
            -0.5f,  0.5f, -0.5f, 0.0f, 1.0f, 1.0f,
            -0.5f,  0.5f,  0.5f, 0.0f, 1.0f, 1.0f,
             0.5f,  0.5f,  0.5f, 0.0f, 1.0f, 1.0f,
             0.5f,  0.5f, -0.5f, 0.0f, 1.0f, 1.0f,

            // Bottom face (magenta)
            -0.5f, -0.5f, -0.5f, 1.0f, 0.0f, 1.0f,
             0.5f, -0.5f, -0.5f, 1.0f, 0.0f, 1.0f,
             0.5f, -0.5f,  0.5f, 1.0f, 0.0f, 1.0f,
            -0.5f, -0.5f,  0.5f, 1.0f, 0.0f, 1.0f
        };

        // Cube indices (6 faces × 2 triangles × 3 vertices)
        int[] indices = {
            // Front face
            0, 1, 2, 2, 3, 0,
            // Back face
            4, 5, 6, 6, 7, 4,
            // Left face
            8, 9, 10, 10, 11, 8,
            // Right face
            12, 13, 14, 14, 15, 12,
            // Top face
            16, 17, 18, 18, 19, 16,
            // Bottom face
            20, 21, 22, 22, 23, 20
        };

        return new Mesh(vertices, indices);
    }

    /**
     * Renders the scene.
     * @param camera Camera for view/projection matrices
     * @param aspect Aspect ratio for projection matrix
     */
    public void render(Camera camera, float aspect) {
        // Clear buffers
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        // Bind shader
        shaderProgram.bind();

        // Set uniforms
        shaderProgram.setUniform("uProjection", camera.getProjectionMatrix(aspect));
        shaderProgram.setUniform("uView", camera.getViewMatrix());
        shaderProgram.setUniform("uModel", new Matrix4f().translate(0, 0, -5)); // Position cube at z=-5

        // Bind and draw mesh
        cubeMesh.bind();
        GL11.glDrawElements(GL11.GL_TRIANGLES, cubeMesh.getVertexCount(), GL11.GL_UNSIGNED_INT, 0);
        cubeMesh.unbind();

        // Unbind shader
        shaderProgram.unbind();
    }

    /**
     * Cleans up renderer resources.
     */
    public void cleanup() {
        if (shaderProgram != null) {
            shaderProgram.cleanup();
        }
        if (cubeMesh != null) {
            cubeMesh.cleanup();
        }
    }

    /**
     * Gets the cube mesh index count (for testing).
     * @return Index count
     */
    int getCubeIndexCount() {
        return cubeMesh != null ? cubeMesh.getVertexCount() : 0;
    }
}
