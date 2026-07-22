#!/usr/bin/env python3
"""Deterministically render non-item entity and GUI bitmap assets.

The renderer keeps every existing path and pixel dimension intact while
replacing the old engineering placeholders with small, resource-pack-friendly
art.  ``--check`` rerenders in memory and compares pixels, dimensions, alpha
coverage, and duplicate groups without changing files.
"""

from __future__ import annotations

import argparse
import hashlib
import random
import sys
from pathlib import Path
from typing import Callable, Iterable

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
ASSET_ROOT = ROOT / "src/main/resources/assets/seeking_immortals"


TextureFactory = Callable[[], Image.Image]


def _rgba(size: tuple[int, int], color: tuple[int, int, int, int] = (0, 0, 0, 0)) -> Image.Image:
    return Image.new("RGBA", size, color)


def _clip_box(image: Image.Image, box: tuple[int, int, int, int]) -> tuple[int, int, int, int] | None:
    left, top, right, bottom = box
    width, height = image.size
    left = max(0, min(width, left))
    top = max(0, min(height, top))
    right = max(0, min(width, right))
    bottom = max(0, min(height, bottom))
    if left >= right or top >= bottom:
        return None
    return left, top, right, bottom


def _rect(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], fill) -> None:
    draw.rectangle(box, fill=fill)


def _gradient(image: Image.Image, top: tuple[int, int, int, int], bottom: tuple[int, int, int, int]) -> None:
    """Fill a vertical gradient without introducing non-deterministic noise."""
    pixels = image.load()
    height = max(1, image.height - 1)
    for y in range(image.height):
        t = y / height
        row = tuple(round(top[i] * (1.0 - t) + bottom[i] * t) for i in range(4))
        for x in range(image.width):
            pixels[x, y] = row


def _blend(base: tuple[int, int, int], amount: int) -> tuple[int, int, int, int]:
    return tuple(max(0, min(255, channel + amount)) for channel in base) + (255,)


def _paint_uv_box(
    draw: ImageDraw.ImageDraw,
    image: Image.Image,
    u: int,
    v: int,
    width: int,
    height: int,
    depth: int,
    base: tuple[int, int, int, int],
    accent: tuple[int, int, int, int],
    face_marks: Iterable[str] = (),
) -> dict[str, tuple[int, int, int, int]]:
    """Paint a Bedrock box atlas and return its six face rectangles.

    The layout follows the standard Bedrock box UV arrangement.  Keeping the
    regions explicit makes the generated entity textures compatible with the
    existing ``*.geo.json`` files rather than relying on a flat illustration.
    """
    regions = {
        "top": (u + depth, v, u + depth + width, v + depth),
        "bottom": (u + depth + width, v, u + depth + 2 * width, v + depth),
        "left": (u, v + depth, u + depth, v + depth + height),
        "front": (u + depth, v + depth, u + depth + width, v + depth + height),
        "right": (u + depth + width, v + depth, u + 2 * depth + width, v + depth + height),
        "back": (u + 2 * depth + width, v + depth, u + 2 * depth + 2 * width,
                 v + depth + height),
    }
    for name, box in regions.items():
        clipped = _clip_box(image, box)
        if clipped is None:
            continue
        _rect(draw, clipped, base)
        left, top, right, bottom = clipped
        if right - left >= 2 and bottom - top >= 2:
            draw.line((left, top, right - 1, top), fill=accent, width=1)
            draw.line((left, bottom - 1, right - 1, bottom - 1), fill=_blend(accent[:3], -35), width=1)
        if name in face_marks and right - left >= 3 and bottom - top >= 3:
            draw.line((left + 1, top + 1, right - 2, bottom - 2), fill=accent, width=1)
    return regions


