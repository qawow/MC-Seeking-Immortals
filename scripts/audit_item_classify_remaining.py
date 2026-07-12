#!/usr/bin/env python3
"""Classify remaining text-material item-field misses into real vs soft."""
from __future__ import annotations

import json
import re
from pathlib import Path

root = Path(r"D:/codex/mc-mod")
java_dir = root / "src/main/java/com/xunxian/seekingimmortals"
assets = root / "src/main/resources/assets/seeking_immortals"
data = root / "src/main/resources/data/seeking_immortals"
text = root / "文本材料"

registered: set[str] = set()
for p in java_dir.rglob("*.java"):
    t = p.read_text(encoding="utf-8", errors="ignore")
    registered.update(re.findall(r'\.register\("([a-z0-9_]+)"', t))
    registered.update(re.findall(r'register\w+\("([a-z0-9_]+)"', t))
cpt = (java_dir / "item/pill/CatalogPillType.java").read_text(encoding="utf-8")
enum_to_id = dict(re.findall(r'([A-Z0-9_]+)\("([a-z0-9_]+)"', cpt))
registered.update(enum_to_id.values())
mi = (java_dir / "registry/ModItems.java").read_text(encoding="utf-8")
for m in re.finditer(r'registerCatalogPill\(CatalogPillType\.([A-Z0-9_]+)(?:,\s*"([a-z0-9_]+)")?', mi):
    base = enum_to_id[m.group(1)]
    registered.add(base if not m.group(2) else f"{base}_{m.group(2)}")
bulk = json.loads((assets / "catalog_bulk_items.json").read_text(encoding="utf-8"))
registered.update(o["id"] for o in bulk["items"])
aliases: dict[str, str] = {}
for e in json.loads((data / "reference/text_material_id_map.json").read_text(encoding="utf-8"))["entries"]:
    sid, cid = e.get("source_id"), e.get("canonical_id") or ""
    if isinstance(sid, str) and isinstance(cid, str) and cid.startswith("seeking_immortals:"):
        aliases[sid] = cid.split(":", 1)[1]
    elif isinstance(sid, str) and isinstance(cid, str) and cid.startswith("minecraft:"):
        aliases[sid] = cid


def covered(i: str) -> bool:
    i = i.split(":")[-1]
    if i in registered:
        return True
    t = aliases.get(i)
    if t:
        if t.startswith("minecraft:") or t in registered:
            return True
        for s in ("_low", "_mid", "_high", "_supreme"):
            if f"{t}{s}" in registered:
                return True
    for s in ("_low", "_mid", "_high", "_supreme"):
        if f"{i}{s}" in registered:
            return True
    return False


