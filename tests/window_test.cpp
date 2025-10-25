#include "poorcraft/core/Window.h"

#include <SDL2/SDL.h>
#include <gtest/gtest.h>

namespace
{
class WindowTest : public ::testing::Test
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

TEST_F(WindowTest, InitSDL)
{
    EXPECT_TRUE(poorcraft::core::Window::initSDL());
}

TEST_F(WindowTest, CreateWindow)
{
    poorcraft::core::Window window("Test Window", 640, 480);
    EXPECT_TRUE(window.isOpen());
    EXPECT_FALSE(window.shouldClose());
}

TEST_F(WindowTest, WindowDimensions)
{
    poorcraft::core::Window window("Size Test", 800, 600);
    EXPECT_EQ(window.getWidth(), 800);
    EXPECT_EQ(window.getHeight(), 600);
}

TEST_F(WindowTest, WindowCloseEvent)
{
    poorcraft::core::Window window("Close Test", 320, 240);

    SDL_Event quitEvent{};
    quitEvent.type = SDL_QUIT;
    SDL_PushEvent(&quitEvent);

    window.pollEvents();
    EXPECT_TRUE(window.shouldClose());
}

TEST_F(WindowTest, VulkanExtensions)
{
    poorcraft::core::Window window("Vulkan Extensions", 400, 300);
    const auto extensions = window.getRequiredVulkanExtensions();
    EXPECT_FALSE(extensions.empty());
}
