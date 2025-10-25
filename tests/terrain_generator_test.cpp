#include "poorcraft/world/TerrainGenerator.h"
#include "poorcraft/world/Block.h"
#include "poorcraft/world/Chunk.h"

#include <gtest/gtest.h>

using poorcraft::world::BlockType;
using poorcraft::world::CHUNK_SIZE_Y;
using poorcraft::world::Chunk;
using poorcraft::world::ChunkPosition;
using poorcraft::world::TerrainGenerator;

TEST(TerrainGeneratorTest, MarksChunkGeneratedAndDirty)
{
    TerrainGenerator generator(1234u);
    Chunk chunk({0, 0});

    EXPECT_FALSE(chunk.isGenerated());
    EXPECT_FALSE(chunk.isDirty());

    generator.generateChunk(chunk);

    EXPECT_TRUE(chunk.isGenerated());
    EXPECT_TRUE(chunk.isDirty());
}

TEST(TerrainGeneratorTest, ProducesLayeredTerrain)
{
    TerrainGenerator generator(5678u);
    Chunk chunk({0, 0});
    generator.generateChunk(chunk);

    const int worldX = static_cast<int>(chunk.getWorldPosition().x);
    const int worldZ = static_cast<int>(chunk.getWorldPosition().z);
    const int surfaceY = static_cast<int>(generator.getHeight(worldX, worldZ));

    ASSERT_GE(surfaceY, 0);
    ASSERT_LT(surfaceY, CHUNK_SIZE_Y);

    const int dirtStart = std::max(0, surfaceY - 3);

    EXPECT_EQ(chunk.getBlock(0, surfaceY, 0), BlockType::Grass);

    for(int y = dirtStart; y < surfaceY; ++y)
    {
        EXPECT_EQ(chunk.getBlock(0, y, 0), BlockType::Dirt);
    }

    for(int y = 0; y < dirtStart; ++y)
    {
        EXPECT_EQ(chunk.getBlock(0, y, 0), BlockType::Stone);
    }

    if(surfaceY + 1 < CHUNK_SIZE_Y)
    {
        EXPECT_EQ(chunk.getBlock(0, surfaceY + 1, 0), BlockType::Air);
    }
}
