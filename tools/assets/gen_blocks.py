"""Procedural block texture generator (64×64 PNG)."""

import argparse
from pathlib import Path
from typing import List, Dict

import numpy as np
from PIL import Image, ImageDraw

from common import (
    generate_perlin_2d, apply_palette, add_grain, validate_size,
    compute_hash, write_manifest, SeededRandom,
    PALETTE_STONE, PALETTE_DIRT, PALETTE_GRASS, PALETTE_WOOD, PALETTE_ORE
)


BLOCK_SIZE = 64


def generate_stone(seed: int, variant: str = 'stone') -> Image.Image:
    """Generate stone texture with Perlin noise."""
    noise = generate_perlin_2d(BLOCK_SIZE, BLOCK_SIZE, scale=8.0, octaves=4, seed=seed)
    rgb = apply_palette(noise, PALETTE_STONE)
    rgb = add_grain(rgb, intensity=0.15)
    return Image.fromarray(rgb, 'RGB')


def generate_dirt(seed: int) -> Image.Image:
    """Generate dirt texture with darker specks."""
    noise = generate_perlin_2d(BLOCK_SIZE, BLOCK_SIZE, scale=6.0, octaves=5, seed=seed)
    rgb = apply_palette(noise, PALETTE_DIRT)
    
    # Add darker specks
    speck_noise = generate_perlin_2d(BLOCK_SIZE, BLOCK_SIZE, scale=20.0, octaves=2, seed=seed + 100)
    mask = speck_noise > 0.7
    rgb[mask] = rgb[mask] * 0.6
    
    rgb = add_grain(rgb, intensity=0.1)
    return Image.fromarray(rgb.astype(np.uint8), 'RGB')


def generate_grass_top(seed: int) -> Image.Image:
    """Generate grass top with blade patterns."""
    noise = generate_perlin_2d(BLOCK_SIZE, BLOCK_SIZE, scale=5.0, octaves=4, seed=seed)
    rgb = apply_palette(noise, PALETTE_GRASS)
    
    # Add grass blade patterns (darker green lines)
    rng = SeededRandom(seed)
    img = Image.fromarray(rgb, 'RGB')
    draw = ImageDraw.Draw(img)
    
    for _ in range(20):
        x = rng.randint(0, BLOCK_SIZE - 1)
        y = rng.randint(0, BLOCK_SIZE - 1)
        length = rng.randint(2, 6)
        color = (20, 100, 20)
        draw.line([(x, y), (x, y + length)], fill=color, width=1)
    
    return img


