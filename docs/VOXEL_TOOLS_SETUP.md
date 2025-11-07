# Voxel Tools Setup Guide

This project relies on Zylann's Voxel Tools to provide smooth, LOD-enabled voxel terrain. Godot 4.3 does not support installing Voxel Tools from the AssetLib, so a custom editor build is required.

## Installation Options

### 1. Recommended: Download Custom Editor Build

Use the prebuilt Godot 4.3 editor that bundles Voxel Tools 1.3.0.

- **Release:** `Godot 4.3.stable.custom_build + Voxel Tools 1.3.0` (August 17, 2024)
- **Download:** <https://github.com/Zylann/godot_voxel/releases/tag/1.3.0>
- Choose the archive for your platform (Windows, Linux, macOS).

### 2. Advanced: Build From Source

If you need to customize the engine, compile Godot 4.3 from source with the voxel module enabled. Follow the instructions in the Voxel Tools repository's `docs/build.md` and ensure you target the same commit as the August 17, 2024 release.

> **Note:** The GDExtension version of Voxel Tools requires Godot 4.4.1 or newer and is **not** compatible with this project.

## Setup Steps

1. Download the custom editor executable for your platform.
2. Extract it to a dedicated folder (e.g. `C:/Godot_Voxel/` on Windows or `~/godot_voxel/` on Linux/macOS).
3. Launch the project using this custom editor (do **not** use the stock Godot 4.3 build).
4. Verify the installation: in the editor, add a node and search for `VoxelLodTerrain`. If it appears, the integration is ready.

## Export Templates

1. Download the matching export templates from the same release page.
2. In the editor, open **Editor → Manage Export Templates** and install the downloaded template archive.
3. Configure each export preset to use the custom templates.

## Team Workflow

- All team members must run the same custom build to avoid compatibility issues.
- Document the build version in `project.godot`, this README, or both.
- Consider storing the editor archive in a shared location or reference this guide in onboarding documentation.

## Troubleshooting

- **Voxel nodes missing:** Ensure you launched the project with the custom build, not vanilla Godot 4.3.
- **Export failures:** Confirm that the custom export templates are installed.
- **Terrain not generating:** Check the in-game console and `ErrorLogger` output for voxel-related errors.

## References

- Voxel Tools repository: <https://github.com/Zylann/godot_voxel>
- Documentation: <https://voxel-tools.readthedocs.io/>
- Releases & custom builds: <https://github.com/Zylann/godot_voxel/releases>
