#include <gtest/gtest.h>
#include "poorcraft/rendering/RendererFactory.h"
#include "poorcraft/rendering/Renderer.h"
#include "poorcraft/rendering/TextureAtlas.h"
#include "poorcraft/core/Window.h"
#include <SDL2/SDL.h>

using namespace poorcraft::rendering;
using namespace poorcraft::core;

class RendererTextureTest : public ::testing::Test {
protected:
    void SetUp() override {
        if (SDL_Init(SDL_INIT_VIDEO) != 0) {
            GTEST_SKIP() << "SDL initialization failed: " << SDL_GetError();
        }
    }

    void TearDown() override {
        SDL_Quit();
    }
};

TEST_F(RendererTextureTest, OpenGLTextureCreationAndBinding) {
#ifdef POORCRAFT_OPENGL_ENABLED
    Window window;
    if (!window.create("Test Window", 800, 600, false)) {
        GTEST_SKIP() << "Failed to create window";
    }

    auto renderer = RendererFactory::create(RendererBackend::OpenGL, window);
    if (!renderer) {
        GTEST_SKIP() << "OpenGL renderer not available";
    }

    if (!renderer->initialize()) {
        GTEST_SKIP() << "Failed to initialize OpenGL renderer";
    }

    // Create a small test texture
    std::vector<uint8_t> textureData(4 * 4 * 4, 255); // 4x4 white RGBA texture
    const Renderer::TextureHandle handle = renderer->createTexture(
        textureData.data(), 4, 4, 4
    );

    EXPECT_NE(handle, 0u) << "Texture creation should return valid handle";

    // Bind texture to slot 0
    renderer->bindTexture(handle, 0);

    // Cleanup
    renderer->destroyTexture(handle);
    renderer->shutdown();
#else
    GTEST_SKIP() << "OpenGL not enabled";
#endif
}

TEST_F(RendererTextureTest, VulkanTextureCreationAndBinding) {
#ifdef POORCRAFT_VULKAN_ENABLED
    Window window;
    if (!window.create("Test Window", 800, 600, false)) {
        GTEST_SKIP() << "Failed to create window";
    }

    auto renderer = RendererFactory::create(RendererBackend::Vulkan, window);
    if (!renderer) {
        GTEST_SKIP() << "Vulkan renderer not available";
    }

    if (!renderer->initialize()) {
        GTEST_SKIP() << "Failed to initialize Vulkan renderer";
    }

    // Create a small test texture
    std::vector<uint8_t> textureData(4 * 4 * 4, 255); // 4x4 white RGBA texture
    const Renderer::TextureHandle handle = renderer->createTexture(
        textureData.data(), 4, 4, 4
    );

    EXPECT_NE(handle, 0u) << "Texture creation should return valid handle";

    // Bind texture to slot 0
    renderer->bindTexture(handle, 0);

    // Cleanup
    renderer->destroyTexture(handle);
    renderer->shutdown();
#else
    GTEST_SKIP() << "Vulkan not enabled";
#endif
}

TEST_F(RendererTextureTest, LightingParametersCanBeSet) {
#ifdef POORCRAFT_OPENGL_ENABLED
    Window window;
    if (!window.create("Test Window", 800, 600, false)) {
        GTEST_SKIP() << "Failed to create window";
    }

    auto renderer = RendererFactory::create(RendererBackend::OpenGL, window);
    if (!renderer || !renderer->initialize()) {
        GTEST_SKIP() << "OpenGL renderer not available";
    }

    Renderer::LightingParams params;
    params.sunDirection = glm::vec3(0.0f, -1.0f, 0.0f);
    params.sunColor = glm::vec3(1.0f, 1.0f, 0.9f);
    params.sunIntensity = 0.8f;
    params.ambientColor = glm::vec3(0.5f, 0.5f, 0.6f);
    params.ambientIntensity = 0.3f;

    // Should not crash
    renderer->setLightingParams(params);

    renderer->shutdown();
#else
    GTEST_SKIP() << "OpenGL not enabled";
#endif
}

