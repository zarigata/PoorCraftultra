package com.poorcraft.ultra.voxel;

import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Converts voxel data into mesh buffers using a greedy quad merging strategy.
 */
public class ChunkMesher {

    public interface NeighborAccessor {
        BlockType getBlock(int worldX, int worldY, int worldZ);
    }

    public ChunkMesh buildMesh(Chunk chunk, NeighborAccessor neighborAccessor) {
        if (chunk == null) {
            return ChunkMesh.empty();
        }

        Map<BlockType, List<Float>> positions = new EnumMap<>(BlockType.class);
        Map<BlockType, List<Float>> normals = new EnumMap<>(BlockType.class);
        Map<BlockType, List<Float>> texCoords = new EnumMap<>(BlockType.class);
        Map<BlockType, List<Integer>> indices = new EnumMap<>(BlockType.class);

        for (Direction direction : Direction.values()) {
            buildFaces(chunk, neighborAccessor, direction, positions, normals, texCoords, indices);
        }

        if (positions.values().stream().allMatch(List::isEmpty)) {
            return ChunkMesh.empty();
        }

        Map<BlockType, float[]> posMap = new EnumMap<>(BlockType.class);
        Map<BlockType, float[]> normalMap = new EnumMap<>(BlockType.class);
        Map<BlockType, float[]> texMap = new EnumMap<>(BlockType.class);
        Map<BlockType, int[]> indexMap = new EnumMap<>(BlockType.class);
        Map<BlockType, Integer> vertexCounts = new EnumMap<>(BlockType.class);
        Map<BlockType, Integer> triangleCounts = new EnumMap<>(BlockType.class);

        positions.forEach((type, list) -> {
            if (list.isEmpty()) {
                return;
            }
            posMap.put(type, toFloatArray(list));
        });
        normals.forEach((type, list) -> {
            if (list.isEmpty()) {
                return;
            }
            normalMap.put(type, toFloatArray(list));
        });
        texCoords.forEach((type, list) -> {
            if (list.isEmpty()) {
                return;
            }
            texMap.put(type, toFloatArray(list));
        });
        indices.forEach((type, list) -> {
            if (list.isEmpty()) {
                return;
            }
            indexMap.put(type, toIntArray(list));
            vertexCounts.put(type, posMap.get(type).length / 3);
            triangleCounts.put(type, indexMap.get(type).length / 3);
        });

        return new ChunkMesh(posMap, normalMap, texMap, indexMap, vertexCounts, triangleCounts);
    }

    private void buildFaces(Chunk chunk,
                            NeighborAccessor neighborAccessor,
                            Direction direction,
                            Map<BlockType, List<Float>> positions,
                            Map<BlockType, List<Float>> normals,
                            Map<BlockType, List<Float>> texCoords,
                            Map<BlockType, List<Integer>> indices) {
        switch (direction) {
            case UP, DOWN -> buildHorizontalFaces(chunk, neighborAccessor, direction, positions, normals, texCoords, indices);
            case NORTH, SOUTH, EAST, WEST -> buildVerticalFaces(chunk, neighborAccessor, direction, positions, normals, texCoords, indices);
        }
    }

    private void buildHorizontalFaces(Chunk chunk,
                                      NeighborAccessor neighborAccessor,
                                      Direction direction,
                                      Map<BlockType, List<Float>> positions,
                                      Map<BlockType, List<Float>> normals,
                                      Map<BlockType, List<Float>> texCoords,
                                      Map<BlockType, List<Integer>> indices) {
        boolean top = direction == Direction.UP;

        for (int y = 0; y < Chunk.SIZE_Y; y++) {
            BlockType[][] mask = new BlockType[Chunk.SIZE_X][Chunk.SIZE_Z];

            for (int x = 0; x < Chunk.SIZE_X; x++) {
                for (int z = 0; z < Chunk.SIZE_Z; z++) {
                    BlockType current = chunk.getBlock(x, y, z);
                    if (current == BlockType.AIR || !current.opaque()) {
                        continue;
                    }
                    int neighborY = top ? y + 1 : y - 1;
                    BlockType neighbor = getNeighborBlock(chunk, neighborAccessor, x, neighborY, z, direction);
                    if (neighbor == BlockType.AIR || !neighbor.opaque()) {
                        mask[x][z] = current;
                    }
                }
            }

            final int sliceY = top ? y + 1 : y;
            greedyMerge(mask, (startX, startZ, width, height, blockType) -> {
                if (blockType == null) {
                    return;
                }
                float yCoord = sliceY;
                float widthF = width;
                float heightF = height;
                Vector3f v0 = new Vector3f(startX, yCoord, startZ);
                Vector3f v1 = new Vector3f(startX, yCoord, startZ + heightF);
                Vector3f v2 = new Vector3f(startX + widthF, yCoord, startZ);
                Vector3f v3 = new Vector3f(startX + widthF, yCoord, startZ + heightF);
                if (!top) {
                    // Flip vertices for downward facing quads
                    v0 = new Vector3f(startX, yCoord, startZ + heightF);
                    v1 = new Vector3f(startX, yCoord, startZ);
                    v2 = new Vector3f(startX + widthF, yCoord, startZ + heightF);
                    v3 = new Vector3f(startX + widthF, yCoord, startZ);
                }
                addQuad(blockType, positions, normals, texCoords, indices, v0, v1, v2, v3, direction.normal(), widthF, heightF);
            });
        }
    }

