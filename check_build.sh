#!/usr/bin/env bash
# Tail a Gradle build log for success/failure markers.
# Usage:
#   ./check_build.sh                  # default: build/build.log if present
#   ./check_build.sh /path/to/log
set -euo pipefail

LOG="${1:-build/build.log}"

if [[ ! -f "$LOG" ]]; then
  echo "Log not found: $LOG"
  echo "Usage: $0 [path/to/build.log]"
  echo "Tip: run  ./gradlew --no-daemon build 2>&1 | tee build/build.log"
  exit 1
fi

echo "Checking compilation status in: $LOG"
echo

if grep -E 'BUILD SUCCESSFUL|BUILD FAILED|FAILURE:|error:|错误' "$LOG" >/dev/null 2>&1; then
  echo "Compilation finished!"
  grep -E 'BUILD SUCCESSFUL|BUILD FAILED|FAILURE:' "$LOG" | tail -n 5
else
  echo "Still compiling / no terminal marker yet. Last lines:"
  tail -n 5 "$LOG"
fi
