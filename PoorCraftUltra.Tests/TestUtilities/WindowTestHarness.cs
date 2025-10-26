using System;
using System.Threading;
using System.Threading.Tasks;
using PoorCraftUltra.Core;
using Silk.NET.Maths;
using Silk.NET.Windowing;

namespace PoorCraftUltra.Tests.TestUtilities;

public sealed class WindowTestHarness : IDisposable
{
    private readonly IWindow _window;
    private readonly CancellationTokenSource _cts = new();

    private Task? _windowTask;
    private TaskCompletionSource<bool>? _loadCompletion;

    public WindowTestHarness(WindowOptions? options = null)
    {
        var opts = options ?? WindowOptions.Default;
        if (options is null)
        {
            opts.Size = new Vector2D<int>(800, 600);
        }
        opts.IsVisible = false;
        opts.Title ??= "Test Window";

        _window = Window.Create(opts);
        _window.Load += OnWindowLoad;
        _window.Render += OnWindowRender;
        _window.Closing += OnWindowClosing;
    }

    public bool IsWindowOpen { get; private set; }

    public int WindowWidth => _window.Size.X;

    public int WindowHeight => _window.Size.Y;

    public int FrameCount { get; private set; }

    public TimeSpan ElapsedTime => GameTimeInstance.TotalElapsed;

    private GameTime GameTimeInstance { get; } = new();

    public void StartWindow()
    {
        if (_windowTask is not null)
        {
            throw new InvalidOperationException("Window already started");
        }

        _loadCompletion = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);

        _windowTask = Task.Run(() =>
        {
            try
            {
                _window.Run();
            }
            catch (Exception ex)
            {
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

        using var timeoutCts = new CancellationTokenSource(effectiveTimeout);

        var completedTask = await Task.WhenAny(_loadCompletion.Task, Task.Delay(Timeout.InfiniteTimeSpan, timeoutCts.Token)).ConfigureAwait(false);
        if (completedTask != _loadCompletion.Task)
        {
            throw new TimeoutException("Window did not become ready within the allotted timeout");
        }

        await _loadCompletion.Task.ConfigureAwait(false);
    }

    public async Task StopWindowAsync(TimeSpan? timeout = null)
    {
        if (_windowTask is null)
        {
            return;
        }

        _window.Close();

        var effectiveTimeout = timeout ?? TimeSpan.FromSeconds(5);
        var completed = await Task.WhenAny(_windowTask, Task.Delay(effectiveTimeout)).ConfigureAwait(false);
        if (completed != _windowTask)
        {
            throw new TimeoutException("Window did not close within the allotted timeout");
        }

    }

    public async Task<double> MeasureFpsAsync(TimeSpan duration)
    {
        var startFrame = FrameCount;
        var startTime = DateTime.UtcNow;

        await Task.Delay(duration).ConfigureAwait(false);

        var frames = FrameCount - startFrame;
        var elapsed = DateTime.UtcNow - startTime;
        var fps = frames / elapsed.TotalSeconds;

        return fps;
    }

    public async Task WaitForFramesAsync(int frameCount, TimeSpan? timeout = null)
    {
        var targetFrame = FrameCount + frameCount;
        var effectiveTimeout = timeout ?? TimeSpan.FromSeconds(5);
        var start = DateTime.UtcNow;

        while (FrameCount < targetFrame)
        {
            if (DateTime.UtcNow - start > effectiveTimeout)
            {
                throw new TimeoutException("Timed out waiting for frames");
            }

            await Task.Delay(10).ConfigureAwait(false);
        }
    }

    public async Task SimulateKeyPressAsync(Game game, Silk.NET.Input.Key key, CancellationToken cancellationToken = default)
    {
        if (game is null)
        {
            throw new ArgumentNullException(nameof(game));
        }

        await game.WaitForInputManagerAsync(TimeSpan.FromSeconds(5), cancellationToken).ConfigureAwait(false);
        await game.SimulateKeyPressAsync(key, cancellationToken).ConfigureAwait(false);
    }

    private void OnWindowLoad()
    {
        try
        {
            IsWindowOpen = true;
            GameTimeInstance.Reset();
            _loadCompletion?.TrySetResult(true);
        }
        catch (Exception ex)
        {
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
        IsWindowOpen = false;
    }

    public void Dispose()
    {
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
            _ = ex;
        }

        _window.Dispose();
        _cts.Dispose();
    }
}
