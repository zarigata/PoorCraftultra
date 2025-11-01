# Poorcraft Ultra

**A production-grade, Java-based voxel sandbox with Steam, Discord, multiplayer, and AI NPCs.**

## Build System Choice: Gradle (Kotlin DSL)

### Decision Rationale
After analyzing dependencies (jMonkeyEngine, LWJGL, steamworks4j, discord-game-sdk4j, Vosk, LLM), we chose **Gradle (Kotlin DSL)** as the primary build system.

**Pros:**
- **Superior native library management**: LWJGL 3.3.6 natives handled via BOM + OS-specific classifiers (`natives-windows`, `natives-linux`, `natives-macos`, `natives-macos-arm64`); Gradle's platform detection and `runtimeOnly` dependencies make this seamless
- **Excellent jlink/jpackage support**: Badass JLink plugin (org.beryx.jlink 3.x) provides unified jlink + jpackage workflow with non-modular dependency merging
- **Build performance**: Incremental builds, build cache, parallel execution critical for large game projects
- **Type-safe configuration**: Kotlin DSL provides IDE autocomplete, refactoring support, compile-time validation
- **Multi-platform flexibility**: Clean per-OS dependency configuration for Windows/Linux/macOS natives
- **Modern ecosystem**: Better suited for complex, multi-module projects with mixed dependencies

**Cons:**
- Steeper learning curve for teams unfamiliar with Gradle/Kotlin DSL
- More flexibility = more configuration complexity

### Fallback Strategy: Maven
If Gradle fails to build or run, the project can be migrated to Maven:
- Equivalent `pom.xml` structure documented below
- Use `maven-jlink-plugin` + `jpackage-maven-plugin` (Panteleyev) for packaging
- LWJGL natives managed via `<classifier>${lwjgl.natives}</classifier>` with profiles per OS
- steamworks4j/discord-game-sdk4j work identically (Maven Central + JitPack)

**Maven equivalent (stub pom.xml):**
```xml
<project>
  <groupId>com.poorcraft</groupId>
  <artifactId>poorcraft-ultra</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <properties>
    <java.version>17</java.version>
    <jme.version>3.7.0-stable</jme.version>
    <lwjgl.version>3.3.6</lwjgl.version>
    <lwjgl.natives>natives-windows</lwjgl.natives> <!-- Override via profiles -->
  </properties>
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.lwjgl</groupId>
        <artifactId>lwjgl-bom</artifactId>
        <version>${lwjgl.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
  <dependencies>
    <!-- jMonkeyEngine -->
    <dependency><groupId>org.jmonkeyengine</groupId><artifactId>jme3-core</artifactId><version>${jme.version}</version></dependency>
    <dependency><groupId>org.jmonkeyengine</groupId><artifactId>jme3-desktop</artifactId><version>${jme.version}</version></dependency>
    <dependency><groupId>org.jmonkeyengine</groupId><artifactId>jme3-lwjgl3</artifactId><version>${jme.version}</version></dependency>
    <!-- LWJGL + natives (repeat for each module) -->
    <dependency><groupId>org.lwjgl</groupId><artifactId>lwjgl</artifactId></dependency>
    <dependency><groupId>org.lwjgl</groupId><artifactId>lwjgl</artifactId><classifier>${lwjgl.natives}</classifier><scope>runtime</scope></dependency>
    <!-- ... (glfw, opengl, stb, openal) -->
  </dependencies>
</project>
```

## Quick Start

### Prerequisites
- **Java 17 LTS** (OpenJDK or Oracle JDK)
- **Git** (for cloning)
- **Python 3.9+** with pip (for asset generation in Phase 0A)

### Build & Run
```bash
# Clone repository
git clone <repo-url>
cd PoorCraftultra

# Generate assets (Phase 0A - required before first build)
./scripts/dev/gen-assets.sh    # Linux/macOS
scripts\dev\gen-assets.bat     # Windows

# Build (uses Gradle wrapper)
./scripts/dev/build.sh    # Linux/macOS
scripts\dev\build.bat     # Windows

# Run
./scripts/dev/run.sh      # Linux/macOS
scripts\dev\run.bat       # Windows
```

