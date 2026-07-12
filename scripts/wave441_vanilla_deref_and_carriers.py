#!/usr/bin/env python3
"""Wave 0.1.441: deref vanilla models + prepare carrier assets."""
from __future__ import annotations

import hashlib
import json
import re
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(".")
ASSETS = ROOT / "src/main/resources/assets/seeking_immortals"
ITEM_MODELS = ASSETS / "models/item"
BLOCK_MODELS = ASSETS / "models/block"
ITEM_TEX = ASSETS / "textures/item"
BLOCK_TEX = ASSETS / "textures/block"

HANDHELD_IDS = {
    "azure_ice_sword",
    "green_bamboo_cloud_sword",
    "green_bamboo_leaf_sword",
    "lengyue_blade",
    "lieyang_short_sword",
    "peerless_flying_knives",
    "scarlet_dragon_blade",
    "talisman_treasure_fire_spear",
    "flying_sword_low",
    "natal_sword_embryo",
    "silver_giant_sword",
    "potian_shovel",
}

TEMPLATE_PARENTS = {
    "minecraft:item/generated",
    "minecraft:item/handheld",
    "minecraft:item/handheld_rod",
    "item/generated",
    "item/handheld",
}


def ensure(path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)


def deterministic_color(name: str):
    digest = hashlib.md5(name.encode()).hexdigest()
    return (
        80 + (int(digest[0:2], 16) % 160),
        80 + (int(digest[2:4], 16) % 160),
        80 + (int(digest[4:6], 16) % 160),
        255,
    )


def gradient(size, c1, c2):
    w, h = size
    im = Image.new("RGBA", size)
    px = im.load()
    for y in range(h):
        for x in range(w):
            t = y / max(1, h - 1)
            px[x, y] = (
                int(c1[0] * (1 - t) + c2[0] * t),
                int(c1[1] * (1 - t) + c2[1] * t),
                int(c1[2] * (1 - t) + c2[2] * t),
                255,
            )
    return im


