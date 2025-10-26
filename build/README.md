# Build & Distribution Guide

## Overview

This directory contains the cross-platform build and packaging system for PoorCraft Ultra. The build scripts wrap `dotnet publish` with platform-specific packaging steps so the game can be distributed as native installers or portable archives on Windows, Linux, and macOS. GitHub Actions workflows automate these steps for every push and tagged release.

**Supported Outputs**

- **Windows**: Portable ZIP, NSIS installer
- **Linux**: Debian package (`.deb`), AppImage, portable tarball
- **macOS**: `.app` bundle, DMG disk image, portable ZIP

All packages are produced as self-contained builds, bundling the .NET runtime and Silk.NET native libraries.

## Directory Layout

```
build/
├── windows/
│   ├── build.ps1              # Windows build and packaging script
│   ├── installer.nsi          # NSIS installer definition
│   └── output/                # Generated Windows artifacts
├── linux/
│   ├── build.sh               # Linux build script (.deb, AppImage, tarball)
│   ├── AppRun                 # AppImage launcher script
│   ├── poorcraftultra.desktop # Desktop entry used by packages
│   └── output/                # Generated Linux artifacts
├── macos/
│   ├── build.sh               # macOS build script (.app, DMG, ZIP)
│   ├── Info.plist.template    # App bundle metadata template
│   └── output/                # Generated macOS artifacts
└── icons/
    ├── poorcraftultra.ico     # Windows icon
    ├── poorcraftultra.icns    # macOS icon
    └── poorcraftultra-*.png   # Linux icon set
```

## Prerequisites

Install the following before running the platform scripts locally:

### Common

- [.NET 8 SDK](https://dotnet.microsoft.com/download) (required for all platforms)

### Windows

- Windows PowerShell 5+ or PowerShell Core
- [NSIS](https://nsis.sourceforge.io/) (for installer creation)
- Optional: 7-Zip or Explorer for ZIP extraction

### Linux

- `bash`
- `dpkg-deb` (package `dpkg-dev`)
- `curl`
- `fuse` (required by AppImage)
- Optional: AppImageLauncher for integration testing

### macOS

- `bash`
- `hdiutil` (bundled with macOS)
- Xcode Command Line Tools (for `hdiutil` and utilities)

## Manual Build Instructions

### Windows

```powershell
# Build portable package and prepare NSIS staging
powershell -File build/windows/build.ps1

# Compile NSIS installer (requires NSIS in PATH)
makensis /DVERSION=0.1.0 build/windows/installer.nsi
```

Artifacts will appear under `build/windows/output/`.

### Linux

```bash
# Produce .deb package, AppImage, and portable tarball
bash build/linux/build.sh
```

Artifacts will appear under `build/linux/output/`.

### macOS

```bash
# Produce .app bundle, DMG, and portable ZIP
bash build/macos/build.sh
```

Artifacts will appear under `build/macos/output/`.

## Version Management

The project version is defined in [`PoorCraftUltra.csproj`](../PoorCraftUltra.csproj) via the `<Version>` property. All build scripts and GitHub Actions workflows read this value to name output artifacts. Update the version before creating official builds or tagging releases.

To create a release:

1. Update `<Version>` in `PoorCraftUltra.csproj`.
2. Commit changes.
3. Tag the commit: `git tag v0.1.0`
4. Push tag: `git push origin v0.1.0`

The release workflow builds all packages and attaches them to the GitHub release automatically.

## Continuous Integration

Two GitHub Actions workflows orchestrate automated builds:

- [`build.yml`](../.github/workflows/build.yml) — runs on every push and pull request. Executes tests first, then builds on Windows, Linux, and macOS and uploads artifacts.
- [`release.yml`](../.github/workflows/release.yml) — runs when a tag matching `v*` is pushed. Builds all packages and uploads them as release assets.

Artifacts retain self-contained binaries suitable for testing or distribution.

## Manual Testing

Before publishing releases, run through the manual playtest checklist in [`PLAYTEST.md`](../PLAYTEST.md). The guide covers installation, launch, input handling, and performance validation across all supported platforms.

## Troubleshooting

| Issue | Resolution |
|-------|------------|
| `dotnet` not found | Install .NET 8 SDK and ensure it is on PATH. |
| Missing NSIS | Install NSIS from the official site or via package manager (Chocolatey on Windows). |
| `dpkg-deb` missing | Install `dpkg-dev`: `sudo apt-get install dpkg-dev`. |
| AppImage fails to run | Ensure FUSE is installed (`sudo apt-get install fuse`). |
| macOS Gatekeeper blocks app | Right-click the app, choose **Open**, then **Open** again to bypass unsigned warning. |
| Native library errors | Clean previous outputs and rebuild. Ensure `<IncludeNativeLibrariesForSelfExtract>` is `true` in the project file. |

## Contributing

- Keep scripts idempotent—running them multiple times should produce clean outputs.
- Update this README when adding new packaging targets.
- Test on actual hardware/VMs where possible.
- Coordinate version bumps with the team to avoid conflicting tags.
