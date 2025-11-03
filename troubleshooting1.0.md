# Troubleshooting 1.0

## Context
- Date/Time: 2025-11-02 (local UTC-03:00 ~21:08)
- Command: `scripts\dev\run.bat` (via `cmd /c`)
- Java: Temurin OpenJDK 17.0.16 (64-bit)
- Recent code updates: reduced cave near-surface buffer (`CaveCarver`), added early break in surface loop (`WorldGenerator`), confirmed chest recipe docs/tests. Comment 4 (multi-face biome grass textures) still **pending**.

## Observed Behaviour
- Game launches to a grey skybox and checkerboard ground (see attached screenshot from user).
- User reports mouse/keyboard input appears unresponsive (cannot move or click UI) and no visible main menu.
- Console log excerpts during run:
  - `UIScaleProcessor initialized at 1280x720`
  - `InGameState enabled`
  - `WorldSaveManager] Loaded chunk (0, 1) from data\worlds\default\region\r.0.1.dat`
  - Similar "Loaded chunk" lines for existing region files; **no** `Generated new chunk` entries.
- FPS counter visible; renderer active; no fatal errors logged before session ended (user closed window).

## Initial Diagnosis
1. **Existing world data masking generator changes**
   - Logs show all chunks loaded from `data/worlds/default/region`. Because terrain already persisted before modifications, chunk data still reflects old flat/checkerboard world.
   - New cave-carving / surface logic only affects chunks generated after the change. Saved chunks bypass new generation paths.
2. **Input lock vs. UI state**
   - `InGameState` enabling means we likely skipped main menu by default (consistent with prior runs). Mouse capture might fail if window is not focused or if Lemur UI still intercepts input.
   - Need to verify whether `FlyCam` is disabled and whether `PlayerController` is active on load. No direct log entries confirming player input binding.
3. **Skybox absence**
   - Existing implementation renders a flat grey clear color when no skybox is configured. No regression detected relative to previous checkpoints; expected until skybox assets exist.

## Recommended Next Steps for Dev Team
1. **World regeneration check**
   - Clear or rename `data/worlds/default/` before running to force chunk regeneration. Confirm new terrain (grass, caves) appears and carving buffer change takes effect.
   - Longer term: introduce world/terrain version metadata and automatic migration (e.g., store generator version in save header and trigger regeneration when mismatch detected).
2. **Input diagnostics**
   - Add debug logging in `InGameState.initialize()` and `PlayerInputSystem` (or equivalent) to confirm mappings register.
   - Verify cursor lock state (`inputManager.setCursorVisible(false)`) and ensure no UI screens remain enabled on startup.
   - If issue persists, capture stack trace of input handling and confirm `AppStateManager` attaches the player controller.
3. **User-facing guidance**
   - Update README/troubleshooting to note that generator changes require deleting/regenerating saves.
   - Provide instructions for toggling debug overlay (F3) to verify chunk counts / player coordinates.
4. **Pending audit follow-up**
   - Implement Comment 4: biome grass variants require side/bottom textures or TODO placeholder.
   - After asset script updates, rerun `scripts/dev/gen-assets.bat` and validate manifest.

## Additional Notes
- No automated tests were executed post-run; impact currently limited to manual verification.
- Consider adding integration test that generates a fresh chunk and asserts cave carving frequency to catch future regressions.
- Screenshot indicates rendering still matches Phase 1 baseline; ensure documentation communicates this until worldgen content is expanded.
