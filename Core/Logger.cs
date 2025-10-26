using System;
using System.IO;
using Serilog;
using Serilog.Core;
using Serilog.Events;

namespace PoorCraftUltra.Core;

public static class Logger
{
    private static readonly object SyncRoot = new();
    private static ILogger? _instance;
    private static LoggingLevelSwitch? _levelSwitch;

    static Logger()
    {
        Initialize();
    }

    public static ILogger Instance => _instance ?? throw new InvalidOperationException("Logger has not been initialized.");

    public static ILogger ForContext<T>() => Instance.ForContext<T>();

    public static ILogger ForContext(Type sourceType)
    {
        if (sourceType is null)
        {
            throw new ArgumentNullException(nameof(sourceType));
        }

        return Instance.ForContext(sourceType);
    }

    public static void Initialize()
    {
        lock (SyncRoot)
        {
            if (_instance is not null)
            {
                return;
            }

            Directory.CreateDirectory(Path.Combine(AppContext.BaseDirectory, "logs"));

            _levelSwitch = new LoggingLevelSwitch(GetMinimumLevelFromEnvironment());

            _instance = new LoggerConfiguration()
                .MinimumLevel.ControlledBy(_levelSwitch)
                .Enrich.FromLogContext()
                .Enrich.WithThreadId()
                .Enrich.WithMachineName()
                .WriteTo.Console(outputTemplate: "[{Timestamp:HH:mm:ss.fff} {Level:u3}] {Message:lj}{NewLine}{Exception}")
                .WriteTo.File(
                    path: Path.Combine("logs", "poorcraftultra-.log"),
                    rollingInterval: RollingInterval.Day,
                    retainedFileCountLimit: 31,
                    outputTemplate: "[{Timestamp:HH:mm:ss.fff} {Level:u3}] {Message:lj}{NewLine}{Exception}")
                .WriteTo.Debug(outputTemplate: "[{Timestamp:HH:mm:ss.fff} {Level:u3}] {Message:lj}{NewLine}{Exception}")
                .CreateLogger();

            Log.Logger = _instance;
        }
    }

    public static void Close()
    {
        lock (SyncRoot)
        {
            if (_instance is null)
            {
                return;
            }

            Log.CloseAndFlush();
            _instance = null;
            _levelSwitch = null;
        }
    }

    private static LogEventLevel GetMinimumLevelFromEnvironment()
    {
        var configuredLevel = Environment.GetEnvironmentVariable("POORCRAFTULTRA_LOG_LEVEL");
        if (string.IsNullOrWhiteSpace(configuredLevel))
        {
            return LogEventLevel.Debug;
        }

        return Enum.TryParse(configuredLevel, ignoreCase: true, out LogEventLevel level)
            ? level
            : LogEventLevel.Debug;
    }
}
