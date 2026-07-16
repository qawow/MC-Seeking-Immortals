#!/usr/bin/env python3
"""Cross-check catalog / text-material item ids against registered carriers."""
from __future__ import annotations

import json
import re
from pathlib import Path

root = Path(__file__).resolve().parents[1]
src = root / "src/main"
java_dir = src / "java/com/xunxian/seekingimmortals"
res = src / "resources"
assets = res / "assets/seeking_immortals"
data = res / "data/seeking_immortals"
text = root / "文本材料/data"

registered: set[str] = set()

for p in (java_dir / "registry").glob("*.java"):
    t = p.read_text(encoding="utf-8", errors="ignore")
    if "DeferredRegister" in t:
        registered.update(re.findall(r'\.register\("([a-z0-9_]+)"', t))

bulk_path = assets / "catalog_bulk_items.json"
bulk = json.loads(bulk_path.read_text(encoding="utf-8"))
bulk_ids = [o["id"] for o in bulk.get("items", []) if isinstance(o, dict) and "id" in o]
registered.update(bulk_ids)

model_ids: set[str] = set()
models_dir = assets / "models/item"
if models_dir.exists():
    model_ids = {p.stem for p in models_dir.glob("*.json")}

lang_zh = json.loads((assets / "lang/zh_cn.json").read_text(encoding="utf-8"))
lang_item_ids: set[str] = set()
for k in lang_zh:
    if k.startswith("item.seeking_immortals."):
        lang_item_ids.add(k[len("item.seeking_immortals.") :])
    if k.startswith("block.seeking_immortals."):
        lang_item_ids.add(k[len("block.seeking_immortals.") :])

id_map = json.loads((data / "reference/text_material_id_map.json").read_text(encoding="utf-8"))
if isinstance(id_map, dict):
    if "mappings" in id_map:
        mappings = id_map["mappings"]
    elif "map" in id_map:
        mappings = id_map["map"]
    else:
        mappings = id_map
else:
    mappings = {}

soft_aliases: dict[str, str] = {}
if isinstance(mappings, dict):
    for k, v in mappings.items():
        if k in ("schema", "version", "comment", "notes", "description", "meta"):
            continue
        if isinstance(v, str):
            soft_aliases[k] = v
        elif isinstance(v, dict):
            target = v.get("target") or v.get("item") or v.get("id") or v.get("mapped_to") or v.get("mod_id")
            if isinstance(target, str):
                soft_aliases[k] = target


def is_covered(i: str) -> bool:
    if i in registered:
        return True
    if i in soft_aliases:
        t = soft_aliases[i]
        if t.startswith("seeking_immortals:"):
            t = t.split(":", 1)[1]
        if ":" in t:
            # external/vanilla mapping
            return True
        if t in registered:
            return True
    return False


def extract_from_obj(obj, out: set[str]) -> None:
    if isinstance(obj, dict):
        for key in (
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
            "shops",
            "products",
            "list",
            "outputs",
            "rewards",
            "ingredients",
            "stock",
            "goods",
        ):
            if key not in obj:
                continue
            val = obj[key]
            if isinstance(val, list):
                for e in val:
                    if isinstance(e, dict):
                        for ik in (
                            "id",
                            "item_id",
                            "item",
                            "output_id",
                            "output",
                            "result",
                            "material_id",
                            "artifact_id",
                            "pill_id",
                            "manual_id",
                            "currency_id",
                            "block_id",
                            "carrier_id",
                            "product_id",
                            "product",
                            "formula_id",
                            "talisman_id",
                        ):
                            if ik in e and isinstance(e[ik], str):
                                s = e[ik]
                                if s.startswith("seeking_immortals:"):
                                    s = s.split(":", 1)[1]
                                if re.fullmatch(r"[a-z][a-z0-9_]{1,80}", s):
                                    out.add(s)
                    elif isinstance(e, str) and re.fullmatch(r"[a-z][a-z0-9_]{1,80}", e):
                        out.add(e)
            elif isinstance(val, dict):
                for k, v in val.items():
                    if re.fullmatch(r"[a-z][a-z0-9_]{1,80}", k):
                        out.add(k)
                    if isinstance(v, dict):
                        for ik in ("id", "item_id", "item", "output", "result"):
                            if ik in v and isinstance(v[ik], str):
                                s = v[ik]
                                if s.startswith("seeking_immortals:"):
                                    s = s.split(":", 1)[1]
                                if re.fullmatch(r"[a-z][a-z0-9_]{1,80}", s):
                                    out.add(s)
        # dict keyed by id
        if all(isinstance(k, str) for k in obj.keys()) and any(
            isinstance(v, dict) and ("name" in v or "display_name" in v or "rarity" in v or "category" in v or "tier" in v)
            for v in list(obj.values())[:5]
            if True
        ):
            for k, v in obj.items():
                if re.fullmatch(r"[a-z][a-z0-9_]{1,80}", k) and isinstance(v, dict):
                    out.add(k)
    elif isinstance(obj, list):
        for e in obj:
            if isinstance(e, dict):
                for ik in ("id", "item_id", "item", "output_id", "output", "result"):
                    if ik in e and isinstance(e[ik], str):
                        s = e[ik]
                        if s.startswith("seeking_immortals:"):
                            s = s.split(":", 1)[1]
                        if re.fullmatch(r"[a-z][a-z0-9_]{1,80}", s):
                            out.add(s)


