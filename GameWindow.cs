using System;
using PoorCraftUltra.Core;
using Serilog;
using Silk.NET.Input;
using Silk.NET.Maths;
using Silk.NET.Windowing;
using Silk.NET.Windowing.Extensions;

namespace PoorCraftUltra;

public sealed class GameWindow : IDisposable
{
    private static readonly ILogger _logger = Logger.ForContext<GameWindow>();
    private readonly IWindow _window;

    public event Action? Load;
    public event Action<double>? Update;
    public event Action<double>? Render;
    public event Action? Closing;

    public GameWindow(WindowOptions options)
    {
        try
        {
            _logger.Information("Creating game window...");
            _logger.Debug("Window options: Size={Width}x{Height}, Title={Title}, Border={Border}, VSync={VSync}",
                options.Size.X,
                options.Size.Y,
                options.Title,
                options.WindowBorder,
                options.VSync);

            _window = Window.Create(options);
        }
        catch (Exception ex)
        {
            _logger.Fatal(ex, "Failed to create Silk.NET window");
            throw;
        }

        _window.Load += HandleLoad;
        _window.Update += HandleUpdate;
        _window.Render += HandleRender;
        _window.Closing += HandleClosing;

        _logger.Information("Game window created successfully");
    }

    public bool IsOpen => !_window.IsClosing;
    public int Width => _window.Size.X;
    public int Height => _window.Size.Y;

    public void Run()
    {
        _logger.Information("Starting window main loop...");
        try
        {
            _window.Run();
        }
        catch (Exception ex)
        {
            _logger.Error(ex, "Unhandled exception during window run loop");
            throw;
        }
        finally
        {
            _logger.Information("Window main loop ended");
        }
    }

    public void Close()
    {
        _logger.Information("Requesting window close...");
        _window.Close();
    }

    public IInputContext CreateInput() => _window.CreateInput();

    private void HandleLoad()
    {
        _window.Center();
        _logger.Information("Window loaded and centered at ({X}, {Y})", _window.Position.X, _window.Position.Y);
        Load?.Invoke();
    }

    private void HandleUpdate(double delta)
    {
        if (_logger.IsEnabled(Serilog.Events.LogEventLevel.Verbose))
        {
            _logger.Verbose("Window update (delta: {Delta:F4}s)", delta);
        }

        Update?.Invoke(delta);
    }

    private void HandleRender(double delta)
    {
        if (_logger.IsEnabled(Serilog.Events.LogEventLevel.Verbose))
        {
            _logger.Verbose("Window render (delta: {Delta:F4}s)", delta);
        }

        Render?.Invoke(delta);
    }

    private void HandleClosing()
    {
        _logger.Information("Window closing requested");
        Closing?.Invoke();
    }

    public void Dispose()
    {
        _logger.Information("Disposing window resources...");
        _window.Load -= HandleLoad;
        _window.Update -= HandleUpdate;
        _window.Render -= HandleRender;
        _window.Closing -= HandleClosing;
        _window.Dispose();
        _logger.Information("Window disposed");
    }
}
