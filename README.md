# RetroForge

Survival RPG with voxel building and LLM companions built in **Godot 4.3+**.

## Project Structure

- `project.godot` – Core project configuration (Forward+ renderer, input, physics, layers).
- `scripts/` – Gameplay logic, systems, managers, and future subsystems (player, voxel, UI, LLM, RPG, networking).
- `scenes/` – Scene trees (`.tscn`) for main game, player rigs, UI, prefabs, and test environments.
- `resources/` – Godot resource files (`.tres`) for data-driven definitions, materials, audio streams, and shaders.
- `assets/` – Imported art/audio/font/shader source assets managed by the art team.
- `addons/` – Editor plugins and third-party integrations (e.g., Zylann Voxel Tools).
- `llm/` – LLM companion backend integrations, prompts, and tooling (Phase B).
- `voxel/` – Voxel configuration data (block definitions, noise presets, biome parameters).
- `building/` – Building system data (piece definitions, stability parameters, snap grids).
- `rpg/` – RPG layer data (factions, NPC stats, quests, dialogue trees).
- `Specifications.md` – Full design specification and phase breakdown.

## Dependencies

- **Custom Godot 4.3 Editor with Voxel Tools 1.3.0**: This project requires a custom build of Godot 4.3 that includes Zylann's Voxel Tools module. The standard Godot 4.3 editor will **not** work.
  - See `docs/VOXEL_TOOLS_SETUP.md` for download links and installation instructions.
  - Download the custom editor from: <https://github.com/Zylann/godot_voxel/releases/tag/1.3.0>

## Setup

1. **Download and install the custom Godot 4.3 editor** with Voxel Tools integrated (see Dependencies above and `docs/VOXEL_TOOLS_SETUP.md`).
2. Open the project by selecting `project.godot` in the Godot Project Manager using the custom editor.
3. Verify Voxel Tools integration: Add a node and search for `VoxelLodTerrain`. If it appears, the setup is correct.
4. Ensure your GPU supports the Forward+ renderer (Vulkan/D3D12/Metal). For lower-end hardware, Godot will fall back to GL Compatibility using the configured mobile renderer setting.

## Development Phases

RetroForge development follows the phases defined in `Specifications.md`:

- ✅ **Phase A.1 – Project Initialization**: Directory structure, project configuration, renderer setup.
- ✅ **Phase A.2 – Core Infrastructure**: Main scene, autoload singletons, error handling, debug tooling.
- ✅ **Phase A.3 – Voxel Engine Integration**: Terrain generation, chunk systems, voxel editing.
- ✅ **Phase A.4 – Environment Setup**: Dynamic lighting, audio system, biome hooks.
- ✅ **Phase A.5 – Player Controller**: FPS movement, state machine, collision-ready CharacterBody3D controller.
- **Phase B – LLM Companions**: AI companion behaviors, dialogue, and tooling.
- **Phase C – RPG Layer**: Factions, quests, NPC interactions, progression.
- **Phase D – Networking & Polishing**: Multiplayer, optimizations, release readiness.

Additional phase documents (e.g., `PHASE_B_LLM_COMPANIONS.md`) will be added alongside future milestones.

## Autoload Singletons

RetroForge uses Godot autoloads to expose core managers globally:

- **ErrorLogger** (`scripts/ErrorLogger.gd`) – Centralized logging, crash reports, log rotation.
- **GameManager** (`scripts/GameManager.gd`) – Game state transitions, settings management, pause handling.
- **InputManager** (`scripts/InputManager.gd`) – Input buffering, remapping, device detection, movement helpers.
- **SaveManager** (`scripts/SaveManager.gd`) – Save/load orchestration, auto-save loop, metadata utilities.
- **EnvironmentManager** (`scripts/Environment.gd`) – Dynamic lighting, sky, and fog control with day/night cycles and biome overrides. Access via `EnvironmentManager.set_time_normalized()` and `EnvironmentManager.get_is_day()`.
- **AudioManager** (`scripts/AudioManager.gd`) – SFX pooling, ambient loops, and volume management. Use `AudioManager.play_sfx()` and `AudioManager.play_ambient()`.

