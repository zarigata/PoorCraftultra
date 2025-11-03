package com.poorcraft.ultra.world;

import com.poorcraft.ultra.voxel.BlockType;
import com.poorcraft.ultra.voxel.Chunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorldGeneratorTest {

    @Test
    void worldGenerationIsDeterministicForSeed() {
        long seed = 42L;
        WorldGenerator generatorFirst = new WorldGenerator();
        generatorFirst.init(seed);

        List<Point> samplePoints = List.of(
            new Point(0, 0),
            new Point(8, 8),
            new Point(15, 15),
            new Point(-1, -1),
            new Point(-16, -16),
            new Point(32, -24),
            new Point(48, 12),
            new Point(-33, 47),
            new Point(127, -95),
            new Point(-64, 96),
            new Point(5, -58),
            new Point(-120, 17)
        );

        List<SurfaceSample> firstSamples = samplePoints.stream()
            .map(point -> sampleSurface(generatorFirst, point.x(), point.z()))
            .toList();

        WorldGenerator generatorSecond = new WorldGenerator();
        generatorSecond.init(seed);

        List<SurfaceSample> secondSamples = samplePoints.stream()
            .map(point -> sampleSurface(generatorSecond, point.x(), point.z()))
            .toList();

        for (int i = 0; i < samplePoints.size(); i++) {
            SurfaceSample first = firstSamples.get(i);
            SurfaceSample second = secondSamples.get(i);
            Point point = samplePoints.get(i);

            assertEquals(first.height(), second.height(),
                () -> "Surface height mismatch at " + point);
            assertEquals(first.topBlock(), second.topBlock(),
                () -> "Surface block mismatch at " + point);
        }
    }

    private SurfaceSample sampleSurface(WorldGenerator generator, int worldX, int worldZ) {
        int chunkX = Math.floorDiv(worldX, Chunk.SIZE_X);
        int chunkZ = Math.floorDiv(worldZ, Chunk.SIZE_Z);
        Chunk chunk = new Chunk(chunkX, chunkZ);
        generator.generate(chunk);

        int localX = Math.floorMod(worldX, Chunk.SIZE_X);
        int localZ = Math.floorMod(worldZ, Chunk.SIZE_Z);

        for (int y = Chunk.SIZE_Y - 1; y >= 0; y--) {
            BlockType type = chunk.getBlock(localX, y, localZ);
            if (type != BlockType.AIR) {
                return new SurfaceSample(y, type);
            }
        }
        return new SurfaceSample(-1, BlockType.AIR);
    }

    private record Point(int x, int z) { }

    private record SurfaceSample(int height, BlockType topBlock) { }
}
