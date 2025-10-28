# Poorcraft Ultra

A production-grade, Java-based voxel sandbox game with Steam, Discord, multiplayer, and AI NPCs.

## Features
- Modular Java 17 architecture with Gradle multi-module build.
- Custom voxel engine with planned world generation and chunk streaming.
- Multiplayer support via jMonkeyEngine SpiderMonkey and Steam Networking.
- Steamworks integration for achievements, overlay, and Workshop.
- Discord Game SDK integration for Rich Presence and invites.
- AI-driven NPCs powered by LLM adapters and speech recognition.
- Extensible modding pipeline with resource packs and plugin API.

## Requirements
- Java 17 LTS (Temurin, Zulu, or compatible distribution).
- Gradle 8.5+ (wrapper included).
- Supported platforms: Windows, Linux, macOS.

## Getting Started
1. Clone the repository.
2. Build the project:
   - Linux/macOS: `./gradlew build`
   - Windows: `gradlew.bat build`
3. Run the client: `./gradlew :app:run`
4. Execute tests: `./gradlew test`

## Configuration
- Default configuration lives in `config/defaults.yml`.
- Override by creating `config/user.yml` or `config/user.json`.
- Environment variables for integrations:
  - `STEAM_APP_ID`
  - `DISCORD_APP_ID`
  - `OPENAI_API_KEY`
  - `GEMINI_API_KEY`
  - `OLLAMA_HOST`
- Enable developer mode with `DEV_MODE=true`.

## Project Structure
- `app`: Entry point, configuration, logging.
- `engine`: jME wrapper, scene bootstrapping.
- `voxel`: Chunk storage, meshing.
- `world`: Worldgen and persistence.
- `player`: Input, camera, inventory.
- `gameplay`: Blocks/items, crafting, entities.
- `net`: Multiplayer networking stack.
- `steam`: Steamworks integration.
- `discord`: Discord Game SDK integration.
- `mods`: Mod loader and resource packs.
- `ai`: AI services, Brigadier commands, Vosk.
- `ui`: Lemur UI, HUD, menus.
- `tools`: Asset pipeline helpers.
- `shared`: DTOs, constants, utilities.
- `tests`: Unit/integration testbed.

## Development Workflow
- Import as a Gradle project in IntelliJ IDEA or Eclipse.
- Use `./gradlew :app:run` for rapid iteration.
- Refer to `docs/architecture.md` for design details.

## Testing
- Run `./gradlew test` locally.
- GitHub Actions runs CI on Windows and Linux for every push/PR.

## License
MIT License (see `LICENSE`). Project assets excluded unless stated.

## Phase 0 Status
✅ PHASE 0 OK – Poorcraft Ultra

## Contributing
Contributions welcome. Modding guide forthcoming in `docs/modding.md`.

## Credits
- jMonkeyEngine
- LWJGL
- Steamworks4j
- Discord Game SDK
- Vosk
- Ollama
- Brigadier
- Kryo
- Jackson, SnakeYAML
- SLF4J, Logback
