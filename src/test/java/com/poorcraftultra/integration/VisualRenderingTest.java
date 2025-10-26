package com.poorcraftultra.integration;

import com.poorcraftultra.rendering.Camera;
import com.poorcraftultra.rendering.Renderer;
import org.junit.jupiter.api.*;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Visual test for rendering pipeline.
 * Verifies cube renders correctly without artifacts.
 */
@Tag("visual")
public class VisualRenderingTest {

    private static long window;

    @BeforeAll
    public static void initGLFW() {
        assertTrue(GLFW.glfwInit(), "Failed to initialize GLFW");

        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE); // Can be changed to true for manual inspection
        window = GLFW.glfwCreateWindow(800, 600, "Visual Test", 0, 0);
        assertNotEquals(0, window, "Failed to create GLFW window");

        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();
    }

    @AfterAll
    public static void cleanupGLFW() {
        if (window != 0) {
            GLFW.glfwDestroyWindow(window);
        }
        GLFW.glfwTerminate();
    }

    @Test
    public void testVisualRendering() throws InterruptedException {
        // Initialize renderer
        Renderer renderer = new Renderer();
        renderer.init();

        Camera camera = new Camera();

        // Render for 3 seconds to allow visual inspection
        double startTime = GLFW.glfwGetTime();
        double testDuration = 3.0; // seconds

        while (GLFW.glfwGetTime() - startTime < testDuration) {
            // Render frame
            renderer.render(camera, 800f / 600f);
            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();

            // Small delay to avoid consuming too much CPU
            Thread.sleep(16); // ~60 FPS
        }

        // Automated checks: read pixels to verify cube is rendered
        // Note: This is a basic check; for thorough testing, consider pixel-perfect comparisons
        IntBuffer width = IntBuffer.allocate(1);
        IntBuffer height = IntBuffer.allocate(1);
        GLFW.glfwGetFramebufferSize(window, width, height);

        int centerX = width.get(0) / 2;
        int centerY = height.get(0) / 2;
        int sampleSize = 10; // Sample 10x10 region around center

        ByteBuffer pixels = ByteBuffer.allocateDirect(sampleSize * sampleSize * 4);
        GL11.glReadPixels(centerX - sampleSize / 2, centerY - sampleSize / 2, sampleSize, sampleSize,
                          GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);

        // Check that pixels in the region differ from clear color with tolerance
        boolean hasNonClearColorPixels = false;
        float tolerance = 5.0f; // Allow small rounding differences
        pixels.rewind();
        while (pixels.hasRemaining()) {
            byte r = pixels.get();
            byte g = pixels.get();
            byte b = pixels.get();
            byte a = pixels.get();

            // Clear color is (0.1, 0.1, 0.1, 1.0) * 255 ≈ (26, 26, 26, 255)
            float clearR = 0.1f * 255;
            float clearG = 0.1f * 255;
            float clearB = 0.1f * 255;
            float clearA = 1.0f * 255;

            if (Math.abs((r & 0xFF) - clearR) > tolerance ||
                Math.abs((g & 0xFF) - clearG) > tolerance ||
                Math.abs((b & 0xFF) - clearB) > tolerance ||
                Math.abs((a & 0xFF) - clearA) > tolerance) {
                hasNonClearColorPixels = true;
                break;
            }
        }

        assertTrue(hasNonClearColorPixels, "Sampled region around screen center should contain pixels that differ from the clear color (cube should be visible)");

        renderer.cleanup();
    }
}
