#!/usr/bin/env python3
"""Deep re-audit for missing physical item carriers after 0.1.448."""
from __future__ import annotations

import json
import re
from collections import defaultdict
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

idmap = json.loads((data / "reference/text_material_id_map.json").read_text(encoding="utf-8"))
aliases: dict[str, str] = {}
status: dict[str, str] = {}
for e in idmap["entries"]:
    sid = e.get("source_id")
    cid = e.get("canonical_id") or ""
    if not isinstance(sid, str):
        continue
    status[sid] = str(e.get("status") or "")
    if isinstance(cid, str) and cid:
        if cid.startswith("seeking_immortals:"):
            aliases[sid] = cid.split(":", 1)[1]
        else:
            aliases[sid] = cid


def covered(i: str) -> bool:
    if not i:
        return True
    i = i.split(":")[-1]
    if i in registered:
        return True
    t = aliases.get(i)
    if t:
        if t.startswith("minecraft:"):
            return True
        if t in registered:
            return True
        for suf in ("_low", "_mid", "_high", "_supreme"):
            if f"{t}{suf}" in registered:
                return True
    for suf in ("_low", "_mid", "_high", "_supreme"):
        if f"{i}{suf}" in registered:
            return True
    return False


ITEMISH_KEYS = {
    "id",
    "item",
    "item_id",
    "itemId",
    "output",
    "output_id",
    "result",
    "product",
    "product_id",
    "material_id",
    "material",
    "artifact_id",
    "pill_id",
    "manual_id",
    "currency_id",
    "block_id",
    "carrier_id",
    "talisman_id",
    "formula_id",
    "puppet_id",
    "vehicle_id",
    "part_id",
    "herb_id",
    "reward_item",
    "cost_item",
    "fuel",
}

META = {
    "schema_version",
    "source",
    "note",
    "type",
    "display",
    "count",
    "tier",
    "low",
    "mid",
    "high",
    "item",
    "entries",
    "craft",
    "result",
    "fail",
    "apply",
    "attack",
    "defense",
    "deploy",
    "equip",
    "operate",
    "study",
    "trade",
    "use",
    "cost",
    "price",
    "amount",
    "weight",
    "chance",
    "name",
    "id",
    "rarity",
    "category",
    "description",
    "lore",
    "text",
    "setting",
    "learn_requirements",
    "realm_min",
    "base_success_rate",
    "requires_method",
    "quest_unlock",
    "materials",
    "learn_source",
    "method",
    "station",
    "role",
    "hp_base",
    "damage",
    "speed",
    "region",
    "faction",
    "grade",
    "effect",
    "components",
    "recipes",
    "vehicles",
    "formations",
    "definitions",
    "tiers",
    "upgrade_tree",
    "control_methods_ref",
    "assembly_note",
    "faction_loops",
    "parts_ref",
    "definitions_ref",
    "craft_station",
    "recipe_count",
    "manual_known",
    "forge_grade",
    "manual_id",
    "artifact_id",
    "recipe_id",
    "by_tier",
    "categories",
    "files",
    "sections",
    "rules",
    "synergy",
    "generation",
    "obtain",
    "distinction",
    "examples_patch",
    "counts",
    "craft_rules",
    "forge",
    "forge_overheat",
    "forge_registration",
    "material_quality",
    "multi_root_weights",
    "realm_control",
    "realm_power_scale",
    "realms",
    "reputation",
    "reputation_risk",
    "special_control",
    "consume",
    "link",
    "chain",
    "chronicle",
}

NON_PREFIX = (
    "realm_",
    "stage_",
    "sect_",
    "faction_",
    "quest_",
    "hook_",
    "event_",
    "chapter_",
    "node_",
    "dim_",
    "biome_",
    "region_",
    "path_",
    "method_",
    "skill_",
    "spell_",
    "technique_",
    "npc_",
    "dialogue_",
    "mission_",
    "story_",
    "war_",
    "route_",
    "anchor_",
    "daily_",
    "economy_",
    "price_",
    "band_",
    "boss_",
    "encounter_",
    "loot_table",
    "shop_",
    "merchant_",
    "chain_",
    "wave_",
    "phase_",
    "zone_",
    "trial_",
    "index_",
    "assemble_",
    "upgrade_",
    "refine_",
    "recipe_",
)
NON_SUFFIX = (
    "_quest",
    "_event",
    "_chapter",
    "_faction",
    "_sect",
    "_region",
    "_dimension",
    "_biome",
    "_technique",
    "_spell",
    "_skill",
    "_method",
    "_path",
    "_dialogue",
    "_mission",
    "_index",
    "_map",
    "_schema",
    "_rules",
    "_catalog",
    "_list",
    "_table",
    "_graph",
    "_network",
    "_flow",
    "_system",
    "_compendium",
    "_guidance",
    "_magnitudes",
    "_bands",
    "_tiers",
    "_templates",
    "_bindings",
    "_manifest",
    "_registry",
    "_line",
    "_optional",
    "_ending",
    "_audience",
    "_survive",
    "_contact",
    "_defense",
    "_run",
    "_clash",
    "_escort",
    "_blockade",
    "_war",
    "_hall",
    "_shop",
    "_market",
    "_stall",
    "_pavilion",
    "_auction",
    "_exchange",
    "_bank",
    "_ferry",
    "_vendor",
    "_lane",
    "_forge",
    "_supply",
)


