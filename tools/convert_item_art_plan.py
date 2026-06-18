#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Convert docs/item-art-plan.md to tools/items_mvp.json.

Expected Markdown block format:

## 凝气丹

- item_id: qi_condensing_pill
- 中文名: 凝气丹
- output: qi_condensing_pill.png
- prompt: A clear Minecraft pixel art item icon...

The output JSON format:

[
  {
    "id": "qi_condensing_pill",
    "name": "凝气丹",
    "prompt": "A clear Minecraft pixel art item icon..."
  }
]
"""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path
from typing import Dict, List


def slugify(value: str) -> str:
    value = value.strip().lower()
    value = value.replace("-", "_")
    value = re.sub(r"[^a-z0-9_]+", "_", value)
    value = re.sub(r"_+", "_", value)
    return value.strip("_")


def normalize_key(key: str) -> str:
    key = key.strip().lower()
    aliases = {
        "item_id": "id",
        "item id": "id",
        "id": "id",
        "物品id": "id",
        "物品_id": "id",

        "中文名": "name",
        "name": "name",
        "zh_name": "name",
        "物品名": "name",

        "prompt": "prompt",
        "提示词": "prompt",
        "英文提示词": "prompt",

        "output": "output",
        "输出": "output",
        "filename": "output",
        "file": "output",
    }
    return aliases.get(key, key)


def parse_markdown_blocks(text: str) -> List[Dict[str, str]]:
    """
    Parse Markdown headings and bullet key-value pairs.
    """
    items: List[Dict[str, str]] = []
    current: Dict[str, str] | None = None

    lines = text.splitlines()

    for raw_line in lines:
        line = raw_line.strip()

        if not line:
            continue

        # New item section: ## 凝气丹
        heading_match = re.match(r"^#{2,6}\s+(.+?)\s*$", line)
        if heading_match:
            if current:
                items.append(current)

            title = heading_match.group(1).strip()
            current = {
                "section_title": title
            }
            continue

        if current is None:
            continue

        # Bullet key-value:
        # - item_id: qi_condensing_pill
        # * prompt：xxx
        kv_match = re.match(r"^[\-\*]\s*([^:：]+)\s*[:：]\s*(.+?)\s*$", line)
        if kv_match:
            key = normalize_key(kv_match.group(1))
            value = kv_match.group(2).strip()
            current[key] = value
            continue

        # Continue previous prompt line if it looks like indented prose
        if "prompt" in current and not re.match(r"^[\-\*]\s*", line):
            current["prompt"] += " " + line.strip()

    if current:
        items.append(current)

    return items


def clean_item(raw: Dict[str, str]) -> Dict[str, str] | None:
    item_id = raw.get("id", "").strip()
    name = raw.get("name", "").strip()
    prompt = raw.get("prompt", "").strip()

    section_title = raw.get("section_title", "").strip()

    # If id is missing, derive from output filename or section title.
    if not item_id:
        output = raw.get("output", "").strip()
        if output:
            item_id = Path(output).stem
        else:
            item_id = slugify(section_title)

    # If name is missing, use section title.
    if not name:
        name = section_title

    if not item_id or not prompt:
        return None

    item_id = slugify(item_id)

    return {
        "id": item_id,
        "name": name,
        "prompt": prompt,
    }


def convert(input_path: Path, output_path: Path, pretty: bool = True) -> None:
    text = input_path.read_text(encoding="utf-8")
    blocks = parse_markdown_blocks(text)

    result: List[Dict[str, str]] = []
    skipped: List[Dict[str, str]] = []

    for block in blocks:
        item = clean_item(block)
        if item:
            result.append(item)
        else:
            skipped.append(block)

    if not result:
        raise RuntimeError(
            f"No valid items found in {input_path}. "
            "Expected sections with item_id/name/prompt fields."
        )

    output_path.parent.mkdir(parents=True, exist_ok=True)

    if pretty:
        output_path.write_text(
            json.dumps(result, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8"
        )
    else:
        output_path.write_text(
            json.dumps(result, ensure_ascii=False, separators=(",", ":")),
            encoding="utf-8"
        )

    print(f"Converted: {input_path}")
    print(f"Output   : {output_path}")
    print(f"Items    : {len(result)}")

    if skipped:
        print(f"Skipped  : {len(skipped)} incomplete block(s)")
        for block in skipped:
            print(f"  - {block.get('section_title', '<unknown>')}")


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Convert item-art-plan.md to items_mvp.json"
    )
    parser.add_argument(
        "--input",
        type=Path,
        default=Path("docs/item-art-plan.md"),
        help="Input Markdown file. Default: docs/item-art-plan.md",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("tools/items_mvp.json"),
        help="Output JSON file. Default: tools/items_mvp.json",
    )
    parser.add_argument(
        "--compact",
        action="store_true",
        help="Write compact JSON instead of pretty JSON.",
    )

    args = parser.parse_args()

    if not args.input.exists():
        raise FileNotFoundError(f"Input file not found: {args.input}")

    convert(args.input, args.output, pretty=not args.compact)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())