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
- ✅ **Phase A.6 – Mining System**: Tools, voxel editing, undo stack, progress UI.
- ✅ **Phase A.7 – Item Physics**: Dropped items, pickup, stacking, lifetime management.
- ✅ **Phase A.8 – Resource Definitions & Inventory**: Item database, inventory backend, UI, hotbar integration.
- ✅ **Phase A.9 – UI Framework**: Centralized theming, transitions, accessibility options.
- ✅ **Phase A.10 – Recipe System & Crafting**: Station registration, queue handling, crafting UI.
- ✅ **Phase A.11 – Interaction System**: Interactable component, raycast focus, prompts, priority handling.
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
- **MiningSystem** (`scripts/MiningSystem.gd`) – Mining state, tool durability, voxel editing, and undo/redo stack. Use `MiningSystem.start_mining()` and `MiningSystem.set_active_tool()`.
- **Inventory** (`scripts/Inventory.gd`) – Slot-based inventory with hotbar, weight checks, stacking, and serialization helpers. Use `Inventory.add_item()`, `Inventory.move_item()`, and `Inventory.select_hotbar_slot()`.
- **UIManager** (`scripts/UIManager.gd`) – Unified UI theme, CanvasLayer registry, screen transitions, scaling, and accessibility helpers. Use `UIManager.apply_theme_to_control()` and `UIManager.transition_to_scene()`.
- **InteractionManager** (`scripts/InteractionManager.gd`) – Centralized interaction focus, priority sorting, and cooldown management. Use `InteractionManager.update_focus()`, `InteractionManager.try_interact()`, and `InteractionManager.get_focused_interactable()`.

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

## Dropped Item System

### Core Features
- **Physics-based items** with realistic bouncing and rolling
- **Automatic pickup** when players walk within 1.5m pickup radius
- **Ground stacking** merges items of the same type within 1.0m once settled
- **Five-minute lifetime** (300s) with 30-second fade warning before despawn
- **Visual effects** via CPUParticles3D on pickup and despawn events
- **Audio feedback** for pickup, bounce, and despawn cues through `AudioManager`
- **Network-ready hooks** with `@rpc` stubs for future multiplayer synchronization

### Item Lifecycle
1. **Spawn** – Mining drops instantiate `DroppedItem` scenes with random impulses.
2. **Bounce** – Items use physics material override for satisfying impacts.
3. **Stack** – Settled items query nearby stacks and merge matching resources.
4. **Pickup** – Player overlap triggers automatic collection via player cooldown.
5. **Despawn** – After 300s, items fade out, play audio/particles, then clean up.

### Physics Properties
- **Mass**: 0.1 kg with tuned damping for smooth movement
- **Bounce**: 0.3, **Friction**: 0.5 for balanced rolling vs. settling
- **Pickup Radius**: 1.5m Area3D sphere for forgiving collection
- **Stack Radius**: 1.0m direct-space query to consolidate piles
- **Bounce Threshold**: 0.5 m/s to throttle impact sounds

### Integration
- Mining drops already spawn `DroppedItem` instances—no extra wiring required
- Player controller emits `item_collected` signal for Inventory phase consumers
- Debug overlay tracks active dropped items with color-coded thresholds
- Items remain unsaved per specification; terrain edits persist separately

## Inventory System

### Core Features
- **30-slot backpack** with per-item stack size limits defined in `resources/data/resources.json`.
- **9-slot hotbar** mapped to number keys (1-9) and mouse wheel cycling for quick access.
- **Weight capacity** (100 units) prevents over-encumbrance; configurable via constants in `Inventory.gd`.
- **Drag-and-drop UI** for rearranging stacks, splitting, and assigning hotbar slots.
- **Search and auto-sort** tools for quickly finding resources (sort order: category → tier → name).
- **Serialization hooks** integrated with `SaveManager` for persistence and corruption tolerance.

### Resource Database
- Defined in `resources/data/resources.json` following the voxel config pattern.
- Each entry maps to a `GameResource` (see `resources/Resource.gd`) with metadata for stack size, weight, icons, and tool links.
- Icons are resolved from `assets/icons/items/` (placeholder `.gitkeep` added for asset pipeline coordination).

### Controls
- **Tab / I** – Toggle inventory UI (pauses game while open).
- **1-9** – Select hotbar slot.
- **Mouse Wheel** – Cycle hotbar selection.
- **Ctrl+Z / Ctrl+Y** – Undo/redo voxel edits via MiningSystem.

