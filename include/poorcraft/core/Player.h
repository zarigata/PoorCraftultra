#pragma once

#include "poorcraft/core/Input.h"

#include <glm/glm.hpp>

#include <vector>

namespace poorcraft::world {
class ChunkManager;
} // namespace poorcraft::world

namespace poorcraft::core {

struct AABB {
    glm::vec3 min{};
    glm::vec3 max{};
};

// Player encapsulates first-person physics with a swept AABB collider. Gravity is applied when
// not in fly mode, collisions are resolved axis-by-axis against solid voxel blocks, and jumping
// is only permitted while grounded.
class Player {
public:
    enum class MovementMode {
        Walk,
        Sprint,
        Fly
    };

    explicit Player(const glm::vec3& position);

    void update(const Input& input, float deltaTime, const world::ChunkManager& chunkManager);

    [[nodiscard]] glm::vec3 getPosition() const noexcept { return m_position; }
    void setPosition(const glm::vec3& position) noexcept;

    [[nodiscard]] glm::vec3 getVelocity() const noexcept { return m_velocity; }

    [[nodiscard]] AABB getAABB() const noexcept;
    [[nodiscard]] bool isOnGround() const noexcept { return m_onGround; }

    [[nodiscard]] MovementMode getMovementMode() const noexcept { return m_mode; }
    void setMovementMode(MovementMode mode) noexcept;

    [[nodiscard]] glm::vec3 getEyePosition() const noexcept;
    void setViewOrientation(const glm::vec3& forward, const glm::vec3& right) noexcept;

private:
    void applyGravity(float deltaTime);
    void applyMovement(const glm::vec3& inputDirection, float deltaTime);
    void resolveCollisions(float deltaTime, const world::ChunkManager& chunkManager);
    void checkGroundCollision(const world::ChunkManager& chunkManager);

    [[nodiscard]] std::vector<glm::ivec3> getBlocksInAABB(const AABB& bounds) const;
    [[nodiscard]] AABB getWorldAABB() const noexcept;

    glm::vec3 m_position{};
    glm::vec3 m_velocity{};
    AABB m_localAABB{};
    MovementMode m_mode{MovementMode::Walk};
    bool m_onGround{false};
    glm::vec3 m_viewForward{0.0f, 0.0f, -1.0f};
    glm::vec3 m_viewRight{1.0f, 0.0f, 0.0f};

    static constexpr float kWalkSpeed = 4.3f;
    static constexpr float kSprintSpeed = 5.6f;
    static constexpr float kFlySpeed = 10.0f;
    static constexpr float kJumpVelocity = 8.0f;
    static constexpr float kGravity = -20.0f;
    static constexpr float kEyeHeight = 1.62f;
};

} // namespace poorcraft::core
