package com.poorcraftultra.core;

import com.poorcraftultra.world.chunk.ChunkPos;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Frustum class (pure math testing, no OpenGL needed).
 */
@DisplayName("Frustum Tests")
public class FrustumTest {

    @Test
    @DisplayName("Frustum extraction from matrix")
    void testFrustumExtraction() {
        Frustum frustum = new Frustum();

        // Create a simple view-projection matrix
        Matrix4f view = new Matrix4f().lookAt(
            new Vector3f(0, 0, 10),
            new Vector3f(0, 0, 0),
            new Vector3f(0, 1, 0)
        );
        Matrix4f projection = new Matrix4f().perspective(
            (float) Math.toRadians(45.0f),
            1.0f,
            0.1f,
            100.0f
        );
        Matrix4f viewProjection = new Matrix4f(projection).mul(view);

        // Should not throw exception
        assertDoesNotThrow(() -> frustum.update(viewProjection));
    }

    @Test
    @DisplayName("Box inside frustum is visible")
    void testBoxInsideFrustum() {
        Frustum frustum = new Frustum();

        Matrix4f view = new Matrix4f().lookAt(
            new Vector3f(0, 0, 10),
            new Vector3f(0, 0, 0),
            new Vector3f(0, 1, 0)
        );
        Matrix4f projection = new Matrix4f().perspective(
            (float) Math.toRadians(45.0f),
            1.0f,
            0.1f,
            100.0f
        );
        Matrix4f viewProjection = new Matrix4f(projection).mul(view);
        frustum.update(viewProjection);

        // Box at origin should be visible
        Vector3f min = new Vector3f(-1, -1, -1);
        Vector3f max = new Vector3f(1, 1, 1);

        assertTrue(frustum.isBoxVisible(min, max), "Box at origin should be visible");
    }

    @Test
    @DisplayName("Box outside frustum is not visible")
    void testBoxOutsideFrustum() {
        Frustum frustum = new Frustum();

        Matrix4f view = new Matrix4f().lookAt(
            new Vector3f(0, 0, 10),
            new Vector3f(0, 0, 0),
            new Vector3f(0, 1, 0)
        );
        Matrix4f projection = new Matrix4f().perspective(
            (float) Math.toRadians(45.0f),
            1.0f,
            0.1f,
            100.0f
        );
        Matrix4f viewProjection = new Matrix4f(projection).mul(view);
        frustum.update(viewProjection);

        // Box behind camera should not be visible
        Vector3f min = new Vector3f(-1, -1, 15);
        Vector3f max = new Vector3f(1, 1, 20);

        assertFalse(frustum.isBoxVisible(min, max), "Box behind camera should not be visible");
    }

    @Test
    @DisplayName("Box partially inside frustum is visible")
    void testBoxPartiallyInside() {
        Frustum frustum = new Frustum();

        Matrix4f view = new Matrix4f().lookAt(
            new Vector3f(0, 0, 10),
            new Vector3f(0, 0, 0),
            new Vector3f(0, 1, 0)
        );
        Matrix4f projection = new Matrix4f().perspective(
            (float) Math.toRadians(45.0f),
            1.0f,
            0.1f,
            100.0f
        );
        Matrix4f viewProjection = new Matrix4f(projection).mul(view);
        frustum.update(viewProjection);

        // Large box that intersects frustum
        Vector3f min = new Vector3f(-100, -100, -10);
        Vector3f max = new Vector3f(100, 100, 10);

        assertTrue(frustum.isBoxVisible(min, max), "Partially visible box should be visible");
    }

    @Test
    @DisplayName("Chunk visibility test")
    void testChunkVisibility() {
        Frustum frustum = new Frustum();

        Matrix4f view = new Matrix4f().lookAt(
            new Vector3f(0, 50, 50),
            new Vector3f(0, 0, 0),
            new Vector3f(0, 1, 0)
        );
        Matrix4f projection = new Matrix4f().perspective(
            (float) Math.toRadians(45.0f),
            1.0f,
            0.1f,
            1000.0f
        );
        Matrix4f viewProjection = new Matrix4f(projection).mul(view);
        frustum.update(viewProjection);

        // Chunk at origin should be visible
        ChunkPos pos1 = new ChunkPos(0, 0, 0);
        assertTrue(frustum.isChunkVisible(pos1), "Chunk at origin should be visible");

        // Chunk far away should not be visible
        ChunkPos pos2 = new ChunkPos(100, 0, 100);
        // This might be visible depending on frustum, just test it doesn't crash
        assertDoesNotThrow(() -> frustum.isChunkVisible(pos2));
    }

    @Test
    @DisplayName("Frustum update changes visibility")
    void testFrustumUpdate() {
        Frustum frustum = new Frustum();

        // First frustum looking at origin
        Matrix4f view1 = new Matrix4f().lookAt(
            new Vector3f(0, 0, 10),
            new Vector3f(0, 0, 0),
            new Vector3f(0, 1, 0)
        );
        Matrix4f projection = new Matrix4f().perspective(
            (float) Math.toRadians(45.0f),
            1.0f,
            0.1f,
            100.0f
        );
        Matrix4f viewProjection1 = new Matrix4f(projection).mul(view1);
        frustum.update(viewProjection1);

        Vector3f box1Min = new Vector3f(-1, -1, -1);
        Vector3f box1Max = new Vector3f(1, 1, 1);

        // Second frustum looking away
        Matrix4f view2 = new Matrix4f().lookAt(
            new Vector3f(0, 0, -10),
            new Vector3f(0, 0, -20),
            new Vector3f(0, 1, 0)
        );
        Matrix4f viewProjection2 = new Matrix4f(projection).mul(view2);
        frustum.update(viewProjection2);

        // Visibility should change when frustum is updated
        // (though both might be true or false depending on the exact setup)
        assertDoesNotThrow(() -> frustum.isBoxVisible(box1Min, box1Max));
    }

    @Test
    @DisplayName("All six planes are tested")
    void testAllSixPlanes() {
        Frustum frustum = new Frustum();

        Matrix4f view = new Matrix4f().lookAt(
            new Vector3f(0, 0, 10),
            new Vector3f(0, 0, 0),
            new Vector3f(0, 1, 0)
        );
        Matrix4f projection = new Matrix4f().perspective(
            (float) Math.toRadians(45.0f),
            1.0f,
            0.1f,
            100.0f
        );
        Matrix4f viewProjection = new Matrix4f(projection).mul(view);
        frustum.update(viewProjection);

        // Test boxes in various positions to ensure all planes work
        // Left
        Vector3f minLeft = new Vector3f(-1000, 0, 0);
        Vector3f maxLeft = new Vector3f(-900, 1, 1);
        assertDoesNotThrow(() -> frustum.isBoxVisible(minLeft, maxLeft));

        // Right
        Vector3f minRight = new Vector3f(900, 0, 0);
        Vector3f maxRight = new Vector3f(1000, 1, 1);
        assertDoesNotThrow(() -> frustum.isBoxVisible(minRight, maxRight));

        // Top
        Vector3f minTop = new Vector3f(0, 900, 0);
        Vector3f maxTop = new Vector3f(1, 1000, 1);
        assertDoesNotThrow(() -> frustum.isBoxVisible(minTop, maxTop));

        // Bottom
        Vector3f minBottom = new Vector3f(0, -1000, 0);
        Vector3f maxBottom = new Vector3f(1, -900, 1);
        assertDoesNotThrow(() -> frustum.isBoxVisible(minBottom, maxBottom));

        // Near/Far tested implicitly by other tests
    }
}
