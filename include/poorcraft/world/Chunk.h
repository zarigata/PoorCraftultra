#pragma once

#include "poorcraft/world/Block.h"

#include <array>
#include <cstddef>
#include <functional>

#include <glm/glm.hpp>

namespace poorcraft::world {

inline constexpr int CHUNK_SIZE_X = 16;
inline constexpr int CHUNK_SIZE_Y = 256;
inline constexpr int CHUNK_SIZE_Z = 16;
inline constexpr std::size_t CHUNK_VOLUME = static_cast<std::size_t>(CHUNK_SIZE_X) * static_cast<std::size_t>(CHUNK_SIZE_Y) * static_cast<std::size_t>(CHUNK_SIZE_Z);

struct ChunkPosition {
    int x{};
    int z{};

    friend bool operator==(const ChunkPosition& lhs, const ChunkPosition& rhs) {
        return lhs.x == rhs.x && lhs.z == rhs.z;
    }
};

struct ChunkPositionHash {
    std::size_t operator()(const ChunkPosition& position) const noexcept {
        std::size_t seed = static_cast<std::size_t>(position.x);
        seed ^= static_cast<std::size_t>(position.z) + 0x9e3779b9 + (seed << 6) + (seed >> 2);
        return seed;
    }
};

class Chunk {
public:
    explicit Chunk(ChunkPosition position);

    [[nodiscard]] BlockType getBlock(int x, int y, int z) const;
    void setBlock(int x, int y, int z, BlockType type);

    [[nodiscard]] ChunkPosition getPosition() const noexcept { return m_position; }
    [[nodiscard]] glm::vec3 getWorldPosition() const noexcept;

    [[nodiscard]] bool isDirty() const noexcept { return m_dirty; }
    void setDirty(bool dirty) noexcept { m_dirty = dirty; }

    [[nodiscard]] bool isGenerated() const noexcept { return m_generated; }
    void setGenerated(bool generated) noexcept { m_generated = generated; }

private:
    [[nodiscard]] std::size_t getBlockIndex(int x, int y, int z) const;

    ChunkPosition m_position{};
    std::array<BlockType, CHUNK_VOLUME> m_blocks{};
    bool m_dirty{false};
    bool m_generated{false};
};

} // namespace poorcraft::world
