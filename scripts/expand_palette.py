#!/usr/bin/env python3
"""Expand a (paper, ink, accent) triple per scene into the 46-token
UiClimate.Palette constant block for InkScene.java.

Usage:
    python3 scripts/expand_palette.py <prototype>
where <prototype> is one of: starchart bronze stone talisman inkwash

Prints four Java argument blocks (QUIET_STUDY / FIELD_NOTES / LEDGER_HALL /
OMEN_RED) ready to paste into client/ui/InkScene.java. Dark prototypes
(starchart/bronze) invert the paper/ink roles automatically; remember to swap
the InkPaletteTest contrast assertions as documented in each prototype file.
"""

import sys

# scene -> (panel/base RGB, text RGB, accent RGB); dark = panel is dark
PROTOTYPES = {
    "starchart": (True, {
        "QUIET_STUDY": (0x101820, 0xD8E4EC, 0x6FA8C8),
        "FIELD_NOTES": (0x12161E, 0xDCE0D8, 0x9AB8C0),
        "LEDGER_HALL": (0x1A141E, 0xE4D8C8, 0xC8A050),
        "OMEN_RED":    (0x1E1012, 0xE8D0C8, 0xC84838),
    }),
    "bronze": (True, {
        "QUIET_STUDY": (0x16201C, 0xE0E0D0, 0x5A9078),
        "FIELD_NOTES": (0x1A1C16, 0xE2DEC8, 0x6E9060),
        "LEDGER_HALL": (0x201814, 0xECDEC0, 0xC89848),
        "OMEN_RED":    (0x1E1210, 0xE8D4C8, 0xB44030),
    }),
    "stone": (True, {
        "QUIET_STUDY": (0x2A2E2C, 0xD8DCD4, 0x6A9070),
        "FIELD_NOTES": (0x302E28, 0xDCD8CC, 0xA08850),
        "LEDGER_HALL": (0x342C24, 0xE4D8C4, 0xC08040),
        "OMEN_RED":    (0x38221E, 0xE6D2C6, 0xC05030),
    }),
    "talisman": (False, {
        "QUIET_STUDY": (0xEEE6C4, 0x2A3028, 0x3A5A78),
        "FIELD_NOTES": (0xF0E4B8, 0x302A20, 0xA83828),
        "LEDGER_HALL": (0xE8D8A0, 0x342C1C, 0xB07828),
        "OMEN_RED":    (0xECDCB0, 0x362418, 0xC03020),
    }),
    "inkwash": (False, {
        "QUIET_STUDY": (0xE0EAE6, 0x28322E, 0x4A8868),
        "FIELD_NOTES": (0xEAE8DA, 0x302E26, 0x3E6A88),
        "LEDGER_HALL": (0xE8DFC2, 0x342E20, 0xA87848),
        "OMEN_RED":    (0xE6DED6, 0x342822, 0xA83C30),
    }),
}

# Shared semantic colors (constant across scenes, per style guide).
CINNABAR = 0x9E3226
CINNABAR_BRIGHT = 0xC05038
WARNING = 0x9A7020
SPIRIT = 0x3A6A72


def shade(rgb, factor):
    r = max(0, min(255, int(((rgb >> 16) & 0xFF) * factor)))
    g = max(0, min(255, int(((rgb >> 8) & 0xFF) * factor)))
    b = max(0, min(255, int((rgb & 0xFF) * factor)))
    return (r << 16) | (g << 8) | b


def argb(alpha, rgb):
    return f"0x{alpha:02X}{rgb:06X}"


