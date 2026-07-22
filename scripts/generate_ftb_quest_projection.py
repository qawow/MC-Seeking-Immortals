#!/usr/bin/env python3
"""Generate deterministic native-stage projection metadata in bundled FTB quests."""

from __future__ import annotations

import argparse
import json
import re
import sys
from dataclasses import dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
QUEST_ROOT = ROOT / "src/main/resources/seeking_immortals/ftbquests/quests"
CHAIN_SOURCE = ROOT / "src/main/resources/data/seeking_immortals/text_material/quest_chains.json"
HOOK_SOURCE = ROOT / "src/main/resources/data/seeking_immortals/text_material/quest_hooks.json"
HANDBOOK_SOURCE = (
    ROOT / "src/main/resources/data/seeking_immortals/text_material/quest_handbook_i18n_v1.json"
)
ZH_LANG_SOURCE = ROOT / "src/main/resources/assets/seeking_immortals/lang/zh_cn.json"
PROJECTION_OUTPUT = (
    ROOT / "src/main/resources/data/seeking_immortals/catalog/ftb_native_stage_projection.json"
)

CHAPTER_CHAINS: dict[str, tuple[str, ...]] = {
    "seeking_immortals_main": (
        "huangfeng_cultivation_path", "qixuan_mortal_path", "blood_forbidden_campaign",
    ),
    "seeking_immortals_chaotic_sea": (
        "chaotic_sea_politics", "void_palace_campaign", "inverse_star_recruit",
        "inverse_star_smuggle_arc", "chaotic_sea_civil_war",
    ),
    "seeking_immortals_dajin_kunwu": (
        "dajin_kunwu_line", "kunwu_mountain_campaign", "dajin_wanbao_route",
        "dajin_clan_line", "dajin_righteous_demon_line",
    ),
    "seeking_immortals_fallen_demon_yin": (
        "ghost_path", "yin_luo_ghost_sect", "fallen_demon_campaign",
        "ancient_demon_line", "nether_river_campaign",
    ),
    "seeking_immortals_mulan_demonic": (
        "mulan_war_campaign", "mulan_tianlan_war", "mulan_fashi_path",
        "tianlan_defense_line", "wutu_mulan_feud_line", "chain_mulan_war_campaign",
        "demonic_six_path", "demonic_six_expanded",
    ),
    "seeking_immortals_spirit_realm_service": (
        "spirit_realm_rise", "tianyuan_merit_path", "chain_tianyuan_enlist",
        "diyuan_campaign", "human_clan_neutral_intro", "spirit_eighteen_clans",
        "spirit_eighteen_pilgrimage", "fengyuan_explorer", "clan_array_mo_line",
        "clan_refinement_yu_line", "clan_alchemy_gu_line", "clan_talisman_ning_line",
        "human_clan_league_hub", "barbarian_kings_line", "barbarian_king_hunt",
    ),
    "seeking_immortals_tiannan_seven_sects": (
        "craft_master", "huadao_blade_path", "giant_sword_gate_path",
        "qianzhu_puppet_path", "yuling_puppet_path", "yanyue_illusion_path",
        "tianfu_talisman_path",
    ),
    "seeking_immortals_star_palace_inverse": (
        "star_palace_internal_politics", "inverse_star_void_heist",
        "chain_void_palace_expedition",
    ),
    "seeking_immortals_ascension_border": (
        "high_realm_endgame", "void_great_cultivation_arc", "diyuan_depth_delve",
        "mortal_to_spirit_bridge", "chain_ascension_spirit_world", "yin_cluster_pilgrim",
        "fallen_demon_expedition", "kunwu_mountain_expedition", "spirit_realm_border",
        "ghost_sect_ban_arc", "chain_seven_sect_outer_to_inner",
    ),
}

ENTRY_BY_CHAPTER = {
    chapter: "quest_native_" + chapter.removeprefix("seeking_immortals_")
    for chapter in CHAPTER_CHAINS
}

MIRROR_PREFIX = "si_native_"
WRITE_PREFIX = "si_native_write_"
READY_PREFIX = "si_native_ready_"
READY_ID_BASE = 3_100_000_000_000_000
MIRROR_ID_BASE = 3_200_000_000_000_000

ADVANCEMENT_TARGETS = {
    "minecraft:story/root": "进入世界并解锁游戏故事根进度",
    "minecraft:story/mine_stone": "取得圆石、深板岩圆石或黑石，达成石器时代进度",
    "minecraft:story/iron_tools": "取得铁镐，达成铁制工具进度",
    "minecraft:story/enter_the_nether": "进入下界，达成勇往直下进度",
    "minecraft:story/follow_ender_eye": "进入要塞范围，达成隔墙有眼进度",
    "minecraft:adventure/root": "参与一次生物战斗，解锁冒险根进度",
    "minecraft:nether/root": "进入下界，解锁下界根进度",
    "minecraft:end/root": "进入末地，解锁末地根进度",
}

ENTITY_NAMES = {
    "minecraft:drowned": "溺尸",
    "minecraft:iron_golem": "铁傀儡",
    "minecraft:pillager": "掠夺者",
    "minecraft:skeleton": "骷髅",
    "minecraft:wither_skeleton": "凋灵骷髅",
}

DIMENSION_NAMES = {
    "seeking_immortals:asura_realm": "修罗界",
    "seeking_immortals:demon_rift": "魔渊裂隙",
    "seeking_immortals:immortal_realm": "仙界",
    "seeking_immortals:nether_river_pocket": "冥河秘境",
    "seeking_immortals:spirit_fengyuan": "风元地域",
    "seeking_immortals:tianyuan": "天渊地域",
    "seeking_immortals:yin_ming_pocket": "阴冥秘境",
}

