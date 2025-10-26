# PoorCraft Ultra

## Overview
PoorCraft Ultra is an open-source, cross-platform voxel game engine inspired by sandbox games like Minecraft. The long-term vision includes scalable multiplayer, rich mod support, and a modern rendering pipeline. This repository represents **Phase 1** of the project: establishing the foundational systems required for a stable, high-performance game loop.

## Current Phase: Project Setup & Minimal Window
- Cross-platform .NET 8 application
- Silk.NET v2 windowing and input integration
- Fixed-timestep update loop targeting 60 FPS
- Clean shutdown when the `ESC` key is pressed

## Requirements
- .NET 8.0 SDK or later
- Windows 10+, most modern Linux distributions (X11/Wayland), or macOS 10.15+
- Optional IDEs: Visual Studio 2022, JetBrains Rider, or VS Code

## Getting Started
1. Restore dependencies: `dotnet restore`
2. Build the project: `dotnet build`
3. Run the game: `dotnet run`

## Project Structure
```
PoorCraftUltra.csproj   // Main project file
Program.cs              // Application entry point
Game.cs                 // Core game lifecycle and loop
GameWindow.cs           // Silk.NET window wrapper
Core/
  GameTime.cs           // High-resolution timing and frame pacing
Input/
  InputManager.cs       // Input abstraction for keyboard and mouse
```

## Controls
- `ESC`: Exit the game window

## Architecture Highlights
- Fixed-timestep update loop (60 Hz) with accumulator
- Variable render step for smooth visuals
- Silk.NET handles cross-platform windowing and input
- Modular design ready for rendering, physics, networking, and mod systems

## Roadmap
This project is planned across numerous phases. Future milestones include:
1. Rendering pipeline initialization
2. Voxel system and chunk management
3. Procedural world generation
4. Multiplayer networking layer
5. Modding and scripting support

## Contributing
Contributions are welcome! Please follow standard C# coding conventions and ensure all changes remain cross-platform compatible. Tests and documentation updates are encouraged with each contribution.

## License
This project is licensed under the terms specified in the [LICENSE](LICENSE) file.
