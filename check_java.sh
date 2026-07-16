#!/usr/bin/env bash
# Verify JDK is on PATH and Gradle wrapper runs.
set -euo pipefail

echo "Checking Java installation..."
if ! command -v java >/dev/null 2>&1; then
  echo "[ERROR] Java not found in PATH"
  echo "Please install JDK 17, e.g.: sudo apt install openjdk-17-jdk"
  echo "Then set JAVA_HOME (see ./set_java_home.sh) and re-open the shell."
  exit 1
fi

java -version
echo
echo "Java found! Checking Gradle wrapper..."
./gradlew --version
