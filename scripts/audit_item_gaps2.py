#!/usr/bin/env python3
"""Accurate item gap audit for Seeking Immortals."""
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

# 1) Java registry helpers + direct registers
helper_pats = [
    r'\.register\("([a-z0-9_]+)"',
    r'registerArtifact\("([a-z0-9_]+)"',
    r'registerMaterial\("([a-z0-9_]+)"',
    r'registerSpiritStone\("([a-z0-9_]+)"',
    r'registerPill\("([a-z0-9_]+)"',
    r'registerCatalogPill\("([a-z0-9_]+)"',
    r'registerFormula\("([a-z0-9_]+)"',
    r'registerManual\("([a-z0-9_]+)"',
    r'registerTalisman\("([a-z0-9_]+)"',
    r'registerHerb\("([a-z0-9_]+)"',
    r'register\w+\("([a-z0-9_]+)"',
]
for p in java_dir.rglob("*.java"):
    t = p.read_text(encoding="utf-8", errors="ignore")
    if "DeferredRegister" not in t and "register" not in t:
        continue
    for pat in helper_pats:
        registered.update(re.findall(pat, t))

# 2) bulk carriers
bulk = json.loads((assets / "catalog_bulk_items.json").read_text(encoding="utf-8"))
bulk_ids = [o["id"] for o in bulk.get("items", []) if isinstance(o, dict) and "id" in o]
registered.update(bulk_ids)

# 3) id map entries
id_map = json.loads((data / "reference/text_material_id_map.json").read_text(encoding="utf-8"))
aliases: dict[str, str] = {}
status_by_source: dict[str, str] = {}
for e in id_map.get("entries", []):
    if not isinstance(e, dict):
        continue
    sid = e.get("source_id")
    cid = e.get("canonical_id") or ""
    st = e.get("status") or ""
    if not isinstance(sid, str):
        continue
    status_by_source[sid] = st
    if isinstance(cid, str) and cid:
        if cid.startswith("seeking_immortals:"):
            aliases[sid] = cid.split(":", 1)[1]
            registered.add(cid.split(":", 1)[1])  # note: only if implemented; we'll filter later
        else:
            aliases[sid] = cid

# Don't treat unimplemented map targets as registered. Rebuild registered cleanly.
registered = set()
for p in (java_dir / "registry").glob("*.java"):
    t = p.read_text(encoding="utf-8", errors="ignore")
    for pat in helper_pats:
        registered.update(re.findall(pat, t))
# also scan item package for static registrations if any
for p in java_dir.rglob("*.java"):
    t = p.read_text(encoding="utf-8", errors="ignore")
    if "ITEMS.register" in t or "registerArtifact(" in t or "registerMaterial(" in t:
        for pat in helper_pats:
            registered.update(re.findall(pat, t))
registered.update(bulk_ids)

# rebuild aliases without polluting registered
aliases = {}
implemented_sources = set()
partial_sources = set()
missing_sources = set()
for e in id_map.get("entries", []):
    if not isinstance(e, dict):
        continue
    sid = e.get("source_id")
    cid = e.get("canonical_id") or ""
    st = (e.get("status") or "").lower()
    ctype = (e.get("canonical_type") or "").lower()
    if not isinstance(sid, str):
        continue
    if ctype and ctype not in ("item", "block", "block_item", "carrier", ""):
        # keep non-item for report but skip as item gap
        pass
    if isinstance(cid, str) and cid:
        if cid.startswith("seeking_immortals:"):
            aliases[sid] = cid.split(":", 1)[1]
        else:
            aliases[sid] = cid
    if st in ("implemented", "implemented-partial", "partial", "mapped", "alias"):
        implemented_sources.add(sid)
    elif st in ("missing", "planned", "todo", "unimplemented", ""):
        missing_sources.add(sid)
    if "partial" in st:
        partial_sources.add(sid)

