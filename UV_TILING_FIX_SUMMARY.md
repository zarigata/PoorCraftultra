# UV Tiling Fix Implementation Summary

## Problem
UV tiling was expanding beyond atlas tile bounds when greedy meshing created merged quads larger than 1×1 blocks. The previous implementation multiplied UVs by `width` and `height`, causing sampling to bleed into adjacent textures in the atlas since `GL_CLAMP_TO_EDGE` does not repeat within sub-rectangles.

## Solution
Implemented shader-based texture repeating using `fract()` to tile textures across merged quads without crossing atlas tile boundaries, preserving greedy meshing optimization.

## Changes Made

### 1. ChunkMesher.java
**Modified vertex format from 8 to 12 floats per vertex:**
- Position (XYZ) - 3 floats
- Color (RGB) - 3 floats  
- TexCoord (UV) - 2 floats (base atlas tile position: u0, v0)
- FaceUV (XY) - 2 floats (face-local coordinates in block units: 0..width, 0..height)
- TileSpan (UV) - 2 floats (tile dimensions: u1-u0, v1-v0)

**Key changes:**
- `generateMesh()`: Updated buffer capacity from 6144 to 7680 to account for 12 floats per vertex
- `calculateUVsForQuad()`: Now returns base tile UVs without scaling (always returns u0,v0 to u1,v1)
- `addQuad()`: Computes faceUV coordinates (0..width, 0..height) and tileSpan for each vertex
- `addQuadVertices()`: Updated to write 12 floats per vertex and calculate baseIndex with new stride

### 2. ChunkMesh.java
**Updated vertex attribute layout:**
- Changed stride from `8 * Float.BYTES` to `12 * Float.BYTES`
- Changed vertex count calculation from `vertices.length / 8` to `vertices.length / 12`
- Added attribute location 3: faceUV (vec2) at offset 8 floats
- Added attribute location 4: tileSpan (vec2) at offset 10 floats
- Updated class documentation to reflect new vertex format

### 3. vertex.glsl
**Added new input attributes and outputs:**
- `layout(location = 3) in vec2 faceUV` - face-local coordinates
- `layout(location = 4) in vec2 tileSpan` - tile dimensions
- `out vec2 fragFaceUV` - pass to fragment shader
- `out vec2 fragTileSpan` - pass to fragment shader

### 4. fragment.glsl
**Implemented shader-based tiling:**
```glsl
vec2 sampleUV = fragTexCoord + fract(fragFaceUV) * fragTileSpan;
vec4 texColor = texture(textureSampler, sampleUV);
```

**How it works:**
- `fragTexCoord` contains base tile position (u0, v0)
- `fract(fragFaceUV)` repeats face coordinates within [0,1] range
- Multiply by `fragTileSpan` to scale to tile size
- Add to base position to get final UV within tile bounds
- This ensures sampling never crosses tile boundaries

### 5. Test Updates
**ChunkMeshTest.java:**
- Updated all test vertex data from 8 to 12 floats per vertex
- Added placeholder values for faceUV (0,0 to 1,1) and tileSpan (1,1)
- Updated comments to reflect new vertex format

**ChunkMesherTest.java:**
- Updated `testVertexStrideIncludesUVs()` test documentation
- Changed comment from "8 floats per vertex" to "12 floats per vertex"
- Updated vertex stride description in assertions

## Benefits
1. **No texture bleeding**: Textures never sample outside their atlas tile bounds
2. **Preserves greedy meshing**: Merged quads still reduce vertex count by 90%+
3. **Correct tiling**: Textures repeat properly across large merged faces
4. **No geometry splitting**: Solution is entirely shader-based, maintaining performance

## Testing Recommendations
1. Verify merged quads (>1×1) display repeating textures, not stretched or bleeding
2. Check that vertex count remains low (greedy meshing still working)
3. Confirm no OpenGL errors with new attribute layout
4. Test with various quad sizes (1×1, 4×1, 4×4, etc.)
5. Verify all 6 face directions render correctly

## Technical Notes
- The `fract()` function in GLSL returns the fractional part of a number, effectively implementing modulo 1.0
- This approach is standard for texture atlases in modern rendering engines
- The solution adds 4 floats per vertex (33% increase) but maintains greedy meshing benefits
- Alternative approaches (geometry splitting) would have increased vertex count by 10-100×
