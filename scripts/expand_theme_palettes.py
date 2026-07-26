#!/usr/bin/env python3
"""Expand the five prototype UI themes into 46-token InkScene palettes.

Encodes the expand_palette rules from project_docs/ui_prototypes/02_bronze_tripod.md
plus each scheme's documented overrides, and prints Java `new UiClimate.Palette(...)`
literals for pasting into client/ui/UiTheme.java.

Token order (must match UiClimate.Palette record):
border, borderDim, voidFill, panel, inner, header, row, rowHovered, rowSelected,
rowDisabled, control, controlHovered, controlDisabled, tabSelected, accent,
accentText, paper, paperMuted, spirit, cinnabar, cinnabarBright, warning,
barBacking, barHighlight, iconInset, sealInset, nodeEmpty, nodeLocked,
dividerGlow, scrollbarTrack, cultivationFill, cultivationHighlight, paperSheen,
paperWeight, rimInner, hudBorder, hudBacking, hudInner, hudEdge, hudSlotFilled,
hudSlotEmpty, hudSkillBorder, hudSkillBacking, hudSkillInner, hudSkillSlotFilled,
hudSkillSlotEmpty, material
"""


def hx(s):
    s = s.lstrip("#")
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16))


def clamp(v):
    return max(0, min(255, int(round(v))))


def dark(rgb, f):
    return tuple(clamp(c * (1 - f)) for c in rgb)


def light(rgb, f):
    return tuple(clamp(c + (255 - c) * f) for c in rgb)


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


def argb(alpha, rgb):
    return (alpha << 24) | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2]


def fmt(v):
    return "0x%08X" % (v & 0xFFFFFFFF)


MIDGRAY = (0x60, 0x60, 0x60)

# Cross-scene semantic colors per theme (docs: schemes keep these constant
# across scenes WITHIN the theme).
THEME_SEMANTICS = {
    # spirit, cinnabar, cinnabarBright, warning, iconInset, sealInset
    "night": ((0x7A, 0xB8, 0xC0), (0xB0, 0x48, 0x38), (0xDC, 0x70, 0x50), (0xC8, 0xA0, 0x50),
              (0x28, 0x38, 0x48), (0x20, 0x30, 0x40)),
    "bronze": ((0x6E, 0x9C, 0x94), (0xB4, 0x40, 0x30), (0xDC, 0x68, 0x48), (0xC8, 0x98, 0x48),
               (0x38, 0x48, 0x40), (0x30, 0x40, 0x38)),
    "cave": ((0x6E, 0x9C, 0x94), (0xB4, 0x44, 0x30), (0xDC, 0x6C, 0x48), (0xC8, 0x98, 0x50),
             (0x48, 0x40, 0x34), (0x40, 0x38, 0x2C)),
    "talisman": ((0x3A, 0x6A, 0x72), (0x9E, 0x32, 0x26), (0xC0, 0x50, 0x38), (0x9A, 0x70, 0x20),
                 (0x7A, 0x46, 0x38), (0x8E, 0x4A, 0x3A)),
    "inkwash": ((0x3A, 0x6A, 0x72), (0x9E, 0x32, 0x26), (0xC0, 0x50, 0x38), (0x9A, 0x70, 0x20),
                (0x7A, 0x46, 0x38), (0x8E, 0x4A, 0x3A)),
    # 凡人修仙传 batch 2
    "vial": ((0x7A, 0xC0, 0x96), (0xB0, 0x44, 0x34), (0xDC, 0x6C, 0x4C), (0xC0, 0xA0, 0x48),
             (0x22, 0x34, 0x28), (0x1A, 0x2C, 0x20)),
    "blood": ((0x6E, 0xA8, 0xA0), (0xB8, 0x38, 0x30), (0xE0, 0x60, 0x48), (0xC8, 0x98, 0x48),
              (0x40, 0x26, 0x2A), (0x38, 0x1E, 0x22)),
    "maple": ((0x3E, 0x6E, 0x58), (0xA0, 0x30, 0x24), (0xC2, 0x52, 0x36), (0x96, 0x6E, 0x1E),
              (0x7C, 0x50, 0x30), (0x8E, 0x56, 0x32)),
    "azure": ((0x38, 0x6E, 0x78), (0x9E, 0x34, 0x28), (0xC0, 0x54, 0x3A), (0x98, 0x72, 0x22),
              (0x3C, 0x5A, 0x5C), (0x34, 0x52, 0x54)),
    "beetle": ((0x7A, 0xA8, 0x88), (0xB2, 0x42, 0x30), (0xDC, 0x6A, 0x46), (0xCC, 0xA0, 0x40),
               (0x36, 0x30, 0x24), (0x2E, 0x28, 0x1E)),
}

