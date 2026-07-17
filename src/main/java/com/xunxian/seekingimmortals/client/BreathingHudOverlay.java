package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

@Mod.EventBusSubscriber(modid = SeekingImmortalsMod.MODID, value = Dist.CLIENT)
public final class BreathingHudOverlay {
    private static final int SETTLEMENT_TICKS = 100;

    private BreathingHudOverlay() {}

    public static void renderOverlay(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || minecraft.screen != null) return;

        if (!ClientCultivationData.isSynced() && !ClientCultivationData.hasPendingMeditating()) return;
        ClientCultivationData.Snapshot data = ClientCultivationData.getSnapshot();
        if (!ClientCultivationData.effectiveMeditating()) return;

        ImmortalHudLayout.Rect panel = ImmortalHudLayout.breathingRect(screenWidth, screenHeight);
        int width = panel.width();
        int height = panel.height();
        int x = panel.x();
        int y = panel.y();

        ImmortalUiSkin.drawBreathingTablet(graphics, x, y, width, height);
        int padding = width >= 100 ? 6 : Math.max(2, Math.min(4, width / 12));
        int contentWidth = Math.max(1, width - padding * 2);
        int titleY = y + Math.max(2, Math.min(5, height / 7));
        String title = ImmortalUiSkin.fitWidth(minecraft.font, "打坐吐纳", contentWidth);
        if (titleY + minecraft.font.lineHeight <= y + height) {
            graphics.drawString(minecraft.font, title,
                    x + Math.max(padding, (width - minecraft.font.width(title)) / 2), titleY,
                    ImmortalUiSkin.JOURNAL_BORDER, false);
        }

        double progress = (minecraft.player.tickCount % SETTLEMENT_TICKS) / (double) SETTLEMENT_TICKS;
        int progressHeight = Math.max(2, Math.min(7, height / 6));
        int progressY = Math.max(y, y + height - Math.min(3, padding) - progressHeight);
        int textY = titleY + minecraft.font.lineHeight + 1;
        int textBottom = progressY - 1;
        if (width >= 176) {
            int columnGap = 8;
            int columnWidth = Math.max(1, (contentWidth - columnGap) / 2);
            int rightX = x + padding + columnWidth + columnGap;
            textY = drawLine(graphics, minecraft,
                    "效率 " + format(data.cultivationSpeedMultiplier()) + "x",
                    x + padding, textY, columnWidth, textBottom, ImmortalUiSkin.JOURNAL_JADE_TEXT);
            drawLine(graphics, minecraft, "灵气 " + data.auraConcentration(),
                    rightX, titleY + minecraft.font.lineHeight + 1, columnWidth, textBottom,
                    ImmortalUiSkin.JOURNAL_SPIRIT);
            drawLine(graphics, minecraft,
                    "功法 " + format(data.physiqueCultivationSpeedMultiplier()) + "x",
                    x + padding, textY, columnWidth, textBottom, ImmortalUiSkin.JOURNAL_PAPER);
            drawLine(graphics, minecraft,
                    "灵根 " + format(data.rootCultivationSpeedCoefficient()) + "x",
                    rightX, textY, columnWidth, textBottom, ImmortalUiSkin.JOURNAL_PAPER);
        } else {
            textY = drawLine(graphics, minecraft,
                    "效率 " + format(data.cultivationSpeedMultiplier()) + "x  灵气 " + data.auraConcentration(),
                    x + padding, textY, contentWidth, textBottom, ImmortalUiSkin.JOURNAL_SPIRIT);
            drawLine(graphics, minecraft,
                    "功法 " + format(data.physiqueCultivationSpeedMultiplier())
                            + "x  灵根 " + format(data.rootCultivationSpeedCoefficient()) + "x",
                    x + padding, textY, contentWidth, textBottom, ImmortalUiSkin.JOURNAL_JADE_TEXT);
        }
        ImmortalUiSkin.drawSemanticStatusBar(graphics, x + padding, progressY,
                contentWidth, progressHeight, progress, ImmortalUiSkin.StatusBarStyle.CULTIVATION);
    }

    private static int drawLine(GuiGraphics graphics, Minecraft minecraft, String text,
                                int x, int y, int maxWidth, int bottom, int color) {
        if (y + minecraft.font.lineHeight > bottom) {
            return y;
        }
        ImmortalUiSkin.drawStringFit(minecraft.font, graphics, text, x, y, maxWidth, color, false);
        return y + minecraft.font.lineHeight + 1;
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

}