SOFT_EXACT = {
    "contribution",
    "contribution_minor",
    "contribution_small",
    "contribution_star",
    "contribution_rank_inner",
    "pill_quality_roll",
    "pill_quality_rank_up_chance_tag",
    "legendary_roll",
    "array_merit",
    "forge_merit",
    "patrol_merit",
    "patrol_merit_bonus",
    "clan_merit",
    "clan_merit_goods",
    "clan_reputation",
    "war_merit",
    "war_merit_huangfeng",
    "alliance_merit",
    "merit_points",
    "merit_points_bulk",
    "merit_points_small",
    "tianyuan_contribution",
    "mulan_rep",
    "star_palace_rep",
    "inverse_rep",
    "reputation",
    "reputation_dajin",
    "dual_clan_rep",
    "dual_rep_neutral",
    "karma_ambiguous",
    "herb",
    "pill",
    "specialty_item",
    "beast_material",
    "beast_material_bulk",
    "yin_material",
    "quality_pills",
    "merchants",
    "peak",
    "random_low_artifact",
    "ascension_insight",
    "holy_bird_blessing_buff",
    "treasure_sense_buff",
    "spirit_realm_gate_fee_discount",
    "tax_reduction_deal",
    "temporary_sect_access",
    "kunwu_mountain_access",
    "foundation_pill_quota",
    "smuggle_route",
    "compliance_note",
    "expansion_changelog",
    "mortal_realm_cap",
    "barbarian_seven_kings",
    "chaotic_sea_factions",
    "dajin_clan_politics",
    "dajin_kunwu",
    "demonic_six_overview",
    "diyuan_depth",
    "ghost_path_summary",
    "human_clan_league",
    "refinement_artifacts",
    "spatial_nodes",
    "spirit_eighteen_clans",
    "spirit_fengyuan",
    "spirit_realm_tribulation",
    "star_palace_governance",
    "tianlan_temple",
    "tianyuan_city",
    "tianyuan_garrison",
    "yin_underworld_cluster",
    "yuling_beast_puppet",
    "alchemy_furnace_slot",
    "auction_slot",
    "inner_manual_slot",
    "nether_core_form_unlock",
    "soul_anchor_rite_unlock",
    "yin_body_condense_unlock",
    "yin_soul_burst_unlock",
    "wutu_side_quest_unlock",
    "nether_river_land",
    "thunder_tribulation",
    "spirit_beast",
    "fire_feather_spawn",
}
SOFT_PREFIX = ("rep_",)
SOFT_SUFFIX = (
    "_hint",
    "_unlock",
    "_overview",
    "_summary",
    "_governance",
    "_politics",
    "_changelog",
    "_access",
    "_quota",
    "_chance",
    "_buff",
    "_discount",
    "_deal",
    "_rank",
    "_points",
    "_rep",
    "_merit",
    "_contribution",
    "_roll",
    "_insight",
    "_slot",
)
SOFT_CONTAINS = (
    "_rep_",
    "reputation",
    "contribution_",
    "merit_",
    "_hint",
    "_unlock",
    "_chance",
    "_buff",
    "_discount",
    "_quota",
    "_overview",
    "_summary",
    "_governance",
    "_politics",
)

REAL_TOKENS = (
    "_token",
    "_fragment",
    "_pill",
    "_talisman",
    "_manual",
    "_scroll",
    "_core",
    "_bone",
    "_scale",
    "_feather",
    "_hide",
    "_fur",
    "_horn",
    "_shell",
    "_gall",
    "_pelt",
    "_pearl",
    "_crystal",
    "_blood",
    "_meat",
    "_sac",
    "_fiber",
    "_resin",
    "_flag",
    "_blank",
    "_paper",
    "_bundle",
    "_shard",
    "_map",
    "_permit",
    "_license",
    "_voucher",
    "_credit",
    "_charm",
    "_bead",
    "_bridle",
    "_contract",
    "_blueprint",
    "_plate",
    "_seed",
    "_bait",
    "_fang",
    "_wing",
    "_mane",
    "_chitin",
    "_marrow",
    "_silk",
    "_dust",
    "_sand",
    "_essence",
    "_trace",
    "_chain",
    "_cloak",
    "_fan",
    "_page",
    "_art",
    "_cipher",
    "_pass",
    "_tag",
    "_note",
    "_lot",
    "_spoil",
    "_trophy",
    "_pact",
    "_truce",
    "_waiver",
    "_vip",
    "_larva",
    "_tail",
    "_skin",
    "_claw",
    "_tusk",
    "_tooth",
    "_egg",
    "_nest",
    "_leaf",
    "_flower",
    "_root",
    "_vine",
    "_moss",
    "_ore",
    "_ingot",
    "_gem",
    "_jade",
    "_stone",
    "_powder",
    "_ointment",
    "_elixir",
    "_dan",
    "_slip",
    "_book",
    "_tome",
    "_seal",
    "_key",
    "_ticket",
    "_coin",
    "_medal",
    "_badge",
    "_flag",
    "_disk",
    "_bowl",
    "_bell",
    "_mirror",
    "_needle",
    "_sword",
    "_blade",
    "_shield",
    "_armor",
    "_robe",
    "_boots",
    "_ring",
    "_amulet",
    "_pendant",
    "_bracelet",
    "_umbrella",
    "_needle",
    "_whip",
    "_net",
    "_shovel",
    "_ruler",
    "_brick",
    "_chain",
    "_disk",
    "_jar",
    "_pot",
    "_cauldron",
    "_anvil",
    "_bellows",
    "_table",
    "_bench",
    "_forge",
    "_furnace",
    "_planter",
    "_altar",
    "_pedestal",
    "_gate",
    "_array",
    "_formation",
    "_puppet",
    "_boat",
    "_raft",
    "_cart",
    "_sedan",
    "_vehicle",
    "_ticket",
    "_receipt",
    "_invoice",
    "_ledger",
    "_chip",
    "_piece",
    "_part",
    "_module",
    "_heart",
    "_soul",
    "_spirit",
    "_qi",
    "_essence",
)


