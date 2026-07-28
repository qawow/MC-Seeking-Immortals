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
    ("cauldron_vessel", ("青铜鼎", "铜鼎", "巨鼎", "宝鼎", "炼丹鼎", "灵鼎", "双鼎", "黑鼎", "小鼎",
                         "鼎中", "鼎上", "鼎外")),
    ("alchemy_furnace", ("丹炉", "紫色铜炉", "巨大火炉", "火炉中")),
    ("bell_chime", ("黑色小钟", "金钟", "铜钟", "灵钟", "宝钟", "古钟", "黑钟", "小钟", "梵钟",
                    "陶羽银铃铛", "银铃铛", "这铃铛俨然", "一口黄钟", "黄钟只是")),
    ("magic_gong", ("黑色巨锣",)),
    ("gourd_vessel", ("葫芦", "玉葫芦", "宝葫芦", "灵葫芦", "玉瓶", "宝瓶", "灵瓶", "净瓶", "玉净瓶", "掌天瓶", "小瓶", "瓶影")),
    ("magic_boat", ("脚下的神风舟却白光一闪", "神风舟就开始飞行的歪歪扭扭",
                    "金舟微微一颤下", "黑色巨舟顿时破空声一响", "血红小舟")),
    ("ritual_bowl", ("聚魂钵", "漆黑钵盂", "乌黑圆钵", "碧绿圆钵", "银色圆钵", "圆钵", "钵盂")),
    ("ritual_altar", ("七八丈高的碧绿石台", "巨大祭坛同时散发", "整个高台仿佛被银浆浇筑",
                      "整个聚星台骤然大亮", "一方不大不小的金色高台", "整座金色高台顿时光芒大作",
                      "八个高台上的巨幡", "祭坛中白光一闪", "祭坛上石傀儡")),
    ("ritual_lamp", ("手捧古灯",)),
    ("ritual_coffin", ("寒玉棺", "石棺中")),
    ("magic_staff", ("乌黑禅杖", "降魔巨杖", "降魔杖", "此宝就化为十余丈之长，水缸般粗细的庞然巨物")),
    ("magic_vajra", ("大晋金刚杵", "金刚杵")),
    ("puppet_figure", ("士兵打扮的玩偶", "真人大小的士兵傀儡", "傀儡士兵的光矢",
                       "十余头傀儡兽和傀儡士兵", "黑黝黝的木人", "数寸大小的傀儡",
                       "人形白玉傀儡", "人形傀儡", "石人残骸", "祭坛上石傀儡",
                       "青甲傀儡", "黑乎乎的铁傀儡", "铜人傀儡")),
    ("magic_bow", ("手持一把大弓",)),
    ("magic_ruyi", ("玉如意", "白色如意", "空中的如意", "刻在如意一侧")),
    ("magic_hook", ("摄魂钩", "钩锁神识边缘")),
    ("magic_whip", ("火鞭", "纤细的兽筋", "丝线状兽筋", "兽筋一绷一拉", "兽筋一下弹射而出")),
    ("magic_rope", ("捆仙绳术", "缚鬼索", "黑青色绳索", "银焰绳索", "五根粗大火索",
                    "数根粗大火索", "粗大火索", "几根金索", "金索", "红绳捆绑成一团")),
    ("magic_box", ("金属方盒", "黑色长匣", "黑匣", "玉盒", "木盒", "方盒", "玉匣", "匣盖",
                   "刺目红芒先从盒中")),
    ("magic_fan", ("三焰扇", "青色羽扇", "羽扇", "从扇子狂涌而出", "从扇上浮现",
                   "三色光焰从扇面上", "三色火焰在扇面上", "单手持扇", "三色火柱从扇面上")),
    ("magic_umbrella", ("玉伞",)),
    ("magic_brick", ("淡蓝色晶砖", "金砖", "火红砖块")),
    ("light_curtain", ("光幕", "光墙", "光帘", "五色光幕", "护幕", "屏障光", "一片光华", "光幕一", "光幕竟", "蓝色霞光", "五色霞光", "霞光一片")),
    ("halo_ring", ("光环", "光圈", "圆环光", "血色光环", "金光圈", "光环一", "光环凭空",
                   "一圈圈淡蓝色光晕", "粗大灵环")),
    ("magic_ruler", ("混元尺", "银色巨尺", "巨大银尺", "半截银尺", "巨尺", "尺影")),
    ("magic_scissors", ("金色剪刀", "青色剪刀")),
    ("banner_streamer", ("阴罗幡", "巨幡", "幡旗", "杆幡", "黑幡", "灵幡", "宝幡", "玉竹幡", "竹幡", "血幡")),
    ("seal_stamp", ("法印一闪", "大印一压", "大印落下", "法印一", "玉印", "宝印", "印诀", "金印", "血印", "印玺")),
    ("command_token", ("黑色令牌", "长老令牌", "禁制令牌", "令牌上血光", "手中令牌",
                       "金色令牌", "从令牌一面", "金银令牌", "玉牌")),
    ("magic_mask", ("一张青色面具",)),
    ("seal_cage", ("铁笼", "黑笼", "金色囚笼")),
    ("bridge_arc", ("虹桥", "光桥", "玉桥", "虹桥光", "长虹光", "七彩桥", "光桥一")),
    ("giant_sword", ("一口薄如纸片，金光闪闪，一口狭窄奇长，青光灿灿，最后一口倒是奇厚无锋，漆黑如墨",
                     "巨大光剑", "巨剑", "石剑")),
    ("flying_sword", ("青竹蜂云剑", "飞剑", "青色小剑", "金色小剑", "小剑", "小青剑", "口剑", "剑影", "剑气纵横", "剑芒", "剑刃飞", "飞出一剑", "剑光分化", "分化出", "剑光在其", "化为三十六道", "化为七十二")),
    ("fire_plume", ("赤红火焰", "真火", "赤焰", "火焰从", "火柱", "火海", "烈焰", "三昧真火", "丹火", "青焰", "火球爆",
                    "婴火", "银色火焰", "黑青色火焰", "五色寒焰", "五种颜色各异寒焰",
                    "银白火焰便如飞鸟一般飞入丹炉", "熊熊银焰", "银焰翻滚的巨大火炉")),
    ("formation_banner", ("阵旗", "令旗", "旗阵", "大旗", "战旗", "令旗一", "小旗", "法旗", "宽大白旗")),
    ("formation_disc", ("阵旗、阵盘", "阵旗阵盘", "是阵盘", "蓝光闪闪的阵盘",
                        "青光阵盘", "海面之上的阵盘", "法盘", "移星子母盘",
                        "这块阵盘终于爆裂", "北斗天星盘")),
    ("pagoda_tower", ("宝塔", "浮屠", "金塔", "玉塔", "玲珑塔", "小塔", "塔影")),
    ("rune_pillar", ("巨大水晶柱", "青铜柱子全都", "青铜巨柱", "金色巨柱", "巨大石柱", "八只圆柱")),
    ("blood_thread", ("血丝", "精血丝", "血线", "血丝一", "血丝密", "淡红丝线", "红丝")),
    ("jade_slip", ("玉简", "青色玉简", "古玉简", "玉简一闪", "传法玉简")),
    ("magic_scroll", ("黑色画卷", "金色卷轴", "卷轴之上")),
    ("magic_cloth", ("一块四方锦帕", "锦帕的古怪符文", "一块黑色轻纱", "血色披风")),
    ("burning_talisman", ("符纸", "符火", "符焰", "燃符", "焚符", "符纸燃烧", "高阶符箓")),
    ("talisman_brush", ("蓝濛濛灵符笔", "灵符笔", "笔尖处")),
    ("spirit_qin", ("通体晶莹的白色古琴", "白色古琴")),
    ("ghost_head", ("巨大鬼头", "狰狞鬼头", "鬼头虚影", "骷髅头虚影", "巨型骷髅头", "白骨骷髅头", "巨大骷髅头", "巨大白骨头", "骷髅头", "鬼头", "鬼首")),
    ("spiked_shield", ("奇特的护盾",)),
    ("shield_plate", ("龟壳法器", "铜盾", "巨大血色光盾", "血色光盾",
                      "巨大盾牌", "银盾", "盾牌", "盾面", "冰玉盾", "剑盾",
                      "元罡盾", "盾击反击", "器盾")),
    ("spirit_armor", ("赤金色的精美战甲", "精美之极的全身战甲", "漆黑如墨的黑色战甲",
                      "黄色战甲在体表一现浮出", "三色花纹的古朴战甲",
                      "式样奇特的战甲", "深黄色的战甲")),
    ("flying_blade", ("黑色匕首", "短刃", "所有银刀", "晶刃", "银刃",
                      "厚背长刀", "长刀虚影", "飞刀", "宝刀", "魔刀", "血刀",
                      "刀影", "巨刃", "月刃", "弯刀")),
    ("giant_axe", ("神念巨斧", "晶莹巨斧", "黑色巨斧", "巨斧", "战斧", "大斧", "斧影")),
    ("giant_hammer", ("八骷髅锤", "绿焰锤", "单手提着一柄大锤", "一柄大锤", "黑色巨锤", "巨大锤", "巨锤", "大锤", "锤影", "锤子")),
    ("spiked_club", ("狼牙棒",)),
    ("giant_claw", ("巨爪", "鬼爪", "火焰鬼爪")),
    ("giant_hand", ("巨手", "大手", "巨掌", "佛掌", "血掌", "擎天手")),
    ("serpent_dragon", ("青龙虚影", "巨龙虚影", "龙形虚影", "五爪青龙", "怪蛟虚影", "蛟影一吸收", "龙影", "火龙", "水龙", "雷龙", "冰龙", "风龙", "血龙", "蛟龙", "青色雷蛟", "金龙缠绕", "金色蟠龙", "火蛇", "灵蛇", "雷蛇", "青蛇", "黑蛇", "巨蛇", "蟒蛇", "巨蟒", "毒蛇")),
    ("flame_bird", ("火鸟", "炎鸟", "朱雀", "凤凰", "火凤", "金乌")),
    ("beast_phantom", ("十余头傀儡兽", "四只傀儡兽", "晶莹玉鼠",
                       "兽影", "虎影", "巨猿", "魔猿", "麒麟", "玄武", "白虎")),
    ("lotus_mandala", ("莲花", "白莲", "金莲", "青莲", "血莲", "莲台", "莲瓣")),
    ("wheel_disc", ("宝轮", "法轮", "光轮", "圆盘", "轮盘", "日轮", "月轮", "圆轮", "指环",
                    "漆黑圆环", "黑乎乎的圆环", "黑乎乎圆环", "银色圆环", "淡黄色手镯", "巨大云环",
                    "骨戒", "戒指的变化")),
    ("mirror_disc", ("宝镜", "古镜", "镜光", "镜面", "水镜", "明镜")),
    ("sword_rain", ("剑雨", "剑阵", "密密麻麻剑")),
    ("ice_prison", ("冰牢", "冰狱", "冰封", "冰墙", "玄冰", "寒冰")),
    ("blood_sea", ("血海", "血河", "血池", "血浪", "血潮")),
    ("tree_avatar", ("巨树", "古树", "树影", "树根", "藤蔓")),
    ("spatial_rift", ("空间裂", "虚空裂", "撕裂空间", "破碎虚空", "空间波动")),
    ("lightning_storm", ("雷海", "雷云", "千雷", "雷柱", "雷暴", "雷雨", "雷霆")),
    ("mountain_meteor", ("山岳", "山峰", "巨山", "陨石", "流星", "坠星")),
    ("insect_swarm", ("虫云", "虫群", "蜂群", "蚁群", "噬金虫", "巨型甲虫", "所有灵虫")),
    ("tidal_wave", ("巨浪", "海浪", "浪潮", "海潮", "洪水", "水幕", "水墙", "海啸")),
    ("magnetic_field", ("元磁", "磁力", "磁光", "磁场", "两极",
                        "灰色光晕。此光晕滴溜溜一转")),
    ("scripture_glyph", ("经文", "书卷", "典籍", "墨字", "金文", "古字", "符字", "真言")),
    ("wing_fan", ("双翼", "羽翼", "翅膀", "羽翅", "风雷翅", "血红晶翅")),
    ("spear_spike", ("金针", "银针", "灵针", "飞针", "金色巨钉", "长矛", "冰矛", "冰锥", "地刺", "骨刺", "蓝色细刺",
                     "晶枪", "冰枪", "火枪", "水枪", "雷枪", "短枪", "长箭", "光箭", "巨箭",
                     "黑色弩箭", "短戈", "长戈", "金戈", "骨矛")),
    ("orb_projectile", ("黑色晶球", "白色晶球", "青色晶球", "蓝色晶球",
                        "晶莹珠子", "黑白圆石", "黑色晶核", "晶球",
                        "光球", "火球", "雷球", "水球", "圆球", "灵珠", "宝珠", "圆珠")),
    ("eye_gaze", ("法目", "灵目", "天眼", "灵眼", "竖目", "瞳孔", "目光")),
    ("sound_wave", ("梵音", "禅音", "魔啸", "长啸", "怒吼", "咆哮", "钟鸣", "钟音", "铃声", "传音", "声波法则")),
    ("fist_barrage", ("拳影", "掌影")),
    ("cloud_vortex", ("漩涡", "云团", "血云", "黑云")),
    ("rune_orbit", ("符文", "符箓", "符印", "法阵", "阵法", "阵图")),
    ("chain_net", ("锁链", "电网", "锁链网", "铁链", "灵链", "缚仙索", "缠丝成网",
                   "巨网", "丝网", "光网", "雷网", "火网", "成千上万根纤细银丝")),
    ("beam_lance", ("光柱", "光束", "剑虹", "剑光")),
    ("projectile_swarm", ("飞射", "箭雨", "无数", "密密麻麻")),
    ("body_aura", ("鳞片", "金身", "铠甲", "护体")),
    ("spirit_avatar", ("法相", "鬼影", "骷髅", "化身", "人形", "女子", "高大人影", "五具骨架",
                       "高大的黑色巨人")),
    ("ground_field", ("地面", "大地", "领域", "地网")),
    ("mist_veil", ("迷雾", "云雾", "雾幕", "烟幕", "雾气腾", "黑雾", "血雾", "毒雾", "雾团", "隐踪匿迹", "隐匿身形")),
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
    if "法盘顿时光芒大放" in text and "五色光阵" in text:
        return "ORBIT"
    if "下方雾海一阵翻滚" in text and "显露出一条数丈宽的通道" in text:
        return "EXPAND"
    if "数股神念之力突然从这几名守卫身上放出" in text:
        return "TRACK"
    if "五口黑色匕首" in text and "黑色电弧" in text:
        return "TRACK"
    if "十六口短刃同时一闪" in text:
        return "TRACK"
    if "飞出一只拳头大小的黑色晶核" in text:
        return "DIRECT"
    if any(token in text for token in (
            "盘旋", "环绕", "旋转", "回旋", "绕", "青铜柱子全都",
            "一百零八根青铜巨柱", "从烛龙四周蓦然升起",
            "数十道光柱发出惊人轰鸣的拔地而起")):
        return "ORBIT"
    if any(token in text for token in ("螺旋", " spiral", "盘绕")):
        return "SPIRAL"
    if any(token in text for token in (
            "扩散", "爆散", "散开", "铺开", "张开", "无数高耸如林的巨大石柱")):
        return "EXPAND"
    if any(token in text for token in ("汇聚", "聚拢", "收束", "凝聚")):
        return "CONVERGE"
    if any(token in text for token in ("升起", "上升", "腾起", "冲天", "浮起")):
        return "RISE"
    if any(token in text for token in ("坠落", "落下", "砸下", "从天", "向下罩来")):
        return "FALL"
    if any(token in text for token in ("散落", "四射", "飞散", "四处")):
        return "SCATTER"
    if any(token in text for token in ("波动", "波纹", "脉动", "起伏")):
        return "WAVE"
    if any(token in text for token in ("飞向", "射向", "冲向", "扑向", "猛扑而上", "贯穿", "穿过")):
        return "TRACK"
    if any(token in text for token in ("轨迹", "飞射", "射出", "光束", "光柱", "血色披风")):
        return "DIRECT"
    return "STATIC"


