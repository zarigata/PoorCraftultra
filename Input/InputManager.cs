using System;
using System.Collections.Concurrent;
using PoorCraftUltra.Core;
using Serilog;
using Serilog.Events;
using Silk.NET.Input;
using Silk.NET.Maths;

namespace PoorCraftUltra.Input;

public sealed class InputManager : IDisposable
{
    private static readonly ILogger _logger = Logger.ForContext<InputManager>();

    private readonly IInputContext _context;
    private readonly IKeyboard? _keyboard;
    private readonly IMouse? _mouse;

    private readonly ConcurrentDictionary<Key, KeyState> _keyStates = new();
    private Vector2D<float> _mousePosition;
    private Vector2D<float> _mouseDelta;
    private bool _exitLogged;

    public InputManager(IInputContext context)
    {
        _logger.Information("Initializing input manager...");

        _context = context;
        _keyboard = _context.Keyboards.Count > 0 ? _context.Keyboards[0] : null;
        _mouse = _context.Mice.Count > 0 ? _context.Mice[0] : null;

        var hasKeyboard = _keyboard is not null;
        var hasMouse = _mouse is not null;
        _logger.Information("Keyboard detected: {HasKeyboard}", hasKeyboard);
        _logger.Information("Mouse detected: {HasMouse}", hasMouse);

        if (_keyboard is not null)
        {
            _keyboard.KeyDown += HandleKeyDown;
            _keyboard.KeyUp += HandleKeyUp;
        }

        if (_mouse is not null)
        {
            _mouse.MouseMove += HandleMouseMove;
            _mouse.MouseDown += HandleMouseDown;
            _mouse.MouseUp += HandleMouseUp;
            _mouse.Scroll += HandleMouseScroll;
        }

        _logger.Information("Input manager initialized");
    }

    public void Update()
    {
        foreach (var key in _keyStates.Keys)
        {
            _keyStates.AddOrUpdate(key,
                _ => KeyState.Released,
                (_, state) => state switch
                {
                    KeyState.PressedThisFrame => KeyState.Held,
                    KeyState.ReleasedThisFrame => KeyState.Released,
                    _ => state
                });
        }
        _mouseDelta = Vector2D<float>.Zero;

        if (_logger.IsEnabled(LogEventLevel.Verbose))
        {
            var activeKeys = 0;
            foreach (var state in _keyStates.Values)
            {
                if (state is KeyState.PressedThisFrame or KeyState.Held)
                {
                    activeKeys++;
                }
            }

            _logger.Verbose("Input state updated (active keys: {KeyCount})", activeKeys);
        }

        if (!IsKeyDownInternal(Key.Escape))
        {
            _exitLogged = false;
        }
    }

    public bool IsKeyPressed(Key key) => _keyStates.TryGetValue(key, out var state) && state == KeyState.PressedThisFrame;

    public bool IsKeyDown(Key key) => _keyStates.TryGetValue(key, out var state) && (state == KeyState.PressedThisFrame || state == KeyState.Held);

    public bool IsKeyReleased(Key key) => _keyStates.TryGetValue(key, out var state) && state == KeyState.ReleasedThisFrame;

    public bool IsExitRequested()
    {
        var exitRequested = IsKeyPressed(Key.Escape);
        if (exitRequested && !_exitLogged)
        {
            _logger.Information("Exit requested via ESC key");
            _exitLogged = true;
        }

        return exitRequested;
    }

    public Vector2D<float> GetMousePosition() => _mousePosition;

    public Vector2D<float> GetMouseDelta() => _mouseDelta;

    public bool IsMouseButtonDown(MouseButton button)
    {
        if (_mouse is null)
        {
            return false;
        }

        return _mouse.IsButtonPressed(button);
    }

    private void HandleKeyDown(IKeyboard keyboard, Key key, int _)
    {
        try
        {
            _keyStates.AddOrUpdate(key,
                _ => KeyState.PressedThisFrame,
                (_, state) => state is KeyState.Released or KeyState.ReleasedThisFrame ? KeyState.PressedThisFrame : state);

            if (ShouldLogKey(key) && _logger.IsEnabled(LogEventLevel.Debug))
            {
                _logger.Debug("Key pressed: {Key}", key);
            }
        }
        catch (Exception ex)
        {
            _logger.Error(ex, "Error handling key down for {Key}", key);
            throw;
        }
    }