def extract_from_file(p: Path, out: set[str]) -> None:
    try:
        obj = json.loads(p.read_text(encoding="utf-8"))
    except Exception:
        return
    extract_from_obj(obj, out)


scan_dirs = [
    data / "catalog",
    data / "artifacts",
    data / "shops",
    data / "alchemy",
    data / "reference",
    text,
]
json_files: list[Path] = []
for d in scan_dirs:
    if d.exists():
        json_files.extend(d.rglob("*.json"))

catalog_ids: set[str] = set()
for p in json_files:
    extract_from_file(p, catalog_ids)

# refinement / shops / recipes item refs
recipe_ids: set[str] = set()
for p in (data / "recipes").rglob("*.json") if (data / "recipes").exists() else []:
    try:
        t = p.read_text(encoding="utf-8")
    except Exception:
        continue
    for m in re.findall(r"seeking_immortals:([a-z0-9_]+)", t):
        recipe_ids.add(m)

# also pull item ids from refinement recipes more carefully
for p in list((data).rglob("*refinement*.json")) + list((data / "artifacts").glob("*.json") if (data / "artifacts").exists() else []):
    extract_from_file(p, catalog_ids)

NON_ITEM_PREFIXES = (
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
)
NON_ITEM_SUFFIXES = (
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
    "_compendium",
)
NON_ITEM_EXACT = {
    "schema",
    "version",
    "comment",
    "notes",
    "description",
    "items",
    "entries",
    "materials",
    "meta",
    "type",
    "name",
    "id",
    "rarity",
    "category",
    "tier",
    "count",
    "amount",
    "price",
    "cost",
    "weight",
    "chance",
}