def program_primitive_path(primitive: str, text: str, fallback: str) -> str:
    if primitive == "magic_vajra":
        return "DIRECT"
    if primitive == "magic_rope":
        if any(token in text for token in (
                "枷锁", "盘旋", "缠", "捆", "绑", "束缚", "锁住")):
            return "ORBIT"
        return fallback if fallback in {"DIRECT", "TRACK", "EXPAND"} else "STATIC"
    if primitive == "magic_boat":
        return "DIRECT" if any(token in text for token in (
            "一闪而过", "激射而出", "激射而起", "破空声", "消失不见",
        )) else "STATIC"
    if primitive == "ritual_altar":
        return "STATIC"
    if primitive == "puppet_figure":
        return "TRACK" if "轻轻一飘" in text and "墙壁" in text else "STATIC"
    if primitive == "formation_disc" and any(token in text for token in (
            "移星子母盘", "这块阵盘终于爆裂", "北斗天星盘")):
        return "ORBIT"
    if primitive == "wheel_disc" and any(token in text for token in (
            "骨戒", "戒指的变化")):
        return "STATIC"
    if primitive == "blood_thread" and "血红小舟" in text:
        return "DIRECT"
    if primitive == "rune_pillar" and "八只圆柱" in text:
        return "STATIC"
    if primitive == "magic_box":
        return "RISE" if "往半空中一抛" in text else "STATIC"
    if primitive == "magic_ruyi":
        return "STATIC"
    if primitive == "magic_hook":
        return "DIRECT"
    if primitive == "magic_whip" and any(token in text for token in (
            "火鞭", "兽筋一绷一拉", "兽筋一下弹射而出")):
        return "DIRECT"
    if primitive == "spiked_shield":
        return "STATIC"
    if "金色元婴正两只小手乱舞不已" in text:
        if primitive == "spirit_avatar":
            return "STATIC"
        if primitive == "eye_gaze":
            return "ORBIT"
    if "粗若蛟龙的青色电弧" in text:
        if primitive == "banner_streamer":
            return "STATIC"
        if primitive == "lightning_storm":
            return "DIRECT"
    if "八只巨幡滴溜溜一转" in text:
        if primitive == "banner_streamer":
            return "ORBIT"
        if primitive == "giant_sword":
            return "RISE"
    if primitive == "shield_plate" and any(token in text for token in (
            "冰玉盾", "剑盾", "元罡盾", "盾击反击", "器盾")):
        return "STATIC"
    if primitive == "wheel_disc" and "圆轮" in text:
        return "ORBIT"
    if primitive == "spear_spike" and "金色巨钉" in text:
        return "DIRECT"
    if primitive != "giant_sword":
        return fallback
    if any(token in text for token in (
            "六口丈许长金色巨剑", "四柄石剑身上爆发")):
        return "ORBIT"
    if any(token in text for token in (
            "从天上狠斩下来", "从空中沉声落下", "一斩而下",
            "从虚空中一闪的斩下", "半截剑身无声无息的滑落下")):
        return "FALL"
    if "从地下激射而出" in text or "冲天而起" in text:
        return "RISE"
    if any(token in text for token in (
            "斩向", "斩在", "斩过", "划开", "飞斩而去", "一斩而去",
            "虚空一斩", "追到黑烟", "破天而去", "直斩空中",
            "狠狠斩下", "狠狠一斩", "斩击处")):
        return "TRACK"
    return fallback


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
# "七十二" is never reduced to the leading "七". Cap is VisualProgramLayer's 72.
# Longer / more-specific tokens first. 十八 before 三百六十 so a quote that
# names both ("三百六十团…只有十八团明亮") keeps the lit count, not the total.
_COMPOSITE_COPIES = (
    ("一百零八", 72), ("一百八", 72),
    ("七十二口", 72), ("七十二", 72),
    ("三十六口", 36), ("三十六", 36),
    ("二十四", 24),
    ("十八团", 18), ("十八", 18),
    ("十二道", 12), ("十二", 12),
    ("三百六十", 72), ("三百六", 72),
)

_MASS_WEAPON_TERMS = frozenset({
    "冰枪", "水枪", "雷枪", "短枪", "长箭", "光箭", "巨箭",
    "黑色弩箭", "短戈", "长戈", "金戈", "骨矛",
})

