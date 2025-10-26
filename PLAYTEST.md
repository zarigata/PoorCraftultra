# Manual Playtest Guide

## Overview

This guide explains how to manually test PoorCraft Ultra builds across Windows, Linux, and macOS. The goal is to ensure that published builds install (or run) correctly, hit target performance, and shut down cleanly when the ESC key is pressed.

## Test Environment Requirements

### Operating Systems
- **Windows:** Windows 10 (64-bit) or Windows 11
- **Linux:** Ubuntu 22.04 LTS (or equivalent modern distro with X11/Wayland)
- **macOS:** macOS 10.15 Catalina or later

### Hardware
- Quad-core CPU
- 8 GB RAM minimum
- Mid-range GPU (e.g., GTX 1060 / RX 580 equivalent)
- 500 MB free disk space

### Clean System Recommendation
Whenever possible, test on a clean machine or virtual machine without the .NET SDK installed. This verifies that the self-contained build includes all required runtime components.

---

## Windows Playtest

### Test A: NSIS Installer
1. Download `PoorCraftUltra-v{VERSION}-windows-x64-installer.exe`.
2. Run installer (admin privileges may be required).
3. Follow wizard:
   - Accept license (if present).
   - Choose install directory (default: `C:\Program Files\PoorCraftUltra`).
   - Enable Start Menu and Desktop shortcut options.
4. Finish installation and launch the game from the **Start Menu** shortcut.

**Expected:**
- Installer completes without errors.
- Game installs to chosen directory.
- Start Menu shortcut launches the game.
- Desktop shortcut created if selected.

### Test B: Portable ZIP
1. Download `PoorCraftUltra-v{VERSION}-windows-x64-portable.zip`.
2. Extract contents to `C:\Games\PoorCraftUltra` (or preferred location).
3. Launch `PoorCraftUltra.exe`.

**Expected:**
- Extraction succeeds.
- Game launches without needing .NET installed on system.

### Test C: Game Functionality
1. Launch game (from either installer or portable build).
2. Confirm window opens within 3 seconds.
3. Note window title (`PoorCraft Ultra`) and default size (800x600).
4. Observe smooth rendering at 60 FPS (check logs after exit if needed).
5. Press `ESC` to exit.
6. Inspect `logs/` directory beside executable for latest log file; check for errors.

**Expected:**
- Stable 60 FPS (allow ±2 FPS in logs).
- ESC cleanly shuts down within 1 second.
- Logs contain Info-level entries, no errors/exceptions.

### Test D: Uninstall (Installer Only)
1. Open **Settings → Apps**.
2. Locate **PoorCraft Ultra** and uninstall.
3. Verify Start Menu/desktop shortcuts removed and install directory deleted.

**Expected:**
- Uninstaller removes files and registry entries.

---

## Linux Playtest

### Test A: Debian Package
1. Download `poorcraftultra_{VERSION}_amd64.deb`.
2. Install: `sudo dpkg -i poorcraftultra_{VERSION}_amd64.deb`.
   - If dependency errors occur: `sudo apt-get install -f`.
3. Launch from application menu (Games section) or run `/usr/lib/poorcraftultra/PoorCraftUltra`.

**Expected:**
- Package installs cleanly.
- Desktop entry appears with icon.
- Game launches via menu or direct command.

### Test B: AppImage
1. Download `PoorCraftUltra-v{VERSION}-linux-x86_64.AppImage`.
2. Make executable: `chmod +x PoorCraftUltra-v{VERSION}-linux-x86_64.AppImage`.
3. Run the AppImage.

**Expected:**
- AppImage runs without installation.
- No missing dependency errors (FUSE must be available).

### Test C: Portable Tarball
1. Download `PoorCraftUltra-v{VERSION}-linux-x64-portable.tar.gz`.
2. Extract: `tar -xzf PoorCraftUltra-v{VERSION}-linux-x64-portable.tar.gz`.
3. Run `./PoorCraftUltra` from extracted folder.

