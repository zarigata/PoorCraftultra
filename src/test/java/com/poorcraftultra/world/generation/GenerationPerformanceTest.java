package com.poorcraftultra.world.generation;

import com.poorcraftultra.world.block.BlockRegistry;
import com.poorcraftultra.world.chunk.Chunk;
import com.poorcraftultra.world.chunk.ChunkPos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Generation Performance Tests")
@Tag("performance")
class GenerationPerformanceTest {
    
    @BeforeAll
    static void setup() {
        BlockRegistry.getInstance();
    }
    
    @Test
    @DisplayName("Chunk generation performance < 5ms average")
    @Timeout(30)
    void testChunkGenerationPerformance() {
        WorldGenerator generator = WorldGenerator.createDefault(12345L);
        
        // Warm up JVM
        for (int i = 0; i < 10; i++) {
            Chunk chunk = new Chunk(new ChunkPos(i, 0, i));
            generator.generateChunk(chunk);
        }
        
        // Measure performance
        int iterations = 100;
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            Chunk chunk = new Chunk(new ChunkPos(i % 10, 0, i / 10));
            generator.generateChunk(chunk);
        }
        
        long endTime = System.nanoTime();
        double totalMs = (endTime - startTime) / 1_000_000.0;
        double avgMs = totalMs / iterations;
        
        System.out.printf("Generated %d chunks in %.2f ms (avg: %.2f ms per chunk)%n", 
            iterations, totalMs, avgMs);
        
        assertTrue(avgMs < 5.0, "Average generation time should be < 5ms, was: " + avgMs + "ms");
    }
    
    @Test
    @DisplayName("Terrain-only generation performance")
    @Timeout(30)
    void testTerrainOnlyPerformance() {
        WorldGenerator generator = WorldGenerator.createDefault(12345L);
        generator.setGenerateCaves(false);
        
        // Warm up
        for (int i = 0; i < 10; i++) {
            Chunk chunk = new Chunk(new ChunkPos(i, 0, i));
            generator.generateChunk(chunk);
        }
        
        // Measure
        int iterations = 100;
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            Chunk chunk = new Chunk(new ChunkPos(i % 10, 0, i / 10));
            generator.generateChunk(chunk);
        }
        
        long endTime = System.nanoTime();
        double avgMs = (endTime - startTime) / 1_000_000.0 / iterations;
        
        System.out.printf("Terrain-only average: %.2f ms per chunk%n", avgMs);
        
        assertTrue(avgMs < 3.0, "Terrain-only should be faster than full generation");
    }
    
    @Test
    @DisplayName("Cave carving performance")
    @Timeout(30)
    void testCaveOnlyPerformance() {
        CaveGenerator caveGen = CaveGenerator.createDefault(12345L);
        
        // Pre-fill chunks with stone
        Chunk[] chunks = new Chunk[100];
        byte stoneId = BlockRegistry.getInstance().getBlock("stone").getId();
        for (int i = 0; i < 100; i++) {
            chunks[i] = new Chunk(new ChunkPos(i % 10, 0, i / 10));
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 256; y++) {
                    for (int z = 0; z < 16; z++) {
                        chunks[i].setBlock(x, y, z, stoneId);
                    }
                }
            }
        }
        
        // Warm up
        for (int i = 0; i < 10; i++) {
            caveGen.carveCaves(chunks[i]);
        }
        
        // Measure
        long startTime = System.nanoTime();
        for (int i = 10; i < 100; i++) {
            caveGen.carveCaves(chunks[i]);
        }
        long endTime = System.nanoTime();
        
        double avgMs = (endTime - startTime) / 1_000_000.0 / 90;
        System.out.printf("Cave carving average: %.2f ms per chunk%n", avgMs);
        
        assertTrue(avgMs < 3.0, "Cave carving should be reasonably fast");
    }
    
    @Test
    @DisplayName("Noise sampling performance")
    @Timeout(30)
    void testNoisePerformance() {
        PerlinNoise perlin = new PerlinNoise(12345L);
        SimplexNoise simplex = new SimplexNoise(12345L);
        
        int samples = 1_000_000;
        
        // Warm up
        for (int i = 0; i < 10000; i++) {
            perlin.noise2D(i * 0.1, i * 0.2);
            simplex.noise3D(i * 0.1, i * 0.15, i * 0.2);
        }
        
        // Measure Perlin 2D
        long start = System.nanoTime();
        for (int i = 0; i < samples; i++) {
            perlin.noise2D(i * 0.1, i * 0.2);
        }
        long perlin2DTime = System.nanoTime() - start;
        
        // Measure Simplex 3D
        start = System.nanoTime();
        for (int i = 0; i < samples; i++) {
            simplex.noise3D(i * 0.1, i * 0.15, i * 0.2);
        }
        long simplex3DTime = System.nanoTime() - start;
        
        double perlin2DRate = samples / (perlin2DTime / 1_000_000_000.0);
        double simplex3DRate = samples / (simplex3DTime / 1_000_000_000.0);
        
        System.out.printf("Perlin 2D: %.2f M samples/sec%n", perlin2DRate / 1_000_000);
        System.out.printf("Simplex 3D: %.2f M samples/sec%n", simplex3DRate / 1_000_000);
        
        assertTrue(perlin2DRate > 1_000_000, "Should achieve > 1M samples/sec");
    }
    
    @Test
    @DisplayName("Octave scaling performance")
    @Timeout(30)
    void testOctaveScaling() {
        PerlinNoise perlin = new PerlinNoise(12345L);
        
        for (int octaves = 1; octaves <= 8; octaves *= 2) {
            OctaveNoise noise = new OctaveNoise(perlin, octaves, 0.01, 1.0, 2.0, 0.5);
            
            // Warm up
            for (int i = 0; i < 1000; i++) {
                noise.sample2D(i * 0.1, i * 0.2);
            }
            
            // Measure
            int samples = 100_000;
            long start = System.nanoTime();
            for (int i = 0; i < samples; i++) {
                noise.sample2D(i * 0.1, i * 0.2);
            }
            long time = System.nanoTime() - start;
            
            double rate = samples / (time / 1_000_000_000.0);
            System.out.printf("%d octaves: %.2f K samples/sec%n", octaves, rate / 1000);
        }
        
        // Just verify it completes without timing out
        assertTrue(true);
    }
    
    @Test
    @DisplayName("Large world generation scalability")
    @Timeout(60)
    void testLargeWorldGeneration() {
        WorldGenerator generator = WorldGenerator.createDefault(12345L);
        
        int gridSize = 16; // 16x16 = 256 chunks
        long startTime = System.nanoTime();
        
        for (int cx = 0; cx < gridSize; cx++) {
            for (int cz = 0; cz < gridSize; cz++) {
                Chunk chunk = new Chunk(new ChunkPos(cx, 0, cz));
                generator.generateChunk(chunk);
            }
        }
        
        long endTime = System.nanoTime();
        double totalSeconds = (endTime - startTime) / 1_000_000_000.0;
        int totalChunks = gridSize * gridSize;
        double avgMs = (totalSeconds * 1000) / totalChunks;
        
        System.out.printf("Generated %dx%d chunks (%d total) in %.2f seconds (avg: %.2f ms per chunk)%n",
            gridSize, gridSize, totalChunks, totalSeconds, avgMs);
        
        assertTrue(avgMs < 10.0, "Should maintain reasonable performance for large worlds");
    }
}
