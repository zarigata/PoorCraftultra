# Vulkan Shader Update - Implementation Summary

## Overview
Updated the Vulkan renderer to fully support textured and lit chunk rendering using a texture atlas and smooth ambient occlusion (AO). The shaders now properly sample the texture atlas, compute directional and ambient lighting, and apply per-vertex AO values.

## Changes Made

### 1. GLSL Shaders Created
- **`assets/shaders/chunk.vert`**: Vertex shader with full attribute support
  - Inputs: `aPosition`, `aNormal`, `aTexCoord`, `aAO` (matching `ChunkVertex` layout)
  - Outputs: `vNormal`, `vTexCoord`, `vAO` to fragment shader
  - Push constants: `view`, `projection`, `model` matrices

- **`assets/shaders/chunk.frag`**: Fragment shader with lighting and texture sampling
  - Descriptor set 0, binding 0: `sampler2D` for texture atlas
  - Descriptor set 0, binding 1: Uniform buffer for lighting parameters
  - Computes ambient + directional (sun) lighting
  - Applies AO factor to final color
  - Formula: `finalColor = textureColor * (ambient + directional) * AO`

### 2. SPIR-V Bytecode
- **`assets/shaders/spirv_bytecode.h`**: Pre-compiled SPIR-V bytecode arrays
  - `vertexShaderCode[]`: Compiled vertex shader
  - `fragmentShaderCode[]`: Compiled fragment shader
  - Included in `VulkanRenderer.cpp` via `#include`

### 3. LightingParams Structure Update
Updated `Renderer::LightingParams` in `include/poorcraft/rendering/Renderer.h` to use vec4 packing for std140 alignment:

**Before:**
```cpp
struct LightingParams {
    glm::vec3 sunDirection;
    glm::vec3 sunColor;
    float sunIntensity;
    glm::vec3 ambientColor;
    float ambientIntensity;
};
```

**After:**
```cpp
struct LightingParams {
    glm::vec4 sunDirAndIntensity;  // xyz = direction, w = intensity
    glm::vec4 sunColor;             // rgb = color, w unused
    glm::vec4 ambientColorAndIntensity; // rgb = color, w = intensity
};
```

This ensures proper alignment with GLSL std140 layout rules where vec3 is padded to 16 bytes.

### 4. Renderer Updates

#### VulkanRenderer (`src/rendering/VulkanRenderer.cpp`)
- Replaced old SPIR-V shader bytecode with new shaders from `spirv_bytecode.h`
- Updated `setLightingParams()` to normalize sun direction from vec4 components
- Descriptor set layout already configured correctly:
  - Binding 0: `VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER` (fragment stage)
  - Binding 1: `VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER` (fragment stage)

#### OpenGLRenderer (`src/rendering/OpenGLRenderer.cpp`)
- Updated `setLightingParams()` to work with vec4-based structure
- Updated uniform uploads to extract components from vec4s:
  - Sun direction: `sunDirAndIntensity.xyz`
  - Sun intensity: `sunDirAndIntensity.w`
  - Ambient color: `ambientColorAndIntensity.xyz`
  - Ambient intensity: `ambientColorAndIntensity.w`

### 5. Application Code Updates

#### Main Application (`src/main.cpp`)
```cpp
Renderer::LightingParams lightingParams{};
glm::vec3 sunDir = glm::normalize(glm::vec3(0.45f, -1.0f, 0.35f));
lightingParams.sunDirAndIntensity = glm::vec4(sunDir, 1.2f);
lightingParams.sunColor = glm::vec4(1.0f, 0.96f, 0.85f, 0.0f);
lightingParams.ambientColorAndIntensity = glm::vec4(0.25f, 0.32f, 0.4f, 0.35f);
renderer->setLightingParams(lightingParams);
```

#### Tests (`tests/renderer_texture_test.cpp`)
Updated all test cases to use the new vec4-based structure.

## Shader Interface Details

### Vertex Shader Interface
```glsl
// Inputs (location matches ChunkVertex offsets)
layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aTexCoord;
layout(location = 3) in float aAO;

// Push constants
layout(push_constant) uniform PushConstants {
    mat4 view;
    mat4 projection;
    mat4 model;
} pc;

// Outputs
layout(location = 0) out vec3 vNormal;
layout(location = 1) out vec2 vTexCoord;
layout(location = 2) out float vAO;
```

### Fragment Shader Interface
```glsl
// Inputs from vertex shader
layout(location = 0) in vec3 vNormal;
layout(location = 1) in vec2 vTexCoord;
layout(location = 2) in float vAO;

// Descriptors
layout(set = 0, binding = 0) uniform sampler2D uTexture;
layout(set = 0, binding = 1) uniform LightingParams {
    vec4 sunDirAndIntensity;
    vec4 sunColor;
    vec4 ambientColorAndIntensity;
} uLight;

// Output
layout(location = 0) out vec4 outColor;
```

## Lighting Calculation

The fragment shader implements the following lighting model:

1. **Ambient Lighting**: `ambient = ambientColor * ambientIntensity`
2. **Directional Lighting**: `diffuse = max(dot(normal, -sunDirection), 0.0)`
   - `directional = sunColor * sunIntensity * diffuse`
3. **Combined**: `lighting = ambient + directional`
4. **Final Color**: `outColor = textureColor * lighting * clamp(AO, 0.0, 1.0)`

## Verification Checklist

- [x] Vertex shader declares all 4 vertex attributes (position, normal, texCoord, AO)
- [x] Fragment shader declares texture sampler at binding 0
- [x] Fragment shader declares lighting UBO at binding 1
- [x] Descriptor set layout matches shader bindings
- [x] LightingParams structure uses vec4 packing for std140 alignment
- [x] Vertex attribute layout matches `ChunkVertex` structure
- [x] Push constants include view, projection, and model matrices
- [x] AO attribute is passed through and multiplied in fragment shader
- [x] Sun direction is normalized in CPU before upload
- [x] Both OpenGL and Vulkan renderers updated consistently

## Testing

To verify the implementation:

1. Build the project: `cmake --build build --config Release`
2. Run the application: `.\build\Release\PoorCraft.exe`
3. Verify that:
   - Blocks show textured faces from the atlas
   - Lighting varies based on face orientation (directional light)
   - Ambient light provides base illumination
   - AO darkens corners and edges of blocks
   - No Vulkan validation errors occur

## Recompiling Shaders

If you need to modify the shaders:

1. Edit `assets/shaders/chunk.vert` or `chunk.frag`
2. Install Vulkan SDK (if not already installed)
3. Run: `cd assets/shaders && compile_shaders.bat`
4. Run: `python compile_and_embed.py > shader_bytecode.txt`
5. Copy the generated arrays into `assets/shaders/spirv_bytecode.h`
6. Rebuild the project

Alternatively, use online tools like Shader Playground (https://shader-playground.timjones.io/) to compile GLSL to SPIR-V.

## Notes

- The SPIR-V bytecode is embedded directly in the source to avoid runtime shader compilation
- The std140 layout ensures consistent memory layout between CPU and GPU
- Sun direction normalization happens on the CPU to avoid redundant GPU work
- The AO value is clamped in the shader to ensure valid range [0.0, 1.0]
- Both renderers (Vulkan and OpenGL) now use the same LightingParams structure
