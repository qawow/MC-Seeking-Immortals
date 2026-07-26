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


def hx(s):
    s = s.lstrip("#")
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16))


def starfield(base, star, density=20, seed=0):
    """方案一《玄夜星图》: 深夜幕 + 微光星点（亮星带十字微光）。"""
    rng = random.Random(seed)
    img = Image.new("RGBA", (TILE, TILE), base + (255,))
    d = ImageDraw.Draw(img)
    for _ in range(density):
        x, y = rng.randrange(TILE), rng.randrange(TILE)
        a = rng.choice((28, 40, 56))
        d.point((x, y), fill=star + (a,))
        if a == 56:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                d.point((x + dx, y + dy), fill=star + (18,))
    return img


def bronze(base, patina, gold, seed=0):
    """方案二《青铜鼎彝》: 锈斑 + 错金细屑。"""
    rng = random.Random(seed)
    img = Image.new("RGBA", (TILE, TILE), base + (255,))
    d = ImageDraw.Draw(img)
    for _ in range(40):
        x, y = rng.randrange(TILE), rng.randrange(TILE)
        r = rng.randrange(1, 3)
        d.ellipse([x - r, y - r, x + r, y + r], fill=patina + (rng.choice((20, 30)),))
    for _ in range(6):
        x, y = rng.randrange(TILE), rng.randrange(TILE)
        d.point((x, y), fill=gold + (48,))
    return img


def stone(base, crack, moss, seed=0):
    """方案三《洞府石刻》: 凿痕短线 + 苔斑。"""
    rng = random.Random(seed)
    img = Image.new("RGBA", (TILE, TILE), base + (255,))
    d = ImageDraw.Draw(img)
    for _ in range(14):
        x, y = rng.randrange(TILE), rng.randrange(TILE)
        l = rng.randrange(2, 6)
        d.line([(x, y), (x + l, y + rng.choice((-1, 0, 1)))], fill=crack + (34,))
    for _ in range(5):
        x, y = rng.randrange(TILE), rng.randrange(TILE)
        d.ellipse([x - 1, y - 1, x + 1, y + 1], fill=moss + (26,))
    return img


def talisman_paper(base, fiber, vermilion, seed=0):
    """方案四《符箓黄纸》: 纸纤维 + 极稀朱砂飞白。"""
    rng = random.Random(seed)
    img = Image.new("RGBA", (TILE, TILE), base + (255,))
    d = ImageDraw.Draw(img)
    for _ in range(22):
        y = rng.randrange(TILE)
        x = rng.randrange(TILE)
        l = rng.randrange(4, 12)
        d.line([(x, y), (x + l, y)], fill=fiber + (46,))
    for _ in range(3):
        x, y = rng.randrange(TILE), rng.randrange(TILE)
        d.point((x, y), fill=vermilion + (30,))
    return img


def silk_wash(base, wash, seed=0):
    """方案五《水墨山水》: 绢纹正交细网 + 两三团淡晕。"""
    rng = random.Random(seed)
    img = Image.new("RGBA", (TILE, TILE), base + (255,))
    d = ImageDraw.Draw(img)
    for i in range(0, TILE, 4):
        d.line([(i, 0), (i, TILE - 1)], fill=wash + (10,))
        d.line([(0, i), (TILE - 1, i)], fill=wash + (8,))
    for _ in range(3):
        x, y = rng.randrange(TILE), rng.randrange(TILE)
        r = rng.randrange(4, 9)
        d.ellipse([x - r, y - r, x + r, y + r], fill=wash + (12,))
    return img


def vial_glass(base, glow, seed=0):
    """《掌天瓶露》: 深瓶壁 + 绿液垂滴微光与细流痕。"""
    rng = random.Random(seed)
    img = Image.new("RGBA", (TILE, TILE), base + (255,))
    d = ImageDraw.Draw(img)
    # 稀疏垂直流痕（瓶壁上的液线）。
    for _ in range(6):
        x = rng.randrange(TILE)
        y0 = rng.randrange(TILE)
        l = rng.randrange(4, 10)
        d.line([(x, y0), (x, min(TILE - 1, y0 + l))], fill=glow + (16,))
    # 悬滴微光：亮点 + 下方一点余晖。
    for _ in range(7):
        x, y = rng.randrange(TILE), rng.randrange(TILE)
        a = rng.choice((30, 44, 58))
        d.point((x, y), fill=glow + (a,))
        if a == 58 and y + 1 < TILE:
            d.point((x, y + 1), fill=glow + (22,))
    return img


