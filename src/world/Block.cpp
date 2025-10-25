#include "poorcraft/world/Block.h"

namespace poorcraft::world::block {

bool isSolid(BlockType type) {
    switch (type) {
    case BlockType::Air:
        return false;
    case BlockType::Grass:
    case BlockType::Dirt:
    case BlockType::Stone:
        return true;
    default:
        return false;
    }
}

bool isOpaque(BlockType type) {
    return isSolid(type);
}

const char* getName(BlockType type) {
    switch (type) {
    case BlockType::Air:
        return "Air";
    case BlockType::Grass:
        return "Grass";
    case BlockType::Dirt:
        return "Dirt";
    case BlockType::Stone:
        return "Stone";
    default:
        return "Unknown";
    }
}

} // namespace poorcraft::world::block
