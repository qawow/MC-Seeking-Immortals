#!/usr/bin/env python3
"""Render the skill icon atlas used by the cultivation HUD.

The atlas is intentionally generated from the file names.  That keeps the
large, data-driven skill catalog maintainable while still giving every skill a
recognisable element and action silhouette.  Rendering is all integer pixel
art at 16x16, so ``--check`` can compare the generated pixels exactly.
"""

from __future__ import annotations

import argparse
import colorsys
import hashlib
import json
import math
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Callable, Iterable

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUTPUT_DIR = ROOT / "src/main/resources/assets/seeking_immortals/textures/gui/skill"
RESOURCE_DATA_DIR = ROOT / "src/main/resources/data/seeking_immortals"
CULTIVATION_DATA_DIR = RESOURCE_DATA_DIR / "cultivation"
TEXT_MATERIAL_DATA_DIR = RESOURCE_DATA_DIR / "text_material/techniques"
SIZE = 16


# Base colors are deliberately separated by element.  The renderer shifts the
# hue a few degrees from the stable file-name hash so skills in one family are
# related without becoming identical.
ELEMENT_COLORS: dict[str, tuple[int, int, int]] = {
    "fire": (218, 71, 38),
    "water": (46, 133, 201),
    "ice": (117, 190, 224),
    "wood": (65, 155, 82),
    "metal": (183, 190, 197),
    "earth": (155, 111, 60),
    "thunder": (151, 106, 224),
    "wind": (91, 190, 164),
    "blood": (181, 42, 54),
    "poison": (126, 169, 53),
    "soul": (153, 105, 193),
    "yin": (92, 70, 135),
    "void": (78, 67, 143),
    "time": (75, 157, 174),
    "light": (235, 186, 65),
    "star": (92, 126, 211),
    "beast": (177, 109, 58),
    "qi": (92, 186, 186),
    "neutral": (144, 124, 92),
}


ELEMENT_GROUPS: tuple[tuple[str, tuple[str, ...]], ...] = (
    ("blood", ("blood", "xue_", "crimson", "asura", "sacrifice")),
    ("thunder", ("thunder", "lightning", "storm", "lei_", "tribulation", "bolt")),
    ("ice", ("ice", "frost", "snow", "cold", "han_", "freezing", "xuanbing")),
    ("water", ("water", "sea", "ocean", "rain", "wave", "tide", "river", "mist", "whirlpool", "shui")),
    ("wind", ("wind", "gale", "cloud", "breeze", "air", "feng", "swift")),
    ("fire", ("fire", "flame", "ember", "lava", "huo", "lieyan", "phoenix", "molten")),
    ("poison", ("poison", "toxin", "venom", "corruption", "miasma", "toxic", "insect")),
    ("yin", ("yin", "ghost", "demon", "demonic", "nether", "underworld", "corpse", "dark")),
    ("void", ("void", "space", "rift", "chaos", "xutian", "spatial", "inverse")),
    ("time", ("time", "reversion", "stasis", "skip", "moment", "chronicle")),
    ("soul", ("soul", "spirit_sense", "divine_sense", "mind", "dream", "reincarnation", "ghost")),
    ("wood", ("wood", "grass", "herb", "leaf", "bamboo", "flower", "ginseng", "root", "vine", "moss", "green")),
    ("metal", ("metal", "iron", "copper", "gold", "silver", "sword", "blade", "needle", "axe", "ring")),
    ("earth", ("earth", "stone", "sand", "mountain", "soil", "rock", "wall", "ground", "bedrock")),
    ("light", ("light", "holy", "buddha", "vajra", "yang", "sun", "golden", "righteous", "heaven")),
    ("star", ("star", "moon", "celestial", "palace", "astral")),
    ("beast", ("beast", "dragon", "bird", "phoenix", "fox", "ape", "wolf", "tiger", "turtle", "snake", "beast")),
    ("qi", ("qi", "aura", "spirit", "cultivat", "condense", "refine", "meridian", "root")),
)


