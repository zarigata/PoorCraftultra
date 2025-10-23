# PoorCraftUltra

An open-source Minecraft clone built with modern Java and LWJGL3, featuring advanced rendering, networking, and modding capabilities.

## Current Phase: Phase 4 - First-Person Camera & Player Movement

Phase 4 implements a first-person camera system with player physics, collision detection, and input handling for interactive gameplay.

### Features Implemented

**Phase 1 - Rendering:**
- ✅ GLFW window management with OpenGL 3.3 core context
- ✅ Shader loading system with GLSL support
- ✅ Basic 3D rendering pipeline with vertex/fragment shaders
- ✅ Rotating colored cube demonstration
- ✅ Depth testing and perspective projection
- ✅ Cross-platform support (Windows, Linux, macOS)

**Phase 2 - Chunk System:**
- ✅ Section-based chunk architecture (16×16×256 blocks per chunk)
- ✅ Y-major memory layout for cache-efficient vertical traversals
- ✅ Sparse world representation with lazy chunk loading
- ✅ Coordinate system handling (world, chunk, and local coordinates)
- ✅ Negative coordinate support using floor division
- ✅ Memory-optimized storage (null sections for empty regions)
- ✅ Chunk neighbor management
- ✅ Comprehensive test suite for all chunk components

**Phase 3 - Chunk Rendering:**
- ✅ Greedy meshing algorithm (90%+ vertex reduction)
- ✅ Face culling between solid blocks
- ✅ Frustum culling for visibility optimization (50-70% chunk reduction)
- ✅ Per-chunk mesh caching for performance
- ✅ VBO/VAO/EBO management for GPU resources
- ✅ Simple per-face shading (directional lighting)
- ✅ Performance targets: 60 FPS with 16 chunk render distance
- ✅ Comprehensive test suite including performance benchmarks

**Phase 4 - Camera & Player:**
- ✅ First-person camera with pitch/yaw rotation and view matrix generation
- ✅ Input handling system with rebindable controls (WASD, Space, Shift, Ctrl, Escape)
- ✅ Player physics with gravity, jumping, sprinting, and crouching
- ✅ AABB collision detection with swept tests against voxel world
- ✅ Mouse look with configurable sensitivity and pitch clamping
- ✅ Frame-rate independent movement using delta time
- ✅ Modular architecture separating Camera, Player, and InputManager
- ✅ Comprehensive test suite for camera math, input handling, and player physics

## Requirements

- **Java 17 or higher** - Modern Java LTS version
- **OpenGL 3.3+ compatible GPU** - For core profile rendering
- **Gradle 8.5+** - Build system (included via wrapper)

## Project Structure

```
PoorCraftUltra/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/poorcraftultra/
│   │   │       ├── Main.java              # Application entry point
│   │   │       ├── core/
│   │   │       │   ├── Window.java        # GLFW window management
│   │   │       │   ├── Camera.java        # First-person camera
│   │   │       │   ├── Shader.java        # Shader compilation & management
│   │   │       │   ├── Renderer.java      # OpenGL rendering pipeline
│   │   │       │   └── Frustum.java       # Frustum culling math
│   │   │       ├── input/
│   │   │       │   ├── InputAction.java   # Input action enum
│   │   │       │   └── InputManager.java  # GLFW input handling
│   │   │       ├── player/
│   │   │       │   ├── Player.java        # Player entity with physics
│   │   │       │   └── PlayerController.java # Player-camera integration
│   │   │       └── world/
│   │   │           └── chunk/
│   │   │               ├── ChunkPos.java      # Chunk position (immutable)
│   │   │               ├── ChunkSection.java  # 16×16×16 block section
│   │   │               ├── Chunk.java         # 16×16×256 chunk column
│   │   │               ├── ChunkManager.java  # World chunk management
│   │   │               ├── ChunkMesh.java     # GPU mesh data storage
│   │   │               ├── ChunkMesher.java   # Greedy meshing algorithm
│   │   │               └── ChunkRenderer.java # Chunk rendering pipeline
│   │   └── resources/
│   │       └── shaders/
│   │           ├── vertex.glsl            # Vertex shader (GLSL 330)
│   │           └── fragment.glsl          # Fragment shader (GLSL 330)
│   └── test/
│       └── java/
│           └── com/poorcraftultra/
│               ├── core/
│               │   ├── WindowTest.java        # Window lifecycle tests
│               │   ├── ShaderTest.java        # Shader compilation tests
│               │   └── RendererTest.java      # Rendering pipeline tests
│               ├── core/
│               │   ├── WindowTest.java        # Window lifecycle tests
│               │   ├── CameraTest.java        # Camera math tests
│               │   ├── ShaderTest.java        # Shader compilation tests
│               │   ├── RendererTest.java      # Rendering pipeline tests
│               │   └── FrustumTest.java       # Frustum culling tests
│               ├── input/
│               │   └── InputManagerTest.java  # Input handling tests
│               ├── player/
│               │   ├── PlayerTest.java        # Player physics tests
│               │   └── PlayerControllerTest.java # Player-camera integration tests
│               └── world/
│                   └── chunk/
│                       ├── ChunkPosTest.java              # Coordinate conversion tests
│                       ├── ChunkSectionTest.java          # Section storage tests
│                       ├── ChunkTest.java                 # Chunk logic tests
│                       ├── ChunkManagerTest.java          # World management tests
│                       ├── ChunkMeshTest.java             # Mesh GPU resource tests
│                       ├── ChunkMesherTest.java           # Greedy meshing algorithm tests
│                       ├── ChunkRendererTest.java         # Rendering pipeline tests
│                       ├── ChunkMeshPerformanceTest.java  # Meshing performance benchmarks
│                       └── ChunkRenderPerformanceTest.java # Rendering performance benchmarks
├── build.gradle                           # Gradle build configuration
├── settings.gradle                        # Gradle settings
└── README.md                              # This file
```