    private void buildVerticalFaces(Chunk chunk,
                                    NeighborAccessor neighborAccessor,
                                    Direction direction,
                                    Map<BlockType, List<Float>> positions,
                                    Map<BlockType, List<Float>> normals,
                                    Map<BlockType, List<Float>> texCoords,
                                    Map<BlockType, List<Integer>> indices) {
        switch (direction) {
            case NORTH -> buildNorthFaces(chunk, neighborAccessor, positions, normals, texCoords, indices);
            case SOUTH -> buildSouthFaces(chunk, neighborAccessor, positions, normals, texCoords, indices);
            case EAST -> buildEastFaces(chunk, neighborAccessor, positions, normals, texCoords, indices);
            case WEST -> buildWestFaces(chunk, neighborAccessor, positions, normals, texCoords, indices);
            default -> throw new IllegalArgumentException("Unsupported direction for vertical faces: " + direction);
        }
    }

    private void buildNorthFaces(Chunk chunk,
                                 NeighborAccessor neighborAccessor,
                                 Map<BlockType, List<Float>> positions,
                                 Map<BlockType, List<Float>> normals,
                                 Map<BlockType, List<Float>> texCoords,
                                 Map<BlockType, List<Integer>> indices) {
        for (int z = 0; z < Chunk.SIZE_Z; z++) {
            BlockType[][] mask = new BlockType[Chunk.SIZE_X][Chunk.SIZE_Y];

            for (int x = 0; x < Chunk.SIZE_X; x++) {
                for (int y = 0; y < Chunk.SIZE_Y; y++) {
                    BlockType current = chunk.getBlock(x, y, z);
                    if (current == BlockType.AIR || !current.opaque()) {
                        continue;
                    }
                    BlockType neighbor = getNeighborBlock(chunk, neighborAccessor, x, y, z - 1, Direction.NORTH);
                    if (neighbor == BlockType.AIR || !neighbor.opaque()) {
                        mask[x][y] = current;
                    }
                }
            }

            final int faceZ = z;
            greedyMerge(mask, (startX, startY, width, height, blockType) -> {
                if (blockType == null) {
                    return;
                }
                float widthF = width;
                float heightF = height;
                float zCoord = faceZ;
                Vector3f v0 = new Vector3f(startX, startY, zCoord);
                Vector3f v1 = new Vector3f(startX + widthF, startY, zCoord);
                Vector3f v2 = new Vector3f(startX, startY + heightF, zCoord);
                Vector3f v3 = new Vector3f(startX + widthF, startY + heightF, zCoord);
                addQuad(blockType, positions, normals, texCoords, indices, v0, v2, v1, v3, Direction.NORTH.normal(), widthF, heightF);
            });
        }
    }

