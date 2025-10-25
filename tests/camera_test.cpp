#include <gtest/gtest.h>

#include "poorcraft/core/Camera.h"

#include <glm/gtc/constants.hpp>
#include <glm/gtc/matrix_transform.hpp>
#include <glm/mat4x4.hpp>

#include <cmath>

namespace
{
constexpr float kEpsilon = 1e-4f;
}

TEST(CameraTest, InitialState)
{
    poorcraft::core::Camera camera(glm::vec3(0.0f), 0.0f, 0.0f);
    EXPECT_NEAR(camera.getPosition().x, 0.0f, kEpsilon);
    EXPECT_NEAR(camera.getPosition().y, 0.0f, kEpsilon);
    EXPECT_NEAR(camera.getPosition().z, 0.0f, kEpsilon);
    EXPECT_NEAR(camera.getYaw(), 0.0f, kEpsilon);
    EXPECT_NEAR(camera.getPitch(), 0.0f, kEpsilon);
}

TEST(CameraTest, SetPosition)
{
    poorcraft::core::Camera camera(glm::vec3(0.0f), 0.0f, 0.0f);
    camera.setPosition(glm::vec3(1.0f, 2.0f, 3.0f));
    EXPECT_NEAR(camera.getPosition().x, 1.0f, kEpsilon);
    EXPECT_NEAR(camera.getPosition().y, 2.0f, kEpsilon);
    EXPECT_NEAR(camera.getPosition().z, 3.0f, kEpsilon);
}

TEST(CameraTest, SetRotation)
{
    poorcraft::core::Camera camera(glm::vec3(0.0f), 0.0f, 0.0f);
    camera.setRotation(glm::radians(45.0f), glm::radians(10.0f));
    EXPECT_NEAR(camera.getYaw(), glm::radians(45.0f), kEpsilon);
    EXPECT_NEAR(camera.getPitch(), glm::radians(10.0f), kEpsilon);
}

TEST(CameraTest, PitchClamping)
{
    poorcraft::core::Camera camera(glm::vec3(0.0f), 0.0f, 0.0f);
    camera.setRotation(0.0f, glm::radians(120.0f));
    EXPECT_NEAR(camera.getPitch(), glm::radians(89.0f), 1e-3f);
    camera.setRotation(0.0f, glm::radians(-120.0f));
    EXPECT_NEAR(camera.getPitch(), glm::radians(-89.0f), 1e-3f);
}

TEST(CameraTest, Translate)
{
    poorcraft::core::Camera camera(glm::vec3(0.0f), 0.0f, 0.0f);
    camera.translate(glm::vec3(1.0f, -2.0f, 0.5f));
    EXPECT_NEAR(camera.getPosition().x, 1.0f, kEpsilon);
    EXPECT_NEAR(camera.getPosition().y, -2.0f, kEpsilon);
    EXPECT_NEAR(camera.getPosition().z, 0.5f, kEpsilon);
}

TEST(CameraTest, Rotate)
{
    poorcraft::core::Camera camera(glm::vec3(0.0f), 0.0f, 0.0f);
    camera.rotate(glm::radians(90.0f), glm::radians(45.0f));
    EXPECT_NEAR(camera.getYaw(), glm::radians(90.0f), kEpsilon);
    EXPECT_NEAR(camera.getPitch(), glm::radians(45.0f), kEpsilon);
}

TEST(CameraTest, DirectionVectors)
{
    poorcraft::core::Camera camera(glm::vec3(0.0f), 0.0f, 0.0f);
    camera.setRotation(0.0f, 0.0f);
    const auto forward = camera.getForward();
    const auto right = camera.getRight();
    const auto up = camera.getUp();

    EXPECT_NEAR(forward.x, 1.0f, kEpsilon);
    EXPECT_NEAR(forward.y, 0.0f, kEpsilon);
    EXPECT_NEAR(forward.z, 0.0f, kEpsilon);

    EXPECT_NEAR(right.x, 0.0f, kEpsilon);
    EXPECT_NEAR(right.y, 0.0f, kEpsilon);
    EXPECT_NEAR(right.z, -1.0f, kEpsilon);

    EXPECT_NEAR(up.x, 0.0f, kEpsilon);
    EXPECT_NEAR(up.y, 1.0f, kEpsilon);
    EXPECT_NEAR(up.z, 0.0f, kEpsilon);
}

TEST(CameraTest, ViewMatrix)
{
    poorcraft::core::Camera camera(glm::vec3(0.0f, 0.0f, 5.0f), 0.0f, 0.0f);
    const auto view = camera.getViewMatrix();
    EXPECT_NEAR(view[3][0], -5.0f, 1e-3f);
    EXPECT_NEAR(view[3][1], 0.0f, 1e-3f);
    EXPECT_NEAR(view[3][2], 0.0f, 1e-3f);
}

TEST(CameraTest, ProjectionMatrix)
{
    poorcraft::core::Camera camera(glm::vec3(0.0f), 0.0f, 0.0f);
    const float fov = glm::radians(90.0f);
    const float aspect = 16.0f / 9.0f;
    const float nearPlane = 0.1f;
    const float farPlane = 100.0f;

    const auto projection = camera.getProjectionMatrix(fov, aspect, nearPlane, farPlane);

    EXPECT_NEAR(projection[0][0], 1.0f / (aspect * std::tan(fov / 2.0f)), 1e-3f);
    EXPECT_NEAR(projection[1][1], 1.0f / std::tan(fov / 2.0f), 1e-3f);
}
