#!/usr/bin/env python3
"""Generate bulk catalog carriers for all missing ids (0.1.444)."""
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
MOD = ROOT / "src/main/java/com/xunxian/seekingimmortals/SeekingImmortalsMod.java"
TABS = ROOT / "src/main/java/com/xunxian/seekingimmortals/registry/ModCreativeTabs.java"
ITEMS = ROOT / "src/main/java/com/xunxian/seekingimmortals/registry/ModItems.java"

# Avoid colliding with existing registrations
EXISTING = set(
    re.findall(
        r'register(?:Artifact|Material|SpiritStone|TechniqueManual|CatalogPill|AlchemyFormula|AlchemyLid|DanFire)?\(\s*"([a-z0-9_]+)"',
        ITEMS.read_text(encoding="utf-8"),
    )
)
EXISTING |= set(re.findall(r'ITEMS\.register\(\s*"([a-z0-9_]+)"', ITEMS.read_text(encoding="utf-8")))
# pill expansions
cpt = ROOT / "src/main/java/com/xunxian/seekingimmortals/item/pill/CatalogPillType.java"
if cpt.exists():
    t = cpt.read_text(encoding="utf-8")
    for m in re.finditer(r'\("([a-z0-9_]+)"', t):
        base = m.group(1)
        EXISTING.add(base)
        for q in ("low", "mid", "high", "perfect", "supreme"):
            EXISTING.add(f"{base}_{q}")
EXISTING |= {p.stem for p in ITEM_MODELS.glob("*.json")}


def ensure(p: Path) -> None:
    p.parent.mkdir(parents=True, exist_ok=True)


def color(name: str):
    h = hashlib.md5(name.encode()).hexdigest()
    return 80 + int(h[0:2], 16) % 160, 80 + int(h[2:4], 16) % 160, 80 + int(h[4:6], 16) % 160, 255