FACTION_NAMES = {
    "dajin": "大晋",
    "mulan": "慕兰",
    "tianyuan": "天渊",
    "tianlan_temple": "天澜圣殿",
    "inverse_star_alliance": "逆星盟",
    "tiannan_seven": "天南七派",
    "yanyue_sect": "掩月宗",
    "tianfu_gate": "天符门",
    "clan_array_mo": "莫家",
    "clan_refinement_yu": "俞家",
    "clan_alchemy_gu": "谷家",
    "clan_talisman_ning": "宁家",
}

REALM_NAMES = {
    "MORTAL": "凡人",
    "QI_REFINING": "炼气",
    "FOUNDATION": "筑基",
    "CORE_FORMATION": "结丹",
    "NASCENT_SOUL": "元婴",
    "DEITY_TRANSFORMATION": "化神",
    "SPIRIT_SEVERING": "化神",
    "VOID_REFINEMENT": "炼虚",
    "BODY_INTEGRATION": "合体",
    "UNITY": "合体",
    "GREAT_VEHICLE": "大乘",
    "MAHAYANA": "大乘",
    "TRIBULATION_LAND": "渡劫",
    "TRUE_IMMORTAL": "真仙",
}

REGION_NAMES = {
    "tiannan": "天南",
    "chaotic_sea": "乱星海",
    "mulan": "慕兰草原",
    "yinming": "阴冥",
    "tianyuan": "天渊城",
    "spirit_fengyuan": "风元大陆",
    "tiannan_north_waste": "天南荒原（魔道）",
    "barbarian_wasteland": "蛮荒之地",
    "fallen_demon_valley": "坠魔谷",
    "kunwu": "昆吾山",
    "spirit_realm_border": "灵界边境荒原",
    "nether_river": "冥河",
    "tianlan": "天澜草原边境",
}

# Mirrors TextQuestChainService.softRewardsFor. Catalog finale items take precedence.
SOFT_FINALE_REWARDS: dict[str, tuple[tuple[str, int], ...]] = {
    "huangfeng_cultivation_path": (("foundation_building_pill_low", 1), ("alliance_merit_token", 2)),
    "ghost_path": (("yin_stone", 8), ("soul_fragment", 2)),
    "dajin_kunwu_line": (("immortal_jade", 1), ("void_crystal", 1)),
    "chaotic_sea_politics": (("star_palace_tax_receipt", 1), ("spirit_stone_shard", 16)),
    "spirit_realm_rise": (("alliance_merit_token", 3), ("immortal_jade", 1)),
    "mulan_tianlan_war": (("war_contribution_token", 2), ("spirit_stone_shard", 12)),
    "chain_seven_sect_outer_to_inner": (("jade_slip_blank", 1), ("alliance_merit_token", 2)),
    "yin_cluster_pilgrim": (("yin_stone", 12), ("soul_gathering_stone", 1)),
    "inverse_star_recruit": (("void_marrow", 1), ("spirit_stone_shard", 20)),
    "chain_ascension_spirit_world": (("immortal_jade", 1), ("jiangchen_pill", 1)),
    "qixuan_mortal_path": (("spirit_stone_shard", 12), ("spirit_recovery_pill", 2)),
    "blood_forbidden_campaign": (("demonic_blood_coral", 1), ("spirit_stone_shard", 16)),
    "fallen_demon_campaign": (("demonic_blood_coral", 1), ("yin_stone", 8)),
    "void_palace_campaign": (("void_crystal", 1), ("void_marrow", 1)),
    "tianyuan_merit_path": (("alliance_merit_token", 4), ("spirit_stone_shard", 20)),
}
DEFAULT_FINALE_REWARD = (("spirit_stone_shard", 4),)
MAX_REWARD_COUNT = 4096

# These nodes have no native target tags of their own. The aliases are display-only:
# they never create FTB mirror/write tasks and never change the authoritative ledger.
NARRATIVE_TARGET_ALIASES: dict[str, tuple[tuple[str, int], ...]] = {
    "1000000000000501": (("chaotic_sea_politics", 1),),
    "1000000000000502": (("chaotic_sea_politics", 1),),
    "1000000000000505": (("inverse_star_recruit", 2),),
    "1000000000000507": (("inverse_star_smuggle_arc", 1),),
    "1000000000000509": (("inverse_star_smuggle_arc", 3),),
    "1000000000000517": (("chaotic_sea_civil_war", 2),),
    "1000000000000605": (("dajin_righteous_demon_line", 2),),
    "1000000000000702": (("fallen_demon_campaign", 2),),
    "1000000000000704": (("fallen_demon_campaign", 4),),
    "1000000000000706": (("ancient_demon_line", 4),),
    "1000000000000709": (("ghost_path", 2),),
    "1000000000000711": (("yin_luo_ghost_sect", 2),),
    "1000000000000713": (("nether_river_campaign", 2),),
    "1000000000000714": (("nether_river_campaign", 3),),
    "1000000000000715": (("ghost_path", 4),),
    "1000000000000716": (("ghost_path", 5),),
    "1000000000000718": (("nether_river_campaign", 5),),
    "1000000000000820": (("demonic_six_path", 2),),
    "1000000000000822": (("demonic_six_path", 4),),
    "1000000000000824": (("demonic_six_path", 5),),
    "1000000000000825": (("demonic_six_expanded", 4),),
    "1000000000000901": (("mortal_to_spirit_bridge", 1),),
    "1000000000000904": (("tianyuan_merit_path", 2),),
    "1000000000000905": (("chain_tianyuan_enlist", 3),),
    "1000000000000909": (("chain_tianyuan_enlist", 4),),
    "1000000000000932": (("barbarian_king_hunt", 2),),
    "1000000000001003": (("huadao_blade_path", 2),),
    "1000000000001007": (("giant_sword_gate_path", 3),),
    "1000000000001011": (("qianzhu_puppet_path", 4),),
    "1000000000001013": (("yuling_puppet_path", 2),),
    "1000000000001014": (("yuling_puppet_path", 3),),
    "1000000000001104": (("star_palace_internal_politics", 2),),
    "1000000000001106": (("star_palace_internal_politics", 3),),
    "1000000000001108": (("star_palace_internal_politics", 3),),
    "1000000000001120": (("inverse_star_void_heist", 1),),
    "1000000000001122": (("chain_void_palace_expedition", 2),),
    "1000000000001124": (("chain_void_palace_expedition", 4),),
    "1000000000001211": (("yin_cluster_pilgrim", 2),),
    "1000000000001217": (("chain_seven_sect_outer_to_inner", 1),),
    "1000000000001222": (("mortal_to_spirit_bridge", 3),),
    "1000000000001229": (("diyuan_depth_delve", 4),),
}

