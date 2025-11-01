print("Testing imports...")

try:
    print("1. Importing PIL...")
    from PIL import Image, ImageDraw, PngImagePlugin
    print("   PIL imported OK")
    
    print("2. Importing numpy...")
    import numpy as np
    print("   numpy imported OK")
    
    print("3. Importing noise...")
    from noise import pnoise2
    print("   noise imported OK")
    
    print("4. Importing common...")
    import common
    print("   common imported OK")
    
    print("5. Importing gen_blocks...")
    import gen_blocks
    print("   gen_blocks imported OK")
    
    print("\nAll imports successful!")
    
except Exception as e:
    import traceback
    print(f"\nERROR: {e}")
    traceback.print_exc()
