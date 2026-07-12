#!/usr/bin/env python3
"""0.1.448: close remaining vehicle/puppet/refinement carriers + formation short-id aliases."""
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
BLOCKS_JAVA = ROOT / "src/main/java/com/xunxian/seekingimmortals/registry/ModBlocks.java"

# Short formation catalog ids -> already registered placeable cores / arrays.
FORMATION_ALIASES: dict[str, str] = {
    "spirit_gather": "spirit_gathering_formation_core",
    "spirit_gathering_minor": "spirit_gathering_minor_formation_core",
    "illusion_maze": "illusion_maze_formation_core",
    "illusion_maze_array": "illusion_maze_formation_core",
    "kill_sword": "kill_sword_formation_core",
    "defense_wall": "defense_formation_core",
    "thunder_tribulation_array": "thunder_tribulation_array_formation_core",
    "five_elements_mountain": "five_elements_mountain_formation_core",
    "seal_demon_array": "seal_demon_formation_core",
    "teleport_array": "teleport_array_pedestal",
    "teleport_array_long_range": "long_range_teleport_array",
    "nine_dragon_flame_barrier": "nine_dragon_flame_barrier_formation_core",
    "inverted_five_elements_array": "inverted_five_elements_formation_core",
    "vajra_prison_array": "vajra_prison_formation_core",
    "demon_seal_pillar_array": "demon_seal_pillar_formation_core",
    "mulan_wind_ride_array": "mulan_wind_ride_formation_core",
    "sword_array_bagua": "sword_array_bagua_formation_core",
    "barrier_sect_protection": "barrier_sect_protection_formation_core",
}

# Soft naming aliases that should not create a second physical item.
SOFT_ALIASES: dict[str, str] = {
    "spirit_bellows": "refinement_bellows",
    "anvil_core": "refinement_anvil",
    "blank_jade_slip": "jade_slip_blank",
    "demon_suppress_blank": "demon_suppress_talisman_blank",
    "war_token": "war_contribution_token",
    "yin_protection_charm": "yin_body_protection_charm",
    "natal_artifact": "natal_artifact_embryo",
}

# Physical carriers still missing as independent inventory items.
NEW_ITEMS: list[dict] = [
    # flight vehicles
    {"id": "spirit_boat_low", "category": "equipment", "rarity": "uncommon", "display": "低阶灵舟"},
    {"id": "spirit_boat_mid", "category": "equipment", "rarity": "rare", "display": "中阶灵舟"},
    {"id": "chaotic_sea_ferry", "category": "equipment", "rarity": "uncommon", "display": "乱星海渡船"},
    {"id": "cloud_sedan", "category": "equipment", "rarity": "epic", "display": "云轿"},
    {"id": "wind_feather_raft", "category": "equipment", "rarity": "rare", "display": "风羽筏"},
    {"id": "bone_wind_cart_vehicle", "category": "equipment", "rarity": "rare", "display": "御风车"},
    {"id": "spirit_boat_chaotic_sea", "category": "equipment", "rarity": "rare", "display": "乱星海灵舟"},
    {"id": "teleport_array_ticket", "category": "access_item", "rarity": "uncommon", "display": "传送阵令牌"},
    # puppets
    {"id": "basic_wood_puppet", "category": "equipment", "rarity": "common", "display": "木人傀儡"},
    {"id": "giant_ape_puppet", "category": "equipment", "rarity": "uncommon", "display": "巨猿傀儡"},
    {"id": "giant_turtle_puppet", "category": "equipment", "rarity": "rare", "display": "巨龟傀儡"},
    {"id": "stone_spirit_puppet", "category": "equipment", "rarity": "rare", "display": "石灵傀儡"},
    {"id": "hunyuan_bowl_core_puppet", "category": "equipment", "rarity": "epic", "display": "混元钵核傀儡"},
    {"id": "stone_guard_puppet", "category": "equipment", "rarity": "uncommon", "display": "石卫傀儡"},
    {"id": "fire_spear_puppet", "category": "equipment", "rarity": "rare", "display": "火矛傀儡"},
    # refinement station components / manuals
    {"id": "refinement_anvil", "category": "material", "rarity": "rare", "display": "锻心"},
    {"id": "refinement_bellows", "category": "material", "rarity": "rare", "display": "灵风囊"},
    {"id": "refinement_manual_high", "category": "manual", "rarity": "epic", "display": "高阶炼器手册"},
]


def ensure(path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)


def load_reg() -> set[str]:
    reg: set[str] = set()
    for java in (ITEMS_JAVA, BLOCKS_JAVA):
        if not java.exists():
            continue
        t = java.read_text(encoding="utf-8")
        reg |= set(re.findall(r'\.register\(\s*"([a-z0-9_]+)"', t))
        reg |= set(
            re.findall(
                r'register(?:Artifact|Material|SpiritStone|TechniqueManual|CatalogPill|AlchemyFormula|AlchemyLid|DanFire)?\(\s*"([a-z0-9_]+)"',
                t,
            )
        )
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