NARRATIVE_ONLY_QUESTS = {
    "1000000000000907": "天渊灵雨活动系统结算当日修炼增益；不计入原生任务链，也不发放原生任务物品。",
    "1000000000001114": "独立黑市悬赏系统结算赏金；当前未纳入原生任务链，不重复发放任务链奖励。",
    "1000000000001232": "飞升与天劫系统的远期资料节点；当前不计入原生任务链，也不发放原生任务物品。",
}
NARRATIVE_ONLY_TITLES = {
    "1000000000001232": "大乘飞升仙界",
}


@dataclass(frozen=True, order=True)
class Target:
    chain: str
    stage: int


@dataclass
class QuestBlock:
    start: int
    end: int
    text: str
    quest_id: str
    tags: list[str]


@dataclass(frozen=True)
class QuestSources:
    chains: dict[str, dict[str, object]]
    hooks: dict[str, dict[str, object]]
    hook_labels: dict[str, str]
    numeric_stage_labels: dict[str, list[str]]
    item_names: dict[str, str]


def load_sources() -> QuestSources:
    """Load only authored/player-facing sources; IDs remain internal to the pack."""
    chains_payload = json.loads(CHAIN_SOURCE.read_text(encoding="utf-8"))
    hooks_payload = json.loads(HOOK_SOURCE.read_text(encoding="utf-8"))
    handbook = json.loads(HANDBOOK_SOURCE.read_text(encoding="utf-8"))
    lang = json.loads(ZH_LANG_SOURCE.read_text(encoding="utf-8"))
    chains = {
        str(entry["id"]): entry
        for entry in chains_payload.get("chains", [])
        if isinstance(entry, dict) and entry.get("id")
    }
    hooks = {
        str(entry["id"]): entry
        for entry in hooks_payload.get("hooks", [])
        if isinstance(entry, dict) and entry.get("id")
    }
    hook_labels = {
        str(key): str(value)
        for key, value in handbook.get("hook_labels_zh", {}).items()
        if isinstance(value, str) and value.strip()
    }
    numeric_stage_labels: dict[str, list[str]] = {}
    for chain, entries in handbook.get("numeric_stage_labels", {}).items():
        if not isinstance(entries, list):
            continue
        labels = [
            str(entry.get("zh", "")).strip()
            for entry in entries
            if isinstance(entry, dict) and str(entry.get("zh", "")).strip()
        ]
        if labels:
            numeric_stage_labels[str(chain)] = labels
    item_names = {
        key.removeprefix("item.seeking_immortals."): value
        for key, value in lang.items()
        if key.startswith("item.seeking_immortals.") and isinstance(value, str) and value.strip()
    }
    return QuestSources(chains, hooks, hook_labels, numeric_stage_labels, item_names)


def load_chains() -> tuple[list[str], dict[str, int]]:
    payload = json.loads(CHAIN_SOURCE.read_text(encoding="utf-8"))
    order: list[str] = []
    counts: dict[str, int] = {}
    for chain in payload["chains"]:
        chain_id = chain["id"]
        steps = chain.get("steps", [])
        count = len(steps) if isinstance(steps, list) else int(steps)
        if count <= 0:
            raise ValueError(f"Quest chain {chain_id} has no stages")
        order.append(chain_id)
        counts[chain_id] = count
    mapped = {chain for chains in CHAPTER_CHAINS.values() for chain in chains}
    if len(order) != 62 or sum(counts.values()) != 241 or set(order) != mapped:
        raise ValueError("FTB chapter mapping must equal the authoritative 62-chain/241-stage corpus")
    return order, counts


def chain_step_count(entry: dict[str, object]) -> int:
    steps = entry.get("steps", [])
    return len(steps) if isinstance(steps, list) else int(steps or 0)


def safe_source_label(value: object) -> str:
    text = str(value or "").strip()
    if not text or re.search(r"[A-Za-z_./:]", text):
        return ""
    return text


def chain_display(sources: QuestSources, chain: str) -> str:
    value = safe_source_label(sources.chains.get(chain, {}).get("display"))
    return value or "修行任务线"


def stage_display(sources: QuestSources, chain: str, stage: int) -> str:
    entry = sources.chains.get(chain, {})
    numeric = sources.numeric_stage_labels.get(chain, [])
    if 1 <= stage <= len(numeric):
        return numeric[stage - 1]
    steps = entry.get("steps", [])
    if isinstance(steps, list) and 1 <= stage <= len(steps):
        step = steps[stage - 1]
        if isinstance(step, dict):
            summary = safe_source_label(step.get("summary"))
            if summary:
                return summary
            hook = str(step.get("hook", "")).strip()
            if hook and sources.hook_labels.get(hook):
                return sources.hook_labels[hook]
            hook_entry = sources.hooks.get(hook, {})
            display = safe_source_label(hook_entry.get("display"))
            if display:
                return display
        elif isinstance(step, str):
            hook = step.strip()
            label = sources.hook_labels.get(hook, "")
            if not label:
                hook_entry = sources.hooks.get(hook, {})
                label = safe_source_label(hook_entry.get("display"))
            if label:
                return label
    return f"第{stage}阶段"


