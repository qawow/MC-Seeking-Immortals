package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Native Minecraft client UI skin for Seeking Immortals screens and overlays.
 *
 * <p>Visual language: 竹简青衿 — mountain-village bamboo-slip notebook for outer-sect
 * disciples. Green bamboo frames, bamboo paper body text, ink-grey secondary,
 * cinnabar seals for primary actions. No antique gold rims, no neon cyan fills.
 * Only vanilla/Forge client primitives ({@link GuiGraphics#fill}, blit, text)
 * are used; no third-party UI framework.</p>
 */
public final class ImmortalUiSkin {
    // Legacy surface aliases — aligned to bamboo-slip journal so leftover
    // drawPanel / skill-slot / health / tooltip paths do not reintroduce gold.
    public static final int PANEL_BORDER = 0xCC6B8F5A;
    public static final int PANEL = 0xD612160F;
    public static final int PANEL_INNER = 0xCC222A1E;
    public static final int PANEL_INNER_BORDER = 0x665A7348;
    public static final int SKILL_EMPTY = 0x22000000;
    public static final int SKILL_EMPTY_BORDER = 0x886B8F5A;
    public static final int SKILL_FILLED = 0xAA111111;
    public static final int SKILL_FILLED_BORDER = 0xFF6B8F5A;
    public static final int STATUS_BAR_BACKING = 0x9912160F;
    public static final int STATUS_BAR_BORDER = 0x996B8F5A;
    public static final int STATUS_BAR_FILL = 0xCC7A9E6A;
    public static final int HEALTH_BAR_FILL = 0xD8B7332B;
    public static final int HEALTH_BAR_HIGHLIGHT = 0xAAE0715F;
    public static final int ABSORPTION_BAR_FILL = 0xDDC4A86A;
    public static final int TOOLTIP_PANEL = 0xEE10140E;
    public static final int TOOLTIP_BORDER = 0xDD6B8F5A;

    // HUD text colors still used by TechniqueSkillBarOverlay / BreathingHudOverlay.
    public static final int COLOR_TEXT_MUTED = 0xFFA8A890;
    public static final int COLOR_TEXT_NORMAL = 0xFFE6E0C8;

    // 竹简青衿 — bamboo-slip journal palette (green bamboo frame, no gold).
    public static final int JOURNAL_TRANSPARENT = 0x00000000;
    public static final int JOURNAL_SHADOW = 0x99000000;
    /** Primary bamboo-green outer rim. */
    public static final int JOURNAL_BORDER = 0xFF6B8F5A;
    /** Dim bamboo inner edge for dual-tone rims. */
    public static final int JOURNAL_BORDER_DIM = 0x885A7348;
    /** Deep bamboo-shade void under the panel body. */
    public static final int JOURNAL_VOID = 0xF012160F;
    /** Panel body — bamboo shade. */
    public static final int JOURNAL_PANEL = 0xF2181E16;
    /** Inner bamboo-paper well fill. */
    public static final int JOURNAL_INNER = 0xE6222A1E;
    /** Title strip — darker bamboo shade. */
    public static final int JOURNAL_HEADER = 0xF010140E;
    public static final int JOURNAL_ROW = 0x33202818;
    public static final int JOURNAL_ROW_HOVERED = 0x77384828;
    public static final int JOURNAL_ROW_SELECTED = 0xAA2E3C28;
    public static final int JOURNAL_ROW_DISABLED = 0x4410140C;
    public static final int JOURNAL_CONTROL = 0xF01A2218;
    public static final int JOURNAL_CONTROL_HOVERED = 0xF0283424;
    public static final int JOURNAL_CONTROL_DISABLED = 0xDD10140E;
    public static final int JOURNAL_TAB_SELECTED = 0xF032402C;
    /** Bamboo-green accent — selected underline / scrollbar (not cold jade neon). */
    public static final int JOURNAL_JADE = 0xFF7A9E6A;
    public static final int JOURNAL_JADE_TEXT = 0xFFB8D0A8;
    /** Bamboo paper body text. */
    public static final int JOURNAL_PAPER = 0xFFE6E0C8;
    public static final int JOURNAL_PAPER_MUTED = 0xFFA8A890;
    /** Misty spirit blue-gray (not ice cyan). */
    public static final int JOURNAL_SPIRIT = 0xFF8AA8A0;
    public static final int JOURNAL_CINNABAR = 0xFF8E3A32;
    public static final int JOURNAL_CINNABAR_BRIGHT = 0xFFD97A62;
    public static final int JOURNAL_WARNING = 0xFFC4A86A;
    public static final int JOURNAL_BAR_BACKING = 0xEE0E120C;
    public static final int JOURNAL_BAR_HIGHLIGHT = 0x3390B070;
    public static final int JOURNAL_ICON_INSET = 0xFF4A2A22;
    public static final int JOURNAL_SEAL_INSET = 0xFF4E2A22;
    public static final int JOURNAL_NODE_EMPTY = 0xFF343C30;
    public static final int JOURNAL_NODE_LOCKED = 0xFF4A5240;
    public static final int JOURNAL_DIVIDER_GLOW = 0x556B8F5A;
    public static final int JOURNAL_SCROLLBAR_TRACK = 0x9910140C;
    /** Cultivation fill — bamboo green, no neon cyan. */
    public static final int JOURNAL_CULTIVATION_FILL = 0x887A9E6A;
    public static final int JOURNAL_CULTIVATION_HIGHLIGHT = 0xAAB8D0A8;
    /** 1px paper sheen — keep alpha ≤ 0x22. */
    public static final int JOURNAL_PAPER_SHEEN = 0x22E6E0C8;
    /** Soft bottom weight on journal panels. */
    public static final int JOURNAL_PAPER_WEIGHT = 0x4412160F;
    /** Dual-rim tender-bamboo hairline. */
    public static final int JOURNAL_RIM_INNER = 0x6690B070;
    /** Soft multi-layer HUD drop shadow (status strip). */
    public static final int HUD_SHADOW_SOFT = 0x44000000;
    public static final int HUD_SLOT_FILLED_SOLID = 0xCC1A2218;
    public static final int HUD_SLOT_EMPTY_SOLID = 0x6610140E;

    // Compact HUD surfaces share bamboo materials with less weight.
    public static final int HUD_SHADOW = 0x66000000;
    public static final int HUD_BORDER = 0xCC6B8F5A;
    public static final int HUD_BACKING = 0xE812160F;
    public static final int HUD_INNER = 0xE8181E16;
    public static final int HUD_EDGE = 0x885A7348;
    public static final int HUD_SKILL_DISABLED_OVERLAY = 0x6610140C;
    public static final int HUD_COOLDOWN_OVERLAY = 0xCC4E1712;
    /** Low-alpha channel for skill placeholder cards (combat-readable, not solid). */
    public static final int HUD_SKILL_PLACEHOLDER_ALPHA = 0xAA000000;

    // Skill-rail-only translucent tokens. Do not reuse for journal/status chrome.
    // Alpha intentionally unchanged from prior climate for combat readability.
    public static final int HUD_SKILL_SHADOW = 0x44000000;
    public static final int HUD_SKILL_BORDER = 0x996B8F5A;
    public static final int HUD_SKILL_BACKING = 0x6612160F;
    public static final int HUD_SKILL_INNER = 0x55181E16;
    public static final int HUD_SKILL_SLOT_FILLED = 0x991A2218;
    public static final int HUD_SKILL_SLOT_EMPTY = 0x4410140E;

    // Layout spacing tokens
    public static final int LINE_HEIGHT = 11;
    public static final int SECTION_GAP = 5;

    public enum InteractionState {
        NORMAL,
        HOVERED,
        SELECTED,
        DISABLED
    }

    public enum StatusBarStyle {
        CULTIVATION,
        SPIRIT,
        HEALTH,
        WARNING,
        DANGER,
        NEUTRAL
    }

    private static final ResourceLocation PAPER_TEXTURE =
            new ResourceLocation(SeekingImmortalsMod.MODID, "textures/gui/paper_texture.png");
    private static final ResourceLocation JADE_TEXTURE =
            new ResourceLocation(SeekingImmortalsMod.MODID, "textures/gui/jade_texture.png");
    private static final ResourceLocation BAMBOO_TEXTURE =
            new ResourceLocation(SeekingImmortalsMod.MODID, "textures/gui/bamboo_grain_texture.png");
    private static final int MATERIAL_TILE = 32;

    /** techniqueIds that have a generated PNG under textures/gui/skill/<id>.png. */
    private static final java.util.Set<String> KNOWN_SKILL_ICONS = java.util.Set.of(
            "qi_guiding_art", "fireball_art", "ice_cone_art", "thunder_strike_art",
            "earth_escape_step", "aura_detection_art", "flying_sword_beginner",
            "single_sword_thrust", "three_talent_sword_array", "divine_sense_expansion",
            "flying_sword_advanced", "aura_body_shield", "five_elements_escape_art",
            "big_dipper_sword_array", "formation_sense");
    private static final Map<String, ResourceLocation> SKILL_ICON_CACHE = new ConcurrentHashMap<>();

    private ImmortalUiSkin() {}

    public static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        drawBox(graphics, x, y, width, height, PANEL, PANEL_BORDER);
        drawBox(graphics, x + 2, y + 2, width - 4, height - 4, PANEL_INNER, PANEL_INNER_BORDER);
    }

    /** Draws the full bamboo-slip, bamboo-green and paper surface used by journal screens. */
    public static void drawLayeredPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;

        // Soft dual shadow for a desk-bound bamboo slip float.
        graphics.fill(x + 3, y + 3, x + width + 3, y + height + 3, HUD_SHADOW_SOFT);
        graphics.fill(x + 2, y + 2, x + width + 2, y + height + 2, JOURNAL_SHADOW);
        // Dual-tone bamboo rim: bright outer bamboo + dim inner edge.
        graphics.fill(x, y, x + width, y + height, JOURNAL_BORDER);
        if (width > 2 && height > 2) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, JOURNAL_VOID);
        }
        if (width > 4 && height > 4) {
            graphics.fill(x + 2, y + 2, x + width - 2, y + 3, JOURNAL_RIM_INNER);
            graphics.fill(x + 2, y + height - 3, x + width - 2, y + height - 2, JOURNAL_BORDER_DIM);
            graphics.fill(x + 2, y + 2, x + 3, y + height - 2, JOURNAL_RIM_INNER);
            graphics.fill(x + width - 3, y + 2, x + width - 2, y + height - 2, JOURNAL_BORDER_DIM);
        }
        if (width > 8 && height > 8) {
            int ix = x + 4;
            int iy = y + 4;
            int iw = width - 8;
            int ih = height - 8;
            graphics.fill(ix, iy, ix + iw, iy + ih, JOURNAL_PANEL);
            // Bamboo paper grain — low-alpha tiled texture, never neon glow.
            drawTiledTexture(graphics, PAPER_TEXTURE, ix, iy, iw, ih);
            // Extremely quiet bamboo node grain over large panels only.
            if (iw >= 48 && ih >= 32) {
                drawTiledTexture(graphics, BAMBOO_TEXTURE, ix, iy, iw, ih);
            }
            if (ih >= 3) {
                graphics.fill(ix, iy, ix + iw, iy + 1, JOURNAL_PAPER_SHEEN);
                graphics.fill(ix, iy + ih - 1, ix + iw, iy + ih, JOURNAL_PAPER_WEIGHT);
            }
        }

        if (width >= 24 && height >= 18) {
            int mark = Math.min(32, Math.max(6, width / 10));
            drawCornerMark(graphics, x + 5, y + 5, mark, 1, 1);
            drawCornerMark(graphics, x + width - 5, y + 5, mark, -1, 1);
            drawCornerMark(graphics, x + 5, y + height - 5, mark, 1, -1);
            drawCornerMark(graphics, x + width - 5, y + height - 5, mark, -1, -1);
            // Bamboo node ticks near mid-sides for large slips.
            if (width >= 80 && height >= 48) {
                drawBambooNodeMark(graphics, x + 3, y + height / 2, true);
                drawBambooNodeMark(graphics, x + width - 4, y + height / 2, true);
            }
        }
        // Large journals get a quiet cinnabar seal in the bottom-right.
        if (width >= 120 && height >= 80) {
            int seal = Math.min(10, Math.max(5, Math.min(width, height) / 24));
            drawCinnabarSeal(graphics, x + width - seal - 8, y + height - seal - 8, seal);
        }
    }

    /** Draws a restrained inner content frame without adding another floating panel. */
    public static void drawInnerFrame(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        // Dual-tone inset bamboo rather than a second heavy panel.
        graphics.fill(x, y, x + width, y + height, JOURNAL_BORDER_DIM);
        if (width > 2 && height > 2) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, JOURNAL_INNER);
        }
        if (width > 4 && height > 4) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + 2, JOURNAL_RIM_INNER);
            drawTiledTexture(graphics, PAPER_TEXTURE, x + 1, y + 1, width - 2, height - 2);
        }
    }

    /** Draws a dark title strip with a cinnabar marker and bamboo baseline. */
    public static void drawTitleBar(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        graphics.fill(x, y, x + width, y + height, JOURNAL_HEADER);
        if (width >= 4) {
            graphics.fill(x, y, x + Math.min(3, width), y + height, JOURNAL_CINNABAR);
            if (height >= 4) {
                graphics.fill(x, y, x + 1, Math.min(y + height, y + 6), JOURNAL_CINNABAR_BRIGHT);
            }
        }
        // Dual baseline: dim bamboo + short bright bamboo tick.
        if (height >= 2) {
            graphics.fill(x, y + height - 1, x + width, y + height, JOURNAL_BORDER_DIM);
            if (width >= 16) {
                graphics.fill(x + 4, y + height - 1, x + Math.max(8, width / 5), y + height, JOURNAL_BORDER);
            }
        }
        // Hairline paper sheen under the title — quiet, not magical bloom.
        if (height >= 4 && width > 8) {
            graphics.fill(x + 4, y + 1, x + width - 2, y + 2, JOURNAL_PAPER_SHEEN);
        }
        // Bamboo-node short mark on the right (replaces gold bind tick).
        if (width >= 24 && height >= 6) {
            drawBambooNodeMark(graphics, x + width - 8, y + height / 2, false);
        }
        // Small seal on wide title bars.
        if (width >= 80 && height >= 12) {
            int seal = Math.min(7, Math.max(4, height - 4));
            drawCinnabarSeal(graphics, x + width - seal - 6, y + Math.max(2, (height - seal) / 2), seal);
        }
    }

    public static void drawHorizontalDivider(GuiGraphics graphics, int x, int y, int width) {
        if (width <= 0) return;
        graphics.fill(x, y, x + width, y + 1, JOURNAL_BORDER_DIM);
        if (width >= 12) {
            graphics.fill(x, y, x + Math.max(4, width / 4), y + 1, JOURNAL_JADE);
        }
    }

    public static void drawVerticalDivider(GuiGraphics graphics, int x, int y, int height) {
        if (height <= 0) return;
        graphics.fill(x, y, x + 1, y + height, JOURNAL_BORDER_DIM);
        if (height >= 12) {
            int inset = Math.max(2, height / 8);
            graphics.fill(x + 1, y + inset, x + 2, y + height - inset, JOURNAL_DIVIDER_GLOW);
        }
    }

    /** Draws a tab surface. Text is intentionally left to the owning widget. */
    public static void drawTab(GuiGraphics graphics, int x, int y, int width, int height,
                               InteractionState state) {
        if (width <= 0 || height <= 0) return;
        InteractionState safeState = safeState(state);
        int fill = switch (safeState) {
            case SELECTED -> JOURNAL_TAB_SELECTED;
            case HOVERED -> JOURNAL_CONTROL_HOVERED;
            case DISABLED -> JOURNAL_CONTROL_DISABLED;
            case NORMAL -> JOURNAL_CONTROL;
        };
        int border = safeState == InteractionState.SELECTED ? JOURNAL_JADE : JOURNAL_BORDER_DIM;
        drawControlBox(graphics, x, y, width, height, fill, border);
        if (safeState == InteractionState.SELECTED && width > 6 && height > 2) {
            graphics.fill(x + 3, y + height - 2, x + width - 3, y + height - 1, JOURNAL_JADE);
        } else if (safeState == InteractionState.HOVERED && width > 4 && height > 2) {
            graphics.fill(x + 4, y + height - 2, x + width - 4, y + height - 1, JOURNAL_BORDER);
        }
    }

    /** Draws a list row for normal, hover, selected and disabled states. */
    public static void drawListRow(GuiGraphics graphics, int x, int y, int width, int height,
                                   InteractionState state) {
        if (width <= 0 || height <= 0) return;
        InteractionState safeState = safeState(state);
        int fill = switch (safeState) {
            case HOVERED -> JOURNAL_ROW_HOVERED;
            case SELECTED -> JOURNAL_ROW_SELECTED;
            case DISABLED -> JOURNAL_ROW_DISABLED;
            case NORMAL -> JOURNAL_ROW;
        };
        graphics.fill(x, y, x + width, y + height, fill);
        // Quiet bottom rule so dense lists keep a paper-ledger rhythm.
        if (height >= 3 && safeState != InteractionState.DISABLED) {
            graphics.fill(x + 2, y + height - 1, x + width - 2, y + height, JOURNAL_BORDER_DIM);
        }
        if (safeState == InteractionState.SELECTED && width >= 3) {
            graphics.fill(x, y, x + 2, y + height, JOURNAL_JADE);
            graphics.fill(x + 2, y, x + 3, y + height, JOURNAL_BORDER_DIM);
        } else if (safeState == InteractionState.HOVERED && width >= 1) {
            graphics.fill(x, y, x + 1, y + height, JOURNAL_BORDER);
        }
    }

    /** Draws the shared button surface, including normal, hover, disabled and primary states. */
    public static void drawButtonBackground(GuiGraphics graphics, int x, int y, int width, int height,
                                            InteractionState state, boolean primary) {
        if (width <= 0 || height <= 0) return;
        InteractionState safeState = safeState(state);
        boolean disabled = safeState == InteractionState.DISABLED;
        int fill = disabled ? JOURNAL_CONTROL_DISABLED
                : safeState == InteractionState.HOVERED ? JOURNAL_CONTROL_HOVERED : JOURNAL_CONTROL;
        int border = disabled ? JOURNAL_BORDER_DIM : primary ? JOURNAL_CINNABAR_BRIGHT : JOURNAL_BORDER;
        drawControlBox(graphics, x, y, width, height, fill, border);
        if (primary && !disabled && width > 4 && height > 4) {
            // Cinnabar seal stripe — primary action reads as a stamped order.
            graphics.fill(x + 2, y + 2, x + 4, y + height - 2, JOURNAL_CINNABAR);
            if (height >= 10 && width >= 12) {
                int seal = Math.min(5, height - 4);
                drawCinnabarSeal(graphics, x + width - seal - 3, y + Math.max(2, (height - seal) / 2), seal);
            }
        } else if (!primary && safeState == InteractionState.HOVERED && width > 6 && height > 4) {
            graphics.fill(x + 3, y + height - 3, x + width - 3, y + height - 2, JOURNAL_BORDER);
        } else if (!primary && !disabled && width > 6 && height > 4) {
            graphics.fill(x + 3, y + height - 2, x + width - 3, y + height - 1, JOURNAL_BORDER_DIM);
        }
    }

    /** Convenience overload for custom Button implementations. */
    public static void drawButtonBackground(GuiGraphics graphics, int x, int y, int width, int height,
                                            boolean hovered, boolean enabled, boolean primary) {
        drawButtonBackground(graphics, x, y, width, height,
                enabled ? (hovered ? InteractionState.HOVERED : InteractionState.NORMAL)
                        : InteractionState.DISABLED,
                primary);
    }

    public static int controlTextColor(InteractionState state) {
        return switch (safeState(state)) {
            case HOVERED -> JOURNAL_JADE_TEXT;
            case SELECTED -> JOURNAL_PAPER;
            case DISABLED -> JOURNAL_PAPER_MUTED;
            case NORMAL -> JOURNAL_PAPER;
        };
    }

    public static void drawSkillSlot(GuiGraphics graphics, int x, int y, int size, boolean filled) {
        drawBox(graphics, x, y, size, size, filled ? SKILL_FILLED : SKILL_EMPTY, filled ? SKILL_FILLED_BORDER : SKILL_EMPTY_BORDER);
    }

    public static void drawSkillIconBacking(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        // Bamboo-paper seal card: dim rim + seeded fill + quiet paper grain on larger icons.
        drawBox(graphics, x, y, width, height, color, JOURNAL_BORDER_DIM);
        if (width >= 6 && height >= 6) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + 2, JOURNAL_RIM_INNER);
            graphics.fill(x + 1, y + 1, x + 2, y + height - 1, JOURNAL_CINNABAR);
        }
        if (width >= 10 && height >= 10) {
            drawTiledTexture(graphics, PAPER_TEXTURE, x + 1, y + 1, width - 2, height - 2);
        }
    }

    /** Deterministic bamboo-biased placeholder fill for techniques without a dedicated icon PNG. */
    public static int skillPlaceholderColor(String techniqueId) {
        int colorSeed = Math.abs(techniqueId == null ? 0 : techniqueId.hashCode());
        int r = 0x14 + (colorSeed & 0x18);
        int g = 0x28 + ((colorSeed >> 3) & 0x28);
        int b = 0x14 + ((colorSeed >> 6) & 0x14);
        return HUD_SKILL_PLACEHOLDER_ALPHA | (r << 16) | (g << 8) | b;
    }

    /** True when a generated skill-icon PNG exists for the given techniqueId. */
    public static boolean hasSkillIcon(String techniqueId) {
        return techniqueId != null && KNOWN_SKILL_ICONS.contains(techniqueId);
    }

    /** Blit the 16x16 skill icon for techniqueId into size x size. Caller must guard with hasSkillIcon. */
    public static void drawSkillIcon(GuiGraphics graphics, int x, int y, int size, String techniqueId) {
        if (size <= 0 || techniqueId == null) return;
        ResourceLocation location = SKILL_ICON_CACHE.computeIfAbsent(techniqueId,
                id -> new ResourceLocation(SeekingImmortalsMod.MODID, "textures/gui/skill/" + id + ".png"));
        graphics.blit(location, x, y, 0, 0, size, size, 16, 16);
    }

    public static void drawStatusBar(GuiGraphics graphics, int x, int y, int width, int height, double fraction) {
        if (width <= 0 || height <= 0) return;
        drawBox(graphics, x, y, width, height, STATUS_BAR_BACKING, STATUS_BAR_BORDER);
        int innerWidth = Math.max(0, width - 4);
        int innerHeight = Math.max(0, height - 4);
        int fillWidth = Math.max(0, Math.min(innerWidth,
                (int)Math.round(innerWidth * clamp01(fraction))));
        if (fillWidth > 0 && innerHeight > 0) {
            graphics.fill(x + 2, y + 2, x + 2 + fillWidth, y + height - 2, STATUS_BAR_FILL);
        }
    }

    /** Draws a compact journal bar with a color selected by gameplay meaning. */
    public static void drawSemanticStatusBar(GuiGraphics graphics, int x, int y, int width, int height,
                                             double fraction, StatusBarStyle style) {
        if (width <= 0 || height <= 0) return;
        drawBox(graphics, x, y, width, height, JOURNAL_BAR_BACKING, JOURNAL_BORDER_DIM);
        if (width <= 2 || height <= 2) return;

        int inset = width >= 5 && height >= 5 ? 2 : 1;
        int innerWidth = Math.max(0, width - inset * 2);
        int innerHeight = Math.max(0, height - inset * 2);
        int fillWidth = Math.max(0, Math.min(innerWidth,
                (int)Math.round(innerWidth * clamp01(fraction))));
        if (fillWidth <= 0 || innerHeight <= 0) return;

        int fillColor = statusBarColor(style);
        graphics.fill(x + inset, y + inset, x + inset + fillWidth, y + inset + innerHeight, fillColor);
        if (innerHeight >= 2) {
            graphics.fill(x + inset, y + inset, x + inset + fillWidth,
                    y + inset + Math.max(1, innerHeight / 2), JOURNAL_BAR_HIGHLIGHT);
        }
    }

    public static int statusBarColor(StatusBarStyle style) {
        StatusBarStyle safeStyle = style == null ? StatusBarStyle.NEUTRAL : style;
        return switch (safeStyle) {
            case CULTIVATION -> JOURNAL_JADE;
            case SPIRIT -> JOURNAL_SPIRIT;
            case HEALTH -> HEALTH_BAR_FILL;
            case WARNING -> JOURNAL_WARNING;
            case DANGER -> JOURNAL_CINNABAR_BRIGHT;
            case NEUTRAL -> JOURNAL_BORDER;
        };
    }

    public static void drawHealthBar(GuiGraphics graphics, int x, int y, int width, int height, double healthFraction, double absorptionFraction) {
        if (width <= 0 || height <= 0) return;
        drawBox(graphics, x, y, width, height, STATUS_BAR_BACKING, STATUS_BAR_BORDER);
        int innerWidth = Math.max(0, width - 4);
        int innerHeight = Math.max(0, height - 4);
        int fillWidth = Math.max(0, Math.min(innerWidth, (int)Math.round(innerWidth * clamp01(healthFraction))));
        if (fillWidth > 0 && innerHeight > 0) {
            graphics.fill(x + 2, y + 2, x + 2 + fillWidth, y + 2 + innerHeight, HEALTH_BAR_FILL);
            int highlightHeight = Math.max(1, innerHeight / 2);
            graphics.fill(x + 2, y + 2, x + 2 + fillWidth, y + 2 + highlightHeight, HEALTH_BAR_HIGHLIGHT);
        }
        int absorptionWidth = Math.max(0, Math.min(innerWidth, (int)Math.round(innerWidth * clamp01(absorptionFraction))));
        if (absorptionWidth > 0 && innerHeight > 0) {
            graphics.fill(x + 2, y + height - 4, x + 2 + absorptionWidth, y + height - 2, ABSORPTION_BAR_FILL);
        }
    }

    /**
     * Cultivation progress meter in native bamboo chrome.
     * The old mauve {@code cultivation_progress_bar.png} frame is no longer blitted —
     * fill tokens alone keep the bar on the 竹简 climate when this helper is re-hooked.
     */
    public static void drawCultivationProgressBar(GuiGraphics graphics, int x, int y, int width, int height, double fraction) {
        double clamped = clamp01(fraction);
        if (width <= 0 || height <= 0) return;

        // Dual-tone bamboo frame (no warm legacy texture).
        graphics.fill(x, y, x + width, y + height, JOURNAL_BORDER);
        if (width > 2 && height > 2) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, JOURNAL_BAR_BACKING);
        }
        if (width > 4 && height > 4) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + 2, JOURNAL_RIM_INNER);
            drawTiledTexture(graphics, PAPER_TEXTURE, x + 1, y + 1, width - 2, height - 2);
        }

        int insetX = Math.min(Math.max(0, (width - 1) / 2), Math.max(2, Math.round(width * 0.04F)));
        int insetY = Math.min(Math.max(0, (height - 1) / 2), Math.max(2, Math.round(height * 0.18F)));
        int innerX = x + insetX;
        int innerY = y + insetY;
        int innerWidth = Math.max(0, width - insetX * 2);
        int innerHeight = Math.max(0, height - insetY * 2);
        int fillWidth = Math.max(0, Math.min(innerWidth, (int) Math.round(innerWidth * clamped)));
        if (fillWidth <= 0 || innerHeight <= 0) return;

        graphics.fill(innerX, innerY, innerX + fillWidth, innerY + innerHeight, JOURNAL_CULTIVATION_FILL);
        int highlightHeight = Math.max(1, innerHeight / 2);
        graphics.fill(innerX, innerY, innerX + fillWidth, innerY + highlightHeight, JOURNAL_CULTIVATION_HIGHLIGHT);
        // Quiet bamboo node tick at the leading fill edge on wider meters.
        if (fillWidth >= 8 && innerHeight >= 4) {
            int nodeX = innerX + fillWidth - 2;
            graphics.fill(nodeX, innerY + 1, nodeX + 1, innerY + innerHeight - 1, JOURNAL_JADE);
        }
    }

    public static void drawTooltipPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        // Mini bamboo-slip chrome so tooltips match the journal desk.
        graphics.fill(x + 1, y + 2, x + width + 1, y + height + 2, HUD_SHADOW);
        graphics.fill(x, y, x + width, y + height, JOURNAL_BORDER);
        if (width > 2 && height > 2) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, TOOLTIP_PANEL);
        }
        if (width > 4 && height > 4) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + 2, JOURNAL_RIM_INNER);
            drawTiledTexture(graphics, PAPER_TEXTURE, x + 1, y + 1, width - 2, height - 2);
        }
        if (width >= 14 && height >= 12) {
            int seal = Math.min(6, Math.max(3, Math.min(width, height) / 6));
            drawCinnabarSeal(graphics, x + width - seal - 3, y + 3, seal);
        }
    }

    /** Draws a two-pixel scrollbar and hides it when all content is visible. */
    public static void drawThinScrollbar(GuiGraphics graphics, int x, int y, int height,
                                         int contentHeight, int viewportHeight, int scrollOffset) {
        if (height <= 0 || contentHeight <= 0 || viewportHeight <= 0 || contentHeight <= viewportHeight) {
            return;
        }

        int padding = height >= 8 ? 2 : 0;
        int trackY = y + padding;
        int trackHeight = Math.max(1, height - padding * 2);
        graphics.fill(x, trackY, x + 2, trackY + trackHeight, JOURNAL_SCROLLBAR_TRACK);

        int thumbHeight = Math.max(Math.min(12, trackHeight),
                (int)Math.round(trackHeight * (viewportHeight / (double)contentHeight)));
        thumbHeight = Math.min(trackHeight, thumbHeight);
        int maxScroll = Math.max(1, contentHeight - viewportHeight);
        int clampedOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        int travel = Math.max(0, trackHeight - thumbHeight);
        int thumbY = trackY + (int)Math.round(travel * (clampedOffset / (double)maxScroll));
        graphics.fill(x, thumbY, x + 2, thumbY + thumbHeight, JOURNAL_JADE);
    }

    /** Draws a compact surface for overlays without the heavier journal corner treatment. */
    public static void drawHudPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        graphics.fill(x + 1, y + 2, x + width + 1, y + height + 2, HUD_SHADOW);
        graphics.fill(x, y, x + width, y + height, HUD_BORDER);
        if (width > 2 && height > 2) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, HUD_BACKING);
        }
        if (width > 6 && height > 6) {
            graphics.fill(x + 3, y + 3, x + width - 3, y + height - 3, HUD_INNER);
            // Quiet bamboo paper grain on compact HUD cards.
            if (width >= 48 && height >= 20) {
                drawTiledTexture(graphics, PAPER_TEXTURE, x + 3, y + 3, width - 6, height - 6);
            }
            graphics.fill(x + 2, y + 2, x + width - 2, y + 3, HUD_EDGE);
        }
        if (width >= 10 && height >= 8) {
            graphics.fill(x + width - 4, y + 4, x + width - 3, y + height - 4, JOURNAL_JADE);
        }
        // Tiny bamboo node on wider HUD cards.
        if (width >= 60 && height >= 18) {
            drawBambooNodeMark(graphics, x + width - 8, y + height / 2, false);
        }
    }

    /**
     * Left-top bamboo-slip chrome for the merged 气血/修为/灵力 strip.
     * {@code fullStrip} adds a taller tablet body and a bottom cinnabar seal mark.
     * Stays more solid than the translucent skill rail for combat readability.
     */
    public static void drawStatusStripChrome(GuiGraphics graphics, int x, int y, int width, int height,
                                             boolean fullStrip) {
        if (width <= 0 || height <= 0) return;
        // Soft multi-layer shadow for a deeper bamboo-slip float.
        graphics.fill(x + 2, y + 3, x + width + 2, y + height + 3, HUD_SHADOW_SOFT);
        graphics.fill(x + 1, y + 2, x + width + 1, y + height + 2, HUD_SHADOW);
        graphics.fill(x, y, x + width, y + height, HUD_BORDER);
        if (width > 2 && height > 2) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, HUD_BACKING);
        }
        if (width > 6 && height > 6) {
            graphics.fill(x + 3, y + 3, x + width - 3, y + height - 3, HUD_INNER);
            drawTiledTexture(graphics, JADE_TEXTURE, x + 3, y + 3, width - 6, height - 6);
            // Quiet bamboo grain on the solid status tablet (still low-α via texture).
            if (width >= 40 && height >= 20) {
                drawTiledTexture(graphics, BAMBOO_TEXTURE, x + 3, y + 3, width - 6, height - 6);
            }
            graphics.fill(x + 2, y + 2, x + width - 2, y + 3, HUD_EDGE);
            if (height > 8) {
                graphics.fill(x + 2, y + height - 3, x + width - 2, y + height - 2, HUD_EDGE);
            }
        }
        // Left cinnabar edge — talisman seal strip on the bamboo tablet.
        if (width >= 8 && height >= 8) {
            graphics.fill(x + 2, y + 3, x + 4, y + height - 3, JOURNAL_CINNABAR);
            if (height >= 14) {
                graphics.fill(x + 2, y + 3, x + 3, Math.min(y + height - 3, y + 10), JOURNAL_CINNABAR_BRIGHT);
            }
        }
        // Bamboo-green tick on the right rim.
        if (width >= 12 && height >= 10) {
            graphics.fill(x + width - 4, y + 4, x + width - 3, y + height - 4, JOURNAL_JADE);
        }
        // Mid-side bamboo node marks + corner L-marks on the full strip only.
        if (fullStrip && width >= 28 && height >= 24) {
            int mark = Math.min(10, Math.max(4, width / 16));
            drawCornerMark(graphics, x + 5, y + 5, mark, 1, 1);
            drawCornerMark(graphics, x + width - 5, y + 5, mark, -1, 1);
            drawCornerMark(graphics, x + 5, y + height - 5, mark, 1, -1);
            drawCornerMark(graphics, x + width - 5, y + height - 5, mark, -1, -1);
            if (height >= 36) {
                drawBambooNodeMark(graphics, x + 3, y + height / 2, true);
                drawBambooNodeMark(graphics, x + width - 4, y + height / 2, true);
            }
            int seal = Math.min(8, Math.max(4, Math.min(width, height) / 10));
            drawCinnabarSeal(graphics, x + width - seal - 5, y + height - seal - 4, seal);
        }
    }

    /** Softer meditation bamboo tablet for the bottom breathing HUD. */
    public static void drawBreathingTablet(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        graphics.fill(x + 1, y + 2, x + width + 1, y + height + 2, HUD_SHADOW);
        graphics.fill(x, y, x + width, y + height, HUD_BORDER);
        if (width > 2 && height > 2) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, HUD_BACKING);
        }
        if (width > 6 && height > 6) {
            graphics.fill(x + 3, y + 3, x + width - 3, y + height - 3, HUD_INNER);
            drawTiledTexture(graphics, JADE_TEXTURE, x + 3, y + 3, width - 6, height - 6);
            if (width >= 48) {
                drawTiledTexture(graphics, BAMBOO_TEXTURE, x + 3, y + 3, width - 6, height - 6);
            }
            graphics.fill(x + 2, y + 2, x + width - 2, y + 3, HUD_EDGE);
        }
        // Left cinnabar side bar + right bamboo-green tick.
        if (width >= 10 && height >= 10) {
            graphics.fill(x + 2, y + 3, x + 3, y + height - 3, JOURNAL_CINNABAR);
            graphics.fill(x + width - 4, y + 4, x + width - 3, y + height - 4, JOURNAL_JADE);
        }
        if (width >= 20 && height >= 14) {
            int seal = Math.min(7, Math.max(3, Math.min(width, height) / 8));
            drawCinnabarSeal(graphics, x + width - seal - 5, y + 4, seal);
        }
    }

    /** Thin vertical bamboo-slip rail behind the seven technique slots (solid legacy path). */
    public static void drawJadeSlipRail(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        graphics.fill(x + 1, y + 2, x + width + 1, y + height + 2, HUD_SHADOW);
        graphics.fill(x, y, x + width, y + height, HUD_BORDER);
        if (width > 2 && height > 2) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, HUD_BACKING);
        }
        if (width > 4 && height > 4) {
            graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, HUD_INNER);
            drawTiledTexture(graphics, JADE_TEXTURE, x + 2, y + 2, width - 4, height - 4);
            if (height >= 48) {
                drawTiledTexture(graphics, BAMBOO_TEXTURE, x + 2, y + 2, width - 4, height - 4);
            }
        }
        if (width >= 6 && height >= 10) {
            graphics.fill(x + width - 3, y + 3, x + width - 2, y + height - 3, JOURNAL_JADE);
        }
        if (width >= 8 && height >= 12) {
            graphics.fill(x + 2, y + 3, x + 3, y + height - 3, JOURNAL_CINNABAR);
        }
        // Quiet bamboo node ticks at rail ends (solid path only).
        if (width >= 10 && height >= 40) {
            drawBambooNodeMark(graphics, x + width / 2, y + 5, false);
            drawBambooNodeMark(graphics, x + width / 2, y + height - 5, false);
        }
    }

    /**
     * Semi-transparent bamboo-slip rail for the live left skill bar.
     * Uses skill-only tokens so journal/status solid HUD chrome stays readable.
     * Grain is low-alpha so the rail stays see-through; skill alpha is never raised.
     */
    public static void drawTranslucentJadeSlipRail(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        graphics.fill(x + 1, y + 2, x + width + 1, y + height + 2, HUD_SKILL_SHADOW);
        graphics.fill(x, y, x + width, y + height, HUD_SKILL_BORDER);
        if (width > 2 && height > 2) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, HUD_SKILL_BACKING);
        }
        if (width > 4 && height > 4) {
            graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, HUD_SKILL_INNER);
            drawTiledTexture(graphics, JADE_TEXTURE, x + 2, y + 2, width - 4, height - 4);
        }
        if (width >= 6 && height >= 10) {
            graphics.fill(x + width - 3, y + 3, x + width - 2, y + height - 3, JOURNAL_JADE);
        }
        if (width >= 8 && height >= 12) {
            graphics.fill(x + 2, y + 3, x + 3, y + height - 3, JOURNAL_CINNABAR);
        }
    }

    /**
     * Bamboo-slip skill slot. Prefer this over the legacy
     * {@link #drawSkillSlot} for solid previews; live HUD uses
     * {@link #drawTranslucentJadeSlipSlot}.
     */
    public static void drawJadeSlipSlot(GuiGraphics graphics, int x, int y, int size, boolean filled) {
        if (size <= 0) return;
        int border = filled ? JOURNAL_BORDER : JOURNAL_BORDER_DIM;
        int fill = filled ? HUD_SLOT_FILLED_SOLID : HUD_SLOT_EMPTY_SOLID;
        drawBox(graphics, x, y, size, size, fill, border);
        if (size > 4) {
            drawTiledTexture(graphics, JADE_TEXTURE, x + 1, y + 1, size - 2, size - 2);
        }
        if (size >= 6) {
            graphics.fill(x + 1, y + 1, x + size - 1, y + 2, filled ? JOURNAL_JADE : HUD_EDGE);
        }
        if (filled && size >= 8) {
            graphics.fill(x + 1, y + 1, x + 2, y + size - 1, JOURNAL_CINNABAR);
        }
        // Tiny bottom bamboo node on larger filled slots.
        if (filled && size >= 16) {
            graphics.fill(x + size / 2 - 2, y + size - 3, x + size / 2 + 2, y + size - 2, JOURNAL_BORDER);
            graphics.fill(x + size / 2 - 1, y + size - 4, x + size / 2 + 1, y + size - 3, JOURNAL_RIM_INNER);
        }
    }

    /** Semi-transparent skill slot for the live left technique rail. */
    public static void drawTranslucentJadeSlipSlot(GuiGraphics graphics, int x, int y, int size, boolean filled) {
        if (size <= 0) return;
        int border = filled ? HUD_SKILL_BORDER : JOURNAL_BORDER_DIM;
        int fill = filled ? HUD_SKILL_SLOT_FILLED : HUD_SKILL_SLOT_EMPTY;
        drawBox(graphics, x, y, size, size, fill, border);
        if (filled && size > 4) {
            // Grain only on filled slots so empty rail stays quieter.
            drawTiledTexture(graphics, JADE_TEXTURE, x + 1, y + 1, size - 2, size - 2);
        }
        if (size >= 6) {
            graphics.fill(x + 1, y + 1, x + size - 1, y + 2, filled ? JOURNAL_JADE : HUD_EDGE);
        }
        if (filled && size >= 8) {
            graphics.fill(x + 1, y + 1, x + 2, y + size - 1, JOURNAL_CINNABAR);
        }
    }

    /** Thin bamboo + jade divider used between health and cultivation bands. */
    public static void drawHudDivider(GuiGraphics graphics, int x, int y, int width) {
        if (width <= 0) return;
        graphics.fill(x, y, x + width, y + 1, JOURNAL_BORDER_DIM);
        if (width >= 10) {
            graphics.fill(x, y, x + Math.max(3, width / 5), y + 1, JOURNAL_JADE);
        }
    }

    /** Small square cinnabar seal ornament (native fills). */
    public static void drawCinnabarSeal(GuiGraphics graphics, int x, int y, int size) {
        if (size <= 0) return;
        // Outer dim ring + bright face + inset core — reads as a stamped seal.
        graphics.fill(x, y, x + size, y + size, JOURNAL_SEAL_INSET);
        if (size >= 3) {
            graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, JOURNAL_CINNABAR_BRIGHT);
        }
        if (size >= 5) {
            graphics.fill(x + 2, y + 2, x + size - 2, y + size - 2, JOURNAL_CINNABAR);
        }
        if (size >= 7) {
            graphics.fill(x + 3, y + 3, x + size - 3, y + size - 3, JOURNAL_CINNABAR_BRIGHT);
        }
    }

    /**
     * Label + right-aligned value + semantic bar under them.
     * Returns the y coordinate just below the drawn meter row.
     */
    public static int drawMeterRow(Font font, GuiGraphics graphics, int x, int y, int width,
                                   String label, String value, double fraction, StatusBarStyle style) {
        if (font == null || graphics == null || width <= 0) return y;
        int lineHeight = Math.max(1, font.lineHeight);
        int labelColor = switch (style == null ? StatusBarStyle.NEUTRAL : style) {
            case CULTIVATION -> JOURNAL_JADE_TEXT;
            case SPIRIT -> JOURNAL_SPIRIT;
            case HEALTH -> JOURNAL_CINNABAR_BRIGHT;
            case WARNING -> JOURNAL_WARNING;
            case DANGER -> JOURNAL_CINNABAR_BRIGHT;
            case NEUTRAL -> JOURNAL_PAPER;
        };
        String safeLabel = label == null ? "" : label;
        String safeValue = value == null ? "" : value;
        int valueWidth = font.width(safeValue);
        int labelMax = Math.max(1, width - valueWidth - 4);
        drawStringFit(font, graphics, safeLabel, x, y, labelMax, labelColor, false);
        if (valueWidth > 0 && valueWidth <= width) {
            graphics.drawString(font, safeValue, x + width - valueWidth, y, labelColor, false);
        }
        int barY = y + lineHeight + 1;
        int barHeight = 4;
        drawSemanticStatusBar(graphics, x, barY, width, barHeight, fraction, style);
        return barY + barHeight + 2;
    }

    /** Runs a renderer inside a GUI-space scissor and always restores the prior scissor. */
    public static void withScissor(GuiGraphics graphics, int x, int y, int width, int height,
                                   Runnable renderer) {
        if (graphics == null || renderer == null || width <= 0 || height <= 0) return;
        graphics.enableScissor(x, y, x + width, y + height);
        try {
            renderer.run();
        } finally {
            graphics.disableScissor();
        }
    }

    /** Draws as many wrapped lines as fit and returns the next available y coordinate. */
    public static int drawWrappedText(Font font, GuiGraphics graphics, Component value,
                                      int x, int y, int maxWidth, int maxHeight,
                                      int color, boolean dropShadow) {
        if (font == null || graphics == null || value == null || maxWidth <= 0 || maxHeight <= 0) {
            return y;
        }
        int lineHeight = Math.max(1, font.lineHeight);
        int maxLines = maxHeight / lineHeight;
        if (maxLines <= 0) return y;

        List<FormattedCharSequence> lines = font.split(value, maxWidth);
        int drawn = Math.min(maxLines, lines.size());
        for (int i = 0; i < drawn; i++) {
            graphics.drawString(font, lines.get(i), x, y + i * lineHeight, color, dropShadow);
        }
        return y + drawn * lineHeight;
    }

    public static int drawWrappedText(Font font, GuiGraphics graphics, String value,
                                      int x, int y, int maxWidth, int maxHeight,
                                      int color, boolean dropShadow) {
        return drawWrappedText(font, graphics, Component.literal(value == null ? "" : value),
                x, y, maxWidth, maxHeight, color, dropShadow);
    }

    /**
     * Tiles a low-alpha material texture into a rectangle. Safe no-op on bad size;
     * texture bind failures are absorbed by the resource system (missing texture).
     */
    public static void drawTiledTexture(GuiGraphics graphics, ResourceLocation texture,
                                        int x, int y, int width, int height) {
        if (graphics == null || texture == null || width <= 0 || height <= 0) return;
        int tile = MATERIAL_TILE;
        for (int ty = 0; ty < height; ty += tile) {
            int th = Math.min(tile, height - ty);
            for (int tx = 0; tx < width; tx += tile) {
                int tw = Math.min(tile, width - tx);
                graphics.blit(texture, x + tx, y + ty, 0, 0, tw, th, tile, tile);
            }
        }
    }

    /** Optional paper grain fill for custom content regions. */
    public static void drawPaperFill(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        graphics.fill(x, y, x + width, y + height, JOURNAL_INNER);
        drawTiledTexture(graphics, PAPER_TEXTURE, x, y, width, height);
    }

    /** Optional jade grain fill for HUD tablets / seals. */
    public static void drawJadeFill(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        graphics.fill(x, y, x + width, y + height, HUD_INNER);
        drawTiledTexture(graphics, JADE_TEXTURE, x, y, width, height);
    }

    private static void drawBox(GuiGraphics graphics, int x, int y, int width, int height, int fillColor, int borderColor) {
        if (width <= 0 || height <= 0) return;
        graphics.fill(x, y, x + width, y + height, borderColor);
        if (width > 2 && height > 2) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, fillColor);
        }
    }

    private static void drawControlBox(GuiGraphics graphics, int x, int y, int width, int height,
                                       int fillColor, int borderColor) {
        drawBox(graphics, x, y, width, height, fillColor, borderColor);
        // Dual rim: bright outer already drawn; add tender-bamboo hairline + bottom weight.
        if (width > 4 && height > 4) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + 2, JOURNAL_RIM_INNER);
            graphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, JOURNAL_BORDER_DIM);
        }
    }

    private static void drawCornerMark(GuiGraphics graphics, int x, int y, int length,
                                       int xDirection, int yDirection) {
        int xEnd = x + length * xDirection;
        int yEnd = y + length * yDirection;
        // Dual-stroke L: outer bamboo + dim tender-bamboo for carved-slip corners.
        fillNormalized(graphics, x, y, xEnd, y + yDirection, JOURNAL_BORDER);
        fillNormalized(graphics, x, y, x + xDirection, yEnd, JOURNAL_BORDER);
        int insetX = x + xDirection;
        int insetY = y + yDirection;
        int insetEndX = x + (length - 1) * xDirection;
        int insetEndY = y + (length - 1) * yDirection;
        if (length >= 4) {
            fillNormalized(graphics, insetX, insetY, insetEndX, insetY + yDirection, JOURNAL_RIM_INNER);
            fillNormalized(graphics, insetX, insetY, insetX + xDirection, insetEndY, JOURNAL_RIM_INNER);
        }
    }

    /**
     * Short bamboo-node decoration used on panel sides and title bars.
     * {@code vertical} draws a horizontal node tick (for side rims);
     * otherwise draws a short horizontal node for title bars.
     */
    private static void drawBambooNodeMark(GuiGraphics graphics, int x, int y, boolean vertical) {
        if (vertical) {
            graphics.fill(x - 1, y - 1, x + 2, y + 2, JOURNAL_BORDER);
            graphics.fill(x - 2, y, x + 3, y + 1, JOURNAL_RIM_INNER);
        } else {
            graphics.fill(x, y - 1, x + 5, y + 2, JOURNAL_BORDER_DIM);
            graphics.fill(x + 1, y, x + 4, y + 1, JOURNAL_BORDER);
        }
    }

    private static void fillNormalized(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        int minX = Math.min(x1, x2);
        int minY = Math.min(y1, y2);
        int maxX = Math.max(x1, x2);
        int maxY = Math.max(y1, y2);
        if (maxX == minX) maxX++;
        if (maxY == minY) maxY++;
        graphics.fill(minX, minY, maxX, maxY, color);
    }

    private static InteractionState safeState(InteractionState state) {
        return state == null ? InteractionState.NORMAL : state;
    }

    // Concentrated formatting helper methods
    public static String formatPercent(double fraction) {
        return String.format(Locale.ROOT, "%.0f%%", Math.max(0.0D, Math.min(1.0D, fraction)) * 100.0D);
    }

    public static String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    public static String getStatusText(ClientCultivationData.Snapshot data) {
        StringBuilder builder = new StringBuilder();
        if (data.meditating()) builder.append("打坐 ");
        if (data.severeInjury()) builder.append("重伤 ");
        if (data.heartDemonLevel() > 0) builder.append("心魔").append(data.heartDemonLevel()).append("层 ");
        if (data.shatteredCore()) builder.append("碎丹 ");
        if (data.realmFallScars() > 0) builder.append("跌境伤痕").append(data.realmFallScars()).append(" ");
        return builder.isEmpty() ? "正常" : builder.toString().trim();
    }

    // Adaptive string drawer
    public static void drawStringFit(Font font, GuiGraphics graphics, String value, int x, int y, int maxWidth, int color, boolean dropShadow) {
        if (font == null || graphics == null || maxWidth <= 0) return;
        String fitted = fitWidth(font, value, maxWidth);
        if (!fitted.isEmpty()) {
            graphics.drawString(font, fitted, x, y, color, dropShadow);
        }
    }

    public static String fitWidth(Font font, String text, int maxWidth) {
        if (font == null || text == null || text.isEmpty() || maxWidth <= 0) return "";
        if (font.width(text) <= maxWidth) return text;
        String ellipsis = "...";
        int ellipsisWidth = font.width(ellipsis);
        if (ellipsisWidth >= maxWidth) {
            return font.plainSubstrByWidth(ellipsis, maxWidth);
        }
        return font.plainSubstrByWidth(text, maxWidth - ellipsisWidth) + ellipsis;
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
