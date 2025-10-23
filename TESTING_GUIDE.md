# UV Tiling Fix - Testing Guide

## Overview
This guide provides instructions for testing the UV tiling fix implementation that prevents texture bleeding across atlas tiles while preserving greedy meshing optimization.

## Running Tests

### 1. Run All Unit Tests
```powershell
.\gradlew test
```

This will run all unit tests including the updated ChunkMeshTest and ChunkMesherTest that verify the new 12-float vertex format.

### 2. Run Specific Test Classes
```powershell
# Test ChunkMesh vertex format and OpenGL operations
.\gradlew test --tests "com.poorcraftultra.world.chunk.ChunkMeshTest"

# Test ChunkMesher greedy meshing with new vertex format
.\gradlew test --tests "com.poorcraftultra.world.chunk.ChunkMesherTest"
```

### 3. Run Performance Tests
```powershell
.\gradlew performanceTest
```

## Visual Testing Checklist

When running the application, verify the following:

### ✓ Texture Tiling
- [ ] Merged quads (>1×1 blocks) display repeating textures
- [ ] Textures do NOT appear stretched across large faces
- [ ] No visible seams or bleeding between different block types
- [ ] Texture patterns align correctly at block boundaries

### ✓ Greedy Meshing
- [ ] Large flat surfaces are still merged into single quads (check vertex count)
- [ ] Performance remains high (FPS should be similar to before)
- [ ] Memory usage is reasonable (only ~50% increase in vertex data)

### ✓ All Face Directions
Test each face orientation:
- [ ] TOP faces (horizontal, facing up)
- [ ] BOTTOM faces (horizontal, facing down)
- [ ] NORTH faces (vertical, facing -Z)
- [ ] SOUTH faces (vertical, facing +Z)
- [ ] EAST faces (vertical, facing +X)
- [ ] WEST faces (vertical, facing -X)

### ✓ Various Quad Sizes
- [ ] 1×1 single block faces
- [ ] 4×1 horizontal strips
- [ ] 1×4 vertical strips
- [ ] 4×4 large planes
- [ ] 8×8 or larger merged faces

### ✓ Different Block Types
- [ ] Stone blocks
- [ ] Grass blocks (different textures per face)
- [ ] Dirt blocks
- [ ] Sand blocks
- [ ] Glass blocks (transparent)
- [ ] Mixed block types (should not merge)

### ✓ Edge Cases
- [ ] Chunk boundaries (faces at x=0, x=15, z=0, z=15)
- [ ] Section boundaries (y=15/16, y=31/32, etc.)
- [ ] World boundaries (y=0, y=255)
- [ ] Adjacent chunks with different block types
- [ ] Transparent/opaque transitions

## Expected Results

### Vertex Format
Each vertex should contain exactly 12 floats:
1. Position X (float)
2. Position Y (float)
3. Position Z (float)
4. Color R (float)
5. Color G (float)
6. Color B (float)
7. TexCoord U (float) - base atlas position
8. TexCoord V (float) - base atlas position
9. FaceUV U (float) - face-local coordinate
10. FaceUV V (float) - face-local coordinate
11. TileSpan U (float) - tile width in atlas
12. TileSpan V (float) - tile height in atlas

### Shader Behavior
The fragment shader should:
1. Take base atlas UV from `fragTexCoord` (u0, v0)
2. Apply `fract()` to `fragFaceUV` to get repeating coordinates [0,1]
3. Multiply by `fragTileSpan` to scale to tile size
4. Add to base UV to get final sampling position
5. Sample texture at computed UV (always within tile bounds)

### Performance Metrics
- **Vertex count**: Should remain low due to greedy meshing
- **Draw calls**: One per chunk (unchanged)
- **Memory**: ~50% increase per vertex (8→12 floats)
- **FPS**: Should be similar to before (shader cost is minimal)

## Debugging Tips

### If textures appear stretched:
- Check that `faceUV` values range from 0 to width/height
- Verify `tileSpan` contains (u1-u0, v1-v0)
- Ensure `fract()` is applied in fragment shader

### If textures bleed into adjacent tiles:
- Verify base UV is set to (u0, v0) not scaled values
- Check that `fract(faceUV) * tileSpan` never exceeds tile bounds
- Confirm atlas uses `GL_CLAMP_TO_EDGE`

### If greedy meshing is broken:
- Check that vertex count is still low for large flat surfaces
- Verify `addQuad()` is still called with width/height parameters
- Ensure faceUV calculation uses correct width/height values

### If OpenGL errors occur:
- Verify stride is set to `12 * Float.BYTES` (48 bytes)
- Check all 5 vertex attributes are enabled (locations 0-4)
- Confirm attribute offsets are correct (0, 12, 24, 32, 40 bytes)
- Ensure shader has all required input attributes

## Common Issues

### Issue: Vertex count calculation wrong
**Symptom**: Tests fail with incorrect vertex counts
**Solution**: Verify `ChunkMesh` constructor divides by 12, not 8

### Issue: OpenGL errors on upload
**Symptom**: `GL_INVALID_OPERATION` or similar errors
**Solution**: Check that all 5 attributes are properly configured with correct offsets

### Issue: Shader compilation errors
**Symptom**: Shader fails to compile or link
**Solution**: Ensure vertex shader outputs match fragment shader inputs

### Issue: Black or missing textures
**Symptom**: Blocks render but textures are black
**Solution**: Verify `tileSpan` is not zero and `faceUV` is properly interpolated

## Success Criteria

The implementation is successful if:
1. ✅ All unit tests pass
2. ✅ No OpenGL errors during rendering
3. ✅ Textures repeat correctly on merged quads
4. ✅ No texture bleeding between atlas tiles
5. ✅ Greedy meshing still reduces vertex count
6. ✅ Performance is acceptable (similar to before)
7. ✅ All block types render correctly
8. ✅ All face orientations work properly

## Rollback Plan

If critical issues are found:
1. Revert `ChunkMesher.java` to use 8-float vertices
2. Revert `ChunkMesh.java` to use stride of 8
3. Revert shaders to original versions
4. Revert test files to 8-float format
5. Document issues for future fix attempt

## Next Steps

After successful testing:
1. Monitor performance in production
2. Gather user feedback on visual quality
3. Consider optimizations (e.g., packing tileSpan into fewer floats)
4. Document any edge cases discovered
5. Update user-facing documentation