def target_label(sources: QuestSources, target: Target) -> str:
    return f"{chain_display(sources, target.chain)}：{stage_display(sources, target.chain, target.stage)}"


def start_requirement_text(sources: QuestSources, chain: str) -> str:
    entry = sources.chains.get(chain, {})
    raw_requirements = entry.get("learn_requirements", {})
    start = raw_requirements.get("start", {}) if isinstance(raw_requirements, dict) else {}
    if not isinstance(start, dict):
        start = {}
    clauses: list[str] = []
    realm = str(start.get("realm_min", "")).strip().upper()
    if realm:
        clauses.append(f"境界不低于{REALM_NAMES.get(realm, '指定境界')}")
    region = str(start.get("region", "")).strip().lower()
    if region:
        region_text = REGION_NAMES.get(region, "指定区域")
        if chain == "qixuan_mortal_path" and region == "tiannan":
            region_text += "（青岚山脉新手起点也可）"
        clauses.append(f"当前区域为{region_text}")
    faction = str(start.get("faction", "")).strip().lower()
    if faction and faction != "none":
        clauses.append(f"已加入{FACTION_NAMES.get(faction, '指定宗门或势力')}")
    return "；".join(clauses) if clauses else "无额外境界、区域或宗门门槛"


def parse_targets_in_text(text: str, prefix: str, counts: dict[str, int]) -> list[Target]:
    targets: list[Target] = []
    for tag in re.findall(r'"([a-z0-9_]+)"', text):
        target = parse_target(tag, prefix, counts)
        if target is not None and target not in targets:
            targets.append(target)
    return targets


def native_quest_targets(block: QuestBlock, chapter: str, counts: dict[str, int]) -> list[Target]:
    targets = parse_targets_in_text(block.text, MIRROR_PREFIX, counts)
    targets.extend(target for target in parse_targets_in_text(block.text, READY_PREFIX, counts)
                   if target not in targets)
    targets.extend(target for target in parse_targets_in_text(block.text, WRITE_PREFIX, counts)
                   if target not in targets)
    for chain in CHAPTER_CHAINS[chapter]:
        if chain in block.tags and not any(target.chain == chain for target in targets):
            targets.append(Target(chain, 1))
    return targets


def quest_chain_targets(block: QuestBlock, chapter: str, counts: dict[str, int]) -> list[Target]:
    targets = native_quest_targets(block, chapter, counts)
    if targets:
        return targets
    aliases = NARRATIVE_TARGET_ALIASES.get(block.quest_id)
    if aliases is not None:
        resolved = [Target(chain, stage) for chain, stage in aliases]
        for target in resolved:
            if target.chain not in counts or not 1 <= target.stage <= counts[target.chain]:
                raise ValueError(f"Invalid narrative target alias for quest {block.quest_id}: {target}")
        return resolved
    if block.quest_id in NARRATIVE_ONLY_QUESTS:
        return []
    raise ValueError(
        f"Narrative quest {block.quest_id} has no explicit native-chain alias or narrative-only record"
    )


def validate_narrative_records(chapter_blocks: dict[str, list[QuestBlock]],
                               counts: dict[str, int]) -> None:
    actual = {
        block.quest_id
        for chapter, blocks in chapter_blocks.items()
        for block in blocks
        if not native_quest_targets(block, chapter, counts)
    }
    expected = set(NARRATIVE_TARGET_ALIASES) | set(NARRATIVE_ONLY_QUESTS)
    if actual != expected:
        missing = sorted(actual - expected)
        stale = sorted(expected - actual)
        raise ValueError(f"Narrative quest records differ: missing={missing}, stale={stale}")


def snbt_string(value: str) -> str:
    return json.dumps(value, ensure_ascii=False)


def field_match(block: str, key: str) -> re.Match[str] | None:
    return re.search(rf'(?<![A-Za-z0-9_]){re.escape(key)}:\s*"((?:\\.|[^"\\])*)"', block)


def field_value(block: str, key: str, default: str = "") -> str:
    match = field_match(block, key)
    if match is None:
        return default
    try:
        return json.loads('"' + match.group(1) + '"')
    except json.JSONDecodeError:
        return match.group(1)


def replace_quoted_field(block: str, key: str, value: str) -> str:
    pattern = rf'(?<![A-Za-z0-9_])({re.escape(key)}:\s*)"(?:\\.|[^"\\])*"'
    return re.sub(pattern, lambda match: f"{match.group(1)}{snbt_string(value)}", block, count=1)


def list_field_span(block: str, key: str) -> tuple[int, int]:
    match = re.search(rf"(?m)^\s*{re.escape(key)}:\s*", block)
    if match is None:
        raise ValueError(f"Quest block has no {key} list")
    start = block.find("[", match.end())
    return start, scan_balanced(block, start, "[", "]")


def replace_string_list(block: str, key: str, values: list[str]) -> str:
    start, end = list_field_span(block, key)
    indent = "        " if key == "description" else "    "
    if not values:
        replacement = "[]"
    elif key == "description":
        replacement = "[\n" + ",\n".join(
            f"        {snbt_string(value)}" for value in values
        ) + "\n      ]"
    else:
        replacement = "[" + ", ".join(snbt_string(value) for value in values) + "]"
    return block[:start] + replacement + block[end:]


def task_blocks(block: str) -> list[tuple[int, int, str]]:
    start, end = list_field_span(block, "tasks")
    result: list[tuple[int, int, str]] = []
    depth = 0
    quoted = False
    escaped = False
    item_start: int | None = None
    for index in range(start + 1, end - 1):
        char = block[index]
        if quoted:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                quoted = False
            continue
        if char == '"':
            quoted = True
        elif char == "{":
            if depth == 0:
                item_start = index
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0 and item_start is not None:
                result.append((item_start, index + 1, block[item_start:index + 1]))
                item_start = None
    return result


