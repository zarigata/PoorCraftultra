@echo off
setlocal enabledelayedexpansion

echo === Poorcraft Ultra Asset Generator ===
echo.

REM Check Python version
python --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Python not found. Please install Python 3.11 or higher.
    exit /b 1
)

for /f "tokens=2" %%i in ('python --version 2^>^&1') do set PYTHON_VERSION=%%i
echo Using Python %PYTHON_VERSION%

REM Create virtual environment if not exists
if not exist ".venv" (
    echo Creating virtual environment...
    python -m venv .venv
    if errorlevel 1 (
        echo ERROR: Failed to create virtual environment
        exit /b 1
    )
)

REM Activate virtual environment
echo Activating virtual environment...
call .venv\Scripts\activate.bat
if errorlevel 1 (
    echo ERROR: Failed to activate virtual environment
    exit /b 1
)

REM Install dependencies
echo Installing dependencies...
pip install -q --upgrade pip
pip install -q Pillow numpy noise
if errorlevel 1 (
    echo ERROR: Failed to install dependencies
    exit /b 1
)

REM Create output directories
echo Creating output directories...
if not exist "assets\blocks" mkdir assets\blocks
if not exist "assets\skins" mkdir assets\skins
if not exist "assets\items" mkdir assets\items
if not exist "assets\sfx" mkdir assets\sfx

REM Run generators
echo.
echo Generating block textures...
python tools\assets\gen_blocks.py --output-dir assets\blocks --seed 42
if errorlevel 1 (
    echo ERROR: Block generation failed
    exit /b 1
)

echo.
echo Generating skin textures...
python tools\assets\gen_skins.py --output-dir assets\skins --seed 42
if errorlevel 1 (
    echo ERROR: Skin generation failed
    exit /b 1
)

echo.
echo Generating item icons...
python tools\assets\gen_items.py --output-dir assets\items --seed 42
if errorlevel 1 (
    echo ERROR: Item generation failed
    exit /b 1
)

REM Count generated files
set BLOCK_COUNT=0
set SKIN_COUNT=0
set ITEM_COUNT=0

for %%f in (assets\blocks\*.png) do set /a BLOCK_COUNT+=1
for %%f in (assets\skins\*.png) do set /a SKIN_COUNT+=1
for %%f in (assets\items\*.png) do set /a ITEM_COUNT+=1

echo.
echo === Generation Complete ===
echo Generated %BLOCK_COUNT% block textures
echo Generated %SKIN_COUNT% skin textures
echo Generated %ITEM_COUNT% item icons
echo.

REM Deactivate virtual environment
call deactivate

endlocal