models = {p.stem for p in (assets / "models/item").glob("*.json")} if (assets / "models/item").exists() else set()
lang = json.loads((assets / "lang/zh_cn.json").read_text(encoding="utf-8"))
lang_ids = set()
for k in lang:
    if k.startswith("item.seeking_immortals."):
        lang_ids.add(k[len("item.seeking_immortals.") :])
    if k.startswith("block.seeking_immortals."):
        lang_ids.add(k[len("block.seeking_immortals.") :])


def resolve(i: str) -> str | None:
    if i in registered:
        return i
    if i in aliases:
        t = aliases[i]
        if t.startswith("minecraft:") or (":" in t and not t.startswith("seeking_immortals:")):
            return t  # external ok
        if t in registered:
            return t
        if t.startswith("seeking_immortals:"):
            tt = t.split(":", 1)[1]
            if tt in registered:
                return tt
    return None


def is_covered(i: str) -> bool:
    return resolve(i) is not None


# Extract ids from key item catalogs only
ITEM_CATALOG_NAMES = {
    "materials_catalog.json",
    "artifacts_catalog.json",
    "consumables_catalog.json",
    "manuals_catalog.json",
    "formation_items_catalog.json",
    "block_items_catalog.json",
    "currency_items.json",
    "moditems_artifacts_draft.json",
    "pills_catalog.json",
    "talisman_catalog.json",
    "talisman_items.json",
    "puppet_definitions.json",
    "puppet_items.json",
    "flight_vehicles.json",
    "alchemy_recipes.json",
    "item_id_index.json",
}


def extract_ids(obj, out: set[str], mode: str = "strict") -> None:
    if isinstance(obj, dict):
        # if this looks like an item entry
        for ik in (
            "id",
            "item_id",
            "item",
            "output",
            "output_id",
            "result",
            "material_id",
            "artifact_id",
            "pill_id",
            "manual_id",
            "currency_id",
            "block_id",
            "carrier_id",
            "product",
            "product_id",
            "formula_id",
            "talisman_id",
            "source_id",
            "canonical_id",
        ):
            if ik in obj and isinstance(obj[ik], str):
                s = obj[ik]
                if s.startswith("seeking_immortals:"):
                    s = s.split(":", 1)[1]
                if re.fullmatch(r"[a-z][a-z0-9_]{1,80}", s):
                    out.add(s)
        for k, v in obj.items():
            if k in (
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
            ):
                extract_ids(v, out, mode)
            elif isinstance(v, (dict, list)) and mode == "loose":
                extract_ids(v, out, mode)
            elif isinstance(v, dict) and re.fullmatch(r"[a-z][a-z0-9_]{1,80}", k):
                # keyed catalog
                out.add(k)
                extract_ids(v, out, mode)
    elif isinstance(obj, list):
        for e in obj:
            extract_ids(e, out, mode)
    elif isinstance(obj, str):
        s = obj
        if s.startswith("seeking_immortals:"):
            s = s.split(":", 1)[1]
            if re.fullmatch(r"[a-z][a-z0-9_]{1,80}", s):
                out.add(s)


strict_ids: set[str] = set()
catalog_files: list[Path] = []
for base in [text, data, root / "文本材料"]:
    if not base.exists():
        continue
    for p in base.rglob("*.json"):
        if p.name in ITEM_CATALOG_NAMES or p.name.endswith("_catalog.json") or "items_catalog" in p.name:
            catalog_files.append(p)

# also refinement recipes
for p in data.rglob("*refinement*.json"):
    catalog_files.append(p)
for p in (data / "artifacts").glob("*.json") if (data / "artifacts").exists() else []:
    catalog_files.append(p)
for p in (data / "shops").rglob("*.json") if (data / "shops").exists() else []:
    catalog_files.append(p)
if (text / "merchant_shops.json").exists():
    catalog_files.append(text / "merchant_shops.json")

