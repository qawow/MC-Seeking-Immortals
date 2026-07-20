package com.xunxian.seekingimmortals.client.ui.widget;

import com.xunxian.seekingimmortals.client.ImmortalUiSkin;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 云笈墨卷 shared row/section widgets: labeled key/value rows and section
 * headers. Replaces per-screen {@code row(...)}/{@code sectionTitle(...)}
 * copies. All colors come from the active {@link ImmortalUiSkin} scene.
 */
public final class InkRows {
    public static final int ROW_HEIGHT = ImmortalUiSkin.LINE_HEIGHT;
    public static final int SECTION_HEIGHT = 14;

    private InkRows() {}

    /** Draws one label/value row; returns the y for the next row. */
    public static int keyValue(GuiGraphics graphics, Font font, int x, int y, int width,
                               Component label, Component value, int valueColor) {
        graphics.drawString(font, label, x, y, ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
        String text = value.getString();
        int valueWidth = font.width(text);
        ImmortalUiSkin.drawStringFit(font, graphics, text,
                Math.max(x + 4, x + width - valueWidth), y,
                Math.max(20, width - 4), valueColor, false);
        return y + ROW_HEIGHT;
    }

    /** Default-ink value row. */
    public static int keyValue(GuiGraphics graphics, Font font, int x, int y, int width,
                               Component label, Component value) {
        return keyValue(graphics, font, x, y, width, label, value, ImmortalUiSkin.JOURNAL_PAPER);
    }

    /** Draws a titled ink-line section header; returns the y for content below. */
    public static int section(GuiGraphics graphics, Font font, int x, int y, int width,
                              Component title) {
        ImmortalUiSkin.drawTitleBar(graphics, x, y, width, SECTION_HEIGHT);
        ImmortalUiSkin.drawStringFit(font, graphics, title.getString(), x + 5, y + 3,
                width - 10, ImmortalUiSkin.JOURNAL_JADE_TEXT, false);
        return y + SECTION_HEIGHT + 3;
    }

    /** Centered waiting-for-sync placeholder inside a viewport. */
    public static void syncWait(GuiGraphics graphics, Font font,
                                int x, int y, int width, int height, Component waitingText) {
        String text = waitingText.getString();
        int textWidth = Math.min(font.width(text), width - 8);
        int tx = x + Math.max(4, (width - textWidth) / 2);
        int ty = y + Math.max(4, (height - ImmortalUiSkin.LINE_HEIGHT) / 2);
        ImmortalUiSkin.drawStringFit(font, graphics, text, tx, ty,
                width - 8, ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
    }
}
