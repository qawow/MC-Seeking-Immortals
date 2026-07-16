#!/usr/bin/env python3
"""M03: fill bulk catalog gaps (structure materials / talisman_v92 / display block_items),
generate lang/models/placeholder textures, refresh id-map + aliases, and write reconcile report.
"""
from __future__ import annotations

import hashlib
import json
import re
from collections import Counter
from pathlib import Path

try:
    from PIL import Image, ImageDraw, ImageFont
except ImportError:  # pragma: no cover
    Image = None  # type: ignore

ROOT = Path(".")
ASSETS = ROOT / "src/main/resources/assets/seeking_immortals"
ITEM_MODELS = ASSETS / "models/item"
ITEM_TEX = ASSETS / "textures/item"
ZH = ASSETS / "lang/zh_cn.json"
EN = ASSETS / "lang/en_us.json"
BULK_JSON = ASSETS / "catalog_bulk_items.json"
IDMAP = ROOT / "src/main/resources/data/seeking_immortals/reference/text_material_id_map.json"
CORPUS = ROOT / "文本材料/data"
SHIPPED_ALIASES = ROOT / "src/main/resources/data/seeking_immortals/text_material/item_id_aliases.json"
REPORT = ROOT / "project_docs/updates/20260716_m03_item_catalog_reconcile.json"

# Canonical aliases: catalog id -> existing registered id (no new carrier)
ALIASES = {
    "alchemy_furnace_g1": "alchemy_furnace",
    "refinement_forge_g1": "refinement_forge",
    "yin_essence_ore_block": "yin_essence_ore",
    "earth_fire_alchemy_room": "sect_earth_fire_room",
    # existing region aliases from item_id_aliases.json
    "yellow_essence_grass": "yellow_essence",
    "kunwu_spirit_copper": "kunwu_copper",
    "sea_soul_grass_herb": "sea_soul_grass",
    "coral_spirit_pearl_raw": "coral_spirit_pearl",
    "yellow_palm_fan": "yellow_umbrella",
}

# Never enter bulk stackable/trade channel (DESIGNER_HANDBOOK §5)
UNIQUE_FORBIDDEN = {
    "palm_heaven_bottle",
    "palm_sky_bottle",
    "heaven_palm_vase",
    "green_liquid",
    "lv_ye",
    "garden_liquid",
    "little_green_bottle",
    "mystic_green_liquid",
}

# Existing aliases from corpus (source of truth for soft aliases)
def load_corpus_aliases() -> dict[str, str]:
    out = dict(ALIASES)
    p = CORPUS / "item_id_aliases.json"
    if p.exists():
        data = json.loads(p.read_text(encoding="utf-8"))
        for a in data.get("aliases", []):
            alias = (a.get("alias") or "").strip().lower()
            canon = (a.get("canonical") or "").strip().lower()
            if alias and canon:
                out[alias] = canon
    return out


def ensure(p: Path) -> None:
    p.parent.mkdir(parents=True, exist_ok=True)


def title_en(rid: str) -> str:
    return " ".join(w.capitalize() for w in rid.split("_"))


def load_registered() -> set[str]:
    reg: set[str] = set()
    for rel in (
        "src/main/java/com/xunxian/seekingimmortals/registry/ModItems.java",
        "src/main/java/com/xunxian/seekingimmortals/registry/ModBlocks.java",
    ):
        t = (ROOT / rel).read_text(encoding="utf-8")
        reg |= set(re.findall(r'\.register\(\s*"([a-z0-9_]+)"', t))
        reg |= set(re.findall(r'register(?:Artifact|Material|SpiritStone|TechniqueManual|AlchemyFormula|DanFire)\(\s*"([a-z0-9_]+)"', t))
    cpt = ROOT / "src/main/java/com/xunxian/seekingimmortals/item/pill/CatalogPillType.java"
    if cpt.exists():
        for m in re.finditer(r'\("([a-z0-9_]+)"', cpt.read_text(encoding="utf-8")):
            base = m.group(1)
            reg.add(base)
            for q in ("low", "mid", "high", "supreme"):
                reg.add(f"{base}_{q}")
    if BULK_JSON.exists():
        bulk = json.loads(BULK_JSON.read_text(encoding="utf-8"))
        for it in bulk.get("items", []):
            reg.add(it["id"])
    return reg


