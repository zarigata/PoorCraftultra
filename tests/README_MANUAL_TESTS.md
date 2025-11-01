# Manual Test Scripts - Phase 0 / Phase 1

This document describes manual tests for Phase 0 checkpoints.

## Prerequisites
- Java 17 installed and in PATH
- Gradle wrapper initialized (run `gradlew.bat` or `./gradlew` once to download)

## Checkpoint 0.1: Window Opens with FPS Counter

**Objective**: Verify basic window creation, rendering, and clean shutdown.

**Steps**:
1. Open terminal/command prompt in project root
2. Run: `scripts\dev\run.bat` (Windows) or `./scripts/dev/run.sh` (Linux/macOS)
3. Observe window opens with title "Poorcraft Ultra"
4. Verify solid dark gray background
5. Verify FPS counter visible in top-left corner
6. Move mouse around window; verify no crashes or freezes
7. Press ESC key
8. Verify window closes cleanly
9. Check console logs for "PoorcraftEngine initialized - CP 0.1 OK"

**Expected Result**: Window opens, displays FPS, responds to input, closes cleanly on ESC.

**Pass Criteria**:
- ✅ Window opens without errors
- ✅ Title bar shows "Poorcraft Ultra"
- ✅ FPS counter visible and updating
- ✅ ESC key closes window
- ✅ No exceptions in console

---

## Checkpoint 0.2: F3 Debug Overlay

**Objective**: Verify debug overlay toggle and information display.

**Steps**:
1. Run game: `scripts\dev\run.bat` or `./scripts/dev/run.sh`
2. Press F3 key
3. Verify debug overlay appears showing:
   - FPS counter
   - Java version
   - OS name and version
   - Heap memory usage (used/max MB)
   - CPU count
   - Hotkey hints (F3/F9/F10/F11/ESC)
4. Press F3 again; verify overlay disappears
5. Press F3 to show overlay again
6. Press F9; check console for "F9: Reload assets (stub)"
7. Press F10; check console for "F10: Rebuild meshes (stub)"
8. Press F11; check console for "F11: Show chunk bounds (stub)"
9. Press ESC; verify clean exit

**Expected Result**: Overlay toggles on/off, displays system info, hotkeys log stub messages.

**Pass Criteria**:
- ✅ F3 toggles overlay visibility
- ✅ Overlay shows FPS, Java version, OS, heap usage, CPU count
- ✅ F9/F10/F11 log stub messages to console
- ✅ ESC closes window cleanly
- ✅ Console logs "Debug overlay enabled - CP 0.2 OK"

---

## Checkpoint 1.35: Save/Load Region Files + Checksum Validation

**Expected Duration:** ~6 minutes

### Part 1: Save Verification
1. Launch game: `scripts\dev\run.bat` (Windows) or `./scripts/dev/run.sh` (Unix)
2. Break 5 blocks and place 5 blocks in the world; note coordinates from console logs
3. Press F3; confirm "Chunks: 9 loaded"
4. Press ESC to exit
5. **VERIFY:** Console shows
   - `PoorcraftEngine shutting down...`
   - `Saving 9 chunks...`
   - `Saved chunk (x, z) to ...` (9 lines)
   - `All chunks saved successfully`
   - `PoorcraftEngine shutdown complete`
6. Inspect `data/worlds/default/region/`
   - 9 files named `r.{x}.{z}.dat`
   - Each file size = 65,568 bytes
   - Modified timestamps within last minute

**Pass Criteria:** All 9 region files exist with correct size; logs confirm save succeeded.

### Part 2: Load Verification
1. Relaunch game
2. **VERIFY:** Console logs `Loaded chunk (x, z) from disk` for all nine chunks; no generation messages
3. Travel to previously modified coordinates
4. Confirm all broken/placed blocks persist with correct types; checkerboard pattern intact elsewhere
5. Press F3; verify "Chunks: 9 loaded"
6. Press ESC to exit

**Pass Criteria:** All previous edits persist; no chunks regenerated unexpectedly.

