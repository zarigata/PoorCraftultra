## Poorcraft Ultra

A production-grade, Java-based voxel sandbox game with Steam, Discord, multiplayer, and AI NPCs.

## Features
- Vast procedural worlds with rich voxel terrain.
- Modular engine architecture for rendering, physics, networking, and AI.
- Steam and Discord integrations for community features.
- Full modding support with scriptable tools pipeline.
- Cross-platform native handling and optimized asset streaming.

## Requirements
- Java 17 LTS
- Gradle 8.x

## Building
- Linux/macOS: `./gradlew clean build`
- Windows: `gradlew.bat clean build`

## Generating Assets

Poorcraft Ultra uses procedural scripts to generate textures and skins.

### Prerequisites

- Python 3.7 or higher
- pip

### Generate Assets

```bash
# Windows
gradlew.bat :tools:assets:generate

# Linux/macOS
./gradlew :tools:assets:generate
```

This task:

1. Installs Python requirements (Pillow, jsonschema)
2. Generates 64×64 block textures (stone, dirt, grass, planks, log, leaves, sand, gravel, glass, water)
3. Packs textures into `assets/textures/blocks_atlas.png` (512×512)
4. Writes `assets/textures/blocks_atlas.json` with atlas indices
5. Creates `assets/textures/manifest.json` metadata
6. Produces player and NPC skins under `assets/skins/`
7. Runs validation tests to confirm dimensions and JSON schema

### Verification

After running the task, confirm these files exist:

- `assets/textures/blocks_atlas.png`
- `assets/textures/blocks_atlas.json`
- `assets/textures/manifest.json`
- `assets/skins/player.png`
- `assets/skins/npc_red.png`, `npc_blue.png`, `npc_green.png`, `npc_yellow.png`, `npc_purple.png`

### Troubleshooting

- **Python not found** – Install Python 3.7+ and ensure `python` is on PATH
- **Missing Pillow/jsonschema** – Run `pip install -r scripts/assets/requirements.txt`
- **Generation failed** – Re-run with `--info` logging: `gradlew.bat :tools:assets:generate --info`

## Running
- `./gradlew :app:run`

## World Saves

Poorcraft Ultra saves worlds to the `saves/` directory using Minecraft-style region files (`.mca`).

### Save Location

- Default world: `saves/TestWorld/`
- Region files: `saves/TestWorld/region/r.<regionX>.<regionZ>.mca`
- Each region stores 32×32 chunks (1,024 chunks)

### Auto-Save

- Auto-save triggers every 5 minutes (configurable via `application.conf`)
- Only dirty chunks are saved, minimizing disk writes
- Console log: `Auto-save: N chunks saved in X ms`

### Save-on-Exit

- World is saved synchronously when the application shuts down
- Ensures all chunks are persisted before exit
- Console log: `Saving world on exit...` followed by `World saved successfully`

### Compression

- Chunk data is compressed with Zstandard (level 3 by default)
- Typical compression ratio: 60–70% (131 KB → ~45 KB per chunk)

### Data Integrity

- Magic number and format version guard against incompatible data
- CRC32 checksum validates chunk contents and detects corruption
- Corrupted chunks are logged and regenerated instead of crashing the game

### Troubleshooting

- **"Failed to save world on exit"**: check disk space and write permissions
- **"Chunk data corrupted"**: remove the affected region file or restore from backup
- **World not loading**: ensure `saves/TestWorld/region/` exists and inspect logs for load messages

## Project Structure
- **app** – Application bootstrap and entry point.
- **engine** – Core game engine runtime and integrations.
- **engine-api** – Lightweight interfaces and constants shared between engine, UI, and player modules.
- **voxel** – Voxel world structures and chunk systems.
- **world** – World generation, saving, and persistence.
- **player** – Player controller, inventory, and interaction.
- **gameplay** – Gameplay systems, crafting, and progression.
- **net** – Multiplayer networking stack.
- **steam** – Steam platform services integration.
- **discord** – Discord rich presence and community tools.
- **mods** – Modding API and sandboxing.
- **ai** – NPC behavior, voice, and LLM integrations.
- **ui** – User interface components and HUD.
- **tools** – Asset pipeline utilities and generators.
- **shared** – Common utilities and configuration.
- **tests** – Verification and integration test suites.

## Development Status
- Phase 0.0 – Project Boilerplate (COMPLETE)
- Phase 0.1 – Asset Pipeline (COMPLETE)
- Phase 0.2 – HUD Overlay & Camera Controls (COMPLETE)
- Phase 0.3 – Texture Atlas Generation (COMPLETE)
- Phase 1.0 – Chunk Generation & Rendering (COMPLETE)
- Phase 1.1 – Superchunk Rendering (COMPLETE)
- Phase 1.2 – Place/Break Blocks & Inventory (COMPLETE)
- Phase 1.3 – Save/Load System (IN PROGRESS)

Phase 1.3 brings the save and load system online with Minecraft-style region files, auto-save, and compression.

## License
MIT License (applies to original Poorcraft Ultra source code; third-party libraries retain their respective licenses.)

## Contributing
Contribution guidelines will be provided in `CONTRIBUTING.md` during Phase 9.2.
