#!/bin/bash
set -e

echo "=== Poorcraft Ultra Asset Generator ==="
echo ""

# Detect Python command
PYTHON_CMD=""
if command -v python3 &> /dev/null; then
    PYTHON_CMD="python3"
elif command -v python &> /dev/null; then
    PYTHON_CMD="python"
elif command -v py &> /dev/null; then
    PYTHON_CMD="py"
else
    echo "ERROR: Python not found. Please install Python 3.9 or higher."
    exit 1
fi

# Default arguments
USER_OUTPUT_BASE=""
USER_SEED=""

# Parse optional CLI overrides
while [ "$#" -gt 0 ]; do
    case "$1" in
        --output-base)
            USER_OUTPUT_BASE="$2"
            shift 2
            ;;
        --seed)
            USER_SEED="$2"
            shift 2
            ;;
        *)
            echo "WARNING: Unknown argument '$1' ignored"
            shift
            ;;
    esac
done

# Show Python version
echo "Python detected: $PYTHON_CMD"
$PYTHON_CMD --version
echo ""

# Paths
VENV_DIR="tools/assets/.venv"
TOOLS_DIR="tools/assets"
ASSETS_DIR="assets"

# Create virtual environment if missing
if [ ! -d "$VENV_DIR" ]; then
    echo "Creating virtual environment..."
    $PYTHON_CMD -m venv "$VENV_DIR"
    echo "✓ Virtual environment created"
    echo ""
fi

# Activate virtual environment
echo "Activating virtual environment..."
source "$VENV_DIR/bin/activate"

# Upgrade pip
echo "Upgrading pip..."
python -m pip install --upgrade pip setuptools wheel --quiet

# Install requirements
echo "Installing dependencies..."
pip install -r "$TOOLS_DIR/requirements.txt" --quiet
echo "✓ Dependencies installed"
echo ""

# Run generator
echo "Generating assets..."
cd "$TOOLS_DIR"

OUTPUT_BASE_ARG="${USER_OUTPUT_BASE:-../../$ASSETS_DIR}"
SEED_ARG="${USER_SEED:-42}"

python generate_all.py --output-base "$OUTPUT_BASE_ARG" --seed "$SEED_ARG"

# Deactivate venv
cd ../..
deactivate

echo ""
echo "=== Assets generated successfully! ==="
echo "Output directory: ${OUTPUT_BASE_ARG}" 
echo ""
