#include "poorcraft/core/Timer.h"

#include <cmath>
#include <gtest/gtest.h>
#include <thread>

namespace
{
constexpr double kEpsilon = 10.0; // generous tolerance due to timing variability
constexpr double kFrameDurationSeconds = 0.016;
} // namespace

TEST(TimerTest, InitialState)
{
    poorcraft::core::Timer timer;
    EXPECT_DOUBLE_EQ(timer.getFPS(), 0.0);
    EXPECT_DOUBLE_EQ(timer.getInstantFPS(), 0.0);
    EXPECT_DOUBLE_EQ(timer.getDeltaTime(), 0.0);
}

TEST(TimerTest, DeltaTime)
{
    poorcraft::core::Timer timer;
    std::this_thread::sleep_for(std::chrono::milliseconds(16));
    timer.tick();
    EXPECT_NEAR(timer.getDeltaTime(), kFrameDurationSeconds, 0.01);
}

TEST(TimerTest, InstantFPS)
{
    poorcraft::core::Timer timer;
    std::this_thread::sleep_for(std::chrono::milliseconds(16));
    timer.tick();
    EXPECT_NEAR(timer.getInstantFPS(), 60.0, kEpsilon);
}

TEST(TimerTest, SmoothedFPS)
{
    poorcraft::core::Timer timer;
    for(int i = 0; i < 40; ++i)
    {
        std::this_thread::sleep_for(std::chrono::milliseconds(16));
        timer.tick();
    }
    EXPECT_NEAR(timer.getFPS(), 60.0, kEpsilon);
}