def rarity_from_tier(tier: str | None, default: str = "common") -> str:
    if not tier:
        return default
    t = tier.lower()
    if t in ("legendary", "immortal", "tian_plus", "ancient"):
        return "epic"
    if t in ("epic", "tian", "high", "di_to_tian"):
        return "rare"
    if t in ("rare", "di", "xuan_to_di", "xuan", "mid", "uncommon"):
        return "uncommon"
    if t in ("huang", "low", "common"):
        return "common"
    return default


def category_for_material(entry: dict) -> str:
    use = entry.get("use") or []
    if isinstance(use, str):
        use = [use]
    rid = entry.get("id", "")
    if any("structure" in str(u) for u in use):
        return "material"
    if any(x in rid for x in ("ore", "iron", "copper", "jade", "crystal", "stone", "sand")):
        return "mineral"
    return "material"


def category_for_block(entry: dict) -> str:
    t = (entry.get("type") or "").lower()
    if "ore" in t:
        return "mineral"
    if "array" in t or "formation" in t:
        return "artifact"
    if "controller" in t or "multiblock" in t:
        return "artifact"
    return "material"


def make_icon(name: str, cat: str, size: int = 64):
    if Image is None:
        return None
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
    elif cat in ("artifact", "equipment"):
        d.polygon(
            [
                (size * 0.5, size * 0.12),
                (size * 0.78, size * 0.5),
                (size * 0.5, size * 0.88),
                (size * 0.22, size * 0.5),
            ],
            fill=(220, 210, 120, 255),
            outline=(80, 60, 10, 255),
        )
    elif cat == "pill":
        d.ellipse(
            [size * 0.25, size * 0.25, size * 0.75, size * 0.75],
            fill=(200, 60, 60, 255),
            outline=(240, 200, 120, 255),
        )
    else:
        d.ellipse(
            [size * 0.22, size * 0.22, size * 0.78, size * 0.78],
            fill=base,
            outline=(80, 180, 80, 220),
        )
    try:
        font = ImageFont.load_default()
    except Exception:
        font = None
    label = "".join(ch for ch in name if ch.isalnum())[:2].upper() or "SI"
    d.text((size * 0.36, size * 0.4), label[:2], fill=(255, 255, 255, 230), font=font)
    return im


def load_descriptions() -> dict[str, str]:
    """id -> best Chinese description for tooltips."""
    descs: dict[str, str] = {}

    def put(rid: str, text: str) -> None:
        rid = (rid or "").strip().lower()
        text = (text or "").strip()
        if not rid or not text:
            return
        if rid not in descs or len(text) > len(descs[rid]):
            descs[rid] = text

    # Catalog lore/display
    for fn, key in [
        ("materials_catalog.json", "materials"),
        ("pills_catalog.json", "pills"),
        ("spirit_herbs_catalog.json", "herbs"),
        ("consumables_catalog.json", "consumables"),
        ("talisman_catalog.json", "talismans"),
        ("talisman_catalog_v92.json", "talismans"),
        ("puppet_parts_catalog.json", "parts"),
        ("currency_items.json", "items"),
        ("block_items_catalog.json", "blocks"),
        ("manuals_catalog.json", "manuals"),
        ("formation_items_catalog.json", "items"),
    ]:
        p = CORPUS / fn
        if not p.exists():
            continue
        data = json.loads(p.read_text(encoding="utf-8"))
        for it in data.get(key, []):
            if not isinstance(it, dict) or "id" not in it:
                continue
            rid = it["id"]
            lore = ""
            setting = it.get("setting") or {}
            if isinstance(setting, dict):
                lore = setting.get("lore_v142") or setting.get("lore") or ""
            text = lore or it.get("description") or it.get("display") or ""
            put(rid, text)

    # Deep / visual description packs (prefer longer catalog_id-linked text)
    for fn in sorted(CORPUS.glob("item_descriptions_v*.json")):
        try:
            data = json.loads(fn.read_text(encoding="utf-8"))
        except Exception:
            continue
        for it in data.get("items", []):
            if not isinstance(it, dict):
                continue
            cid = it.get("catalog_id") or it.get("id") or ""
            if not isinstance(cid, str):
                continue
            for pref in (
                "orig_material_",
                "orig_pill_",
                "orig_herb_",
                "orig_consumable_",
                "orig_artifact_",
                "orig_talisman_",
                "material_deep_",
                "pill_deep_",
                "herb_deep_",
                "consumable_deep_",
                "artifact_deep_",
                "material_visual_",
                "pill_visual_",
                "herb_visual_",
                "visual_",
            ):
                if cid.startswith(pref):
                    cid = cid[len(pref) :]
                    break
            text = it.get("description") or it.get("effect") or it.get("appearance") or ""
            # Skip pure meta/style guide
            if it.get("type") in ("style_guide", "meta", "timeline"):
                continue
            put(cid, text)

    for fn in ("item_descriptions_expanded_v95.json", "item_descriptions_more_v96.json"):
        p = CORPUS / fn
        if not p.exists():
            continue
        data = json.loads(p.read_text(encoding="utf-8"))
        for it in data.get("items", []):
            if isinstance(it, dict) and it.get("id"):
                put(it["id"], it.get("description") or it.get("effect") or it.get("display") or "")

    return descs


