#!/usr/bin/env python3
"""Final item-carrier completeness audit for remaining text/catalog sources."""
from __future__ import annotations

import json
import re
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
    bulk = json.loads(
        (ROOT / "src/main/resources/assets/seeking_immortals/catalog_bulk_items.json").read_text(encoding="utf-8")
    )
    for it in bulk.get("items", []):
        reg.add(it["id"])
    reg |= {p.stem for p in (ROOT / "src/main/resources/assets/seeking_immortals/models/item").glob("*.json")}
    mb = ROOT / "src/main/java/com/xunxian/seekingimmortals/registry/ModBlocks.java"
    if mb.exists():
        reg |= set(re.findall(r'register\(\s*"([a-z0-9_]+)"', mb.read_text(encoding="utf-8")))
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

    return resolve


def extract(obj) -> set[str]:
    keys = {
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
    }
    found: set[str] = set()

    def walk(o):
        if isinstance(o, dict):
            for k, v in o.items():
                if k in keys and isinstance(v, str):
                    s = v.split(":")[-1]
                    if re.fullmatch(r"[a-z0-9_]+", s) and len(s) > 2:
                        found.add(s)
                elif k in (
                    "entries",
                    "materials",
                    "artifacts",
                    "pills",
                    "consumables",
                    "items",
                    "herbs",
                    "talismans",
                    "parts",
                    "rewards",
                    "drops",
                    "loot",
                    "waves",
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


SKIP = {
    "schema_version",
    "note",
    "type",
    "display",
    "item",
    "entries",
    "materials",
    "recipes",
    "count",
    "tier",
    "low",
    "mid",
    "high",
    "name",
    "category",
}


def main() -> None:
    reg = load_reg()
    resolve = load_resolve(reg)
    print(
        "version",
        [l for l in Path("gradle.properties").read_text(encoding="utf-8").splitlines() if "mod_version" in l][0],
    )
    print("reg", len(reg))

    files = [
        "src/main/resources/data/seeking_immortals/text_material/tribulation_items.json",
        "src/main/resources/data/seeking_immortals/text_material/block_items_catalog.json",
        "src/main/resources/data/seeking_immortals/text_material/currency_items.json",
        "src/main/resources/data/seeking_immortals/text_material/novel_items_waves.json",
        "src/main/resources/data/seeking_immortals/text_material/novel_items_master_index.json",
        "src/main/resources/data/seeking_immortals/text_material/items_by_region.json",
        "src/main/resources/data/seeking_immortals/text_material/item_id_index.json",
        "src/main/resources/data/seeking_immortals/text_material/item_id_aliases.json",
        "src/main/resources/data/seeking_immortals/text_material/moditems_artifacts_draft.json",
        "src/main/resources/data/seeking_immortals/text_material/merchant_shops.json",
        "src/main/resources/data/seeking_immortals/catalog/currency_items_index.json",
        "src/main/resources/data/seeking_immortals/catalog/block_items_catalog_index.json",
        "src/main/resources/data/seeking_immortals/catalog/talisman_materials_index.json",
        "src/main/resources/data/seeking_immortals/catalog/talisman_treasure_templates_index.json",
        "src/main/resources/data/seeking_immortals/artifacts/talisman_treasure_templates.json",
        "src/main/resources/data/seeking_immortals/catalog/spirit_herbs_catalog_index.json",
        "src/main/resources/data/seeking_immortals/catalog/talisman_catalog_index.json",
        "src/main/resources/data/seeking_immortals/catalog/puppet_parts_catalog_index.json",
        "src/main/resources/data/seeking_immortals/catalog/formation_items_catalog_index.json",
        "src/main/resources/data/seeking_immortals/catalog/materials_index.json",
        "src/main/resources/data/seeking_immortals/catalog/artifacts_index.json",
        "src/main/resources/data/seeking_immortals/catalog/manuals_catalog.json",
        "src/main/resources/data/seeking_immortals/text_material/pills_catalog.json",
        "src/main/resources/data/seeking_immortals/text_material/consumables_catalog.json",
        "src/main/resources/data/seeking_immortals/text_material/talisman_catalog.json",
        "src/main/resources/data/seeking_immortals/text_material/talisman_recipes.json",
        "src/main/resources/data/seeking_immortals/text_material/materials_catalog.json",
        "src/main/resources/data/seeking_immortals/text_material/artifacts_catalog.json",
        "src/main/resources/data/seeking_immortals/text_material/spirit_herbs_catalog.json",
        "src/main/resources/data/seeking_immortals/text_material/puppet_parts_catalog.json",
        "src/main/resources/data/seeking_immortals/text_material/formation_items_catalog.json",
        "src/main/resources/data/seeking_immortals/text_material/alchemy_recipes.json",
    ]

    total_miss = set()
    for rel in files:
        p = Path(rel)
        if not p.exists():
            print("NOFILE", rel)
            continue
        d = json.loads(p.read_text(encoding="utf-8"))
        ids = extract(d)
        ids |= set(re.findall(r"seeking_immortals:([a-z0-9_]+)", json.dumps(d)))
        miss = []
        for i in sorted(ids):
            if i in SKIP:
                continue
            if i.startswith("craft_") or i.startswith("refine_") or i.startswith("recipe_"):
                continue
            # shop ids often end with market/hall etc but item ids rarely
            if not re.fullmatch(r"[a-z0-9_]+", i):
                continue
            if not resolve(i):
                miss.append(i)
        print(p.name, "miss", len(miss), miss[:20] if miss else [])
        total_miss |= set(miss)

    # runtime
    shop_miss = set()
    shops = ROOT / "src/main/resources/data/seeking_immortals/shops"
    if shops.exists():
        for p in shops.rglob("*.json"):
            d = json.loads(p.read_text(encoding="utf-8"))
            for m in re.findall(r'"item"\s*:\s*"([^"]+)"', json.dumps(d)):
                iid = m.split(":")[-1]
                if iid not in ("item", "sect_contribution") and not resolve(iid):
                    shop_miss.add(iid)
    loot_miss = set()
    lt = ROOT / "src/main/resources/data/seeking_immortals/loot_tables"
    if lt.exists():
        for p in lt.rglob("*.json"):
            d = json.loads(p.read_text(encoding="utf-8"))
            for m in re.findall(r"seeking_immortals:([a-z0-9_]+)", json.dumps(d)):
                if not resolve(m):
                    loot_miss.add(m)
    rec_miss = set()
    for p in (ROOT / "src/main/resources/data/seeking_immortals/recipes").rglob("*.json"):
        d = json.loads(p.read_text(encoding="utf-8"))
        for m in re.findall(r"seeking_immortals:([a-z0-9_]+)", json.dumps(d)):
            if m in ("crafting_shaped", "crafting_shapeless", "refinement"):
                continue
            if not resolve(m):
                rec_miss.add(m)
    alch_miss = set()
    for p in (ROOT / "src/main/resources/data/seeking_immortals").rglob("*.json"):
        path = str(p).replace("\\", "/")
        if "/alchemy/recipes/" not in path:
            continue
        d = json.loads(p.read_text(encoding="utf-8"))
        for m in re.findall(r"seeking_immortals:([a-z0-9_]+)", json.dumps(d)):
            if not resolve(m):
                alch_miss.add(m)

    recipes = json.loads(
        (ROOT / "src/main/resources/data/seeking_immortals/artifacts/refinement_recipes.json").read_text(
            encoding="utf-8"
        )
    )
    arts = {r["artifact_id"].split(":")[-1] for r in recipes["recipes"] if r.get("artifact_id")}
    ref_art_miss = [a for a in sorted(arts) if not resolve(a)]
    mats = set()
    for r in recipes["recipes"]:
        for m in r.get("materials") or []:
            mid = (m.get("id") or "").split(":")[-1]
            if mid:
                mats.add(mid)
    ref_mat_miss = [m for m in sorted(mats) if not resolve(m)]

    print("TOTAL_CATALOG_TEXT_MISS", len(total_miss), sorted(total_miss)[:50])
    print("shops", sorted(shop_miss))
    print("loot", sorted(loot_miss))
    print("recipes", sorted(rec_miss))
    print("alchemy datapack", sorted(alch_miss))
    print("ref arts", ref_art_miss)
    print("ref mats", ref_mat_miss)

    clear = not any([total_miss, shop_miss, loot_miss, rec_miss, alch_miss, ref_art_miss, ref_mat_miss])
    print("ALL_CLEAR" if clear else "HAS_GAPS")


if __name__ == "__main__":
    main()