# (theme, scene) -> (base/panel, text/paper, accent, material)
SCENES = ["QUIET_STUDY", "FIELD_NOTES", "LEDGER_HALL", "OMEN_RED"]
MATERIALS = ["JADE", "BAMBOO", "LACQUER", "SEAL"]

THEMES = {
    "night": ["#101820 #D8E4EC #6FA8C8", "#12161E #DCE0D8 #9AB8C0",
              "#1A141E #E4D8C8 #C8A050", "#1E1012 #E8D0C8 #C84838"],
    "bronze": ["#16201C #E0E0D0 #5A9078", "#1A1C16 #E2DEC8 #6E9060",
               "#201814 #ECDEC0 #C89848", "#1E1210 #E8D4C8 #B44030"],
    "cave": ["#2A2E2C #D8DCD4 #6A9070", "#302E28 #DCD8CC #A08850",
             "#342C24 #E4D8C4 #C08040", "#38221E #E6D2C6 #C05030"],
    "talisman": ["#EEE6C4 #2A3028 #3A5A78", "#F0E4B8 #302A20 #A83828",
                 "#E8D8A0 #342C1C #B07828", "#ECDCB0 #362418 #C03020"],
    "inkwash": ["#E0EAE6 #28322E #4A8868", "#EAE8DA #302E26 #3E6A88",
                "#E8DFC2 #342E20 #A87848", "#E6DED6 #342822 #A83C30"],
    # 凡人修仙传 batch 2 — 掌天瓶/血色禁地/黄枫谷/青元剑光/噬金虫
    "vial": ["#0E1C16 #D4E8DA #56A878", "#101E14 #D8E4D0 #78A860",
             "#16201A #E0DEC4 #B0A050", "#1A1614 #E6D4C8 #C04834"],
    "blood": ["#200E12 #E8D6D2 #A85868", "#1E1210 #E4D8CC #B06848",
              "#241012 #ECD8C0 #C08850", "#280C0C #F0D0C4 #CC4030"],
    "maple": ["#F0E2C0 #322A1E #987830", "#EEDEB4 #342C1C #B05828",
              "#E8D4A4 #362A18 #A87020", "#ECD8B0 #38241A #B83424"],
    "azure": ["#DEEAEC #26323A #3E7488", "#E2ECE4 #2A342C #48887C",
              "#E6E4D0 #322F20 #8A7838", "#E4DEDA #342826 #A04034"],
    "beetle": ["#16141A #E0DCE8 #9098A8", "#181610 #E4E0CC #A89048",
               "#1C160E #ECDEBC #C89838", "#1C1010 #E8D2C8 #C04838"],
}

DARK_THEMES = {"night", "bronze", "cave", "vial", "blood", "beetle"}


