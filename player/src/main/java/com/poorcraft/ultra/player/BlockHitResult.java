package com.poorcraft.ultra.player;

import com.jme3.math.Vector3f;

public record BlockHitResult(Vector3f hitPoint,
                             Vector3f blockPosition,
                             Vector3f faceNormal,
                             float distance) {

    public Vector3f getPlacementPosition() {
        return blockPosition.add(faceNormal);
    }

    public int getBlockX() {
        return (int) blockPosition.x;
    }

    public int getBlockY() {
        return (int) blockPosition.y;
    }

    public int getBlockZ() {
        return (int) blockPosition.z;
    }

    public int getPlacementX() {
        return (int) (blockPosition.x + faceNormal.x);
    }

    public int getPlacementY() {
        return (int) (blockPosition.y + faceNormal.y);
    }

    public int getPlacementZ() {
        return (int) (blockPosition.z + faceNormal.z);
    }
}
