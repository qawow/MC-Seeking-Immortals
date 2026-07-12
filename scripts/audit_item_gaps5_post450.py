#!/usr/bin/env python3
"""Strict full item-gap reaudit after 0.1.450."""
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
idmap = json.loads((data / "reference/text_material_id_map.json").read_text(encoding="utf-8"))
for e in idmap["entries"]:
    sid = e.get("source_id")
    cid = e.get("canonical_id") or ""
    if isinstance(sid, str) and isinstance(cid, str):
        if cid.startswith("seeking_immortals:"):
            aliases[sid] = cid.split(":", 1)[1]
        else:
            aliases[sid] = cid


def covered(i: str) -> bool:
    i = (i or "").split(":")[-1]
    if not i:
        return True
    if i in registered:
        return True
    t = aliases.get(i)
    if t:
        if t.startswith("minecraft:") or (":" in t and not t.startswith("seeking_immortals:")):
            return True
        if t in registered:
            return True
        for s in ("_low", "_mid", "_high", "_supreme"):
            if f"{t}{s}" in registered:
                return True
    for s in ("_low", "_mid", "_high", "_supreme"):
        if f"{i}{s}" in registered:
            return True
    return False


NON_ITEM_EXACT = {
    "fail",
    "explosion",
    "schema_version",
    "result",
    "craft",
    "deploy",
    "operate",
    "study",
    "trade",
    "use",
    "cost",
    "price",
    "count",
    "tier",
    "low",
    "mid",
    "high",
    "item",
    "id",
    "name",
    "type",
    "note",
    "display",
    "rarity",
    "category",
    "description",
    "lore",
    "text",
    "setting",
    "materials",
    "ingredients",
    "outputs",
    "rewards",
    "entries",
    "items",
    "list",
    "stock",
    "goods",
    "recipes",
    "vehicles",
    "formations",
    "definitions",
    "tiers",
    "components",
    "files",
    "sections",
    "rules",
    "synergy",
    "generation",
    "obtain",
    "distinction",
    "apply",
    "attack",
    "defense",
    "equip",
    "consume",
    "link",
    "chain",
    "chronicle",
    "method",
    "station",
    "role",
    "grade",
    "effect",
    "speed",
    "region",
    "faction",
    "source",
    "path",
    "bonus",
    "stack",
    "alias_of_grade",
    "realm_min",
    "base_success_rate",
    "yield",
    "ink",
    "paper_grade",
    "paper_item_id",
    "talisman_id",
    "output",
    "product",
    "chance",
    "weight",
    "rolls",
    "pools",
    "boss_id",
    "secret_realm",
    "crossref",
    "disclaimer",
    "tables",
    "recipe_tier_to_table",
    "artifact_drops_extension",
    "craft_station",
    "rules_ref",
    "paper_grade_map_ref",
    "recipe_paper_field",
    "papers",
    "inks",
    "craft_recipes",
    "source_file",
    "ref",
    "learn_requirements",
    "learn_source",
    "manual",
    "sect_talisman_hall",
    "beast_tribulation_bonus",
    "demon_qi_penalty",
    "five_elements_mountain_array",
    "nether_river_shield_zone",
}
NON_PREFIX = (
    "recipe_",
    "craft_",
    "assemble_",
    "upgrade_",
    "refine_",
    "quest_",
    "hook_",
    "event_",
    "chapter_",
    "node_",
    "sect_",
    "faction_",
    "realm_",
    "stage_",
    "skill_",
    "spell_",
    "technique_",
    "npc_",
    "dialogue_",
    "mission_",
    "story_",
    "wave_",
    "phase_",
    "zone_",
    "trial_",
    "index_",
    "dim_",
    "biome_",
    "region_",
    "path_",
    "method_",
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
)
NON_SUFFIX = (
    "_hall",
    "_shop",
    "_market",
    "_stall",
    "_pavilion",
    "_auction",
    "_exchange",
    "_bank",
    "_vendor",
    "_lane",
    "_supply",
    "_general",
    "_trade",
    "_forge",
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
    "_loop",
    "_outpost",
    "_chance",
)


def is_non_item(i: str) -> bool:
    if i in NON_ITEM_EXACT:
        return True
    if any(i.startswith(p) for p in NON_PREFIX):
        return True
    if any(i.endswith(s) for s in NON_SUFFIX):
        return True
    return False


ITEM_KEYS = {
    "item",
    "item_id",
    "itemId",
    "output",
    "output_id",
    "result",
    "product",
    "product_id",
    "material_id",
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
    "paper_item_id",
}
LIST_KEYS = {
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
    "papers",
    "inks",
    "drops",
    "pools",
}