    private void HandleKeyUp(IKeyboard keyboard, Key key, int _)
    {
        try
        {
            _keyStates.AddOrUpdate(key,
                _ => KeyState.ReleasedThisFrame,
                (_, state) => state is KeyState.Held or KeyState.PressedThisFrame ? KeyState.ReleasedThisFrame : state);

            if (ShouldLogKey(key) && _logger.IsEnabled(LogEventLevel.Debug))
            {
                _logger.Debug("Key released: {Key}", key);
            }
        }
        catch (Exception ex)
        {
            _logger.Error(ex, "Error handling key up for {Key}", key);
            throw;
        }
    }

    private void HandleMouseMove(IMouse mouse, Vector2 position)
    {
        try
        {
            var newPosition = new Vector2D<float>(position.X, position.Y);
            _mouseDelta = newPosition - _mousePosition;
            _mousePosition = newPosition;

            if (_logger.IsEnabled(LogEventLevel.Verbose))
            {
                _logger.Verbose("Mouse moved to ({X:F2}, {Y:F2}), delta: ({DeltaX:F2}, {DeltaY:F2})",
                    _mousePosition.X,
                    _mousePosition.Y,
                    _mouseDelta.X,
                    _mouseDelta.Y);
            }
        }
        catch (Exception ex)
        {
            _logger.Error(ex, "Error handling mouse move event");
            throw;
        }
    }

    private void HandleMouseDown(IMouse mouse, MouseButton button)
    {
        try
        {
            if (_logger.IsEnabled(LogEventLevel.Debug))
            {
                _logger.Debug("Mouse button {Button} pressed", button);
            }

            // Placeholder for future mouse button state tracking.
        }
        catch (Exception ex)
        {
            _logger.Error(ex, "Error handling mouse button press for {Button}", button);
            throw;
        }
    }

    private void HandleMouseUp(IMouse mouse, MouseButton button)
    {
        try
        {
            if (_logger.IsEnabled(LogEventLevel.Debug))
            {
                _logger.Debug("Mouse button {Button} released", button);
            }

            // Placeholder for future mouse button state tracking.
        }
        catch (Exception ex)
        {
            _logger.Error(ex, "Error handling mouse button release for {Button}", button);
            throw;
        }
    }

    private void HandleMouseScroll(IMouse mouse, ScrollWheel scroll)
    {
        try
        {
            if (_logger.IsEnabled(LogEventLevel.Debug))
            {
                _logger.Debug("Mouse scrolled: {ScrollX:F2}, {ScrollY:F2}", scroll.X, scroll.Y);
            }

            // Placeholder for future mouse scroll handling.
        }
        catch (Exception ex)
        {
            _logger.Error(ex, "Error handling mouse scroll event");
            throw;
        }
    }

    public void Dispose()
    {
        _logger.Information("Disposing input manager...");

        if (_keyboard is not null)
        {
            _keyboard.KeyDown -= HandleKeyDown;
            _keyboard.KeyUp -= HandleKeyUp;
        }

        if (_mouse is not null)
        {
            _mouse.MouseMove -= HandleMouseMove;
            _mouse.MouseDown -= HandleMouseDown;
            _mouse.MouseUp -= HandleMouseUp;
            _mouse.Scroll -= HandleMouseScroll;
        }

        _context.Dispose();

        _logger.Information("Input manager disposed");
    }

    private bool IsKeyDownInternal(Key key)
    {
        return _keyStates.TryGetValue(key, out var state) && (state == KeyState.PressedThisFrame || state == KeyState.Held);
    }

    private static bool ShouldLogKey(Key key)
    {
        return key == Key.Escape || (key >= Key.F1 && key <= Key.F24) || key == Key.Enter || key == Key.Space;
    }

    private enum KeyState
    {
        Released,
        PressedThisFrame,
        Held,
        ReleasedThisFrame
    }
}