def expand(scene, panel, text, accent, dark, material):
    lighter = 1.15 if dark else 0.96
    darker = 0.80 if dark else 1.06  # "recessed" direction flips with polarity
    void = shade(panel, 0.60 if dark else 0.90)
    tokens = [
        argb(0xFF, accent),                       # border
        argb(0x88, shade(accent, 0.70)),          # borderDim
        argb(0xE8 if dark else 0xC8, void),       # voidFill
        argb(0xF2, panel),                        # panel
        argb(0xEE, shade(panel, lighter)),        # inner
        argb(0xF0, shade(panel, darker)),         # header
        argb(0x1E, accent),                       # row
        argb(0x3A, accent),                       # rowHovered
        argb(0x55, shade(accent, 1.15)),          # rowSelected
        argb(0x12, shade(panel, 1.4 if dark else 0.7)),  # rowDisabled
        argb(0xF0, shade(panel, 1.20 if dark else 0.93)),  # control
        argb(0xF0, shade(panel, 1.32 if dark else 0.88)),  # controlHovered
        argb(0xDD, shade(panel, 0.85 if dark else 1.02)),  # controlDisabled
        argb(0xF0, shade(panel, 1.42 if dark else 0.84)),  # tabSelected
        argb(0xFF, accent),                       # accent
        argb(0xFF, shade(accent, 1.35 if dark else 0.75)),  # accentText
        argb(0xFF, text),                         # paper (=text color!)
        argb(0xFF, shade(text, 0.72 if dark else 1.9)),  # paperMuted
        argb(0xFF, SPIRIT),                       # spirit
        argb(0xFF, CINNABAR),                     # cinnabar
        argb(0xFF, CINNABAR_BRIGHT),              # cinnabarBright
        argb(0xFF, WARNING),                      # warning
        argb(0xFF, shade(panel, darker)),         # barBacking
        argb(0x33, accent),                       # barHighlight
        argb(0xFF, shade(panel, 1.6 if dark else 0.72)),  # iconInset
        argb(0xFF, shade(panel, 1.45 if dark else 0.78)),  # sealInset
        argb(0xFF, shade(panel, 1.5 if dark else 0.80)),  # nodeEmpty
        argb(0xFF, shade(panel, 1.9 if dark else 0.68)),  # nodeLocked
        argb(0x55, accent),                       # dividerGlow
        argb(0x66, shade(panel, 0.7 if dark else 0.86)),  # scrollbarTrack
        argb(0xCC, accent),                       # cultivationFill
        argb(0xEE, shade(accent, 1.3 if dark else 1.25)),  # cultivationHighlight
        argb(0x1A if dark else 0x2A, 0xFFFFFF),   # paperSheen
        argb(0x44, shade(panel, 0.5)),            # paperWeight
        argb(0x66, shade(accent, 1.2)),           # rimInner
        argb(0xC8, accent),                       # hudBorder
        argb(0xD8, panel),                        # hudBacking
        argb(0xD0, shade(panel, lighter)),        # hudInner
        argb(0x88, shade(accent, 0.70)),          # hudEdge
        argb(0xC0, shade(panel, 1.2 if dark else 0.9)),  # hudSlotFilled
        argb(0x5E, panel),                        # hudSlotEmpty
        argb(0x90, accent),                       # hudSkillBorder
        argb(0x52, panel),                        # hudSkillBacking
        argb(0x44, shade(panel, lighter)),        # hudSkillInner
        argb(0x8C, shade(panel, 1.2 if dark else 0.9)),  # hudSkillSlotFilled
        argb(0x38, panel),                        # hudSkillSlotEmpty
    ]
    lines = []
    for i in range(0, len(tokens), 6):
        lines.append("            " + ", ".join(tokens[i:i + 6]) + ",")
    body = "\n".join(lines)
    return f"    {scene}(new UiClimate.Palette(\n{body}\n            UiClimate.Material.{material}\n    ))"


def main():
    if len(sys.argv) != 2 or sys.argv[1] not in PROTOTYPES:
        print(__doc__)
        sys.exit(1)
    dark, scenes = PROTOTYPES[sys.argv[1]]
    materials = {"QUIET_STUDY": "JADE", "FIELD_NOTES": "BAMBOO",
                 "LEDGER_HALL": "LACQUER", "OMEN_RED": "SEAL"}
    blocks = []
    for scene in ("QUIET_STUDY", "FIELD_NOTES", "LEDGER_HALL", "OMEN_RED"):
        panel, text, accent = scenes[scene]
        blocks.append(expand(scene, panel, text, accent, dark, materials[scene]))
    print(",\n".join(blocks) + ";")
    if dark:
        print("\n// DARK prototype: swap InkPaletteTest contrast assertions "
              "(see prototype doc).", file=sys.stderr)


if __name__ == "__main__":
    main()
