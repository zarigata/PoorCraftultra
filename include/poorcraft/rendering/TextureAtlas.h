#pragma once

#include <cstdint>
#include <unordered_map>
#include <utility>
#include <vector>

#include <glm/vec2.hpp>

#include "poorcraft/world/Block.h"
#include "poorcraft/world/ChunkMesher.h"

namespace poorcraft::rendering {

struct AtlasRegion {
    glm::vec2 uvMin{};
    glm::vec2 uvMax{};
};

class TextureAtlas {
public:
    TextureAtlas() = default;

    bool initialize(std::uint32_t textureSize);

    // Layout: 6 columns (faces) by 4 rows (block types), each cell textureSize × textureSize pixels.

    [[nodiscard]] AtlasRegion getRegion(
        world::BlockType blockType,
        world::ChunkMesher::FaceDirection face
    ) const;

    [[nodiscard]] const std::vector<std::uint8_t>& getAtlasData() const noexcept { return m_atlasData; }
    [[nodiscard]] std::uint32_t getAtlasWidth() const noexcept { return m_atlasWidth; }
    [[nodiscard]] std::uint32_t getAtlasHeight() const noexcept { return m_atlasHeight; }

private:
    using RegionKey = std::pair<world::BlockType, world::ChunkMesher::FaceDirection>;

    struct RegionKeyHash {
        std::size_t operator()(const RegionKey& key) const noexcept;
    };

    void generateGrassTexture(std::uint8_t* pixels, std::uint32_t size, world::ChunkMesher::FaceDirection face) const;
    void generateDirtTexture(std::uint8_t* pixels, std::uint32_t size) const;
    void generateStoneTexture(std::uint8_t* pixels, std::uint32_t size) const;

    void packTextures(std::uint32_t textureSize);

    std::vector<std::uint8_t> m_atlasData;
    std::uint32_t m_atlasWidth{0};
    std::uint32_t m_atlasHeight{0};
    std::unordered_map<RegionKey, AtlasRegion, RegionKeyHash> m_regions;
};

} // namespace poorcraft::rendering
