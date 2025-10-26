using System;
using System.Collections.Concurrent;
using Silk.NET.Input;
using Silk.NET.Maths;

namespace PoorCraftUltra.Input;

public sealed class InputManager : IDisposable
{
    private readonly IInputContext _context;
    private readonly IKeyboard? _keyboard;
    private readonly IMouse? _mouse;

    private readonly ConcurrentDictionary<Key, KeyState> _keyStates = new();
    private Vector2D<float> _mousePosition;
    private Vector2D<float> _mouseDelta;

    public InputManager(IInputContext context)
    {
        _context = context;
        _keyboard = _context.Keyboards.Count > 0 ? _context.Keyboards[0] : null;
        _mouse = _context.Mice.Count > 0 ? _context.Mice[0] : null;

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
    }

    public bool IsKeyPressed(Key key) => _keyStates.TryGetValue(key, out var state) && state == KeyState.PressedThisFrame;

    public bool IsKeyDown(Key key) => _keyStates.TryGetValue(key, out var state) && (state == KeyState.PressedThisFrame || state == KeyState.Held);

    public bool IsKeyReleased(Key key) => _keyStates.TryGetValue(key, out var state) && state == KeyState.ReleasedThisFrame;

    public bool IsExitRequested() => IsKeyPressed(Key.Escape);

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
        _keyStates.AddOrUpdate(key,
            _ => KeyState.PressedThisFrame,
            (_, state) => state is KeyState.Released or KeyState.ReleasedThisFrame ? KeyState.PressedThisFrame : state);
    }

    private void HandleKeyUp(IKeyboard keyboard, Key key, int _)
    {
        _keyStates.AddOrUpdate(key,
            _ => KeyState.ReleasedThisFrame,
            (_, state) => state is KeyState.Held or KeyState.PressedThisFrame ? KeyState.ReleasedThisFrame : state);
    }

    private void HandleMouseMove(IMouse mouse, Vector2 position)
    {
        var newPosition = new Vector2D<float>(position.X, position.Y);
        _mouseDelta = newPosition - _mousePosition;
        _mousePosition = newPosition;
    }

    private void HandleMouseDown(IMouse mouse, MouseButton button)
    {
        // Placeholder for future mouse button state tracking.
    }

    private void HandleMouseUp(IMouse mouse, MouseButton button)
    {
        // Placeholder for future mouse button state tracking.
    }

    private void HandleMouseScroll(IMouse mouse, ScrollWheel scroll)
    {
        // Placeholder for future mouse scroll handling.
    }

    public void Dispose()
    {
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
    }

    private enum KeyState
    {
        Released,
        PressedThisFrame,
        Held,
        ReleasedThisFrame
    }
}