def string_list_value(block: str, key: str) -> list[str]:
    match = re.search(rf'(?<![A-Za-z0-9_]){re.escape(key)}:\s*\[([^]]*)\]', block)
    if match is None:
        return []
    return re.findall(r'"((?:\\.|[^"\\])*)"', match.group(1))


def number_value(block: str, key: str, default: int = 1) -> int:
    match = re.search(rf'\b{re.escape(key)}:\s*(-?\d+)', block)
    return int(match.group(1)) if match else default


def item_display(sources: QuestSources, raw: str) -> str:
    item_id = raw.rsplit(":", 1)[-1].strip().lower()
    if item_id in sources.item_names:
        return sources.item_names[item_id]
    aliases = {
        "puppet_core_blank": "空白傀儡灵核",
        "diyuan_permit": "地渊令",
        "pressure_resist_charm": "抗压符",
        "wind_feather_raft_ticket": "风羽舟票",
        "spirit_recovery_pill": "回灵丹",
        "jiangchen_pill": "降尘丹",
    }
    if item_id in aliases:
        return aliases[item_id]
    # Unknown catalog aliases are intentionally rendered as a generic Chinese
    # label instead of leaking a registry path to players.
    return "任务奖励物品"


def reward_entries(sources: QuestSources, chain: str) -> tuple[tuple[str, int], ...]:
    source = sources.chains.get(chain, {})
    raw_rewards = source.get("rewards_finale", [])
    if isinstance(raw_rewards, list):
        resolved: list[tuple[str, int]] = []
        for raw in raw_rewards:
            token = str(raw).strip()
            count = 1
            for separator in ("*", ":"):
                if separator not in token:
                    continue
                item_id, maybe_count = token.rsplit(separator, 1)
                if item_id.strip() and maybe_count.strip().isdigit():
                    token = item_id.strip()
                    count = min(MAX_REWARD_COUNT, max(1, int(maybe_count.strip())))
                    break
            if token.rsplit(":", 1)[-1] in sources.item_names:
                resolved.append((token.rsplit(":", 1)[-1], count))
        if resolved:
            return tuple(resolved)
    return SOFT_FINALE_REWARDS.get(chain, DEFAULT_FINALE_REWARD)


def reward_text(sources: QuestSources, chain: str) -> str:
    entries = reward_entries(sources, chain)
    finale = "、".join(f"{item_display(sources, item)} × {count}" for item, count in entries)
    count = chain_step_count(sources.chains.get(chain, {}))
    if count >= 4:
        mid = f"中段第{max(2, count // 2)}阶段另得灵石碎片 × 2"
    else:
        mid = "本任务链阶段较短，不设中段额外奖励"
    return f"整条{chain_display(sources, chain)}完成后结算：{finale}；{mid}；结局分支加赠：正道功勋令 × 1、中立灵石碎片 × 2、魔道阴石 × 4"


def advancement_text(raw: str) -> str:
    if raw in ADVANCEMENT_TARGETS:
        return ADVANCEMENT_TARGETS[raw]
    path = raw.rsplit("/", 1)[-1].replace("_", "")
    return f"达成原版进度目标（{path}）"


def entity_text(raw: str) -> str:
    return ENTITY_NAMES.get(raw, "目标生物")


def dimension_text(raw: str) -> str:
    return DIMENSION_NAMES.get(raw, "目标地域")


def native_task_target(task: str, counts: dict[str, int]) -> tuple[str, Target] | None:
    for tag in string_list_value(task, "tags"):
        for prefix in (READY_PREFIX, MIRROR_PREFIX):
            target = parse_target(tag, prefix, counts)
            if target is not None:
                return prefix, target
    return None


def safe_existing_title(title: str) -> bool:
    return bool(title) and not re.search(
        r"[A-Za-z_./:]|(?i:ftb|npc|ui|boss|buff|debuff|pve|dlc)|后续|占位|暂不|来源|源自",
        title,
    )


def native_task_title(sources: QuestSources, target: Target) -> str:
    label = target_label(sources, target)
    return f"接取里程碑：{label}" if target.stage == 1 else label


def task_title(sources: QuestSources, task: str, counts: dict[str, int]) -> str:
    native = native_task_target(task, counts)
    if native is not None:
        return native_task_title(sources, native[1])
    tags = string_list_value(task, "tags")
    for tag in tags:
        if tag == "si_war_active":
            return "等待宗门战开启"
        match = re.fullmatch(r"si_rep_([a-z0-9_]+)_(\d+)", tag)
        if match:
            return f"{FACTION_NAMES.get(match.group(1), '所属势力')}声望达到 {match.group(2)}"
    task_type = field_value(task, "type")
    if task_type == "item":
        return f"持有{item_display(sources, field_value(task, 'item'))} × {number_value(task, 'count')}"
    if task_type == "kill":
        return f"击败{entity_text(field_value(task, 'entity'))} × {number_value(task, 'value')}"
    if task_type == "advancement":
        return f"达成检测目标：{advancement_text(field_value(task, 'advancement'))}"
    if task_type == "dimension":
        return f"进入{dimension_text(field_value(task, 'dimension'))}"
    old = field_value(task, "title")
    return old if safe_existing_title(old) else "完成本节点目标"


