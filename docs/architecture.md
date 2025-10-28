# Poorcraft Ultra Architecture

## Overview
Poorcraft Ultra is organized as a Gradle multi-module project targeting Java 17. Each module encapsulates a distinct responsibility and is designed to be dependency-injection ready, testable, and platform-agnostic across Windows, Linux, and macOS.

## Module Diagram
```
app -> engine -> (voxel, player, gameplay)
engine -> shared
voxel -> shared
world -> voxel -> shared
player -> engine, voxel, shared
net -> shared, steam
steam -> shared
discord -> shared
mods -> engine, shared
ai -> gameplay, shared
ui -> engine, shared
tools -> shared
tests -> all modules
```

## Module Descriptions
- **app**: Entry point, dependency injection wiring, configuration loading, logging bootstrap.
- **engine**: jMonkeyEngine wrapper, scene management, asset loading, ECS integration stubs.
- **voxel**: Chunk storage, meshing, lighting (Phase 1).
- **world**: World generation, persistence (Phase 2).
- **player**: Input handling, camera, inventory (Phases 1 & 3).
- **gameplay**: Registries, crafting, entities (Phases 1 & 3).
- **net**: SpiderMonkey networking, Steam sockets integration (Phase 4).
- **steam**: Steamworks4j bindings, achievements, overlay (Phase 5).
- **discord**: Discord Game SDK bindings, presence, invites (Phase 6).
- **mods**: Mod loader, resource packs, plugin API (Phase 7).
- **ai**: LLM adapters, intent/action DSL, Vosk STT (Phases 9 & 10).
- **ui**: Lemur-based menus, HUD, chat, mod manager (Phases 3, 7, 10, 11).
- **tools**: Asset validation utilities, importers.
- **shared**: DTOs, constants, utilities reused across modules.
- **tests**: Smoke tests, integration tests, headless harness.

## Design Principles
1. Small, cohesive classes with clear responsibilities.
2. Interfaces and dependency injection points to allow swapping implementations.
3. Extensive logging through SLF4J/Logback with async appenders.
4. Platform guardrails for native libraries and feature toggles.
5. Testability via JUnit 5, Mockito, and AssertJ.
6. Configuration-driven behavior via YAML/JSON with safe defaults.
7. Gradle Kotlin DSL build scripts for reproducible builds.

## Technology Stack
- **Runtime**: Java 17 LTS, jMonkeyEngine 3.x, LWJGL 3, Zay-ES, SpiderMonkey.
- **Integrations**: Steamworks4j, Discord Game SDK, Vosk, Brigadier, OkHttp.
- **Serialization**: Jackson, SnakeYAML, Kryo.
- **Logging**: SLF4J with Logback.
- **Build**: Gradle 8.5 (Kotlin DSL), GitHub Actions CI.

## Future Sections (Stubs)
- Networking flow and architecture (Phase 4).
- AI interaction flow and model orchestration (Phases 9 & 10).
- Modding API design and security model (Phase 7).
- Asset pipeline and tooling (later phases).
