#include "poorcraft/core/Window.h"
#include "poorcraft/rendering/RendererFactory.h"
#include "poorcraft/rendering/Renderer.h"

#include <SDL2/SDL.h>
#include <gtest/gtest.h>

namespace
{
class RendererTest : public ::testing::Test
{
protected:
    void SetUp() override
    {
        ASSERT_TRUE(poorcraft::core::Window::initSDL());
    }

    void TearDown() override
    {
        SDL_Quit();
    }
};
} // namespace

TEST_F(RendererTest, CreateRenderer)
{
    auto selection = poorcraft::rendering::createRenderer("Renderer Test", 640, 480);
    auto& window = selection.window;
    auto& renderer = selection.renderer;
    if(renderer == nullptr || window == nullptr)
    {
        GTEST_SKIP() << "Renderer could not be initialized on this platform.";
    }
    EXPECT_NE(renderer, nullptr);
}

TEST_F(RendererTest, RendererCapabilities)
{
    auto selection = poorcraft::rendering::createRenderer("Capabilities Test", 640, 480);
    auto& renderer = selection.renderer;
    if(renderer == nullptr)
    {
        GTEST_SKIP() << "Renderer could not be initialized on this platform.";
    }
    const auto capabilities = renderer->getCapabilities();
    EXPECT_TRUE(capabilities.backend == poorcraft::rendering::RendererBackend::Vulkan ||
                capabilities.backend == poorcraft::rendering::RendererBackend::OpenGL);
    EXPECT_FALSE(capabilities.backendVersion.empty());
}

TEST_F(RendererTest, RenderLoop)
{
    auto selection = poorcraft::rendering::createRenderer("Render Loop Test", 640, 480);
    auto& renderer = selection.renderer;
    if(renderer == nullptr)
    {
        GTEST_SKIP() << "Renderer could not be initialized on this platform.";
    }

    for(int i = 0; i < 10; ++i)
    {
        renderer->beginFrame();
        renderer->clear(0.1f, 0.2f, 0.3f, 1.0f);
        renderer->endFrame();
    }
}

TEST_F(RendererTest, VSyncToggle)
{
    auto selection = poorcraft::rendering::createRenderer("VSync Test", 640, 480);
    auto& renderer = selection.renderer;
    if(renderer == nullptr)
    {
        GTEST_SKIP() << "Renderer could not be initialized on this platform.";
    }

    renderer->setVSync(true);
    renderer->setVSync(false);
}
