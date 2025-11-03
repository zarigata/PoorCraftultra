"""Procedural item icon generator (64×64 PNG)."""

import argparse
from pathlib import Path
from typing import Tuple

import numpy as np
from PIL import Image, ImageDraw

from common import (
    validate_size, compute_hash, write_manifest, SeededRandom
)


ITEM_SIZE = 64


def draw_tool(canvas: Image.Image, tool_type: str, tier: str, seed: int):
    """Draw tool icon (pickaxe, axe, shovel, hoe, sword)."""
    draw = ImageDraw.Draw(canvas)
    
    # Tier colors
    tier_colors = {
        'wood': (139, 90, 43),
        'stone': (128, 128, 128),
        'iron': (192, 192, 192),
        'gold': (255, 215, 0),
        'diamond': (0, 191, 255)
    }
    
    head_color = tier_colors.get(tier, tier_colors['wood'])
    handle_color = (101, 67, 33)  # Brown handle
    
    if tool_type == 'pickaxe':
        # Handle
        draw.rectangle([28, 40, 32, 60], fill=handle_color)
        # Head
        points = [(20, 20), (44, 20), (44, 28), (20, 28)]
        draw.polygon(points, fill=head_color)
        # Pick points
        draw.polygon([(16, 20), (20, 20), (20, 28), (16, 24)], fill=head_color)
        draw.polygon([(44, 20), (48, 24), (44, 28)], fill=head_color)
        
    elif tool_type == 'axe':
        # Handle
        draw.rectangle([28, 35, 32, 60], fill=handle_color)
        # Blade
        points = [(24, 20), (40, 20), (44, 28), (40, 36), (24, 36)]
        draw.polygon(points, fill=head_color)
        
    elif tool_type == 'shovel':
        # Handle
        draw.rectangle([28, 30, 32, 60], fill=handle_color)
        # Blade
        points = [(24, 20), (40, 20), (36, 32), (28, 32)]
        draw.polygon(points, fill=head_color)
        
    elif tool_type == 'hoe':
        # Handle
        draw.rectangle([28, 35, 32, 60], fill=handle_color)
        # Blade
        draw.rectangle([20, 20, 42, 26], fill=head_color)
        draw.rectangle([28, 26, 32, 35], fill=head_color)
        
    elif tool_type == 'sword':
        # Handle (guard + grip + pommel)
        draw.rectangle([28, 44, 36, 56], fill=handle_color)
        draw.rectangle([24, 42, 40, 46], fill=(80, 80, 80))  # Guard
        draw.ellipse([26, 56, 38, 62], fill=(80, 80, 80))  # Pommel
        # Blade
        points = [(30, 10), (34, 10), (34, 44), (30, 44)]
        draw.polygon(points, fill=head_color)
        # Tip
        draw.polygon([(30, 10), (32, 4), (34, 10)], fill=head_color)
    
    # Add outline
    draw.rectangle([0, 0, ITEM_SIZE - 1, ITEM_SIZE - 1], outline=(0, 0, 0), width=1)


def draw_material(canvas: Image.Image, material_type: str, seed: int):
    """Draw material icon."""
    draw = ImageDraw.Draw(canvas)
    
    if material_type == 'stick':
        # Brown stick
        draw.rectangle([28, 10, 32, 54], fill=(101, 67, 33))
        
    elif material_type == 'coal':
        # Black coal chunk
        points = [(28, 20), (40, 24), (38, 36), (26, 38), (24, 28)]
        draw.polygon(points, fill=(30, 30, 30))
        
    elif material_type == 'iron_ingot':
        # Silver ingot
        draw.rectangle([20, 28, 44, 40], fill=(192, 192, 192))
        # Add gradient
        for i in range(12):
            shade = 192 - i * 8
            draw.line([(20, 28 + i), (44, 28 + i)], fill=(shade, shade, shade))
            
    elif material_type == 'gold_ingot':
        # Gold ingot
        draw.rectangle([20, 28, 44, 40], fill=(255, 215, 0))
        for i in range(12):
            shade = 255 - i * 10
            draw.line([(20, 28 + i), (44, 28 + i)], fill=(shade, int(215 - i * 8), 0))
            
    elif material_type == 'diamond':
        # Cyan diamond
        points = [(32, 16), (42, 26), (32, 48), (22, 26)]
        draw.polygon(points, fill=(0, 191, 255))
        # Add facets
        draw.line([(32, 16), (32, 48)], fill=(0, 150, 200), width=1)
        draw.line([(22, 26), (42, 26)], fill=(0, 150, 200), width=1)
        
    elif material_type == 'redstone':
        # Red dust pile
        for _ in range(20):
            x = seed % 30 + 17
            y = seed % 30 + 17
            seed = (seed * 1103515245 + 12345) & 0x7fffffff
            draw.ellipse([x, y, x + 4, y + 4], fill=(255, 0, 0))
            
    elif material_type == 'emerald':
        # Green emerald
        points = [(32, 18), (40, 26), (40, 38), (32, 46), (24, 38), (24, 26)]
        draw.polygon(points, fill=(0, 201, 87))