**Expected:**
- Executable launches directly.
- No permission issues (script sets executable bit).

### Test D: Game Functionality
Follow the same steps as Windows Test C. Check logs in:
- `/usr/share/doc/poorcraftultra/` for packaged docs.
- Local directory (`logs/`) for AppImage/tarball builds.

### Test E: Uninstall (.deb)
1. Run `sudo dpkg -r poorcraftultra`.
2. Verify that `/usr/lib/poorcraftultra` and desktop entry are removed.

**Expected:**
- Package removes cleanly without leftover files.

---

## macOS Playtest

### Test A: DMG Installer
1. Download `PoorCraftUltra-v{VERSION}-macos-x64.dmg`.
2. Mount DMG and drag **PoorCraft Ultra.app** into **Applications**.
3. Eject DMG and launch from Applications folder.
4. If Gatekeeper warns about an unidentified developer:
   - Cancel.
   - Right-click the app, choose **Open**, then confirm.

**Expected:**
- DMG mounts and copies without errors.
- App launches after Gatekeeper override.

### Test B: Portable ZIP
1. Download `PoorCraftUltra-v{VERSION}-macos-x64-portable.zip`.
2. Extract and launch the `.app` bundle from the extracted folder.

**Expected:**
- App runs from any location.

### Test C: Game Functionality
Repeat steps from Windows Test C. For logs, right-click the app, choose **Show Package Contents**, then inspect `Contents/MacOS/logs/`.

### Test D: Uninstall
1. Delete **PoorCraft Ultra.app** from Applications (or location of portable build).
2. Empty Trash.

**Expected:**
- App bundle removes cleanly with no extra files.

---

## Performance Verification (All Platforms)

### FPS Check
1. Let the game run for 60 seconds.
2. Exit with ESC.
3. Open latest log file and confirm average FPS between 58–62.

### Memory Usage
1. Launch game.
2. Monitor process with Task Manager / System Monitor / Activity Monitor for 5 minutes.
3. Ensure memory usage stabilises (< 200 MB, no unbounded growth).

### Startup Time
1. Close game.
2. Start timer and relaunch.
3. Confirm window appears in under 5 seconds.

---

## Issue Reporting Template
When filing issues, include:
1. **Platform & Version:** OS name/version, package type used.
2. **Steps to Reproduce:** Detailed steps from this guide.
3. **Expected vs Actual:** What should happen vs what occurred.
4. **Logs:** Attach the relevant log file from `logs/`.
5. **Hardware Specs:** CPU, GPU (and driver version), RAM.
6. **Screenshots / Videos:** If applicable.

Submit issues on GitHub: <https://github.com/yourusername/PoorCraftUltra/issues> (prefix titles with `[Playtest]`).

---

## Checklist Summary

### Windows
- [ ] Installer installs and launches
- [ ] Portable ZIP runs
- [ ] Window opens and renders at 60 FPS
- [ ] ESC closes cleanly
- [ ] Logs captured without errors
- [ ] Uninstaller removes files

### Linux
- [ ] .deb installs and launches
- [ ] AppImage runs and integrates
- [ ] Portable tarball runs
- [ ] Window opens and renders at 60 FPS
- [ ] ESC closes cleanly
- [ ] Logs captured without errors
- [ ] `dpkg -r` removes files

### macOS
- [ ] DMG installs to Applications
- [ ] Portable ZIP runs from any location
- [ ] Gatekeeper bypass documented
- [ ] Window opens and renders at 60 FPS
- [ ] ESC closes cleanly
- [ ] Logs captured without errors
- [ ] App bundle removable

---

## Next Steps
After successfully completing all tests:
1. Update the project roadmap status.
2. Bump the version for the next phase in `PoorCraftUltra.csproj`.
3. Follow the release workflow to publish updated binaries.
4. Proceed to Phase 3: Basic 3D Rendering Pipeline.
