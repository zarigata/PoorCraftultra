#include "poorcraft/world/Raycaster.h"

#include "poorcraft/world/Block.h"
#include "poorcraft/world/ChunkManager.h"

#include <algorithm>
#include <cmath>
#include <limits>

#include <glm/gtx/norm.hpp>

namespace poorcraft::world {

namespace {
constexpr float MAX_ALLOWED_DISTANCE = 10.0f;
} // namespace

RaycastHit Raycaster::raycast(const glm::vec3& origin,
                              const glm::vec3& direction,
                              float maxDistance,
                              const ChunkManager& chunkManager) {
    RaycastHit result{};

    if (maxDistance <= 0.0f) {
        return result;
    }

    const glm::vec3 dir = glm::normalize(direction);
    if (glm::length2(dir) == 0.0f) {
        return result;
    }

    const float clampedDistance = std::min(maxDistance, MAX_ALLOWED_DISTANCE);

    glm::ivec3 currentBlock = glm::floor(origin);
    glm::ivec3 previousBlock = currentBlock;

    const int stepX = (dir.x > 0.0f) - (dir.x < 0.0f);
    const int stepY = (dir.y > 0.0f) - (dir.y < 0.0f);
    const int stepZ = (dir.z > 0.0f) - (dir.z < 0.0f);

    const float invDirX = (dir.x != 0.0f) ? 1.0f / dir.x : std::numeric_limits<float>::infinity();
    const float invDirY = (dir.y != 0.0f) ? 1.0f / dir.y : std::numeric_limits<float>::infinity();
    const float invDirZ = (dir.z != 0.0f) ? 1.0f / dir.z : std::numeric_limits<float>::infinity();

    const auto computeBoundary = [](int step, float originComponent, int blockCoordinate) {
        if (step > 0) {
            return static_cast<float>(blockCoordinate + 1);
        }
        if (step < 0) {
            return static_cast<float>(blockCoordinate);
        }
        return std::numeric_limits<float>::infinity();
    };

    float nextBoundaryX = computeBoundary(stepX, origin.x, currentBlock.x);
    float nextBoundaryY = computeBoundary(stepY, origin.y, currentBlock.y);
    float nextBoundaryZ = computeBoundary(stepZ, origin.z, currentBlock.z);

    float tMaxX = (stepX != 0) ? (nextBoundaryX - origin.x) * invDirX : std::numeric_limits<float>::infinity();
    float tMaxY = (stepY != 0) ? (nextBoundaryY - origin.y) * invDirY : std::numeric_limits<float>::infinity();
    float tMaxZ = (stepZ != 0) ? (nextBoundaryZ - origin.z) * invDirZ : std::numeric_limits<float>::infinity();

    float tDeltaX = (stepX != 0) ? std::abs(invDirX) : std::numeric_limits<float>::infinity();
    float tDeltaY = (stepY != 0) ? std::abs(invDirY) : std::numeric_limits<float>::infinity();
    float tDeltaZ = (stepZ != 0) ? std::abs(invDirZ) : std::numeric_limits<float>::infinity();

    float distanceTravelled = 0.0f;
    int lastStepAxis = -1;

    constexpr int MAX_STEPS = 512;
    for (int i = 0; i < MAX_STEPS && distanceTravelled <= clampedDistance; ++i) {
        const BlockType blockType = chunkManager.getBlockAt(currentBlock.x, currentBlock.y, currentBlock.z);
        if (block::isSolid(blockType)) {
            result.hit = true;
            result.blockPosition = currentBlock;
            result.previousBlockPosition = previousBlock;
            result.blockType = blockType;
            result.hitPoint = origin + dir * distanceTravelled;

            switch (lastStepAxis) {
            case 0: result.normal = glm::vec3(-static_cast<float>(stepX), 0.0f, 0.0f); break;
            case 1: result.normal = glm::vec3(0.0f, -static_cast<float>(stepY), 0.0f); break;
            case 2: result.normal = glm::vec3(0.0f, 0.0f, -static_cast<float>(stepZ)); break;
            default: result.normal = glm::vec3(0.0f); break;
            }

            return result;
        }

        previousBlock = currentBlock;

        if (tMaxX <= tMaxY && tMaxX <= tMaxZ) {
            if (stepX == 0) {
                break;
            }
            distanceTravelled = tMaxX;
            currentBlock.x += stepX;
            tMaxX += tDeltaX;
            lastStepAxis = 0;
        } else if (tMaxY <= tMaxX && tMaxY <= tMaxZ) {
            if (stepY == 0) {
                break;
            }
            distanceTravelled = tMaxY;
            currentBlock.y += stepY;
            tMaxY += tDeltaY;
            lastStepAxis = 1;
        } else {
            if (stepZ == 0) {
                break;
            }
            distanceTravelled = tMaxZ;
            currentBlock.z += stepZ;
            tMaxZ += tDeltaZ;
            lastStepAxis = 2;
        }

        if (distanceTravelled > clampedDistance) {
            break;
        }
    }

    return result;
}

} // namespace poorcraft::world
