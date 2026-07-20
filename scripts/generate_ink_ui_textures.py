#!/usr/bin/env python3
"""Generate the 云笈墨卷 (InkScroll) UI paper tiles for seeking_immortals.

Emits light rice-paper tiles (32x32) with subtle fiber noise plus a 16x16
cinnabar seal-paste grain. All procedural, idempotent; real art can replace
any PNG by name later without code changes.

Outputs (assets/seeking_immortals/textures/gui/ink/):
  paper_rice.png  - FIELD_NOTES 行录: warm rice paper
  paper_cool.png  - QUIET_STUDY 静室: cool grey-green paper
  paper_aged.png  - LEDGER_HALL 账房: ochre aged paper with foxing
  paper_dry.png   - OMEN_RED 凶兆: pale paper, dry-brush red speckle
  seal_grain.png  - cinnabar seal-paste grain
"""

import random

from PIL import Image, ImageDraw

OUTPUT_DIR = "src/main/resources/assets/seeking_immortals/textures/gui/ink"
TILE = 32


def paper(base, fiber, stain=None, stain_count=0, speckle=None, speckle_count=0, seed=0):
    rng = random.Random(seed)
    img = Image.new("RGBA", (TILE, TILE), base + (255,))
    draw = ImageDraw.Draw(img)
    # Horizontal fiber strokes: 1px lines slightly darker/lighter than base.
    for _ in range(26):
        y = rng.randrange(TILE)
        x0 = rng.randrange(TILE)
        length = rng.randrange(4, 14)
        delta = rng.choice((-8, -5, 5, 8))
        color = tuple(max(0, min(255, c + delta)) for c in fiber) + (60,)
        draw.line([(x0, y), (min(TILE - 1, x0 + length), y)], fill=color)
    # Sparse vertical fibers for weave.
    for _ in range(8):
        x = rng.randrange(TILE)
        y0 = rng.randrange(TILE)
        length = rng.randrange(3, 8)
        color = tuple(max(0, min(255, c - 6)) for c in fiber) + (40,)
        draw.line([(x, y0), (x, min(TILE - 1, y0 + length))], fill=color)
    # Optional foxing stains (aged paper).
    if stain and stain_count:
        for _ in range(stain_count):
            cx, cy = rng.randrange(TILE), rng.randrange(TILE)
            r = rng.randrange(1, 3)
            draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=stain + (28,))
    # Optional dry-brush speckle (omen paper).
    if speckle and speckle_count:
        for _ in range(speckle_count):
            x, y = rng.randrange(TILE), rng.randrange(TILE)
            draw.point((x, y), fill=speckle + (46,))
    return img


def seal_grain(seed=7):
    rng = random.Random(seed)
    size = 16
    img = Image.new("RGBA", (size, size), (176, 58, 40, 255))
    draw = ImageDraw.Draw(img)
    for _ in range(46):
        x, y = rng.randrange(size), rng.randrange(size)
        shade = rng.choice(((150, 44, 30), (196, 74, 52), (160, 50, 34)))
        draw.point((x, y), fill=shade + (170,))
    for _ in range(6):
        x, y = rng.randrange(size), rng.randrange(size)
        draw.point((x, y), fill=(120, 34, 24, 140))
    return img


def main():
    import os

    os.makedirs(OUTPUT_DIR, exist_ok=True)
    tiles = {
        "paper_rice.png": paper((236, 230, 210), (222, 214, 190), seed=11),
        "paper_cool.png": paper((228, 234, 224), (210, 220, 208), seed=22),
        "paper_aged.png": paper((230, 216, 180), (212, 196, 156),
                                stain=(176, 148, 96), stain_count=5, seed=33),
        "paper_dry.png": paper((232, 220, 210), (216, 200, 188),
                               speckle=(168, 66, 48), speckle_count=10, seed=44),
    }
    for name, img in tiles.items():
        img.save(f"{OUTPUT_DIR}/{name}")
        print("wrote", name)
    seal_grain().save(f"{OUTPUT_DIR}/seal_grain.png")
    print("wrote seal_grain.png")


if __name__ == "__main__":
    main()
