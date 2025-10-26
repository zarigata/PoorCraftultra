using System;
using Silk.NET.Input;
using Silk.NET.Maths;
using Silk.NET.Windowing;
using Silk.NET.Windowing.Extensions;

namespace PoorCraftUltra;

public sealed class GameWindow : IDisposable
{
    private readonly IWindow _window;

    public event Action? Load;
    public event Action<double>? Update;
    public event Action<double>? Render;
    public event Action? Closing;

    public GameWindow(WindowOptions options)
    {
        _window = Window.Create(options);
        _window.Load += HandleLoad;
        _window.Update += HandleUpdate;
        _window.Render += HandleRender;
        _window.Closing += HandleClosing;
    }

    public bool IsOpen => !_window.IsClosing;
    public int Width => _window.Size.X;
    public int Height => _window.Size.Y;

    public void Run() => _window.Run();

    public void Close() => _window.Close();

    public IInputContext CreateInput() => _window.CreateInput();

    private void HandleLoad()
    {
        _window.Center();
        Load?.Invoke();
    }

    private void HandleUpdate(double delta) => Update?.Invoke(delta);

    private void HandleRender(double delta) => Render?.Invoke(delta);

    private void HandleClosing() => Closing?.Invoke();

    public void Dispose()
    {
        _window.Load -= HandleLoad;
        _window.Update -= HandleUpdate;
        _window.Render -= HandleRender;
        _window.Closing -= HandleClosing;
        _window.Dispose();
    }
}
