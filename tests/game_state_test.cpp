#include <gtest/gtest.h>

#include "poorcraft/core/GameState.h"

namespace poorcraft::core
{
namespace
{
class GameStateManagerTest : public ::testing::Test
{
protected:
    GameStateManager manager{};
};
} // namespace

TEST_F(GameStateManagerTest, StartsInMainMenu)
{
    EXPECT_EQ(manager.getCurrentState(), GameState::MainMenu);
    EXPECT_FALSE(manager.shouldQuit());
}

TEST_F(GameStateManagerTest, AllowsValidTransitionFromMainMenuToLoading)
{
    manager.setState(GameState::Loading);
    EXPECT_EQ(manager.getCurrentState(), GameState::Loading);
}

TEST_F(GameStateManagerTest, RejectsInvalidTransitionFromMainMenuToPlaying)
{
    manager.setState(GameState::Playing);
    EXPECT_EQ(manager.getCurrentState(), GameState::MainMenu);
}

TEST_F(GameStateManagerTest, PushAndPopStateRestorePrevious)
{
    manager.setState(GameState::Loading);
    manager.setState(GameState::Playing);
    manager.pushState(GameState::Paused);
    EXPECT_EQ(manager.getCurrentState(), GameState::Paused);

    manager.popState();
    EXPECT_EQ(manager.getCurrentState(), GameState::Playing);
}

TEST_F(GameStateManagerTest, EmitsCallbackOnStateChange)
{
    GameState previous = GameState::MainMenu;
    GameState current = GameState::MainMenu;

    manager.setOnStateChangeCallback([&](GameState prev, GameState curr) {
        previous = prev;
        current = curr;
    });

    manager.setState(GameState::Loading);
    EXPECT_EQ(previous, GameState::MainMenu);
    EXPECT_EQ(current, GameState::Loading);
}

TEST_F(GameStateManagerTest, ShouldQuitWhenQuittingStateSet)
{
    manager.setState(GameState::Quitting);
    EXPECT_TRUE(manager.shouldQuit());
}
} // namespace poorcraft::core
