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

[[nodiscard]] glm::vec3 floorVec3(const glm::vec3& v) {
    return glm::vec3(std::floor(v.x), std::floor(v.y), std::floor(v.z));
}

[[nodiscard]] glm::vec3 ceilVec3(const glm::vec3& v) {
    return glm::vec3(std::ceil(v.x), std::ceil(v.y), std::ceil(v.z));
}
} // namespace

Player::Player(const glm::vec3& position)
    : m_position(position)
    , m_velocity(0.0f)
    , m_localAABB({glm::vec3(-0.3f, 0.0f, -0.3f), glm::vec3(0.3f, 1.8f, 0.3f)})
    , m_mode(MovementMode::Walk)
    , m_onGround(false) {}

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

AABB Player::getWorldAABB() const noexcept {
    return translateAABB(m_localAABB, m_position);
}

AABB Player::getAABB() const noexcept {
    return getWorldAABB();
}

void Player::update(const Input& input, float deltaTime, const world::ChunkManager& chunkManager) {
    glm::vec3 direction(0.0f);
    if (input.isKeyDown(Input::KeyCode::W)) {
        direction.z -= 1.0f;
    }
    if (input.isKeyDown(Input::KeyCode::S)) {
        direction.z += 1.0f;
    }
    if (input.isKeyDown(Input::KeyCode::A)) {
        direction.x -= 1.0f;
    }
    if (input.isKeyDown(Input::KeyCode::D)) {
        direction.x += 1.0f;
    }

    bool sprintRequested = input.isKeyDown(Input::KeyCode::LeftShift);
    bool jumpRequested = input.isKeyPressed(Input::KeyCode::Space);
    bool flyToggle = input.isKeyPressed(Input::KeyCode::F);

    if (flyToggle) {
        if (m_mode == MovementMode::Fly) {
            setMovementMode(MovementMode::Walk);
        } else {
            setMovementMode(MovementMode::Fly);
        }
    }

    const bool isFlyMode = m_mode == MovementMode::Fly;
    if (!isFlyMode) {
        applyGravity(deltaTime);
    }

    if (!isFlyMode && jumpRequested && m_onGround) {
        m_velocity.y = kJumpVelocity;
        m_onGround = false;
    }

    glm::vec3 moveDirection = direction;
    if (isFlyMode) {
        if (input.isKeyDown(Input::KeyCode::Space)) {
            moveDirection.y += 1.0f;
        }
        if (input.isKeyDown(Input::KeyCode::LeftShift)) {
            moveDirection.y -= 1.0f;
        }
    }

    applyMovement(input, moveDirection, deltaTime);
    resolveCollisions(deltaTime, chunkManager);
    checkGroundCollision(chunkManager);

    if (!isFlyMode && sprintRequested) {
        m_mode = MovementMode::Sprint;
    } else if (!isFlyMode) {
        m_mode = MovementMode::Walk;
    }
}

void Player::applyGravity(float deltaTime) {
    m_velocity.y += kGravity * deltaTime;
}

void Player::applyMovement(const Input& input, const glm::vec3& inputDirection, float deltaTime) {
    (void)input;
    glm::vec3 direction = normalizeOrZero(inputDirection);

    float speed = kWalkSpeed;
    if (m_mode == MovementMode::Sprint) {
        speed = kSprintSpeed;
    } else if (m_mode == MovementMode::Fly) {
        speed = kFlySpeed;
    }

    glm::vec3 desiredVelocity = direction * speed;

    if (m_mode != MovementMode::Fly) {
        desiredVelocity.y = m_velocity.y;
        m_velocity.x = desiredVelocity.x;
        m_velocity.z = desiredVelocity.z;
    } else {
        m_velocity = desiredVelocity;
    }
}

std::vector<glm::ivec3> Player::getBlocksInAABB(const AABB& bounds) const {
    const glm::vec3 min = floorVec3(bounds.min);
    const glm::vec3 max = ceilVec3(bounds.max);

    std::vector<glm::ivec3> blocks;
    for (int x = static_cast<int>(min.x); x < static_cast<int>(max.x); ++x) {
        for (int y = static_cast<int>(min.y); y < static_cast<int>(max.y); ++y) {
            for (int z = static_cast<int>(min.z); z < static_cast<int>(max.z); ++z) {
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

    glm::vec3 newPosition = m_position;
    glm::vec3 velocity = m_velocity * deltaTime;

    const int axes[3] = {0, 1, 2};
    for (int axis : axes) {
        newPosition[axis] += velocity[axis];
        AABB aabb = translateAABB(m_localAABB, newPosition);
        const auto blocks = getBlocksInAABB(aabb);
        bool collided = false;

        for (const auto& block : blocks) {
            const glm::vec3 blockCenter = glm::vec3(block) + glm::vec3(0.5f);
            if (!chunkManager.isBlockSolid(blockCenter)) {
                continue;
            }

            const float blockMin = static_cast<float>(block[axis]);
            const float blockMax = blockMin + 1.0f;

            if (aabb.min[axis] < blockMax && aabb.max[axis] > blockMin) {
                if (velocity[axis] > 0.0f) {
                    newPosition[axis] = blockMin - m_localAABB.max[axis];
                } else if (velocity[axis] < 0.0f) {
                    newPosition[axis] = blockMax - m_localAABB.min[axis];
                }
                m_velocity[axis] = 0.0f;
                velocity[axis] = 0.0f;
                collided = true;
                break;
            }
        }

        if (!collided) {
            continue;
        }

        // Update AABB for next axis checks after resolving this axis.
        aabb = translateAABB(m_localAABB, newPosition);
    }

    m_position = newPosition;
}

void Player::checkGroundCollision(const world::ChunkManager& chunkManager) {
    const float epsilon = 0.05f;
    const glm::vec3 footPosition = m_position + glm::vec3(0.0f, m_localAABB.min.y - epsilon, 0.0f);
    m_onGround = chunkManager.isBlockSolid(footPosition);
}

} // namespace poorcraft::core
