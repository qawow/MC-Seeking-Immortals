#!/usr/bin/env python3
"""Compile the authored 22-status palette into readable status atlas icons."""

from __future__ import annotations

import argparse
import hashlib
import io
import json
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "文本材料/data/status_effects.json"
OUTPUT = ROOT / "src/main/resources/assets/seeking_immortals/textures/mob_effect"


def rgb(value: str) -> tuple[int, int, int]:
    value = value.strip().lstrip("#")
    return tuple(int(value[index:index + 2], 16) for index in (0, 2, 4))


def shade(color: tuple[int, int, int], factor: float) -> tuple[int, int, int]:
    return tuple(max(0, min(255, round(channel * factor))) for channel in color)


def render(effect: dict) -> bytes:
    icon = effect["icon"]
    color = rgb(effect["color"])
    image = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    dark = shade(color, 0.38) + (255,)
    bright = shade(color, 1.28) + (255,)
    draw.rectangle((2, 2, 15, 15), fill=dark)
    draw.rectangle((3, 3, 14, 14), outline=bright, width=1)
    digest = hashlib.sha256(icon.encode("utf-8")).digest()
    # A deterministic glyph built from a small number of cells stays legible
    # at Minecraft's 18x18 status-icon scale without embedding text.
    cells = set()
    for index in range(10):
        x = 4 + digest[index] % 8
        y = 4 + digest[index + 7] % 8
        cells.add((x, y))
    if icon in {"burn", "berserk"}:
        cells.update({(8, 4), (7, 6), (8, 8), (9, 10), (8, 13)})
    elif icon in {"freeze", "stun"}:
        cells.update({(8, 4), (8, 13), (4, 8), (12, 8), (6, 6), (10, 10)})
    elif icon in {"shield", "array_bind"}:
        cells.update({(6, 5), (10, 5), (5, 7), (11, 7), (6, 12), (10, 12), (8, 14)})
    elif icon in {"bleed", "poison"}:
        cells.update({(8, 4), (7, 7), (8, 10), (8, 13)})
    elif icon in {"soul_wound", "fear"}:
        cells.update({(6, 5), (10, 5), (6, 11), (10, 11), (8, 8)})
    elif icon in {"qi_disorder", "tribulation_mark"}:
        cells.update({(5, 6), (11, 6), (8, 8), (5, 11), (11, 11)})
    else:
        cells.update({(8, 5), (8, 7), (8, 9), (8, 11), (8, 13)})
    for x, y in cells:
        draw.rectangle((x, y, x + 1, y + 1), fill=bright)
    output = io.BytesIO()
    image.save(output, "PNG", optimize=False, compress_level=9)
    return output.getvalue()


def files() -> dict[Path, bytes]:
    payload = json.loads(SOURCE.read_text(encoding="utf-8"))
    effects = payload.get("effects", [])
    if len(effects) != 22:
        raise ValueError(f"expected 22 authored statuses, got {len(effects)}")
    return {OUTPUT / f"{effect['id']}.png": render(effect) for effect in effects}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    generated = files()
    if args.check:
        stale = [path for path, data in generated.items() if not path.exists() or path.read_bytes() != data]
        if stale:
            for path in stale:
                print(f"stale status texture: {path.relative_to(ROOT)}")
            return 1
        print(f"status textures are current: {len(generated)}")
        return 0
    for path, data in generated.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
    print(f"wrote {len(generated)} status textures")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