def is_soft(i: str) -> bool:
    if i in SOFT_EXACT:
        return True
    if any(i.startswith(p) for p in SOFT_PREFIX):
        return True
    if any(i.endswith(s) for s in SOFT_SUFFIX):
        return True
    if any(x in i for x in SOFT_CONTAINS):
        return True
    return False


def looks_physical(i: str) -> bool:
    if is_soft(i):
        return False
    if any(t in i for t in REAL_TOKENS):
        return True
    # short material-ish multiword without soft markers
    if "_" in i and not i.startswith(("rep_", "contribution", "merit", "karma")):
        # still skip pure abstract multiwords without material tokens
        return False
    return False


# Soft alias candidates (name variants of existing items)
SOFT_ALIASES = {
    "talisman_fire_burst": "fire_burst_talisman",
    "low_grade_spirit_stone": "spirit_stone_shard",  # or low spirit stone if exists
    "spatial_crystal": "space_crystal",
    "alliance_merit": "alliance_merit_token",
}

# collect item fields
item_fields: set[tuple[str, str]] = set()
ITEM_KEYS = {
    "item",
    "item_id",
    "output",
    "result",
    "product",
    "reward_item",
    "cost_item",
    "material_id",
    "talisman_id",
    "manual_id",
    "pill_id",
    "fuel",
    "paper_item_id",
}
for p in text.rglob("*.json"):
    try:
        obj = json.loads(p.read_text(encoding="utf-8"))
    except Exception:
        continue

    def walk(o):
        if isinstance(o, dict):
            for k, v in o.items():
                if k in ITEM_KEYS and isinstance(v, str):
                    s = v.split(":")[-1]
                    if re.fullmatch(r"[a-z][a-z0-9_]{1,80}", s):
                        item_fields.add((s, str(p.relative_to(root))))
                elif k in ("items", "materials", "rewards", "drops", "entries", "stock", "goods", "ingredients", "outputs", "papers", "inks", "parts", "definitions", "talismans", "pills", "artifacts", "manuals", "herbs", "pools") and isinstance(v, list):
                    for e in v:
                        if isinstance(e, dict):
                            for ik in ("id", "item", "output", "result", "product"):
                                if ik in e and isinstance(e[ik], str):
                                    s = e[ik].split(":")[-1]
                                    if re.fullmatch(r"[a-z][a-z0-9_]{1,80}", s):
                                        item_fields.add((s, str(p.relative_to(root))))
                            walk(e)
                        elif isinstance(e, str) and re.fullmatch(r"[a-z][a-z0-9_]{1,80}", e):
                            item_fields.add((e, str(p.relative_to(root))))
                elif isinstance(v, (dict, list)):
                    walk(v)
        elif isinstance(o, list):
            for e in o:
                walk(e)

    walk(obj)

miss = sorted({i for i, f in item_fields if not covered(i)})
physical = [i for i in miss if looks_physical(i)]
soft = [i for i in miss if is_soft(i)]
other = [i for i in miss if i not in physical and i not in soft]

print("miss total", len(miss))
print("soft", len(soft))
print("physical", len(physical))
for i in physical:
    files = sorted({f for x, f in item_fields if x == i})[:3]
    print("PHYS", i, files)
print("other", len(other))
for i in other[:80]:
    files = sorted({f for x, f in item_fields if x == i})[:2]
    print("OTH", i, files)

# verify alias targets
print("--- alias targets ---")
for s, t in SOFT_ALIASES.items():
    print(s, "->", t, "target_ok" if covered(t) else "TARGET_MISSING", "src_covered" if covered(s) else "src_missing")

# low spirit stone variants
print("spirit stone variants", sorted(i for i in registered if "spirit_stone" in i)[:30])
print("formation_flag", sorted(i for i in registered if "formation_flag" in i))
print("artifact_blank", sorted(i for i in registered if "blank" in i and "artifact" in i))
print("registered", len(registered), "bulk", len(bulk["items"]))