    private void buildSouthFaces(Chunk chunk,
                                 NeighborAccessor neighborAccessor,
                                 Map<BlockType, List<Float>> positions,
                                 Map<BlockType, List<Float>> normals,
                                 Map<BlockType, List<Float>> texCoords,
                                 Map<BlockType, List<Integer>> indices) {
        for (int z = 0; z < Chunk.SIZE_Z; z++) {
            BlockType[][] mask = new BlockType[Chunk.SIZE_X][Chunk.SIZE_Y];

            for (int x = 0; x < Chunk.SIZE_X; x++) {
                for (int y = 0; y < Chunk.SIZE_Y; y++) {
                    BlockType current = chunk.getBlock(x, y, z);
                    if (current == BlockType.AIR || !current.opaque()) {
                        continue;
                    }
                    BlockType neighbor = getNeighborBlock(chunk, neighborAccessor, x, y, z + 1, Direction.SOUTH);
                    if (neighbor == BlockType.AIR || !neighbor.opaque()) {
                        mask[x][y] = current;
                    }
                }
            }

            final int faceZ = z + 1;
            greedyMerge(mask, (startX, startY, width, height, blockType) -> {
                if (blockType == null) {
                    return;
                }
                float widthF = width;
                float heightF = height;
                float zCoord = faceZ;
                Vector3f v0 = new Vector3f(startX, startY, zCoord);
                Vector3f v1 = new Vector3f(startX + widthF, startY, zCoord);
                Vector3f v2 = new Vector3f(startX, startY + heightF, zCoord);
                Vector3f v3 = new Vector3f(startX + widthF, startY + heightF, zCoord);
                addQuad(blockType, positions, normals, texCoords, indices, v0, v1, v2, v3, Direction.SOUTH.normal(), widthF, heightF);
            });
        }
    }

    private void buildEastFaces(Chunk chunk,
                                NeighborAccessor neighborAccessor,
                                Map<BlockType, List<Float>> positions,
                                Map<BlockType, List<Float>> normals,
                                Map<BlockType, List<Float>> texCoords,
                                Map<BlockType, List<Integer>> indices) {
        for (int x = 0; x < Chunk.SIZE_X; x++) {
            BlockType[][] mask = new BlockType[Chunk.SIZE_Z][Chunk.SIZE_Y];

            for (int z = 0; z < Chunk.SIZE_Z; z++) {
                for (int y = 0; y < Chunk.SIZE_Y; y++) {
                    BlockType current = chunk.getBlock(x, y, z);
                    if (current == BlockType.AIR || !current.opaque()) {
                        continue;
                    }
                    BlockType neighbor = getNeighborBlock(chunk, neighborAccessor, x + 1, y, z, Direction.EAST);
                    if (neighbor == BlockType.AIR || !neighbor.opaque()) {
                        mask[z][y] = current;
                    }
                }
            }

            final int faceX = x + 1;
            greedyMerge(mask, (startZ, startY, width, height, blockType) -> {
                if (blockType == null) {
                    return;
                }
                float widthF = width;
                float heightF = height;
                float xCoord = faceX;
                Vector3f v0 = new Vector3f(xCoord, startY, startZ);
                Vector3f v1 = new Vector3f(xCoord, startY, startZ + widthF);
                Vector3f v2 = new Vector3f(xCoord, startY + heightF, startZ);
                Vector3f v3 = new Vector3f(xCoord, startY + heightF, startZ + widthF);
                addQuad(blockType, positions, normals, texCoords, indices, v0, v2, v1, v3, Direction.EAST.normal(), widthF, heightF);
            });
        }
    }

    private void buildWestFaces(Chunk chunk,
                                NeighborAccessor neighborAccessor,
                                Map<BlockType, List<Float>> positions,
                                Map<BlockType, List<Float>> normals,
                                Map<BlockType, List<Float>> texCoords,
                                Map<BlockType, List<Integer>> indices) {
        for (int x = 0; x < Chunk.SIZE_X; x++) {
            BlockType[][] mask = new BlockType[Chunk.SIZE_Z][Chunk.SIZE_Y];

            for (int z = 0; z < Chunk.SIZE_Z; z++) {
                for (int y = 0; y < Chunk.SIZE_Y; y++) {
                    BlockType current = chunk.getBlock(x, y, z);
                    if (current == BlockType.AIR || !current.opaque()) {
                        continue;
                    }
                    BlockType neighbor = getNeighborBlock(chunk, neighborAccessor, x - 1, y, z, Direction.WEST);
                    if (neighbor == BlockType.AIR || !neighbor.opaque()) {
                        mask[z][y] = current;
                    }
                }
            }

            final int faceX = x;
            greedyMerge(mask, (startZ, startY, width, height, blockType) -> {
                if (blockType == null) {
                    return;
                }
                float widthF = width;
                float heightF = height;
                float xCoord = faceX;
                Vector3f v0 = new Vector3f(xCoord, startY, startZ);
                Vector3f v1 = new Vector3f(xCoord, startY + heightF, startZ);
                Vector3f v2 = new Vector3f(xCoord, startY, startZ + widthF);
                Vector3f v3 = new Vector3f(xCoord, startY + heightF, startZ + widthF);
                addQuad(blockType, positions, normals, texCoords, indices, v0, v1, v2, v3, Direction.WEST.normal(), widthF, heightF);
            });
        }
    }