def task_condition(sources: QuestSources, task: str, counts: dict[str, int]) -> str:
    native = native_task_target(task, counts)
    if native is not None:
        prefix, target = native
        label = target_label(sources, target)
        if prefix == READY_PREFIX:
            if target.stage == 1:
                return (f"接取门槛：{start_requirement_text(sources, target.chain)}；"
                        f"完成接取里程碑（{label}）")
            return f"满足{chain_display(sources, target.chain)}的阶段推进条件并完成{label}"
        if target.stage == 1:
            return (f"先在原生任务追踪中接取{chain_display(sources, target.chain)}"
                    f"（接取门槛：{start_requirement_text(sources, target.chain)}）；"
                    f"本镜像读取已记录的接取里程碑（{label}）")
        return f"原生任务账本已记录{label}"
    tags = string_list_value(task, "tags")
    for tag in tags:
        if tag == "si_war_active":
            return "宗门战争处于开启状态"
        match = re.fullmatch(r"si_rep_([a-z0-9_]+)_(\d+)", tag)
        if match:
            return f"{FACTION_NAMES.get(match.group(1), '所属势力')}声望达到 {match.group(2)}"
    task_type = field_value(task, "type")
    if task_type == "item":
        return f"持有{item_display(sources, field_value(task, 'item'))} × {number_value(task, 'count')}（检测不消耗物品）"
    if task_type == "kill":
        return f"击败{entity_text(field_value(task, 'entity'))} × {number_value(task, 'value')}"
    if task_type == "advancement":
        return f"达成检测目标：{advancement_text(field_value(task, 'advancement'))}"
    if task_type == "dimension":
        return f"进入{dimension_text(field_value(task, 'dimension'))}"
    return task_title(sources, task, counts)


def rewrite_task_fields(sources: QuestSources, block: str, counts: dict[str, int]) -> tuple[str, list[str]]:
    conditions: list[str] = []
    replacements: list[tuple[int, int, str]] = []
    for start, end, task in task_blocks(block):
        title = task_title(sources, task, counts)
        conditions.append(task_condition(sources, task, counts))
        updated = replace_quoted_field(task, "title", title)
        replacements.append((start, end, updated))
    for start, end, updated in reversed(replacements):
        block = block[:start] + updated + block[end:]
    return block, conditions


def polish_quest_block(sources: QuestSources, chapter: str, original: QuestBlock,
                       block: str, counts: dict[str, int]) -> str:
    targets = quest_chain_targets(original, chapter, counts)
    old_title = NARRATIVE_ONLY_TITLES.get(original.quest_id, field_value(block, "title"))
    if targets:
        primary = targets[0]
        fallback_title = stage_display(sources, primary.chain, primary.stage)
        subtitle = f"{chain_display(sources, primary.chain)} · {fallback_title}"
    else:
        primary = None
        fallback_title = "章节剧情节点"
        subtitle = "章节剧情节点"
    title = old_title if safe_existing_title(old_title) else fallback_title
    block = replace_quoted_field(block, "title", title)
    block = replace_quoted_field(block, "subtitle", subtitle)
    block, conditions = rewrite_task_fields(sources, block, counts)
    condition_text = "；".join(dict.fromkeys(conditions)) or "完成本节点指定目标"
    if primary is None:
        reward_note = NARRATIVE_ONLY_QUESTS[original.quest_id]
        description = [
            f"任务提示：完成“{title}”对应的章节剧情节点，按顺序完成当前节点的实际检测目标。",
            f"完成条件：{condition_text}。",
            f"完成奖励：{reward_note}",
        ]
    else:
        reward_chains = list(dict.fromkeys(target.chain for target in targets))
        reward_texts = "；".join(reward_text(sources, chain) for chain in reward_chains)
        description = [
            f"任务提示：围绕“{title}”推进{chain_display(sources, primary.chain)}，按顺序完成当前节点的实际检测目标。",
            f"完成条件：{condition_text}。",
            f"完成奖励：本节点仅展示并核对进度，不直接发放或消耗奖励物品；以下物品由原生任务账本一次性结算：{reward_texts}。",
        ]
    return replace_string_list(block, "description", description)


def scan_balanced(text: str, start: int, opener: str, closer: str) -> int:
    depth = 0
    quoted = False
    escaped = False
    for index in range(start, len(text)):
        char = text[index]
        if quoted:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                quoted = False
            continue
        if char == '"':
            quoted = True
        elif char == opener:
            depth += 1
        elif char == closer:
            depth -= 1
            if depth == 0:
                return index + 1
    raise ValueError(f"Unbalanced {opener}{closer} starting at offset {start}")


def quest_blocks(text: str) -> list[QuestBlock]:
    marker = re.search(r"(?m)^  quests:\s*\[", text)
    if marker is None:
        raise ValueError("Missing top-level quests list")
    list_start = text.index("[", marker.start())
    list_end = scan_balanced(text, list_start, "[", "]")
    blocks: list[QuestBlock] = []
    depth = 0
    quoted = False
    escaped = False
    block_start: int | None = None
    for index in range(list_start + 1, list_end - 1):
        char = text[index]
        if quoted:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                quoted = False
            continue
        if char == '"':
            quoted = True
        elif char == "{":
            if depth == 0:
                block_start = index
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0 and block_start is not None:
                block_end = index + 1
                block = text[block_start:block_end]
                quest_id_match = re.search(r'(?m)^      id:\s*"([0-9]+)"', block)
                tags_match = re.search(r'(?m)^      tags:\s*\[(.*)]\s*,?$', block)
                if quest_id_match is None or tags_match is None:
                    raise ValueError("Every packaged FTB quest must have an id and one-line tags")
                tags = re.findall(r'"([a-z0-9_]+)"', tags_match.group(1))
                blocks.append(QuestBlock(
                    start=block_start,
                    end=block_end,
                    text=block,
                    quest_id=quest_id_match.group(1),
                    tags=tags,
                ))
                block_start = None
    return blocks


def parse_target(tag: str, prefix: str, counts: dict[str, int]) -> Target | None:
    if not tag.startswith(prefix):
        return None
    body = tag[len(prefix):]
    chain, separator, stage_raw = body.rpartition("_")
    if not separator or chain not in counts or not stage_raw.isdigit():
        return None
    stage = int(stage_raw)
    if stage < 1 or stage > counts[chain]:
        return None
    return Target(chain, stage)


