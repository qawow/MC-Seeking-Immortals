package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Native Minecraft client UI skin for Seeking Immortals screens and overlays.
 *
 * <p>Visual language: 凡人问道录 · 四材分层 — one journal chassis, four materials
 * ({@link UiClimate}): jade slip (cultivation), bamboo slip (errands/lore),
 * warm lacquer (market/sect), cinnabar seal (danger). Screens push a climate;
 * draw helpers and {@code JOURNAL_*} aliases track the active palette on the
 * client render thread. No third-party UI framework; no neon cyan fills.</p>
 */
public final class ImmortalUiSkin {
    // Compatibility aliases — reassigned by {@link #applyPalette} on climate push.
    // Defaults match BAMBOO_SLIP so leftover callers stay on the humble journal.
    public static int PANEL_BORDER = 0xCC6B8F5A;
    public static int PANEL = 0xD612160F;
    public static int PANEL_INNER = 0xCC222A1E;
    public static int PANEL_INNER_BORDER = 0x665A7348;
    public static int SKILL_EMPTY = 0x22000000;
    public static int SKILL_EMPTY_BORDER = 0x886B8F5A;
    public static int SKILL_FILLED = 0xAA111111;
    public static int SKILL_FILLED_BORDER = 0xFF6B8F5A;
    public static int STATUS_BAR_BACKING = 0x9912160F;
    public static int STATUS_BAR_BORDER = 0x996B8F5A;
    public static int STATUS_BAR_FILL = 0xCC7A9E6A;
    public static final int HEALTH_BAR_FILL = 0xD8B7332B;
    public static final int HEALTH_BAR_HIGHLIGHT = 0xAAE0715F;
    public static final int ABSORPTION_BAR_FILL = 0xDDC4A86A;
    public static int TOOLTIP_PANEL = 0xEE10140E;
    public static int TOOLTIP_BORDER = 0xDD6B8F5A;

    public static int COLOR_TEXT_MUTED = 0xFFA8A890;
    public static int COLOR_TEXT_NORMAL = 0xFFE6E0C8;

    public static final int JOURNAL_TRANSPARENT = 0x00000000;
    public static final int JOURNAL_SHADOW = 0x99000000;
    public static int JOURNAL_BORDER = 0xFF6B8F5A;
    public static int JOURNAL_BORDER_DIM = 0x885A7348;
    public static int JOURNAL_VOID = 0xF012160F;
    public static int JOURNAL_PANEL = 0xF2181E16;
    public static int JOURNAL_INNER = 0xE6222A1E;
    public static int JOURNAL_HEADER = 0xF010140E;
    public static int JOURNAL_ROW = 0x33202818;
    public static int JOURNAL_ROW_HOVERED = 0x77384828;
    public static int JOURNAL_ROW_SELECTED = 0xAA2E3C28;
    public static int JOURNAL_ROW_DISABLED = 0x4410140C;
    public static int JOURNAL_CONTROL = 0xF01A2218;
    public static int JOURNAL_CONTROL_HOVERED = 0xF0283424;
    public static int JOURNAL_CONTROL_DISABLED = 0xDD10140E;
    public static int JOURNAL_TAB_SELECTED = 0xF032402C;
    public static int JOURNAL_JADE = 0xFF7A9E6A;
    public static int JOURNAL_JADE_TEXT = 0xFFB8D0A8;
    public static int JOURNAL_PAPER = 0xFFE6E0C8;
    public static int JOURNAL_PAPER_MUTED = 0xFFA8A890;
    public static int JOURNAL_SPIRIT = 0xFF8AA8A0;
    public static int JOURNAL_CINNABAR = 0xFF8E3A32;
    public static int JOURNAL_CINNABAR_BRIGHT = 0xFFD97A62;
    public static int JOURNAL_WARNING = 0xFFC4A86A;
    public static int JOURNAL_BAR_BACKING = 0xEE0E120C;
    public static int JOURNAL_BAR_HIGHLIGHT = 0x3390B070;
    public static int JOURNAL_ICON_INSET = 0xFF4A2A22;
    public static int JOURNAL_SEAL_INSET = 0xFF4E2A22;
    public static int JOURNAL_NODE_EMPTY = 0xFF343C30;
    public static int JOURNAL_NODE_LOCKED = 0xFF4A5240;
    public static int JOURNAL_DIVIDER_GLOW = 0x556B8F5A;
    public static int JOURNAL_SCROLLBAR_TRACK = 0x9910140C;
    public static int JOURNAL_CULTIVATION_FILL = 0x887A9E6A;
    public static int JOURNAL_CULTIVATION_HIGHLIGHT = 0xAAB8D0A8;
    public static int JOURNAL_PAPER_SHEEN = 0x22E6E0C8;
    public static int JOURNAL_PAPER_WEIGHT = 0x4412160F;
    public static int JOURNAL_RIM_INNER = 0x6690B070;
    public static final int HUD_SHADOW_SOFT = 0x44000000;
    public static int HUD_SLOT_FILLED_SOLID = 0xCC1A2218;
    public static int HUD_SLOT_EMPTY_SOLID = 0x6610140E;

    public static final int HUD_SHADOW = 0x66000000;
    public static int HUD_BORDER = 0xCC6B8F5A;
    public static int HUD_BACKING = 0xE812160F;
    public static int HUD_INNER = 0xE8181E16;
    public static int HUD_EDGE = 0x885A7348;
    public static int HUD_SKILL_DISABLED_OVERLAY = 0x6610140C;
    public static final int HUD_COOLDOWN_OVERLAY = 0xCC4E1712;
    public static final int HUD_SKILL_PLACEHOLDER_ALPHA = 0xAA000000;

    public static int HUD_SKILL_SHADOW = 0x44000000;
    public static int HUD_SKILL_BORDER = 0x996B8F5A;
    public static int HUD_SKILL_BACKING = 0x6612160F;
    public static int HUD_SKILL_INNER = 0x55181E16;
    public static int HUD_SKILL_SLOT_FILLED = 0x991A2218;
    public static int HUD_SKILL_SLOT_EMPTY = 0x4410140E;

    public static final int LINE_HEIGHT = 11;
    public static final int SECTION_GAP = 5;

