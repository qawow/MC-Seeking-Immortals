#!/usr/bin/env bash
# Hint helper for setting JAVA_HOME to a local JDK 17.
# Does not mutate the shell of the caller; print export lines to eval.
set -euo pipefail

CANDIDATES=(
  "${JAVA_HOME:-}"
  /usr/lib/jvm/java-17-openjdk-amd64
  /usr/lib/jvm/java-17-openjdk
  /usr/lib/jvm/temurin-17-jdk-amd64
  /usr/lib/jvm/adoptium-17-jdk-amd64
  "$HOME/.sdkman/candidates/java/current"
)

found=""
for c in "${CANDIDATES[@]}"; do
  [[ -z "$c" ]] && continue
  if [[ -x "$c/bin/java" ]]; then
    found="$c"
    break
  fi
done

if [[ -z "$found" ]] && command -v java >/dev/null 2>&1; then
  # Resolve JAVA_HOME from the java on PATH when possible
  java_bin=$(readlink -f "$(command -v java)" 2>/dev/null || command -v java)
  # .../bin/java -> parent of bin
  found=$(dirname "$(dirname "$java_bin")")
fi

if [[ -z "$found" || ! -x "$found/bin/java" ]]; then
  echo "[ERROR] JDK 17 not found. Install with e.g.:" >&2
  echo "  sudo apt install openjdk-17-jdk" >&2
  echo "Then re-run: source ./set_java_home.sh" >&2
  exit 1
fi

echo "export JAVA_HOME=\"$found\""
echo "export PATH=\"\$JAVA_HOME/bin:\$PATH\""
echo "# JAVA_HOME -> $found" >&2
echo "# Apply in current shell:  eval \"\$(./set_java_home.sh)\"" >&2
echo "# Or:                      source <(./set_java_home.sh)" >&2