def chapter_path(chapter: str) -> Path:
    return QUEST_ROOT / "chapters" / f"{chapter}.snbt"


def mentioned_chains(text: str, chapter: str) -> list[str]:
    return [
        chain for chain in CHAPTER_CHAINS[chapter]
        if re.search(rf"(?<![a-z0-9_]){re.escape(chain)}(?![a-z0-9_])", text)
    ]


def add_field_before(block: str, key: str, value: str, before: tuple[str, ...]) -> str:
    if re.search(rf"(?m)^      {re.escape(key)}\s*:", block):
        return re.sub(
            rf'(?m)^(      {re.escape(key)}\s*:)\s*.*$',
            lambda match: f"{match.group(1)} {value},",
            block,
            count=1,
        )
    positions = [block.find(f"\n      {candidate}:") for candidate in before]
    positions = [position for position in positions if position >= 0]
    if not positions:
        raise ValueError(f"Cannot insert {key} in quest block")
    position = min(positions)
    return block[:position] + f"\n      {key}: {value}," + block[position:]


def task_list_span(block: str) -> tuple[int, int]:
    match = re.search(r"(?m)^      tasks:\s*", block)
    if match is None:
        raise ValueError("Quest block has no tasks list")
    start = block.find("[", match.end())
    return start, scan_balanced(block, start, "[", "]")


def append_task(block: str, task: str) -> str:
    start, end = task_list_span(block)
    value = block[start:end]
    inner = value[1:-1].rstrip()
    if not inner.strip():
        replacement = f"[\n        {task}\n      ]"
    elif "\n" in inner:
        replacement = f"[{inner},\n        {task}\n      ]"
    else:
        replacement = f"[{inner},\n        {task}\n      ]"
    return block[:start] + replacement + block[end:]


def remove_rewards(block: str) -> str:
    # All authored bundled rewards are compact one-line lists.
    if re.search(r"(?m)^      rewards:", block) and not re.search(
            r"(?m)^      rewards:\s*\[[^\n]*]\s*,?$", block):
        raise ValueError("Generator only removes compact quest reward lists")
    updated = re.sub(r"(?m)^      rewards:[ \t]*\[[^\n]*][ \t]*,?\r?\n?", "", block)
    closing = updated.rfind("}")
    last = closing - 1
    while last >= 0 and updated[last].isspace():
        last -= 1
    if last >= 0 and updated[last] == ",":
        updated = updated[:last] + updated[last + 1:]
    return updated


def replace_blocks(text: str, replacements: dict[str, str]) -> str:
    blocks = quest_blocks(text)
    for block in reversed(blocks):
        replacement = replacements.get(block.quest_id)
        if replacement is not None:
            text = text[:block.start] + replacement + text[block.end:]
    return text


def custom_task(task_id: int, title: str, tag: str) -> str:
    return (
        f'{{id: "{task_id}", type: "custom", title: "{title}", '
        f'tags: ["{tag}"], check_timer: 20, max_progress: 1L}}'
    )


