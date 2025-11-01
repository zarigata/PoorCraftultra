import sys
from noise import pnoise2

print("Testing pnoise2 with different parameters...")

try:
    print("Test 1: Basic call", flush=True)
    value = pnoise2(0.5, 0.5)
    print(f"  Result: {value}", flush=True)
    
    print("Test 2: With octaves", flush=True)
    value = pnoise2(0.5, 0.5, octaves=4)
    print(f"  Result: {value}", flush=True)
    
    print("Test 3: With base seed", flush=True)
    value = pnoise2(0.5, 0.5, octaves=4, base=42)
    print(f"  Result: {value}", flush=True)
    
    print("Test 4: With repeat parameters (int)", flush=True)
    value = pnoise2(0.5, 0.5, octaves=4, base=42, repeatx=4, repeaty=4)
    print(f"  Result: {value}", flush=True)
    
    print("Test 5: With repeat parameters (float)", flush=True)
    value = pnoise2(0.5, 0.5, octaves=4, base=42, repeatx=4.0, repeaty=4.0)
    print(f"  Result: {value}", flush=True)
    
    print("\nAll tests passed!", flush=True)
    
except Exception as e:
    import traceback
    print(f"\nERROR: {e}", flush=True, file=sys.stderr)
    traceback.print_exc(file=sys.stderr)
    sys.exit(1)
