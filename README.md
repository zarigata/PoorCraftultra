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

### Phase 0 - Boot & Assets
- **CP v0.1**: Window opens, FPS counter, ESC quits
- **CP v0.2**: Main menu with animated background, "Assets OK" badge

### Phase 1 - Voxel Core
- **CP v1.05**: Static single-chunk plateau (16×16×16 blocks) visible
- **CP v1.1**: 3×3 chunk grid, greedy meshing, frustum culling, stable 60 FPS
- **CP v1.2**: Ray-picking, block highlight, place/break with hotbar
- **CP v1.3**: Save/load 3×3 chunks with SHA-256 checksum validation

### Phase 2 - World Generation & Lighting
- **CP v2.0**: Seed-based heightmap terrain, biome distribution (5 biomes), biome tints, seed UI
- **CP v2.1**: Caves/ravines (3D noise carving), surface features (trees, ores), basic structures (huts)
- **CP v2.2**: Skylight + block light propagation (BFS), AO, smooth lighting, day-night cycle, bed sleep

### Phase 3 - Items, Inventory, Crafting & Smelting
- **CP v3.1**: Inventory UI (hotbar + 27 slots), drag/drop/shift-click, stack rules, tooltips, durability tracking, persistence
- **CP v3.2**: Crafting 2×2 (inventory) + 3×3 (table), recipe book with search/pins, recipe discovery
- **CP v3.3**: Furnace smelting with fuel, progress bars, XP yields

## Phase 1 Architecture

**Voxel Engine** (`com.poorcraft.ultra.voxel`):
- `ChunkManager`: Chunk lifecycle, meshing, rendering
- `Chunk`: 16×16×16 block storage (YZX order)
- `GreedyMesher`: Optimized mesh generation (0fps algorithm)
- `TextureAtlas`: 8×8 atlas (64 block types)
- `ChunkRenderer`: jME Geometry creation

**Block System** (`com.poorcraft.ultra.blocks`):
- `BlockRegistry`: 26 block types (stone, dirt, grass, wood, ores, etc.)
- `BlockDefinition`: Block properties (textures, hardness, drops)
- `BlockFace`: 6-direction enum

**Player & Interaction** (`com.poorcraft.ultra.player`):
- `FirstPersonController`: BetterCharacterControl + camera
- `BlockInteraction`: 3D DDA ray-picking, place/break
- `GameSessionAppState`: Main in-game state

**Persistence** (`com.poorcraft.ultra.save`):
- `ChunkSerializer`: Binary format with SHA-256 checksums
- `SaveManager`: World save/load coordination

## Phase 3 Architecture

**Item System** (`com.poorcraft.ultra.items`):
- `ItemRegistry`: 75+ items (tools, materials, food, block items)
- `ItemDefinition`: Item properties (ID, name, icon, stack size, durability, tool tier/type)
- `ToolType`: PICKAXE, AXE, SHOVEL, HOE, SWORD (mining effectiveness)
- `ToolTier`: WOOD, STONE, IRON, GOLD, DIAMOND (mining speed, durability)

**Inventory System** (`com.poorcraft.ultra.inventory`):
- `ItemStack`: Immutable item + count + durability + NBT
- `PlayerInventory`: 9 hotbar + 27 main slots, stack rules, merge/split operations
- `InventoryAppState`: Lemur UI with drag/drop, shift-click, tooltips
- Persistence: JSON via SaveManager to `player/playerdata.json`

**Crafting System** (`com.poorcraft.ultra.crafting`):
- `Recipe`: Shaped/shapeless recipes loaded from JSON resources
- `RecipeRegistry`: Loads from `src/main/resources/recipes/*.json`
- `CraftingGrid`: 2×2 (inventory) or 3×3 (table), evaluates recipes on slot change
- `RecipeBook`: Tracks discovered recipes, auto-discovery when ingredients obtained

**Smelting System** (`com.poorcraft.ultra.smelting`):
- `FurnaceBlockEntity`: Input/fuel/output slots, burn time, smelt time, XP accumulation
- `BlockEntityManager`: Ticks active furnaces (20 ticks/sec), serializes with chunks
- `FuelRegistry`: Coal (1600 ticks), wood (300 ticks), stick (100 ticks)
- `SmeltingRecipeRegistry`: Ore → ingot (200 ticks, 0.7 XP), raw meat → cooked (200 ticks, 0.35 XP)
- `FurnaceAppState`: Lemur UI with progress bars (flame, arrow), XP display

## Items & Crafting

**Item Types:**
- **Tools** (25 items): wooden/stone/iron/gold/diamond × pickaxe/axe/shovel/hoe/sword
  - Durability: wood (59), stone (131), iron (250), gold (32), diamond (1561)
  - Mining speed: wood (2x), stone (4x), iron (6x), gold (12x), diamond (8x)
- **Materials** (7 items): stick, coal, iron_ingot, gold_ingot, diamond, redstone, emerald
- **Food** (4 items): apple, bread, raw_meat, cooked_meat
- **Block items** (40+ items): stone, dirt, grass, wood, ores, glass, leaves, crafting_table, furnace, chest, torch, etc.

**Inventory:**
- 9 hotbar slots (always visible, number keys 1-9 to select)
- 27 main inventory slots (3×9 grid, E key to open)
- Drag/drop: Left-click = pick up stack, right-click = pick up half
- Shift-click: Quick transfer between hotbar and main inventory
- Stack rules: Tools stack to 1, materials to 64, same item + durability can merge

**Crafting:**
- 2×2 grid in inventory (always available)
- 3×3 grid at crafting table (right-click table to open)
- Shaped recipes: Pattern matters (e.g., pickaxe = planks on top, sticks below)
- Shapeless recipes: Pattern doesn't matter (e.g., dye mixing)
- Recipe book: Automatically discovers recipes when ingredients obtained
- Search/filter: Find recipes by name or result item

**Smelting:**
- Furnace: Right-click to open UI
- Input slot: Ore or raw food
- Fuel slot: Coal, wood, stick
- Output slot: Ingot or cooked food
- Progress bars: Flame (fuel burn), arrow (smelting)
- XP: Accumulated per smelt, collected when output taken
- Fuel efficiency: 1 coal smelts 8 items, 1 wood smelts 1.5 items

## License

MIT License (no Mojang/Microsoft code or assets)
