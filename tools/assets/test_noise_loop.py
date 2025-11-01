import sys
import numpy as np
from noise import pnoise2

print("Testing noise generation loop...")

try:
    width, height = 64, 64
    seed = 42
    octaves = 6
    freq = 2.0
    
    # Ensure seed is within valid range
    seed = int(seed) % 2147483647
    
    print(f"Generating {width}x{height} noise array with seed={seed}", flush=True)
    
    noise_array = np.zeros((height, width))
    
    for y in range(height):
        if y % 16 == 0:
            print(f"  Row {y}/{height}...", flush=True)
        for x in range(width):
            nx = x / width
            ny = y / height
            
            value = pnoise2(
                nx * freq,
                ny * freq,
                octaves=octaves,
                persistence=0.5,
                lacunarity=2.0,
                repeatx=freq,
                repeaty=freq,
                base=seed
            )
            
            noise_array[y, x] = (value + 1) * 127.5
    
    print(f"Noise array generated: shape={noise_array.shape}, dtype={noise_array.dtype}", flush=True)
    print(f"Min={noise_array.min()}, Max={noise_array.max()}", flush=True)
    print("SUCCESS!", flush=True)
    
except Exception as e:
    import traceback
    print(f"\nERROR: {e}", flush=True, file=sys.stderr)
    traceback.print_exc(file=sys.stderr)
    sys.exit(1)