_EXACT_LOCAL_PALETTE_TERMS = {
    "magic_boat": frozenset({
        "脚下的神风舟却白光一闪", "神风舟就开始飞行的歪歪扭扭",
        "金舟微微一颤下", "黑色巨舟顿时破空声一响", "血红小舟",
    }),
    "ritual_altar": frozenset({
        "七八丈高的碧绿石台", "巨大祭坛同时散发", "整个高台仿佛被银浆浇筑",
        "整个聚星台骤然大亮", "一方不大不小的金色高台", "整座金色高台顿时光芒大作",
        "八个高台上的巨幡", "祭坛中白光一闪", "祭坛上石傀儡",
    }),
    "puppet_figure": frozenset({
        "士兵打扮的玩偶", "真人大小的士兵傀儡", "傀儡士兵的光矢",
        "十余头傀儡兽和傀儡士兵", "黑黝黝的木人", "数寸大小的傀儡",
        "人形白玉傀儡", "人形傀儡", "石人残骸", "祭坛上石傀儡",
        "青甲傀儡", "黑乎乎的铁傀儡", "铜人傀儡",
    }),
    "magic_rope": frozenset({
        "黑青色绳索", "银焰绳索", "五根粗大火索", "数根粗大火索", "粗大火索",
        "几根金索", "金索", "红绳捆绑成一团",
    }),
    "chain_net": frozenset({"巨网", "丝网", "光网", "雷网", "火网", "成千上万根纤细银丝"}),
    "spear_spike": frozenset({
        "晶枪", "冰枪", "火枪", "水枪", "雷枪", "短枪", "长箭", "光箭", "巨箭",
        "黑色弩箭", "短戈", "长戈", "金戈", "骨矛", "金色巨钉", "蓝色细刺",
    }),
    "wheel_disc": frozenset({
        "漆黑圆环", "黑乎乎的圆环", "黑乎乎圆环", "银色圆环", "淡黄色手镯", "巨大云环", "圆轮", "指环",
        "骨戒", "戒指的变化",
    }),
    "shield_plate": frozenset({
        "龟壳法器", "铜盾", "冰玉盾", "剑盾", "元罡盾", "盾击反击", "器盾",
    }),
    "spiked_shield": frozenset({"奇特的护盾"}),
    "flying_blade": frozenset({"黑色匕首", "短刃", "所有银刀", "晶刃", "银刃"}),
    "orb_projectile": frozenset({
        "黑色晶球", "白色晶球", "青色晶球", "蓝色晶球",
        "晶莹珠子", "黑白圆石", "黑色晶核", "晶球",
    }),
    "beast_phantom": frozenset({"十余头傀儡兽", "四只傀儡兽", "晶莹玉鼠"}),
    "seal_cage": frozenset({"铁笼", "黑笼", "金色囚笼"}),
    "serpent_dragon": frozenset({"金龙缠绕", "金色蟠龙"}),
    "magic_gong": frozenset({"黑色巨锣"}),
    "magic_mask": frozenset({"一张青色面具"}),
    "magic_cloth": frozenset({"一块四方锦帕", "锦帕的古怪符文", "一块黑色轻纱", "血色披风"}),
    "rune_pillar": frozenset({"巨大水晶柱", "青铜柱子全都", "青铜巨柱", "金色巨柱", "巨大石柱", "八只圆柱"}),
    "spirit_armor": frozenset({
        "赤金色的精美战甲", "精美之极的全身战甲", "漆黑如墨的黑色战甲",
        "黄色战甲在体表一现浮出", "三色花纹的古朴战甲",
        "式样奇特的战甲", "深黄色的战甲",
    }),
}

_EXACT_LOCAL_PALETTE_FALLBACKS = {
    "magic_boat": "qi", "ritual_altar": "earth", "puppet_figure": "qi",
    "magic_rope": "qi",
    "chain_net": "qi", "spear_spike": "qi", "wheel_disc": "qi", "seal_cage": "metal",
    "shield_plate": "earth", "spiked_shield": "wood",
    "flying_blade": "qi", "orb_projectile": "qi",
    "beast_phantom": "qi",
    "magic_gong": "yin", "magic_mask": "water", "magic_cloth": "qi",
    "rune_pillar": "earth", "spirit_armor": "metal", "serpent_dragon": "metal",
}


def _copies_from_text(text: str) -> int:
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


def program_copies(text: str, evidence_terms: list[str] | tuple[str, ...] = ()) -> int:
    # Bind counts through an object classifier instead of leaking unrelated numerals
    # (百颗火球 / one ghost, or one three-headed six-armed ape) across the quote.
    if evidence_terms:
        for term in evidence_terms:
            if term.startswith(("无数", "漫天", "铺天盖地", "密密麻麻", "千", "万")):
                return _copies_from_text(term)
            if term == "双鼎":
                return 2
            offset = 0
            while True:
                index = text.find(term, offset)
                if index < 0:
                    break
                prefix = text[max(0, index - 20):index]
                if term == "巨型甲虫" and any(token in text for token in ("上千只", "千余只")):
                    return 20
                if term == "五具骨架":
                    return 5
                if term == "青铜柱子全都":
                    return 72
                if term == "巨大石柱" and "无数高耸如林" in text:
                    return 20
                if term in {"巨大石柱", "粗大灵环"} and "数十道光柱" in text:
                    return 12
                if term == "成千上万根纤细银丝":
                    return 20
                if term == "所有灵虫":
                    return 20
                if term == "金龙缠绕" and "七根金色巨柱" in text:
                    return 7
                if term in _MASS_WEAPON_TERMS:
                    weapon_prefix = text[max(0, index - 40):index]
                    if re.search(r"(?:无数|千余)(?:根)?[^，。；：、]{0,20}$", weapon_prefix):
                        return 20
                    if re.search(r"(?:数百余|一根根|一杆杆|一支支)[^，。；：、]{0,24}$", weapon_prefix):
                        return 12
                    if term == "雷枪" and re.search(r"六杆[^，。；：、]{0,12}$", weapon_prefix):
                        return 6
                    if term == "短枪" and re.search(r"十二杆[^，。；：、]{0,20}$", weapon_prefix):
                        return 12
                    if term in {"短戈", "金戈"} and (
                            re.search(r"三柄[^，。；：、]{0,16}$", weapon_prefix)
                            or "三道金芒" in text):
                        return 3
                    if term == "长戈" and "九具傀儡" in text and "银色长戈" in text:
                        return 9
                if term == "青色雷蛟":
                    local_count = re.search(r"(?P<count>六|两)条[^，。；：、]{0,16}$", prefix)
                    if local_count:
                        return _copies_from_text(local_count.group("count"))
                count = re.search(
                    r"(?P<count>一百零八|一百八|三百六十|三百六|七十二|三十六|二十四|十八|十二|数百|上百|百余|数十|千万|千|万|百|九|八|七|六|五|四|三|两|二|一)"
                    r"(?:口|柄|把|只|颗|枚|道|团|朵|座|条|头|面|杆|株|缕|片|层|尊|具|根|个|束|股|对|排)"
                    r"(?:[赤红橙黄绿青蓝紫黑白金银灰血墨乌碧苍淡深亮暗晶莹剔透大小巨迷怪异狰狞粗细长短灵魔鬼阴阳玄]{0,8}|(?:车轮|拳头|脸盆|拇指)(?:般)?大小的?)$",
                    prefix)
                if count:
                    return _copies_from_text(count.group("count"))
                mass = re.search(
                    r"(?P<count>无数|漫天|铺天盖地|密密麻麻)"
                    r"[赤红橙黄绿青蓝紫黑白金银灰血墨乌碧苍淡深亮暗晶莹剔透大小巨迷怪异狰狞粗细长短灵魔鬼阴阳玄]{0,8}$",
                    prefix)
                if mass:
                    return _copies_from_text(mass.group("count"))
                offset = index + len(term)
        return 1
    return _copies_from_text(text)


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


# Color-word map used by 由X转Y / 自X转Y gradient extraction (must stay inside the
# 11-key v118 palette — never invent ARGB ints).
_COLOR_WORD = (
    ("绿金", "heal"), ("紫金", "thunder"), ("紫电", "thunder"), ("赤金", "metal"),
    ("金红", "fire"), ("紫红", "fire"), ("血红", "fire"), ("血色", "fire"),
    ("苍青", "wood"), ("碧青", "wood"), ("青金", "metal"), ("银白", "metal"),
    ("淡金", "metal"), ("淡蓝", "water"), ("深蓝", "water"), ("湛蓝", "water"),
    ("玄黑", "yin"), ("墨黑", "yin"), ("乌黑", "yin"), ("雪白", "qi"),
    ("乳白", "qi"), ("澄蓝", "water"), ("蓝白", "water"), ("青绿", "wood"),
    ("银金", "metal"), ("青白", "qi"), ("淡青", "qi"), ("橙红", "fire"),
    ("赤红", "fire"), ("金色", "metal"), ("青色", "wood"), ("红色", "fire"),
    ("蓝色", "water"), ("绿色", "wood"), ("黑色", "yin"), ("白色", "qi"),
    ("紫色", "yin"), ("火", "fire"), ("赤", "fire"), ("红", "fire"),
    ("金", "metal"), ("水", "water"), ("蓝", "water"), ("木", "wood"),
    ("碧", "wood"), ("绿", "wood"), ("土", "earth"), ("褐", "earth"),
    ("冰", "water"), ("寒", "water"), ("黑", "yin"), ("白", "qi"),
    ("紫", "yin"), ("雷", "thunder"), ("阴", "yin"), ("魂", "soul"),
    ("毒", "poison"),
)


def _color_key_at(text: str, index: int, argbs: dict[str, int]) -> str | None:
    """Longest-match color token starting at index."""
    best = None
    best_len = 0
    for token, key in _COLOR_WORD:
        if key not in argbs:
            continue
        if text.startswith(token, index) and len(token) > best_len:
            best = key
            best_len = len(token)
    return best


def program_palette(text: str, fallback: str, argbs: dict[str, int]) -> tuple[str, str]:
    # Prefer explicit gradient phrases: 由红转金 / 自赤转金 / 从绿变乌黑.
    import re
    for match in re.finditer(r"(?:由|自|从|继而由|继而自)(.{1,6}?)(?:转|变|化作|化为)(.{1,6})", text):
        left = match.group(1)
        right = match.group(2)
        pk = sk = None
        for i in range(len(left)):
            pk = _color_key_at(left, i, argbs)
            if pk:
                break
        for i in range(len(right)):
            sk = _color_key_at(right, i, argbs)
            if sk:
                break
        if pk and sk and pk != sk:
            return pk, sk
    keys = []
    for token, key in PROGRAM_COLOR_RULES:
        if token in text and key in argbs and key not in keys:
            keys.append(key)
    primary = keys[0] if keys else fallback
    secondary = keys[1] if len(keys) > 1 else fallback
    return primary, secondary


_PRIMITIVE_PALETTE_FALLBACK = {
    "giant_sword": "qi",
    "magic_boat": "qi",
    "ritual_bowl": "yin",
    "ritual_altar": "earth",
    "ritual_lamp": "qi",
    "ritual_coffin": "yin",
    "magic_ruler": "qi",
    "magic_staff": "metal",
    "magic_vajra": "qi",
    "puppet_figure": "qi",
    "magic_bow": "metal",
    "magic_ruyi": "qi",
    "magic_hook": "yin",
    "magic_whip": "earth",
    "magic_rope": "qi",
    "magic_box": "earth",
    "spiked_shield": "wood",
    "magic_fan": "qi",
    "magic_umbrella": "qi",
    "magic_scissors": "metal",
    "command_token": "qi",
    "magic_brick": "metal",
    "bell_chime": "metal",
    "magic_scroll": "metal",
    "formation_disc": "qi",
    "spiked_club": "metal",
    "talisman_brush": "water",
    "spirit_qin": "qi",
}

_LOCAL_PALETTE_PRIMITIVES = frozenset({
    "giant_sword",
    "magic_boat", "ritual_bowl", "ritual_altar", "ritual_lamp", "ritual_coffin",
    "magic_ruler", "magic_staff", "magic_vajra", "puppet_figure",
    "magic_bow", "magic_ruyi", "magic_hook", "magic_whip", "magic_rope", "magic_box",
    "magic_fan", "magic_umbrella", "magic_scissors", "command_token",
    "magic_brick", "bell_chime", "magic_scroll", "formation_disc", "spiked_club",
    "spiked_shield",
    "talisman_brush", "spirit_qin", "giant_hammer",
})


