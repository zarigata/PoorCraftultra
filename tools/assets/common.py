"""
Common utility module for procedural texture generation.

Provides reusable functions for creating seamless, deterministic textures
using Perlin noise, color palettes, and image processing.
"""

import hashlib
import numpy as np
from PIL import Image, PngImagePlugin


def stable_int_seed(name: str) -> int:
    """Convert a stable identifier into a deterministic 32-bit seed."""
    # Python's built-in hash is salted per process; assets must remain reproducible.
    digest = hashlib.sha256(name.encode('utf-8')).digest()
    return int.from_bytes(digest[:4], 'big', signed=False)


def seed_to_rng(seed: int) -> np.random.Generator:
    """Create an isolated RNG seeded deterministically within uint32 range."""
    return np.random.default_rng(seed % (2**32))


def create_seamless_noise(width, height, seed, octaves=4, freq=4.0):
    """
    Generate seamless Perlin noise texture.
    
    Args:
        width: Image width in pixels
        height: Image height in pixels
        seed: Random seed for deterministic generation
        octaves: Number of noise octaves (detail levels)
        freq: Base frequency (higher = more detail)
    
    Returns:
        NumPy array (height, width) with values normalized to 0-255
    
    Example:
        >>> noise = create_seamless_noise(64, 64, seed=42, octaves=4, freq=4.0)
        >>> print(noise.shape)
        (64, 64)
    """
    # Generate deterministic base noise
    rng = seed_to_rng(seed)
    base = rng.random((height, width), dtype=np.float32)

    # Encourage seamless edges by averaging opposing sides
    base[:, 0] = base[:, -1]
    base[0, :] = base[-1, :]

    # Multi-octave smoothing to mimic Perlin-style structure
    noise_array = np.zeros_like(base)
    amplitude = 1.0
    total_amplitude = 0.0

    for octave in range(max(1, octaves)):
        kernel_size = max(1, int(freq * (octave + 1)))
        smoothed = _box_blur(base, kernel_size)
        noise_array += smoothed * amplitude
        total_amplitude += amplitude
        amplitude *= 0.5

    noise_array /= max(total_amplitude, 1e-6)

    # Normalize to [0, 255]
    noise_array -= noise_array.min()
    max_val = noise_array.max()
    if max_val > 0:
        noise_array /= max_val
    noise_array = (noise_array * 255.0).astype(np.uint8)

    return noise_array


def _box_blur(data: np.ndarray, radius: int) -> np.ndarray:
    if radius <= 1:
        return data

    blurred = data.copy()
    for _ in range(radius):
        blurred = (
            blurred +
            np.roll(blurred, 1, axis=0) +
            np.roll(blurred, -1, axis=0) +
            np.roll(blurred, 1, axis=1) +
            np.roll(blurred, -1, axis=1)
        ) / 5.0
    return blurred


def apply_palette(noise_array, palette):
    """
    Map grayscale noise to RGB color palette.
    
    Args:
        noise_array: 2D NumPy array with values 0-255
        palette: List of (r, g, b) tuples defining color stops
                 Colors are interpolated linearly across the value range
    
    Returns:
        PIL Image in RGB mode
    
    Example:
        >>> noise = create_seamless_noise(64, 64, seed=42)
        >>> brown_palette = [(101, 67, 33), (139, 90, 43), (160, 110, 60)]
        >>> image = apply_palette(noise, brown_palette)
    """
    height, width = noise_array.shape
    rgb_array = np.zeros((height, width, 3), dtype=np.uint8)
    
    num_stops = len(palette)
    
    for y in range(height):
        for x in range(width):
            value = noise_array[y, x] / 255.0  # Normalize to 0-1
            
            # Find palette stops to interpolate between
            stop_index = value * (num_stops - 1)
            lower_idx = int(np.floor(stop_index))
            upper_idx = min(lower_idx + 1, num_stops - 1)
            
            # Interpolation factor
            t = stop_index - lower_idx
            
            # Linear interpolation between color stops
            lower_color = np.array(palette[lower_idx])
            upper_color = np.array(palette[upper_idx])
            color = lower_color * (1 - t) + upper_color * t
            
            rgb_array[y, x] = color.astype(np.uint8)
    
    return Image.fromarray(rgb_array, mode='RGB')


def add_grain(image, intensity=0.1, seed=42):
    """
    Add subtle grain/noise overlay for texture variation.
    
    Args:
        image: PIL Image to modify (modified in-place)
        intensity: Grain strength (0.0 = none, 1.0 = maximum)
        seed: Random seed for deterministic grain
    
    Returns:
        Modified PIL Image
    
    Example:
        >>> image = Image.new('RGB', (64, 64), color=(100, 100, 100))
        >>> image = add_grain(image, intensity=0.15, seed=42)
    """
    rng = seed_to_rng(int(seed))
    width, height = image.size

    # Generate random grain
    max_variation = int(intensity * 255)
    grain = rng.integers(-max_variation, max_variation + 1,
                         size=(height, width, 3), dtype=np.int16)
    
    # Apply grain to image
    img_array = np.array(image, dtype=np.int16)
    img_array = np.clip(img_array + grain, 0, 255).astype(np.uint8)
    
    return Image.fromarray(img_array, mode=image.mode)


def ensure_tiling(image):
    """
    Verify seamless tiling by comparing edge pixels.
    
    Args:
        image: PIL Image to check
    
    Returns:
        bool: True if edges match (seamless), False otherwise
    
    Logs warning if mismatch detected.
    
    Example:
        >>> image = Image.new('RGB', (64, 64))
        >>> is_seamless = ensure_tiling(image)
    """
    width, height = image.size
    img_array = np.array(image)
    
    # Check top/bottom edges
    top_edge = img_array[0, :]
    bottom_edge = img_array[-1, :]
    top_bottom_match = np.allclose(top_edge, bottom_edge, atol=5)
    
    # Check left/right edges
    left_edge = img_array[:, 0]
    right_edge = img_array[:, -1]
    left_right_match = np.allclose(left_edge, right_edge, atol=5)
    
    if not (top_bottom_match and left_right_match):
        print(f"WARNING: Tiling mismatch detected (top/bottom: {top_bottom_match}, "
              f"left/right: {left_right_match})")
        return False
    
    return True


def compute_sha256(filepath):
    """
    Compute SHA-256 hash of file.
    
    Args:
        filepath: Path to file (string or Path object)
    
    Returns:
        Hex string of SHA-256 hash
    
    Example:
        >>> hash_value = compute_sha256("assets/blocks/wood_oak.png")
        >>> print(len(hash_value))
        64
    """
    sha256_hash = hashlib.sha256()
    
    with open(filepath, "rb") as f:
        # Read in chunks for memory efficiency
        for byte_block in iter(lambda: f.read(4096), b""):
            sha256_hash.update(byte_block)
    
    return sha256_hash.hexdigest()


def save_with_metadata(image, filepath, metadata):
    """
    Save PNG with embedded metadata (tEXt chunks) for debugging.
    
    Args:
        image: PIL Image to save
        filepath: Output path (string or Path object)
        metadata: Dict of key-value pairs to embed
    
    Example:
        >>> image = Image.new('RGB', (64, 64))
        >>> metadata = {"generator": "gen_blocks.py", "seed": "42", "type": "wood_oak"}
        >>> save_with_metadata(image, "wood_oak.png", metadata)
    """
    pnginfo = PngImagePlugin.PngInfo()
    
    for key, value in metadata.items():
        pnginfo.add_text(key, str(value))
    
    image.save(filepath, "PNG", pnginfo=pnginfo)
