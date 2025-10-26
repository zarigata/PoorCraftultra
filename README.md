# PoorCraft Ultra

An open-source voxel game engine built with Java, LWJGL 3, and JOML.

## Current Phase

**Phase 2 - OpenGL Rendering Pipeline + Single Cube**

This phase establishes the foundation with:
- Gradle build system with LWJGL 3 and JOML dependencies
- GLFW window creation (1280x720)
- Keyboard input handling (WASD movement, ESC to exit)
- Mouse input handling (first-person camera rotation)
- First-person camera system with JOML math
- Comprehensive unit and integration tests

## Features Implemented

### Core Engine
- **Engine**: Main game loop orchestrating window, input, and camera
- **Window**: GLFW-based window management with OpenGL context
- **InputManager**: Coordinates keyboard and mouse input systems

### Input System
- **KeyboardInput**: WASD movement, sprint (Shift), ESC to close
- **MouseInput**: First-person camera rotation with pitch clamping
- Frame-rate independent movement with delta time

### Rendering (Foundation)
- **Camera**: First-person camera with view and projection matrices
- Position, yaw/pitch rotation, movement vectors
- JOML-based matrix calculations

### Rendering Pipeline
- **ShaderProgram**: GLSL shader management with compilation, linking, and uniform handling
- **Mesh**: VAO/VBO/EBO buffer management for vertex data
- **Renderer**: Orchestrates rendering pipeline with depth testing and draw calls
- GLSL vertex and fragment shaders (version 330 core)
- Single colored cube rendering in 3D space
- Performance testing (60+ FPS verified)

### Utilities
- **Constants**: Game-wide configuration values
- Window dimensions, camera settings, movement speeds, mouse sensitivity

### Testing
- Unit tests for Camera (JOML operations)
- Integration tests for Window (GLFW initialization)
- Integration tests for InputManager + Camera
- Unit tests for ShaderProgram, Mesh, and Renderer (OpenGL context required)
- Performance tests for 60+ FPS requirement
- Visual tests for rendering correctness
- Placeholder unit tests for input classes (GLFW mocking needed)

## Build Instructions

### Prerequisites
- Java 17 or higher
- Gradle (or use included wrapper)

### Building
```bash
# Using Gradle wrapper (recommended)
./gradlew build

# Or using system Gradle
gradle build
```

### Running
```bash
# Using Gradle wrapper
./gradlew run

# Or using system Gradle
gradle run
```

### Testing
```bash
# Run all tests
./gradlew test

# Run unit tests only
./gradlew test --tests "*Test"

# Run integration tests only
./gradlew test --tests "*IntegrationTest"

# Run performance tests
./gradlew performanceTest

# Run visual tests
./gradlew visualTest
```

## Controls

- **WASD**: Move camera forward/backward/left/right
- **Left Shift**: Sprint (faster movement)
- **Mouse**: Look around (first-person camera)
- **ESC**: Close window

## Project Structure

```
PoorCraftUltra/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/poorcraftultra/
│   │   │       ├── Main.java                 # Application entry point
│   │   │       ├── core/                     # Core engine components
│   │   │       │   ├── Engine.java           # Main game loop
│   │   │       │   └── Window.java           # GLFW window management
│   │   │       ├── input/                    # Input handling
│   │   │       │   ├── InputManager.java     # Input coordination
│   │   │       │   ├── KeyboardInput.java    # Keyboard handling
│   │   │       │   └── MouseInput.java       # Mouse handling
│   │   │       ├── rendering/                # Rendering components
│   │   │       │   ├── Camera.java           # First-person camera
│   │   │       │   ├── Renderer.java         # Rendering pipeline
│   │   │       │   ├── ShaderProgram.java    # GLSL shader management
│   │   │       │   └── Mesh.java             # VAO/VBO/EBO management
│   │   │       └── util/                     # Utilities
│   │   │           └── Constants.java        # Game constants
│   │   └── resources/                        # Resources
│   │       └── shaders/                      # GLSL shader files
│   │           ├── vertex.glsl               # Vertex shader
│   │           └── fragment.glsl             # Fragment shader
│   └── test/
│       └── java/
│           └── com/poorcraftultra/
│               ├── input/                    # Input tests
│               │   ├── KeyboardInputTest.java
│               │   └── MouseInputTest.java
│               ├── rendering/                # Rendering tests
│               │   ├── CameraTest.java
│               │   ├── ShaderProgramTest.java
│               │   ├── MeshTest.java
│               │   └── RendererTest.java
│               └── integration/              # Integration tests
│                   ├── WindowIntegrationTest.java
│                   ├── InputCameraIntegrationTest.java
│                   ├── RenderingPerformanceTest.java
│                   └── VisualRenderingTest.java
├── build.gradle                              # Gradle build configuration
├── settings.gradle                           # Gradle project settings
├── gradle/                                   # Gradle wrapper
│   └── wrapper/
├── gradlew                                   # Unix Gradle wrapper script
├── gradlew.bat                               # Windows Gradle wrapper script
├── .gitignore                                # Git ignore patterns
├── README.md                                 # This file
└── LICENSE                                   # License file
```

## Dependencies

- **LWJGL 3.3.3**: OpenGL/GLFW bindings for Java
- **JOML 1.10.5**: Math library for 3D operations
- **JUnit 5.10.0**: Testing framework

## Future Phases

1. **Phase 1** ✅ - Project setup, window, input, camera
2. **Phase 2** ✅ - Rendering pipeline (shaders, meshes)
3. **Phase 3** - Chunk system and terrain generation
4. **Phase 4** - Block placement and destruction
5. **Phase 5** - World saving/loading
6. **Phase 6** - Multiplayer networking
7. **Phase 7** - Advanced rendering (lighting, textures)
8. **Phase 8** - Physics and collision detection
9. **Phase 9** - Entities and AI
10. **Phase 10** - UI and inventory system
11. **Phase 11** - Crafting and recipes
12. **Phase 12** - Sound and audio
13. **Phase 13** - Modding API
14. **Phase 14** - Performance optimizations
15. **Phase 15** - Cross-platform support
16. **Phase 16** - Mobile port
17. **Phase 17** - Web deployment
18. **Phase 18** - Final polish and release

## Contributing

This is an educational open-source project. Contributions are welcome!

### Development Setup
1. Fork the repository
2. Clone your fork
3. Create a feature branch
4. Make changes with tests
5. Run `./gradlew build` to ensure everything works
6. Submit a pull request

### Code Style
- Follow standard Java conventions
- Add Javadoc for public APIs
- Include unit tests for new features
- Keep methods small and focused

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- LWJGL community for the excellent OpenGL bindings
- JOML project for the math library
- Minecraft and other voxel games for inspiration
