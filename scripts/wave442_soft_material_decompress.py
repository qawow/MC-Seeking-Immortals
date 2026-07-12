#!/usr/bin/env python3
"""0.1.442: decompress 7 soft-alias refinement materials into independent carriers."""
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
TEST = ROOT / "src/test/java/com/xunxian/seekingimmortals/artifact/ArtifactRefinementServiceTest.java"
ITEM_MODELS = ROOT / "src/main/resources/assets/seeking_immortals/models/item"
ITEM_TEX = ROOT / "src/main/resources/assets/seeking_immortals/textures/item"

# const, id, category, rarity, use_item_rarity, desc, zh, en, kind, old_canonical
MATERIALS = [
    (
        "BEAST_BLOOD_VIAL",
        "beast_blood_vial",
        "BEAST_MATERIAL",
        "COMMON",
        False,
        "Low-tier beast blood vial for refinement",
        "兽血瓶",
        "Beast Blood Vial",
        "item",
        "seeking_immortals:true_dragon_blood",
    ),
    (
        "TRUE_SPIRIT_BLOOD_DROP",
        "true_spirit_blood_drop",
        "BEAST_MATERIAL",
        "RARE",
        True,
        "True-spirit blood drop for high refinement",
        "真灵血滴",
        "True Spirit Blood Drop",
        "item",
        "seeking_immortals:true_dragon_blood",
    ),
    (
        "BEAST_BONE_BLOCK",
        "beast_bone_block",
        "BEAST_MATERIAL",
        "UNCOMMON",
        True,
        "Bound beast bone block for bulk refinement",
        "兽骨块",
        "Beast Bone Block",
        "ore",
        "seeking_immortals:spirit_beast_bone",
    ),
    (
        "DEMON_CORRUPTION_FUNGUS",
        "demon_corruption_fungus",
        "SPIRITUAL_HERB",
        "UNCOMMON",
        True,
        "Demon-corruption fungus for dark artifacts",
        "魔污菌",
        "Demon Corruption Fungus",
        "item",
        "seeking_immortals:cloud_mushroom",
    ),
    (
        "EARTH_SPINE_ROOT",
        "earth_spine_root",
        "SPIRITUAL_HERB",
        "RARE",
        True,
        "Diyuan earth-spine root for shield refinement",
        "地脊根",
        "Earth Spine Root",
        "item",
        "seeking_immortals:diyuan_pressure_moss",
    ),
    (
        "SPACE_CRYSTAL_FRAGMENT",
        "space_crystal_fragment",
        "SPECIAL",
        "UNCOMMON",
        True,
        "Space crystal fragment for storage artifacts",
        "空间晶碎片",
        "Space Crystal Fragment",
        "item",
        "seeking_immortals:void_crystal",
    ),
    (
        "STAR_SAND",
        "star_sand",
        "MINERAL",
        "UNCOMMON",
        True,
        "Star sand for star-path refinement",
        "星沙",
        "Star Sand",
        "ore",
        "seeking_immortals:star_meteorite",
    ),
]


def ensure(path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)


def color(name: str):
    h = hashlib.md5(name.encode()).hexdigest()
    return 80 + int(h[0:2], 16) % 160, 80 + int(h[2:4], 16) % 160, 80 + int(h[4:6], 16) % 160, 255