def looks_item(i: str) -> bool:
    if i in META:
        return False
    if any(i.startswith(p) for p in NON_PREFIX):
        return False
    if any(i.endswith(s) for s in NON_SUFFIX):
        return False
    if len(i) < 3:
        return False
    if "_" not in i:
        return False
    return True


def extract_ids(obj, out: set[str]) -> None:
    if isinstance(obj, dict):
        for k, v in obj.items():
            if k in ITEMISH_KEYS and isinstance(v, str):
                s = v.split(":")[-1]
                if re.fullmatch(r"[a-z][a-z0-9_]{1,80}", s):
                    out.add(s)
            elif k in {
                "items",
                "materials",
                "entries",
                "artifacts",
                "pills",
                "talismans",
                "manuals",
                "blocks",
                "currencies",
                "consumables",
                "formations",
                "recipes",
                "products",
                "list",
                "stock",
                "goods",
                "outputs",
                "ingredients",
                "rewards",
                "vehicles",
                "puppets",
                "definitions",
                "herbs",
                "parts",
                "tier_ladder",
            }:
                extract_ids(v, out)
            elif isinstance(v, (dict, list)):
                if isinstance(v, dict) and re.fullmatch(r"[a-z][a-z0-9_]{1,80}", k):
                    if any(
                        x in v
                        for x in (
                            "display",
                            "rarity",
                            "category",
                            "tier",
                            "grade",
                            "hp_base",
                            "speed",
                            "fuel",
                            "effect",
                            "craft_materials",
                        )
                    ):
                        out.add(k)
                extract_ids(v, out)
    elif isinstance(obj, list):
        for e in obj:
            extract_ids(e, out)


sources: list[Path] = []
for base in [
    text,
    data / "catalog",
    data / "artifacts",
    data / "alchemy",
    data / "shops",
    data / "text_material",
    data / "reference",
]:
    if base.exists():
        sources.extend(base.rglob("*.json"))
sources = sorted(set(sources))

recipe_refs: set[str] = set()
for sub in ("recipes", "shops", "alchemy"):
    d = data / sub
    if d.exists():
        for p in d.rglob("*.json"):
            recipe_refs.update(
                re.findall(
                    r"seeking_immortals:([a-z0-9_]+)",
                    p.read_text(encoding="utf-8", errors="ignore"),
                )
            )

catalog_ids: set[str] = set()
by_file: dict[str, set[str]] = defaultdict(set)
for p in sources:
    try:
        obj = json.loads(p.read_text(encoding="utf-8"))
    except Exception:
        continue
    ids: set[str] = set()
    extract_ids(obj, ids)
    ids = {i for i in ids if looks_item(i)}
    catalog_ids |= ids
    miss = {i for i in ids if not covered(i)}
    if miss:
        by_file[str(p.relative_to(root))] = miss

missing = sorted(i for i in catalog_ids if not covered(i))
missing_recipe = sorted(i for i in recipe_refs if not covered(i))

print("registered", len(registered), "bulk", len(bulk["items"]))
print("sources scanned", len(sources))
print("catalog_ids itemish", len(catalog_ids))
print("missing itemish", len(missing))
print("missing recipe/shop/alchemy refs", len(missing_recipe), missing_recipe)
print("--- missing ---")
for i in missing:
    print(i, "status=", status.get(i), "alias=", aliases.get(i))
print("--- files with missing top ---")
for f, miss in sorted(by_file.items(), key=lambda x: -len(x[1]))[:50]:
    print(len(miss), f)
    for i in sorted(miss)[:20]:
        print("   ", i)

models = {p.stem for p in (assets / "models/item").glob("*.json")}
missing_model_impl = []
for e in idmap["entries"]:
    st = str(e.get("status") or "")
    ct = e.get("canonical_type")
    cid = e.get("canonical_id") or ""
    if st.startswith("implemented") and ct == "item" and isinstance(cid, str) and cid.startswith("seeking_immortals:"):
        rid = cid.split(":", 1)[1]
        if rid not in models:
            missing_model_impl.append(rid)
print("implemented items missing model", len(missing_model_impl), missing_model_impl[:40])

# key catalogs strict missing
KEY_FILES = [
    "artifacts_catalog.json",
    "materials_catalog.json",
    "consumables_catalog.json",
    "manuals_catalog.json",
    "formation_items_catalog.json",
    "formation_catalog.json",
    "block_items_catalog.json",
    "currency_items.json",
    "flight_vehicles.json",
    "puppet_definitions.json",
    "puppet_parts_catalog.json",
    "refine_manual_index.json",
    "refinement_system.json",
    "spirit_herbs_catalog.json",
    "talisman_catalog.json",
    "talisman_materials.json",
    "alchemy_recipes.json",
    "merchant_shops.json",
    "novel_items_waves.json",
    "tribulation_items.json",
]
print("--- key file missing ---")
for name in KEY_FILES:
    paths = list(text.rglob(name)) + list(data.rglob(name))
    if not paths:
        print(name, "FILE_NOT_FOUND")
        continue
    ids: set[str] = set()
    for p in paths:
        try:
            extract_ids(json.loads(p.read_text(encoding="utf-8")), ids)
        except Exception:
            pass
    miss = sorted(i for i in ids if looks_item(i) and not covered(i))
    print(f"{name}: ids={len(ids)} missing={len(miss)}")
    for i in miss:
        print("   ", i)

gp = (root / "gradle.properties").read_text(encoding="utf-8", errors="ignore")
print([ln for ln in gp.splitlines() if "mod_version" in ln][0])
