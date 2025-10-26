using System;
using System.Diagnostics;
using System.Threading;
using PoorCraftUltra.Core;
using Serilog;
using Serilog.Events;

namespace PoorCraftUltra.Core;

public sealed class GameTime
{
    public const int TargetFps = 60;
    public const double FixedTimeStep = 1.0 / TargetFps;

    private static readonly long TargetFrameTicks = Stopwatch.Frequency / TargetFps;
    private static readonly ILogger _logger = Logger.ForContext<GameTime>();

    private readonly Stopwatch _stopwatch = Stopwatch.StartNew();
    private long _frameStartTicks;
    private long _lastFrameTicks;
    private long _elapsedTicks;
    private long _frameCounter;
    private double _pacingErrorAccum;
    private int _errorOver1ms;
    private int _errorOver2ms;
    private int _errorOver5ms;

    public GameTime()
    {
        Reset();
        _logger.Information("Initializing game time...");
        _logger.Information("Target FPS: {TargetFps}, Fixed timestep: {FixedTimeStep:F6}s", TargetFps, FixedTimeStep);
        _logger.Information("Game time initialized");
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
        _frameCounter = 0;
        _pacingErrorAccum = 0d;
        _errorOver1ms = 0;
        _errorOver2ms = 0;
        _errorOver5ms = 0;

        _logger.Debug("Game time reset");
    }

    public double GetElapsedSeconds()
    {
        var currentTicks = _stopwatch.ElapsedTicks;
        _frameStartTicks = _lastFrameTicks;
        _elapsedTicks = currentTicks - _lastFrameTicks;
        _lastFrameTicks = currentTicks;

        if (_logger.IsEnabled(LogEventLevel.Verbose))
        {
            _logger.Verbose("Frame time: {ElapsedSeconds:F6}s ({ElapsedTicks} ticks)", ElapsedSeconds, _elapsedTicks);
        }

        if (ElapsedSeconds > 0.1)
        {
            _logger.Warning("Abnormal frame time detected: {ElapsedSeconds:F3}s", ElapsedSeconds);
        }

        return ElapsedSeconds;
    }

    public long GetElapsedTicks()
    {
        var currentTicks = _stopwatch.ElapsedTicks;
        _frameStartTicks = _lastFrameTicks;
        _elapsedTicks = currentTicks - _lastFrameTicks;
        _lastFrameTicks = currentTicks;

        if (_logger.IsEnabled(LogEventLevel.Verbose))
        {
            _logger.Verbose("Frame time: {ElapsedSeconds:F6}s ({ElapsedTicks} ticks)", ElapsedSeconds, _elapsedTicks);
        }

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
                if (_logger.IsEnabled(LogEventLevel.Verbose))
                {
                    _logger.Verbose("Frame pacing: sleeping ({MsRemaining:F2}ms remaining)", msRemaining);
                }

                Thread.Sleep(1);
                continue;
            }

            if (msRemaining > 0.1)
            {
                if (_logger.IsEnabled(LogEventLevel.Verbose))
                {
                    _logger.Verbose("Frame pacing: spin-wait/yield ({MsRemaining:F4}ms remaining)", msRemaining);
                }

                Thread.Yield();
                Thread.SpinWait(50);
                continue;
            }

            spinWait.SpinOnce();
        }

        TrackFramePacing();
    }

    private void TrackFramePacing()
    {
        _frameCounter++;

        var actualTicks = _lastFrameTicks - _frameStartTicks;
        var errorTicks = actualTicks - TargetFrameTicks;
        var errorMs = errorTicks * 1000.0 / Stopwatch.Frequency;

        _pacingErrorAccum += Math.Abs(errorMs);

        if (Math.Abs(errorMs) > 1.0)
        {
            _errorOver1ms++;
        }

        if (Math.Abs(errorMs) > 2.0)
        {
            _errorOver2ms++;
        }

        if (Math.Abs(errorMs) > 5.0)
        {
            _errorOver5ms++;
        }

        if (_frameCounter % 60 == 0)
        {
            var avgError = _pacingErrorAccum / 60.0;
            _logger.Debug("Frame pacing: target={TargetTicks} ticks, actual={ActualTicks} ticks, error={Error:F2}ms",
                TargetFrameTicks,
                actualTicks,
                errorMs);

            if (avgError > 2.0)
            {
                _logger.Warning("Frame pacing unstable: average error {AvgError:F2}ms over last 60 frames", avgError);
            }

            var under1Percent = 100.0 * (60 - _errorOver1ms) / 60.0;
            var under2Percent = 100.0 * (60 - _errorOver2ms) / 60.0;
            var under5Percent = 100.0 * (60 - _errorOver5ms) / 60.0;
            var over5Percent = 100.0 - under5Percent;

            _logger.Debug("Frame pacing stats: <1ms: {Under1:F1}%, <2ms: {Under2:F1}%, <5ms: {Under5:F1}%, >5ms: {Over5:F1}%",
                under1Percent,
                under2Percent,
                under5Percent,
                over5Percent);

            _pacingErrorAccum = 0d;
            _errorOver1ms = 0;
            _errorOver2ms = 0;
            _errorOver5ms = 0;
        }
    }
}
