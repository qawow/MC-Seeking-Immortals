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
            for chain in CHAPTER_CHAINS[chapter]:
                if chain in block.tags or chain in mentioned_chains(block.text, chapter):
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
                f"提交原生任务阶段 {target.stage}",
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
                f"同步原生任务阶段 {target.stage}/{counts[target.chain]}",
                f"{MIRROR_PREFIX}{target.chain}_{target.stage}",
            ),
        )

    outputs: dict[Path, str] = {}
    for chapter, text in chapter_texts.items():
        chapter_replacements = {
            block.quest_id: replacements[block.quest_id]
            for block in chapter_blocks[chapter]
        }
        updated = replace_blocks(text, chapter_replacements)
        updated = re.sub(r"(?m)^},$", "    },", updated)
        updated = updated.replace("并消耗。", "但不消耗；原生任务账本负责实际上交。")
        updated = updated.replace("并消耗，", "但不消耗，原生任务账本负责实际上交，")
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
