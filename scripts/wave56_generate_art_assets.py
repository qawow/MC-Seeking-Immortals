#!/usr/bin/env python3
"""Wave56 asset generator: servitor textures, portraits, biomes, missing icons, sounds.json."""
from __future__ import annotations

import hashlib
import json
import math
import struct
import wave
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(".")
ASSETS = ROOT / "src/main/resources/assets/seeking_immortals"
DATA = ROOT / "src/main/resources/data/seeking_immortals"


def ensure(path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)


def save_png(image: Image.Image, path: Path) -> None:
    ensure(path)
    image.save(path, format="PNG")


def gradient(size, c1, c2, vertical=True):
    width, height = size
    image = Image.new("RGBA", size)
    pixels = image.load()
    for y in range(height):
        for x in range(width):
            t = y / max(1, height - 1) if vertical else x / max(1, width - 1)
            r = int(c1[0] * (1 - t) + c2[0] * t)
            g = int(c1[1] * (1 - t) + c2[1] * t)
            b = int(c1[2] * (1 - t) + c2[2] * t)
            a = int(c1[3] * (1 - t) + c2[3] * t) if len(c1) > 3 else 255
            pixels[x, y] = (r, g, b, a)
    return image


def deterministic_color(name: str):
    digest = hashlib.md5(name.encode()).hexdigest()
    r = 80 + (int(digest[0:2], 16) % 160)
    g = 80 + (int(digest[2:4], 16) % 160)
    b = 80 + (int(digest[4:6], 16) % 160)
    return (r, g, b, 255)


