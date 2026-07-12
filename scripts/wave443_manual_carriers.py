#!/usr/bin/env python3
"""0.1.443: register manuals_catalog carriers + fix invisible_needle id-map."""
from __future__ import annotations

import hashlib
import json
import re
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(".")
MOD_ITEMS = ROOT / "src/main/java/com/xunxian/seekingimmortals/registry/ModItems.java"
MOD_TABS = ROOT / "src/main/java/com/xunxian/seekingimmortals/registry/ModCreativeTabs.java"
ZH = ROOT / "src/main/resources/assets/seeking_immortals/lang/zh_cn.json"
EN = ROOT / "src/main/resources/assets/seeking_immortals/lang/en_us.json"
IDMAP = ROOT / "src/main/resources/data/seeking_immortals/reference/text_material_id_map.json"
ITEM_MODELS = ROOT / "src/main/resources/assets/seeking_immortals/models/item"
ITEM_TEX = ROOT / "src/main/resources/assets/seeking_immortals/textures/item"

# Only manuals that currently have NO id-map and NO item.
MANUALS = [
    ("refinement_manual_low", "炼器入门篇", "Refinement Manual (Low)"),
    ("refinement_manual_mid", "炼器进阶篇", "Refinement Manual (Mid)"),
    ("refinement_manual_ancient", "古宝炼器要诀", "Ancient Refinement Manual"),
    ("recipe_refine_flying_sword", "低阶飞剑谱", "Flying Sword Refinement Recipe"),
    ("recipe_refine_evil_mirror", "邪幻镜炼器要诀", "Evil Mirror Refinement Recipe"),
    ("recipe_refine_giant_turtle", "巨龟傀儡图", "Giant Turtle Puppet Manual"),
    ("ancient_puppet_method", "上古傀儡术", "Ancient Puppet Method"),
    ("ghost_cultivation_manual", "鬼道修炼录", "Ghost Cultivation Manual"),
    ("fashi_array_manual", "法式阵法要诀", "Fashi Array Manual"),
    ("array_blueprint_scroll", "阵图残卷", "Array Blueprint Scroll"),
    ("recipe_foundation", "筑基丹方", "Foundation Pill Recipe Slip"),
    ("recipe_bu_tian", "补天丹方", "Bu Tian Pill Recipe Slip"),
    ("illusion_talisman_scroll", "幻符要诀", "Illusion Talisman Scroll"),
    ("dayan_solution_fragment", "大衍诀残页", "Dayan Solution Fragment"),
    ("stolen_jade_slip", "失窃玉简", "Stolen Jade Slip"),
    ("manual_dayan_true_solution", "大衍真解", "Dayan True Solution Manual"),
    ("manual_ancient_puppet_art", "上古傀儡真传", "Ancient Puppet Art Manual"),
]


def ensure(path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)


