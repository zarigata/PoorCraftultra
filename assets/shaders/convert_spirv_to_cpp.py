#!/usr/bin/env python3
"""
Convert SPIR-V binary files to C++ uint32_t arrays for embedding in source code.
"""

import sys
import struct

def spirv_to_cpp_array(spirv_file, array_name):
    """Read SPIR-V file and output as C++ array."""
    with open(spirv_file, 'rb') as f:
        data = f.read()
    
    # SPIR-V is uint32_t array
    if len(data) % 4 != 0:
        print(f"Error: {spirv_file} size is not a multiple of 4", file=sys.stderr)
        return None
    
    uint32_count = len(data) // 4
    values = struct.unpack(f'<{uint32_count}I', data)
    
    # Format as C++ array
    lines = [f"static constexpr uint32_t {array_name}[] = {{"]
    
    # Format 8 values per line
    for i in range(0, len(values), 8):
        chunk = values[i:i+8]
        hex_values = ','.join(f'0x{v:08x}' for v in chunk)
        if i + 8 < len(values):
            lines.append(f"    {hex_values},")
        else:
            lines.append(f"    {hex_values}}};")
    
    return '\n'.join(lines)

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("Usage: convert_spirv_to_cpp.py <vertex.spv> <fragment.spv>")
        sys.exit(1)
    
    vertex_file = sys.argv[1]
    fragment_file = sys.argv[2] if len(sys.argv) > 2 else None
    
    print("// Generated SPIR-V shader code")
    print()
    
    vertex_array = spirv_to_cpp_array(vertex_file, "vertexShaderCode")
    if vertex_array:
        print(vertex_array)
        print()
    
    if fragment_file:
        fragment_array = spirv_to_cpp_array(fragment_file, "fragmentShaderCode")
        if fragment_array:
            print(fragment_array)