    /** HUD / stats 走火警告阈值（百分数）。 */
    public static final int QI_DEV_WARN_THRESHOLD = 50;
    /** HUD / stats 走火危签阈值（百分数）。 */
    public static final int QI_DEV_DANGER_THRESHOLD = 70;

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

    // 云笈墨卷 paper tiles (scripts/generate_ink_ui_textures.py).
    private static final ResourceLocation PAPER_TEXTURE =
            new ResourceLocation(SeekingImmortalsMod.MODID, "textures/gui/ink/paper_rice.png");
    private static final ResourceLocation JADE_TEXTURE =
            new ResourceLocation(SeekingImmortalsMod.MODID, "textures/gui/ink/paper_cool.png");
    private static final ResourceLocation BAMBOO_TEXTURE =
            new ResourceLocation(SeekingImmortalsMod.MODID, "textures/gui/ink/paper_rice.png");
    private static final ResourceLocation LACQUER_TEXTURE =
            new ResourceLocation(SeekingImmortalsMod.MODID, "textures/gui/ink/paper_aged.png");
    private static final ResourceLocation OMEN_TEXTURE =
            new ResourceLocation(SeekingImmortalsMod.MODID, "textures/gui/ink/paper_dry.png");
    private static final int MATERIAL_TILE = 32;

    private static final java.util.Set<String> KNOWN_SKILL_ICONS = java.util.Set.of(
            "qi_guiding_art", "fireball_art", "ice_cone_art", "thunder_strike_art",
            "earth_escape_step", "aura_detection_art", "flying_sword_beginner",
            "single_sword_thrust", "three_talent_sword_array", "divine_sense_expansion",
            "flying_sword_advanced", "aura_body_shield", "five_elements_escape_art",
            "big_dipper_sword_array", "formation_sense");
    private static final Map<String, ResourceLocation> SKILL_ICON_CACHE = new ConcurrentHashMap<>();

    /** Client-render-thread climate stack. Default bamboo when empty. */
    private static final Deque<UiClimate> CLIMATE_STACK = new ArrayDeque<>();

    static {
        applyPalette(com.xunxian.seekingimmortals.client.ui.InkScene.FIELD_NOTES.palette());
    }

    private ImmortalUiSkin() {}

    public static UiClimate currentClimate() {
        UiClimate top = CLIMATE_STACK.peek();
        return top == null ? UiClimate.BAMBOO_SLIP : top;
    }

    public static UiClimate.Palette palette() {
        return com.xunxian.seekingimmortals.client.ui.InkScene
                .fromClimate(currentClimate()).palette();
    }

    /** Pushes a climate and rebinds {@code JOURNAL_*} / HUD aliases. Pair with {@link #popClimate()}. */
    public static void pushClimate(UiClimate climate) {
        UiClimate next = UiClimate.safe(climate);
        CLIMATE_STACK.push(next);
        applyPalette(com.xunxian.seekingimmortals.client.ui.InkScene
                .fromClimate(next).palette());
    }

    public static void popClimate() {
        if (!CLIMATE_STACK.isEmpty()) {
            CLIMATE_STACK.pop();
        }
        applyPalette(palette());
    }

    /** Runs {@code action} under {@code climate}, always restoring the previous climate. */
    public static void withClimate(UiClimate climate, Runnable action) {
        if (action == null) return;
        pushClimate(climate);
        try {
            action.run();
        } finally {
            popClimate();
        }
    }

    /**
     * 走火风险字色：≥{@link #QI_DEV_DANGER_THRESHOLD} 朱砂危签，
     * ≥{@link #QI_DEV_WARN_THRESHOLD} 琥珀警告，否则 {@code calmColor}。
     */
    public static int qiDevRiskColor(int riskPercent, int calmColor) {
        if (riskPercent >= QI_DEV_DANGER_THRESHOLD) {
            return JOURNAL_CINNABAR_BRIGHT;
        }
        if (riskPercent >= QI_DEV_WARN_THRESHOLD) {
            return JOURNAL_WARNING;
        }
        return calmColor;
    }

    /** 走火风险字色；平静态默认正文纸色。 */
    public static int qiDevRiskColor(int riskPercent) {
        return qiDevRiskColor(riskPercent, JOURNAL_PAPER);
    }

    /** Test-only: climate stack depth (0 means default bamboo with no pushes). */
    static int climateStackDepthForTest() {
        return CLIMATE_STACK.size();
    }

    /** Test-only: clear stack and rebind default bamboo palette. */
    static void forceResetClimateForTest() {
        CLIMATE_STACK.clear();
        applyPalette(com.xunxian.seekingimmortals.client.ui.InkScene.FIELD_NOTES.palette());
    }

