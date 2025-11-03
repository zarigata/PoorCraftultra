"""Shared utilities for procedural asset generation."""

import hashlib
import json
import random
from pathlib import Path
from typing import Dict, List, Tuple

import numpy as np
from PIL import Image
from noise import pnoise2, snoise2


# Color palettes (RGB tuples)
PALETTE_STONE = [
    (80, 80, 80),
    (100, 100, 100),
    (120, 120, 120),
    (140, 140, 140),
    (160, 160, 160)
]

PALETTE_DIRT = [
    (101, 67, 33),
    (121, 85, 58),
    (139, 90, 43),
    (160, 108, 58)
]

PALETTE_GRASS = [
    (34, 139, 34),
    (50, 205, 50),
    (60, 179, 113),
    (85, 107, 47),
    (107, 142, 35)
]

PALETTE_WOOD = [
    (139, 90, 43),
    (160, 108, 58),
    (139, 69, 19),
    (101, 67, 33)
]

PALETTE_ORE = {
    'coal': (50, 50, 50),
    'iron': (216, 175, 147),
    'gold': (255, 215, 0),
    'diamond': (0, 191, 255),
    'redstone': (255, 0, 0),
    'emerald': (0, 201, 87)
}

PALETTE_BIOME_TINTS = {
    'plains': (1.0, 1.0, 1.0),
    'forest': (0.8, 1.0, 0.8),
    'desert': (1.0, 0.9, 0.7),
    'snow': (0.9, 0.9, 1.0)
}


def generate_perlin_2d(width: int, height: int, scale: float = 10.0, 
                       octaves: int = 6, persistence: float = 0.5, 
                       lacunarity: float = 2.0, seed: int = 0) -> np.ndarray:
    """Generate 2D Perlin noise with seamless tiling.
    
    Returns:
        numpy array of shape (height, width) with values in [0, 1]
    """
    noise_array = np.zeros((height, width))
    
    for y in range(height):
        for x in range(width):
            # Normalize coordinates for seamless tiling
            nx = x / width
            ny = y / height
            
            value = pnoise2(
                nx * scale,
                ny * scale,
                octaves=octaves,
                persistence=persistence,
                lacunarity=lacunarity,
                repeatx=width,
                repeaty=height,
                base=seed
            )
            
            # Normalize from [-1, 1] to [0, 1]
            noise_array[y, x] = (value + 1) / 2
    
    return noise_array


def generate_simplex_2d(width: int, height: int, scale: float = 10.0,
                        octaves: int = 6, persistence: float = 0.5,
                        lacunarity: float = 2.0, seed: int = 0) -> np.ndarray:
    """Generate 2D Simplex noise.
    
    Returns:
        numpy array of shape (height, width) with values in [0, 1]
    """
    noise_array = np.zeros((height, width))
    
    for y in range(height):
        for x in range(width):
            nx = x / width * scale
            ny = y / height * scale
            
            value = snoise2(nx, ny, octaves=octaves, persistence=persistence,
                           lacunarity=lacunarity, base=seed)
            
            noise_array[y, x] = (value + 1) / 2
    
    return noise_array


def generate_fbm(width: int, height: int, scale: float = 10.0,
                 octaves: int = 6, persistence: float = 0.5,
                 lacunarity: float = 2.0, seed: int = 0) -> np.ndarray:
    """Generate Fractal Brownian Motion for terrain-like textures.
    
    Returns:
        numpy array of shape (height, width) with values in [0, 1]
    """
    return generate_perlin_2d(width, height, scale, octaves, persistence, 
                             lacunarity, seed)


def apply_palette(noise_array: np.ndarray, palette: List[Tuple[int, int, int]]) -> np.ndarray:
    """Map noise values to color palette.
    
    Args:
        noise_array: 2D array with values in [0, 1]
        palette: List of RGB tuples
        
    Returns:
        RGB array of shape (height, width, 3)
    """
    height, width = noise_array.shape
    rgb_array = np.zeros((height, width, 3), dtype=np.uint8)
    
    for y in range(height):
        for x in range(width):
            value = noise_array[y, x]
            # Map to palette index
            idx = int(value * (len(palette) - 1))
            idx = min(idx, len(palette) - 1)
            rgb_array[y, x] = palette[idx]
    
    return rgb_array


