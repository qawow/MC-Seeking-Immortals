#!/usr/bin/env python3
"""Generate deterministic particle, ribbon, and beam textures for authored VFX."""

from __future__ import annotations

import argparse
import io
import json
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/seeking_immortals"
PARTICLE_DIR = ASSETS / "textures/particle"
TRAIL_DIR = ASSETS / "textures/effect/trail"
EFFECT_DIR = ASSETS / "textures/effect"
PARTICLE_DEFINITION_DIR = ASSETS / "particles"

PARTICLES = (
    "qi_soft",
    "fire_ember",
    "water_mist",
    "wood_pollen",
    "metal_spark",
    "earth_dust",
    "thunder_arc",
    "yin_smoke",
    "soul_wisps",
    "blood_mist",
    "heal_motes",
    "space_glitch",
)

TRAILS = (
    "sword_thin",
    "heavy_weapon",
    "flying_sword_orbit",
    "talisman_ash",
    "blood_ribbon",
    "thunder_jagged",
    "soul_afterimage",
    "movement_wind",
)


def gaussian_alpha(size: int, radius: float, power: float = 1.0) -> Image.Image:
    image = Image.new("RGBA", (size, size), (255, 255, 255, 0))
    pixels = image.load()
    center = (size - 1) * 0.5
    for y in range(size):
        for x in range(size):
            distance = math.hypot(x - center, y - center) / max(0.01, radius)
            alpha = max(0.0, 1.0 - distance)
            pixels[x, y] = (255, 255, 255, round(255 * alpha ** power))
    return image


def render_particle(name: str) -> Image.Image:
    size = 16
    base = Image.new("RGBA", (size, size), (255, 255, 255, 0))
    draw = ImageDraw.Draw(base)
    if name == "qi_soft":
        return gaussian_alpha(size, 7.1, 2.2).filter(ImageFilter.GaussianBlur(0.45))
    if name == "fire_ember":
        draw.polygon(((8, 1), (11, 6), (9, 14), (6, 14), (4, 8)), fill=(255, 255, 255, 210))
        draw.ellipse((6, 7, 9, 13), fill=(255, 255, 255, 255))
    elif name == "water_mist":
        draw.ellipse((1, 5, 14, 12), fill=(255, 255, 255, 96))
        draw.ellipse((4, 3, 11, 10), fill=(255, 255, 255, 116))
        base = base.filter(ImageFilter.GaussianBlur(1.25))
    elif name == "wood_pollen":
        for x, y, radius in ((4, 5, 2), (10, 4, 1), (8, 10, 2), (12, 12, 1)):
            draw.ellipse((x - radius, y - radius, x + radius, y + radius), fill=(255, 255, 255, 210))
    elif name == "metal_spark":
        draw.line((8, 0, 8, 15), fill=(255, 255, 255, 240), width=1)
        draw.line((1, 8, 14, 8), fill=(255, 255, 255, 220), width=1)
        draw.line((4, 4, 12, 12), fill=(255, 255, 255, 150), width=1)
        draw.ellipse((6, 6, 9, 9), fill=(255, 255, 255, 255))
    elif name == "earth_dust":
        for x, y, radius, alpha in ((4, 10, 3, 150), (9, 9, 4, 130), (12, 6, 2, 115)):
            draw.ellipse((x - radius, y - radius, x + radius, y + radius), fill=(255, 255, 255, alpha))
        base = base.filter(ImageFilter.GaussianBlur(0.55))
    elif name == "thunder_arc":
        draw.line(((10, 0), (6, 6), (10, 6), (5, 15)), fill=(255, 255, 255, 255), width=2)
    elif name == "yin_smoke":
        draw.ellipse((3, 6, 12, 15), fill=(255, 255, 255, 118))
        draw.ellipse((6, 1, 13, 10), fill=(255, 255, 255, 92))
        base = base.filter(ImageFilter.GaussianBlur(1.0))
    elif name == "soul_wisps":
        draw.arc((3, 1, 12, 15), 78, 285, fill=(255, 255, 255, 220), width=2)
        draw.ellipse((4, 2, 8, 6), fill=(255, 255, 255, 235))
        base = base.filter(ImageFilter.GaussianBlur(0.35))
    elif name == "blood_mist":
        for x, y, radius, alpha in ((5, 6, 3, 150), (10, 9, 4, 140), (8, 3, 2, 175)):
            draw.ellipse((x - radius, y - radius, x + radius, y + radius), fill=(255, 255, 255, alpha))
        base = base.filter(ImageFilter.GaussianBlur(0.75))
    elif name == "heal_motes":
        draw.ellipse((5, 5, 10, 10), fill=(255, 255, 255, 255))
        draw.line((8, 1, 8, 14), fill=(255, 255, 255, 140), width=1)
        draw.line((1, 8, 14, 8), fill=(255, 255, 255, 140), width=1)
        base = base.filter(ImageFilter.GaussianBlur(0.25))
    elif name == "space_glitch":
        for y, left, right, alpha in ((2, 4, 12, 180), (5, 1, 9, 230), (8, 6, 15, 255), (11, 3, 11, 205), (14, 7, 13, 145)):
            draw.rectangle((left, y, right, y + 1), fill=(255, 255, 255, alpha))
    else:
        raise ValueError(f"unknown particle {name}")
    return base


