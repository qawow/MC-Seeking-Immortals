#!/usr/bin/env python3
"""0.1.446: register remaining catalog carriers (herbs/talismans/puppet/formation/etc)."""
from __future__ import annotations

import hashlib
import json
import re
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(".")
ASSETS = ROOT / "src/main/resources/assets/seeking_immortals"
ITEM_MODELS = ASSETS / "models/item"
ITEM_TEX = ASSETS / "textures/item"
ZH = ASSETS / "lang/zh_cn.json"
EN = ASSETS / "lang/en_us.json"
IDMAP = ROOT / "src/main/resources/data/seeking_immortals/reference/text_material_id_map.json"
BULK_JSON = ASSETS / "catalog_bulk_items.json"
ITEMS_JAVA = ROOT / "src/main/java/com/xunxian/seekingimmortals/registry/ModItems.java"

EXCLUDE = {
    "beast_tribulation_bonus",
    "demon_qi_penalty",
    "nether_river_shield_zone",
    "five_elements_mountain_array",
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
}

# Prefer alias over new item
ALIASES = {
    "earth_fire_alchemy_room": "sect_earth_fire_room",
    "refinement_forge_g1": "refinement_forge",
    "yin_essence_ore_block": "yin_essence_ore",
    "alchemy_furnace_g1": "alchemy_furnace",
}

SOURCE_FILES = [
    "src/main/resources/data/seeking_immortals/catalog/spirit_herbs_catalog_index.json",
    "src/main/resources/data/seeking_immortals/text_material/spirit_herbs_catalog.json",
    "src/main/resources/data/seeking_immortals/catalog/talisman_catalog_index.json",
    "src/main/resources/data/seeking_immortals/text_material/talisman_catalog.json",
    "src/main/resources/data/seeking_immortals/catalog/talisman_materials_index.json",
    "src/main/resources/data/seeking_immortals/catalog/puppet_parts_catalog_index.json",
    "src/main/resources/data/seeking_immortals/text_material/puppet_parts_catalog.json",
    "src/main/resources/data/seeking_immortals/catalog/formation_items_catalog_index.json",
    "src/main/resources/data/seeking_immortals/text_material/formation_items_catalog.json",
    "src/main/resources/data/seeking_immortals/catalog/block_items_catalog_index.json",
    "src/main/resources/data/seeking_immortals/catalog/currency_items_index.json",
    "src/main/resources/data/seeking_immortals/text_material/alchemy_recipes.json",
]

EXTRA_FORCE = [
    "anti_demon_herb",
    "frost_moon_flower",
    "purple_ganoderma",
    "snow_lotus",
    "wind_herb",
    "spirit_herb_common",
    "sect_contribution_token",
    "yin_talisman_paper",
    "talisman_paper_spirit_realm",
    "thunder_ward_sign",
    "tianyuan_guard_talisman",
    "kunwu_copper_ore",
]


def ensure(p: Path) -> None:
    p.parent.mkdir(parents=True, exist_ok=True)


def load_reg() -> set[str]:
    mi = ITEMS_JAVA.read_text(encoding="utf-8")
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
    if BULK_JSON.exists():
        bulk = json.loads(BULK_JSON.read_text(encoding="utf-8"))
        for it in bulk.get("items", []):
            reg.add(it["id"])
    reg |= {p.stem for p in ITEM_MODELS.glob("*.json")}
    return reg


def extract_ids(obj) -> set[str]:
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
    }
    found: set[str] = set()

    def walk(o):
        if isinstance(o, dict):
            for k, v in o.items():
                if k in keys and isinstance(v, str):
                    s = v.split(":")[-1]
                    if re.fullmatch(r"[a-z0-9_]+", s) and len(s) > 2:
                        found.add(s)
                else:
                    walk(v)
        elif isinstance(o, list):
            for x in o:
                walk(x)

    walk(obj)
    return found


def collect_displays() -> dict[str, str]:
    displays: dict[str, str] = {}
    for p in (ROOT / "src/main/resources/data/seeking_immortals").rglob("*.json"):
        try:
            d = json.loads(p.read_text(encoding="utf-8"))
        except Exception:
            continue
        if isinstance(d, dict):
            for k, v in d.items():
                if isinstance(v, dict) and re.fullmatch(r"[a-z0-9_]+", k):
                    disp = v.get("display") or v.get("name")
                    if isinstance(disp, str):
                        displays[k] = disp
            for key in (
                "entries",
                "materials",
                "artifacts",
                "pills",
                "consumables",
                "recipes",
                "manuals",
                "items",
                "herbs",
                "talismans",
                "parts",
            ):
                v = d.get(key)
                if isinstance(v, dict):
                    for kk, vv in v.items():
                        if isinstance(vv, dict):
                            disp = vv.get("display") or vv.get("name")
                            if isinstance(disp, str):
                                displays[kk] = disp
                elif isinstance(v, list):
                    for e in v:
                        if isinstance(e, dict) and e.get("id") and e.get("display"):
                            displays[str(e["id"]).split(":")[-1]] = e["display"]
    return displays


