#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/../.."
./gradlew :app:run --args="--server --headless"
