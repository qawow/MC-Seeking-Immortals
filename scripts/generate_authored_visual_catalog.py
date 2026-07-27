#!/usr/bin/env python3
"""Compile every authored visual domain into one typed runtime catalog.

Technique visuals come from the complete spell-effect compiler. Other historical
domains retain their source-compatible projections. Runtime code therefore reads
enums, numbers, and resource references without interpreting author prose.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path
from typing import Any, Iterable


ROOT = Path(__file__).resolve().parents[1]
SOURCE_DIR = ROOT / "文本材料" / "data"
VISUAL_DIR = ROOT / "src/main/resources/data/seeking_immortals/visual"
RUNTIME_DIR = ROOT / "src/main/resources/data/seeking_immortals"
RUNTIME_TEXT_DIR = RUNTIME_DIR / "text_material"
WORLD_DIR = RUNTIME_DIR / "worldpack"
CATALOG_DIR = RUNTIME_DIR / "catalog"
OUTPUT = VISUAL_DIR / "authored_visual_catalog.json"
CROSSWALK = ROOT / "scripts/authored_visual_crosswalk.json"

OLD_SOURCES = {
    "TECHNIQUE": VISUAL_DIR / "authored_spell_effects.json",
    "ARTIFACT": VISUAL_DIR / "authored_artifact_vfx_profiles.json",
    "PILL": VISUAL_DIR / "authored_consumable_vfx_profiles.json",
    "CONSUMABLE": VISUAL_DIR / "authored_consumable_vfx_profiles.json",
}
RAW_TYPES = {
    "TECHNIQUE": (121, "technique_frame_sheet"),
    "ARTIFACT": (121, "artifact_state_visual"),
    "PILL": (121, "pill_frame_sheet"),
    "CONSUMABLE": (120, "consumable_use_sheet"),
}
DOMAIN_ORDER = {
    "TECHNIQUE": 0, "ARTIFACT": 1, "PILL": 2, "CONSUMABLE": 3,
    "METHOD": 4, "HERB": 5, "MATERIAL": 6, "BEAST": 7, "NPC": 8,
    "REALM": 9, "ZONE": 10, "BOSS": 11, "STATUS": 12, "STRUCTURE": 13,
    "VEHICLE": 14, "FORMATION": 15, "TRIBULATION": 16,
}
CANONICAL_COUNTS = {
    "TECHNIQUE": 0, "ARTIFACT": 217, "PILL": 114, "CONSUMABLE": 57,
    "METHOD": 136, "HERB": 79, "MATERIAL": 457, "BEAST": 1890,
    "NPC": 179, "REALM": 19, "ZONE": 75, "BOSS": 27, "STATUS": 22,
    "STRUCTURE": 92, "VEHICLE": 8, "FORMATION": 56, "TRIBULATION": 7,
}
GENERIC_TAGS = {
    "visual_v118", "visual_v119", "visual_v120", "visual_v121", "visual_v122",
    "technique", "frames", "storyboard",
}

PARTICLE_REFS = {
    "qi_soft", "fire_ember", "water_mist", "wood_pollen", "metal_spark", "earth_dust",
    "thunder_arc", "yin_smoke", "soul_wisps", "blood_mist", "heal_motes", "space_glitch",
}
TRAIL_REFS = {
    "none", "sword_thin", "heavy_weapon", "flying_sword_orbit", "talisman_ash",
    "blood_ribbon", "thunder_jagged", "soul_afterimage", "movement_wind",
}

TRIGGER_VALUES = {
    "PRE": "TELEGRAPH",
    "大招预兆": "TELEGRAPH",
    "起手": "ANTICIPATION",
    "成型": "FORMATION",
    "出击": "RELEASE",
    "命中": "IMPACT",
    "回收": "DECAY",
}

PALETTE_FAMILY = {
    "FIRE": "fire", "WATER": "water", "ICE": "water", "WOOD": "wood",
    "METAL": "metal", "EARTH": "earth", "THUNDER": "thunder", "SOUL": "soul",
    "DARK": "yin", "BLOOD": "poison", "LIGHT": "heal", "ILLUSION": "soul",
    "VOID": "qi", "WIND": "qi", "NEUTRAL": "qi",
}

# These tokens are the authored color vocabulary.  Values come from v118's
# palette rather than invented renderer colors.  Ordering handles composite
# phrases such as 绿金 before the single-character tokens.
COLOR_TOKEN_PALETTE = (
    ("绿金", "heal"), ("疗愈", "heal"), ("治愈", "heal"),
    ("诡绿", "poison"), ("毒", "poison"),
    ("紫金", "thunder"), ("紫电", "thunder"), ("雷", "thunder"),
    ("幽紫", "yin"), ("阴", "yin"), ("魂", "soul"), ("灵魂", "soul"),
    ("火", "fire"), ("赤", "fire"), ("橙红", "fire"),
    ("水", "water"), ("澄蓝", "water"), ("蓝白", "water"),
    ("木", "wood"), ("碧", "wood"), ("青绿", "wood"),
    ("金属", "metal"), ("银金", "metal"), ("金", "metal"),
    ("土", "earth"), ("褐", "earth"), ("尘", "earth"),
    ("冰", "water"), ("寒", "water"), ("青白", "qi"), ("淡青", "qi"),
)

NEW_DOMAIN_SOURCES = {
    "METHOD": (RUNTIME_TEXT_DIR / "cultivation_methods.json", "methods"),
    "HERB": (RUNTIME_TEXT_DIR / "spirit_herbs_catalog.json", "herbs"),
    # The source material contains the merged v135 rows (461 rows / 457 IDs),
    # while the shipped compact copy intentionally contains only the first
    # visual batch.  The prose file is therefore the canonical material list.
    "MATERIAL": (SOURCE_DIR / "materials_catalog.json", "materials"),
    "BEAST": (RUNTIME_TEXT_DIR / "beast_bestiary_runtime.json", "creatures"),
    "NPC": (RUNTIME_TEXT_DIR / "named_npcs_v116.json", "npcs"),
    "REALM": (WORLD_DIR / "secret_realm_runtime.json", "realms"),
    "ZONE": (WORLD_DIR / "secret_realm_runtime.json", "realms.layers"),
    "BOSS": (WORLD_DIR / "boss_loot_runtime.json", "tables"),
    "STATUS": (RUNTIME_TEXT_DIR / "status_effects.json", "effects"),
    "STRUCTURE": (RUNTIME_TEXT_DIR / "multiblock_structure_index.json", "entries"),
    "VEHICLE": (RUNTIME_TEXT_DIR / "flight_vehicles.json", "vehicles"),
    "FORMATION": (RUNTIME_TEXT_DIR / "formation_field_params.json", "fields"),
    "TRIBULATION": (SOURCE_DIR / "tribulation_rules.json", "types"),
}

# NPC seeds add eleven IDs to the v116 named-NPC list.  Keeping the seed file
# separate in source_hashes makes the merge auditable while preserving the
# runtime's stable canonical ID set.
NPC_SEED_SOURCE = RUNTIME_TEXT_DIR / "named_npc_seeds_v137.json"
STRUCTURE_STATE_SOURCE = RUNTIME_TEXT_DIR / "multiblock_operational_states_v134.json"
FORMATION_ARRAY_SOURCE = RUNTIME_TEXT_DIR / "formation_array_catalog_v97.json"
VISUAL_SOURCE_TYPES = {
    "METHOD": ("method_visual", "method_aura_sheet", "method_look_card"),
    "HERB": ("herb_visual", "herb_visual_sheet", "herb_growth_visual", "herb_look_card"),
    "MATERIAL": ("material_visual", "material_visual_sheet", "forge_stage_visual", "material_look_card"),
    "BEAST": ("beast_visual", "beast_portrait", "beast_anim_sheet", "beast_hitloc_visual", "beast_look_card"),
    "NPC": ("npc_visual", "npc_portrait", "npc_expression_sheet", "npc_look_card"),
    "REALM": ("realm_visual", "realm_lighting"),
    "ZONE": ("zone_visual",),
    "BOSS": ("boss_storyboard", "boss_style_card"),
    "STATUS": ("effect_style_card",),
    "STRUCTURE": ("prop_visual",),
}

DOMAIN_LIFECYCLES = {
    "METHOD": ("train_enter", "train_pulse", "combat_ready", "combat_cast", "decay"),
    "HERB": ("unripe", "mature", "harvested", "processing"),
    "MATERIAL": ("idle", "held", "processing", "inserted", "depleted"),
    "BEAST": ("idle", "aggro", "attack", "hit", "death"),
    "NPC": ("idle", "talk", "trade", "hostile", "depart"),
    "REALM": ("enter", "ambient", "combat", "exit"),
    "ZONE": ("enter", "ambient", "threat", "exit"),
    "BOSS": ("p1", "p2", "p3", "telegraph", "attack", "weak", "death"),
    "STATUS": ("applied", "tick_stack", "dispel", "expire"),
    "STRUCTURE": ("intact", "damaged", "critical", "disabled"),
    "VEHICLE": ("docked", "board", "cruise", "boost", "damage", "dock"),
    "FORMATION": ("undeployed", "deploying", "active", "pulse", "disrupted", "dismantled"),
    "TRIBULATION": ("announce", "wave", "intermission", "failure", "success"),
}

FAMILY_PARTICLE = {
    "FIRE": "fire_ember", "WATER": "water_mist", "ICE": "water_mist",
    "WOOD": "wood_pollen", "METAL": "metal_spark", "EARTH": "earth_dust",
    "THUNDER": "thunder_arc", "SOUL": "soul_wisps", "DARK": "yin_smoke",
    "BLOOD": "blood_mist", "LIGHT": "heal_motes", "ILLUSION": "soul_wisps",
    "VOID": "space_glitch", "WIND": "qi_soft", "NEUTRAL": "qi_soft",
}
FAMILY_TRAIL = {
    "FIRE": "none", "WATER": "none", "ICE": "none", "WOOD": "none",
    "METAL": "sword_thin", "EARTH": "heavy_weapon", "THUNDER": "thunder_jagged",
    "SOUL": "soul_afterimage", "DARK": "soul_afterimage", "BLOOD": "blood_ribbon",
    "LIGHT": "none", "ILLUSION": "soul_afterimage", "VOID": "none", "WIND": "movement_wind",
    "NEUTRAL": "none",
}

# Runtime renderer enums are intentionally finite.  Authored role/category
# labels are normalized here so the catalog never silently degrades to a
# generic client style merely because a source row used prose vocabulary.
RUNTIME_FAMILIES = frozenset({
    "FIRE", "WATER", "METAL", "WOOD", "EARTH", "WIND", "ICE", "THUNDER",
    "LIGHT", "DARK", "SOUL", "BLOOD", "VOID", "ILLUSION", "NEUTRAL",
})
FAMILY_ALIASES = {
    "QI": "NEUTRAL", "FLAME": "FIRE", "YIN": "DARK", "HEAL": "LIGHT",
    "YANG": "LIGHT", "DEMON": "DARK", "DEMONIC": "DARK", "SPACE": "VOID",
    "SPATIAL": "VOID", "GHOST": "SOUL", "SPIRIT": "SOUL", "MOVEMENT": "WIND",
    "SWORD": "METAL", "PUPPET": "METAL", "BUDDHIST": "LIGHT", "RECOVERY": "LIGHT",
    "EARTH_WIND": "EARTH", "ELEMENTAL": "NEUTRAL", "BEAST": "NEUTRAL",
    "MIXED": "NEUTRAL", "FASHI": "NEUTRAL",
}
RUNTIME_MOTIFS = frozenset({
    "GENERIC", "PROJECTILE", "BLADE", "SHIELD", "DOMAIN", "TELEPORT", "SUMMON",
    "WALL", "CHAIN", "CHANNEL", "RAIN", "HEAL", "CLEANSE", "SEAL", "FORMATION",
    "BUDDHIST", "CONFUCIAN", "DAO", "GHOST", "TALISMAN", "ILLUSION", "MARTIAL",
})
MOTIF_ALIASES = {
    "HUMANOID_OTHER": "MARTIAL", "YAOSHOU": "SUMMON", "CRAFT": "FORMATION",
    "LINGSHOU": "SUMMON", "CULTIVATION": "DOMAIN", "ENVIRONMENT": "DOMAIN",
    "GROWTH": "HEAL", "SHI_GUI": "GHOST", "STRUCTURE": "FORMATION", "CHONG": "SUMMON",
    "PATROL_CAPTAIN": "MARTIAL", "GREAT_ELDER": "DAO", "OUTER_DEACON": "MARTIAL",
    "SECT_MASTER": "DAO", "ALCHEMY_ELDER": "FORMATION", "BOSS": "DOMAIN",
    "ZHENLING": "SUMMON", "CATALOG_GENERIC": "GENERIC", "STATUS": "GENERIC",
    "PLANT_SPIRIT": "SUMMON", "GU_MO": "GHOST", "MULTIBLOCK_CONTROLLER": "FORMATION",
    "ARRAY_BLOCK": "FORMATION", "SINGLE_BLOCK": "FORMATION", "KILL_SWORD": "BLADE",
    "SPIRIT_GATHER": "FORMATION", "VEHICLE": "TELEPORT", "DEFENSE": "SHIELD",
    "QUEST_GIVER_MAIN": "DAO", "SEAL_DEMON": "SEAL", "BLACK_MARKET_CONTACT": "GENERIC",
    "MAJOR": "RAIN", "MINOR": "RAIN", "ILLUSION_MAZE": "ILLUSION", "ORE": "FORMATION",
    "UTILITY_BLOCK": "FORMATION", "CROP_BLOCK": "FORMATION", "IMMORTAL": "DAO",
    "NPC": "GENERIC", "ZHENLING_BLOODLINE": "SUMMON", "传送网注册": "TELEPORT",
    "契约与交易": "GENERIC", "律法与通缉": "MARTIAL", "授业/任务发放": "DAO",
    "航海/走私边界": "TELEPORT", "贡献与禁地配额": "DOMAIN",
    "飞升落地登记、界门收费": "TELEPORT", "高风险向导": "GENERIC",
}
DOMAIN_MOTIF_DEFAULTS = {
    "METHOD": "DOMAIN", "HERB": "GENERIC", "MATERIAL": "FORMATION", "BEAST": "SUMMON",
    "NPC": "GENERIC", "REALM": "DOMAIN", "ZONE": "DOMAIN", "BOSS": "DOMAIN",
    "STATUS": "GENERIC", "STRUCTURE": "FORMATION", "VEHICLE": "TELEPORT",
    "FORMATION": "FORMATION", "TRIBULATION": "RAIN",
}


def read_json(path: Path) -> dict[str, Any]:
    payload = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(payload, dict):
        raise ValueError(f"JSON object expected: {path}")
    return payload


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def clean(value: Any) -> str:
    if value is None:
        return ""
    return str(value).strip()


def norm(value: Any) -> str:
    return re.sub(r"_+", "_", clean(value).lower().replace("-", "_").replace(" ", "_"))


def canonical_family(value: Any) -> str:
    normalized = norm(value).upper()
    if normalized in RUNTIME_FAMILIES:
        return normalized
    return FAMILY_ALIASES.get(normalized, "NEUTRAL")


def canonical_motif(domain: str, value: Any) -> str:
    normalized = norm(value).upper()
    if normalized in RUNTIME_MOTIFS:
        return normalized
    return MOTIF_ALIASES.get(normalized, DOMAIN_MOTIF_DEFAULTS.get(domain, "GENERIC"))


def argb(hex_rgb: str) -> int:
    value = clean(hex_rgb).lstrip("#")
    if len(value) == 6:
        value = "FF" + value
    if len(value) != 8 or not re.fullmatch(r"[0-9a-fA-F]{8}", value):
        raise ValueError(f"invalid ARGB color {hex_rgb!r}")
    return int(value, 16)


def load_palette() -> tuple[dict[str, str], dict[str, int]]:
    style = read_json(SOURCE_DIR / "visual_style_v118.json")
    raw = style.get("palette", {})
    if not isinstance(raw, dict):
        raise ValueError("visual_style_v118.palette must be an object")
    colors = {norm(key): clean(value) for key, value in raw.items() if clean(value)}
    return colors, {key: argb(value) for key, value in colors.items()}


def index_raw(version: int, entry_type: str) -> dict[str, list[dict[str, Any]]]:
    payload = read_json(SOURCE_DIR / f"item_descriptions_v{version}.json")
    result: dict[str, list[dict[str, Any]]] = {}
    for entry in payload.get("items", []):
        if not isinstance(entry, dict) or entry.get("type") != entry_type:
            continue
        key = clean(entry.get("catalog_id"))
        if key:
            result.setdefault(key, []).append(entry)
    return result


def school(entry: dict[str, Any]) -> str:
    values = [norm(tag) for tag in entry.get("tags", [])]
    values = [value for value in values if value and value not in GENERIC_TAGS]
    return values[-1] if values else "misc"


def choose_raw(candidates: list[dict[str, Any]], profile: dict[str, Any]) -> dict[str, Any]:
    if not candidates:
        return {}
    desired_school = norm(profile.get("school"))
    desired_particle = norm(profile.get("particle"))
    desired_trail = norm(profile.get("trail"))

    def score(entry: dict[str, Any]) -> tuple[int, str]:
        points = 0
        if desired_school and school(entry) == desired_school:
            points += 8
        if desired_particle and norm(entry.get("particle_ref")) == desired_particle:
            points += 4
        if desired_trail and norm(entry.get("trail_ref")) == desired_trail:
            points += 2
        # Stable tie-breaker: source id, then serialized row.
        return points, json.dumps(entry, ensure_ascii=False, sort_keys=True)

    return max(candidates, key=score)


def parse_range(value: Any) -> tuple[int, int]:
    text = clean(value).upper().replace(" ", "")
    match = re.fullmatch(r"F?(\d+)(?:-(\d+))?", text)
    if match:
        start = int(match.group(1))
        end = int(match.group(2) or match.group(1))
        return start, max(1, end - start + 1)
    if text == "PRE":
        return 0, 8
    if text == "HIT":
        return 21, 2
    if text == "REC":
        return 23, 4
    return 0, 1


def trigger_for(name: str, frame: str = "") -> str:
    return TRIGGER_VALUES.get(clean(name), TRIGGER_VALUES.get(clean(frame).upper(), "USE"))


def action_for(trigger: str, state: str = "", text: str = "") -> str:
    if state:
        state_key = norm(state)
        if state_key in {"broken", "damaged"}:
            return "MODEL_ANIMATION"
        if state_key in {"impact", "reflect"}:
            return "FLASH"
        if state_key in {"orbit", "launch"}:
            return "RIBBON"
        if state_key in {"idle_bound", "active"}:
            return "AURA"
        return "STATE_TRANSITION"
    return {
        "TELEGRAPH": "SCREEN_OVERLAY",
        "ANTICIPATION": "EMITTER",
        "FORMATION": "AURA",
        "RELEASE": "RIBBON",
        "IMPACT": "FLASH",
        "DECAY": "DISSIPATE",
        "USE": "BURST",
    }.get(trigger, "EMITTER")


def anchor_for(trigger: str, action: str) -> str:
    if action == "SCREEN_OVERLAY":
        return "SCREEN"
    if trigger == "IMPACT":
        return "TARGET"
    if trigger == "RELEASE" or action == "RIBBON":
        return "PATH"
    if action == "MODEL_ANIMATION":
        return "ITEM"
    return "CASTER"


def timeline_event(ordinal: int, trigger: str, start: int, duration: int, action: str,
                   particle: str, trail: str, source: str, state: str = "",
                   intensity: int = 16, radius: float = 0.9) -> dict[str, Any]:
    return {
        "ordinal": ordinal,
        "trigger": trigger,
        "start_tick": max(0, int(start)),
        "duration_ticks": max(1, int(duration)),
        "action": action,
        "anchor": anchor_for(trigger, action),
        "target": "TARGET" if trigger == "IMPACT" else "NONE",
        "state": norm(state),
        "particle": particle if particle in PARTICLE_REFS else "qi_soft",
        "trail": trail if trail in TRAIL_REFS else "none",
        "radius": max(0.1, min(8.0, float(radius))),
        "intensity": max(1, min(64, int(intensity))),
        "condition": "ALWAYS",
        "source": clean(source),
    }


def technique_timeline(raw: dict[str, Any], profile: dict[str, Any]) -> list[dict[str, Any]]:
    events: list[dict[str, Any]] = []
    frames = raw.get("frames", []) if isinstance(raw, dict) else []
    for ordinal, frame in enumerate(frames):
        if not isinstance(frame, dict):
            continue
        start, duration = parse_range(frame.get("frame"))
        trigger = trigger_for(frame.get("name", ""), frame.get("frame", ""))
        action = action_for(trigger, text=clean(frame.get("vis")))
        base_intensity = max(1, min(64, int(profile.get("intensity", 16) or 16)))
        intensity = min(64, base_intensity + 4) if trigger == "IMPACT" else base_intensity
        events.append(timeline_event(ordinal, trigger, start, duration, action,
                                     norm(profile.get("particle")), norm(profile.get("trail")),
                                     clean(frame.get("vis")), intensity=intensity,
                                     radius=float(profile.get("radius", 0.9) or 0.9)))
    if not events:
        events.append(timeline_event(0, "USE", 0, 1, "BURST", norm(profile.get("particle")),
                                     norm(profile.get("trail")), "generated fallback event"))
    return sorted(events, key=lambda event: (event["start_tick"], event["ordinal"]))


# The compiler intentionally produces a small executable vocabulary.  These
# are primitives, not finished spell templates: one source quote can combine
# several of them and can alter path, scale, count, colour, and motion.
PROGRAM_RULES = (
    ("spatial_rift", ("空间裂", "虚空裂", "撕裂空间", "破碎虚空", "空间波动")),
    ("ice_prison", ("冰牢", "冰狱", "冰封", "冰墙", "玄冰", "寒冰")),
    ("blood_sea", ("血海", "血河", "血池", "血浪", "血潮")),
    ("tree_avatar", ("巨树", "古树", "树影", "树根", "藤蔓")),
    ("flame_bird", ("火鸟", "炎鸟", "朱雀", "凤凰", "火凤", "金乌")),
    ("beast_phantom", ("兽影", "虎影", "巨猿", "魔猿", "麒麟", "玄武", "白虎")),
    ("insect_swarm", ("虫云", "虫群", "蜂群", "蚁群", "噬金虫")),
    ("lightning_storm", ("雷海", "雷云", "千雷", "雷柱", "雷暴", "雷雨", "雷霆")),
    ("tidal_wave", ("巨浪", "海浪", "浪潮", "海潮", "洪水", "水幕", "水墙", "海啸")),
    ("mountain_meteor", ("山岳", "山峰", "巨山", "陨石", "流星", "坠星")),
    ("giant_claw", ("巨爪", "鬼爪", "火焰鬼爪")),
    ("giant_hand", ("巨手", "大手", "巨掌", "佛掌", "血掌", "擎天手")),
    ("eye_gaze", ("法目", "灵目", "天眼", "灵眼", "竖目", "瞳孔", "目光")),
    ("sound_wave", ("梵音", "禅音", "魔啸", "长啸", "怒吼", "咆哮", "钟鸣", "铃声", "传音")),
    ("lotus_mandala", ("莲花", "金莲", "青莲", "血莲", "莲台", "莲瓣")),
    ("mirror_disc", ("宝镜", "古镜", "镜光", "镜面", "水镜", "明镜")),
    ("scripture_glyph", ("经文", "书卷", "典籍", "墨字", "金文", "古字", "符字", "真言")),
    ("magnetic_field", ("元磁", "磁力", "磁光", "磁场", "两极")),
    ("wheel_disc", ("宝轮", "法轮", "光轮", "圆盘", "轮盘", "日轮", "月轮")),
    ("wing_fan", ("双翼", "羽翼", "翅膀", "羽翅", "风雷翅", "羽扇")),
    ("spear_spike", ("金针", "银针", "灵针", "飞针", "长矛", "冰矛", "冰锥", "地刺", "骨刺")),
    ("orb_projectile", ("光球", "火球", "雷球", "水球", "圆球", "灵珠", "宝珠", "圆珠")),
    ("mist_veil", ("雾气", "迷雾", "云雾", "雾幕", "烟幕", "霞光", "隐踪", "隐匿")),
    ("fist_barrage", ("拳影", "掌影")),
    ("sword_rain", ("剑雨", "剑阵", "密密麻麻剑")),
    ("serpent_dragon", ("火龙", "水龙", "火蛇", "蛟龙", "巨蟒", "毒蛇")),
    ("cloud_vortex", ("漩涡", "云团", "血云", "黑云")),
    ("rune_orbit", ("符文", "符箓", "符印", "法阵", "阵法", "阵图")),
    ("chain_net", ("锁链", "电网", "光丝", "丝线", "丝连")),
    ("beam_lance", ("光柱", "光束", "剑虹", "剑光")),
    ("projectile_swarm", ("飞射", "箭雨", "无数", "密密麻麻")),
    ("body_aura", ("鳞片", "金身", "铠甲", "护体")),
    ("spirit_avatar", ("法相", "鬼影", "骷髅", "化身", "人形", "女子")),
    ("ground_field", ("地面", "大地", "领域", "地网")),
    # semantic_layers_v3 — authored figure silhouettes (layer-scoped dispatch). These tokens
    # take priority over generic swarm/aura fallbacks so the named vessels, banners,
    # seals, curtains and haloes render with their own geometry rather than degrading
    # to the closest existing primitive (defect 4 audit).
    ("cauldron_vessel", ("青铜鼎", "巨鼎", "宝鼎", "炼丹鼎", "灵鼎", "双鼎", "黑鼎", "小鼎", "鼎中")),
    ("bell_chime", ("黑色小钟", "金钟", "铜钟", "灵钟", "宝钟", "古钟", "黑钟", "小钟", "梵钟")),
    ("gourd_vessel", ("葫芦", "玉葫芦", "宝葫芦", "灵葫芦", "玉瓶", "宝瓶", "灵瓶", "净瓶", "玉净瓶")),
    ("light_curtain", ("光幕", "光墙", "光帘", "五色光幕", "护幕", "屏障光", "一片光华", "光幕一", "光幕竟")),
    ("halo_ring", ("光环", "光圈", "圆环光", "血色光环", "金光圈", "光环一", "光环凭空")),
    ("banner_streamer", ("幡旗", "幡", "旗阵", "杆幡", "黑幡", "灵幡", "宝幡", "玉竹幡")),
    ("seal_stamp", ("法印一闪", "大印一压", "大印落下", "法印一", "玉印", "宝印", "印诀", "金印", "血印", "印玺")),
    ("bridge_arc", ("虹桥", "光桥", "玉桥", "虹桥光", "长虹光", "七彩桥", "光桥一")),
)

PROGRAM_SHAPES = {name for name, _ in PROGRAM_RULES} | {
    "aura_burst", "single_projectile", "array_rings", "chain_links", "summon_gate",
    "serpent_dragon", "sphere_field", "seal_cage", "barrier_plane", "body_shell",
    "afterimage_path", "layered_afterimages", "channel_stream", "blade_arc",
    "impact_arcs", "rising_motes", "cleansing_ring", "burning_talisman",
    "falling_barrage",
}

PROGRAM_COLOR_RULES = (
    # Longer multi-char tokens first so first-match-wins picks the richer mapping.
    ("绿金", "heal"), ("疗愈", "heal"), ("治愈", "heal"), ("诡绿", "poison"),
    ("毒", "poison"), ("紫金", "thunder"), ("紫电", "thunder"), ("雷", "thunder"),
    ("幽紫", "yin"), ("阴", "yin"), ("魂", "soul"), ("灵魂", "soul"),
    ("赤金", "metal"), ("金红", "fire"), ("紫红", "fire"), ("血红", "fire"),
    ("苍青", "wood"), ("碧青", "wood"), ("青金", "metal"), ("银白", "metal"),
    ("淡金", "metal"), ("淡蓝", "water"), ("深蓝", "water"), ("湛蓝", "water"),
    ("玄黑", "yin"), ("墨黑", "yin"), ("乌黑", "yin"), ("雪白", "qi"),
    ("乳白", "qi"), ("五色", "qi"), ("七彩", "qi"), ("血色", "fire"),
    ("火", "fire"), ("赤", "fire"), ("橙红", "fire"), ("水", "water"),
    ("澄蓝", "water"), ("蓝白", "water"), ("木", "wood"), ("碧", "wood"),
    ("青绿", "wood"), ("金属", "metal"), ("银金", "metal"), ("金", "metal"),
    ("土", "earth"), ("褐", "earth"), ("尘", "earth"), ("冰", "water"),
    ("寒", "water"), ("青白", "qi"), ("淡青", "qi"), ("黑", "yin"),
    ("白", "qi"), ("紫", "yin"), ("红", "fire"), ("蓝", "water"), ("绿", "wood"),
)


def program_path(text: str) -> str:
    if any(token in text for token in ("盘旋", "环绕", "旋转", "回旋", "绕")):
        return "ORBIT"
    if any(token in text for token in ("螺旋", " spiral", "盘绕")):
        return "SPIRAL"
    if any(token in text for token in ("扩散", "爆散", "散开", "铺开", "张开")):
        return "EXPAND"
    if any(token in text for token in ("汇聚", "聚拢", "收束", "凝聚")):
        return "CONVERGE"
    if any(token in text for token in ("升起", "上升", "腾起", "冲天", "浮起")):
        return "RISE"
    if any(token in text for token in ("坠落", "落下", "砸下", "从天")):
        return "FALL"
    if any(token in text for token in ("散落", "四射", "飞散", "四处")):
        return "SCATTER"
    if any(token in text for token in ("波动", "波纹", "脉动", "起伏")):
        return "WAVE"
    if any(token in text for token in ("飞向", "射向", "冲向", "扑向", "贯穿", "穿过")):
        return "TRACK"
    if any(token in text for token in ("轨迹", "飞射", "射出", "光束", "光柱")):
        return "DIRECT"
    return "STATIC"


def program_motion(text: str, inferred: bool) -> str:
    if any(token in text for token in ("渐渐凝聚", "凝聚清晰", "显现", "成型")):
        return "MATERIALIZE"
    if any(token in text for token in ("消散", "消失", "溃散", "化为虚影", "散去")):
        return "DISSOLVE"
    if any(token in text for token in ("闪烁", "忽明忽暗", "闪动")):
        return "FLICKER"
    if any(token in text for token in ("脉动", "呼吸", "一缩一涨")):
        return "PULSE"
    if any(token in text for token in ("加速", "骤快", "疾驰")):
        return "ACCELERATE"
    if any(token in text for token in ("缓慢", "渐慢", "迟缓")):
        return "DECELERATE"
    return "STEADY" if not inferred else "PULSE"


def program_anchor(text: str, path: str, trigger: str) -> str:
    if any(token in text for token in ("暗角", "镜头", "屏幕", "视野")):
        return "SCREEN"
    if trigger == "IMPACT" or any(token in text for token in ("敌人", "目标", "对方", "身上")):
        return "TARGET"
    if path in {"DIRECT", "TRACK", "WAVE"}:
        return "PATH"
    if any(token in text for token in ("掌心", "手中", "自身", "周身", "体内")):
        return "CASTER"
    return "MIDPOINT"


# Chinese compound numerals that appear as exact counts in source quotes
# (七十二口剑 / 三十六口金光 / 十八团道纹). Matched before single-digit rules so
# "七十二" is never reduced to the leading "七". Cap is VisualProgramLayer's 24.
# Longer / more-specific tokens first. 十八 before 三百六十 so a quote that
# names both ("三百六十团…只有十八团明亮") keeps the lit count, not the total.
_COMPOSITE_COPIES = (
    ("一百零八", 24), ("一百八", 24),
    ("七十二口", 24), ("七十二", 24),
    ("三十六口", 24), ("三十六", 24),
    ("二十四", 24),
    ("十八团", 18), ("十八", 18),
    ("十二道", 12), ("十二", 12),
    ("三百六十", 24), ("三百六", 24),
)


def program_copies(text: str) -> int:
    # Exact composite numerals first (七十二 / 三十六 / 十八).
    for token, value in _COMPOSITE_COPIES:
        if token in text:
            return value
    if any(token in text for token in ("无数", "漫天", "铺天盖地", "千", "万")):
        return 20
    if any(token in text for token in ("百", "数十", "密密麻麻")):
        return 12
    for token, value in (("九", 9), ("八", 8), ("七", 7), ("六", 6), ("五", 5),
                         ("四", 4), ("三", 3), ("两", 2), ("二", 2)):
        if token in text:
            return value
    return 1


def program_scale(text: str, base_radius: float) -> tuple[float, float, float]:
    tier = 1.0
    if any(token in text for token in ("千丈", "万丈", "遮天", "天地", "山岳般")):
        tier = 3.2
    elif any(token in text for token in ("百丈", "巨大", "硕大", "漫天", "房屋般")):
        tier = 2.2
    elif any(token in text for token in ("数十丈", "十丈", "大片", "云团", "漩涡")):
        tier = 1.6
    elif any(token in text for token in ("数丈", "丈许", "半人高", "人高")):
        tier = 1.4
    elif "丈" in text:
        tier = 1.4
    elif any(token in text for token in ("细小", "微弱", "一缕", "丝")):
        tier = 0.55
    return (max(0.2, min(4.8, tier * max(0.55, base_radius))),
            max(0.2, min(4.8, tier * (1.0 if "轨迹" not in text else 1.4))),
            max(0.2, min(4.8, tier * (1.25 if any(t in text for t in ("高", "天", "云")) else 0.85))))


def program_palette(text: str, fallback: str, argbs: dict[str, int]) -> tuple[str, str]:
    keys = []
    for token, key in PROGRAM_COLOR_RULES:
        if token in text and key in argbs and key not in keys:
            keys.append(key)
    primary = keys[0] if keys else fallback
    secondary = keys[1] if len(keys) > 1 else fallback
    return primary, secondary


def program_primitives(text: str, base_shape: str) -> tuple[list[str], list[str]]:
    selected: list[str] = []
    evidence: list[str] = []
    for primitive, terms in PROGRAM_RULES:
        hits = [term for term in terms if term in text]
        if hits:
            selected.append(primitive)
            evidence.extend(hits[:3])
    secondary = (
        ("aura_burst", ("光点", "灵光", "光芒", "闪白")),
        ("mist_veil", ("雾", "烟", "霜雾", "余烬")),
        ("impact_arcs", ("爆裂", "爆开", "爆散", "震")),
        ("layered_afterimages", ("残影", "虚影", "透明", "影子")),
    )
    for primitive, terms in secondary:
        hits = [term for term in terms if term in text]
        if hits and primitive not in selected:
            selected.append(primitive)
            evidence.extend(hits[:2])
    if not selected:
        selected.append(base_shape if base_shape in PROGRAM_SHAPES else "aura_burst")
        evidence.append("profile_shape:" + (base_shape or "aura_burst"))
    return selected[:4], evidence[:10]


def make_visual_program(profile: dict[str, Any], raw: dict[str, Any],
                        timeline: list[dict[str, Any]], argbs: dict[str, int]) -> dict[str, Any]:
    source_values = raw.get("visual_descriptions", []) if isinstance(raw, dict) else []
    sources = [clean(value) for value in source_values if clean(value)]
    inferred = clean(raw.get("visual_source_kind")) in {"novel_setting_fallback", "technique_description"}
    if not sources:
        sources = [clean(event.get("source")) for event in timeline if clean(event.get("source"))]
        inferred = True
    if not timeline:
        return {"compiler": "semantic_layers_v3", "source_quote_count": len(sources),
                "covered_quote_count": len(sources), "inferred_fallback": inferred, "layers": []}
    base_shape = norm(profile.get("shape"))
    fallback_palette = norm(profile.get("palette_key")) or "qi"
    layers: list[dict[str, Any]] = []
    seen_quote_layers = 0
    for source_index, source in enumerate(sources):
        matching = [index for index, event in enumerate(timeline) if clean(event.get("source")) == source]
        event_index = matching[0] if matching else source_index % len(timeline)
        event = timeline[event_index]
        trigger = clean(event.get("trigger"))
        primitive_ids, evidence = program_primitives(source, base_shape)
        path = program_path(source)
        anchor = program_anchor(source, path, trigger)
        motion = program_motion(source, inferred)
        copies = program_copies(source)
        radius, length, height = program_scale(source, float(profile.get("radius", 0.9) or 0.9))
        primary_key, secondary_key = program_palette(source, fallback_palette, argbs)
        digest = hashlib.sha256(f"{profile.get('id')}:{source_index}:{source}".encode("utf-8")).digest()
        phase = int.from_bytes(digest[:2], "big") % 360
        speed = 0.3 if "缓" in source or "慢" in source else 1.0
        if any(token in source for token in ("骤", "疾", "瞬", "一闪")):
            speed = 2.0
        spread = 180.0 if path in {"SCATTER", "EXPAND"} else (360.0 if path == "ORBIT" else 18.0)
        rotation = 360.0 if path in {"ORBIT", "SPIRAL"} else (phase if path == "WAVE" else 0.0)
        for primitive_index, primitive in enumerate(primitive_ids):
            layers.append({
                "layer_index": len(layers),
                "event_ordinal": int(event.get("ordinal", event_index)),
                "primitive": primitive,
                "anchor": anchor,
                "path": path,
                "motion": motion,
                "copies": copies if primitive_index == 0 else max(1, copies // 2),
                "radius_scale": radius / max(0.1, float(profile.get("radius", 0.9) or 0.9)),
                "length_scale": length,
                "height_scale": height,
                "speed": speed,
                "spread_degrees": spread,
                "rotation_degrees": rotation,
                "vertical_offset": 0.0 if anchor in {"TARGET", "PATH"} else (0.18 if "地面" not in source else 0.03),
                "jitter": 0.06 if motion in {"FLICKER", "PULSE"} else 0.02,
                "primary_argb": int(argbs.get(primary_key, argbs[fallback_palette])),
                "secondary_argb": int(argbs.get(secondary_key, argbs[fallback_palette])),
                "evidence_terms": evidence + ["quote_index:" + str(source_index)],
                "source_quote": source,
                "inferred": inferred,
            })
        seen_quote_layers += 1
    if not layers:
        layers.append({
            "layer_index": 0, "event_ordinal": 0, "primitive": base_shape if base_shape in PROGRAM_SHAPES else "aura_burst",
            "anchor": "CASTER", "path": "STATIC", "motion": "PULSE", "copies": 1,
            "radius_scale": 1.0, "length_scale": 1.0, "height_scale": 1.0, "speed": 0.5,
            "spread_degrees": 0.0, "rotation_degrees": 0.0, "vertical_offset": 0.0, "jitter": 0.0,
            "primary_argb": int(argbs.get(fallback_palette, 0xffd9ffff)),
            "secondary_argb": int(argbs.get(fallback_palette, 0xffd9ffff)),
            "evidence_terms": ["generated_fallback"], "source_quote": "", "inferred": True,
        })
    return {
        "compiler": "semantic_layers_v3",
        "source_quote_count": len(sources) if not inferred else len(sources),
        "covered_quote_count": seen_quote_layers,
        "inferred_fallback": inferred,
        "layers": layers,
    }


def artifact_timeline(raw: dict[str, Any], profile: dict[str, Any]) -> tuple[list[dict[str, Any]], dict[str, str], dict[str, str]]:
    events: list[dict[str, Any]] = []
    states: dict[str, str] = {}
    state_sources: dict[str, str] = {}
    raw_states = raw.get("states", {}) if isinstance(raw, dict) else {}
    if not isinstance(raw_states, dict):
        raw_states = {}
    for ordinal, (state, source) in enumerate(raw_states.items()):
        state_key = norm(state)
        source_text = clean(source)
        action = action_for("STATE", state_key, source_text)
        states[state_key] = action
        state_sources[state_key] = source_text
        events.append(timeline_event(ordinal, "STATE", 0, 1, action,
                                     norm(profile.get("particle")), norm(profile.get("trail")),
                                     source_text, state=state_key,
                                     intensity=20 if state_key in {"active", "impact"} else 12))
    if not events:
        events.append(timeline_event(0, "STATE", 0, 1, "STATE_TRANSITION",
                                     norm(profile.get("particle")), norm(profile.get("trail")),
                                     "generated fallback state", state="active"))
    return events, states, state_sources


def consumable_timeline(raw: dict[str, Any], profile: dict[str, Any]) -> list[dict[str, Any]]:
    events: list[dict[str, Any]] = []
    frames = raw.get("frames", []) if isinstance(raw, dict) else []
    for ordinal, frame in enumerate(frames):
        if not isinstance(frame, dict):
            continue
        try:
            step = max(1, int(frame.get("step", ordinal + 1)))
        except (TypeError, ValueError):
            step = ordinal + 1
        source = clean(frame.get("vis"))
        events.append(timeline_event(ordinal, "USE", (step - 1) * 4, 4, "BURST",
                                     norm(profile.get("particle")), norm(profile.get("trail")),
                                     source, intensity=int(profile.get("intensity", 16))))
    if not events:
        kind = norm(profile.get("vfx_kind"))
        action = "AURA" if kind in {"aura", "status"} else "BURST"
        events.append(timeline_event(0, "USE", 0, 6, action, norm(profile.get("particle")),
                                     norm(profile.get("trail")), "generated use event",
                                     intensity=int(profile.get("intensity", 16)),
                                     radius=float(profile.get("radius", 0.9))))
    return sorted(events, key=lambda event: (event["start_tick"], event["ordinal"]))


def family_for(profile: dict[str, Any]) -> str:
    value = clean(profile.get("family")) or clean(profile.get("element")) or "NEUTRAL"
    value = norm(value).upper()
    aliases = {"QI": "NEUTRAL", "FLAME": "FIRE", "YIN": "DARK", "HEAL": "LIGHT"}
    return aliases.get(value, value if value else "NEUTRAL")


def motif_for(profile: dict[str, Any]) -> str:
    value = clean(profile.get("motif"))
    if value:
        return norm(value).upper()
    shape = clean(profile.get("shape"))
    trail = norm(profile.get("trail"))
    shape_map = {
        "冲击环": "MARTIAL", "地网": "FORMATION", "丝连": "CHAIN", "绿金粒": "HEAL",
        "纸焚轨迹": "TALISMAN", "薄线": "BLADE", "重影": "ILLUSION", "残影": "ILLUSION",
        "半透": "GHOST", "湿暗": "GHOST", "风骨混合": "TELEPORT", "弹/扇/柱随元素": "PROJECTILE",
    }
    if shape in shape_map:
        return shape_map[shape]
    return {
        "sword_thin": "BLADE", "talisman_ash": "TALISMAN", "movement_wind": "TELEPORT",
        "heavy_weapon": "MARTIAL", "soul_afterimage": "ILLUSION",
    }.get(trail, "GENERIC")


def palette_for(profile: dict[str, Any], colors: dict[str, str], argbs: dict[str, int]) -> tuple[str, bool]:
    prose = clean(profile.get("color"))
    for token, key in COLOR_TOKEN_PALETTE:
        if token in prose and key in colors:
            return key, False
    family = family_for(profile)
    key = PALETTE_FAMILY.get(family, "qi")
    return key, True


def palette_for_new(domain: str, row: dict[str, Any], bundle: dict[str, Any] | None,
                    family: str, colors: dict[str, str], argbs: dict[str, int]) -> tuple[str, bool]:
    """Resolve a new-domain palette without letting incidental prose win."""
    explicit = clean(row.get("family")) or clean(row.get("element")) or clean(row.get("element_required"))
    color_values: list[str] = []
    if isinstance(bundle, dict):
        merged = bundle.get("merged", {})
        for key in ("color", "primary_color", "palette", "style_tags"):
            if merged.get(key) not in (None, "", [], {}):
                color_values.append(flatten_text(merged.get(key)))
    palette_text = " ".join(color_values)
    # Explicit canonical elements are authoritative.  A visual color field is
    # only consulted when the runtime row has no element/family declaration.
    if explicit:
        key = PALETTE_FAMILY.get(family, "qi")
        return key, key not in colors
    return palette_for({"family": family, "color": palette_text}, colors, argbs)


def source_ids(profile: dict[str, Any]) -> dict[str, str]:
    value = profile.get("sources", {})
    return {norm(key): clean(item) for key, item in value.items() if clean(key) and clean(item)} if isinstance(value, dict) else {}


def make_profile(domain: str, profile: dict[str, Any], raw: dict[str, Any], colors: dict[str, str], argbs: dict[str, int]) -> dict[str, Any]:
    profile_id = norm(profile.get("id"))
    family = canonical_family(family_for(profile))
    motif = canonical_motif(domain, motif_for(profile))
    palette_key, palette_fallback = palette_for(profile, colors, argbs)
    particle = norm(profile.get("particle")) or "qi_soft"
    trail = norm(profile.get("trail")) or "none"
    if domain == "TECHNIQUE":
        timeline = technique_timeline(raw, profile)
        states: dict[str, str] = {}
        state_sources: dict[str, str] = {}
    elif domain == "ARTIFACT":
        timeline, states, state_sources = artifact_timeline(raw, profile)
    else:
        timeline = consumable_timeline(raw, profile)
        states = {}
        state_sources = {}
    visual_program = make_visual_program(profile, raw, timeline, argbs) \
        if domain == "TECHNIQUE" else {"compiler": "none_v1", "source_quote_count": 0,
        "covered_quote_count": 0, "inferred_fallback": True,
                                        "layers": []}
    return {
        "key": f"{domain.lower()}:{profile_id}",
        "domain": domain,
        "id": profile_id,
        "runtime_id": clean(profile.get("runtime_id")) or f"seeking_immortals:{profile_id}",
        "display": clean(profile.get("display")),
        "authored": True,
        "fallback": False,
        "palette_fallback": palette_fallback,
        "family": family,
        "motif": motif,
        "shape": clean(profile.get("shape")) or clean(profile.get("look", {}).get("silhouette") if isinstance(profile.get("look"), dict) else ""),
        "particle": particle if particle in PARTICLE_REFS else "qi_soft",
        "trail": trail if trail in TRAIL_REFS else "none",
        "color_prose": clean(profile.get("color")) or clean(profile.get("look", {}).get("color") if isinstance(profile.get("look"), dict) else ""),
        "palette_key": palette_key,
        "primary_argb": argbs[palette_key],
        "telegraphed": bool(profile.get("telegraphed", profile.get("has_telegraph", False))),
        "radius": max(0.1, min(8.0, float(profile.get("radius", 0.9) or 0.9))),
        "intensity": max(1, min(64, int(profile.get("intensity", 16) or 16))),
        "visual_program": visual_program,
        "timeline": timeline,
        "states": states,
        "state_sources": state_sources,
        "sources": source_ids(profile),
    }


def relative(path: Path) -> str:
    """Return a stable repository-relative source reference."""
    return str(path.relative_to(ROOT)).replace("\\", "/")


def canonical_rows(domain: str) -> tuple[list[dict[str, Any]], Path, str]:
    """Load and normalize one runtime-authoritative domain list."""
    path, key = NEW_DOMAIN_SOURCES[domain]
    payload = read_json(path)
    rows = [
        {**row, "_source_file": relative(path)}
        for row in payload.get(key, []) if isinstance(row, dict)
    ]
    if domain == "NPC":
        seeds = read_json(NPC_SEED_SOURCE).get("npcs", [])
        rows = rows + [
            {**row, "_source_file": relative(NPC_SEED_SOURCE)}
            for row in seeds if isinstance(row, dict)
        ]
    if domain == "BOSS":
        rows = [{**row, "id": clean(row.get("boss_id") or row.get("id"))}
                for row in rows]
    if domain == "ZONE":
        # A zone is a runtime realm layer, not a separate top-level file.
        realm_path = WORLD_DIR / "secret_realm_runtime.json"
        realm_payload = read_json(realm_path)
        rows = []
        for realm in realm_payload.get("realms", []):
            if not isinstance(realm, dict):
                continue
            for layer in realm.get("layers", []):
                if not isinstance(layer, dict) or not clean(layer.get("id")):
                    continue
                rows.append({
                    **layer,
                    "realm_id": clean(realm.get("id")),
                    "realm_display": clean(realm.get("display")),
                    "_source_file": relative(realm_path),
                })
        path, key = realm_path, "realms.layers"
    # Canonical IDs are unique even when the source intentionally carries
    # multiple rows for one material or an NPC seed overlaps v116.
    unique: dict[str, dict[str, Any]] = {}
    for row in rows:
        row_id = norm(row.get("id"))
        if not row_id:
            continue
        if row_id not in unique:
            unique[row_id] = row
        else:
            # Preserve additional prose/fields without allowing source order
            # to change the selected canonical row.
            unique[row_id] = merge_rows(unique[row_id], row)
    return [unique[key] for key in sorted(unique)], path, key


def merge_rows(base: dict[str, Any], extra: dict[str, Any]) -> dict[str, Any]:
    """Deterministically merge two source rows, retaining non-empty prose."""
    result = dict(base)
    for key, value in extra.items():
        if key not in result or result[key] in (None, "", [], {}):
            result[key] = value
        elif isinstance(result[key], dict) and isinstance(value, dict):
            nested = dict(result[key])
            for nested_key, nested_value in value.items():
                if nested_key not in nested or nested[nested_key] in (None, "", [], {}):
                    nested[nested_key] = nested_value
            result[key] = nested
        elif isinstance(result[key], list) and isinstance(value, list):
            merged_values: list[Any] = []
            seen_values: set[str] = set()
            for item in result[key] + value:
                marker = json.dumps(item, ensure_ascii=False, sort_keys=True, default=str)
                if marker not in seen_values:
                    seen_values.add(marker)
                    merged_values.append(item)
            result[key] = merged_values
    return result


def visual_rows_by_type() -> dict[str, list[dict[str, Any]]]:
    """Index all authored v118-v122 rows once, with their source version."""
    result: dict[str, list[dict[str, Any]]] = {domain: [] for domain in VISUAL_SOURCE_TYPES}
    for version in range(118, 123):
        path = SOURCE_DIR / f"item_descriptions_v{version}.json"
        payload = read_json(path)
        for domain, types in VISUAL_SOURCE_TYPES.items():
            for row in payload.get("items", []):
                if not isinstance(row, dict) or row.get("type") not in types:
                    continue
                source_key = clean(row.get("catalog_id")) or clean(row.get("id"))
                if not source_key:
                    continue
                result[domain].append({
                    "version": version,
                    "type": clean(row.get("type")),
                    "source_key": norm(source_key),
                    "row": row,
                    "path": path,
                })
    return result


def visual_mapping(crosswalk: dict[str, Any], domain: str) -> dict[str, str]:
    maps = crosswalk.get("id_maps", {})
    values = maps.get(domain, {}) if isinstance(maps, dict) else {}
    return {norm(source): norm(target) for source, target in values.items()
            if norm(source) and norm(target)} if isinstance(values, dict) else {}


def style_mapping(crosswalk: dict[str, Any], domain: str) -> dict[str, str]:
    maps = crosswalk.get("style_maps", {})
    values = maps.get(domain, {}) if isinstance(maps, dict) else {}
    return {norm(profile_id): norm(style_id) for profile_id, style_id in values.items()
            if norm(profile_id) and norm(style_id)} if isinstance(values, dict) else {}


def visual_bundles(domain: str, crosswalk: dict[str, Any]) -> dict[str, dict[str, Any]]:
    """Group authored rows by canonical ID and retain every source reference."""
    mapping = visual_mapping(crosswalk, domain)
    bundles: dict[str, dict[str, Any]] = {}
    for candidate in visual_rows_by_type()[domain]:
        source_key = candidate["source_key"]
        canonical_id = mapping.get(source_key, source_key)
        bundle = bundles.setdefault(canonical_id, {"rows": [], "merged": {}, "sources": {}})
        bundle["rows"].append(candidate)
        bundle["merged"] = merge_rows(bundle["merged"], candidate["row"])
        version_key = f"v{candidate['version']}"
        source_value = clean(candidate["row"].get("id")) or source_key
        if version_key in bundle["sources"]:
            # Multiple cards in a version are retained rather than silently
            # dropping a style card that crosswalks to the same profile.
            bundle["sources"][version_key] += "," + source_value
        else:
            bundle["sources"][version_key] = source_value
    return bundles


def flatten_text(value: Any) -> str:
    if value is None:
        return ""
    if isinstance(value, dict):
        return " ".join(flatten_text(item) for item in value.values())
    if isinstance(value, (list, tuple, set)):
        return " ".join(flatten_text(item) for item in value)
    return clean(value)


def prose_excerpt(row: dict[str, Any], bundle: dict[str, Any] | None = None) -> str:
    values: list[str] = []
    prose_keys = (
        "appearance", "description", "effect_text", "effect", "active_vfx", "idle_vfx",
        "cast_vfx", "train_vfx", "combat_vfx", "weak_vfx", "death_vfx", "stages",
        "setting", "lore", "habitat", "role", "category", "kind", "display",
    )

    def row_prose(source: dict[str, Any]) -> str:
        return " ".join(flatten_text(source.get(key)) for key in prose_keys
                         if source.get(key) not in (None, "", [], {}))

    if isinstance(bundle, dict):
        values.append(row_prose(bundle.get("merged", {})))
        values.extend(row_prose(item.get("row", {})) for item in bundle.get("rows", []))
    values.append(row_prose(row))
    text = re.sub(r"\s+", " ", " ".join(value for value in values if value))
    return text[:420]


def source_event_ref(source_map: dict[str, str], domain: str, profile_id: str) -> str:
    refs = [value for key, value in sorted(source_map.items())
            if key.startswith("v") and value]
    if refs:
        return f"{domain.lower()}:{profile_id} [" + ",".join(refs) + "]"
    return f"{domain.lower()}:{profile_id} [fallback]"


def field_text(row: dict[str, Any], bundle: dict[str, Any] | None = None) -> str:
    pieces = [
        clean(row.get("id")), clean(row.get("display")), clean(row.get("family")),
        clean(row.get("element")), clean(row.get("element_required")),
        clean(row.get("category")), clean(row.get("kind")), clean(row.get("type")),
        clean(row.get("effect")), clean(row.get("description")),
    ]
    if isinstance(bundle, dict):
        pieces.append(flatten_text(bundle.get("merged", {})))
    return " ".join(piece for piece in pieces if piece).lower()


def infer_family(domain: str, row: dict[str, Any], bundle: dict[str, Any] | None = None) -> str:
    if domain == "STATUS":
        status_id = norm(row.get("id"))
        if status_id in {"burn", "fire_mark"}:
            return "FIRE"
        if status_id in {"frozen", "freeze"}:
            return "ICE"
        if status_id in {"soul_shock", "soul_wound", "fear"}:
            return "SOUL"
        if status_id in {"poison", "poisoned", "marrow_drain"}:
            return "BLOOD"
        if status_id in {"conceal_qi", "shape_shift"}:
            return "DARK"
        if clean(row.get("category")) == "beneficial":
            return "LIGHT"
    token_map = (
        (("THUNDER", "LIGHTNING", "紫电", "雷劫", "雷"), "THUNDER"),
        (("FIRE", "FLAME", "炎", "焰", "火属性", "火系"), "FIRE"),
        (("ICE", "FROST", "COLD", "冰属性", "寒冰"), "ICE"),
        (("WATER", "AQUA", "水属性", "水系"), "WATER"),
        (("WOOD", "PLANT", "木属性", "木系"), "WOOD"),
        (("METAL", "SWORD", "金属性", "金系"), "METAL"),
        (("EARTH", "STONE", "土属性", "土系"), "EARTH"),
        (("BLOOD", "POISON", "血系", "毒系", "血煞"), "BLOOD"),
        (("SOUL", "GHOST", "SPIRIT", "魂系", "鬼修", "冥河"), "SOUL"),
        (("VOID", "SPACE", "虚空", "空间法则", "星空"), "VOID"),
        (("WIND", "AIR", "风属性", "风系"), "WIND"),
        (("LIGHT", "HEAL", "HOLY", "疗愈", "治愈", "圣辉"), "LIGHT"),
        (("ILLUSION", "MIND", "幻术", "幻阵", "幻惑"), "ILLUSION"),
        (("YIN", "DARK", "阴属性", "阴系", "魔气"), "DARK"),
    )

    # Prefer explicit canonical fields.  Generic visual prose often mentions
    # unrelated colors (for example a fire-lit forge), which must not override
    # the method/material's declared element.
    explicit_values = [clean(row.get(key)).upper() for key in ("family", "element", "element_required")]
    for value in explicit_values:
        normalized = norm(value).upper()
        if normalized in FAMILY_PARTICLE:
            return normalized
        for tokens, family in token_map:
            if any(token.upper() == normalized for token in tokens):
                return family

    text = field_text(row, bundle).upper()
    ascii_tokens = set(re.split(r"[^A-Z0-9]+", text))
    ascii_tokens.discard("")
    # English identifiers/tags are reliable fallback signals.  Chinese prose
    # is checked only for distinctive multi-character phrases to avoid common
    # characters such as 火 in "炼器" selecting FIRE accidentally.
    for tokens, family in token_map:
        if any(token.upper() in ascii_tokens for token in tokens if token.isascii()):
            return family
        if any(token in text for token in tokens if not token.isascii() and len(token) >= 2):
            return family
    if domain == "STATUS":
        return "LIGHT" if clean(row.get("category")) == "beneficial" else "NEUTRAL"
    if domain in {"REALM", "ZONE", "BOSS", "TRIBULATION"}:
        return "VOID"
    return "NEUTRAL"


def infer_motif(domain: str, row: dict[str, Any], bundle: dict[str, Any] | None = None) -> str:
    if domain == "BOSS":
        return "BOSS"
    if domain in {"REALM", "ZONE"}:
        return "ENVIRONMENT"
    if domain == "STATUS":
        return "STATUS"
    if domain == "STRUCTURE":
        return norm(row.get("type") or "STRUCTURE").upper()
    if domain == "VEHICLE":
        return "VEHICLE"
    if domain == "FORMATION":
        return norm(row.get("kind") or "FORMATION").upper()
    if domain == "TRIBULATION":
        return norm(row.get("tribulation_tier") or "TRIBULATION").upper()
    if domain == "METHOD":
        return "CULTIVATION"
    if domain == "HERB":
        return "GROWTH"
    if domain == "MATERIAL":
        return "CRAFT"
    if domain == "BEAST":
        return norm(row.get("category") or "BEAST").upper()
    if domain == "NPC":
        return norm(row.get("role") or "NPC").upper()
    return "GENERIC"


def infer_particle_trail(domain: str, family: str, row: dict[str, Any], bundle: dict[str, Any] | None) -> tuple[str, str]:
    text = field_text(row, bundle)
    particle = ""
    trail = ""
    if isinstance(bundle, dict):
        merged = bundle.get("merged", {})
        particle = norm(merged.get("particle_ref") or merged.get("particle"))
        trail = norm(merged.get("trail_ref") or merged.get("trail"))
    if not particle:
        for marker, value in (
            ("space_glitch", "space_glitch"), ("thunder_arc", "thunder_arc"),
            ("blood_mist", "blood_mist"), ("yin_smoke", "yin_smoke"),
            ("soul_wisps", "soul_wisps"), ("heal_motes", "heal_motes"),
            ("water_mist", "water_mist"), ("wood_pollen", "wood_pollen"),
            ("metal_spark", "metal_spark"), ("earth_dust", "earth_dust"),
            ("fire_ember", "fire_ember"),
        ):
            if marker in text:
                particle = value
                break
    if not particle:
        particle = FAMILY_PARTICLE.get(family, "qi_soft")
    if not trail:
        if domain in {"METHOD", "VEHICLE"}:
            trail = "movement_wind"
        elif domain in {"BEAST", "BOSS"} and family in {"METAL", "EARTH"}:
            trail = "heavy_weapon"
        else:
            trail = FAMILY_TRAIL.get(family, "none")
    if particle not in PARTICLE_REFS:
        particle = "qi_soft"
    if trail not in TRAIL_REFS:
        trail = "none"
    return particle, trail


def numeric_hint(row: dict[str, Any], key: str, default: float) -> float:
    value = row.get(key)
    if isinstance(value, (int, float)):
        return float(value)
    if isinstance(value, list):
        numbers = [float(item) for item in value if isinstance(item, (int, float))]
        if numbers:
            return max(numbers)
    return default


def profile_radius_intensity(domain: str, row: dict[str, Any], profile_id: str) -> tuple[float, int]:
    digest_bytes = hashlib.sha256(f"{domain}:{profile_id}".encode("utf-8")).digest()
    default_radius = 0.65 + (digest_bytes[0] % 30) / 20.0
    radius = numeric_hint(row, "radius", default_radius)
    threat = numeric_hint(row, "threat", 0.0)
    if threat:
        radius = 0.7 + threat * 0.28
    radius = max(0.1, min(8.0, radius))
    intensity = int(numeric_hint(row, "intensity", 10 + digest_bytes[1] % 18))
    intensity += int(threat * 3)
    intensity += int(numeric_hint(row, "waves", 0.0))
    if domain in {"BOSS", "TRIBULATION"}:
        intensity += 10
    return radius, max(1, min(64, intensity))


def lifecycle_trigger(state: str, domain: str) -> tuple[str, str]:
    state = norm(state)
    if state in {"p1", "p2", "p3", "telegraph", "announce"}:
        return "TELEGRAPH", "SCREEN_OVERLAY" if state in {"telegraph", "announce"} else "AURA"
    if state in {"attack", "combat_cast", "wave", "boost", "pulse", "active", "processing", "tick_stack"}:
        return "RELEASE", "RIBBON" if state in {"attack", "combat_cast", "boost"} else "EMITTER"
    if state in {"hit", "weak", "damaged", "critical", "disrupted"}:
        return "IMPACT", "FLASH"
    if state in {"death", "expire", "decay", "exit", "depart", "depleted", "disabled", "dismantled", "failure"}:
        return "DECAY", "DISSIPATE"
    if state in {"held", "inserted", "board", "talk", "trade", "harvested", "dispel", "success"}:
        return "USE", "BURST"
    if state in {"enter", "train_enter", "unripe", "idle", "docked", "dock", "undeployed", "intact", "applied"}:
        return "ANTICIPATION", "AURA"
    return "FORMATION", "STATE_TRANSITION"


def lifecycle_timeline(domain: str, row: dict[str, Any], particle: str, trail: str,
                       source: str, states: Iterable[str]) -> tuple[list[dict[str, Any]], dict[str, str]]:
    events: list[dict[str, Any]] = []
    actions: dict[str, str] = {}
    state_list = list(states)
    for ordinal, state in enumerate(state_list):
        trigger, action = lifecycle_trigger(state, domain)
        actions[norm(state)] = action
        events.append(timeline_event(
            ordinal, trigger, ordinal * 4, 4 if trigger not in {"TELEGRAPH", "IMPACT"} else 2,
            action, particle, trail, source, state=state,
            intensity=18 if trigger in {"TELEGRAPH", "IMPACT"} else 12,
            radius=0.9 + (ordinal % 3) * 0.25,
        ))
    return events or [timeline_event(0, "USE", 0, 1, "BURST", particle, trail, source)], actions


def status_style_for(row: dict[str, Any], visual_bundle: dict[str, Any] | None) -> str:
    explicit = clean(row.get("_visual_style_id"))
    if explicit:
        return explicit
    if isinstance(visual_bundle, dict):
        for candidate in visual_bundle.get("rows", []):
            source_key = clean(candidate.get("source_key"))
            if source_key:
                return source_key
    text = field_text(row, visual_bundle)
    families = (
        ("poison", "fx_poison"), ("burn", "fx_burn"), ("freeze", "fx_freeze"),
        ("stun", "fx_stun"), ("heal", "fx_heal"), ("beneficial", "fx_heal"),
        ("soul", "fx_soul"), ("stealth", "fx_stealth"), ("seal", "fx_seal"),
        ("fear", "fx_fear"), ("demonic", "fx_demonic"), ("cultivation", "fx_cultivation"),
    )
    for token, style in families:
        if token in text:
            return style
    return "fx_neutral"


def new_profile(domain: str, row: dict[str, Any], bundle: dict[str, Any] | None,
                authored: bool, source_path: Path, crosswalk: dict[str, Any],
                colors: dict[str, str], argbs: dict[str, int]) -> dict[str, Any]:
    profile_id = norm(row.get("id"))
    family = canonical_family(infer_family(domain, row, bundle))
    motif = canonical_motif(domain, infer_motif(domain, row, bundle))
    particle, trail = infer_particle_trail(domain, family, row, bundle)
    radius, intensity = profile_radius_intensity(domain, row, profile_id)
    text = prose_excerpt(row, bundle)
    palette_key, palette_fallback = palette_for_new(domain, row, bundle, family, colors, argbs)
    source_map: dict[str, str] = {
        "canonical": f"{relative(source_path)}#{profile_id}",
        "fallback_recipe": f"{domain.lower()}_deterministic_v1",
    }
    if authored and isinstance(bundle, dict):
        source_map.update(bundle.get("sources", {}))
        source_map["prose"] = text
    else:
        source_map["prose"] = text
    if domain == "STATUS":
        source_map["visual_style"] = status_style_for(row, bundle)
        source_map["status_icon"] = clean(row.get("icon"))
    states = list(DOMAIN_LIFECYCLES.get(domain, ("idle", "active", "decay")))
    if domain == "STRUCTURE" and norm(row.get("type")) == "ore":
        states = ["unmined", "depleted"]
    if domain == "BOSS":
        # Runtime bosses with only one phase still use the same readable
        # telegraph/attack/weak/death contract.
        states = list(DOMAIN_LIFECYCLES["BOSS"])
    event_source = source_event_ref(source_map, domain, profile_id)
    timeline, state_actions = lifecycle_timeline(
        domain, row, particle, trail, event_source, states)
    display = clean(row.get("display")) or clean(row.get("name")) or profile_id
    shape = clean((bundle or {}).get("merged", {}).get("appearance"))[:240]
    if not shape:
        shape = clean(row.get("type") or row.get("category") or row.get("kind"))
    telegraph_text = text.lower()
    telegraphed = domain in {"BOSS", "REALM", "ZONE", "TRIBULATION"} or any(
        token in telegraph_text for token in ("telegraph", "前摇", "预兆", "蓄能", "警示", "telegraph")
    )
    return {
        "key": f"{domain.lower()}:{profile_id}",
        "domain": domain,
        "id": profile_id,
        "runtime_id": f"seeking_immortals:{profile_id}",
        "display": display,
        "authored": bool(authored),
        "fallback": not authored,
        "palette_fallback": palette_fallback,
        "family": family,
        "motif": motif,
        "shape": shape,
        "particle": particle,
        "trail": trail,
        "color_prose": text[:360],
        "palette_key": palette_key,
        "primary_argb": argbs[palette_key],
        "telegraphed": telegraphed,
        "radius": radius,
        "intensity": intensity,
        "timeline": timeline,
        "states": state_actions,
        "state_sources": {
            state: (source_map.get("prose", event_source)[:120] or event_source)
            for state in states
        },
        "sources": source_map,
    }


def aliases_for_domain(crosswalk: dict[str, Any], domain: str, profiles: dict[str, dict[str, Any]]) -> list[dict[str, str]]:
    aliases: list[dict[str, str]] = []
    for source, target in visual_mapping(crosswalk, domain).items():
        if source == target or target not in profiles:
            continue
        aliases.append({
            "domain": domain,
            "alias": source,
            "key": f"{domain.lower()}:{source}",
            "target": target,
            "target_key": f"{domain.lower()}:{target}",
        })
    return aliases


def compile_new_domains(crosswalk: dict[str, Any], colors: dict[str, str],
                        argbs: dict[str, int]) -> tuple[list[dict[str, Any]], list[dict[str, str]], list[Path]]:
    visual_index = visual_bundles_by_domain(crosswalk)
    all_profiles: list[dict[str, Any]] = []
    aliases: list[dict[str, str]] = []
    source_paths: list[Path] = []
    profile_maps: dict[str, dict[str, dict[str, Any]]] = {}
    for domain in DOMAIN_ORDER:
        if domain in OLD_SOURCES:
            continue
        rows, source_path, _ = canonical_rows(domain)
        source_paths.append(source_path)
        if domain == "NPC":
            source_paths.append(NPC_SEED_SOURCE)
        if domain == "STRUCTURE":
            source_paths.append(STRUCTURE_STATE_SOURCE)
        if domain == "FORMATION":
            source_paths.append(FORMATION_ARRAY_SOURCE)
        bundles = visual_index.get(domain, {})
        profile_map: dict[str, dict[str, Any]] = {}
        for row in rows:
            profile_id = norm(row.get("id"))
            if not profile_id:
                continue
            bundle = bundles.get(profile_id)
            # Structure prop visuals are explicitly crosswalked; all other
            # domains use the presence of a source bundle as authored coverage.
            authored = bool(bundle)
            if domain == "STATUS":
                # A status row is authored when its explicit v119 style card
                # exists.  Multiple runtime statuses may intentionally share
                # one card, but provenance remains the real card id/version.
                style_id = style_mapping(crosswalk, domain).get(profile_id)
                if not style_id:
                    raise ValueError(f"missing explicit STATUS style mapping for {profile_id}")
                row = {**row, "_visual_style_id": style_id}
                bundle = bundles.get(style_id)
                if bundle is None:
                    raise ValueError(f"missing STATUS style card {style_id} for {profile_id}")
                authored = True
            if domain in {"VEHICLE", "FORMATION", "TRIBULATION"}:
                authored = False
            row_source = clean(row.get("_source_file"))
            profile_source = ROOT / row_source if row_source else source_path
            profile = new_profile(domain, row, bundle, authored, profile_source, crosswalk, colors, argbs)
            profile_map[profile_id] = profile
        # Keep aliases domain-scoped and only emit targets that really exist.
        aliases.extend(aliases_for_domain(crosswalk, domain, profile_map))
        all_profiles.extend(profile_map.values())
        profile_maps[domain] = profile_map
    return all_profiles, aliases, source_paths


def visual_bundles_by_domain(crosswalk: dict[str, Any]) -> dict[str, dict[str, dict[str, Any]]]:
    # Build the index once per compile, then merge by canonical crosswalk ID.
    indexed = visual_rows_by_type()
    result: dict[str, dict[str, dict[str, Any]]] = {}
    for domain, candidates in indexed.items():
        mapping = visual_mapping(crosswalk, domain)
        bundles: dict[str, dict[str, Any]] = {}
        for candidate in candidates:
            source_key = candidate["source_key"]
            canonical_id = mapping.get(source_key, source_key)
            bundle = bundles.setdefault(canonical_id, {"rows": [], "merged": {}, "sources": {}})
            bundle["rows"].append(candidate)
            bundle["merged"] = merge_rows(bundle["merged"], candidate["row"])
            version_key = f"v{candidate['version']}"
            source_value = clean(candidate["row"].get("id")) or source_key
            if version_key in bundle["sources"]:
                bundle["sources"][version_key] += "," + source_value
            else:
                bundle["sources"][version_key] = source_value
        result[domain] = bundles
    return result


def load_old_profiles() -> dict[str, list[dict[str, Any]]]:
    technique = read_json(OLD_SOURCES["TECHNIQUE"]).get("profiles", [])
    artifact = read_json(OLD_SOURCES["ARTIFACT"]).get("profiles", [])
    consumable = read_json(OLD_SOURCES["CONSUMABLE"])
    return {
        "TECHNIQUE": [x for x in technique if isinstance(x, dict)],
        "ARTIFACT": [x for x in artifact if isinstance(x, dict)],
        "PILL": [x for x in consumable.get("pills", []) if isinstance(x, dict)],
        "CONSUMABLE": [x for x in consumable.get("consumables", []) if isinstance(x, dict)],
    }


def compile_catalog() -> dict[str, Any]:
    crosswalk = read_json(CROSSWALK)
    colors, argbs = load_palette()
    old = load_old_profiles()
    raw_indexes = {
        domain: index_raw(version, entry_type)
        for domain, (version, entry_type) in RAW_TYPES.items()
    }
    profiles: list[dict[str, Any]] = []
    for domain in ("TECHNIQUE", "ARTIFACT", "PILL", "CONSUMABLE"):
        for source_profile in old[domain]:
            profile_id = norm(source_profile.get("id"))
            if not profile_id:
                continue
            raw = source_profile if domain == "TECHNIQUE" else choose_raw(
                raw_indexes[domain].get(profile_id, []), source_profile)
            profiles.append(make_profile(domain, source_profile, raw, colors, argbs))
    new_profiles, new_aliases, new_source_paths = compile_new_domains(crosswalk, colors, argbs)
    profiles.extend(new_profiles)
    profiles.sort(key=lambda item: (DOMAIN_ORDER[item["domain"]], item["id"]))
    aliases = []
    for alias in crosswalk.get("aliases", []):
        if not isinstance(alias, dict):
            continue
        domain = norm(alias.get("domain")).upper()
        source = norm(alias.get("alias"))
        target = norm(alias.get("target"))
        if domain and source and target:
            aliases.append({
                "domain": domain,
                "alias": source,
                "key": f"{domain.lower()}:{source}",
                "target": target,
                "target_key": f"{domain.lower()}:{target}",
            })
    aliases.extend(new_aliases)
    aliases.sort(key=lambda item: (DOMAIN_ORDER.get(item["domain"], 99), item["alias"]))
    source_paths = [
        SOURCE_DIR / f"item_descriptions_v{version}.json" for version in range(118, 123)
    ] + list(OLD_SOURCES.values()) + [CROSSWALK, SOURCE_DIR / "visual_style_v118.json"]
    source_paths.extend(new_source_paths)
    source_paths.extend(
        SOURCE_DIR / f"item_descriptions_v{version}.json" for version in range(118, 123)
    )
    source_paths.extend(
        SOURCE_DIR / name for name in (
            "visual_effect_sheets_v120.json", "visual_fx_pipeline_v121.json",
            "visual_look_cards_v122.json", "visual_storyboards_v119.json",
        )
    )
    # De-duplicate while preserving stable order for reproducible hashes.
    source_paths = list(dict.fromkeys(source_paths))
    source_hashes = {str(path.relative_to(ROOT)): digest(path) for path in source_paths}
    counts = {domain: sum(1 for profile in profiles if profile["domain"] == domain) for domain in DOMAIN_ORDER}
    expected = dict(CANONICAL_COUNTS)
    expected["TECHNIQUE"] = len(old["TECHNIQUE"])
    if counts != expected:
        raise ValueError(f"canonical visual counts mismatch: expected {expected}, got {counts}")
    invalid_families = sorted({profile["family"] for profile in profiles
                               if profile["family"] not in RUNTIME_FAMILIES})
    invalid_motifs = sorted({profile["motif"] for profile in profiles
                             if profile["motif"] not in RUNTIME_MOTIFS})
    if invalid_families or invalid_motifs:
        raise ValueError(f"renderer enum closure failed: families={invalid_families}, motifs={invalid_motifs}")
    return {
        "schema_version": 3,
        "description": "全量作者法术效果与其余视觉资料的统一类型化运行时目录",
        "source_hashes": source_hashes,
        "palette": {
            key: {"rgb": colors[key], "argb": argbs[key]}
            for key in sorted(colors)
        },
        "budgets": {
            "max_concurrent_particle_systems": 4,
            "max_vignette_ticks": 40,
            "max_intensity": 64,
        },
        "counts": counts,
        "profile_count": len(profiles),
        "canonical_counts": expected,
        "raw_visual_counts": crosswalk.get("raw_card_counts", {}),
        "unmapped_visuals": crosswalk.get("unmapped_visuals", {}),
        "fallback_recipe_version": "deterministic_v1",
        "aliases": aliases,
        "collision_resolutions": crosswalk.get("collision_resolutions", {}),
        "profiles": profiles,
    }


def encoded(catalog: dict[str, Any]) -> str:
    return json.dumps(catalog, ensure_ascii=False, indent=2) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="fail if generated output is stale")
    args = parser.parse_args()
    catalog = compile_catalog()
    content = encoded(catalog)
    if args.check:
        if not OUTPUT.exists() or OUTPUT.read_text(encoding="utf-8") != content:
            print(f"stale generated file: {OUTPUT.relative_to(ROOT)}")
            return 1
        print(f"authored visual catalog is current: {len(catalog['profiles'])} profiles")
        return 0
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(content, encoding="utf-8")
    print(f"wrote {OUTPUT.relative_to(ROOT)} ({len(catalog['profiles'])} profiles)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
