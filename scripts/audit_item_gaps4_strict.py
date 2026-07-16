#!/usr/bin/env python3
from __future__ import annotations

import json
import re
from pathlib import Path

root = Path(__file__).resolve().parents[1]
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
        if t.startswith("minecraft:"):
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


def extract_item_fields(obj, out: set[str]) -> None:
    if isinstance(obj, dict):
        for k, v in obj.items():
            if k in {
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
            } and isinstance(v, str):
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
                if isinstance(v, list):
                    for e in v:
                        if isinstance(e, dict):
                            for ik in ("id", "item", "item_id", "output", "result", "product"):
                                if ik in e and isinstance(e[ik], str):
                                    s = e[ik].split(":")[-1]
                                    if re.fullmatch(r"[a-z][a-z0-9_]{1,80}", s):
                                        out.add(s)
                            extract_item_fields(e, out)
                        elif isinstance(e, str) and re.fullmatch(r"[a-z][a-z0-9_]{1,80}", e):
                            out.add(e)
                elif isinstance(v, dict):
                    extract_item_fields(v, out)
            elif isinstance(v, (dict, list)):
                extract_item_fields(v, out)
    elif isinstance(obj, list):
        for e in obj:
            extract_item_fields(e, out)


def is_non_item(i: str) -> bool:
    if i.endswith(
        (
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
        )
    ):
        return True
    if i in {
        "beast_tribulation_bonus",
        "demon_qi_penalty",
        "five_elements_mountain_array",
        "nether_river_shield_zone",
    }:
        return True
    if i.startswith("recipe_") or i.startswith("refine_") or i.startswith("assemble_"):
        return True
    return False


paths: list[Path] = []
for base in [text, data]:
    if not base.exists():
        continue
    for pat in (
        "*catalog*.json",
        "currency_items.json",
        "merchant_shops.json",
        "alchemy_recipes.json",
        "novel_items_waves.json",
        "tribulation_items.json",
        "item_id_index.json",
        "moditems_artifacts_draft.json",
        "flight_vehicles.json",
        "puppet_definitions.json",
        "refinement_system.json",
        "refine_manual_index.json",
    ):
        paths.extend(base.rglob(pat))
paths = sorted(set(paths))

all_miss: list[str] = []
print("=== catalog hard missing ===")
for p in paths:
    try:
        obj = json.loads(p.read_text(encoding="utf-8"))
    except Exception:
        continue
    ids: set[str] = set()
    extract_item_fields(obj, ids)
    miss2 = sorted(i for i in ids if not covered(i) and not is_non_item(i))
    if miss2:
        print(p.name, "missing", len(miss2))
        for i in miss2[:40]:
            print("  ", i)
        all_miss.extend(miss2)
print("TOTAL hard missing unique", len(set(all_miss)))
print(sorted(set(all_miss)))

loot_miss = []
if (data / "loot_tables").exists():
    for p in (data / "loot_tables").rglob("*.json"):
        t = p.read_text(encoding="utf-8", errors="ignore")
        for m in re.findall(r"seeking_immortals:([a-z0-9_]+)", t):
            if not covered(m):
                loot_miss.append(m)
print("loot missing unique", sorted(set(loot_miss)))

ftb_miss = []
for p in root.rglob("*.snbt"):
    sp = str(p).lower()
    if "ftb" not in sp and "quest" not in sp:
        continue
    t = p.read_text(encoding="utf-8", errors="ignore")
    for m in re.findall(r"seeking_immortals:([a-z0-9_]+)", t):
        if not covered(m):
            ftb_miss.append(m)
print("ftb missing unique", sorted(set(ftb_miss)))

print("talisman related files:")
for p in list(text.rglob("*talisman*")) + list(data.rglob("*talisman*")):
    try:
        print(" ", p.relative_to(root))
    except Exception:
        print(" ", p)

print("id_map non-implemented item-like:")
for e in idmap["entries"]:
    st = str(e.get("status") or "")
    ct = str(e.get("canonical_type") or "")
    if ct in ("item", "future_item") and not st.startswith("implemented"):
        print(e.get("source_id"), st, ct, e.get("canonical_id"))

# shipped shops: only item fields
shop_miss = []
if (data / "shops").exists():
    for p in (data / "shops").rglob("*.json"):
        t = p.read_text(encoding="utf-8", errors="ignore")
        for m in re.findall(r'"item"\s*:\s*"seeking_immortals:([a-z0-9_]+)"', t):
            if not covered(m):
                shop_miss.append(m)
print("shipped shop item field missing", sorted(set(shop_miss)))

# alchemy recipe outputs
alc_miss = []
if (data / "alchemy").exists():
    for p in (data / "alchemy").rglob("*.json"):
        t = p.read_text(encoding="utf-8", errors="ignore")
        for m in re.findall(r"seeking_immortals:([a-z0-9_]+)", t):
            if not covered(m):
                alc_miss.append(m)
print("alchemy missing", sorted(set(alc_miss)))

# vanilla recipes
rec_miss = []
if (data / "recipes").exists():
    for p in (data / "recipes").rglob("*.json"):
        t = p.read_text(encoding="utf-8", errors="ignore")
        for m in re.findall(r"seeking_immortals:([a-z0-9_]+)", t):
            if not covered(m):
                rec_miss.append(m)
print("recipes missing", sorted(set(rec_miss)))

print("registered", len(registered), "bulk", len(bulk["items"]))
gp = (root / "gradle.properties").read_text(encoding="utf-8", errors="ignore")
print([ln for ln in gp.splitlines() if "mod_version" in ln][0])
