#!/usr/bin/env python3
from __future__ import annotations

import json
import re
from pathlib import Path

root = Path(__file__).resolve().parents[1]
java = root / "src/main/java/com/xunxian/seekingimmortals"
assets = root / "src/main/resources/assets/seeking_immortals"
data = root / "src/main/resources/data/seeking_immortals"
text = root / "文本材料"

reg: set[str] = set()
for p in java.rglob("*.java"):
    t = p.read_text(encoding="utf-8", errors="ignore")
    reg.update(re.findall(r'\.register\("([a-z0-9_]+)"', t))
    reg.update(re.findall(r'register\w+\("([a-z0-9_]+)"', t))
cpt = (java / "item/pill/CatalogPillType.java").read_text(encoding="utf-8")
enum_to_id = dict(re.findall(r'([A-Z0-9_]+)\("([a-z0-9_]+)"', cpt))
reg.update(enum_to_id.values())
mi = (java / "registry/ModItems.java").read_text(encoding="utf-8")
for m in re.finditer(r'registerCatalogPill\(CatalogPillType\.([A-Z0-9_]+)(?:,\s*"([a-z0-9_]+)")?', mi):
    base = enum_to_id[m.group(1)]
    reg.add(base if not m.group(2) else f"{base}_{m.group(2)}")
bulk = json.loads((assets / "catalog_bulk_items.json").read_text(encoding="utf-8"))
reg.update(o["id"] for o in bulk["items"])

aliases: dict[str, str] = {}
for e in json.loads((data / "reference/text_material_id_map.json").read_text(encoding="utf-8"))["entries"]:
    sid = e.get("source_id")
    cid = e.get("canonical_id") or ""
    if isinstance(sid, str) and isinstance(cid, str) and cid.startswith("seeking_immortals:"):
        aliases[sid] = cid.split(":", 1)[1]
    elif isinstance(sid, str) and isinstance(cid, str) and cid.startswith("minecraft:"):
        aliases[sid] = cid


def covered(i: str) -> bool:
    i = (i or "").split(":")[-1]
    if i in reg:
        return True
    t = aliases.get(i)
    if t:
        if t.startswith("minecraft:") or t in reg:
            return True
        for s in ("_low", "_mid", "_high", "_supreme"):
            if f"{t}{s}" in reg:
                return True
    for s in ("_low", "_mid", "_high", "_supreme"):
        if f"{i}{s}" in reg:
            return True
    return False


af = json.loads((text / "data/item_id_aliases.json").read_text(encoding="utf-8"))
print("item_id_aliases:")
for a in af.get("aliases", []):
    alias = a.get("alias", "")
    can = a.get("canonical", "")
    print(" ", alias, "->", can, "OK" if covered(alias) else "MISSING")

refs: set[str] = set()
for sub in ("shops", "alchemy", "recipes", "loot_tables"):
    d = data / sub
    if d.exists():
        for p in d.rglob("*.json"):
            refs.update(re.findall(r"seeking_immortals:([a-z0-9_]+)", p.read_text(encoding="utf-8", errors="ignore")))
print("shipped refs missing", sorted(i for i in refs if not covered(i)))

item_fields: set[str] = set()
for p in text.rglob("*.json"):
    try:
        obj = json.loads(p.read_text(encoding="utf-8"))
    except Exception:
        continue

    def walk(o):
        if isinstance(o, dict):
            for k, v in o.items():
                if k in {
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
                } and isinstance(v, str):
                    s = v.split(":")[-1]
                    if re.fullmatch(r"[a-z][a-z0-9_]{1,80}", s):
                        item_fields.add(s)
                elif k in {
                    "items",
                    "materials",
                    "rewards",
                    "drops",
                    "entries",
                    "stock",
                    "goods",
                    "ingredients",
                    "outputs",
                    "papers",
                    "inks",
                    "parts",
                    "definitions",
                    "talismans",
                    "pills",
                    "artifacts",
                    "manuals",
                    "herbs",
                    "pools",
                } and isinstance(v, list):
                    for e in v:
                        if isinstance(e, dict):
                            for ik in ("id", "item", "output", "result", "product"):
                                if ik in e and isinstance(e[ik], str):
                                    s = e[ik].split(":")[-1]
                                    if re.fullmatch(r"[a-z][a-z0-9_]{1,80}", s):
                                        item_fields.add(s)
                            walk(e)
                        elif isinstance(e, str) and re.fullmatch(r"[a-z][a-z0-9_]{1,80}", e):
                            item_fields.add(e)
                elif isinstance(v, (dict, list)):
                    walk(v)
        elif isinstance(o, list):
            for e in o:
                walk(e)

    walk(obj)

miss = [i for i in sorted(item_fields) if not covered(i)]
soft_or_non = 0
hard: list[str] = []
SOFT = {
    "fail",
    "explosion",
    "beast_tribulation_bonus",
    "demon_qi_penalty",
    "five_elements_mountain_array",
    "nether_river_shield_zone",
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
    "contribution",
    "contribution_minor",
    "contribution_small",
    "contribution_star",
    "pill_quality_roll",
    "array_merit",
    "forge_merit",
    "legendary_roll",
    "clan_merit",
    "clan_merit_goods",
    "clan_reputation",
    "war_merit",
    "war_merit_huangfeng",
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
    "alchemy_furnace_slot",
    "auction_slot",
    "inner_manual_slot",
    "alchemy_flow",
    "ascension_flow",
    "beast_tiers",
    "boss_drops",
    "economy_tiers",
    "puppet_flow",
    "refinement_flow",
    "talisman_flow",
    "qianzhu_puppet_loop",
    "spirit_roots_catalog",
    "diyuan_layer_chest",
    "kunwu_layer_chest",
    "changchun_gong",
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
    "mulan_tianlan_war",
    "inverse_star_black_market",
    "wanbao_auction",
    "contribution_rank_inner",
    "contribution_exchange",
    "ancient_fragment_chance",
    "dayan_fragment_chance",
    "fire_feather_spawn",
    "nether_core_form_unlock",
    "soul_anchor_rite_unlock",
    "yin_body_condense_unlock",
    "yin_soul_burst_unlock",
    "wutu_side_quest_unlock",
    "nether_river_land",
    "thunder_tribulation",
    "spirit_beast",
    "node_fallen_demon_rift_open",
}
for i in miss:
    if i in SOFT:
        soft_or_non += 1
        continue
    if i.startswith(("recipe_", "craft_", "assemble_", "upgrade_", "rep_")):
        soft_or_non += 1
        continue
    if i.endswith(("_hint", "_unlock", "_merit", "_rep", "_contribution", "_roll", "_quota", "_buff", "_discount", "_access", "_chance", "_slot")):
        soft_or_non += 1
        continue
    if any(x in i for x in ("contribution", "reputation", "_merit", "_rep_")):
        soft_or_non += 1
        continue
    hard.append(i)

print("item-field total uncovered", len(miss), "soft/non", soft_or_non, "hard", len(hard), hard)
print("reg", len(reg), "bulk", len(bulk["items"]))
print([ln for ln in (root / "gradle.properties").read_text(encoding="utf-8", errors="ignore").splitlines() if "mod_version" in ln][0])
print("models for bulk sample ok", all((assets / f"models/item/{x['id']}.json").exists() for x in bulk["items"][-5:]))
