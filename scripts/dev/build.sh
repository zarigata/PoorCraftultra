#!/bin/bash

# Build wrapper script for Unix/Linux/macOS
# Detects Gradle or Maven and runs appropriate build command

set -e

echo "=== Poorcraft Ultra Build Script ==="
echo "Detecting build tool..."

# Show Java version
if command -v java &> /dev/null; then
    echo "Java version:"
    java -version
else
    echo "ERROR: Java not found in PATH"
    exit 1
fi

# Detect and run build tool
if [ -f "./gradlew" ]; then
    echo "Using Gradle wrapper"
    chmod +x ./gradlew
    ./gradlew clean build
elif [ -f "./mvnw" ]; then
    echo "Using Maven wrapper (fallback)"
    chmod +x ./mvnw
    ./mvnw clean package
else
    echo "ERROR: No build tool found (gradlew or mvnw)"
    exit 1
fi

echo "Build complete!"
