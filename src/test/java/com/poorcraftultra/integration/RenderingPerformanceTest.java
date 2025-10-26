package com.poorcraftultra.integration;

import com.poorcraftultra.rendering.Camera;
import com.poorcraftultra.rendering.Renderer;
import org.junit.jupiter.api.*;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance test for rendering pipeline.
 * Verifies 60+ FPS requirement is met.
 */
@Tag("performance")
public class RenderingPerformanceTest {

    private static long window;

    @BeforeAll
    public static void initGLFW() {
        assertTrue(GLFW.glfwInit(), "Failed to initialize GLFW");

        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        window = GLFW.glfwCreateWindow(800, 600, "Performance Test", 0, 0);
        assertNotEquals(0, window, "Failed to create GLFW window");

        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(0); // Disable vsync for accurate FPS measurement
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
    public void testRenderingPerformance() {
        // Initialize minimal rendering setup
        Renderer renderer = new Renderer();
        renderer.init();

        Camera camera = new Camera();

        // Run render loop for 5 seconds
        int frames = 0;
        double[] frameTimes = new double[10000]; // Sufficient for 5s at high FPS

        double startTime = GLFW.glfwGetTime();

        while (GLFW.glfwGetTime() - startTime < 5.0) {
            double frameStart = GLFW.glfwGetTime();

            // Render frame
            renderer.render(camera, 800f / 600f);

            // Ensure rendering is complete
            GL11.glFinish();

            // Swap buffers (simulate window update)
            GLFW.glfwSwapBuffers(window);

            // Poll events
            GLFW.glfwPollEvents();

            double frameEnd = GLFW.glfwGetTime();
            frameTimes[frames] = frameEnd - frameStart;
            frames++;
        }

        double endTime = GLFW.glfwGetTime();
        double elapsedSeconds = endTime - startTime;

        // Calculate metrics
        double averageFps = frames / elapsedSeconds;

        double minFrameTime = Double.MAX_VALUE;
        double maxFrameTime = Double.MIN_VALUE;
        for (int i = 0; i < frames; i++) {
            minFrameTime = Math.min(minFrameTime, frameTimes[i]);
            maxFrameTime = Math.max(maxFrameTime, frameTimes[i]);
        }
        double minFps = 1.0 / maxFrameTime; // Slowest frame
        double maxFps = 1.0 / minFrameTime; // Fastest frame

        // Log performance metrics
        System.out.printf("Rendering Performance Test Results:%n");
        System.out.printf("Total frames: %d%n", frames);
        System.out.printf("Total time: %.2f seconds%n", elapsedSeconds);
        System.out.printf("Average FPS: %.2f%n", averageFps);
        System.out.printf("Minimum FPS: %.2f%n", minFps);
        System.out.printf("Maximum FPS: %.2f%n", maxFps);

        // Assertions
        assertTrue(averageFps >= 60.0, "Average FPS should be at least 60, got: " + averageFps);
        assertTrue(minFps >= 30.0, "Minimum FPS should be at least 30 to avoid frame drops, got: " + minFps);

        renderer.cleanup();
    }
}