MOTIF_GROUPS: tuple[tuple[str, tuple[str, ...]], ...] = (
    ("pill", ("pill", "dan", "salve", "medicine", "antidote", "detoxify", "healing")),
    ("talisman", ("talisman", "charm", "seal", "edict", "cipher", "receipt", "ticket", "coupon", "token", "pass", "letter", "formula")),
    ("array", ("array", "formation", "zhen", "domain", "field", "wall_array", "circle")),
    ("time", ("time", "hourglass", "stasis", "reversion", "slow")),
    ("heal", ("heal", "recovery", "repair", "revive", "nourish", "rejuven", "life")),
    ("summon", ("summon", "call", "invoke", "beast_call", "avatar")),
    ("book", ("manual", "scripture", "scroll", "slip", "pages", "recipe", "lesson", "canon", "blueprint")),
    ("coin", ("auction", "market", "buy", "sell", "pay", "debt", "tax", "bid", "price", "favor", "token")),
    ("map", ("map", "compass", "navigation", "route", "appraisal", "lens", "probe", "scan", "sense", "eye", "glimpse")),
    ("mirror", ("mirror", "reflect", "reflection", "phantom", "illusion")),
    ("shield", ("shield", "barrier", "guard", "bastion", "defense", "ward", "protect", "prison")),
    ("armor", ("armor", "robe", "body", "skin", "hardness", "vajra_body", "shell")),
    ("sword", ("sword", "blade", "slash", "thrust", "cleave", "saber", "saber", "giant_sword", "short_sword")),
    ("spear", ("spear", "lance", "javelin", "needle", "dart", "spike", "bolt")),
    ("fist", ("fist", "palm", "punch", "hand", "claw", "crush", "smash")),
    ("flame", ("fireball", "flame", "fire", "burn", "explosion", "explode", "burst", "lava", "phoenix")),
    ("ice", ("ice", "frost", "snow", "freez", "blizzard", "cold")),
    ("lightning", ("thunder", "lightning", "bolt", "electric", "tribulation")),
    ("wave", ("water", "wave", "rain", "tide", "river", "sea", "whirlpool", "mist")),
    ("wind", ("wind", "gale", "cloud", "storm", "cyclone", "breeze")),
    ("mountain", ("mountain", "earth", "stone", "rock", "sand", "quake", "wall", "tower")),
    ("leaf", ("wood", "vine", "leaf", "bamboo", "herb", "grass", "flower", "root")),
    ("blood", ("blood", "sacrifice", "crimson", "red_mist", "bloodline")),
    ("poison", ("poison", "toxin", "venom", "miasma", "insect", "spore", "toxic")),
    ("soul", ("soul", "ghost", "corpse", "yin", "devour", "reincarnation", "spirit")),
    ("beast", ("beast", "dragon", "bird", "phoenix", "fox", "ape", "wolf", "tiger", "turtle", "snake", "pet", "tame")),
    ("puppet", ("puppet", "construct", "automaton", "qianzhu")),
    ("eye", ("eye", "sense", "scan", "read", "appraisal", "glimpse", "monitor")),
    ("portal", ("void", "rift", "space", "teleport", "portal", "boundary", "bridge")),
    ("step", ("escape", "step", "walk", "flash", "blink", "ride", "haste", "flee", "shift")),
    ("bind", ("bind", "lock", "chain", "net", "snare", "trap", "prison", "suppress", "capture")),
    ("sound", ("sound", "roar", "voice", "song", "zither", "note", "bell", "chant")),
    ("container", ("bag", "pouch", "bundle", "crate", "jar", "bottle", "flask", "bowl", "vial", "storage")),
    ("star", ("star", "moon", "sun", "heaven", "celestial", "light")),
    ("social", ("alliance", "team", "party", "ally", "rescue", "escort", "cooperate", "favor", "contest", "crew")),
)


@dataclass(frozen=True)
class SkillSpec:
    skill_id: str
    element: str
    motif: str
    variant: int
    seed: bytes


def clamp(value: int) -> int:
    return max(0, min(255, int(value)))


def mix(a: tuple[int, int, int], b: tuple[int, int, int], amount: float) -> tuple[int, int, int]:
    return tuple(clamp(round(x * (1.0 - amount) + y * amount)) for x, y in zip(a, b))


def shade(color: tuple[int, int, int], factor: float) -> tuple[int, int, int]:
    return tuple(clamp(round(channel * factor)) for channel in color)


def rgba(color: tuple[int, int, int], alpha: int = 255) -> tuple[int, int, int, int]:
    return color[0], color[1], color[2], clamp(alpha)


def shifted(color: tuple[int, int, int], seed: bytes) -> tuple[int, int, int]:
    h, s, v = colorsys.rgb_to_hsv(*(channel / 255.0 for channel in color))
    h = (h + (seed[0] / 255.0 - 0.5) * 0.055) % 1.0
    s = max(0.22, min(0.92, s + (seed[1] / 255.0 - 0.5) * 0.13))
    v = max(0.38, min(0.98, v + (seed[2] / 255.0 - 0.5) * 0.10))
    return tuple(round(channel * 255) for channel in colorsys.hsv_to_rgb(h, s, v))


def contains(text: str, terms: Iterable[str]) -> bool:
    return any(term in text for term in terms)


def flattened_pixels(image: Image.Image):
    """Use Pillow's warning-free API while retaining older Pillow support."""
    getter = getattr(image, "get_flattened_data", None)
    return getter() if getter is not None else image.getdata()


