package com.poorcraftultra.world.chunk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ChunkManager Tests")
class ChunkManagerTest {

    private ChunkManager manager;

    @BeforeEach
    void setUp() {
        manager = new ChunkManager();
    }

    @Test
    @DisplayName("Load chunk creates and tracks chunk")
    void testLoadChunk() {
        ChunkPos pos = new ChunkPos(0, 0, 0);
        Chunk chunk = manager.loadChunk(pos);
        
        assertNotNull(chunk);
        assertTrue(manager.isChunkLoaded(pos));
        assertEquals(1, manager.getLoadedChunkCount());
    }

    @Test
    @DisplayName("Loading same chunk twice returns same instance")
    void testLoadSameChunkTwice() {
        ChunkPos pos = new ChunkPos(5, 5, 5);
        Chunk chunk1 = manager.loadChunk(pos);
        Chunk chunk2 = manager.loadChunk(pos);
        
        assertSame(chunk1, chunk2);
        assertEquals(1, manager.getLoadedChunkCount());
    }

    @Test
    @DisplayName("Unload chunk removes it from manager")
    void testUnloadChunk() {
        ChunkPos pos = new ChunkPos(1, 1, 1);
        manager.loadChunk(pos);
        
        Chunk unloaded = manager.unloadChunk(pos);
        
        assertNotNull(unloaded);
        assertNull(manager.getChunk(pos));
        assertEquals(0, manager.getLoadedChunkCount());
    }

    @Test
    @DisplayName("Get chunk at world coordinates")
    void testGetChunkAt() {
        ChunkPos pos = new ChunkPos(1, 0, 1);
        manager.loadChunk(pos);
        
        Chunk chunk = manager.getChunkAt(16, 64, 16);
        
        assertNotNull(chunk);
        assertEquals(pos, chunk.getPosition());
    }

    @Test
    @DisplayName("Multiple chunks can be loaded")
    void testMultipleChunks() {
        for (int i = 0; i < 10; i++) {
            manager.loadChunk(new ChunkPos(i, 0, 0));
        }
        
        assertEquals(10, manager.getLoadedChunkCount());
        
        for (int i = 0; i < 10; i++) {
            assertTrue(manager.isChunkLoaded(new ChunkPos(i, 0, 0)));
        }
    }

    @Test
    @DisplayName("Unload all chunks clears manager")
    void testUnloadAllChunks() {
        manager.loadChunk(new ChunkPos(0, 0, 0));
        manager.loadChunk(new ChunkPos(1, 0, 0));
        manager.loadChunk(new ChunkPos(0, 1, 0));
        
        manager.unloadAllChunks();
        
        assertEquals(0, manager.getLoadedChunkCount());
    }

    @Test
    @DisplayName("Get and set block using world coordinates")
    void testGetSetBlockWorldCoordinates() {
        manager.setBlock(10, 64, 10, (byte) 42);
        
        assertEquals(42, manager.getBlock(10, 64, 10));
        assertEquals(1, manager.getLoadedChunkCount());
    }

    @Test
    @DisplayName("Block access across chunk boundaries")
    void testBlockAccessAcrossChunks() {
        manager.setBlock(0, 64, 0, (byte) 1);   // Chunk (0,0,0)
        manager.setBlock(16, 64, 0, (byte) 2);  // Chunk (1,0,0)
        
        assertEquals(1, manager.getBlock(0, 64, 0));
        assertEquals(2, manager.getBlock(16, 64, 0));
        assertEquals(2, manager.getLoadedChunkCount());
    }

    @Test
    @DisplayName("Get neighbor returns correct adjacent chunk")
    void testGetNeighbor() {
        ChunkPos center = new ChunkPos(0, 0, 0);
        manager.loadChunk(center);
        manager.loadChunk(new ChunkPos(1, 0, 0));
        manager.loadChunk(new ChunkPos(-1, 0, 0));
        manager.loadChunk(new ChunkPos(0, 1, 0));
        manager.loadChunk(new ChunkPos(0, -1, 0));
        manager.loadChunk(new ChunkPos(0, 0, 1));
        manager.loadChunk(new ChunkPos(0, 0, -1));
        
        assertNotNull(manager.getNeighbor(center, 1, 0, 0));
        assertNotNull(manager.getNeighbor(center, -1, 0, 0));
        assertNotNull(manager.getNeighbor(center, 0, 1, 0));
        assertNotNull(manager.getNeighbor(center, 0, -1, 0));
        assertNotNull(manager.getNeighbor(center, 0, 0, 1));
        assertNotNull(manager.getNeighbor(center, 0, 0, -1));
    }

    @Test
    @DisplayName("Get neighbors returns only loaded neighbors")
    void testGetNeighborsPartiallyLoaded() {
        ChunkPos center = new ChunkPos(0, 0, 0);
        manager.loadChunk(center);
        manager.loadChunk(new ChunkPos(1, 0, 0));
        manager.loadChunk(new ChunkPos(0, 1, 0));
        
        List<Chunk> neighbors = manager.getNeighbors(center);
        
        assertEquals(2, neighbors.size());
    }

    @Test
    @DisplayName("Sparse world doesn't allocate intermediate chunks")
    void testSparseWorld() {
        manager.loadChunk(new ChunkPos(0, 0, 0));
        manager.loadChunk(new ChunkPos(100, 0, 100));
        
        assertEquals(2, manager.getLoadedChunkCount());
        
        assertNotNull(manager.getChunk(new ChunkPos(0, 0, 0)));
        assertNotNull(manager.getChunk(new ChunkPos(100, 0, 100)));
        assertNull(manager.getChunk(new ChunkPos(50, 0, 50)));
    }

    @Test
    @DisplayName("Memory efficiency with many empty chunks")
    void testMemoryEfficiency() {
        for (int i = 0; i < 100; i++) {
            manager.loadChunk(new ChunkPos(i, 0, 0));
        }
        
        assertEquals(100, manager.getLoadedChunkCount());
        
        for (Chunk chunk : manager.getLoadedChunks()) {
            assertTrue(chunk.isEmpty());
        }
    }

    @Test
    @DisplayName("Vertical chunk resolution across 256-block boundaries")
    void testVerticalChunkResolution() {
        // Load chunk at Y=1 (covers world Y 256-511)
        ChunkPos pos = new ChunkPos(0, 1, 0);
        manager.loadChunk(pos);
        
        // World Y=300 should map to chunk Y=1
        Chunk chunk = manager.getChunkAt(0, 300, 0);
        
        assertNotNull(chunk);
        assertEquals(1, chunk.getPosition().getY());
        
        // Set a block at Y=300 and verify it reads back
        manager.setBlock(0, 300, 0, (byte) 99);
        assertEquals(99, manager.getBlock(0, 300, 0));
        
        // Verify chunk Y=0 (world Y 0-255) is not affected
        assertEquals(0, manager.getBlock(0, 100, 0));
        assertNull(manager.getChunkAt(0, 100, 0));
    }
}
