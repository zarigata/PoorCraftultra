import sys
import traceback

try:
    import generate_all
    sys.argv = ['generate_all.py', '--output-base', '../../assets', '--seed', '42']
    result = generate_all.main()
    print(f"Result: {result}")
except Exception as e:
    print(f"Error: {e}")
    traceback.print_exc()
