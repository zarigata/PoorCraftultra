using System;
using System.Threading.Tasks;
using FluentAssertions;
using PoorCraftUltra;
using PoorCraftUltra.Tests.TestUtilities;
using Serilog;
using Silk.NET.Input;
using Silk.NET.Windowing;
using Xunit;
using Xunit.Abstractions;

namespace PoorCraftUltra.Tests.Integration;

[Collection("InputTests")]
[Trait("Category", "Integration")]
[Trait("Category", "RequiresDisplay")]
public sealed class InputTests : IDisposable
{
    private readonly ILogger _logger;
    private readonly Game _game;
    private Task? _gameTask;

    public InputTests(ITestOutputHelper output)
    {
        _logger = TestLogger.CreateTestContext<InputTests>(output);

        var options = WindowOptions.Default;
        options.IsVisible = false;
        options.Title = "Input Test";

        _game = new Game(options);
    }

    [Fact]
    public async Task Esc_Key_Closes_Window_Cleanly()
    {
        await StartGameAsync();
        await SimulateEscAndAwaitExitAsync();
        _game.IsRunning.Should().BeFalse();
    }

    [Fact]
    public async Task Esc_Key_Stops_Game_Loop()
    {
        await StartGameAsync();
        await Task.Delay(TimeSpan.FromMilliseconds(200));
        _game.IsRunning.Should().BeTrue();

        await SimulateEscAndAwaitExitAsync();
        _game.IsRunning.Should().BeFalse();
    }

    [Fact]
    public async Task Multiple_Esc_Presses_Do_Not_Crash()
    {
        await StartGameAsync();

        for (var i = 0; i < 3; i++)
        {
            await SimulateEscAsync();
            await Task.Delay(TimeSpan.FromMilliseconds(10));
        }

        await AwaitWindowCloseAsync();
        _game.IsRunning.Should().BeFalse();
    }

    [Fact]
    public async Task Esc_During_Initialization_Closes_Cleanly()
    {
        StartGameWithoutAwaiting();
        await SimulateEscAsync();

        await AwaitWindowCloseAsync();
        _game.IsRunning.Should().BeFalse();
    }

    public void Dispose()
    {
        _game.Dispose();
    }

    private void StartGameWithoutAwaiting()
    {
        _gameTask = Task.Run(() =>
        {
            try
            {
                _game.Run();
            }
            catch (Exception ex)
            {
                _logger.Error(ex, "Game loop threw during input test");
            }
        });
    }

    private async Task StartGameAsync()
    {
        StartGameWithoutAwaiting();
        await _game.WaitForInputManagerAsync(TimeSpan.FromSeconds(5)).ConfigureAwait(false);
    }

    private Task SimulateEscAsync()
    {
        return _game.SimulateKeyPressAsync(Key.Escape);
    }

    private async Task SimulateEscAndAwaitExitAsync()
    {
        await SimulateEscAsync().ConfigureAwait(false);
        await AwaitWindowCloseAsync();
    }

    private async Task AwaitWindowCloseAsync()
    {
        if (_gameTask is null)
        {
            return;
        }

        await _gameTask.ConfigureAwait(false);
    }
}
