package com.poorcraftultra;

import com.poorcraftultra.core.Renderer;
import com.poorcraftultra.core.Window;
import com.poorcraftultra.world.chunk.ChunkManager;
import com.poorcraftultra.world.chunk.ChunkRenderer;
import com.poorcraftultra.world.chunk.ChunkPos;
import com.poorcraftultra.world.chunk.Chunk;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Main entry point for PoorCraftUltra.
 */
public class Main {
    private static final int WINDOW_WIDTH = 1280;
    private static final int WINDOW_HEIGHT = 720;
    private static final String WINDOW_TITLE = "PoorCraftUltra - Phase 3: Chunk Rendering";

    public static void main(String[] args) {
        System.out.println("Starting PoorCraftUltra...");

        Window window = null;
        Renderer renderer = null;
        ChunkManager chunkManager = null;
        ChunkRenderer chunkRenderer = null;

        try {
            // Initialize GLFW
            if (!glfwInit()) {
                throw new RuntimeException("Failed to initialize GLFW");
            }
            System.out.println("GLFW initialized");

            // Create and initialize window
            window = new Window(WINDOW_WIDTH, WINDOW_HEIGHT, WINDOW_TITLE);
            window.init();

            // Create and initialize renderer
            renderer = new Renderer();
            renderer.init();

            // Create chunk manager and chunk renderer
            chunkManager = new ChunkManager();
            chunkRenderer = new ChunkRenderer(chunkManager, renderer.getShader());
            chunkRenderer.init();

            // Generate test world: 5x5x2 grid of chunks (50 chunks total)
            System.out.println("Generating test world...");
            for (int cx = -2; cx <= 2; cx++) {
                for (int cz = -2; cz <= 2; cz++) {
                    for (int cy = 0; cy < 2; cy++) {
                        ChunkPos pos = new ChunkPos(cx, cy, cz);
                        Chunk chunk = chunkManager.loadChunk(pos);
                        
                        // Fill with test blocks
                        for (int x = 0; x < 16; x++) {
                            for (int z = 0; z < 16; z++) {
                                for (int y = 0; y < 16; y++) {
                                    int worldY = cy * 16 + y;
                                    
                                    // Bottom layer (y=0-15): solid blocks
                                    if (worldY < 16) {
                                        // Checkerboard pattern for visual interest
                                        if ((x + z) % 2 == 0) {
                                            chunk.setBlock(x, y, z, (byte) 1);
                                        }
                                    }
                                    // Create some variation at y=16-31
                                    else if (worldY < 32 && (x % 4 == 0 && z % 4 == 0)) {
                                        chunk.setBlock(x, y, z, (byte) 1);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            System.out.println("Test world generated: " + chunkManager.getLoadedChunkCount() + " chunks");

            // Camera parameters
            float cameraDistance = 50.0f;
            float cameraHeight = 30.0f;
            float cameraRotation = 0.0f;

            // Main game loop
            System.out.println("Entering main loop...");
            int frameCount = 0;
            while (!window.shouldClose()) {
                // Poll for window events
                glfwPollEvents();

                // Update camera rotation
                cameraRotation += 0.3f;
                if (cameraRotation > 360.0f) {
                    cameraRotation -= 360.0f;
                }

                // Create transformation matrices
                float radians = (float) Math.toRadians(cameraRotation);
                Vector3f cameraPos = new Vector3f(
                    (float) Math.sin(radians) * cameraDistance,
                    cameraHeight,
                    (float) Math.cos(radians) * cameraDistance
                );

                Matrix4f view = new Matrix4f()
                    .identity()
                    .lookAt(cameraPos,
                            new Vector3f(0.0f, 10.0f, 0.0f),
                            new Vector3f(0.0f, 1.0f, 0.0f));

                float aspectRatio = (float) window.getFramebufferWidth() / (float) window.getFramebufferHeight();
                Matrix4f projection = new Matrix4f()
                    .identity()
                    .perspective((float) Math.toRadians(45.0f), aspectRatio, 0.1f, 1000.0f);

                // Render chunks
                chunkRenderer.render(view, projection);

                // Swap buffers
                window.swapBuffers();

                // Display statistics every 60 frames
                frameCount++;
                if (frameCount % 60 == 0) {
                    System.out.println("Rendered: " + chunkRenderer.getRenderedChunkCount() + 
                                     " chunks, Culled: " + chunkRenderer.getCulledChunkCount() + " chunks");
                }
            }

            System.out.println("Exiting main loop...");

        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);

        } finally {
            // Cleanup
            if (chunkRenderer != null) {
                chunkRenderer.cleanup();
            }

            if (renderer != null) {
                renderer.cleanup();
            }

            if (window != null) {
                window.destroy();
            }

            glfwTerminate();
            System.out.println("GLFW terminated");
            System.out.println("PoorCraftUltra shutdown complete");
        }
    }
}
