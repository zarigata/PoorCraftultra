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