def generate() -> tuple[dict[Path, str], dict[str, object]]:
    sources = load_sources()
    chain_order, counts = load_chains()
    target_index = {
        Target(chain, stage): index
        for index, (chain, stage) in enumerate(
            (pair for chain in chain_order for pair in ((chain, stage) for stage in range(1, counts[chain] + 1))),
            start=1,
        )
    }
    chapter_for = {chain: chapter for chapter, chains in CHAPTER_CHAINS.items() for chain in chains}

    chapter_texts = {chapter: chapter_path(chapter).read_text(encoding="utf-8") for chapter in CHAPTER_CHAINS}
    chapter_blocks = {chapter: quest_blocks(text) for chapter, text in chapter_texts.items()}
    validate_narrative_records(chapter_blocks, counts)

    quest_by_id: dict[str, tuple[str, QuestBlock]] = {}
    chain_quests: dict[str, list[QuestBlock]] = {chain: [] for chain in chain_order}
    existing_mirrors: dict[Target, list[tuple[str, str]]] = {}
    existing_ready: set[Target] = set()
    writes: dict[Target, tuple[str, str]] = {}
    used_task_ids = set(re.findall(r'(?m)\bid:\s*"([0-9]+)"', "\n".join(chapter_texts.values())))

    for chapter, blocks in chapter_blocks.items():
        for block in blocks:
            if block.quest_id in quest_by_id:
                raise ValueError(f"Duplicate quest id {block.quest_id}")
            quest_by_id[block.quest_id] = (chapter, block)
            native_chain_ids: set[str] = set()
            for tagged in re.findall(r'"(si_native_[a-z0-9_]+)"', block.text):
                for prefix in (READY_PREFIX, WRITE_PREFIX, MIRROR_PREFIX):
                    tagged_target = parse_target(tagged, prefix, counts)
                    if tagged_target is not None:
                        native_chain_ids.add(tagged_target.chain)
                        break
            for chain in CHAPTER_CHAINS[chapter]:
                if (chain in block.tags or chain in native_chain_ids
                        or chain in mentioned_chains(block.text, chapter)):
                    chain_quests[chain].append(block)
            for tag in block.tags:
                target = parse_target(tag, WRITE_PREFIX, counts)
                if target is not None:
                    if target in writes:
                        raise ValueError(f"Duplicate native write target {target}")
                    writes[target] = (chapter, block.quest_id)
            for task_tag in re.findall(r'"(si_native_[a-z0-9_]+)"', block.text):
                ready = parse_target(task_tag, READY_PREFIX, counts)
                if ready is not None:
                    existing_ready.add(ready)
                    continue
                mirror = parse_target(task_tag, MIRROR_PREFIX, counts)
                if mirror is not None:
                    existing_mirrors.setdefault(mirror, []).append((chapter, block.quest_id))

    missing_tag_chains = [chain for chain, quests in chain_quests.items() if not quests]
    if missing_tag_chains:
        raise ValueError(f"Every native chain needs at least one tagged FTB quest: {missing_tag_chains}")

    replacements: dict[str, str] = {}
    for quest_id, (chapter, original) in quest_by_id.items():
        block = original.text.replace("consume_items: true", "consume_items: false")
        block = remove_rewards(block)
        guide_page = f"seeking_immortals:{ENTRY_BY_CHAPTER[chapter]}"
        block = add_field_before(block, "guide_page", f'"{guide_page}"', ("description", "tags", "tasks"))
        replacements[quest_id] = block

    for target, (chapter, quest_id) in writes.items():
        block = replacements[quest_id]
        block = add_field_before(block, "require_sequential_tasks", "true", ("tasks",))
        if target not in existing_ready:
            ready_id = READY_ID_BASE + target_index[target]
            if str(ready_id) in used_task_ids:
                raise ValueError(f"Generated ready task id collision: {ready_id}")
            used_task_ids.add(str(ready_id))
            block = append_task(block, custom_task(
                ready_id,
                native_task_title(sources, target),
                f"{READY_PREFIX}{target.chain}_{target.stage}",
            ))
        replacements[quest_id] = block

    all_targets = list(target_index)
    for target in all_targets:
        if target in existing_mirrors:
            continue
        quests = chain_quests[target.chain]
        selected = quests[min(len(quests) - 1, (target.stage - 1) * len(quests) // counts[target.chain])]
        quest_id = selected.quest_id
        mirror_id = MIRROR_ID_BASE + target_index[target]
        if str(mirror_id) in used_task_ids:
            raise ValueError(f"Generated mirror task id collision: {mirror_id}")
        used_task_ids.add(str(mirror_id))
        replacements[quest_id] = append_task(
            replacements[quest_id],
            custom_task(
                mirror_id,
                native_task_title(sources, target),
                f"{MIRROR_PREFIX}{target.chain}_{target.stage}",
            ),
        )

    for quest_id, (chapter, original) in quest_by_id.items():
        current = QuestBlock(
            start=original.start,
            end=original.end,
            text=replacements[quest_id],
            quest_id=original.quest_id,
            tags=original.tags,
        )
        replacements[quest_id] = polish_quest_block(
            sources, chapter, current, current.text, counts
        )

    outputs: dict[Path, str] = {}
    for chapter, text in chapter_texts.items():
        chapter_replacements = {
            block.quest_id: replacements[block.quest_id]
            for block in chapter_blocks[chapter]
        }
        updated = replace_blocks(text, chapter_replacements)
        updated = re.sub(r"(?m)^},$", "    },", updated)
        title_match = re.search(r'(?m)^  title:\s*"((?:\\.|[^"\\])*)"', updated)
        chapter_title = field_value(title_match.group(0), "title") if title_match else "修行任务"
        short_title = chapter_title.removeprefix("寻仙问道：")
        chapter_subtitle = (
            f"{short_title}修行篇章。按节点核对实际完成条件，原生任务线奖励由原生任务账本结算。"
        )
        updated = re.sub(
            r"(?m)^  subtitle:\s*\[[^\n]*]\s*,?$",
            f"  subtitle: [{snbt_string(chapter_subtitle)}],",
            updated,
            count=1,
        )
        outputs[chapter_path(chapter)] = updated

    projection_rows: list[dict[str, object]] = []
    generated_text = "\n".join(outputs.values())
    generated_blocks = {
        block.quest_id: (chapter, block)
        for chapter, text in outputs.items()
        for block in quest_blocks(text)
    }
    del generated_text
    for target in all_targets:
        mirrors: list[dict[str, str]] = []
        tag = f"{MIRROR_PREFIX}{target.chain}_{target.stage}"
        for quest_id, (chapter_path_value, block) in generated_blocks.items():
            if f'"{tag}"' in block.text:
                chapter = chapter_path_value.stem
                task_match = re.search(
                    rf'\{{id:\s*"([0-9]+)"[^{{}}]*tags:\s*\[[^]]*"{re.escape(tag)}"[^]]*]',
                    block.text,
                )
                mirrors.append({
                    "chapter": chapter,
                    "quest_id": quest_id,
                    "task_id": task_match.group(1) if task_match else "",
                })
        if not mirrors:
            raise ValueError(f"Missing generated mirror for {target}")
        projection_rows.append({
            "chain": target.chain,
            "stage": target.stage,
            "chapter": chapter_for[target.chain],
            "guide_entry": f"seeking_immortals:{ENTRY_BY_CHAPTER[chapter_for[target.chain]]}",
            "guide_anchor": f"chain_{target.chain}",
            "mirrors": mirrors,
            "write_quest_id": writes.get(target, ("", ""))[1] or None,
        })

    manifest = {
        "schema_version": 1,
        "chain_count": len(chain_order),
        "stage_count": len(projection_rows),
        "chapter_count": len(CHAPTER_CHAINS),
        "quest_node_count": len(quest_by_id),
        "authority": "native_player_ledger",
        "stages": projection_rows,
    }
    outputs[PROJECTION_OUTPUT] = json.dumps(manifest, ensure_ascii=False, indent=2) + "\n"
    return outputs, manifest


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="fail when generated files differ")
    args = parser.parse_args()
    outputs, manifest = generate()
    stale: list[Path] = []
    for path, content in outputs.items():
        current = path.read_text(encoding="utf-8") if path.exists() else None
        if current != content:
            stale.append(path)
            if not args.check:
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(content, encoding="utf-8", newline="\n")
    if args.check and stale:
        for path in stale:
            print(f"stale: {path.relative_to(ROOT)}", file=sys.stderr)
        return 1
    action = "checked" if args.check else "generated"
    print(
        f"{action} {manifest['chapter_count']} chapters, {manifest['quest_node_count']} quests, "
        f"{manifest['chain_count']} chains and {manifest['stage_count']} stages"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