def render_trail(name: str) -> Image.Image:
    width, height = 64, 8
    image = Image.new("RGBA", (width, height), (255, 255, 255, 0))
    pixels = image.load()
    for x in range(width):
        progress = x / (width - 1)
        fade = math.sin(math.pi * progress) ** 0.55
        for y in range(height):
            center_distance = abs(y - (height - 1) * 0.5) / 3.5
            alpha = max(0.0, 1.0 - center_distance)
            if name == "sword_thin":
                alpha = alpha ** 5.0
            elif name == "heavy_weapon":
                alpha = alpha ** 1.5
            elif name == "flying_sword_orbit":
                alpha = alpha ** 3.2 * (0.74 + 0.26 * math.sin(progress * math.tau * 3.0) ** 2)
            elif name == "talisman_ash":
                alpha = alpha ** 2.8 * (0.45 + 0.55 * ((x * 17 + y * 29) % 11 > 5))
            elif name == "blood_ribbon":
                alpha = alpha ** 1.8 * (0.76 + 0.24 * math.sin(progress * math.tau * 2.0 + y))
            elif name == "thunder_jagged":
                line_y = 3.5 + math.sin(progress * math.tau * 5.0) * 1.8
                alpha = max(0.0, 1.0 - abs(y - line_y)) ** 2.0
            elif name == "soul_afterimage":
                alpha = alpha ** 2.3 * (1.0 - progress * 0.45)
            elif name == "movement_wind":
                alpha = alpha ** 4.0 * (0.55 + 0.45 * math.sin(progress * math.pi * 4.0) ** 2)
            else:
                raise ValueError(f"unknown trail {name}")
            pixels[x, y] = (255, 255, 255, round(255 * fade * max(0.0, alpha)))
    return image


def render_beam() -> Image.Image:
    image = Image.new("RGBA", (64, 16), (255, 255, 255, 0))
    pixels = image.load()
    for x in range(64):
        edge = math.sin(math.pi * x / 63.0) ** 0.35
        for y in range(16):
            center = 1.0 - abs(y - 7.5) / 7.5
            alpha = edge * max(0.0, center) ** 2.4
            pixels[x, y] = (255, 255, 255, round(255 * alpha))
    return image


def encode(image: Image.Image) -> bytes:
    output = io.BytesIO()
    image.save(output, "PNG", optimize=False, compress_level=9)
    return output.getvalue()


def generated_files() -> dict[Path, bytes]:
    files: dict[Path, bytes] = {}
    for name in PARTICLES:
        files[PARTICLE_DIR / f"{name}.png"] = encode(render_particle(name))
        definition = {"textures": [f"seeking_immortals:particle/{name}"]}
        files[PARTICLE_DEFINITION_DIR / f"{name}.json"] = (
            json.dumps(definition, indent=2, sort_keys=True) + "\n"
        ).encode("utf-8")
    for name in TRAILS:
        files[TRAIL_DIR / f"{name}.png"] = encode(render_trail(name))
    files[EFFECT_DIR / "beam_soft.png"] = encode(render_beam())
    return files


def check(files: dict[Path, bytes]) -> int:
    stale = [path for path, content in files.items() if not path.exists() or path.read_bytes() != content]
    if stale:
        for path in stale:
            print(f"stale generated visual texture: {path.relative_to(ROOT)}")
        return 1
    for path in sorted(files):
        if path.suffix != ".png":
            continue
        with Image.open(path) as image:
            expected_size = (16, 16) if path.parent == PARTICLE_DIR else (64, 16) if path.name == "beam_soft.png" else (64, 8)
            if image.mode != "RGBA" or image.size != expected_size:
                print(f"invalid generated visual texture: {path.relative_to(ROOT)}")
                return 1
    print(f"visual effect textures are current: {len(PARTICLES)} particles, {len(TRAILS)} trails, 1 beam")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    files = generated_files()
    if args.check:
        return check(files)
    for path, content in files.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(content)
    print(f"wrote {len(files)} visual effect resources")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
