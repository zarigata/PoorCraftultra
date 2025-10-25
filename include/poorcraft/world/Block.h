#pragma once

#include <cstdint>

namespace poorcraft::world {

enum class BlockType : std::uint8_t {
    Air = 0,
    Grass,
    Dirt,
    Stone,
};

inline constexpr float BLOCK_SIZE = 1.0f;

namespace block {

bool isSolid(BlockType type);
bool isOpaque(BlockType type);
const char* getName(BlockType type);

} // namespace block

} // namespace poorcraft::world
