# Poorcraft Ultra

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

## Running
- `./gradlew :app:run`

## Project Structure
- **app** – Application bootstrap and entry point.
- **engine** – Core game engine runtime and integrations.
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
Phase 0.0 – Bootstrap & Skeleton (IN PROGRESS)

## License
MIT License (applies to original Poorcraft Ultra source code; third-party libraries retain their respective licenses.)

## Contributing
Contribution guidelines will be provided in `CONTRIBUTING.md` during Phase 9.2.
