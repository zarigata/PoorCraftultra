#!/bin/bash

# Run wrapper script for Unix/Linux/macOS
# Detects Gradle or Maven and runs appropriate run command

set -e

echo "=== Starting Poorcraft Ultra ==="

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
    ./gradlew run
elif [ -f "./mvnw" ]; then
    echo "Using Maven wrapper (fallback)"
    chmod +x ./mvnw
    ./mvnw exec:java
else
    echo "ERROR: No build tool found (gradlew or mvnw)"
    exit 1
fi
