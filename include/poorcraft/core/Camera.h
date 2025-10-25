#ifndef POORCRAFT_CORE_CAMERA_H
#define POORCRAFT_CORE_CAMERA_H

#include <glm/glm.hpp>
#include <glm/gtc/matrix_transform.hpp>

namespace poorcraft::core
{
// Camera implementing a right-handed coordinate system with Y-up (OpenGL/Vulkan convention).
class Camera
{
public:
    Camera(const glm::vec3& position, float yawRadians, float pitchRadians);

    void setPosition(const glm::vec3& position);
    const glm::vec3& getPosition() const;

    void setRotation(float yawRadians, float pitchRadians);
    float getYaw() const;
    float getPitch() const;

    void translate(const glm::vec3& offset);
    void rotate(float deltaYaw, float deltaPitch);

    glm::mat4 getViewMatrix() const;
    glm::mat4 getProjectionMatrix(float fovRadians, float aspectRatio, float nearPlane, float farPlane) const;

    const glm::vec3& getForward() const;
    const glm::vec3& getRight() const;
    const glm::vec3& getUp() const;

private:
    void updateVectors();

    glm::vec3 m_position;
    float m_yaw;
    float m_pitch;

    glm::vec3 m_forward;
    glm::vec3 m_right;
    glm::vec3 m_up;
};
} // namespace poorcraft::core

#endif // POORCRAFT_CORE_CAMERA_H
