using System;
using PoorCraftUltra.Core;
using PoorCraftUltra.Input;
using Serilog;
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

    private readonly GameWindow _window;
    private readonly GameTime _gameTime = new();

    private InputManager? _inputManager;
    private bool _isRunning;
    private double _accumulator;
    private int _frameCounter;

    public Game()
    {
        _logger.Information("Initializing game...");

        var options = WindowOptions.Default;
        options.Title = WindowTitle;
        options.Size = new Vector2D<int>(WindowWidth, WindowHeight);
        options.WindowBorder = WindowBorder.Resizable;
        options.IsVisible = true;
        options.VSync = false;

        _logger.Debug("Window configuration: Title={Title}, Size={Width}x{Height}, Border={Border}, VSync={VSync}",
            options.Title,
            options.Size.X,
            options.Size.Y,
            options.WindowBorder,
            options.VSync);

        try
        {
            _window = new GameWindow(options);
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
            _logger.Information("Input manager initialized");

            _logger.Information("Game loaded and ready");
        }
        catch (Exception ex)
        {
            _logger.Error(ex, "Error during game load");
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

            if (!_isRunning)
            {
                _logger.Information("Game stopping...");
                _window.Close();
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

        _logger.Information("Game disposed");
    }
}
