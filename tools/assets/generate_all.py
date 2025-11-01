"""
Master script to run all asset generators and write manifest.

Orchestrates generation of blocks, skins, and items, then aggregates
manifest entries into a single JSON file.
"""

import argparse
import json
import sys
import traceback
from pathlib import Path
from datetime import datetime

# Import generators
import gen_blocks
import gen_skins
import gen_icons


def main():
    print("[gen] Starting asset generation")
    parser = argparse.ArgumentParser(description='Generate all procedural assets')
    parser.add_argument('--output-base', default='../assets', help='Base output directory')
    parser.add_argument('--seed', type=int, default=42, help='Random seed')
    parser.add_argument('--force', action='store_true', 
                       help='Regenerate even if files exist')
    
    args = parser.parse_args()
    
    base_path = Path(args.output_base)
    
    # Create output directories
    print("=== Poorcraft Ultra Asset Generator ===\n")
    print(f"Output directory: {base_path.absolute()}")
    print(f"Seed: {args.seed}\n")
    
    blocks_dir = base_path / 'blocks'
    skins_dir = base_path / 'skins'
    items_dir = base_path / 'items'
    
    blocks_dir.mkdir(parents=True, exist_ok=True)
    skins_dir.mkdir(parents=True, exist_ok=True)
    items_dir.mkdir(parents=True, exist_ok=True)
    
    # Check if assets already exist
    manifest_path = base_path / 'manifest.json'
    if manifest_path.exists() and not args.force:
        print(f"[gen] Assets already exist at {base_path}")
        print("[gen] Use --force to regenerate")
        
        # Load and display existing manifest
        with open(manifest_path, 'r') as f:
            manifest = json.load(f)
        
        print(f"[gen] Existing manifest (version {manifest['version']}):")
        print(f"[gen]   Generated: {manifest['generated']}")
        print(f"[gen]   Seed: {manifest['seed']}")
        print(f"[gen]   Total assets: {len(manifest['assets'])}")
        
        # Count by category
        categories = {}
        for asset in manifest['assets']:
            cat = asset['category']
            categories[cat] = categories.get(cat, 0) + 1
        
        for cat, count in categories.items():
            print(f"    {cat}: {count}")
        
        return 0
    
    # Generate assets
    all_entries = []
    
    try:
        print("--- Generating Blocks ---")
        block_entries = gen_blocks.generate_all_blocks(str(blocks_dir), args.seed)
        all_entries.extend(block_entries)
        print(f"[OK] Generated {len(block_entries)} block textures\n")
        
        print("--- Generating Skins ---")
        skin_entries = gen_skins.generate_all_skins(str(skins_dir), args.seed)
        all_entries.extend(skin_entries)
        print(f"[OK] Generated {len(skin_entries)} skins\n")
        
        print("--- Generating Items ---")
        item_entries = gen_icons.generate_all_icons(str(items_dir), args.seed)
        all_entries.extend(item_entries)
        print(f"[OK] Generated {len(item_entries)} item icons\n")
        
    except Exception as e:
        print(f"\nERROR during generation: {e}", file=sys.stderr)
        import traceback
        traceback.print_exc()
        return 1
    
    # Validate all files exist
    print("--- Validating Generated Files ---")
    missing_files = []
    
    for entry in all_entries:
        filepath = base_path / entry['path']
        if not filepath.exists():
            missing_files.append(entry['path'])
            print(f"[FAIL] Missing: {entry['path']}")
    
    if missing_files:
        print(f"\nERROR: {len(missing_files)} files missing after generation", 
              file=sys.stderr)
        return 1
    
    print(f"[OK] All {len(all_entries)} files exist\n")
    
    # Validate dimensions
    print("--- Validating Dimensions ---")
    from PIL import Image
    
    dimension_errors = []
    
    for entry in all_entries:
        filepath = base_path / entry['path']
        
        try:
            with Image.open(filepath) as img:
                if img.width != entry['width'] or img.height != entry['height']:
                    error = (f"{entry['path']}: expected {entry['width']}×{entry['height']}, "
                            f"got {img.width}×{img.height}")
                    dimension_errors.append(error)
                    print(f"[FAIL] {error}")
        except Exception as e:
            error = f"{entry['path']}: failed to load - {e}"
            dimension_errors.append(error)
            print(f"[FAIL] {error}")
    
    if dimension_errors:
        print(f"\nERROR: {len(dimension_errors)} dimension mismatches", 
              file=sys.stderr)
        return 1
    
    print(f"[OK] All dimensions valid\n")
    
    # Create manifest
    manifest = {
        'version': '1.0',
        'generated': datetime.utcnow().isoformat() + 'Z',
        'seed': args.seed,
        'assets': all_entries
    }
    
    # Write manifest
    print(f"--- Writing Manifest ---")
    with open(manifest_path, 'w') as f:
        json.dump(manifest, f, indent=2)
    
    print(f"[OK] Manifest written to {manifest_path}\n")
    
    # Summary
    print("=== Generation Complete ===")
    print(f"Total assets: {len(all_entries)}")
    
    # Count by category
    categories = {}
    for entry in all_entries:
        cat = entry['category']
        categories[cat] = categories.get(cat, 0) + 1
    
    for cat, count in sorted(categories.items()):
        print(f"  {cat}: {count}")
    
    print(f"\nManifest: {manifest_path}")
    print("Assets ready for validation and use!")
    
    return 0


if __name__ == '__main__':
    try:
        exit_code = main()
    except Exception as exc:  # pragma: no cover - defensive logging for CLI
        print(f"[gen] Unhandled error during asset generation: {exc}", file=sys.stderr)
        traceback.print_exc()
        exit_code = 1
    sys.exit(exit_code)
