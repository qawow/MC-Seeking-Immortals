package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Native Minecraft client UI skin for Seeking Immortals screens and overlays.
 *
 * <p>The UI deliberately uses only vanilla/Forge client rendering primitives
 * such as {@link GuiGraphics#fill}, {@link GuiGraphics#blit} and text drawing.
 * This keeps custom HUDs and screens client-only without any third-party UI
 * framework dependency.</p>
 */
public final class ImmortalUiSkin {
    // Panel styling colors
    public static final int PANEL_BORDER = 0xCCE6D59A;
    public static final int PANEL = 0xD61B1208;
    public static final int PANEL_INNER = 0xCC2A1B0D;
    public static final int PANEL_INNER_BORDER = 0x663B2F18;
    public static final int SKILL_EMPTY = 0x22000000;
    public static final int SKILL_EMPTY_BORDER = 0x88E6D59A;
    public static final int SKILL_FILLED = 0xAA111111;
    public static final int SKILL_FILLED_BORDER = 0xFFE6D59A;
    public static final int STATUS_BAR_BACKING = 0x991B1208;
    public static final int STATUS_BAR_BORDER = 0x99E6D59A;
    public static final int STATUS_BAR_FILL = 0xCC66D17A;
    public static final int HEALTH_BAR_FILL = 0xD8B7332B;
    public static final int HEALTH_BAR_HIGHLIGHT = 0xAAE0715F;
    public static final int ABSORPTION_BAR_FILL = 0xDDE6C46A;
    public static final int TOOLTIP_PANEL = 0xEE130C05;
    public static final int TOOLTIP_BORDER = 0xDDE6D59A;

    // Theme semantic text colors
    public static final int COLOR_TITLE = 0xFFE6D59A;       // 修仙金黄色标题
    public static final int COLOR_TEXT_MUTED = 0xFFBFAF8A;  // 淡褐色辅助/等待文本
    public static final int COLOR_TEXT_NORMAL = 0xFFEFE4C2; // 米黄色普通正文/标签值
    public static final int COLOR_TEXT_SUCCESS = 0xFFB8F5A2;// 淡绿色正面提示/可释放
    public static final int COLOR_TEXT_DANGER = 0xFFFF8A8A; // 淡红色负面/不可释放/警告
    public static final int COLOR_TEXT_BLUE = 0xFF9AD1FF;   // 淡蓝色（用于法力/灵力提示）
    public static final int COLOR_HOVER_BG = 0x332F8F45;    // 悬停选中的浅绿半透明背景

    // Layout layout spacing tokens
    public static final int LINE_HEIGHT = 11;
    public static final int SECTION_GAP = 5;

    private static final ResourceLocation CULTIVATION_PROGRESS_BAR = new ResourceLocation(SeekingImmortalsMod.MODID, "textures/gui/cultivation_progress_bar.png");
    private static final int CULTIVATION_PROGRESS_TEXTURE_WIDTH = 1204;
    private static final int CULTIVATION_PROGRESS_TEXTURE_HEIGHT = 153;

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

    public static void drawSkillSlot(GuiGraphics graphics, int x, int y, int size, boolean filled) {
        drawBox(graphics, x, y, size, size, filled ? SKILL_FILLED : SKILL_EMPTY, filled ? SKILL_FILLED_BORDER : SKILL_EMPTY_BORDER);
    }

    public static void drawSkillIconBacking(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        drawBox(graphics, x, y, width, height, color, PANEL_INNER_BORDER);
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
        drawBox(graphics, x, y, width, height, STATUS_BAR_BACKING, STATUS_BAR_BORDER);
        int fillWidth = Math.max(0, Math.min(width - 4, (int) Math.round((width - 4) * clamp01(fraction))));
        if (fillWidth > 0) {
            graphics.fill(x + 2, y + 2, x + 2 + fillWidth, y + height - 2, STATUS_BAR_FILL);
        }
    }

    public static void drawHealthBar(GuiGraphics graphics, int x, int y, int width, int height, double healthFraction, double absorptionFraction) {
        drawBox(graphics, x, y, width, height, STATUS_BAR_BACKING, STATUS_BAR_BORDER);
        int innerWidth = Math.max(0, width - 4);
        int innerHeight = Math.max(1, height - 4);
        int fillWidth = Math.max(0, Math.min(innerWidth, (int)Math.round(innerWidth * clamp01(healthFraction))));
        if (fillWidth > 0) {
            graphics.fill(x + 2, y + 2, x + 2 + fillWidth, y + 2 + innerHeight, HEALTH_BAR_FILL);
            int highlightHeight = Math.max(1, innerHeight / 2);
            graphics.fill(x + 2, y + 2, x + 2 + fillWidth, y + 2 + highlightHeight, HEALTH_BAR_HIGHLIGHT);
        }
        int absorptionWidth = Math.max(0, Math.min(innerWidth, (int)Math.round(innerWidth * clamp01(absorptionFraction))));
        if (absorptionWidth > 0) {
            graphics.fill(x + 2, y + height - 4, x + 2 + absorptionWidth, y + height - 2, ABSORPTION_BAR_FILL);
        }
    }

    public static void drawCultivationProgressBar(GuiGraphics graphics, int x, int y, int width, int height, double fraction) {
        double clamped = clamp01(fraction);
        if (width <= 0 || height <= 0) return;

        graphics.blit(CULTIVATION_PROGRESS_BAR, x, y, width, height, 0.0F, 0.0F,
                CULTIVATION_PROGRESS_TEXTURE_WIDTH, CULTIVATION_PROGRESS_TEXTURE_HEIGHT,
                CULTIVATION_PROGRESS_TEXTURE_WIDTH, CULTIVATION_PROGRESS_TEXTURE_HEIGHT);

        int insetX = Math.max(6, Math.round(width * 0.055F));
        int insetY = Math.max(2, Math.round(height * 0.20F));
        int innerX = x + insetX;
        int innerY = y + insetY;
        int innerWidth = Math.max(0, width - insetX * 2);
        int innerHeight = Math.max(1, height - insetY * 2);
        int fillWidth = Math.max(0, Math.min(innerWidth, (int) Math.round(innerWidth * clamped)));
        if (fillWidth <= 0) return;

        graphics.fill(innerX, innerY, innerX + fillWidth, innerY + innerHeight, 0x8836E6D0);
        int highlightHeight = Math.max(1, innerHeight / 2);
        graphics.fill(innerX, innerY, innerX + fillWidth, innerY + highlightHeight, 0xAA8FFFF0);
    }

    public static void drawTooltipPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        drawBox(graphics, x, y, width, height, TOOLTIP_PANEL, TOOLTIP_BORDER);
    }

    private static void drawBox(GuiGraphics graphics, int x, int y, int width, int height, int fillColor, int borderColor) {
        if (width <= 0 || height <= 0) return;
        graphics.fill(x, y, x + width, y + height, borderColor);
        if (width > 2 && height > 2) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, fillColor);
        }
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
        String fitted = fitWidth(font, value, maxWidth);
        graphics.drawString(font, fitted, x, y, color, dropShadow);
    }

    public static String fitWidth(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("..."))) + "...";
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