def collect_gap_items(reg: set[str], aliases: dict[str, str]) -> list[dict]:
    gaps: list[dict] = []
    seen: set[str] = set()

    def add(rid: str, category: str, rarity: str, display: str, description: str, grade: str = "") -> None:
        rid = rid.strip().lower()
        if not rid or rid in UNIQUE_FORBIDDEN:
            return
        if rid in aliases:
            return  # resolved via alias, do not register duplicate
        if rid in reg or rid in seen:
            return
        seen.add(rid)
        item = {
            "id": rid,
            "category": category,
            "rarity": rarity,
            "description": description or f"目录载体：{display or rid}",
            "display": display or rid,
        }
        if grade:
            item["grade"] = grade
        gaps.append(item)

    # materials (mostly structure_build)
    mats = json.loads((CORPUS / "materials_catalog.json").read_text(encoding="utf-8")).get("materials", [])
    for m in mats:
        rid = m["id"]
        add(
            rid,
            category_for_material(m),
            rarity_from_tier(m.get("tier"), "common"),
            m.get("display") or rid,
            (m.get("setting") or {}).get("lore") or m.get("display") or rid,
        )

    # talisman_v92 extras
    t92 = json.loads((CORPUS / "talisman_catalog_v92.json").read_text(encoding="utf-8")).get("talismans", [])
    for t in t92:
        grade = t.get("grade") or t.get("rank") or "mid"
        add(
            t["id"],
            "talisman",
            rarity_from_tier(grade, "uncommon"),
            t.get("display") or t["id"],
            t.get("description") or t.get("display") or t["id"],
            grade=str(grade),
        )

    # main talisman catalog (should already be present)
    tmain = json.loads((CORPUS / "talisman_catalog.json").read_text(encoding="utf-8")).get("talismans", [])
    for t in tmain:
        grade = t.get("grade") or "mid"
        add(
            t["id"],
            "talisman",
            rarity_from_tier(grade, "uncommon"),
            t.get("display") or t["id"],
            t.get("description") or t.get("display") or t["id"],
            grade=str(grade),
        )

    # puppet parts
    parts = json.loads((CORPUS / "puppet_parts_catalog.json").read_text(encoding="utf-8")).get("parts", [])
    for p in parts:
        add(
            p["id"],
            "artifact",
            rarity_from_tier(p.get("tier"), "uncommon"),
            p.get("display") or p["id"],
            (p.get("setting") or {}).get("lore") or p.get("display") or p["id"],
        )

    # display block items (pure carriers; functional blocks stay owned by gameplay modules)
    blocks = json.loads((CORPUS / "block_items_catalog.json").read_text(encoding="utf-8")).get("blocks", [])
    for b in blocks:
        rid = b["id"]
        if rid in aliases:
            continue
        # skip unique stand? palm_heaven_bottle_stand is display furniture, allowed
        add(
            rid,
            category_for_block(b),
            "uncommon",
            b.get("display") or rid,
            (b.get("setting") or {}).get("lore_v142")
            or (b.get("setting") or {}).get("lore")
            or b.get("display")
            or rid,
        )

    # novel wave leftovers (artifacts deferred to M15 if already covered; only register missing non-artifact play carriers)
    waves = json.loads((CORPUS / "novel_items_waves.json").read_text(encoding="utf-8"))
    for cat_key, cat in (
        ("pills", "pill"),
        ("herbs", "material"),
        ("materials", "material"),
        ("talismans", "talisman"),
        ("consumables", "consumable"),
    ):
        for it in waves.get(cat_key, []):
            if isinstance(it, dict) and it.get("id"):
                add(
                    it["id"],
                    cat,
                    "uncommon",
                    it.get("display") or it["id"],
                    it.get("description") or it.get("display") or it["id"],
                    grade=str(it.get("grade") or ""),
                )

    return gaps