## Building the Project

### Windows (PowerShell/CMD)

```powershell
# Build the project
.\gradlew build

# Run the application
.\gradlew run

# Run tests
.\gradlew test

# Run chunk system tests only
.\gradlew test --tests "com.poorcraftultra.world.chunk.*"

# Run performance tests
.\gradlew performanceTest

# Clean build artifacts
.\gradlew clean
```

### Linux/macOS

```bash
# Build the project
./gradlew build

# Run the application
./gradlew run

# Run tests
./gradlew test

# Run chunk system tests only
./gradlew test --tests "com.poorcraftultra.world.chunk.*"

# Run performance tests
./gradlew performanceTest

# Clean build artifacts
./gradlew clean
```

## Running the Application

After building, you should see a window displaying a voxel world with first-person controls:
- **50 chunks** loaded in a 5×5×2 grid
- **First-person camera** with mouse look (cursor locked)
- **Player movement** with WASD keys
- **Physics simulation** with gravity and collision
- **Performance statistics** printed every 60 frames (FPS, player position, chunks rendered/culled)

### Controls
- **WASD**: Move forward/left/backward/right
- **Space**: Jump
- **Left Shift**: Sprint
- **Left Control**: Crouch
- **Mouse**: Look around
- **Escape**: Toggle cursor lock (release/capture mouse)

## Dependencies

- **LWJGL 3.3.3** - Lightweight Java Game Library
  - `lwjgl` - Core library
  - `lwjgl-glfw` - Window and input management
  - `lwjgl-opengl` - OpenGL bindings
  - `lwjgl-stb` - Image loading utilities
- **JOML 1.10.5** - Java OpenGL Math Library
- **JUnit 5** - Testing framework

## Architecture

### Phase 1: Rendering System

#### Window Management (`Window.java`)
- Initializes GLFW and creates an OpenGL 3.3 core context
- Manages window lifecycle (creation, event polling, destruction)
- Handles buffer swapping and v-sync

#### Shader System (`Shader.java`)
- Loads GLSL shader source files from resources
- Compiles vertex and fragment shaders
- Links shader programs with error handling
- Provides uniform variable management (matrices, vectors, floats)

#### Rendering Pipeline (`Renderer.java`)
- Sets up VAO/VBO/EBO for cube geometry
- Manages vertex attributes (position, color)
- Implements model-view-projection transformation matrices
- Renders with depth testing enabled
- Handles cleanup of OpenGL resources

#### Main Loop (`Main.java`)
- Initializes GLFW, window, and renderer
- Runs the main game loop (event polling, rendering, buffer swapping)
- Ensures proper cleanup on exit

### Phase 2: Chunk System

#### Coordinate Systems
The chunk system uses three coordinate systems:
- **World coordinates**: Global position in the infinite world (can be negative)
- **Chunk coordinates**: Position of chunks in the chunk grid (each chunk is 16×16×16 blocks)
- **Local coordinates**: Position within a chunk (0-15 for X/Z, 0-255 for Y)

Conversion between coordinate systems uses `Math.floorDiv()` and `Math.floorMod()` to correctly handle negative coordinates.