def make_icon(name: str, size: int = 64, kind: str = "item") -> Image.Image:
    base = deterministic_color(name)
    image = gradient((size, size), (base[0] // 2, base[1] // 2, base[2] // 2, 255), base)
    draw = ImageDraw.Draw(image)
    draw.rectangle([1, 1, size - 2, size - 2], outline=(255, 240, 200, 220), width=2)
    if kind == "manual":
        draw.rectangle(
            [size * 0.25, size * 0.15, size * 0.75, size * 0.85],
            fill=(250, 240, 210, 255),
            outline=(90, 60, 20, 255),
        )
        for i in range(4):
            yy = size * 0.28 + i * size * 0.12
            draw.line([size * 0.32, yy, size * 0.68, yy], fill=(120, 80, 40, 255), width=1)
    elif kind == "artifact":
        draw.polygon(
            [
                (size * 0.5, size * 0.12),
                (size * 0.78, size * 0.5),
                (size * 0.5, size * 0.88),
                (size * 0.22, size * 0.5),
            ],
            fill=(220, 210, 120, 255),
            outline=(80, 60, 10, 255),
        )
    elif kind == "pill":
        draw.ellipse(
            [size * 0.2, size * 0.2, size * 0.8, size * 0.8],
            fill=base,
            outline=(255, 255, 255, 200),
        )
        draw.ellipse(
            [size * 0.3, size * 0.28, size * 0.48, size * 0.42],
            fill=(255, 255, 255, 120),
        )
    elif kind == "ore":
        draw.polygon(
            [
                (size * 0.2, size * 0.7),
                (size * 0.35, size * 0.25),
                (size * 0.55, size * 0.45),
                (size * 0.8, size * 0.2),
                (size * 0.85, size * 0.75),
            ],
            fill=base,
            outline=(30, 30, 30, 255),
        )
    else:
        draw.rounded_rectangle(
            [size * 0.2, size * 0.2, size * 0.8, size * 0.8],
            radius=8,
            fill=base,
            outline=(255, 255, 255, 180),
        )
    label = "".join(ch for ch in name if ch.isalnum())[:2].upper() or "SI"
    try:
        font = ImageFont.load_default()
    except Exception:
        font = None
    draw.text((size * 0.35, size * 0.4), label[:2], fill=(255, 255, 255, 230), font=font)
    return image


def make_servitor(path: Path, palette: dict) -> None:
    image = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.ellipse([20, 4, 44, 28], fill=palette["skin"], outline=palette["line"])
    draw.rectangle([22, 28, 42, 48], fill=palette["body"], outline=palette["line"])
    draw.rectangle([12, 28, 22, 46], fill=palette["body"], outline=palette["line"])
    draw.rectangle([42, 28, 52, 46], fill=palette["body"], outline=palette["line"])
    draw.rectangle([22, 48, 30, 62], fill=palette["legs"], outline=palette["line"])
    draw.rectangle([34, 48, 42, 62], fill=palette["legs"], outline=palette["line"])
    draw.ellipse([26, 12, 30, 16], fill=palette["eye"])
    draw.ellipse([34, 12, 38, 16], fill=palette["eye"])
    save_png(image, path)


def make_portrait(path: Path, title: str, colors) -> None:
    image = gradient((72, 88), colors[0], colors[1])
    draw = ImageDraw.Draw(image)
    draw.ellipse([16, 18, 56, 62], fill=colors[2], outline=(20, 20, 20, 255))
    draw.ellipse([28, 32, 34, 38], fill=(20, 20, 20, 255))
    draw.ellipse([40, 32, 46, 38], fill=(20, 20, 20, 255))
    draw.arc([30, 42, 44, 54], 20, 160, fill=(80, 40, 40, 255), width=2)
    draw.polygon([(10, 70), (36, 55), (62, 70), (62, 88), (10, 88)], fill=colors[3])
    draw.rectangle([0, 0, 71, 87], outline=(220, 190, 120, 255), width=2)
    try:
        font = ImageFont.load_default()
    except Exception:
        font = None
    draw.text((8, 4), title[:8], fill=(255, 245, 220, 255), font=font)
    save_png(image, path)


def write_tone_wav(path: Path, freq: float, duration: float = 0.25, rate: int = 22050) -> None:
    ensure(path)
    with wave.open(str(path), "w") as handle:
        handle.setnchannels(1)
        handle.setsampwidth(2)
        handle.setframerate(rate)
        frames = bytearray()
        total = int(rate * duration)
        for index in range(total):
            t = index / rate
            envelope = 1.0 - (t / duration)
            value = int(12000 * math.sin(2 * math.pi * freq * t) * envelope)
            frames += struct.pack("<h", max(-32767, min(32767, value)))
        handle.writeframes(frames)


def main() -> None:
    make_servitor(
        ASSETS / "textures/entity/summoned_servitor.png",
        {
            "skin": (210, 190, 160, 255),
            "body": (70, 120, 180, 255),
            "legs": (40, 70, 110, 255),
            "line": (20, 20, 30, 255),
            "eye": (240, 240, 255, 255),
        },
    )
    make_servitor(
        ASSETS / "textures/entity/summoned_servitor_beast.png",
        {
            "skin": (180, 120, 70, 255),
            "body": (120, 70, 30, 255),
            "legs": (90, 50, 20, 255),
            "line": (40, 20, 10, 255),
            "eye": (255, 220, 80, 255),
        },
    )
    make_servitor(
        ASSETS / "textures/entity/summoned_servitor_puppet.png",
        {
            "skin": (190, 190, 190, 255),
            "body": (140, 140, 150, 255),
            "legs": (100, 100, 110, 255),
            "line": (40, 40, 50, 255),
            "eye": (80, 220, 255, 255),
        },
    )
    make_servitor(
        ASSETS / "textures/entity/summoned_servitor_ghost.png",
        {
            "skin": (160, 200, 210, 180),
            "body": (90, 140, 160, 160),
            "legs": (70, 110, 130, 140),
            "line": (200, 240, 255, 200),
            "eye": (180, 255, 255, 255),
        },
    )
    print("servitor textures ok")

    portraits = {
        "portrait_default.png": (
            "Guide",
            ((30, 40, 70, 255), (60, 90, 140, 255), (220, 190, 160, 255), (50, 70, 120, 255)),
        ),
        "portrait_mo_lao.png": (
            "Mo Lao",
            ((40, 30, 20, 255), (90, 70, 40, 255), (210, 180, 140, 255), (70, 50, 30, 255)),
        ),
        "portrait_mulan.png": (
            "Mulan",
            ((40, 20, 20, 255), (120, 40, 40, 255), (230, 190, 160, 255), (120, 30, 30, 255)),
        ),
        "portrait_yinluo.png": (
            "Yinluo",
            ((20, 10, 30, 255), (50, 20, 70, 255), (180, 170, 200, 255), (40, 20, 60, 255)),
        ),
        "portrait_star_broker.png": (
            "Star",
            ((10, 20, 50, 255), (30, 50, 110, 255), (220, 200, 170, 255), (20, 40, 90, 255)),
        ),
        "portrait_kunwu.png": (
            "Kunwu",
            ((30, 40, 30, 255), (70, 90, 50, 255), (200, 180, 150, 255), (50, 70, 40, 255)),
        ),
    }
    for name, (title, colors) in portraits.items():
        make_portrait(ASSETS / "textures/gui/dialogue" / name, title, colors)
    print("portraits ok")

    sound_dir = ASSETS / "sounds/dialogue"
    frequencies = {
        "dialogue_greeting": 440.0,
        "dialogue_advance": 520.0,
        "dialogue_branch": 360.0,
        "dialogue_npc_mo_lao": 300.0,
        "dialogue_npc_guide": 480.0,
    }
    for name, freq in frequencies.items():
        write_tone_wav(sound_dir / f"{name}.wav", freq)

    sounds_json = {
        "dialogue_greeting": {
            "subtitle": "subtitles.seeking_immortals.dialogue_greeting",
            "sounds": [{"name": "minecraft:entity.villager.yes", "type": "event"}],
        },
        "dialogue_advance": {
            "subtitle": "subtitles.seeking_immortals.dialogue_advance",
            "sounds": [{"name": "minecraft:ui.button.click", "type": "event"}],
        },
        "dialogue_branch": {
            "subtitle": "subtitles.seeking_immortals.dialogue_branch",
            "sounds": [{"name": "minecraft:item.book.page_turn", "type": "event"}],
        },
        "dialogue_npc_mo_lao": {
            "subtitle": "subtitles.seeking_immortals.dialogue_npc_mo_lao",
            "sounds": [{"name": "minecraft:entity.villager.ambient", "type": "event"}],
        },
        "dialogue_npc_guide": {
            "subtitle": "subtitles.seeking_immortals.dialogue_npc_guide",
            "sounds": [{"name": "minecraft:entity.villager.trade", "type": "event"}],
        },
    }
    ensure(ASSETS / "sounds.json")
    (ASSETS / "sounds.json").write_text(json.dumps(sounds_json, indent=2) + "\n", encoding="utf-8")
    print("sounds.json ok")

    biomes = {
        "secret_mist_cave": {
            "temperature": 0.5,
            "downfall": 0.8,
            "has_precipitation": True,
            "effects": {
                "sky_color": 0x3A4A5A,
                "fog_color": 0x6A7A8A,
                "water_color": 0x3D5C63,
                "water_fog_color": 0x2A3A40,
                "grass_color": 0x5A7A6A,
                "foliage_color": 0x4A6A5A,
                "mood_sound": {
                    "sound": "minecraft:ambient.cave",
                    "tick_delay": 6000,
                    "block_search_extent": 8,
                    "offset": 2.0,
                },
            },
            "spawners": {},
            "spawn_costs": {},
            "carvers": {},
            "features": [],
        },
        "secret_blood_forbidden": {
            "temperature": 1.1,
            "downfall": 0.1,
            "has_precipitation": False,
            "effects": {
                "sky_color": 0x401010,
                "fog_color": 0x602020,
                "water_color": 0x8A1A1A,
                "water_fog_color": 0x4A0A0A,
                "grass_color": 0x6A2020,
                "foliage_color": 0x5A1010,
                "mood_sound": {
                    "sound": "minecraft:ambient.nether_wastes.mood",
                    "tick_delay": 6000,
                    "block_search_extent": 8,
                    "offset": 2.0,
                },
            },
            "spawners": {},
            "spawn_costs": {},
            "carvers": {},
            "features": [],
        },
        "secret_void_palace": {
            "temperature": 0.2,
            "downfall": 0.0,
            "has_precipitation": False,
            "effects": {
                "sky_color": 0x101028,
                "fog_color": 0x1A1A3A,
                "water_color": 0x2A2A6A,
                "water_fog_color": 0x101040,
                "grass_color": 0x3A3A5A,
                "foliage_color": 0x2A2A4A,
                "mood_sound": {
                    "sound": "minecraft:ambient.soul_sand_valley.mood",
                    "tick_delay": 6000,
                    "block_search_extent": 8,
                    "offset": 2.0,
                },
            },
            "spawners": {},
            "spawn_costs": {},
            "carvers": {},
            "features": [],
        },
        "secret_fallen_demon": {
            "temperature": 1.4,
            "downfall": 0.0,
            "has_precipitation": False,
            "effects": {
                "sky_color": 0x2A1010,
                "fog_color": 0x401818,
                "water_color": 0x6A1010,
                "water_fog_color": 0x300808,
                "grass_color": 0x4A1810,
                "foliage_color": 0x3A1008,
                "mood_sound": {
                    "sound": "minecraft:ambient.basalt_deltas.mood",
                    "tick_delay": 6000,
                    "block_search_extent": 8,
                    "offset": 2.0,
                },
            },
            "spawners": {},
            "spawn_costs": {},
            "carvers": {},
            "features": [],
        },
    }
    for biome_id, body in biomes.items():
        path = DATA / "worldgen/biome" / f"{biome_id}.json"
        ensure(path)
        path.write_text(json.dumps(body, indent=2) + "\n", encoding="utf-8")
    print("biomes ok")

    dim_map = {
        "secret_realm_mist_cave.json": ("secret_mist_cave", "minecraft:caves", "secret_realm_mist"),
        "secret_realm_blood_forbidden.json": (
            "secret_blood_forbidden",
            "minecraft:nether",
            "secret_realm_blood",
        ),
        "secret_realm_void_palace.json": ("secret_void_palace", "minecraft:end", "secret_realm_void"),
        "secret_realm_fallen_demon.json": (
            "secret_fallen_demon",
            "minecraft:nether",
            "secret_realm_demon",
        ),
    }
    type_defs = {
        "secret_realm_mist": (0.15, "minecraft:overworld", False, True, False, -64, 384),
        "secret_realm_blood": (0.05, "minecraft:the_nether", True, False, True, 0, 256),
        "secret_realm_void": (0.02, "minecraft:the_end", False, False, False, 0, 256),
        "secret_realm_demon": (0.08, "minecraft:the_nether", True, False, True, 0, 256),
    }
    for type_id, (ambient, effects, ultrawarm, skylight, ceiling, min_y, height) in type_defs.items():
        body = {
            "ultrawarm": ultrawarm,
            "natural": False,
            "coordinate_scale": 1.0,
            "piglin_safe": True,
            "respawn_anchor_works": False,
            "bed_works": False,
            "has_raids": False,
            "has_skylight": skylight,
            "has_ceiling": ceiling,
            "ambient_light": ambient,
            "logical_height": height,
            "infiniburn": "#minecraft:infiniburn_nether" if ultrawarm else "#minecraft:infiniburn_overworld",
            "effects": effects,
            "min_y": min_y,
            "height": height,
            "monster_spawn_light_level": {
                "type": "minecraft:uniform",
                "value": {"min_inclusive": 0, "max_inclusive": 7},
            },
            "monster_spawn_block_light_limit": 0,
        }
        path = DATA / "dimension_type" / f"{type_id}.json"
        ensure(path)
        path.write_text(json.dumps(body, indent=2) + "\n", encoding="utf-8")

    for file_name, (biome, settings, type_id) in dim_map.items():
        body = {
            "type": f"seeking_immortals:{type_id}",
            "generator": {
                "type": "minecraft:noise",
                "settings": settings,
                "biome_source": {
                    "type": "minecraft:fixed",
                    "biome": f"seeking_immortals:{biome}",
                },
            },
        }
        path = DATA / "dimension" / file_name
        path.write_text(json.dumps(body, indent=2) + "\n", encoding="utf-8")
    print("dimensions remapped")

    models_dir = ASSETS / "models/item"
    tex_dir = ASSETS / "textures/item"
    models = {path.stem for path in models_dir.glob("*.json")}
    textures = {path.stem for path in tex_dir.glob("*.png")}
    missing = sorted(models - textures)
    created = 0
    for name in missing:
        if "manual" in name or "formula" in name or "jade_slip" in name:
            kind = "manual"
        elif "pill" in name:
            kind = "pill"
        elif any(
            token in name
            for token in (
                "sword",
                "shield",
                "mirror",
                "bell",
                "fan",
                "boots",
                "bracelet",
                "artifact",
                "ruler",
                "needle",
                "disk",
                "pendant",
                "umbrella",
                "bowl",
                "chain",
                "brick",
                "bead",
            )
        ):
            kind = "artifact"
        elif any(token in name for token in ("ore", "iron", "copper", "stone", "crystal", "jade", "seam")):
            kind = "ore"
        else:
            kind = "item"
        save_png(make_icon(name, 64, kind), tex_dir / f"{name}.png")
        created += 1
    print("created item textures", created)

    block_models = ASSETS / "models/block"
    block_tex = ASSETS / "textures/block"
    block_created = 0
    if block_models.exists():
        bmodels = {path.stem for path in block_models.glob("*.json")}
        btex = {path.stem for path in block_tex.glob("*.png")} if block_tex.exists() else set()
        for name in sorted(bmodels - btex):
            save_png(make_icon(name, 64, "ore"), block_tex / f"{name}.png")
            block_created += 1
    print("created block textures", block_created)

    manifest = {
        "wave": "0.1.439",
        "item_textures_created": created,
        "block_textures_created": block_created,
        "portraits": list(portraits.keys()),
        "servitor_textures": [
            "summoned_servitor.png",
            "summoned_servitor_beast.png",
            "summoned_servitor_puppet.png",
            "summoned_servitor_ghost.png",
        ],
        "biomes": list(biomes.keys()),
        "sounds": list(sounds_json.keys()),
    }
    manifest_path = Path("project_docs/art_pack_manifest_0.1.439.json")
    ensure(manifest_path)
    manifest_path.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print("manifest", manifest_path)
    print("ALL ASSETS DONE")


if __name__ == "__main__":
    main()
