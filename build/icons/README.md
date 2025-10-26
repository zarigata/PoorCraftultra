# PoorCraft Ultra Icons

This directory stores platform-specific application icons used by build scripts and installers.

## Current Status

- **Placeholder artwork** generated programmatically via [`generate_icons.py`](generate_icons.py).
- Icons display a blue square with the letters "PC". Replace with professional artwork in a future phase.

## Required Assets

| Platform | File(s) | Notes |
|----------|---------|-------|
| Windows  | `poorcraftultra.ico` | Multi-resolution ICO (256→16 px). Used for executable and NSIS installer. |
| Linux    | `poorcraftultra-<size>.png` | PNG set at 512, 256, 128, 64, 48, 32, 16px. Installed into `/usr/share/icons/hicolor`. |
| macOS    | `poorcraftultra.icns` | ICNS bundle containing Retina resolutions (1024→16 px). Used in `.app` bundle & DMG. |

## Regenerating Placeholder Icons

Install Python 3 and run:

```bash
python build/icons/generate_icons.py
```

This script creates the ICO, ICNS, and PNG variants using basic vector math. It requires no external dependencies.

## Updating Artwork

1. Design a 1024×1024 source image with transparency.
2. Export platform-specific assets using ImageMagick, `sips`/`iconutil` (macOS), or other tooling.
3. Replace files in this directory while keeping filenames consistent.
4. Re-run platform build scripts to ensure icons appear correctly in installers and applications.
5. Update this README if the process changes (e.g., new sizes or formats).

## Licensing

Ensure any replacement assets are original work or appropriately licensed. Document attribution details here when upgrading from placeholder icons.
