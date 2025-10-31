package com.poorcraft.ultra.voxel;

import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public final class ChunkMesh {

    private static final float BLOCK_SIZE = 1.0f;

    private ChunkMesh() {
        throw new UnsupportedOperationException("Utility class, do not instantiate");
    }

    public static Mesh generateFlatPatch(int width, int depth, TextureAtlas atlas) {
        if (width <= 0 || depth <= 0) {
            throw new IllegalArgumentException("width and depth must be positive");
        }
        if (atlas == null) {
            throw new IllegalArgumentException("atlas must not be null");
        }

        int topIndex = atlas.getAtlasIndex("grass", "top");
        int sideIndex = atlas.getAtlasIndex("grass", "side");
        int bottomIndex = atlas.getAtlasIndex("dirt");

        float[] topUV = atlas.getUVCoordinates(topIndex);
        float[] sideUV = atlas.getUVCoordinates(sideIndex);
        float[] bottomUV = atlas.getUVCoordinates(bottomIndex);

        List<Vector3f> positions = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        List<Float> uvs = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        float widthUnits = width * BLOCK_SIZE;
        float depthUnits = depth * BLOCK_SIZE;
        float heightUnits = BLOCK_SIZE;

        Vector3f topNorthWest = new Vector3f(0f, heightUnits, 0f);
        Vector3f topNorthEast = new Vector3f(widthUnits, heightUnits, 0f);
        Vector3f topSouthEast = new Vector3f(widthUnits, heightUnits, depthUnits);
        Vector3f topSouthWest = new Vector3f(0f, heightUnits, depthUnits);

        Vector3f bottomNorthWest = new Vector3f(0f, 0f, 0f);
        Vector3f bottomNorthEast = new Vector3f(widthUnits, 0f, 0f);
        Vector3f bottomSouthEast = new Vector3f(widthUnits, 0f, depthUnits);
        Vector3f bottomSouthWest = new Vector3f(0f, 0f, depthUnits);

        addQuad(positions, normals, uvs, indices,
                new Vector3f[]{topNorthWest, topNorthEast, topSouthEast, topSouthWest},
                Vector3f.UNIT_Y, topUV);

        addQuad(positions, normals, uvs, indices,
                new Vector3f[]{bottomNorthWest, bottomNorthEast, bottomSouthEast, bottomSouthWest},
                new Vector3f(0f, -1f, 0f), bottomUV);

        addQuad(positions, normals, uvs, indices,
                new Vector3f[]{bottomNorthWest, bottomSouthWest, topSouthWest, topNorthWest},
                new Vector3f(-1f, 0f, 0f), sideUV);

        addQuad(positions, normals, uvs, indices,
                new Vector3f[]{bottomSouthEast, bottomNorthEast, topNorthEast, topSouthEast},
                new Vector3f(1f, 0f, 0f), sideUV);

        addQuad(positions, normals, uvs, indices,
                new Vector3f[]{bottomNorthEast, bottomNorthWest, topNorthWest, topNorthEast},
                new Vector3f(0f, 0f, -1f), sideUV);

        addQuad(positions, normals, uvs, indices,
                new Vector3f[]{bottomSouthWest, bottomSouthEast, topSouthEast, topSouthWest},
                new Vector3f(0f, 0f, 1f), sideUV);

        Mesh mesh = new Mesh();

        FloatBuffer positionBuffer = BufferUtils.createFloatBuffer(positions.toArray(new Vector3f[0]));
        FloatBuffer normalBuffer = BufferUtils.createFloatBuffer(normals.toArray(new Vector3f[0]));
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

    private static void addQuad(List<Vector3f> positions,
                                List<Vector3f> normals,
                                List<Float> uvs,
                                List<Integer> indices,
                                Vector3f[] corners,
                                Vector3f normal,
                                float[] tileUV) {
        Vector3f[] ordered = new Vector3f[corners.length];
        for (int i = 0; i < corners.length; i++) {
            ordered[i] = corners[i].clone();
        }

        Vector3f edge1 = ordered[1].subtract(ordered[0]);
        Vector3f edge2 = ordered[2].subtract(ordered[0]);
        Vector3f cross = edge1.cross(edge2);
        if (cross.dot(normal) < 0f) {
            Vector3f temp = ordered[1];
            ordered[1] = ordered[3];
            ordered[3] = temp;
        }

        int baseIndex = positions.size();
        for (Vector3f corner : ordered) {
            positions.add(corner.clone());
            normals.add(normal.clone());
        }

        float u0 = tileUV[0];
        float v0 = tileUV[1];
        float u1 = tileUV[2];
        float v1 = tileUV[3];

        uvs.add(u0);
        uvs.add(v1);
        uvs.add(u1);
        uvs.add(v1);
        uvs.add(u1);
        uvs.add(v0);
        uvs.add(u0);
        uvs.add(v0);

        indices.add(baseIndex);
        indices.add(baseIndex + 1);
        indices.add(baseIndex + 2);
        indices.add(baseIndex);
        indices.add(baseIndex + 2);
        indices.add(baseIndex + 3);
    }
}
