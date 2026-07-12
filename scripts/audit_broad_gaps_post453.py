#!/usr/bin/env python3
"""Broad gap audit: items/artifacts/blocks/models/lang/recipes/techniques."""
from __future__ import annotations

import json
import re
from pathlib import Path

root = Path(r"D:/codex/mc-mod")
java = root / "src/main/java/com/xunxian/seekingimmortals"
assets = root / "src/main/resources/assets/seeking_immortals"
data = root / "src/main/resources/data/seeking_immortals"
text = root / "文本材料"

# ---- registered ----
reg_items: set[str] = set()
reg_blocks: set[str] = set()
for p in java.rglob("*.java"):
    t = p.read_text(encoding="utf-8", errors="ignore")
    if "DeferredRegister" in t or "register(" in t:
        if "Item" in t or "ITEMS" in t:
            reg_items.update(re.findall(r'\.register\("([a-z0-9_]+)"', t))
            reg_items.update(re.findall(r'register\w+\("([a-z0-9_]+)"', t))
        if "Block" in t or "BLOCKS" in t:
            reg_blocks.update(re.findall(r'\.register\("([a-z0-9_]+)"', t))
cpt = (java / "item/pill/CatalogPillType.java").read_text(encoding="utf-8")
enum_to_id = dict(re.findall(r'([A-Z0-9_]+)\("([a-z0-9_]+)"', cpt))
reg_items.update(enum_to_id.values())
mi = (java / "registry/ModItems.java").read_text(encoding="utf-8")
for m in re.finditer(r'registerCatalogPill\(CatalogPillType\.([A-Z0-9_]+)(?:,\s*"([a-z0-9_]+)")?', mi):
    base = enum_to_id[m.group(1)]
    reg_items.add(base if not m.group(2) else f"{base}_{m.group(2)}")
bulk = json.loads((assets / "catalog_bulk_items.json").read_text(encoding="utf-8"))
reg_items.update(o["id"] for o in bulk["items"])

# blocks from ModBlocks
mb = java / "registry/ModBlocks.java"
if mb.exists():
    reg_blocks.update(re.findall(r'\.register\("([a-z0-9_]+)"', mb.read_text(encoding="utf-8")))

aliases: dict[str, str] = {}
idmap = json.loads((data / "reference/text_material_id_map.json").read_text(encoding="utf-8"))
for e in idmap["entries"]:
    sid, cid = e.get("source_id"), e.get("canonical_id") or ""
    if isinstance(sid, str) and isinstance(cid, str) and cid.startswith("seeking_immortals:"):
        aliases[sid] = cid.split(":", 1)[1]
    elif isinstance(sid, str) and isinstance(cid, str) and cid.startswith("minecraft:"):
        aliases[sid] = cid


def covered_item(i: str) -> bool:
    i = (i or "").split(":")[-1]
    if not i:
        return True
    if i in reg_items or i in reg_blocks:
        return True
    t = aliases.get(i)
    if t:
        if t.startswith("minecraft:") or t in reg_items or t in reg_blocks:
            return True
        for s in ("_low", "_mid", "_high", "_supreme"):
            if f"{t}{s}" in reg_items:
                return True
    for s in ("_low", "_mid", "_high", "_supreme"):
        if f"{i}{s}" in reg_items:
            return True
    return False


# ---- assets ----
item_models = {p.stem for p in (assets / "models/item").glob("*.json")}
item_tex = {p.stem for p in (assets / "textures/item").glob("*.png")}
block_models = {p.stem for p in (assets / "models/block").glob("*.json")} if (assets / "models/block").exists() else set()
blockstates = {p.stem for p in (assets / "blockstates").glob("*.json")} if (assets / "blockstates").exists() else set()
zh = json.loads((assets / "lang/zh_cn.json").read_text(encoding="utf-8"))
en = json.loads((assets / "lang/en_us.json").read_text(encoding="utf-8"))
zh_items = {k[len("item.seeking_immortals.") :] for k in zh if k.startswith("item.seeking_immortals.")}
zh_blocks = {k[len("block.seeking_immortals.") :] for k in zh if k.startswith("block.seeking_immortals.")}

