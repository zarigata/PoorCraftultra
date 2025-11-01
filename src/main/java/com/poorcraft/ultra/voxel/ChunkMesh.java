package com.poorcraft.ultra.voxel;

import java.util.Map;
import java.util.Set;

public record ChunkMesh(
    Map<BlockType, float[]> positions,
    Map<BlockType, float[]> normals,
    Map<BlockType, float[]> texCoords,
    Map<BlockType, int[]> indices,
    Map<BlockType, Integer> vertexCounts,
    Map<BlockType, Integer> triangleCounts
) {
    public static ChunkMesh empty() {
        return new ChunkMesh(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
    }

    public boolean isEmpty() {
        return vertexCounts.values().stream().mapToInt(Integer::intValue).sum() == 0;
    }

    public Set<BlockType> blockTypes() {
        return vertexCounts.keySet();
    }

    public int totalVertexCount() {
        return vertexCounts.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int totalTriangleCount() {
        return triangleCounts.values().stream().mapToInt(Integer::intValue).sum();
    }

    public float[] positions(BlockType type) {
        return positions.get(type);
    }

    public float[] normals(BlockType type) {
        return normals.get(type);
    }

    public float[] texCoords(BlockType type) {
        return texCoords.get(type);
    }

    public int[] indices(BlockType type) {
        return indices.get(type);
    }

    public int vertexCount(BlockType type) {
        return vertexCounts.getOrDefault(type, 0);
    }

    public int triangleCount(BlockType type) {
        return triangleCounts.getOrDefault(type, 0);
    }
}
