# PoorCraftUltra

An open-source Minecraft clone built with modern Java and LWJGL3, featuring advanced rendering, networking, and modding capabilities.

## Current Phase: Phase 3 - Chunk Meshing & Rendering

Phase 3 implements an optimized chunk rendering system with greedy meshing, face culling, and frustum culling for high-performance voxel rendering.

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
│   │   │       │   ├── Shader.java        # Shader compilation & management
│   │   │       │   ├── Renderer.java      # OpenGL rendering pipeline
│   │   │       │   └── Frustum.java       # Frustum culling math
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
│               │   ├── ShaderTest.java        # Shader compilation tests
│               │   ├── RendererTest.java      # Rendering pipeline tests
│               │   └── FrustumTest.java       # Frustum culling tests
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

After building, you should see a window displaying a voxel world with chunks:
- **50 chunks** loaded in a 5×5×2 grid
- **Rotating camera** orbiting the world
- **Greedy meshing** optimizing vertex count
- **Frustum culling** hiding chunks outside view
- **Performance statistics** printed every 60 frames

The camera rotates around the world to demonstrate chunk rendering, frustum culling, and mesh caching.

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

Run all tests:
```powershell
.\gradlew test
```

Run chunk system tests only:
```powershell
.\gradlew test --tests "com.poorcraftultra.world.chunk.*"
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

### Phase 3: Chunk Meshing & Rendering ✅ (Current)
- Greedy meshing algorithm (90%+ vertex reduction)
- Face culling between solid blocks
- Frustum culling (50-70% chunk reduction)
- Per-chunk mesh caching
- VBO/VAO/EBO management
- Performance benchmarks (60 FPS @ 16 chunk distance)
- Comprehensive test coverage including performance tests

### Phase 4: Networking (Planned)
- Client-server architecture
- Multiplayer synchronization
- Entity replication

### Phase 5: Modding API (Planned)
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

**Current Version**: Phase 3 - v3.0-SNAPSHOT  
**Last Updated**: October 2025
