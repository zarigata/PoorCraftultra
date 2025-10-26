using System;
using System.Runtime;
using PoorCraftUltra.Core;

var exitCode = 0;

try
{
    Logger.Initialize();
    Logger.Instance.Information("PoorCraft Ultra starting...");

    GCSettings.LatencyMode = GCLatencyMode.SustainedLowLatency;

    using var game = new PoorCraftUltra.Game();
    game.Run();
}
catch (Exception ex)
{
    exitCode = 1;
    Logger.Instance.Fatal(ex, "Fatal error during game execution");
}
finally
{
    Logger.Instance.Information("PoorCraft Ultra shutting down...");
    Logger.Close();
}

return exitCode;
