#!/usr/bin/env python3
"""Compile the v118-v122 authored pill and consumable VFX layers.

The source corpus deliberately has two namespaces which share a few ids.  The
generated document therefore keeps ``pills`` and ``consumables`` separate and
uses the runtime pill effect catalog as the authority for pill semantics.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from collections import Counter
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
SOURCE_DIR = ROOT / "文本材料" / "data"
PILL_CATALOG = SOURCE_DIR / "pills_catalog.json"
CONSUMABLE_CATALOG = SOURCE_DIR / "consumables_catalog.json"
RUNTIME_PILL_CATALOG = (
    ROOT / "src/main/resources/data/seeking_immortals/alchemy/pill_effect_catalog.json"
)
OUTPUT = ROOT / "src/main/resources/data/seeking_immortals/visual/authored_consumable_vfx_profiles.json"

VERSIONS = (118, 119, 120, 121, 122)
PILL_LAYER_TYPES = {
    118: "pill_visual",
    119: "pill_icon",
    120: "pill_effect_sheet",
    121: "pill_frame_sheet",
    122: "pill_look_card",
}
CONSUMABLE_LAYER_TYPES = {
    118: "consumable_visual",
    120: "consumable_use_sheet",
    122: "consumable_look_card",
}
EXPECTED_PILL_COUNT = 114
EXPECTED_CONSUMABLE_COUNT = 57
EXPECTED_RUNTIME_PILL_COUNT = 113

# PillEffectCatalog deliberately keeps this historical spelling as an alias.
PILL_ALIASES = {"jiangying_pill": "jiangchen_pill"}

INFERRED_BREAKTHROUGH_TARGETS = {
    "foundation_pill": "foundation",
    "condensation_pill": "core_formation",
    "jiedan_pill": "core_formation",
    "ningyuan_pill": "core_formation",
    "ningjin_pill": "core_formation",
    "golden_core_pill": "core_formation",
    "nascent_soul_pill": "nascent_soul",
    "ningying_pill": "nascent_soul",
    "huashen_pill": "deity_transformation",
    "spirit_severing_pill": "deity_transformation",
    "void_condense_pill": "void_refinement",
}

GENERIC_EFFECT_BY_PILL_ID = {
    "bone_marrow_pill": "marrow_cleansing",
    "xiyu_pill": "marrow_cleansing",
    "ice_fire_pill": "elemental_cultivation",
    "xuanbing_pill": "elemental_cultivation",
    "lieyan_pill": "elemental_cultivation",
    "wuxing_pill": "elemental_cultivation",
    "pressure_resist_pill": "diyuan_adaptation",
    "diyuan_adapt_pill": "diyuan_adaptation",
    "spirit_realm_condense_pill": "high_tier_cultivation",
    "yangyuan_pill": "restorative_tonic",
    "jingxin_pill": "restorative_tonic",
    "jieqi_pill": "restorative_tonic",
    "huiyuan_pill": "restorative_tonic",
    "huanti_pill": "body_tempering",
    "huanxue_pill": "body_tempering",
    "body_refine_pill": "body_tempering",
    "meridian_open_pill": "body_tempering",
    "barbarian_strength_pill": "body_tempering",
    "wangchen_pill": "erase_memory_12h",
    "ghost_cultivate_pill": "toxic_cultivation",
    "demonic_blood_pill": "toxic_cultivation",
    "xueying_pill": "toxic_cultivation",
    "ghost_gate_pill": "toxic_cultivation",
    "poison_insect_pill": "toxic_cultivation",
    "blood_curse_pill": "toxic_cultivation",
    "hehuan_pill": "dual_cultivation_bonus",
    "illusion_pill": "illusion_tonic",
    "fox_illusion_pill": "illusion_tonic",
    "tribulation_guard_pill": "tribulation_guard",
    "tianling_pill": "legendary_essence",
    "biyu_pill": "jade_spirit_tonic",
    "juyuan_pill": "yuan_gathering",
    "longhu_pill": "dragon_tiger_temper",
    "spirit_condense_minor": "spirit_gather_tonic",
    "spirit_condense_pill": "spirit_gather_tonic",
    "juling_pill": "spirit_gather_tonic",
    "yin_yang_pill": "yin_yang_balance",
    "spirit_seed_pill": "spirit_seed_growth",
    "star_sea_pill": "star_sea_voyage",
    "huanglong_pill": "sect_spirit_tonic",
    "luoyun_spirit_pill": "sect_spirit_tonic",
    "ninghun_dan": "soul_heal",
    "huichun_pill": "hp_regen",
    "qingxin_pill": "heart_demon_reduce",
    "anti_poison_pill": "detox",
    "tianyuan_merit_pill": "merit_tonic",
}

FAMILY_VALUES = {
    "FIRE",
    "WATER",
    "METAL",
    "WOOD",
    "EARTH",
    "WIND",
    "ICE",
    "THUNDER",
    "LIGHT",
    "DARK",
    "SOUL",
    "BLOOD",
    "VOID",
    "ILLUSION",
    "NEUTRAL",
}
MOTIF_VALUES = {
    "GENERIC",
    "PROJECTILE",
    "BLADE",
    "SHIELD",
    "DOMAIN",
    "TELEPORT",
    "SUMMON",
    "WALL",
    "CHAIN",
    "CHANNEL",
    "RAIN",
    "HEAL",
    "CLEANSE",
    "SEAL",
    "FORMATION",
    "BUDDHIST",
    "CONFUCIAN",
    "DAO",
    "GHOST",
    "TALISMAN",
    "ILLUSION",
    "MARTIAL",
}
VFX_KIND_VALUES = {
    "CAST",
    "BURST",
    "PATH",
    "AURA",
    "SCAN",
    "BEAM",
    "CONE",
    "IMPACT",
    "FORMATION",
    "STATUS",
    "DISSIPATE",
}

# These are the authored names used by TechniqueVfxPacket.ParticleStyle and
# TrailStyle (the Java enum values are obtained by upper-casing these refs).
PARTICLE_VALUES = {
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
TRAIL_VALUES = {
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

NULL_WORDS = {"", "none", "null", "n/a", "na", "nil"}

FIRE_EFFECTS = {"fireball_cast"}
ICE_EFFECTS = {"ice_shard_cast"}
THUNDER_EFFECTS = {
    "lightning_mitigate_once",
    "formation_tribulation_buff",
    "tribulation_lightning_reduce",
}
TRAVEL_EFFECTS = {
    "forced_escape_once",
    "short_escape",
    "travel_spirit_boat",
    "travel_nether_ferry",
    "travel_chaotic_sea",
    "travel_diyuan",
    "board_teleport_array",
    "vehicle_craft",
}
PROJECTILE_EFFECTS = {"fireball_cast", "ice_shard_cast"}
FORMATION_EFFECTS = {"array_fuel", "deploy_spirit_gather_disk", "formation_tribulation_buff"}
SCAN_EFFECTS = {
    "reveal_spirit_roots",
    "discover_void_palace",
    "discover_fallen_demon",
    "discover_kunwu",
    "open_auction_invite",
    "show_sect_identity",
    "star_palace_patrol",
}
STORAGE_EFFECTS = {
    "portable_storage_9",
    "portable_storage_18",
    "portable_storage_27",
    "extra_inventory_slots_9",
    "extra_inventory_slots_18",
}
HEAL_EFFECTS = {
    "minor_spirit_regen",
    "restore_spirit",
    "restore_spirit_30pct",
    "restore_mana",
    "restore_mana_50pct",
    "restore_health",
    "hp_regen",
    "restore_soul_minor",
    "soul_heal",
    "save_life_self_damage",
}
CLEANSE_EFFECTS = {
    "clear_poison",
    "clear_cold_poison",
    "sea_poison_cure",
    "detox",
    "detox_minor",
    "purge_demon_qi",
    "reduce_demon_qi_stack",
    "demon_qi_resist",
    "yin_corruption_mitigate",
    "suppress_demon_qi_24h",
    "resist_demon_qi_corruption",
    "calm_inner_demon",
    "heart_demon_reduce",
    "heart_demon_resist",
    "killing_intent_control",
}
POISON_EFFECTS = {
    "power_now_lifespan_debt",
    "lethal_silent",
    "force_power_side_effect",
    "toxic_cultivation",
}
BUFF_EFFECTS = {
    "courage_buff_short",
    "satiation_plus_strength",
    "cultivation_speed_1h",
    "cultivation_speed_tianyuan",
    "blood_cultivation_boost",
    "movement_speed",
    "dual_cultivation_compatible",
    "dual_cultivation_bonus",
    "demonic_dual_cultivation",
    "pet_loyalty_plus",
    "pet_growth",
    "beast_contract_bonus",
    "physique_plus",
    "demon_body_temper",
    "body_tempering",
    "temp_mana_shield",
    "golden_armor",
}

# CatalogConsumableService/BulkItemClassifier supplies these effects when the
# authored consumables catalog leaves effect null or uses a generic alias.
CONSUMABLE_EFFECT_OVERRIDES = {
    "talisman_crate_low": "random_talisman_low",
    "talisman_crate_mid": "random_talisman_mid",
    "talisman_crate_high": "random_talisman_high",
    "inverse_star_smuggler_pack": "open_random_contraband",
    "spirit_stone_mid_bulk": "spirit_stone_mid_bundle",
    "spirit_stone_high_bulk": "spirit_stone_high_bundle",
    "escape_talisman": "short_escape",
    "fireball_talisman": "fireball_cast",
    "ice_shard_talisman": "ice_shard_cast",
    "golden_armor_talisman": "golden_armor",
    "beast_feed_spirit": "pet_loyalty_plus",
    "storage_bag_low": "portable_storage_9",
    "storage_bag_high": "portable_storage_27",
    "storage_pouch_low": "portable_storage_9",
    "storage_pouch_mid": "portable_storage_18",
    "spirit_water_flask": "restore_spirit",
    "healing_salve": "restore_health",
    "poison_antidote_pack": "clear_poison",
    "smoke_bomb_spirit": "smoke_screen",
    "sound_beacon": "sound_beacon",
    "detox_minor_pill": "detox_minor",
    "talisman_ink_bottle": "talisman_craft_material",
    "spirit_sand_pouch": "array_fuel",
    "yin_coffin_nail": "corpse_control",
    "wind_feather_raft_blueprint": "vehicle_craft",
    "spirit_boat_ticket": "travel_spirit_boat",
    "ferry_pass": "travel_nether_ferry",
    "teleport_talisman_chaotic_sea": "travel_chaotic_sea",
    "spirit_gathering_array_disk": "deploy_spirit_gather_disk",
    "auction_invitation": "open_auction_invite",
    "sect_identity_token": "show_sect_identity",
    "star_palace_tax_receipt": "star_palace_tax_paid",
    "star_palace_patrol_seal": "star_palace_patrol",
    "void_palace_map_fragment": "discover_void_palace",
    "fallen_demon_scout_report": "discover_fallen_demon",
    "kunwu_map_scroll": "discover_kunwu",
}


def read_json(path: Path) -> dict[str, Any]:
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise ValueError(f"unable to read JSON source {path}: {exc}") from exc
    if not isinstance(payload, dict):
        raise ValueError(f"JSON source must be an object: {path}")
    return payload


def source_digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def clean(value: object) -> str:
    if value is None:
        return ""
    text = str(value).strip()
    return "" if text.casefold() in NULL_WORDS else text


def norm(value: object) -> str:
    text = clean(value).lower().replace("-", "_").replace(" ", "_")
    return re.sub(r"_+", "_", text)


def merged_text(*values: object) -> str:
    result: list[str] = []
    for value in values:
        text = clean(value)
        if text and text not in result:
            result.append(text)
    return "；".join(result)


def catalog_index(payload: dict[str, Any], key: str, expected: int, label: str) -> dict[str, dict[str, Any]]:
    rows = payload.get(key)
    if not isinstance(rows, list):
        raise ValueError(f"{label} must contain a list at {key}")
    if len(rows) != expected:
        raise ValueError(f"{label} must contain {expected} rows, got {len(rows)}")
    result: dict[str, dict[str, Any]] = {}
    for row in rows:
        if not isinstance(row, dict):
            raise ValueError(f"{label} contains a non-object row")
        item_id = clean(row.get("id"))
        if not item_id:
            raise ValueError(f"{label} contains a row without id")
        if item_id in result:
            raise ValueError(f"{label} contains duplicate id {item_id}")
        result[item_id] = row
    return result


def layer_index(payload: dict[str, Any], entry_type: str, label: str) -> tuple[dict[str, dict[str, Any]], list[dict[str, Any]]]:
    rows = payload.get("items")
    if not isinstance(rows, list):
        raise ValueError(f"{label} must contain an items list")
    result: dict[str, dict[str, Any]] = {}
    extras: list[dict[str, Any]] = []
    for row in rows:
        if not isinstance(row, dict) or row.get("type") != entry_type:
            continue
        catalog_id = clean(row.get("catalog_id"))
        if not catalog_id:
            extras.append(row)
            continue
        if catalog_id in result:
            raise ValueError(f"{label}/{entry_type} contains duplicate catalog_id {catalog_id}")
        result[catalog_id] = row
    return result, extras


def validate_layers(
    layers: dict[int, dict[str, Any]],
    layer_types: dict[int, str],
    expected_ids: set[str],
    label: str,
    allow_v118_extras: bool = False,
) -> dict[int, dict[str, dict[str, Any]]]:
    indexes: dict[int, dict[str, dict[str, Any]]] = {}
    for version, entry_type in layer_types.items():
        index, extras = layer_index(layers[version], entry_type, f"v{version} {label}")
        if set(index) != expected_ids:
            missing = sorted(expected_ids - set(index))
            extra = sorted(set(index) - expected_ids)
            raise ValueError(f"v{version} {label} ids differ; missing={missing}, extra={extra}")
        if allow_v118_extras and version == 118:
            extra_ids = [clean(row.get("id")) for row in extras]
            if sorted(extra_ids) != ["vis_ultra_foundation_pill", "vis_ultra_jiangchen"]:
                raise ValueError(
                    "v118 pill_visual must contain exactly the foundation/jiangchen ultra sheets"
                )
        elif extras:
            raise ValueError(f"v{version} {label} has unexpected catalog-less entries")
        indexes[version] = index
    return indexes


def validate_runtime_pills(runtime_payload: dict[str, Any], pill_ids: set[str]) -> dict[str, dict[str, Any]]:
    rows = runtime_payload.get("entries")
    if not isinstance(rows, list) or len(rows) != EXPECTED_RUNTIME_PILL_COUNT:
        got = len(rows) if isinstance(rows, list) else "missing"
        raise ValueError(f"runtime pill_effect_catalog must contain {EXPECTED_RUNTIME_PILL_COUNT} entries, got {got}")
    result: dict[str, dict[str, Any]] = {}
    for row in rows:
        if not isinstance(row, dict):
            raise ValueError("runtime pill_effect_catalog contains a non-object entry")
        item_id = clean(row.get("pill_id"))
        if not item_id or item_id in result:
            raise ValueError(f"runtime pill_effect_catalog has invalid/duplicate pill_id {item_id!r}")
        category = clean(row.get("category"))
        effect = clean(row.get("effect"))
        if not category or not effect:
            raise ValueError(f"runtime pill {item_id} must have non-empty category and effect")
        result[item_id] = row
    expected_runtime_ids = pill_ids - set(PILL_ALIASES)
    if set(result) != expected_runtime_ids:
        raise ValueError(
            "runtime pill ids differ after aliases; "
            f"missing={sorted(expected_runtime_ids - set(result))}, "
            f"extra={sorted(set(result) - expected_runtime_ids)}"
        )
    for alias, canonical in PILL_ALIASES.items():
        if canonical not in result:
            raise ValueError(f"pill alias {alias} points to missing runtime id {canonical}")
    return result


def validate_particle_bible(layer: dict[str, Any]) -> None:
    systems: dict[str, Any] | None = None
    trails: dict[str, Any] | None = None
    for row in layer.get("items", []):
        if not isinstance(row, dict):
            continue
        if row.get("id") == "particle_system_bible_v121":
            systems = row.get("systems")
        elif row.get("id") == "trail_ribbon_styles_v121":
            trails = row.get("trails")
    if not isinstance(systems, dict) or not PARTICLE_VALUES.issubset(systems):
        raise ValueError("v121 particle bible is missing an authored particle reference")
    if not isinstance(trails, dict) or not (TRAIL_VALUES - {"none"}).issubset(trails):
        raise ValueError("v121 trail bible is missing an authored trail reference")


def resolve_generic_pill_effect(
    pill_id: str,
    category: str,
    realm_target: str,
    spirit_gain: int,
    tags: set[str],
) -> str:
    tagged = (
        ("death_substitute_once", "death_substitute_once"),
        ("tribulation_aid", "tribulation_breakthrough_aid"),
        ("diyuan_debuff_reduce", "diyuan_adaptation"),
        ("soul_heal", "soul_heal"),
        ("hp_regen", "hp_regen"),
        ("heart_demon_reduce", "heart_demon_reduce"),
        ("detox", "detox"),
    )
    for tag, effect in tagged:
        if tag in tags:
            return effect
    if spirit_gain > 0:
        return "spirit_gain_flat"
    if category == "breakthrough" and realm_target:
        return "targeted_breakthrough_aid"
    if pill_id.startswith("cultivation_aid_"):
        return "realm_cultivation_aid"
    explicit = GENERIC_EFFECT_BY_PILL_ID.get(pill_id)
    if explicit:
        return explicit
    return {
        "breakthrough": "tribulation_guard",
        "recovery": "restorative_tonic",
        "poison": "toxic_cultivation",
        "legendary": "legendary_essence",
        "special": "special_tonic",
    }.get(category, "cultivation_progress")


def runtime_pill(runtime: dict[str, Any], design: dict[str, Any], pill_id: str) -> tuple[str, str]:
    canonical_id = PILL_ALIASES.get(pill_id, pill_id)
    row = runtime.get(canonical_id)
    if row is None:
        raise ValueError(f"no runtime pill entry for {pill_id} (canonical {canonical_id})")
    # Mirror PillEffectCatalog.mergeDesignMetadata/resolveGenericEffect exactly:
    # the runtime resource supplies the base effect while design metadata owns
    # category, tags, targets and flat-spirit overrides.
    category = norm(design.get("category")) or norm(row.get("category")) or "cultivation"
    effect = norm(row.get("effect")) or "generic_cultivation"
    realm_target = norm(design.get("realm_target")) or INFERRED_BREAKTHROUGH_TARGETS.get(pill_id, "")
    try:
        spirit_gain = int(design.get("spirit_gain_flat") or 0)
    except (TypeError, ValueError):
        spirit_gain = 0
    raw_tags = design.get("effect_tags")
    tags = {norm(tag) for tag in raw_tags} if isinstance(raw_tags, list) else set()
    if not effect or effect == "generic_cultivation":
        effect = resolve_generic_pill_effect(pill_id, category, realm_target, spirit_gain, tags)
    return category, effect


def runtime_consumable_effect(item_id: str, row: dict[str, Any]) -> str:
    if item_id in CONSUMABLE_EFFECT_OVERRIDES:
        return CONSUMABLE_EFFECT_OVERRIDES[item_id]
    effect = norm(row.get("effect"))
    # The two overlapping ids are intentionally retained in this array.  Their
    # pill-side runtime route is represented independently in ``pills``.
    if item_id in {"bigu_pill", "spirit_stone_low"} and not effect:
        return ""
    return effect


def semantic_blob(item_id: str, category: str, effect: str, design: dict[str, Any]) -> str:
    element = norm(design.get("element"))
    return norm(" ".join(part for part in (item_id, category, effect, element) if part))


def family_for(item_id: str, category: str, effect: str, design: dict[str, Any]) -> str:
    blob = semantic_blob(item_id, category, effect, design)
    element = norm(design.get("element"))
    if item_id == "foundation_pill":
        return "LIGHT"
    if item_id in {"jiangchen_pill", "jiangying_pill"}:
        return "EARTH"
    if element in {"fire", "flame", "yang"} or effect in FIRE_EFFECTS or any(
        token in blob for token in ("fireball", "lieyan", "huoyuan")
    ):
        return "FIRE"
    if element in {"ice", "frost", "cold", "ice_fire"} or effect in ICE_EFFECTS or any(
        token in blob for token in ("ice_shard", "xuanbing", "snow_lotus", "cold_poison")
    ):
        return "ICE"
    if effect in THUNDER_EFFECTS or any(token in blob for token in ("thunder", "lightning", "tribulation", "避雷")):
        return "THUNDER"
    if effect in TRAVEL_EFFECTS or category in {"travel", "storage"} or any(
        token in blob for token in ("teleport", "escape", "space", "void", "rift", "map_fragment")
    ):
        return "VOID" if any(token in blob for token in ("teleport", "void", "space", "map", "escape")) else "WIND"
    if effect in {"illusion_tonic", "smoke_screen"} or "illusion" in blob or "mirage" in blob:
        return "ILLUSION"
    if effect in {"restore_soul_minor", "soul_heal"} or any(
        token in blob for token in ("soul", "ghost", "corpse", "yin_", "yin")
    ):
        return "SOUL"
    if category == "poison" or effect in POISON_EFFECTS or any(
        token in blob for token in ("poison", "toxic", "blood", "curse", "marrow_extract")
    ):
        return "BLOOD"
    if any(token in blob for token in ("demon", "demonic", "demon_qi", "yin_corruption")):
        return "DARK"
    if effect in CLEANSE_EFFECTS:
        return "LIGHT"
    if effect in HEAL_EFFECTS:
        return "LIGHT" if effect in {"restore_health", "hp_regen", "save_life_self_damage"} else "WATER"
    if category in {"food", "pet", "beast"} or any(token in blob for token in ("beast", "herb", "fertilizer")):
        return "WOOD"
    if effect in {"movement_speed", "vehicle_craft"} or "wind" in blob:
        return "WIND"
    if category in {"formation_consumable", "craft"} or any(token in blob for token in ("array", "formation", "sand")):
        return "EARTH"
    if category in {"currency", "currency_bundle"} or any(token in blob for token in ("stone", "currency", "tax")):
        return "METAL"
    if effect in {"body_tempering", "physique_plus", "demon_body_temper"} or any(
        token in blob for token in ("body", "barbarian", "marrow", "longhu")
    ):
        return "EARTH"
    if category == "legendary" or any(token in blob for token in ("root_purify", "longevity", "merit")):
        return "LIGHT"
    return "NEUTRAL"


def motif_for(item_id: str, category: str, effect: str, family: str) -> str:
    blob = norm(" ".join((item_id, category, effect)))
    if effect in PROJECTILE_EFFECTS or category == "talisman_consumable":
        return "TALISMAN" if category == "talisman_consumable" else "PROJECTILE"
    if effect in TRAVEL_EFFECTS or category == "travel":
        return "TELEPORT"
    if effect in FORMATION_EFFECTS or category == "formation_consumable":
        return "FORMATION"
    if effect in SCAN_EFFECTS or category == "quest" and any(
        token in blob for token in ("map", "report", "receipt", "invitation", "patrol", "identity", "discover")
    ):
        return "DAO"
    if effect in STORAGE_EFFECTS or category in {"storage", "loot", "currency_bundle"}:
        return "DOMAIN"
    if effect in CLEANSE_EFFECTS or any(token in blob for token in ("antidote", "detox", "purge", "ward", "guard")):
        return "CLEANSE"
    if effect in {"temp_mana_shield", "golden_armor", "yin_corruption_mitigate", "demon_qi_resist"}:
        return "SHIELD"
    if effect in HEAL_EFFECTS or category in {"recovery", "food"}:
        return "HEAL"
    if category in {"beast", "pet"} or effect in {"pet_growth", "pet_loyalty_plus", "beast_contract_bonus"}:
        return "SUMMON"
    if category == "poison" or effect in POISON_EFFECTS:
        return "GHOST" if family == "SOUL" else "CHANNEL"
    if effect in {"physique_plus", "body_tempering", "demon_body_temper", "satiation_plus_strength"}:
        return "MARTIAL"
    if category == "breakthrough" or item_id in {"foundation_pill", "jiangchen_pill", "jiangying_pill"}:
        return "CHANNEL"
    if category == "craft" or "talisman" in blob:
        return "TALISMAN"
    if effect in {"smoke_screen", "illusion_tonic"} or family == "ILLUSION":
        return "ILLUSION"
    if family in {"DARK", "SOUL"}:
        return "GHOST"
    return "GENERIC"


def vfx_kind_for(item_id: str, category: str, effect: str, motif: str) -> str:
    if effect in PROJECTILE_EFFECTS:
        return "CAST"
    if effect in TRAVEL_EFFECTS or category == "travel":
        return "PATH"
    if effect in FORMATION_EFFECTS or category == "formation_consumable":
        return "FORMATION"
    if effect in SCAN_EFFECTS:
        return "SCAN"
    if effect in STORAGE_EFFECTS or category in {"storage", "loot", "currency_bundle"}:
        return "BURST"
    if category in {"breakthrough", "legendary"} or effect in BUFF_EFFECTS or category == "cultivation":
        return "AURA"
    if category in {"poison", "tribulation", "ghost_path"} or effect in POISON_EFFECTS:
        return "STATUS"
    if effect in HEAL_EFFECTS or effect in CLEANSE_EFFECTS or category in {"recovery", "food", "pet", "beast"}:
        return "BURST"
    if effect in {"smoke_screen", "sound_beacon"}:
        return "BURST"
    if motif in {"DOMAIN", "SUMMON", "TALISMAN"}:
        return "BURST"
    return "STATUS"


def particle_for(family: str, motif: str, effect: str, category: str) -> str:
    if motif == "HEAL" or family == "LIGHT":
        return "heal_motes"
    if family == "VOID":
        return "space_glitch"
    if family == "FIRE":
        return "fire_ember"
    if family == "WATER":
        return "water_mist"
    if family == "METAL":
        return "metal_spark"
    if family == "WOOD":
        return "wood_pollen"
    if family == "EARTH":
        return "earth_dust"
    if family == "WIND":
        return "qi_soft"
    if family == "ICE":
        return "water_mist"
    if family == "THUNDER":
        return "thunder_arc"
    if family == "DARK":
        return "yin_smoke"
    if family == "SOUL":
        return "soul_wisps"
    if family == "BLOOD":
        return "blood_mist"
    if family == "ILLUSION":
        return "soul_wisps"
    return "qi_soft"


def trail_for(family: str, motif: str, vfx_kind: str, category: str) -> str:
    if vfx_kind == "PATH":
        return "movement_wind"
    if motif == "TALISMAN":
        return "talisman_ash"
    if family == "THUNDER":
        return "thunder_jagged"
    if family == "BLOOD":
        return "blood_ribbon"
    if family == "SOUL":
        return "soul_afterimage"
    if motif == "BLADE":
        return "sword_thin"
    return "none"


def telegraphed_for(item_id: str, category: str, effect: str, vfx_kind: str) -> bool:
    if category in {"breakthrough", "legendary", "poison", "tribulation"}:
        return True
    if effect in PROJECTILE_EFFECTS | FORMATION_EFFECTS | {
        "ascension_tribulation_aid",
        "tribulation_lightning_reduce",
        "lethal_silent",
        "force_power_side_effect",
        "demon_body_temper",
    }:
        return True
    if vfx_kind in {"CAST", "FORMATION"}:
        return True
    return item_id in {"foundation_pill", "jiangchen_pill", "jiangying_pill", "dujie_pill"}


def radius_for(category: str, effect: str, vfx_kind: str, telegraphed: bool) -> float:
    radius = {
        "CAST": 0.9,
        "PATH": 1.2,
        "FORMATION": 2.2,
        "SCAN": 1.5,
        "AURA": 1.05,
        "BURST": 0.9,
        "STATUS": 0.75,
    }.get(vfx_kind, 0.85)
    if category in {"breakthrough", "legendary"}:
        radius += 0.35
    if category in {"storage", "loot", "currency_bundle"}:
        radius += 0.2
    if telegraphed:
        radius += 0.15
    return round(min(8.0, max(0.25, radius)), 2)


def intensity_for(category: str, effect: str, vfx_kind: str, telegraphed: bool) -> int:
    intensity = {
        "CAST": 30,
        "PATH": 28,
        "FORMATION": 36,
        "SCAN": 24,
        "AURA": 26,
        "BURST": 24,
        "STATUS": 20,
    }.get(vfx_kind, 20)
    if category in {"breakthrough", "legendary"}:
        intensity += 8
    if category in {"poison", "tribulation"}:
        intensity += 4
    if telegraphed:
        intensity += 4
    return min(96, max(1, intensity))


def semantic_fields(item_id: str, category: str, effect: str, design: dict[str, Any]) -> dict[str, Any]:
    family = family_for(item_id, category, effect, design)
    motif = motif_for(item_id, category, effect, family)
    vfx_kind = vfx_kind_for(item_id, category, effect, motif)
    particle = particle_for(family, motif, effect, category)
    trail = trail_for(family, motif, vfx_kind, category)
    telegraphed = telegraphed_for(item_id, category, effect, vfx_kind)
    if family not in FAMILY_VALUES or motif not in MOTIF_VALUES or vfx_kind not in VFX_KIND_VALUES:
        raise ValueError(f"{item_id}: unsupported visual enum {family}/{motif}/{vfx_kind}")
    if particle not in PARTICLE_VALUES or particle == "DEFAULT":
        raise ValueError(f"{item_id}: unsupported/default particle {particle}")
    if trail not in TRAIL_VALUES or trail == "DEFAULT":
        raise ValueError(f"{item_id}: unsupported/default trail {trail}")
    return {
        "family": family,
        "motif": motif,
        "vfx_kind": vfx_kind,
        "particle": particle,
        "trail": trail,
        "telegraphed": telegraphed,
        "radius": radius_for(category, effect, vfx_kind, telegraphed),
        "intensity": intensity_for(category, effect, vfx_kind, telegraphed),
    }


def pill_frames(frame: dict[str, Any], item_id: str) -> list[str]:
    raw = frame.get("frames")
    if not isinstance(raw, list) or not raw:
        raise ValueError(f"{item_id}: v121 pill_frame_sheet has no frames")
    steps = [entry.get("step") for entry in raw if isinstance(entry, dict)]
    if steps != list(range(1, len(raw) + 1)):
        raise ValueError(f"{item_id}: v121 frame steps are not contiguous")
    result = [clean(entry.get("vis")) for entry in raw if isinstance(entry, dict)]
    if len(result) != len(raw) or any(not value for value in result):
        raise ValueError(f"{item_id}: v121 contains an empty frame description")
    return result


def source_ids(indexes: dict[int, dict[str, dict[str, Any]]], item_id: str) -> dict[str, str]:
    return {f"v{version}": clean(indexes[version][item_id].get("id")) for version in sorted(indexes)}


def make_pill_profile(
    item_id: str,
    design: dict[str, Any],
    runtime: dict[str, dict[str, Any]],
    indexes: dict[int, dict[str, dict[str, Any]]],
    ultra: dict[str, dict[str, Any]],
) -> dict[str, Any]:
    category, effect = runtime_pill(runtime, design, item_id)
    visual = indexes[118][item_id]
    overview = indexes[120][item_id]
    frame = indexes[121][item_id]
    look = indexes[122][item_id]
    semantic = semantic_fields(item_id, category, effect, design)
    ultra_row = ultra.get(item_id, {})
    appearance = merged_text(
        ultra_row.get("appearance"),
        visual.get("appearance"),
        look.get("appearance"),
    )
    active = (
        clean(ultra_row.get("active_vfx"))
        or clean(overview.get("active_vfx"))
        or clean(visual.get("active_vfx"))
        or clean(look.get("active_vfx"))
        or f"{effect} 使用反馈"
    )
    body = clean(overview.get("body_vfx")) or active
    ui = clean(overview.get("ui_vfx")) or "使用反馈"
    profile = {
        "id": item_id,
        "category": category,
        "effect": effect,
        **semantic,
        "appearance": appearance,
        "active_vfx": active,
        "body_vfx": body,
        "ui_vfx": ui,
        "frames": pill_frames(frame, item_id),
        "frame_count": len(frame["frames"]),
        "sources": source_ids(indexes, item_id),
    }
    if ultra_row:
        profile["sources"]["v118_ultra"] = clean(ultra_row.get("id"))
    return profile


def consumable_frames(active: str, vfx_kind: str) -> list[str]:
    return [
        "取出并启封，材质轮廓清晰",
        active or "使用反馈",
        "效果落定，持续状态以脚底淡环提示" if vfx_kind in {"AURA", "STATUS"} else "灵光收束",
    ]


def make_consumable_profile(
    item_id: str,
    design: dict[str, Any],
    indexes: dict[int, dict[str, dict[str, Any]]],
) -> dict[str, Any]:
    visual = indexes[118][item_id]
    overview = indexes[120][item_id]
    look = indexes[122][item_id]
    category = norm(design.get("category"))
    effect = runtime_consumable_effect(item_id, design)
    semantic = semantic_fields(item_id, category, effect, design)
    appearance = merged_text(visual.get("appearance"), overview.get("appearance"), look.get("appearance"))
    active = clean(overview.get("active_vfx")) or clean(visual.get("active_vfx")) or clean(look.get("active_vfx"))
    body = clean(overview.get("body_vfx")) or active or "使用后效果反馈"
    ui = clean(overview.get("ui_vfx")) or (
        "持续效果脚底淡环" if semantic["vfx_kind"] in {"AURA", "STATUS"} else "一次性使用反馈"
    )
    frames = consumable_frames(active, semantic["vfx_kind"])
    return {
        "id": item_id,
        "category": category,
        "effect": effect,
        **semantic,
        "appearance": appearance,
        "active_vfx": active,
        "body_vfx": body,
        "ui_vfx": ui,
        "frames": frames,
        "frame_count": len(frames),
        "sources": source_ids(indexes, item_id),
    }


def compile_profiles() -> dict[str, Any]:
    description_layers = {
        version: read_json(SOURCE_DIR / f"item_descriptions_v{version}.json") for version in VERSIONS
    }
    pill_payload = read_json(PILL_CATALOG)
    consumable_payload = read_json(CONSUMABLE_CATALOG)
    runtime_payload = read_json(RUNTIME_PILL_CATALOG)
    pill_catalog = catalog_index(pill_payload, "pills", EXPECTED_PILL_COUNT, "pills_catalog.json")
    consumable_catalog = catalog_index(
        consumable_payload, "consumables", EXPECTED_CONSUMABLE_COUNT, "consumables_catalog.json"
    )
    pill_ids = set(pill_catalog)
    consumable_ids = set(consumable_catalog)
    pill_indexes = validate_layers(
        description_layers,
        PILL_LAYER_TYPES,
        pill_ids,
        "pill",
        allow_v118_extras=True,
    )
    consumable_layers = {version: description_layers[version] for version in CONSUMABLE_LAYER_TYPES}
    consumable_indexes = validate_layers(
        consumable_layers,
        CONSUMABLE_LAYER_TYPES,
        consumable_ids,
        "consumable",
    )
    validate_particle_bible(description_layers[121])
    runtime = validate_runtime_pills(runtime_payload, pill_ids)
    ultra_rows = [
        row
        for row in description_layers[118].get("items", [])
        if row.get("type") == "pill_visual" and not clean(row.get("catalog_id"))
    ]
    ultra = {
        "foundation_pill": next(row for row in description_layers[118]["items"] if row.get("id") == "vis_ultra_foundation_pill"),
        "jiangchen_pill": next(row for row in description_layers[118]["items"] if row.get("id") == "vis_ultra_jiangchen"),
    }
    if set(clean(row.get("id")) for row in ultra_rows) != {
        "vis_ultra_foundation_pill",
        "vis_ultra_jiangchen",
    }:
        raise ValueError("v118 pill ultra source rows changed unexpectedly")

    pills = [
        make_pill_profile(item_id, pill_catalog[item_id], runtime, pill_indexes, ultra)
        for item_id in sorted(pill_ids)
    ]
    consumables = [
        make_consumable_profile(item_id, consumable_catalog[item_id], consumable_indexes)
        for item_id in sorted(consumable_ids)
    ]
    source_paths = [
        *(SOURCE_DIR / f"item_descriptions_v{version}.json" for version in VERSIONS),
        PILL_CATALOG,
        CONSUMABLE_CATALOG,
        RUNTIME_PILL_CATALOG,
    ]
    source_hashes = {path.name: source_digest(path) for path in source_paths}
    return {
        "schema_version": 1,
        "description": "v118-v122 作者丹药与消耗品外观、服用/使用演出运行时档案",
        "pill_profile_count": len(pills),
        "consumable_profile_count": len(consumables),
        "source_hashes": source_hashes,
        "constraints": {
            "max_concurrent_particle_systems": 4,
            "max_vignette_seconds": 2.0,
            "unique_language": "space_glitch",
            "particle_whitelist": sorted(PARTICLE_VALUES),
            "trail_whitelist": sorted(TRAIL_VALUES),
            "forbid": ["DEFAULT", "rainbow_particle_rain", "thick_neon_sword_trail"],
            "pill_aliases": dict(sorted(PILL_ALIASES.items())),
            "separate_arrays_for_overlapping_ids": ["bigu_pill", "demon_qi_purge_pill"],
        },
        "pills": pills,
        "consumables": consumables,
    }


def encoded_output(document: dict[str, Any]) -> str:
    return json.dumps(document, ensure_ascii=False, indent=2) + "\n"


def print_distribution(document: dict[str, Any]) -> None:
    for key in ("pills", "consumables"):
        rows = document[key]
        family = Counter(row["family"] for row in rows)
        kind = Counter(row["vfx_kind"] for row in rows)
        particle = Counter(row["particle"] for row in rows)
        print(f"{key}: {len(rows)} profiles")
        print("  family=" + ", ".join(f"{name}:{family[name]}" for name in sorted(family)))
        print("  vfx_kind=" + ", ".join(f"{name}:{kind[name]}" for name in sorted(kind)))
        print("  particle=" + ", ".join(f"{name}:{particle[name]}" for name in sorted(particle)))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="fail if the generated JSON is stale")
    args = parser.parse_args()
    document = compile_profiles()
    content = encoded_output(document)
    if args.check:
        if not OUTPUT.exists() or OUTPUT.read_text(encoding="utf-8") != content:
            print(f"stale generated file: {OUTPUT.relative_to(ROOT)}")
            return 1
        print(
            "authored consumable VFX profiles are current: "
            f"{document['pill_profile_count']} pills, {document['consumable_profile_count']} consumables"
        )
        print_distribution(document)
        return 0
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(content, encoding="utf-8")
    print(
        f"wrote {OUTPUT.relative_to(ROOT)} "
        f"({document['pill_profile_count']} pills, {document['consumable_profile_count']} consumables)"
    )
    print_distribution(document)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
