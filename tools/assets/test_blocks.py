print("Testing block generation...")

try:
    import gen_blocks
    from pathlib import Path
    
    output_dir = Path("../../assets/blocks")
    output_dir.mkdir(parents=True, exist_ok=True)
    
    print(f"Output directory: {output_dir.absolute()}")
    print("Generating blocks...")
    
    entries = gen_blocks.generate_all_blocks(str(output_dir), 42)
    
    print(f"Success! Generated {len(entries)} blocks")
    for entry in entries[:3]:
        print(f"  - {entry['name']}: {entry['width']}x{entry['height']}")
    
except Exception as e:
    import traceback
    print(f"ERROR: {e}")
    traceback.print_exc()
