"""
Generate simple placeholder textures for biome blocks.
Creates 32x32 PNG images with appropriate colors for each biome block type.
"""
from PIL import Image, ImageDraw
import random
import os

# Ensure textures directory exists
os.makedirs('src/main/resources/textures', exist_ok=True)

def add_noise(base_color, variance=5):
    """Add slight random variation to a color."""
    return tuple(max(0, min(255, c + random.randint(-variance, variance))) for c in base_color)

def create_solid_texture(filename, base_color, variance=5):
    """Create a solid color texture with slight noise."""
    random.seed(42)
    img = Image.new('RGB', (32, 32))
    pixels = img.load()
    for x in range(32):
        for y in range(32):
            pixels[x, y] = add_noise(base_color, variance)
    img.save(filename)
    print(f"Created {filename}")

def create_layered_texture(filename, top_color, bottom_color):
    """Create a texture with horizontal layers."""
    random.seed(42)
    img = Image.new('RGB', (32, 32))
    pixels = img.load()
    for x in range(32):
        for y in range(32):
            # Create horizontal bands
            if y % 4 < 2:
                pixels[x, y] = add_noise(top_color, 3)
            else:
                pixels[x, y] = add_noise(bottom_color, 3)
    img.save(filename)
    print(f"Created {filename}")

def create_vertical_texture(filename, base_color):
    """Create a texture with vertical lines (for logs/cactus)."""
    random.seed(42)
    img = Image.new('RGB', (32, 32))
    pixels = img.load()
    for x in range(32):
        for y in range(32):
            # Create vertical lines
            if x % 4 < 3:
                pixels[x, y] = add_noise(base_color, 5)
            else:
                darker = tuple(max(0, c - 20) for c in base_color)
                pixels[x, y] = add_noise(darker, 5)
    img.save(filename)
    print(f"Created {filename}")

def create_radial_texture(filename, base_color):
    """Create a texture with radial pattern (for log tops)."""
    random.seed(42)
    img = Image.new('RGB', (32, 32))
    draw = ImageDraw.Draw(img)
    draw.rectangle([(0, 0), (32, 32)], fill=base_color)
    # Draw concentric circles
    for r in range(2, 16, 3):
        darker = tuple(max(0, c - 10) for c in base_color)
        draw.ellipse([(16-r, 16-r), (16+r, 16+r)], outline=darker)
    img.save(filename)
    print(f"Created {filename}")

def create_transparent_texture(filename, base_color, alpha=200):
    """Create a semi-transparent texture."""
    random.seed(42)
    img = Image.new('RGBA', (32, 32))
    pixels = img.load()
    for x in range(32):
        for y in range(32):
            color = add_noise(base_color, 5)
            pixels[x, y] = color + (alpha,)
    img.save(filename)
    print(f"Created {filename}")

# Generate all biome textures
print("Generating biome textures...")

# Snow block - white with slight variation
create_solid_texture('src/main/resources/textures/snow.png', (240, 248, 255), 5)

# Jungle grass top - vibrant green
create_solid_texture('src/main/resources/textures/jungle_grass_top.png', (80, 200, 80), 8)

# Jungle grass side - green on top, brown on bottom
img = Image.new('RGB', (32, 32))
pixels = img.load()
random.seed(42)
for x in range(32):
    for y in range(32):
        if y < 16:
            pixels[x, y] = add_noise((80, 200, 80), 5)
        else:
            pixels[x, y] = add_noise((139, 90, 60), 5)
img.save('src/main/resources/textures/jungle_grass_side.png')
print("Created src/main/resources/textures/jungle_grass_side.png")

# Sandstone - beige with horizontal layers
create_layered_texture('src/main/resources/textures/sandstone.png', (220, 190, 150), (210, 180, 140))

# Mountain stone - dark gray
create_solid_texture('src/main/resources/textures/mountain_stone.png', (100, 100, 110), 8)

# Desert sand - light sandy color
create_solid_texture('src/main/resources/textures/desert_sand.png', (245, 215, 190), 6)

# Ice - light blue, semi-transparent
create_transparent_texture('src/main/resources/textures/ice.png', (180, 220, 255), 200)

# Cactus side - green with vertical ridges
create_vertical_texture('src/main/resources/textures/cactus_side.png', (100, 140, 80))

# Cactus top - darker green with radial pattern
create_radial_texture('src/main/resources/textures/cactus_top.png', (80, 120, 70))

# Jungle log side - brown with vertical bark
create_vertical_texture('src/main/resources/textures/jungle_log_side.png', (120, 80, 50))

# Jungle log top - lighter brown with rings
create_radial_texture('src/main/resources/textures/jungle_log_top.png', (140, 100, 70))

print("\nAll textures generated successfully!")
