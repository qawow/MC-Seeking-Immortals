#!/usr/bin/env python3
"""Compile the v118-v122 authoring stack into compact runtime VFX profiles."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOURCE_DIR = ROOT / "文本材料" / "data"
OUTPUT = (
    ROOT
    / "src/main/resources/data/seeking_immortals/visual/authored_technique_vfx_profiles.json"
)
VERSIONS = (118, 119, 120, 121, 122)
EXPECTED_PROFILE_COUNT = 344
TECHNIQUE_TYPES = {
    118: "technique_vfx",
    119: "tech_storyboard",
    120: "technique_cast_sheet",
    121: "technique_frame_sheet",
    122: "technique_style_card",
}
GENERIC_TAGS = {
    "visual_v118",
    "visual_v119",
    "visual_v120",
    "visual_v121",
    "visual_v122",
    "technique",
    "frames",
    "storyboard",
}
PARTICLE_REFS = {
    "qi_soft",
    "fire_ember",
    "water_mist",
    "wood_pollen",
    "metal_spark",
    "earth_dust",
    "thunder_arc",
    "yin_smoke",
    "soul_wisps",
    "blood_mist",
    "heal_motes",
    "space_glitch",
}
TRAIL_REFS = {
    "none",
    "sword_thin",
    "heavy_weapon",
    "flying_sword_orbit",
    "talisman_ash",
    "blood_ribbon",
    "thunder_jagged",
    "soul_afterimage",
    "movement_wind",
}
SUPPORTED_PARTICLE_EXPRESSIONS = PARTICLE_REFS | {"water_mist+metal_spark"}


def read_layer(version: int) -> tuple[Path, dict]:
    path = SOURCE_DIR / f"item_descriptions_v{version}.json"
    return path, json.loads(path.read_text(encoding="utf-8"))


def school(entry: dict) -> str:
    candidates = [
        str(tag).strip().lower()
        for tag in entry.get("tags", [])
        if str(tag).strip().lower() not in GENERIC_TAGS
    ]
    return candidates[-1] if candidates else "misc"


def entry_priority(entry: dict) -> tuple[int, str]:
    value = school(entry)
    return (0 if value == "misc" else 1, value)


def choose(entries: list[dict], preferred_school: str = "") -> dict | None:
    if not entries:
        return None
    matching = [entry for entry in entries if school(entry) == preferred_school]
    pool = matching or entries
    return sorted(pool, key=entry_priority, reverse=True)[0]


def index_layer(items: list[dict], entry_type: str) -> dict[str, list[dict]]:
    result: dict[str, list[dict]] = {}
    for entry in items:
        if entry.get("type") != entry_type:
            continue
        catalog_id = str(entry.get("catalog_id", "")).strip()
        if catalog_id:
            result.setdefault(catalog_id, []).append(entry)
    return result


def match_token(text: str, label: str) -> str:
    match = re.search(rf"{re.escape(label)}([a-z_]+)", text or "", re.IGNORECASE)
    return match.group(1).lower() if match else ""


def parse_style(appearance: str) -> tuple[str, str]:
    match = re.search(r"形状语言(.+?)\s+主色(.+)$", appearance or "")
    if not match:
        return "", ""
    return match.group(1).strip(), match.group(2).strip()


def frame_visual(entry: dict, name: str) -> str:
    for frame in entry.get("frames", []):
        if str(frame.get("name", "")).strip() == name:
            return str(frame.get("vis", "")).strip()
    return ""


def source_digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def compile_profiles() -> dict:
    layers: dict[int, dict] = {}
    source_hashes: dict[str, str] = {}
    for version in VERSIONS:
        path, payload = read_layer(version)
        layers[version] = payload
        source_hashes[path.name] = source_digest(path)

    indexes = {
        version: index_layer(layers[version]["items"], TECHNIQUE_TYPES[version])
        for version in VERSIONS
    }
    core_sets = {version: set(indexes[version]) for version in (118, 120, 121, 122)}
    if len({frozenset(ids) for ids in core_sets.values()}) != 1:
        raise ValueError("v118/v120/v121/v122 technique catalog_id sets do not match")
    if set(indexes[119]) != {"qingyuan_sword_ray"}:
        raise ValueError("v119 intentionally contains exactly the qingyuan_sword_ray technique storyboard")
    if len(core_sets[121]) != EXPECTED_PROFILE_COUNT:
        raise ValueError(f"expected {EXPECTED_PROFILE_COUNT} authored technique ids")
    qingyuan_ultra = next(
        entry
        for entry in layers[118]["items"]
        if entry.get("id") == "vis_ultra_qingyuan_ray"
    )
    particle_bible = next(
        entry["systems"]
        for entry in layers[121]["items"]
        if entry.get("id") == "particle_system_bible_v121"
    )
    trail_bible = next(
        entry["trails"]
        for entry in layers[121]["items"]
        if entry.get("id") == "trail_ribbon_styles_v121"
    )
    if not PARTICLE_REFS.issubset(particle_bible):
        raise ValueError("particle system bible is missing a supported runtime reference")
    if not (TRAIL_REFS - {"none"}).issubset(trail_bible):
        raise ValueError("trail bible is missing a supported runtime reference")

    profiles: list[dict] = []
    duplicate_resolutions: dict[str, dict] = {}
    for catalog_id in sorted(indexes[121]):
        frame_candidates = indexes[121][catalog_id]
        frame = choose(frame_candidates)
        assert frame is not None
        preferred_school = school(frame)
        selected = {
            version: choose(indexes[version].get(catalog_id, []), preferred_school)
            for version in VERSIONS
        }
        selected[121] = frame

        particle_ref = str(frame.get("particle_ref", "")).strip().lower()
        trail_ref = str(frame.get("trail_ref", "none")).strip().lower()
        if particle_ref not in SUPPORTED_PARTICLE_EXPRESSIONS:
            raise ValueError(f"{catalog_id}: unsupported particle expression {particle_ref}")
        if trail_ref not in TRAIL_REFS:
            raise ValueError(f"{catalog_id}: unknown trail {trail_ref}")

        style = selected[122] or {}
        overview = selected[120] or {}
        base = selected[118] or {}
        ultra = qingyuan_ultra if catalog_id == "qingyuan_sword_ray" else {}
        shape, color = parse_style(str(style.get("appearance", "")))
        appearance = str(frame.get("appearance", ""))
        profile = {
            "id": catalog_id,
            "school": preferred_school,
            "effect_type": match_token(appearance, "类型"),
            "element": match_token(appearance, "元素"),
            "particle": particle_ref,
            "trail": trail_ref,
            "shape": shape,
            "color": color,
            "cast": str(
                ultra.get("cast_vfx")
                or style.get("cast_vfx")
                or overview.get("cast_vfx")
                or base.get("cast_vfx")
                or ""
            ).strip(),
            "telegraph": frame_visual(frame, "大招预兆"),
            "anticipation": frame_visual(frame, "起手"),
            "formation": frame_visual(frame, "成型"),
            "release": frame_visual(frame, "出击"),
            "impact": frame_visual(frame, "命中"),
            "decay": frame_visual(frame, "回收"),
            "frame_count": len(frame.get("frames", [])),
            "has_telegraph": bool(frame_visual(frame, "大招预兆")),
            "sources": {
                f"v{version}": entry.get("id", "")
                for version, entry in selected.items()
                if entry is not None
            },
        }
        profiles.append(profile)
        if ultra:
            profile["sources"]["v118_ultra"] = ultra["id"]
        if len(frame_candidates) > 1:
            duplicate_resolutions[catalog_id] = {
                "selected_school": preferred_school,
                "selected_particle": particle_ref,
                "candidates": [
                    {
                        "school": school(candidate),
                        "particle": candidate.get("particle_ref", ""),
                        "trail": candidate.get("trail_ref", ""),
                    }
                    for candidate in frame_candidates
                ],
            }

    return {
        "schema_version": 1,
        "description": "v118-v122 作者视觉栈的紧凑术法运行时档案",
        "source_hashes": source_hashes,
        "constraints": {
            "max_concurrent_particle_systems": 4,
            "max_vignette_seconds": 2.0,
            "sword_trail": "thin",
            "unique_language": "space_glitch",
            "forbid": ["rainbow_particle_rain", "thick_neon_sword_trail"],
        },
        "duplicate_resolutions": duplicate_resolutions,
        "profile_count": len(profiles),
        "profiles": profiles,
    }


def encoded_output() -> str:
    return json.dumps(compile_profiles(), ensure_ascii=False, indent=2) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="fail if the committed output is stale")
    args = parser.parse_args()
    content = encoded_output()
    if args.check:
        if not OUTPUT.exists() or OUTPUT.read_text(encoding="utf-8") != content:
            print(f"stale generated file: {OUTPUT.relative_to(ROOT)}")
            return 1
        print(f"authored VFX profiles are current: {len(compile_profiles()['profiles'])}")
        return 0
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(content, encoding="utf-8")
    print(f"wrote {OUTPUT.relative_to(ROOT)} ({len(compile_profiles()['profiles'])} profiles)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