### Manual Gradle Commands
```bash
./gradlew generateAssets  # Generate procedural assets
./gradlew validateAssets  # Validate assets without full build
./gradlew clean build     # Build project (validates assets automatically)
./gradlew run             # Run game
./gradlew test            # Run tests
```

## Project Structure
```
PoorCraftultra/
├── src/main/java/com/poorcraft/ultra/
│   ├── app/          # Bootstrap, config, logging
│   ├── engine/       # jME application, scene, ECS
│   ├── voxel/        # Chunk data, meshing (Phase 1)
│   ├── world/        # Worldgen, save/load (Phase 2)
│   ├── player/       # Input, camera, inventory (Phase 1)
│   ├── gameplay/     # Blocks, items, crafting (Phase 2)
│   ├── net/          # Multiplayer (Phase 3)
│   ├── steam/        # Steam API (Phase 4)
│   ├── discord/      # Discord SDK (Phase 5)
│   ├── mods/         # Mod loader (Phase 6)
│   ├── ai/           # STT, TTS, LLM, intent (Phase 8-10)
│   ├── ui/           # Menus, HUD, chat (Phase 0.2+)
│   ├── tools/        # Asset validators (Phase 0A)
│   └── shared/       # DTOs, constants, utils
├── config/           # YAML configuration files (user overrides)
├── scripts/dev/      # Build/run wrappers, asset generation
├── tools/assets/     # Python asset generators (Phase 0A)
└── docs/             # Architecture, modding, testing (Phase 11)
```

## Configuration

The game loads configuration from `config/client.yaml` with the following priority:
1. **Filesystem** (`config/client.yaml` in application directory) - user overrides
2. **Classpath** (embedded `config/client.yaml` in JAR) - default configuration bundled in the JAR
3. **Hardcoded defaults** - fallback if no config file found

### Customizing Configuration

To customize settings, create a `config/client.yaml` file next to the executable (or in the working directory). This file will override the embedded defaults.

**Example custom config:**
```yaml
# config/client.yaml
displayWidth: 1920
displayHeight: 1080
fullscreen: true
vsync: true
fpsLimit: 144
logLevel: DEBUG
```

The embedded default configuration (`src/main/resources/config/client.yaml`) is bundled in the JAR, ensuring the application runs with sane defaults even when no external config file is present.

## Asset Generation (Phase 0A)

Poorcraft Ultra uses **procedurally generated assets** (no Mojang/Microsoft content). All block textures, skins, and item icons are generated via Python scripts.

### Prerequisites
- **Python 3.9+** with pip
- **Pillow, NumPy, noise** libraries (auto-installed by scripts)

### Generate Assets
```bash
# Unix/Linux/macOS
./scripts/dev/gen-assets.sh

# Windows
scripts\dev\gen-assets.bat
```

This will:
1. Create a Python virtual environment in `tools/assets/.venv` 
2. Install dependencies from `tools/assets/requirements.txt` 
3. Generate textures in `/assets/{blocks,skins,items}/` 
4. Create `assets/manifest.json` with metadata and hashes

### Validation
Assets are automatically validated during build:
```bash
./gradlew validateAssets  # Validate without full build
./gradlew build           # Validates assets before building JAR
```

Validation enforces:
- **Block textures**: 64×64 pixels
- **Skins**: 256×256 pixels
- **Item icons**: 64×64 pixels
- **Manifest integrity**: all referenced files exist with correct dimensions

If validation fails, the build will stop with error messages. Run `gen-assets.sh/bat` to regenerate.

### Manual Asset Inspection
Generated assets are in:
- `/assets/blocks/` - Block textures (wood, stone, dirt, grass, leaves, ores)
- `/assets/skins/` - Player and NPC skins
- `/assets/items/` - Item icons (tools, resources, food)
- `/assets/manifest.json` - Metadata (dimensions, hashes, generation timestamp)