TEST_F(RendererTextureTest, AtlasIntegrationWithOpenGL) {
#ifdef POORCRAFT_OPENGL_ENABLED
    Window window;
    if (!window.create("Test Window", 800, 600, false)) {
        GTEST_SKIP() << "Failed to create window";
    }

    auto renderer = RendererFactory::create(RendererBackend::OpenGL, window);
    if (!renderer || !renderer->initialize()) {
        GTEST_SKIP() << "OpenGL renderer not available";
    }

    // Create and initialize texture atlas
    TextureAtlas atlas;
    ASSERT_TRUE(atlas.initialize(32));

    // Upload atlas to GPU
    const Renderer::TextureHandle atlasHandle = renderer->createTexture(
        atlas.getAtlasData().data(),
        atlas.getAtlasWidth(),
        atlas.getAtlasHeight(),
        4
    );

    EXPECT_NE(atlasHandle, 0u) << "Atlas upload should succeed";

    // Bind atlas
    renderer->bindTexture(atlasHandle, 0);

    // Set lighting
    Renderer::LightingParams params;
    params.sunDirection = glm::normalize(glm::vec3(0.3f, -0.7f, 0.4f));
    params.sunColor = glm::vec3(1.0f, 1.0f, 0.9f);
    params.sunIntensity = 0.8f;
    params.ambientColor = glm::vec3(0.4f, 0.4f, 0.5f);
    params.ambientIntensity = 0.2f;
    renderer->setLightingParams(params);

    // Begin frame to apply uniforms
    renderer->beginFrame();
    renderer->clear(0.0f, 0.0f, 0.0f, 1.0f);
    renderer->endFrame();

    // Cleanup
    renderer->destroyTexture(atlasHandle);
    renderer->shutdown();
#else
    GTEST_SKIP() << "OpenGL not enabled";
#endif
}

TEST_F(RendererTextureTest, AtlasIntegrationWithVulkan) {
#ifdef POORCRAFT_VULKAN_ENABLED
    Window window;
    if (!window.create("Test Window", 800, 600, false)) {
        GTEST_SKIP() << "Failed to create window";
    }

    auto renderer = RendererFactory::create(RendererBackend::Vulkan, window);
    if (!renderer || !renderer->initialize()) {
        GTEST_SKIP() << "Vulkan renderer not available";
    }

    // Create and initialize texture atlas
    TextureAtlas atlas;
    ASSERT_TRUE(atlas.initialize(32));

    // Upload atlas to GPU
    const Renderer::TextureHandle atlasHandle = renderer->createTexture(
        atlas.getAtlasData().data(),
        atlas.getAtlasWidth(),
        atlas.getAtlasHeight(),
        4
    );

    EXPECT_NE(atlasHandle, 0u) << "Atlas upload should succeed";

    // Bind atlas
    renderer->bindTexture(atlasHandle, 0);

    // Set lighting
    Renderer::LightingParams params;
    params.sunDirection = glm::normalize(glm::vec3(0.3f, -0.7f, 0.4f));
    params.sunColor = glm::vec3(1.0f, 1.0f, 0.9f);
    params.sunIntensity = 0.8f;
    params.ambientColor = glm::vec3(0.4f, 0.4f, 0.5f);
    params.ambientIntensity = 0.2f;
    renderer->setLightingParams(params);

    // Cleanup
    renderer->destroyTexture(atlasHandle);
    renderer->shutdown();
#else
    GTEST_SKIP() << "Vulkan not enabled";
#endif
}

TEST_F(RendererTextureTest, InvalidTextureHandleIsHandledGracefully) {
#ifdef POORCRAFT_OPENGL_ENABLED
    Window window;
    if (!window.create("Test Window", 800, 600, false)) {
        GTEST_SKIP() << "Failed to create window";
    }

    auto renderer = RendererFactory::create(RendererBackend::OpenGL, window);
    if (!renderer || !renderer->initialize()) {
        GTEST_SKIP() << "OpenGL renderer not available";
    }

    // Try to bind invalid handle - should not crash
    renderer->bindTexture(999999, 0);

    // Try to destroy invalid handle - should not crash
    renderer->destroyTexture(999999);

    renderer->shutdown();
#else
    GTEST_SKIP() << "OpenGL not enabled";
#endif
}
