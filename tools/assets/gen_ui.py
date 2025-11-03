#!/usr/bin/env python3
"""
Generate UI textures for PoorCraft Ultra.
Creates slot backgrounds, inventory panels, and other UI elements.
"""

import argparse
from pathlib import Path
from PIL import Image, ImageDraw

def create_slot(size=40):
    """Create a slot background texture."""
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Dark background
    draw.rectangle([0, 0, size-1, size-1], fill=(139, 139, 139, 180))
    
    # Border
    draw.rectangle([0, 0, size-1, size-1], outline=(55, 55, 55, 255), width=2)
    
    # Inner highlight
    draw.line([2, 2, size-3, 2], fill=(198, 198, 198, 255), width=1)
    draw.line([2, 2, 2, size-3], fill=(198, 198, 198, 255), width=1)
    
    return img

def create_inventory_bg(width=440, height=344):
    """Create inventory panel background."""
    img = Image.new('RGBA', (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Main background
    draw.rectangle([0, 0, width-1, height-1], fill=(198, 198, 198, 255))
    
    # Border
    draw.rectangle([0, 0, width-1, height-1], outline=(55, 55, 55, 255), width=3)
    
    # Inner shadow
    draw.rectangle([3, 3, width-4, height-4], outline=(139, 139, 139, 255), width=1)
    
    return img

def create_furnace_bg(width=300, height=250):
    """Create furnace panel background."""
    img = Image.new('RGBA', (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Main background
    draw.rectangle([0, 0, width-1, height-1], fill=(198, 198, 198, 255))
    
    # Border
    draw.rectangle([0, 0, width-1, height-1], outline=(55, 55, 55, 255), width=3)
    
    # Inner shadow
    draw.rectangle([3, 3, width-4, height-4], outline=(139, 139, 139, 255), width=1)
    
    return img

def create_fire(width=14, height=14):
    """Create fire icon for furnace."""
    img = Image.new('RGBA', (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Fire shape (simplified)
    draw.polygon([(width//2, 0), (width-1, height-1), (0, height-1)], 
                 fill=(255, 128, 0, 255))
    draw.polygon([(width//2, 2), (width-3, height-1), (2, height-1)], 
                 fill=(255, 200, 0, 255))
    
    return img

def create_arrow(width=24, height=16):
    """Create progress arrow for furnace."""
    img = Image.new('RGBA', (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Arrow shape
    y_mid = height // 2
    draw.polygon([
        (0, y_mid - 3), (width - 6, y_mid - 3),
        (width - 6, 0), (width - 1, y_mid),
        (width - 6, height - 1), (width - 6, y_mid + 3),
        (0, y_mid + 3)
    ], fill=(128, 128, 128, 255), outline=(55, 55, 55, 255))
    
    return img

def main():
    parser = argparse.ArgumentParser(description='Generate UI textures')
    parser.add_argument('--output-dir', type=str, default='assets/ui',
                       help='Output directory for UI textures')
    args = parser.parse_args()
    
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    
    print("Generating UI textures...")
    
    # Generate textures
    textures = {
        'slot.png': create_slot(),
        'inventory_bg.png': create_inventory_bg(),
        'furnace_bg.png': create_furnace_bg(),
        'fire.png': create_fire(),
        'arrow.png': create_arrow(),
    }
    
    for filename, img in textures.items():
        filepath = output_dir / filename
        img.save(filepath)
        print(f"  Created {filepath}")
    
    print(f"\nGenerated {len(textures)} UI textures in {output_dir}")

if __name__ == '__main__':
    main()
