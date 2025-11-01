package com.poorcraft.ultra.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.math.Vector3f;
import com.poorcraft.ultra.player.BlockPicker.BlockPickResult;
import com.poorcraft.ultra.voxel.BlockType;
import com.poorcraft.ultra.voxel.ChunkManager;
import com.poorcraft.ultra.voxel.Direction;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BlockPickerTest {

    private BlockPicker picker;
    private TestChunkManager chunkManager;

    @BeforeEach
    void setUp() {
        picker = new BlockPicker();
        chunkManager = new TestChunkManager();
        picker.init(chunkManager);
    }

    @Test
    void testPickBlockHit() {
        chunkManager.setBlock(0, 64, 0, BlockType.STONE);

        picker.updateCamera(new Vector3f(0.5f, 64.5f, 5f), new Vector3f(0f, 0f, -1f));
        BlockPickResult result = picker.pickBlock();

        assertNotNull(result, "Expected block hit");
        assertEquals(0, result.blockX());
        assertEquals(64, result.blockY());
        assertEquals(0, result.blockZ());
        assertEquals(Direction.SOUTH, result.face());
        assertTrue(result.distance() > 0f);
    }

    @Test
    void testPickBlockMiss() {
        picker.updateCamera(new Vector3f(0f, 64.5f, 5f), new Vector3f(0f, 0f, -1f));

        assertNull(picker.pickBlock(), "No blocks in layout should yield null");
    }

    @Test
    void testMaxDistanceRespected() {
        chunkManager.setBlock(0, 64, 0, BlockType.STONE);
        picker.setMaxDistance(5f);

        picker.updateCamera(new Vector3f(0f, 64.5f, 10f), new Vector3f(0f, 0f, -1f));
        assertNull(picker.pickBlock(), "Block beyond max distance must not be hit");
    }

    @Test
    void testFaceDetectionAllSides() {
        chunkManager.setBlock(0, 64, 0, BlockType.STONE);

        FaceScenario[] scenarios = new FaceScenario[] {
            new FaceScenario(new Vector3f(-1f, 64.5f, 0.5f), new Vector3f(1f, 0f, 0f), Direction.WEST),
            new FaceScenario(new Vector3f(2f, 64.5f, 0.5f), new Vector3f(-1f, 0f, 0f), Direction.EAST),
            new FaceScenario(new Vector3f(0.5f, 64.5f, -1f), new Vector3f(0f, 0f, 1f), Direction.NORTH),
            new FaceScenario(new Vector3f(0.5f, 64.5f, 2f), new Vector3f(0f, 0f, -1f), Direction.SOUTH),
            new FaceScenario(new Vector3f(0.5f, 65.5f, 0.5f), new Vector3f(0f, -1f, 0f), Direction.UP),
            new FaceScenario(new Vector3f(0.5f, 63.5f, 0.5f), new Vector3f(0f, 1f, 0f), Direction.DOWN)
        };

        for (FaceScenario scenario : scenarios) {
            picker.updateCamera(scenario.position(), scenario.direction());
            BlockPickResult result = picker.pickBlock();

            assertNotNull(result, () -> "Expected hit for scenario " + scenario);
            assertEquals(0, result.blockX());
            assertEquals(64, result.blockY());
            assertEquals(0, result.blockZ());
            assertEquals(scenario.expectedFace(), result.face(),
                () -> "Unexpected face for scenario " + scenario);
        }
    }

    @Test
    void testPickAccuracyInCluster() {
        for (int x = 0; x < 3; x++) {
            for (int y = 64; y < 67; y++) {
                for (int z = 0; z < 3; z++) {
                    chunkManager.setBlock(x, y, z, BlockType.STONE);
                }
            }
        }

        picker.updateCamera(new Vector3f(-1.5f, 65.5f, 1.5f), new Vector3f(1f, 0f, 0f));
        BlockPickResult result = picker.pickBlock();

        assertNotNull(result, "Cluster should be hittable");
        assertEquals(0, result.blockX(), "Ray must hit outermost block first");
        assertEquals(65, result.blockY());
        assertEquals(1, result.blockZ());
        assertEquals(Direction.WEST, result.face());
    }

    private record FaceScenario(Vector3f position, Vector3f direction, Direction expectedFace) {}

    private static class TestChunkManager extends ChunkManager {
        private final Map<BlockPos, BlockType> blocks = new HashMap<>();

        public void setBlock(int x, int y, int z, BlockType type) {
            BlockPos key = new BlockPos(x, y, z);
            if (type == BlockType.AIR) {
                blocks.remove(key);
            } else {
                blocks.put(key, type);
            }
        }

        @Override
        public BlockType getBlock(int worldX, int worldY, int worldZ) {
            return blocks.getOrDefault(new BlockPos(worldX, worldY, worldZ), BlockType.AIR);
        }
    }

    private record BlockPos(int x, int y, int z) {}
}
