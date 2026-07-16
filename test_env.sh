#!/usr/bin/env bash
# Quick smoke check for Java + Gradle wrapper.
set -euo pipefail

echo "=== Testing Java ==="
java -version
echo
echo "=== Testing Gradle ==="
./gradlew --version
