#include "poorcraft/world/Block.h"

#include <gtest/gtest.h>

using poorcraft::world::BlockType;
using poorcraft::world::block::getName;
using poorcraft::world::block::isOpaque;
using poorcraft::world::block::isSolid;

TEST(BlockTest, SolidBlocks)
{
    EXPECT_FALSE(isSolid(BlockType::Air));
    EXPECT_TRUE(isSolid(BlockType::Grass));
    EXPECT_TRUE(isSolid(BlockType::Dirt));
    EXPECT_TRUE(isSolid(BlockType::Stone));
}

TEST(BlockTest, OpaqueMatchesSolid)
{
    for(int value = static_cast<int>(BlockType::Air); value <= static_cast<int>(BlockType::Stone); ++value)
    {
        const auto type = static_cast<BlockType>(value);
        EXPECT_EQ(isOpaque(type), isSolid(type));
    }
}

TEST(BlockTest, GetName)
{
    EXPECT_STREQ("Air", getName(BlockType::Air));
    EXPECT_STREQ("Grass", getName(BlockType::Grass));
    EXPECT_STREQ("Dirt", getName(BlockType::Dirt));
    EXPECT_STREQ("Stone", getName(BlockType::Stone));
}
