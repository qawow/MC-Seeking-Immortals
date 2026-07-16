#!/usr/bin/env bash
# AI preflight gate: ensure shippable changes bump numeric SemVer mod_version (X.Y.Z).
# Linux/bash port of scripts/preflight.ps1. Flags mirror the PowerShell version:
#   --skip-version-bump-check / -SkipVersionBumpCheck
#   --record-state-only       / -RecordStateOnly
set -euo pipefail

SKIP_VERSION_BUMP_CHECK=0
RECORD_STATE_ONLY=0

for arg in "$@"; do
  case "$arg" in
    -SkipVersionBumpCheck|--skip-version-bump-check|SkipVersionBumpCheck)
      SKIP_VERSION_BUMP_CHECK=1
      ;;
    -RecordStateOnly|--record-state-only|RecordStateOnly)
      RECORD_STATE_ONLY=1
      ;;
    -h|--help)
      echo "Usage: $0 [--skip-version-bump-check] [--record-state-only]"
      exit 0
      ;;
  esac
done

get_repo_root() {
  local root
  if root=$(git rev-parse --show-toplevel 2>/dev/null); then
    (cd "$root" && pwd -P)
    return
  fi
  pwd -P
}

get_mod_version() {
  local root="$1"
  local properties_path="$root/gradle.properties"
  if [[ ! -f "$properties_path" ]]; then
    echo "gradle.properties not found; cannot validate mod_version." >&2
    exit 1
  fi
  local line
  line=$(grep -E '^mod_version[[:space:]]*=' "$properties_path" | head -n1 || true)
  if [[ -z "$line" ]]; then
    echo "mod_version is missing from gradle.properties." >&2
    exit 1
  fi
  echo "${line#*=}" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//'
}

# Relative path of $2 under $1, forward-slash normalized.
rel_path() {
  local root="$1"
  local full="$2"
  local root_slash="${root%/}/"
  if [[ "$full" == "$root_slash"* ]]; then
    echo "${full#"$root_slash"}"
  else
    # Fallback: try realpath --relative-to when available
    if command -v realpath >/dev/null 2>&1; then
      realpath --relative-to="$root" "$full" 2>/dev/null || echo "$full"
    else
      echo "$full"
    fi
  fi
}

