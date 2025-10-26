using System;
using System.Threading;
using System.Threading.Tasks;
using FluentAssertions;
using PoorCraftUltra.Core;
using Serilog;
using Silk.NET.Windowing;

namespace PoorCraftUltra.Tests.TestUtilities;

public sealed class WindowTestHarness : IDisposable
{
    private static readonly ILogger _logger = Logger.ForContext<WindowTestHarness>();

    private readonly GameWindow _window;
    private readonly CancellationTokenSource _cts = new();

    private Task? _windowTask;
    private TaskCompletionSource<bool>? _loadCompletion;

    public WindowTestHarness(WindowOptions? options = null)
    {
        Logger.Initialize();
        _logger.Information("Creating window test harness...");

        var opts = options ?? WindowOptions.Default;
        opts.IsVisible = false;
        opts.Title ??= "Test Window";

        _window = new GameWindow(opts);
        _window.Load += OnWindowLoad;
        _window.Render += OnWindowRender;
        _window.Closing += OnWindowClosing;

        _logger.Information("Window test harness created");
    }

    public bool IsWindowOpen { get; private set; }

    public int WindowWidth => _window.Width;

    public int WindowHeight => _window.Height;

    public int FrameCount { get; private set; }

    public TimeSpan ElapsedTime => GameTimeInstance.TotalElapsed;

    private GameTime GameTimeInstance { get; } = new();

    public void StartWindow()
    {
        if (_windowTask is not null)
        {
            throw new InvalidOperationException("Window already started");
        }

        _logger.Information("Starting test window on background thread...");
        _loadCompletion = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);

        _windowTask = Task.Run(() =>
        {
            try
            {
                _window.Run();
            }
            catch (Exception ex)
            {
                _logger.Error(ex, "Window run loop threw an exception");
                _loadCompletion?.TrySetException(ex);
            }
        }, _cts.Token);
    }

    public async Task WaitForWindowReadyAsync(TimeSpan? timeout = null)
    {
        if (_loadCompletion is null)
        {
            throw new InvalidOperationException("Window has not been started");
        }

        var effectiveTimeout = timeout ?? TimeSpan.FromSeconds(5);

        _logger.Information("Waiting for window to signal ready state (timeout: {Timeout}ms)...", effectiveTimeout.TotalMilliseconds);
        using var timeoutCts = new CancellationTokenSource(effectiveTimeout);

        var completedTask = await Task.WhenAny(_loadCompletion.Task, Task.Delay(Timeout.InfiniteTimeSpan, timeoutCts.Token)).ConfigureAwait(false);
        if (completedTask != _loadCompletion.Task)
        {
            _logger.Error("Window failed to load within {Timeout}ms", effectiveTimeout.TotalMilliseconds);
            throw new TimeoutException("Window did not become ready within the allotted timeout");
        }

        await _loadCompletion.Task.ConfigureAwait(false);
        _logger.Information("Window ready");
    }

    public async Task StopWindowAsync(TimeSpan? timeout = null)
    {
        if (_windowTask is null)
        {
            return;
        }

        _logger.Information("Stopping test window...");
        _window.Close();

        var effectiveTimeout = timeout ?? TimeSpan.FromSeconds(5);
        var completed = await Task.WhenAny(_windowTask, Task.Delay(effectiveTimeout)).ConfigureAwait(false);
        if (completed != _windowTask)
        {
            _logger.Error("Window did not close within {Timeout}ms", effectiveTimeout.TotalMilliseconds);
            throw new TimeoutException("Window did not close within the allotted timeout");
        }

        _logger.Information("Window stopped");
    }

    public async Task<double> MeasureFpsAsync(TimeSpan duration)
    {
        _logger.Information("Measuring FPS over {Duration}ms", duration.TotalMilliseconds);
        var startFrame = FrameCount;
        var startTime = DateTime.UtcNow;

        await Task.Delay(duration).ConfigureAwait(false);

        var frames = FrameCount - startFrame;
        var elapsed = DateTime.UtcNow - startTime;
        var fps = frames / elapsed.TotalSeconds;

        _logger.Information("Measured FPS: {Fps:F2} over {Elapsed}ms", fps, elapsed.TotalMilliseconds);
        return fps;
    }

    public async Task WaitForFramesAsync(int frameCount, TimeSpan? timeout = null)
    {
        var targetFrame = FrameCount + frameCount;
        var effectiveTimeout = timeout ?? TimeSpan.FromSeconds(5);
        var start = DateTime.UtcNow;

        _logger.Information("Waiting for {FrameCount} frames to render", frameCount);

        while (FrameCount < targetFrame)
        {
            if (DateTime.UtcNow - start > effectiveTimeout)
            {
                _logger.Error("Timed out waiting for frames");
                throw new TimeoutException("Timed out waiting for frames");
            }

            await Task.Delay(10).ConfigureAwait(false);
        }

        _logger.Information("Frame wait complete");
    }

    public Task SimulateKeyPressAsync(Silk.NET.Input.Key key)
    {
        _logger.Warning("Input simulation is not implemented. This method is a placeholder.");
        return Task.CompletedTask;
    }

    private void OnWindowLoad()
    {
        try
        {
            _logger.Information("Test window load event received");
            IsWindowOpen = true;
            GameTimeInstance.Reset();
            _loadCompletion?.TrySetResult(true);
        }
        catch (Exception ex)
        {
            _logger.Error(ex, "Error in window load handler");
            _loadCompletion?.TrySetException(ex);
            throw;
        }
    }

    private void OnWindowRender(double delta)
    {
        FrameCount++;
        GameTimeInstance.GetElapsedSeconds();
    }

    private void OnWindowClosing()
    {
        _logger.Information("Test window closing event received");
        IsWindowOpen = false;
    }

    public void Dispose()
    {
        _logger.Information("Disposing window test harness...");

        _window.Load -= OnWindowLoad;
        _window.Render -= OnWindowRender;
        _window.Closing -= OnWindowClosing;

        _cts.Cancel();

        try
        {
            _windowTask?.Wait(TimeSpan.FromSeconds(1));
        }
        catch (Exception ex)
        {
            _logger.Warning(ex, "Exception while waiting for window task during disposal");
        }

        _window.Dispose();
        _cts.Dispose();

        _logger.Information("Window test harness disposed");
    }
}