def blood_mist(base, mist, vein, seed=0):
    """《血色禁地》: 暗赤底 + 血雾团 + 隐约赤脉。"""
    rng = random.Random(seed)
    img = Image.new("RGBA", (TILE, TILE), base + (255,))
    d = ImageDraw.Draw(img)
    for _ in range(5):
        x, y = rng.randrange(TILE), rng.randrange(TILE)
        r = rng.randrange(2, 6)
        d.ellipse([x - r, y - r, x + r, y + r], fill=mist + (18,))
    for _ in range(8):
        x, y = rng.randrange(TILE), rng.randrange(TILE)
        l = rng.randrange(3, 8)
        d.line([(x, y), (x + l, y + rng.choice((-2, -1, 1, 2)))], fill=vein + (24,))
    return img


def maple_paper(base, fiber, leaf, seed=0):
    """《黄枫秋色》: 暖纸纤维 + 斜落枫红碎叶。"""
    rng = random.Random(seed)
    img = Image.new("RGBA", (TILE, TILE), base + (255,))
    d = ImageDraw.Draw(img)
    for _ in range(22):
        y = rng.randrange(TILE)
        x = rng.randrange(TILE)
        l = rng.randrange(4, 12)
        d.line([(x, y), (x + l, y)], fill=fiber + (46,))
    # 斜向短笔触模拟飘落枫叶。
    for _ in range(5):
        x, y = rng.randrange(TILE), rng.randrange(TILE)
        d.line([(x, y), (x + 2, y + 2)], fill=leaf + (34,))
    return img