### Integration
- Player emits `item_collected` → `Inventory.add_item()` handles stacking/weight.
- `SaveManager` serializes `Inventory.serialize()` into `player_data.inventory`.
- `GameManager` settings include `inventory/auto_sort_on_pickup`, `inventory/show_weight`, and `inventory/show_tooltips`.
- Debug overlay displays slots used, weight load, and selected hotbar slot (color coded).
- UI theme is applied via `UIManager` so Inventory UI follows global styling and accessibility preferences.

### Audio
- `res://assets/audio/sfx/items/pickup.ogg` – Pickup feedback (placeholder handled gracefully)
- `res://assets/audio/sfx/items/bounce.ogg` – Ground contact thud (rate limited)
- `res://assets/audio/sfx/items/despawn.ogg` – Soft poof on timeout

### Performance
- Lifetime timer prevents unchecked accumulation
- Spatial queries limited to small radii for efficient stacking checks
- Particle effects use CPU-based emitters for wide hardware compatibility
- Audio playback leverages pooled `AudioManager` channels

## Interaction System

### Core Features
- **Interactable component** (`scripts/Interactable.gd`) attachable to any `Node3D`.
- **Raycast-driven focus** updated from `Player` via `InteractionManager.update_focus()`.
- **Priority sorting** combining distance weighting and manual priority overrides.
- **Visual prompts** using `InteractionPromptUI` (`scenes/interaction_prompt_ui.tscn`) with fade animations.
- **Cooldowns & validation**: global debounce plus per-interactable cooldowns and optional custom validation.
- **Accessibility hooks**: Screen reader announcements via `UIManager` and optional disabled prompt messaging.

### Flow
1. Player looks at an interactable – camera raycast hits parent collider.
2. `InteractionManager` resolves the interactable, applies priority, and sets focus.
3. Prompt UI updates to show the interactable's `prompt_text` and optional screen reader copy.
4. Pressing **E/F** (`interact` action) triggers `InteractionManager.try_interact()` which enforces cooldowns.
5. Interactable emits `interacted(player)` so parent nodes (e.g., stations, chests) handle gameplay logic.

### Components
- **Interactable.gd**
  - Exports: `prompt_text`, `interaction_range`, `priority`, `interaction_type`, `cooldown_duration`, accessibility settings.
  - Signals: `interacted`, `focus_gained`, `focus_lost`, `validation_failed`.
  - Helpers: `can_interact()`, `interact()`, `get_prompt_text()`, `set_focused()`.
- **InteractionManager.gd**
  - Signals: `focused_interactable_changed`, `interaction_triggered`, `interaction_failed`.
  - APIs: `register_interactable()`, `unregister_interactable()`, `get_all_interactables()`, `get_interactables_in_range()`.
  - Cooldown constants and automatic pruning of orphaned interactables.
- **InteractionPromptUI.gd**
  - CanvasLayer at layer 45 with fade-in/out tweening.
  - Listens to `InteractionManager.focused_interactable_changed` to update prompt text and screen reader announcements.

### Integration
- `Player.gd` now caches `InteractionManager` and calls `_update_interaction()` each physics frame.
- Stations (`StationWorkbench.gd`, `StationFurnace.gd`) attach Interactable components and respond to `interacted` signals instead of `Area3D` polling.
- Main scene instantiates `InteractionPromptUI`, and `project.godot` registers `InteractionManager` as an autoload.
- Debug overlay exposes focus and registration counts for quick diagnostics.

### Future Extensions
- Multi-option context menus for overlapping interactables.
- Network-safe interaction negotiation.
- LLM-driven dialog wheels leveraging `interaction_type` metadata.

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
- **Space** – Jump / ascend while flying
- **Escape** – Toggle mouse capture / pause
- **Left Mouse** – Mine (hold)
- **F** – Toggle fly mode (debug builds only)
- **E / Space** – Ascend while flying
- **Q** – Descend while flying

### Integration
- Registers with `GameManager` for global access and pause awareness
- Provides camera reference to `VoxelWorld` and `EnvironmentManager`
- Polls `VoxelWorld.check_biome_at_position()` for ambience updates
- Emits signals and exposes helpers consumed by `DebugOverlay` and future systems

## Development Tools

