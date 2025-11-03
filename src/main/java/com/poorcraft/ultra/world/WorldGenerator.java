package com.poorcraft.ultra.world;

import com.poorcraft.ultra.voxel.BlockType;
import com.poorcraft.ultra.voxel.Chunk;
import com.poorcraft.ultra.world.noise.FastNoiseLite;

/**
 * Coordinates biome, terrain, and cave generation for chunks.
 */
public final class WorldGenerator {
    private final TerrainGenerator terrainGenerator = new TerrainGenerator();
    private final BiomeMap biomeMap = new BiomeMap();
    private final CaveCarver caveCarver = new CaveCarver();
    private final FastNoiseLite oreNoise = new FastNoiseLite();
    private long seed;

    public void init(long seed) {
        this.seed = seed;
        terrainGenerator.init(seed);
        biomeMap.init(seed);
        caveCarver.init(seed);
        oreNoise.SetSeed((int) Math.abs(seed + 3000));
        oreNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
        oreNoise.SetFrequency(0.08f);
    }

    public long getSeed() {
        return seed;
    }

    public void generate(Chunk chunk) {
        if (chunk == null) {
            return;
        }

        int chunkX = chunk.chunkX();
        int chunkZ = chunk.chunkZ();

        for (int localX = 0; localX < Chunk.SIZE_X; localX++) {
            for (int localZ = 0; localZ < Chunk.SIZE_Z; localZ++) {
                int worldX = chunkX * Chunk.SIZE_X + localX;
                int worldZ = chunkZ * Chunk.SIZE_Z + localZ;

                BiomeDefinition biome = biomeMap.getBiomeDefinition(worldX, worldZ);
                int surfaceHeight = terrainGenerator.getHeight(worldX, worldZ, biome);

                for (int y = 0; y < Chunk.SIZE_Y; y++) {
                    if (y > surfaceHeight) {
                        chunk.setBlock(localX, y, localZ, BlockType.AIR);
                        break;
                    }

                    BlockType blockType;
                    if (y == 0) {
                        blockType = BlockType.STONE;
                    } else if (caveCarver.shouldCarve(worldX, y, worldZ, surfaceHeight)) {
                        blockType = BlockType.AIR;
                    } else if (y == surfaceHeight) {
                        blockType = biome.surfaceBlock();
                    } else if (y >= surfaceHeight - 3) {
                        blockType = biome.subsurfaceBlock();
                    } else {
                        blockType = biome.stoneBlock();
                    }

                    if (blockType == BlockType.STONE && y < surfaceHeight - 5 && y > 2) {
                        float oreSample = oreNoise.GetNoise(worldX, y, worldZ);
                        if (oreSample > 0.7f) {
                            blockType = BlockType.COAL_ORE;
                        }
                    }

                    chunk.setBlock(localX, y, localZ, blockType);
                }
            }
        }

        chunk.setModified(false);
        chunk.setDirty(true);
    }
}
