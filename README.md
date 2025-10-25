# PoorCraft – Open Source Voxel Game Engine

![CI](https://github.com/zarigata/PoorCraftultra/actions/workflows/ci.yml/badge.svg)

PoorCraft is a modern, moddable voxel engine inspired by the classics. The project targets AAA production values while remaining fully open source to empower community-driven innovation.

## Highlights

- ✅ **Cross-platform window management (SDL2)**
- ✅ **Vulkan rendering with OpenGL fallback**
- ✅ **GPU vendor detection (NVIDIA, AMD, Intel, Apple)**
- ✅ **FPS counter and VSync toggle**
- ✅ **Automated CI/CD with headless testing**
- 🔄 **Upcoming**: Deferred rendering, world generation, gameplay systems

## Project Structure

```
.
├── assets/        # Game resources (textures, shaders, audio, models)
├── cmake/         # Custom CMake modules and toolchains (future phases)
├── docs/          # Design docs, API references, contributor guides
├── include/       # Public engine headers
├── src/           # Engine executable entry point and subsystems
├── tests/         # GoogleTest-powered unit and integration tests
├── CMakeLists.txt # Root CMake configuration (CMake 3.31+)
├── CMakePresets.json
└── .github/workflows/ci.yml
```

## Getting Started

### Prerequisites

- CMake **3.31+**
- A C++17-capable toolchain (MSVC 19.3x, GCC 13+, Clang 17+)
- Git
- Ninja (Linux/macOS) – installed automatically in CI, recommended locally

### Configure, Build, Test (Using Presets)

1. **Configure**
   ```bash
   cmake --preset windows-msvc        # Windows (MSVC)
   cmake --preset linux-gcc           # Linux (GCC)
   cmake --preset linux-clang         # Linux (Clang)
   cmake --preset macos-clang         # macOS (AppleClang)
   ```

2. **Build**
   ```bash
   cmake --build --preset windows-msvc-release
   cmake --build --preset linux-gcc-release
   cmake --build --preset linux-clang-release
   cmake --build --preset macos-clang-release
   ```

3. **Test**
   ```bash
   ctest --preset windows-msvc-release
   ctest --preset linux-gcc-release
   ctest --preset linux-clang-release
   ctest --preset macos-clang-release
   ```

4. **Single-command workflows** (configure → build → test):
   ```bash
   cmake --workflow --preset windows-release-workflow
   cmake --workflow --preset linux-gcc-release-workflow
   cmake --workflow --preset linux-clang-release-workflow
   cmake --workflow --preset macos-release-workflow
   ```

Artifacts are installed under `install/<preset>` when you run `cmake --install`.

## Current Status (Phase 3 Complete)

PoorCraft now boots into a fully managed SDL2 window, selects the optimal renderer (Vulkan first, OpenGL fallback), clears the screen at >60 FPS, reports GPU vendor and capabilities, exposes a toggleable VSync, and ships with a first-person camera controller. The new input system captures keyboard and mouse state (including SDL relative mouse mode) to drive smooth WASD movement, fly-mode vertical controls, and mouse-look rotation. Continuous integration spins up Xvfb with Mesa's lavapipe for headless testing, ensuring rendering logic and foundational systems are validated on Linux alongside native Windows/macOS runs.

## Features

- ✅ Cross-platform window management (SDL2)
- ✅ Vulkan rendering with OpenGL fallback
- ✅ GPU vendor detection (NVIDIA, AMD, Intel, Apple)
- ✅ FPS counter and VSync toggle
- ✅ Input system for keyboard and mouse with relative mouse mode
- ✅ First-person camera with WASD movement and fly-mode vertical controls
- 🔄 Upcoming: Deferred rendering, world generation, gameplay systems

## Running the Engine

```bash
cmake --preset <your-configure-preset>
cmake --build --preset <your-build-preset>
```

Executables are emitted under `build/<preset>/`. Launch the `poorcraft` binary to open a window displaying a cornflower blue clear color. Use **WASD** for horizontal movement, **Space** / **Left Shift** for fly-mode vertical movement, and the mouse for camera look; press **Escape** to release the cursor. The console prints renderer selection, GPU information, and an FPS + camera position log every 60 frames.

## Testing

```bash
ctest --preset <your-test-preset>
```

Linux developers without a display can mirror CI by starting `Xvfb :99` and exporting `DISPLAY=:99` plus `VK_ICD_FILENAMES=/usr/share/vulkan/icd.d/lvp_icd.x86_64.json` before running the tests.

## Dependencies

- SDL2 **2.32.x**
- GLM **1.0.2** (header-only)
- Vulkan SDK **1.4.x** (glslc included)
- OpenGL **4.6** core profile (falls back to 3.3)
- GoogleTest **1.17.0**
- CMake **3.31+**

## Development Roadmap (Phases 1–11)

1. ✅ **Bootstrap** – Tooling, CI, hello-world executable
2. ✅ **Rendering Core** – Vulkan renderer, windowing, swapchain management
3. ✅ **Player Interaction** – Input system, first-person camera, fly-mode prototype
4. **Graphics Enhancements** – Deferred pipeline, ray tracing experiments, PBR materials
5. **World Generation** – Procedural terrain, biomes, caves, structure placement
6. **Gameplay Systems** – Inventory, crafting tree, survival mechanics
7. **Networking** – Deterministic simulation, rollback, dedicated server launcher
8. **Asset Pipeline** – Texture packs, shader hot-reload, audio integration
9. **Modding API** – Scripting bindings, Steam Workshop publishing, sandboxing
10. **Tooling & UX** – In-engine editors, profiling HUD, telemetry dashboards
11. **Optimization & QA** – Performance budgets, regression suites, fuzzing harnesses
12. **Release Polish** – Localization, accessibility, certification, storefront launch assets

## Contributing

We welcome pull requests, feature proposals, and discussion. Please:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-improvement`)
3. Build & test with the appropriate presets
4. Submit a PR describing your changes and test coverage

Contributor guidelines, coding standards, and CLA details will be added to `docs/` in upcoming phases.

## License

PoorCraft is released under the [MIT License](./LICENSE). Commercial projects, mods, and distributions are encouraged—please share what you build!