#### ChunkPos (`ChunkPos.java`)
- Immutable value class representing chunk coordinates
- Serves as HashMap key for chunk storage
- Provides bidirectional conversion between world and chunk coordinates
- Uses proper equals/hashCode for collection usage

#### ChunkSection (`ChunkSection.java`)
- Represents a 16×16×16 section of blocks
- Uses Y-major memory layout: `(y << 8) | (z << 4) | x`
- Optimized for cache locality during vertical traversals (lighting, meshing)
- Lazy allocation: null for empty sections to minimize memory
- Supports optimization to free memory when all blocks become air

#### Chunk (`Chunk.java`)
- Represents a 16×16×256 column of blocks
- Divided into 16 vertical sections (each 16×16×16)
- Manages coordinate conversion between world and local space
- Lazy section allocation for memory efficiency
- Provides both local and world coordinate access methods

#### ChunkManager (`ChunkManager.java`)
- Central manager for the sparse voxel world
- Loads/unloads chunks on-demand
- Provides world-coordinate block access across chunk boundaries
- Manages chunk neighbors for meshing and lighting
- Supports infinite worlds (within integer bounds) without allocating empty space

### Phase 3: Chunk Rendering System

#### ChunkMesh (`ChunkMesh.java`)
- Stores vertex and index data for a single chunk
- Manages GPU resources (VAO, VBO, EBO)
- Interleaved vertex format: position (XYZ) + color (RGB)
- Handles upload to GPU and rendering via glDrawElements
- Automatic cleanup of OpenGL resources

#### ChunkMesher (`ChunkMesher.java`)
- Implements greedy meshing algorithm for vertex optimization
- Scans each axis and merges adjacent faces of the same block type
- Reduces vertex count by 90%+ compared to naive per-block meshing
- Face culling: skips faces between solid blocks
- Cross-chunk face culling using ChunkManager neighbor access
- Simple per-face shading (top=bright, bottom=dark, sides=medium)

#### ChunkRenderer (`ChunkRenderer.java`)
- Manages rendering pipeline for multiple chunks
- Frustum culling to skip chunks outside camera view
- Per-chunk mesh caching (meshes generated once and reused)
- Provides rendering statistics (rendered/culled chunk counts)
- Integrates with existing shader system

#### Frustum (`Frustum.java`)
- Implements frustum culling math using Gribb-Hartmann method
- Extracts 6 planes from view-projection matrix
- AABB (axis-aligned bounding box) intersection tests
- Typically reduces rendered chunks by 50-70%
- Optimized for chunk-based visibility testing

### Phase 4: Camera & Player System

#### Camera (`Camera.java`)
- Pure math class for first-person camera view matrix generation
- Position and rotation (pitch/yaw) management
- Pitch clamping (-89° to 89°) to prevent gimbal lock
- Yaw wrapping (0° to 360°) for smooth rotation
- Direction vector calculation (front, right, up)
- JOML integration for efficient matrix operations
- No dependencies on player or input systems (testable in isolation)

#### InputManager (`InputManager.java`)
- GLFW callback-based input handling
- Rebindable key mappings using InputAction enum
- Active action tracking (Set of currently pressed actions)
- Mouse look with configurable sensitivity
- Cursor lock/unlock for mouse capture
- Callback registration for pitch/yaw deltas
- Default bindings: W/A/S/D (movement), Space (jump), Shift (sprint), Ctrl (crouch), Escape (toggle cursor)

#### Player (`Player.java`)
- Player entity with physics simulation and collision detection
- Movement states: walking, sprinting, crouching
- Physics constants: gravity (32 blocks/s²), jump velocity (8 blocks/s), speeds (walk/sprint/crouch)
- AABB collision detection with swept tests
- Queries ChunkManager for block collision (0=air, non-zero=solid)
- Separate collision resolution per axis (X, Y, Z)
- Eye position calculation for camera placement
- Frame-rate independent movement using delta time

#### PlayerController (`PlayerController.java`)
- Integration layer between Player and Camera
- Synchronizes camera position to player eye position
- Synchronizes camera rotation to player rotation
- Handles mouse look input and updates player rotation
- Orchestrates player update and camera sync each frame
- Clean separation of concerns for future networking support

## Testing

The project includes comprehensive unit tests for all core components:

### Phase 1 Tests
- **WindowTest**: Validates window creation, lifecycle, and GLFW integration
- **ShaderTest**: Tests shader compilation, linking, binding, and uniform handling
- **RendererTest**: Verifies rendering initialization, resource management, and OpenGL state

