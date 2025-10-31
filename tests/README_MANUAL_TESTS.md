# Manual Test Procedures for Poorcraft Ultra

## Phase 0.1 Manual Tests (MT)

### Test 1: Application Launch
1. Open a terminal or command prompt in the project root.
2. Run `gradlew.bat :app:run` on Windows or `./gradlew :app:run` on Linux/macOS.
3. **Expected:** Application starts and a window appears within 5 seconds.
4. **Verify:** Console output shows platform, configuration, and native extraction messages without errors.

### Test 2: Window Properties
1. After the window appears, verify the following:
   - Title bar shows **"Poorcraft Ultra"**.
   - Window size is **1280 × 720** pixels.
   - Window is not fullscreen.
   - Window can be resized.
2. **Expected:** All properties match values from `app/src/main/resources/application.conf`.

### Test 3: Main Menu UI
1. Observe the screen contents.
2. **Verify:**
   - An **Exit** button is visible.
   - Button is centered horizontally and vertically.
   - Button size is roughly **200 × 50** pixels.
   - Button uses Lemur's default "glass" style (translucent, rounded edges).
3. **Expected:** UI renders without visual artifacts.

### Test 4: Exit Button Interaction
1. Hover the mouse cursor over the Exit button.
2. **Verify:** Button highlights on hover.
3. Click the button.
4. **Expected:** Window closes within 1 second.
5. **Verify:** No error dialogs appear and the Java process terminates.

### Test 5: Log File Verification
1. After closing the application, open `logs/poorcraft.log`.
2. **Verify** the log contains, in order:
   - Platform and Java information.
   - Configuration origin.
   - Native extraction summary.
   - "Engine initialized" and "Main menu initialized" messages.
   - Checkpoint banner `CP v0.1 OK – Poorcraft Ultra`.
   - "Exit button clicked, shutting down..." if the button was used.
   - "Application stopped".
3. **Expected:** No WARN or ERROR entries, and no stack traces.

### Test 6: Window Close Button
1. Launch the application again.
2. Close the window using the operating system's close control (X button).
3. **Expected:** Application shuts down cleanly with the same log sequence (minus the Exit button message).

### Test 7: Multiple Launch Cycles
1. Launch and exit the application three times in succession.
2. **Verify:**
   - Each launch succeeds without errors.
   - No resource or file lock issues occur.
   - Log file appends new entries instead of overwriting previous ones.

## Pass Criteria
- All tests complete without errors or crashes.
- Window opens at the correct resolution with the correct title.
- Exit button is centered and functional.
- Log file includes the checkpoint banner `CP v0.1 OK – Poorcraft Ultra`.
- No warnings, errors, or stack traces are present in logs.

## Failure Actions
- Record details in `logs/verification_phase01.log`.
- Do **not** proceed to the next phase until all issues are resolved.
- Re-run the full manual test suite after applying fixes.

## Notes
- These tests require an active graphical display.
- Recommended target environments: Windows 10/11, Ubuntu 22.04, macOS 12+.
- VSync may slightly delay the initial frame; this is expected behavior.

## Phase 0.2 Manual Tests (MT)

### Test 8: Camera Movement (WASD)
1. Run the application using `gradlew.bat :app:run` on Windows or `./gradlew :app:run` on Linux/macOS.
2. Once the window is visible, confirm the main menu is hidden (gameplay mode by default).
3. Hold **W**.
4. **Verify:** Camera moves forward smoothly at constant speed (~5 units/sec).
5. Release **W**, hold **S**.
6. **Verify:** Camera moves backward smoothly.
7. Hold **A**.
8. **Verify:** Camera strafes left (perpendicular to view direction).
9. Hold **D**.
10. **Verify:** Camera strafes right.
11. Hold **W + D** simultaneously.
12. **Verify:** Camera travels diagonally forward-right without exceeding single-axis speed.
13. **Expected:** Movement remains smooth and frame-rate independent.

### Test 9: Camera Fly Mode (Space/Shift)
1. Hold **Space**.
2. **Verify:** Camera moves straight up on the Y-axis regardless of orientation.
3. Release **Space**, hold **Left Shift**.
4. **Verify:** Camera descends straight down.
5. Hold **W + Space**.
6. **Verify:** Camera moves diagonally forward and upward (free-fly behavior).
7. **Expected:** No gravity or ground constraints, consistent speed.

