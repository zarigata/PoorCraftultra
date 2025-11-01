package com.poorcraft.ultra.voxel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ChunkTest {

    private Chunk chunk;

    @BeforeEach
    void setUp() {
        chunk = new Chunk(0, 0);
    }

    @Test
    void testChunkInitialization() {
        for (int x = 0; x < Chunk.SIZE_X; x++) {
            for (int y = 0; y < Chunk.SIZE_Y; y++) {
                for (int z = 0; z < Chunk.SIZE_Z; z++) {
                    assertEquals(BlockType.AIR, chunk.getBlock(x, y, z));
                }
            }
        }
    }

    @Test
    void testSetGetBlock() {
        chunk.setBlock(5, 64, 7, BlockType.STONE);
        assertEquals(BlockType.STONE, chunk.getBlock(5, 64, 7));

        chunk.setBlock(5, 64, 7, BlockType.AIR);
        assertEquals(BlockType.AIR, chunk.getBlock(5, 64, 7));
    }

    @Test
    void testOutOfBoundsGetReturnsAir() {
        assertEquals(BlockType.AIR, chunk.getBlock(-1, 0, 0));
        assertEquals(BlockType.AIR, chunk.getBlock(Chunk.SIZE_X, 0, 0));
        assertEquals(BlockType.AIR, chunk.getBlock(0, -1, 0));
        assertEquals(BlockType.AIR, chunk.getBlock(0, Chunk.SIZE_Y, 0));
    }

    @Test
    void testOutOfBoundsSetThrows() {
        assertThrows(IllegalArgumentException.class, () -> chunk.setBlock(-1, 0, 0, BlockType.STONE));
        assertThrows(IllegalArgumentException.class, () -> chunk.setBlock(Chunk.SIZE_X, 0, 0, BlockType.STONE));
        assertThrows(IllegalArgumentException.class, () -> chunk.setBlock(0, -1, 0, BlockType.STONE));
        assertThrows(IllegalArgumentException.class, () -> chunk.setBlock(0, Chunk.SIZE_Y, 0, BlockType.STONE));
    }

    @Test
    void testToIndexFromIndexRoundTrip() {
        int x = 3;
        int y = 100;
        int z = 7;
        int index = Chunk.toIndex(x, y, z);
        int[] coords = Chunk.fromIndex(index);
        assertEquals(x, coords[0]);
        assertEquals(y, coords[1]);
        assertEquals(z, coords[2]);
    }

    @Test
    void testFillCheckerboard() {
        chunk.fillCheckerboard();
        int y = 64;
        for (int x = 0; x < Chunk.SIZE_X; x++) {
            for (int z = 0; z < Chunk.SIZE_Z; z++) {
                BlockType expected = ((x + z) & 1) == 0 ? BlockType.STONE : BlockType.DIRT;
                String message = String.format("Unexpected block at (%d,%d,%d)", x, y, z);
                assertEquals(expected, chunk.getBlock(x, y, z), message);
                assertEquals(BlockType.AIR, chunk.getBlock(x, y + 1, z));
                assertEquals(BlockType.AIR, chunk.getBlock(x, y - 1, z));
            }
        }
    }

    @Test
    void testDirtyFlag() {
        chunk.setDirty(false);
        assertFalse(chunk.isDirty());

        chunk.setBlock(1, 64, 1, BlockType.DIRT);
        assertTrue(chunk.isDirty());

        chunk.setDirty(false);
        assertFalse(chunk.isDirty());
    }

    @Test
    void testGetBlockData() {
        chunk.setBlock(5, 64, 7, BlockType.STONE);
        byte[] data = chunk.getBlockData();

        data[0] = (byte) 99;
        assertNotEquals((byte) 99, chunk.getBlockId(0, 0, 0));
        assertEquals(Chunk.VOLUME, data.length);
        assertEquals(BlockType.STONE.id(), data[Chunk.toIndex(5, 64, 7)]);
    }

    @Test
    void testSetBlockData() {
        byte[] data = new byte[Chunk.VOLUME];
        Arrays.fill(data, BlockType.DIRT.id());
        data[Chunk.toIndex(3, 100, 7)] = BlockType.STONE.id();

        chunk.setBlockData(data);

        assertEquals(BlockType.DIRT, chunk.getBlock(0, 0, 0));
        assertEquals(BlockType.STONE, chunk.getBlock(3, 100, 7));
        assertTrue(chunk.isDirty());
    }

    @Test
    void testSetBlockDataInvalidLength() {
        byte[] data = new byte[1000];
        assertThrows(IllegalArgumentException.class, () -> chunk.setBlockData(data));
    }

    @Test
    void testGetSetBlockDataRoundTrip() {
        chunk.setBlock(1, 64, 2, BlockType.STONE);
        chunk.setBlock(5, 100, 7, BlockType.GRASS);
        chunk.setBlock(10, 200, 15, BlockType.WOOD_OAK);

        byte[] data = chunk.getBlockData();

        Chunk newChunk = new Chunk(0, 0);
        newChunk.setBlockData(data);

        assertEquals(BlockType.STONE, newChunk.getBlock(1, 64, 2));
        assertEquals(BlockType.GRASS, newChunk.getBlock(5, 100, 7));
        assertEquals(BlockType.WOOD_OAK, newChunk.getBlock(10, 200, 15));
    }
}
