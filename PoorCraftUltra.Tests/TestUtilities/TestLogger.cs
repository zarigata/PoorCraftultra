using System;
using System.IO;
using System.Threading;
using Serilog;
using Serilog.Events;
using Serilog.Sinks.XUnit.Injectable;
using Xunit.Abstractions;

namespace PoorCraftUltra.Tests.TestUtilities;

public static class TestLogger
{
    private static readonly AsyncLocal<ITestOutputHelper?> OutputAccessor = new();
    private static readonly object SyncRoot = new();
    private static ILogger? _logger;

    public static ILogger ConfigureForTest(ITestOutputHelper output)
    {
        if (output is null)
        {
            throw new ArgumentNullException(nameof(output));
        }

        OutputAccessor.Value = output;
        EnsureLoggerInitialized();
        return _logger!;
    }

    public static ILogger ConfigureForIntegrationTest(ITestOutputHelper output, string testName)
    {
        if (output is null)
        {
            throw new ArgumentNullException(nameof(output));
        }

        if (string.IsNullOrWhiteSpace(testName))
        {
            throw new ArgumentException("Test name must be provided", nameof(testName));
        }

        OutputAccessor.Value = output;

        var logDirectory = Path.Combine(AppContext.BaseDirectory, "test-logs");
        Directory.CreateDirectory(logDirectory);

        var logFilePath = Path.Combine(logDirectory, $"{SanitizeFileName(testName)}-{DateTime.UtcNow:yyyyMMdd-HHmmss}.log");

        return new LoggerConfiguration()
            .MinimumLevel.Verbose()
            .Enrich.WithProperty("TestName", testName)
            .WriteTo.Debug(outputTemplate: "[{Timestamp:HH:mm:ss.fff} {Level:u3}] {Message:lj}{NewLine}{Exception}")
            .WriteTo.XUnit(() => OutputAccessor.Value ?? output)
            .WriteTo.File(
                logFilePath,
                outputTemplate: "[{Timestamp:HH:mm:ss.fff} {Level:u3}] {Message:lj}{NewLine}{Exception}",
                shared: true)
            .CreateLogger();
    }

    public static ILogger CreateTestContext<T>(ITestOutputHelper output)
    {
        var logger = ConfigureForTest(output);
        return logger.ForContext<T>();
    }

    private static void EnsureLoggerInitialized()
    {
        if (_logger is not null)
        {
            return;
        }

        lock (SyncRoot)
        {
            if (_logger is not null)
            {
                return;
            }

            Directory.CreateDirectory(Path.Combine(AppContext.BaseDirectory, "test-logs"));

            _logger = new LoggerConfiguration()
                .MinimumLevel.Verbose()
                .WriteTo.Debug(outputTemplate: "[{Timestamp:HH:mm:ss.fff} {Level:u3}] {Message:lj}{NewLine}{Exception}")
                .WriteTo.XUnit(() => OutputAccessor.Value ?? throw new InvalidOperationException("Test output helper is not set."))
                .CreateLogger();
        }
    }

    private static string SanitizeFileName(string value)
    {
        foreach (var invalid in Path.GetInvalidFileNameChars())
        {
            value = value.Replace(invalid, '_');
        }

        return value;
    }
}
