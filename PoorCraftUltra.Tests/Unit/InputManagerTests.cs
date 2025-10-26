using System;
using FluentAssertions;
using Moq;
using PoorCraftUltra.Input;
using Silk.NET.Input;
using Xunit;

namespace PoorCraftUltra.Tests.Unit;

public sealed class InputManagerTests
{
    [Fact]
    public void KeyDown_SetsPressedState()
    {
        var manager = CreateManager(out var contextMock, out var keyboardMock);

        try
        {
            keyboardMock.Raise(k => k.KeyDown += null!, keyboardMock.Object, Key.Space, 0);

            manager.IsKeyPressed(Key.Space).Should().BeTrue();
            manager.IsKeyDown(Key.Space).Should().BeTrue();
        }
        finally
        {
            manager.Dispose();
        }

        contextMock.Verify(c => c.Dispose(), Times.Once);
    }

    [Fact]
    public void Update_Transitions_Pressed_To_Held()
    {
        var manager = CreateManager(out _, out var keyboardMock);

        try
        {
            keyboardMock.Raise(k => k.KeyDown += null!, keyboardMock.Object, Key.F1, 0);
            manager.IsKeyPressed(Key.F1).Should().BeTrue();

            manager.Update();

            manager.IsKeyPressed(Key.F1).Should().BeFalse();
            manager.IsKeyDown(Key.F1).Should().BeTrue();
        }
        finally
        {
            manager.Dispose();
        }
    }

    [Fact]
    public void KeyUp_Registers_Release_And_Clears_After_Update()
    {
        var manager = CreateManager(out _, out var keyboardMock);

        try
        {
            keyboardMock.Raise(k => k.KeyDown += null!, keyboardMock.Object, Key.Escape, 0);
            manager.Update();

            keyboardMock.Raise(k => k.KeyUp += null!, keyboardMock.Object, Key.Escape, 0);

            manager.IsKeyReleased(Key.Escape).Should().BeTrue();
            manager.IsKeyDown(Key.Escape).Should().BeFalse();

            manager.Update();

            manager.IsKeyReleased(Key.Escape).Should().BeFalse();
            manager.IsKeyDown(Key.Escape).Should().BeFalse();
        }
        finally
        {
            manager.Dispose();
        }
    }

    [Fact]
    public void SimulateKeyPress_SetsTransientStates()
    {
        var manager = CreateManager(out _, out _);

        try
        {
            manager.SimulateKeyPress(Key.Enter);

            manager.IsKeyReleased(Key.Enter).Should().BeTrue();
            manager.IsKeyDown(Key.Enter).Should().BeFalse();

            manager.Update();

            manager.IsKeyReleased(Key.Enter).Should().BeFalse();
        }
        finally
        {
            manager.Dispose();
        }
    }

    [Fact]
    public void Escape_Key_Sets_Exit_Request_Flag()
    {
        var manager = CreateManager(out _, out var keyboardMock);

        try
        {
            keyboardMock.Raise(k => k.KeyDown += null!, keyboardMock.Object, Key.Escape, 0);

            manager.IsExitRequested().Should().BeTrue();

            manager.Update();

            manager.IsExitRequested().Should().BeFalse();
        }
        finally
        {
            manager.Dispose();
        }
    }

    private static InputManager CreateManager(out Mock<IInputContext> contextMock, out Mock<IKeyboard> keyboardMock)
    {
        contextMock = new Mock<IInputContext>(MockBehavior.Strict);
        keyboardMock = new Mock<IKeyboard>();

        var keyboards = new[] { keyboardMock.Object };
        var mice = Array.Empty<IMouse>();

        contextMock.SetupGet(c => c.Keyboards).Returns(keyboards);
        contextMock.SetupGet(c => c.Mice).Returns(mice);
        contextMock.Setup(c => c.Dispose());

        return new InputManager(contextMock.Object);
    }
}
