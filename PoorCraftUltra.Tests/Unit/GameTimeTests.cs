using System;
using System.Diagnostics;
using System.Threading;
using FluentAssertions;
using PoorCraftUltra.Core;
using Xunit;

namespace PoorCraftUltra.Tests.Unit;

public sealed class GameTimeTests
{
    [Fact]
    public void Target_Constants_Are_Correct()
    {
        GameTime.TargetFps.Should().Be(60);
        GameTime.FixedTimeStep.Should().BeApproximately(1.0 / 60.0, 1e-12);
        GameTime.TicksPerSecond.Should().Be(Stopwatch.Frequency);
    }

    [Fact]
    public void GetElapsedSeconds_Advances_Monotonically()
    {
        var gameTime = new GameTime();

        Thread.Sleep(2);
        var first = gameTime.GetElapsedSeconds();

        Thread.Sleep(4);
        var second = gameTime.GetElapsedSeconds();

        first.Should().BeGreaterThan(0);
        second.Should().BeGreaterThan(first);
        gameTime.ElapsedSeconds.Should().Be(second);
    }

    [Fact]
    public void Reset_Clears_Elapsed_State()
    {
        var gameTime = new GameTime();
        gameTime.GetElapsedSeconds();

        gameTime.Reset();

        gameTime.ElapsedTicks.Should().Be(0);
        gameTime.ElapsedSeconds.Should().Be(0);
        gameTime.FrameStartTicks.Should().Be(gameTime.LastFrameTicks);
    }

    [Fact]
    public void CalculateFramePacing_Waits_Near_Target_Frame_Time()
    {
        var gameTime = new GameTime();

        // Prime the timer so CalculateFramePacing has a recent frame boundary.
        gameTime.GetElapsedSeconds();

        var stopwatch = Stopwatch.StartNew();
        gameTime.CalculateFramePacing();
        stopwatch.Stop();

        stopwatch.Elapsed.TotalMilliseconds.Should().BeGreaterThan(5);
        stopwatch.Elapsed.TotalMilliseconds.Should().BeLessThan(50);
    }

    [Fact]
    public void CalculateFramePacing_Reduce_Drift_Over_Multiple_Frames()
    {
        var gameTime = new GameTime();
        var totalElapsed = 0d;

        for (var i = 0; i < 5; i++)
        {
            gameTime.GetElapsedSeconds();
            var frameStopwatch = Stopwatch.StartNew();
            gameTime.CalculateFramePacing();
            frameStopwatch.Stop();
            totalElapsed += frameStopwatch.Elapsed.TotalMilliseconds;
        }

        var averageFrameDuration = totalElapsed / 5d;
        averageFrameDuration.Should().BeApproximately(GameTime.FixedTimeStep * 1000d, 8);
    }
}
