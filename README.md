# Poorcraft Ultra

## Build System Choice: Gradle Kotlin DSL

**Rationale:**
- **Gradle 9.x** (current stable) offers superior configuration cache, reducing build times for iterative game development
- **Kotlin DSL** provides type-safe build scripts with IDE completion, better than Maven XML or Groovy DSL
- **Java Toolchains** are first-class in Gradle, ensuring consistent JDK 17 across all environments
- **Ecosystem alignment**: jMonkeyEngine community primarily uses Gradle; better plugin/template support
- **Fallback plan**: If Gradle blocks progress, Maven 3.9.x is ready as alternative (Maven 4.0 still in RC)

## Java Version: 17 LTS

**Rationale:**
- **jMonkeyEngine 3.7.0-stable** officially supports Java 17 (SDK ships with JDK 17.0.9)
- **LTS stability**: Java 17 is current LTS (released Sept 2021, support until Sept 2029)
- **Performance**: Significant improvements over Java 11 (ZGC, pattern matching, sealed classes)
- **Library compatibility**: All dependencies (Lemur 1.16.0, LWJGL 3.3.6) confirmed compatible

## Engine Stack

- **jMonkeyEngine**: 3.7.0-stable (LWJGL3 backend)
- **UI Framework**: Lemur 1.16.0 + Lemur-Proto 1.13.0
- **Physics**: Bullet (via jme3-bullet)
- **Testing**: JUnit 5 with headless jME context

## Asset Pipeline

**Strict validation enforced:**
- Block textures: **64×64 PNG** (build fails on mismatch)
- Player/NPC skins: **256×256 PNG** (build fails on mismatch)
- Item icons: **64×64 PNG**

**Generation flow:**
1. Python scripts (`/tools/assets/`) generate procedural textures
2. Gradle tasks validate sizes and SHA-256 hashes
3. Java runtime validator checks manifest on startup

## Project Structure

Single-module layout for Phase 0 (multi-module split deferred to later phases):

```
com.poorcraft.ultra.app        - Bootstrap, DI, config, logging
com.poorcraft.ultra.engine     - jME app, scene manager, input
com.poorcraft.ultra.voxel      - (Phase 1) Chunk store, meshing
com.poorcraft.ultra.world      - (Phase 2+) Worldgen, biomes
com.poorcraft.ultra.player     - (Phase 2+) Controller, camera
com.poorcraft.ultra.items      - (Phase 3+) Items, tools
com.poorcraft.ultra.blocks     - (Phase 1+) Block registry
com.poorcraft.ultra.inventory  - (Phase 3+) Containers, hotbar
com.poorcraft.ultra.crafting   - (Phase 3+) Recipe system
com.poorcraft.ultra.smelting   - (Phase 3+) Furnace
com.poorcraft.ultra.mobs       - (Phase 5+) Mob system
com.poorcraft.ultra.weather    - (Phase 2+) Day-night, weather
com.poorcraft.ultra.audio      - (Phase 6+) Music, SFX
com.poorcraft.ultra.ui         - UI components, menus
com.poorcraft.ultra.save       - (Phase 1+) Save/load
com.poorcraft.ultra.tools      - Asset validators, debug overlays
com.poorcraft.ultra.tests      - Test utilities
```

## Building

```bash
# Generate assets
./gradlew generateAssets

# Validate assets (fails build on mismatch)
./gradlew validateAssets

# Build
./gradlew build

# Run
./gradlew run

# Run smoke tests
./gradlew smokeTest
```

## Checkpoints

- **CP v0.1**: Window opens, FPS counter, ESC quits
- **CP v0.2**: Main menu with animated background, "Assets OK" badge

## License

MIT License (no Mojang/Microsoft code or assets)