def make_icon(name: str, kind: str = "item", size: int = 64) -> Image.Image:
    base = deterministic_color(name)
    im = gradient((size, size), (base[0] // 2, base[1] // 2, base[2] // 2, 255), base)
    d = ImageDraw.Draw(im)
    d.rectangle([1, 1, size - 2, size - 2], outline=(255, 240, 200, 220), width=2)
    if kind == "artifact":
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
    elif kind == "ore":
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
        d.rounded_rectangle(
            [size * 0.2, size * 0.2, size * 0.8, size * 0.8],
            radius=8,
            fill=base,
            outline=(255, 255, 255, 180),
        )
    label = "".join(ch for ch in name if ch.isalnum())[:2].upper() or "SI"
    try:
        font = ImageFont.load_default()
    except Exception:
        font = None
    d.text((size * 0.35, size * 0.4), label[:2], fill=(255, 255, 255, 230), font=font)
    return im


def save_png(im: Image.Image, path: Path) -> None:
    ensure(path)
    im.save(path, format="PNG")


def rewrite_item_models() -> tuple[int, int]:
    changed = 0
    skipped = 0
    for path in sorted(ITEM_MODELS.glob("*.json")):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except Exception:
            continue
        stem = path.stem
        parent = data.get("parent", "")
        textures = data.get("textures") or {}
        layer0 = textures.get("layer0", "")
        png = ITEM_TEX / f"{stem}.png"
        need = False
        new_parent = parent
        new_layer = layer0

        specific_parent = (
            isinstance(parent, str)
            and parent.startswith("minecraft:item/")
            and parent not in TEMPLATE_PARENTS
        )
        mc_layer = isinstance(layer0, str) and layer0.startswith("minecraft:")

        if specific_parent or mc_layer:
            if not png.exists():
                skipped += 1
                continue
            need = True
            if stem in HANDHELD_IDS or "sword" in stem or parent.endswith("_sword"):
                new_parent = "minecraft:item/handheld"
            else:
                new_parent = "minecraft:item/generated"
            new_layer = f"seeking_immortals:item/{stem}"

        if need:
            data["parent"] = new_parent
            data["textures"] = {"layer0": new_layer}
            path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
            changed += 1
    return changed, skipped


def rewrite_block_models() -> int:
    changed = 0
    for path in sorted(BLOCK_MODELS.glob("*.json")):
        stem = path.stem
        if stem in {"spirit_gathering_array", "spirit_ore", "alchemy_furnace"}:
            continue
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except Exception:
            continue
        textures = data.get("textures")
        if not isinstance(textures, dict):
            continue
        own = f"seeking_immortals:block/{stem}"
        own_png = BLOCK_TEX / f"{stem}.png"
        if not own_png.exists():
            continue
        dirty = False
        new_tex = {}
        for key, val in textures.items():
            if not isinstance(val, str):
                new_tex[key] = val
                continue
            if val in {
                "seeking_immortals:block/spirit_gathering_array",
                "seeking_immortals:block/spirit_ore",
                "seeking_immortals:block/alchemy_furnace",
            } or val.startswith("minecraft:"):
                new_tex[key] = own
                dirty = True
            else:
                new_tex[key] = val
        if dirty:
            data["textures"] = new_tex
            path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
            changed += 1
    return changed


def audit_item_models() -> dict:
    layer0_mc = 0
    parent_specific = 0
    for path in ITEM_MODELS.glob("*.json"):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except Exception:
            continue
        parent = data.get("parent", "")
        layer0 = (data.get("textures") or {}).get("layer0", "")
        if isinstance(layer0, str) and layer0.startswith("minecraft:"):
            layer0_mc += 1
        if (
            isinstance(parent, str)
            and parent.startswith("minecraft:item/")
            and parent not in TEMPLATE_PARENTS
        ):
            parent_specific += 1
    return {"layer0_mc": layer0_mc, "parent_specific": parent_specific}


def registered_ids() -> set[str]:
    text = Path("src/main/java/com/xunxian/seekingimmortals/registry/ModItems.java").read_text(
        encoding="utf-8"
    )
    ids = set(re.findall(r'register\(\s*"([a-z0-9_]+)"', text))
    ids |= set(re.findall(r'registerArtifact\(\s*"([a-z0-9_]+)"', text))
    ids |= set(re.findall(r'registerMaterial\(\s*"([a-z0-9_]+)"', text))
    return ids


def missing_artifacts() -> list[str]:
    data = json.loads(
        Path("src/main/resources/data/seeking_immortals/artifacts/refinement_recipes.json").read_text(
            encoding="utf-8"
        )
    )
    arts = {
        r["artifact_id"].split(":")[-1]
        for r in data["recipes"]
        if isinstance(r, dict) and r.get("artifact_id")
    }
    reg = registered_ids()
    return sorted(a for a in arts if a not in reg)


def ensure_item_assets(ids: list[str], kind: str) -> int:
    created = 0
    for item_id in ids:
        png = ITEM_TEX / f"{item_id}.png"
        if not png.exists():
            save_png(make_icon(item_id, kind=kind), png)
            created += 1
        model = ITEM_MODELS / f"{item_id}.json"
        parent = "minecraft:item/handheld" if item_id in HANDHELD_IDS else "minecraft:item/generated"
        model.write_text(
            json.dumps(
                {
                    "parent": parent,
                    "textures": {"layer0": f"seeking_immortals:item/{item_id}"},
                },
                indent=2,
            )
            + "\n",
            encoding="utf-8",
        )
    return created


def main() -> None:
    item_changed, item_skipped = rewrite_item_models()
    block_changed = rewrite_block_models()
    print("item models rewritten", item_changed, "skipped_no_png", item_skipped)
    print("block models rewritten", block_changed)
    print("audit after", audit_item_models())

    materials = ["wind_feather", "beast_hide", "turtle_shell", "poison_sac"]
    print("material tex/models", ensure_item_assets(materials, "item"))

    missing = missing_artifacts()
    print("missing artifacts", len(missing))
    print(missing)
    print("artifact tex/models", ensure_item_assets(missing, "artifact"))

    # write list for java generation
    Path("project_docs/_tmp_missing_artifacts_0.1.441.json").write_text(
        json.dumps(missing, indent=2) + "\n", encoding="utf-8"
    )


if __name__ == "__main__":
    main()
