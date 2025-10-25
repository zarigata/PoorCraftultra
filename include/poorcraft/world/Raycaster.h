#pragma once

#include "poorcraft/world/Block.h"

#include <glm/glm.hpp>

namespace poorcraft::world {

class ChunkManager;

struct RaycastHit {
    bool hit{false};
    glm::ivec3 blockPosition{};            // Solid block that was intersected
    glm::ivec3 previousBlockPosition{};    // Empty block immediately before the hit, used for placement
    BlockType blockType{BlockType::Air};
    glm::vec3 hitPoint{};                  // World-space hit position
    glm::vec3 normal{};                    // Normal of the face that was hit
};

class Raycaster {
public:
    Raycaster() = delete;

    /**
     * Perform voxel raycasting using the Digital Differential Analyzer (DDA) algorithm.
     * DDA walks through the voxel grid one block boundary at a time, advancing to the
     * next cell along the ray direction while tracking the last empty position. The
     * first solid block encountered within the given maxDistance is returned.
     */
    static RaycastHit raycast(const glm::vec3& origin,
                              const glm::vec3& direction,
                              float maxDistance,
                              const ChunkManager& chunkManager);
};

} // namespace poorcraft::world