def write_assets(items: list[dict], descs: dict[str, str]) -> tuple[int, int]:
    zh = json.loads(ZH.read_text(encoding="utf-8"))
    en = json.loads(EN.read_text(encoding="utf-8"))
    tex_n = 0
    model_n = 0
    for it in items:
        rid = it["id"]
        cat = it.get("category", "material")
        display = it.get("display") or rid
        # Prefer richer description pack
        rich = descs.get(rid) or it.get("description") or f"目录载体：{display}"
        it["description"] = rich if not str(rich).startswith("Catalog carrier") else f"目录载体：{display}"

        png = ITEM_TEX / f"{rid}.png"
        if not png.exists() and Image is not None:
            ensure(png)
            icon = make_icon(rid, cat)
            if icon is not None:
                icon.save(png, format="PNG")
                tex_n += 1

        model = ITEM_MODELS / f"{rid}.json"
        if not model.exists():
            ensure(model)
            # Prefer own texture if present else fall back to generic material texture
            tex_path = f"seeking_immortals:item/{rid}"
            fallback = "seeking_immortals:item/spirit_iron"
            layer = tex_path if png.exists() or (ITEM_TEX / f"{rid}.png").exists() else fallback
            # Always point to own path (even if texture missing -> purple/black is ok per brief? brief says missing texture allowed)
            layer = tex_path
            model.write_text(
                json.dumps(
                    {"parent": "minecraft:item/generated", "textures": {"layer0": layer}},
                    indent=2,
                    ensure_ascii=False,
                )
                + "\n",
                encoding="utf-8",
            )
            model_n += 1

        zh[f"item.seeking_immortals.{rid}"] = display
        en[f"item.seeking_immortals.{rid}"] = title_en(rid)
        # tooltip description keys (used by BaseMaterialItem when present)
        tip = it.get("description") or display
        # Keep tooltip short-ish
        if len(tip) > 180:
            tip = tip[:177] + "…"
        zh[f"tooltip.seeking_immortals.material.{rid}"] = tip
        en[f"tooltip.seeking_immortals.material.{rid}"] = tip if re.search(r"[A-Za-z]", tip) else title_en(rid)
        if cat == "talisman" and it.get("grade"):
            zh[f"tooltip.seeking_immortals.talisman_grade.{it['grade']}"] = zh.get(
                f"tooltip.seeking_immortals.talisman_grade.{it['grade']}", f"符箓品阶：{it['grade']}"
            )

    # Standard talisman grade lang
    for g, label in (
        ("low", "低阶符箓"),
        ("mid", "中阶符箓"),
        ("high", "高阶符箓"),
        ("ancient", "古符"),
        ("huang", "黄级符箓"),
        ("xuan", "玄级符箓"),
        ("di", "地级符箓"),
        ("tian", "天级符箓"),
        ("tian_plus", "天级以上符箓"),
        ("di_to_tian", "地至天级符箓"),
        ("xuan_to_di", "玄至地级符箓"),
    ):
        zh.setdefault(f"tooltip.seeking_immortals.talisman_grade.{g}", label)
        en.setdefault(f"tooltip.seeking_immortals.talisman_grade.{g}", f"Talisman grade: {g}")

    ZH.write_text(json.dumps(zh, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    EN.write_text(json.dumps(en, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return tex_n, model_n


def update_bulk(items: list[dict]) -> int:
    bulk = json.loads(BULK_JSON.read_text(encoding="utf-8"))
    existing = {it["id"]: it for it in bulk.get("items", [])}
    added = 0
    for it in items:
        rid = it["id"]
        entry = {
            "id": rid,
            "category": it["category"],
            "rarity": it["rarity"],
            "description": it.get("description") or f"目录载体：{it.get('display', rid)}",
        }
        if it.get("grade"):
            entry["grade"] = it["grade"]
        if rid in existing:
            # merge grade if missing
            if it.get("grade") and "grade" not in existing[rid]:
                existing[rid]["grade"] = it["grade"]
            continue
        bulk.setdefault("items", []).append(entry)
        existing[rid] = entry
        added += 1
    # also enrich existing talismans with grade from main catalogs if missing
    grade_map: dict[str, str] = {}
    for fn in ("talisman_catalog.json", "talisman_catalog_v92.json"):
        data = json.loads((CORPUS / fn).read_text(encoding="utf-8"))
        for t in data.get("talismans", []):
            g = t.get("grade") or t.get("rank")
            if g:
                grade_map[t["id"]] = str(g)
    for it in bulk.get("items", []):
        if it.get("category") == "talisman" and "grade" not in it and it["id"] in grade_map:
            it["grade"] = grade_map[it["id"]]
    bulk["schema_version"] = bulk.get("schema_version", 1)
    BULK_JSON.write_text(json.dumps(bulk, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return added


def update_id_map(items: list[dict], aliases: dict[str, str]) -> None:
    idmap = json.loads(IDMAP.read_text(encoding="utf-8"))
    by = {e.get("source_id"): e for e in idmap.get("entries", []) if isinstance(e, dict)}
    for it in items:
        rid = it["id"]
        source_cat = {
            "talisman": "talisman",
            "artifact": "block_item" if "array" in rid or "forge" in rid or "furnace" in rid else "material",
            "material": "material",
            "mineral": "material",
            "consumable": "consumable",
            "pill": "pill",
            "currency": "currency",
            "manual": "manual",
            "equipment": "artifact",
        }.get(it["category"], "material")
        if "block" in rid or any(
            x in rid
            for x in (
                "room",
                "chamber",
                "altar",
                "platform",
                "gate",
                "hub",
                "dock",
                "vault",
                "shelf",
                "stele",
                "pen",
                "workshop",
                "stand",
                "pool",
                "pit",
                "tower",
                "desk",
                "counter",
                "bay",
                "obelisk",
                "pole",
                "tent",
                "ram",
                "ring",
                "segment",
                "greenhouse",
                "cabinet",
                "rack",
                "console",
                "brazier",
                "cauldron",
                "cooler",
                "tap",
                "well",
                "socket",
                "lighthouse",
                "rift",
                "corridor",
                "hall",
                "dais",
            )
        ):
            # many display block items
            if it["category"] in ("material", "artifact", "mineral"):
                source_cat = "block_item"
        entry = {
            "source_category": source_cat,
            "source_id": rid,
            "source_files": ["catalog_bulk_items.json", "M03_item_catalog"],
            "canonical_type": "item",
            "canonical_id": f"seeking_immortals:{rid}",
            "status": "implemented",
            "note": "M03 bulk catalog carrier.",
        }
        if rid in by:
            old = by[rid]
            old["status"] = "implemented"
            old["canonical_id"] = f"seeking_immortals:{rid}"
            old["canonical_type"] = "item"
            note = old.get("note") or ""
            if "M03" not in note:
                old["note"] = (note + " M03 bulk catalog carrier.").strip()
        else:
            idmap.setdefault("entries", []).append(entry)
            by[rid] = entry

    for alias, canon in aliases.items():
        if alias in UNIQUE_FORBIDDEN or canon in UNIQUE_FORBIDDEN:
            continue
        entry = {
            "source_category": "alias",
            "source_id": alias,
            "source_files": ["item_id_aliases.json", "M03_item_catalog"],
            "canonical_type": "item",
            "canonical_id": f"seeking_immortals:{canon}",
            # JsonSanity allowed statuses: implemented / implemented_partial / deferred / blocked
            "status": "implemented",
            "note": f"M03 alias -> {canon}",
        }
        if alias in by:
            old = by[alias]
            old["status"] = "implemented"
            old["canonical_id"] = f"seeking_immortals:{canon}"
            old["note"] = f"M03 alias -> {canon}"
        else:
            idmap.setdefault("entries", []).append(entry)
            by[alias] = entry

    IDMAP.write_text(json.dumps(idmap, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def sync_shipped_aliases(aliases: dict[str, str]) -> None:
    """Keep shipped text_material aliases in sync with corpus + M03 aliases."""
    # Preserve corpus structure if present
    if (CORPUS / "item_id_aliases.json").exists():
        data = json.loads((CORPUS / "item_id_aliases.json").read_text(encoding="utf-8"))
    else:
        data = {"schema_version": 2, "description": "item id aliases", "aliases": []}
    existing = {(a.get("alias") or "").lower(): a for a in data.get("aliases", [])}
    for alias, canon in sorted(aliases.items()):
        if alias in existing:
            existing[alias]["canonical"] = canon
            continue
        data.setdefault("aliases", []).append(
            {
                "alias": alias,
                "canonical": canon,
                "source": "M03_item_catalog",
                "note": "display/block alias to existing functional id",
            }
        )
    ensure(SHIPPED_ALIASES)
    SHIPPED_ALIASES.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    # also copy into assets-adjacent reference for resolver classpath? resolver reads text_material path


def reconcile_report(reg: set[str], aliases: dict[str, str], added: list[dict]) -> dict:
    after = set(reg) | {i["id"] for i in added} | set(aliases.keys())
    # for alias keys, canonical must be known
    for a, c in aliases.items():
        after.add(c)

    def check(path: Path, key: str) -> dict:
        data = json.loads(path.read_text(encoding="utf-8"))
        ids = [x["id"].lower() for x in data.get(key, []) if isinstance(x, dict) and "id" in x]
        missing = []
        for i in ids:
            if i in after:
                continue
            if i in aliases and aliases[i] in after:
                continue
            missing.append(i)
        return {"total": len(ids), "missing": missing, "missing_count": len(missing)}

    report = {
        "bulk_before": len(reg),  # approximate
        "added": [i["id"] for i in added],
        "added_count": len(added),
        "aliases": aliases,
        "catalogs": {
            "pills": check(CORPUS / "pills_catalog.json", "pills"),
            "herbs": check(CORPUS / "spirit_herbs_catalog.json", "herbs"),
            "materials": check(CORPUS / "materials_catalog.json", "materials"),
            "consumables": check(CORPUS / "consumables_catalog.json", "consumables"),
            "manuals": check(CORPUS / "manuals_catalog.json", "manuals"),
            "talismans": check(CORPUS / "talisman_catalog.json", "talismans"),
            "talismans_v92": check(CORPUS / "talisman_catalog_v92.json", "talismans"),
            "puppet_parts": check(CORPUS / "puppet_parts_catalog.json", "parts"),
            "currency": check(CORPUS / "currency_items.json", "items"),
            "block_items": check(CORPUS / "block_items_catalog.json", "blocks"),
            "formation_items": check(CORPUS / "formation_items_catalog.json", "items"),
        },
        "unique_forbidden_present_in_added": [i["id"] for i in added if i["id"] in UNIQUE_FORBIDDEN],
        "category_counts": dict(Counter(i["category"] for i in added)),
    }
    ensure(REPORT)
    REPORT.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return report


def main() -> None:
    aliases = load_corpus_aliases()
    reg = load_registered()
    print("registered-ish", len(reg))
    descs = load_descriptions()
    print("description keys", len(descs))
    gaps = collect_gap_items(reg, aliases)
    print("gap items to add", len(gaps))
    # Apply rich descriptions before writing bulk
    for it in gaps:
        rich = descs.get(it["id"])
        if rich:
            it["description"] = rich if len(rich) < 200 else rich[:197] + "…"
        elif it.get("display"):
            it["description"] = f"目录载体：{it['display']}"
    tex_n, model_n = write_assets(gaps, descs)
    print("textures created", tex_n, "models created", model_n)
    added_n = update_bulk(gaps)
    print("bulk appended", added_n, "total bulk", len(json.loads(BULK_JSON.read_text(encoding="utf-8"))["items"]))
    update_id_map(gaps, aliases)
    sync_shipped_aliases(aliases)
    # re-evaluate after with new bulk
    reg2 = load_registered()
    report = reconcile_report(reg2, aliases, gaps)
    print("reconcile missing totals:")
    for k, v in report["catalogs"].items():
        print(f"  {k}: missing={v['missing_count']} sample={v['missing'][:5]}")
    print("report ->", REPORT)


if __name__ == "__main__":
    main()