def draw_food(canvas: Image.Image, food_type: str, seed: int):
    """Draw food icon."""
    draw = ImageDraw.Draw(canvas)
    
    if food_type == 'apple':
        # Red apple
        draw.ellipse([22, 26, 42, 46], fill=(220, 20, 60))
        # Highlight
        draw.ellipse([26, 28, 32, 34], fill=(255, 100, 120))
        # Leaf
        draw.ellipse([30, 20, 36, 26], fill=(34, 139, 34))
        
    elif food_type == 'bread':
        # Tan loaf
        draw.ellipse([18, 28, 46, 42], fill=(245, 222, 179))
        # Crust detail
        for i in range(3):
            x = 22 + i * 8
            draw.line([(x, 30), (x, 40)], fill=(210, 180, 140), width=1)
            
    elif food_type == 'cooked_meat':
        # Brown cooked meat
        points = [(24, 26), (40, 26), (42, 32), (40, 38), (24, 38), (22, 32)]
        draw.polygon(points, fill=(139, 69, 19))
        
    elif food_type == 'raw_meat':
        # Red raw meat
        points = [(24, 26), (40, 26), (42, 32), (40, 38), (24, 38), (22, 32)]
        draw.polygon(points, fill=(220, 20, 60))


def generate_tool(tool_type: str, tier: str, seed: int) -> Image.Image:
    """Generate tool icon."""
    img = Image.new('RGBA', (ITEM_SIZE, ITEM_SIZE), (0, 0, 0, 0))
    draw_tool(img, tool_type, tier, seed)
    return img.convert('RGB')


def generate_material(material_type: str, seed: int) -> Image.Image:
    """Generate material icon."""
    img = Image.new('RGBA', (ITEM_SIZE, ITEM_SIZE), (0, 0, 0, 0))
    draw_material(img, material_type, seed)
    return img.convert('RGB')


def generate_food(food_type: str, seed: int) -> Image.Image:
    """Generate food icon."""
    img = Image.new('RGBA', (ITEM_SIZE, ITEM_SIZE), (0, 0, 0, 0))
    draw_food(img, food_type, seed)
    return img.convert('RGB')


def generate_block_item(block_name: str, seed: int) -> Image.Image:
    """Generate simplified block-as-item icon."""
    img = Image.new('RGB', (ITEM_SIZE, ITEM_SIZE), (150, 150, 150))
    draw = ImageDraw.Draw(img)
    
    # Simple isometric cube representation
    # Top face
    points = [(32, 16), (48, 24), (32, 32), (16, 24)]
    draw.polygon(points, fill=(180, 180, 180))
    
    # Left face
    points = [(16, 24), (32, 32), (32, 48), (16, 40)]
    draw.polygon(points, fill=(140, 140, 140))
    
    # Right face
    points = [(32, 32), (48, 24), (48, 40), (32, 48)]
    draw.polygon(points, fill=(160, 160, 160))
    
    return img


def generate_all_items(output_dir: Path, seed: int):
    """Generate all item icons."""
    output_dir.mkdir(parents=True, exist_ok=True)
    
    entries = []
    
    # Tools
    tool_types = ['pickaxe', 'axe', 'shovel', 'hoe', 'sword']
    tiers = ['wooden', 'stone', 'iron', 'gold', 'diamond']
    
    for tier in tiers:
        for tool_type in tool_types:
            img = generate_tool(tool_type, tier.replace('wooden', 'wood'), seed)
            validate_size(img, ITEM_SIZE, ITEM_SIZE)
            
            filename = f"{tier}_{tool_type}.png"
            filepath = output_dir / filename
            img.save(filepath)
            
            entries.append({
                'name': filename,
                'width': ITEM_SIZE,
                'height': ITEM_SIZE,
                'hash': compute_hash(filepath)
            })
    
    # Materials
    materials = ['stick', 'coal', 'iron_ingot', 'gold_ingot', 'diamond', 'redstone', 'emerald']
    
    for material in materials:
        img = generate_material(material, seed)
        validate_size(img, ITEM_SIZE, ITEM_SIZE)
        
        filename = f"{material}.png"
        filepath = output_dir / filename
        img.save(filepath)
        
        entries.append({
            'name': filename,
            'width': ITEM_SIZE,
            'height': ITEM_SIZE,
            'hash': compute_hash(filepath)
        })
    
    # Food
    foods = ['apple', 'bread', 'cooked_meat', 'raw_meat']
    
    for food in foods:
        img = generate_food(food, seed)
        validate_size(img, ITEM_SIZE, ITEM_SIZE)
        
        filename = f"{food}.png"
        filepath = output_dir / filename
        img.save(filepath)
        
        entries.append({
            'name': filename,
            'width': ITEM_SIZE,
            'height': ITEM_SIZE,
            'hash': compute_hash(filepath)
        })
    
    # Block items
    block_items = ['dirt', 'stone', 'wood']
    
    for block in block_items:
        img = generate_block_item(block, seed)
        validate_size(img, ITEM_SIZE, ITEM_SIZE)
        
        filename = f"{block}_item.png"
        filepath = output_dir / filename
        img.save(filepath)
        
        entries.append({
            'name': filename,
            'width': ITEM_SIZE,
            'height': ITEM_SIZE,
            'hash': compute_hash(filepath)
        })
    
    # Special items
    special_items = ['crafting_table', 'furnace', 'chest']
    
    for item in special_items:
        img = generate_block_item(item, seed)
        validate_size(img, ITEM_SIZE, ITEM_SIZE)
        
        filename = f"{item}_item.png"
        filepath = output_dir / filename
        img.save(filepath)
        
        entries.append({
            'name': filename,
            'width': ITEM_SIZE,
            'height': ITEM_SIZE,
            'hash': compute_hash(filepath)
        })
    
    # Write manifest
    write_manifest(output_dir, entries)
    
    print(f"Generated {len(entries)} item icons in {output_dir}")
    return len(entries)


def main():
    parser = argparse.ArgumentParser(description='Generate procedural item icons')
    parser.add_argument('--output-dir', type=Path, default=Path('assets/items'),
                       help='Output directory')
    parser.add_argument('--seed', type=int, default=42,
                       help='Random seed')
    
    args = parser.parse_args()
    
    generate_all_items(args.output_dir, args.seed)


if __name__ == '__main__':
    main()