## Smoke Tests
```bash
# Generate and validate assets
./scripts/dev/gen-assets.sh
./gradlew validateAssets

# Run automated tests
./gradlew test --tests "*ConfigLoaderTest"
./gradlew test --tests "*AssetValidatorTest"
./gradlew test --tests "*PoorcraftEngineTest"
```

**Manual Test (CP 0.1):**
1. Run `./scripts/dev/run.sh` (or `.bat`)
2. Verify window opens with title "Poorcraft Ultra"
3. Verify FPS counter visible in top-left
4. Move mouse; verify no crashes
5. Press ESC; verify window closes cleanly

**Manual Test (CP 0.2):**
1. Run game
2. Press F3; verify debug overlay appears (FPS, Java version, OS, heap usage)
3. Press F3 again; verify overlay hides
4. Press F9/F10/F11; verify stub messages logged to console
5. Press ESC to exit

**Manual Test (CP 0.15):**
1. Run `./scripts/dev/gen-assets.sh` 
2. Verify `/assets/blocks/`, `/assets/skins/`, `/assets/items/` directories created
3. Verify `assets/manifest.json` exists and contains asset entries
4. Run game; press F3; verify "Assets: OK" badge in debug overlay
5. Delete `assets/manifest.json`; run `./gradlew build`; verify build fails with validation error

**Manual Test (CP 1.05):**
1. Run game
2. Verify checkerboard plateau visible (alternating stone/dirt blocks)
3. Press F3; verify "Chunks: 1 loaded" in overlay
4. Verify FPS counter shows stable framerate
5. Press ESC; verify clean exit

**Manual Test (CP 1.1):**
1. Run game
2. Verify 3×3 chunk grid visible
3. Press F3; verify "Chunks: 9 loaded" and vertex/triangle counts displayed
4. Press F10; verify meshes rebuild
5. Press F11; verify cyan wireframe boxes toggle chunk bounds
6. Press ESC to exit

**Manual Test (CP 1.2):**
1. Run game
2. Verify cursor hidden; mouse look works
3. Verify WASD movement and sprint
4. Point at block; verify wireframe highlight appears and disappears appropriately
5. Press ESC to exit

**Manual Test (CP 1.3):**
1. Run game
2. Break block with LMB; verify block disappears and console logs inventory update
3. Place block with RMB adjacent to existing block; verify placement and log
4. Break/place multiple blocks; verify inventory counts adjust
5. Press F3; verify chunk stats update after modifications
6. Press ESC to exit

**Manual Test (CP 1.35):**
1. Run game; break 5 blocks and place 5 blocks across different chunks (note coordinates)
2. Press ESC to exit
3. **VERIFY:** Console logs "Saving 9 chunks..." followed by "All chunks saved successfully"
4. Check `data/worlds/default/region/` exists with 9 files (`r.{x}.{z}.dat`) each 65,568 bytes
5. Relaunch game
6. **VERIFY:** Console logs "Loaded chunk (x, z) from disk" for all 9 chunks; no "Generated new chunk" messages
7. **VERIFY:** Previously modified blocks persist with correct types; checkerboard untouched elsewhere
8. Press ESC to exit

## Checkpoints (Phase 0)
- **CP 0.1**: Window opens with title "Poorcraft Ultra", solid background, FPS counter, ESC to quit
- **CP 0.2**: F3 debug overlay (FPS, Java/OS, heap usage); hotkeys F9/F10/F11 (stubs)
- **CP 0.15**: Assets generated and validated; "Assets: OK" badge in F3 overlay

## Checkpoints (Phase 1)
- **CP 1.05**: Single checkerboard chunk renders; overlay shows "Chunks: 1 loaded"
- **CP 1.1**: 3×3 chunk grid with greedy meshing; F10 rebuilds meshes; F11 shows chunk bounds
- **CP 1.2**: FPS camera (WASD + mouse look); ray-pick with crosshair highlight
- **CP 1.3**: LMB breaks blocks, RMB places blocks; inventory counts update
- **CP 1.35**: Save/load region files; reload produces identical block IDs; checksums validated

