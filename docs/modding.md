# Poorcraft Ultra Modding Guide

## Overview
Poorcraft Ultra is built with modding in mind. Players will be able to extend gameplay through Java-based mods, resource packs, and Steam Workshop distribution. This guide outlines the planned structure for Phase 7 when the modding subsystem is implemented.

## Mod Structure (Planned)
```
/mods/<mod-id>/manifest.json
/mods/<mod-id>/resources/
/mods/<mod-id>/scripts/
```
- **manifest.json**: metadata including id, name, version, dependencies, entrypoint class.
- **resources/**: textures, sounds, data assets packaged with the mod.
- **scripts/** (optional): scripting assets for supported scripting languages.

## Resource Packs (Planned)
```
/resourcepacks/<pack-id>/manifest.json
/resourcepacks/<pack-id>/textures/
```
Resource packs override visual/audio assets without modifying gameplay code.

## Plugin API (Planned)
Mods will interact with the game through a sandboxed API exposed via Java Service Provider Interfaces (SPI). Key features:
1. Safe execution environment with lifecycle callbacks.
2. Access to registries (blocks, items, recipes) via high-level interfaces.
3. Event bus for reacting to gameplay events.
4. Optional scripting bridge for rapid iteration.

## Steam Workshop (Planned)
Steam integration will enable browsing, subscribing, and updating mods via Steam Workshop:
- Mods downloaded to `/workshop` directory.
- Auto-sync on game launch.
- In-game browser and search UI.

## Examples (Coming Soon)
Example mods and resource packs will be published alongside the Phase 7 implementation.

## Timeline
Full documentation and tooling will be delivered with Phase 7 (Modding). Earlier phases focus on engine, gameplay, multiplayer, and integrations.
