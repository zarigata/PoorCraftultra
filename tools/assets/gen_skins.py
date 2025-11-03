"""Procedural player/NPC skin generator (256×256 PNG)."""

import argparse
from pathlib import Path
from typing import Tuple

import numpy as np
from PIL import Image, ImageDraw

from common import (
    generate_perlin_2d, validate_size, compute_hash, write_manifest, SeededRandom
)


SKIN_SIZE = 256


def draw_cube_face(canvas: Image.Image, x: int, y: int, w: int, h: int, 
                   color: Tuple[int, int, int], add_shading: bool = True):
    """Fill UV region with color and optional shading."""
    draw = ImageDraw.Draw(canvas)
    draw.rectangle([x, y, x + w - 1, y + h - 1], fill=color)
    
    if add_shading:
        # Add subtle gradient for 3D effect
        overlay = Image.new('RGBA', (w, h), (0, 0, 0, 0))
        overlay_draw = ImageDraw.Draw(overlay)
        
        # Darker at bottom
        for i in range(h):
            alpha = int((i / h) * 30)
            overlay_draw.line([(0, i), (w, i)], fill=(0, 0, 0, alpha))
        
        canvas.paste(overlay, (x, y), overlay)


def generate_player_base(seed: int) -> Image.Image:
    """Generate default player skin (Steve-like layout)."""
    img = Image.new('RGBA', (SKIN_SIZE, SKIN_SIZE), (0, 0, 0, 0))
    
    rng = SeededRandom(seed)
    
    # Skin tone
    skin_tone = (255, 220, 177)
    
    # Hair color
    hair_colors = [(101, 67, 33), (139, 69, 19), (70, 50, 30), (30, 20, 10)]
    hair_color = rng.choice(hair_colors)
    
    # Clothing colors
    shirt_color = (59, 68, 75)
    pants_color = (45, 55, 70)
    
    # Minecraft skin layout (Java Edition)
    # Head (front, back, left, right, top, bottom)
    # Front face
    draw_cube_face(img, 8, 8, 8, 8, skin_tone)
    
    # Add simple face features
    draw = ImageDraw.Draw(img)
    # Eyes
    draw.rectangle([10, 10, 11, 11], fill=(60, 100, 150))
    draw.rectangle([13, 10, 14, 11], fill=(60, 100, 150))
    # Mouth
    draw.line([(10, 14), (14, 14)], fill=(100, 70, 70), width=1)
    
    # Head sides
    draw_cube_face(img, 0, 8, 8, 8, skin_tone)  # Right
    draw_cube_face(img, 16, 8, 8, 8, skin_tone)  # Left
    draw_cube_face(img, 24, 8, 8, 8, skin_tone)  # Back
    
    # Head top/bottom
    draw_cube_face(img, 8, 0, 8, 8, hair_color)  # Top (hair)
    draw_cube_face(img, 16, 0, 8, 8, skin_tone)  # Bottom
    
    # Body
    draw_cube_face(img, 20, 20, 8, 12, shirt_color)  # Front
    draw_cube_face(img, 32, 20, 8, 12, shirt_color)  # Back
    draw_cube_face(img, 16, 20, 4, 12, shirt_color)  # Right
    draw_cube_face(img, 28, 20, 4, 12, shirt_color)  # Left
    draw_cube_face(img, 20, 16, 8, 4, shirt_color)  # Top
    draw_cube_face(img, 28, 16, 8, 4, shirt_color)  # Bottom
    
    # Right arm
    draw_cube_face(img, 44, 20, 4, 12, skin_tone)  # Front
    draw_cube_face(img, 52, 20, 4, 12, skin_tone)  # Back
    draw_cube_face(img, 40, 20, 4, 12, skin_tone)  # Right
    draw_cube_face(img, 48, 20, 4, 12, skin_tone)  # Left
    draw_cube_face(img, 44, 16, 4, 4, skin_tone)  # Top
    draw_cube_face(img, 48, 16, 4, 4, skin_tone)  # Bottom
    
    # Left arm
    draw_cube_face(img, 36, 52, 4, 12, skin_tone)  # Front
    draw_cube_face(img, 44, 52, 4, 12, skin_tone)  # Back
    draw_cube_face(img, 32, 52, 4, 12, skin_tone)  # Right
    draw_cube_face(img, 40, 52, 4, 12, skin_tone)  # Left
    draw_cube_face(img, 36, 48, 4, 4, skin_tone)  # Top
    draw_cube_face(img, 40, 48, 4, 4, skin_tone)  # Bottom
    
    # Right leg
    draw_cube_face(img, 4, 20, 4, 12, pants_color)  # Front
    draw_cube_face(img, 12, 20, 4, 12, pants_color)  # Back
    draw_cube_face(img, 0, 20, 4, 12, pants_color)  # Right
    draw_cube_face(img, 8, 20, 4, 12, pants_color)  # Left
    draw_cube_face(img, 4, 16, 4, 4, pants_color)  # Top
    draw_cube_face(img, 8, 16, 4, 4, pants_color)  # Bottom
    
    # Left leg
    draw_cube_face(img, 20, 52, 4, 12, pants_color)  # Front
    draw_cube_face(img, 28, 52, 4, 12, pants_color)  # Back
    draw_cube_face(img, 16, 52, 4, 12, pants_color)  # Right
    draw_cube_face(img, 24, 52, 4, 12, pants_color)  # Left
    draw_cube_face(img, 20, 48, 4, 4, pants_color)  # Top
    draw_cube_face(img, 24, 48, 4, 4, pants_color)  # Bottom
    
    # Second layer (overlay) - hat
    draw_cube_face(img, 40, 8, 8, 8, hair_color, add_shading=False)  # Front
    draw_cube_face(img, 32, 8, 8, 8, hair_color, add_shading=False)  # Right
    draw_cube_face(img, 48, 8, 8, 8, hair_color, add_shading=False)  # Left
    draw_cube_face(img, 56, 8, 8, 8, hair_color, add_shading=False)  # Back
    draw_cube_face(img, 40, 0, 8, 8, hair_color, add_shading=False)  # Top
    
    return img.convert('RGB')


