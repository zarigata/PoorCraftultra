print("Testing single texture generation...")

try:
    import gen_blocks
    from pathlib import Path
    
    output_dir = Path("../../assets/blocks")
    output_dir.mkdir(parents=True, exist_ok=True)
    
    print(f"Output directory: {output_dir.absolute()}")
    print("Generating single wood texture...")
    
    image = gen_blocks.generate_wood('oak', 42, size=64)
    
    print(f"Image generated: {image.size}")
    
    output_file = output_dir / "test_wood_oak.png"
    image.save(output_file, "PNG")
    
    print(f"Saved to: {output_file}")
    print("Success!")
    
except Exception as e:
    import traceback
    print(f"\nERROR: {e}")
    traceback.print_exc()
