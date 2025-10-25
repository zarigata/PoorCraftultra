#include "poorcraft/core/Player.h"

#include "poorcraft/world/Block.h"
#include "poorcraft/world/ChunkManager.h"

#include <algorithm>
#include <cmath>

namespace poorcraft::core {
namespace {
[[nodiscard]] glm::vec3 normalizeOrZero(const glm::vec3& v) {
    const float length = glm::length(v);
    if (length <= 0.0001f) {
        return glm::vec3(0.0f);
    }
    return v / length;
}

[[nodiscard]] AABB translateAABB(const AABB& aabb, const glm::vec3& offset) {
    return {aabb.min + offset, aabb.max + offset};
}

[[nodiscard]] bool intersectsBlock(const AABB& aabb, const glm::ivec3& block) {
    const glm::vec3 blockMin = glm::vec3(block) * world::BLOCK_SIZE;
    const glm::vec3 blockMax = blockMin + glm::vec3(world::BLOCK_SIZE);

    return (aabb.max.x > blockMin.x && aabb.min.x < blockMax.x) &&
           (aabb.max.y > blockMin.y && aabb.min.y < blockMax.y) &&
           (aabb.max.z > blockMin.z && aabb.min.z < blockMax.z);
}
} // namespace

Player::Player(const glm::vec3& position)
    : m_position(position)
    , m_velocity(0.0f)
    , m_localAABB({glm::vec3(-0.3f, 0.0f, -0.3f), glm::vec3(0.3f, 1.8f, 0.3f)})
    , m_mode(MovementMode::Walk)
    , m_onGround(false)
    , m_viewForward(0.0f, 0.0f, -1.0f)
    , m_viewRight(1.0f, 0.0f, 0.0f) {}

void Player::setPosition(const glm::vec3& position) noexcept {
    m_position = position;
}

void Player::setMovementMode(MovementMode mode) noexcept {
    m_mode = mode;
    if (m_mode == MovementMode::Fly) {
        m_velocity.y = 0.0f;
    }
}

glm::vec3 Player::getEyePosition() const noexcept {
    return m_position + glm::vec3(0.0f, kEyeHeight, 0.0f);
}

void Player::setViewOrientation(const glm::vec3& forward, const glm::vec3& right) noexcept {
    glm::vec3 flatForward(forward.x, 0.0f, forward.z);
    if (glm::length(flatForward) < 0.0001f) {
        flatForward = glm::vec3(0.0f, 0.0f, -1.0f);
    } else {
        flatForward = glm::normalize(flatForward);
    }

    glm::vec3 flatRight(right.x, 0.0f, right.z);
    if (glm::length(flatRight) < 0.0001f) {
        flatRight = glm::normalize(glm::cross(glm::vec3(0.0f, 1.0f, 0.0f), flatForward));
    } else {
        flatRight = glm::normalize(flatRight);
    }

    m_viewForward = flatForward;
    m_viewRight = flatRight;
}

AABB Player::getWorldAABB() const noexcept {
    return translateAABB(m_localAABB, m_position);
}

AABB Player::getAABB() const noexcept {
    return getWorldAABB();
}

void Player::update(const Input& input, float deltaTime, const world::ChunkManager& chunkManager) {
    const bool flyToggle = input.isKeyPressed(Input::KeyCode::F);
    if (flyToggle) {
        if (m_mode == MovementMode::Fly) {
            setMovementMode(MovementMode::Walk);
        } else {
            setMovementMode(MovementMode::Fly);
        }
    }

    const bool isFlyMode = m_mode == MovementMode::Fly;

    MovementMode targetMode = m_mode;
    if (!isFlyMode) {
        if (input.isKeyDown(Input::KeyCode::LeftShift)) {
            targetMode = MovementMode::Sprint;
        } else {
            targetMode = MovementMode::Walk;
        }
        if (targetMode != m_mode) {
            setMovementMode(targetMode);
        }
    }

    if (!isFlyMode) {
        applyGravity(deltaTime);
    }

    if (!isFlyMode && input.isKeyPressed(Input::KeyCode::Space) && m_onGround) {
        m_velocity.y = kJumpVelocity;
        m_onGround = false;
    }

    glm::vec3 moveDirection(0.0f);
    if (input.isKeyDown(Input::KeyCode::W)) {
        moveDirection += m_viewForward;
    }
    if (input.isKeyDown(Input::KeyCode::S)) {
        moveDirection -= m_viewForward;
    }
    if (input.isKeyDown(Input::KeyCode::A)) {
        moveDirection -= m_viewRight;
    }
    if (input.isKeyDown(Input::KeyCode::D)) {
        moveDirection += m_viewRight;
    }

    if (isFlyMode) {
        if (input.isKeyDown(Input::KeyCode::Space)) {
            moveDirection.y += 1.0f;
        }
        if (input.isKeyDown(Input::KeyCode::LeftShift)) {
            moveDirection.y -= 1.0f;
        }
    }

    applyMovement(moveDirection, deltaTime);
    resolveCollisions(deltaTime, chunkManager);
    checkGroundCollision(chunkManager);
}

void Player::applyGravity(float deltaTime) {
    m_velocity.y += kGravity * deltaTime;
}

void Player::applyMovement(const glm::vec3& inputDirection, float /*deltaTime*/) {
    const bool isFlyMode = m_mode == MovementMode::Fly;

    glm::vec3 direction = inputDirection;
    if (!isFlyMode) {
        direction.y = 0.0f;
    }
    direction = normalizeOrZero(direction);

    float speed = kWalkSpeed;
    if (m_mode == MovementMode::Sprint) {
        speed = kSprintSpeed;
    } else if (m_mode == MovementMode::Fly) {
        speed = kFlySpeed;
    }

    const glm::vec3 desiredVelocity = direction * speed;

    if (!isFlyMode) {
        m_velocity.x = desiredVelocity.x;
        m_velocity.z = desiredVelocity.z;
    } else {
        m_velocity = desiredVelocity;
    }
}

std::vector<glm::ivec3> Player::getBlocksInAABB(const AABB& bounds) const {
    const glm::vec3 scaledMin = bounds.min / world::BLOCK_SIZE;
    const glm::vec3 scaledMax = bounds.max / world::BLOCK_SIZE;

    const int minX = static_cast<int>(std::floor(scaledMin.x));
    const int minY = static_cast<int>(std::floor(scaledMin.y));
    const int minZ = static_cast<int>(std::floor(scaledMin.z));
    const int maxX = static_cast<int>(std::ceil(scaledMax.x));
    const int maxY = static_cast<int>(std::ceil(scaledMax.y));
    const int maxZ = static_cast<int>(std::ceil(scaledMax.z));

    std::vector<glm::ivec3> blocks;
    blocks.reserve(static_cast<std::size_t>(std::max(1, (maxX - minX))) * std::max(1, (maxY - minY)) * std::max(1, (maxZ - minZ)));

    for (int x = minX; x < maxX; ++x) {
        for (int y = minY; y < maxY; ++y) {
            for (int z = minZ; z < maxZ; ++z) {
                blocks.emplace_back(x, y, z);
            }
        }
    }
    return blocks;
}

void Player::resolveCollisions(float deltaTime, const world::ChunkManager& chunkManager) {
    if (deltaTime <= 0.0f) {
        return;
    }

    glm::vec3 position = m_position;
    glm::vec3 displacement = m_velocity * deltaTime;

    for (int axis = 0; axis < 3; ++axis) {
        position[axis] += displacement[axis];
        AABB aabb = translateAABB(m_localAABB, position);
        const auto blocks = getBlocksInAABB(aabb);

        for (const auto& block : blocks) {
            const glm::vec3 blockCenter = glm::vec3(block) * world::BLOCK_SIZE + glm::vec3(world::BLOCK_SIZE * 0.5f);
            if (!chunkManager.isBlockSolid(blockCenter)) {
                continue;
            }

            if (!intersectsBlock(aabb, block)) {
                continue;
            }

            const glm::vec3 blockMin = glm::vec3(block) * world::BLOCK_SIZE;
            const glm::vec3 blockMax = blockMin + glm::vec3(world::BLOCK_SIZE);

            if (displacement[axis] > 0.0f) {
                position[axis] = blockMin[axis] - m_localAABB.max[axis];
            } else if (displacement[axis] < 0.0f) {
                position[axis] = blockMax[axis] - m_localAABB.min[axis];
            }

            m_velocity[axis] = 0.0f;
            displacement[axis] = 0.0f;
            aabb = translateAABB(m_localAABB, position);
        }
    }

    m_position = position;
}

void Player::checkGroundCollision(const world::ChunkManager& chunkManager) {
    const float epsilon = 0.05f;
    const glm::vec3 samplePoint = m_position + glm::vec3(0.0f, m_localAABB.min.y - epsilon, 0.0f);
    m_onGround = chunkManager.isBlockSolid(samplePoint);
}

} // namespace poorcraft::core
