package com.poorcraft.ultra.voxel;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.debug.WireBox;
import java.util.HashMap;
import java.util.Map;

public class ChunkDoctorService {
    private final Map<ChunkManager.ChunkCoord, Geometry> boundingBoxes = new HashMap<>();

    private ChunkManager chunkManager;
    private Node rootNode;
    private AssetManager assetManager;
    private boolean enabled;

    public void init(ChunkManager chunkManager, Node rootNode, AssetManager assetManager) {
        this.chunkManager = chunkManager;
        this.rootNode = rootNode;
        this.assetManager = assetManager;
    }

    public void toggle() {
        enabled = !enabled;
        update();
    }

    public void update() {
        if (!enabled) {
            boundingBoxes.values().forEach(box -> box.removeFromParent());
            boundingBoxes.clear();
            return;
        }

        Map<ChunkManager.ChunkCoord, Geometry> newBoxes = new HashMap<>();
        chunkManager.forEachChunk(entry -> {
            ChunkManager.ChunkCoord coord = entry.getKey();
            Geometry geometry = boundingBoxes.get(coord);
            if (geometry == null) {
                geometry = createBoundingBox(coord);
            } else if (geometry.getParent() == null) {
                rootNode.attachChild(geometry);
            }
            newBoxes.put(coord, geometry);
        });

        boundingBoxes.forEach((coord, geometry) -> {
            if (!newBoxes.containsKey(coord)) {
                geometry.removeFromParent();
            }
        });

        boundingBoxes.clear();
        boundingBoxes.putAll(newBoxes);
    }

    public ChunkDoctorStats getStats() {
        int loaded = chunkManager.getLoadedChunkCount();
        final int[] totals = new int[2];

        chunkManager.forEachChunk(entry -> {
            ChunkManager.ChunkCoord coord = entry.getKey();
            ChunkManager.ChunkStats stats = chunkManager.getChunkStats(coord);
            totals[0] += stats.vertices();
            totals[1] += stats.triangles();
        });

        float avgVertices = loaded == 0 ? 0f : (float) totals[0] / loaded;
        return new ChunkDoctorStats(loaded, totals[0], totals[1], avgVertices);
    }

    private Geometry createBoundingBox(ChunkManager.ChunkCoord coord) {
        float halfX = Chunk.SIZE_X / 2f;
        float halfY = Chunk.SIZE_Y / 2f;
        float halfZ = Chunk.SIZE_Z / 2f;

        WireBox wireBox = new WireBox(halfX, halfY, halfZ);
        Geometry geometry = new Geometry("chunk-bounds-" + coord.chunkX() + "." + coord.chunkZ(), wireBox);
        geometry.setLocalTranslation(new Vector3f(coord.chunkX() * Chunk.SIZE_X + halfX,
            halfY, coord.chunkZ() * Chunk.SIZE_Z + halfZ));
        geometry.setCullHint(Geometry.CullHint.Never);

        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.getAdditionalRenderState().setWireframe(true);
        material.setColor("Color", ColorRGBA.Cyan);
        geometry.setMaterial(material);

        rootNode.attachChild(geometry);
        return geometry;
    }

    public record ChunkDoctorStats(int loadedChunks, int totalVertices, int totalTriangles, float avgVerticesPerChunk) {
    }
}
