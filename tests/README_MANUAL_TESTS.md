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
