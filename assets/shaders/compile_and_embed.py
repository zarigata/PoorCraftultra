#!/usr/bin/env python3
"""
Compile GLSL shaders to SPIR-V and generate C++ embedded arrays.
Attempts multiple compilation methods.
"""

import os
import sys
import struct
import subprocess
from pathlib import Path

def find_shader_compiler():
    """Try to find a GLSL to SPIR-V compiler."""
    compilers = [
        ('glslangValidator', ['-V']),
        ('glslc', ['-fshader-stage=vertex']),  # Will be adjusted per shader
    ]
    
    for compiler, _ in compilers:
        try:
            result = subprocess.run([compiler, '--version'], 
                                  capture_output=True, 
                                  timeout=5)
            if result.returncode == 0:
                return compiler
        except (FileNotFoundError, subprocess.TimeoutExpired):
            continue
    
    return None

def compile_shader_glslang(input_file, output_file):
    """Compile using glslangValidator."""
    cmd = ['glslangValidator', '-V', '-o', output_file, input_file]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"Error compiling {input_file}:", file=sys.stderr)
        print(result.stderr, file=sys.stderr)
        return False
    return True

def compile_shader_glslc(input_file, output_file, stage):
    """Compile using glslc."""
    cmd = ['glslc', f'-fshader-stage={stage}', '-o', output_file, input_file]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"Error compiling {input_file}:", file=sys.stderr)
        print(result.stderr, file=sys.stderr)
        return False
    return True

def spirv_to_cpp_array(spirv_file, array_name):
    """Read SPIR-V file and output as C++ array."""
    with open(spirv_file, 'rb') as f:
        data = f.read()
    
    if len(data) % 4 != 0:
        print(f"Error: {spirv_file} size is not a multiple of 4", file=sys.stderr)
        return None
    
    uint32_count = len(data) // 4
    values = struct.unpack(f'<{uint32_count}I', data)
    
    lines = [f"static constexpr uint32_t {array_name}[] = {{"]
    
    for i in range(0, len(values), 8):
        chunk = values[i:i+8]
        hex_values = ','.join(f'0x{v:08x}' for v in chunk)
        if i + 8 < len(values):
            lines.append(f"    {hex_values},")
        else:
            lines.append(f"    {hex_values}}};")
    
    return '\n'.join(lines)

def main():
    script_dir = Path(__file__).parent
    
    # Find compiler
    compiler = find_shader_compiler()
    if not compiler:
        print("Error: No GLSL to SPIR-V compiler found!", file=sys.stderr)
        print("Please install Vulkan SDK with glslangValidator or glslc", file=sys.stderr)
        return 1
    
    print(f"Using compiler: {compiler}")
    
    # Compile shaders
    shaders = [
        ('chunk.vert', 'chunk.vert.spv', 'vertex'),
        ('chunk.frag', 'chunk.frag.spv', 'fragment'),
    ]
    
    for src, dst, stage in shaders:
        src_path = script_dir / src
        dst_path = script_dir / dst
        
        print(f"Compiling {src}...")
        
        if compiler == 'glslangValidator':
            success = compile_shader_glslang(str(src_path), str(dst_path))
        else:
            success = compile_shader_glslc(str(src_path), str(dst_path), stage)
        
        if not success:
            return 1
    
    # Generate C++ arrays
    print("\n// Generated SPIR-V shader code for VulkanRenderer.cpp")
    print("// Replace the vertexShaderCode and fragmentShaderCode arrays with these:\n")
    
    vert_array = spirv_to_cpp_array(script_dir / 'chunk.vert.spv', 'vertexShaderCode')
    if vert_array:
        print(vert_array)
        print()
    
    frag_array = spirv_to_cpp_array(script_dir / 'chunk.frag.spv', 'fragmentShaderCode')
    if frag_array:
        print(frag_array)
    
    return 0

if __name__ == '__main__':
    sys.exit(main())
