using System;
using System.Threading;
using System.Threading.Tasks;
using PoorCraftUltra.Core;
using PoorCraftUltra.Input;
using Serilog;
using Silk.NET.Input;
using Silk.NET.Maths;
using Silk.NET.Windowing;

namespace PoorCraftUltra;

public sealed class Game : IDisposable
{
    private const string WindowTitle = "PoorCraft Ultra";
    private const int WindowWidth = 800;
    private const int WindowHeight = 600;
    private const double MaxFrameTime = GameTime.FixedTimeStep * 5.0;

    private static readonly ILogger _logger = Logger.ForContext<Game>();

    private readonly WindowOptions _windowOptions;
    private readonly GameWindow _window;
    private readonly GameTime _gameTime = new();
    private readonly TaskCompletionSource<InputManager> _inputReadyTcs = new(TaskCreationOptions.RunContinuationsAsynchronously);

    private InputManager? _inputManager;
    private bool _isRunning;
    private double _accumulator;
    private int _frameCounter;

    public event Action? Loaded;
    public event Action<double>? Updated;
    public event Action<double>? FixedUpdated;
    public event Action<double>? Rendered;
    public event Action? Stopped;

    public Game(WindowOptions? windowOptions = null)
    {
        _logger.Information("Initializing game...");

        _windowOptions = windowOptions ?? CreateDefaultWindowOptions();

        _logger.Debug("Window configuration: Title={Title}, Size={Width}x{Height}, Border={Border}, VSync={VSync}",
            _windowOptions.Title,
            _windowOptions.Size.X,
            _windowOptions.Size.Y,
            _windowOptions.WindowBorder,
            _windowOptions.VSync);

        try
        {
            _window = new GameWindow(_windowOptions);
        }
        catch (Exception ex)
        {
            _logger.Fatal(ex, "Failed to create game window");
            throw;
        }

        _window.Load += OnLoad;
        _window.Update += OnUpdate;
        _window.Render += OnRender;
        _window.Closing += OnClosing;

        _logger.Information("Game initialized successfully");
    }

    public WindowOptions WindowOptions => _windowOptions;

    public bool IsRunning => _isRunning;

    internal InputManager? InputManager => _inputManager;

    internal Task<InputManager> WaitForInputManagerAsync(TimeSpan? timeout = null, CancellationToken cancellationToken = default)
    {
        if (_inputManager is not null)
        {
            return Task.FromResult(_inputManager);
        }

        var task = _inputReadyTcs.Task;

        if (timeout is { } timeoutValue)
        {
            return task.WaitAsync(timeoutValue, cancellationToken);
        }

        return task.WaitAsync(cancellationToken);
    }

    internal async Task SimulateKeyPressAsync(Key key, CancellationToken cancellationToken = default)
    {
        var manager = await WaitForInputManagerAsync(null, cancellationToken).ConfigureAwait(false);
        manager.SimulateKeyDown(key);
        manager.SimulateKeyUp(key);
    }

    public void Run()
    {
        _logger.Information("Starting game loop...");

        try
        {
            _window.Run();
        }
        catch (Exception ex)
        {
            _logger.Error(ex, "Unhandled exception during game loop execution");
            throw;
        }
        finally
        {
            _logger.Information("Game loop ended");
        }
    }

    public void RequestExit(string reason = "Exit requested")
    {
        if (!_isRunning)
        {
            return;
        }

        _logger.Information("Exit requested programmatically: {Reason}", reason);
        _isRunning = false;
        _window.Close();
    }

    private void OnLoad()
    {
        try
        {
            _logger.Information("Game loading...");

            _gameTime.Reset();
            _accumulator = 0d;
            _frameCounter = 0;
            _isRunning = true;

            _inputManager = new InputManager(_window.CreateInput());
            _inputReadyTcs.TrySetResult(_inputManager);
            _logger.Information("Input manager initialized");

            _logger.Information("Game loaded and ready");

            Loaded?.Invoke();
        }
        catch (Exception ex)
        {
            _logger.Error(ex, "Error during game load");
            _inputReadyTcs.TrySetException(ex);
            throw;
        }
    }