    private static void applyPalette(UiClimate.Palette p) {
        JOURNAL_BORDER = p.border();
        JOURNAL_BORDER_DIM = p.borderDim();
        JOURNAL_VOID = p.voidFill();
        JOURNAL_PANEL = p.panel();
        JOURNAL_INNER = p.inner();
        JOURNAL_HEADER = p.header();
        JOURNAL_ROW = p.row();
        JOURNAL_ROW_HOVERED = p.rowHovered();
        JOURNAL_ROW_SELECTED = p.rowSelected();
        JOURNAL_ROW_DISABLED = p.rowDisabled();
        JOURNAL_CONTROL = p.control();
        JOURNAL_CONTROL_HOVERED = p.controlHovered();
        JOURNAL_CONTROL_DISABLED = p.controlDisabled();
        JOURNAL_TAB_SELECTED = p.tabSelected();
        JOURNAL_JADE = p.accent();
        JOURNAL_JADE_TEXT = p.accentText();
        JOURNAL_PAPER = p.paper();
        JOURNAL_PAPER_MUTED = p.paperMuted();
        JOURNAL_SPIRIT = p.spirit();
        JOURNAL_CINNABAR = p.cinnabar();
        JOURNAL_CINNABAR_BRIGHT = p.cinnabarBright();
        JOURNAL_WARNING = p.warning();
        JOURNAL_BAR_BACKING = p.barBacking();
        JOURNAL_BAR_HIGHLIGHT = p.barHighlight();
        JOURNAL_ICON_INSET = p.iconInset();
        JOURNAL_SEAL_INSET = p.sealInset();
        JOURNAL_NODE_EMPTY = p.nodeEmpty();
        JOURNAL_NODE_LOCKED = p.nodeLocked();
        JOURNAL_DIVIDER_GLOW = p.dividerGlow();
        JOURNAL_SCROLLBAR_TRACK = p.scrollbarTrack();
        JOURNAL_CULTIVATION_FILL = p.cultivationFill();
        JOURNAL_CULTIVATION_HIGHLIGHT = p.cultivationHighlight();
        JOURNAL_PAPER_SHEEN = p.paperSheen();
        JOURNAL_PAPER_WEIGHT = p.paperWeight();
        JOURNAL_RIM_INNER = p.rimInner();

        HUD_BORDER = p.hudBorder();
        HUD_BACKING = p.hudBacking();
        HUD_INNER = p.hudInner();
        HUD_EDGE = p.hudEdge();
        HUD_SLOT_FILLED_SOLID = p.hudSlotFilled();
        HUD_SLOT_EMPTY_SOLID = p.hudSlotEmpty();
        HUD_SKILL_BORDER = p.hudSkillBorder();
        HUD_SKILL_BACKING = p.hudSkillBacking();
        HUD_SKILL_INNER = p.hudSkillInner();
        HUD_SKILL_SLOT_FILLED = p.hudSkillSlotFilled();
        HUD_SKILL_SLOT_EMPTY = p.hudSkillSlotEmpty();
        HUD_SKILL_DISABLED_OVERLAY = p.rowDisabled();

        PANEL_BORDER = (PANEL_BORDER & 0xFF000000) | (p.border() & 0x00FFFFFF);
        PANEL = p.panel();
        PANEL_INNER = p.inner();
        PANEL_INNER_BORDER = p.borderDim();
        SKILL_EMPTY_BORDER = p.borderDim();
        SKILL_FILLED_BORDER = p.border();
        STATUS_BAR_BACKING = p.barBacking();
        STATUS_BAR_BORDER = p.borderDim();
        STATUS_BAR_FILL = p.cultivationFill();
        TOOLTIP_PANEL = p.voidFill();
        TOOLTIP_BORDER = p.border();
        COLOR_TEXT_MUTED = p.paperMuted();
        COLOR_TEXT_NORMAL = p.paper();
    }

    private static ResourceLocation grainTexture(UiClimate.Material material) {
        return switch (material) {
            case JADE -> JADE_TEXTURE;
            case LACQUER -> LACQUER_TEXTURE;
            case SEAL -> OMEN_TEXTURE;
            case BAMBOO -> BAMBOO_TEXTURE;
        };
    }

    private static ResourceLocation bodyTexture(UiClimate.Material material) {
        return switch (material) {
            case JADE -> JADE_TEXTURE;
            case LACQUER -> LACQUER_TEXTURE;
            case SEAL -> OMEN_TEXTURE;
            case BAMBOO -> PAPER_TEXTURE;
        };
    }