### Test 10: Mouse Look
1. Move the mouse left.
2. **Verify:** Camera rotates left (yaw decreases).
3. Move mouse right.
4. **Verify:** Camera rotates right (yaw increases).
5. Move mouse up.
6. **Verify:** Camera looks up (pitch decreases) but clamps before flipping.
7. Move mouse down.
8. **Verify:** Camera looks down (pitch increases) with clamping near vertical limits.
9. **Expected:** Smooth, responsive look with invisible cursor during gameplay.

### Test 11: FPS Counter
1. Observe the top-left corner of the screen.
2. **Verify:** Label reads `FPS: [value]` in white text.
3. Wait roughly 0.5 seconds.
4. **Verify:** FPS value updates periodically (no flicker).
5. Move camera rapidly.
6. **Verify:** Updates continue every ~0.5 seconds.
7. **Expected:** FPS remains ≥ 60 in empty scene.

### Test 12: Position Display
1. Observe the top-right corner of the screen.
2. **Verify:** Label shows formatted coordinates `X: %.1f Y: %.1f Z: %.1f`.
3. Hold **W** for 2 seconds.
4. **Verify:** X/Z values change smoothly; Y remains constant.
5. Hold **Space** for 2 seconds.
6. **Verify:** Y value increases while formatting stays at one decimal place.
7. **Expected:** Label stays right-aligned with ~10px padding.

### Test 13: ESC Menu Toggle
1. Press **ESC**.
2. **Verify:** Main menu appears centered, cursor becomes visible, camera stops responding.
3. Move mouse and press WASD.
4. **Verify:** No camera movement while menu is active.
5. Press **ESC** again.
6. **Verify:** Menu hides, cursor disappears, camera input resumes.
7. **Expected:** Logs include `Menu opened (ESC)` / `Menu closed (ESC)` entries.

