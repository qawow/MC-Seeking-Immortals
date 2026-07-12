#!/usr/bin/env python3
"""Register Wave 0.1.441 materials + missing artifact carriers and update resources."""
from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(".")
mod_items = ROOT / "src/main/java/com/xunxian/seekingimmortals/registry/ModItems.java"
mod_tabs = ROOT / "src/main/java/com/xunxian/seekingimmortals/registry/ModCreativeTabs.java"
zh = ROOT / "src/main/resources/assets/seeking_immortals/lang/zh_cn.json"
en = ROOT / "src/main/resources/assets/seeking_immortals/lang/en_us.json"
idmap = ROOT / "src/main/resources/data/seeking_immortals/reference/text_material_id_map.json"
test = ROOT / "src/test/java/com/xunxian/seekingimmortals/artifact/ArtifactRefinementServiceTest.java"

materials = [
    (
        "WIND_FEATHER",
        "wind_feather",
        "BEAST_MATERIAL",
        "UNCOMMON",
        "Wind-aspect feather for sails and light artifacts",
        "风羽",
        "Wind Feather",
        True,
    ),
    (
        "BEAST_HIDE",
        "beast_hide",
        "BEAST_MATERIAL",
        "COMMON",
        "Tanned beast hide for armor and bridles",
        "兽皮",
        "Beast Hide",
        False,
    ),
    (
        "TURTLE_SHELL",
        "turtle_shell",
        "BEAST_MATERIAL",
        "UNCOMMON",
        "Hard turtle shell plate for puppet cores",
        "龟甲",
        "Turtle Shell",
        True,
    ),
    (
        "POISON_SAC",
        "poison_sac",
        "BEAST_MATERIAL",
        "COMMON",
        "Beast poison sac for needle and toxin recipes",
        "毒囊",
        "Poison Sac",
        False,
    ),
]

missing = json.loads(Path("project_docs/_tmp_missing_artifacts_0.1.441.json").read_text(encoding="utf-8"))

zh_names = {
    "azure_ice_sword": "青冰剑",
    "azure_rope_net": "青索网",
    "beast_soul_bell": "兽魂铃",
    "black_boots": "玄黑靴",
    "bone_wind_cart": "骨风车",
    "dark_iron_ring": "玄铁环",
    "demon_ape_armor": "魔猿甲",
    "dragon_scale_armor": "龙鳞甲",
    "fire_crow_fan": "火鸦扇",
    "fire_rain_needles": "火雨针",
    "flat_crown_replica": "平天冠残件",
    "giant_ape_puppet_token": "巨猿傀儡符",
    "giant_turtle_puppet_core": "巨龟傀儡核",
    "glazed_guard_shield": "琉璃护盾",
    "green_bamboo_cloud_sword": "青竹云剑",
    "green_bamboo_leaf_sword": "青竹叶剑",
    "huangsi_robe_artifact": "黄丝袍法器",
    "hunyuan_bowl": "混元钵",
    "hunyuan_bowl_replica": "混元钵仿制",
    "ice_fire_dual_orb": "冰火双珠",
    "invisible_needle_set": "无形针匣",
    "lengyue_blade": "冷月刀",
    "lieyang_short_sword": "烈阳短剑",
    "peerless_flying_knives": "绝世飞刀",
    "phoenix_feather_fan": "凤凰羽扇",
    "poluo_beads": "破罗珠",
    "potian_shovel": "破天铲",
    "red_thread_needles_replica": "红线针仿制",
    "scarlet_dragon_blade": "赤龙刃",
    "seven_star_disk": "七星盘",
    "silver_spirit_mirror": "银灵镜",
    "soul_capturing_bell": "摄魂铃",
    "soul_gathering_bowl": "聚魂钵",
    "soul_summon_bell": "召魂铃",
    "talisman_treasure_fire_spear": "火矛符宝",
    "talisman_treasure_golden_wheel": "金轮符宝",
    "talisman_treasure_ice_shield": "冰盾符宝",
    "talisman_treasure_thunder_rod": "雷杖符宝",
    "thousand_bee_needles": "万蜂针",
    "thunder_pearl_talisman": "雷珠符",
    "vajra_shield": "金刚盾",
    "xuanguang_mirror_replica": "玄光镜仿制",
    "xuantie_flying_shield": "玄铁飞盾",
}
en_names = {k: " ".join(w.capitalize() for w in k.split("_")) for k in missing}


