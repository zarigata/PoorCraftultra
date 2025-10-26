#pragma once

#include <array>

#include <glm/mat4x4.hpp>
#include <glm/vec3.hpp>

#include "poorcraft/core/Player.h"

namespace poorcraft::rendering
{
struct Plane
{
    glm::vec3 normal{0.0f};
    float distance{0.0f};
};

// View frustum defined by six clipping planes in world space. Planes follow the
// convention normal · point + distance = 0 (points inside yield non-negative values).
class Frustum
{
public:
    static Frustum fromViewProjection(const glm::mat4& viewProjection);

    [[nodiscard]] bool intersects(const core::AABB& aabb) const;
    [[nodiscard]] bool contains(const glm::vec3& point) const;

private:
    // Normalize plane so |normal| == 1 which simplifies distance tests.
    void normalizePlane(Plane& plane);

    std::array<Plane, 6> m_planes{}; // left, right, bottom, top, near, far
};

} // namespace poorcraft::rendering
