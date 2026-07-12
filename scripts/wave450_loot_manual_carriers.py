#!/usr/bin/env python3
"""0.1.450: add remaining hard-missing loot/manual carriers."""
from __future__ import annotations

import hashlib
import json
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(".")
ASSETS = ROOT / "src/main/resources/assets/seeking_immortals"
BULK = ASSETS / "catalog_bulk_items.json"
ITEM_MODELS = ASSETS / "models/item"
ITEM_TEX = ASSETS / "textures/item"
ZH = ASSETS / "lang/zh_cn.json"
EN = ASSETS / "lang/en_us.json"
IDMAP = ROOT / "src/main/resources/data/seeking_immortals/reference/text_material_id_map.json"

NEW_ITEMS = [
    {
        "id": "ancient_artifact_fragment",
        "category": "material",
        "rarity": "epic",
        "display": "上古法宝碎片",
        "source_files": ["loot_tables.json", "merchant_shops.json", "region_cards/great_jin_central.json"],
        "source_category": "material",
    },
    {
        "id": "dayan_fragment",
        "category": "material",
        "rarity": "epic",
        "display": "大衍残片",
        "source_files": ["boss_loot_tables.json", "secret_realms.json", "quest_hooks.json"],
        "source_category": "material",
    },
    {
        "id": "manual_ancient_puppet_method",
        "category": "manual",
        "rarity": "epic",
        "display": "上古傀儡术手册",
        "source_files": ["boss_loot_tables.json", "puppet_craft_recipes.json"],
        "source_category": "manual",
    },
]


def make_icon(name: str, cat: str, size: int = 64) -> Image.Image:
    h = hashlib.md5(name.encode()).hexdigest()
    base = (80 + int(h[0:2], 16) % 160, 80 + int(h[2:4], 16) % 160, 80 + int(h[4:6], 16) % 160, 255)
    im = Image.new("RGBA", (size, size), (base[0] // 2, base[1] // 2, base[2] // 2, 255))
    d = ImageDraw.Draw(im)
    d.rectangle([1, 1, size - 2, size - 2], outline=(255, 240, 200, 220), width=2)
    if cat == "manual":
        d.rectangle(
            [size * 0.25, size * 0.15, size * 0.75, size * 0.85],
            fill=(240, 230, 200, 255),
            outline=(90, 50, 20, 255),
        )
    else:
        d.polygon(
            [
                (size * 0.5, size * 0.18),
                (size * 0.78, size * 0.38),
                (size * 0.70, size * 0.78),
                (size * 0.30, size * 0.78),
                (size * 0.22, size * 0.38),
            ],
            fill=base,
            outline=(255, 255, 255, 200),
        )
    try:
        font = ImageFont.load_default()
    except Exception:
        font = None
    label = "".join(ch for ch in name if ch.isalnum())[:2].upper() or "SI"
    d.text((size * 0.36, size * 0.42), label[:2], fill=(255, 255, 255, 230), font=font)
    return im


def title_en(rid: str) -> str:
    return " ".join(w.capitalize() for w in rid.split("_"))


def main() -> None:
    bulk = json.loads(BULK.read_text(encoding="utf-8"))
    existing = {it["id"] for it in bulk.get("items", [])}
    zh = json.loads(ZH.read_text(encoding="utf-8"))
    en = json.loads(EN.read_text(encoding="utf-8"))
    idmap = json.loads(IDMAP.read_text(encoding="utf-8"))
    by = {e.get("source_id"): e for e in idmap["entries"] if isinstance(e, dict)}

    added = []
    for it in NEW_ITEMS:
        rid = it["id"]
        if rid not in existing:
            bulk["items"].append(
                {
                    "id": rid,
                    "category": it["category"],
                    "rarity": it["rarity"],
                    "description": f"Catalog carrier for {rid}",
                }
            )
            existing.add(rid)
            added.append(rid)
        png = ITEM_TEX / f"{rid}.png"
        if not png.exists():
            png.parent.mkdir(parents=True, exist_ok=True)
            make_icon(rid, it["category"]).save(png, format="PNG")
        (ITEM_MODELS / f"{rid}.json").write_text(
            json.dumps(
                {
                    "parent": "minecraft:item/generated",
                    "textures": {"layer0": f"seeking_immortals:item/{rid}"},
                },
                indent=2,
            )
            + "\n",
            encoding="utf-8",
        )
        zh[f"item.seeking_immortals.{rid}"] = it["display"]
        en[f"item.seeking_immortals.{rid}"] = title_en(rid)
        if rid in by:
            e = by[rid]
            e["canonical_type"] = "item"
            e["canonical_id"] = f"seeking_immortals:{rid}"
            e["status"] = "implemented"
            e["note"] = "Wave 0.1.450 hard-missing loot/manual carrier."
        else:
            idmap["entries"].append(
                {
                    "source_category": it["source_category"],
                    "source_id": rid,
                    "source_files": it["source_files"],
                    "canonical_type": "item",
                    "canonical_id": f"seeking_immortals:{rid}",
                    "status": "implemented",
                    "note": "Wave 0.1.450 hard-missing loot/manual carrier.",
                }
            )

    BULK.write_text(json.dumps(bulk, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    ZH.write_text(json.dumps(zh, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    EN.write_text(json.dumps(en, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    IDMAP.write_text(json.dumps(idmap, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    gp = Path("gradle.properties")
    raw = gp.read_text(encoding="utf-8")
    if "mod_version=0.1.449" in raw:
        gp.write_text(raw.replace("mod_version=0.1.449", "mod_version=0.1.450"), encoding="utf-8")
    elif "mod_version=0.1.450" not in raw:
        raise SystemExit("unexpected mod_version")
    print("added", added)
    print("bulk total", len(bulk["items"]))
    print("version", [ln for ln in gp.read_text(encoding="utf-8").splitlines() if "mod_version" in ln][0])


if __name__ == "__main__":
    main()
