print("Starting test...")

try:
    print("Importing common...")
    import common
    print("Common imported successfully")
    
    print("Creating noise...")
    noise = common.create_seamless_noise(64, 64, 42, octaves=4, freq=4.0)
    print(f"Noise created: shape={noise.shape}")
    
    print("Test completed successfully!")
except Exception as e:
    import traceback
    print(f"ERROR: {e}")
    traceback.print_exc()
