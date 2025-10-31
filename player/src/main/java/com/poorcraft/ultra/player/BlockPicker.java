package com.poorcraft.ultra.player;

import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import java.util.Optional;

public final class BlockPicker {

    public static final float DEFAULT_REACH_DISTANCE = 5.0f;

    private BlockPicker() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Optional<BlockHitResult> raycast(Camera camera, Node chunkRootNode) {
        return raycast(camera, chunkRootNode, DEFAULT_REACH_DISTANCE);
    }

    public static Optional<BlockHitResult> raycast(Camera camera, Node chunkRootNode, float reachDistance) {
        if (camera == null || chunkRootNode == null) {
            return Optional.empty();
        }

        Vector3f origin = camera.getLocation();
        Vector3f direction = camera.getDirection().normalize();
        Ray ray = new Ray(origin, direction);

        CollisionResults results = new CollisionResults();
        chunkRootNode.collideWith(ray, results);

        if (results.isEmpty()) {
            return Optional.empty();
        }

        CollisionResult closest = results.getClosestCollision();
        if (closest == null || closest.getDistance() > reachDistance) {
            return Optional.empty();
        }

        Vector3f contactPoint = closest.getContactPoint();
        Vector3f faceNormal = normalizeToCardinal(closest.getContactNormal());
        Vector3f blockPosition = floorVector(contactPoint.subtract(faceNormal.mult(0.001f)));

        return Optional.of(new BlockHitResult(contactPoint, blockPosition, faceNormal, closest.getDistance()));
    }

    public static Vector3f normalizeToCardinal(Vector3f normal) {
        if (normal == null) {
            return Vector3f.ZERO;
        }
        float absX = Math.abs(normal.x);
        float absY = Math.abs(normal.y);
        float absZ = Math.abs(normal.z);

        if (absX >= absY && absX >= absZ) {
            return normal.x >= 0 ? Vector3f.UNIT_X : Vector3f.UNIT_X.negate();
        }
        if (absY >= absX && absY >= absZ) {
            return normal.y >= 0 ? Vector3f.UNIT_Y : Vector3f.UNIT_Y.negate();
        }
        return normal.z >= 0 ? Vector3f.UNIT_Z : Vector3f.UNIT_Z.negate();
    }

    public static Vector3f floorVector(Vector3f worldPos) {
        if (worldPos == null) {
            return Vector3f.ZERO;
        }
        return new Vector3f((float) Math.floor(worldPos.x),
                (float) Math.floor(worldPos.y),
                (float) Math.floor(worldPos.z));
    }
}
