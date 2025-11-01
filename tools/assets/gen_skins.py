"""
Generate 256×256 procedural player/NPC skins.

Creates character skins following standard humanoid layout (4× Minecraft's 64×64 base).
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
    save_with_metadata
)


def generate_player_base(seed, size=256):
    """Generate base player skin with humanoid template."""
    image = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    
    # Skin tone palette
    skin_palette = [(220, 180, 140), (230, 190, 150), (240, 200, 160)]
    
    # Generate skin texture
    skin_noise = create_seamless_noise(size, size, seed, octaves=4, freq=2.0)
    skin_texture = apply_palette(skin_noise, skin_palette)
    skin_texture = add_grain(skin_texture, intensity=0.05, seed=seed)
    skin_array = np.array(skin_texture.convert('RGBA'))
    
    # Head (top section, 64×64 scaled to 128×128 at 4× resolution)
    # Standard layout: head at top-left
    head_size = 128
    head_x, head_y = 0, 0
    
    # Fill head region with skin texture
    for y in range(head_y, head_y + head_size):
        for x in range(head_x, head_x + head_size):
            if y < size and x < size:
                image.putpixel((x, y), tuple(skin_array[y % size, x % size]))
    
    # Body (center section, 64×128 scaled to 128×256 at 4× resolution)
    body_x, body_y = 64, 128
    body_width, body_height = 128, 192
    
    # Shirt color (simple blue)
    shirt_color = (60, 100, 180)
    for y in range(body_y, min(body_y + body_height, size)):
        for x in range(body_x, min(body_x + body_width, size)):
            # Add noise to shirt
            noise_val = skin_noise[y % size, x % size]
            color_variation = int((noise_val - 128) * 0.1)
            color = tuple(np.clip(np.array(shirt_color) + color_variation, 0, 255))
            image.putpixel((x, y), color + (255,))
    
    # Arms (32×128 each, scaled to 64×256 at 4× resolution)
    # Left arm
    left_arm_x, left_arm_y = 192, 128
    arm_width, arm_height = 64, 192
    
    for y in range(left_arm_y, min(left_arm_y + arm_height, size)):
        for x in range(left_arm_x, min(left_arm_x + arm_width, size)):
            image.putpixel((x, y), tuple(skin_array[y % size, x % size]))
    
    # Right arm (mirrored)
    right_arm_x = 0
    for y in range(left_arm_y, min(left_arm_y + arm_height, size)):
        for x in range(right_arm_x, min(right_arm_x + arm_width, size)):
            image.putpixel((x, y), tuple(skin_array[y % size, x % size]))
    
    # Legs (32×128 each, scaled to 64×256 at 4× resolution)
    # Pants color (brown)
    pants_color = (80, 60, 40)
    
    # Left leg
    left_leg_x, left_leg_y = 0, 64
    leg_width, leg_height = 64, 192
    
    for y in range(left_leg_y, min(left_leg_y + leg_height, size)):
        for x in range(left_leg_x, min(left_leg_x + leg_width, size)):
            noise_val = skin_noise[y % size, x % size]
            color_variation = int((noise_val - 128) * 0.1)
            color = tuple(np.clip(np.array(pants_color) + color_variation, 0, 255))
            image.putpixel((x, y), color + (255,))
    
    # Right leg
    right_leg_x = 64
    for y in range(left_leg_y, min(left_leg_y + leg_height, size)):
        for x in range(right_leg_x, min(right_leg_x + leg_width, size)):
            noise_val = skin_noise[y % size, x % size]
            color_variation = int((noise_val - 128) * 0.1)
            color = tuple(np.clip(np.array(pants_color) + color_variation, 0, 255))
            image.putpixel((x, y), color + (255,))
    
    return image


def generate_npc_villager(seed, size=256):
    """Generate villager NPC skin (brown robe)."""
    image = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    
    # Skin tone
    skin_palette = [(200, 160, 120), (210, 170, 130), (220, 180, 140)]
    skin_noise = create_seamless_noise(size, size, seed, octaves=4, freq=2.0)
    skin_texture = apply_palette(skin_noise, skin_palette)
    skin_array = np.array(skin_texture.convert('RGBA'))
    
    # Robe color (brown)
    robe_palette = [(100, 70, 40), (120, 85, 50), (140, 100, 60)]
    robe_noise = create_seamless_noise(size, size, seed + 1, octaves=4, freq=3.0)
    robe_texture = apply_palette(robe_noise, robe_palette)
    robe_array = np.array(robe_texture.convert('RGBA'))
    
    # Head (skin)
    head_size = 128
    for y in range(0, head_size):
        for x in range(0, head_size):
            if y < size and x < size:
                image.putpixel((x, y), tuple(skin_array[y, x]))
    
    # Body (robe)
    for y in range(128, size):
        for x in range(0, size):
            image.putpixel((x, y), tuple(robe_array[y % size, x % size]))
    
    return image


def generate_npc_guard(seed, size=256):
    """Generate guard NPC skin (armor texture)."""
    image = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    
    # Metallic gray armor
    armor_palette = [(120, 120, 130), (140, 140, 150), (160, 160, 170)]
    armor_noise = create_seamless_noise(size, size, seed, octaves=6, freq=4.0)
    armor_texture = apply_palette(armor_noise, armor_palette)
    armor_texture = add_grain(armor_texture, intensity=0.08, seed=seed)
    armor_array = np.array(armor_texture.convert('RGBA'))
    
    # Skin for head
    skin_palette = [(210, 170, 130), (220, 180, 140), (230, 190, 150)]
    skin_noise = create_seamless_noise(128, 128, seed + 1, octaves=4, freq=2.0)
    skin_texture = apply_palette(skin_noise, skin_palette)
    skin_array = np.array(skin_texture.convert('RGBA'))
    
    # Head (skin)
    head_size = 128
    for y in range(0, head_size):
        for x in range(0, head_size):
            if y < size and x < size:
                image.putpixel((x, y), tuple(skin_array[y % 128, x % 128]))
    
    # Body and limbs (armor)
    for y in range(128, size):
        for x in range(0, size):
            image.putpixel((x, y), tuple(armor_array[y % size, x % size]))
    
    return image


def generate_npc_merchant(seed, size=256):
    """Generate merchant NPC skin (colorful tunic)."""
    image = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    
    # Skin tone
    skin_palette = [(200, 160, 120), (210, 170, 130), (220, 180, 140)]
    skin_noise = create_seamless_noise(size, size, seed, octaves=4, freq=2.0)
    skin_texture = apply_palette(skin_noise, skin_palette)
    skin_array = np.array(skin_texture.convert('RGBA'))
    
    # Colorful tunic (red/orange)
    tunic_palette = [(180, 60, 40), (200, 80, 60), (220, 100, 80)]
    tunic_noise = create_seamless_noise(size, size, seed + 2, octaves=4, freq=3.0)
    tunic_texture = apply_palette(tunic_noise, tunic_palette)
    tunic_array = np.array(tunic_texture.convert('RGBA'))
    
    # Head (skin)
    head_size = 128
    for y in range(0, head_size):
        for x in range(0, head_size):
            if y < size and x < size:
                image.putpixel((x, y), tuple(skin_array[y, x]))
    
    # Body (tunic)
    for y in range(128, size):
        for x in range(0, size):
            image.putpixel((x, y), tuple(tunic_array[y % size, x % size]))
    
    return image


def validate_skin_layout(image):
    """
    Validate skin follows standard layout.
    
    Checks that non-transparent regions match expected positions:
    - Head at top
    - Body/limbs below
    """
    width, height = image.size
    img_array = np.array(image)
    
    # Check head region (top 128 pixels) has content
    head_region = img_array[:128, :128, 3]  # Alpha channel
    head_has_content = np.any(head_region > 0)
    
    if not head_has_content:
        print("WARNING: Head region appears empty")
        return False
    
    # Check body region has content
    body_region = img_array[128:, :, 3]
    body_has_content = np.any(body_region > 0)
    
    if not body_has_content:
        print("WARNING: Body region appears empty")
        return False
    
    return True


def generate_all_skins(output_dir, seed):
    """Generate all skin textures and return manifest entries."""
    output_dir = Path(output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    
    manifest_entries = []
    
    skins = [
        ('player_base', generate_player_base),
        ('npc_villager', generate_npc_villager),
        ('npc_guard', generate_npc_guard),
        ('npc_merchant', generate_npc_merchant)
    ]
    
    for name, generator_func in skins:
        filename = f'{name}.png'
        filepath = output_dir / filename
        print(f"Generating {filename}...")
        
        image = generator_func(seed)
        
        # Validate layout
        if not validate_skin_layout(image):
            print(f"  Layout validation warning for {filename}")
        
        metadata = {
            'generator': 'gen_skins.py',
            'seed': str(seed),
            'type': name,
            'category': 'skins'
        }
        save_with_metadata(image, filepath, metadata)
        
        manifest_entries.append({
            'name': name,
            'category': 'skins',
            'path': f'skins/{filename}',
            'width': 256,
            'height': 256,
            'sha256': compute_sha256(filepath)
        })
    
    return manifest_entries


def main():
    parser = argparse.ArgumentParser(description='Generate procedural player/NPC skins')
    parser.add_argument('--output', default='../assets/skins', help='Output directory')
    parser.add_argument('--seed', type=int, default=42, help='Random seed')
    
    args = parser.parse_args()
    
    try:
        manifest_entries = generate_all_skins(args.output, args.seed)
        print(f"\nGenerated {len(manifest_entries)} skins")
        return manifest_entries
    except Exception as e:
        print(f"ERROR: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()
