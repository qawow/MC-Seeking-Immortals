#!/usr/bin/env python3
"""Rebuild the shipped text-material runtime inventory without losing curated metadata."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any


REPO_ROOT = Path(__file__).resolve().parents[1]
TEXT_ROOT = REPO_ROOT / "src/main/resources/data/seeking_immortals/text_material"
MANIFEST_PATH = TEXT_ROOT / "manifest.json"


def collection_size(data: Any, primary_key: str) -> int | None:
    if primary_key == "array" and isinstance(data, list):
        return len(data)
    if isinstance(data, dict) and isinstance(data.get(primary_key), (list, dict)):
        return len(data[primary_key])
    return None


def derive_primary(data: Any) -> tuple[str, int]:
    if isinstance(data, list):
        return "array", len(data)
    if isinstance(data, dict):
        collections = [
            (key, len(value), isinstance(value, list), -index)
            for index, (key, value) in enumerate(data.items())
            if isinstance(value, (list, dict))
        ]
        if collections:
            key, size, _, _ = max(collections, key=lambda item: (item[1], item[2], item[3]))
            return key, size
        return "object", len(data)
    return "value", 1


def main() -> None:
    manifest = json.loads(MANIFEST_PATH.read_text(encoding="utf-8"))
    existing_entries = list(manifest.get("files", []))
    existing = {entry["file"]: entry for entry in existing_entries}
    entries: list[dict[str, Any]] = []

    files = {
        path.relative_to(TEXT_ROOT).as_posix(): path
        for path in TEXT_ROOT.rglob("*.json")
        if path != MANIFEST_PATH
    }
    ordered_files = [
        entry["file"] for entry in existing_entries
        if entry.get("file") in files
    ]
    ordered_files.extend(sorted(set(files) - set(ordered_files)))

    for relative in ordered_files:
        path = files[relative]
        data = json.loads(path.read_text(encoding="utf-8"))
        previous = existing.get(relative)
        if previous is not None:
            primary_key = str(previous.get("primary_key", ""))
            entries_count = collection_size(data, primary_key)
            if entries_count is None:
                entries_count = int(previous.get("entries", 0))
            entry_id = str(previous.get("id", relative.removesuffix(".json")))
        else:
            primary_key, entries_count = derive_primary(data)
            entry_id = relative.removesuffix(".json")
        entries.append({
            "id": entry_id,
            "file": relative,
            "entries": entries_count,
            "primary_key": primary_key,
        })

    technique_files = sum(entry["file"].startswith("techniques/") for entry in entries)
    manifest["catalog_files"] = len(entries) - technique_files
    manifest["technique_files"] = technique_files
    manifest["total_files"] = len(entries)
    manifest["files"] = entries
    MANIFEST_PATH.write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
