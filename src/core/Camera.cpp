#include "poorcraft/core/Camera.h"
#include "poorcraft/rendering/Frustum.h"

#include <cmath>

#include <glm/gtc/constants.hpp>

namespace poorcraft::core
{
namespace
{
constexpr float kPitchLimit = glm::radians(89.0f);
const glm::vec3 kWorldUp(0.0f, 1.0f, 0.0f);

float wrapYaw(float yaw)
{
    const float twoPi = glm::two_pi<float>();
    yaw = std::fmod(yaw, twoPi);
    if(yaw < 0.0f)
    {
        yaw += twoPi;
    }
    return yaw;
}
}

Camera::Camera(const glm::vec3& position, float yawRadians, float pitchRadians)
    : m_position(position), m_yaw(wrapYaw(yawRadians)), m_pitch(glm::clamp(pitchRadians, -kPitchLimit, kPitchLimit))
{
    updateVectors();
}

void Camera::setPosition(const glm::vec3& position)
{
    m_position = position;
}

const glm::vec3& Camera::getPosition() const
{
    return m_position;
}

void Camera::setRotation(float yawRadians, float pitchRadians)
{
    m_yaw = wrapYaw(yawRadians);
    m_pitch = glm::clamp(pitchRadians, -kPitchLimit, kPitchLimit);
    updateVectors();
}

float Camera::getYaw() const
{
    return m_yaw;
}

float Camera::getPitch() const
{
    return m_pitch;
}

void Camera::translate(const glm::vec3& offset)
{
    m_position += offset;
}

void Camera::rotate(float deltaYaw, float deltaPitch)
{
    m_yaw = wrapYaw(m_yaw + deltaYaw);
    m_pitch = glm::clamp(m_pitch + deltaPitch, -kPitchLimit, kPitchLimit);
    updateVectors();
}

glm::mat4 Camera::getViewMatrix() const
{
    return glm::lookAt(m_position, m_position + m_forward, m_up);
}

glm::mat4 Camera::getProjectionMatrix(float fovRadians, float aspectRatio, float nearPlane, float farPlane) const
{
    if(fovRadians <= 0.0f || aspectRatio <= 0.0f || nearPlane <= 0.0f || farPlane <= 0.0f || nearPlane >= farPlane)
    {
        return glm::mat4(1.0f);
    }

    return glm::perspective(fovRadians, aspectRatio, nearPlane, farPlane);
}

rendering::Frustum Camera::getFrustum(float fovRadians, float aspectRatio, float nearPlane, float farPlane) const
{
    if(fovRadians <= 0.0f || aspectRatio <= 0.0f || nearPlane <= 0.0f || farPlane <= 0.0f || nearPlane >= farPlane)
    {
        return {};
    }

    const glm::mat4 view = getViewMatrix();
    const glm::mat4 projection = getProjectionMatrix(fovRadians, aspectRatio, nearPlane, farPlane);
    const glm::mat4 viewProjection = projection * view; // Column-major: projection applied after view.

    return rendering::Frustum::fromViewProjection(viewProjection);
}

const glm::vec3& Camera::getForward() const
{
    return m_forward;
}

const glm::vec3& Camera::getRight() const
{
    return m_right;
}

const glm::vec3& Camera::getUp() const
{
    return m_up;
}

void Camera::updateVectors()
{
    const float cosPitch = std::cos(m_pitch);
    const float sinPitch = std::sin(m_pitch);
    const float cosYaw = std::cos(m_yaw);
    const float sinYaw = std::sin(m_yaw);

    m_forward = glm::normalize(glm::vec3(cosYaw * cosPitch, sinPitch, sinYaw * cosPitch));
    m_right = glm::normalize(glm::cross(kWorldUp, m_forward));
    m_up = glm::normalize(glm::cross(m_forward, m_right));
}
} // namespace poorcraft::core