    public static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        drawLayeredPanel(graphics, x, y, width, height);
    }

    public static void drawLayeredPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        UiClimate.Palette p = palette();
        UiClimate.Material mat = p.material();

        graphics.fill(x + 3, y + 3, x + width + 3, y + height + 3, HUD_SHADOW_SOFT);
        graphics.fill(x + 2, y + 2, x + width + 2, y + height + 2, JOURNAL_SHADOW);
        graphics.fill(x, y, x + width, y + height, p.border());
        if (width > 2 && height > 2) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, p.voidFill());
        }
        if (width > 4 && height > 4) {
            graphics.fill(x + 2, y + 2, x + width - 2, y + 3, p.rimInner());
            graphics.fill(x + 2, y + height - 3, x + width - 2, y + height - 2, p.borderDim());
            graphics.fill(x + 2, y + 2, x + 3, y + height - 2, p.rimInner());
            graphics.fill(x + width - 3, y + 2, x + width - 2, y + height - 2, p.borderDim());
        }
        if (width > 8 && height > 8) {
            int ix = x + 4;
            int iy = y + 4;
            int iw = width - 8;
            int ih = height - 8;
            graphics.fill(ix, iy, ix + iw, iy + ih, p.panel());
            drawTiledTexture(graphics, bodyTexture(mat), ix, iy, iw, ih);
            if (iw >= 48 && ih >= 32) {
                drawTiledTexture(graphics, grainTexture(mat), ix, iy, iw, ih);
            }
            if (ih >= 3) {
                graphics.fill(ix, iy, ix + iw, iy + 1, p.paperSheen());
                graphics.fill(ix, iy + ih - 1, ix + iw, iy + ih, p.paperWeight());
            }
        }

        if (width >= 24 && height >= 18) {
            int mark = Math.min(32, Math.max(6, width / 10));
            drawCornerMark(graphics, x + 5, y + 5, mark, 1, 1);
            drawCornerMark(graphics, x + width - 5, y + 5, mark, -1, 1);
            drawCornerMark(graphics, x + 5, y + height - 5, mark, 1, -1);
            drawCornerMark(graphics, x + width - 5, y + height - 5, mark, -1, -1);
            if (width >= 80 && height >= 48) {
                drawMaterialNodeMark(graphics, x + 3, y + height / 2, true, mat);
                drawMaterialNodeMark(graphics, x + width - 4, y + height / 2, true, mat);
            }
        }
        if (width >= 120 && height >= 80) {
            int seal = Math.min(mat == UiClimate.Material.SEAL ? 12 : 10,
                    Math.max(5, Math.min(width, height) / 24));
            drawCinnabarSeal(graphics, x + width - seal - 8, y + height - seal - 8, seal);
        }
    }

    public static void drawInnerFrame(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        UiClimate.Palette p = palette();
        graphics.fill(x, y, x + width, y + height, p.borderDim());
        if (width > 2 && height > 2) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, p.inner());
        }
        if (width > 4 && height > 4) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + 2, p.rimInner());
            drawTiledTexture(graphics, bodyTexture(p.material()), x + 1, y + 1, width - 2, height - 2);
        }
    }

    public static void drawTitleBar(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        UiClimate.Palette p = palette();
        graphics.fill(x, y, x + width, y + height, p.header());
        if (width >= 4) {
            graphics.fill(x, y, x + Math.min(3, width), y + height, p.cinnabar());
            if (height >= 4) {
                graphics.fill(x, y, x + 1, Math.min(y + height, y + 6), p.cinnabarBright());
            }
        }
        if (height >= 2) {
            graphics.fill(x, y + height - 1, x + width, y + height, p.borderDim());
            if (width >= 16) {
                graphics.fill(x + 4, y + height - 1, x + Math.max(8, width / 5), y + height, p.border());
            }
        }
        if (height >= 4 && width > 8) {
            graphics.fill(x + 4, y + 1, x + width - 2, y + 2, p.paperSheen());
        }
        if (width >= 24 && height >= 6) {
            drawMaterialNodeMark(graphics, x + width - 8, y + height / 2, false, p.material());
        }
        if (width >= 80 && height >= 12) {
            int seal = Math.min(7, Math.max(4, height - 4));
            drawCinnabarSeal(graphics, x + width - seal - 6, y + Math.max(2, (height - seal) / 2), seal);
        }
    }

    public static void drawHorizontalDivider(GuiGraphics graphics, int x, int y, int width) {
        if (width <= 0) return;
        UiClimate.Palette p = palette();
        graphics.fill(x, y, x + width, y + 1, p.borderDim());
        if (width >= 12) {
            graphics.fill(x, y, x + Math.max(4, width / 4), y + 1, p.accent());
        }
    }

    public static void drawVerticalDivider(GuiGraphics graphics, int x, int y, int height) {
        if (height <= 0) return;
        UiClimate.Palette p = palette();
        graphics.fill(x, y, x + 1, y + height, p.borderDim());
        if (height >= 12) {
            int inset = Math.max(2, height / 8);
            graphics.fill(x + 1, y + inset, x + 2, y + height - inset, p.dividerGlow());
        }
    }

    public static void drawTab(GuiGraphics graphics, int x, int y, int width, int height,
                               InteractionState state) {
        if (width <= 0 || height <= 0) return;
        UiClimate.Palette p = palette();
        InteractionState safeState = safeState(state);
        int fill = switch (safeState) {
            case SELECTED -> p.tabSelected();
            case HOVERED -> p.controlHovered();
            case DISABLED -> p.controlDisabled();
            case NORMAL -> p.control();
        };
        int border = safeState == InteractionState.SELECTED ? p.accent() : p.borderDim();
        drawControlBox(graphics, x, y, width, height, fill, border);
        if (safeState == InteractionState.SELECTED && width > 6 && height > 2) {
            graphics.fill(x + 3, y + height - 2, x + width - 3, y + height - 1, p.accent());
        } else if (safeState == InteractionState.HOVERED && width > 4 && height > 2) {
            graphics.fill(x + 4, y + height - 2, x + width - 4, y + height - 1, p.border());
        }
    }

    public static void drawListRow(GuiGraphics graphics, int x, int y, int width, int height,
                                   InteractionState state) {
        if (width <= 0 || height <= 0) return;
        UiClimate.Palette p = palette();
        InteractionState safeState = safeState(state);
        int fill = switch (safeState) {
            case HOVERED -> p.rowHovered();
            case SELECTED -> p.rowSelected();
            case DISABLED -> p.rowDisabled();
            case NORMAL -> p.row();
        };
        graphics.fill(x, y, x + width, y + height, fill);
        if (height >= 3 && safeState != InteractionState.DISABLED) {
            graphics.fill(x + 2, y + height - 1, x + width - 2, y + height, p.borderDim());
        }
        if (safeState == InteractionState.SELECTED && width >= 3) {
            graphics.fill(x, y, x + 2, y + height, p.accent());
            graphics.fill(x + 2, y, x + 3, y + height, p.borderDim());
        } else if (safeState == InteractionState.HOVERED && width >= 1) {
            graphics.fill(x, y, x + 1, y + height, p.border());
        }
    }

    public static void drawButtonBackground(GuiGraphics graphics, int x, int y, int width, int height,
                                            InteractionState state, boolean primary) {
        if (width <= 0 || height <= 0) return;
        UiClimate.Palette p = palette();
        InteractionState safeState = safeState(state);
        boolean disabled = safeState == InteractionState.DISABLED;
        boolean dangerClimate = currentClimate() == UiClimate.CINNABAR_SEAL;
        int fill = disabled ? p.controlDisabled()
                : safeState == InteractionState.HOVERED ? p.controlHovered() : p.control();
        int border = disabled ? p.borderDim()
                : (primary || dangerClimate) ? p.cinnabarBright() : p.border();
        drawControlBox(graphics, x, y, width, height, fill, border);
        if ((primary || dangerClimate) && !disabled && width > 4 && height > 4) {
            graphics.fill(x + 2, y + 2, x + 4, y + height - 2, p.cinnabar());
            if (height >= 10 && width >= 12) {
                int seal = Math.min(5, height - 4);
                drawCinnabarSeal(graphics, x + width - seal - 3, y + Math.max(2, (height - seal) / 2), seal);
            }
        } else if (!primary && safeState == InteractionState.HOVERED && width > 6 && height > 4) {
            graphics.fill(x + 3, y + height - 3, x + width - 3, y + height - 2, p.border());
        } else if (!primary && !disabled && width > 6 && height > 4) {
            graphics.fill(x + 3, y + height - 2, x + width - 3, y + height - 1, p.borderDim());
        }
    }

    public static void drawButtonBackground(GuiGraphics graphics, int x, int y, int width, int height,
                                            boolean hovered, boolean enabled, boolean primary) {
        drawButtonBackground(graphics, x, y, width, height,
                enabled ? (hovered ? InteractionState.HOVERED : InteractionState.NORMAL)
                        : InteractionState.DISABLED,
                primary);
    }

    public static int controlTextColor(InteractionState state) {
        UiClimate.Palette p = palette();
        return switch (safeState(state)) {
            case HOVERED -> p.accentText();
            case SELECTED -> p.paper();
            case DISABLED -> p.paperMuted();
            case NORMAL -> p.paper();
        };
    }

    public static void drawSkillSlot(GuiGraphics graphics, int x, int y, int size, boolean filled) {
        drawJadeSlipSlot(graphics, x, y, size, filled);
    }

    public static void drawSkillIconBacking(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        UiClimate.Palette p = palette();
        drawBox(graphics, x, y, width, height, color, p.borderDim());
        if (width >= 6 && height >= 6) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + 2, p.rimInner());
            graphics.fill(x + 1, y + 1, x + 2, y + height - 1, p.cinnabar());
        }
        if (width >= 10 && height >= 10) {
            drawTiledTexture(graphics, bodyTexture(p.material()), x + 1, y + 1, width - 2, height - 2);
        }
    }

    public static int skillPlaceholderColor(String techniqueId) {
        int colorSeed = Math.abs(techniqueId == null ? 0 : techniqueId.hashCode());
        UiClimate.Material mat = palette().material();
        int r;
        int g;
        int b;
        if (mat == UiClimate.Material.JADE) {
            r = 0x10 + (colorSeed & 0x14);
            g = 0x2C + ((colorSeed >> 3) & 0x28);
            b = 0x20 + ((colorSeed >> 6) & 0x18);
        } else if (mat == UiClimate.Material.LACQUER) {
            r = 0x28 + (colorSeed & 0x20);
            g = 0x1C + ((colorSeed >> 3) & 0x18);
            b = 0x10 + ((colorSeed >> 6) & 0x10);
        } else {
            r = 0x14 + (colorSeed & 0x18);
            g = 0x28 + ((colorSeed >> 3) & 0x28);
            b = 0x14 + ((colorSeed >> 6) & 0x14);
        }
        return HUD_SKILL_PLACEHOLDER_ALPHA | (r << 16) | (g << 8) | b;
    }

    public static boolean hasSkillIcon(String techniqueId) {
        return techniqueId != null && KNOWN_SKILL_ICONS.contains(techniqueId);
    }

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
                (int) Math.round(innerWidth * clamp01(fraction))));
        if (fillWidth > 0 && innerHeight > 0) {
            graphics.fill(x + 2, y + 2, x + 2 + fillWidth, y + height - 2, STATUS_BAR_FILL);
        }
    }

    public static void drawSemanticStatusBar(GuiGraphics graphics, int x, int y, int width, int height,
                                             double fraction, StatusBarStyle style) {
        if (width <= 0 || height <= 0) return;
        UiClimate.Palette p = palette();
        drawBox(graphics, x, y, width, height, p.barBacking(), p.borderDim());
        if (width <= 2 || height <= 2) return;

        int inset = width >= 5 && height >= 5 ? 2 : 1;
        int innerWidth = Math.max(0, width - inset * 2);
        int innerHeight = Math.max(0, height - inset * 2);
        int fillWidth = Math.max(0, Math.min(innerWidth,
                (int) Math.round(innerWidth * clamp01(fraction))));
        if (fillWidth <= 0 || innerHeight <= 0) return;

        int fillColor = statusBarColor(style);
        graphics.fill(x + inset, y + inset, x + inset + fillWidth, y + inset + innerHeight, fillColor);
        if (innerHeight >= 2) {
            graphics.fill(x + inset, y + inset, x + inset + fillWidth,
                    y + inset + Math.max(1, innerHeight / 2), p.barHighlight());
        }
    }

    public static int statusBarColor(StatusBarStyle style) {
        UiClimate.Palette p = palette();
        StatusBarStyle safeStyle = style == null ? StatusBarStyle.NEUTRAL : style;
        return switch (safeStyle) {
            case CULTIVATION -> p.accent();
            case SPIRIT -> p.spirit();
            case HEALTH -> HEALTH_BAR_FILL;
            case WARNING -> p.warning();
            case DANGER -> p.cinnabarBright();
            case NEUTRAL -> p.border();
        };
    }

    public static void drawHealthBar(GuiGraphics graphics, int x, int y, int width, int height,
                                     double healthFraction, double absorptionFraction) {
        if (width <= 0 || height <= 0) return;
        drawBox(graphics, x, y, width, height, STATUS_BAR_BACKING, STATUS_BAR_BORDER);
        int innerWidth = Math.max(0, width - 4);
        int innerHeight = Math.max(0, height - 4);
        int fillWidth = Math.max(0, Math.min(innerWidth, (int) Math.round(innerWidth * clamp01(healthFraction))));
        if (fillWidth > 0 && innerHeight > 0) {
            graphics.fill(x + 2, y + 2, x + 2 + fillWidth, y + 2 + innerHeight, HEALTH_BAR_FILL);
            int highlightHeight = Math.max(1, innerHeight / 2);
            graphics.fill(x + 2, y + 2, x + 2 + fillWidth, y + 2 + highlightHeight, HEALTH_BAR_HIGHLIGHT);
        }
        int absorptionWidth = Math.max(0, Math.min(innerWidth, (int) Math.round(innerWidth * clamp01(absorptionFraction))));
        if (absorptionWidth > 0 && innerHeight > 0) {
            graphics.fill(x + 2, y + height - 4, x + 2 + absorptionWidth, y + height - 2, ABSORPTION_BAR_FILL);
        }
    }

    public static void drawCultivationProgressBar(GuiGraphics graphics, int x, int y, int width, int height,
                                                  double fraction) {
        double clamped = clamp01(fraction);
        if (width <= 0 || height <= 0) return;
        UiClimate.Palette p = palette();

        graphics.fill(x, y, x + width, y + height, p.border());
        if (width > 2 && height > 2) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, p.barBacking());
        }
        if (width > 4 && height > 4) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + 2, p.rimInner());
            drawTiledTexture(graphics, bodyTexture(p.material()), x + 1, y + 1, width - 2, height - 2);
        }

        int insetX = Math.min(Math.max(0, (width - 1) / 2), Math.max(2, Math.round(width * 0.04F)));
        int insetY = Math.min(Math.max(0, (height - 1) / 2), Math.max(2, Math.round(height * 0.18F)));
        int innerX = x + insetX;
        int innerY = y + insetY;
        int innerWidth = Math.max(0, width - insetX * 2);
        int innerHeight = Math.max(0, height - insetY * 2);
        int fillWidth = Math.max(0, Math.min(innerWidth, (int) Math.round(innerWidth * clamped)));
        if (fillWidth <= 0 || innerHeight <= 0) return;

        graphics.fill(innerX, innerY, innerX + fillWidth, innerY + innerHeight, p.cultivationFill());
        int highlightHeight = Math.max(1, innerHeight / 2);
        graphics.fill(innerX, innerY, innerX + fillWidth, innerY + highlightHeight, p.cultivationHighlight());
        if (fillWidth >= 8 && innerHeight >= 4) {
            int nodeX = innerX + fillWidth - 2;
            graphics.fill(nodeX, innerY + 1, nodeX + 1, innerY + innerHeight - 1, p.accent());
        }
    }

    public static void drawTooltipPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        UiClimate.Palette p = palette();
        graphics.fill(x + 1, y + 2, x + width + 1, y + height + 2, HUD_SHADOW);
        graphics.fill(x, y, x + width, y + height, p.border());
        if (width > 2 && height > 2) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, TOOLTIP_PANEL);
        }
        if (width > 4 && height > 4) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + 2, p.rimInner());
            drawTiledTexture(graphics, bodyTexture(p.material()), x + 1, y + 1, width - 2, height - 2);
        }
        if (width >= 14 && height >= 12) {
            int seal = Math.min(6, Math.max(3, Math.min(width, height) / 6));
            drawCinnabarSeal(graphics, x + width - seal - 3, y + 3, seal);
        }
    }

    public static void drawThinScrollbar(GuiGraphics graphics, int x, int y, int height,
                                         int contentHeight, int viewportHeight, int scrollOffset) {
        if (height <= 0 || contentHeight <= 0 || viewportHeight <= 0 || contentHeight <= viewportHeight) {
            return;
        }
        UiClimate.Palette p = palette();
        int padding = height >= 8 ? 2 : 0;
        int trackY = y + padding;
        int trackHeight = Math.max(1, height - padding * 2);
        graphics.fill(x, trackY, x + 2, trackY + trackHeight, p.scrollbarTrack());

        int thumbHeight = Math.max(Math.min(12, trackHeight),
                (int) Math.round(trackHeight * (viewportHeight / (double) contentHeight)));
        thumbHeight = Math.min(trackHeight, thumbHeight);
        int maxScroll = Math.max(1, contentHeight - viewportHeight);
        int clampedOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        int travel = Math.max(0, trackHeight - thumbHeight);
        int thumbY = trackY + (int) Math.round(travel * (clampedOffset / (double) maxScroll));
        graphics.fill(x, thumbY, x + 2, thumbY + thumbHeight, p.accent());
    }

    public static void drawHudPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        UiClimate.Palette p = palette();
        graphics.fill(x + 1, y + 2, x + width + 1, y + height + 2, HUD_SHADOW);
        graphics.fill(x, y, x + width, y + height, p.hudBorder());
        if (width > 2 && height > 2) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, p.hudBacking());
        }
        if (width > 6 && height > 6) {
            graphics.fill(x + 3, y + 3, x + width - 3, y + height - 3, p.hudInner());
            if (width >= 48 && height >= 20) {
                drawTiledTexture(graphics, bodyTexture(p.material()), x + 3, y + 3, width - 6, height - 6);
            }
            graphics.fill(x + 2, y + 2, x + width - 2, y + 3, p.hudEdge());
        }
        if (width >= 10 && height >= 8) {
            graphics.fill(x + width - 4, y + 4, x + width - 3, y + height - 4, p.accent());
        }
        if (width >= 60 && height >= 18) {
            drawMaterialNodeMark(graphics, x + width - 8, y + height / 2, false, p.material());
        }
    }

    public static void drawStatusStripChrome(GuiGraphics graphics, int x, int y, int width, int height,
                                             boolean fullStrip) {
        if (width <= 0 || height <= 0) return;
        UiClimate.Palette p = palette();
        graphics.fill(x + 2, y + 3, x + width + 2, y + height + 3, HUD_SHADOW_SOFT);
        graphics.fill(x + 1, y + 2, x + width + 1, y + height + 2, HUD_SHADOW);
        graphics.fill(x, y, x + width, y + height, p.hudBorder());
        if (width > 2 && height > 2) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, p.hudBacking());
        }
        if (width > 6 && height > 6) {
            graphics.fill(x + 3, y + 3, x + width - 3, y + height - 3, p.hudInner());
            drawTiledTexture(graphics, JADE_TEXTURE, x + 3, y + 3, width - 6, height - 6);
            if (width >= 40 && height >= 20) {
                drawTiledTexture(graphics, grainTexture(p.material()), x + 3, y + 3, width - 6, height - 6);
            }
            graphics.fill(x + 2, y + 2, x + width - 2, y + 3, p.hudEdge());
            if (height > 8) {
                graphics.fill(x + 2, y + height - 3, x + width - 2, y + height - 2, p.hudEdge());
            }
        }
        if (width >= 8 && height >= 8) {
            graphics.fill(x + 2, y + 3, x + 4, y + height - 3, p.cinnabar());
            if (height >= 14) {
                graphics.fill(x + 2, y + 3, x + 3, Math.min(y + height - 3, y + 10), p.cinnabarBright());
            }
        }
        if (width >= 12 && height >= 10) {
            graphics.fill(x + width - 4, y + 4, x + width - 3, y + height - 4, p.accent());
        }
        if (fullStrip && width >= 28 && height >= 24) {
            int mark = Math.min(10, Math.max(4, width / 16));
            drawCornerMark(graphics, x + 5, y + 5, mark, 1, 1);
            drawCornerMark(graphics, x + width - 5, y + 5, mark, -1, 1);
            drawCornerMark(graphics, x + 5, y + height - 5, mark, 1, -1);
            drawCornerMark(graphics, x + width - 5, y + height - 5, mark, -1, -1);
            if (height >= 36) {
                drawMaterialNodeMark(graphics, x + 3, y + height / 2, true, p.material());
                drawMaterialNodeMark(graphics, x + width - 4, y + height / 2, true, p.material());
            }
            int seal = Math.min(8, Math.max(4, Math.min(width, height) / 10));
            drawCinnabarSeal(graphics, x + width - seal - 5, y + height - seal - 4, seal);
        }
    }

    public static void drawBreathingTablet(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        UiClimate.Palette p = palette();
        graphics.fill(x + 1, y + 2, x + width + 1, y + height + 2, HUD_SHADOW);
        graphics.fill(x, y, x + width, y + height, p.hudBorder());
        if (width > 2 && height > 2) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, p.hudBacking());
        }
        if (width > 6 && height > 6) {
            graphics.fill(x + 3, y + 3, x + width - 3, y + height - 3, p.hudInner());
            drawTiledTexture(graphics, JADE_TEXTURE, x + 3, y + 3, width - 6, height - 6);
            if (width >= 48) {
                drawTiledTexture(graphics, grainTexture(p.material()), x + 3, y + 3, width - 6, height - 6);
            }
            graphics.fill(x + 2, y + 2, x + width - 2, y + 3, p.hudEdge());
        }
        if (width >= 10 && height >= 10) {
            graphics.fill(x + 2, y + 3, x + 3, y + height - 3, p.cinnabar());
            graphics.fill(x + width - 4, y + 4, x + width - 3, y + height - 4, p.accent());
        }
        if (width >= 20 && height >= 14) {
            int seal = Math.min(7, Math.max(3, Math.min(width, height) / 8));
            drawCinnabarSeal(graphics, x + width - seal - 5, y + 4, seal);
        }
    }

    public static void drawJadeSlipRail(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        UiClimate.Palette p = palette();
        graphics.fill(x + 1, y + 2, x + width + 1, y + height + 2, HUD_SHADOW);
        graphics.fill(x, y, x + width, y + height, p.hudBorder());
        if (width > 2 && height > 2) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, p.hudBacking());
        }
        if (width > 4 && height > 4) {
            graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, p.hudInner());
            drawTiledTexture(graphics, JADE_TEXTURE, x + 2, y + 2, width - 4, height - 4);
            if (height >= 48) {
                drawTiledTexture(graphics, grainTexture(p.material()), x + 2, y + 2, width - 4, height - 4);
            }
        }
        if (width >= 6 && height >= 10) {
            graphics.fill(x + width - 3, y + 3, x + width - 2, y + height - 3, p.accent());
        }
        if (width >= 8 && height >= 12) {
            graphics.fill(x + 2, y + 3, x + 3, y + height - 3, p.cinnabar());
        }
        if (width >= 10 && height >= 40) {
            drawMaterialNodeMark(graphics, x + width / 2, y + 5, false, p.material());
            drawMaterialNodeMark(graphics, x + width / 2, y + height - 5, false, p.material());
        }
    }

    public static void drawTranslucentJadeSlipRail(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        UiClimate.Palette p = palette();
        graphics.fill(x + 1, y + 2, x + width + 1, y + height + 2, HUD_SKILL_SHADOW);
        graphics.fill(x, y, x + width, y + height, p.hudSkillBorder());
        if (width > 2 && height > 2) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, p.hudSkillBacking());
        }
        if (width > 4 && height > 4) {
            graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, p.hudSkillInner());
            drawTiledTexture(graphics, JADE_TEXTURE, x + 2, y + 2, width - 4, height - 4);
        }
        if (width >= 6 && height >= 10) {
            graphics.fill(x + width - 3, y + 3, x + width - 2, y + height - 3, p.accent());
        }
        if (width >= 8 && height >= 12) {
            graphics.fill(x + 2, y + 3, x + 3, y + height - 3, p.cinnabar());
        }
    }

    public static void drawJadeSlipSlot(GuiGraphics graphics, int x, int y, int size, boolean filled) {
        if (size <= 0) return;
        UiClimate.Palette p = palette();
        int border = filled ? p.border() : p.borderDim();
        int fill = filled ? p.hudSlotFilled() : p.hudSlotEmpty();
        drawBox(graphics, x, y, size, size, fill, border);
        if (size > 4) {
            drawTiledTexture(graphics, JADE_TEXTURE, x + 1, y + 1, size - 2, size - 2);
        }
        if (size >= 6) {
            graphics.fill(x + 1, y + 1, x + size - 1, y + 2, filled ? p.accent() : p.hudEdge());
        }
        if (filled && size >= 8) {
            graphics.fill(x + 1, y + 1, x + 2, y + size - 1, p.cinnabar());
        }
        if (filled && size >= 16) {
            graphics.fill(x + size / 2 - 2, y + size - 3, x + size / 2 + 2, y + size - 2, p.border());
            graphics.fill(x + size / 2 - 1, y + size - 4, x + size / 2 + 1, y + size - 3, p.rimInner());
        }
    }

    public static void drawTranslucentJadeSlipSlot(GuiGraphics graphics, int x, int y, int size, boolean filled) {
        if (size <= 0) return;
        UiClimate.Palette p = palette();
        int border = filled ? p.hudSkillBorder() : p.borderDim();
        int fill = filled ? p.hudSkillSlotFilled() : p.hudSkillSlotEmpty();
        drawBox(graphics, x, y, size, size, fill, border);
        if (filled && size > 4) {
            drawTiledTexture(graphics, JADE_TEXTURE, x + 1, y + 1, size - 2, size - 2);
        }
        if (size >= 6) {
            graphics.fill(x + 1, y + 1, x + size - 1, y + 2, filled ? p.accent() : p.hudEdge());
        }
        if (filled && size >= 8) {
            graphics.fill(x + 1, y + 1, x + 2, y + size - 1, p.cinnabar());
        }
    }

    public static void drawHudDivider(GuiGraphics graphics, int x, int y, int width) {
        if (width <= 0) return;
        UiClimate.Palette p = palette();
        graphics.fill(x, y, x + width, y + 1, p.borderDim());
        if (width >= 10) {
            graphics.fill(x, y, x + Math.max(3, width / 5), y + 1, p.accent());
        }
    }

    public static void drawCinnabarSeal(GuiGraphics graphics, int x, int y, int size) {
        if (size <= 0) return;
        UiClimate.Palette p = palette();
        graphics.fill(x, y, x + size, y + size, p.sealInset());
        if (size >= 3) {
            graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, p.cinnabarBright());
        }
        if (size >= 5) {
            graphics.fill(x + 2, y + 2, x + size - 2, y + size - 2, p.cinnabar());
        }
        if (size >= 7) {
            graphics.fill(x + 3, y + 3, x + size - 3, y + size - 3, p.cinnabarBright());
        }
    }

    public static int drawMeterRow(Font font, GuiGraphics graphics, int x, int y, int width,
                                   String label, String value, double fraction, StatusBarStyle style) {
        if (font == null || graphics == null || width <= 0) return y;
        UiClimate.Palette p = palette();
        int lineHeight = Math.max(1, font.lineHeight);
        int labelColor = switch (style == null ? StatusBarStyle.NEUTRAL : style) {
            case CULTIVATION -> p.accentText();
            case SPIRIT -> p.spirit();
            case HEALTH -> p.cinnabarBright();
            case WARNING -> p.warning();
            case DANGER -> p.cinnabarBright();
            case NEUTRAL -> p.paper();
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

    public static void drawPaperFill(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        UiClimate.Palette p = palette();
        graphics.fill(x, y, x + width, y + height, p.inner());
        drawTiledTexture(graphics, bodyTexture(p.material()), x, y, width, height);
    }

    public static void drawJadeFill(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        UiClimate.Palette p = palette();
        graphics.fill(x, y, x + width, y + height, p.hudInner());
        drawTiledTexture(graphics, JADE_TEXTURE, x, y, width, height);
    }

    private static void drawBox(GuiGraphics graphics, int x, int y, int width, int height,
                                int fillColor, int borderColor) {
        if (width <= 0 || height <= 0) return;
        graphics.fill(x, y, x + width, y + height, borderColor);
        if (width > 2 && height > 2) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, fillColor);
        }
    }

    private static void drawControlBox(GuiGraphics graphics, int x, int y, int width, int height,
                                       int fillColor, int borderColor) {
        UiClimate.Palette p = palette();
        drawBox(graphics, x, y, width, height, fillColor, borderColor);
        if (width > 4 && height > 4) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + 2, p.rimInner());
            graphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, p.borderDim());
        }
    }

    private static void drawCornerMark(GuiGraphics graphics, int x, int y, int length,
                                       int xDirection, int yDirection) {
        UiClimate.Palette p = palette();
        int xEnd = x + length * xDirection;
        int yEnd = y + length * yDirection;
        fillNormalized(graphics, x, y, xEnd, y + yDirection, p.border());
        fillNormalized(graphics, x, y, x + xDirection, yEnd, p.border());
        int insetX = x + xDirection;
        int insetY = y + yDirection;
        int insetEndX = x + (length - 1) * xDirection;
        int insetEndY = y + (length - 1) * yDirection;
        if (length >= 4) {
            fillNormalized(graphics, insetX, insetY, insetEndX, insetY + yDirection, p.rimInner());
            fillNormalized(graphics, insetX, insetY, insetX + xDirection, insetEndY, p.rimInner());
        }
    }

    private static void drawMaterialNodeMark(GuiGraphics graphics, int x, int y, boolean vertical,
                                             UiClimate.Material material) {
        UiClimate.Palette p = palette();
        if (material == UiClimate.Material.LACQUER) {
            // Short bind-tick (warm copper) for lacquer desks.
            if (vertical) {
                graphics.fill(x - 1, y - 2, x + 2, y + 3, p.border());
                graphics.fill(x, y - 1, x + 1, y + 2, p.rimInner());
            } else {
                graphics.fill(x, y - 1, x + 6, y + 2, p.borderDim());
                graphics.fill(x + 1, y, x + 5, y + 1, p.border());
            }
            return;
        }
        if (material == UiClimate.Material.JADE) {
            // Small jade-vein diamond.
            if (vertical) {
                graphics.fill(x, y - 1, x + 1, y + 2, p.accent());
                graphics.fill(x - 1, y, x + 2, y + 1, p.rimInner());
            } else {
                graphics.fill(x + 1, y, x + 4, y + 1, p.accent());
                graphics.fill(x + 2, y - 1, x + 3, y + 2, p.rimInner());
            }
            return;
        }
        // Bamboo / seal: classic node tick.
        if (vertical) {
            graphics.fill(x - 1, y - 1, x + 2, y + 2, p.border());
            graphics.fill(x - 2, y, x + 3, y + 1, p.rimInner());
        } else {
            graphics.fill(x, y - 1, x + 5, y + 2, p.borderDim());
            graphics.fill(x + 1, y, x + 4, y + 1, p.border());
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

    public static String formatPercent(double fraction) {
        return String.format(Locale.ROOT, "%.0f%%", Math.max(0.0D, Math.min(1.0D, fraction)) * 100.0D);
    }

    public static String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    public static String getStatusText(ClientCultivationData.Snapshot data) {
        StringBuilder builder = new StringBuilder();
        if (data.meditating()) builder.append(affliction("meditating")).append(' ');
        if (data.severeInjury()) builder.append(affliction("severe")).append(' ');
        if (data.heartDemonLevel() > 0) builder.append(net.minecraft.network.chat.Component
                .translatable("status.seeking_immortals.affliction.heart_demon_layers",
                        data.heartDemonLevel()).getString()).append(' ');
        if (data.shatteredCore()) builder.append(affliction("shattered_core")).append(' ');
        if (data.realmFallScars() > 0) builder.append(net.minecraft.network.chat.Component
                .translatable("status.seeking_immortals.affliction.fall_scars",
                        data.realmFallScars()).getString()).append(' ');
        return builder.isEmpty() ? affliction("normal") : builder.toString().trim();
    }

    private static String affliction(String suffix) {
        return net.minecraft.network.chat.Component
                .translatable("status.seeking_immortals.affliction." + suffix).getString();
    }

    public static void drawStringFit(Font font, GuiGraphics graphics, String value, int x, int y,
                                     int maxWidth, int color, boolean dropShadow) {
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
