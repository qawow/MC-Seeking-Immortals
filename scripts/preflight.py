#!/usr/bin/env python3
"""Linux/macOS preflight gate mirroring scripts/preflight.ps1."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
import sys
from datetime import datetime
from pathlib import Path


def repo_root() -> Path:
    try:
        result = subprocess.run(
            ["git", "rev-parse", "--show-toplevel"],
            capture_output=True,
            text=True,
            check=False,
        )
        if result.returncode == 0 and result.stdout.strip():
            return Path(result.stdout.strip()).resolve()
    except OSError:
        pass
    return Path.cwd().resolve()


def mod_version(root: Path) -> str:
    properties = root / "gradle.properties"
    if not properties.is_file():
        raise SystemExit("gradle.properties not found; cannot validate mod_version.")
    for line in properties.read_text(encoding="utf-8").splitlines():
        if re.match(r"^mod_version\s*=", line):
            return re.sub(r"^mod_version\s*=\s*", "", line).strip()
    raise SystemExit("mod_version is missing from gradle.properties.")


def relative_path(root: Path, full: Path) -> str:
    return full.resolve().relative_to(root.resolve()).as_posix()


def tracked_file_fingerprint(root: Path, paths: list[str]) -> str:
    files: list[Path] = []
    for path in paths:
        full = root / path
        if full.is_file():
            files.append(full.resolve())
        elif full.is_dir():
            files.extend(p.resolve() for p in full.rglob("*") if p.is_file())

    builder = []
    for full in sorted(set(files), key=lambda p: relative_path(root, p)):
        rel = relative_path(root, full)
        digest = hashlib.sha256(full.read_bytes()).hexdigest()
        builder.append(f"{rel}:{digest}")
    text = "\n".join(builder)
    if builder:
        text += "\n"
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def changed_paths_from_git(root: Path) -> list[str] | None:
    try:
        inside = subprocess.run(
            ["git", "-C", str(root), "rev-parse", "--is-inside-work-tree"],
            capture_output=True,
            text=True,
            check=False,
        )
        if inside.returncode != 0 or inside.stdout.strip() != "true":
            return None

        porcelain = subprocess.run(
            ["git", "-C", str(root), "status", "--porcelain=v1", "-uall"],
            capture_output=True,
            text=True,
            check=False,
        )
        if porcelain.returncode != 0:
            return None

        paths: list[str] = []
        for line in porcelain.stdout.splitlines():
            if not line or len(line) < 4:
                continue
            path = line[3:].strip()
            if " -> " in path:
                path = path.split(" -> ")[-1].strip()
            path = path.strip('"').replace("\\", "/")
            if path:
                paths.append(path)
        return paths
    except OSError:
        return None


def is_shippable_path(path: str) -> bool:
    normalized = path.replace("\\", "/")
    if normalized == "gradle.properties":
        return False
    return (
        normalized.startswith("src/main/java/")
        or normalized.startswith("src/main/resources/")
        or normalized.startswith("src/main/generated/")
        or normalized == "build.gradle"
        or normalized == "settings.gradle"
        or normalized.startswith("gradle/")
        or normalized.startswith("scripts/")
    )


def is_network_path(path: str) -> bool:
    normalized = path.replace("\\", "/")
    return normalized.startswith("src/main/java/com/xunxian/seekingimmortals/network/")


def mod_version_changed_in_git(root: Path) -> bool:
    result = subprocess.run(
        ["git", "-C", str(root), "diff", "--", "gradle.properties"],
        capture_output=True,
        text=True,
        check=False,
    )
    if result.returncode != 0:
        return False
    return any(re.match(r"^[+-]mod_version\s*=", line) for line in result.stdout.splitlines())


def main() -> int:
    parser = argparse.ArgumentParser(description="AI preflight version bump gate")
    parser.add_argument("--skip-version-bump-check", action="store_true")
    parser.add_argument("--record-state-only", action="store_true")
    args = parser.parse_args()

    root = repo_root()
    version = mod_version(root)
    tracked_paths = [
        "src/main/java",
        "src/main/resources",
        "src/main/generated",
        "build.gradle",
        "settings.gradle",
        "gradle.properties",
        "gradle",
        "scripts",
    ]

    if args.skip_version_bump_check:
        print("WARNING: AI preflight version bump check skipped by --skip-version-bump-check.", file=sys.stderr)
        return 0

    if not re.match(r"^0\.1\.\d+$", version):
        print(f"mod_version '{version}' does not match required 0.1.X format.", file=sys.stderr)
        return 1

    changed_paths = changed_paths_from_git(root)
    shippable_changes: list[str] = []
    network_changes: list[str] = []
    version_changed = False

    if changed_paths is not None:
        shippable_changes = [p for p in changed_paths if is_shippable_path(p)]
        network_changes = [p for p in changed_paths if is_network_path(p)]
        version_changed = mod_version_changed_in_git(root)

        if shippable_changes and not version_changed:
            joined = "\n".join(shippable_changes)
            print(
                "AI preflight failed: shippable code/resource/build changes exist, "
                "but gradle.properties did not change mod_version.\n\n"
                "Required flow:\n"
                "  1. Bump mod_version in gradle.properties by one 0.1.X patch version.\n"
                "  2. Re-run ./gradlew build.\n\n"
                f"Changed shippable paths:\n{joined}",
                file=sys.stderr,
            )
            return 1

        if network_changes:
            joined = "\n".join(network_changes)
            print(
                "WARNING: Network package changed. If packet fields, field order, "
                "decode/encode format, or compatibility changed, bump "
                "ModNetwork.PROTOCOL_VERSION before release.\n"
                f"Network paths:\n{joined}",
                file=sys.stderr,
            )
    else:
        print(
            "WARNING: Git status is unavailable; using last successful preflight fingerprint as a fallback.",
            file=sys.stderr,
        )

    state_dir = root / ".gradle" / "ai-preflight"
    state_path = state_dir / "last-success.json"
    fingerprint = tracked_file_fingerprint(root, tracked_paths)

    if args.record_state_only:
        state_dir.mkdir(parents=True, exist_ok=True)
        state_path.write_text(
            json.dumps(
                {
                    "modVersion": version,
                    "fingerprint": fingerprint,
                    "checkedAt": datetime.now().isoformat(timespec="seconds"),
                },
                indent=2,
            )
            + "\n",
            encoding="utf-8",
        )
        print(f"AI preflight state recorded: mod_version={version}")
        return 0

    last_state = None
    if state_path.is_file():
        try:
            last_state = json.loads(state_path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            print(
                "WARNING: Could not read previous AI preflight state; rewriting it after this successful check.",
                file=sys.stderr,
            )

    if (
        last_state is not None
        and last_state.get("fingerprint") != fingerprint
        and last_state.get("modVersion") == version
    ):
        print(
            f"AI preflight failed: shippable tracked files changed since the last successful "
            f"build fingerprint, but mod_version is still {version}.\n\n"
            "Required flow:\n"
            "  1. Bump mod_version in gradle.properties by one 0.1.X patch version.\n"
            "  2. Re-run ./gradlew build.\n\n"
            "If this build is intentionally docs-only or an emergency rebuild, rerun with:\n"
            "  ./gradlew build -PaiSkipVersionBumpCheck=true",
            file=sys.stderr,
        )
        return 1

    print(f"AI preflight passed: mod_version={version}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
