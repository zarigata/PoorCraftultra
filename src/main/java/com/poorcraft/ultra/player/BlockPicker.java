package com.poorcraft.ultra.player;

import com.jme3.math.Vector3f;
import com.poorcraft.ultra.voxel.BlockType;
import com.poorcraft.ultra.voxel.ChunkManager;
import com.poorcraft.ultra.voxel.Direction;

public class BlockPicker {
    private static final float DEFAULT_MAX_DISTANCE = 5.0f;
    private static final float STEP = 0.1f;

    private Vector3f cameraPosition;
    private Vector3f cameraDirection;
    private ChunkManager chunkManager;
    private float maxDistance = DEFAULT_MAX_DISTANCE;

    public void init(ChunkManager chunkManager) {
        this.chunkManager = chunkManager;
    }

    public void updateCamera(Vector3f position, Vector3f direction) {
        this.cameraPosition = position.clone();
        this.cameraDirection = direction.normalize();
    }

    public BlockPickResult pickBlock() {
        if (cameraPosition == null || cameraDirection == null || chunkManager == null) {
            return null;
        }

        Vector3f current = cameraPosition.clone();
        Vector3f stepVector = cameraDirection.mult(STEP);
        float distance = 0f;

        while (distance <= maxDistance) {
            current.addLocal(stepVector);
            distance += STEP;

            int blockX = (int) Math.floor(current.x);
            int blockY = (int) Math.floor(current.y);
            int blockZ = (int) Math.floor(current.z);

            BlockType blockType = chunkManager.getBlock(blockX, blockY, blockZ);
            if (blockType != BlockType.AIR) {
                Direction face = determineFace(current, blockX, blockY, blockZ);
                return new BlockPickResult(blockX, blockY, blockZ, face, distance);
            }
        }

        return null;
    }

    private Direction determineFace(Vector3f hitPoint, int blockX, int blockY, int blockZ) {
        Vector3f localHit = hitPoint.subtract(blockX + 0.5f, blockY + 0.5f, blockZ + 0.5f);
        float absX = Math.abs(localHit.x);
        float absY = Math.abs(localHit.y);
        float absZ = Math.abs(localHit.z);

        if (absX > absY && absX > absZ) {
            return localHit.x > 0 ? Direction.EAST : Direction.WEST;
        }
        if (absY > absX && absY > absZ) {
            return localHit.y > 0 ? Direction.UP : Direction.DOWN;
        }
        return localHit.z > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    public void setMaxDistance(float maxDistance) {
        this.maxDistance = maxDistance;
    }

    public record BlockPickResult(int blockX, int blockY, int blockZ, Direction face, float distance) {}
}