def generate_grass_side(seed: int) -> Image.Image:
    """Generate grass side (dirt bottom, grass top)."""
    img = Image.new('RGB', (BLOCK_SIZE, BLOCK_SIZE))
    
    # Bottom half: dirt
    dirt = generate_dirt(seed)
    dirt_array = np.array(dirt)
    
    # Top half: grass
    grass_noise = generate_perlin_2d(BLOCK_SIZE, BLOCK_SIZE // 2, scale=5.0, octaves=3, seed=seed)
    grass_rgb = apply_palette(grass_noise, PALETTE_GRASS)
    
    # Combine with blend
    result = np.zeros((BLOCK_SIZE, BLOCK_SIZE, 3), dtype=np.uint8)
    result[BLOCK_SIZE // 2:, :] = dirt_array[BLOCK_SIZE // 2:, :]
    result[:BLOCK_SIZE // 2, :] = grass_rgb
    
    # Blend transition
    blend_height = 4
    for i in range(blend_height):
        alpha = i / blend_height
        y = BLOCK_SIZE // 2 - blend_height + i
        result[y, :] = (alpha * dirt_array[y, :] + (1 - alpha) * grass_rgb[min(i, grass_rgb.shape[0] - 1), :])
    
    return Image.fromarray(result, 'RGB')


def generate_wood_planks(seed: int, wood_type: str = 'oak') -> Image.Image:
    """Generate wood planks with vertical grain."""
    base_colors = {
        'oak': (160, 108, 58),
        'birch': (216, 194, 140),
        'spruce': (101, 67, 33)
    }
    base_color = base_colors.get(wood_type, base_colors['oak'])
    
    img = Image.new('RGB', (BLOCK_SIZE, BLOCK_SIZE), base_color)
    rgb = np.array(img)
    
    # Vertical grain lines
    for x in range(BLOCK_SIZE):
        noise_val = generate_perlin_2d(1, BLOCK_SIZE, scale=10.0, seed=seed + x)[0, 0]
        variation = int((noise_val - 0.5) * 40)
        rgb[:, x] = np.clip(rgb[:, x] + variation, 0, 255)
    
    rgb = add_grain(rgb, intensity=0.08)
    return Image.fromarray(rgb, 'RGB')


def generate_wood_log_top(seed: int) -> Image.Image:
    """Generate wood log top with tree rings."""
    img = Image.new('RGB', (BLOCK_SIZE, BLOCK_SIZE), (139, 90, 43))
    draw = ImageDraw.Draw(img)
    
    center = BLOCK_SIZE // 2
    rng = SeededRandom(seed)
    
    # Draw concentric rings
    for radius in range(4, center, 3):
        color_var = rng.randint(-20, 20)
        color = (139 + color_var, 90 + color_var // 2, 43 + color_var // 3)
        draw.ellipse([center - radius, center - radius, center + radius, center + radius],
                    outline=color, width=2)
    
    return img


def generate_wood_log_side(seed: int) -> Image.Image:
    """Generate wood log side with vertical bark texture."""
    noise = generate_perlin_2d(BLOCK_SIZE, BLOCK_SIZE, scale=4.0, octaves=6, seed=seed)
    
    # Dark brown palette for bark
    bark_palette = [(60, 40, 20), (80, 53, 26), (100, 66, 33), (70, 46, 23)]
    rgb = apply_palette(noise, bark_palette)
    
    # Add vertical streaks
    for x in range(0, BLOCK_SIZE, 4):
        streak_noise = generate_perlin_2d(1, BLOCK_SIZE, scale=15.0, seed=seed + x * 10)
        rgb[:, x] = rgb[:, x] * (0.7 + streak_noise[0, 0] * 0.3)
    
    rgb = add_grain(rgb, intensity=0.2)
    return Image.fromarray(rgb.astype(np.uint8), 'RGB')


def generate_ore(seed: int, ore_type: str) -> Image.Image:
    """Generate ore texture (stone base + colored veins)."""
    # Base stone
    stone = generate_stone(seed)
    rgb = np.array(stone)
    
    # Ore veins
    ore_color = PALETTE_ORE[ore_type]
    vein_noise = generate_perlin_2d(BLOCK_SIZE, BLOCK_SIZE, scale=12.0, octaves=3, seed=seed + 500)
    
    # Apply ore color where noise is high
    mask = vein_noise > 0.65
    for c in range(3):
        rgb[:, :, c][mask] = ore_color[c]
    
    return Image.fromarray(rgb, 'RGB')


def generate_sand(seed: int) -> Image.Image:
    """Generate sand texture (tan/beige fine grain)."""
    noise = generate_perlin_2d(BLOCK_SIZE, BLOCK_SIZE, scale=15.0, octaves=4, seed=seed)
    
    sand_palette = [(238, 214, 175), (245, 222, 179), (255, 228, 181), (250, 235, 215)]
    rgb = apply_palette(noise, sand_palette)
    rgb = add_grain(rgb, intensity=0.05)
    
    return Image.fromarray(rgb, 'RGB')


def generate_gravel(seed: int) -> Image.Image:
    """Generate gravel texture (gray with angular chunks)."""
    noise = generate_perlin_2d(BLOCK_SIZE, BLOCK_SIZE, scale=8.0, octaves=5, seed=seed)
    
    gravel_palette = [(128, 128, 128), (105, 105, 105), (119, 136, 153), (112, 128, 144)]
    rgb = apply_palette(noise, gravel_palette)
    
    # Add angular chunks
    chunk_noise = generate_perlin_2d(BLOCK_SIZE, BLOCK_SIZE, scale=20.0, octaves=2, seed=seed + 200)
    mask = chunk_noise > 0.6
    rgb[mask] = rgb[mask] * 0.7
    
    rgb = add_grain(rgb, intensity=0.15)
    return Image.fromarray(rgb.astype(np.uint8), 'RGB')


def generate_glass(seed: int) -> Image.Image:
    """Generate glass texture (transparent with subtle grid)."""
    img = Image.new('RGBA', (BLOCK_SIZE, BLOCK_SIZE), (200, 220, 255, 100))
    draw = ImageDraw.Draw(img)
    
    # Subtle grid pattern
    for i in range(0, BLOCK_SIZE, 8):
        draw.line([(i, 0), (i, BLOCK_SIZE)], fill=(180, 200, 230, 50), width=1)
        draw.line([(0, i), (BLOCK_SIZE, i)], fill=(180, 200, 230, 50), width=1)
    
    return img.convert('RGB')  # Convert to RGB for consistency


def generate_leaves(seed: int, tree_type: str = 'oak') -> Image.Image:
    """Generate leaves texture with organic pattern."""
    noise = generate_perlin_2d(BLOCK_SIZE, BLOCK_SIZE, scale=6.0, octaves=4, seed=seed)
    rgb = apply_palette(noise, PALETTE_GRASS)
    
    # Add darker spots for depth
    spot_noise = generate_perlin_2d(BLOCK_SIZE, BLOCK_SIZE, scale=10.0, octaves=2, seed=seed + 300)
    mask = spot_noise > 0.6
    rgb[mask] = rgb[mask] * 0.7
    
    return Image.fromarray(rgb.astype(np.uint8), 'RGB')


def generate_crafting_table_top(seed: int) -> Image.Image:
    """Generate crafting table top with grid pattern."""
    base = generate_wood_planks(seed, 'oak')
    img = base.copy()
    draw = ImageDraw.Draw(img)
    
    # Draw grid
    grid_color = (100, 66, 33)
    for i in range(0, BLOCK_SIZE, BLOCK_SIZE // 3):
        draw.line([(i, 0), (i, BLOCK_SIZE)], fill=grid_color, width=2)
        draw.line([(0, i), (BLOCK_SIZE, i)], fill=grid_color, width=2)
    
    return img


def generate_furnace_front(seed: int) -> Image.Image:
    """Generate furnace front (stone with dark opening)."""
    stone = generate_stone(seed)
    img = stone.copy()
    draw = ImageDraw.Draw(img)
    
    # Draw furnace opening
    opening_rect = [BLOCK_SIZE // 4, BLOCK_SIZE // 3, 3 * BLOCK_SIZE // 4, 2 * BLOCK_SIZE // 3]
    draw.rectangle(opening_rect, fill=(20, 20, 20))
    
    # Draw grill lines
    for i in range(opening_rect[1] + 4, opening_rect[3], 4):
        draw.line([(opening_rect[0], i), (opening_rect[2], i)], fill=(40, 40, 40), width=1)
    
    return img


def generate_chest_front(seed: int) -> Image.Image:
    """Generate chest front (wood with latch)."""
    base = generate_wood_planks(seed, 'oak')
    img = base.copy()
    draw = ImageDraw.Draw(img)
    
    # Draw latch
    latch_color = (80, 80, 80)
    center_x = BLOCK_SIZE // 2
    center_y = BLOCK_SIZE // 2
    
    # Horizontal bar
    draw.rectangle([center_x - 16, center_y - 2, center_x + 16, center_y + 2], fill=latch_color)
    
    # Lock
    draw.ellipse([center_x - 4, center_y - 4, center_x + 4, center_y + 4], fill=(60, 60, 60))
    
    return img


def generate_torch(seed: int) -> Image.Image:
    """Generate torch texture (stick with flame)."""
    img = Image.new('RGB', (BLOCK_SIZE, BLOCK_SIZE), (0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Draw stick (brown vertical bar in center)
    stick_width = BLOCK_SIZE // 8
    stick_x = (BLOCK_SIZE - stick_width) // 2
    stick_color = (101, 67, 33)
    draw.rectangle([stick_x, BLOCK_SIZE // 2, stick_x + stick_width, BLOCK_SIZE], fill=stick_color)
    
    # Draw flame (yellow-orange gradient at top)
    flame_height = BLOCK_SIZE // 3
    for y in range(flame_height):
        alpha = y / flame_height
        color_r = int(255 * (1 - alpha * 0.2))
        color_g = int(200 - alpha * 100)
        color_b = int(50 - alpha * 50)
        draw.ellipse([stick_x - 4, y, stick_x + stick_width + 4, y + 8], 
                    fill=(color_r, color_g, color_b))
    
    return img


def generate_lava(seed: int) -> Image.Image:
    """Generate lava texture (animated-looking orange/red)."""
    noise = generate_perlin_2d(BLOCK_SIZE, BLOCK_SIZE, scale=8.0, octaves=4, seed=seed)
    
    # Lava palette (dark red to bright orange)
    lava_palette = [(139, 0, 0), (178, 34, 34), (255, 69, 0), (255, 140, 0), (255, 165, 0)]
    rgb = apply_palette(noise, lava_palette)
    
    # Add bright spots
    bright_noise = generate_perlin_2d(BLOCK_SIZE, BLOCK_SIZE, scale=12.0, octaves=2, seed=seed + 500)
    mask = bright_noise > 0.7
    rgb[mask] = [255, 200, 0]
    
    return Image.fromarray(rgb.astype(np.uint8), 'RGB')


def generate_glowstone(seed: int) -> Image.Image:
    """Generate glowstone texture (bright yellow with cracks)."""
    noise = generate_perlin_2d(BLOCK_SIZE, BLOCK_SIZE, scale=6.0, octaves=3, seed=seed)
    
    # Glowstone palette (yellow-gold)
    glowstone_palette = [(255, 230, 150), (255, 240, 180), (255, 250, 200), (255, 255, 220)]
    rgb = apply_palette(noise, glowstone_palette)
    
    # Add darker cracks
    crack_noise = generate_perlin_2d(BLOCK_SIZE, BLOCK_SIZE, scale=15.0, octaves=2, seed=seed + 200)
    mask = crack_noise > 0.75
    rgb[mask] = rgb[mask] * 0.6
    
    return Image.fromarray(rgb.astype(np.uint8), 'RGB')


def generate_all_blocks(output_dir: Path, seed: int, count: int = 1):
    """Generate all block textures."""
    output_dir.mkdir(parents=True, exist_ok=True)
    
    entries = []
    
    # Stone variants
    for i in range(count):
        for variant in ['stone', 'cobblestone', 'stone_bricks']:
            img = generate_stone(seed + i * 100, variant)
            validate_size(img, BLOCK_SIZE, BLOCK_SIZE)
            
            filename = f"{variant}_{i + 1:02d}.png"
            filepath = output_dir / filename
            img.save(filepath)
            
            entries.append({
                'name': filename,
                'width': BLOCK_SIZE,
                'height': BLOCK_SIZE,
                'hash': compute_hash(filepath)
            })
    
    # Other blocks
    block_generators = [
        ('dirt', generate_dirt),
        ('grass_top', generate_grass_top),
        ('grass_side', generate_grass_side),
        ('sand', generate_sand),
        ('gravel', generate_gravel),
        ('glass', generate_glass),
    ]
    
    for name, generator in block_generators:
        for i in range(count):
            img = generator(seed + i * 100)
            validate_size(img, BLOCK_SIZE, BLOCK_SIZE)
            
            filename = f"{name}_{i + 1:02d}.png" if count > 1 else f"{name}.png"
            filepath = output_dir / filename
            img.save(filepath)
            
            entries.append({
                'name': filename,
                'width': BLOCK_SIZE,
                'height': BLOCK_SIZE,
                'hash': compute_hash(filepath)
            })
    
    # Wood variants
    for wood_type in ['oak', 'birch', 'spruce']:
        img = generate_wood_planks(seed, wood_type)
        validate_size(img, BLOCK_SIZE, BLOCK_SIZE)
        
        filename = f"{wood_type}_planks.png"
        filepath = output_dir / filename
        img.save(filepath)
        
        entries.append({
            'name': filename,
            'width': BLOCK_SIZE,
            'height': BLOCK_SIZE,
            'hash': compute_hash(filepath)
        })
    
    # Wood logs
    img = generate_wood_log_top(seed)
    validate_size(img, BLOCK_SIZE, BLOCK_SIZE)
    filepath = output_dir / "wood_log_top.png"
    img.save(filepath)
    entries.append({
        'name': "wood_log_top.png",
        'width': BLOCK_SIZE,
        'height': BLOCK_SIZE,
        'hash': compute_hash(filepath)
    })
    
    img = generate_wood_log_side(seed)
    validate_size(img, BLOCK_SIZE, BLOCK_SIZE)
    filepath = output_dir / "wood_log_side.png"
    img.save(filepath)
    entries.append({
        'name': "wood_log_side.png",
        'width': BLOCK_SIZE,
        'height': BLOCK_SIZE,
        'hash': compute_hash(filepath)
    })
    
    # Ores
    for ore_type in PALETTE_ORE.keys():
        img = generate_ore(seed, ore_type)
        validate_size(img, BLOCK_SIZE, BLOCK_SIZE)
        
        filename = f"{ore_type}_ore.png"
        filepath = output_dir / filename
        img.save(filepath)
        
        entries.append({
            'name': filename,
            'width': BLOCK_SIZE,
            'height': BLOCK_SIZE,
            'hash': compute_hash(filepath)
        })
    
    # Leaves
    for tree_type in ['oak', 'birch', 'spruce']:
        img = generate_leaves(seed, tree_type)
        validate_size(img, BLOCK_SIZE, BLOCK_SIZE)
        
        filename = f"{tree_type}_leaves.png"
        filepath = output_dir / filename
        img.save(filepath)
        
        entries.append({
            'name': filename,
            'width': BLOCK_SIZE,
            'height': BLOCK_SIZE,
            'hash': compute_hash(filepath)
        })
    
    # Special blocks
    img = generate_crafting_table_top(seed)
    validate_size(img, BLOCK_SIZE, BLOCK_SIZE)
    filepath = output_dir / "crafting_table_top.png"
    img.save(filepath)
    entries.append({
        'name': "crafting_table_top.png",
        'width': BLOCK_SIZE,
        'height': BLOCK_SIZE,
        'hash': compute_hash(filepath)
    })
    
    img = generate_furnace_front(seed)
    validate_size(img, BLOCK_SIZE, BLOCK_SIZE)
    filepath = output_dir / "furnace_front.png"
    img.save(filepath)
    entries.append({
        'name': "furnace_front.png",
        'width': BLOCK_SIZE,
        'height': BLOCK_SIZE,
        'hash': compute_hash(filepath)
    })
    
    img = generate_chest_front(seed)
    validate_size(img, BLOCK_SIZE, BLOCK_SIZE)
    filepath = output_dir / "chest_front.png"
    img.save(filepath)
    entries.append({
        'name': "chest_front.png",
        'width': BLOCK_SIZE,
        'height': BLOCK_SIZE,
        'hash': compute_hash(filepath)
    })
    
    # Light-emitting blocks
    img = generate_torch(seed)
    validate_size(img, BLOCK_SIZE, BLOCK_SIZE)
    filepath = output_dir / "torch.png"
    img.save(filepath)
    entries.append({
        'name': "torch.png",
        'width': BLOCK_SIZE,
        'height': BLOCK_SIZE,
        'hash': compute_hash(filepath)
    })
    
    img = generate_lava(seed)
    validate_size(img, BLOCK_SIZE, BLOCK_SIZE)
    filepath = output_dir / "lava.png"
    img.save(filepath)
    entries.append({
        'name': "lava.png",
        'width': BLOCK_SIZE,
        'height': BLOCK_SIZE,
        'hash': compute_hash(filepath)
    })
    
    img = generate_glowstone(seed)
    validate_size(img, BLOCK_SIZE, BLOCK_SIZE)
    filepath = output_dir / "glowstone.png"
    img.save(filepath)
    entries.append({
        'name': "glowstone.png",
        'width': BLOCK_SIZE,
        'height': BLOCK_SIZE,
        'hash': compute_hash(filepath)
    })
    
    # Write manifest
    write_manifest(output_dir, entries)
    
    print(f"Generated {len(entries)} block textures in {output_dir}")
    return len(entries)


def main():
    parser = argparse.ArgumentParser(description='Generate procedural block textures')
    parser.add_argument('--output-dir', type=Path, default=Path('assets/blocks'),
                       help='Output directory')
    parser.add_argument('--seed', type=int, default=42,
                       help='Random seed')
    parser.add_argument('--count', type=int, default=1,
                       help='Number of variants per block type')
    
    args = parser.parse_args()
    
    generate_all_blocks(args.output_dir, args.seed, args.count)


if __name__ == '__main__':
    main()
