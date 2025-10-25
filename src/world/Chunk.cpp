#include "poorcraft/world/Chunk.h"

#include <algorithm>
#include <stdexcept>

namespace poorcraft::world {
namespace {
[[nodiscard]] bool inBounds(int value, int minInclusive, int maxExclusive) {
    return value >= minInclusive && value < maxExclusive;
}
}

Chunk::Chunk(ChunkPosition position)
    : m_position(position) {
    m_blocks.fill(BlockType::Air);
}

std::size_t Chunk::getBlockIndex(int x, int y, int z) const {
    if (!inBounds(x, 0, CHUNK_SIZE_X) || !inBounds(y, 0, CHUNK_SIZE_Y) || !inBounds(z, 0, CHUNK_SIZE_Z)) {
        throw std::out_of_range("Chunk block coordinates out of bounds");
    }

    const auto ux = static_cast<std::size_t>(x);
    const auto uy = static_cast<std::size_t>(y);
    const auto uz = static_cast<std::size_t>(z);
    return ux + uz * static_cast<std::size_t>(CHUNK_SIZE_X) + uy * static_cast<std::size_t>(CHUNK_SIZE_X * CHUNK_SIZE_Z);
}

BlockType Chunk::getBlock(int x, int y, int z) const {
    if (!inBounds(x, 0, CHUNK_SIZE_X) || !inBounds(y, 0, CHUNK_SIZE_Y) || !inBounds(z, 0, CHUNK_SIZE_Z)) {
        return BlockType::Air;
    }
    return m_blocks[getBlockIndex(x, y, z)];
}

void Chunk::setBlock(int x, int y, int z, BlockType type) {
    if (!inBounds(x, 0, CHUNK_SIZE_X) || !inBounds(y, 0, CHUNK_SIZE_Y) || !inBounds(z, 0, CHUNK_SIZE_Z)) {
        return;
    }

    const auto index = getBlockIndex(x, y, z);
    if (m_blocks[index] != type) {
        m_blocks[index] = type;
        m_dirty = true;
    }
}

glm::vec3 Chunk::getWorldPosition() const noexcept {
    return glm::vec3(
        static_cast<float>(m_position.x * CHUNK_SIZE_X),
        0.0f,
        static_cast<float>(m_position.z * CHUNK_SIZE_Z)
    );
}

} // namespace poorcraft::world