def expand(theme, scene_idx, base, text, accent):
    dark_theme = theme in DARK_THEMES
    sem = THEME_SEMANTICS[theme]
    spirit, cinnabar, cinnabar_bright, warning, icon_inset, seal_inset = sem

    border = accent if dark_theme else dark(mix(base, accent, 0.55), 0.35)
    if theme == "inkwash":
        # 裱边: no hard accent border — deep ink-gray line derived from text.
        border = mix(text, accent, 0.25)
    if theme == "talisman":
        border = accent  # 符线 border is the accent itself.
    border_dim = dark(border, 0.30)
    void_fill = dark(base, 0.40)
    if theme == "inkwash":
        void_fill = (0x1A, 0x24, 0x20)  # 裱边深青灰 per doc
    panel = base
    inner = light(base, 0.08) if dark_theme else light(base, 0.30)
    header = dark(base, 0.15) if dark_theme else dark(base, 0.06)
    row_rgb = accent
    row_sel = light(accent, 0.15)
    control = light(base, 0.12) if dark_theme else dark(base, 0.05)
    control_h = light(base, 0.20) if dark_theme else dark(base, 0.10)
    control_d = dark(base, 0.20)
    tab_sel = light(base, 0.25) if dark_theme else light(base, 0.45)
    accent_text = light(accent, 0.35)
    if not dark_theme:
        accent_text = dark(accent, 0.25)  # light paper needs darker accent text
    paper = text
    paper_muted = light(text, 0.35) if dark_theme is False else dark(text, 0.35)
    if not dark_theme:
        paper_muted = mix(text, base, 0.40)
    else:
        paper_muted = dark(text, 0.35)
    bar_backing = dark(base, 0.15) if dark_theme else dark(base, 0.10)
    node_empty = light(base, 0.10) if dark_theme else dark(base, 0.12)
    node_locked = mix(base, text, 0.25)
    scroll_track = dark(base, 0.35)
    cult_fill = accent
    cult_high = light(accent, 0.30)
    paper_weight = dark(base, 0.55)
    rim_inner = light(border, 0.20)

    # Alphas / washes
    row_a, row_h_a, row_s_a = (0x1E, 0x3A, 0x55)
    if theme == "talisman":
        row_a, row_h_a, row_s_a = (0x1F, 0x3D, 0x61)  # 12%/24%/38% of accent
    divider_glow_a = 0x55
    sheen = 0x2AFFFFFF
    if theme in ("night", "bronze", "vial", "blood", "beetle"):
        sheen = 0x1AFFFFFF
    if theme == "cave":
        sheen = 0x20FFFFFF
    if theme == "inkwash":
        sheen = 0x30FFFFFF

    mat = MATERIALS[scene_idx]

    tokens = [
        argb(0xFF, border),
        argb(0x88, border_dim),
        argb(0xC8 if theme == "inkwash" else 0xE8 if dark_theme else 0xC8, void_fill),
        argb(0xF2, panel),
        argb(0xEE, inner),
        argb(0xF0, header),
        argb(row_a, row_rgb),
        argb(row_h_a, row_rgb),
        argb(row_s_a, row_sel),
        argb(0x12 if dark_theme else 0x0F, MIDGRAY if dark_theme else dark(base, 0.45)),
        argb(0xF0, control),
        argb(0xF0, control_h),
        argb(0xDD, control_d),
        argb(0xF0, tab_sel),
        argb(0xFF, accent),
        argb(0xFF, accent_text),
        argb(0xFF, paper),
        argb(0xFF, paper_muted),
        argb(0xFF, spirit),
        argb(0xFF, cinnabar),
        argb(0xFF, cinnabar_bright),
        argb(0xFF, warning),
        argb(0xFF, bar_backing),
        argb(0x33, accent),
        argb(0xFF, icon_inset),
        argb(0xFF, seal_inset),
        argb(0xFF, node_empty),
        argb(0xFF, node_locked),
        argb(divider_glow_a, border),
        argb(0x66, scroll_track),
        argb(0xCC, cult_fill),
        argb(0xEE, cult_high),
        sheen,
        argb(0x44 if dark_theme else 0x38, paper_weight),
        argb(0x66, rim_inner),
        argb(0xC8, border),
        argb(0xD8, panel),
        argb(0xD0, inner),
        argb(0x88, border_dim),
        argb(0xC0, control),
        argb(0x5E, panel),
        argb(0x90, border),
        argb(0x52, panel),
        argb(0x44, inner),
        argb(0x8C, control),
        argb(0x38, panel),
    ]
    return tokens, mat


def main():
    for theme, rows in THEMES.items():
        print("// ===== %s =====" % theme)
        for i, row in enumerate(rows):
            base_s, text_s, accent_s = row.split()
            tokens, mat = expand(theme, i, hx(base_s), hx(text_s), hx(accent_s))
            body = ", ".join(fmt(t) for t in tokens)
            print("%s: new UiClimate.Palette(" % SCENES[i])
            for j in range(0, len(tokens), 6):
                chunk = ", ".join(fmt(t) for t in tokens[j:j + 6])
                print("    %s," % chunk)
            print("    UiClimate.Material.%s)" % mat)
        print()


if __name__ == "__main__":
    main()
