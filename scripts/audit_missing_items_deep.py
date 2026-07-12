#!/usr/bin/env python3
"""Deep re-audit of missing item carriers after 0.1.446."""
from __future__ import annotations

import json
import re
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(".")


def load_reg() -> set[str]:
    mi = (ROOT / "src/main/java/com/xunxian/seekingimmortals/registry/ModItems.java").read_text(encoding="utf-8")
    reg = set(
        re.findall(
            r'register(?:Artifact|Material|SpiritStone|TechniqueManual|CatalogPill|AlchemyFormula|AlchemyLid|DanFire)?\(\s*"([a-z0-9_]+)"',
            mi,
        )
    )
    reg |= set(re.findall(r'ITEMS\.register\(\s*"([a-z0-9_]+)"', mi))
    cpt = ROOT / "src/main/java/com/xunxian/seekingimmortals/item/pill/CatalogPillType.java"
    if cpt.exists():
        t = cpt.read_text(encoding="utf-8")
        for m in re.finditer(r'\("([a-z0-9_]+)"', t):
            base = m.group(1)
            reg.add(base)
            for q in ("low", "mid", "high", "supreme"):
                reg.add(f"{base}_{q}")
    bulk_path = ROOT / "src/main/resources/assets/seeking_immortals/catalog_bulk_items.json"
    if bulk_path.exists():
        bulk = json.loads(bulk_path.read_text(encoding="utf-8"))
        for it in bulk.get("items", []):
            reg.add(it["id"])
    mb = ROOT / "src/main/java/com/xunxian/seekingimmortals/registry/ModBlocks.java"
    if mb.exists():
        reg |= set(re.findall(r'register\(\s*"([a-z0-9_]+)"', mb.read_text(encoding="utf-8")))
    models = {p.stem for p in (ROOT / "src/main/resources/assets/seeking_immortals/models/item").glob("*.json")}
    reg |= models
    return reg


def load_resolve(reg: set[str]):
    idmap = json.loads(
        (ROOT / "src/main/resources/data/seeking_immortals/reference/text_material_id_map.json").read_text(
            encoding="utf-8"
        )
    )
    source_to_canon = {}
    for e in idmap["entries"]:
        if str(e.get("status", "")).startswith("implemented") and e.get("canonical_type") == "item":
            cid = e.get("canonical_id") or ""
            if cid.startswith("seeking_immortals:") or cid.startswith("minecraft:"):
                source_to_canon[e["source_id"]] = cid

    def resolve(i: str) -> bool:
        i = i.split(":")[-1]
        if i in reg:
            return True
        c = source_to_canon.get(i)
        if not c:
            return False
        if c.startswith("minecraft:"):
            return True
        return c.split(":")[-1] in reg

    return resolve, source_to_canon, idmap


ITEMISH_KEYS = {
    "id",
    "item",
    "item_id",
    "artifact_id",
    "pill_id",
    "talisman_id",
    "material_id",
    "output",
    "result",
    "product",
    "herb_id",
    "part_id",
    "disk_id",
    "token_id",
    "carrier_item",
    "fuel_item",
    "ticket_item",
    "currency_item",
    "reward_item",
    "cost_item",
    "require_item",
}

NON_ITEM_PREFIXES = (
    "craft_",
    "refine_",
    "recipe_",
    "hook_",
    "quest_",
    "chain_",
    "event_",
    "node_",
    "region_",
    "chapter_",
    "shop_",
    "route_",
    "biome_",
    "dim_",
)

NON_ITEM_EXACT = {
    "schema_version",
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
    "materials",
    "artifacts",
    "pills",
    "consumables",
    "recipes",
    "manuals",
    "herbs",
    "talismans",
    "parts",
    "items",
    "name",
    "desc",
    "description",
    "category",
    "rarity",
    "status",
    "source_id",
    "canonical_id",
    "canonical_type",
    "source_category",
    "source_files",
    "seeking_immortals",
    "minecraft",
    "forge",
    "overworld",
    "nether",
    "end",
}


def extract_ids(obj) -> set[str]:
    found: set[str] = set()

    def walk(o):
        if isinstance(o, dict):
            for k, v in o.items():
                if k in ITEMISH_KEYS and isinstance(v, str):
                    s = v.split(":")[-1]
                    if re.fullmatch(r"[a-z0-9_]+", s) and len(s) > 2:
                        found.add(s)
                elif k in (
                    "entries",
                    "materials",
                    "artifacts",
                    "pills",
                    "consumables",
                    "recipes",
                    "manuals",
                    "herbs",
                    "talismans",
                    "parts",
                    "items",
                ) and isinstance(v, dict):
                    for kk in v.keys():
                        if re.fullmatch(r"[a-z0-9_]+", kk) and len(kk) > 2:
                            found.add(kk)
                    walk(v)
                else:
                    walk(v)
        elif isinstance(o, list):
            for x in o:
                walk(x)

    walk(obj)
    return found