### Test 14: Skybox and Lighting
1. Observe background color.
2. **Verify:** Viewport shows solid sky blue (#87CEEB) without artifacts.
3. **Note:** Directional light is active but no geometry yet; confirm no dark/black screen.

### Test 15: Window Resize (HUD Repositioning)
1. Resize window by dragging a corner and by maximizing/restoring.
2. **Verify:** FPS label remains top-left with padding.
3. **Verify:** Position label remains top-right with padding.
4. **Expected:** Labels reposition correctly for every window size.

### Test 16: Log File Verification (Phase 0.2)
1. After closing the application, open `logs/poorcraft.log`.
2. **Verify** Phase 0.2 entries appear in addition to Phase 0.1 messages:
   - `Default flyCam disabled`
   - `3D scene initialized (skybox + directional light)`
   - `Camera controller attached`
   - `HUD attached`
   - `Input mappings registered`
   - `Camera controller initialized`
   - `HUD initialized`
   - `Main menu initialized (disabled by default)`
   - `CP v0.2 OK – Poorcraft Ultra`
3. **Verify:** `Menu opened (ESC)` / `Menu closed (ESC)` appear when toggling menu.
4. **Expected:** No WARN/ERROR entries or stack traces.

## Phase 0.2 Pass Criteria
- All Tests 8–16 succeed without errors.
- Camera movement and mouse look feel smooth and responsive.
- HUD elements display with correct timing, formatting, and positioning.
- Menu toggle pauses camera input and restores it reliably.
- Log file includes the updated checkpoint banner and new initialization messages.

## Phase 0.2 Failure Actions
- Log details to `logs/verification_phase02.log` for any failure.
- Do **not** proceed to Phase 0.3 until all issues are resolved.
- Re-run Phase 0.1 manual tests to ensure no regressions.

## Phase 0.3 Manual Tests (MT)

### Prerequisite: Generate Assets
Before executing Phase 0.3 tests, confirm procedural assets are generated.

1. Open a terminal in the project root.
2. Run `gradlew.bat :tools:assets:generate` on Windows or `./gradlew :tools:assets:generate` on Linux/macOS.
3. **Expected output:**
   - "Installing Python dependencies (Pillow, jsonschema)..."
   - "Generated 13 block textures, 6 skins" (values may vary slightly)
   - "Asset validation tests passed"
4. **Verify files exist:**
   - `assets/textures/blocks_atlas.png`
   - `assets/textures/blocks_atlas.json`
   - `assets/textures/manifest.json`
   - `assets/skins/player.png`
   - `assets/skins/npc_red.png`, `npc_blue.png`, `npc_green.png`, `npc_yellow.png`, `npc_purple.png`
5. **Failure handling:** If the task fails, ensure Python 3.7+ is installed and rerun after addressing errors.

### Test 17: Asset Generation (Python Script)
1. Run `gradlew.bat :tools:assets:generate` (Windows) or `./gradlew :tools:assets:generate` (Linux/macOS).
2. **Observe console:** Should match prerequisite expectations.
3. **Verify outputs:** All files listed above must exist.
4. **Expected:** Task completes without errors.

### Test 18: Atlas Texture Inspection
1. Open `assets/textures/blocks_atlas.png` in an image viewer.
2. **Verify:**
   - Dimensions are 512×512 pixels.
   - Texture shows 8×8 grid of unique 64×64 tiles.
   - No blank or corrupted tiles.

### Test 19: Atlas JSON Validation
1. Open `assets/textures/blocks_atlas.json` in a text editor.
2. **Verify:**
   - JSON is valid and formatted.
   - Contains mappings for stone, dirt, grass, planks, log, leaves, sand, gravel, glass, water, etc.
   - Grass includes multiple faces (top/side/bottom) with distinct indices.
   - All indices are unique and between 0 and 63.

### Test 20: Manifest JSON Validation
1. Open `assets/textures/manifest.json`.
2. **Verify:**
   - Fields: `version`, `blocks`, `skins`, `atlas_size`, `block_size`, `skin_size`.
   - Values: version `0.3`, atlas_size `512`, block_size `64`, skin_size `256`.
   - `blocks` contains at least 11 entries.
   - `skins` contains at least 6 filenames.

### Test 21: Player Skin Inspection
1. Open `assets/skins/player.png` in an image viewer.
2. **Verify:**
   - Dimensions: 256×256 pixels.
   - Texture matches Minecraft skin layout (head, torso, limbs).
   - Face and clothing details are visible.

### Test 22: NPC Skin Variants
1. Open each NPC skin file (`npc_red.png`, `npc_blue.png`, `npc_green.png`, `npc_yellow.png`, `npc_purple.png`).
2. **Verify:**
   - Dimensions: 256×256 pixels.
   - Clothing colors match filename tint.
   - Base skin tone remains consistent.

### Test 23: Gallery UI (Texture Cycling)
1. Ensure `debug.showGallery = true` in `app/src/main/resources/application.conf`.
2. Run `gradlew.bat :app:run` (Windows) or `./gradlew :app:run` (Linux/macOS).
3. **Verify on-screen:**
   - Centered quad displays block textures.
   - Lemur label shows "Block: <name> (Index: <n>)".
   - Every 2 seconds, texture cycles to next block in the atlas.
4. Observe cycling for at least 30 seconds to ensure all textures appear.

### Test 24: Gallery Texture Quality
1. While gallery cycles, inspect each texture.
2. **Verify:**
   - Stone/dirt/gravel show noise variation.
   - Grass side displays green top and brown bottom with gradient.
   - Logs, planks, leaves, glass, water exhibit appropriate patterns and transparency.
   - No missing-texture magenta tiles.

### Test 25: Log File Verification (Phase 0.3)
1. After closing the application, open `logs/poorcraft.log`.
2. **Verify log entries:**
   - "Gallery attached (debug mode)" when enabled.
   - "Gallery initialized with <n> textures".
   - Cycling messages for each texture.
   - Checkpoint banner `CP v0.3 OK – Poorcraft Ultra`.
   - No WARN/ERROR entries related to assets or gallery.

### Test 26: Gallery Disabled (Production Mode)
1. Set `debug.showGallery = false` in `application.conf`.
2. Run the application.
3. **Verify:**
   - Gallery quad and label do not appear.
   - Log contains "Gallery disabled (debug.showGallery = false)".

## Phase 0.3 Pass Criteria
- Asset generation task completes without errors.
- Required atlas and skin files exist with correct dimensions.
- Atlas/manifest JSON contents are valid and logically correct.
- Gallery cycles through textures accurately when enabled.
- Gallery can be toggled off via configuration.
- Log contains the updated checkpoint banner.

## Phase 0.3 Failure Actions
- Record issues in `logs/verification_phase03.log`.
- Do **not** proceed to Phase 0.4 until failures are resolved.
- Re-run Phase 0.0–0.2 tests to confirm no regressions before continuing.

## Phase 0.4 Manual Tests (MT)

### Prerequisite: Generate Assets (if not already done)
- Ensure assets from Phase 0.3 are generated: `./gradlew :tools:assets:generate`
- Verify `assets/textures/blocks_atlas.png` and `blocks_atlas.json` exist

### Test 27: Textured Cube Display
1. Run application: `./gradlew :app:run`
2. **Verify window opens with:**
   - Rotating cube visible in center of view (5 meters in front of camera)
   - Cube displays stone texture (gray with noise) on all 6 faces
   - No pink/magenta "missing texture" color
   - Cube is 1×1×1 meter in size (appears as reasonable size on screen)
3. **Expected:** Cube renders without visual glitches, texture is crisp (not blurry)

### Test 28: Cube Rotation Animation
1. With application running, observe the cube for 60 seconds
2. **Verify:**
   - Cube rotates smoothly around vertical (Y) axis
   - Rotation speed is constant (1 RPM = one full rotation per minute)
   - No stuttering, jitter, or pauses in rotation
   - Rotation continues indefinitely (no stopping after one rotation)
3. **Time the rotation:** Use a stopwatch to measure one full rotation
4. **Expected:** One full rotation takes approximately 60 seconds (±2 seconds tolerance)

### Test 29: Version Label Display
1. Observe the bottom-center of the screen
2. **Verify:**
   - Label reads "Poorcraft Ultra v0.4" in white text
   - Label is centered horizontally
   - Label is positioned approximately 40 pixels from bottom of window
   - Label font size is 20px (readable but not obtrusive)
   - Label has slight transparency (alpha 0.9)
3. **Expected:** Label is clearly visible against skybox background

### Test 30: Atlas Loading Verification
1. After launching application, check console output
2. **Verify console contains:**
   - "Texture atlas loaded successfully (N blocks, M total faces)"
   - "Atlas dimensions validated: 512×512"
   - "Atlas indices validated: N unique indices in range [0, 63]"
   - "Textured cube initialized (stone texture, 1 RPM rotation)"
   - "Textured cube attached"
3. **Expected:** No errors or warnings related to atlas loading

### Test 31: Material and Texture Filtering
1. Move camera close to the cube (press W to move forward)
2. **Verify texture quality:**
   - At close range, texture shows crisp pixels (mag filter = Nearest)
   - At medium range, texture is smooth (min filter = Trilinear with mipmaps)
   - No blurry or pixelated artifacts at any distance
3. Move camera far from cube (press S to move backward)
4. **Verify:** Texture remains smooth at distance (mipmaps working)
5. **Expected:** Texture quality is consistent at all viewing distances

### Test 32: Camera Controls with Cube
1. With cube visible, test all camera controls:
   - WASD movement: verify camera moves around cube
   - Mouse look: verify camera rotates to view cube from different angles
   - Space/Shift: verify camera moves up/down relative to cube
2. **Verify:**
   - Cube remains visible and rotating while camera moves
   - No clipping or disappearing of cube geometry
   - Cube rotation is independent of camera movement
3. **Expected:** Camera controls work exactly as in Phase 0.2 (no regressions)

### Test 33: HUD Elements with Cube
1. Observe HUD elements (FPS counter, position display) while cube is visible
2. **Verify:**
   - FPS counter (top-left) still displays and updates every 0.5s
   - Position display (top-right) still updates in real-time
   - Version label (bottom-center) is visible and does not overlap HUD
   - All UI elements are readable (no occlusion by cube)
3. **Expected:** HUD elements function exactly as in Phase 0.2

### Test 34: Menu Toggle with Cube
1. Press ESC to open main menu
2. **Verify:**
   - Main menu appears centered
   - Cube continues rotating in background (visible behind menu)
   - Camera input is paused (WASD/mouse have no effect)
3. Press ESC again to close menu
4. **Verify:**
   - Menu closes
   - Cube still rotating
   - Camera input resumes
5. **Expected:** Menu toggle works exactly as in Phase 0.2 (no regressions)

### Test 35: FPS Performance Check
1. With cube visible, observe FPS counter (top-left)
2. **Verify:** FPS is >= 200 (Phase 0.4 requirement)
3. Move camera around cube rapidly (WASD + mouse)
4. **Verify:** FPS remains >= 200 (or drops minimally, e.g., to 180+)
5. **Expected:** Empty scene with one cube should achieve very high FPS (200+)
6. **Note:** If FPS < 200, check:
   - VSync is enabled (limits FPS to monitor refresh rate, e.g., 60 or 144)
   - Set `render.maxFps = 0` in `application.conf` to disable FPS cap
   - Disable VSync: `window.vsync = false` in `application.conf`

### Test 36: Window Resize with Cube
1. Resize window by dragging corner
2. **Verify:**
   - Cube remains visible and centered in view
   - Version label remains centered at bottom
   - HUD elements reposition correctly (FPS top-left, position top-right)
3. Maximize window
4. **Verify:** All elements scale/reposition correctly
5. Restore window to original size
6. **Verify:** All elements return to correct positions
7. **Expected:** No visual glitches or misaligned elements during resize

### Test 37: Log File Verification (Phase 0.4)
1. After running the application, open `logs/poorcraft.log`
2. **Verify log contains (in addition to Phase 0.0-0.3 entries):**
   - "Texture atlas loaded successfully (N blocks, M total faces)"
   - "Atlas validation passed: 512×512, N blocks, M indices"
   - "Atlas dimensions validated: 512×512"
   - "Tile size validated: 64×64"
   - "Atlas indices validated: N unique indices in range [0, 63]"
   - "Block mappings validated: N blocks"
   - "Textured cube initialized (stone texture, 1 RPM rotation)"
   - "Textured cube attached"
   - "CP v0.4 OK – Poorcraft Ultra" (updated checkpoint banner)
3. **Verify:** No "Gallery attached" or "Gallery initialized" messages (gallery disabled)
4. **Expected:** No ERROR or WARN level messages related to atlas or cube
5. **Expected:** No exceptions or stack traces

### Test 38: Gallery Disabled Verification
1. Verify `debug.showGallery = false` in `application.conf`
2. Run application
3. **Verify:**
   - Gallery quad and cycling textures do NOT appear
   - Only the rotating cube and version label are visible (plus HUD)
   - Log contains "Gallery disabled (debug.showGallery = false)" (if EngineCore still checks flag)
4. **Expected:** Gallery is completely disabled; cube is the only voxel feature visible

### Test 39: Multiple Launch Cycles (Regression Check)
1. Launch and exit the application 3 times in succession
2. **Verify:**
   - Each launch succeeds without errors
   - Cube displays and rotates correctly every time
   - Atlas loads successfully every time (no caching issues)
   - Log file appends entries (does not overwrite)
3. **Expected:** Consistent behavior across all launches

### Phase 0.4 Pass Criteria
- All 39 tests pass without errors
- Textured cube displays with stone texture on all faces
- Cube rotates smoothly at 1 RPM (60 seconds per rotation)
- Version label "Poorcraft Ultra v0.4" is visible at bottom-center
- Atlas loads successfully with validation passing
- Material uses correct texture filters (Nearest mag, Trilinear min)
- FPS >= 200 on empty scene with one cube (VSync disabled)
- Camera controls, HUD, and menu toggle work exactly as in Phase 0.2 (no regressions)
- Gallery is disabled (not visible)
- Logs contain checkpoint banner "CP v0.4 OK – Poorcraft Ultra"
- No exceptions or errors in logs

### Phase 0.4 Failure Actions
- If any test fails, log details to `logs/verification_phase04.log`
- Do **NOT** proceed to Phase 0.5
- Fix issues, re-run all Phase 0.4 tests
- Re-run Phase 0.0-0.3 tests to ensure no regressions
- Only proceed when all tests pass

### Notes
- Phase 0.4 builds on Phases 0.0-0.3—all previous tests must still pass
- Asset generation (Phase 0.3) is a prerequisite; run `:tools:generateAssets` if assets missing
- FPS target of 200+ requires VSync disabled; adjust `application.conf` if needed
- Cube uses jME's Box mesh with default UVs; texture atlas provides correct tile
- Rotation uses quaternions for smooth, gimbal-lock-free animation
- Version label is static (no updates), unlike FPS counter which updates every 0.5s

## Phase 0.5 Manual Tests (MT)

### Prerequisite: Generate Assets (if not already done)
- Ensure assets from Phase 0.3 are generated: `./gradlew :tools:assets:generate`
- Verify `assets/textures/blocks_atlas.png` and `blocks_atlas.json` exist

### Test 40: Flat Patch Display
1. Run application: `./gradlew :app:run`
2. **Verify window opens with:**
   - Camera positioned above a large flat terrain patch (32×32 blocks)
   - Camera looking down at patch (pitch ~-30° to -45°)
   - Patch displays grass texture on top (green)
   - Patch edges display grass_side texture (green top + brown bottom)
   - No rotating cube visible (replaced by patch)
3. **Expected:** Patch renders without visual glitches, textures are crisp

### Test 41: Patch Dimensions and Position
1. Observe the patch from camera spawn position
2. **Verify:**
   - Patch is 32×32 blocks in size (32 meters × 32 meters)
   - Patch is 1 block tall (1 meter height)
   - Patch is centered below camera (camera at 16,2,16; patch center at 16,0,16)
   - Patch extends from world coordinates (0,0,0) to (32,1,32)
3. Move camera to different angles (WASD + mouse) to view patch from sides
4. **Verify:** Patch edges are visible with grass_side texture (green top + brown bottom gradient)

### Test 42: Greedy Meshing Verification
1. Check console output after application starts
2. **Verify log contains:**
   - "Flat patch initialized (32×32×1, greedy meshed, physics enabled)"
   - Triangle count or face count (should be much less than 4352 triangles without greedy meshing)
3. **Optional:** Enable wireframe mode (if implemented) to visually inspect merged quads
4. **Expected:** Greedy meshing reduces geometry complexity (fewer triangles than naive approach)

### Test 43: Texture Quality on Patch
1. Move camera close to patch surface (press W to move forward and down)
2. **Verify texture quality:**
   - Grass_top texture (green) is visible on top surface
   - Texture shows noise variation (not solid green)
   - At close range, texture shows crisp pixels (mag filter = Nearest)
   - At medium range, texture is smooth (min filter = Trilinear with mipmaps)
3. Move camera to patch edge, observe side faces
4. **Verify:**
   - Grass_side texture visible (green top + brown bottom)
   - Gradient transition between green and brown is smooth
5. **Expected:** No missing textures (pink/magenta). Note that greedy-merged quads stretch the tile across the surface in Phase 0.5, so mild UV stretching is expected and acceptable.

### Test 44: Collision Detection (Walking on Patch)
1. With application running, use WASD to move camera toward patch surface
2. Move camera down (Left Shift) until close to patch surface (Y ≈ 1.5)
3. **Verify:**
   - Camera does NOT fall through patch (collision prevents penetration)
   - Camera can move horizontally across patch surface (WASD movement works)
   - Camera can move up/down (Space/Shift) but collision stops downward movement at surface
4. Try to move camera below patch surface (hold Shift to descend)
5. **Verify:** Collision prevents camera from going below Y=1 (patch top surface)
6. **Expected:** Solid collision, no fall-through, no jitter or stuttering

### Test 45: Collision Ray Test Verification
1. After launching application, check console output
2. **Verify log contains:**
   - "Collision verification: ray hit patch at Y=[value]" where value ≈ 1.0
   - Ray test from (16, 10, 16) downward should hit patch at Y=1 (top surface)
3. If log shows "Collision verification failed", collision is not working correctly
4. **Expected:** Ray test succeeds, hit point Y-coordinate is approximately 1.0 (±0.1 tolerance)

### Test 46: Camera Spawn Position and Orientation
1. Immediately after application starts, observe camera position (top-right HUD)
2. **Verify:**
   - Position display shows approximately "X: 16.0 Y: 2.0 Z: 16.0"
   - Camera is looking down at patch (can see patch surface below)
   - Camera is centered above patch (patch extends equally in all directions from camera)
3. **Expected:** Camera spawns at correct position (16, 2, 16) with correct orientation (looking down)

### Test 47: Physics Manager Initialization
1. Check console output after application starts
2. **Verify log contains:**
   - "Physics manager initialized (Bullet physics active)"
   - "Physics manager attached"
   - "Flat patch attached"
   - No errors related to physics initialization
3. **Expected:** Physics initializes successfully before patch is created

### Test 48: Camera Controls with Patch
1. With patch visible, test all camera controls:
   - WASD movement: verify camera moves around patch
   - Mouse look: verify camera rotates to view patch from different angles
   - Space/Shift: verify camera moves up/down (fly mode still active)
2. **Verify:**
   - Patch remains visible while camera moves
   - No clipping or disappearing of patch geometry
   - Camera can move freely in 3D space (no ground constraints yet)
3. **Expected:** Camera controls work exactly as in Phase 0.2 (no regressions)

### Test 49: HUD Elements with Patch
1. Observe HUD elements (FPS counter, position display) while patch is visible
2. **Verify:**
   - FPS counter (top-left) displays and updates every 0.5s
   - Position display (top-right) updates in real-time as camera moves
   - Version label (bottom-center) shows "Poorcraft Ultra v0.5"
   - All UI elements are readable (no occlusion by patch)
3. **Expected:** HUD elements function exactly as in Phase 0.2-0.4

### Test 50: Menu Toggle with Patch
1. Press ESC to open main menu
2. **Verify:**
   - Main menu appears centered
   - Patch remains visible in background
   - Camera input is paused (WASD/mouse have no effect)
3. Press ESC again to close menu
4. **Verify:**
   - Menu closes
   - Patch still visible
   - Camera input resumes
5. **Expected:** Menu toggle works exactly as in Phase 0.2-0.4 (no regressions)

### Test 51: FPS Performance Check
1. With patch visible, observe FPS counter (top-left)
2. **Verify:** FPS is >= 60 (Phase 0.5 requirement)
3. Move camera around patch rapidly (WASD + mouse)
4. **Verify:** FPS remains >= 60 (should be much higher, e.g., 200+)
5. **Expected:** 32×32 patch should render at high FPS (>= 60 minimum)
6. **Note:** If FPS < 60, check:
   - VSync enabled (limits to monitor refresh rate)
   - Greedy meshing working (triangle count should be low)
   - No physics performance issues (Bullet should handle static mesh easily)

### Test 52: Window Resize with Patch
1. Resize window by dragging corner
2. **Verify:**
   - Patch remains visible and correctly rendered
   - Version label remains centered at bottom
   - HUD elements reposition correctly (FPS top-left, position top-right)
3. Maximize window
4. **Verify:** All elements scale/reposition correctly
5. Restore window to original size
6. **Verify:** All elements return to correct positions
7. **Expected:** No visual glitches or misaligned elements during resize

### Test 53: Log File Verification (Phase 0.5)
1. After running the application, open `logs/poorcraft.log`
2. **Verify log contains (in addition to Phase 0.0-0.4 entries):**
   - "Camera spawned at (16, 2, 16) looking down at patch"
   - "Physics manager initialized (Bullet physics active)"
   - "Physics manager attached"
   - "Texture atlas loaded successfully (N blocks, M total faces)"
   - "Flat patch initialized (32×32×1, greedy meshed, physics enabled)"
   - "Collision verification: ray hit patch at Y=[value]" (value ≈ 1.0)
   - "Flat patch attached"
   - "CP v0.5 OK – Poorcraft Ultra" (updated checkpoint banner)
3. **Verify:** No "Textured cube" messages (cube removed)
4. **Expected:** No ERROR or WARN level messages related to patch or physics
5. **Expected:** No exceptions or stack traces

### Test 54: Multiple Launch Cycles (Regression Check)
1. Launch and exit the application 3 times in succession
2. **Verify:**
   - Each launch succeeds without errors
   - Patch displays correctly every time
   - Physics initializes successfully every time
   - Collision works every time
   - Log file appends entries (does not overwrite)
3. **Expected:** Consistent behavior across all launches

### Test 55: No Fall-Through (Critical Collision Test)
1. With application running, position camera above patch (Y > 2)
2. Hold Left Shift to descend toward patch surface
3. **Verify:**
   - Camera stops at patch surface (Y ≈ 1.5-2.0, depending on collision shape)
   - Camera does NOT fall through patch to Y < 1
   - No jitter or bouncing at surface (smooth collision response)
4. Try moving horizontally while at surface (WASD)
5. **Verify:** Camera can slide along surface without falling through
6. **Expected:** Collision is solid and reliable (no penetration)

### Pass Criteria for Phase 0.5
- All 55 tests pass without errors
- Flat patch (32×32×1) displays with grass_top on top, grass_side on edges, dirt on bottom
- Greedy meshing reduces triangle count by >= 40% (from 4352 to < 2600)
- Camera spawns at (16, 2, 16) looking down at patch
- Collision works: camera cannot fall through patch
- Ray test from (16, 10, 16) downward hits patch at Y ≈ 1.0
- FPS >= 60 with patch rendered (should be much higher, e.g., 200+)
- Camera controls, HUD, and menu toggle work exactly as in Phase 0.2-0.4 (no regressions)
- Version label shows "Poorcraft Ultra v0.5"
- Logs contain checkpoint banner "CP v0.5 OK – Poorcraft Ultra"
- No exceptions or errors in logs

### Failure Actions
- If any test fails, log details to `logs/verification_phase05.log`
- Do **NOT** proceed to VERIFICATION PHASE 0
- Fix issues, re-run all Phase 0.5 tests
- Re-run Phase 0.0-0.4 tests to ensure no regressions
- Only proceed when all tests pass

### Notes
- Phase 0.5 builds on Phases 0.0-0.4—all previous tests must still pass
- Asset generation (Phase 0.3) is a prerequisite; run `:tools:generateAssets` if assets missing
- Bullet physics (jme3-jbullet) is required; build will fail if dependency not added
- Greedy meshing is simplified for flat patch (2D merging); full 3D greedy meshing in Phase 1.1
- Collision uses `MeshCollisionShape` for accuracy; future phases may use optimized shapes (box, heightmap)
- Camera spawn position (16, 2, 16) is 2 meters above patch center, providing good initial view
- Fly mode is still active (no gravity); Phase 1.2+ will add player physics with gravity

## Phase 1.0 Manual Tests (MT)

### Prerequisite: Generate Assets (if not already done)
- Ensure assets from Phase 0.3 are generated: `./gradlew :tools:generateAssets`
- Verify `assets/textures/blocks_atlas.png` and `blocks_atlas.json` exist

### Test 56: Block Registry Initialization
1. Run application: `./gradlew :app:run`
2. **Verify console output contains:**
   - `Texture atlas loaded successfully (N blocks, M total faces)`
   - `Block registry loaded (11 blocks)`
   - `CP v1.0 OK – Poorcraft Ultra`
3. **Expected:** Registry initializes immediately after atlas load without errors.

### Test 57: Block Registry Contents
1. After startup, review console or log output.
2. **Verify:**
   - `Atlas references validated for 11 blocks`
   - No warnings about missing atlas entries
   - No errors about invalid atlas indices
3. **Expected:** All 11 base blocks (air, stone, dirt, grass, planks, log, leaves, sand, gravel, glass, water) are registered with valid atlas references.

### Test 58: Version Label Update
1. Observe the bottom-center HUD label.
2. **Verify:** Label reads `Poorcraft Ultra v1.0`, centered and positioned ~40px from the bottom.
3. **Expected:** Version matches checkpoint banner and application banner.

### Test 59: Flat Patch Regression Check
1. Confirm flat patch still renders correctly.
2. **Verify:**
   - Grass top texture on surface
   - Grass side texture on edges
   - Collision prevents falling through
3. **Expected:** No regressions from Phase 0.5 visuals or physics.

### Test 60: Core Systems Regression Sweep
1. Exercise camera controls (WASD, Space, Shift, mouse look).
2. Toggle menu with ESC.
3. Observe HUD (FPS counter, position display).
4. **Expected:** All Phase 0.2–0.5 features behave as before.

### Test 61: Log File Verification (Phase 1.0)
1. Open `logs/poorcraft.log` after exit.
2. **Verify log contains:**
   - `Block registry loaded (11 blocks)`
   - `Atlas references validated for 11 blocks`
   - `CP v1.0 OK – Poorcraft Ultra`
3. **Expected:** No WARN or ERROR entries related to registry or atlas.

### Test 62: Multiple Launch Cycles (Regression Check)
1. Launch and exit the application three times.
2. **Verify:**
   - Registry loads successfully each run
   - Flat patch renders correctly
   - Logs append new entries without overwriting
3. **Expected:** Stable behavior across repeated runs.

### Pass Criteria for Phase 1.0
- All Tests 56–62 pass without issues.
- Block registry loads with 11 validated block definitions.
- Checkpoint banner and version labels display `v1.0`.
- No regressions in rendering, collision, HUD, or controls.
- Log files show registry init and validation messages without errors.

### Failure Actions
- Record failures in `logs/verification_phase10.log`.
- Do **NOT** proceed to the next phase until all issues are resolved.
- Re-run full Phase 1.0 suite and preceding phase tests after fixes.

### Notes
- Phase 1.0 focuses on data-layer infrastructure; visual output matches Phase 0.5.
- Registry depends on `TextureAtlas` being initialized; follow startup order.
- `Chunk` and `ChunkStorage` are foundational for Phase 1.1 but not yet wired into rendering.
