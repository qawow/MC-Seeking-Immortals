#!/usr/bin/env python3
"""Generate the complete 16x16 item texture set in the project art style.

The renderer is deterministic and data-driven. It consumes the shipped item
catalog plus visual metadata, then selects a silhouette, element palette,
rarity treatment, and small per-item details. It intentionally never writes
outside textures/item unless a contact-sheet directory is explicitly given.
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
from typing import Iterable

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/seeking_immortals"
TEXTURE_DIR = ASSETS / "textures/item"
BULK_CATALOG = ASSETS / "catalog_bulk_items.json"
VISUAL_DATA_DIR = ROOT / "\u6587\u672c\u6750\u6599/data"
OUTPUT_SIZE = 16
WORK_SIZE = 64


ELEMENT_COLORS = {
    "qi": (142, 211, 224),
    "fire": (225, 83, 42),
    "water": (67, 151, 203),
    "ice": (146, 209, 226),
    "wood": (72, 151, 92),
    "metal": (201, 196, 166),
    "earth": (164, 125, 78),
    "thunder": (143, 105, 204),
    "yin": (94, 73, 133),
    "soul": (138, 116, 184),
    "blood": (151, 47, 45),
    "poison": (100, 158, 63),
    "heal": (105, 174, 111),
    "star": (88, 122, 177),
    "void": (77, 65, 111),
    "jade": (79, 156, 133),
    "neutral": (149, 126, 91),
}

RARITY_COLORS = {
    "common": (215, 207, 184),
    "uncommon": (91, 184, 160),
    "rare": (80, 132, 190),
    "epic": (148, 97, 178),
    "legendary": (191, 143, 64),
    "unique": (185, 201, 219),
}

RARITY_ORDER = {
    "common": 0,
    "uncommon": 1,
    "rare": 2,
    "epic": 3,
    "legendary": 4,
    "unique": 5,
}


# Catalog categories describe runtime behavior, not always the item's visible
# form. Keep known mismatches explicit so future catalog edits cannot turn a
# gate, array disk, weapon, or story item into a generic material icon.
VISUAL_CATEGORY_OVERRIDES = {
    "beast_gate": "block_item",
    "blood_forbidden_trial_altar": "block_item",
    "bloodline_array": "block_item",
    "demon_subdue_bell": "artifact",
    "green_liquid_drop": "material",
    "iron_sword_mortal": "artifact",
    "major_thunder_array_token": "block_item",
    "palm_heaven_bottle": "artifact",
    "price_tag_array": "block_item",
    "space_rift_compass": "artifact",
    "spirit_gathering_array_disk": "block_item",
    "spirit_realm_gate_pass": "currency",
    "stone_table": "block_item",
    "yinyang_cave_gate": "block_item",
}

VISUAL_MOTIF_OVERRIDES = {
    "beast_gate": "gate",
    "blood_forbidden_trial_altar": "array_block",
    "bloodline_array": "array_block",
    "demon_subdue_bell": "bell",
    "green_liquid_drop": "drop",
    "iron_sword_mortal": "weapon",
    "major_thunder_array_token": "array_block",
    "palm_heaven_bottle": "bottle",
    "price_tag_array": "array_block",
    "space_rift_compass": "compass",
    "spirit_gathering_array_disk": "array_block",
    "spirit_realm_gate_pass": "ticket",
    "stone_table": "workstation",
    "yinyang_cave_gate": "gate",
}

VISUAL_ELEMENT_OVERRIDES = {
    # The authored appearance is explicitly a small green bottle; the word
    # "heaven" in the id must not win the generic star-element heuristic.
    "palm_heaven_bottle": "wood",
}


@dataclass(frozen=True)
class ItemSpec:
    item_id: str
    category: str
    motif: str
    element: str
    rarity: str
    variant: int
    visual_text: str


def clamp(value: int) -> int:
    return max(0, min(255, value))


def mix(a: tuple[int, int, int], b: tuple[int, int, int], amount: float) -> tuple[int, int, int]:
    return tuple(clamp(round(x * (1.0 - amount) + y * amount)) for x, y in zip(a, b))


def shade(color: tuple[int, int, int], factor: float) -> tuple[int, int, int]:
    return tuple(clamp(round(channel * factor)) for channel in color)


def rgba(color: tuple[int, int, int], alpha: int = 255) -> tuple[int, int, int, int]:
    return color[0], color[1], color[2], alpha


def stable_bytes(value: str) -> bytes:
    return hashlib.sha256(value.encode("utf-8")).digest()


def shifted(color: tuple[int, int, int], seed: bytes) -> tuple[int, int, int]:
    red, green, blue = (channel / 255 for channel in color)
    hue, saturation, value = colorsys.rgb_to_hsv(red, green, blue)
    hue = (hue + ((seed[0] / 255.0) - 0.5) * 0.045) % 1.0
    saturation = max(0.18, min(0.88, saturation + ((seed[1] / 255.0) - 0.5) * 0.12))
    value = max(0.42, min(0.94, value + ((seed[2] / 255.0) - 0.5) * 0.10))
    return tuple(round(channel * 255) for channel in colorsys.hsv_to_rgb(hue, saturation, value))


def contains_any(text: str, tokens: Iterable[str]) -> bool:
    return any(token in text for token in tokens)


def load_bulk_metadata() -> dict[str, dict]:
    data = json.loads(BULK_CATALOG.read_text(encoding="utf-8"))
    return {entry["id"]: entry for entry in data.get("items", [])}


def load_visual_metadata() -> dict[str, str]:
    result: dict[str, list[str]] = defaultdict(list)
    for file_name in (
        "item_descriptions_v118.json",
        "item_descriptions_v119.json",
        "item_descriptions_v120.json",
        "item_descriptions_v122.json",
    ):
        path = VISUAL_DATA_DIR / file_name
        if not path.exists():
            continue
        data = json.loads(path.read_text(encoding="utf-8"))
        for entry in data.get("items", []):
            item_id = entry.get("catalog_id")
            if not item_id:
                continue
            parts = [
                str(entry.get("type", "")),
                str(entry.get("appearance", "")),
                str(entry.get("effect_text", "")),
                str(entry.get("description", "")),
                " ".join(str(tag) for tag in entry.get("tags", [])),
                " ".join(str(tag) for tag in entry.get("style_tags", [])),
            ]
            result[item_id].append(" ".join(parts).lower())
    return {item_id: " ".join(parts) for item_id, parts in result.items()}


def infer_rarity(item_id: str, metadata: dict | None) -> str:
    if metadata:
        value = str(metadata.get("rarity", "")).lower()
        if value in RARITY_ORDER:
            return value
    if contains_any(item_id, ("palm_heaven", "green_liquid", "primordial", "chaos_", "immortal_")):
        return "unique"
    if contains_any(item_id, ("supreme", "perfect", "celestial", "true_dragon", "dao_ancestor")):
        return "legendary"
    if contains_any(item_id, ("ancient", "epic", "void", "star_sea", "great_vehicle", "nascent_soul")):
        return "epic"
    if contains_any(item_id, ("_high", "rare", "spirit_realm", "core_formation", "deity")):
        return "rare"
    if contains_any(item_id, ("_mid", "_medium", "uncommon", "foundation")):
        return "uncommon"
    return "common"


def infer_element(item_id: str, visual_text: str) -> str:
    if item_id in VISUAL_ELEMENT_OVERRIDES:
        return VISUAL_ELEMENT_OVERRIDES[item_id]
    text = f"{item_id} {visual_text}".lower()
    groups = (
        ("blood", ("blood", "xue_", "crimson", "asura")),
        ("fire", ("fire", "flame", "ember", "lava", "huo_", "lieyan", "phoenix")),
        ("ice", ("ice", "frost", "snow", "cold", "han_")),
        ("water", ("water", "sea", "ocean", "rain", "mist", "wave", "tide")),
        ("thunder", ("thunder", "lightning", "storm", "lei_", "tribulation")),
        ("poison", ("poison", "toxin", "venom", "corruption", "miasma")),
        ("yin", ("yin", "ghost", "demon", "demonic", "nether", "underworld", "corpse")),
        ("void", ("void", "space", "rift", "chaos", "xutian")),
        ("soul", ("soul", "spirit_sense", "divine_sense", "mind", "dream")),
        ("star", ("star", "moon", "sun", "celestial", "heaven")),
        ("wood", ("wood", "grass", "herb", "leaf", "bamboo", "flower", "ginseng", "root", "moss", "green")),
        ("metal", ("metal", "iron", "copper", "gold", "silver", "sword", "blade", "needle")),
        ("earth", ("earth", "stone", "sand", "mountain", "soil", "rock")),
        ("heal", ("heal", "recovery", "rejuven", "life", "longevity", "antidote", "detox")),
        ("jade", ("jade", "emerald", "qing_", "azure")),
        ("qi", ("qi", "spirit", "aura", "cultivat", "foundation", "condens")),
    )
    for element, tokens in groups:
        if contains_any(text, tokens):
            return element
    return "neutral"


def infer_category(item_id: str, metadata: dict | None) -> str:
    if item_id in VISUAL_CATEGORY_OVERRIDES:
        return VISUAL_CATEGORY_OVERRIDES[item_id]
    if contains_any(item_id, (
        "alchemy_formula", "technique_manual", "_manual", "manual_", "recipe_", "_recipe",
        "jade_slip", "scroll", "blueprint", "art_page", "method", "scripture", "cipher",
    )):
        return "manual"
    if metadata:
        category = str(metadata.get("category", "material")).lower()
        if category == "access_item":
            return "currency"
        if category == "equipment":
            return "equipment"
        if category == "artifact":
            return "artifact"
        if category == "manual":
            return "manual"
        if category == "talisman":
            return "talisman"
        if category == "pill":
            return "pill"
        if category == "currency":
            return "currency"
        if category == "consumable":
            if contains_any(item_id, ("pill", "medicine", "salve", "powder", "elixir")):
                return "pill"
            if contains_any(item_id, ("talisman", "charm")):
                return "talisman"
            return "consumable"
        return "material"

    if contains_any(item_id, (
        "alchemy_formula", "technique_manual", "_manual", "manual_", "recipe_", "_recipe",
        "jade_slip", "scroll", "blueprint", "art_page", "method", "scripture", "cipher",
    )):
        return "manual"
    if contains_any(item_id, ("pill", "powder", "salve", "medicine", "true_water", "elixir")):
        return "pill"
    if contains_any(item_id, ("talisman", "charm", "spirit_seal", "seal_paper")):
        return "talisman"
    if contains_any(item_id, (
        "token", "permit", "ticket", "pass", "receipt", "invite", "invitation", "credit",
        "merit", "voucher", "identity", "contract", "evidence", "report", "map_fragment",
    )):
        return "currency"
    if contains_any(item_id, (
        "furnace", "forge", "formation_core", "array_core", "_altar", "_gate", "_table",
        "_bench", "_planter", "_slab", "_cushion", "_room", "_ore", "surface_marker",
    )):
        return "block_item"
    if contains_any(item_id, (
        "sword", "blade", "saber", "shield", "mirror", "bell", "ruler", "disk", "fan",
        "bracelet", "boots", "artifact", "needle", "umbrella", "bowl", "chain", "staff",
        "spear", "halberd", "armor", "pendant", "bead", "pearl", "banner", "flag",
    )):
        return "artifact"
    if contains_any(item_id, ("boat", "raft", "sedan", "cart", "puppet", "flying_artifact")):
        return "equipment"
    if contains_any(item_id, ("pouch", "bundle", "pack", "bomb", "jerky", "rice_bowl")):
        return "consumable"
    return "material"


def infer_motif(item_id: str, category: str) -> str:
    if item_id in VISUAL_MOTIF_OVERRIDES:
        return VISUAL_MOTIF_OVERRIDES[item_id]
    if category == "pill":
        if contains_any(item_id, ("powder", "dust", "salve")):
            return "powder_jar"
        if contains_any(item_id, ("water", "elixir", "medicine")):
            return "vial"
        return "pill_bottle"
    if category == "manual":
        if contains_any(item_id, ("jade_slip", "jade_formula")) or item_id.endswith("_jade"):
            return "jade_slip"
        if contains_any(item_id, ("fragment", "page", "evidence", "report")):
            return "page"
        if contains_any(item_id, ("blueprint", "scroll", "formula", "recipe", "paper")):
            return "scroll"
        return "book"
    if category == "talisman":
        return "talisman"
    if category == "currency":
        if contains_any(item_id, ("ticket", "receipt", "invite", "report", "map", "cipher")):
            return "ticket"
        if contains_any(item_id, ("pass", "permit", "identity", "contract")):
            return "jade_token"
        if contains_any(item_id, ("stone", "crystal", "jade")):
            return "gem"
        return "coin"
    if category == "block_item":
        if contains_any(item_id, ("furnace", "forge", "cauldron")):
            return "furnace"
        if contains_any(item_id, ("gate", "portal", "rift")):
            return "gate"
        if contains_any(item_id, ("array", "formation", "altar")):
            return "array_block"
        if contains_any(item_id, ("planter", "table", "bench", "room")):
            return "workstation"
        return "block"
    if category in ("artifact", "equipment"):
        artifact_motifs = (
            ("weapon", ("sword", "blade", "saber", "needle", "spear", "halberd", "staff", "axe", "hammer", "hook")),
            ("bow", ("bow",)),
            ("shield", ("shield", "armor")),
            ("mirror", ("mirror", "lens")),
            ("bell", ("bell",)),
            ("fan", ("fan",)),
            ("ring", ("ring", "bracelet", "pendant", "necklace")),
            ("boots", ("boots", "shoe")),
            ("umbrella", ("umbrella",)),
            ("vessel", ("bowl", "pot", "cauldron", "furnace", "jar", "bottle", "vase")),
            ("vehicle", ("boat", "raft", "sedan", "cart", "ferry")),
            ("puppet", ("puppet",)),
            ("flag", ("flag", "banner")),
            ("disk", ("disk", "ruler", "plate", "brick", "seal")),
            ("orb", ("bead", "pearl", "core", "orb")),
        )
        for motif, tokens in artifact_motifs:
            if contains_any(item_id, tokens):
                return motif
        return "relic"
    if category == "consumable":
        if contains_any(item_id, ("talisman", "charm")):
            return "talisman"
        if contains_any(item_id, ("pouch", "bundle", "pack", "bag")):
            return "pouch"
        if contains_any(item_id, ("bomb", "beacon", "flare")):
            return "orb"
        if contains_any(item_id, ("meat", "jerky", "rice", "food")):
            return "food"
        if contains_any(item_id, ("scroll", "report", "map", "recipe")):
            return "scroll"
        return "vial"

    material_motifs = (
        ("flower", ("flower", "lotus", "orchid", "blossom")),
        ("mushroom", ("mushroom", "fungus", "lingzhi")),
        ("plant", ("grass", "herb", "leaf", "ginseng", "root", "moss", "vine", "seed")),
        ("bamboo", ("bamboo",)),
        ("feather", ("feather", "plume")),
        ("bone", ("bone", "fang", "tooth", "horn", "claw")),
        ("scale", ("scale", "shell", "carapace")),
        ("hide", ("hide", "pelt", "skin", "leather")),
        ("vial", ("blood", "venom", "sac", "oil", "dew", "liquid", "ink", "drop")),
        ("ingot", ("ingot", "metal", "iron", "copper", "gold", "silver", "bronze")),
        ("wood", ("wood", "timber", "resin", "branch", "log")),
        ("cloth", ("silk", "cloth", "fiber", "thread", "rope")),
        ("pile", ("sand", "powder", "dust", "ash", "soil", "salt")),
        ("shard", ("fragment", "shard", "scrap", "broken", "blank", "chip")),
        ("core", ("core", "marrow", "essence", "heart")),
        ("gem", ("crystal", "jade", "stone", "ore", "pearl", "diamond", "quartz")),
        ("food", ("meat", "jerky", "rice", "fruit")),
    )
    for motif, tokens in material_motifs:
        if contains_any(item_id, tokens):
            return motif
    return "component"


def build_specs(texture_ids: Iterable[str]) -> list[ItemSpec]:
    bulk = load_bulk_metadata()
    visuals = load_visual_metadata()
    specs = []
    for item_id in sorted(texture_ids):
        metadata = bulk.get(item_id)
        visual_text = visuals.get(item_id, "")
        category = infer_category(item_id, metadata)
        specs.append(ItemSpec(
            item_id=item_id,
            category=category,
            motif=infer_motif(item_id, category),
            element=infer_element(item_id, visual_text),
            rarity=infer_rarity(item_id, metadata),
            variant=stable_bytes(item_id)[3] % 5,
            visual_text=visual_text,
        ))
    return specs


def discover_texture_ids() -> list[str]:
    """Include direct generated-item model stems that lacked a same-name PNG."""
    ids = {path.stem for path in TEXTURE_DIR.glob("*.png")}
    model_dir = ASSETS / "models/item"
    for path in model_dir.glob("*.json"):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except Exception:
            continue
        layer0 = data.get("textures", {}).get("layer0")
        if isinstance(layer0, str) and layer0.startswith("seeking_immortals:item/"):
            ids.add(path.stem)
    return sorted(ids)


class Painter:
    def __init__(self, spec: ItemSpec):
        self.spec = spec
        self.seed = stable_bytes(spec.item_id)
        self.image = Image.new("RGBA", (WORK_SIZE, WORK_SIZE), (0, 0, 0, 0))
        self.draw = ImageDraw.Draw(self.image, "RGBA")
        self.base = shifted(ELEMENT_COLORS[spec.element], self.seed)
        self.accent = shifted(RARITY_COLORS[spec.rarity], self.seed[4:] + self.seed[:4])
        self.dark = shade(mix(self.base, (33, 29, 28), 0.40), 0.72)
        self.shadow = shade(self.base, 0.64)
        self.light = mix(self.base, (249, 243, 215), 0.46)
        self.paper = shifted((214, 196, 153), self.seed[8:])
        self.ink = shade(mix(self.base, (67, 43, 34), 0.55), 0.66)

    @staticmethod
    def pt(value: float) -> int:
        return round(value * WORK_SIZE / OUTPUT_SIZE)

    def coords(self, values: Iterable[float]) -> tuple[int, ...]:
        return tuple(self.pt(value) for value in values)

    def polygon(self, points: Iterable[tuple[float, float]], fill, outline=None, width: float = 1.0):
        pixels = [(self.pt(x), self.pt(y)) for x, y in points]
        self.draw.polygon(pixels, fill=fill)
        if outline:
            self.draw.line(pixels + [pixels[0]], fill=outline, width=self.pt(width), joint="curve")

    def line(self, points: Iterable[tuple[float, float]], fill, width: float = 1.0):
        self.draw.line([(self.pt(x), self.pt(y)) for x, y in points], fill=fill, width=self.pt(width), joint="curve")

    def ellipse(self, box: tuple[float, float, float, float], fill, outline=None, width: float = 1.0):
        self.draw.ellipse(self.coords(box), fill=fill, outline=outline, width=self.pt(width) if outline else 1)

    def rectangle(self, box: tuple[float, float, float, float], fill, outline=None, width: float = 1.0):
        self.draw.rectangle(self.coords(box), fill=fill, outline=outline, width=self.pt(width) if outline else 1)

    def rounded(self, box: tuple[float, float, float, float], radius: float, fill, outline=None, width: float = 1.0):
        self.draw.rounded_rectangle(
            self.coords(box),
            radius=self.pt(radius),
            fill=fill,
            outline=outline,
            width=self.pt(width) if outline else 1,
        )

    def aura(self):
        tier = RARITY_ORDER[self.spec.rarity]
        if tier == 0:
            return
        self.ellipse((2.2, 5.0, 13.8, 15.2), rgba(self.accent, 12 + tier * 5))
        count = min(5, tier + 1)
        positions = ((3.0, 5.0), (12.7, 4.0), (2.7, 11.2), (13.0, 10.3), (8.0, 1.8))
        for index in range(count):
            x, y = positions[(index + self.seed[5]) % len(positions)]
            size = 0.35 if tier < 3 else 0.48
            self.polygon(
                ((x, y - size), (x + size, y), (x, y + size), (x - size, y)),
                rgba(self.accent, 145 + tier * 12),
            )

    def pill_bottle(self, vial: bool = False, powder: bool = False):
        if powder:
            self.rounded((4.0, 5.0, 12.0, 13.7), 1.6, rgba(mix(self.paper, self.base, 0.18)), rgba(self.dark), 0.8)
            self.rectangle((4.8, 4.0, 11.2, 5.7), rgba(shade(self.paper, 0.72)), rgba(self.dark), 0.7)
            self.ellipse((6.1, 7.5, 10.2, 11.6), rgba(self.base), rgba(self.shadow), 0.6)
            self.line(((6.6, 9.6), (9.6, 9.6)), rgba(self.light), 0.45)
            return
        if vial:
            body = ((5.2, 5.2), (10.8, 5.2), (11.8, 12.8), (10.6, 14.0), (5.4, 14.0), (4.2, 12.8))
            self.polygon(body, rgba(mix((214, 231, 222), self.base, 0.20), 235), rgba(self.dark), 0.85)
            self.rectangle((6.5, 2.3, 9.5, 5.5), rgba(mix((220, 230, 211), self.base, 0.12), 238), rgba(self.dark), 0.75)
            self.rectangle((6.0, 1.8, 10.0, 3.0), rgba(shade(self.paper, 0.66)), rgba(self.dark), 0.65)
            self.polygon(((4.8, 10.4), (11.2, 10.4), (11.5, 13.0), (10.4, 13.5), (5.6, 13.5), (4.5, 13.0)), rgba(self.base, 205))
            self.line(((6.0, 6.2), (5.4, 9.4)), rgba((255, 255, 238), 155), 0.55)
            return

        porcelain = rgba(mix((226, 221, 194), self.base, 0.10), 248)
        neck = rgba(mix((223, 228, 204), self.base, 0.10), 248)
        if self.spec.variant == 0:
            self.rounded((4.2, 5.0, 11.8, 14.1), 2.2, porcelain, rgba(self.dark), 0.85)
            self.rounded((6.2, 2.5, 9.8, 6.0), 0.7, neck, rgba(self.dark), 0.75)
        elif self.spec.variant == 1:
            self.polygon(((6.0, 4.7), (10.0, 4.7), (11.5, 7.0), (11.1, 13.2), (9.8, 14.2), (6.2, 14.2), (4.9, 13.2), (4.5, 7.0)), rgba(mix((225, 220, 192), self.base, 0.12), 248), rgba(self.dark), 0.85)
            self.rectangle((6.5, 2.4, 9.5, 5.3), neck, rgba(self.dark), 0.75)
        elif self.spec.variant == 2:
            self.ellipse((4.0, 7.0, 12.0, 14.2), porcelain, rgba(self.dark), 0.85)
            self.ellipse((5.1, 3.3, 10.9, 9.2), neck, rgba(self.dark), 0.75)
            self.rounded((6.4, 1.9, 9.6, 4.2), 0.6, neck, rgba(self.dark), 0.65)
        elif self.spec.variant == 3:
            self.rounded((3.7, 7.2, 12.3, 13.9), 1.8, porcelain, rgba(self.dark), 0.85)
            self.rounded((5.4, 4.0, 10.6, 8.3), 1.0, neck, rgba(self.dark), 0.72)
            self.rounded((6.2, 2.2, 9.8, 4.7), 0.55, neck, rgba(self.dark), 0.65)
        else:
            self.polygon(((5.8, 4.0), (10.2, 4.0), (11.8, 6.0), (11.2, 14.1), (4.8, 14.1), (4.2, 6.0)), porcelain, rgba(self.dark), 0.85)
            self.rectangle((6.5, 2.2, 9.5, 4.8), neck, rgba(self.dark), 0.7)
        self.rectangle((6.0, 1.7, 10.0, 3.0), rgba(shade(self.paper, 0.68)), rgba(self.dark), 0.65)
        pill_y = 8.2 if self.spec.variant != 3 else 9.0
        self.ellipse((6.1, pill_y, 9.9, pill_y + 3.8), rgba(self.base), rgba(self.shadow), 0.65)
        self.ellipse((6.8, pill_y + 0.5, 8.2, pill_y + 1.8), rgba(self.light, 210))
        self.line(((5.5, 6.1), (5.2, 10.0)), rgba((255, 255, 239), 145), 0.45)
        if RARITY_ORDER[self.spec.rarity] >= 2:
            band_y = 7.0 if self.spec.variant != 3 else 7.7
            self.line(((4.8, band_y), (11.2, band_y)), rgba(self.accent), 0.6)

    def manual(self):
        motif = self.spec.motif
        if motif == "jade_slip":
            for index in range(4):
                x = 4.0 + index * 2.0
                top = 2.6 + (index % 2) * 0.5
                self.rounded((x, top, x + 1.6, 13.4), 0.45, rgba(mix(self.base, (126, 194, 158), 0.50)), rgba(self.dark), 0.45)
                self.line(((x + 0.8, top + 1.0), (x + 0.8, 12.4)), rgba(self.light, 130), 0.3)
            self.line(((3.8, 7.0), (11.8, 8.0)), rgba((145, 52, 43)), 0.8)
            return
        if motif == "book":
            self.polygon(((3.2, 3.7), (7.5, 2.8), (8.0, 12.9), (3.5, 13.7)), rgba(shade(self.paper, 0.92)), rgba(self.ink), 0.7)
            self.polygon(((8.0, 2.8), (12.8, 3.8), (12.4, 13.7), (8.0, 12.9)), rgba(self.paper), rgba(self.ink), 0.7)
            self.line(((8.0, 3.2), (8.0, 12.5)), rgba(shade(self.ink, 0.8)), 0.6)
            self.line(((4.5, 6.2), (6.7, 5.8)), rgba(self.base), 0.45)
            self.line(((9.3, 6.0), (11.4, 6.4)), rgba(self.base), 0.45)
            self.ellipse((9.5, 9.3, 11.5, 11.3), rgba(self.accent), rgba(self.ink), 0.45)
            return
        if motif == "page":
            self.polygon(((4.1, 2.2), (11.7, 3.0), (11.2, 13.8), (8.8, 13.1), (6.8, 14.0), (3.7, 12.7)), rgba(self.paper), rgba(self.ink), 0.75)
            self.line(((5.2, 5.0), (10.3, 5.5)), rgba(self.ink, 180), 0.45)
            self.line(((5.0, 7.2), (9.2, 7.6)), rgba(self.ink, 160), 0.4)
            self.polygon(((7.1, 9.0), (9.7, 9.8), (8.4, 12.0)), rgba(self.accent), rgba(self.ink), 0.35)
            return
        self.polygon(((3.1, 4.0), (11.8, 2.7), (12.9, 12.0), (4.1, 13.4)), rgba(self.paper), rgba(self.ink), 0.75)
        self.ellipse((2.6, 3.5, 5.0, 13.7), rgba(shade(self.paper, 0.82)), rgba(self.ink), 0.55)
        self.ellipse((11.1, 2.3, 13.5, 12.4), rgba(shade(self.paper, 0.88)), rgba(self.ink), 0.55)
        self.line(((5.7, 5.2), (10.4, 4.5)), rgba(self.ink, 170), 0.42)
        self.line(((5.9, 7.2), (10.6, 6.5)), rgba(self.ink, 150), 0.42)
        self.line(((6.2, 9.2), (10.7, 8.5)), rgba(self.base), 0.55)
        self.ellipse((8.0, 10.1, 10.5, 12.4), rgba(self.accent), rgba(self.ink), 0.4)

    def talisman(self):
        paper = mix(self.paper, (230, 211, 157), 0.32)
        self.polygon(((4.4, 1.8), (11.4, 2.8), (10.8, 14.2), (8.1, 13.4), (5.6, 14.0), (3.7, 12.5)), rgba(paper), rgba(self.ink), 0.75)
        self.polygon(((9.8, 2.6), (11.4, 2.8), (10.9, 4.4)), rgba(shade(paper, 0.78)), rgba(self.ink), 0.35)
        ink = mix(self.base, (157, 42, 35), 0.48)
        variant = self.spec.variant
        if variant == 0:
            self.line(((7.8, 4.0), (6.2, 6.1), (9.7, 6.8), (6.1, 9.0), (9.2, 11.7)), rgba(ink), 0.85)
        elif variant == 1:
            self.line(((6.2, 4.3), (9.4, 5.0), (7.0, 7.3), (9.7, 9.0), (6.5, 11.5)), rgba(ink), 0.82)
            self.line(((5.4, 7.1), (10.2, 7.8)), rgba(ink), 0.55)
        elif variant == 2:
            self.ellipse((5.6, 5.1, 9.9, 9.4), None, rgba(ink), 0.65)
            self.line(((7.8, 3.8), (7.6, 11.7)), rgba(ink), 0.72)
        elif variant == 3:
            self.polygon(((7.8, 4.1), (9.8, 7.2), (7.7, 11.5), (5.5, 7.5)), None, rgba(ink), 0.7)
            self.line(((5.4, 7.5), (9.9, 7.2)), rgba(ink), 0.55)
        else:
            self.line(((5.8, 4.1), (9.7, 5.3), (6.0, 7.1), (9.6, 9.0), (6.2, 11.8)), rgba(ink), 0.82)
        self.ellipse((6.5, 11.3, 9.2, 13.5), rgba(self.accent, 205), rgba(self.ink), 0.35)

    def currency(self):
        motif = self.spec.motif
        if motif == "ticket":
            self.polygon(((2.7, 5.0), (12.7, 3.4), (13.4, 10.8), (3.5, 12.6)), rgba(self.paper), rgba(self.ink), 0.7)
            self.line(((5.1, 6.1), (10.8, 5.2)), rgba(self.base), 0.55)
            self.line(((5.4, 8.0), (9.0, 7.4)), rgba(self.ink, 145), 0.45)
            self.ellipse((9.3, 8.0, 11.8, 10.5), rgba(self.accent), rgba(self.ink), 0.4)
            return
        if motif == "jade_token":
            self.rounded((4.0, 2.0, 12.0, 14.0), 1.5, rgba(mix(self.base, (119, 190, 157), 0.45)), rgba(self.dark), 0.8)
            self.ellipse((6.4, 3.3, 9.6, 6.4), rgba((0, 0, 0), 0), rgba(self.light), 0.65)
            self.polygon(((8.0, 7.0), (10.1, 9.2), (8.0, 12.1), (5.9, 9.2)), rgba(self.accent), rgba(self.dark), 0.45)
            return
        if motif == "gem":
            self.gem()
            return
        self.ellipse((3.0, 3.0, 13.0, 13.0), rgba(self.dark, 230))
        self.ellipse((3.7, 2.6, 12.4, 11.7), rgba(mix(self.base, self.accent, 0.42)), rgba(self.dark), 0.75)
        self.ellipse((5.2, 4.1, 10.9, 9.9), rgba(shade(self.base, 0.82)), rgba(self.light), 0.45)
        sides = 3 + self.spec.variant
        points = []
        for index in range(sides):
            angle = -math.pi / 2 + index * (2 * math.pi / sides)
            points.append((8.0 + math.cos(angle) * 2.0, 7.0 + math.sin(angle) * 2.0))
        self.polygon(points, rgba(self.accent), rgba(self.dark), 0.35)

    def gem(self):
        self.polygon(((7.5, 1.7), (11.0, 5.0), (12.2, 10.8), (8.0, 14.3), (3.8, 10.6), (4.6, 5.1)), rgba(self.base), rgba(self.dark), 0.85)
        self.polygon(((7.5, 1.7), (8.1, 9.0), (4.6, 5.1)), rgba(self.light, 210))
        self.polygon(((8.1, 9.0), (12.2, 10.8), (8.0, 14.3)), rgba(self.shadow, 220))
        self.line(((4.7, 5.3), (8.1, 9.0), (11.0, 5.1)), rgba(self.accent, 170), 0.45)

    def plant(self, flower: bool = False, mushroom: bool = False, bamboo: bool = False):
        if bamboo:
            self.line(((5.2, 13.8), (7.0, 2.2)), rgba(shade(self.base, 0.72)), 1.5)
            self.line(((9.0, 13.5), (10.2, 3.0)), rgba(self.base), 1.4)
            for y in (5.0, 8.0, 11.0):
                self.line(((5.9, y), (7.4, y + 0.2)), rgba(self.light), 0.45)
            self.polygon(((9.6, 5.0), (13.1, 3.7), (10.2, 7.0)), rgba(self.light), rgba(self.dark), 0.35)
            return
        if mushroom:
            self.rounded((6.4, 7.0, 9.6, 14.0), 1.0, rgba(mix(self.paper, self.base, 0.24)), rgba(self.dark), 0.65)
            self.ellipse((3.2, 2.4, 12.8, 9.3), rgba(self.base), rgba(self.dark), 0.8)
            self.ellipse((5.0, 3.5, 7.0, 5.2), rgba(self.light, 210))
            self.ellipse((9.3, 5.1, 10.7, 6.4), rgba(self.accent, 190))
            return
        stem = shade(self.base, 0.68)
        self.line(((7.7, 14.2), (7.3, 5.0)), rgba(stem), 0.8)
        self.line(((7.4, 9.0), (4.0, 6.3)), rgba(stem), 0.55)
        self.line(((7.5, 10.7), (11.4, 7.4)), rgba(stem), 0.55)
        self.ellipse((2.7, 4.7, 7.0, 7.8), rgba(self.base), rgba(self.dark), 0.5)
        self.ellipse((8.8, 5.7, 13.2, 9.0), rgba(self.light), rgba(self.dark), 0.5)
        self.ellipse((4.1, 8.5, 7.8, 11.5), rgba(mix(self.base, self.light, 0.25)), rgba(self.dark), 0.45)
        if flower:
            center = (7.4, 4.4)
            for dx, dy in ((-1.8, 0), (1.8, 0), (0, -1.7), (0, 1.7)):
                self.ellipse((center[0] + dx - 1.2, center[1] + dy - 1.1, center[0] + dx + 1.2, center[1] + dy + 1.1), rgba(mix(self.base, self.accent, 0.32)), rgba(self.dark), 0.35)
            self.ellipse((6.2, 3.2, 8.6, 5.6), rgba(self.accent), rgba(self.dark), 0.35)

    def feather(self):
        self.line(((4.0, 13.8), (11.7, 2.3)), rgba(self.dark), 0.85)
        self.polygon(((5.0, 11.8), (3.4, 8.3), (8.3, 4.0), (11.7, 2.3), (11.2, 6.6), (7.2, 11.0)), rgba(self.base), rgba(self.dark), 0.65)
        self.line(((4.8, 12.1), (10.7, 3.4)), rgba(self.light), 0.45)
        self.line(((7.0, 9.0), (4.4, 8.4)), rgba(self.shadow), 0.4)
        self.line(((8.5, 6.8), (11.0, 6.1)), rgba(self.shadow), 0.4)

    def bone(self):
        bone = mix((218, 206, 170), self.base, 0.16)
        self.line(((4.1, 12.3), (11.6, 4.2)), rgba(self.dark), 2.5)
        self.line(((4.1, 12.3), (11.6, 4.2)), rgba(bone), 1.45)
        for x, y in ((3.4, 12.7), (4.7, 13.0), (11.2, 3.5), (12.1, 4.5)):
            self.ellipse((x - 1.0, y - 1.0, x + 1.0, y + 1.0), rgba(bone), rgba(self.dark), 0.45)

    def scale(self):
        self.polygon(((8.0, 1.8), (12.2, 5.3), (11.3, 11.8), (8.0, 14.2), (4.5, 11.3), (3.8, 5.4)), rgba(self.base), rgba(self.dark), 0.8)
        self.line(((4.5, 6.0), (8.0, 8.7), (11.5, 5.9)), rgba(self.light, 190), 0.55)
        self.line(((5.0, 10.2), (8.0, 8.7), (10.8, 10.3)), rgba(self.shadow), 0.55)

    def vial(self):
        self.polygon(((6.2, 4.8), (9.8, 4.8), (11.6, 8.0), (10.9, 13.5), (5.1, 13.5), (4.4, 8.0)), rgba(mix((205, 225, 213), self.base, 0.15), 210), rgba(self.dark), 0.75)
        self.rectangle((6.5, 2.2, 9.5, 5.1), rgba(mix((210, 224, 207), self.base, 0.10), 225), rgba(self.dark), 0.65)
        self.rectangle((6.0, 1.7, 10.0, 2.9), rgba(shade(self.paper, 0.68)), rgba(self.dark), 0.55)
        self.polygon(((4.8, 9.3), (11.2, 9.3), (10.7, 13.0), (5.3, 13.0)), rgba(self.base, 225))
        self.ellipse((6.0, 10.0, 7.5, 11.5), rgba(self.light, 180))

    def ingot(self):
        self.polygon(((3.0, 8.0), (5.1, 4.6), (11.4, 4.3), (13.1, 8.4), (10.8, 12.4), (5.0, 12.6)), rgba(self.base), rgba(self.dark), 0.8)
        self.polygon(((5.1, 4.6), (11.4, 4.3), (10.1, 7.3), (6.0, 7.5)), rgba(self.light, 210))
        self.polygon(((3.0, 8.0), (6.0, 7.5), (5.0, 12.6)), rgba(self.shadow, 220))

    def wood(self):
        self.rounded((3.0, 5.0, 13.0, 11.8), 1.8, rgba(self.base), rgba(self.dark), 0.8)
        self.ellipse((9.4, 5.0, 13.3, 11.8), rgba(mix(self.base, self.paper, 0.34)), rgba(self.dark), 0.6)
        self.ellipse((10.3, 6.4, 12.5, 10.4), None, rgba(self.shadow), 0.45)
        self.line(((4.4, 6.4), (8.9, 6.7)), rgba(self.light, 170), 0.45)

    def cloth(self):
        self.polygon(((3.2, 4.4), (10.6, 2.6), (12.8, 9.9), (5.3, 13.6)), rgba(self.base), rgba(self.dark), 0.75)
        self.line(((5.0, 5.3), (10.7, 4.0), (11.6, 7.0)), rgba(self.light, 175), 0.55)
        self.line(((4.5, 8.2), (10.8, 6.7), (11.7, 9.8)), rgba(self.shadow), 0.55)

    def pile(self):
        self.ellipse((3.2, 10.0, 12.8, 13.8), rgba(self.shadow, 220))
        self.polygon(((3.8, 11.6), (6.6, 5.0), (9.3, 4.0), (12.2, 11.7)), rgba(self.base), rgba(self.dark), 0.7)
        for x, y in ((6.4, 8.0), (8.8, 6.0), (9.8, 9.2), (7.5, 11.0)):
            self.ellipse((x - 0.45, y - 0.45, x + 0.45, y + 0.45), rgba(self.light, 195))

    def shard(self):
        self.polygon(((7.5, 1.8), (11.2, 5.1), (9.8, 8.1), (12.1, 10.5), (7.8, 14.2), (3.9, 10.6), (5.2, 7.9), (4.5, 4.8)), rgba(self.base), rgba(self.dark), 0.75)
        self.polygon(((7.5, 1.8), (7.4, 8.5), (4.5, 4.8)), rgba(self.light, 185))
        self.line(((7.4, 8.5), (9.8, 8.1), (7.8, 14.2)), rgba(self.shadow), 0.5)

    def core(self):
        self.ellipse((3.0, 3.0, 13.0, 13.0), rgba(self.dark, 220))
        self.ellipse((3.8, 2.6, 12.2, 11.5), rgba(self.base), rgba(self.dark), 0.7)
        self.polygon(((8.0, 4.0), (10.7, 7.0), (8.0, 10.4), (5.3, 7.1)), rgba(self.light, 215), rgba(self.accent), 0.5)
        self.ellipse((6.8, 5.7, 8.2, 7.1), rgba((255, 255, 238), 190))

    def pouch(self):
        self.rounded((3.5, 5.2, 12.5, 14.0), 2.1, rgba(mix(self.paper, self.base, 0.22)), rgba(self.dark), 0.75)
        self.polygon(((5.0, 5.8), (6.2, 2.5), (9.8, 2.5), (11.0, 5.8)), rgba(shade(self.paper, 0.78)), rgba(self.dark), 0.55)
        self.line(((4.7, 6.2), (11.3, 6.2)), rgba((129, 65, 42)), 0.65)
        self.polygon(((8.0, 8.0), (10.0, 10.2), (8.0, 12.5), (6.0, 10.2)), rgba(self.accent), rgba(self.dark), 0.4)

    def material(self):
        motif = self.spec.motif
        if motif == "drop":
            self.polygon(((8.0, 1.7), (11.7, 7.7), (11.2, 11.8), (8.0, 14.2), (4.8, 11.8), (4.3, 7.7)), rgba(self.base, 235), rgba(self.dark), 0.75)
            self.ellipse((5.8, 5.2, 8.0, 8.2), rgba(self.light, 195))
            self.ellipse((8.8, 10.0, 10.3, 11.5), rgba(self.accent, 190))
        elif motif == "flower":
            self.plant(flower=True)
        elif motif == "mushroom":
            self.plant(mushroom=True)
        elif motif == "plant":
            self.plant()
        elif motif == "bamboo":
            self.plant(bamboo=True)
        elif motif == "feather":
            self.feather()
        elif motif == "bone":
            self.bone()
        elif motif == "scale" or motif == "hide":
            self.scale()
        elif motif == "vial":
            self.vial()
        elif motif == "ingot":
            self.ingot()
        elif motif == "wood":
            self.wood()
        elif motif == "cloth":
            self.cloth()
        elif motif == "pile":
            self.pile()
        elif motif == "shard":
            self.shard()
        elif motif == "core":
            self.core()
        elif motif == "gem":
            self.gem()
        elif motif == "food":
            self.pouch()
        else:
            if self.spec.variant in (0, 1):
                self.shard()
            elif self.spec.variant == 2:
                self.core()
            elif self.spec.variant == 3:
                self.ingot()
            else:
                self.pouch()

    def weapon(self):
        if self.spec.variant % 2:
            self.line(((3.0, 13.2), (12.7, 2.4)), rgba(self.dark), 2.2)
            self.polygon(((6.0, 10.6), (11.8, 2.2), (13.1, 1.6), (12.5, 3.8), (7.0, 11.4)), rgba(self.light), rgba(self.dark), 0.55)
            self.line(((3.5, 11.0), (5.8, 13.0)), rgba(self.accent), 1.0)
            self.line(((3.2, 13.0), (2.2, 14.0)), rgba((114, 67, 43)), 1.1)
        else:
            self.line(((2.7, 12.5), (13.0, 3.0)), rgba(self.dark), 2.1)
            self.polygon(((6.0, 10.3), (12.4, 2.1), (13.4, 2.0), (12.4, 4.2), (6.8, 11.1)), rgba(self.base), rgba(self.dark), 0.55)
            self.line(((4.0, 10.3), (6.3, 12.6)), rgba(self.accent), 0.9)
            self.line(((3.2, 12.3), (2.2, 13.4)), rgba((105, 63, 40)), 1.0)

    def shield(self):
        self.polygon(((8.0, 1.8), (13.0, 4.2), (12.2, 10.4), (8.0, 14.1), (3.8, 10.4), (3.0, 4.2)), rgba(self.base), rgba(self.dark), 0.85)
        self.polygon(((8.0, 3.4), (10.8, 5.0), (10.2, 9.2), (8.0, 11.7), (5.8, 9.2), (5.2, 5.0)), rgba(self.shadow), rgba(self.light), 0.45)
        self.line(((8.0, 3.6), (8.0, 11.4)), rgba(self.accent), 0.65)

    def mirror(self):
        self.ellipse((3.3, 2.0, 12.7, 11.4), rgba(self.dark))
        self.ellipse((4.1, 2.8, 11.9, 10.6), rgba(mix(self.base, (189, 217, 220), 0.50)), rgba(self.accent), 0.55)
        self.line(((7.0, 10.4), (6.5, 14.0)), rgba(self.dark), 1.7)
        self.line(((7.2, 10.4), (6.8, 13.8)), rgba(self.accent), 0.8)
        self.polygon(((5.2, 4.2), (8.1, 3.3), (6.1, 6.3)), rgba((255, 255, 240), 145))

    def compass(self):
        self.ellipse((2.5, 2.5, 13.5, 13.5), rgba(self.dark, 235))
        self.ellipse((3.3, 2.1, 12.7, 11.8), rgba(mix(self.base, (188, 202, 190), 0.52)), rgba(self.accent), 0.65)
        self.ellipse((5.1, 3.9, 10.9, 9.7), rgba(shade(self.base, 0.72)), rgba(self.light), 0.45)
        self.polygon(((8.0, 3.2), (9.2, 7.1), (8.0, 10.6), (6.8, 7.1)), rgba(self.accent), rgba(self.dark), 0.4)
        self.polygon(((8.0, 3.2), (9.2, 7.1), (8.0, 7.6)), rgba(self.light, 220))
        self.ellipse((7.2, 6.3, 8.8, 7.9), rgba(self.dark))

    def bell(self):
        self.polygon(((5.0, 4.0), (7.0, 2.0), (9.0, 2.0), (11.0, 4.0), (12.4, 11.2), (3.6, 11.2)), rgba(self.base), rgba(self.dark), 0.8)
        self.ellipse((3.4, 9.6, 12.6, 13.0), rgba(self.shadow), rgba(self.dark), 0.6)
        self.ellipse((7.0, 11.5, 9.0, 14.0), rgba(self.accent), rgba(self.dark), 0.45)
        self.line(((6.3, 5.0), (5.3, 9.0)), rgba(self.light, 160), 0.5)

    def fan(self):
        self.polygon(((7.9, 13.8), (3.0, 7.7), (4.3, 3.2), (8.0, 2.0), (11.8, 3.5), (13.0, 7.8)), rgba(self.paper), rgba(self.dark), 0.8)
        for x in (4.2, 6.0, 8.0, 10.0, 11.8):
            self.line(((7.9, 13.4), (x, 4.0)), rgba(self.base, 185), 0.45)
        self.ellipse((6.9, 12.4, 8.9, 14.4), rgba(self.accent), rgba(self.dark), 0.4)

    def ring(self):
        self.ellipse((2.8, 3.0, 13.2, 13.4), rgba(self.dark))
        self.ellipse((3.8, 2.2, 12.2, 11.8), rgba(self.base), rgba(self.accent), 0.65)
        self.ellipse((5.7, 4.2, 10.3, 9.4), rgba((0, 0, 0), 0), rgba(self.dark), 0.8)
        self.polygon(((8.0, 1.4), (10.3, 3.2), (8.0, 5.0), (5.7, 3.2)), rgba(self.accent), rgba(self.dark), 0.45)

    def boots(self):
        self.polygon(((3.3, 2.0), (7.2, 2.5), (7.0, 9.0), (10.0, 11.2), (9.0, 13.7), (3.1, 12.0)), rgba(self.base), rgba(self.dark), 0.75)
        self.polygon(((8.0, 3.0), (11.7, 3.5), (11.2, 9.0), (13.1, 11.4), (12.0, 13.5), (7.4, 11.5)), rgba(self.shadow), rgba(self.dark), 0.7)
        self.line(((4.3, 4.2), (6.3, 4.5)), rgba(self.accent), 0.55)
        self.line(((9.0, 5.0), (11.1, 5.2)), rgba(self.accent), 0.55)

    def umbrella(self):
        self.polygon(((2.0, 7.0), (4.0, 3.7), (8.0, 1.8), (12.0, 3.7), (14.0, 7.0)), rgba(self.base), rgba(self.dark), 0.8)
        self.line(((8.0, 2.0), (8.0, 12.4), (6.8, 14.0), (5.8, 13.2)), rgba(self.dark), 0.85)
        self.line(((4.0, 4.0), (8.0, 7.0), (12.0, 4.0)), rgba(self.light, 180), 0.45)

    def vessel(self):
        self.ellipse((3.0, 4.5, 13.0, 9.0), rgba(self.base), rgba(self.dark), 0.75)
        self.polygon(((3.3, 7.0), (12.7, 7.0), (11.4, 13.4), (4.6, 13.4)), rgba(self.shadow), rgba(self.dark), 0.75)
        self.ellipse((4.4, 4.9, 11.6, 7.5), rgba(shade(self.base, 0.55)), rgba(self.accent), 0.45)
        self.line(((5.0, 9.3), (11.0, 9.3)), rgba(self.light, 170), 0.45)

    def bottle(self):
        glass = mix((139, 188, 152), self.base, 0.45)
        self.rounded((6.1, 1.6, 9.9, 4.8), 0.6, rgba(self.light, 235), rgba(self.dark), 0.65)
        self.rectangle((5.7, 1.4, 10.3, 2.6), rgba(self.accent), rgba(self.dark), 0.55)
        self.polygon(
            ((6.0, 4.0), (10.0, 4.0), (11.8, 6.1), (11.2, 13.3), (9.9, 14.2), (6.1, 14.2), (4.8, 13.3), (4.2, 6.1)),
            rgba(glass, 245),
            rgba(self.dark),
            0.8,
        )
        self.polygon(((4.8, 8.3), (11.2, 8.3), (10.8, 13.1), (9.6, 13.7), (6.4, 13.7), (5.2, 13.1)), rgba(self.base, 220))
        self.line(((5.7, 5.6), (5.2, 10.0)), rgba((251, 255, 232), 175), 0.5)
        self.ellipse((7.0, 9.0, 9.3, 11.3), rgba(self.accent, 215), rgba(self.dark), 0.35)

    def vehicle(self):
        self.polygon(((2.0, 9.0), (5.0, 12.8), (11.8, 12.0), (14.0, 8.2), (10.0, 9.3), (6.0, 8.3)), rgba(self.base), rgba(self.dark), 0.8)
        self.polygon(((5.0, 8.2), (7.7, 3.0), (8.2, 9.0)), rgba(mix(self.paper, self.base, 0.18)), rgba(self.dark), 0.55)
        self.polygon(((8.0, 3.0), (12.0, 6.5), (8.3, 8.7)), rgba(self.light, 210), rgba(self.dark), 0.55)
        self.line(((3.5, 10.0), (12.6, 9.2)), rgba(self.accent), 0.55)

    def puppet(self):
        self.ellipse((5.5, 1.8, 10.5, 6.5), rgba(self.base), rgba(self.dark), 0.7)
        self.rounded((5.0, 6.0, 11.0, 12.2), 1.0, rgba(self.shadow), rgba(self.dark), 0.75)
        self.line(((5.2, 7.0), (2.8, 10.5)), rgba(self.dark), 1.1)
        self.line(((10.8, 7.0), (13.2, 10.5)), rgba(self.dark), 1.1)
        self.line(((6.5, 11.5), (5.2, 14.2)), rgba(self.dark), 1.1)
        self.line(((9.5, 11.5), (10.8, 14.2)), rgba(self.dark), 1.1)
        self.ellipse((6.7, 3.2, 7.6, 4.1), rgba(self.accent))
        self.ellipse((8.4, 3.2, 9.3, 4.1), rgba(self.accent))

    def flag(self):
        self.line(((4.0, 1.6), (4.0, 14.3)), rgba(self.dark), 1.0)
        self.polygon(((4.4, 2.1), (12.7, 4.0), (10.2, 8.0), (4.4, 6.7)), rgba(self.base), rgba(self.dark), 0.7)
        self.polygon(((7.0, 3.5), (10.5, 4.2), (8.8, 6.4), (6.2, 5.7)), rgba(self.accent), rgba(self.dark), 0.35)

    def disk(self):
        self.ellipse((2.6, 2.6, 13.4, 13.4), rgba(self.dark))
        self.ellipse((3.4, 2.2, 12.6, 11.5), rgba(self.base), rgba(self.accent), 0.6)
        self.polygon(((8.0, 3.5), (10.6, 7.0), (8.0, 10.3), (5.4, 7.0)), rgba(self.shadow), rgba(self.light), 0.45)
        self.ellipse((7.0, 5.9, 9.0, 7.9), rgba(self.accent))

    def artifact(self):
        motif = self.spec.motif
        if motif == "weapon" or motif == "bow":
            self.weapon()
        elif motif == "shield":
            self.shield()
        elif motif == "mirror":
            self.mirror()
        elif motif == "compass":
            self.compass()
        elif motif == "bell":
            self.bell()
        elif motif == "fan":
            self.fan()
        elif motif == "ring":
            self.ring()
        elif motif == "boots":
            self.boots()
        elif motif == "umbrella":
            self.umbrella()
        elif motif == "vessel":
            self.vessel()
        elif motif == "bottle":
            self.bottle()
        elif motif == "vehicle":
            self.vehicle()
        elif motif == "puppet":
            self.puppet()
        elif motif == "flag":
            self.flag()
        elif motif == "disk":
            self.disk()
        elif motif == "orb":
            self.core()
        else:
            if self.spec.variant in (0, 1):
                self.disk()
            elif self.spec.variant == 2:
                self.core()
            elif self.spec.variant == 3:
                self.vessel()
            else:
                self.mirror()

    def block_item(self):
        motif = self.spec.motif
        if motif == "gate":
            self.polygon(((3.0, 13.7), (3.0, 4.2), (6.0, 1.8), (10.0, 1.8), (13.0, 4.2), (13.0, 13.7), (10.6, 13.7), (10.6, 6.0), (8.0, 4.2), (5.4, 6.0), (5.4, 13.7)), rgba(self.base), rgba(self.dark), 0.7)
            self.ellipse((5.2, 4.0, 10.8, 13.8), rgba(self.accent, 85), rgba(self.light), 0.45)
            return
        if motif == "furnace":
            self.polygon(((3.0, 5.0), (6.0, 2.2), (11.7, 3.3), (13.0, 6.0), (12.0, 13.5), (4.0, 13.5)), rgba(self.base), rgba(self.dark), 0.8)
            self.ellipse((5.0, 6.3, 11.0, 12.0), rgba(shade(self.base, 0.45)), rgba(self.accent), 0.55)
            self.polygon(((6.4, 10.8), (8.0, 7.0), (9.8, 10.8)), rgba(mix(ELEMENT_COLORS["fire"], self.accent, 0.2)))
            self.rectangle((5.2, 2.0, 11.2, 3.5), rgba(self.light), rgba(self.dark), 0.6)
            return
        if motif == "array_block":
            self.polygon(((2.3, 10.0), (7.8, 6.3), (13.7, 9.3), (8.2, 13.7)), rgba(self.base), rgba(self.dark), 0.75)
            self.polygon(((8.0, 7.6), (11.2, 9.4), (8.0, 12.0), (4.8, 10.0)), rgba(self.shadow), rgba(self.accent), 0.45)
            self.ellipse((7.0, 8.7, 9.0, 10.7), rgba(self.light))
            self.polygon(((6.4, 6.9), (8.0, 2.0), (9.6, 6.9)), rgba(self.accent), rgba(self.dark), 0.55)
            return
        if motif == "workstation":
            self.polygon(((2.5, 6.0), (8.0, 3.0), (13.5, 5.8), (8.0, 9.0)), rgba(self.light), rgba(self.dark), 0.7)
            self.polygon(((2.5, 6.0), (8.0, 9.0), (8.0, 14.0), (2.5, 11.0)), rgba(self.base), rgba(self.dark), 0.7)
            self.polygon(((8.0, 9.0), (13.5, 5.8), (13.5, 11.0), (8.0, 14.0)), rgba(self.shadow), rgba(self.dark), 0.7)
            self.line(((5.0, 5.4), (10.4, 5.8)), rgba(self.accent), 0.55)
            return
        self.polygon(((3.0, 5.5), (8.0, 2.7), (13.0, 5.5), (8.0, 8.6)), rgba(self.light), rgba(self.dark), 0.7)
        self.polygon(((3.0, 5.5), (8.0, 8.6), (8.0, 14.0), (3.0, 11.0)), rgba(self.base), rgba(self.dark), 0.7)
        self.polygon(((8.0, 8.6), (13.0, 5.5), (13.0, 11.0), (8.0, 14.0)), rgba(self.shadow), rgba(self.dark), 0.7)
        self.polygon(((8.0, 4.0), (10.4, 5.5), (8.0, 7.0), (5.6, 5.5)), rgba(self.accent), rgba(self.dark), 0.35)

    def consumable(self):
        motif = self.spec.motif
        if motif == "talisman":
            self.talisman()
        elif motif == "pouch" or motif == "food":
            self.pouch()
        elif motif == "orb":
            self.core()
        elif motif == "scroll":
            self.manual()
        else:
            self.vial()

    def signature(self, image: Image.Image):
        pixels = image.load()
        candidates = []
        for y in range(3, 14):
            for x in range(3, 13):
                if pixels[x, y][3] >= 180:
                    candidates.append((x, y))
        if not candidates:
            return
        x, y = candidates[self.seed[15] % len(candidates)]
        old = pixels[x, y]
        tint = shifted(self.accent, self.seed[16:])
        amount = 0.14 + (self.seed[17] % 4) * 0.04
        new = mix(old[:3], tint, amount)
        pixels[x, y] = (*new, old[3])

    def render(self) -> Image.Image:
        self.aura()
        category = self.spec.category
        if category == "pill":
            self.pill_bottle(vial=self.spec.motif == "vial", powder=self.spec.motif == "powder_jar")
        elif category == "manual":
            self.manual()
        elif category == "talisman":
            self.talisman()
        elif category == "currency":
            self.currency()
        elif category == "block_item":
            self.block_item()
        elif category in ("artifact", "equipment"):
            self.artifact()
        elif category == "consumable":
            self.consumable()
        else:
            self.material()
        result = self.image.resize((OUTPUT_SIZE, OUTPUT_SIZE), Image.Resampling.LANCZOS)
        self.signature(result)
        return result


def ensure_unique(images: dict[str, Image.Image]) -> int:
    seen: dict[str, str] = {}
    changed = 0
    for item_id in sorted(images):
        image = images[item_id]
        attempt = 0
        while True:
            digest = hashlib.sha256(image.tobytes()).hexdigest()
            if digest not in seen:
                seen[digest] = item_id
                break
            pixels = image.load()
            seed = stable_bytes(f"{item_id}:{attempt}")
            candidates = [
                (x, y)
                for y in range(3, 14)
                for x in range(3, 13)
                if pixels[x, y][3] >= 160
            ]
            if not candidates:
                raise RuntimeError(f"cannot disambiguate empty texture: {item_id}")
            x, y = candidates[seed[0] % len(candidates)]
            old = pixels[x, y]
            pixels[x, y] = (
                clamp(old[0] + 7 + seed[1] % 19),
                clamp(old[1] + 3 + seed[2] % 13),
                clamp(old[2] + 5 + seed[3] % 17),
                old[3],
            )
            attempt += 1
            changed += 1
            if attempt > 32:
                raise RuntimeError(f"failed to make texture unique: {item_id}")
    return changed


def audit_textures(paths: Iterable[Path]) -> dict:
    issues = []
    hashes: dict[str, list[str]] = defaultdict(list)
    coverage = []
    for path in sorted(paths):
        try:
            with Image.open(path) as image:
                image.load()
                if image.size != (OUTPUT_SIZE, OUTPUT_SIZE):
                    issues.append(f"{path.name}: size {image.size}")
                if image.mode != "RGBA":
                    issues.append(f"{path.name}: mode {image.mode}")
                rgba_image = image.convert("RGBA")
                alpha = rgba_image.getchannel("A")
                nonzero = sum(
                    1
                    for y in range(alpha.height)
                    for x in range(alpha.width)
                    if alpha.getpixel((x, y)) > 0
                )
                ratio = nonzero / (rgba_image.width * rgba_image.height)
                coverage.append(ratio)
                if not 0.08 <= ratio <= 0.82:
                    issues.append(f"{path.name}: coverage {ratio:.3f}")
                corner_points = (
                    (0, 0),
                    (rgba_image.width - 1, 0),
                    (0, rgba_image.height - 1),
                    (rgba_image.width - 1, rgba_image.height - 1),
                )
                corners = [rgba_image.getpixel(point)[3] for point in corner_points]
                if any(corners):
                    issues.append(f"{path.name}: opaque corner")
                digest = hashlib.sha256(rgba_image.tobytes()).hexdigest()
                hashes[digest].append(path.name)
        except Exception as exc:
            issues.append(f"{path.name}: {exc}")
    duplicates = [names for names in hashes.values() if len(names) > 1]
    if duplicates:
        issues.append(f"duplicate pixel groups: {len(duplicates)}")
    return {
        "count": len(list(paths)) if not isinstance(paths, list) else len(paths),
        "issues": issues,
        "duplicate_groups": duplicates,
        "coverage_min": min(coverage) if coverage else 0.0,
        "coverage_max": max(coverage) if coverage else 0.0,
        "coverage_mean": sum(coverage) / len(coverage) if coverage else 0.0,
    }


def write_contact_sheets(specs: list[ItemSpec], directory: Path):
    directory.mkdir(parents=True, exist_ok=True)
    by_category: dict[str, list[ItemSpec]] = defaultdict(list)
    for spec in specs:
        by_category[spec.category].append(spec)
    font = ImageFont.load_default()
    for category, category_specs in sorted(by_category.items()):
        columns = 10
        cell_width = 80
        cell_height = 86
        rows = math.ceil(len(category_specs) / columns)
        sheet = Image.new("RGBA", (columns * cell_width, rows * cell_height), (30, 28, 24, 255))
        draw = ImageDraw.Draw(sheet)
        for index, spec in enumerate(category_specs):
            texture = Image.open(TEXTURE_DIR / f"{spec.item_id}.png").convert("RGBA")
            texture = texture.resize((64, 64), Image.Resampling.NEAREST)
            x = (index % columns) * cell_width + 8
            y = (index // columns) * cell_height + 2
            sheet.alpha_composite(texture, (x, y))
            label = spec.item_id[:12]
            draw.text((x, y + 66), label, fill=(232, 224, 202, 255), font=font)
        sheet.save(directory / f"{category}.png", format="PNG", optimize=True)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="audit and verify existing textures without writing")
    parser.add_argument("--contact-sheets", type=Path, help="optional directory for visual review sheets")
    args = parser.parse_args()

    texture_paths = sorted(TEXTURE_DIR.glob("*.png"))
    if not texture_paths:
        raise SystemExit(f"no item textures found under {TEXTURE_DIR}")
    texture_ids = discover_texture_ids()
    specs = build_specs(texture_ids)

    images = {spec.item_id: Painter(spec).render() for spec in specs}
    adjusted = ensure_unique(images)
    render_mismatches = []
    if args.check:
        for item_id, expected in images.items():
            path = TEXTURE_DIR / f"{item_id}.png"
            if not path.exists():
                render_mismatches.append(item_id)
                continue
            try:
                with Image.open(path) as actual:
                    if actual.convert("RGBA").tobytes() != expected.tobytes():
                        render_mismatches.append(item_id)
            except Exception:
                render_mismatches.append(item_id)
    else:
        for item_id, image in images.items():
            image.save(TEXTURE_DIR / f"{item_id}.png", format="PNG", optimize=True)
        print(f"wrote {len(images)} textures; uniqueness adjustments={adjusted}")

    audit = audit_textures(sorted(TEXTURE_DIR.glob("*.png")))
    audit["category_counts"] = dict(Counter(spec.category for spec in specs))
    audit["motif_counts"] = dict(Counter(spec.motif for spec in specs))
    audit["element_counts"] = dict(Counter(spec.element for spec in specs))
    audit["render_mismatch_count"] = len(render_mismatches)
    audit["uniqueness_adjustments"] = adjusted
    if render_mismatches:
        audit["issues"].append(
            f"deterministic render mismatches: {len(render_mismatches)} "
            f"({', '.join(render_mismatches[:8])})"
        )
    print(json.dumps(audit, indent=2, sort_keys=True))
    if audit["issues"]:
        return 1
    if args.contact_sheets:
        write_contact_sheets(specs, args.contact_sheets)
        print(f"contact sheets: {args.contact_sheets}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
