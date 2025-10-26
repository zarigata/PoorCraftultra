using System;
using System.Runtime;

try
{
    GCSettings.LatencyMode = GCLatencyMode.SustainedLowLatency;
    using var game = new PoorCraftUltra.Game();
    game.Run();
}
catch (Exception ex)
{
    Console.Error.WriteLine($"Fatal error: {ex.Message}");
    Console.Error.WriteLine(ex);
}
