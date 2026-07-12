#!/usr/bin/env python3
"""0.1.447: add remaining novel talismans + merchant stock item carriers."""
from __future__ import annotations

import hashlib
import json
import re
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
ITEMS_JAVA = ROOT / "src/main/java/com/xunxian/seekingimmortals/registry/ModItems.java"

EXCLUDE_EFFECTS = {
    "beast_tribulation_bonus",
    "demon_qi_penalty",
    "five_elements_mountain_array",
    "nether_river_shield_zone",
}

SHOP_ID_SUFFIXES = (
    "_market",
    "_hall",
    "_pavilion",
    "_stall",
    "_bank",
    "_auction",
    "_exchange",
    "_vendor",
    "_supply",
)


def ensure(path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)


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
    bulk = json.loads(BULK.read_text(encoding="utf-8"))
    for it in bulk.get("items", []):
        reg.add(it["id"])
    reg |= {p.stem for p in ITEM_MODELS.glob("*.json")}
    return reg


def load_resolve(reg: set[str]):
    idmap = json.loads(IDMAP.read_text(encoding="utf-8"))
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

    return resolve, idmap


def make_icon(name: str, cat: str, size: int = 64) -> Image.Image:
    h = hashlib.md5(name.encode()).hexdigest()
    base = (80 + int(h[0:2], 16) % 160, 80 + int(h[2:4], 16) % 160, 80 + int(h[4:6], 16) % 160, 255)
    im = Image.new("RGBA", (size, size), (base[0] // 2, base[1] // 2, base[2] // 2, 255))
    d = ImageDraw.Draw(im)
    d.rectangle([1, 1, size - 2, size - 2], outline=(255, 240, 200, 220), width=2)
    if cat == "talisman":
        d.rectangle(
            [size * 0.3, size * 0.15, size * 0.7, size * 0.85],
            fill=(250, 240, 210, 255),
            outline=(120, 40, 40, 255),
        )
    elif cat == "pill":
        d.ellipse([size * 0.22, size * 0.22, size * 0.78, size * 0.78], fill=base, outline=(255, 255, 255, 200))
    else:
        d.rounded_rectangle(
            [size * 0.2, size * 0.2, size * 0.8, size * 0.8],
            radius=8,
            fill=base,
            outline=(255, 255, 255, 180),
        )
    try:
        font = ImageFont.load_default()
    except Exception:
        font = None
    label = "".join(ch for ch in name if ch.isalnum())[:2].upper() or "SI"
    d.text((size * 0.36, size * 0.4), label[:2], fill=(255, 255, 255, 230), font=font)
    return im


def title_en(rid: str) -> str:
    return " ".join(w.capitalize() for w in rid.split("_"))


def is_shop_id(rid: str) -> bool:
    return any(rid.endswith(sfx) for sfx in SHOP_ID_SUFFIXES)


def collect_new(resolve) -> list[dict]:
    new: list[dict] = []
    displays: dict[str, str] = {}
    seen: set[str] = set()

    def add(rid: str, category: str, rarity: str, display: str = "") -> None:
        if not rid or rid in seen or resolve(rid) or rid in EXCLUDE_EFFECTS:
            return
        if is_shop_id(rid):
            return
        seen.add(rid)
        if display:
            displays[rid] = display
        new.append(
            {
                "id": rid,
                "category": category,
                "rarity": rarity,
                "description": f"Catalog carrier for {rid}",
                "display": display or rid,
            }
        )

    # novel talismans
    novel = json.loads(
        (ROOT / "src/main/resources/data/seeking_immortals/text_material/novel_items_waves.json").read_text(
            encoding="utf-8"
        )
    )
    for e in novel.get("talismans") or []:
        if isinstance(e, dict) and e.get("id"):
            add(e["id"], "talisman", "uncommon", e.get("display") or "")

    # merchant stock item fields
    ms = json.loads(
        (ROOT / "src/main/resources/data/seeking_immortals/text_material/merchant_shops.json").read_text(
            encoding="utf-8"
        )
    )
    stock: set[str] = set()
    shop_ids: set[str] = set()

    def walk(o, in_goods: bool = False):
        if isinstance(o, dict):
            if "id" in o and isinstance(o["id"], str) and any(
                k in o for k in ("goods", "stock", "entries", "items", "inventory")
            ):
                shop_ids.add(o["id"])
            for k, v in o.items():
                if k in ("item", "item_id", "itemId") and isinstance(v, str):
                    stock.add(v.split(":")[-1])
                if in_goods and k == "id" and isinstance(v, str):
                    stock.add(v.split(":")[-1])
                walk(v, in_goods or k in ("goods", "stock", "entries", "inventory", "items"))
        elif isinstance(o, list):
            for x in o:
                walk(x, in_goods)

    walk(ms)
    for rid in sorted(stock):
        if rid in shop_ids:
            continue
        add(rid, "material", "uncommon")

    # tribulation items (exclude effects)
    tri = json.loads(
        (ROOT / "src/main/resources/data/seeking_immortals/text_material/tribulation_items.json").read_text(
            encoding="utf-8"
        )
    )
    for e in tri.get("items") or []:
        if not isinstance(e, dict) or not e.get("id"):
            continue
        rid = e["id"]
        if rid in EXCLUDE_EFFECTS:
            continue
        if "talisman" in rid or "sign" in rid or "charm" in rid:
            cat, rar = "talisman", "rare"
        elif "pill" in rid:
            cat, rar = "pill", "rare"
        else:
            cat, rar = "consumable", "rare"
        add(rid, cat, rar, e.get("display") or "")
    for e in tri.get("tier_ladder") or []:
        for rid in e.get("items") or []:
            if not isinstance(rid, str) or rid in EXCLUDE_EFFECTS:
                continue
            if "talisman" in rid or "sign" in rid or "charm" in rid:
                cat = "talisman"
            elif "pill" in rid:
                cat = "pill"
            else:
                cat = "consumable"
            add(rid, cat, "rare")

    return new, displays


def main() -> None:
    reg = load_reg()
    resolve, idmap = load_resolve(reg)
    new, displays = collect_new(resolve)
    print("to add", len(new), [n["id"] for n in new])

    bulk = json.loads(BULK.read_text(encoding="utf-8"))
    existing = {it["id"] for it in bulk.get("items", [])}
    added = 0
    for it in new:
        if it["id"] in existing:
            continue
        bulk["items"].append(
            {
                "id": it["id"],
                "category": it["category"],
                "rarity": it["rarity"],
                "description": it["description"],
            }
        )
        added += 1
    BULK.write_text(json.dumps(bulk, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("bulk appended", added, "total", len(bulk["items"]))

    zh = json.loads(ZH.read_text(encoding="utf-8"))
    en = json.loads(EN.read_text(encoding="utf-8"))
    tex_n = 0
    for it in new:
        rid = it["id"]
        png = ITEM_TEX / f"{rid}.png"
        if not png.exists():
            ensure(png)
            make_icon(rid, it["category"]).save(png, format="PNG")
            tex_n += 1
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
        disp = displays.get(rid) or it.get("display") or rid
        zh[f"item.seeking_immortals.{rid}"] = disp
        en[f"item.seeking_immortals.{rid}"] = title_en(rid)
    ZH.write_text(json.dumps(zh, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    EN.write_text(json.dumps(en, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("tex created", tex_n)

    by = {e.get("source_id"): e for e in idmap["entries"]}
    for it in new:
        rid = it["id"]
        sc = {
            "talisman": "talisman",
            "pill": "pill",
            "consumable": "consumable",
            "material": "material",
        }.get(it["category"], "material")
        if rid in by:
            e = by[rid]
            e["canonical_type"] = "item"
            e["canonical_id"] = f"seeking_immortals:{rid}"
            e["status"] = "implemented"
            e["note"] = "Wave 0.1.447 remaining shop/novel/tribulation carrier."
        else:
            idmap["entries"].append(
                {
                    "source_category": sc,
                    "source_id": rid,
                    "source_files": ["novel_items_waves/merchant_shops/tribulation_items"],
                    "canonical_type": "item",
                    "canonical_id": f"seeking_immortals:{rid}",
                    "status": "implemented",
                    "note": "Wave 0.1.447 remaining shop/novel/tribulation carrier.",
                }
            )

    for rid in EXCLUDE_EFFECTS:
        if rid in by:
            e = by[rid]
            e["status"] = "deferred"
            e["canonical_type"] = "effect"
            e["canonical_id"] = rid
            e["note"] = "Non-item tribulation effect/zone; no physical carrier by design (0.1.447)."
        else:
            idmap["entries"].append(
                {
                    "source_category": "effect",
                    "source_id": rid,
                    "source_files": ["tribulation_items.json"],
                    "canonical_type": "effect",
                    "canonical_id": rid,
                    "status": "deferred",
                    "note": "Non-item tribulation effect/zone; no physical carrier by design (0.1.447).",
                }
            )
    IDMAP.write_text(json.dumps(idmap, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    gp = Path("gradle.properties")
    t = gp.read_text(encoding="utf-8")
    t2 = t.replace("mod_version=0.1.446", "mod_version=0.1.447")
    if t2 == t and "mod_version=0.1.447" not in t:
        raise SystemExit("version bump failed")
    if t2 != t:
        gp.write_text(t2, encoding="utf-8")
    print("version", [l for l in gp.read_text(encoding="utf-8").splitlines() if "mod_version" in l][0])
    print("DONE", [n["id"] for n in new])


if __name__ == "__main__":
    main()