def make_icon(name: str, kind: str = "item", size: int = 64) -> Image.Image:
    base = color(name)
    im = Image.new("RGBA", (size, size), (base[0] // 2, base[1] // 2, base[2] // 2, 255))
    d = ImageDraw.Draw(im)
    d.rectangle([1, 1, size - 2, size - 2], outline=(255, 240, 200, 220), width=2)
    if kind == "ore":
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
        d.ellipse([size * 0.22, size * 0.22, size * 0.78, size * 0.78], fill=base, outline=(255, 255, 255, 180))
    label = "".join(ch for ch in name if ch.isalnum())[:2].upper() or "SI"
    try:
        font = ImageFont.load_default()
    except Exception:
        font = None
    d.text((size * 0.36, size * 0.4), label[:2], fill=(255, 255, 255, 230), font=font)
    return im


def write_assets() -> None:
    for _const, rid, _c, _r, _ur, _d, _zh, _en, kind, _old in MATERIALS:
        png = ITEM_TEX / f"{rid}.png"
        if not png.exists():
            ensure(png)
            make_icon(rid, kind=kind).save(png, format="PNG")
        model = ITEM_MODELS / f"{rid}.json"
        model.write_text(
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
    print("assets ok")


def patch_mod_items() -> None:
    text = MOD_ITEMS.read_text(encoding="utf-8")
    if "BEAST_BLOOD_VIAL" in text:
        print("ModItems already has materials")
        return
    lines = ["    // Wave 0.1.442: decompress refinement soft-alias materials."]
    for const, rid, cat, rar, use_rarity, desc, _zh, _en, _k, _old in MATERIALS:
        props = "new Item.Properties()"
        if use_rarity:
            props = f"new Item.Properties().rarity(net.minecraft.world.item.Rarity.{rar})"
        lines.append(
            f'    public static final RegistryObject<Item> {const} = ITEMS.register("{rid}",\n'
            f"            () -> new com.xunxian.seekingimmortals.item.material.BaseMaterialItem(\n"
            f"                    {props},\n"
            f"                    com.xunxian.seekingimmortals.item.material.MaterialCategory.{cat},\n"
            f"                    com.xunxian.seekingimmortals.item.material.MaterialRarity.{rar},\n"
            f'                    "{desc}"));'
        )
    block = "\n".join(lines) + "\n"
    anchor = 'public static final RegistryObject<Item> POISON_SAC = ITEMS.register("poison_sac",'
    idx = text.find(anchor)
    if idx < 0:
        raise SystemExit("POISON_SAC not found")
    end = text.find("));", idx)
    end = text.find("\n", end) + 1
    MOD_ITEMS.write_text(text[:end] + "\n" + block + text[end:], encoding="utf-8")
    print("ModItems inserted")


def patch_tabs() -> None:
    text = MOD_TABS.read_text(encoding="utf-8")
    if "BEAST_BLOOD_VIAL" in text:
        print("tabs already has materials")
        return
    # place after poison sac if present, else after true dragon blood
    if "output.accept(ModItems.POISON_SAC.get());" in text:
        needle = "output.accept(ModItems.POISON_SAC.get());"
        insert = (
            needle
            + "\n"
            + "                output.accept(ModItems.BEAST_BLOOD_VIAL.get());\n"
            + "                output.accept(ModItems.TRUE_SPIRIT_BLOOD_DROP.get());\n"
            + "                output.accept(ModItems.BEAST_BONE_BLOCK.get());\n"
            + "                output.accept(ModItems.DEMON_CORRUPTION_FUNGUS.get());\n"
            + "                output.accept(ModItems.EARTH_SPINE_ROOT.get());"
        )
        text = text.replace(needle, insert, 1)
    else:
        raise SystemExit("POISON_SAC tab accept missing")

    # special/mineral extras near space crystal / star meteorite
    if "output.accept(ModItems.SPACE_CRYSTAL.get());" in text and "SPACE_CRYSTAL_FRAGMENT" not in text:
        text = text.replace(
            "output.accept(ModItems.SPACE_CRYSTAL.get());",
            "output.accept(ModItems.SPACE_CRYSTAL.get());\n"
            "                output.accept(ModItems.SPACE_CRYSTAL_FRAGMENT.get());",
            1,
        )
    if "output.accept(ModItems.STAR_METEORITE.get());" in text and "STAR_SAND" not in text:
        text = text.replace(
            "output.accept(ModItems.STAR_METEORITE.get());",
            "output.accept(ModItems.STAR_METEORITE.get());\n"
            "                output.accept(ModItems.STAR_SAND.get());",
            1,
        )
    MOD_TABS.write_text(text, encoding="utf-8")
    print("tabs updated")


def patch_lang() -> None:
    zh_map = {f"item.seeking_immortals.{rid}": zh for _c, rid, *_rest, zh, en, _k, _o in [
        (*m[:6], m[6], m[7], m[8], m[9]) for m in MATERIALS
    ]}
    # rebuild cleanly
    zh_map = {}
    en_map = {}
    for _const, rid, _cat, _rar, _ur, _desc, zh, en, _k, _old in MATERIALS:
        zh_map[f"item.seeking_immortals.{rid}"] = zh
        en_map[f"item.seeking_immortals.{rid}"] = en
    for path, mapping in [(ZH, zh_map), (EN, en_map)]:
        data = json.loads(path.read_text(encoding="utf-8"))
        data.update(mapping)
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print("lang", path.name)


def patch_idmap() -> None:
    data = json.loads(IDMAP.read_text(encoding="utf-8"))
    wanted = {m[1] for m in MATERIALS}
    for entry in data["entries"]:
        sid = entry.get("source_id")
        if sid in wanted:
            entry["canonical_id"] = f"seeking_immortals:{sid}"
            entry["status"] = "implemented"
            entry["note"] = "Wave 0.1.442 independent carrier; no longer soft-compressed alias."
    IDMAP.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("id-map updated")


def patch_tests() -> None:
    text = TEST.read_text(encoding="utf-8")
    reps = [
        (
            '"beast_blood_vial", "seeking_immortals:true_dragon_blood"',
            '"beast_blood_vial", "seeking_immortals:beast_blood_vial"',
        ),
        (
            '"true_spirit_blood_drop", "seeking_immortals:true_dragon_blood"',
            '"true_spirit_blood_drop", "seeking_immortals:true_spirit_blood_drop"',
        ),
        (
            '"beast_bone_block", "seeking_immortals:spirit_beast_bone"',
            '"beast_bone_block", "seeking_immortals:beast_bone_block"',
        ),
        (
            '"demon_corruption_fungus", "seeking_immortals:cloud_mushroom"',
            '"demon_corruption_fungus", "seeking_immortals:demon_corruption_fungus"',
        ),
        (
            '"earth_spine_root", "seeking_immortals:diyuan_pressure_moss"',
            '"earth_spine_root", "seeking_immortals:earth_spine_root"',
        ),
        (
            '"space_crystal_fragment", "seeking_immortals:void_crystal"',
            '"space_crystal_fragment", "seeking_immortals:space_crystal_fragment"',
        ),
        (
            '"star_sand", "seeking_immortals:star_meteorite"',
            '"star_sand", "seeking_immortals:star_sand"',
        ),
    ]
    for old, new in reps:
        print(old, "count", text.count(old))
        text = text.replace(old, new)
    TEST.write_text(text, encoding="utf-8")
    print("tests patched")


def bump_version() -> None:
    gp = Path("gradle.properties")
    text = gp.read_text(encoding="utf-8")
    text2 = text.replace("mod_version=0.1.441", "mod_version=0.1.442")
    if text2 == text:
        raise SystemExit("version bump failed")
    gp.write_text(text2, encoding="utf-8")
    print("version 0.1.442")


def verify() -> None:
    mi = MOD_ITEMS.read_text(encoding="utf-8")
    for _c, rid, *_ in MATERIALS:
        assert rid in mi, rid
        assert (ITEM_MODELS / f"{rid}.json").exists(), rid
        assert (ITEM_TEX / f"{rid}.png").exists(), rid
    data = json.loads(IDMAP.read_text(encoding="utf-8"))
    for entry in data["entries"]:
        if entry.get("source_id") in {m[1] for m in MATERIALS}:
            assert entry["canonical_id"] == f"seeking_immortals:{entry['source_id']}"
            assert entry["status"] == "implemented"
    print("verify ok")


def main() -> None:
    write_assets()
    patch_mod_items()
    patch_tabs()
    patch_lang()
    patch_idmap()
    patch_tests()
    bump_version()
    verify()


if __name__ == "__main__":
    main()