# registered items missing assets (exclude entities/tabs/helpers)
SKIP_ASSET = {
    "cultivation_fireball",
    "cushion_seat",
    "earth_wall",
    "formation_core",
    "market_trader",
    "refinement",
    "sect_steward",
    "seeking_immortals_tab",
    "spirit_boat",
    "storage_bracelet",
    "summoned_servitor",
    "sword_projectile",
}
no_model = sorted(i for i in reg_items if i not in item_models and i not in SKIP_ASSET and i not in reg_blocks)
no_tex = sorted(i for i in reg_items if i not in item_tex and i not in SKIP_ASSET and i not in reg_blocks and i in item_models)
no_zh = sorted(i for i in reg_items if i not in zh_items and i not in zh_blocks and i not in SKIP_ASSET)

# block assets
no_block_model = sorted(i for i in reg_blocks if i not in block_models and i not in blockstates)
no_block_zh = sorted(i for i in reg_blocks if i not in zh_blocks and f"item.seeking_immortals.{i}" not in zh)

# ---- key catalogs item coverage ----
FOCUS = [
    "artifacts_catalog.json",
    "materials_catalog.json",
    "consumables_catalog.json",
    "manuals_catalog.json",
    "pills_catalog.json",
    "talisman_catalog.json",
    "talisman_materials_catalog.json",
    "talisman_recipes.json",
    "formation_items_catalog.json",
    "block_items_catalog.json",
    "currency_items.json",
    "flight_vehicles.json",
    "puppet_definitions.json",
    "puppet_parts_catalog.json",
    "spirit_herbs_catalog.json",
    "alchemy_recipes.json",
    "refinement_recipes.json",
    "novel_items_waves.json",
    "merchant_shops.json",
    "boss_loot_tables.json",
    "loot_tables.json",
    "item_id_aliases.json",
]