def make_icon(name: str, cat: str, size: int = 64) -> Image.Image:
    base = color(name)
    im = Image.new("RGBA", (size, size), (base[0] // 2, base[1] // 2, base[2] // 2, 255))
    d = ImageDraw.Draw(im)
    d.rectangle([1, 1, size - 2, size - 2], outline=(255, 240, 200, 220), width=2)
    if cat == "artifact":
        d.polygon(
            [(size * 0.5, size * 0.12), (size * 0.78, size * 0.5), (size * 0.5, size * 0.88), (size * 0.22, size * 0.5)],
            fill=(220, 210, 120, 255),
            outline=(80, 60, 10, 255),
        )
    elif cat == "pill":
        d.ellipse([size * 0.22, size * 0.22, size * 0.78, size * 0.78], fill=base, outline=(255, 255, 255, 200))
    elif cat == "talisman":
        d.rectangle([size * 0.3, size * 0.15, size * 0.7, size * 0.85], fill=(250, 240, 210, 255), outline=(120, 40, 40, 255))
    elif cat == "material":
        d.polygon(
            [
                (size * 0.2, size * 0.7),
                (size * 0.35, size * 0.25),
                (size * 0.55, size * 0.45),
                (size * 0.8, size * 0.2),
                (size * 0.85, size * 0.75),
            ],
            fill=base,
            outline=(30, 30, 30, 255),
        )
    else:
        d.rounded_rectangle([size * 0.2, size * 0.2, size * 0.8, size * 0.8], radius=8, fill=base, outline=(255, 255, 255, 180))
    try:
        font = ImageFont.load_default()
    except Exception:
        font = None
    label = "".join(ch for ch in name if ch.isalnum())[:2].upper() or "SI"
    d.text((size * 0.36, size * 0.4), label[:2], fill=(255, 255, 255, 230), font=font)
    return im


def title_en(rid: str) -> str:
    return " ".join(w.capitalize() for w in rid.split("_"))


def rarity_for(cat: str, rid: str) -> str:
    if any(x in rid for x in ("ancient", "immortal", "asura", "true_", "supreme", "great_vehicle")):
        return "epic"
    if cat == "artifact":
        return "rare"
    if cat == "pill":
        return "uncommon"
    if cat == "currency":
        return "uncommon"
    return "common"


def main() -> None:
    enriched = json.loads(Path("project_docs/_tmp_bulk_missing_enriched_0.1.444.json").read_text(encoding="utf-8"))
    # filter existing
    items = []
    for e in enriched:
        rid = e["id"]
        if rid in EXISTING:
            continue
        if not re.fullmatch(r"[a-z0-9_]+", rid):
            continue
        cat = e.get("category") or "material"
        disp = e.get("display") or ""
        items.append(
            {
                "id": rid,
                "category": cat,
                "rarity": rarity_for(cat, rid),
                "description": f"Catalog carrier for {rid}",
                "display_zh": disp if disp else rid,
                "display_en": title_en(rid),
            }
        )
    print("bulk items to register", len(items), "skipped existing", len(enriched) - len(items))

    # write bulk json for Java loader
    ensure(BULK_JSON)
    BULK_JSON.write_text(
        json.dumps({"schema_version": 1, "items": [{"id": i["id"], "category": i["category"], "rarity": i["rarity"], "description": i["description"]} for i in items]}, ensure_ascii=False, indent=2)
        + "\n",
        encoding="utf-8",
    )
    print("wrote", BULK_JSON)

    # assets + lang
    zh = json.loads(ZH.read_text(encoding="utf-8"))
    en = json.loads(EN.read_text(encoding="utf-8"))
    created_tex = 0
    for it in items:
        rid = it["id"]
        png = ITEM_TEX / f"{rid}.png"
        if not png.exists():
            ensure(png)
            make_icon(rid, it["category"]).save(png, format="PNG")
            created_tex += 1
        (ITEM_MODELS / f"{rid}.json").write_text(
            json.dumps(
                {"parent": "minecraft:item/generated", "textures": {"layer0": f"seeking_immortals:item/{rid}"}},
                indent=2,
            )
            + "\n",
            encoding="utf-8",
        )
        zh[f"item.seeking_immortals.{rid}"] = it["display_zh"] if it["display_zh"] else title_en(rid)
        en[f"item.seeking_immortals.{rid}"] = it["display_en"]
    ZH.write_text(json.dumps(zh, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    EN.write_text(json.dumps(en, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("textures created", created_tex, "lang keys added", len(items))

    # id-map: for each bulk id, if source exists update; else append
    idmap = json.loads(IDMAP.read_text(encoding="utf-8"))
    by = {e.get("source_id"): e for e in idmap["entries"]}
    for it in items:
        rid = it["id"]
        cat = it["category"]
        source_cat = {
            "artifact": "artifact",
            "pill": "pill",
            "talisman": "talisman",
            "material": "material",
            "consumable": "consumable",
            "currency": "currency",
            "equipment": "equipment",
            "manual": "manual",
            "formula": "recipe_unlock",
        }.get(cat, "material")
        if rid in by:
            e = by[rid]
            e["canonical_type"] = "item"
            e["canonical_id"] = f"seeking_immortals:{rid}"
            e["status"] = "implemented"
            e["note"] = "Wave 0.1.444 bulk catalog carrier."
        else:
            idmap["entries"].append(
                {
                    "source_category": source_cat,
                    "source_id": rid,
                    "source_files": ["catalog_bulk"],
                    "canonical_type": "item",
                    "canonical_id": f"seeking_immortals:{rid}",
                    "status": "implemented",
                    "note": "Wave 0.1.444 bulk catalog carrier.",
                }
            )
    # also: any compressed aliases where source has no own item but we just created it - already handled if source_id == rid
    # For compressed list where source != canonical, if we created source id, update those entries too
    created_ids = {it["id"] for it in items}
    for e in idmap["entries"]:
        sid = e.get("source_id")
        if sid in created_ids:
            e["canonical_id"] = f"seeking_immortals:{sid}"
            e["canonical_type"] = "item"
            e["status"] = "implemented"
            e["note"] = "Wave 0.1.444 bulk catalog carrier / decompressed."
    IDMAP.write_text(json.dumps(idmap, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("id-map updated")

    # wire ModBulkItems into mod constructor if needed
    mod = MOD.read_text(encoding="utf-8")
    if "ModBulkItems" not in mod:
        if "import com.xunxian.seekingimmortals.registry.ModItems;" in mod:
            mod = mod.replace(
                "import com.xunxian.seekingimmortals.registry.ModItems;",
                "import com.xunxian.seekingimmortals.registry.ModItems;\nimport com.xunxian.seekingimmortals.registry.ModBulkItems;",
            )
        else:
            mod = mod.replace(
                "import com.xunxian.seekingimmortals.registry.ModBlocks;",
                "import com.xunxian.seekingimmortals.registry.ModBlocks;\nimport com.xunxian.seekingimmortals.registry.ModBulkItems;",
            )
        mod = mod.replace("ModItems.register(modBus);", "ModItems.register(modBus);\n        ModBulkItems.register(modBus);")
        MOD.write_text(mod, encoding="utf-8")
        print("SeekingImmortalsMod wired")
    else:
        print("SeekingImmortalsMod already wired")

    # creative tab: add bulk accept loop near end of displayItems before closing
    tabs = TABS.read_text(encoding="utf-8")
    if "ModBulkItems" not in tabs:
        # add import not needed same package
        needle = "output.accept(ModItems.WASTE_PILL.get());"
        # find a stable late accept; if waste pill missing use last accept before `})`
        if "ModBulkItems.byId()" not in tabs:
            insert = (
                "\n                // Wave 0.1.444 bulk catalog carriers\n"
                "                for (var bulk : com.xunxian.seekingimmortals.registry.ModBulkItems.byId().values()) {\n"
                "                    output.accept(bulk.get());\n"
                "                }\n"
            )
            # insert before closing of displayItems lambda: look for `            })\n`
            marker = "            })\n            .build());"
            if marker not in tabs:
                # alternate
                marker = "            }).build());"
            if marker not in tabs:
                # try find last output.accept then insert after creative block end
                idx = tabs.rfind("output.accept(")
                if idx < 0:
                    raise SystemExit("cannot find creative tab insertion point")
                # find end of that statement line
                end = tabs.find("\n", idx)
                tabs = tabs[: end + 1] + insert + tabs[end + 1 :]
            else:
                tabs = tabs.replace(marker, insert + marker, 1)
            TABS.write_text(tabs, encoding="utf-8")
            print("creative tab bulk loop added")
    else:
        print("creative tab already has bulk")

    # version bump
    gp = Path("gradle.properties")
    t = gp.read_text(encoding="utf-8")
    t2 = t.replace("mod_version=0.1.443", "mod_version=0.1.444")
    if t2 == t:
        # maybe already
        if "mod_version=0.1.444" not in t:
            raise SystemExit("version bump failed from 0.1.443")
    else:
        gp.write_text(t2, encoding="utf-8")
    print("version", [l for l in gp.read_text(encoding="utf-8").splitlines() if "mod_version" in l][0])
    print("DONE bulk gen", len(items))


if __name__ == "__main__":
    main()
