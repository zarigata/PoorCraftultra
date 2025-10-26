# Vulkan Shaders

This directory contains GLSL shaders for the Vulkan renderer.

## Compiling Shaders

To compile the shaders to SPIR-V, you need the Vulkan SDK installed.

### Windows

```batch
cd assets\shaders
compile_shaders.bat
python compile_and_embed.py > shader_bytecode.txt
```

### Linux/Mac

```bash
cd assets/shaders
glslangValidator -V -o chunk.vert.spv chunk.vert
glslangValidator -V -o chunk.frag.spv chunk.frag
python3 compile_and_embed.py > shader_bytecode.txt
```

## Manual Compilation

If you don't have the Vulkan SDK installed locally, you can use:
- Shader Playground: https://shader-playground.timjones.io/
- Khronos GLSL Validator online tools

Then copy the generated SPIR-V bytecode into `VulkanRenderer.cpp`.

## Shader Files

- `chunk.vert` - Vertex shader for chunk rendering with AO support
- `chunk.frag` - Fragment shader with texture atlas sampling and lighting