def extract_ids(obj, ids: set[str]) -> None:
    keys = {
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
            if k in keys and isinstance(v, str):
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
    if i.startswith(("recipe_", "craft_", "assemble_", "upgrade_", "refine_", "quest_", "hook_", "event_", "rep_", "loot_")):
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
            "_bank",
            "_forge",
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


print("=== KEY CATALOG ITEM GAPS ===")
cat_miss: dict[str, list[str]] = {}
for name in FOCUS:
    paths = list(text.rglob(name)) + list(data.rglob(name))
    if not paths:
        print(name, "NOT_FOUND")
        continue
    ids: set[str] = set()
    for p in paths:
        try:
            extract_ids(json.loads(p.read_text(encoding="utf-8")), ids)
        except Exception:
            pass
    miss = sorted(i for i in ids if not covered_item(i) and not is_non_item(i))
    print(f"{name}: ids={len(ids)} missing={len(miss)}" + ((" -> " + str(miss[:20])) if miss else ""))
    if miss:
        cat_miss[name] = miss

# shipped refs
refs: set[str] = set()
for sub in ("shops", "alchemy", "recipes", "loot_tables", "artifacts"):
    d = data / sub
    if d.exists():
        for p in d.rglob("*.json"):
            refs.update(re.findall(r"seeking_immortals:([a-z0-9_]+)", p.read_text(encoding="utf-8", errors="ignore")))
print("shipped refs missing", sorted(i for i in refs if not covered_item(i)))

# techniques coverage if csv exists
csv = root / "project_docs/text_material_technique_coverage.csv"
if csv.exists():
    rows = csv.read_text(encoding="utf-8", errors="ignore").splitlines()
    print("technique csv rows", len(rows))
    # count missing markers
    miss_tech = [ln for ln in rows[1:] if ",missing" in ln.lower() or ",todo" in ln.lower() or ",partial" in ln.lower()]
    print("technique non-done sample", len(miss_tech), miss_tech[:10])

# technique data ids vs text techniques
tech_data_ids = set()
if (data / "cultivation").exists():
    for p in (data / "cultivation").rglob("*.json"):
        try:
            obj = json.loads(p.read_text(encoding="utf-8"))
        except Exception:
            continue
        if isinstance(obj, dict) and "techniques" in obj and isinstance(obj["techniques"], list):
            for e in obj["techniques"]:
                if isinstance(e, dict) and isinstance(e.get("id"), str):
                    tech_data_ids.add(e["id"])
        # sometimes top-level list or entries
        extract_ids(obj, tech_data_ids)
print("cultivation technique-ish ids in data", len(tech_data_ids))

# text techniques folder
text_tech = set()
for p in list((text / "data/techniques").rglob("*.json")) if (text / "data/techniques").exists() else []:
    try:
        obj = json.loads(p.read_text(encoding="utf-8"))
    except Exception:
        continue
    if isinstance(obj, list):
        for e in obj:
            if isinstance(e, dict) and isinstance(e.get("id"), str):
                text_tech.add(e["id"])
    elif isinstance(obj, dict):
        for key in ("techniques", "entries", "items", "list"):
            if key in obj and isinstance(obj[key], list):
                for e in obj[key]:
                    if isinstance(e, dict) and isinstance(e.get("id"), str):
                        text_tech.add(e["id"])
        # keyed dict
        for k, v in obj.items():
            if isinstance(v, dict) and re.fullmatch(r"[a-z][a-z0-9_]{1,80}", k) and ("display" in v or "cost" in v or "realm" in v or "name" in v):
                text_tech.add(k)
print("text techniques ids", len(text_tech))
# don't claim technique missing as item; just report

# creative tab: bulk loop exists?
tab = (java / "registry/ModCreativeTabs.java").read_text(encoding="utf-8", errors="ignore")
print("creative tab has bulk loop", "ModBulkItems" in tab)

# recipes missing model outputs?
recipe_items = set()
if (data / "recipes").exists():
    for p in (data / "recipes").rglob("*.json"):
        recipe_items.update(re.findall(r"seeking_immortals:([a-z0-9_]+)", p.read_text(encoding="utf-8", errors="ignore")))
print("recipe refs missing reg", sorted(i for i in recipe_items if not covered_item(i)))

print("=== ASSET GAPS ===")
print("reg_items", len(reg_items), "reg_blocks", len(reg_blocks))
print("item no_model", len(no_model), no_model[:30])
print("item no_tex(with model)", len(no_tex), no_tex[:30])
print("item no_zh", len(no_zh), no_zh[:30])
print("block no_model/state", len(no_block_model), no_block_model[:30])
print("block no_zh", len(no_block_zh), no_block_zh[:30])

# id_map implemented item missing model
miss_model_impl = []
for e in idmap["entries"]:
    st = str(e.get("status") or "")
    ct = e.get("canonical_type")
    cid = e.get("canonical_id") or ""
    if st.startswith("implemented") and ct == "item" and isinstance(cid, str) and cid.startswith("seeking_immortals:"):
        rid = cid.split(":", 1)[1]
        if rid not in item_models and rid in reg_items and rid not in SKIP_ASSET:
            miss_model_impl.append(rid)
print("implemented item missing model", len(miss_model_impl), miss_model_impl[:20])

# block_items_catalog placeables
bic = None
for p in list(text.rglob("block_items_catalog.json")) + list(data.rglob("block_items_catalog.json")):
    bic = json.loads(p.read_text(encoding="utf-8"))
    break
if bic:
    bids = set()
    extract_ids(bic, bids)
    print("block_items_catalog missing", sorted(i for i in bids if not covered_item(i) and not is_non_item(i)))

print("DONE")