def main() -> None:
    text = mod_items.read_text(encoding="utf-8")
    if "WIND_FEATHER" not in text:
        lines = ["    // Wave 0.1.441: decompress remaining vanilla material aliases."]
        for const, rid, cat, rar, desc, _zh, _en, use_rarity in materials:
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
        mat_code = "\n".join(lines) + "\n"
        anchor = 'public static final RegistryObject<Item> SPACE_CRYSTAL = ITEMS.register("space_crystal",'
        idx = text.find(anchor)
        if idx < 0:
            raise SystemExit("SPACE_CRYSTAL not found")
        end = text.find("));", idx)
        end = text.find("\n", end) + 1
        text = text[:end] + "\n" + mat_code + text[end:]
        print("materials inserted")
    else:
        print("materials already present")

    if "GREEN_BAMBOO_LEAF_SWORD" not in text and "green_bamboo_leaf_sword" not in text:
        art_lines = ["    // Wave 0.1.441: missing refinement output carriers."]
        for rid in missing:
            art_lines.append(
                f'    public static final RegistryObject<Item> {rid.upper()} = registerArtifact("{rid}");'
            )
        art_code = "\n".join(art_lines) + "\n"
        anchor = (
            'public static final RegistryObject<Item> THREE_FLAME_FAN_REPLICA = '
            'registerArtifact("three_flame_fan_replica");'
        )
        idx = text.find(anchor)
        if idx < 0:
            raise SystemExit("THREE_FLAME_FAN_REPLICA not found")
        end = text.find("\n", idx) + 1
        text = text[:end] + art_code + text[end:]
        print("artifacts inserted", len(missing))
    else:
        print("artifacts already present")

    mod_items.write_text(text, encoding="utf-8")

    tabs = mod_tabs.read_text(encoding="utf-8")
    if "WIND_FEATHER" not in tabs:
        needle = "output.accept(ModItems.TRUE_DRAGON_BLOOD.get());"
        if needle not in tabs:
            raise SystemExit("TRUE_DRAGON_BLOOD accept not found")
        insert = (
            needle
            + "\n"
            + "                output.accept(ModItems.WIND_FEATHER.get());\n"
            + "                output.accept(ModItems.BEAST_HIDE.get());\n"
            + "                output.accept(ModItems.TURTLE_SHELL.get());\n"
            + "                output.accept(ModItems.POISON_SAC.get());"
        )
        tabs = tabs.replace(needle, insert, 1)
        print("tab materials ok")

    if "GREEN_BAMBOO_LEAF_SWORD" not in tabs:
        marker = None
        for cand in [
            "output.accept(ModItems.THREE_FLAME_FAN_REPLICA.get());",
            "output.accept(ModItems.FOUR_SYMBOLS_RULER_REPLICA.get());",
            "output.accept(ModItems.NATAL_SWORD_EMBRYO.get());",
            "output.accept(ModItems.VOID_REFINING_BELL.get());",
        ]:
            if cand in tabs:
                marker = cand
                break
        if not marker:
            raise SystemExit("no artifact accept marker")
        lines = [marker]
        for rid in missing:
            lines.append(f"                output.accept(ModItems.{rid.upper()}.get());")
        tabs = tabs.replace(marker, "\n".join(lines), 1)
        print("tab artifacts ok")
    mod_tabs.write_text(tabs, encoding="utf-8")

    zh_map = {
        f"item.seeking_immortals.{rid}": zh_names.get(rid, rid) for rid in missing
    }
    zh_map.update(
        {
            "item.seeking_immortals.wind_feather": "风羽",
            "item.seeking_immortals.beast_hide": "兽皮",
            "item.seeking_immortals.turtle_shell": "龟甲",
            "item.seeking_immortals.poison_sac": "毒囊",
        }
    )
    en_map = {f"item.seeking_immortals.{rid}": en_names[rid] for rid in missing}
    en_map.update(
        {
            "item.seeking_immortals.wind_feather": "Wind Feather",
            "item.seeking_immortals.beast_hide": "Beast Hide",
            "item.seeking_immortals.turtle_shell": "Turtle Shell",
            "item.seeking_immortals.poison_sac": "Poison Sac",
        }
    )
    for path, mapping in [(zh, zh_map), (en, en_map)]:
        data = json.loads(path.read_text(encoding="utf-8"))
        data.update(mapping)
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print("lang", path.name, "keys", len(data))

    data = json.loads(idmap.read_text(encoding="utf-8"))
    for entry in data["entries"]:
        sid = entry.get("source_id")
        if sid in ("wind_feather", "beast_hide", "turtle_shell", "poison_sac"):
            entry["canonical_id"] = f"seeking_immortals:{sid}"
            entry["status"] = "implemented"
            entry["note"] = "Wave 0.1.441 independent carrier; no longer vanilla alias."
    idmap.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("id-map updated")

    t = test.read_text(encoding="utf-8")
    reps = [
        (
            '"wind_feather", "minecraft:feather"',
            '"wind_feather", "seeking_immortals:wind_feather"',
        ),
        (
            '"beast_hide", "minecraft:leather"',
            '"beast_hide", "seeking_immortals:beast_hide"',
        ),
        (
            '"turtle_shell", "minecraft:scute"',
            '"turtle_shell", "seeking_immortals:turtle_shell"',
        ),
        (
            '"poison_sac", "minecraft:spider_eye"',
            '"poison_sac", "seeking_immortals:poison_sac"',
        ),
    ]
    for old, new in reps:
        print(old, "count", t.count(old))
        t = t.replace(old, new)
    test.write_text(t, encoding="utf-8")

    gp = Path("gradle.properties")
    g = gp.read_text(encoding="utf-8")
    g2 = g.replace("mod_version=0.1.440", "mod_version=0.1.441")
    if g2 == g:
        raise SystemExit("version bump failed")
    gp.write_text(g2, encoding="utf-8")
    print("version 0.1.441")

    text = mod_items.read_text(encoding="utf-8")
    reg = set(re.findall(r'register\(\s*"([a-z0-9_]+)"', text))
    reg |= set(re.findall(r'registerArtifact\(\s*"([a-z0-9_]+)"', text))
    recipes = json.loads(
        Path("src/main/resources/data/seeking_immortals/artifacts/refinement_recipes.json").read_text(
            encoding="utf-8"
        )
    )
    arts = {
        r["artifact_id"].split(":")[-1]
        for r in recipes["recipes"]
        if r.get("artifact_id")
    }
    still = sorted(a for a in arts if a not in reg)
    print("still missing artifacts", still)
    data = json.loads(idmap.read_text(encoding="utf-8"))
    van = [
        e["source_id"]
        for e in data["entries"]
        if isinstance(e.get("canonical_id"), str)
        and e["canonical_id"].startswith("minecraft:")
        and e.get("source_category") == "material"
    ]
    print("vanilla material aliases left", van)


if __name__ == "__main__":
    main()
