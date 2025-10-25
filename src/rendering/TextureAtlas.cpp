#include "poorcraft/rendering/TextureAtlas.h"

#include <algorithm>
#include <array>
#include <cmath>
#include <random>

#include <glm/common.hpp>

namespace poorcraft::rendering {

namespace {
constexpr std::uint32_t FACE_COUNT = 6;
constexpr std::uint32_t BLOCK_TYPE_COUNT = 4;

std::uint32_t blockTypeIndex(world::BlockType type) {
    switch (type) {
    case world::BlockType::Grass:
        return 0;
    case world::BlockType::Dirt:
        return 1;
    case world::BlockType::Stone:
        return 2;
    default:
        return 3; // Other/placeholder
    }
}

std::uint32_t faceIndex(common::FaceDirection face) {
    return static_cast<std::uint32_t>(face);
}

std::mt19937::result_type seedFor(world::BlockType type, common::FaceDirection face) {
    return static_cast<std::mt19937::result_type>(blockTypeIndex(type) * 10 + faceIndex(face));
}
} // namespace

std::size_t TextureAtlas::RegionKeyHash::operator()(const RegionKey& key) const noexcept {
    const auto typeValue = static_cast<std::size_t>(key.first);
    const auto faceValue = static_cast<std::size_t>(key.second);
    return typeValue ^ (faceValue << 8U);
}

bool TextureAtlas::initialize(std::uint32_t textureSize) {
    if (textureSize == 0U) {
        return false;
    }

    m_atlasWidth = FACE_COUNT * textureSize;
    m_atlasHeight = BLOCK_TYPE_COUNT * textureSize;
    const std::size_t pixelCount = static_cast<std::size_t>(m_atlasWidth) * static_cast<std::size_t>(m_atlasHeight);
    m_atlasData.resize(pixelCount * 4U, 0U);

    packTextures(textureSize);

    return true;
}

AtlasRegion TextureAtlas::getRegion(world::BlockType blockType, common::FaceDirection face) const {
    const RegionKey key{blockType, face};
    if (auto it = m_regions.find(key); it != m_regions.end()) {
        return it->second;
    }
    return AtlasRegion{glm::vec2(0.0f), glm::vec2(1.0f)};
}

void TextureAtlas::generateGrassTexture(
    std::uint8_t* pixels,
    std::uint32_t size,
    common::FaceDirection face
) const {
    const std::array<std::uint8_t, 4> dirtColor{120U, 72U, 38U, 255U};
    const std::array<std::uint8_t, 4> grassColorTop{102U, 188U, 88U, 255U};
    const std::array<std::uint8_t, 4> grassColorSide{92U, 178U, 78U, 255U};

    const bool isTop = face == common::FaceDirection::PosY;
    const bool isBottom = face == common::FaceDirection::NegY;

    std::mt19937 rng(seedFor(world::BlockType::Grass, face));
    std::uniform_int_distribution<int> noise(-10, 10);

    for (std::uint32_t y = 0; y < size; ++y) {
        for (std::uint32_t x = 0; x < size; ++x) {
            std::array<std::uint8_t, 4> color{};
            if (isBottom) {
                color = dirtColor;
            } else if (isTop) {
                color = grassColorTop;
            } else {
                const bool dirtBand = y > size - 8U;
                color = dirtBand ? dirtColor : grassColorSide;
            }

            const int n = noise(rng);
            const std::size_t idx = static_cast<std::size_t>(y * size + x) * 4U;
            for (int c = 0; c < 3; ++c) {
                const int component = static_cast<int>(color[c]) + n;
                pixels[idx + c] = static_cast<std::uint8_t>(std::clamp(component, 0, 255));
            }
            pixels[idx + 3] = color[3];
        }
    }
}

void TextureAtlas::generateDirtTexture(std::uint8_t* pixels, std::uint32_t size) const {
    const std::array<std::uint8_t, 4> dirtColor{139U, 90U, 43U, 255U};

    std::mt19937 rng(seedFor(world::BlockType::Dirt, common::FaceDirection::PosY));
    std::uniform_int_distribution<int> noise(-12, 12);

    for (std::uint32_t y = 0; y < size; ++y) {
        for (std::uint32_t x = 0; x < size; ++x) {
            const int n = noise(rng);
            const std::size_t idx = static_cast<std::size_t>(y * size + x) * 4U;
            for (int c = 0; c < 3; ++c) {
                const int component = static_cast<int>(dirtColor[c]) + n;
                pixels[idx + c] = static_cast<std::uint8_t>(std::clamp(component, 0, 255));
            }
            pixels[idx + 3] = dirtColor[3];
        }
    }
}

void TextureAtlas::generateStoneTexture(std::uint8_t* pixels, std::uint32_t size) const {
    const std::array<std::uint8_t, 4> stoneColor{132U, 132U, 132U, 255U};

    std::mt19937 rng(seedFor(world::BlockType::Stone, common::FaceDirection::PosY));
    std::uniform_int_distribution<int> noise(-15, 15);

    for (std::uint32_t y = 0; y < size; ++y) {
        for (std::uint32_t x = 0; x < size; ++x) {
            const int n = noise(rng);
            const std::size_t idx = static_cast<std::size_t>(y * size + x) * 4U;
            for (int c = 0; c < 3; ++c) {
                const int component = static_cast<int>(stoneColor[c]) + n;
                pixels[idx + c] = static_cast<std::uint8_t>(std::clamp(component, 0, 255));
            }
            pixels[idx + 3] = stoneColor[3];
        }
    }
}

void TextureAtlas::packTextures(std::uint32_t textureSize) {
    m_regions.clear();

    std::vector<std::uint8_t> facePixels(static_cast<std::size_t>(textureSize) * textureSize * 4U);

    const std::array<world::BlockType, BLOCK_TYPE_COUNT> blocks{
        world::BlockType::Grass,
        world::BlockType::Dirt,
        world::BlockType::Stone,
        world::BlockType::Stone
    };

    for (std::uint32_t blockIdx = 0; blockIdx < BLOCK_TYPE_COUNT; ++blockIdx) {
        for (std::uint32_t faceIdx = 0; faceIdx < FACE_COUNT; ++faceIdx) {
            const world::BlockType blockType = blocks[blockIdx];
            const common::FaceDirection face = static_cast<common::FaceDirection>(faceIdx);

            switch (blockType) {
            case world::BlockType::Grass:
                generateGrassTexture(facePixels.data(), textureSize, face);
                break;
            case world::BlockType::Dirt:
                generateDirtTexture(facePixels.data(), textureSize);
                break;
            case world::BlockType::Stone:
            default:
                generateStoneTexture(facePixels.data(), textureSize);
                break;
            }

            const std::uint32_t xOffset = faceIdx * textureSize;
            const std::uint32_t yOffset = blockIdx * textureSize;

            for (std::uint32_t y = 0; y < textureSize; ++y) {
                const std::size_t dstRow = static_cast<std::size_t>(yOffset + y) * m_atlasWidth;
                const std::size_t srcRow = static_cast<std::size_t>(y) * textureSize;
                for (std::uint32_t x = 0; x < textureSize; ++x) {
                    const std::size_t dstIdx = (dstRow + xOffset + x) * 4U;
                    const std::size_t srcIdx = (srcRow + x) * 4U;
                    m_atlasData[dstIdx + 0] = facePixels[srcIdx + 0];
                    m_atlasData[dstIdx + 1] = facePixels[srcIdx + 1];
                    m_atlasData[dstIdx + 2] = facePixels[srcIdx + 2];
                    m_atlasData[dstIdx + 3] = facePixels[srcIdx + 3];
                }
            }

            const glm::vec2 uvMin{
                static_cast<float>(xOffset) / static_cast<float>(m_atlasWidth),
                static_cast<float>(yOffset) / static_cast<float>(m_atlasHeight)
            };
            const glm::vec2 uvMax{
                static_cast<float>(xOffset + textureSize) / static_cast<float>(m_atlasWidth),
                static_cast<float>(yOffset + textureSize) / static_cast<float>(m_atlasHeight)
            };

            m_regions.emplace(RegionKey{blockType, face}, AtlasRegion{uvMin, uvMax});
        }
    }
}

} // namespace poorcraft::rendering
