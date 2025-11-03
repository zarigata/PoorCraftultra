package com.poorcraft.ultra.world;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerrainGeneratorTest {

    private TerrainGenerator generator;
    private BiomeDefinition biome;

    @BeforeEach
    void setUp() {
        generator = new TerrainGenerator();
        generator.init(12345L);
        biome = BiomeDefinition.forType(BiomeType.PLAINS);
    }

    @Test
    void heightIsDeterministicForFixedSeed() {
        int first = generator.getHeight(100, -200, biome);
        int second = generator.getHeight(100, -200, biome);
        assertEquals(first, second, "Height should be deterministic for same coordinates and seed");
    }

    @Test
    void differentCoordinatesProduceDifferentHeights() {
        int reference = generator.getHeight(0, 0, biome);
        int offset = generator.getHeight(32, 32, biome);
        assertNotEquals(reference, offset, "Heights should vary across the terrain");
    }

    @Test
    void heightsStayWithinBiomeRange() {
        for (int x = -128; x <= 128; x += 16) {
            for (int z = -128; z <= 128; z += 16) {
                final int sampleX = x;
                final int sampleZ = z;
                int height = generator.getHeight(sampleX, sampleZ, biome);
                int min = biome.minHeight();
                int max = biome.minHeight() + biome.maxHeight();
                assertTrue(height >= min && height <= max,
                    () -> "Height out of range at (" + sampleX + "," + sampleZ + "): " + height);
            }
        }
    }
}