def generate_villager(seed: int, variant: int = 1) -> Image.Image:
    """Generate villager NPC skin."""
    img = Image.new('RGBA', (SKIN_SIZE, SKIN_SIZE), (0, 0, 0, 0))
    
    rng = SeededRandom(seed + variant * 1000)
    
    # Villager skin tone (slightly different)
    skin_tone = (245, 205, 160)
    
    # Robe colors by profession
    robe_colors = [
        (139, 90, 43),   # Brown - farmer
        (80, 80, 80),    # Gray - blacksmith
        (255, 255, 255), # White - priest
        (100, 50, 150),  # Purple - librarian
    ]
    robe_color = robe_colors[(variant - 1) % len(robe_colors)]
    
    # Head (larger nose for villager)
    draw_cube_face(img, 8, 8, 8, 8, skin_tone)
    
    # Add villager features
    draw = ImageDraw.Draw(img)
    # Eyes (smaller, beady)
    draw.rectangle([10, 10, 10, 11], fill=(50, 50, 50))
    draw.rectangle([14, 10, 14, 11], fill=(50, 50, 50))
    # Large nose
    draw.rectangle([11, 11, 13, 14], fill=(220, 180, 140))
    # Unibrow
    draw.line([(9, 9), (15, 9)], fill=(80, 60, 40), width=1)
    
    # Head sides
    draw_cube_face(img, 0, 8, 8, 8, skin_tone)
    draw_cube_face(img, 16, 8, 8, 8, skin_tone)
    draw_cube_face(img, 24, 8, 8, 8, skin_tone)
    draw_cube_face(img, 8, 0, 8, 8, skin_tone)
    draw_cube_face(img, 16, 0, 8, 8, skin_tone)
    
    # Body (robe)
    draw_cube_face(img, 20, 20, 8, 12, robe_color)
    draw_cube_face(img, 32, 20, 8, 12, robe_color)
    draw_cube_face(img, 16, 20, 4, 12, robe_color)
    draw_cube_face(img, 28, 20, 4, 12, robe_color)
    draw_cube_face(img, 20, 16, 8, 4, robe_color)
    draw_cube_face(img, 28, 16, 8, 4, robe_color)
    
    # Arms (hidden in robe)
    draw_cube_face(img, 44, 20, 4, 12, robe_color)
    draw_cube_face(img, 52, 20, 4, 12, robe_color)
    draw_cube_face(img, 40, 20, 4, 12, robe_color)
    draw_cube_face(img, 48, 20, 4, 12, robe_color)
    
    draw_cube_face(img, 36, 52, 4, 12, robe_color)
    draw_cube_face(img, 44, 52, 4, 12, robe_color)
    draw_cube_face(img, 32, 52, 4, 12, robe_color)
    draw_cube_face(img, 40, 52, 4, 12, robe_color)
    
    # Legs (robe)
    draw_cube_face(img, 4, 20, 4, 12, robe_color)
    draw_cube_face(img, 12, 20, 4, 12, robe_color)
    draw_cube_face(img, 0, 20, 4, 12, robe_color)
    draw_cube_face(img, 8, 20, 4, 12, robe_color)
    
    draw_cube_face(img, 20, 52, 4, 12, robe_color)
    draw_cube_face(img, 28, 52, 4, 12, robe_color)
    draw_cube_face(img, 16, 52, 4, 12, robe_color)
    draw_cube_face(img, 24, 52, 4, 12, robe_color)
    
    return img.convert('RGB')