def make_icon(name: str, cat: str, size: int = 64) -> Image.Image:
    h = hashlib.md5(name.encode()).hexdigest()
    base = (80 + int(h[0:2], 16) % 160, 80 + int(h[2:4], 16) % 160, 80 + int(h[4:6], 16) % 160, 255)
    im = Image.new("RGBA", (size, size), (base[0] // 2, base[1] // 2, base[2] // 2, 255))
    d = ImageDraw.Draw(im)
    d.rectangle([1, 1, size - 2, size - 2], outline=(255, 240, 200, 220), width=2)
    if cat in ("equipment", "vehicle", "puppet"):
        d.polygon(
            [
                (size * 0.5, size * 0.15),
                (size * 0.82, size * 0.55),
                (size * 0.68, size * 0.85),
                (size * 0.32, size * 0.85),
                (size * 0.18, size * 0.55),
            ],
            fill=base,
            outline=(255, 255, 255, 200),
        )
    elif cat == "manual":
        d.rectangle(
            [size * 0.25, size * 0.15, size * 0.75, size * 0.85],
            fill=(240, 230, 200, 255),
            outline=(90, 50, 20, 255),
        )
    elif cat == "access_item":
        d.ellipse([size * 0.2, size * 0.2, size * 0.8, size * 0.8], fill=base, outline=(255, 255, 255, 200))
        d.ellipse([size * 0.38, size * 0.38, size * 0.62, size * 0.62], fill=(20, 20, 20, 180))
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


def upsert_idmap(
    idmap: dict,
    source_id: str,
    canonical_id: str,
    source_category: str,
    source_files: list[str],
    note: str,
    status: str = "implemented",
    canonical_type: str = "item",
) -> None:
    by = {e.get("source_id"): e for e in idmap["entries"] if isinstance(e, dict)}
    if source_id in by:
        e = by[source_id]
        e["canonical_type"] = canonical_type
        e["canonical_id"] = canonical_id
        e["status"] = status
        e["note"] = note
        files = e.get("source_files")
        if isinstance(files, list):
            for f in source_files:
                if f not in files:
                    files.append(f)
        else:
            e["source_files"] = source_files
    else:
        idmap["entries"].append(
            {
                "source_category": source_category,
                "source_id": source_id,
                "source_files": source_files,
                "canonical_type": canonical_type,
                "canonical_id": canonical_id,
                "status": status,
                "note": note,
            }
        )


def main() -> None:
    reg = load_reg()
    idmap = json.loads(IDMAP.read_text(encoding="utf-8"))
    bulk = json.loads(BULK.read_text(encoding="utf-8"))
    existing_bulk = {it["id"] for it in bulk.get("items", [])}

    # 1) new physical carriers first (aliases may target them)
    zh = json.loads(ZH.read_text(encoding="utf-8"))
    en = json.loads(EN.read_text(encoding="utf-8"))
    added: list[str] = []
    tex_n = 0
    for it in NEW_ITEMS:
        rid = it["id"]
        if rid in reg or rid in existing_bulk:
            # still ensure id-map points at itself
            upsert_idmap(
                idmap,
                rid,
                f"seeking_immortals:{rid}",
                it["category"],
                ["flight_vehicles.json", "puppet_definitions.json", "refinement_system.json"],
                "Wave 0.1.448 already present carrier verified.",
            )
            continue
        bulk["items"].append(
            {
                "id": rid,
                "category": it["category"],
                "rarity": it["rarity"],
                "description": f"Catalog carrier for {rid}",
            }
        )
        existing_bulk.add(rid)
        reg.add(rid)
        added.append(rid)

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
        zh[f"item.seeking_immortals.{rid}"] = it["display"]
        en[f"item.seeking_immortals.{rid}"] = title_en(rid)
        upsert_idmap(
            idmap,
            rid,
            f"seeking_immortals:{rid}",
            it["category"],
            ["flight_vehicles.json", "puppet_definitions.json", "refinement_system.json", "refine_manual_index.json"],
            "Wave 0.1.448 remaining vehicle/puppet/refinement carrier.",
        )

    # 2) formation + soft aliases only (no new physical ids)
    alias_count = 0
    for src, dst in {**FORMATION_ALIASES, **SOFT_ALIASES}.items():
        if dst not in reg and dst not in existing_bulk:
            raise SystemExit(f"alias target missing: {src} -> {dst}")
        cat = "formation" if src in FORMATION_ALIASES else "material"
        files = (
            ["formation_catalog.json"]
            if src in FORMATION_ALIASES
            else ["refinement_system.json", "merchant_shops.json"]
        )
        upsert_idmap(
            idmap,
            src,
            f"seeking_immortals:{dst}",
            cat,
            files,
            f"Wave 0.1.448 alias {src} -> {dst}",
            status="implemented",
        )
        alias_count += 1

    BULK.write_text(json.dumps(bulk, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    ZH.write_text(json.dumps(zh, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    EN.write_text(json.dumps(en, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    IDMAP.write_text(json.dumps(idmap, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    # version bump 0.1.447 -> 0.1.448
    gp = ROOT / "gradle.properties"
    raw = gp.read_text(encoding="utf-8")
    if "mod_version=0.1.447" in raw:
        gp.write_text(raw.replace("mod_version=0.1.447", "mod_version=0.1.448"), encoding="utf-8")
    elif "mod_version=0.1.448" not in raw:
        raise SystemExit("unexpected mod_version; expected 0.1.447")

    print("aliases", alias_count)
    print("added", len(added), added)
    print("bulk total", len(bulk["items"]))
    print("textures created", tex_n)
    print("version", [ln for ln in gp.read_text(encoding="utf-8").splitlines() if "mod_version" in ln][0])


if __name__ == "__main__":
    main()
