@echo off
REM Compile Vulkan shaders to SPIR-V
REM Requires Vulkan SDK to be installed

echo Compiling chunk vertex shader...
glslangValidator -V -o chunk.vert.spv chunk.vert
if %errorlevel% neq 0 (
    echo Failed to compile vertex shader
    exit /b 1
)

echo Compiling chunk fragment shader...
glslangValidator -V -o chunk.frag.spv chunk.frag
if %errorlevel% neq 0 (
    echo Failed to compile fragment shader
    exit /b 1
)

echo.
echo Shaders compiled successfully!
echo.
echo To embed in C++, use the following Python script or manually convert:
echo python convert_spirv_to_cpp.py chunk.vert.spv chunk.frag.spv