def add_grain(image_array: np.ndarray, intensity: float = 0.1) -> np.ndarray:
    """Add subtle noise grain for texture detail.
    
    Args:
        image_array: RGB array
        intensity: Grain strength (0-1)
        
    Returns:
        Modified RGB array
    """
    height, width, _ = image_array.shape
    grain = np.random.randint(-int(255 * intensity), int(255 * intensity), 
                             (height, width, 3))
    result = np.clip(image_array.astype(np.int16) + grain, 0, 255)
    return result.astype(np.uint8)


def create_tile_border(width: int, height: int, color: Tuple[int, int, int], 
                       thickness: int = 1) -> np.ndarray:
    """Create border for debugging tile boundaries.
    
    Returns:
        RGB array with border
    """
    border = np.zeros((height, width, 3), dtype=np.uint8)
    border[:thickness, :] = color  # Top
    border[-thickness:, :] = color  # Bottom
    border[:, :thickness] = color  # Left
    border[:, -thickness:] = color  # Right
    return border


def ensure_seamless(image_array: np.ndarray) -> np.ndarray:
    """Blend edges to ensure perfect tiling.
    
    Args:
        image_array: RGB array
        
    Returns:
        Modified RGB array with seamless edges
    """
    # Simple edge blending - average opposite edges
    height, width, channels = image_array.shape
    blend_width = min(4, width // 8)
    
    result = image_array.copy()
    
    # Blend left-right
    for i in range(blend_width):
        alpha = i / blend_width
        result[:, i] = (alpha * result[:, i] + (1 - alpha) * result[:, -(blend_width - i)])
        result[:, -(i + 1)] = ((1 - alpha) * result[:, -(i + 1)] + alpha * result[:, blend_width - i - 1])
    
    # Blend top-bottom
    for i in range(blend_width):
        alpha = i / blend_width
        result[i, :] = (alpha * result[i, :] + (1 - alpha) * result[-(blend_width - i), :])
        result[-(i + 1), :] = ((1 - alpha) * result[-(i + 1), :] + alpha * result[blend_width - i - 1, :])
    
    return result.astype(np.uint8)


def validate_size(image: Image.Image, expected_width: int, expected_height: int):
    """Validate image dimensions.
    
    Raises:
        ValueError: If size doesn't match
    """
    if image.size != (expected_width, expected_height):
        raise ValueError(
            f"Image size mismatch: expected {expected_width}x{expected_height}, "
            f"got {image.size[0]}x{image.size[1]}"
        )


def compute_hash(image_path: Path) -> str:
    """Compute SHA-256 hash of file.
    
    Returns:
        Hex string of hash
    """
    sha256 = hashlib.sha256()
    with open(image_path, 'rb') as f:
        for chunk in iter(lambda: f.read(4096), b''):
            sha256.update(chunk)
    return sha256.hexdigest()


def write_manifest(output_dir: Path, entries: List[Dict]):
    """Write manifest.json with asset metadata.
    
    Args:
        output_dir: Directory to write manifest
        entries: List of dicts with keys: name, width, height, hash
    """
    manifest = {
        'generator': 'asset_generator',
        'version': '0.1.0',
        'textures': entries
    }
    
    manifest_path = output_dir / 'manifest.json'
    with open(manifest_path, 'w') as f:
        json.dump(manifest, f, indent=2)


def read_manifest(manifest_path: Path) -> Dict:
    """Parse manifest.json.
    
    Returns:
        Manifest dictionary
    """
    with open(manifest_path, 'r') as f:
        return json.load(f)


class SeededRandom:
    """Wrapper for seeded random generation."""
    
    def __init__(self, seed: int):
        self.rng = random.Random(seed)
        np.random.seed(seed)
    
    def randint(self, a: int, b: int) -> int:
        return self.rng.randint(a, b)
    
    def random(self) -> float:
        return self.rng.random()
    
    def choice(self, seq):
        return self.rng.choice(seq)
