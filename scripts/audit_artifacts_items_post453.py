#!/usr/bin/env python3
"""Focused artifact/item gap audit after 0.1.453."""
from __future__ import annotations

import json
import re
from pathlib import Path

root = Path(r"D:/codex/mc-mod")
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
    if not i:
        return True
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


def extract_ids(obj, ids: set[str]) -> None:
    item_keys = {
        "id",
        "item",
        "item_id",
        "output",
        "result",
        "product",
        "artifact_id",
        "material_id",
        "pill_id",
        "manual_id",
        "talisman_id",
        "carrier_id",
        "reward_item",
        "cost_item",
        "fuel",
        "paper_item_id",
        "part_id",
        "vehicle_id",
        "puppet_id",
        "herb_id",
        "block_id",
        "currency_id",
        "alias",
        "canonical",
    }
    if isinstance(obj, dict):
        for k, v in obj.items():
            if k in item_keys and isinstance(v, str):
                s = v.split(":")[-1]
                if re.fullmatch(r"[a-z][a-z0-9_]{1,80}", s):
                    ids.add(s)
            elif isinstance(v, (dict, list)):
                extract_ids(v, ids)
    elif isinstance(obj, list):
        for e in obj:
            extract_ids(e, ids)


def is_non_item(i: str) -> bool:
    if i in {
        "fail",
        "explosion",
        "schema_version",
        "result",
        "craft",
        "id",
        "item",
        "display",
        "count",
        "tier",
        "low",
        "mid",
        "high",
        "type",
        "note",
        "rarity",
        "category",
        "grade",
        "effect",
        "beast_tribulation_bonus",
        "demon_qi_penalty",
        "five_elements_mountain_array",
        "nether_river_shield_zone",
        "pill_quality_rank_up_chance_tag",
        "skill_trees_overview",
    }:
        return True
    if i.startswith(("recipe_", "craft_", "assemble_", "upgrade_", "refine_", "quest_", "hook_", "event_", "rep_")):
        return True
    if i.endswith(
        (
            "_hall",
            "_shop",
            "_market",
            "_stall",
            "_pavilion",
            "_auction",
            "_exchange",
            "_vendor",
            "_lane",
            "_supply",
            "_general",
            "_trade",
            "_rules",
            "_map",
            "_catalog",
            "_index",
            "_schema",
            "_system",
            "_hint",
            "_unlock",
            "_merit",
            "_rep",
            "_contribution",
            "_roll",
            "_quota",
            "_buff",
            "_discount",
            "_access",
            "_chance",
            "_slot",
            "_overview",
            "_summary",
            "_flow",
        )
    ):
        return True
    return False


FOCUS = [
    "artifacts_catalog.json",
    "artifact_taxonomy_111.json",
    "artifact_tier_map.json",
    "artifact_eleven_tier_map.json",
    "artifact_realm_drops.json",
    "artifact_faction_specialty.json",
    "moditems_artifacts_draft.json",
    "refinement_recipes.json",
    "refine_manual_index.json",
    "forge_artifact_priority.json",
    "materials_catalog.json",
    "consumables_catalog.json",
    "manuals_catalog.json",
    "pills_catalog.json",
    "talisman_catalog.json",
    "talisman_materials_catalog.json",
    "talisman_recipes.json",
    "talisman_treasure_templates.json",
    "formation_items_catalog.json",
    "formation_catalog.json",
    "block_items_catalog.json",
    "currency_items.json",
    "flight_vehicles.json",
    "puppet_definitions.json",
    "puppet_parts_catalog.json",
    "puppet_craft_recipes.json",
    "spirit_herbs_catalog.json",
    "alchemy_recipes.json",
    "merchant_shops.json",
    "novel_items_waves.json",
    "boss_loot_tables.json",
    "loot_tables.json",
    "item_id_aliases.json",
    "ancient_treasure_index.json",
]

print("reg", len(reg), "bulk", len(bulk["items"]))
print(
    "version",
    [
        ln
        for ln in (root / "gradle.properties").read_text(encoding="utf-8", errors="ignore").splitlines()
        if "mod_version" in ln
    ][0],
)

all_miss: dict[str, list[str]] = {}
for name in FOCUS:
    paths = list(text.rglob(name)) + list(data.rglob(name))
    if not paths and (data / "artifacts").exists():
        paths = list((data / "artifacts").rglob(name))
    if not paths:
        print(name, "NOT_FOUND")
        continue
    ids: set[str] = set()
    for p in paths:
        try:
            obj = json.loads(p.read_text(encoding="utf-8"))
        except Exception:
            continue
        extract_ids(obj, ids)
    miss = sorted(i for i in ids if not covered(i) and not is_non_item(i))
    print(f"{name}: ids={len(ids)} missing={len(miss)}" + ((" -> " + str(miss[:30])) if miss else ""))
    if miss:
        all_miss[name] = miss

# shipped refs
refs: set[str] = set()
for sub in ("shops", "alchemy", "recipes", "loot_tables", "artifacts"):
    d = data / sub
    if d.exists():
        for p in d.rglob("*.json"):
            refs.update(re.findall(r"seeking_immortals:([a-z0-9_]+)", p.read_text(encoding="utf-8", errors="ignore")))
print("shipped refs missing", sorted(i for i in refs if not covered(i)))

# recipes refine_* specifically
refine_refs: set[str] = set()
if (data / "recipes").exists():
    for p in (data / "recipes").rglob("*.json"):
        if "refine" in p.name:
            refine_refs.update(
                re.findall(r"seeking_immortals:([a-z0-9_]+)", p.read_text(encoding="utf-8", errors="ignore"))
            )
print("refine recipe refs missing", sorted(i for i in refine_refs if not covered(i)))

flat = sorted({i for v in all_miss.values() for i in v})
print("TOTAL files with misses", len(all_miss))
print("FLAT MISS", len(flat))
for i in flat:
    print(" ", i)

# check artifact-ish registered count
art_like = sorted(i for i in reg if any(x in i for x in ("sword", "mirror", "shield", "fan", "umbrella", "bracelet", "disk", "needle", "bell", "bowl", "pendant", "boots", "chain", "brick", "ruler", "artifact", "treasure")))
print("artifact-ish registered sample count", len(art_like))