def sword_silk(base, streak, seed=0):
    """《青元剑光》: 淡青绢底 + 细斜剑光掠痕。"""
    rng = random.Random(seed)
    img = Image.new("RGBA", (TILE, TILE), base + (255,))
    d = ImageDraw.Draw(img)
    for i in range(0, TILE, 4):
        d.line([(i, 0), (i, TILE - 1)], fill=streak + (8,))
        d.line([(0, i), (TILE - 1, i)], fill=streak + (6,))
    # 斜向剑光掠痕：长细线，极淡。
    for _ in range(4):
        x, y = rng.randrange(TILE), rng.randrange(TILE)
        l = rng.randrange(6, 14)
        d.line([(x, y), (x + l, y - l // 2)], fill=streak + (18,))
    return img


def chitin(base, sheen, gold, seed=0):
    """《噬金虫甲》: 漆黑甲壳弧纹 + 噬金碎屑。"""
    rng = random.Random(seed)
    img = Image.new("RGBA", (TILE, TILE), base + (255,))
    d = ImageDraw.Draw(img)
    # 甲壳弧线：浅弧段。
    for _ in range(5):
        x, y = rng.randrange(TILE), rng.randrange(TILE)
        r = rng.randrange(4, 9)
        d.arc([x - r, y - r, x + r, y + r], start=rng.randrange(0, 180),
              end=rng.randrange(180, 360), fill=sheen + (22,))
    # 金屑。
    for _ in range(9):
        x, y = rng.randrange(TILE), rng.randrange(TILE)
        d.point((x, y), fill=gold + (rng.choice((36, 50)),))
    return img


def theme_tiles():
    """十套可选主题 × 四场景（quiet/field/ledger/omen）的 40 张纸/底纹。"""
    tiles = {}
    # 玄夜星图: 夜幕底 + 星光字色作星点。
    night = [("quiet", "#101820", "#D8E4EC"), ("field", "#12161E", "#DCE0D8"),
             ("ledger", "#1A141E", "#E4D8C8"), ("omen", "#1E1012", "#E8D0C8")]
    for i, (scene, base, star) in enumerate(night):
        tiles[f"night_{scene}.png"] = starfield(hx(base), hx(star), seed=101 + i)
    # 青铜鼎彝: 铜底 + 锈斑 + 错金。
    bronze_scenes = [("quiet", "#16201C"), ("field", "#1A1C16"),
                     ("ledger", "#201814"), ("omen", "#1E1210")]
    for i, (scene, base) in enumerate(bronze_scenes):
        tiles[f"bronze_{scene}.png"] = bronze(hx(base), (74, 112, 96), (200, 152, 72), seed=201 + i)
    # 洞府石刻: 石底 + 凿痕 + 苔斑。
    cave = [("quiet", "#2A2E2C"), ("field", "#302E28"),
            ("ledger", "#342C24"), ("omen", "#38221E")]
    for i, (scene, base) in enumerate(cave):
        b = hx(base)
        crack = tuple(max(0, c - 24) for c in b)
        tiles[f"cave_{scene}.png"] = stone(b, crack, (106, 144, 112), seed=301 + i)
    # 符箓黄纸: 黄纸纤维 + 朱砂飞白。
    talisman = [("quiet", "#EEE6C4"), ("field", "#F0E4B8"),
                ("ledger", "#E8D8A0"), ("omen", "#ECDCB0")]
    for i, (scene, base) in enumerate(talisman):
        b = hx(base)
        fiber = tuple(max(0, c - 18) for c in b)
        tiles[f"talisman_{scene}.png"] = talisman_paper(b, fiber, (168, 56, 40), seed=401 + i)
    # 水墨山水: 绢底 + 场景设色淡晕（半装: 只上色板与绢纹）。
    inkwash = [("quiet", "#E0EAE6", "#4A8868"), ("field", "#EAE8DA", "#3E6A88"),
               ("ledger", "#E8DFC2", "#A87848"), ("omen", "#E6DED6", "#A83C30")]
    for i, (scene, base, wash) in enumerate(inkwash):
        tiles[f"inkwash_{scene}.png"] = silk_wash(hx(base), hx(wash), seed=501 + i)
    # 掌天瓶露: 深瓶壁底 + 绿液悬滴/流痕。
    vial = [("quiet", "#0E1C16", "#56A878"), ("field", "#101E14", "#78A860"),
            ("ledger", "#16201A", "#B0A050"), ("omen", "#1A1614", "#C04834")]
    for i, (scene, base, glow) in enumerate(vial):
        tiles[f"vial_{scene}.png"] = vial_glass(hx(base), hx(glow), seed=601 + i)
    # 血色禁地: 暗赤底 + 血雾 + 赤脉。
    blood = [("quiet", "#200E12"), ("field", "#1E1210"),
             ("ledger", "#241012"), ("omen", "#280C0C")]
    for i, (scene, base) in enumerate(blood):
        b = hx(base)
        mist = tuple(min(255, c + 22) for c in b)
        tiles[f"blood_{scene}.png"] = blood_mist(b, mist, (140, 46, 44), seed=701 + i)
    # 黄枫秋色: 暖黄纸 + 枫红碎叶。
    maple = [("quiet", "#F0E2C0"), ("field", "#EEDEB4"),
             ("ledger", "#E8D4A4"), ("omen", "#ECD8B0")]
    for i, (scene, base) in enumerate(maple):
        b = hx(base)
        fiber = tuple(max(0, c - 18) for c in b)
        tiles[f"maple_{scene}.png"] = maple_paper(b, fiber, (176, 74, 40), seed=801 + i)
    # 青元剑光: 淡青绢底 + 斜剑光掠痕。
    azure = [("quiet", "#DEEAEC", "#3E7488"), ("field", "#E2ECE4", "#48887C"),
             ("ledger", "#E6E4D0", "#8A7838"), ("omen", "#E4DEDA", "#A04034")]
    for i, (scene, base, streak) in enumerate(azure):
        tiles[f"azure_{scene}.png"] = sword_silk(hx(base), hx(streak), seed=901 + i)
    # 噬金虫甲: 漆黑甲壳 + 弧纹 + 金屑。
    beetle = [("quiet", "#16141A"), ("field", "#181610"),
              ("ledger", "#1C160E"), ("omen", "#1C1010")]
    for i, (scene, base) in enumerate(beetle):
        b = hx(base)
        sheen = tuple(min(255, c + 30) for c in b)
        tiles[f"beetle_{scene}.png"] = chitin(b, sheen, (200, 152, 56), seed=1001 + i)
    return tiles


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
    tiles.update(theme_tiles())
    for name, img in tiles.items():
        img.save(f"{OUTPUT_DIR}/{name}")
        print("wrote", name)
    seal_grain().save(f"{OUTPUT_DIR}/seal_grain.png")
    print("wrote seal_grain.png")


if __name__ == "__main__":
    main()