### Part 3: Checksum Validation (Corruption Handling)
1. Locate `data/worlds/default/region/r.0.0.dat`
2. Corrupt file using hex editor or script (flip a byte after the 32-byte header)
3. Launch game
4. **VERIFY:** Console logs warning about checksum mismatch and regenerates chunk `(0, 0)`
5. Inspect world; chunk `(0, 0)` reset to checkerboard while others remain intact
6. Press ESC to exit

**Pass Criteria:** Corrupt file detected and regenerated without affecting other chunks.

### Part 4: Abnormal Exit (Shutdown Hook)
1. Launch game and modify a few blocks
2. Instead of ESC, terminate via Ctrl+C in terminal
3. **VERIFY:** Console logs `Shutdown hook triggered - saving world...` followed by save messages
4. Relaunch game; confirm modifications persist

**Pass Criteria:** Shutdown hook saves world even on forced termination.

---

## Automated Tests

Run automated unit tests:
```bash
gradlew.bat test          # Windows
./gradlew test            # Linux/macOS
```

**Expected Result**: All tests pass. For smoke tests only, run `./gradlew test --tests "*SaveLoadIntegrationTest"`.

---

## Troubleshooting

### Window doesn't open
- Check Java version: `java -version` (should be 17.x)
- Check console for LWJGL native loading errors
- Verify graphics drivers are up to date

### FPS counter missing
- Check console logs for StatsAppState attachment
- Verify jME initialization completed

### F3 overlay doesn't appear
- Check console for "DebugOverlayAppState initialized"
- Verify input mappings registered

### Build fails
- Ensure JAVA_HOME is set correctly
- Run `gradlew.bat clean build` to rebuild from scratch
- Check internet connection (Gradle needs to download dependencies)

---

## Phase 1.5: UI System, Menus, Input Configuration

### CP 1.5.0: Main Menu
**Expected Duration:** 3 minutes

1. Launch game: `./scripts/dev/run.sh` (or `.bat`)
2. **VERIFY:** Main menu appears with title "Poorcraft Ultra"
3. **VERIFY:** Three buttons visible: "Start Game", "Settings", "Exit"
4. **VERIFY:** Cursor is visible and can move freely
5. **VERIFY:** No chunks loaded yet (no terrain visible)
6. Click "Exit" button
7. **VERIFY:** Game closes cleanly
8. Launch game again
9. Click "Settings" button
10. **VERIFY:** Settings menu opens with tabs: Graphics, Controls, Audio
11. Click "Cancel" or ESC
12. **VERIFY:** Returns to main menu

**PASS CRITERIA:** Main menu renders, buttons work, cursor visible, no auto-load.

---

### CP 1.5.1: In-Game State & Pause Menu
**Expected Duration:** 4 minutes