def load_metadata_directory(directory: Path) -> dict[str, dict]:
    metadata: dict[str, dict] = {}
    for path in sorted(directory.glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        if not isinstance(data, dict):
            continue
        for entry in data.get("techniques", []):
            if isinstance(entry, dict) and entry.get("id"):
                metadata[str(entry["id"])] = entry
    return metadata


def load_technique_metadata() -> tuple[dict[str, dict], set[str], set[str]]:
    cultivation = load_metadata_directory(CULTIVATION_DATA_DIR)
    text_material = load_metadata_directory(TEXT_MATERIAL_DATA_DIR)
    # Preserve the established text-material semantic mapping. Cultivation
    # data contributes runtime ids only; letting its legacy fields alter the
    # renderer would change already-published icons and become stateful after
    # the newly discovered files are written.
    return text_material, set(cultivation), set(text_material)


def normalized_element(value: str) -> str | None:
    value = value.lower()
    aliases = {
        "demon": "yin",
        "demonic": "yin",
        "ghost": "yin",
        "dark": "yin",
        "illusion": "soul",
        "yang": "light",
        "buddhist": "light",
        "space": "void",
        "spatial": "void",
        "movement": "wind",
        "sword": "metal",
        "elemental": "qi",
        "mixed": "qi",
        "spirit": "qi",
        "puppet": "metal",
        "recovery": "wood",
        "fashi": "qi",
        "earth_wind": "earth",
    }
    value = aliases.get(value, value)
    return value if value in ELEMENT_COLORS else None


def infer_element(skill_id: str, metadata: dict | None = None) -> str:
    if metadata:
        candidates = (metadata.get("element"), metadata.get("effect", {}).get("element"))
        for candidate in candidates:
            if candidate and (element := normalized_element(str(candidate))):
                return element
        attribute = str(metadata.get("attribute", "")).lower()
        attribute_aliases = (
            ("thunder", ("雷", "thunder", "lightning")),
            ("ice", ("冰", "ice", "frost")),
            ("water", ("水", "water")),
            ("fire", ("火", "炎", "fire", "flame")),
            ("wood", ("木", "wood")),
            ("metal", ("金", "剑", "metal", "sword")),
            ("earth", ("土", "earth")),
            ("wind", ("风", "wind")),
            ("blood", ("血", "blood")),
            ("poison", ("毒", "poison")),
            ("soul", ("魂", "神识", "神念", "soul")),
            ("yin", ("阴", "鬼", "魔", "yin", "ghost", "demon")),
            ("void", ("空间", "虚空", "space", "void")),
            ("light", ("佛", "阳", "光", "buddha", "yang", "light")),
            ("beast", ("妖", "兽", "虫", "beast", "insect")),
        )
        for element, terms in attribute_aliases:
            if contains(attribute, terms):
                return element
    text = skill_id.lower()
    for element, terms in ELEMENT_GROUPS:
        if contains(text, terms):
            return element
    return "neutral"


def element_motif(element: str) -> str:
    return {
        "fire": "flame",
        "water": "wave",
        "ice": "ice",
        "wood": "leaf",
        "earth": "mountain",
        "thunder": "lightning",
        "wind": "wind",
        "blood": "blood",
        "poison": "poison",
        "soul": "soul",
        "yin": "soul",
        "void": "portal",
        "time": "time",
        "light": "star",
        "star": "star",
        "beast": "beast",
    }.get(element, "orb")


def metadata_motif(metadata: dict, element: str) -> str | None:
    path = str(metadata.get("path") or metadata.get("school") or "").lower()
    effect_type = str(metadata.get("effect", {}).get("type") or metadata.get("type") or "").lower()
    if path == "formation" or effect_type in ("field", "domain", "buff_zone", "summon_field"):
        return "array"
    if path == "talisman" or effect_type == "talisman_consume":
        return "talisman"
    if path == "sword":
        return "sword"
    if path == "puppet":
        return "puppet"
    if path == "divine_sense" or effect_type in ("scan", "inspect", "scout"):
        return "eye"
    if path == "movement" or effect_type in ("dash", "movement", "escape"):
        return "step"
    if effect_type.startswith("teleport"):
        return "portal"
    if path == "recovery" or effect_type in ("heal", "heal_spirit", "cleanse"):
        return "heal"
    if path == "illusion" or effect_type == "transform":
        return "mirror"
    if path in ("ghost", "xuan_yin") or effect_type in ("soul_attack", "drain"):
        return "soul"
    if effect_type in ("summon", "command"):
        return "summon"
    if effect_type in ("control", "trap", "debuff"):
        return "bind"
    if path == "body":
        return "armor" if effect_type in ("buff", "buff_self") else "fist"
    if path == "buddhist":
        return "armor" if effect_type in ("buff", "buff_self") else "fist"
    if path in ("spirit_beast_mountain", "wanhu_sect"):
        return "beast"
    if path in ("demon", "demonic_six", "tianmo_sect", "xuewu_sect", "hehuan_sect", "guiling_gate", "qingluo_sect"):
        if effect_type in ("buff", "buff_self", "melee", "strike"):
            return "fist"
        return element_motif(element)
    if effect_type in ("wall",):
        return "shield"
    if effect_type in ("projectile", "beam", "aoe", "aoe_dot", "cone", "dot", "strike", "melee", "ultimate", "secret_art"):
        return element_motif(element)
    return None


def infer_motif(skill_id: str, metadata: dict | None = None, element: str | None = None) -> str:
    text = skill_id.lower()
    # Perception is an eye; actual map-like objects remain compass/map icons.
    if contains(text, ("sense", "scan", "_eye", "glimpse", "monitor", "mind_read")):
        return "eye"
    if contains(text, ("map", "compass", "navigation", "route", "appraisal_lens", "probe")):
        return "map"
    # A few broad words are intentionally checked after concrete objects.  For
    # example, ``fireball_talisman`` should read as a talisman, not a flame.
    for motif, terms in MOTIF_GROUPS:
        if contains(text, terms):
            return motif
    if metadata and (motif := metadata_motif(metadata, element or "neutral")):
        return motif
    if contains(text, ("kill", "snipe", "hunt", "strike", "harvest", "force_take", "rob_")):
        return "sword"
    if contains(text, ("contract", "bond", "commit", "oath", "command", "mandate")):
        return "bind"
    if contains(text, ("transform", "clone", "afterimage", "invisibility", "stealth", "hide_", "smuggle")):
        return "mirror"
    if contains(text, ("pearl", "sphere", "orb", "bead", "core", "essence", "inlay", "ring", "bracelet")):
        return "orb"
    if contains(text, ("ascend", "break", "success", "fail", "retry", "push", "accept", "reject", "ignore", "default")):
        return "sigil"
    if contains(text, ("cultivat", "qi", "aura", "spirit", "refine", "condense", "focus", "meditat")):
        return "orb"
    return "sigil"


def infer_variant(skill_id: str, seed: bytes) -> int:
    text = skill_id.lower()
    for marker, value in (("_3", 3), ("_2", 2), ("_1", 1), ("high", 3), ("advanced", 3), ("ultimate", 3), ("secret", 3), ("mid", 2), ("medium", 2), ("low", 1), ("basic", 1)):
        if marker in text:
            return value
    return 1 + seed[3] % 3


def build_specs(ids: Iterable[str], metadata: dict[str, dict] | None = None) -> list[SkillSpec]:
    metadata = metadata or {}
    specs = []
    for skill_id in sorted(ids):
        seed = hashlib.sha256(skill_id.encode("utf-8")).digest()
        entry = metadata.get(skill_id)
        element = infer_element(skill_id, entry)
        specs.append(SkillSpec(skill_id, element, infer_motif(skill_id, entry, element), infer_variant(skill_id, seed), seed))
    return specs


class Painter:
    """Small crisp-pixel painter.  All coordinates intentionally stay in 1..14."""

    def __init__(self, spec: SkillSpec):
        self.spec = spec
        self.image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
        self.draw = ImageDraw.Draw(self.image)
        base = shifted(ELEMENT_COLORS[spec.element], spec.seed)
        self.base = base
        self.bg = shade(base, 0.27)
        self.shadow = shade(base, 0.12)
        self.border = shade(base, 0.60)
        self.mid = base
        self.light = mix(base, (255, 245, 210), 0.62)
        self.glow = mix(base, (255, 255, 255), 0.82)

    def pixel(self, x: int, y: int, color: tuple[int, int, int], alpha: int = 255) -> None:
        if 0 <= x < SIZE and 0 <= y < SIZE:
            self.draw.point((x, y), fill=rgba(color, alpha))

    def panel(self) -> None:
        outline = [(3, 1), (12, 1), (14, 3), (14, 12), (12, 14), (3, 14), (1, 12), (1, 3)]
        self.draw.polygon([(x, y + 1) for x, y in outline], fill=rgba(self.shadow, 230))
        self.draw.polygon(outline, fill=rgba(self.border, 245))
        inner = [(3, 2), (12, 2), (13, 3), (13, 12), (12, 13), (3, 13), (2, 12), (2, 3)]
        self.draw.polygon(inner, fill=rgba(self.bg, 242))
        self.draw.line(outline + [outline[0]], fill=rgba(self.light, 190), width=1)
        # Element-specific atmospheric pixels keep otherwise similar motifs
        # readable at a glance without filling the transparent corners.
        if self.spec.element in ("fire", "light", "blood"):
            for x, y in ((3, 4), (12, 5), (4, 11)):
                self.pixel(x, y, self.mid, 210)
        elif self.spec.element in ("water", "ice", "wind"):
            self.draw.line([(3, 5), (5, 4)], fill=rgba(self.mid, 180), width=1)
            self.draw.line([(11, 11), (13, 10)], fill=rgba(self.mid, 180), width=1)
        elif self.spec.element in ("void", "yin", "soul", "time"):
            self.pixel(3, 4, self.border, 210)
            self.pixel(12, 11, self.border, 210)
        else:
            self.pixel(3, 4, self.border, 190)
            self.pixel(12, 11, self.border, 190)

    def orb(self, ring: bool = False) -> None:
        self.draw.ellipse((3, 3, 12, 12), fill=rgba(self.shadow, 255))
        self.draw.ellipse((4, 3, 11, 11), fill=rgba(self.mid, 255))
        if ring:
            self.draw.ellipse((5, 4, 10, 10), outline=rgba(self.light, 255), width=1)
        else:
            self.draw.ellipse((5, 4, 10, 10), fill=rgba(mix(self.mid, self.light, 0.28), 255))
        self.pixel(6, 5, self.glow)
        self.pixel(7, 5, self.glow)

    def pill(self) -> None:
        self.draw.ellipse((3, 4, 12, 12), fill=rgba(self.shadow))
        self.draw.ellipse((4, 4, 11, 11), fill=rgba(self.mid))
        self.draw.line([(5, 7), (10, 7)], fill=rgba(self.light), width=2)
        self.draw.line([(5, 8), (10, 8)], fill=rgba(self.base, 240), width=1)
        self.pixel(6, 5, self.glow)
        if self.spec.variant > 1:
            self.pixel(9, 10, self.glow)

    def talisman(self) -> None:
        points = [(4, 2), (12, 3), (11, 13), (3, 12)]
        self.draw.polygon([(x + 1, y + 1) for x, y in points], fill=rgba(self.shadow))
        self.draw.polygon(points, fill=rgba(self.light))
        self.draw.line([(5, 4), (10, 5), (6, 10), (10, 11)], fill=rgba(self.base), width=1)
        self.draw.line([(4, 8), (11, 8)], fill=rgba(self.glow), width=1)
        self.pixel(5, 3, self.glow)

    def array(self) -> None:
        self.draw.ellipse((2, 2, 13, 13), outline=rgba(self.shadow), width=2)
        self.draw.ellipse((4, 4, 11, 11), outline=rgba(self.light), width=1)
        self.draw.line([(8, 2), (8, 13)], fill=rgba(self.mid), width=1)
        self.draw.line([(2, 8), (13, 8)], fill=rgba(self.mid), width=1)
        self.draw.line([(4, 4), (12, 12)], fill=rgba(self.light, 190), width=1)
        self.draw.line([(12, 4), (4, 12)], fill=rgba(self.light, 190), width=1)
        self.pixel(8, 8, self.glow)

    def book(self) -> None:
        self.draw.polygon([(3, 3), (7, 4), (8, 5), (8, 13), (4, 12)], fill=rgba(self.shadow))
        self.draw.polygon([(8, 5), (12, 3), (13, 12), (8, 13)], fill=rgba(self.mid))
        self.draw.line([(8, 5), (8, 13)], fill=rgba(self.light), width=1)
        self.draw.line([(10, 5), (12, 5)], fill=rgba(self.light), width=1)
        self.draw.line([(10, 7), (12, 7)], fill=rgba(self.light), width=1)
        self.pixel(4, 4, self.glow)

    def coin(self) -> None:
        self.draw.ellipse((3, 3, 12, 12), fill=rgba(self.shadow))
        self.draw.ellipse((4, 4, 11, 11), fill=rgba(self.mid), outline=rgba(self.light), width=1)
        self.draw.line([(8, 5), (8, 10)], fill=rgba(self.glow), width=1)
        self.draw.line([(6, 7), (10, 7)], fill=rgba(self.glow), width=1)

    def map_icon(self) -> None:
        self.draw.ellipse((2, 3, 13, 12), fill=rgba(self.shadow))
        self.draw.ellipse((3, 4, 12, 11), fill=rgba(self.mid), outline=rgba(self.light), width=1)
        self.draw.line([(5, 9), (7, 6), (9, 9), (11, 5)], fill=rgba(self.glow), width=1)
        self.pixel(7, 6, self.glow)

    def mirror(self) -> None:
        self.draw.polygon([(8, 2), (13, 5), (13, 11), (8, 14), (3, 11), (3, 5)], fill=rgba(self.shadow))
        self.draw.polygon([(8, 3), (12, 5), (12, 10), (8, 13), (4, 10), (4, 5)], fill=rgba(self.mid))
        self.draw.line([(5, 6), (10, 11)], fill=rgba(self.glow), width=1)
        self.pixel(6, 5, self.light)

    def shield(self) -> None:
        points = [(8, 2), (13, 4), (12, 10), (8, 13), (4, 10), (3, 4)]
        self.draw.polygon([(x, y + 1) for x, y in points], fill=rgba(self.shadow))
        self.draw.polygon(points, fill=rgba(self.mid), outline=rgba(self.light))
        self.draw.line([(8, 4), (8, 11)], fill=rgba(self.glow), width=1)
        self.draw.line([(5, 7), (11, 7)], fill=rgba(self.glow), width=1)

    def armor(self) -> None:
        self.draw.polygon([(4, 3), (7, 2), (8, 4), (9, 2), (12, 3), (12, 8), (10, 9), (10, 13), (6, 13), (6, 9), (4, 8)], fill=rgba(self.shadow))
        self.draw.polygon([(5, 3), (7, 3), (8, 5), (9, 3), (11, 3), (11, 8), (9, 9), (9, 12), (7, 12), (7, 9), (5, 8)], fill=rgba(self.mid))
        self.draw.line([(8, 5), (8, 11)], fill=rgba(self.light), width=1)

    def sword(self) -> None:
        self.draw.polygon([(4, 12), (5, 10), (10, 4), (12, 2), (11, 5), (6, 11)], fill=rgba(self.shadow))
        self.draw.polygon([(5, 10), (10, 4), (12, 2), (11, 5), (6, 11)], fill=rgba(self.light))
        self.draw.line([(4, 11), (2, 13)], fill=rgba(self.mid), width=2)
        self.draw.line([(3, 11), (6, 13)], fill=rgba(self.glow), width=1)
        self.pixel(10, 4, self.glow)

    def spear(self) -> None:
        self.draw.line([(4, 13), (11, 4)], fill=rgba(self.shadow), width=3)
        self.draw.polygon([(11, 2), (13, 5), (10, 5)], fill=rgba(self.glow))
        self.draw.line([(4, 13), (11, 4)], fill=rgba(self.light), width=1)

    def fist(self) -> None:
        self.draw.polygon([(4, 6), (5, 4), (7, 4), (8, 3), (10, 4), (11, 6), (12, 7), (11, 11), (8, 13), (5, 11), (4, 9)], fill=rgba(self.shadow))
        self.draw.polygon([(5, 6), (6, 5), (7, 6), (8, 4), (9, 5), (10, 5), (10, 7), (12, 8), (10, 10), (8, 12), (6, 10), (5, 9)], fill=rgba(self.mid))
        self.draw.line([(6, 7), (10, 8)], fill=rgba(self.light), width=1)

    def flame(self) -> None:
        points = [(8, 2), (11, 6), (10, 8), (12, 10), (8, 13), (4, 11), (6, 8), (5, 6)]
        self.draw.polygon([(x, y + 1) for x, y in points], fill=rgba(self.shadow))
        self.draw.polygon(points, fill=rgba(self.mid))
        self.draw.polygon([(8, 5), (10, 8), (8, 11), (6, 9)], fill=rgba(self.light))
        self.pixel(8, 6, self.glow)

    def ice(self) -> None:
        self.draw.polygon([(8, 2), (11, 6), (10, 13), (8, 11), (6, 13), (5, 6)], fill=rgba(self.shadow))
        self.draw.polygon([(8, 2), (10, 6), (9, 12), (8, 10), (7, 12), (6, 6)], fill=rgba(self.light))
        self.draw.line([(8, 3), (8, 10)], fill=rgba(self.glow), width=1)

    def lightning(self) -> None:
        points = [(10, 2), (6, 7), (9, 7), (5, 14), (7, 9), (5, 9)]
        self.draw.line(points, fill=rgba(self.shadow), width=3, joint="curve")
        self.draw.line(points, fill=rgba(self.glow), width=1, joint="curve")
        self.pixel(9, 4, self.light)

    def wave(self) -> None:
        self.draw.arc((2, 3, 12, 12), 195, 350, fill=rgba(self.shadow), width=3)
        self.draw.arc((3, 4, 13, 13), 195, 350, fill=rgba(self.light), width=1)
        self.draw.line([(3, 11), (6, 9), (9, 11), (12, 8)], fill=rgba(self.glow), width=1)

    def wind(self) -> None:
        self.draw.arc((2, 3, 12, 11), 200, 345, fill=rgba(self.light), width=2)
        self.draw.arc((4, 6, 14, 13), 200, 345, fill=rgba(self.glow), width=1)
        self.draw.line([(3, 8), (8, 8)], fill=rgba(self.mid), width=1)

    def mountain(self) -> None:
        self.draw.polygon([(2, 12), (6, 5), (8, 8), (10, 3), (14, 12)], fill=rgba(self.shadow))
        self.draw.polygon([(3, 11), (6, 6), (8, 9), (10, 4), (13, 11)], fill=rgba(self.mid))
        self.draw.line([(6, 6), (7, 8)], fill=rgba(self.glow), width=1)
        self.draw.line([(10, 4), (11, 7)], fill=rgba(self.light), width=1)

    def leaf(self) -> None:
        self.draw.polygon([(3, 11), (4, 6), (8, 3), (12, 4), (11, 9), (7, 12)], fill=rgba(self.shadow))
        self.draw.polygon([(4, 10), (5, 6), (8, 4), (11, 5), (10, 8), (7, 11)], fill=rgba(self.mid))
        self.draw.line([(5, 10), (10, 5)], fill=rgba(self.glow), width=1)

    def blood(self) -> None:
        points = [(8, 2), (12, 8), (11, 11), (8, 13), (5, 11), (4, 8)]
        self.draw.polygon([(x, y + 1) for x, y in points], fill=rgba(self.shadow))
        self.draw.polygon(points, fill=rgba(self.mid))
        self.pixel(7, 5, self.glow)

    def poison(self) -> None:
        self.draw.rectangle((4, 6, 11, 12), fill=rgba(self.shadow))
        self.draw.rectangle((5, 7, 10, 11), fill=rgba(self.mid))
        self.draw.rectangle((6, 3, 9, 6), fill=rgba(self.light))
        self.draw.rectangle((5, 4, 10, 5), fill=rgba(self.light))
        self.pixel(7, 8, self.glow)

    def soul(self) -> None:
        self.draw.polygon([(5, 4), (8, 2), (11, 4), (12, 10), (10, 13), (8, 11), (6, 13), (4, 10)], fill=rgba(self.shadow))
        self.draw.polygon([(6, 4), (8, 3), (10, 4), (11, 10), (9, 11), (8, 9), (7, 11), (5, 10)], fill=rgba(self.mid))
        self.pixel(7, 6, self.glow)
        self.pixel(9, 6, self.glow)

    def beast(self) -> None:
        self.draw.polygon([(3, 5), (5, 5), (6, 3), (8, 4), (10, 3), (11, 5), (13, 5), (12, 11), (9, 13), (7, 11), (4, 12)], fill=rgba(self.shadow))
        self.draw.polygon([(4, 6), (6, 5), (7, 5), (8, 6), (9, 5), (11, 6), (11, 10), (9, 11), (8, 10), (7, 11), (5, 10)], fill=rgba(self.mid))
        self.pixel(7, 7, self.glow)
        self.pixel(9, 7, self.glow)

    def puppet(self) -> None:
        self.draw.rectangle((5, 4, 10, 11), fill=rgba(self.shadow))
        self.draw.rectangle((6, 5, 9, 10), fill=rgba(self.mid), outline=rgba(self.light))
        self.draw.line([(4, 6), (6, 7)], fill=rgba(self.light), width=1)
        self.draw.line([(10, 7), (12, 6)], fill=rgba(self.light), width=1)
        self.pixel(7, 7, self.glow)
        self.pixel(8, 7, self.glow)

    def eye(self) -> None:
        self.draw.polygon([(2, 8), (5, 5), (8, 4), (11, 5), (14, 8), (11, 11), (8, 12), (5, 11)], fill=rgba(self.shadow))
        self.draw.polygon([(3, 8), (6, 6), (8, 5), (10, 6), (13, 8), (10, 10), (8, 11), (6, 10)], fill=rgba(self.light))
        self.draw.ellipse((6, 6, 10, 10), fill=rgba(self.mid))
        self.pixel(8, 7, self.glow)

    def portal(self) -> None:
        self.draw.ellipse((2, 2, 13, 13), outline=rgba(self.shadow), width=2)
        self.draw.ellipse((4, 4, 11, 11), outline=rgba(self.light), width=2)
        self.draw.ellipse((6, 6, 9, 9), fill=rgba(self.mid))
        self.pixel(7, 7, self.glow)

    def step(self) -> None:
        self.draw.line([(3, 11), (8, 7), (12, 7)], fill=rgba(self.shadow), width=3)
        self.draw.line([(3, 10), (8, 6), (12, 6)], fill=rgba(self.light), width=1)
        self.draw.polygon([(12, 6), (9, 4), (9, 8)], fill=rgba(self.glow))

    def bind(self) -> None:
        self.draw.ellipse((3, 4, 8, 10), outline=rgba(self.shadow), width=2)
        self.draw.ellipse((8, 6, 13, 12), outline=rgba(self.light), width=2)
        self.draw.line([(6, 8), (10, 8)], fill=rgba(self.glow), width=2)

    def summon(self) -> None:
        self.draw.ellipse((3, 3, 12, 12), outline=rgba(self.light), width=1)
        self.draw.polygon([(8, 2), (10, 6), (8, 5), (6, 6)], fill=rgba(self.glow))
        self.draw.polygon([(13, 8), (9, 10), (10, 8), (9, 6)], fill=rgba(self.glow))
        self.pixel(8, 8, self.mid)

    def heal(self) -> None:
        self.draw.rectangle((6, 3, 9, 13), fill=rgba(self.shadow))
        self.draw.rectangle((3, 6, 12, 9), fill=rgba(self.shadow))
        self.draw.rectangle((7, 3, 8, 12), fill=rgba(self.glow))
        self.draw.rectangle((4, 7, 11, 8), fill=rgba(self.glow))

    def sound(self) -> None:
        self.draw.ellipse((3, 5, 7, 11), fill=rgba(self.shadow))
        self.draw.polygon([(6, 6), (9, 4), (9, 12), (6, 10)], fill=rgba(self.mid))
        self.draw.arc((8, 4, 14, 12), 285, 75, fill=rgba(self.light), width=1)
        self.draw.arc((9, 5, 15, 11), 285, 75, fill=rgba(self.glow), width=1)

    def container(self) -> None:
        self.draw.rectangle((4, 5, 11, 12), fill=rgba(self.shadow))
        self.draw.rectangle((5, 6, 10, 11), fill=rgba(self.mid), outline=rgba(self.light))
        self.draw.rectangle((6, 3, 9, 6), fill=rgba(self.light))
        self.pixel(7, 7, self.glow)

    def time(self) -> None:
        self.draw.polygon([(4, 3), (12, 3), (10, 7), (10, 9), (12, 13), (4, 13), (6, 9), (6, 7)], fill=rgba(self.shadow))
        self.draw.polygon([(5, 4), (11, 4), (9, 7), (9, 9), (11, 12), (5, 12), (7, 9), (7, 7)], fill=rgba(self.light))
        self.draw.line([(7, 8), (9, 10)], fill=rgba(self.mid), width=1)

    def star(self) -> None:
        points = [(8, 2), (9, 6), (13, 6), (10, 8), (11, 12), (8, 10), (5, 12), (6, 8), (3, 6), (7, 6)]
        self.draw.polygon([(x, y + 1) for x, y in points], fill=rgba(self.shadow))
        self.draw.polygon(points, fill=rgba(self.light))
        self.pixel(8, 6, self.glow)

    def social(self) -> None:
        self.draw.ellipse((3, 3, 7, 7), fill=rgba(self.light))
        self.draw.ellipse((9, 4, 13, 8), fill=rgba(self.mid))
        self.draw.arc((2, 6, 9, 14), 180, 350, fill=rgba(self.glow), width=2)
        self.draw.arc((8, 7, 14, 14), 180, 350, fill=rgba(self.light), width=2)

    def sigil(self) -> None:
        self.draw.polygon([(8, 2), (13, 8), (8, 14), (3, 8)], outline=rgba(self.light), fill=rgba(self.shadow))
        self.draw.line([(5, 8), (11, 8)], fill=rgba(self.glow), width=1)
        self.draw.line([(8, 5), (8, 11)], fill=rgba(self.mid), width=1)
        self.pixel(8, 8, self.glow)

    def signature(self) -> None:
        # Eight inner frame pixels encode 64 stable hash bits.  They are
        # deliberately low contrast, but make same-family variants distinct.
        positions = ((2, 3), (3, 2), (12, 2), (13, 3), (13, 12), (12, 13), (3, 13), (2, 12))
        for index, (x, y) in enumerate(positions):
            value = self.spec.seed[8 + index]
            amount = 0.06 + (value / 255.0) * 0.24
            color = mix(self.border, self.light, amount)
            self.pixel(x, y, color, 196 + value % 53)

    def render(self) -> Image.Image:
        self.panel()
        painters: dict[str, Callable[[], None]] = {
            "pill": self.pill,
            "talisman": self.talisman,
            "array": self.array,
            "book": self.book,
            "coin": self.coin,
            "map": self.map_icon,
            "mirror": self.mirror,
            "shield": self.shield,
            "armor": self.armor,
            "sword": self.sword,
            "spear": self.spear,
            "fist": self.fist,
            "flame": self.flame,
            "ice": self.ice,
            "lightning": self.lightning,
            "wave": self.wave,
            "wind": self.wind,
            "mountain": self.mountain,
            "leaf": self.leaf,
            "blood": self.blood,
            "poison": self.poison,
            "soul": self.soul,
            "beast": self.beast,
            "puppet": self.puppet,
            "eye": self.eye,
            "portal": self.portal,
            "step": self.step,
            "bind": self.bind,
            "summon": self.summon,
            "heal": self.heal,
            "sound": self.sound,
            "container": self.container,
            "time": self.time,
            "star": self.star,
            "social": self.social,
            "orb": lambda: self.orb(ring=True),
            "sigil": self.sigil,
        }
        painters.get(self.spec.motif, self.sigil)()
        # Tier marks are tiny and stay inside the panel, avoiding text or
        # variable dimensions while still differentiating *_1/_2/_3 skills.
        for index in range(self.spec.variant - 1):
            self.pixel(4 + index * 2, 12, self.glow)
        self.signature()
        return self.image


def ensure_unique(images: dict[str, Image.Image]) -> int:
    seen: dict[str, str] = {}
    changed = 0
    for skill_id in sorted(images):
        image = images[skill_id]
        attempt = 0
        while True:
            digest = hashlib.sha256(image.tobytes()).hexdigest()
            if digest not in seen:
                seen[digest] = skill_id
                break
            pixels = image.load()
            seed = hashlib.sha256(f"{skill_id}:{attempt}".encode("utf-8")).digest()
            candidates = [(x, y) for y in range(3, 14) for x in range(3, 13) if pixels[x, y][3] >= 180]
            if not candidates:
                raise RuntimeError(f"cannot disambiguate empty skill icon: {skill_id}")
            x, y = candidates[seed[0] % len(candidates)]
            old = pixels[x, y]
            pixels[x, y] = (clamp(old[0] + 3 + seed[1] % 13), clamp(old[1] + 2 + seed[2] % 11), clamp(old[2] + 3 + seed[3] % 17), old[3])
            attempt += 1
            changed += 1
            if attempt > 32:
                raise RuntimeError(f"failed to make skill icon unique: {skill_id}")
    return changed


def audit_textures(paths: Iterable[Path], expected_ids: set[str]) -> dict:
    paths = list(sorted(paths))
    issues: list[str] = []
    hashes: dict[str, list[str]] = defaultdict(list)
    coverage: list[float] = []
    for path in paths:
        try:
            with Image.open(path) as source:
                source.load()
                if source.size != (SIZE, SIZE):
                    issues.append(f"{path.name}: size {source.size}")
                if source.mode != "RGBA":
                    issues.append(f"{path.name}: mode {source.mode}")
                image = source.convert("RGBA")
                alpha = image.getchannel("A")
                nonzero = sum(1 for value in flattened_pixels(alpha) if value > 0)
                ratio = nonzero / (SIZE * SIZE)
                coverage.append(ratio)
                if not 0.25 <= ratio <= 0.82:
                    issues.append(f"{path.name}: coverage {ratio:.3f}")
                corners = [image.getpixel(point)[3] for point in ((0, 0), (15, 0), (0, 15), (15, 15))]
                if any(corners):
                    issues.append(f"{path.name}: opaque corner")
                hashes[hashlib.sha256(image.tobytes()).hexdigest()].append(path.name)
        except Exception as exc:
            issues.append(f"{path.name}: {exc}")
    duplicates = [names for names in hashes.values() if len(names) > 1]
    if duplicates:
        issues.append(f"duplicate pixel groups: {len(duplicates)}")
    found_ids = {path.stem for path in paths}
    missing_ids = sorted(expected_ids - found_ids)
    if missing_ids:
        issues.append(f"missing expected skill textures: {len(missing_ids)} ({', '.join(missing_ids[:8])})")
    if len(paths) != len(expected_ids):
        issues.append(f"expected {len(expected_ids)} skill textures, found {len(paths)}")
    return {
        "count": len(paths),
        "issues": issues,
        "duplicate_groups": duplicates,
        "coverage_min": min(coverage) if coverage else 0.0,
        "coverage_max": max(coverage) if coverage else 0.0,
        "coverage_mean": sum(coverage) / len(coverage) if coverage else 0.0,
    }


def write_contact_sheets(specs: list[SkillSpec], directory: Path) -> None:
    directory.mkdir(parents=True, exist_ok=True)
    by_motif: dict[str, list[SkillSpec]] = defaultdict(list)
    for spec in specs:
        by_motif[spec.motif].append(spec)
    font = ImageFont.load_default()
    for motif, group in sorted(by_motif.items()):
        columns, cell = 10, 80
        rows = math.ceil(len(group) / columns)
        sheet = Image.new("RGBA", (columns * cell, rows * 78), (26, 27, 31, 255))
        draw = ImageDraw.Draw(sheet)
        for index, spec in enumerate(group):
            image = Image.open(OUTPUT_DIR / f"{spec.skill_id}.png").convert("RGBA").resize((64, 64), Image.Resampling.NEAREST)
            x = (index % columns) * cell + 8
            y = (index // columns) * 78
            sheet.alpha_composite(image, (x, y))
            draw.text((x, y + 64), spec.skill_id[:11], fill=(232, 226, 211, 255), font=font)
        sheet.save(directory / f"{motif}.png", format="PNG", optimize=True)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="audit existing icons and compare exact deterministic pixels")
    parser.add_argument("--contact-sheets", type=Path, help="optional directory for visual review sheets")
    args = parser.parse_args()

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    texture_paths = sorted(OUTPUT_DIR.glob("*.png"))
    if not texture_paths:
        raise SystemExit(f"no skill icons found under {OUTPUT_DIR}")
    metadata, cultivation_ids, reference_ids = load_technique_metadata()
    existing_ids = {path.stem for path in texture_paths}
    expected_ids = existing_ids | cultivation_ids | reference_ids
    specs = build_specs(expected_ids, metadata)
    images = {spec.skill_id: Painter(spec).render() for spec in specs}
    adjusted = ensure_unique(images)
    mismatches: list[str] = []
    if args.check:
        for skill_id, expected in images.items():
            path = OUTPUT_DIR / f"{skill_id}.png"
            try:
                with Image.open(path) as actual:
                    if actual.mode != "RGBA" or actual.size != (SIZE, SIZE) or actual.convert("RGBA").tobytes() != expected.tobytes():
                        mismatches.append(skill_id)
            except Exception:
                mismatches.append(skill_id)
    else:
        written = 0
        for skill_id, image in images.items():
            path = OUTPUT_DIR / f"{skill_id}.png"
            matches = False
            if path.is_file():
                try:
                    with Image.open(path) as actual:
                        matches = (actual.mode == "RGBA" and actual.size == (SIZE, SIZE)
                                   and actual.tobytes() == image.tobytes())
                except Exception:
                    pass
            if not matches:
                image.save(path, format="PNG", optimize=True)
                written += 1
        print(f"wrote {written} of {len(images)} skill icons; uniqueness adjustments={adjusted}")

    audit = audit_textures(sorted(OUTPUT_DIR.glob("*.png")), expected_ids)
    found_ids = {path.stem for path in OUTPUT_DIR.glob("*.png")}
    audit.update({
        "element_counts": dict(Counter(spec.element for spec in specs)),
        "motif_counts": dict(Counter(spec.motif for spec in specs)),
        "metadata_matches": sum(1 for spec in specs if spec.skill_id in metadata),
        "reference_count": len(reference_ids),
        "cultivation_technique_count": len(cultivation_ids),
        "cultivation_icon_coverage": len(cultivation_ids & found_ids),
        "render_mismatch_count": len(mismatches),
        "uniqueness_adjustments": adjusted,
    })
    if mismatches:
        audit["issues"].append(f"deterministic render mismatches: {len(mismatches)} ({', '.join(mismatches[:8])})")
    print(json.dumps(audit, indent=2, sort_keys=True))
    if args.contact_sheets:
        write_contact_sheets(specs, args.contact_sheets)
        print(f"contact sheets: {args.contact_sheets}")
    return 1 if audit["issues"] else 0


if __name__ == "__main__":
    raise SystemExit(main())
