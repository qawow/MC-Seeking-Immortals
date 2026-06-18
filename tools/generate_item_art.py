#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
tools/generate_item_art.py

Batch-generate Minecraft mod item textures through an OpenAI-compatible image API
(such as chatgpt2api), then convert the generated images into Minecraft-friendly
16x16 / 32x32 / 64x64 PNG textures and generate item model JSON files.

Typical usage:
  python tools/generate_item_art.py --modid xiuxian --items tools/items_mvp.json

PowerShell example:
  $env:CHATGPT2API_BASE_URL="http://localhost:8200/v1"
  $env:CHATGPT2API_API_KEY="your-api-key"
  $env:IMAGE_MODEL="gpt-image-2"
  python tools/generate_item_art.py --modid xiuxian --items tools/items_mvp.json --limit 2 --keep-raw

Input JSON format:
[
  {
    "id": "qi_condensing_pill",
    "name": "凝气丹",
    "prompt": "A pale blue cultivation pill with a soft spiritual glow..."
  }
]

Generated files:
  src/main/resources/assets/<modid>/textures/item/<item_id>.png
  src/main/resources/assets/<modid>/models/item/<item_id>.json
  generated_art/item_art_manifest.json
"""

from __future__ import annotations

import argparse
import base64
import json
import os
import re
import sys
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Dict, List, Optional

try:
    import requests
except ImportError as exc:
    print("Missing dependency: requests. Install with: pip install requests pillow", file=sys.stderr)
    raise exc

try:
    from PIL import Image
except ImportError as exc:
    print("Missing dependency: pillow. Install with: pip install requests pillow", file=sys.stderr)
    raise exc


DEFAULT_BASE_URL = os.getenv("CHATGPT2API_BASE_URL", "http://localhost:8200/v1").rstrip("/")
DEFAULT_API_KEY = os.getenv("CHATGPT2API_API_KEY", "")
DEFAULT_MODEL = os.getenv("IMAGE_MODEL", "gpt-image-2")


@dataclass
class ArtItem:
    id: str
    name: str
    prompt: str


def slugify_item_id(value: str) -> str:
    """Convert item id into a safe Minecraft resource path segment."""
    value = value.strip().lower().replace("-", "_")
    value = re.sub(r"[^a-z0-9_./-]+", "_", value)
    value = value.replace("/", "_").replace(".", "_")
    value = re.sub(r"_+", "_", value)
    return value.strip("_")


def ensure_prompt_style(prompt: str) -> str:
    """Append stable Minecraft item texture constraints unless prompt already has them."""
    constraints = (
        "\n\nStyle constraints: Minecraft mod item texture, pixel art inventory icon, "
        "transparent background, centered single object, clear silhouette, "
        "readable at 16x16 and 32x32, no text, no watermark, no UI, no hands, "
        "no photorealism, hard pixel edges, limited color palette."
    )
    lower = prompt.lower()
    if "minecraft" in lower and "pixel" in lower and "transparent" in lower:
        return prompt
    return prompt.rstrip() + constraints


def load_items(path: Path) -> List[ArtItem]:
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, list):
        raise ValueError(f"{path} must contain a JSON array")

    items: List[ArtItem] = []
    for idx, raw in enumerate(data, start=1):
        if not isinstance(raw, dict):
            raise ValueError(f"Item #{idx} must be an object")

        item_id = raw.get("id") or raw.get("item_id")
        name = raw.get("name") or raw.get("zh_name") or item_id
        prompt = raw.get("prompt")

        if not item_id or not prompt:
            raise ValueError(f"Item #{idx} must include id and prompt")

        items.append(
            ArtItem(
                id=slugify_item_id(str(item_id)),
                name=str(name),
                prompt=str(prompt),
            )
        )
    return items


def default_items() -> List[ArtItem]:
    """Fallback MVP item list if no --items file is supplied."""
    return [
        ArtItem(
            "qi_condensing_pill",
            "凝气丹",
            "A pale blue cultivation pill with a soft spiritual glow, small round elixir, xianxia fantasy theme.",
        ),
        ArtItem(
            "foundation_establishment_pill",
            "筑基丹",
            "A rare golden red cultivation pill with subtle aura rings and tiny sparks, extremely rare elixir.",
        ),
        ArtItem(
            "mind_stabilizing_pill",
            "稳神丹",
            "A calm jade green pill with white spirit mist, peaceful and stable aura, xianxia elixir.",
        ),
        ArtItem(
            "spirit_recovery_pill",
            "回灵丹",
            "A blue white mana recovery pill with sparkling spiritual particles, clean magical aura.",
        ),
        ArtItem(
            "waste_pill",
            "废丹",
            "A cracked dark gray failed alchemy pill with faint smoke, broken elixir, low value item.",
        ),
        ArtItem(
            "mystic_vial",
            "神秘小瓶",
            "A small mysterious jade vial filled with glowing blue spiritual liquid, ancient xianxia treasure.",
        ),
        ArtItem(
            "spirit_liquid",
            "灵液",
            "A single drop of glowing cyan spiritual liquid, magical dew, bright aura, cultivation resource.",
        ),
        ArtItem(
            "low_grade_spirit_stone",
            "下品灵石",
            "A small translucent pale cyan spirit stone crystal, low grade cultivation currency, glowing core.",
        ),
        ArtItem(
            "spirit_root_tablet",
            "灵根鉴定石板",
            "A small ancient stone tablet with glowing five element symbols, cultivation testing artifact.",
        ),
        ArtItem(
            "alchemy_furnace_item",
            "丹炉",
            "A small bronze alchemy furnace cauldron with subtle blue flame, xianxia crafting station item.",
        ),
    ]


def call_image_api(
    *,
    base_url: str,
    api_key: str,
    model: str,
    prompt: str,
    size: str,
    quality: str,
    timeout: int,
    output_format: Optional[str],
    transparent: bool,
) -> bytes:
    """Call OpenAI-compatible /images/generations and return image bytes."""
    url = f"{base_url}/images/generations"
    headers = {"Content-Type": "application/json"}
    if api_key:
        headers["Authorization"] = f"Bearer {api_key}"

    payload: Dict[str, Any] = {
        "model": model,
        "prompt": prompt,
        "size": size,
        "quality": quality,
        "n": 1,
    }

    # Many image APIs support these. If your proxy rejects unknown fields,
    # the script automatically retries without them.
    if output_format:
        payload["output_format"] = output_format
    if transparent:
        payload["background"] = "transparent"

    response = requests.post(url, headers=headers, json=payload, timeout=timeout)

    # Fallback for strict OpenAI-compatible gateways.
    if response.status_code >= 400 and (output_format or transparent):
        fallback_payload = {
            "model": model,
            "prompt": prompt,
            "size": size,
            "quality": quality,
            "n": 1,
        }
        response = requests.post(url, headers=headers, json=fallback_payload, timeout=timeout)

    response.raise_for_status()
    data = response.json()

    if "data" not in data or not data["data"]:
        raise RuntimeError(f"Unexpected image API response: {data}")

    first = data["data"][0]

    if first.get("b64_json"):
        return base64.b64decode(first["b64_json"])

    if first.get("url"):
        image_response = requests.get(first["url"], timeout=timeout)
        image_response.raise_for_status()
        return image_response.content

    raise RuntimeError(f"No b64_json or url found in image response: {data}")


def remove_near_white_background(img: Image.Image, threshold: int = 245) -> Image.Image:
    """Best-effort alpha removal for near-white backgrounds."""
    img = img.convert("RGBA")
    pixels = img.load()
    width, height = img.size
    for y in range(height):
        for x in range(width):
            r, g, b, a = pixels[x, y]
            if a == 0:
                continue
            if r >= threshold and g >= threshold and b >= threshold:
                pixels[x, y] = (r, g, b, 0)
    return img


def crop_to_visible_content(img: Image.Image, padding_ratio: float = 0.14) -> Image.Image:
    """Crop transparent edges, then add padding on a square canvas."""
    img = img.convert("RGBA")
    bbox = img.getbbox()
    if bbox:
        img = img.crop(bbox)

    width, height = img.size
    side = max(width, height)
    padding = max(1, int(side * padding_ratio))
    canvas_side = side + padding * 2
    canvas = Image.new("RGBA", (canvas_side, canvas_side), (0, 0, 0, 0))
    x = (canvas_side - width) // 2
    y = (canvas_side - height) // 2
    canvas.alpha_composite(img, (x, y))
    return canvas


def process_texture(
    raw_bytes: bytes,
    output_path: Path,
    raw_path: Optional[Path],
    texture_size: int,
    remove_white: bool,
    keep_raw: bool,
) -> None:
    """Convert high-res generated image into Minecraft item texture."""
    if keep_raw and raw_path:
        raw_path.parent.mkdir(parents=True, exist_ok=True)
        raw_path.write_bytes(raw_bytes)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    tmp_path = output_path.with_suffix(".tmp_input")
    tmp_path.write_bytes(raw_bytes)
    try:
        img = Image.open(tmp_path).convert("RGBA")
    finally:
        try:
            tmp_path.unlink()
        except OSError:
            pass

    if remove_white:
        img = remove_near_white_background(img)

    img = crop_to_visible_content(img)
    img = img.resize((texture_size, texture_size), Image.Resampling.NEAREST)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    img.save(output_path)


def write_item_model(modid: str, item_id: str, model_dir: Path, overwrite: bool) -> None:
    model_path = model_dir / f"{item_id}.json"
    if model_path.exists() and not overwrite:
        return
    model_path.parent.mkdir(parents=True, exist_ok=True)
    model = {
        "parent": "minecraft:item/generated",
        "textures": {
            "layer0": f"{modid}:item/{item_id}"
        },
    }
    model_path.write_text(json.dumps(model, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def write_manifest(path: Path, rows: List[Dict[str, Any]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(rows, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def build_arg_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Generate Minecraft item textures via chatgpt2api/OpenAI-compatible image API"
    )
    parser.add_argument("--items", type=Path, default=None, help="JSON item list. If omitted, built-in MVP examples are used.")
    parser.add_argument("--modid", required=True, help="Minecraft mod id, e.g. xiuxian")
    parser.add_argument("--base-url", default=DEFAULT_BASE_URL, help=f"OpenAI-compatible base URL. Default: {DEFAULT_BASE_URL}")
    parser.add_argument("--api-key", default=DEFAULT_API_KEY, help="API key. Defaults to CHATGPT2API_API_KEY env var.")
    parser.add_argument("--model", default=DEFAULT_MODEL, help=f"Image model. Default: {DEFAULT_MODEL}")
    parser.add_argument("--size", default="1024x1024", help="Generation size. Default: 1024x1024")
    parser.add_argument("--quality", default="low", choices=["low", "medium", "high", "auto"], help="Image quality. Default: low")
    parser.add_argument("--texture-size", type=int, default=32, choices=[16, 32, 64, 128, 256, 512], help="Final texture size. Default: 32")
    parser.add_argument("--output-format", default="png", choices=["png", "jpeg", "webp", "none"], help="Requested image output format. Default: png")
    parser.add_argument("--no-transparent", action="store_true", help="Do not request transparent background")
    parser.add_argument("--remove-white", action="store_true", help="Best-effort remove near-white backgrounds after generation")
    parser.add_argument("--overwrite", action="store_true", help="Overwrite existing textures and model JSON")
    parser.add_argument("--keep-raw", action="store_true", help="Keep raw generated images under generated_art/raw")
    parser.add_argument("--delay", type=float, default=2.0, help="Delay between requests in seconds. Default: 2")
    parser.add_argument("--timeout", type=int, default=300, help="HTTP timeout seconds. Default: 300")
    parser.add_argument("--dry-run", action="store_true", help="Print planned work without calling image API")
    parser.add_argument("--limit", type=int, default=0, help="Generate at most N items. 0 means all.")
    parser.add_argument("--start", type=int, default=1, help="Start from 1-based item index. Default: 1")
    parser.add_argument("--textures-dir", type=Path, default=None, help="Override textures/item output dir")
    parser.add_argument("--models-dir", type=Path, default=None, help="Override models/item output dir")
    return parser


def main() -> int:
    args = build_arg_parser().parse_args()

    items = load_items(args.items) if args.items else default_items()
    if args.start > 1:
        items = items[args.start - 1:]
    if args.limit and args.limit > 0:
        items = items[: args.limit]

    textures_dir = args.textures_dir or Path("src/main/resources/assets") / args.modid / "textures" / "item"
    models_dir = args.models_dir or Path("src/main/resources/assets") / args.modid / "models" / "item"
    raw_dir = Path("generated_art/raw")
    manifest_path = Path("generated_art/item_art_manifest.json")

    output_format = None if args.output_format == "none" else args.output_format
    transparent = not args.no_transparent

    print("=== Item Art Generation ===")
    print(f"Base URL     : {args.base_url}")
    print(f"Model        : {args.model}")
    print(f"Mod ID       : {args.modid}")
    print(f"Items        : {len(items)}")
    print(f"Texture size : {args.texture_size}x{args.texture_size}")
    print(f"Textures dir : {textures_dir}")
    print(f"Models dir   : {models_dir}")
    print(f"Dry run      : {args.dry_run}")
    print()

    rows: List[Dict[str, Any]] = []

    for index, item in enumerate(items, start=1):
        texture_path = textures_dir / f"{item.id}.png"
        model_path = models_dir / f"{item.id}.json"
        raw_path = raw_dir / f"{item.id}.png"
        prompt = ensure_prompt_style(item.prompt)

        print(f"[{index}/{len(items)}] {item.id} ({item.name})")

        row = {
            "id": item.id,
            "name": item.name,
            "texture": str(texture_path).replace("\\", "/"),
            "model": str(model_path).replace("\\", "/"),
            "prompt": prompt,
            "status": "pending",
        }

        if texture_path.exists() and not args.overwrite:
            print(f"  skip existing texture: {texture_path}")
            write_item_model(args.modid, item.id, models_dir, overwrite=False)
            row["status"] = "skipped_exists"
            rows.append(row)
            continue

        if args.dry_run:
            print("  dry-run prompt:")
            print("  " + prompt.replace("\n", "\n  "))
            row["status"] = "dry_run"
            rows.append(row)
            continue

        try:
            raw = call_image_api(
                base_url=args.base_url,
                api_key=args.api_key,
                model=args.model,
                prompt=prompt,
                size=args.size,
                quality=args.quality,
                timeout=args.timeout,
                output_format=output_format,
                transparent=transparent,
            )
            process_texture(
                raw,
                output_path=texture_path,
                raw_path=raw_path,
                texture_size=args.texture_size,
                remove_white=args.remove_white,
                keep_raw=args.keep_raw,
            )
            write_item_model(args.modid, item.id, models_dir, overwrite=args.overwrite)
            print(f"  saved texture: {texture_path}")
            print(f"  saved model  : {model_path}")
            row["status"] = "generated"
        except Exception as exc:
            print(f"  ERROR: {exc}", file=sys.stderr)
            row["status"] = "error"
            row["error"] = str(exc)

        rows.append(row)
        if args.delay > 0:
            time.sleep(args.delay)

    write_manifest(manifest_path, rows)
    print()
    print(f"Manifest written: {manifest_path}")
    print("Done.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
