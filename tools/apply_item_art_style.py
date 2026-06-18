#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Batch rewrite tools/items_mvp.json prompts into a unified crystal pixel-art style.

Usage:
  python tools/apply_item_art_style.py
  python tools/apply_item_art_style.py --input tools/items_mvp.json --output tools/items_mvp.json
"""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path


STYLE_SUFFIX = (
    "pixel art fantasy RPG inventory icon, "
    "same style as a Minecraft mod mineral item sprite, "
    "transparent background, centered single object, "
    "chunky pixel clusters, thick dark outline, "
    "bright blocky highlights, high contrast, "
    "limited color palette, crisp hard pixel edges, "
    "readable at 32x32 and 64x64, "
    "no realistic rendering, no smooth 3D, "
    "no text, no watermark, no UI, no hands."
)

NEGATIVE_SUFFIX = (
    "Avoid: realistic rendering, smooth gradients, complex background, "
    "tiny unreadable details, scene, character, excessive glow, text labels."
)


def classify_item(item_id: str, name: str, prompt: str) -> str:
    text = f"{item_id} {name} {prompt}".lower()

    if any(k in text for k in ["pill", "丹", "elixir"]):
        return "pill"

    if any(k in text for k in ["stone", "crystal", "ore", "灵石", "矿", "晶", "spirit_stone"]):
        return "crystal"

    if any(k in text for k in ["grass", "herb", "leaf", "flower", "草", "灵芝", "芝"]):
        return "herb"

    if any(k in text for k in ["vial", "bottle", "小瓶", "瓶"]):
        return "vial"

    if any(k in text for k in ["furnace", "cauldron", "丹炉", "炉"]):
        return "furnace"

    if any(k in text for k in ["tablet", "stone tablet", "石板"]):
        return "tablet"

    if any(k in text for k in ["scroll", "卷轴", "recipe"]):
        return "scroll"

    if any(k in text for k in ["talisman", "符"]):
        return "talisman"

    return "generic"


def color_hint(item_id: str, name: str, prompt: str) -> str:
    text = f"{item_id} {name} {prompt}".lower()

    if any(k in text for k in ["foundation", "筑基", "gold", "黄金", "上品", "high_grade"]):
        return "gold, red, orange and warm yellow"

    if any(k in text for k in ["qi_condensing", "凝气", "blue", "cyan", "回灵", "mana"]):
        return "pale blue, cyan, white and dark blue outline"

    if any(k in text for k in ["mind", "stabilizing", "稳神", "jade", "green", "碧", "定神"]):
        return "jade green, emerald, white and dark green outline"

    if any(k in text for k in ["waste", "废"]):
        return "dark gray, brown, muted purple and black outline"

    if any(k in text for k in ["blood", "血", "red"]):
        return "crimson red, orange, dark red and black outline"

    if any(k in text for k in ["spirit_stone", "灵石", "crystal"]):
        return "cyan, blue, green, gold or violet depending on item rarity"

    return "limited item-specific palette with strong dark outline"


def subject_phrase(kind: str, item_id: str, name: str, old_prompt: str) -> str:
    base = name.strip() or item_id

    if kind == "pill":
        return f"a crystalline cultivation pill representing {base}, simple round elixir shape"

    if kind == "crystal":
        return f"a spirit stone crystal cluster representing {base}, jagged mineral silhouette"

    if kind == "herb":
        return f"a spiritual herb or crystal leaf representing {base}, simple readable plant silhouette"

    if kind == "vial":
        return f"a small mysterious jade vial representing {base}, filled with glowing spiritual liquid"

    if kind == "furnace":
        return f"a small bronze alchemy furnace cauldron representing {base}, simple chunky furnace shape"

    if kind == "tablet":
        return f"an ancient stone tablet representing {base}, with simple glowing elemental marks"

    if kind == "scroll":
        return f"an ancient parchment scroll representing {base}, simple rolled scroll shape with a small seal"

    if kind == "talisman":
        return f"a paper talisman representing {base}, simple rectangular charm silhouette"

    # fallback: preserve some old meaning
    cleaned = old_prompt.strip()
    cleaned = re.sub(r"\s+", " ", cleaned)
    if cleaned:
        return cleaned[:180]
    return f"a xianxia cultivation item representing {base}"


def rewrite_prompt(item: dict) -> str:
    item_id = str(item.get("id", ""))
    name = str(item.get("name", ""))
    old_prompt = str(item.get("prompt", ""))

    kind = classify_item(item_id, name, old_prompt)
    subject = subject_phrase(kind, item_id, name, old_prompt)
    colors = color_hint(item_id, name, old_prompt)

    glow = ""
    rarity_text = f"{item_id} {name}".lower()
    if any(k in rarity_text for k in ["foundation", "筑基", "high", "上品", "top", "极品", "rare"]):
        glow = "subtle clean magical glow aura, "
    elif kind in ["crystal", "vial"]:
        glow = "small clean magical glow, "

    prompt = (
        f"A clear {STYLE_SUFFIX} "
        f"Subject: {subject}. "
        f"Visual details: front view, object fills 80 percent of canvas, "
        f"{glow}bright blocky highlights, thick dark outline, simple readable silhouette. "
        f"Main colors: {colors}. "
        f"{NEGATIVE_SUFFIX}"
    )

    # Clean accidental repeated spaces
    prompt = re.sub(r"\s+", " ", prompt).strip()
    return prompt


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", type=Path, default=Path("tools/items_mvp.json"))
    parser.add_argument("--output", type=Path, default=Path("tools/items_mvp.json"))
    parser.add_argument("--backup", action="store_true", help="Write a .bak file before overwriting")
    args = parser.parse_args()

    if not args.input.exists():
        raise FileNotFoundError(f"Input file not found: {args.input}")

    data = json.loads(args.input.read_text(encoding="utf-8"))
    if not isinstance(data, list):
        raise ValueError("Input JSON must be an array")

    if args.backup and args.output.exists():
        backup_path = args.output.with_suffix(args.output.suffix + ".bak")
        backup_path.write_text(args.output.read_text(encoding="utf-8"), encoding="utf-8")
        print(f"Backup written: {backup_path}")

    rewritten = []
    for item in data:
        if not isinstance(item, dict):
            continue

        new_item = dict(item)
        new_item["prompt"] = rewrite_prompt(new_item)
        rewritten.append(new_item)

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(
        json.dumps(rewritten, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8"
    )

    print(f"Updated: {args.output}")
    print(f"Items  : {len(rewritten)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