def make_icon(name: str, size: int = 64) -> Image.Image:
    h = hashlib.md5(name.encode()).hexdigest()
    base = (80 + int(h[0:2], 16) % 160, 80 + int(h[2:4], 16) % 160, 80 + int(h[4:6], 16) % 160, 255)
    im = Image.new("RGBA", (size, size), (base[0] // 2, base[1] // 2, base[2] // 2, 255))
    d = ImageDraw.Draw(im)
    d.rectangle([1, 1, size - 2, size - 2], outline=(255, 240, 200, 220), width=2)
    # book shape
    d.rectangle([size * 0.22, size * 0.15, size * 0.78, size * 0.85], fill=(250, 240, 210, 255), outline=(90, 60, 20, 255))
    for i in range(4):
        yy = size * 0.28 + i * size * 0.12
        d.line([size * 0.3, yy, size * 0.7, yy], fill=(120, 80, 40, 255), width=1)
    try:
        font = ImageFont.load_default()
    except Exception:
        font = None
    label = "".join(ch for ch in name if ch.isalnum())[:2].upper() or "MN"
    d.text((size * 0.38, size * 0.42), label[:2], fill=(80, 40, 10, 255), font=font)
    return im


def write_assets() -> None:
    for rid, _zh, _en in MANUALS:
        png = ITEM_TEX / f"{rid}.png"
        if not png.exists():
            ensure(png)
            make_icon(rid).save(png, format="PNG")
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
    print("assets", len(MANUALS))


def const_name(rid: str) -> str:
    return rid.upper()


def patch_mod_items() -> None:
    text = MOD_ITEMS.read_text(encoding="utf-8")
    if "REFINEMENT_MANUAL_LOW" in text or "refinement_manual_low" in text:
        print("manuals already in ModItems")
        return
    # ensure import
    if "CatalogManualItem" not in text:
        text = text.replace(
            "import com.xunxian.seekingimmortals.item.*;",
            "import com.xunxian.seekingimmortals.item.*;\nimport com.xunxian.seekingimmortals.item.CatalogManualItem;",
            1,
        )
    lines = ["    // Wave 0.1.443: manuals_catalog physical carriers."]
    for rid, _zh, _en in MANUALS:
        lines.append(
            f'    public static final RegistryObject<Item> {const_name(rid)} = ITEMS.register("{rid}",\n'
            f"            () -> new CatalogManualItem(new Item.Properties().stacksTo(16), \"{rid}\"));"
        )
    block = "\n".join(lines) + "\n"
    # insert after last technique manual block - after TECHNIQUE_MANUAL_GRAY_IMMORTAL or similar last one
    # find last registerTechniqueManual line
    matches = list(re.finditer(r'public static final RegistryObject<Item> TECHNIQUE_MANUAL_[A-Z0-9_]+ = registerTechniqueManual\("[^"]+", "[^"]*"\);', text))
    if not matches:
        raise SystemExit("no technique manuals found")
    end = matches[-1].end()
    # include newline
    if end < len(text) and text[end] == "\n":
        end += 1
    text = text[:end] + block + text[end:]
    MOD_ITEMS.write_text(text, encoding="utf-8")
    print("ModItems manuals inserted", len(MANUALS))


def patch_tabs() -> None:
    text = MOD_TABS.read_text(encoding="utf-8")
    if "REFINEMENT_MANUAL_LOW" in text:
        print("tabs already")
        return
    # after last technique manual accept if any, else after jade slip
    marker = None
    for cand in [
        "output.accept(ModItems.TECHNIQUE_MANUAL_GRAY_IMMORTAL_HERITAGE.get());",
        "output.accept(ModItems.TECHNIQUE_MANUAL_AZURE_SEA_TRUE_LORD_SKILL.get());",
        "output.accept(ModItems.TECHNIQUE_MANUAL_COMMON.get());",
        "output.accept(ModItems.JADE_SLIP_BLANK.get());",
    ]:
        if cand in text:
            marker = cand
            break
    if not marker:
        # find any TECHNIQUE_MANUAL accept
        m = re.search(r"output\.accept\(ModItems\.TECHNIQUE_MANUAL_[A-Z0-9_]+\.get\(\)\);", text)
        if not m:
            raise SystemExit("no technique manual tab marker")
        marker = m.group(0)
    lines = [marker]
    for rid, _zh, _en in MANUALS:
        lines.append(f"                output.accept(ModItems.{const_name(rid)}.get());")
    text = text.replace(marker, "\n".join(lines), 1)
    MOD_TABS.write_text(text, encoding="utf-8")
    print("tabs ok")


def patch_lang() -> None:
    zh_map = {f"item.seeking_immortals.{rid}": zh for rid, zh, _en in MANUALS}
    en_map = {f"item.seeking_immortals.{rid}": en for rid, _zh, en in MANUALS}
    zh_map["item.seeking_immortals.catalog_manual.tooltip"] = "文本手册：%s（右键研读）"
    en_map["item.seeking_immortals.catalog_manual.tooltip"] = "Catalog manual: %s (right-click to study)"
    for path, mapping in [(ZH, zh_map), (EN, en_map)]:
        data = json.loads(path.read_text(encoding="utf-8"))
        data.update(mapping)
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print("lang", path.name)


def patch_idmap() -> None:
    data = json.loads(IDMAP.read_text(encoding="utf-8"))
    by_source = {e.get("source_id"): e for e in data["entries"]}
    # manuals
    for rid, zh, en in MANUALS:
        if rid in by_source:
            e = by_source[rid]
            e["canonical_id"] = f"seeking_immortals:{rid}"
            e["canonical_type"] = "item"
            e["status"] = "implemented"
            e["note"] = "Wave 0.1.443 physical CatalogManualItem carrier."
        else:
            data["entries"].append(
                {
                    "source_category": "manual",
                    "source_id": rid,
                    "source_files": ["manuals_catalog.json"],
                    "canonical_type": "item",
                    "canonical_id": f"seeking_immortals:{rid}",
                    "status": "implemented",
                    "note": f"Wave 0.1.443 physical carrier for manuals_catalog ({zh} / {en}).",
                }
            )
    # invisible needle: already registered, fix alias
    if "invisible_needle_set" in by_source:
        e = by_source["invisible_needle_set"]
        e["canonical_id"] = "seeking_immortals:invisible_needle_set"
        e["status"] = "implemented"
        e["note"] = "Wave 0.1.443 independent carrier (no longer aliases flying_needle_set)."
    IDMAP.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("id-map ok")


def bump() -> None:
    gp = Path("gradle.properties")
    t = gp.read_text(encoding="utf-8")
    t2 = t.replace("mod_version=0.1.442", "mod_version=0.1.443")
    if t2 == t:
        raise SystemExit("version bump failed")
    gp.write_text(t2, encoding="utf-8")
    print("version 0.1.443")


def verify() -> None:
    mi = MOD_ITEMS.read_text(encoding="utf-8")
    for rid, _zh, _en in MANUALS:
        assert f'register("{rid}"' in mi or f'register("{rid}",' in mi, rid
        assert (ITEM_MODELS / f"{rid}.json").exists()
        assert (ITEM_TEX / f"{rid}.png").exists()
    print("verify ok")


def main() -> None:
    write_assets()
    patch_mod_items()
    patch_tabs()
    patch_lang()
    patch_idmap()
    bump()
    verify()


if __name__ == "__main__":
    main()
