using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;

namespace PoorCraftUltra.Tests.TestUtilities;

public sealed class FpsCounter
{
    private readonly Stopwatch _stopwatch = new();
    private readonly List<double> _frameTimes = new();

    private double _lastFrameTime;

    public int FrameCount { get; private set; }

    public TimeSpan Duration => _stopwatch.Elapsed;

    public void Start()
    {
        _frameTimes.Clear();
        FrameCount = 0;
        _lastFrameTime = 0d;
        _stopwatch.Restart();
    }

    public void RecordFrame()
    {
        if (!_stopwatch.IsRunning)
        {
            throw new InvalidOperationException("FPS counter has not been started.");
        }

        var elapsedTotal = _stopwatch.Elapsed.TotalSeconds;
        if (FrameCount > 0)
        {
            var frameTime = elapsedTotal - _lastFrameTime;
            if (frameTime > 0)
            {
                _frameTimes.Add(frameTime);
            }
        }

        _lastFrameTime = elapsedTotal;
        FrameCount++;
    }

    public double GetAverageFps()
    {
        if (Duration.TotalSeconds <= 0)
        {
            return 0;
        }

        return FrameCount / Duration.TotalSeconds;
    }

    public double GetInstantaneousFps()
    {
        if (_frameTimes.Count == 0)
        {
            return 0;
        }

        var lastFrame = _frameTimes[^1];
        return lastFrame > 0 ? 1d / lastFrame : 0d;
    }

    public FpsStatistics GetFpsStatistics()
    {
        if (_frameTimes.Count == 0)
        {
            return new FpsStatistics(0, 0, 0, 0, 0, Duration);
        }

        var fpsValues = _frameTimes.Select(ft => ft > 0 ? 1d / ft : 0d).ToArray();
        var average = fpsValues.Average();
        var min = fpsValues.Min();
        var max = fpsValues.Max();
        var stdDev = CalculateStandardDeviation(fpsValues, average);

        return new FpsStatistics(average, min, max, stdDev, FrameCount, Duration);
    }

    public IReadOnlyDictionary<int, double> GetFrameTimePercentiles(params int[] percentiles)
    {
        if (_frameTimes.Count == 0)
        {
            return percentiles.ToDictionary(p => p, _ => 0d);
        }

        return percentiles.ToDictionary(p => p, p => CalculatePercentile(_frameTimes, p));
    }

    public void Stop()
    {
        _stopwatch.Stop();
    }

    private static double CalculateStandardDeviation(IReadOnlyList<double> values, double mean)
    {
        if (values.Count <= 1)
        {
            return 0;
        }

        var variance = values.Sum(v => Math.Pow(v - mean, 2)) / values.Count;
        return Math.Sqrt(variance);
    }

    private static double CalculatePercentile(IReadOnlyList<double> sortedValues, int percentile)
    {
        if (sortedValues.Count == 0)
        {
            return 0d;
        }

        var ordered = sortedValues.OrderBy(v => v).ToArray();
        var position = (percentile / 100d) * (ordered.Length - 1);
        var lowerIndex = (int)Math.Floor(position);
        var upperIndex = (int)Math.Ceiling(position);

        if (lowerIndex == upperIndex)
        {
            return ordered[lowerIndex] * 1000.0;
        }

        var weight = position - lowerIndex;
        var interpolated = ordered[lowerIndex] * (1 - weight) + ordered[upperIndex] * weight;
        return interpolated * 1000.0;
    }

    public record FpsStatistics(
        double AverageFps,
        double MinFps,
        double MaxFps,
        double StandardDeviation,
        int FrameCount,
        TimeSpan Duration);
}
