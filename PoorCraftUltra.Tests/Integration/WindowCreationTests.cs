using System;
using System.Threading.Tasks;
using FluentAssertions;
using PoorCraftUltra.Tests.TestUtilities;
using Serilog;
using Silk.NET.Windowing;
using Xunit;
using Xunit.Abstractions;

namespace PoorCraftUltra.Tests.Integration;

[Collection("WindowTests")] // Prevent parallel window tests
[Trait("Category", "Integration")]
[Trait("Category", "RequiresDisplay")]
public sealed class WindowCreationTests : IDisposable
{
    private readonly ILogger _logger;
    private WindowTestHarness? _harness;

    public WindowCreationTests(ITestOutputHelper output)
    {
        _logger = TestLogger.CreateTestContext<WindowCreationTests>(output);
    }

    [Fact]
    public async Task Window_Opens_Successfully()
    {
        _harness = new WindowTestHarness();
        _harness.StartWindow();

        await _harness.WaitForWindowReadyAsync();

        _harness.IsWindowOpen.Should().BeTrue();
        _harness.WindowWidth.Should().Be(800);
        _harness.WindowHeight.Should().Be(600);

        await _harness.StopWindowAsync();
    }

    [Fact]
    public async Task Window_Closes_Cleanly()
    {
        _harness = new WindowTestHarness();
        _harness.StartWindow();
        await _harness.WaitForWindowReadyAsync();

        await _harness.StopWindowAsync();

        _harness.IsWindowOpen.Should().BeFalse();
    }

    [Fact]
    public async Task Window_Handles_Multiple_Open_Close_Cycles()
    {
        for (var i = 0; i < 3; i++)
        {
            using var harness = new WindowTestHarness();
            harness.StartWindow();
            await harness.WaitForWindowReadyAsync();
            await harness.StopWindowAsync();
            harness.IsWindowOpen.Should().BeFalse();
        }
    }

    [Fact]
    public async Task Window_Initializes_Within_Timeout()
    {
        _harness = new WindowTestHarness();
        _harness.StartWindow();

        var start = DateTime.UtcNow;
        await _harness.WaitForWindowReadyAsync(TimeSpan.FromSeconds(3));
        var elapsed = DateTime.UtcNow - start;

        elapsed.Should().BeLessThan(TimeSpan.FromSeconds(3));
        await _harness.StopWindowAsync();
    }

    [Fact]
    public async Task Window_Properties_Are_Correct()
    {
        var options = WindowOptions.Default;
        options.Title = "Custom Test Window";
        options.Size = new Silk.NET.Maths.Vector2D<int>(1024, 768);
        options.IsVisible = false;

        _harness = new WindowTestHarness(options);
        _harness.StartWindow();
        await _harness.WaitForWindowReadyAsync();

        _harness.WindowWidth.Should().Be(1024);
        _harness.WindowHeight.Should().Be(768);

        await _harness.StopWindowAsync();
    }

    public void Dispose()
    {
        _harness?.Dispose();
    }
}