def collect_item_fields(obj, out: set[tuple[str, str]], file: str) -> None:
    if isinstance(obj, dict):
        for k, v in obj.items():
            if k in ITEM_KEYS and isinstance(v, str):
                s = v.split(":")[-1]
                if re.fullmatch(r"[a-z][a-z0-9_]{1,80}", s):
                    out.add((s, file))
            elif k in LIST_KEYS and isinstance(v, list):
                for e in v:
                    if isinstance(e, dict):
                        for ik in ("id", "item", "item_id", "output", "result", "product"):
                            if ik in e and isinstance(e[ik], str):
                                s = e[ik].split(":")[-1]
                                if re.fullmatch(r"[a-z][a-z0-9_]{1,80}", s):
                                    out.add((s, file))
                        collect_item_fields(e, out, file)
                    elif isinstance(e, str) and re.fullmatch(r"[a-z][a-z0-9_]{1,80}", e):
                        out.add((e, file))
            elif isinstance(v, (dict, list)):
                collect_item_fields(v, out, file)
    elif isinstance(obj, list):
        for e in obj:
            collect_item_fields(e, out, file)


item_fields: set[tuple[str, str]] = set()
for p in text.rglob("*.json"):
    try:
        obj = json.loads(p.read_text(encoding="utf-8"))
    except Exception:
        continue
    collect_item_fields(obj, item_fields, str(p.relative_to(root)))

miss1 = sorted({i for i, f in item_fields if not covered(i) and not is_non_item(i)})
print("TEXT item-field missing", len(miss1))
for i in miss1:
    files = sorted({f for x, f in item_fields if x == i})[:4]
    print(" ", i, files)

refs: set[str] = set()
for sub in ("shops", "alchemy", "recipes", "loot_tables", "artifacts", "worldpack", "catalog"):
    d = data / sub
    if d.exists():
        for p in d.rglob("*.json"):
            refs.update(
                re.findall(
                    r"seeking_immortals:([a-z0-9_]+)",
                    p.read_text(encoding="utf-8", errors="ignore"),
                )
            )
for p in root.rglob("*.snbt"):
    sp = str(p).lower()
    if "ftb" in sp or "quest" in sp:
        refs.update(
            re.findall(
                r"seeking_immortals:([a-z0-9_]+)",
                p.read_text(encoding="utf-8", errors="ignore"),
            )
        )
miss2 = sorted(i for i in refs if not covered(i))
print("SHIPPED refs missing", len(miss2), miss2[:80])

KEY = [
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
    "talisman_materials_catalog.json",
    "talisman_recipes.json",
    "alchemy_recipes.json",
    "merchant_shops.json",
    "novel_items_waves.json",
    "pills_catalog.json",
    "boss_loot_tables.json",
    "loot_tables.json",
    "craft_daily_loops.json",
    "puppet_craft_recipes.json",
]
print("--- key catalogs ---")
for name in KEY:
    paths = list(text.rglob(name)) + list(data.rglob(name))
    if not paths:
        print(name, "NOT_FOUND")
        continue
    ids: set[str] = set()
    for p in paths:
        try:
            obj = json.loads(p.read_text(encoding="utf-8"))
        except Exception:
            continue
        tmp: set[tuple[str, str]] = set()
        collect_item_fields(obj, tmp, p.name)
        ids |= {i for i, _ in tmp}
    miss = sorted(i for i in ids if not covered(i) and not is_non_item(i))
    print(f"{name}: ids={len(ids)} missing={len(miss)}" + ((" -> " + str(miss)) if miss else ""))

print("id_map non-impl item-like:")
for e in idmap["entries"]:
    st = str(e.get("status") or "")
    ct = str(e.get("canonical_type") or "")
    if ct in ("item", "future_item") and not st.startswith("implemented"):
        print(" ", e.get("source_id"), st, ct, e.get("canonical_id"))

models = {p.stem for p in (assets / "models/item").glob("*.json")}
miss_model = []
for e in idmap["entries"]:
    st = str(e.get("status") or "")
    ct = e.get("canonical_type")
    cid = e.get("canonical_id") or ""
    if st.startswith("implemented") and ct == "item" and isinstance(cid, str) and cid.startswith("seeking_immortals:"):
        rid = cid.split(":", 1)[1]
        if rid not in models:
            miss_model.append(rid)
print("impl missing model", len(miss_model), miss_model[:20])
print("registered", len(registered), "bulk", len(bulk["items"]))
print([ln for ln in (root / "gradle.properties").read_text(encoding="utf-8", errors="ignore").splitlines() if "mod_version" in ln][0])