def main() -> None:
    reg = load_reg()
    resolve, source_to_canon, idmap = load_resolve(reg)
    bulk_n = len(
        json.loads(
            (ROOT / "src/main/resources/assets/seeking_immortals/catalog_bulk_items.json").read_text(encoding="utf-8")
        )["items"]
    )
    print(
        "version",
        [l for l in Path("gradle.properties").read_text(encoding="utf-8").splitlines() if "mod_version" in l][0],
    )
    print("reg_set", len(reg), "bulk", bulk_n)

    miss_by_file: dict[str, set[str]] = defaultdict(set)
    data_root = ROOT / "src/main/resources/data/seeking_immortals"
    for p in data_root.rglob("*.json"):
        rel = str(p.relative_to(ROOT)).replace("\\", "/")
        # skip patchouli narrative mostly, but still ok
        try:
            d = json.loads(p.read_text(encoding="utf-8"))
        except Exception:
            continue
        ids = extract_ids(d)
        ids |= set(re.findall(r"seeking_immortals:([a-z0-9_]+)", json.dumps(d)))
        for i in ids:
            if i in NON_ITEM_EXACT:
                continue
            if any(i.startswith(pref) for pref in NON_ITEM_PREFIXES):
                continue
            if not re.fullmatch(r"[a-z0-9_]+", i) or len(i) < 3:
                continue
            if not resolve(i):
                miss_by_file[rel].add(i)

    unique = sorted({i for s in miss_by_file.values() for i in s})
    print("unique unresolved itemish ids", len(unique))
    if unique:
        print("sample", unique[:100])
        print("top files:")
        for f, s in sorted(miss_by_file.items(), key=lambda kv: -len(kv[1]))[:30]:
            print(len(s), f, sorted(list(s))[:8])

    # runtime hard
    recipes = json.loads(
        (ROOT / "src/main/resources/data/seeking_immortals/artifacts/refinement_recipes.json").read_text(
            encoding="utf-8"
        )
    )
    arts = {r["artifact_id"].split(":")[-1] for r in recipes["recipes"] if r.get("artifact_id")}
    print("ref arts missing", [a for a in sorted(arts) if not resolve(a)])
    mats = set()
    for r in recipes["recipes"]:
        for m in r.get("materials") or []:
            mid = (m.get("id") or "").split(":")[-1]
            if mid:
                mats.add(mid)
    print("ref mats missing", [m for m in sorted(mats) if not resolve(m)])

    # key catalogs
    key_sources = [
        "catalog/materials_index.json",
        "catalog/artifacts_index.json",
        "catalog/spirit_herbs_catalog_index.json",
        "catalog/talisman_catalog_index.json",
        "catalog/puppet_parts_catalog_index.json",
        "catalog/formation_items_catalog_index.json",
        "catalog/block_items_catalog_index.json",
        "catalog/currency_items_index.json",
        "catalog/manuals_catalog.json",
        "catalog/consumables_index.json",
        "catalog/alchemy_recipes_index.json",
        "text_material/pills_catalog.json",
        "text_material/consumables_catalog.json",
        "text_material/talisman_catalog.json",
        "text_material/talisman_recipes.json",
        "text_material/materials_catalog.json",
        "text_material/artifacts_catalog.json",
        "text_material/spirit_herbs_catalog.json",
        "text_material/puppet_parts_catalog.json",
        "text_material/formation_items_catalog.json",
        "text_material/alchemy_recipes.json",
    ]
    for rel in key_sources:
        p = data_root / rel
        if not p.exists():
            print("NOFILE", rel)
            continue
        d = json.loads(p.read_text(encoding="utf-8"))
        ids = extract_ids(d)
        if isinstance(d, dict):
            for k in (
                "entries",
                "materials",
                "artifacts",
                "pills",
                "consumables",
                "recipes",
                "manuals",
                "herbs",
                "talismans",
                "parts",
                "items",
            ):
                v = d.get(k)
                if isinstance(v, dict):
                    ids |= set(v.keys())
                elif isinstance(v, list):
                    for e in v:
                        if isinstance(e, dict) and e.get("id"):
                            ids.add(str(e["id"]).split(":")[-1])
        miss = sorted(
            i
            for i in ids
            if i not in NON_ITEM_EXACT
            and not any(i.startswith(pref) for pref in NON_ITEM_PREFIXES)
            and re.fullmatch(r"[a-z0-9_]+", i)
            and not resolve(i)
        )
        print(rel, "missing", len(miss), miss[:12] if miss else [])

    # compressed without own body
    comp = []
    for e in idmap["entries"]:
        if not str(e.get("status", "")).startswith("implemented"):
            continue
        sid = e.get("source_id")
        cid = e.get("canonical_id") or ""
        if not cid.startswith("seeking_immortals:") or not sid:
            continue
        last = cid.split(":")[-1]
        if sid != last and sid not in reg:
            # if resolve(sid) via map, it's intentional alias; still no own body
            comp.append((sid, cid, e.get("source_category")))
    print("compressed aliases (source!=own id)", len(comp))
    # of those, source has no physical item (same as not in reg/models - already)
    print("sample compressed", comp[:20])

    print("DONE")


if __name__ == "__main__":
    main()
