"""
Generate 64×64 procedural block textures.

Creates seamless, deterministic textures for wood, stone, dirt, grass, leaves, and ores.
"""

import argparse
import sys
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw

from common import (
    create_seamless_noise,
    apply_palette,
    add_grain,
    ensure_tiling,
    compute_sha256,
    save_with_metadata,
    stable_int_seed,
    seed_to_rng
)


def generate_wood(variant, seed, size=64):
    """Generate wood texture with vertical grain."""
    # Brown palettes for different wood types
    palettes = {
        'oak': [(101, 67, 33), (139, 90, 43), (160, 110, 60)],
        'birch': [(200, 190, 160), (220, 210, 180), (240, 230, 200)],
        'spruce': [(80, 60, 40), (100, 75, 50), (115, 85, 55)],
        'dark_oak': [(50, 35, 20), (70, 50, 30), (85, 60, 35)]
    }
    
    palette = palettes.get(variant, palettes['oak'])
    
    # Deterministic variant offset derived from stable identifier; avoid hash() salt.
    variant_offset = stable_int_seed(f"wood:{variant}") & 0x7fffffff
    mixed_seed = (seed + variant_offset) % (2**32)

    # Create vertical grain pattern
    noise = create_seamless_noise(size, size, mixed_seed, octaves=6, freq=2.0)
    
    # Emphasize vertical direction - generate vertical noise once
    vertical_noise = create_seamless_noise(size, size, (mixed_seed + 100) % (2**32), 
                                           octaves=3, freq=8.0)
    
    # Blend the two noise patterns
    for y in range(size):
        for x in range(size):
            noise[y, x] = int(noise[y, x] * 0.7 + vertical_noise[y, x] * 0.3)
    
    image = apply_palette(noise, palette)
    image = add_grain(image, intensity=0.08, seed=mixed_seed)
    
    # Add subtle horizontal rings
    draw = ImageDraw.Draw(image, 'RGBA')
    for i in range(3):
        y_pos = (i * size // 3 + mixed_seed % 10) % size
        draw.line([(0, y_pos), (size, y_pos)], fill=(0, 0, 0, 20), width=1)
    
    return image


def generate_stone(variant, seed, size=64):
    """Generate stone texture with mineral inclusions."""
    palettes = {
        'stone': [(100, 100, 100), (130, 130, 130), (150, 150, 150)],
        'granite': [(120, 90, 80), (140, 110, 100), (160, 130, 120)],
        'diorite': [(180, 180, 180), (200, 200, 200), (220, 220, 220)],
        'andesite': [(90, 90, 90), (110, 110, 110), (130, 130, 130)]
    }
    
    palette = palettes.get(variant, palettes['stone'])
    
    variant_offset = stable_int_seed(f"stone:{variant}") & 0x7fffffff
    mixed_seed = (seed + variant_offset) % (2**32)

    # Multi-octave noise for rocky texture
    noise = create_seamless_noise(size, size, mixed_seed, octaves=8, freq=6.0)
    image = apply_palette(noise, palette)
    image = add_grain(image, intensity=0.12, seed=mixed_seed)

    # Add small dark spots for mineral inclusions
    rng = seed_to_rng(mixed_seed)
    img_array = np.array(image)

    for _ in range(size // 4):  # Random spots
        x = int(rng.integers(0, size))
        y = int(rng.integers(0, size))
        spot_size = int(rng.integers(1, 3))
        
        for dy in range(-spot_size, spot_size + 1):
            for dx in range(-spot_size, spot_size + 1):
                px, py = (x + dx) % size, (y + dy) % size
                img_array[py, px] = img_array[py, px] * 0.7  # Darken
    
    return Image.fromarray(img_array)


def generate_dirt(seed, size=64):
    """Generate dirt texture."""
    palette = [(90, 60, 30), (110, 75, 40), (130, 90, 50)]
    
    seed = seed % (2**32)
    noise = create_seamless_noise(size, size, seed, octaves=6, freq=5.0)
    image = apply_palette(noise, palette)
    image = add_grain(image, intensity=0.15, seed=seed)

    # Add small particle details
    rng = seed_to_rng(seed)
    img_array = np.array(image)

    for _ in range(size // 2):
        x = int(rng.integers(0, size))
        y = int(rng.integers(0, size))
        img_array[y, x] = img_array[y, x] * 0.8
    
    return Image.fromarray(img_array)


def generate_grass(face, seed, size=64):
    """Generate grass texture (top/side/bottom faces)."""
    if face == 'top':
        # Green grass with variation
        palette = [(60, 120, 40), (80, 140, 60), (100, 160, 80)]
        noise = create_seamless_noise(size, size, seed, octaves=6, freq=6.0)
        image = apply_palette(noise, palette)
        image = add_grain(image, intensity=0.1, seed=seed)
        return image
    
    elif face == 'side':
        # Green top strip + brown dirt
        image = Image.new('RGB', (size, size))
        img_array = np.array(image)
        
        # Top 1/4 is grass
        grass_height = size // 4
        grass_palette = [(60, 120, 40), (80, 140, 60), (100, 160, 80)]
        grass_noise = create_seamless_noise(size, grass_height, seed, octaves=4, freq=4.0)
        grass_img = apply_palette(grass_noise, grass_palette)
        img_array[:grass_height, :] = np.array(grass_img)
        
        # Bottom 3/4 is dirt
        dirt_palette = [(90, 60, 30), (110, 75, 40), (130, 90, 50)]
        dirt_noise = create_seamless_noise(size, size - grass_height, seed + 1, octaves=4, freq=4.0)
        dirt_img = apply_palette(dirt_noise, dirt_palette)
        img_array[grass_height:, :] = np.array(dirt_img)
        
        return Image.fromarray(img_array)
    
    else:  # bottom
        return generate_dirt(seed + 2, size)


def generate_leaves(variant, seed, size=64):
    """Generate leaf texture with semi-transparency."""
    palettes = {
        'oak': [(50, 100, 30), (70, 120, 50), (90, 140, 70)],
        'birch': [(80, 130, 60), (100, 150, 80), (120, 170, 100)]
    }
    
    palette = palettes.get(variant, palettes['oak'])
    
    variant_offset = stable_int_seed(f"leaves:{variant}") & 0x7fffffff
    mixed_seed = (seed + variant_offset) % (2**32)

    # High-frequency noise for leaf texture
    noise = create_seamless_noise(size, size, mixed_seed, octaves=8, freq=8.0)
    image = apply_palette(noise, palette)
    
    # Convert to RGBA for transparency
    image = image.convert('RGBA')
    img_array = np.array(image)
    
    # Add semi-transparent edges
    for y in range(size):
        for x in range(size):
            # Distance from center
            dx = (x - size / 2) / (size / 2)
            dy = (y - size / 2) / (size / 2)
            dist = np.sqrt(dx * dx + dy * dy)
            
            # Fade edges
            if dist > 0.8:
                alpha = int(255 * (1.0 - (dist - 0.8) / 0.2))
                img_array[y, x, 3] = max(0, min(255, alpha))
    
    return Image.fromarray(img_array, mode='RGBA')


def generate_ore(ore_type, seed, size=64):
    """Generate ore texture (stone base with colored veins)."""
    # Stone base
    base_image = generate_stone('stone', seed, size)
    img_array = np.array(base_image)
    
    # Ore colors
    ore_colors = {
        'coal': (40, 40, 40),
        'iron': (180, 140, 100),
        'gold': (255, 215, 0)
    }
    
    ore_color = ore_colors.get(ore_type, ore_colors['coal'])
    
    type_offset = stable_int_seed(f"ore:{ore_type}") & 0x7fffffff
    mixed_seed = (seed + type_offset) % (2**32)

    # Generate ore vein pattern using warped noise
    vein_noise = create_seamless_noise(size, size, mixed_seed, octaves=4, freq=3.0)
    
    # Apply ore color where noise is high
    threshold = 180  # Only brightest areas become ore
    for y in range(size):
        for x in range(size):
            if vein_noise[y, x] > threshold:
                # Blend ore color with base
                blend = (vein_noise[y, x] - threshold) / (255 - threshold)
                img_array[y, x] = (
                    img_array[y, x] * (1 - blend) + np.array(ore_color) * blend
                ).astype(np.uint8)
    
    return Image.fromarray(img_array)


def generate_all_blocks(output_dir, seed):
    """Generate all block textures and return manifest entries."""
    output_dir = Path(output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    
    manifest_entries = []
    
    # Wood variants
    for variant in ['oak', 'birch', 'spruce', 'dark_oak']:
        filename = f'wood_{variant}.png'
        filepath = output_dir / filename
        print(f"Generating {filename}...")
        
        image = generate_wood(variant, seed)
        ensure_tiling(image)
        
        metadata = {
            'generator': 'gen_blocks.py',
            'seed': str(seed),
            'type': f'wood_{variant}',
            'category': 'blocks'
        }
        save_with_metadata(image, filepath, metadata)
        
        manifest_entries.append({
            'name': f'wood_{variant}',
            'category': 'blocks',
            'path': f'blocks/{filename}',
            'width': 64,
            'height': 64,
            'sha256': compute_sha256(filepath)
        })
    
    # Stone variants
    for variant in ['stone', 'granite', 'diorite', 'andesite']:
        filename = f'stone_{variant}.png'
        filepath = output_dir / filename
        print(f"Generating {filename}...")
        
        image = generate_stone(variant, seed)
        ensure_tiling(image)
        
        metadata = {
            'generator': 'gen_blocks.py',
            'seed': str(seed),
            'type': f'stone_{variant}',
            'category': 'blocks'
        }
        save_with_metadata(image, filepath, metadata)
        
        manifest_entries.append({
            'name': f'stone_{variant}',
            'category': 'blocks',
            'path': f'blocks/{filename}',
            'width': 64,
            'height': 64,
            'sha256': compute_sha256(filepath)
        })
    
    # Dirt
    filename = 'dirt.png'
    filepath = output_dir / filename
    print(f"Generating {filename}...")
    
    image = generate_dirt(seed)
    ensure_tiling(image)
    
    metadata = {'generator': 'gen_blocks.py', 'seed': str(seed), 'type': 'dirt', 'category': 'blocks'}
    save_with_metadata(image, filepath, metadata)
    
    manifest_entries.append({
        'name': 'dirt',
        'category': 'blocks',
        'path': f'blocks/{filename}',
        'width': 64,
        'height': 64,
        'sha256': compute_sha256(filepath)
    })
    
    # Grass (3 faces)
    for face in ['top', 'side', 'bottom']:
        filename = f'grass_{face}.png'
        filepath = output_dir / filename
        print(f"Generating {filename}...")
        
        image = generate_grass(face, seed)
        ensure_tiling(image)
        
        metadata = {'generator': 'gen_blocks.py', 'seed': str(seed), 'type': f'grass_{face}', 'category': 'blocks'}
        save_with_metadata(image, filepath, metadata)
        
        manifest_entries.append({
            'name': f'grass_{face}',
            'category': 'blocks',
            'path': f'blocks/{filename}',
            'width': 64,
            'height': 64,
            'sha256': compute_sha256(filepath)
        })
    
    # Leaves
    for variant in ['oak', 'birch']:
        filename = f'leaves_{variant}.png'
        filepath = output_dir / filename
        print(f"Generating {filename}...")
        
        image = generate_leaves(variant, seed)
        # Note: RGBA images, tiling check may show warnings due to alpha
        
        metadata = {'generator': 'gen_blocks.py', 'seed': str(seed), 'type': f'leaves_{variant}', 'category': 'blocks'}
        save_with_metadata(image, filepath, metadata)
        
        manifest_entries.append({
            'name': f'leaves_{variant}',
            'category': 'blocks',
            'path': f'blocks/{filename}',
            'width': 64,
            'height': 64,
            'sha256': compute_sha256(filepath)
        })
    
    # Ores
    for ore_type in ['coal', 'iron', 'gold']:
        filename = f'ore_{ore_type}.png'
        filepath = output_dir / filename
        print(f"Generating {filename}...")
        
        image = generate_ore(ore_type, seed)
        ensure_tiling(image)
        
        metadata = {'generator': 'gen_blocks.py', 'seed': str(seed), 'type': f'ore_{ore_type}', 'category': 'blocks'}
        save_with_metadata(image, filepath, metadata)
        
        manifest_entries.append({
            'name': f'ore_{ore_type}',
            'category': 'blocks',
            'path': f'blocks/{filename}',
            'width': 64,
            'height': 64,
            'sha256': compute_sha256(filepath)
        })
    
    return manifest_entries


def main():
    parser = argparse.ArgumentParser(description='Generate procedural block textures')
    parser.add_argument('--output', default='../assets/blocks', help='Output directory')
    parser.add_argument('--seed', type=int, default=42, help='Random seed')
    
    args = parser.parse_args()
    
    try:
        manifest_entries = generate_all_blocks(args.output, args.seed)
        print(f"\nGenerated {len(manifest_entries)} block textures")
        return manifest_entries
    except Exception as e:
        print(f"ERROR: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()
