package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Native Minecraft client UI skin for Seeking Immortals screens and overlays.
 *
 * <p>The UI deliberately uses only vanilla/Forge client rendering primitives
 * such as {@link GuiGraphics#fill}, {@link GuiGraphics#blit} and text drawing.
 * This keeps custom HUDs and screens client-only without any third-party UI
 * framework dependency.</p>
 */
public final class ImmortalUiSkin {
    private static final int PANEL_BORDER = 0xCCE6D59A;
    private static final int PANEL = 0xD61B1208;
    private static final int PANEL_INNER = 0xCC2A1B0D;
    private static final int PANEL_INNER_BORDER = 0x663B2F18;
    private static final int SKILL_EMPTY = 0x22000000;
    private static final int SKILL_EMPTY_BORDER = 0x88E6D59A;
    private static final int SKILL_FILLED = 0xAA111111;
    private static final int SKILL_FILLED_BORDER = 0xFFE6D59A;
    private static final int STATUS_BAR_BACKING = 0x991B1208;
    private static final int STATUS_BAR_BORDER = 0x99E6D59A;
    private static final int STATUS_BAR_FILL = 0xCC66D17A;
    private static final int TOOLTIP_PANEL = 0xEE130C05;
    private static final int TOOLTIP_BORDER = 0xDDE6D59A;
    private static final ResourceLocation CULTIVATION_PROGRESS_BAR = new ResourceLocation(SeekingImmortalsMod.MODID, "textures/gui/cultivation_progress_bar.png");
    private static final int CULTIVATION_PROGRESS_TEXTURE_WIDTH = 1204;
    private static final int CULTIVATION_PROGRESS_TEXTURE_HEIGHT = 153;

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

    public static void drawStatusBar(GuiGraphics graphics, int x, int y, int width, int height, double fraction) {
        drawBox(graphics, x, y, width, height, STATUS_BAR_BACKING, STATUS_BAR_BORDER);
        int fillWidth = Math.max(0, Math.min(width - 4, (int) Math.round((width - 4) * clamp01(fraction))));
        if (fillWidth > 0) {
            graphics.fill(x + 2, y + 2, x + 2 + fillWidth, y + height - 2, STATUS_BAR_FILL);
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

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
