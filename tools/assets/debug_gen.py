import sys
import traceback

def main():
    try:
        print("Step 1: Imports", flush=True)
        import gen_blocks
        from pathlib import Path
        
        print("Step 2: Create output dir", flush=True)
        output_dir = Path("../../assets/blocks")
        output_dir.mkdir(parents=True, exist_ok=True)
        
        print(f"Step 3: Output directory: {output_dir.absolute()}", flush=True)
        print("Step 4: Generating wood texture...", flush=True)
        
        image = gen_blocks.generate_wood('oak', 42, size=64)
        
        print(f"Step 5: Image generated: {image.size}", flush=True)
        
        output_file = output_dir / "test_wood_oak.png"
        image.save(output_file, "PNG")
        
        print(f"Step 6: Saved to: {output_file}", flush=True)
        print("SUCCESS!", flush=True)
        return 0
        
    except Exception as e:
        print(f"\nFATAL ERROR: {e}", flush=True, file=sys.stderr)
        traceback.print_exc(file=sys.stderr)
        return 1

if __name__ == "__main__":
    sys.exit(main())
