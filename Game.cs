using System;
using PoorCraftUltra.Core;
using PoorCraftUltra.Input;
using Silk.NET.Maths;
using Silk.NET.Windowing;

namespace PoorCraftUltra;

public sealed class Game : IDisposable
{
    private const string WindowTitle = "PoorCraft Ultra";
    private const int WindowWidth = 800;
    private const int WindowHeight = 600;
    private const double MaxFrameTime = GameTime.FixedTimeStep * 5.0;

    private readonly GameWindow _window;
    private readonly GameTime _gameTime = new();

    private InputManager? _inputManager;
    private bool _isRunning;
    private double _accumulator;

    public Game()
    {
        var options = WindowOptions.Default;
        options.Title = WindowTitle;
        options.Size = new Vector2D<int>(WindowWidth, WindowHeight);
        options.WindowBorder = WindowBorder.Resizable;
        options.IsVisible = true;
        options.VSync = false;

        _window = new GameWindow(options);
        _window.Load += OnLoad;
        _window.Update += OnUpdate;
        _window.Render += OnRender;
        _window.Closing += OnClosing;
    }

    public void Run()
    {
        _window.Run();
    }

    private void OnLoad()
    {
        _gameTime.Reset();
        _accumulator = 0d;
        _isRunning = true;

        _inputManager = new InputManager(_window.CreateInput());
    }

    private void OnUpdate(double _)
    {
        if (!_isRunning)
        {
            _window.Close();
            return;
        }

        if (_inputManager is null)
        {
            return;
        }

        var elapsedSeconds = _gameTime.GetElapsedSeconds();
        elapsedSeconds = Math.Clamp(elapsedSeconds, 0d, MaxFrameTime);
        _accumulator += elapsedSeconds;

        while (_accumulator >= GameTime.FixedTimeStep && _isRunning)
        {
            FixedUpdate(GameTime.FixedTimeStep);
            _accumulator -= GameTime.FixedTimeStep;
        }

        _inputManager.Update();

        if (!_isRunning)
        {
            _window.Close();
        }
    }

    private void FixedUpdate(double fixedDeltaTime)
    {
        if (_inputManager!.IsExitRequested())
        {
            _isRunning = false;
            return;
        }

        // Future game logic will be placed here using fixedDeltaTime.
    }

    private void OnRender(double _)
    {
        if (!_isRunning)
        {
            return;
        }

        var alpha = Math.Clamp(_accumulator / GameTime.FixedTimeStep, 0d, 1d);
        Render(alpha);

        _gameTime.CalculateFramePacing();
    }

    private void Render(double interpolationAlpha)
    {
        // Rendering pipeline will be implemented in future phases.
    }

    private void OnClosing()
    {
        _isRunning = false;
    }

    public void Dispose()
    {
        _window.Load -= OnLoad;
        _window.Update -= OnUpdate;
        _window.Render -= OnRender;
        _window.Closing -= OnClosing;

        _inputManager?.Dispose();
        _window.Dispose();
    }
}
