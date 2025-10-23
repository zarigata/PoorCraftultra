package com.poorcraftultra.core;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import com.poorcraftultra.world.chunk.ChunkPos;

/**
 * Implements frustum culling for visibility optimization.
 * 
 * <p>Frustum culling eliminates chunks outside the camera view, typically reducing
 * rendered chunks by 50-70%. Uses plane extraction from view-projection matrix
 * (Gribb-Hartmann method) and AABB intersection tests.
 * 
 * <p>The frustum is defined by 6 planes: left, right, bottom, top, near, and far.
 * Each plane is represented by a normal vector and a distance from the origin.
 */
public class Frustum {
    /**
     * Represents a plane in 3D space.
     */
    private static class Plane {
        Vector3f normal;
        float distance;
        
        Plane() {
            this.normal = new Vector3f();
            this.distance = 0;
        }
        
        /**
         * Calculates the signed distance from a point to this plane.
         * 
         * @param point the point to test
         * @return signed distance (positive = in front, negative = behind)
         */
        float distanceToPoint(Vector3f point) {
            return normal.dot(point) + distance;
        }
        
        /**
         * Normalizes the plane equation.
         */
        void normalize() {
            float length = normal.length();
            if (length != 0f) {
                normal.div(length);
                distance /= length;
            }
        }
    }
    
    private final Plane[] planes;
    
    // Plane indices
    private static final int LEFT = 0;
    private static final int RIGHT = 1;
    private static final int BOTTOM = 2;
    private static final int TOP = 3;
    private static final int NEAR = 4;
    private static final int FAR = 5;
    
    /**
     * Creates a new frustum with uninitialized planes.
     */
    public Frustum() {
        planes = new Plane[6];
        for (int i = 0; i < 6; i++) {
            planes[i] = new Plane();
        }
    }
    
    /**
     * Updates the frustum planes from a view-projection matrix.
     * Uses the Gribb-Hartmann method to extract planes from the matrix.
     * 
     * @param viewProjection the combined view-projection matrix
     */
    public void update(Matrix4f viewProjection) {
        // Extract planes using Gribb-Hartmann method
        // Matrix is in column-major order
        
        float m00 = viewProjection.m00(), m01 = viewProjection.m01(), m02 = viewProjection.m02(), m03 = viewProjection.m03();
        float m10 = viewProjection.m10(), m11 = viewProjection.m11(), m12 = viewProjection.m12(), m13 = viewProjection.m13();
        float m20 = viewProjection.m20(), m21 = viewProjection.m21(), m22 = viewProjection.m22(), m23 = viewProjection.m23();
        float m30 = viewProjection.m30(), m31 = viewProjection.m31(), m32 = viewProjection.m32(), m33 = viewProjection.m33();
        
        // Left plane: add 4th column to 1st column
        planes[LEFT].normal.set(m30 + m00, m31 + m01, m32 + m02);
        planes[LEFT].distance = m33 + m03;
        planes[LEFT].normalize();
        
        // Right plane: subtract 1st column from 4th column
        planes[RIGHT].normal.set(m30 - m00, m31 - m01, m32 - m02);
        planes[RIGHT].distance = m33 - m03;
        planes[RIGHT].normalize();
        
        // Bottom plane: add 4th column to 2nd column
        planes[BOTTOM].normal.set(m30 + m10, m31 + m11, m32 + m12);
        planes[BOTTOM].distance = m33 + m13;
        planes[BOTTOM].normalize();
        
        // Top plane: subtract 2nd column from 4th column
        planes[TOP].normal.set(m30 - m10, m31 - m11, m32 - m12);
        planes[TOP].distance = m33 - m13;
        planes[TOP].normalize();
        
        // Near plane: add 4th column to 3rd column
        planes[NEAR].normal.set(m30 + m20, m31 + m21, m32 + m22);
        planes[NEAR].distance = m33 + m23;
        planes[NEAR].normalize();
        
        // Far plane: subtract 3rd column from 4th column
        planes[FAR].normal.set(m30 - m20, m31 - m21, m32 - m22);
        planes[FAR].distance = m33 - m23;
        planes[FAR].normalize();
    }
    
    /**
     * Tests if an axis-aligned bounding box is visible in the frustum.
     * 
     * @param min minimum corner of the AABB
     * @param max maximum corner of the AABB
     * @return true if the box is at least partially visible
     */
    public boolean isBoxVisible(Vector3f min, Vector3f max) {
        // Test each plane
        for (Plane plane : planes) {
            // Find the positive vertex (corner farthest in plane normal direction)
            Vector3f positiveVertex = new Vector3f(
                plane.normal.x > 0 ? max.x : min.x,
                plane.normal.y > 0 ? max.y : min.y,
                plane.normal.z > 0 ? max.z : min.z
            );
            
            // If positive vertex is outside, box is completely outside
            if (plane.distanceToPoint(positiveVertex) < 0) {
                return false;
            }
        }
        
        // Box is at least partially inside
        return true;
    }
    
    /**
     * Tests if a chunk is visible in the frustum.
     * Convenience method that converts chunk position to world AABB.
     * 
     * @param chunkPos the chunk position
     * @return true if the chunk is at least partially visible
     */
    public boolean isChunkVisible(ChunkPos chunkPos) {
        Vector3f min = new Vector3f(chunkPos.getX() * 16.0f, chunkPos.getY() * 256.0f, chunkPos.getZ() * 16.0f);
        Vector3f max = new Vector3f(min.x + 16.0f, min.y + 256.0f, min.z + 16.0f);
        return isBoxVisible(min, max);
    }
}