    private BlockType getNeighborBlock(Chunk chunk,
                                       NeighborAccessor neighborAccessor,
                                       int x,
                                       int y,
                                       int z,
                                       Direction direction) {
        if (y < 0 || y >= Chunk.SIZE_Y) {
            return BlockType.AIR;
        }
        if (x < 0 || x >= Chunk.SIZE_X || z < 0 || z >= Chunk.SIZE_Z) {
            Vector3f worldPos = toWorld(chunk, x, y, z);
            return neighborAccessor != null
                ? neighborAccessor.getBlock((int) worldPos.x, (int) worldPos.y, (int) worldPos.z)
                : BlockType.AIR;
        }
        return chunk.getBlock(x, y, z);
    }

    private Vector3f toWorld(Chunk chunk, int x, int y, int z) {
        int worldX = chunk.chunkX() * Chunk.SIZE_X + x;
        int worldZ = chunk.chunkZ() * Chunk.SIZE_Z + z;
        return new Vector3f(worldX, y, worldZ);
    }

    private void greedyMerge(BlockType[][] mask, MaskProcessor processor) {
        int width = mask.length;
        if (width == 0) {
            return;
        }
        int height = mask[0].length;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                BlockType blockType = mask[x][y];
                if (blockType == null) {
                    continue;
                }

                int quadWidth = 1;
                while (x + quadWidth < width && Objects.equals(mask[x + quadWidth][y], blockType)) {
                    quadWidth++;
                }

                int quadHeight = 1;
                boolean done = false;
                while (y + quadHeight < height && !done) {
                    for (int k = 0; k < quadWidth; k++) {
                        if (!Objects.equals(mask[x + k][y + quadHeight], blockType)) {
                            done = true;
                            break;
                        }
                    }
                    if (!done) {
                        quadHeight++;
                    }
                }

                processor.emit(x, y, quadWidth, quadHeight, blockType);

                for (int dx = 0; dx < quadWidth; dx++) {
                    for (int dy = 0; dy < quadHeight; dy++) {
                        mask[x + dx][y + dy] = null;
                    }
                }
            }
        }
    }

    private void addQuad(BlockType blockType,
                         Map<BlockType, List<Float>> positions,
                         Map<BlockType, List<Float>> normals,
                         Map<BlockType, List<Float>> texCoords,
                         Map<BlockType, List<Integer>> indices,
                         Vector3f v0,
                         Vector3f v1,
                         Vector3f v2,
                         Vector3f v3,
                         Vector3f normal,
                         float width,
                         float height) {
        List<Float> posList = positions.computeIfAbsent(blockType, key -> new ArrayList<>());
        List<Float> normalList = normals.computeIfAbsent(blockType, key -> new ArrayList<>());
        List<Float> texList = texCoords.computeIfAbsent(blockType, key -> new ArrayList<>());
        List<Integer> indexList = indices.computeIfAbsent(blockType, key -> new ArrayList<>());

        int baseIndex = posList.size() / 3;
        addVertex(posList, normalList, texList, v0, normal, 0f, 0f);
        addVertex(posList, normalList, texList, v1, normal, 0f, height);
        addVertex(posList, normalList, texList, v2, normal, width, 0f);
        addVertex(posList, normalList, texList, v3, normal, width, height);

        indexList.add(baseIndex);
        indexList.add(baseIndex + 1);
        indexList.add(baseIndex + 2);
        indexList.add(baseIndex + 2);
        indexList.add(baseIndex + 1);
        indexList.add(baseIndex + 3);
    }

    private void addVertex(List<Float> positions,
                           List<Float> normals,
                           List<Float> texCoords,
                           Vector3f position,
                           Vector3f normal,
                           float u,
                           float v) {
        positions.add(position.x);
        positions.add(position.y);
        positions.add(position.z);
        normals.add(normal.x);
        normals.add(normal.y);
        normals.add(normal.z);
        texCoords.add(u);
        texCoords.add(v);
    }

    private float[] toFloatArray(List<Float> values) {
        float[] array = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            array[i] = values.get(i);
        }
        return array;
    }

    private int[] toIntArray(List<Integer> values) {
        int[] array = new int[values.size()];
        for (int i = 0; i < values.size(); i++) {
            array[i] = values.get(i);
        }
        return array;
    }

    private interface MaskProcessor {
        void emit(int startX, int startY, int width, int height, BlockType type);
    }
}