## Checkpoints (Phase 1.5)
- **CP 1.5.0**: Main menu with Start Game, Settings, Exit buttons
- **CP 1.5.1**: In-game state with pause menu (ESC); Resume, Settings, Save & Exit
- **CP 1.5.2**: Settings menu with Graphics, Controls, Audio tabs
- **CP 1.5.3**: Configurable keybinds; mouse sensitivity and Y-axis inversion
- **CP 1.5.4**: Window resize handling; UI scales correctly

## License
MIT License. See `LICENSE` file.

## Development Notes
- **Java 17 LTS**: Chosen for long-term support; compatible with jMonkeyEngine 3.7.0-stable and LWJGL 3.3.6
- **No Mojang/Microsoft assets**: All textures/skins procedurally generated (Phase 0A)
- **Micro-phase approach**: Each checkpoint produces a runnable build with smoke tests

## Troubleshooting
- **Build fails**: Check Java version (`java -version` should show 17.x); ensure `JAVA_HOME` set correctly
- **Window doesn't open**: Check logs in console; verify LWJGL natives loaded for your OS
- **FPS counter missing**: Verify `StatsAppState` attached in `PoorcraftEngine.simpleInitApp()` 

## World Saves (Phase 1.35)

Poorcraft Ultra automatically persists your world when you exit the game (ESC key) or when the JVM shuts down unexpectedly.

```
data/
  worlds/
    default/
      region/
        r.{chunkX}.{chunkZ}.dat
```

- **Format:** 32-byte header + 65,536-byte block payload (65,568 bytes total)
- **Checksum:** CRC32 stored in header; corrupted files regenerate safely
- **Fallback:** Missing/corrupt chunks regenerate using the Phase 1 checkerboard worldgen

**Backup:** Copy the entire `data/worlds/default/` directory.

**Reset World:** Delete `data/worlds/default/` and restart the game; new chunks will be generated.

**Troubleshooting:**
- `Failed to load chunk ... will regenerate` → File corrupt; chunk replaced with fallback
- Missing `.dat` files → Normal for unexplored chunks
- Repeated checksum warnings → Check disk health or permissions

## Controls (Phase 1.5)

**Default Keybinds:**
- **Movement**: WASD (configurable in Settings → Controls)
- **Sprint**: Left Shift
- **Break Block**: Left Mouse Button
- **Place Block**: Right Mouse Button
- **Pause**: ESC (opens pause menu in-game; exits in main menu)
- **Debug Overlay**: F3

**Mouse Settings:**
- Sensitivity: Adjustable in Settings → Controls (default 1.5)
- Invert Y-Axis: Toggle in Settings → Controls (default off)

**Changing Keybinds:**
1. Open Settings (from main menu or pause menu)
2. Go to Controls tab
3. Click "Rebind" next to any action
4. Press the desired key or mouse button
5. Click "Apply" to save changes

## UI and Input Issues

**Mouse feels inverted:**
- Open Settings → Controls
- Enable "Invert Mouse Y"
- Click Apply

**Mouse too sensitive/slow:**
- Open Settings → Controls
- Adjust "Mouse Sensitivity" slider
- Click Apply

**Can't change keybinds:**
- Ensure you click "Apply" after rebinding
- Check `config/client.yaml` for `controls` section
- Delete config file to reset to defaults

**UI too small/large after resize:**
- UI should auto-scale on window resize
- If not, restart game
- Check logs for "UI scale processor" errors

**Cursor stuck hidden/visible:**
- Press ESC to toggle between game and menu states
- Cursor hidden in-game, visible in menus
- If stuck, restart game

## Next Steps
- **Phase 0A**: Generate procedural assets (blocks, skins, items) via Python scripts
- **Phase 1**: ~~Implement voxel core (chunks, meshing, place/break)~~ ✓ COMPLETE
- **Phase 1.35**: ~~Implement save/load (region files, checksums)~~ ✓ COMPLETE
- **Phase 1.5**: ~~Implement UI system, menus, input configuration~~ ✓ COMPLETE
- **Phase 2**: Add worldgen (terrain, biomes, caves)

For detailed architecture and phase breakdown, see `/docs/architecture.md` (Phase 11).
