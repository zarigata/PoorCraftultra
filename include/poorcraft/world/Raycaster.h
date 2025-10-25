#pragma once

#include "poorcraft/world/Block.h"

#include <glm/glm.hpp>

namespace poorcraft::world {

class ChunkManager;

struct RaycastHit {
    bool hit{false};
    glm::ivec3 blockPosition{};            // Solid block intersected by the ray
    glm::ivec3 previousBlockPosition{};    // Last empty voxel visited before the hit (placement target)
    BlockType blockType{BlockType::Air};
    glm::vec3 hitPoint{};                  // World-space intersection point on the block face
    glm::vec3 normal{};                    // Outward facing normal of the impacted face (unit vector)
};

class Raycaster {
public:
    Raycaster() = delete;

    /**
     * Perform voxel raycasting using the Digital Differential Analyzer (DDA) algorithm.
     * The algorithm marches the ray through the voxel grid by stepping from boundary
     * to boundary, visiting cells in the exact order the ray crosses them. Each step
     * keeps track of the previously empty voxel so the caller can determine a valid
     * placement position adjacent to the hit block. The first solid block encountered
     * within maxDistance is returned; otherwise hit will be false.
     */
    static RaycastHit raycast(const glm::vec3& origin,
                              const glm::vec3& direction,
                              float maxDistance,
                              const ChunkManager& chunkManager);
};

} // namespace poorcraft::world
