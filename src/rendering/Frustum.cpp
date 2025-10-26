#include "poorcraft/rendering/Frustum.h"

#include <glm/glm.hpp>

#include <algorithm>
#include <array>
#include <cmath>

namespace poorcraft::rendering
{
namespace
{
constexpr float kPlaneEpsilon = 1e-6f;
}

Frustum Frustum::fromViewProjection(const glm::mat4& viewProjection)
{
    Frustum frustum;

    // Gribb-Hartmann extraction. Each plane is derived from the combined matrix rows.
    const glm::vec4 row0 = viewProjection[0];
    const glm::vec4 row1 = viewProjection[1];
    const glm::vec4 row2 = viewProjection[2];
    const glm::vec4 row3 = viewProjection[3];

    std::array<glm::vec4, 6> planeEquations{
        row3 + row0, // Left
        row3 - row0, // Right
        row3 + row1, // Bottom
        row3 - row1, // Top
        row3 + row2, // Near
        row3 - row2  // Far
    };

    for(std::size_t i = 0; i < planeEquations.size(); ++i)
    {
        Plane plane;
        plane.normal = glm::vec3(planeEquations[i]);
        plane.distance = planeEquations[i].w;
        frustum.normalizePlane(plane);
        frustum.m_planes[i] = plane;
    }

    return frustum;
}

bool Frustum::intersects(const core::AABB& aabb) const
{
    const glm::vec3 center = (aabb.min + aabb.max) * 0.5f;
    const glm::vec3 extents = (aabb.max - aabb.min) * 0.5f;

    for(const Plane& plane : m_planes)
    {
        const float distance = glm::dot(plane.normal, center) + plane.distance;
        const float radius = glm::dot(glm::abs(plane.normal), extents);

        if(distance + radius < 0.0f)
        {
            return false; // Completely outside this plane.
        }
    }

    return true;
}

bool Frustum::contains(const glm::vec3& point) const
{
    for(const Plane& plane : m_planes)
    {
        const float distance = glm::dot(plane.normal, point) + plane.distance;
        if(distance < 0.0f)
        {
            return false;
        }
    }

    return true;
}

void Frustum::normalizePlane(Plane& plane)
{
    const float magnitude = glm::length(plane.normal);
    if(magnitude < kPlaneEpsilon)
    {
        plane.normal = glm::vec3(0.0f, 1.0f, 0.0f);
        plane.distance = 0.0f;
        return;
    }

    const float invMagnitude = 1.0f / magnitude;
    plane.normal *= invMagnitude;
    plane.distance *= invMagnitude;
}

} // namespace poorcraft::rendering