# SHA-256 of the sorted "relpath:filehash" listing for tracked paths.
get_tracked_file_fingerprint() {
  local root="$1"
  shift
  local listing=""
  local path full rel hash
  local -a files=()

  for path in "$@"; do
    full="$root/$path"
    if [[ -f "$full" ]]; then
      files+=("$full")
    elif [[ -d "$full" ]]; then
      while IFS= read -r -d '' f; do
        files+=("$f")
      done < <(find "$full" -type f -print0 2>/dev/null)
    fi
  done

  # Sort unique
  if ((${#files[@]} == 0)); then
    printf '' | sha256sum | awk '{print $1}'
    return
  fi

  # Produce "rel:hash" lines, sorted by rel path
  {
    for full in "${files[@]}"; do
      rel=$(rel_path "$root" "$full")
      if command -v sha256sum >/dev/null 2>&1; then
        hash=$(sha256sum "$full" | awk '{print $1}')
      else
        hash=$(shasum -a 256 "$full" | awk '{print $1}')
      fi
      printf '%s:%s\n' "$rel" "$hash"
    done
  } | LC_ALL=C sort -u | sha256sum | awk '{print $1}'
}

get_changed_paths_from_git() {
  local root="$1"
  if ! git -C "$root" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    return 1
  fi
  # porcelain=v1 -uall: "XY path" or "XY old -> new"
  git -C "$root" status --porcelain=v1 -uall 2>/dev/null | while IFS= read -r line; do
    [[ -z "$line" || ${#line} -lt 4 ]] && continue
    local path="${line:3}"
    path="${path#"${path%%[![:space:]]*}"}"  # ltrim
    if [[ "$path" == *' -> '* ]]; then
      path="${path##* -> }"
    fi
    path="${path%\"}"
    path="${path#\"}"
    path="${path//\\//}"
    [[ -n "$path" ]] && printf '%s\n' "$path"
  done
}

is_shippable_path() {
  local p="${1//\\//}"
  [[ "$p" == "gradle.properties" ]] && return 1
  case "$p" in
    src/main/java/*|src/main/resources/*|src/main/generated/*) return 0 ;;
    build.gradle|settings.gradle) return 0 ;;
    gradle/*|scripts/*) return 0 ;;
    *) return 1 ;;
  esac
}

is_network_path() {
  local p="${1//\\//}"
  case "$p" in
    src/main/java/com/xunxian/seekingimmortals/network/*) return 0 ;;
    *) return 1 ;;
  esac
}

mod_version_changed_in_git() {
  local root="$1"
  local diff
  diff=$(git -C "$root" diff -- gradle.properties 2>/dev/null || true)
  echo "$diff" | grep -Eq '^[+-]mod_version[[:space:]]*='
}

REPO_ROOT=$(get_repo_root)
MOD_VERSION=$(get_mod_version "$REPO_ROOT")
TRACKED_PATHS=(
  src/main/java
  src/main/resources
  src/main/generated
  build.gradle
  settings.gradle
  gradle.properties
  gradle
  scripts
)

STATE_DIR="$REPO_ROOT/.gradle/ai-preflight"
STATE_PATH="$STATE_DIR/last-success.json"
FINGERPRINT=$(get_tracked_file_fingerprint "$REPO_ROOT" "${TRACKED_PATHS[@]}")

# Record-state-only must not re-run the version-bump gate: it is the post-build
# fingerprint writer used by Gradle build.doLast (including emergency rebuilds
# that intentionally skip the bump check).
if [[ "$RECORD_STATE_ONLY" -eq 1 ]]; then
  mkdir -p "$STATE_DIR"
  cat > "$STATE_PATH" <<EOF
{
  "modVersion": "$MOD_VERSION",
  "fingerprint": "$FINGERPRINT",
  "checkedAt": "$(date -Iseconds 2>/dev/null || date '+%Y-%m-%dT%H:%M:%S')"
}
EOF
  echo "AI preflight state recorded: mod_version=$MOD_VERSION"
  exit 0
fi

if [[ "$SKIP_VERSION_BUMP_CHECK" -eq 1 ]]; then
  echo "AI preflight version bump check skipped by --skip-version-bump-check." >&2
  exit 0
fi

if [[ ! "$MOD_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "mod_version '$MOD_VERSION' does not match required numeric X.Y.Z format." >&2
  exit 1
fi

SHIPPABLE_CHANGES=()
NETWORK_CHANGES=()
VERSION_CHANGED=0
GIT_AVAILABLE=0

if CHANGED_RAW=$(get_changed_paths_from_git "$REPO_ROOT"); then
  GIT_AVAILABLE=1
  while IFS= read -r p; do
    [[ -z "$p" ]] && continue
    if is_shippable_path "$p"; then
      SHIPPABLE_CHANGES+=("$p")
    fi
    if is_network_path "$p"; then
      NETWORK_CHANGES+=("$p")
    fi
  done <<< "$CHANGED_RAW"

  if mod_version_changed_in_git "$REPO_ROOT"; then
    VERSION_CHANGED=1
  fi

  if ((${#SHIPPABLE_CHANGES[@]} > 0)) && [[ "$VERSION_CHANGED" -eq 0 ]]; then
    {
      echo "AI preflight failed: shippable code/resource/build changes exist, but gradle.properties did not change mod_version."
      echo
      echo "Required flow:"
      echo "  1. Bump mod_version in gradle.properties by one patch version."
      echo "  2. Re-run ./gradlew build."
      echo
      echo "Changed shippable paths:"
      printf '%s\n' "${SHIPPABLE_CHANGES[@]}"
    } >&2
    exit 1
  fi

  if ((${#NETWORK_CHANGES[@]} > 0)); then
    {
      echo "Network package changed. If packet fields, field order, decode/encode format, or compatibility changed, bump ModNetwork.PROTOCOL_VERSION before release."
      echo "Network paths:"
      printf '%s\n' "${NETWORK_CHANGES[@]}"
    } >&2
  fi
else
  echo "Git status is unavailable; using last successful preflight fingerprint as a fallback." >&2
fi

if [[ -f "$STATE_PATH" ]]; then
  LAST_MOD=""
  LAST_FP=""
  # Prefer jq; fall back to sed/grep
  if command -v jq >/dev/null 2>&1; then
    LAST_MOD=$(jq -r '.modVersion // empty' "$STATE_PATH" 2>/dev/null || true)
    LAST_FP=$(jq -r '.fingerprint // empty' "$STATE_PATH" 2>/dev/null || true)
  else
    LAST_MOD=$(grep -oE '"modVersion"[[:space:]]*:[[:space:]]*"[^"]*"' "$STATE_PATH" | head -n1 | sed -E 's/.*"([^"]*)"[[:space:]]*$/\1/')
    LAST_FP=$(grep -oE '"fingerprint"[[:space:]]*:[[:space:]]*"[^"]*"' "$STATE_PATH" | head -n1 | sed -E 's/.*"([^"]*)"[[:space:]]*$/\1/')
  fi

  if [[ -n "$LAST_FP" && -n "$LAST_MOD" && "$LAST_FP" != "$FINGERPRINT" && "$LAST_MOD" == "$MOD_VERSION" ]]; then
    {
      echo "AI preflight failed: shippable tracked files changed since the last successful build fingerprint, but mod_version is still $MOD_VERSION."
      echo
      echo "Required flow:"
      echo "  1. Bump mod_version in gradle.properties by one patch version."
      echo "  2. Re-run ./gradlew build."
      echo
      echo "If this build is intentionally docs-only or an emergency rebuild, rerun with:"
      echo "  ./gradlew build -PaiSkipVersionBumpCheck=true"
    } >&2
    exit 1
  fi
fi

echo "AI preflight passed: mod_version=$MOD_VERSION"