These load in the order listed so each manager can depend on previously initialized systems.

## Environment & Audio Systems

### Dynamic Lighting
- **Day/night cycle** with configurable duration (default: 20 minutes)
- **Smooth transitions** at dawn/dusk with color interpolation
- **Biome-specific overrides** for fog color, ambient light, sky tint
- **Performance optimizations**: Light LOD (distance-based shadow fading)
- Configure via `resources/environment_config.json`

### Audio System
- **Object pooling** for efficient SFX playback (max 32 concurrent sounds)
- **3D spatial audio** for world sounds with distance attenuation
- **Ambient sound loops** per biome with day/night variations
- **Volume control** integrated with GameManager settings
- **Audio buses**: Master → SFX/Music/Ambient
- Configure biome ambience via `resources/biome_ambience.json`

### Integration
- Environment and Audio systems automatically sync with biome changes
- Time-of-day affects ambient sounds (day/night loops)
- Light LOD reduces shadow cost at distance from player
- Audio pool prevents performance issues from too many simultaneous sounds

## Player Controller

### First-Person Movement
- **WASD movement** with smooth acceleration, friction, and slope handling
- **Mouse look** with configurable sensitivity sourced from settings
- **Sprint** (hold Shift) for increased ground speed
- **Jump** with realistic gravity and terminal velocity capping
- **State machine** covering Idle, Walking, Sprinting, Jumping, Falling, and debug Flying
- **Collision** handled through `CharacterBody3D` capsule tuned for voxel terrain

### Features
- **Head bobbing** feedback while moving on the ground
- **Footstep audio hooks** ready for biome-aware SFX playback
- **Fall tracking** with damage calculations prepared for future health integration
- **Biome polling** to trigger ambient/environment transitions
- **Mouse capture** that respects the pause state and settings updates
- **Debug fly mode** (F key) for rapid traversal in development builds

### Controls
- **WASD** – Move
- **Mouse** – Look around (captured on spawn)
- **Shift** – Sprint (hold)
- **Space** – Jump
- **Escape** – Toggle mouse capture / pause
- **F** – Toggle fly mode (debug builds only)
- **E / Space** – Ascend while flying
- **Q** – Descend while flying

### Integration
- Registers with `GameManager` for global access and pause awareness
- Provides camera reference to `VoxelWorld` and `EnvironmentManager`
- Polls `VoxelWorld.check_biome_at_position()` for ambience updates
- Emits signals and exposes helpers consumed by `DebugOverlay` and future systems

## Development Tools

- Press **F3** to toggle the debug overlay (FPS, memory, state, stubs for future metrics).
- Press **F5** to run the game starting at `scenes/main.tscn`.
- Use **Esc** or **P** to pause gameplay once player controls are active.
- Debug overlay now shows:
  - Time of day (HH:MM format with Day/Night indicator)
  - Current biome (when biome detection is active)
  - Audio pool usage (active players vs. max pool size)
- Player stats in debug overlay:
  - Position (X, Y, Z)
  - Velocity (horizontal speed in m/s)
  - State (IDLE/WALKING/SPRINTING/JUMPING/FALLING/FLYING)
  - Grounded status (Yes/No)

## Integrations

- **Zylann's Godot Voxel Tools 1.3.0**: Integrated via custom Godot 4.3 editor build (see Dependencies section).
- LLM backends will be configured under `llm/` (supporting local runtimes or external APIs in Phase B).

## Version Control Notes

- `.gitignore` follows Godot 4.4 best practices: editor cache (`.godot/`) and platform export directories are ignored, while `*.uid` and `export_presets.cfg` remain tracked.
- Commit Godot-generated metadata files (`.import`, `.uid`) to maintain scene/resource integrity across collaborators.

## Team Workflow

- **Artistic & Project Management**: Maintains `assets/`, provides production-ready media, tracks milestones.
- **AI Code Builder**: Implements gameplay systems, tooling, and integrations across `scripts/`, `scenes/`, and supporting directories.
- Collaboration should reference `Specifications.md` and this README to ensure consistent organization and documentation.
