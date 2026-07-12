#!/usr/bin/env python3
"""Generate 16x16 RGBA placeholder skill icons for seeking_immortals.

For each releaseable techniqueId in SkillType (skill/SkillType.java), emit a
16x16 PNG to assets/seeking_immortals/textures/gui/skill/<id>.png. Coloring by
spiritual-root attribute; a simple symbol per skill family distinguishes icons.

Idempotent: re-running overwrites. Real pixel art can replace any PNG by name
later without code changes. No external art assets required.
"""

from PIL import Image, ImageDraw

OUTPUT_DIR = "src/main/resources/assets/seeking_immortals/textures/gui/skill"
SIZE = 16

# Attribute -> (background RGB, symbol RGB)
ATTR_COLOR = {
    "fire": ((178, 52, 26), (255, 200, 80)),
    "water": ((40, 90, 178), (170, 220, 255)),
    "ice": ((110, 170, 210), (240, 250, 255)),
    "wood": ((54, 132, 54), (170, 230, 130)),
    "earth": ((138, 104, 48), (220, 190, 120)),
    "metal": ((150, 150, 160), (240, 240, 245)),
    "thunder": ((120, 80, 170), (255, 230, 90)),
    "divine": ((150, 120, 40), (255, 235, 150)),
    "sword": ((90, 110, 120), (220, 230, 240)),
    "formation": ((110, 70, 130), (210, 170, 240)),
    "escape": ((120, 86, 56), (235, 200, 150)),
    "generic": ((60, 50, 40), (200, 180, 130)),
}


def new_image():
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    return img, ImageDraw.Draw(img)


def fill_bg(draw, color):
    r, g, b = color
    draw.rectangle([0, 0, SIZE - 1, SIZE - 1], fill=(r, g, b, 235))


def sym_dot(draw, color):
    r, g, b = color
    draw.ellipse([5, 5, 10, 10], fill=(r, g, b, 255))


def sym_triangle(draw, color):
    r, g, b = color
    draw.polygon([(8, 3), (13, 12), (3, 12)], fill=(r, g, b, 255))


def sym_up_triangle(draw, color):
    r, g, b = color
    draw.polygon([(8, 2), (14, 13), (2, 13)], fill=(r, g, b, 255))


def sym_lightning(draw, color):
    r, g, b = color
    draw.line([(10, 2), (6, 8), (9, 8), (5, 14)], fill=(r, g, b, 255), width=2)


def sym_vertical(draw, color):
    r, g, b = color
    draw.line([(8, 2), (8, 14)], fill=(r, g, b, 255), width=3)
    draw.line([(5, 4), (8, 2), (11, 4)], fill=(r, g, b, 255), width=2)


def sym_square_dots(draw, color):
    r, g, b = color
    draw.rectangle([3, 3, 12, 12], outline=(r, g, b, 255), width=1)
    draw.point((8, 8), fill=(r, g, b, 255))


def sym_arrow(draw, color):
    r, g, b = color
    draw.line([(2, 8), (13, 8)], fill=(r, g, b, 255), width=2)
    draw.polygon([(13, 8), (9, 5), (9, 11)], fill=(r, g, b, 255))


def sym_rings(draw, color):
    r, g, b = color
    draw.ellipse([2, 2, 13, 13], outline=(r, g, b, 255), width=1)
    draw.ellipse([5, 5, 10, 10], outline=(r, g, b, 255), width=1)


def sym_ring(draw, color):
    r, g, b = color
    draw.ellipse([3, 3, 12, 12], outline=(r, g, b, 255), width=2)


def sym_diamond(draw, color):
    r, g, b = color
    draw.polygon([(8, 2), (14, 8), (8, 14), (2, 8)], fill=(r, g, b, 255))


# techniqueId -> (attribute key, symbol fn)
SKILLS = {
    "qi_guiding_art": ("divine", sym_diamond),
    "fireball_art": ("fire", sym_dot),
    "ice_cone_art": ("ice", sym_triangle),
    "thunder_strike_art": ("thunder", sym_lightning),
    "earth_escape_step": ("escape", sym_arrow),
    "aura_detection_art": ("divine", sym_rings),
    "flying_sword_beginner": ("sword", sym_up_triangle),
    "single_sword_thrust": ("sword", sym_vertical),
    "three_talent_sword_array": ("sword", sym_square_dots),
    "divine_sense_expansion": ("divine", sym_ring),
    "flying_sword_advanced": ("sword", sym_up_triangle),
    "aura_body_shield": ("generic", sym_ring),
    "five_elements_escape_art": ("escape", sym_arrow),
    "big_dipper_sword_array": ("formation", sym_square_dots),
    "formation_sense": ("formation", sym_rings),
}


def main():
    import os
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    for tid, (attr, sym) in SKILLS.items():
        bg, sym_color = ATTR_COLOR.get(attr, ATTR_COLOR["generic"])
        img, draw = new_image()
        fill_bg(draw, bg)
        sym(draw, sym_color)
        path = os.path.join(OUTPUT_DIR, tid + ".png")
        img.save(path)
        print("wrote", path)
    print("done:", len(SKILLS), "icons")


if __name__ == "__main__":
    main()