- Press **F3** to toggle the debug overlay (FPS, memory, state, mining stats, and more).
- Press **F5** to run the game starting at `scenes/main.tscn`.
- Press **F6** to run the standalone crafting testbed at `scenes/test_crafting.tscn` for rapid crafting UI and queue validation.
- Use **Esc** or **P** to pause gameplay once player controls are active.
- Debug overlay now shows:
  - Time of day (HH:MM format with Day/Night indicator)
  - Current biome (when biome detection is active)
  - Audio pool usage (active players vs. max pool size)
  - Mining state (IDLE/TARGETING/MINING/COOLDOWN) with progress when active
  - Active tool plus durability percentage
  - Undo stack usage (current/50)
  - Dropped item count (green < 50, yellow 50-100, red > 100)
- Interaction stats (current focus prompt text and total registered interactables)
- UI stats (scale multiplier and registered CanvasLayers) sourced from `UIManager`
- Player stats in debug overlay:
  - Position (X, Y, Z)
  - Velocity (horizontal speed in m/s)
  - State (IDLE/WALKING/SPRINTING/JUMPING/FALLING/FLYING)
  - Grounded status (Yes/No)

## UI Framework (UIManager)

### Core Features
- **Unified theme system** built from `resources/ui_theme_config.json` for low-poly, flat-shaded aesthetics.
- **CanvasLayer registry** so overlays can report, hide, or show collectively.
- **Screen transitions** (fade & slide) with async signals (`transition_started`, `transition_completed`).
- **Resolution scaling & safe areas** with adjustable UI scale (0.75x–1.5x) and safe area margins.
- **Accessibility options** including color blind palettes, high contrast mode, and text size multiplier.

### Theme System
- JSON-driven palette, font sizes, and stylebox definitions for panels, buttons, and overlays.
- Programmatic StyleBox generation keeps look consistent across Inventory, Mining Progress, Debug Overlay, and future UIs.
- Accessibility overrides swap palette entries for protanopia, deuteranopia, and tritanopia.
- High contrast mode collapses colors to pure black/white for maximum legibility.

### API Highlights
- `UIManager.apply_theme_to_control(node)` – Recursively apply the active theme to controls under any node.
- `UIManager.register_ui(canvas_layer, name)` / `unregister_ui()` – Track overlays and coordinate visibility.
- `UIManager.transition_to_scene(path, type, duration)` – Start fade or slide transitions without blocking gameplay.
- `UIManager.set_ui_scale(scale)` / `get_ui_scale()` – Adjust global UI scale persisted in GameManager settings.
- `UIManager.set_color_blind_mode(mode)` – Switch accessibility palette at runtime (0 = none).

### Integration
- Main scene ensures UIManager is ready before instantiating UI scenes.
- Inventory, Mining Progress, and Debug Overlay connect to `theme_changed` to refresh live.
- Debug overlay displays registered UI count and the current scale multiplier for quick diagnostics.
- Settings saved via GameManager (`ui/scale`, `ui/color_blind_mode`, `ui/high_contrast`, `ui/text_size`, `ui/safe_area_margin`).

## Mining System

### Core Features
- Hold-to-mine mechanics with progress feedback and cooldown management.
- Tool system with tiers, durability, mining speed/radius, and voxel-specific efficiencies.
- Sphere-based voxel editing backed by undo/redo stack snapshots (50 entries).
- Material-driven drop tables ready for future loot spawning.
- Mining sounds with automatic category fallbacks and completion cues.
- Mining progress UI overlay showing voxel, progress, and tool durability.

### Tools
- **Hand** (`resources/tools/tool_hand.tres`) – Default infinite durability tool for soft blocks.
- **Stone Pickaxe** (`resources/tools/tool_pickaxe_stone.tres`) – Tier 2 test pickaxe for stone and ores.
- Future tiers (wood, iron, steel, titanium) can extend `Tool.gd` via `.tres` resources.

### Mining Mechanics
- Mining time formula: `duration = (base_time * hardness) / (tool_speed * efficiency)` clamped to ≥0.1s.
- Tool durability decreases by 1 per completed edit; broken tools emit `tool_broke` and revert to hand.
- Mining distance capped at 10 meters; radius capped at 5 voxels for safety.
- Rate limiter prevents more than 10 edits per second.
- Movement, pause, or releasing the mine key cancels active mining cleanly.

### Integration
- Player controller drives mining via `MiningSystem.start_mining(self)` and `stop_mining()`.
- `VoxelWorld.apply_edit()` performs sphere edits using `VoxelTool` with mutex safety.
- Debug overlay displays live mining stats; UI scene auto-loads in `Main.gd`.
- Save data hooks prepared via future serialization of edit history (`VoxelWorld._serialize_edits`).

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
