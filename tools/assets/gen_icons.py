"""
Generate 64×64 procedural item icons.

Creates item icons for tools, resources, and food with transparent backgrounds.
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
    compute_sha256,
    save_with_metadata,
    stable_int_seed,
    seed_to_rng
)


def generate_tool_icon(tool_type, material, seed, size=64):
    """Generate tool icon (pickaxe, axe, shovel)."""
    image = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    
    # Material colors
    material_colors = {
        'wood': (139, 90, 43),
        'stone': (130, 130, 130),
        'iron': (180, 180, 190),
        'gold': (255, 215, 0),
        'diamond': (80, 200, 220)
    }
    
    handle_color = (100, 70, 40)  # Brown handle
    head_color = material_colors.get(material, material_colors['wood'])
    
    if tool_type == 'pickaxe':
        # Handle (vertical line)
        draw.line([(size // 2, size // 2), (size // 2, size - 8)], fill=handle_color, width=4)
        
        # Pickaxe head (horizontal with points)
        head_y = size // 2 - 4
        draw.line([(12, head_y), (size - 12, head_y)], fill=head_color, width=6)
        
        # Points
        draw.polygon([(10, head_y), (12, head_y - 4), (14, head_y)], fill=head_color)
        draw.polygon([(size - 10, head_y), (size - 12, head_y - 4), (size - 14, head_y)], fill=head_color)
        
    elif tool_type == 'axe':
        # Handle (diagonal)
        draw.line([(size // 2, size - 8), (size // 2 - 8, size // 2)], fill=handle_color, width=4)
        
        # Axe head (blade shape)
        blade_points = [
            (size // 2 - 8, size // 2),
            (size // 2 - 4, size // 2 - 12),
            (size // 2 + 8, size // 2 - 8),
            (size // 2, size // 2 + 4)
        ]
        draw.polygon(blade_points, fill=head_color)
        
    elif tool_type == 'shovel':
        # Handle (vertical)
        draw.line([(size // 2, size // 2), (size // 2, size - 8)], fill=handle_color, width=4)
        
        # Shovel head (rounded rectangle)
        head_top = size // 2 - 12
        head_bottom = size // 2 + 4
        head_left = size // 2 - 6
        head_right = size // 2 + 6
        draw.rounded_rectangle(
            [(head_left, head_top), (head_right, head_bottom)],
            radius=3,
            fill=head_color
        )
    
    # Add shading for 3D effect
    img_array = np.array(image)
    for y in range(size):
        for x in range(size):
            if img_array[y, x, 3] > 0:  # Non-transparent
                # Darken bottom-right
                if x > size // 2 and y > size // 2:
                    img_array[y, x, :3] = (img_array[y, x, :3] * 0.8).astype(np.uint8)
    
    return Image.fromarray(img_array, mode='RGBA')


def generate_resource_icon(resource_type, seed, size=64):
    """Generate resource icon (miniature block texture)."""
    image = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    
    if resource_type == 'wood_log':
        # Brown wood texture
        palette = [(101, 67, 33), (139, 90, 43), (160, 110, 60)]
        noise = create_seamless_noise(size, size, seed, octaves=4, freq=3.0)
        texture = apply_palette(noise, palette)
        
    elif resource_type == 'stone_chunk':
        # Gray stone texture
        palette = [(100, 100, 100), (130, 130, 130), (150, 150, 150)]
        noise_seed = (seed + 1) % (2**32)
        noise = create_seamless_noise(size, size, noise_seed, octaves=6, freq=4.0)
        texture = apply_palette(noise, palette)
        
    elif resource_type.startswith('ore_'):
        # Ore nugget
        ore_type = resource_type.split('_')[1]
        ore_colors = {
            'coal': (40, 40, 40),
            'iron': (180, 140, 100),
            'gold': (255, 215, 0)
        }
        base_color = ore_colors.get(ore_type, ore_colors['coal'])
        
        # Create nugget shape
        texture = Image.new('RGB', (size, size), base_color)
        type_offset = stable_int_seed(f"resource:{ore_type}") & 0x7fffffff
        noise_seed = (seed + type_offset) % (2**32)
        noise = create_seamless_noise(size, size, noise_seed, octaves=4, freq=4.0)
        texture_array = np.array(texture)
        
        # Add noise variation
        for y in range(size):
            for x in range(size):
                variation = int((noise[y, x] - 128) * 0.2)
                texture_array[y, x] = np.clip(texture_array[y, x] + variation, 0, 255)
        
        texture = Image.fromarray(texture_array)
    
    else:
        texture = Image.new('RGB', (size, size), (100, 100, 100))
    
    # Add border and shadow
    draw = ImageDraw.Draw(texture)
    
    # Shadow (bottom-right)
    shadow_offset = 2
    draw.rectangle(
        [(shadow_offset, shadow_offset), (size - 1, size - 1)],
        outline=(0, 0, 0),
        width=1
    )
    
    # Convert to RGBA and make corners transparent
    texture = texture.convert('RGBA')
    img_array = np.array(texture)
    
    # Rounded corners
    corner_radius = 4
    for y in range(size):
        for x in range(size):
            # Distance from corners
            dist_tl = np.sqrt((x - 0) ** 2 + (y - 0) ** 2)
            dist_tr = np.sqrt((x - (size - 1)) ** 2 + (y - 0) ** 2)
            dist_bl = np.sqrt((x - 0) ** 2 + (y - (size - 1)) ** 2)
            dist_br = np.sqrt((x - (size - 1)) ** 2 + (y - (size - 1)) ** 2)
            
            if (dist_tl < corner_radius or dist_tr < corner_radius or 
                dist_bl < corner_radius or dist_br < corner_radius):
                img_array[y, x, 3] = 0  # Transparent
    
    return Image.fromarray(img_array, mode='RGBA')


def generate_food_icon(food_type, seed, size=64):
    """Generate food icon."""
    image = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    
    if food_type == 'apple':
        # Red apple
        apple_color = (200, 40, 40)
        
        # Apple body (circle)
        center = size // 2
        radius = size // 3
        draw.ellipse(
            [(center - radius, center - radius), (center + radius, center + radius)],
            fill=apple_color
        )
        
        # Highlight
        highlight_color = (255, 100, 100)
        draw.ellipse(
            [(center - radius // 2, center - radius // 2), 
             (center, center)],
            fill=highlight_color
        )
        
        # Stem (brown)
        stem_color = (100, 70, 40)
        draw.line([(center, center - radius), (center, center - radius - 6)], 
                 fill=stem_color, width=2)
        
    elif food_type == 'bread':
        # Brown bread
        bread_color = (180, 140, 80)
        
        # Bread loaf (rounded rectangle)
        margin = size // 4
        draw.rounded_rectangle(
            [(margin, margin), (size - margin, size - margin)],
            radius=8,
            fill=bread_color
        )
        
        # Crust lines
        crust_color = (140, 100, 50)
        for i in range(3):
            y = margin + (size - 2 * margin) * (i + 1) // 4
            draw.line([(margin + 4, y), (size - margin - 4, y)], 
                     fill=crust_color, width=1)
    
    # Add shading
    img_array = np.array(image)
    for y in range(size):
        for x in range(size):
            if img_array[y, x, 3] > 0:  # Non-transparent
                # Darken bottom
                if y > size // 2:
                    darken = 1.0 - (y - size // 2) / (size // 2) * 0.2
                    img_array[y, x, :3] = (img_array[y, x, :3] * darken).astype(np.uint8)
    
    return Image.fromarray(img_array, mode='RGBA')


def generate_all_icons(output_dir, seed):
    """Generate all item icons and return manifest entries."""
    output_dir = Path(output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    
    manifest_entries = []
    
    # Tools
    tools = [
        ('pickaxe', 'wood'),
        ('pickaxe', 'stone'),
        ('pickaxe', 'iron'),
        ('axe', 'wood'),
        ('axe', 'stone'),
        ('shovel', 'wood'),
        ('shovel', 'stone')
    ]
    
    for tool_type, material in tools:
        filename = f'{tool_type}_{material}.png'
        filepath = output_dir / filename
        print(f"Generating {filename}...")
        
        # Stable per-tool offset; avoid built-in hash for reproducibility.
        tool_key = f"{tool_type}_{material}"
        tool_offset = stable_int_seed(f"tool:{tool_key}") & 0x7fffffff
        image_seed = (seed + tool_offset) % (2**32)
        image = generate_tool_icon(tool_type, material, image_seed)
        
        metadata = {
            'generator': 'gen_icons.py',
            'seed': str(seed),
            'type': f'{tool_type}_{material}',
            'category': 'items'
        }
        save_with_metadata(image, filepath, metadata)
        
        manifest_entries.append({
            'name': f'{tool_type}_{material}',
            'category': 'items',
            'path': f'items/{filename}',
            'width': 64,
            'height': 64,
            'sha256': compute_sha256(filepath)
        })
    
    # Resources
    resources = ['wood_log', 'stone_chunk', 'ore_coal', 'ore_iron', 'ore_gold']
    
    for resource in resources:
        filename = f'{resource}.png'
        filepath = output_dir / filename
        print(f"Generating {filename}...")
        
        resource_offset = stable_int_seed(f"resource:{resource}") & 0x7fffffff
        image_seed = (seed + resource_offset) % (2**32)
        image = generate_resource_icon(resource, image_seed)
        
        metadata = {
            'generator': 'gen_icons.py',
            'seed': str(seed),
            'type': resource,
            'category': 'items'
        }
        save_with_metadata(image, filepath, metadata)
        
        manifest_entries.append({
            'name': resource,
            'category': 'items',
            'path': f'items/{filename}',
            'width': 64,
            'height': 64,
            'sha256': compute_sha256(filepath)
        })
    
    # Food
    foods = ['apple', 'bread']
    
    for food in foods:
        filename = f'{food}.png'
        filepath = output_dir / filename
        print(f"Generating {filename}...")
        
        food_offset = stable_int_seed(f"food:{food}") & 0x7fffffff
        image_seed = (seed + food_offset) % (2**32)
        image = generate_food_icon(food, image_seed)
        
        metadata = {
            'generator': 'gen_icons.py',
            'seed': str(seed),
            'type': food,
            'category': 'items'
        }
        save_with_metadata(image, filepath, metadata)
        
        manifest_entries.append({
            'name': food,
            'category': 'items',
            'path': f'items/{filename}',
            'width': 64,
            'height': 64,
            'sha256': compute_sha256(filepath)
        })
    
    return manifest_entries


def main():
    parser = argparse.ArgumentParser(description='Generate procedural item icons')
    parser.add_argument('--output', default='../assets/items', help='Output directory')
    parser.add_argument('--seed', type=int, default=42, help='Random seed')
    
    args = parser.parse_args()
    
    try:
        manifest_entries = generate_all_icons(args.output, args.seed)
        print(f"\nGenerated {len(manifest_entries)} item icons")
        return manifest_entries
    except Exception as e:
        print(f"ERROR: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()
