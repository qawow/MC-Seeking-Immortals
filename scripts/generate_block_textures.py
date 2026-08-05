#!/usr/bin/env python3
"""Generate deterministic 16x16 textures for every shipped mod block.

The renderer favors readable Minecraft-scale materials over inventory-style
icons. Ores use irregular veins, machines use framed metal panels, formations
use carved runes, and utility blocks use wood, cloth, soil, or masonry. The
check mode rerenders every texture in memory and also validates local model
references, image format, opacity, and pixel uniqueness.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/seeking_immortals"
TEXTURE_DIR = ASSETS / "textures/block"
BLOCK_MODEL_DIR = ASSETS / "models/block"
ITEM_MODEL_DIR = ASSETS / "models/item"
BLOCKSTATE_DIR = ASSETS / "blockstates"
OUTPUT_SIZE = 16

# These historical textures are no longer direct block-model references, but
# remain part of the shipped compatibility resource set.
COMPAT_TEXTURE_IDS = {
    "altar",
    "formation_core",
    "ling_gen_identification_slab",
    "portal_gate",
}


PALETTES = {
    "stone": ((78, 76, 70), (111, 106, 94), (45, 44, 42), (177, 166, 137)),
    "deepstone": ((42, 43, 49), (68, 69, 78), (24, 25, 31), (118, 111, 145)),
    "earth": ((117, 78, 42), (151, 106, 59), (73, 49, 31), (184, 139, 76)),
    "wood": ((107, 67, 37), (148, 96, 52), (62, 39, 28), (194, 143, 78)),
    "metal": ((77, 82, 82), (122, 127, 122), (39, 43, 45), (186, 173, 132)),
    "bronze": ((99, 68, 43), (150, 105, 59), (55, 40, 30), (211, 146, 70)),
    "jade": ((51, 91, 78), (78, 132, 106), (29, 51, 48), (125, 207, 161)),
    "cloth": ((104, 43, 47), (154, 65, 66), (61, 30, 35), (211, 144, 97)),
    "obsidian": ((39, 35, 48), (66, 57, 76), (21, 20, 27), (118, 93, 144)),
}

ACCENTS = {
    "neutral": (203, 177, 111),
    "spirit": (100, 221, 190),
    "water": (78, 163, 204),
    "wind": (144, 215, 204),
    "fire": (229, 100, 45),
    "blood": (197, 51, 54),
    "thunder": (235, 200, 75),
    "void": (133, 92, 181),
    "yin": (121, 95, 167),
    "metal": (202, 211, 205),
    "earth": (196, 143, 69),
    "wood": (104, 176, 95),
    "ascension": (236, 203, 117),
    "portal": (105, 184, 206),
}


MOTIF_OVERRIDES = {
    "alchemy_furnace_array_node": "furnace_node",
    "altar": "altar",
    "ascension_gate": "ascension",
    "barrier_sect_protection_formation_core": "barrier",
    "blood_sacrifice_altar": "blood_altar",
    "defense_formation_core": "shield",
    "demon_seal_pillar_formation_core": "pillar_seal",
    "five_elements_mountain_formation_core": "five_elements",
    "formation_core": "formation",
    "illusion_maze_formation_core": "maze",
    "inverted_five_elements_formation_core": "inverted_five",
    "kill_sword_formation_core": "sword",
    "long_range_teleport_array": "teleport",
    "mulan_wind_ride_formation_core": "wind",
    "nine_dragon_flame_barrier_formation_core": "flame_ring",
    "seal_demon_formation_core": "seal",
    "sect_gate_array": "sect_array",
    "spirit_gathering_array": "gathering",
    "spirit_gathering_formation_core": "gathering",
    "spirit_gathering_minor_formation_core": "minor_gathering",
    "sword_array_bagua_formation_core": "sword_bagua",
    "teleport_array_pedestal": "teleport",
    "thunder_tribulation_altar": "thunder",
    "thunder_tribulation_array_formation_core": "thunder",
    "vajra_prison_formation_core": "vajra",
}


@dataclass(frozen=True)
class BlockSpec:
    block_id: str
    base_id: str
    face: str
    kind: str
    motif: str
    palette: str
    accent: str
    tier: int
    formed: bool
    seed: bytes


def clamp(value: int) -> int:
    return max(0, min(255, value))


def mix(a: tuple[int, int, int], b: tuple[int, int, int], amount: float) -> tuple[int, int, int]:
    return tuple(clamp(round(left * (1.0 - amount) + right * amount)) for left, right in zip(a, b))


def shade(color: tuple[int, int, int], factor: float) -> tuple[int, int, int]:
    return tuple(clamp(round(channel * factor)) for channel in color)


def vary(color: tuple[int, int, int], amount: int) -> tuple[int, int, int]:
    return tuple(clamp(channel + amount) for channel in color)


def stable_bytes(value: str) -> bytes:
    return hashlib.sha256(value.encode("utf-8")).digest()


def stable_value(block_id: str, x: int, y: int, salt: str = "grain") -> int:
    payload = f"{block_id}:{salt}:{x}:{y}".encode("utf-8")
    return hashlib.sha256(payload).digest()[0]


def infer_tier(block_id: str) -> int:
    for tier in range(6, 1, -1):
        if f"tier_{tier}" in block_id or f"_g{tier}" in block_id:
            return tier
    if "high" in block_id:
        return 3
    if "mid" in block_id:
        return 2
    return 1


def infer_accent(block_id: str) -> str:
    groups = (
        ("blood", ("blood",)),
        ("thunder", ("thunder",)),
        ("fire", ("fire", "flame", "alchemy", "ancient_rift")),
        ("wind", ("wind", "mulan")),
        ("void", ("void", "rift", "hidden", "cycle", "illusion")),
        ("yin", ("yin", "demon", "nether")),
        ("water", ("ferry", "teleport")),
        ("metal", ("sword", "forge", "iron", "vajra")),
        ("earth", ("earth", "mountain", "defense")),
        ("wood", ("herb", "planter", "puppet", "talisman")),
        ("ascension", ("ascension", "king_territory")),
        ("spirit", ("spirit", "ling_gen", "formation", "array", "barrier")),
    )
    for accent, tokens in groups:
        if any(token in block_id for token in tokens):
            return accent
    return "neutral"


# 0.2.267: core blocks for the authored single_core stations. Without these rules the wooden
# furniture and the copper ore all fall through to grey masonry.
SINGLE_CORE_ORES = {"kunwu_copper_ore"}
SINGLE_CORE_WORKSTATIONS = {
    "ice_crystal_cooler",
    "inner_sect_task_board",
    "market_stall_counter",
    "pill_cabinet",
    "scripture_pavilion_shelf",
    "spirit_vein_tap",
    "weapon_rack_artifact",
}


def infer_kind(block_id: str) -> str:
    if block_id in {"spirit_ore", "low_spirit_iron_ore", "yin_essence_ore"} or block_id.endswith("_spirit_ore"):
        return "ore"
    if block_id in SINGLE_CORE_ORES:
        return "ore"
    if block_id in SINGLE_CORE_WORKSTATIONS:
        return "workstation"
    if block_id == "leyline_surface_marker":
        return "marker"
    if block_id.startswith("alchemy_furnace") and block_id != "alchemy_furnace_array_node":
        return "furnace"
    if block_id.startswith("alchemy_lid"):
        return "lid"
    if block_id.startswith("ling_gen_identification_slab"):
        return "identification_slab"
    if block_id == "earth_wall":
        return "earth"
    if block_id == "meditation_cushion":
        return "cushion"
    if block_id == "sect_gate_array":
        return "formation"
    if "gate" in block_id or block_id == "portal_gate":
        return "gate"
    if block_id in {
        "puppet_assembly_bench",
        "refinement_forge",
        "refinement_forge_g2",
        "refinement_forge_g3",
        "refinement_forge_g4",
        "refinement_forge_g5",
        "refinement_forge_g6",
        "sect_earth_fire_room",
        "spirit_herb_planter",
        "talisman_table",
    }:
        return "workstation"
    if (
        block_id.endswith("_formation_core")
        or block_id in {
            "alchemy_furnace_array_node",
            "altar",
            "blood_sacrifice_altar",
            "formation_core",
            "long_range_teleport_array",
            "spirit_gathering_array",
            "teleport_array_pedestal",
            "thunder_tribulation_altar",
        }
    ):
        return "formation"
    return "masonry"


def infer_palette(block_id: str, kind: str) -> str:
    if kind == "earth":
        return "earth"
    if kind == "cushion":
        return "cloth"
    if kind == "workstation":
        if block_id in {"talisman_table", "puppet_assembly_bench", "spirit_herb_planter"}:
            return "wood"
        if block_id == "sect_earth_fire_room":
            return "deepstone"
        # Sect furniture reads as lacquered wood; the two spirit-tech fittings read as jade.
        if block_id in {"inner_sect_task_board", "market_stall_counter", "pill_cabinet",
                        "scripture_pavilion_shelf", "weapon_rack_artifact"}:
            return "wood"
        if block_id in {"ice_crystal_cooler", "spirit_vein_tap"}:
            return "jade"
        return "metal"
    if kind in {"furnace", "lid"}:
        return ("bronze", "bronze", "metal", "jade", "obsidian")[min(5, infer_tier(block_id)) - 1]
    if kind == "ore":
        if block_id == "yin_essence_ore":
            return "deepstone"
        if block_id == "kunwu_copper_ore":
            return "bronze"
        return "stone"
    if kind == "marker":
        return "jade"
    if "demon" in block_id or "hidden" in block_id or "nether" in block_id or "cycle" in block_id:
        return "obsidian"
    if "spirit" in block_id or "ling_gen" in block_id:
        return "jade"
    if "blood" in block_id or "ancient_rift" in block_id:
        return "deepstone"
    if "forge" in block_id or "sword" in block_id or "thunder" in block_id or "vajra" in block_id:
        return "metal"
    return "stone"


def split_texture_face(block_id: str) -> tuple[str, str]:
    for suffix, face in (("_side", "side"), ("_top", "top")):
        if block_id.endswith(suffix):
            return block_id.removesuffix(suffix), face
    return block_id, "all"


def build_spec(block_id: str) -> BlockSpec:
    base_id, face = split_texture_face(block_id)
    kind = infer_kind(base_id)
    return BlockSpec(
        block_id=block_id,
        base_id=base_id,
        face=face,
        kind=kind,
        motif=MOTIF_OVERRIDES.get(base_id, kind),
        palette=infer_palette(base_id, kind),
        accent=infer_accent(base_id),
        tier=infer_tier(base_id),
        formed=base_id.endswith("_formed"),
        seed=stable_bytes(block_id),
    )


def extract_local_texture_refs(data: object) -> set[str]:
    refs: set[str] = set()
    stack = [data]
    while stack:
        value = stack.pop()
        if isinstance(value, dict):
            textures = value.get("textures")
            if isinstance(textures, dict):
                for texture in textures.values():
                    if isinstance(texture, str) and texture.startswith("seeking_immortals:block/"):
                        refs.add(texture.removeprefix("seeking_immortals:block/"))
            stack.extend(value.values())
        elif isinstance(value, list):
            stack.extend(value)
    return refs


def discover_texture_ids() -> list[str]:
    ids = {path.stem for path in TEXTURE_DIR.glob("*.png")}
    ids.update(COMPAT_TEXTURE_IDS)
    for directory in (BLOCK_MODEL_DIR, ITEM_MODEL_DIR):
        for path in directory.rglob("*.json"):
            try:
                data = json.loads(path.read_text(encoding="utf-8"))
            except (OSError, json.JSONDecodeError):
                continue
            ids.update(extract_local_texture_refs(data))
    return sorted(ids)


class BlockPainter:
    def __init__(self, spec: BlockSpec):
        self.spec = spec
        self.image = Image.new("RGBA", (OUTPUT_SIZE, OUTPUT_SIZE), (0, 0, 0, 255))
        self.draw = ImageDraw.Draw(self.image)
        self.base, self.light, self.dark, default_accent = PALETTES[spec.palette]
        semantic_accent = ACCENTS[spec.accent]
        self.accent = mix(default_accent, semantic_accent, 0.62)
        self.glow = mix(self.accent, (250, 239, 185), 0.35)

    def material(self, palette: str | None = None, contrast: int = 13) -> None:
        base, light, dark, _ = PALETTES[palette or self.spec.palette]
        for y in range(OUTPUT_SIZE):
            for x in range(OUTPUT_SIZE):
                coarse = stable_value(self.spec.block_id, x // 2, y // 2, "coarse") % 11 - 5
                fine = stable_value(self.spec.block_id, x, y, "fine") % 7 - 3
                band = 2 if (x + 2 * y + self.spec.seed[2]) % 9 == 0 else 0
                amount = round((coarse * 0.65 + fine * 0.55 + band) * contrast / 9)
                color = vary(base, amount)
                if stable_value(self.spec.block_id, x, y, "fleck") > 247:
                    color = mix(color, light, 0.24)
                elif stable_value(self.spec.block_id, x, y, "pit") < 7:
                    color = mix(color, dark, 0.28)
                self.image.putpixel((x, y), (*color, 255))

    def frame(self, inset: int = 1, accent: bool = False) -> None:
        outer = mix(self.dark, (16, 17, 18), 0.28)
        inner = mix(self.light, self.accent if accent else self.base, 0.24)
        self.draw.rectangle((inset, inset, 15 - inset, 15 - inset), outline=outer)
        if inset + 1 <= 7:
            self.draw.line((inset + 1, inset + 1, 14 - inset, inset + 1), fill=inner)
            self.draw.line((inset + 1, inset + 1, inset + 1, 14 - inset), fill=inner)

    def rune_circle(self, color: tuple[int, int, int] | None = None, radius: int = 5) -> None:
        ink = color or self.accent
        cx = cy = 7
        self.draw.ellipse((cx - radius, cy - radius, cx + radius + 1, cy + radius + 1), outline=ink)
        for x, y in ((cx, cy - radius - 1), (cx + radius + 1, cy), (cx, cy + radius + 1), (cx - radius - 1, cy)):
            if 0 <= x < 16 and 0 <= y < 16:
                self.draw.point((x, y), fill=self.glow)

    def ore(self) -> None:
        self.material(contrast=17)
        block_id = self.spec.base_id
        if block_id == "spirit_ore":
            vein = ACCENTS["spirit"]
            secondary = (213, 188, 96)
        elif block_id == "metal_spirit_ore":
            vein = (204, 214, 209)
            secondary = (132, 170, 160)
        elif block_id == "wood_spirit_ore":
            vein = ACCENTS["wood"]
            secondary = (181, 205, 103)
        elif block_id == "water_spirit_ore":
            vein = ACCENTS["water"]
            secondary = (104, 211, 220)
        elif block_id == "fire_spirit_ore":
            vein = ACCENTS["fire"]
            secondary = (240, 185, 72)
        elif block_id == "earth_spirit_ore":
            vein = ACCENTS["earth"]
            secondary = (217, 187, 100)
        elif block_id == "low_spirit_iron_ore":
            vein = (157, 169, 166)
            secondary = (91, 183, 164)
        else:
            vein = ACCENTS["yin"]
            secondary = (84, 136, 170)
        paths = (
            ((-1, 3), (3, 5), (5, 9), (9, 10), (12, 14), (16, 13)),
            ((2, -1), (4, 3), (8, 5), (11, 4), (14, 7), (16, 8)),
            ((-1, 13), (3, 12), (6, 14), (9, 13)),
        )
        offset = self.spec.seed[1] % len(paths)
        for index in range(2):
            points = paths[(offset + index) % len(paths)]
            color = vein if index == 0 else secondary
            self.draw.line(points, fill=shade(color, 0.62), width=2)
            self.draw.line(points, fill=color)
            for point_index, (x, y) in enumerate(points[1:-1]):
                if (self.spec.seed[point_index + 4] + index) % 2 == 0:
                    self.draw.point((x, max(0, y - 1)), fill=mix(color, (244, 241, 210), 0.45))

    def model_side(self) -> None:
        self.material(contrast=10)
        lower = mix(self.dark, (19, 20, 22), 0.28)
        trim = mix(self.accent, self.light, 0.24)
        self.draw.line((0, 1, 15, 1), fill=trim)
        self.draw.line((0, 4, 15, 4), fill=self.dark)
        self.draw.line((0, 11, 15, 11), fill=lower)
        self.draw.line((0, 14, 15, 14), fill=trim)
        for x in (2, 7, 12):
            self.draw.rectangle((x, 6, x + 1, 9), fill=mix(self.base, self.accent, 0.22))
            self.draw.point((x, 6), fill=self.glow)

    def model_top(self) -> None:
        block_id = self.spec.base_id
        if block_id.startswith("alchemy_furnace"):
            self.material(contrast=10)
            self.frame(0, accent=True)
            rim = mix(self.accent, (223, 186, 104), min(0.52, self.spec.tier * 0.08))
            self.draw.ellipse((2, 2, 13, 13), fill=mix(self.base, self.dark, 0.14), outline=self.dark)
            self.draw.ellipse((4, 4, 11, 11), fill=(25, 23, 25), outline=rim)
            self.draw.ellipse((6, 6, 9, 9), fill=shade(ACCENTS["fire"], 0.45))
            self.draw.point((7, 7), fill=mix(self.glow, ACCENTS["fire"], 0.36))
            for index in range(min(self.spec.tier, 5)):
                x, y = ((7, 2), (12, 7), (7, 12), (2, 7), (10, 10))[index]
                self.draw.point((x, y), fill=rim)
            return
        if block_id.startswith("refinement_forge"):
            self.material("metal", contrast=11)
            self.frame(0, accent=True)
            tier_trim = {
                1: (190, 167, 116),
                2: (87, 184, 174),
                3: (111, 157, 205),
                4: (221, 166, 69),
                5: (171, 112, 202),
                6: (215, 225, 211),
            }[self.spec.tier]
            trim = mix(tier_trim, self.accent, 0.18)
            self.draw.rectangle((2, 2, 13, 13), fill=mix(self.base, self.dark, 0.16), outline=self.dark)
            self.draw.rectangle((4, 4, 11, 11), outline=trim)
            self.draw.polygon(((5, 6), (10, 6), (12, 8), (10, 10), (5, 10), (3, 8)), fill=shade(self.dark, 0.62), outline=trim)
            self.draw.line((5, 8, 10, 8), fill=self.glow)
            return
        self.material(contrast=9)
        self.frame(0, accent=True)

    def marker(self) -> None:
        self.material("jade", contrast=12)
        self.frame(0, accent=True)
        self.draw.line((2, 13, 6, 9, 8, 5, 12, 2), fill=shade(self.accent, 0.62), width=2)
        self.draw.line((3, 12, 6, 9, 8, 5, 12, 2), fill=self.accent)
        self.draw.line((7, 14, 8, 10, 12, 7, 14, 5), fill=self.glow)
        for x, y in ((3, 4), (6, 12), (11, 10), (13, 3)):
            self.draw.rectangle((x, y, x + 1, y + 1), fill=self.accent)
        self.draw.point((8, 5), fill=(239, 229, 161))

    def furnace(self) -> None:
        self.material(contrast=12)
        self.frame(0)
        tier_trim = mix(self.accent, (226, 190, 109), min(0.58, self.spec.tier * 0.09))
        self.draw.rectangle((2, 3, 13, 14), fill=mix(self.base, self.dark, 0.22), outline=self.dark)
        self.draw.line((3, 4, 12, 4), fill=self.light)
        self.draw.rectangle((4, 6, 11, 12), fill=shade(self.dark, 0.66), outline=tier_trim)
        self.draw.rectangle((5, 8, 10, 12), fill=(24, 23, 25))
        if self.spec.formed:
            ember = self.glow
            self.draw.rectangle((6, 9, 9, 11), fill=mix(ember, ACCENTS["fire"], 0.35))
            self.draw.point((7, 8), fill=(250, 236, 173))
            self.draw.line((3, 5, 3, 12), fill=tier_trim)
            self.draw.line((12, 5, 12, 12), fill=tier_trim)
        else:
            self.draw.rectangle((6, 11, 9, 12), fill=shade(ACCENTS["fire"], 0.52))
        for x in (2, 13):
            for y in (2, 13):
                self.draw.point((x, y), fill=tier_trim)
        for mark in range(min(self.spec.tier, 5)):
            self.draw.point((5 + mark, 2), fill=tier_trim)

    def lid(self) -> None:
        self.material(contrast=10)
        self.frame(0)
        trim = mix(self.accent, (218, 181, 104), min(0.55, self.spec.tier * 0.08))
        self.draw.ellipse((2, 2, 13, 13), fill=mix(self.base, self.dark, 0.10), outline=self.dark)
        self.draw.ellipse((4, 4, 11, 11), outline=trim)
        self.draw.rectangle((6, 6, 9, 9), fill=mix(self.base, trim, 0.28), outline=self.dark)
        self.draw.point((7, 6), fill=self.glow)
        for index in range(min(5, self.spec.tier)):
            x, y = ((3, 7), (7, 3), (12, 7), (7, 12), (10, 10))[index]
            self.draw.point((x, y), fill=trim)

    def formation_symbol(self) -> None:
        motif = self.spec.motif
        ink = self.glow
        shadow = shade(self.accent, 0.58)
        if motif in {"gathering", "minor_gathering", "formation", "furnace_node"}:
            self.draw.ellipse((5, 5, 10, 10), outline=ink)
            self.draw.point((7, 7), fill=ink)
            if motif != "minor_gathering":
                for point in ((7, 3), (12, 7), (7, 12), (3, 7)):
                    self.draw.rectangle((point[0], point[1], point[0] + 1, point[1] + 1), fill=self.accent)
        elif motif in {"shield", "barrier"}:
            self.draw.polygon(((7, 3), (11, 5), (10, 10), (7, 13), (4, 10), (3, 5)), fill=shadow, outline=ink)
            self.draw.line((7, 4, 7, 11), fill=ink)
        elif motif == "sword":
            self.draw.line((4, 11, 11, 4), fill=shadow, width=3)
            self.draw.line((4, 11, 11, 4), fill=ink)
            self.draw.line((3, 9, 6, 12), fill=ink)
        elif motif in {"seal", "pillar_seal"}:
            self.draw.rectangle((5, 3, 10, 12), outline=ink)
            self.draw.line((7, 4, 7, 11), fill=shadow)
            self.draw.line((5, 7, 10, 7), fill=ink)
            self.draw.point((7, 5), fill=ink)
            self.draw.point((8, 10), fill=ink)
        elif motif == "maze":
            self.draw.line((4, 4, 11, 4, 11, 11, 5, 11, 5, 6, 9, 6, 9, 9, 7, 9, 7, 7), fill=ink)
        elif motif == "ascension":
            self.draw.line((7, 12, 7, 4), fill=ink, width=2)
            self.draw.line((4, 7, 7, 3, 11, 7), fill=ink)
            self.draw.line((4, 11, 11, 11), fill=shadow)
        elif motif == "five_elements":
            points = ((7, 3), (12, 6), (10, 12), (4, 12), (2, 6))
            self.draw.line(points + (points[0],), fill=ink)
            for x, y in points:
                self.draw.rectangle((x, y, x + 1, y + 1), fill=self.accent)
        elif motif == "inverted_five":
            self.draw.polygon(((3, 4), (12, 4), (7, 12)), outline=ink)
            self.draw.line((4, 10, 11, 10, 7, 3), fill=shadow)
        elif motif == "flame_ring":
            self.draw.ellipse((3, 3, 12, 12), outline=ink)
            self.draw.arc((5, 5, 10, 11), 40, 300, fill=ACCENTS["fire"], width=2)
            self.draw.point((7, 6), fill=(246, 205, 93))
        elif motif == "wind":
            self.draw.arc((2, 3, 11, 8), 205, 350, fill=ink, width=2)
            self.draw.arc((4, 6, 13, 12), 25, 175, fill=self.accent, width=2)
            self.draw.line((4, 11, 9, 11), fill=shadow)
        elif motif == "vajra":
            self.draw.polygon(((7, 2), (10, 5), (8, 7), (11, 10), (7, 13), (4, 10), (6, 7), (3, 5)), outline=ink)
            self.draw.line((7, 3, 7, 12), fill=self.accent)
        elif motif == "sword_bagua":
            self.draw.ellipse((3, 3, 12, 12), outline=ink)
            self.draw.line((7, 3, 7, 12), fill=shadow)
            self.draw.arc((4, 4, 10, 9), 270, 90, fill=self.accent)
            self.draw.arc((5, 7, 11, 12), 90, 270, fill=ink)
        elif motif == "thunder":
            self.draw.line((9, 2, 5, 8, 8, 8, 5, 13, 11, 6, 8, 6), fill=ink, width=2)
        elif motif == "teleport":
            self.draw.ellipse((3, 3, 12, 12), outline=ink)
            self.draw.ellipse((5, 5, 10, 10), outline=self.accent)
            self.draw.line((7, 2, 7, 5), fill=ink)
            self.draw.line((7, 10, 7, 13), fill=ink)
            self.draw.line((2, 7, 5, 7), fill=ink)
            self.draw.line((10, 7, 13, 7), fill=ink)
        elif motif == "sect_array":
            self.draw.polygon(((7, 3), (11, 7), (7, 12), (3, 7)), outline=ink)
            self.draw.rectangle((5, 5, 9, 9), outline=self.accent)
            self.draw.point((7, 7), fill=ink)
        elif motif in {"altar", "blood_altar"}:
            self.draw.polygon(((3, 11), (5, 5), (10, 5), (12, 11)), fill=shadow, outline=ink)
            self.draw.line((4, 11, 11, 11), fill=ink)
            self.draw.ellipse((6, 3, 9, 6), fill=self.accent, outline=ink)
        else:
            self.draw.polygon(((7, 3), (11, 7), (7, 12), (3, 7)), outline=ink)
            self.draw.point((7, 7), fill=self.accent)

    def formation(self) -> None:
        self.material(contrast=9)
        self.frame(0, accent=True)
        self.rune_circle(shade(self.accent, 0.64), radius=6)
        self.formation_symbol()
        for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
            self.draw.point((x, y), fill=self.accent)

    def gate(self) -> None:
        self.material(contrast=12)
        mortar = mix(self.dark, (24, 24, 28), 0.35)
        for y in (3, 7, 11, 15):
            self.draw.line((0, y, 15, y), fill=mortar)
        offset = 2 if self.spec.seed[3] % 2 else 0
        for y in (0, 4, 8, 12):
            for x in range(offset if (y // 4) % 2 else 0, 16, 5):
                self.draw.point((x, y + 3 if y + 3 < 16 else 15), fill=self.light)
        portal = mix(self.accent, self.dark, 0.35)
        self.draw.rectangle((3, 2, 12, 14), fill=self.dark, outline=self.light)
        self.draw.rectangle((5, 4, 10, 14), fill=portal, outline=self.accent)
        for y in range(5, 14, 2):
            self.draw.point((7 + (y + self.spec.seed[0]) % 3, y), fill=self.glow)
        if self.spec.block_id == "cycle_gate":
            self.draw.ellipse((5, 5, 10, 10), outline=self.glow)
        elif self.spec.block_id == "ascension_gate":
            self.draw.line((7, 12, 7, 6, 5, 8, 7, 5, 9, 8), fill=self.glow)
        elif "blood" in self.spec.block_id:
            self.draw.line((7, 5, 9, 8, 7, 12, 5, 8, 7, 5), fill=self.glow)
        elif "rift" in self.spec.block_id:
            self.draw.line((8, 4, 6, 7, 9, 9, 7, 14), fill=self.glow)

    def workstation(self) -> None:
        self.material(contrast=13)
        self.frame(0)
        block_id = self.spec.block_id
        if block_id.startswith("refinement_forge"):
            tier = self.spec.tier
            trim = {
                1: (190, 167, 116),
                2: (87, 184, 174),
                3: (111, 157, 205),
                4: (221, 166, 69),
                5: (171, 112, 202),
                6: (215, 225, 211),
            }[tier]
            self.draw.rectangle((2, 4, 13, 12), fill=mix(self.base, self.dark, 0.20), outline=self.dark)
            self.draw.rectangle((4, 6, 11, 10), fill=shade(self.dark, 0.62), outline=trim)
            self.draw.line((5, 8, 10, 8), fill=self.glow)
            for index in range(min(tier, 6)):
                self.draw.point((4 + index, 3), fill=trim)
            self.draw.point((2, 13), fill=trim)
            self.draw.point((13, 13), fill=trim)
        elif block_id == "talisman_table":
            self.draw.rectangle((2, 2, 13, 13), outline=self.dark)
            self.draw.rectangle((4, 3, 11, 12), fill=(190, 164, 108), outline=(70, 39, 30))
            self.draw.line((7, 4, 6, 7, 9, 8, 6, 11), fill=(165, 43, 40))
        elif block_id == "puppet_assembly_bench":
            self.draw.line((1, 5, 14, 5), fill=self.light)
            self.draw.line((3, 10, 12, 10), fill=self.dark, width=2)
            self.draw.line((5, 3, 10, 12), fill=self.accent)
            self.draw.rectangle((2, 2, 4, 4), outline=self.dark)
            self.draw.rectangle((11, 11, 13, 13), outline=self.dark)
        elif block_id == "spirit_herb_planter":
            soil = (82, 57, 37)
            self.draw.rectangle((2, 3, 13, 13), fill=soil, outline=self.dark)
            self.draw.line((3, 6, 12, 6), fill=(118, 81, 47))
            self.draw.line((3, 10, 12, 10), fill=(58, 43, 31))
            self.draw.line((7, 12, 7, 5), fill=(71, 135, 72))
            self.draw.polygon(((7, 6), (4, 4), (5, 8)), fill=(96, 174, 91))
            self.draw.polygon(((8, 8), (11, 5), (10, 10)), fill=(78, 151, 77))
        else:
            self.draw.rectangle((2, 2, 13, 13), outline=self.dark)
            self.draw.rectangle((5, 5, 10, 10), fill=shade(ACCENTS["fire"], 0.66), outline=self.accent)
            self.draw.point((7, 7), fill=self.glow)

    def earth(self) -> None:
        self.material("earth", contrast=18)
        for y in (3, 7, 11):
            self.draw.line((0, y, 15, y), fill=mix(self.dark, self.base, 0.35))
            start = (self.spec.seed[y] % 4) - 1
            for x in range(start, 16, 5):
                self.draw.line((x, y, min(15, x + 2), y), fill=mix(self.light, self.base, 0.42))
        for index in range(8):
            x = self.spec.seed[index] % 16
            y = self.spec.seed[index + 8] % 16
            self.draw.point((x, y), fill=self.light if index % 2 else self.dark)

    def cushion(self) -> None:
        self.material("cloth", contrast=8)
        self.frame(0)
        self.draw.rectangle((2, 2, 13, 13), outline=mix(self.accent, (220, 174, 91), 0.38))
        self.draw.polygon(((7, 3), (12, 7), (7, 12), (3, 7)), fill=mix(self.base, self.light, 0.10), outline=self.dark)
        self.draw.ellipse((5, 5, 9, 9), outline=self.accent)
        self.draw.point((7, 7), fill=self.glow)
        for y in range(1, 15, 2):
            self.draw.point((1, y), fill=self.light)
            self.draw.point((14, y), fill=self.dark)

    def identification_slab(self) -> None:
        suffix = self.spec.block_id.removeprefix("ling_gen_identification_slab")
        if suffix == "_top":
            self.material("jade", contrast=9)
            self.frame(0, accent=True)
            self.draw.ellipse((3, 3, 12, 12), outline=self.accent)
            self.draw.line((7, 3, 7, 12), fill=self.glow)
            self.draw.line((3, 7, 12, 7), fill=self.glow)
            self.draw.point((7, 7), fill=(236, 222, 148))
        elif suffix == "_side":
            self.material("stone", contrast=10)
            self.draw.rectangle((0, 0, 15, 3), fill=mix(self.base, self.accent, 0.22))
            self.draw.line((0, 3, 15, 3), fill=self.glow)
            self.draw.line((0, 11, 15, 11), fill=self.dark)
            for x in (2, 7, 12):
                self.draw.rectangle((x, 5, x + 1, 8), fill=mix(self.base, self.accent, 0.20))
        elif suffix == "_bottom":
            self.material("stone", contrast=8)
            self.frame(1)
            self.draw.line((3, 7, 12, 7), fill=self.dark)
            self.draw.line((7, 3, 7, 12), fill=self.dark)
        else:
            self.material("jade", contrast=8)
            self.frame(0, accent=True)
            self.draw.ellipse((4, 4, 11, 11), outline=self.accent)

    def masonry(self) -> None:
        self.material(contrast=12)
        self.frame(0)
        for y in (5, 10):
            self.draw.line((1, y, 14, y), fill=self.dark)
        for x, y in ((5, 1), (10, 6), (4, 11)):
            self.draw.line((x, y, x, min(14, y + 4)), fill=self.dark)
        self.draw.point((7, 7), fill=self.accent)

    def render(self) -> Image.Image:
        if self.spec.kind == "identification_slab":
            self.identification_slab()
            return self.image
        if self.spec.face == "side":
            self.model_side()
            return self.image
        if self.spec.face == "top":
            self.model_top()
            return self.image
        renderers = {
            "cushion": self.cushion,
            "earth": self.earth,
            "formation": self.formation,
            "furnace": self.furnace,
            "gate": self.gate,
            "lid": self.lid,
            "marker": self.marker,
            "masonry": self.masonry,
            "ore": self.ore,
            "workstation": self.workstation,
        }
        renderers[self.spec.kind]()
        return self.image


def ensure_unique(images: dict[str, Image.Image]) -> int:
    seen: dict[str, str] = {}
    adjustments = 0
    for block_id in sorted(images):
        image = images[block_id]
        attempt = 0
        while True:
            digest = hashlib.sha256(image.tobytes()).hexdigest()
            if digest not in seen:
                seen[digest] = block_id
                break
            seed = stable_bytes(f"{block_id}:unique:{attempt}")
            x = 2 + seed[0] % 12
            y = 2 + seed[1] % 12
            red, green, blue, alpha = image.getpixel((x, y))
            image.putpixel(
                (x, y),
                (
                    clamp(red + 5 + seed[2] % 11),
                    clamp(green + 3 + seed[3] % 7),
                    clamp(blue + 4 + seed[4] % 9),
                    alpha,
                ),
            )
            attempt += 1
            adjustments += 1
            if attempt > 24:
                raise RuntimeError(f"failed to make block texture unique: {block_id}")
    return adjustments


def referenced_block_textures() -> tuple[set[str], list[str]]:
    refs: set[str] = set()
    issues: list[str] = []
    for directory in (BLOCK_MODEL_DIR, ITEM_MODEL_DIR):
        for path in sorted(directory.rglob("*.json")):
            try:
                data = json.loads(path.read_text(encoding="utf-8"))
            except (OSError, json.JSONDecodeError) as exc:
                issues.append(f"{path.relative_to(ASSETS)}: invalid JSON ({exc})")
                continue
            refs.update(extract_local_texture_refs(data))
    for path in sorted(BLOCKSTATE_DIR.glob("*.json")):
        try:
            json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as exc:
            issues.append(f"{path.relative_to(ASSETS)}: invalid JSON ({exc})")
    return refs, issues


def audit_textures(texture_ids: Iterable[str]) -> dict:
    expected_ids = sorted(texture_ids)
    issues: list[str] = []
    pixel_hashes: dict[str, list[str]] = defaultdict(list)
    opaque_ratios: list[float] = []
    paths = sorted(TEXTURE_DIR.glob("*.png"))
    actual_ids = {path.stem for path in paths}
    missing_files = sorted(set(expected_ids) - actual_ids)
    if missing_files:
        issues.append(f"missing generated textures: {', '.join(missing_files[:8])}")
    for path in paths:
        try:
            with Image.open(path) as image:
                image.load()
                if image.size != (OUTPUT_SIZE, OUTPUT_SIZE):
                    issues.append(f"{path.name}: size {image.size}, expected {(OUTPUT_SIZE, OUTPUT_SIZE)}")
                if image.mode != "RGBA":
                    issues.append(f"{path.name}: mode {image.mode}, expected RGBA")
                rgba_image = image.convert("RGBA")
                alpha_values = rgba_image.getchannel("A").tobytes()
                ratio = sum(alpha == 255 for alpha in alpha_values) / len(alpha_values)
                opaque_ratios.append(ratio)
                if ratio != 1.0:
                    issues.append(f"{path.name}: fully opaque ratio {ratio:.3f}")
                digest = hashlib.sha256(rgba_image.tobytes()).hexdigest()
                pixel_hashes[digest].append(path.name)
        except Exception as exc:
            issues.append(f"{path.name}: {exc}")
    duplicate_groups = [names for names in pixel_hashes.values() if len(names) > 1]
    if duplicate_groups:
        issues.append(f"duplicate pixel groups: {len(duplicate_groups)}")
    refs, model_issues = referenced_block_textures()
    issues.extend(model_issues)
    missing_refs = sorted(refs - actual_ids)
    if missing_refs:
        issues.append(f"missing model texture references: {', '.join(missing_refs[:8])}")
    return {
        "count": len(paths),
        "duplicate_groups": duplicate_groups,
        "issues": issues,
        "model_reference_count": len(refs),
        "opaque_ratio_min": min(opaque_ratios) if opaque_ratios else 0.0,
        "opaque_ratio_max": max(opaque_ratios) if opaque_ratios else 0.0,
    }


def write_contact_sheet(specs: list[BlockSpec], images: dict[str, Image.Image], directory: Path) -> None:
    directory.mkdir(parents=True, exist_ok=True)
    font = ImageFont.load_default()
    columns = 6
    cell_width = 150
    cell_height = 100
    rows = math.ceil(len(specs) / columns)
    sheet = Image.new("RGBA", (columns * cell_width, rows * cell_height), (29, 28, 27, 255))
    draw = ImageDraw.Draw(sheet)
    for index, spec in enumerate(specs):
        x = (index % columns) * cell_width
        y = (index // columns) * cell_height
        texture = images[spec.block_id].resize((64, 64), Image.Resampling.NEAREST)
        sheet.alpha_composite(texture, (x + 4, y + 4))
        draw.text((x + 72, y + 8), spec.block_id[:20], fill=(232, 225, 206, 255), font=font)
        if len(spec.block_id) > 20:
            draw.text((x + 72, y + 20), spec.block_id[20:40], fill=(232, 225, 206, 255), font=font)
        draw.text((x + 72, y + 36), f"{spec.kind}/{spec.motif}"[:22], fill=(153, 194, 177, 255), font=font)
        tiled = Image.new("RGBA", (72, 24), (0, 0, 0, 255))
        tile = images[spec.block_id].resize((24, 24), Image.Resampling.NEAREST)
        for tile_x in range(3):
            tiled.alpha_composite(tile, (tile_x * 24, 0))
        sheet.alpha_composite(tiled, (x + 72, y + 60))
    sheet.save(directory / "block_textures.png", format="PNG", optimize=True)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="verify shipped textures without writing")
    parser.add_argument("--contact-sheets", type=Path, help="optional directory for a visual review sheet")
    args = parser.parse_args()

    if not TEXTURE_DIR.exists():
        raise SystemExit(f"missing block texture directory: {TEXTURE_DIR}")
    texture_ids = discover_texture_ids()
    specs = [build_spec(block_id) for block_id in texture_ids]
    images = {spec.block_id: BlockPainter(spec).render() for spec in specs}
    uniqueness_adjustments = ensure_unique(images)

    render_mismatches: list[str] = []
    if args.check:
        for block_id, expected in images.items():
            path = TEXTURE_DIR / f"{block_id}.png"
            if not path.exists():
                render_mismatches.append(block_id)
                continue
            try:
                with Image.open(path) as actual:
                    if actual.convert("RGBA").tobytes() != expected.tobytes():
                        render_mismatches.append(block_id)
            except Exception:
                render_mismatches.append(block_id)
    else:
        for block_id, image in images.items():
            image.save(TEXTURE_DIR / f"{block_id}.png", format="PNG", optimize=True)
        print(f"wrote {len(images)} block textures; uniqueness adjustments={uniqueness_adjustments}")

    audit = audit_textures(texture_ids)
    audit["kind_counts"] = dict(Counter(spec.kind for spec in specs))
    audit["render_mismatch_count"] = len(render_mismatches)
    audit["uniqueness_adjustments"] = uniqueness_adjustments
    if render_mismatches:
        audit["issues"].append(
            f"deterministic render mismatches: {len(render_mismatches)} "
            f"({', '.join(render_mismatches[:8])})"
        )
    print(json.dumps(audit, indent=2, sort_keys=True))

    if args.contact_sheets:
        write_contact_sheet(specs, images, args.contact_sheets)
        print(f"contact sheet: {args.contact_sheets / 'block_textures.png'}")
    return 1 if audit["issues"] else 0


if __name__ == "__main__":
    raise SystemExit(main())
