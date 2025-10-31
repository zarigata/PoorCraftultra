package com.poorcraft.ultra.voxel;

import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Greedy mesh generator that merges co-planar faces within a chunk into tiled quads.
 */
public final class GreedyMesher {

    private GreedyMesher() {
        throw new UnsupportedOperationException("Utility class; do not instantiate");
    }

    public static Mesh generateMesh(Chunk chunk,
                                    ChunkStorage storage,
                                    BlockRegistry registry,
                                    TextureAtlas atlas) {
        Objects.requireNonNull(chunk, "chunk must not be null");
        Objects.requireNonNull(storage, "storage must not be null");
        Objects.requireNonNull(registry, "registry must not be null");
        Objects.requireNonNull(atlas, "atlas must not be null");

        List<Vector3f> positions = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        List<Float> uvs = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        int[] dimensions = {
                Chunk.CHUNK_SIZE_X,
                Chunk.CHUNK_SIZE_Y,
                Chunk.CHUNK_SIZE_Z
        };

        int chunkOriginX = chunk.getPosition().getWorldX();
        int chunkOriginZ = chunk.getPosition().getWorldZ();

        int[] coordinate = new int[3];
        int[] offset = new int[3];

        for (int axis = 0; axis < 3; axis++) {
            int axisU = (axis + 1) % 3;
            int axisV = (axis + 2) % 3;
            int sizeU = dimensions[axisU];
            int sizeV = dimensions[axisV];

            Face[] mask = new Face[sizeU * sizeV];
            Arrays.fill(mask, null);

            Arrays.fill(offset, 0);
            offset[axis] = 1;

            for (coordinate[axis] = -1; coordinate[axis] < dimensions[axis]; ) {
                int maskIndex = 0;
                for (coordinate[axisV] = 0; coordinate[axisV] < sizeV; coordinate[axisV]++) {
                    for (coordinate[axisU] = 0; coordinate[axisU] < sizeU; coordinate[axisU]++) {
                        short blockA = getBlockAt(chunk, storage, chunkOriginX, chunkOriginZ,
                                coordinate[0], coordinate[1], coordinate[2]);
                        short blockB = getBlockAt(chunk, storage, chunkOriginX, chunkOriginZ,
                                coordinate[0] + offset[0], coordinate[1] + offset[1], coordinate[2] + offset[2]);

                        BlockDefinition defA = registry.getDefinition(blockA);
                        BlockDefinition defB = registry.getDefinition(blockB);

                        Face face = null;
                        if (shouldRenderFace(defA, defB)) {
                            face = createFace(blockA, defA, FaceDirection.fromAxis(axis, true), atlas);
                        } else if (shouldRenderFace(defB, defA)) {
                            face = createFace(blockB, defB, FaceDirection.fromAxis(axis, false), atlas);
                        }
                        mask[maskIndex++] = face;
                    }
                }

                ++coordinate[axis];

                for (int v = 0; v < sizeV; v++) {
                    int rowIndex = v * sizeU;
                    for (int u = 0; u < sizeU; ) {
                        Face face = mask[rowIndex + u];
                        if (face == null) {
                            u++;
                            continue;
                        }

                        int width = 1;
                        while (u + width < sizeU) {
                            Face candidate = mask[rowIndex + u + width];
                            if (!face.matches(candidate)) {
                                break;
                            }
                            width++;
                        }

                        int height = 1;
                        boolean canExpand = true;
                        while (v + height < sizeV && canExpand) {
                            int nextRow = (v + height) * sizeU;
                            for (int k = 0; k < width; k++) {
                                Face candidate = mask[nextRow + u + k];
                                if (!face.matches(candidate)) {
                                    canExpand = false;
                                    break;
                                }
                            }
                            if (canExpand) {
                                height++;
                            }
                        }

                        emitQuad(face, axis, axisU, axisV, coordinate[axis], u, v, width, height,
                                positions, normals, uvs, indices);

                        for (int y = 0; y < height; y++) {
                            int clearRow = (v + y) * sizeU;
                            for (int k = 0; k < width; k++) {
                                mask[clearRow + u + k] = null;
                            }
                        }

                        u += width;
                    }
                }
            }
        }

        if (positions.isEmpty()) {
            return new Mesh();
        }

        Mesh mesh = new Mesh();

        Vector3f[] positionArray = positions.toArray(new Vector3f[0]);
        FloatBuffer positionBuffer = BufferUtils.createFloatBuffer(positionArray);

        Vector3f[] normalArray = normals.toArray(new Vector3f[0]);
        FloatBuffer normalBuffer = BufferUtils.createFloatBuffer(normalArray);

        FloatBuffer uvBuffer = BufferUtils.createFloatBuffer(uvs.size());
        for (Float uv : uvs) {
            uvBuffer.put(uv);
        }
        uvBuffer.flip();

        int[] indexArray = indices.stream().mapToInt(Integer::intValue).toArray();
        IntBuffer indexBuffer = BufferUtils.createIntBuffer(indexArray.length);
        indexBuffer.put(indexArray).flip();

        mesh.setBuffer(VertexBuffer.Type.Position, 3, positionBuffer);
        mesh.setBuffer(VertexBuffer.Type.Normal, 3, normalBuffer);
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, uvBuffer);
        mesh.setBuffer(VertexBuffer.Type.Index, 3, indexBuffer);
        mesh.updateBound();
        mesh.updateCounts();
        mesh.setStatic();
        return mesh;
    }

    private static boolean shouldRenderFace(BlockDefinition definition, BlockDefinition neighborDefinition) {
        if (!isRenderable(definition)) {
            return false;
        }
        if (neighborDefinition == null) {
            return true;
        }

        BlockType neighborType = neighborDefinition.getType();
        if (neighborType == BlockType.AIR) {
            return true;
        }
        if (!neighborDefinition.isSolid()) {
            return true;
        }

        if (definition.isTransparent() && neighborDefinition.isTransparent()) {
            return definition.getId() != neighborDefinition.getId();
        }

        if (definition.isTransparent() && !neighborDefinition.isTransparent()) {
            return true;
        }

        if (!definition.isTransparent() && neighborDefinition.isTransparent()) {
            return true;
        }

        return false;
    }

    private static boolean isRenderable(BlockDefinition definition) {
        return definition != null && definition.getType() != BlockType.AIR;
    }

    private static Face createFace(short blockId,
                                   BlockDefinition definition,
                                   FaceDirection direction,
                                   TextureAtlas atlas) {
        int atlasIndex = definition.getAtlasIndex(direction.getFaceName());
        if (atlasIndex < 0) {
            atlasIndex = atlas.getAtlasIndex(definition.getName(), direction.getFaceName());
        }
        float[] baseUV = atlas.getUVCoordinates(atlasIndex);
        return new Face(blockId, definition, direction, atlasIndex, baseUV);
    }

    private static short getBlockAt(Chunk chunk,
                                    ChunkStorage storage,
                                    int chunkOriginX,
                                    int chunkOriginZ,
                                    int x,
                                    int y,
                                    int z) {
        if (y < 0 || y >= Chunk.CHUNK_SIZE_Y) {
            return BlockType.AIR.getId();
        }

        if (x >= 0 && x < Chunk.CHUNK_SIZE_X && z >= 0 && z < Chunk.CHUNK_SIZE_Z) {
            return chunk.getBlock(x, y, z);
        }

        int worldX = chunkOriginX + x;
        int worldZ = chunkOriginZ + z;
        return storage.getBlock(worldX, y, worldZ);
    }

    private static void emitQuad(Face face,
                                 int axis,
                                 int axisU,
                                 int axisV,
                                 int plane,
                                 int startU,
                                 int startV,
                                 int width,
                                 int height,
                                 List<Vector3f> positions,
                                 List<Vector3f> normals,
                                 List<Float> uvs,
                                 List<Integer> indices) {
        FaceDirection direction = face.direction();
        Vector3f normal = direction.getNormal();

        Vector3f base = new Vector3f();
        base.set(axis, plane);
        base.set(axisU, startU);
        base.set(axisV, startV);

        Vector3f du = axisVector(axisU, width);
        Vector3f dv = axisVector(axisV, height);

        Vector3f[] corners = new Vector3f[4];
        corners[0] = base.clone();
        corners[1] = base.clone().addLocal(dv);
        corners[2] = base.clone().addLocal(du).addLocal(dv);
        corners[3] = base.clone().addLocal(du);

        int[] order = direction.isPositive()
                ? new int[]{0, 3, 2, 1}
                : new int[]{0, 1, 2, 3};

        int baseIndex = positions.size();
        for (int idx : order) {
            Vector3f vertex = corners[idx];
            positions.add(vertex);
            normals.add(normal.clone());
        }

        float u0 = face.baseUV()[0];
        float v0 = face.baseUV()[1];
        float u1 = face.baseUV()[2];
        float v1 = face.baseUV()[3];

        float uSpan = (u1 - u0) * width;
        float vSpan = (v1 - v0) * height;

        float[][] uvCorners = new float[][]{
                {u0, v0},
                {u0, v0 + vSpan},
                {u0 + uSpan, v0 + vSpan},
                {u0 + uSpan, v0}
        };

        for (int idx : order) {
            float[] uv = uvCorners[idx];
            uvs.add(uv[0]);
            uvs.add(uv[1]);
        }

        indices.add(baseIndex);
        indices.add(baseIndex + 1);
        indices.add(baseIndex + 2);
        indices.add(baseIndex);
        indices.add(baseIndex + 2);
        indices.add(baseIndex + 3);
    }

    private static Vector3f axisVector(int axis, float length) {
        return switch (axis) {
            case 0 -> new Vector3f(length, 0f, 0f);
            case 1 -> new Vector3f(0f, length, 0f);
            case 2 -> new Vector3f(0f, 0f, length);
            default -> throw new IllegalArgumentException("Invalid axis: " + axis);
        };
    }

    private record Face(short blockId,
                        BlockDefinition definition,
                        FaceDirection direction,
                        int atlasIndex,
                        float[] baseUV) {
        boolean matches(Face other) {
            return other != null
                    && blockId == other.blockId
                    && direction == other.direction
                    && atlasIndex == other.atlasIndex;
        }
    }

    private enum FaceDirection {
        POS_X(0, true, new Vector3f(1f, 0f, 0f), "side"),
        NEG_X(0, false, new Vector3f(-1f, 0f, 0f), "side"),
        POS_Y(1, true, Vector3f.UNIT_Y.clone(), "top"),
        NEG_Y(1, false, new Vector3f(0f, -1f, 0f), "bottom"),
        POS_Z(2, true, new Vector3f(0f, 0f, 1f), "side"),
        NEG_Z(2, false, new Vector3f(0f, 0f, -1f), "side");

        private final int axis;
        private final boolean positive;
        private final Vector3f normal;
        private final String faceName;

        FaceDirection(int axis, boolean positive, Vector3f normal, String faceName) {
            this.axis = axis;
            this.positive = positive;
            this.normal = normal;
            this.faceName = faceName;
        }

        static FaceDirection fromAxis(int axis, boolean positive) {
            return switch (axis) {
                case 0 -> positive ? POS_X : NEG_X;
                case 1 -> positive ? POS_Y : NEG_Y;
                case 2 -> positive ? POS_Z : NEG_Z;
                default -> throw new IllegalArgumentException("Invalid axis: " + axis);
            };
        }

        int getAxis() {
            return axis;
        }

        boolean isPositive() {
            return positive;
        }

        Vector3f getNormal() {
            return normal.clone();
        }

        String getFaceName() {
            return faceName;
        }
    }
}