def generate_template_overlay(output_dir: Path):
    """Generate UV layout template for reference."""
    img = Image.new('RGB', (SKIN_SIZE, SKIN_SIZE), (255, 255, 255))
    draw = ImageDraw.Draw(img)
    
    # Draw grid
    for i in range(0, SKIN_SIZE, 8):
        draw.line([(i, 0), (i, SKIN_SIZE)], fill=(200, 200, 200), width=1)
        draw.line([(0, i), (SKIN_SIZE, i)], fill=(200, 200, 200), width=1)
    
    # Label regions
    labels = [
        (8, 8, "Head Front"),
        (20, 20, "Body"),
        (44, 20, "R Arm"),
        (4, 20, "R Leg"),
    ]
    
    for x, y, label in labels:
        draw.text((x, y), label, fill=(0, 0, 0))
    
    filepath = output_dir / "template_overlay.png"
    img.save(filepath)


def generate_all_skins(output_dir: Path, seed: int, variants: int = 3):
    """Generate all skin textures."""
    output_dir.mkdir(parents=True, exist_ok=True)
    
    entries = []
    
    # Player base skin
    img = generate_player_base(seed)
    validate_size(img, SKIN_SIZE, SKIN_SIZE)
    filepath = output_dir / "player_base.png"
    img.save(filepath)
    entries.append({
        'name': "player_base.png",
        'width': SKIN_SIZE,
        'height': SKIN_SIZE,
        'hash': compute_hash(filepath)
    })
    
    # Villager variants
    for i in range(1, variants + 1):
        img = generate_villager(seed, i)
        validate_size(img, SKIN_SIZE, SKIN_SIZE)
        
        filename = f"npc_villager_{i:02d}.png"
        filepath = output_dir / filename
        img.save(filepath)
        
        entries.append({
            'name': filename,
            'width': SKIN_SIZE,
            'height': SKIN_SIZE,
            'hash': compute_hash(filepath)
        })
    
    # Generate template
    generate_template_overlay(output_dir)
    
    # Write manifest
    write_manifest(output_dir, entries)
    
    print(f"Generated {len(entries)} skin textures in {output_dir}")
    return len(entries)


def main():
    parser = argparse.ArgumentParser(description='Generate procedural player/NPC skins')
    parser.add_argument('--output-dir', type=Path, default=Path('assets/skins'),
                       help='Output directory')
    parser.add_argument('--seed', type=int, default=42,
                       help='Random seed')
    parser.add_argument('--variants', type=int, default=3,
                       help='Number of NPC variants')
    
    args = parser.parse_args()
    
    generate_all_skins(args.output_dir, args.seed, args.variants)


if __name__ == '__main__':
    main()
