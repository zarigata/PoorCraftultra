package com.poorcraft.ultra.world;

import com.poorcraft.ultra.voxel.Chunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiomeMapTest {

    private BiomeMap biomeMap;

    @BeforeEach
    void setUp() {
        biomeMap = new BiomeMap();
        biomeMap.init(54321L);
    }

    @Test
    void sameCoordinatesYieldSameBiome() {
        BiomeType first = biomeMap.getBiome(64, -32);
        BiomeType second = biomeMap.getBiome(64, -32);
        assertSame(first, second, "Biome selection should be deterministic");
    }

    @Test
    void biomeDistributionCoversAllTypes() {
        EnumSet<BiomeType> observed = EnumSet.noneOf(BiomeType.class);

        for (int x = -1024; x <= 1024; x += Chunk.SIZE_X) {
            for (int z = -1024; z <= 1024; z += Chunk.SIZE_Z) {
                observed.add(biomeMap.getBiome(x, z));
            }
        }

        assertTrue(observed.containsAll(EnumSet.allOf(BiomeType.class)),
            "Expected all biome types to appear across sampled chunks");
    }
}
