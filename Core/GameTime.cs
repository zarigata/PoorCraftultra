using System;
using System.Diagnostics;
using System.Threading;

namespace PoorCraftUltra.Core;

public sealed class GameTime
{
    public const int TargetFps = 60;
    public const double FixedTimeStep = 1.0 / TargetFps;

    private static readonly long TargetFrameTicks = Stopwatch.Frequency / TargetFps;

    private readonly Stopwatch _stopwatch = Stopwatch.StartNew();
    private long _frameStartTicks;
    private long _lastFrameTicks;
    private long _elapsedTicks;

    public GameTime()
    {
        Reset();
    }

    public static long TicksPerSecond => Stopwatch.Frequency;

    public double TargetFrameTime => FixedTimeStep;

    public TimeSpan TotalElapsed => TimeSpan.FromSeconds(_stopwatch.ElapsedTicks / (double)Stopwatch.Frequency);

    public long FrameStartTicks => _frameStartTicks;

    public long LastFrameTicks => _lastFrameTicks;

    public long ElapsedTicks => _elapsedTicks;

    public double ElapsedSeconds => _elapsedTicks / (double)Stopwatch.Frequency;

    public void Reset()
    {
        _stopwatch.Restart();
        _frameStartTicks = _stopwatch.ElapsedTicks;
        _lastFrameTicks = _frameStartTicks;
        _elapsedTicks = 0;
    }

    public double GetElapsedSeconds()
    {
        var currentTicks = _stopwatch.ElapsedTicks;
        _frameStartTicks = _lastFrameTicks;
        _elapsedTicks = currentTicks - _lastFrameTicks;
        _lastFrameTicks = currentTicks;
        return ElapsedSeconds;
    }

    public long GetElapsedTicks()
    {
        var currentTicks = _stopwatch.ElapsedTicks;
        _frameStartTicks = _lastFrameTicks;
        _elapsedTicks = currentTicks - _lastFrameTicks;
        _lastFrameTicks = currentTicks;
        return _elapsedTicks;
    }

    public void CalculateFramePacing()
    {
        var targetTick = _lastFrameTicks + TargetFrameTicks;
        var spinWait = new SpinWait();

        while (true)
        {
            var currentTicks = _stopwatch.ElapsedTicks;
            var ticksRemaining = targetTick - currentTicks;
            if (ticksRemaining <= 0)
            {
                break;
            }

            var msRemaining = ticksRemaining * 1000.0 / Stopwatch.Frequency;
            if (msRemaining > 2.0)
            {
                Thread.Sleep(1);
                continue;
            }

            if (msRemaining > 0.1)
            {
                Thread.Yield();
                Thread.SpinWait(50);
                continue;
            }

            spinWait.SpinOnce();
        }
    }
}
