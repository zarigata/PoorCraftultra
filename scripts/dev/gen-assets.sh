#!/bin/bash
set -e

echo "=== Poorcraft Ultra Asset Generator ==="
echo ""

# Check Python version
if ! command -v python3 &> /dev/null; then
    echo "ERROR: Python 3 not found. Please install Python 3.11 or higher."
    exit 1
fi

PYTHON_VERSION=$(python3 --version 2>&1 | awk '{print $2}')
echo "Using Python $PYTHON_VERSION"

# Create virtual environment if not exists
if [ ! -d ".venv" ]; then
    echo "Creating virtual environment..."
    python3 -m venv .venv
fi

# Activate virtual environment
echo "Activating virtual environment..."
source .venv/bin/activate

# Install dependencies
echo "Installing dependencies..."
pip install -q --upgrade pip
pip install -q Pillow numpy noise

# Create output directories
echo "Creating output directories..."
mkdir -p assets/blocks
mkdir -p assets/skins
mkdir -p assets/items
mkdir -p assets/sfx

# Run generators
echo ""
echo "Generating block textures..."
python3 tools/assets/gen_blocks.py --output-dir assets/blocks --seed 42

echo ""
echo "Generating skin textures..."
python3 tools/assets/gen_skins.py --output-dir assets/skins --seed 42

echo ""
echo "Generating item icons..."
python3 tools/assets/gen_items.py --output-dir assets/items --seed 42

# Count generated files
BLOCK_COUNT=$(find assets/blocks -name "*.png" | wc -l)
SKIN_COUNT=$(find assets/skins -name "*.png" | wc -l)
ITEM_COUNT=$(find assets/items -name "*.png" | wc -l)

echo ""
echo "=== Generation Complete ==="
echo "Generated $BLOCK_COUNT block textures"
echo "Generated $SKIN_COUNT skin textures"
echo "Generated $ITEM_COUNT item icons"
echo ""

# Deactivate virtual environment
deactivate