    private void OnUpdate(double _)
    {
        try
        {
            if (!_isRunning)
            {
                _logger.Debug("Update invoked while game is stopping; requesting window close");
                _window.Close();
                return;
            }

            if (_inputManager is null)
            {
                _logger.Warning("Update called before input manager was initialized");
                return;
            }

            var elapsedSeconds = _gameTime.GetElapsedSeconds();
            elapsedSeconds = Math.Clamp(elapsedSeconds, 0d, MaxFrameTime);
            _accumulator += elapsedSeconds;

            var actualFps = elapsedSeconds > double.Epsilon ? 1d / elapsedSeconds : 0d;

            while (_accumulator >= GameTime.FixedTimeStep && _isRunning)
            {
                FixedUpdate(GameTime.FixedTimeStep);
                _accumulator -= GameTime.FixedTimeStep;
            }

            _inputManager.Update();

            _frameCounter++;
            if (_frameCounter % 60 == 0)
            {
                _logger.Debug("FPS: {ActualFps:F2}, Accumulator: {Accumulator:F4}s", actualFps, _accumulator);
            }

            Updated?.Invoke(elapsedSeconds);

            if (!_isRunning)
            {
                _logger.Information("Game stopping...");
                _window.Close();
                Stopped?.Invoke();
            }
        }
        catch (Exception ex)
        {
            _logger.Error(ex, "Unhandled exception during game update");
            throw;
        }
    }

    private void FixedUpdate(double fixedDeltaTime)
    {
        if (_logger.IsEnabled(Serilog.Events.LogEventLevel.Verbose))
        {
            _logger.Verbose("Fixed update: {FixedDeltaTime:F4}s", fixedDeltaTime);
        }

        if (_inputManager!.IsExitRequested())
        {
            _logger.Information("Exit requested by user (ESC key)");
            _isRunning = false;
            return;
        }

        // Future game logic will be placed here using fixedDeltaTime.

        FixedUpdated?.Invoke(fixedDeltaTime);
    }

    private void OnRender(double _)
    {
        try
        {
            if (!_isRunning)
            {
                return;
            }

            var alpha = Math.Clamp(_accumulator / GameTime.FixedTimeStep, 0d, 1d);

            if (_logger.IsEnabled(Serilog.Events.LogEventLevel.Verbose))
            {
                _logger.Verbose("Rendering frame (alpha: {InterpolationAlpha:F4})", alpha);
            }

            Render(alpha);

            _gameTime.CalculateFramePacing();

            Rendered?.Invoke(alpha);
        }
        catch (Exception ex)
        {
            _logger.Error(ex, "Unhandled exception during rendering");
            throw;
        }
    }

    private void Render(double interpolationAlpha)
    {
        // Rendering pipeline will be implemented in future phases.
    }

    private void OnClosing()
    {
        _logger.Information("Window closing event received");
        _isRunning = false;
    }

    public void Dispose()
    {
        _logger.Information("Disposing game resources...");

        _window.Load -= OnLoad;
        _window.Update -= OnUpdate;
        _window.Render -= OnRender;
        _window.Closing -= OnClosing;

        _inputManager?.Dispose();
        _window.Dispose();

        _inputReadyTcs.TrySetCanceled();

        _logger.Information("Game disposed");
    }

    internal static WindowOptions CreateDefaultWindowOptions()
    {
        var options = WindowOptions.Default;
        options.Title = WindowTitle;
        options.Size = new Vector2D<int>(WindowWidth, WindowHeight);
        options.WindowBorder = WindowBorder.Resizable;
        options.IsVisible = true;
        options.VSync = false;
        return options;
    }
}