1. Launch game
2. Click "Start Game" button
3. **VERIFY:** Main menu disappears
4. **VERIFY:** Terrain loads (checkerboard or 3×3 chunk grid)
5. **VERIFY:** Cursor is hidden (not visible on screen)
6. **VERIFY:** Mouse look works (move mouse → camera rotates)
7. **VERIFY:** WASD movement works
8. Press ESC
9. **VERIFY:** Pause menu appears with "Paused" title
10. **VERIFY:** Three buttons visible: "Resume", "Settings", "Save & Exit to Menu"
11. **VERIFY:** Cursor is visible again
12. **VERIFY:** Game world still visible in background (not unloaded)
13. **VERIFY:** Camera/movement frozen (can't move while paused)
14. Click "Resume" button
15. **VERIFY:** Pause menu disappears
16. **VERIFY:** Cursor hidden again
17. **VERIFY:** Can move and look around
18. Press ESC again
19. Click "Save & Exit to Menu"
20. **VERIFY:** Returns to main menu
21. **VERIFY:** Console logs "Saving 9 chunks..."
22. Click "Start Game" again
23. **VERIFY:** World loads from save (modifications preserved)

**PASS CRITERIA:** Pause menu works, cursor toggles correctly, save/exit works.

---

### CP 1.5.2: Settings Menu (Graphics Tab)
**Expected Duration:** 5 minutes

1. Launch game
2. Click "Settings" from main menu
3. **VERIFY:** Graphics tab selected by default
4. **VERIFY:** Settings visible: Resolution dropdown, Window Mode dropdown, VSync checkbox, FPS Limit slider
5. Change resolution to "1920x1080"
6. Change Window Mode to "Fullscreen"
7. Disable VSync
8. Set FPS Limit to 144
9. Click "Apply"
10. **VERIFY:** Prompt appears: "Some changes require restart. Restart now?"
11. Click "Yes"
12. **VERIFY:** Game restarts in fullscreen 1920×1080
13. Open Settings again
14. **VERIFY:** Settings show new values (1920×1080, Fullscreen, VSync off, 144 FPS)
15. Change back to "1280x720" Windowed
16. Click "Apply" and restart
17. **VERIFY:** Window returns to 1280×720 windowed mode

**PASS CRITERIA:** Graphics settings apply correctly, persist across restarts.

---

### CP 1.5.3: Settings Menu (Controls Tab)
**Expected Duration:** 6 minutes

1. Launch game
2. Click "Settings" → "Controls" tab
3. **VERIFY:** Mouse Sensitivity slider visible (default 1.5)
4. **VERIFY:** "Invert Mouse Y" checkbox visible (default unchecked)
5. **VERIFY:** Keybind list visible with actions: Move Forward (W), Move Backward (S), etc.
6. Drag Mouse Sensitivity slider to 3.0
7. Click "Apply"
8. Click "Start Game"
9. **VERIFY:** Mouse look is faster (more sensitive)
10. Press ESC → Settings → Controls
11. Enable "Invert Mouse Y" checkbox
12. Click "Apply"
13. Resume game
14. Move mouse up
15. **VERIFY:** Camera looks down (Y-axis inverted)
16. Move mouse down
17. **VERIFY:** Camera looks up
18. Press ESC → Settings → Controls
19. Click "Rebind" next to "Move Forward"
20. **VERIFY:** Button changes to "Press a key..."
21. Press E key
22. **VERIFY:** Keybind updates to "E"
23. Click "Apply"
24. Resume game
25. Press W
26. **VERIFY:** No forward movement (W unbound)
27. Press E
28. **VERIFY:** Character moves forward
29. Press ESC → Settings → Controls
30. Rebind "Move Forward" back to W
31. Click "Apply"
32. Exit to main menu
33. Restart game
34. Start game
35. **VERIFY:** Mouse sensitivity still 3.0, Y-axis still inverted, keybinds persisted

**PASS CRITERIA:** Mouse settings work, keybind rebinding works, settings persist.

---

### CP 1.5.4: Window Resize & UI Scaling
**Expected Duration:** 3 minutes

1. Launch game in windowed mode (1280×720)
2. **VERIFY:** Main menu buttons centered and readable
3. Drag window corner to resize to ~800×600
4. **VERIFY:** UI scales down proportionally (buttons smaller but still readable)
5. **VERIFY:** Text not cut off or overlapping
6. Resize window to ~1920×1080
7. **VERIFY:** UI scales up proportionally (buttons larger)
8. Click "Start Game"
9. Resize window while in-game
10. **VERIFY:** Debug overlay (F3) scales correctly
11. **VERIFY:** Crosshair highlight scales correctly
12. Press ESC (pause menu)
13. Resize window
14. **VERIFY:** Pause menu scales correctly
15. Maximize window
16. **VERIFY:** UI fills screen without distortion

**PASS CRITERIA:** UI scales correctly on resize, no clipping or distortion.

---

### Smoke Test: Config Persistence (Automated)
**Run via:** `./gradlew test --tests "*ConfigSaverTest"` 

**PASS CRITERIA:** All config save/load tests pass (round-trip, async save, etc.).
