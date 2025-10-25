#include <gtest/gtest.h>

#include "poorcraft/core/Input.h"

#include <SDL2/SDL.h>

namespace
{
class InputTest : public ::testing::Test
{
protected:
    void SetUp() override
    {
        const Uint32 mask = SDL_INIT_VIDEO | SDL_INIT_EVENTS;
        if((SDL_WasInit(mask) & mask) != mask)
        {
            ASSERT_EQ(SDL_Init(mask), 0) << SDL_GetError();
        }

        m_window = SDL_CreateWindow(
            "InputTest",
            SDL_WINDOWPOS_CENTERED,
            SDL_WINDOWPOS_CENTERED,
            640,
            480,
            SDL_WINDOW_HIDDEN | SDL_WINDOW_INPUT_FOCUS);
        ASSERT_NE(m_window, nullptr) << SDL_GetError();
    }

    void TearDown() override
    {
        if(m_window != nullptr)
        {
            SDL_DestroyWindow(m_window);
            m_window = nullptr;
        }
        SDL_QuitSubSystem(SDL_INIT_VIDEO | SDL_INIT_EVENTS);
    }

    SDL_Window* m_window{nullptr};
};
} // namespace

TEST_F(InputTest, InitialState)
{
    poorcraft::core::Input input;
    EXPECT_FALSE(input.isKeyDown(poorcraft::core::Input::KeyCode::W));
    EXPECT_FALSE(input.isKeyPressed(poorcraft::core::Input::KeyCode::W));
    const auto delta = input.getMouseDelta();
    EXPECT_EQ(delta.x, 0);
    EXPECT_EQ(delta.y, 0);
}

TEST_F(InputTest, KeyDownUp)
{
    poorcraft::core::Input input;

    SDL_Event event{};
    event.type = SDL_KEYDOWN;
    event.key.keysym.scancode = SDL_SCANCODE_W;
    input.processEvent(event);

    EXPECT_TRUE(input.isKeyDown(poorcraft::core::Input::KeyCode::W));

    event.type = SDL_KEYUP;
    input.processEvent(event);

    EXPECT_FALSE(input.isKeyDown(poorcraft::core::Input::KeyCode::W));
}

TEST_F(InputTest, KeyPressed)
{
    poorcraft::core::Input input;

    SDL_Event event{};
    event.type = SDL_KEYDOWN;
    event.key.keysym.scancode = SDL_SCANCODE_W;
    input.processEvent(event);

    EXPECT_TRUE(input.isKeyPressed(poorcraft::core::Input::KeyCode::W));

    input.reset();
    EXPECT_FALSE(input.isKeyPressed(poorcraft::core::Input::KeyCode::W));

    event.type = SDL_KEYUP;
    input.processEvent(event);
    input.reset();

    event.type = SDL_KEYDOWN;
    input.processEvent(event);
    EXPECT_TRUE(input.isKeyPressed(poorcraft::core::Input::KeyCode::W));
}

TEST_F(InputTest, MouseMotion)
{
    poorcraft::core::Input input;
    input.setRelativeMouseMode(true);

    SDL_Event event{};
    event.type = SDL_MOUSEMOTION;
    event.motion.xrel = 10;
    event.motion.yrel = -5;
    input.processEvent(event);

    const auto delta = input.getMouseDelta();
    EXPECT_EQ(delta.x, 10);
    EXPECT_EQ(delta.y, -5);

    input.reset();
    const auto resetDelta = input.getMouseDelta();
    EXPECT_EQ(resetDelta.x, 0);
    EXPECT_EQ(resetDelta.y, 0);
}

TEST_F(InputTest, MouseButtons)
{
    poorcraft::core::Input input;

    SDL_Event event{};
    event.type = SDL_MOUSEBUTTONDOWN;
    event.button.button = SDL_BUTTON_LEFT;
    input.processEvent(event);

    EXPECT_TRUE(input.isMouseButtonDown(poorcraft::core::Input::MouseButton::Left));
    EXPECT_TRUE(input.isMouseButtonPressed(poorcraft::core::Input::MouseButton::Left));

    input.reset();
    EXPECT_FALSE(input.isMouseButtonPressed(poorcraft::core::Input::MouseButton::Left));

    event.type = SDL_MOUSEBUTTONUP;
    input.processEvent(event);

    EXPECT_FALSE(input.isMouseButtonDown(poorcraft::core::Input::MouseButton::Left));
}

TEST_F(InputTest, RelativeMouseMode)
{
    poorcraft::core::Input input;

    input.setRelativeMouseMode(true);
    EXPECT_TRUE(input.isRelativeMouseMode());

    SDL_Event event{};
    event.type = SDL_MOUSEMOTION;
    event.motion.xrel = 4;
    event.motion.yrel = -2;
    input.processEvent(event);

    auto delta = input.getMouseDelta();
    EXPECT_EQ(delta.x, 4);
    EXPECT_EQ(delta.y, -2);

    input.setRelativeMouseMode(false);
    EXPECT_FALSE(input.isRelativeMouseMode());

    event.motion.xrel = 7;
    event.motion.yrel = 3;
    input.processEvent(event);

    delta = input.getMouseDelta();
    EXPECT_EQ(delta.x, 0);
    EXPECT_EQ(delta.y, 0);
}