def _draw_head_face(draw: ImageDraw.ImageDraw, regions: dict[str, tuple[int, int, int, int]],
                    skin: tuple[int, int, int, int], hair: tuple[int, int, int, int],
                    eye: tuple[int, int, int, int], mark: tuple[int, int, int, int] | None = None) -> None:
    front = regions.get("front")
    if not front:
        return
    left, top, right, bottom = front
    # Face plane is intentionally inset so the hair remains visible on the
    # upper and side texels after the cube is wrapped by GeckoLib.
    clipped = (max(0, left), max(0, top), max(left, right), max(top, bottom))
    _rect(draw, clipped, skin)
    if right - left >= 5 and bottom - top >= 5:
        draw.rectangle((left, top, right - 1, top + 1), fill=hair)
        draw.rectangle((left, top, left + 1, bottom - 1), fill=hair)
        draw.rectangle((right - 2, top, right - 1, bottom - 1), fill=hair)
        eye_y = top + max(2, (bottom - top) // 3)
        eye_x = left + max(2, (right - left) // 4)
        draw.point((eye_x, eye_y), fill=eye)
        draw.point((right - 1 - (eye_x - left), eye_y), fill=eye)
        if mark is not None:
            draw.line((left + 2, bottom - 3, right - 3, bottom - 3), fill=mark, width=1)


def _servitor_texture(kind: str) -> Image.Image:
    palettes = {
        "generic": {
            "skin": (211, 177, 137, 255), "hair": (45, 31, 37, 255),
            "body": (43, 93, 88, 255), "accent": (219, 177, 80, 255),
            "dark": (22, 36, 43, 255), "eye": (138, 239, 214, 255),
        },
        "beast": {
            "skin": (190, 116, 55, 255), "hair": (71, 35, 19, 255),
            "body": (122, 54, 27, 255), "accent": (235, 190, 74, 255),
            "dark": (53, 24, 18, 255), "eye": (255, 225, 91, 255),
        },
        "puppet": {
            "skin": (156, 118, 77, 255), "hair": (63, 42, 31, 255),
            "body": (73, 74, 81, 255), "accent": (104, 211, 198, 255),
            "dark": (36, 35, 43, 255), "eye": (115, 235, 255, 255),
        },
        "ghost": {
            "skin": (126, 204, 217, 210), "hair": (46, 115, 141, 200),
            "body": (69, 153, 177, 180), "accent": (184, 246, 238, 230),
            "dark": (35, 87, 119, 180), "eye": (226, 255, 244, 255),
        },
    }
    palette = palettes[kind]
    image = _rgba((64, 64))
    draw = ImageDraw.Draw(image)
    # The UV coordinates mirror summoned_servitor.geo.json.
    _draw_head_face(
        draw,
        _paint_uv_box(draw, image, 0, 0, 8, 8, 8, palette["skin"], palette["hair"]),
        palette["skin"], palette["hair"], palette["eye"], palette["accent"],
    )
    body = _paint_uv_box(draw, image, 16, 16, 8, 12, 4, palette["body"], palette["accent"], ("front",))
    left_arm = _paint_uv_box(draw, image, 40, 16, 4, 12, 4, palette["body"], palette["accent"], ("front",))
    right_arm = _paint_uv_box(draw, image, 32, 48, 4, 12, 4, palette["body"], palette["accent"], ("front",))
    left_leg = _paint_uv_box(draw, image, 16, 48, 4, 12, 4, palette["dark"], palette["accent"], ("front",))
    right_leg = _paint_uv_box(draw, image, 0, 16, 4, 12, 4, palette["dark"], palette["accent"], ("front",))

    # Robe/band details are placed on the front planes used by the model.
    for regions in (body, left_arm, right_arm):
        front = regions.get("front")
        if front:
            left, top, right, bottom = front
            if right - left > 2:
                y = top + max(2, (bottom - top) * 2 // 3)
                draw.line((left, y, right - 1, y), fill=palette["accent"], width=1)
    for regions in (left_leg, right_leg):
        front = regions.get("front")
        if front:
            left, top, right, bottom = front
            draw.line((left, top + 2, right - 1, top + 2), fill=palette["accent"], width=1)

    if kind == "beast":
        # Ears and muzzle on the head atlas; stripes on the torso/limbs.
        head_front = (8, 8, 16, 16)
        draw.line((head_front[0] + 1, head_front[1] + 3, head_front[0] + 2, head_front[1] + 1),
                  fill=palette["dark"], width=1)
        draw.line((head_front[2] - 2, head_front[1] + 1, head_front[2] - 1, head_front[1] + 3),
                  fill=palette["dark"], width=1)
        for regions in (body, left_arm, right_arm):
            front = regions.get("front")
            if front:
                left, top, right, bottom = front
                for offset in (3, 7):
                    if top + offset < bottom:
                        draw.line((left, top + offset, right - 1, top + offset), fill=palette["dark"], width=1)
    elif kind == "puppet":
        # Joint pins and a central rune keep the construct readable at 16 px.
        for regions in (body, left_arm, right_arm, left_leg, right_leg):
            front = regions.get("front")
            if front:
                left, top, right, bottom = front
                draw.point(((left + right - 1) // 2, top + 1), fill=palette["accent"])
                draw.point(((left + right - 1) // 2, bottom - 2), fill=palette["accent"])
        front = body.get("front")
        if front:
            left, top, right, bottom = front
            draw.line(((left + right - 1) // 2, top + 2, (left + right - 1) // 2, bottom - 3),
                      fill=palette["accent"], width=1)
    elif kind == "ghost":
        # A translucent trailing hem and soul wisps on the atlas.
        front = body.get("front")
        if front:
            left, top, right, bottom = front
            for x in range(left, right):
                if (x - left) % 3 == 0:
                    draw.line((x, bottom - 3, x, bottom - 1), fill=palette["accent"], width=1)
        for x, y in ((2, 2), (58, 6), (6, 42), (58, 44)):
            draw.point((x, y), fill=palette["accent"])
    else:
        front = body.get("front")
        if front:
            left, top, right, bottom = front
            draw.line(((left + right - 1) // 2, top + 2, (left + right - 1) // 2, bottom - 2),
                      fill=palette["accent"], width=1)
    return image


def _boat_texture(cloud: bool) -> Image.Image:
    image = _rgba((128, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    if cloud:
        hull = _paint_uv_box(
            draw, image, 0, 0, 16, 4, 28,
            (112, 178, 198, 255), (224, 248, 242, 255), ("top", "front", "back"),
        )
        deck = _paint_uv_box(
            draw, image, 0, 32, 12, 1, 20,
            (202, 231, 235, 255), (91, 157, 180, 255), ("top",),
        )
        glow = _paint_uv_box(
            draw, image, 64, 32, 4, 2, 4,
            (93, 211, 201, 255), (238, 255, 230, 255), ("front", "back", "top"),
        )
        hull_line = (67, 136, 166, 255)
        deck_line = (244, 253, 247, 255)
    else:
        hull = _paint_uv_box(
            draw, image, 0, 0, 16, 4, 28,
            (79, 39, 34, 255), (221, 155, 61, 255), ("top", "front", "back"),
        )
        deck = _paint_uv_box(
            draw, image, 0, 32, 12, 1, 20,
            (128, 68, 39, 255), (237, 190, 82, 255), ("top",),
        )
        glow = _paint_uv_box(
            draw, image, 64, 32, 4, 2, 4,
            (41, 129, 119, 255), (211, 249, 189, 255), ("front", "back", "top"),
        )
        hull_line = (48, 27, 30, 255)
        deck_line = (196, 111, 49, 255)

    hull_top = hull["top"]
    for y in range(hull_top[1] + 4, hull_top[3] - 1, 4):
        draw.line((hull_top[0] + 1, y, hull_top[2] - 2, y), fill=hull_line, width=1)
    hull_front = hull["front"]
    draw.line((hull_front[0] + 2, hull_front[1] + 1,
               hull_front[2] - 3, hull_front[1] + 1), fill=deck_line, width=1)

    deck_top = deck["top"]
    center_x = (deck_top[0] + deck_top[2] - 1) // 2
    draw.line((center_x, deck_top[1] + 2, center_x, deck_top[3] - 2), fill=deck_line, width=1)
    for y in range(deck_top[1] + 5, deck_top[3] - 1, 5):
        draw.line((deck_top[0] + 1, y, deck_top[2] - 2, y), fill=deck_line, width=1)

    glow_top = glow["top"]
    draw.point(((glow_top[0] + glow_top[2] - 1) // 2,
                (glow_top[1] + glow_top[3] - 1) // 2), fill=(255, 255, 222, 255))
    return image


def _portrait_texture(style: str) -> Image.Image:
    specs = {
        "default": ((34, 55, 79), (92, 129, 144), (205, 164, 118), (28, 51, 59), (37, 29, 31)),
        "mo_lao": ((46, 31, 25), (124, 78, 46), (208, 161, 113), (69, 42, 29), (40, 25, 24)),
        "mulan": ((62, 26, 31), (145, 55, 58), (226, 174, 133), (105, 30, 38), (39, 21, 28)),
        "yinluo": ((29, 22, 61), (86, 48, 117), (181, 173, 199), (55, 31, 86), (23, 18, 39)),
        "star_broker": ((22, 38, 78), (48, 92, 148), (213, 177, 124), (34, 62, 117), (24, 31, 55)),
        "kunwu": ((31, 57, 41), (87, 124, 67), (201, 160, 112), (45, 84, 52), (27, 34, 28)),
    }
    sky_top, sky_bottom, skin, robe, hair = specs[style]
    image = _rgba((72, 88), sky_top + (255,))
    _gradient(image, sky_top + (255,), sky_bottom + (255,))
    draw = ImageDraw.Draw(image)
    # Atmospheric motes and a simple ink-wash halo.
    seed = int(hashlib.sha256(style.encode()).hexdigest()[:8], 16)
    rng = random.Random(seed)
    for _ in range(18):
        x, y = rng.randrange(4, 68), rng.randrange(4, 74)
        draw.point((x, y), fill=(235, 214, 153, 100))
    draw.ellipse((14, 16, 58, 63), fill=skin + (255,), outline=hair + (255,), width=2)
    # Hair cap and side locks.
    draw.pieslice((12, 10, 60, 57), 180, 360, fill=hair + (255,))
    draw.rectangle((14, 30, 21, 57), fill=hair + (255,))
    draw.rectangle((51, 30, 58, 57), fill=hair + (255,))
    # Brows, eyes, nose and mouth remain deliberately tiny and icon-readable.
    draw.line((25, 34, 32, 33), fill=hair + (255,), width=2)
    draw.line((40, 33, 47, 34), fill=hair + (255,), width=2)
    draw.ellipse((27, 36, 30, 39), fill=(21, 20, 24, 255))
    draw.ellipse((42, 36, 45, 39), fill=(21, 20, 24, 255))
    draw.line((35, 39, 34, 45), fill=(119, 77, 62, 255), width=1)
    draw.arc((31, 43, 42, 51), 15, 165, fill=(104, 47, 54, 255), width=1)
    draw.polygon(((8, 71), (29, 56), (43, 56), (64, 71), (64, 88), (8, 88)), fill=robe + (255,))
    draw.line((36, 58, 36, 87), fill=(226, 188, 101, 220), width=2)
    if style == "mo_lao":
        draw.line((15, 18, 56, 18), fill=(224, 187, 112, 220), width=2)
        draw.rectangle((31, 57, 41, 65), fill=(184, 137, 77, 255))
    elif style == "mulan":
        draw.line((18, 17, 54, 22), fill=(235, 201, 132, 220), width=2)
        draw.polygon(((28, 7), (36, 2), (48, 10), (43, 16), (29, 14)), fill=(125, 33, 42, 255))
    elif style == "yinluo":
        draw.ellipse((27, 2, 45, 18), outline=(157, 125, 220, 210), width=2)
        draw.point((36, 10), fill=(218, 221, 255, 255))
    elif style == "star_broker":
        for x, y in ((10, 11), (62, 24), (7, 45), (62, 60)):
            draw.point((x, y), fill=(235, 225, 141, 240))
        draw.rectangle((49, 57, 57, 65), fill=(178, 137, 63, 255))
    elif style == "kunwu":
        draw.line((23, 14, 49, 8), fill=(196, 169, 94, 220), width=2)
        draw.rectangle((31, 57, 41, 64), fill=(155, 113, 57, 255))
    else:
        draw.rectangle((31, 57, 41, 64), fill=(168, 126, 66, 255))
    draw.rectangle((0, 0, 71, 87), outline=(228, 192, 108, 255), width=2)
    draw.rectangle((3, 3, 68, 84), outline=(61, 35, 39, 180), width=1)
    return image


def _paper_texture(kind: str) -> Image.Image:
    specs = {
        "paper_rice": ((236, 230, 210), (222, 214, 190), None, 0, None, 0, 111),
        "paper_cool": ((225, 234, 225), (205, 219, 207), None, 0, (95, 123, 108), 8, 222),
        "paper_aged": ((230, 216, 180), (212, 196, 156), (176, 148, 96), 7, None, 0, 333),
        "paper_dry": ((232, 220, 210), (216, 200, 188), None, 0, (168, 66, 48), 14, 444),
    }
    base, fiber, stain, stain_count, speckle, speckle_count, seed = specs[kind]
    image = _rgba((32, 32), base + (255,))
    draw = ImageDraw.Draw(image)
    rng = random.Random(seed)
    for _ in range(32):
        y = rng.randrange(32)
        x0 = rng.randrange(32)
        length = rng.randrange(4, 15)
        delta = rng.choice((-10, -6, 5, 9))
        color = tuple(max(0, min(255, c + delta)) for c in fiber) + (54,)
        draw.line((x0, y, min(31, x0 + length), y), fill=color, width=1)
    for _ in range(11):
        x = rng.randrange(32)
        y0 = rng.randrange(32)
        length = rng.randrange(3, 9)
        color = tuple(max(0, c - 7) for c in fiber) + (36,)
        draw.line((x, y0, x, min(31, y0 + length)), fill=color, width=1)
    if stain is not None:
        for _ in range(stain_count):
            cx, cy = rng.randrange(32), rng.randrange(32)
            radius = rng.randrange(1, 3)
            draw.ellipse((cx - radius, cy - radius, cx + radius, cy + radius), fill=stain + (30,))
    if speckle is not None:
        for _ in range(speckle_count):
            x, y = rng.randrange(32), rng.randrange(32)
            draw.point((x, y), fill=speckle + (52,))
    return image


def _seal_grain() -> Image.Image:
    image = _rgba((16, 16), (170, 54, 39, 255))
    draw = ImageDraw.Draw(image)
    rng = random.Random(707)
    for _ in range(52):
        x, y = rng.randrange(16), rng.randrange(16)
        shade = rng.choice(((143, 39, 28), (198, 74, 48), (157, 48, 33)))
        draw.point((x, y), fill=shade + (180,))
    for y in (2, 7, 12):
        draw.line((2, y, 13, y), fill=(104, 31, 23, 125), width=1)
    return image


def _slot_texture(kind: str) -> Image.Image:
    image = _rgba((1024, 1024))
    draw = ImageDraw.Draw(image)
    if kind == "artifact":
        # A jade-and-gold artifact ring with a centered blade silhouette.
        for inset, alpha in ((110, 42), (96, 72), (82, 110), (68, 150)):
            draw.ellipse((inset, inset, 1024 - inset, 1024 - inset), outline=(91, 211, 185, alpha), width=26)
        draw.ellipse((172, 172, 852, 852), outline=(232, 190, 81, 215), width=22)
        draw.polygon(((512, 112), (580, 428), (666, 742), (512, 914), (358, 742), (444, 428)),
                     fill=(94, 186, 191, 190), outline=(231, 216, 133, 235))
        draw.polygon(((512, 140), (537, 472), (512, 820), (487, 472)), fill=(210, 247, 221, 220))
        draw.ellipse((448, 410, 576, 538), fill=(230, 199, 95, 235), outline=(255, 241, 173, 245), width=10)
    else:
        # A vermilion charm cord and jade talisman, kept separate from artifact.
        draw.ellipse((100, 100, 924, 924), outline=(178, 67, 59, 72), width=34)
        draw.arc((154, 86, 870, 760), 196, 344, fill=(151, 44, 40, 220), width=42)
        draw.arc((218, 154, 806, 730), 192, 352, fill=(219, 100, 62, 225), width=28)
        draw.rounded_rectangle((300, 286, 724, 822), radius=48, fill=(133, 190, 171, 210),
                               outline=(244, 194, 85, 235), width=22)
        draw.polygon(((512, 350), (624, 462), (512, 730), (400, 462)), fill=(75, 131, 128, 155),
                     outline=(220, 237, 192, 220))
        draw.line((512, 378, 512, 690), fill=(240, 212, 120, 220), width=14)
        draw.line((444, 520, 580, 520), fill=(240, 212, 120, 220), width=14)
    return image


FACTORIES: dict[str, TextureFactory] = {
    "textures/entity/spirit_boat.png": lambda: _boat_texture(False),
    "textures/entity/spirit_boat_cloud.png": lambda: _boat_texture(True),
    "textures/entity/summoned_servitor.png": lambda: _servitor_texture("generic"),
    "textures/entity/summoned_servitor_beast.png": lambda: _servitor_texture("beast"),
    "textures/entity/summoned_servitor_puppet.png": lambda: _servitor_texture("puppet"),
    "textures/entity/summoned_servitor_ghost.png": lambda: _servitor_texture("ghost"),
    "textures/gui/dialogue/portrait_default.png": lambda: _portrait_texture("default"),
    "textures/gui/dialogue/portrait_mo_lao.png": lambda: _portrait_texture("mo_lao"),
    "textures/gui/dialogue/portrait_mulan.png": lambda: _portrait_texture("mulan"),
    "textures/gui/dialogue/portrait_yinluo.png": lambda: _portrait_texture("yinluo"),
    "textures/gui/dialogue/portrait_star_broker.png": lambda: _portrait_texture("star_broker"),
    "textures/gui/dialogue/portrait_kunwu.png": lambda: _portrait_texture("kunwu"),
    "textures/gui/ink/paper_rice.png": lambda: _paper_texture("paper_rice"),
    "textures/gui/ink/paper_cool.png": lambda: _paper_texture("paper_cool"),
    "textures/gui/ink/paper_aged.png": lambda: _paper_texture("paper_aged"),
    "textures/gui/ink/paper_dry.png": lambda: _paper_texture("paper_dry"),
    "textures/gui/ink/seal_grain.png": _seal_grain,
    "textures/gui/empty_artifact_slot.png": lambda: _slot_texture("artifact"),
    "textures/gui/empty_charm_slot.png": lambda: _slot_texture("charm"),
}


EXPECTED_SIZES = {
    "textures/entity/spirit_boat.png": (128, 64),
    "textures/entity/spirit_boat_cloud.png": (128, 64),
    "textures/entity/summoned_servitor.png": (64, 64),
    "textures/entity/summoned_servitor_beast.png": (64, 64),
    "textures/entity/summoned_servitor_puppet.png": (64, 64),
    "textures/entity/summoned_servitor_ghost.png": (64, 64),
    **{f"textures/gui/dialogue/portrait_{name}.png": (72, 88)
       for name in ("default", "mo_lao", "mulan", "yinluo", "star_broker", "kunwu")},
    **{f"textures/gui/ink/{name}.png": (32, 32)
       for name in ("paper_rice", "paper_cool", "paper_aged", "paper_dry")},
    "textures/gui/ink/seal_grain.png": (16, 16),
    "textures/gui/empty_artifact_slot.png": (1024, 1024),
    "textures/gui/empty_charm_slot.png": (1024, 1024),
}

TRANSPARENT_BACKGROUND = {
    "textures/entity/spirit_boat.png",
    "textures/entity/spirit_boat_cloud.png",
    "textures/entity/summoned_servitor.png",
    "textures/entity/summoned_servitor_beast.png",
    "textures/entity/summoned_servitor_puppet.png",
    "textures/entity/summoned_servitor_ghost.png",
    "textures/gui/empty_artifact_slot.png",
    "textures/gui/empty_charm_slot.png",
}

FULL_RECTANGLE = set(EXPECTED_SIZES) - TRANSPARENT_BACKGROUND


def _render_all() -> dict[str, Image.Image]:
    return {relative: factory() for relative, factory in FACTORIES.items()}


def _alpha_stats(image: Image.Image) -> tuple[int, int, float]:
    alpha = image.getchannel("A")
    values = list(alpha.get_flattened_data())
    return min(values), max(values), sum(value > 0 for value in values) / len(values)


def _validate(images: dict[str, Image.Image], compare_files: bool) -> list[str]:
    errors: list[str] = []
    if set(images) != set(EXPECTED_SIZES):
        errors.append("factory/spec key set mismatch")
    hashes: dict[str, list[str]] = {}
    for relative, rendered in images.items():
        expected_size = EXPECTED_SIZES.get(relative)
        if expected_size is None:
            continue
        if rendered.size != expected_size:
            errors.append(f"{relative}: generated size {rendered.size}, expected {expected_size}")
        if rendered.mode != "RGBA":
            errors.append(f"{relative}: generated mode {rendered.mode}, expected RGBA")
        alpha_min, alpha_max, visible = _alpha_stats(rendered)
        if alpha_max == 0 or visible <= 0.001:
            errors.append(f"{relative}: generated image has no visible pixels")
        if relative in TRANSPARENT_BACKGROUND:
            if alpha_min != 0:
                errors.append(f"{relative}: transparent-background asset has alpha minimum {alpha_min}")
            if not 0.05 <= visible <= 0.85:
                errors.append(f"{relative}: implausible visible coverage {visible:.3f}")
        elif relative in FULL_RECTANGLE and visible != 1.0:
            errors.append(f"{relative}: rectangular/tile asset has holes (coverage {visible:.3f})")
        digest_source = f"{rendered.mode}:{rendered.width}x{rendered.height}:".encode() + rendered.tobytes()
        digest = hashlib.sha256(digest_source).hexdigest()
        hashes.setdefault(digest, []).append(relative)
        if not compare_files:
            continue
        target = ASSET_ROOT / relative
        if not target.exists():
            errors.append(f"{relative}: file is missing")
            continue
        try:
            with Image.open(target) as actual:
                actual.load()
                if actual.mode != "RGBA":
                    errors.append(f"{relative}: mode {actual.mode}, expected RGBA")
                if actual.size != expected_size:
                    errors.append(f"{relative}: size {actual.size}, expected {expected_size}")
                if actual.mode == "RGBA" and actual.size == expected_size and actual.tobytes() != rendered.tobytes():
                    errors.append(f"{relative}: pixels differ from deterministic render")
        except Exception as exc:  # pragma: no cover - defensive resource audit
            errors.append(f"{relative}: unreadable PNG ({exc})")
    for digest, paths in hashes.items():
        if len(paths) > 1:
            errors.append("duplicate pixel group: " + ", ".join(sorted(paths)))
    return errors


def _write(images: dict[str, Image.Image]) -> None:
    for relative, image in images.items():
        target = ASSET_ROOT / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        image.save(target, format="PNG", optimize=False, compress_level=9)
        print(f"wrote {relative} {image.width}x{image.height}")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="audit files against an in-memory deterministic render")
    args = parser.parse_args(argv)
    images = _render_all()
    if args.check:
        errors = _validate(images, compare_files=True)
        if errors:
            for error in errors:
                print(f"ERROR: {error}", file=sys.stderr)
            return 1
        coverage = [_alpha_stats(image)[2] for image in images.values()]
        print(
            f"entity/gui texture check: {len(images)} files; deterministic pixels 0 mismatches; "
            f"duplicate groups 0; visible coverage {min(coverage):.3f}-{max(coverage):.3f}"
        )
        return 0
    _write(images)
    errors = _validate(images, compare_files=False)
    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1
    print(f"entity/gui textures generated: {len(images)} files; duplicate groups 0")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