catalog_files = sorted(set(catalog_files))
for p in catalog_files:
    try:
        obj = json.loads(p.read_text(encoding="utf-8"))
    except Exception:
        continue
    extract_ids(obj, strict_ids, "strict")

# recipe refs
recipe_ids: set[str] = set()
if (data / "recipes").exists():
    for p in (data / "recipes").rglob("*.json"):
        t = p.read_text(encoding="utf-8", errors="ignore")
        recipe_ids.update(re.findall(r"seeking_immortals:([a-z0-9_]+)", t))

# id_map item-type sources that are not covered
map_item_missing = []
map_item_partial = []
for e in id_map.get("entries", []):
    if not isinstance(e, dict):
        continue
    sid = e.get("source_id")
    st = (e.get("status") or "").lower()
    ctype = (e.get("canonical_type") or "item").lower()
    if ctype not in ("item", "block", "block_item", "carrier"):
        continue
    if not isinstance(sid, str):
        continue
    if not is_covered(sid):
        map_item_missing.append((sid, st, e.get("canonical_id"), e.get("note")))
    elif "partial" in st or st in ("mapped", "alias"):
        # covered via alias; still note soft
        target = resolve(sid)
        if target != sid and target is not None:
            map_item_partial.append((sid, target, st))

missing_strict = sorted({i for i in strict_ids if not is_covered(i)})
missing_recipe = sorted({i for i in recipe_ids if not is_covered(i)})
missing_model = sorted(i for i in registered if i not in models)
missing_lang = sorted(i for i in registered if i not in lang_ids)

# sanity checks
checks = [
    "bedrock_shield",
    "spirit_iron",
    "void_crystal",
    "beast_core",
    "cold_jade",
    "storage_bracelet_low",
    "alchemy_furnace_g1",
    "yin_essence_ore_block",
    "teleport_array",
    "refinement_forge_g1",
    "invisible_needle_set",
    "low_spirit_iron",
    "metal_spirit_stone",
]
print("registered", len(registered), "bulk", len(bulk_ids), "aliases", len(aliases))
print("catalog_files", len(catalog_files), "strict_ids", len(strict_ids))
print("missing_strict", len(missing_strict))
print("missing_recipe", len(missing_recipe))
print("map_item_missing", len(map_item_missing))
print("map_item_partial_alias", len(map_item_partial))
print("missing_model", len(missing_model))
print("missing_lang", len(missing_lang))
print("--- checks ---")
for c in checks:
    print(c, "covered" if is_covered(c) else "MISSING", "reg" if c in registered else "-", "alias->", aliases.get(c))
print("--- missing_strict ---")
for i in missing_strict:
    print(i, "alias", aliases.get(i), "status", status_by_source.get(i) if "status_by_source" in dir() else "")
print("--- missing_recipe ---")
for i in missing_recipe:
    print(i)
print("--- map missing sample ---")
for row in map_item_missing[:80]:
    print(row)
print("--- missing_model ---")
for i in missing_model[:40]:
    print(i)
print("--- missing_lang ---")
for i in missing_lang[:40]:
    print(i)

# status_by_source rebuild for print
status_by_source = {}
for e in id_map.get("entries", []):
    if isinstance(e, dict) and isinstance(e.get("source_id"), str):
        status_by_source[e["source_id"]] = e.get("status")

report = {
    "registered": len(registered),
    "bulk": len(bulk_ids),
    "aliases": len(aliases),
    "strict_ids": len(strict_ids),
    "missing_strict": missing_strict,
    "missing_recipe": missing_recipe,
    "map_item_missing": [
        {"source_id": a, "status": b, "canonical_id": c, "note": d} for a, b, c, d in map_item_missing
    ],
    "missing_model": missing_model,
    "missing_lang": missing_lang,
    "checks": {c: {"covered": is_covered(c), "registered": c in registered, "alias": aliases.get(c)} for c in checks},
}
out = root / "project_docs/item_gap_audit_tmp.json"
out.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
print("wrote", out)
