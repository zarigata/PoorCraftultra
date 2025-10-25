#include "poorcraft/world/Chunk.h"

#include <gtest/gtest.h>

using poorcraft::world::BlockType;
using poorcraft::world::CHUNK_SIZE_X;
using poorcraft::world::CHUNK_SIZE_Z;
using poorcraft::world::Chunk;
using poorcraft::world::ChunkPosition;

namespace
{
constexpr ChunkPosition kOriginPos{0, 0};
}

TEST(ChunkTest, BlocksDefaultToAir)
{
    Chunk chunk(kOriginPos);

    EXPECT_EQ(chunk.getBlock(0, 0, 0), BlockType::Air);
    EXPECT_EQ(chunk.getBlock(CHUNK_SIZE_X - 1, 0, CHUNK_SIZE_Z - 1), BlockType::Air);
}

TEST(ChunkTest, SetBlockMarksDirtyAndStoresType)
{
    Chunk chunk(kOriginPos);

    EXPECT_FALSE(chunk.isDirty());
    chunk.setBlock(1, 2, 3, BlockType::Stone);
    EXPECT_EQ(chunk.getBlock(1, 2, 3), BlockType::Stone);
    EXPECT_TRUE(chunk.isDirty());

    chunk.setDirty(false);
    chunk.setBlock(1, 2, 3, BlockType::Stone);
    EXPECT_FALSE(chunk.isDirty());
}

TEST(ChunkTest, WorldPositionMatchesChunkPosition)
{
    constexpr ChunkPosition kPos{2, -3};
    Chunk chunk(kPos);

    const auto worldPos = chunk.getWorldPosition();
    EXPECT_FLOAT_EQ(worldPos.x, static_cast<float>(kPos.x * CHUNK_SIZE_X));
    EXPECT_FLOAT_EQ(worldPos.y, 0.0f);
    EXPECT_FLOAT_EQ(worldPos.z, static_cast<float>(kPos.z * CHUNK_SIZE_Z));
}