### Phase 2 Tests
- **ChunkPosTest**: Validates coordinate conversion (world ↔ chunk) and negative coordinate handling
- **ChunkSectionTest**: Tests Y-major indexing, bounds checking, memory optimization, and block storage
- **ChunkTest**: Verifies multi-section management, coordinate conversions, and lazy allocation
- **ChunkManagerTest**: Tests chunk loading/unloading, neighbor management, and sparse world representation

### Phase 3 Tests
- **FrustumTest**: Validates frustum plane extraction, AABB intersection tests, and chunk visibility
- **ChunkMeshTest**: Tests mesh creation, GPU upload, rendering, and resource cleanup
- **ChunkMesherTest**: Validates greedy meshing algorithm, face culling, and cross-chunk culling
- **ChunkRendererTest**: Tests rendering pipeline, mesh caching, frustum culling integration
- **ChunkMeshPerformanceTest**: Benchmarks meshing performance (<5ms target), vertex reduction (90%+ target)
- **ChunkRenderPerformanceTest**: Benchmarks rendering performance (60 FPS target with 16 chunk distance)

### Phase 4 Tests
- **CameraTest**: Validates camera math (view matrix, rotation, direction vectors, clamping/wrapping)
- **InputManagerTest**: Tests input handling, key bindings, cursor locking, mouse callbacks
- **PlayerTest**: Validates player physics (gravity, jumping, collision, movement speeds)
- **PlayerControllerTest**: Tests player-camera integration and mouse look

Run all tests:
```powershell
.\gradlew test
```

Run chunk system tests only:
```powershell
.\gradlew test --tests "com.poorcraftultra.world.chunk.*"
```

Run camera tests:
```powershell
.\gradlew test --tests "*Camera*"
```

Run player tests:
```powershell
.\gradlew test --tests "*Player*"
```

Run performance tests:
```powershell
.\gradlew performanceTest
```

Test results are displayed in the console with detailed pass/fail information. Performance tests include timing data and benchmarks.

## Development Roadmap

### Phase 1: Basic OpenGL Rendering Setup ✅
- GLFW window management
- Shader loading system
- Basic cube rendering
- Testing framework

### Phase 2: Voxel Chunk System ✅
- Section-based chunk architecture (16×16×256)
- Y-major memory layout for cache efficiency
- Sparse world with lazy loading
- Coordinate system handling (world/chunk/local)
- Memory optimization (null sections)
- Comprehensive test coverage

### Phase 3: Chunk Meshing & Rendering ✅
- Greedy meshing algorithm (90%+ vertex reduction)
- Face culling between solid blocks
- Frustum culling (50-70% chunk reduction)
- Per-chunk mesh caching
- VBO/VAO/EBO management
- Performance benchmarks (60 FPS @ 16 chunk distance)
- Comprehensive test coverage including performance tests

### Phase 4: First-Person Camera & Player Movement ✅ (Current)
- First-person camera with pitch/yaw rotation
- Input handling with rebindable controls
- Player physics (gravity, jumping, sprinting, crouching)
- AABB collision detection with voxel world
- Mouse look with sensitivity control
- Frame-rate independent movement
- Modular architecture (Camera, InputManager, Player, PlayerController)
- Comprehensive test coverage

### Phase 5: Block System (Planned)
- Block placement and breaking
- Raycast block selection
- Block interaction events
- Inventory system

### Phase 6: Networking (Planned)
- Client-server architecture
- Multiplayer synchronization
- Entity replication

### Phase 7: Modding API (Planned)
- Plugin system
- Event-driven architecture
- Custom block/item registration

## Troubleshooting

### "Failed to initialize GLFW"
- Ensure your system has OpenGL 3.3+ support
- Update graphics drivers

### "Shader file not found"
- Verify `src/main/resources/shaders/` contains `vertex.glsl` and `fragment.glsl`
- Rebuild the project with `.\gradlew clean build`

### Tests failing
- Ensure you have a GPU with OpenGL 3.3+ support
- Some tests require a valid OpenGL context (headless systems may fail)

## Contributing

This is a learning/demonstration project. Contributions, suggestions, and feedback are welcome!

## License

This project is open-source. License information to be determined.

## Acknowledgments

- **LWJGL** - Enabling Java game development with native library bindings
- **JOML** - Providing efficient math operations for 3D graphics
- **Minecraft** - Inspiration for this project

---

**Current Version**: Phase 4 - v4.0-SNAPSHOT  
**Last Updated**: October 2025