def looks_item(i: str) -> bool:
    if i in NON_ITEM_EXACT:
        return False
    if any(i.startswith(p) for p in NON_ITEM_PREFIXES):
        return False
    if any(i.endswith(s) for s in NON_ITEM_SUFFIXES):
        return False
    tokens = (
        "_pill",
        "_ore",
        "_stone",
        "_herb",
        "_talisman",
        "_manual",
        "_sword",
        "_shield",
        "_core",
        "_jade",
        "_iron",
        "_silk",
        "_paper",
        "_formula",
        "_seed",
        "_token",
        "_charm",
        "_mirror",
        "_array",
        "_formation",
        "_furnace",
        "_bracelet",
        "_needle",
        "_umbrella",
        "_pearl",
        "_blood",
        "_root",
        "_moss",
        "_crystal",
        "_fragment",
        "_shard",
        "_ticket",
        "_permit",
        "_receipt",
        "_ginseng",
        "_coral",
        "_mushroom",
        "_feather",
        "_hide",
        "_shell",
        "_sac",
        "_wood",
        "_copper",
        "_bell",
        "_fan",
        "_ruler",
        "_brick",
        "_chain",
        "_disk",
        "_pendant",
        "_boots",
        "_bead",
        "_jar",
        "_slip",
        "_grass",
        "_ingot",
        "_dust",
        "_powder",
        "_fruit",
        "_flower",
        "_leaf",
        "_bark",
        "_bone",
        "_scale",
        "_horn",
        "_fang",
        "_claw",
        "_meat",
        "_skin",
        "_fur",
        "_egg",
        "_vine",
        "_resin",
        "_sap",
        "_oil",
        "_ash",
        "_sand",
        "_clay",
        "_gem",
        "_coin",
        "_medal",
        "_badge",
        "_flag",
        "_seal",
        "_scroll",
        "_book",
        "_tome",
        "_plate",
        "_blade",
        "_spear",
        "_bow",
        "_armor",
        "_robe",
        "_crown",
        "_ring",
        "_amulet",
        "_lantern",
        "_cauldron",
        "_pot",
        "_bowl",
        "_cup",
        "_banner",
        "_key",
        "_gate",
        "_altar",
        "_pedestal",
        "_bench",
        "_table",
        "_planter",
        "_forge",
        "_anvil",
        "_hammer",
        "_chisel",
        "_thread",
        "_cloth",
        "_rope",
        "_net",
        "_hook",
        "_compass",
        "_map",
        "_chart",
        "_pass",
        "_license",
        "_contract",
        "_deed",
        "_voucher",
        "_coupon",
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
        "_elixir",
        "_dan",
        "_san",
        "_gao",
        "_tang",
        "_jiu",
        "_low",
        "_mid",
        "_high",
        "_perfect",
    )
    if any(t in i for t in tokens):
        return True
    if "_" in i:
        return True
    return False


missing_catalog = sorted(i for i in catalog_ids if not is_covered(i))
missing_items = [i for i in missing_catalog if looks_item(i)]
missing_recipe = sorted(i for i in recipe_ids if i not in registered)

strict_files: list[Path] = []
for pat in (
    "*catalog*.json",
    "currency_items.json",
    "materials_catalog.json",
    "artifacts_catalog.json",
    "consumables_catalog.json",
    "manuals_catalog.json",
    "formation_items_catalog.json",
    "block_items_catalog.json",
    "moditems_artifacts_draft.json",
):
    if text.exists():
        strict_files.extend(text.glob(pat))
    strict_files.extend(data.rglob(pat))
    if (root / "文本材料").exists():
        strict_files.extend((root / "文本材料").rglob(pat))
strict_files = sorted(set(strict_files))
strict_ids: set[str] = set()
for p in strict_files:
    extract_from_file(p, strict_ids)
missing_strict = sorted(i for i in strict_ids if not is_covered(i) and looks_item(i))

# also extract from item_id_index if present
for name in ("item_id_index.json", "item_id_aliases.json", "items_by_region.json"):
    p = text / name
    if p.exists():
        extract_from_file(p, strict_ids)

missing_model = sorted(i for i in registered if i not in model_ids)
missing_lang = sorted(i for i in registered if i not in lang_item_ids)

print("registered", len(registered))
print("bulk", len(bulk_ids))
print("soft_aliases", len(soft_aliases))
print("catalog_ids", len(catalog_ids))
print("strict_files", len(strict_files))
print("strict_ids", len(strict_ids))
print("missing_itemish", len(missing_items))
print("missing_strict", len(missing_strict))
print("missing_recipe", len(missing_recipe))
print("missing_model", len(missing_model))
print("missing_lang", len(missing_lang))
print("--- missing_strict ---")
for i in missing_strict[:200]:
    print(i)
print("--- missing_itemish sample ---")
for i in missing_items[:100]:
    print(i)
print("--- missing_recipe ---")
for i in missing_recipe[:50]:
    print(i)
print("--- missing_model sample ---")
for i in missing_model[:40]:
    print(i)
print("--- missing_lang sample ---")
for i in missing_lang[:40]:
    print(i)

report = {
    "registered": len(registered),
    "bulk": len(bulk_ids),
    "soft_aliases": len(soft_aliases),
    "catalog_ids": len(catalog_ids),
    "strict_ids": len(strict_ids),
    "missing_itemish": missing_items,
    "missing_strict": missing_strict,
    "missing_recipe": missing_recipe,
    "missing_model": missing_model,
    "missing_lang": missing_lang,
}
outp = root / "project_docs/item_gap_audit_tmp.json"
outp.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
print("wrote", outp)
