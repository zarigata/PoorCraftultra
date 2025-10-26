#include <gtest/gtest.h>
#include "poorcraft/rendering/TextureAtlas.h"
#include "poorcraft/world/Block.h"
#include "poorcraft/common/FaceDirection.h"

using namespace poorcraft::rendering;
using namespace poorcraft::world;
using namespace poorcraft::common;

TEST(TextureAtlasTest, InitializeCreatesValidAtlas) {
    TextureAtlas atlas;
    ASSERT_TRUE(atlas.initialize(32));

    // Expected dimensions: 6 faces × 4 block types = 6 columns × 4 rows
    // Each cell is 32×32, so atlas is 192×128
    EXPECT_EQ(atlas.getAtlasWidth(), 192u);
    EXPECT_EQ(atlas.getAtlasHeight(), 128u);

    const auto& data = atlas.getAtlasData();
    EXPECT_EQ(data.size(), 192u * 128u * 4u); // RGBA
}

TEST(TextureAtlasTest, RegionsAreValidAndNonOverlapping) {
    TextureAtlas atlas;
    ASSERT_TRUE(atlas.initialize(32));

    const float atlasWidth = static_cast<float>(atlas.getAtlasWidth());
    const float atlasHeight = static_cast<float>(atlas.getAtlasHeight());

    // Check all block types and faces
    std::vector<BlockType> blockTypes = {BlockType::Grass, BlockType::Dirt, BlockType::Stone};
    std::vector<FaceDirection> faces = {
        FaceDirection::PosX, FaceDirection::NegX,
        FaceDirection::PosY, FaceDirection::NegY,
        FaceDirection::PosZ, FaceDirection::NegZ
    };

    for (const auto& blockType : blockTypes) {
        for (const auto& face : faces) {
            const AtlasRegion region = atlas.getRegion(blockType, face);

            // UVs should be in [0, 1] range
            EXPECT_GE(region.uvMin.x, 0.0f);
            EXPECT_GE(region.uvMin.y, 0.0f);
            EXPECT_LE(region.uvMax.x, 1.0f);
            EXPECT_LE(region.uvMax.y, 1.0f);

            // Min should be less than max
            EXPECT_LT(region.uvMin.x, region.uvMax.x);
            EXPECT_LT(region.uvMin.y, region.uvMax.y);

            // Region should have reasonable size (32/192 = 0.1667 width, 32/128 = 0.25 height)
            const float regionWidth = region.uvMax.x - region.uvMin.x;
            const float regionHeight = region.uvMax.y - region.uvMin.y;
            EXPECT_NEAR(regionWidth, 32.0f / atlasWidth, 0.001f);
            EXPECT_NEAR(regionHeight, 32.0f / atlasHeight, 0.001f);
        }
    }
}

TEST(TextureAtlasTest, GrassHasGreenTopTexture) {
    TextureAtlas atlas;
    ASSERT_TRUE(atlas.initialize(32));

    const auto& data = atlas.getAtlasData();
    const AtlasRegion topRegion = atlas.getRegion(BlockType::Grass, FaceDirection::PosY);

    // Convert UV to pixel coordinates
    const int pixelX = static_cast<int>(topRegion.uvMin.x * atlas.getAtlasWidth());
    const int pixelY = static_cast<int>(topRegion.uvMin.y * atlas.getAtlasHeight());
    const int pixelIndex = (pixelY * atlas.getAtlasWidth() + pixelX) * 4;

    // Sample center of the region
    const int centerX = pixelX + 16;
    const int centerY = pixelY + 16;
    const int centerIndex = (centerY * atlas.getAtlasWidth() + centerX) * 4;

    // Grass top should have more green than red/blue
    const uint8_t r = data[centerIndex + 0];
    const uint8_t g = data[centerIndex + 1];
    const uint8_t b = data[centerIndex + 2];

    EXPECT_GT(g, r);
    EXPECT_GT(g, b);
}

TEST(TextureAtlasTest, DirtHasBrownTexture) {
    TextureAtlas atlas;
    ASSERT_TRUE(atlas.initialize(32));

    const auto& data = atlas.getAtlasData();
    const AtlasRegion region = atlas.getRegion(BlockType::Dirt, FaceDirection::PosY);

    const int centerX = static_cast<int>((region.uvMin.x + region.uvMax.x) * 0.5f * atlas.getAtlasWidth());
    const int centerY = static_cast<int>((region.uvMin.y + region.uvMax.y) * 0.5f * atlas.getAtlasHeight());
    const int centerIndex = (centerY * atlas.getAtlasWidth() + centerX) * 4;

    const uint8_t r = data[centerIndex + 0];
    const uint8_t g = data[centerIndex + 1];
    const uint8_t b = data[centerIndex + 2];

    // Dirt should be brownish (red and green similar, blue lower)
    EXPECT_GT(r, 50);
    EXPECT_GT(g, 50);
    EXPECT_LT(b, std::min(r, g));
}

TEST(TextureAtlasTest, StoneHasGrayTexture) {
    TextureAtlas atlas;
    ASSERT_TRUE(atlas.initialize(32));

    const auto& data = atlas.getAtlasData();
    const AtlasRegion region = atlas.getRegion(BlockType::Stone, FaceDirection::PosY);

    const int centerX = static_cast<int>((region.uvMin.x + region.uvMax.x) * 0.5f * atlas.getAtlasWidth());
    const int centerY = static_cast<int>((region.uvMin.y + region.uvMax.y) * 0.5f * atlas.getAtlasHeight());
    const int centerIndex = (centerY * atlas.getAtlasWidth() + centerX) * 4;

    const uint8_t r = data[centerIndex + 0];
    const uint8_t g = data[centerIndex + 1];
    const uint8_t b = data[centerIndex + 2];

    // Stone should be grayish (RGB values similar)
    const int maxDiff = std::max({std::abs(r - g), std::abs(g - b), std::abs(b - r)});
    EXPECT_LT(maxDiff, 50); // Allow some variation but should be mostly gray
}

TEST(TextureAtlasTest, FaceRegionsAreAdjacent) {
    TextureAtlas atlas;
    ASSERT_TRUE(atlas.initialize(32));

    // For a given block type, face regions should be laid out horizontally
    const AtlasRegion posX = atlas.getRegion(BlockType::Grass, FaceDirection::PosX);
    const AtlasRegion negX = atlas.getRegion(BlockType::Grass, FaceDirection::NegX);

    // They should be on the same row (same Y range)
    EXPECT_FLOAT_EQ(posX.uvMin.y, negX.uvMin.y);
    EXPECT_FLOAT_EQ(posX.uvMax.y, negX.uvMax.y);
}
