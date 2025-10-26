using System;
using System.Threading.Tasks;
using FluentAssertions;
using PoorCraftUltra;
using PoorCraftUltra.Core;
using PoorCraftUltra.Tests.TestUtilities;
using Serilog;
using Silk.NET.Windowing;
using Xunit;
using Xunit.Abstractions;

namespace PoorCraftUltra.Tests.Integration;

[Collection("GameLoopTests")]
[Trait("Category", "Integration")]
[Trait("Category", "RequiresDisplay")]
public sealed class GameLoopTests : IDisposable
{
    private readonly ILogger _logger;
    private readonly Game _game;
    private readonly FpsCounterHarness _fpsHarness;

    private Task? _gameTask;

    public GameLoopTests(ITestOutputHelper output)
    {
        _logger = TestLogger.CreateTestContext<GameLoopTests>(output);

        var options = WindowOptions.Default;
        options.IsVisible = false;
        options.Title = "Game Loop Test";

        _game = new Game(options);
        _fpsHarness = new FpsCounterHarness();
    }

    [Fact]
    public async Task GameLoop_Runs_At_Target_Fps()
    {
        await StartGameAsync();
        await _fpsHarness.WaitForWarmupAsync();

        var stats = await _fpsHarness.MeasureFpsAsync(TimeSpan.FromSeconds(2));

        stats.AverageFps.Should().BeInRange(58.0, 62.0);
        stats.StandardDeviation.Should().BeLessThan(2.0);

        await StopGameAsync();
    }

    [Fact]
    public async Task GameLoop_Maintains_Fixed_Timestep()
    {
        await StartGameAsync();

        var fixedDeltas = await _fpsHarness.CaptureFixedUpdatesAsync(100);
        foreach (var delta in fixedDeltas)
        {
            delta.Should().BeApproximately(GameTime.FixedTimeStep, 0.0001);
        }

        await StopGameAsync();
    }

    [Fact]
    public async Task GameLoop_Frame_Pacing_Is_Accurate()
    {
        await StartGameAsync();
        await _fpsHarness.WaitForWarmupAsync();

        var percentiles = await _fpsHarness.MeasureFrameTimePercentilesAsync(TimeSpan.FromSeconds(2));

        percentiles[50].Should().BeApproximately(16.7, 0.5);
        percentiles[95].Should().BeLessThan(18.0);
        percentiles[99].Should().BeLessThan(20.0);

        await StopGameAsync();
    }

    [Fact]
    [Trait("Category", "Slow")]
    public async Task GameLoop_Runs_For_Extended_Period()
    {
        await StartGameAsync();

        var stats = await _fpsHarness.MeasureFpsAsync(TimeSpan.FromSeconds(10));

        stats.AverageFps.Should().BeInRange(58.0, 62.0);

        await StopGameAsync();
    }

    private Task StartGameAsync()
    {
        _logger.Information("Starting game for test...");

        _fpsHarness.Attach(_game);

        var tcs = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);

        _game.Loaded += () => tcs.TrySetResult(true);

        _gameTask = Task.Run(() =>
        {
            try
            {
                _game.Run();
            }
            catch (Exception ex)
            {
                _logger.Error(ex, "Game run loop threw an exception");
                tcs.TrySetException(ex);
            }
        });

        return tcs.Task;
    }

    private async Task StopGameAsync()
    {
        _logger.Information("Stopping game for test...");
        _game.RequestExit("Test completion");

        if (_gameTask is not null)
        {
            await _gameTask.ConfigureAwait(false);
        }

        _fpsHarness.Detach();
    }

    public void Dispose()
    {
        _fpsHarness.Dispose();
        _game.Dispose();
    }

    private sealed class FpsCounterHarness : IDisposable
    {
        private readonly FpsCounter _counter = new();
        private readonly System.Collections.Concurrent.ConcurrentQueue<double> _fixedDeltas = new();

        private Game? _game;

        public void Attach(Game game)
        {
            _game = game;
            _game.Rendered += OnRendered;
            _game.FixedUpdated += OnFixedUpdated;
            _game.Updated += OnUpdated;
            _counter.Start();
        }

        public void Detach()
        {
            if (_game is null)
            {
                return;
            }

            _game.Rendered -= OnRendered;
            _game.FixedUpdated -= OnFixedUpdated;
            _game.Updated -= OnUpdated;
            _counter.Stop();
            _game = null;
        }

        public async Task WaitForWarmupAsync()
        {
            await Task.Delay(TimeSpan.FromSeconds(1));
        }

        public async Task<FpsCounter.FpsStatistics> MeasureFpsAsync(TimeSpan duration)
        {
            await Task.Delay(duration);
            return _counter.GetFpsStatistics();
        }

        public async Task<double[]> CaptureFixedUpdatesAsync(int count)
        {
            var results = new double[count];
            var index = 0;
            var timeout = TimeSpan.FromSeconds(5);
            var start = DateTime.UtcNow;

            while (index < count)
            {
                while (_fixedDeltas.TryDequeue(out var delta))
                {
                    results[index++] = delta;
                    if (index >= count)
                    {
                        break;
                    }
                }

                if (DateTime.UtcNow - start > timeout)
                {
                    throw new TimeoutException("Timed out waiting for fixed updates");
                }

                await Task.Delay(10);
            }

            return results;
        }

        public async Task<System.Collections.Generic.Dictionary<int, double>> MeasureFrameTimePercentilesAsync(TimeSpan duration)
        {
            await Task.Delay(duration);
            var percentiles = _counter.GetFrameTimePercentiles(50, 95, 99);
            return new System.Collections.Generic.Dictionary<int, double>(percentiles);
        }

        private void OnRendered(double _)
        {
            _counter.RecordFrame();
        }

        private void OnUpdated(double delta)
        {
            // Frame delta captured for potential future assertions.
        }

        private void OnFixedUpdated(double delta)
        {
            _fixedDeltas.Enqueue(delta);
        }

        public void Dispose()
        {
            Detach();
        }
    }
}