def category_for(rid: str, source_hint: str = "") -> str:
    s = rid + " " + source_hint
    if "talisman" in s or rid.endswith("_talisman"):
        return "talisman"
    if "puppet" in s or "blueprint" in s or rid.endswith("_plate") or "limb" in rid or "joint" in rid:
        return "artifact"
    if "disk" in rid or "flag" in rid or "array" in rid or "formation" in s:
        return "artifact"
    if "herb" in s or "lotus" in rid or "grass" in rid or "flower" in rid or "moss" in rid or "ginseng" in rid:
        return "material"
    if "paper" in rid or "token" in rid or "permit" in rid:
        return "consumable"
    if "ore" in rid or "furnace" in rid or "forge" in rid:
        return "material"
    return "material"


def rarity_for(cat: str, rid: str) -> str:
    if any(x in rid for x in ("ancient", "immortal", "thousand_year", "ten_thousand", "void_palace")):
        return "epic"
    if cat == "talisman" or cat == "artifact":
        return "uncommon"
    return "common"


def make_icon(name: str, cat: str, size: int = 64) -> Image.Image:
    h = hashlib.md5(name.encode()).hexdigest()
    base = (80 + int(h[0:2], 16) % 160, 80 + int(h[2:4], 16) % 160, 80 + int(h[4:6], 16) % 160, 255)
    im = Image.new("RGBA", (size, size), (base[0] // 2, base[1] // 2, base[2] // 2, 255))
    d = ImageDraw.Draw(im)
    d.rectangle([1, 1, size - 2, size - 2], outline=(255, 240, 200, 220), width=2)
    if cat == "talisman":
        d.rectangle([size * 0.3, size * 0.15, size * 0.7, size * 0.85], fill=(250, 240, 210, 255), outline=(120, 40, 40, 255))
    elif cat == "artifact":
        d.polygon(
            [(size * 0.5, size * 0.12), (size * 0.78, size * 0.5), (size * 0.5, size * 0.88), (size * 0.22, size * 0.5)],
            fill=(220, 210, 120, 255),
            outline=(80, 60, 10, 255),
        )
    else:
        d.ellipse([size * 0.22, size * 0.22, size * 0.78, size * 0.78], fill=base, outline=(80, 180, 80, 220))
    try:
        font = ImageFont.load_default()
    except Exception:
        font = None
    label = "".join(ch for ch in name if ch.isalnum())[:2].upper() or "SI"
    d.text((size * 0.36, size * 0.4), label[:2], fill=(255, 255, 255, 230), font=font)
    return im


def title_en(rid: str) -> str:
    return " ".join(w.capitalize() for w in rid.split("_"))


def main() -> None:
    reg = load_reg()
    displays = collect_displays()
    candidates: set[str] = set(EXTRA_FORCE)
    for rel in SOURCE_FILES:
        p = ROOT / rel
        if not p.exists():
            print("skip missing", rel)
            continue
        try:
            d = json.loads(p.read_text(encoding="utf-8"))
        except Exception as e:
            print("bad json", rel, e)
            continue
        ids = extract_ids(d)
        if isinstance(d, dict):
            for key in ("entries", "herbs", "items", "talismans", "parts", "materials", "recipes"):
                v = d.get(key)
                if isinstance(v, dict):
                    ids |= set(v.keys())
                elif isinstance(v, list):
                    for e in v:
                        if isinstance(e, dict) and e.get("id"):
                            ids.add(str(e["id"]).split(":")[-1])
        print(rel, "ids", len(ids))
        candidates |= ids

    # filter
    miss = []
    for rid in sorted(candidates):
        if rid in EXCLUDE or rid in ALIASES:
            continue
        if rid.startswith("craft_") or rid.startswith("refine_") or rid.startswith("recipe_"):
            # keep recipe_ only if looks like item from force list
            if rid not in EXTRA_FORCE:
                continue
        if not re.fullmatch(r"[a-z0-9_]+", rid) or len(rid) < 3:
            continue
        if rid in reg:
            continue
        miss.append(rid)
    print("remaining miss to bulk", len(miss))

    # load bulk and append
    bulk = json.loads(BULK_JSON.read_text(encoding="utf-8"))
    existing_bulk = {it["id"] for it in bulk.get("items", [])}
    new_items = []
    for rid in miss:
        if rid in existing_bulk:
            continue
        # guess category from filename membership is hard; use name
        cat = category_for(rid)
        if "talisman" in rid:
            cat = "talisman"
        elif any(x in rid for x in ("disk", "flag", "array_core", "stake", "palace_disk")):
            cat = "artifact"
        elif any(x in rid for x in ("puppet", "blueprint", "plating", "limb", "joint", "hammer", "shell")):
            cat = "artifact"
        elif rid.endswith("_token") or "paper" in rid:
            cat = "consumable"
        elif any(x in rid for x in ("herb", "lotus", "grass", "flower", "moss", "ginseng", "fruit", "root", "vine", "orchid", "lichen", "kelp", "rice", "bamboo", "wood", "sand")):
            cat = "material"
        item = {
            "id": rid,
            "category": cat,
            "rarity": rarity_for(cat, rid),
            "description": f"Catalog carrier for {rid}",
        }
        bulk["items"].append(item)
        new_items.append(item)
    BULK_JSON.write_text(json.dumps(bulk, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("appended bulk", len(new_items), "total bulk", len(bulk["items"]))

    # assets + lang
    zh = json.loads(ZH.read_text(encoding="utf-8"))
    en = json.loads(EN.read_text(encoding="utf-8"))
    tex_n = 0
    for it in new_items:
        rid = it["id"]
        png = ITEM_TEX / f"{rid}.png"
        if not png.exists():
            ensure(png)
            make_icon(rid, it["category"]).save(png, format="PNG")
            tex_n += 1
        (ITEM_MODELS / f"{rid}.json").write_text(
            json.dumps(
                {"parent": "minecraft:item/generated", "textures": {"layer0": f"seeking_immortals:item/{rid}"}},
                indent=2,
            )
            + "\n",
            encoding="utf-8",
        )
        disp = displays.get(rid) or rid
        zh[f"item.seeking_immortals.{rid}"] = disp
        en[f"item.seeking_immortals.{rid}"] = title_en(rid)
    ZH.write_text(json.dumps(zh, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    EN.write_text(json.dumps(en, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("tex created", tex_n)

    # id-map: new items + aliases
    idmap = json.loads(IDMAP.read_text(encoding="utf-8"))
    by = {e.get("source_id"): e for e in idmap["entries"]}
    for it in new_items:
        rid = it["id"]
        cat = it["category"]
        source_cat = {
            "talisman": "talisman",
            "artifact": "artifact",
            "material": "material",
            "consumable": "consumable",
            "pill": "pill",
        }.get(cat, "material")
        if rid in by:
            e = by[rid]
            e["canonical_type"] = "item"
            e["canonical_id"] = f"seeking_immortals:{rid}"
            e["status"] = "implemented"
            e["note"] = "Wave 0.1.446 remaining catalog carrier."
        else:
            idmap["entries"].append(
                {
                    "source_category": source_cat,
                    "source_id": rid,
                    "source_files": ["remaining_catalog"],
                    "canonical_type": "item",
                    "canonical_id": f"seeking_immortals:{rid}",
                    "status": "implemented",
                    "note": "Wave 0.1.446 remaining catalog carrier.",
                }
            )
    for src, dst in ALIASES.items():
        if src in by:
            e = by[src]
            e["canonical_type"] = "item"
            e["canonical_id"] = f"seeking_immortals:{dst}"
            e["status"] = "implemented"
            e["note"] = f"Wave 0.1.446 alias to existing {dst}."
        else:
            idmap["entries"].append(
                {
                    "source_category": "equipment",
                    "source_id": src,
                    "source_files": ["block_items_catalog"],
                    "canonical_type": "item",
                    "canonical_id": f"seeking_immortals:{dst}",
                    "status": "implemented",
                    "note": f"Wave 0.1.446 alias to existing {dst}.",
                }
            )
    IDMAP.write_text(json.dumps(idmap, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("id-map updated aliases", len(ALIASES))

    # version
    gp = Path("gradle.properties")
    t = gp.read_text(encoding="utf-8")
    t2 = t.replace("mod_version=0.1.445", "mod_version=0.1.446")
    if t2 == t and "mod_version=0.1.446" not in t:
        raise SystemExit("version bump failed")
    if t2 != t:
        gp.write_text(t2, encoding="utf-8")
    print("version", [l for l in gp.read_text(encoding="utf-8").splitlines() if "mod_version" in l][0])

    # verify remaining for source files
    reg2 = load_reg()
    # reload bulk after write
    bulk = json.loads(BULK_JSON.read_text(encoding="utf-8"))
    for it in bulk["items"]:
        reg2.add(it["id"])
    reg2 |= {p.stem for p in ITEM_MODELS.glob("*.json")}

    def has(i: str) -> bool:
        i = i.split(":")[-1]
        if i in reg2:
            return True
        # aliases
        if i in ALIASES and ALIASES[i] in reg2:
            return True
        # idmap resolve
        return False

    # idmap resolve
    idmap = json.loads(IDMAP.read_text(encoding="utf-8"))
    source_to_canon = {}
    for e in idmap["entries"]:
        if str(e.get("status", "")).startswith("implemented") and e.get("canonical_type") == "item":
            cid = e.get("canonical_id") or ""
            if cid.startswith("seeking_immortals:"):
                source_to_canon[e["source_id"]] = cid.split(":")[-1]

    def resolve(i: str) -> bool:
        i = i.split(":")[-1]
        if i in reg2:
            return True
        c = source_to_canon.get(i)
        return bool(c and c in reg2)

    left = []
    for rid in miss:
        if not resolve(rid):
            left.append(rid)
    print("left unresolved after wave", left)
    print("DONE new", len(new_items))


if __name__ == "__main__":
    main()