def program_palette_source(text: str, primitive: str, matched_terms: list[str]) -> str:
    """Bind colors to the matched object instead of unrelated objects in the quote."""
    exact_terms = _EXACT_LOCAL_PALETTE_TERMS.get(primitive, frozenset())
    exact_matches = [term for term in matched_terms if term in exact_terms]
    if (primitive not in _LOCAL_PALETTE_PRIMITIVES and not exact_matches) or not matched_terms:
        return text
    term = exact_matches[0] if exact_matches else matched_terms[0]
    index = text.find(term)
    if index < 0:
        return text
    if primitive == "giant_hammer":
        # Its white skulls and green flames deliberately follow the hammer noun.
        return text[max(0, index - 20):min(len(text), index + len(term) + 48)]
    if primitive in {"magic_rope", "chain_net"}:
        # Rope/net colours are often carried by their strands, not by the noun.
        return text[max(0, index - 48):min(len(text), index + len(term) + 12)]
    if primitive in {"seal_cage", "wheel_disc", "spear_spike"}:
        return text[max(0, index - 16):index + len(term)]
    # Object colors are adjectival. Keep the window tight enough that a nearby
    # mountain, aura, or target cannot recolor a separately authored implement.
    return text[max(0, index - 6):index + len(term)]


_FIGURE_PRIMITIVES = {
    "cauldron_vessel", "alchemy_furnace", "bell_chime", "gourd_vessel", "magic_boat",
    "light_curtain", "halo_ring", "ritual_bowl", "ritual_altar", "ritual_lamp",
    "ritual_coffin", "magic_ruler", "magic_staff", "magic_vajra", "puppet_figure",
    "magic_bow", "magic_ruyi", "magic_hook", "magic_whip", "magic_rope", "magic_box",
    "magic_fan", "magic_umbrella", "magic_scissors",
    "banner_streamer", "seal_stamp", "command_token", "magic_brick", "seal_cage", "bridge_arc",
    "magic_gong", "magic_mask", "magic_cloth", "rune_pillar", "spirit_armor",
    "flying_sword", "giant_sword", "fire_plume", "formation_banner",
    "pagoda_tower", "blood_thread", "jade_slip", "magic_scroll", "burning_talisman",
    "formation_disc", "spiked_club",
    "talisman_brush", "spirit_qin",
    "ghost_head", "shield_plate", "spiked_shield", "flying_blade", "giant_axe", "giant_hammer",
    "giant_claw", "giant_hand", "serpent_dragon", "flame_bird", "beast_phantom",
    "lotus_mandala", "wheel_disc", "mirror_disc", "sword_rain", "ice_prison",
    "blood_sea", "tree_avatar", "spatial_rift", "lightning_storm", "mountain_meteor",
    "insect_swarm", "tidal_wave", "spirit_avatar", "pagoda_tower",
}


# Named techniques assembled from several text-material sheets often have generic frame
# prose on most quotes. Their id is the only stable source evidence for the implement.
_PROFILE_PRIMITIVE_OVERRIDES = {
    "immortal_rope": ("magic_rope",),
    "ghost_bind": ("magic_rope",),
    "soul_banner_wave": ("banner_streamer",),
    "miaoyin_zither_domain": ("spirit_qin", "sound_wave"),
    "kunwu_tower_bind": ("pagoda_tower", "rune_orbit"),
}


