# PoorCraft Ultra

## Overview
PoorCraft Ultra is an open-source, cross-platform voxel game engine inspired by sandbox experiences like Minecraft. The long-term roadmap includes scalable multiplayer, rich mod support, and a modern rendering pipeline. This repository now reflects **Phase 2** of the project: delivering a robust build and distribution system alongside the core game loop.

## Current Phase: Build System & Distribution (Phase 2)
- ✅ Cross-platform window creation and fixed 60 FPS game loop
- ✅ Silk.NET input handling with ESC shutdown
- ✅ Comprehensive xUnit test suite and logging via Serilog
- ✅ Platform build scripts for Windows, Linux, and macOS
- ✅ Automated CI/CD with GitHub Actions
- ✅ Platform installers/packages (NSIS, .deb, AppImage, DMG)

## Downloads
- Latest release: [GitHub Releases](https://github.com/yourusername/PoorCraftUltra/releases)
- Windows: Installer (.exe) & Portable (.zip)
- Linux: Debian package (.deb), AppImage, Portable (.tar.gz)
- macOS: Disk image (.dmg) & Portable (.zip)

## Requirements
- .NET 8.0 SDK or later (for building from source)
- Windows 10+, modern Linux (X11/Wayland), or macOS 10.15+
- Optional IDEs: Visual Studio 2022, JetBrains Rider, VS Code

## Getting Started
```bash
dotnet restore
dotnet build
dotnet run
```

### Running Pre-Built Binaries
Download platform builds from the Releases page and follow the install/run instructions in [PLAYTEST.md](PLAYTEST.md).

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
build/
  windows/              # Windows build scripts & NSIS config
  linux/                # Linux build scripts & packaging assets
  macos/                # macOS build scripts & bundle assets
  icons/                # Application icons for all platforms
.github/workflows/      # GitHub Actions CI/CD workflows
PoorCraftUltra.Tests/   # Automated test project
logs/                   # Runtime logs (gitignored)
```

## Controls
- `ESC`: Exit the game window

## Architecture Highlights
- Fixed-timestep update loop (60 Hz) with accumulator
- Variable render step for smooth visuals
- Silk.NET handles cross-platform windowing and input
- Modular design ready for rendering, physics, networking, and mod systems

## Building from Source

### Prerequisites
- .NET 8.0 SDK or later
- Platform-specific packaging tools (see [build/README.md](build/README.md))

### Quick Build
```bash
git clone https://github.com/yourusername/PoorCraftUltra.git
cd PoorCraftUltra
dotnet restore
dotnet build
dotnet run
```

### Platform Packages
Detailed packaging instructions live in [build/README.md](build/README.md).

## Testing
```bash
dotnet test

# Unit tests only
dotnet test --filter "Category!=Integration"

# Integration tests only
dotnet test --filter "Category=Integration"
```

## Continuous Integration
This project uses GitHub Actions for automated testing and builds:
- **Build Workflow:** Runs on every push and PR (`.github/workflows/build.yml`)
- **Release Workflow:** Publishes tagged releases (`.github/workflows/release.yml`)

![Build Status](https://github.com/yourusername/PoorCraftUltra/workflows/build/badge.svg)

## Manual Testing
Refer to [PLAYTEST.md](PLAYTEST.md) for the full manual playtest checklist covering Windows, Linux, and macOS installers and portable builds.

## Roadmap
Phase progression:
- ✅ Phase 1: Core loop & windowing
- ✅ Phase 2: Build system & distribution
- ☐ Phase 3: Basic 3D rendering pipeline (up next)
- ☐ Phase 4+: Voxel systems, world gen, networking, modding

## Contributing
Contributions are welcome! Please follow standard C# coding conventions and ensure all changes remain cross-platform compatible. Tests and documentation updates are encouraged with each contribution.

## License
This project is licensed under the terms specified in the [LICENSE](LICENSE) file.
