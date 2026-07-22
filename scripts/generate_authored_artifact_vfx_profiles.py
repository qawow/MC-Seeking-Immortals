#!/usr/bin/env python3
"""Compile the v118-v122 authored artifact visual stack into runtime profiles."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOURCE_DIR = ROOT / "文本材料" / "data"
RUNTIME_CATALOG = ROOT / "src/main/resources/data/seeking_immortals/artifacts/artifacts_catalog.json"
OUTPUT = ROOT / "src/main/resources/data/seeking_immortals/visual/authored_artifact_vfx_profiles.json"
EXPECTED_PROFILE_COUNT = 217
LAYER_TYPES = {
    118: "artifact_visual",
    119: "artifact_icon",
    120: "artifact_performance_sheet",
    121: "artifact_state_visual",
    122: "artifact_look_card",
}
REQUIRED_STATES = {"sheathed", "idle_bound", "active", "impact", "damaged", "broken"}
FAMILY_VALUES = {
    "FIRE", "WATER", "METAL", "WOOD", "EARTH", "WIND", "ICE", "THUNDER",
    "LIGHT", "DARK", "SOUL", "BLOOD", "VOID", "ILLUSION", "NEUTRAL",
}
MOTIF_VALUES = {
    "GENERIC", "PROJECTILE", "BLADE", "SHIELD", "DOMAIN", "TELEPORT", "SUMMON",
    "WALL", "CHAIN", "CHANNEL", "RAIN", "HEAL", "CLEANSE", "SEAL", "FORMATION",
    "BUDDHIST", "CONFUCIAN", "DAO", "GHOST", "TALISMAN", "ILLUSION", "MARTIAL",
}
PARTICLE_VALUES = {
    "qi_soft", "fire_ember", "water_mist", "wood_pollen", "metal_spark", "earth_dust",
    "thunder_arc", "yin_smoke", "soul_wisps", "blood_mist", "heal_motes", "space_glitch",
}
TRAIL_VALUES = {
    "none", "sword_thin", "heavy_weapon", "flying_sword_orbit", "talisman_ash",
    "blood_ribbon", "thunder_jagged", "soul_afterimage", "movement_wind",
}


def read_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def source_digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def clean(value: object) -> str:
    text = "" if value is None else str(value).strip()
    return "" if text.lower() in {"none", "null", "n/a"} else text


def norm(value: object) -> str:
    return clean(value).lower().replace("-", "_").replace(" ", "_")


def combined_text(*values: object) -> str:
    return norm(" ".join(clean(value) for value in values if value is not None))


def contains(text: str, *tokens: str) -> bool:
    for token in tokens:
        if token.isascii():
            if re.search(rf"(?<![a-z0-9]){re.escape(token)}(?![a-z0-9])", text):
                return True
        elif token in text:
            return True
    return False


def parse_type(visual: dict, fallback: str) -> str:
    text = clean(visual.get("appearance")) + " " + clean(visual.get("effect_text"))
    match = re.search(r"类型\s*[:：]?\s*([a-zA-Z0-9_]+)", text, re.IGNORECASE)
    return norm(match.group(1) if match else fallback)


def parse_tier(visual: dict, fallback: str) -> str:
    text = clean(visual.get("appearance")) + " " + clean(visual.get("effect_text"))
    match = re.search(r"品阶\s*[:：]?\s*([a-zA-Z0-9_]+)", text, re.IGNORECASE)
    return norm(match.group(1) if match else fallback)


def parse_color(look: dict) -> str:
    text = clean(look.get("effect_text"))
    match = re.search(r"催动主色\s*(.+)$", text)
    return clean(match.group(1) if match else "")


def parse_silhouette(look: dict) -> str:
    text = clean(look.get("appearance"))
    match = re.search(r"剪影[:：]\s*(.+?)(?:[。；]|$)", text)
    return clean(match.group(1) if match else "")


def family_for(artifact: dict, visual: dict, states: dict) -> str:
    artifact_id = norm(artifact.get("id"))
    artifact_type = norm(artifact.get("type"))
    element = norm(artifact.get("element"))
    tags = combined_text(artifact.get("tags"))
    text = combined_text(artifact_id, artifact_type, element, tags, artifact.get("effect"),
                         visual.get("active_vfx"), states.get("active"), states.get("impact"))
    explicit_element = {
        "fire": "FIRE", "flame": "FIRE", "water": "WATER", "ice": "ICE",
        "wood": "WOOD", "metal": "METAL", "earth": "EARTH", "wind": "WIND",
        "thunder": "THUNDER", "light": "LIGHT", "dark": "DARK", "soul": "SOUL",
        "blood": "BLOOD", "void": "VOID", "illusion": "ILLUSION",
    }.get(element)
    if explicit_element:
        return explicit_element
    if artifact_type in {"storage", "teleport_protection", "space_control", "world"} \
            or contains(text, "black_hole", "黑洞", "收纳"):
        return "VOID"
    if artifact_type in {"soul_attack", "soul_destroy", "soul", "yin", "soul_stabilize", "sound"}:
        return "SOUL"
    if artifact_type in {"illusion", "mirror", "anti_illusion"}:
        return "ILLUSION"
    if artifact_type in {"beast", "beast_control", "beast_refine", "beast_spirit", "puppet",
                         "puppet_core", "puppet_control", "puppet_summon", "swarm", "hybrid_puppet_core"}:
        return "WOOD"
    if artifact_type == "magnet":
        return "METAL"
    if artifact_type in {"thunder"} or contains(text, "thunder", "lightning", "雷电", "落雷", "雷弧", "紫金"):
        return "THUNDER"
    if artifact_type in {"poison"} or contains(text, "poison", "venom", "毒针", "毒雾"):
        return "WOOD"
    if contains(text, "ice", "frost", "cold", "冰霜", "寒气"):
        return "ICE"
    if artifact_type in {"talisman_treasure", "refinement"} or contains(text, "fire", "flame", "lava", "灵焰", "火焰", "火蛟"):
        return "FIRE"
    if contains(text, "water", "rain", "ocean", "water_mist", "水雾", "潮涌"):
        return "WATER"
    if contains(text, "blood", "demon", "corpse", "血雾", "魔气", "湿暗"):
        return "BLOOD"
    if contains(text, "soul", "ghost", "yin", "魂絮", "鬼火", "幽紫"):
        return "SOUL"
    if contains(text, "void", "space", "rift", "portal", "虚空", "空间涟漪", "漩涡"):
        return "VOID"
    if contains(text, "illusion", "mirror", "dream", "幻术", "镜光", "镜面"):
        return "ILLUSION"
    if artifact_type in {"movement", "vehicle", "vehicle_key"} or contains(text, "wind", "sail", "风遁", "云轿", "飞遁"):
        return "WIND"
    if contains(text, "wood", "herb", "beast", "swarm", "puppet", "木灵", "兽魂", "虫群", "花粉"):
        return "WOOD"
    if artifact_type in {"defense", "formation", "formation_deploy", "formation_token"} or contains(text, "earth", "shield", "armor", "stone", "尘团", "护盾", "地网"):
        return "EARTH"
    if contains(text, "metal", "sword", "blade", "gold", "ruler", "magnet", "yuanci", "剑气", "刃光", "金属锐屑"):
        return "METAL"
    if contains(text, "heal", "focus", "utility", "repair", "light", "灵液", "疗愈"):
        return "LIGHT"
    if artifact_type in {"generic", "material_artifact", "ruler", "offense", "attack", "flying_sword"}:
        return "METAL"
    return "LIGHT"


def motif_for(artifact: dict, visual: dict, states: dict) -> str:
    artifact_type = norm(artifact.get("type"))
    artifact_id = norm(artifact.get("id"))
    text = combined_text(artifact_id, artifact_type, artifact.get("tags"), artifact.get("effect"),
                         visual.get("active_vfx"), visual.get("description"), states.get("active"))
    if contains(text, "void_heaven_cauldron", "虚天鼎", "黑洞", "漩涡"):
        return "DOMAIN"
    if artifact_type == "storage" or contains(text, "black_hole", "黑洞", "收纳"):
        return "DOMAIN"
    if artifact_type in {"defense", "anti_illusion"} or contains(text, "shield", "armor", "膜白", "护罩"):
        return "SHIELD"
    if artifact_type in {"movement", "vehicle", "vehicle_key", "teleport_protection"} or contains(text, "teleport", "space_shift", "风遁"):
        return "TELEPORT"
    if artifact_type in {"formation", "formation_deploy", "formation_token", "magnet", "world", "space_control"}:
        return "FORMATION" if artifact_type.startswith("formation") else "DOMAIN"
    if artifact_type in {"control", "capture", "puppet_control"} or contains(text, "bind", "chain", "锁链", "缚"):
        return "CHAIN"
    if artifact_type in {"illusion", "mirror", "anti_illusion"}:
        return "ILLUSION"
    if artifact_type in {"soul_attack", "soul_destroy", "soul", "yin", "soul_stabilize"}:
        return "GHOST"
    if artifact_type in {"beast", "beast_control", "beast_refine", "beast_spirit", "puppet", "puppet_summon", "swarm", "hybrid_puppet_core"}:
        return "SUMMON"
    if artifact_type in {"material_artifact", "generic"}:
        return "GENERIC"
    if artifact_type in {"quest_key", "natal_slot"}:
        return "CLEANSE"
    if artifact_type in {"sound", "refinement"} or contains(text, "sound", "声纹"):
        return "CHANNEL"
    if artifact_type in {"talisman_treasure"} or contains(text, "talisman", "符宝", "符纸"):
        return "TALISMAN"
    if artifact_type in {"spirit_liquid"} or contains(text, "heal", "recovery", "灵液"):
        return "HEAL"
    if artifact_type in {"utility", "focus", "repair", "storage"}:
        return "CLEANSE"
    if artifact_type in {"flying_sword", "offense", "attack", "ruler", "thunder"}:
        return "BLADE" if artifact_type in {"flying_sword", "offense", "ruler"} else "PROJECTILE"
    if artifact_type in {"poison"}:
        return "PROJECTILE"
    return "PROJECTILE"


def particle_for(family: str, artifact: dict, visual: dict, states: dict) -> str:
    artifact_type = norm(artifact.get("type"))
    text = combined_text(artifact.get("id"), artifact_type, artifact.get("effect"),
                         visual.get("active_vfx"), states.get("active"), states.get("impact"))
    if artifact_type == "storage" or contains(text, "黑洞", "收纳"):
        return "space_glitch"
    if family == "VOID":
        return "space_glitch"
    if artifact_type in {"defense", "anti_illusion"}:
        return "qi_soft"
    if artifact_type in {"utility", "focus", "repair", "spirit_liquid"}:
        return "heal_motes"
    return {
        "FIRE": "fire_ember",
        "WATER": "water_mist",
        "METAL": "metal_spark",
        "WOOD": "wood_pollen",
        "EARTH": "earth_dust",
        "WIND": "qi_soft",
        "ICE": "water_mist",
        "THUNDER": "thunder_arc",
        "LIGHT": "heal_motes",
        "DARK": "yin_smoke",
        "SOUL": "soul_wisps",
        "BLOOD": "blood_mist",
        "VOID": "space_glitch",
        "ILLUSION": "soul_wisps",
        "NEUTRAL": "qi_soft",
    }[family]


def trail_for(artifact: dict, visual: dict, states: dict, silhouette: str) -> str:
    artifact_type = norm(artifact.get("type"))
    text = combined_text(artifact.get("id"), artifact_type, artifact.get("effect"), silhouette,
                         visual.get("active_vfx"), visual.get("description"), states.get("active"), states.get("launch"))
    if artifact_type in {"movement", "vehicle", "vehicle_key", "teleport_protection"}:
        return "movement_wind"
    if artifact_type == "flying_sword" or "orbit" in states or "launch" in states:
        return "flying_sword_orbit"
    if artifact_type in {"talisman_treasure", "formation_token"} or contains(text, "talisman", "焚化"):
        return "talisman_ash"
    if artifact_type == "thunder" or contains(text, "thunder", "lightning", "雷", "折线"):
        return "thunder_jagged"
    if contains(text, "blood", "demon", "血", "粘稠", "下挂"):
        return "blood_ribbon"
    if artifact_type in {"soul_attack", "soul_destroy", "soul", "yin", "soul_stabilize", "sound"}:
        return "soul_afterimage"
    if contains(text, "锤", "斧", "杵", "砖", "鼎", "重击", "砸环"):
        return "heavy_weapon"
    if artifact_type in {"offense", "attack", "ruler", "poison"}:
        return "sword_thin"
    return "none"


def telegraphed_for(artifact: dict, motif: str, visual: dict, states: dict) -> bool:
    tier = int(artifact.get("game_tier", 0) or 0)
    artifact_type = norm(artifact.get("type"))
    text = combined_text(visual.get("active_vfx"), visual.get("description"), states.get("active"), states.get("impact"))
    authored_warning = contains(text, "前摇", "蓄", "预兆", "危险", "天威")
    high_tier = tier >= 8 and artifact_type not in {"material_artifact", "utility", "storage", "quest_key"}
    return authored_warning or (high_tier and motif in {"BLADE", "PROJECTILE", "DOMAIN", "FORMATION", "GHOST", "CHAIN", "TALISMAN"})


def index_layer(payload: dict, entry_type: str) -> dict[str, dict]:
    result: dict[str, dict] = {}
    duplicates: list[str] = []
    for entry in payload.get("items", []):
        if entry.get("type") != entry_type:
            continue
        catalog_id = clean(entry.get("catalog_id"))
        if not catalog_id:
            continue
        if catalog_id in result:
            duplicates.append(catalog_id)
        result[catalog_id] = entry
    if duplicates:
        raise ValueError(f"duplicate {entry_type} catalog ids: {sorted(set(duplicates))}")
    return result


def compile_profiles() -> dict:
    layers = {
        version: read_json(SOURCE_DIR / f"item_descriptions_v{version}.json")
        for version in LAYER_TYPES
    }
    indexes = {version: index_layer(layers[version], entry_type) for version, entry_type in LAYER_TYPES.items()}
    if any(len(indexes[version]) != EXPECTED_PROFILE_COUNT for version in indexes):
        raise ValueError(f"artifact visual layer counts must all be {EXPECTED_PROFILE_COUNT}: "
                         f"{ {version: len(index) for version, index in indexes.items()} }")
    layer_sets = [set(index) for index in indexes.values()]
    if any(ids != layer_sets[0] for ids in layer_sets[1:]):
        raise ValueError("v118-v122 artifact visual catalog_id sets do not match")

    runtime = read_json(RUNTIME_CATALOG)
    artifacts = {clean(entry.get("id")): entry for entry in runtime.get("artifacts", []) if clean(entry.get("id"))}
    if len(artifacts) != EXPECTED_PROFILE_COUNT:
        raise ValueError(f"runtime artifact catalog must contain {EXPECTED_PROFILE_COUNT} ids, got {len(artifacts)}")
    if set(artifacts) != layer_sets[0]:
        missing = sorted(set(artifacts) - layer_sets[0])
        extra = sorted(layer_sets[0] - set(artifacts))
        raise ValueError(f"visual/runtime artifact ids differ; missing={missing}, extra={extra}")

    v118_extras = [
        entry for entry in layers[118].get("items", [])
        if entry.get("type") == LAYER_TYPES[118] and not clean(entry.get("catalog_id"))
    ]
    if [entry.get("id") for entry in v118_extras] != ["vis_ultra_void_cauldron"]:
        raise ValueError("v118 artifact visual layer must have only the known void-cauldron special sheet")

    source_hashes = {
        f"item_descriptions_v{version}.json": source_digest(SOURCE_DIR / f"item_descriptions_v{version}.json")
        for version in LAYER_TYPES
    }
    source_hashes[RUNTIME_CATALOG.name] = source_digest(RUNTIME_CATALOG)
    special = v118_extras[0]
    profiles: list[dict] = []
    for artifact_id in sorted(artifacts):
        artifact = artifacts[artifact_id]
        visual = indexes[118][artifact_id]
        performance = indexes[120][artifact_id]
        state_entry = indexes[121][artifact_id]
        look = indexes[122][artifact_id]
        states = {norm(key): clean(value) for key, value in state_entry.get("states", {}).items()}
        missing_states = REQUIRED_STATES - set(states)
        if missing_states:
            raise ValueError(f"{artifact_id}: missing required state keys {sorted(missing_states)}")
        silhouette = parse_silhouette(look)
        family = family_for(artifact, visual, states)
        motif = motif_for(artifact, visual, states)
        particle = particle_for(family, artifact, visual, states)
        trail = trail_for(artifact, visual, states, silhouette)
        if family not in FAMILY_VALUES or motif not in MOTIF_VALUES:
            raise ValueError(f"{artifact_id}: unsupported family/motif {family}/{motif}")
        if particle not in PARTICLE_VALUES or trail not in TRAIL_VALUES:
            raise ValueError(f"{artifact_id}: unsupported particle/trail {particle}/{trail}")
        source_ids = {
            "v118": clean(visual.get("id")),
            "v119": clean(indexes[119][artifact_id].get("id")),
            "v120": clean(performance.get("id")),
            "v121": clean(state_entry.get("id")),
            "v122": clean(look.get("id")),
        }
        authored_appearance = clean(visual.get("appearance"))
        authored_active = clean(visual.get("active_vfx"))
        if artifact_id == "void_heaven_cauldron_shard":
            authored_appearance = clean(special.get("appearance")) or authored_appearance
            authored_active = clean(special.get("active_vfx")) or authored_active
            source_ids["v118_ultra"] = clean(special.get("id"))
        profiles.append({
            "id": artifact_id,
            "runtime_id": f"seeking_immortals:{artifact_id}",
            "display": clean(artifact.get("display")),
            "type": norm(artifact.get("type")),
            "runtime_kind": (
                "material" if norm(artifact.get("type")) == "material_artifact"
                else "utility_deferred" if artifact_id == "space_rift_compass"
                else "activation"
            ),
            "tier": norm(artifact.get("tier")),
            "game_tier": int(artifact.get("game_tier", 0) or 0),
            "catalog": {
                "realm_min": clean(artifact.get("realm_min")) or None,
                "element": clean(artifact.get("element")) or None,
                "effect": clean(artifact.get("effect")) or None,
                "tags": artifact.get("tags") if isinstance(artifact.get("tags"), list) else [],
                "consumable": bool(artifact.get("consumable", False)),
                "uses": int(artifact.get("uses", 0) or 0),
            },
            "family": family,
            "motif": motif,
            "particle": particle,
            "trail": trail,
            "telegraphed": telegraphed_for(artifact, motif, visual, states),
            "appearance": authored_appearance,
            "semantic_active": authored_active,
            "performance": {
                "idle": clean(performance.get("idle_vfx")),
                "active": clean(performance.get("active_vfx")),
            },
            "look": {
                "silhouette": silhouette,
                "color": parse_color(look),
                "active": clean(look.get("active_vfx")),
            },
            "states": states,
            "state_count": len(states),
            "has_orbit": "orbit" in states,
            "has_launch": "launch" in states,
            "has_open": "open" in states,
            "has_reflect": "reflect" in states,
            "sources": source_ids,
        })

    return {
        "schema_version": 1,
        "description": "v118-v122 作者法宝外观、演出与状态机的紧凑运行时档案",
        "source_hashes": source_hashes,
        "constraints": {
            "max_concurrent_particle_systems": 4,
            "unique_language": "space_glitch",
            "forbid": ["rainbow_particle_rain", "thick_neon_sword_trail"],
            "required_states": sorted(REQUIRED_STATES),
        },
        "special_sources": {
            "void_heaven_cauldron_shard": {
                "id": special["id"],
                "appearance": clean(special.get("appearance")),
                "active": clean(special.get("active_vfx")),
                "description": clean(special.get("description")),
            }
        },
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
        print(f"authored artifact VFX profiles are current: {EXPECTED_PROFILE_COUNT}")
        return 0
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(content, encoding="utf-8")
    print(f"wrote {OUTPUT.relative_to(ROOT)} ({EXPECTED_PROFILE_COUNT} profiles)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