def program_primitives(text: str, base_shape: str,
                       profile_id: str = "") -> tuple[list[str], list[str]]:
    selected: list[str] = []
    evidence: list[str] = []
    for primitive, terms in PROGRAM_RULES:
        hits = [term for term in terms if term in text]
        if hits:
            selected.append(primitive)
            evidence.extend(hits[:3])
    secondary = (
        # Narrowed in 0.2.200: bare 光芒/灵光 used to force aura_burst on almost every
        # quote that only mentioned light as an adjective. Keep only explicit mote bursts.
        # Template cards often say "光点/闪白" generically — require concrete burst verbs.
        ("aura_burst", ("光点迸", "光屑飞", "星点洒", "闪白光点爆", "掌心聚光点", "光点密布")),
        ("mist_veil", ("迷雾笼罩", "烟幕弥漫", "余烬飘散", "雾气腾腾")),
        ("mist_veil", ("丹炉顶部的空隙中，开始升起一团淡紫色雾气", "丹炉内紫雾由淡转浓")),
        ("mist_veil", ("化为一团黑色雾气将其身形一罩",)),
        ("mist_veil", ("下方雾海一阵翻滚",)),
        ("impact_arcs", ("爆裂开来", "轰然爆开", "爆散而开", "震碎虚空", "五道粗大金弧")),
        ("impact_arcs", ("一团刺目爆裂而开", "刺目耀眼的光团")),
        ("layered_afterimages", ("残影重重", "层层虚影")),
        ("afterimage_path", ("人就化为一股轻风", "瞬间化为一缕清风从莲影中",
                             "最终化为了一道几乎淡若不见的虚影", "血色披风",
                             "黑袍妇人正在拼命飞遁中")),
        ("magnetic_field", ("灰色光浪从底部滚滚而出", "粗大灰霞飞卷而上")),
        ("blade_arc", ("凭空被斩切了开来",)),
        ("blade_arc", ("巨剑的猛击之下", "光片丝毫停顿没有")),
        ("sound_wave", ("狼牙棒一挥，顿时一股狂风呼啸", "声波法则",
                        "一声雷鸣后，剪刀化为", "发出金属碰撞般的尖鸣")),
        ("channel_stream", ("数股神念之力突然从这几名守卫身上放出",)),
        ("chain_net", ("数十丈范围的银色光丝都处在了此环的控制之下",)),
        ("projectile_swarm", ("三种不同电弧就同时击在了雷兽身体上",)),
        ("spirit_avatar", ("浑身生满妖目的淡银色佛像",)),
        ("eye_gaze", ("浑身生满妖目的淡银色佛像",)),
        ("summon_gate", ("十余头傀儡兽和傀儡士兵",
                         "分别召唤出红黄两只小狼")),
        ("lightning_storm", (
            "剑面上弹射出了数十道淡金色的细长电弧",
            "剑身表面金光狂闪，无数电弧狂涌而出",
            "雷电巨剑", "粗大金色电弧", "紫色火焰和金色电弧",
            "金色电弧在剑身上浮现", "无数电弧闪动不已",
            "化为一道金色雷电", "耀目的金色电弧一下弹射而出",
        )),
        ("serpent_dragon", ("两条电蟒摇头摆尾的往青色巨剑", "化为一条栩栩如生的电蟒")),
        ("fire_plume", ("巨剑表面一层银焰冒出", "剑上泛出的紫色火焰")),
        ("chain_net", ("上百条灰丝出来。众丝线", "密密麻麻金丝终于靠拢")),
        ("spatial_rift", ("黑乎乎的细长裂缝", "长约十几丈的空间缝隙")),
        ("afterimage_path", ("翠绿长虹破天而去", "化为一道金紫色惊虹",
                             "化为一道十丈长的青虹")),
        ("layered_afterimages", ("飞射出一把一般无二的巨剑",)),
        ("ground_field", ("巨剑开山",)),
        ("blade_arc", ("巨剑门重劈",)),
        ("projectile_swarm", ("百余道青光聚到一起", "化为两道金光同时激射向下",
                              "数十根青丝一闪即逝的从虚空中激射而出",
                              "一道道迷蒙的青色灵光如剑般飞出")),
        ("rune_orbit", ("传送阵的一角",)),
        ("flying_blade", ("六口金刃骤然间融合一体",)),
    )
    for primitive, terms in secondary:
        hits = [term for term in terms if term in text]
        if hits and primitive not in selected:
            selected.append(primitive)
            evidence.extend(hits[:2])
    profile_override = _PROFILE_PRIMITIVE_OVERRIDES.get(profile_id, ())
    for primitive in reversed(profile_override):
        selected = [current for current in selected if current != primitive]
        selected.insert(0, primitive)
    if profile_override:
        evidence.append("forced:profile:" + profile_id)
    if "刺目红芒先从盒中爆发" in text and "impact_arcs" not in selected:
        selected.append("impact_arcs")
        evidence.append("forced:box_light_burst")
    if "紫色铜炉" in text and "婴火" in text:
        selected = [primitive for primitive in selected if primitive != "cloud_vortex"]
        evidence.append("forced:furnace_fire_cloud")
    if "magic_vajra" in selected and "impact_arcs" not in selected:
        selected.append("impact_arcs")
        evidence.append("forced:vajra_impact")
    if "shield_plate" in selected and any(token in text for token in (
            "盾击反击", "器盾")) and "impact_arcs" not in selected:
        selected.append("impact_arcs")
        evidence.append("forced:shield_impact")
    if "一个玉盒内的金色书页" in text:
        if "scripture_glyph" not in selected:
            selected.append("scripture_glyph")
        selected = [primitive for primitive in selected if primitive != "projectile_swarm"]
        evidence.append("forced:boxed_scripture")
    if "金色元婴正两只小手乱舞不已" in text and "乳白色木盒" in text:
        for primitive in ("spirit_avatar", "eye_gaze"):
            if primitive not in selected:
                selected.append(primitive)
        evidence.append("forced:box_nascent_soul_eyes")
    if "一叠法旗" in text and "一层青色霞光" in text:
        if "light_curtain" not in selected:
            selected.append("light_curtain")
        evidence.append("forced:banner_curtain")
    if "六团黑色光球" in text and "一团魔云在空中形成" in text:
        if "cloud_vortex" not in selected:
            selected.append("cloud_vortex")
        evidence.append("forced:black_orbs_cloud")
    if "八只巨幡滴溜溜一转" in text and "青色光剑瞬间竖立而起" in text:
        if "giant_sword" not in selected:
            selected.append("giant_sword")
        selected = [primitive for primitive in selected if primitive != "projectile_swarm"]
        evidence.append("forced:eight_banner_sword")
    if "粗若蛟龙的青色电弧" in text:
        selected = [primitive for primitive in selected if primitive != "serpent_dragon"]
        if "lightning_storm" not in selected:
            selected.append("lightning_storm")
        evidence.append("forced:single_banner_arc")
    if "幡面上浮现出一个直径数尺的大洞" in text:
        if "banner_streamer" not in selected:
            selected.insert(0, "banner_streamer")
        evidence.append("forced:banner_portal_face")
    if "圆轮之上，二十四团半透明符纹" in text:
        if "rune_orbit" not in selected:
            selected.append("rune_orbit")
        evidence.append("forced:wheel_twenty_four_runes")
    if "chain_net" in selected and any(token in text for token in (
            "丝网般的无数白痕", "自投罗网", "蛛网般爬满")):
        selected.remove("chain_net")
    if ("giant_sword" in selected and "巨剑门" in text
            and "巨剑门重劈" not in text):
        selected.remove("giant_sword")
    if "puppet_figure" in selected:
        selected = ["puppet_figure"] + [
            primitive for primitive in selected
            if primitive != "puppet_figure"
            and (primitive != "spirit_avatar"
                 or any(token in text for token in ("元婴", "圭灵")))
        ]
        if "黑黝黝的木人" in text:
            selected = [primitive for primitive in selected if primitive != "eye_gaze"]
        evidence.append("forced:puppet_body")
    if "magic_boat" in selected:
        selected = ["magic_boat"] + [
            primitive for primitive in selected if primitive != "magic_boat"
        ]
        evidence.append("forced:moving_boat")
    if not selected:
        # Avoid dumping every underspecified card onto aura_burst: pick a calmer default
        # from the text when possible.
        if any(t in text for t in ("护体", "周身", "金身", "鳞片", "铠甲")):
            selected.append("body_aura")
        elif any(t in text for t in ("遁", "身形", "隐", "影遁", "残影")):
            selected.append("afterimage_path")
        elif any(t in text for t in ("阵", "符", "法阵")):
            selected.append("rune_orbit")
        else:
            selected.append(base_shape if base_shape in PROGRAM_SHAPES else "aura_burst")
        evidence.append("profile_shape:" + (base_shape or selected[-1]))
    # Force sword-centric quotes onto flying_sword as the lead silhouette.
    swordish = any(tok in text for tok in (
        "飞剑", "小剑", "口剑", "剑芒", "剑影", "剑诀", "青元剑", "青竹剑", "液态飞剑",
        "剑光分化", "一模一样的剑", "真假难辨的剑", "指剑", "刃指", "小剑阵", "剑阵"))
    if swordish and "giant_sword" not in selected:
        if "flying_sword" not in selected:
            selected.insert(0, "flying_sword")
            evidence.append("forced:flying_sword")
        selected = ["flying_sword"] + [p for p in selected if p != "flying_sword"]
        # Suppress co-mentioned generic beam/swarm/rain/aura/mist so the sword quote
        # does not spend its 4-layer budget on non-sword geometry.
        suppress = {
            "beam_lance", "sword_rain", "projectile_swarm", "aura_burst",
            "mist_veil", "chain_net", "impact_arcs", "single_projectile",
        }
        selected = [p for p in selected if p not in suppress or p == "flying_sword"]
        # Preserve narrowly evidenced companions without turning sword similes such as
        # "剑影如山峰" or "剑群如蜂群" into literal mountains or insect swarms.
        keep_extra = {"lotus_mandala", "sword_rain", "projectile_swarm", "fire_plume",
                      "light_curtain", "spirit_avatar", "ghost_head", "wheel_disc",
                      "ritual_bowl", "magic_ruler", "magic_staff", "giant_hammer",
                      "bell_chime", "magic_whip"}
        selected = [p for p in selected if p == "flying_sword" or p in keep_extra]
    elif "giant_sword" in selected:
        explicit_small_swords = any(token in text for token in (
            "飞剑", "小剑", "口剑", "青竹蜂云剑", "青色小剑", "金色小剑"))
        if not explicit_small_swords:
            selected = [p for p in selected if p != "flying_sword"]
        elif "flying_sword" in selected:
            selected = [p for p in selected if p != "projectile_swarm"]
        selected = ["giant_sword"] + [p for p in selected if p != "giant_sword"]
        if any(token in text for token in (
                "上百条灰丝出来。众丝线", "密密麻麻金丝终于靠拢")):
            selected = [p for p in selected if p != "projectile_swarm"]
        if any(token in text for token in (
                "无数电弧狂涌而出", "无数电弧闪动不已")):
            selected = [p for p in selected if p != "projectile_swarm"]
    if "bell_chime" in selected and any(token in text for token in ("铃铛", "黄钟")):
        if any(token in text for token in ("声波法则", "钟音")) and "sound_wave" not in selected:
            selected.append("sound_wave")
        selected = ["bell_chime"] + [p for p in selected if p != "bell_chime"]
    if "wheel_disc" in selected and any(token in text for token in (
            "漆黑圆环", "黑乎乎的圆环", "黑乎乎圆环", "银色圆环", "淡黄色手镯",
            "巨大云环", "骨戒", "戒指的变化")):
        selected = ["wheel_disc"] + [p for p in selected if p != "wheel_disc"]
    if "formation_disc" in selected and "五色光阵" in text and "rune_orbit" not in selected:
        selected.append("rune_orbit")
    if "orb_projectile" in selected and "黑白圆石" in text:
        selected = [p for p in selected if p != "projectile_swarm"]
    if "rune_pillar" in selected:
        selected = ["rune_pillar"] + [p for p in selected if p != "rune_pillar"]
    # The eight skulls are mounted details of the authored green-flame hammer. Rendering
    # a second free-floating ghost-head swarm would duplicate the same source object.
    if "giant_hammer" in selected and "锤" in text:
        selected = [p for p in selected if p not in {"ghost_head", "spirit_avatar"}]
    if "ghost_head" in selected and "spirit_avatar" in selected and not any(
            token in text for token in ("法相", "鬼影", "化身", "人形", "女子", "骨架")):
        selected.remove("spirit_avatar")
    # Prefer authored figure silhouettes as the lead layer.
    figures = [p for p in selected if p in _FIGURE_PRIMITIVES]
    others = [p for p in selected if p not in _FIGURE_PRIMITIVES]
    ordered = figures + others
    # When a figure leads, drop pure-generic secondaries that only restate light/fog.
    if ordered and ordered[0] in _FIGURE_PRIMITIVES:
        generic = {"aura_burst", "mist_veil", "impact_arcs", "layered_afterimages"}
        head, tail = ordered[0], [p for p in ordered[1:] if p not in generic]
        if head in {"alchemy_furnace", "command_token"} and "mist_veil" in ordered[1:]:
            tail.append("mist_veil")
        if head == "magic_gong":
            tail = [primitive for primitive in tail if primitive != "eye_gaze"]
        if head == "magic_cloth":
            tail = [primitive for primitive in tail if primitive != "projectile_swarm"]
            if "impact_arcs" in ordered[1:] and any(token in text for token in (
                    "五道粗大金弧", "锦帕的古怪符文银光一亮")):
                tail.append("impact_arcs")
        if head == "rune_pillar" and "无数高耸如林的巨大石柱" in text:
            tail = [primitive for primitive in tail if primitive != "projectile_swarm"]
        if head == "spirit_armor":
            tail = [primitive for primitive in tail if primitive != "projectile_swarm"]
            if "mist_veil" in ordered[1:] and "血雾" in text:
                tail.append("mist_veil")
        if head == "magic_rope":
            tail = [primitive for primitive in tail if primitive != "projectile_swarm"]
        if (head == "magic_box" and "刺目红芒先从盒中爆发" in text
                and "impact_arcs" in ordered[1:]):
            tail.append("impact_arcs")
        if (head == "banner_streamer" and profile_id == "soul_banner_wave"
                and "mist_veil" in ordered[1:]):
            tail.append("mist_veil")
        if head == "giant_sword":
            if ("layered_afterimages" in ordered[1:]
                    and "飞射出一把一般无二的巨剑" in text):
                tail.append("layered_afterimages")
            if ("impact_arcs" in ordered[1:]
                    and any(token in text for token in ("爆裂开来", "刺目耀眼的光团"))):
                tail.append("impact_arcs")
        if head == "magic_vajra" and "impact_arcs" in ordered[1:]:
            tail.append("impact_arcs")
        if head == "shield_plate" and "impact_arcs" in ordered[1:] and any(
                token in text for token in ("盾击反击", "器盾")):
            tail.append("impact_arcs")
        if head == "wheel_disc" and "impact_arcs" in ordered[1:] and any(
                token in text for token in ("爆裂开来", "爆裂开", "爆裂")):
            tail.append("impact_arcs")
        if head == "formation_disc" and "impact_arcs" in ordered[1:] and "爆裂" in text:
            tail.append("impact_arcs")
        ordered = [head] + tail
    return ordered[:4], evidence[:10]


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
    profile_id = norm(profile.get("id"))
    fallback_palette = norm(profile.get("palette_key")) or "qi"
    layers: list[dict[str, Any]] = []
    seen_quote_layers = 0
    for source_index, source in enumerate(sources):
        matching = [index for index, event in enumerate(timeline) if clean(event.get("source")) == source]
        event_index = matching[0] if matching else source_index % len(timeline)
        event = timeline[event_index]
        trigger = clean(event.get("trigger"))
        primitive_ids, evidence = program_primitives(source, base_shape, profile_id)
        path = program_path(source)
        motion = program_motion(source, inferred)
        radius, length, height = program_scale(source, float(profile.get("radius", 0.9) or 0.9))
        digest = hashlib.sha256(f"{profile.get('id')}:{source_index}:{source}".encode("utf-8")).digest()
        phase = int.from_bytes(digest[:2], "big") % 360
        speed = 0.3 if "缓" in source or "慢" in source else 1.0
        if any(token in source for token in ("骤", "疾", "瞬", "一闪")):
            speed = 2.0
        spread = 180.0 if path in {"SCATTER", "EXPAND"} else (360.0 if path == "ORBIT" else 18.0)
        rotation = 360.0 if path in {"ORBIT", "SPIRAL"} else (phase if path == "WAVE" else 0.0)
        for primitive_index, primitive in enumerate(primitive_ids):
            matched_terms = [term for rule, terms in PROGRAM_RULES if rule == primitive
                             for term in terms if term in source]
            exact_local_palette = any(
                term in _EXACT_LOCAL_PALETTE_TERMS.get(primitive, frozenset())
                for term in matched_terms)
            primitive_fallback = (
                _EXACT_LOCAL_PALETTE_FALLBACKS[primitive]
                if exact_local_palette else _PRIMITIVE_PALETTE_FALLBACK.get(primitive, fallback_palette))
            palette_source = program_palette_source(source, primitive, matched_terms)
            if primitive == "giant_sword":
                if "石剑" in source:
                    primary_key = "earth"
                    secondary_key = "yin" if "黑色光芒" in source else "earth"
                elif "巨大光剑" in source and "金、黑、红三色交错" in source:
                    primary_key, secondary_key = "metal", "fire"
                elif "一口薄如纸片，金光闪闪" in source:
                    primary_key = secondary_key = "metal"
                elif any(token in source for token in (
                        "青色巨剑", "青色光剑瞬间竖立而起", "绿色的巨剑", "青光巨剑", "翠绿长虹",
                        "墨绿之光", "青色光柱", "青濛濛", "青光濛濛")):
                    primary_key = "wood"
                    secondary_key = "thunder" if any(token in source for token in (
                        "金色雷剑", "金色电弧", "金弧", "霹雳")) else "wood"
                elif "雷电巨剑" in source:
                    primary_key, secondary_key = "thunder", "yin"
                elif "雪白巨剑" in source:
                    primary_key, secondary_key = "qi", "water"
                elif "银色巨剑" in source:
                    primary_key = secondary_key = "qi"
                elif any(token in source for token in ("金色巨剑", "金色霞光", "金色雷电")):
                    primary_key = "metal"
                    secondary_key = "thunder" if any(token in source for token in (
                        "雷", "电弧", "金弧")) else "metal"
                elif "血剑" in source:
                    primary_key = secondary_key = "fire"
                elif "紫色火焰" in source:
                    primary_key, secondary_key = "metal", "fire"
                elif "金紫色惊虹" in source:
                    primary_key, secondary_key = "metal", "yin"
                elif any(token in source for token in ("金色电弧", "金弧", "霹雳")):
                    primary_key, secondary_key = "metal", "thunder"
                elif any(token in source for token in ("淡金色雷弧", "淡金色的细长电弧")):
                    primary_key, secondary_key = "metal", "thunder"
                elif "巨剑表面一层银焰" in source:
                    primary_key, secondary_key = "qi", "fire"
                elif "百丈巨剑" in source and "青龙上人" in source:
                    primary_key = secondary_key = "wood"
                elif "空间缝隙" in source and "巨剑斩击处" in source:
                    primary_key = secondary_key = "wood"
                else:
                    primary_key, secondary_key = program_palette(
                        palette_source, primitive_fallback, argbs)
            elif primitive == "lightning_storm" and "giant_sword" in primitive_ids:
                primary_key, secondary_key = "metal", "thunder"
            elif (primitive == "lightning_storm"
                    and "粗若蛟龙的青色电弧" in source):
                primary_key, secondary_key = "wood", "thunder"
            elif primitive == "serpent_dragon" and "电蟒" in source:
                primary_key, secondary_key = "metal", "thunder"
            elif (primitive in {"orb_projectile", "flame_bird", "fire_plume"}
                    and "giant_sword" in primitive_ids and "银色火球" in source):
                primary_key, secondary_key = "qi", "fire"
            elif (primitive in {"flying_sword", "projectile_swarm", "beam_lance"}
                    and "giant_sword" in primitive_ids
                    and any(token in source for token in (
                        "这些小剑围着他身体", "六道青光却清鸣发出",
                        "七道青光从手中飞射而出", "百余道青光聚到一起",
                        "一道道迷蒙的青色灵光如剑般飞出",
                        "数十根青丝一闪即逝的从虚空中激射而出",
                        "七十二青色小剑", "七十二口青色小剑",
                        "七十二口青濛濛小剑", "所有青色飞剑",
                        "一道碗口粗的青色光柱"))):
                primary_key = secondary_key = "wood"
            elif (primitive == "afterimage_path"
                    and "化为一道十丈长的青虹" in source):
                primary_key = secondary_key = "wood"
            elif (primitive in {"beam_lance", "projectile_swarm"}
                    and "斩在了黑色光柱上" in source):
                primary_key = secondary_key = "yin"
            elif (primitive in {"beam_lance", "projectile_swarm"}
                    and "无数黑色符文" in source and "一道粗大乌光" in source):
                primary_key = secondary_key = "yin"
            elif primitive == "magic_boat" and "血红小舟" in source:
                primary_key = secondary_key = "fire"
            elif primitive == "magic_boat" and "金舟" in source:
                primary_key = secondary_key = "metal"
            elif primitive == "magic_boat" and "黑色巨舟" in source:
                primary_key = secondary_key = "yin"
            elif primitive == "magic_boat":
                primary_key = secondary_key = "qi"
            elif primitive == "ritual_altar" and "碧绿石台" in source:
                primary_key = secondary_key = "wood"
            elif primitive == "ritual_altar" and any(token in source for token in (
                    "银浆浇筑", "聚星台")):
                primary_key = secondary_key = "qi"
            elif primitive == "ritual_altar" and "金色高台" in source:
                primary_key = secondary_key = "metal"
            elif primitive == "ritual_altar":
                primary_key = secondary_key = "earth"
            elif primitive == "puppet_figure" and "黑黝黝的木人" in source:
                primary_key, secondary_key = "yin", "wood"
            elif primitive == "puppet_figure" and "人形白玉傀儡" in source:
                primary_key = secondary_key = "qi"
            elif primitive == "puppet_figure" and any(token in source for token in (
                    "石人残骸", "祭坛上石傀儡")):
                primary_key = secondary_key = "earth"
            elif primitive == "puppet_figure" and "黑乎乎的铁傀儡" in source:
                primary_key, secondary_key = "yin", "water"
            elif primitive == "puppet_figure" and "青甲傀儡" in source:
                primary_key, secondary_key = "water", "metal"
            elif primitive == "puppet_figure" and "铜人傀儡" in source:
                primary_key = secondary_key = "metal"
            elif primitive == "puppet_figure" and "铁甲" in source:
                primary_key = secondary_key = "metal"
            elif primitive == "puppet_figure" and "银光一闪" in source:
                primary_key, secondary_key = "qi", "metal"
            elif primitive == "puppet_figure":
                primary_key = secondary_key = "qi"
            elif primitive == "wheel_disc" and any(token in source for token in (
                    "骨戒", "戒指的变化")):
                primary_key = secondary_key = "qi"
            elif primitive == "formation_disc" and any(token in source for token in (
                    "移星子母盘", "这块阵盘终于爆裂", "北斗天星盘")):
                primary_key = secondary_key = "qi"
            elif primitive == "formation_banner" and "宽大白旗" in source:
                primary_key = secondary_key = "qi"
            elif primitive == "wing_fan" and "血红晶翅" in source:
                primary_key = secondary_key = "fire"
            elif primitive == "spear_spike" and "蓝色细刺" in source:
                primary_key = secondary_key = "water"
            elif primitive == "ghost_head" and "骨戒" in source:
                primary_key = secondary_key = "qi"
            elif primitive == "blood_thread" and "血红小舟" in source:
                primary_key = secondary_key = "fire"
            elif primitive == "rune_pillar" and "八只圆柱" in source:
                primary_key = secondary_key = "earth"
            elif primitive == "fire_plume" and "五种颜色各异寒焰" in source:
                primary_key, secondary_key = "qi", "water"
            elif primitive == "magic_rope" and profile_id == "ghost_bind":
                primary_key = secondary_key = "yin"
            elif primitive == "magic_rope" and "黑青色绳索" in source:
                primary_key, secondary_key = "yin", "wood"
            elif primitive == "magic_rope" and "银焰绳索" in source:
                primary_key, secondary_key = "qi", "fire"
            elif primitive == "magic_rope" and "金索" in source:
                primary_key = secondary_key = "metal"
            elif primitive == "magic_rope" and any(token in source for token in (
                    "火索", "红绳", "红丝")):
                primary_key = secondary_key = "fire"
            elif primitive == "magic_rope":
                primary_key = secondary_key = "qi"
            elif primitive == "ritual_bowl" and "聚魂钵" in source:
                primary_key = secondary_key = "yin"
            elif primitive == "cauldron_vessel" and "铜鼎" in source:
                primary_key = secondary_key = "metal"
            elif (primitive == "cauldron_vessel"
                    and profile_id in {"technique_530", "technique_921", "technique_1023"}):
                primary_key = secondary_key = "metal"
            elif primitive == "alchemy_furnace" and "紫色铜炉" in source:
                primary_key = secondary_key = "yin"
            elif primitive == "fire_plume" and "黑青色火焰" in source:
                primary_key, secondary_key = "yin", "wood"
            elif primitive == "fire_plume" and "银色火焰" in source:
                primary_key, secondary_key = "qi", "fire"
            elif primitive == "fire_plume" and "五色寒焰" in source:
                primary_key, secondary_key = "qi", "water"
            elif primitive == "fire_plume" and "婴火" in source:
                primary_key = secondary_key = "fire"
            elif (primitive in {"spirit_qin", "sound_wave"}
                    and profile_id == "miaoyin_zither_domain"):
                primary_key = secondary_key = "yin"
            elif (primitive in {"pagoda_tower", "rune_orbit"}
                    and profile_id == "kunwu_tower_bind"):
                primary_key = secondary_key = "earth"
            elif primitive == "banner_streamer" and profile_id == "soul_banner_wave":
                primary_key = secondary_key = "yin"
            elif primitive == "chain_net" and "上百条灰丝出来。众丝线" in source:
                primary_key = secondary_key = "yin"
            elif primitive == "chain_net" and "密密麻麻金丝终于靠拢" in source:
                primary_key = secondary_key = "metal"
            elif primitive == "blood_thread" and "脖颈处一丝血线" in source:
                primary_key = secondary_key = "fire"
            elif (exact_local_palette and primitive == "rune_pillar"
                    and "巨大水晶柱" in matched_terms):
                primary_key, secondary_key = "water", "qi"
            elif (exact_local_palette and primitive == "rune_pillar"
                    and any("青铜" in term for term in matched_terms)):
                primary_key = secondary_key = "metal"
            elif (exact_local_palette and primitive == "rune_pillar"
                    and "数十道光柱" in source):
                primary_key = secondary_key = "qi"
            elif (exact_local_palette and primitive == "spirit_armor"
                    and "赤金色的精美战甲" in matched_terms):
                primary_key, secondary_key = "metal", "qi"
            elif (exact_local_palette and primitive == "spirit_armor"
                    and "五色羽衣" in source):
                primary_key, secondary_key = "qi", "fire"
            elif (exact_local_palette and primitive == "spirit_armor"
                    and any(token in source for token in ("漆黑如墨", "黑气一闪"))):
                primary_key = secondary_key = "yin"
            elif (exact_local_palette and primitive == "spirit_armor"
                    and any(token in source for token in ("黄色战甲", "深黄色的战甲"))):
                primary_key = secondary_key = "earth"
            elif (exact_local_palette and primitive == "spirit_armor"
                    and "三色花纹" in source):
                primary_key = secondary_key = "qi"
            elif (exact_local_palette and primitive == "spear_spike"
                    and any(term in matched_terms for term in ("冰枪", "水枪"))):
                primary_key = secondary_key = "water"
            elif exact_local_palette and primitive == "spear_spike" and "火枪" in matched_terms:
                primary_key = secondary_key = "fire"
            elif exact_local_palette and primitive == "spear_spike" and "雷枪" in matched_terms:
                primary_key, secondary_key = "thunder", "metal" if "金色" in source else "thunder"
            elif (exact_local_palette and primitive == "spear_spike"
                    and "长箭" in matched_terms and "青色长箭" in source):
                primary_key = secondary_key = "wood"
            elif primitive == "spear_spike" and "金色巨钉" in source:
                primary_key = secondary_key = "metal"
            elif (primitive == "chain_net"
                    and "数十丈范围的银色光丝都处在了此环的控制之下" in source):
                primary_key = secondary_key = "qi"
            elif exact_local_palette and primitive == "chain_net" and "银色火网" in source:
                primary_key, secondary_key = "qi", "fire"
            elif exact_local_palette and primitive == "chain_net" and "黑色雷网" in source:
                primary_key, secondary_key = "yin", "thunder"
            elif exact_local_palette and primitive == "chain_net" and "金色雷网" in source:
                primary_key, secondary_key = "metal", "thunder"
            elif exact_local_palette and primitive == "chain_net" and "黑蓝" in source:
                primary_key, secondary_key = "yin", "water"
            elif exact_local_palette and primitive == "chain_net" and "蓝色丝网" in source:
                primary_key = secondary_key = "water"
            elif exact_local_palette and primitive == "chain_net" and "青色丝网" in source:
                primary_key = secondary_key = "wood"
            elif exact_local_palette and primitive == "chain_net" and "红色丝网" in source:
                primary_key = secondary_key = "fire"
            elif exact_local_palette and primitive == "chain_net" and "灰色丝网" in source:
                primary_key = secondary_key = "yin"
            elif (exact_local_palette and primitive == "chain_net"
                    and "黑色细线" in source and "火网" in source):
                primary_key, secondary_key = "yin", "fire"
            elif (exact_local_palette and primitive == "chain_net"
                    and "青濛濛剑丝" in source and "巨网" in source):
                primary_key = secondary_key = "wood"
            elif (exact_local_palette and primitive == "chain_net"
                    and any(token in source for token in ("迎着巨网一抓", "触手形成的巨网"))):
                primary_key = secondary_key = primitive_fallback
            elif exact_local_palette and primitive == "chain_net" and "煞气" in source:
                primary_key = secondary_key = "soul"
            elif exact_local_palette and primitive == "wheel_disc" and "指环" in source:
                primary_key = secondary_key = "yin"
            elif exact_local_palette and primitive == "wheel_disc" and "淡黄色手镯" in source:
                primary_key = secondary_key = "earth"
            elif exact_local_palette and primitive == "wheel_disc" and "巨大云环" in source:
                primary_key = secondary_key = "qi"
            elif primitive == "wheel_disc" and "巨大圆轮虚影" in source:
                primary_key, secondary_key = "yin", "water"
            elif primitive == "wheel_disc" and "金色圆轮" in source:
                primary_key = secondary_key = "metal"
            elif primitive == "wheel_disc" and "圆轮" in source:
                primary_key = secondary_key = "qi"
            elif primitive == "spiked_shield":
                primary_key, secondary_key = "wood", "soul"
            elif exact_local_palette and primitive == "shield_plate" and "铜盾" in source:
                primary_key = "metal"
                secondary_key = "earth" if "强烈的黄芒" in source else "metal"
            elif exact_local_palette and primitive == "shield_plate" and "龟壳法器" in source:
                primary_key = secondary_key = "earth"
            elif primitive == "shield_plate" and "冰玉盾" in source:
                primary_key, secondary_key = "water", "qi"
            elif primitive == "shield_plate" and "剑盾" in source:
                primary_key = secondary_key = "metal"
            elif primitive == "shield_plate" and "元罡盾" in source:
                primary_key = secondary_key = "qi"
            elif primitive == "shield_plate" and any(
                    token in source for token in ("盾击反击", "器盾")):
                primary_key = secondary_key = "qi"
            elif (exact_local_palette and primitive == "flying_blade"
                    and "黑色匕首" in source):
                primary_key, secondary_key = "yin", "thunder"
            elif (exact_local_palette and primitive == "flying_blade"
                    and any(token in source for token in ("白色寒气", "晶冰", "冰层"))):
                primary_key, secondary_key = "qi", "water"
            elif exact_local_palette and primitive == "orb_projectile" and "黑色晶球" in source:
                primary_key = "yin"
                secondary_key = "wood" if "绿气" in source else "yin"
            elif exact_local_palette and primitive == "orb_projectile" and "黑白圆石" in source:
                primary_key, secondary_key = "yin", "qi"
            elif exact_local_palette and primitive == "orb_projectile" and "黑色晶核" in source:
                primary_key = secondary_key = "yin"
            elif exact_local_palette and primitive == "orb_projectile" and "青色晶球" in source:
                primary_key = secondary_key = "wood"
            elif exact_local_palette and primitive == "orb_projectile" and "蓝色晶球" in source:
                primary_key = secondary_key = "water"
            elif (exact_local_palette and primitive == "orb_projectile"
                    and any(token in source for token in ("奇寒无比", "白茫茫的奇寒"))):
                primary_key, secondary_key = "water", "qi"
            elif (exact_local_palette and primitive == "orb_projectile"
                    and "血色符文" in source):
                primary_key, secondary_key = "qi", "fire"
            elif (((primitive in {"ritual_bowl", "magic_ruler"})
                    or (exact_local_palette and primitive in {"spear_spike", "wheel_disc", "chain_net"}))
                    and any(token in palette_source for token in ("银色", "银的", "银尺", "银钵"))):
                primary_key = secondary_key = "qi"
            elif primitive == "magic_staff" and "七色佛光" in source:
                primary_key, secondary_key = "metal", "qi"
            elif primitive == "magic_vajra":
                primary_key = secondary_key = "qi"
            elif primitive == "magic_box" and profile_id == "technique_1403":
                primary_key = secondary_key = "metal"
            elif primitive == "magic_box" and profile_id in {"technique_371", "technique_769"}:
                primary_key, secondary_key = "qi", "water"
            elif primitive == "magic_box" and "玉匣" in source:
                primary_key, secondary_key = "qi", "water"
            elif primitive == "magic_box" and "黑" in palette_source:
                primary_key = secondary_key = "yin"
            elif primitive == "magic_box" and "金属方盒" in source:
                primary_key = secondary_key = "metal"
            elif primitive == "magic_box" and "玉盒" in source:
                primary_key = secondary_key = "qi"
            elif primitive == "magic_box" and "乳白色木盒" in source:
                primary_key = secondary_key = "qi"
            elif primitive == "magic_box":
                primary_key = secondary_key = "earth"
            elif (primitive == "spirit_avatar"
                    and "金色元婴正两只小手乱舞不已" in source):
                primary_key = secondary_key = "metal"
            elif (primitive == "eye_gaze"
                    and "附近盘旋飞舞的五色眼珠" in source):
                primary_key = secondary_key = "qi"
            elif (primitive == "scripture_glyph"
                    and "一个玉盒内的金色书页" in source):
                primary_key = secondary_key = "metal"
            elif (primitive == "light_curtain"
                    and "一层青色霞光顿时浮现而出" in source):
                primary_key = secondary_key = "wood"
            elif (primitive == "orb_projectile"
                    and "六团黑色光球" in source):
                primary_key = secondary_key = "yin"
            elif (primitive == "cloud_vortex"
                    and "一团魔云在空中形成" in source):
                primary_key = secondary_key = "yin"
            elif (primitive == "rune_orbit"
                    and "二十四团半透明符纹" in source):
                primary_key = secondary_key = "qi"
            elif primitive == "magic_bow" and "箭矢状红芒" in source:
                primary_key, secondary_key = "metal", "fire"
            elif primitive == "magic_ruyi" and "红黄两色玉如意" in source:
                primary_key, secondary_key = "fire", "earth"
            elif (primitive == "magic_ruyi" and "数团精血" in source
                    and "白色光霞" in source):
                primary_key, secondary_key = "fire", "qi"
            elif primitive == "magic_ruyi" and "白色如意" in source:
                primary_key = secondary_key = "qi"
            elif primitive == "magic_hook":
                primary_key = secondary_key = "yin"
            elif primitive == "magic_whip" and "火鞭" in source:
                primary_key = secondary_key = "fire"
            elif primitive == "magic_whip":
                primary_key = secondary_key = "earth"
            elif primitive == "formation_banner" and "赤红法旗" in source:
                primary_key = secondary_key = "fire"
            elif primitive == "formation_banner" and "青色小旗" in source:
                primary_key = secondary_key = "wood"
            elif primitive == "formation_banner" and "黑色小旗" in source:
                primary_key = secondary_key = "yin"
            elif (primitive == "formation_banner"
                    and "重新化为一张符箓和一面小旗坠落而下" in source):
                primary_key = secondary_key = "yin"
            elif primitive == "formation_banner" and "黄色小旗" in source:
                primary_key = secondary_key = "earth"
            elif primitive == "formation_banner" and any(token in source for token in (
                    "五色小旗", "晶莹小旗")):
                primary_key = secondary_key = "qi"
            elif primitive == "formation_banner" and "一叠法旗" in source:
                primary_key = secondary_key = "wood"
            elif primitive == "banner_streamer" and "阴罗幡" in source:
                primary_key, secondary_key = "yin", "wood"
            elif (primitive == "banner_streamer"
                    and "幡面上浮现出一个直径数尺的大洞" in source):
                primary_key, secondary_key = "yin", "wood"
            elif primitive == "banner_streamer" and "巨幡" in source:
                primary_key = secondary_key = "qi"
            elif primitive == "magic_fan" and "青色羽扇" in source:
                primary_key = secondary_key = "wood"
            elif (primitive == "magic_fan" and any(token in source for token in (
                    "三焰扇", "三色火凤", "三色光焰", "三色火焰", "三色火柱"))):
                primary_key, secondary_key = "fire", "qi"
            elif primitive == "magic_umbrella" and "金光在玉伞" in source:
                primary_key, secondary_key = "qi", "metal"
            elif primitive == "magic_scissors" and "青色剪刀" in source:
                primary_key = secondary_key = "thunder"
            elif (primitive == "command_token"
                    and any(token in source for token in ("灰丝", "灰色光浪"))):
                primary_key = secondary_key = "yin"
            elif primitive == "command_token" and "令牌上血光" in source:
                primary_key = secondary_key = "fire"
            elif primitive == "command_token" and "玉牌上最后一层蓝光" in source:
                primary_key = secondary_key = "water"
            elif primitive == "formation_disc" and "青光阵盘" in source:
                primary_key = secondary_key = "wood"
            elif (primitive == "beam_lance" and "一道金色光柱" in source
                    and "白色晶球" in source):
                primary_key = secondary_key = "metal"
            elif (primitive == "beam_lance" and "黑色光柱一击在光团上" in source):
                primary_key = secondary_key = "yin"
            elif (primitive == "rune_orbit" and "无数金银符文" in source
                    and "黑白圆石" in source):
                primary_key, secondary_key = "metal", "qi"
            elif (primitive == "talisman_brush" and "金色符文" in source
                    and "笔尖" in source):
                primary_key, secondary_key = "water", "metal"
            else:
                primary_key, secondary_key = program_palette(
                    palette_source, primitive_fallback, argbs)
            preserve_local_gradient = (
                (primitive == "shield_plate" and "铜盾" in source and "强烈的黄芒" in source)
                or (primitive == "orb_projectile" and any(token in source for token in (
                    "黑白圆石", "奇寒无比", "白茫茫的奇寒")))
                or (primitive == "puppet_figure" and "青甲傀儡" in source)
                or (primitive == "magic_ruyi" and "数团精血" in source
                    and "白色光霞" in source)
            )
            if ((primitive in _LOCAL_PALETTE_PRIMITIVES or exact_local_palette) and matched_terms
                    and primary_key != primitive_fallback and secondary_key == primitive_fallback):
                if (not preserve_local_gradient
                        and not (primitive == "magic_fan" and primary_key == "fire")):
                    secondary_key = primary_key
            copies = program_copies(source, matched_terms)
            if not matched_terms and primitive_index > 0:
                copies = max(1, copies // 2)
            if primitive == "formation_disc" and "法盘" in source:
                copies = 1
            if primitive in {"magic_vajra", "magic_box", "magic_ruyi", "magic_hook", "magic_whip",
                             "spiked_shield"}:
                copies = 1
            if primitive == "magic_boat":
                copies = 1
            if primitive == "ritual_altar":
                copies = 8 if "八个高台上的巨幡" in source else 1
            if primitive == "puppet_figure":
                copies = 1
                if "两只士兵打扮的玩偶" in source:
                    copies = 2
                elif "数个真人大小的士兵傀儡" in source:
                    copies = 5
                elif "黑黝黝的木人" in source:
                    copies = 8
                elif "人形白玉傀儡" in source:
                    copies = 4
                elif "三只傀儡" in source and "青甲傀儡" in source:
                    copies = 3
            if primitive == "magic_rope":
                copies = 1
                if "几根金索" in source or "五根粗大火索" in source:
                    copies = 5
                elif "数根粗大火索" in source:
                    copies = 5
                elif "粗大火索" in source and "巨人双足" in source:
                    copies = 2
            if primitive == "blood_thread":
                if "十根淡淡红丝" in source or "十道红丝" in source:
                    copies = 10
                elif "十余根红丝" in source or "周身红丝" in source:
                    copies = 12
                elif "几根红丝" in source:
                    copies = 5
            if primitive == "formation_banner":
                if "宽大白旗" in source and "四道白光" in source:
                    copies = 4
                elif "四具傀儡" in source and "手中大旗" in source:
                    copies = 4
                elif any(token in source for token in (
                        "十几道光柱从这些小旗", "十几杆五色小旗")):
                    copies = 12
                elif "八块颜色的一个青色小旗" in source:
                    copies = 8
                elif "一叠法旗" in source:
                    copies = 6
            if primitive == "banner_streamer" and any(token in source for token in (
                    "八个高台上的巨幡", "八只巨幡")):
                copies = 8
            if (primitive == "banner_streamer"
                    and "幡面上浮现出一个直径数尺的大洞" in source):
                copies = 1
            if primitive == "shield_plate" and any(token in source for token in (
                    "冰玉盾", "剑盾", "元罡盾", "盾击反击", "器盾")):
                copies = 1
            if primitive == "wheel_disc" and "圆轮" in source:
                copies = 1
            if primitive == "wheel_disc" and any(token in source for token in (
                    "骨戒", "戒指的变化")):
                copies = 5
            if primitive == "spear_spike" and "金色巨钉" in source:
                copies = 5
            if primitive == "spear_spike" and "蓝色细刺" in source:
                copies = 12
            if primitive == "rune_pillar" and "八只圆柱" in source:
                copies = 8
            if (primitive == "scripture_glyph"
                    and "一个玉盒内的金色书页" in source):
                copies = 12
            if (primitive == "spirit_avatar"
                    and "金色元婴正两只小手乱舞不已" in source):
                copies = 1
            if primitive == "eye_gaze" and "五色眼珠" in source:
                copies = 5
            if primitive == "ghost_head" and "骨戒" in source:
                copies = 5
            if primitive == "fire_plume" and "五种颜色各异寒焰" in source:
                copies = 5
            if primitive == "wing_fan" and "血红晶翅" in source:
                copies = 4
            if (primitive == "light_curtain"
                    and "一层青色霞光顿时浮现而出" in source):
                copies = 1
            if primitive == "orb_projectile" and "六团黑色光球" in source:
                copies = 6
            if primitive == "cloud_vortex" and "一团魔云在空中形成" in source:
                copies = 1
            if (primitive == "lightning_storm"
                    and "粗若蛟龙的青色电弧" in source):
                copies = 1
            if primitive == "rune_orbit" and "二十四团半透明符纹" in source:
                copies = 24
            if primitive == "beam_lance" and "八道碗口粗光柱" in source:
                copies = 8
            if (primitive == "projectile_swarm"
                    and "十几团灵光飞射而出" in source):
                copies = 12
            if (primitive == "rune_orbit" and "法盘顿时光芒大放" in source
                    and "五色光阵" in source):
                copies = 1
            if primitive == "shield_plate" and any(
                    token in source for token in ("龟壳法器", "铜盾")):
                copies = 1
            if primitive == "beast_phantom" and "十余头傀儡兽" in source:
                copies = 12
            if primitive == "beast_phantom" and "四只傀儡兽" in source:
                copies = 4
            if primitive == "beast_phantom" and "晶莹玉鼠" in source:
                copies = 1
            if primitive == "beam_lance" and "四道碗口粗的光柱" in source:
                copies = 4
            if primitive == "blade_arc" and "铜盾" in source:
                copies = 1
            if primitive == "wheel_disc" and "指环" in source:
                copies = 1
            if (primitive == "chain_net"
                    and "数十丈范围的银色光丝都处在了此环的控制之下" in source):
                copies = 1
            if primitive == "flying_blade" and "五口黑色匕首" in source:
                copies = 5
            if primitive == "flying_blade" and "十六口短刃" in source:
                copies = 16
            if (primitive == "flying_blade"
                    and "插满了一圈晶莹的短刃" in source):
                copies = 12
            if (primitive == "flying_blade"
                    and any(token in source for token in ("所有银刀", "晶刃", "银刃"))):
                copies = 16
            if (primitive == "projectile_swarm"
                    and "三种不同电弧就同时击在了雷兽身体上" in source):
                copies = 5
            if primitive == "orb_projectile" and any(token in source for token in (
                    "晶球", "晶莹珠子", "黑白圆石", "黑色晶核")):
                copies = 1
            if primitive == "summon_gate" and "十余头傀儡兽和傀儡士兵" in source:
                copies = 1
            if primitive == "giant_sword":
                copies = 1
                if "六口丈许长金色巨剑" in source:
                    copies = 6
                elif ("两道一模一样的巨剑" in source
                      or "两把巨剑同时" in source
                      or "飞射出一把一般无二的巨剑" in source):
                    copies = 2
                elif "四柄石剑身上爆发" in source:
                    copies = 4
            if primitive == "flying_sword" and "这些小剑围着他身体" in source:
                copies = 7
            if primitive == "flying_sword" and "数十口金色飞剑" in source:
                copies = 12
            if primitive == "flying_sword" and "三十六口金色飞剑" in source:
                copies = 36
            if primitive == "flying_sword" and any(token in source for token in (
                    "七十二青色小剑", "七十二口青濛濛小剑", "所有青色飞剑")):
                copies = 72
            if (primitive == "beam_lance" and "其中六道瞬间合成一柄巨大青色巨剑" in source
                    and "另外六道" in source):
                copies = 6
            if primitive == "projectile_swarm" and "七道青光从手中飞射而出" in source:
                copies = 7
            if primitive == "projectile_swarm" and "百余道青光聚到一起" in source:
                copies = 12
            if primitive == "projectile_swarm" and "化为两道金光同时激射向下" in source:
                copies = 2
            if (primitive == "projectile_swarm"
                    and "数十根青丝一闪即逝的从虚空中激射而出" in source):
                copies = 12
            if primitive == "lightning_storm" and "巨剑" in source:
                if "无数电弧" in source:
                    copies = 20
                elif any(token in source for token in (
                        "数十道淡金色的细长电弧", "粗大金色电弧")):
                    copies = 12
                else:
                    copies = 1
            if primitive == "serpent_dragon" and "两条电蟒" in source:
                copies = 2
            if primitive == "serpent_dragon" and "一条栩栩如生的电蟒" in source:
                copies = 1
            if primitive == "chain_net" and "上百条灰丝" in source:
                copies = 12
            if primitive == "chain_net" and "无数金丝切割在巨剑" in source:
                copies = 20
            if primitive == "flying_blade" and "六口金刃骤然间融合一体" in source:
                copies = 6
            if (primitive == "rune_orbit" and "无数黑色符文" in source
                    and "一道粗大乌光" in source):
                copies = 20
            if (primitive in {"beam_lance", "projectile_swarm"}
                    and "无数黑色符文" in source and "一道粗大乌光" in source):
                copies = 1
            if (primitive == "projectile_swarm"
                    and "一道道迷蒙的青色灵光如剑般飞出" in source):
                copies = 12
            if primitive == "afterimage_path" and "化为一道十丈长的青虹" in source:
                copies = 1
            if primitive == "impact_arcs" and "银色圆环" in source:
                copies = 1
            if primitive == "impact_arcs" and "一团刺目爆裂而开" in source:
                copies = 1
            if primitive == "impact_arcs" and "这块阵盘终于爆裂" in source:
                copies = 1
            if primitive == "impact_arcs" and "五道粗大金弧" in source:
                copies = 5
            if primitive == "impact_arcs" and "锦帕的古怪符文银光一亮" in source:
                copies = 1
            if primitive == "afterimage_path" and "血色披风" in source:
                copies = 1
            layer_path = program_primitive_path(primitive, source, path)
            layer_anchor = program_anchor(source, layer_path, trigger)
            layer_spread = 360.0 if layer_path == "ORBIT" else spread
            layer_rotation = 360.0 if layer_path in {"ORBIT", "SPIRAL"} else rotation
            layers.append({
                "layer_index": len(layers),
                "event_ordinal": int(event.get("ordinal", event_index)),
                "primitive": primitive,
                "anchor": layer_anchor,
                "path": layer_path,
                "motion": motion,
                "copies": copies,
                "radius_scale": radius / max(0.1, float(profile.get("radius", 0.9) or 0.9)),
                "length_scale": length,
                "height_scale": height,
                "speed": speed,
                "spread_degrees": layer_spread,
                "rotation_degrees": layer_rotation,
                "vertical_offset": 0.0 if layer_anchor in {"TARGET", "PATH"} else (0.18 if "地面" not in source else 0.03),
                "jitter": 0.06 if motion in {"FLICKER", "PULSE"} else 0.02,
                "primary_argb": int(argbs.get(primary_key, argbs[fallback_palette])),
                "secondary_argb": int(argbs.get(secondary_key, argbs[fallback_palette])),
                "evidence_terms": (matched_terms[:3] or evidence) + ["quote_index:" + str(source_index)],
                "source_quote": source,
                "inferred": inferred,
            })
            giant_variants = None
            if primitive == "giant_sword" and "金、黑、红三色交错" in source:
                giant_variants = (("metal", 0.0), ("yin", 0.0), ("fire", 0.0))
            elif primitive == "giant_sword" and "一口薄如纸片，金光闪闪" in source:
                giant_variants = (("metal", -0.32), ("wood", 0.0), ("yin", 0.32))
            if giant_variants:
                base_layer = layers[-1]
                base_layer["primary_argb"] = int(argbs[giant_variants[0][0]])
                base_layer["secondary_argb"] = int(argbs[giant_variants[0][0]])
                base_layer["vertical_offset"] += giant_variants[0][1]
                base_layer["evidence_terms"] = base_layer["evidence_terms"] + ["color_variant:0"]
                for variant_index, (variant_key, variant_offset) in enumerate(giant_variants[1:], start=1):
                    variant = base_layer.copy()
                    variant["layer_index"] = len(layers)
                    variant["primary_argb"] = int(argbs[variant_key])
                    variant["secondary_argb"] = int(argbs[variant_key])
                    variant["vertical_offset"] = (0.0 if layer_anchor in {"TARGET", "PATH"}
                                                  else (0.18 if "地面" not in source else 0.03)) + variant_offset
                    variant["evidence_terms"] = base_layer["evidence_terms"][:-1] + [
                        "color_variant:" + str(variant_index)]
                    layers.append(variant)
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
