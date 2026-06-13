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
    private static final int DEFAULT_WIDTH = 222;
    private static final int HEIGHT = 46;
    private static final int BOTTOM_SAFE_GAP = 50;
    private static final int SETTLEMENT_TICKS = 100;

    private BreathingHudOverlay() {}

    public static void renderOverlay(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || minecraft.screen != null) return;

        if (!ClientCultivationData.isSynced() && !ClientCultivationData.hasPendingMeditating()) return;
        ClientCultivationData.Snapshot data = ClientCultivationData.getSnapshot();
        if (!ClientCultivationData.effectiveMeditating()) return;

        int availableWidth = Math.max(40, screenWidth - 8);
        int width = Math.min(DEFAULT_WIDTH, Math.max(80, availableWidth));
        int x = Math.max(4, Math.min((screenWidth - width) / 2, screenWidth - width - 4));
        int minY = 4;
        int maxY = Math.max(minY, screenHeight - HEIGHT - 4);
        int targetY = screenHeight - HEIGHT - BOTTOM_SAFE_GAP;
        int y = Math.max(minY, Math.min(targetY, maxY));

        ImmortalUiSkin.drawPanel(graphics, x, y, width, HEIGHT);
        graphics.drawCenteredString(minecraft.font, "打坐吐纳", x + width / 2, y + 5, 0xFFE6D59A);

        int left = x + 9;
        int rightColumnX = left + Math.max(84, (width - 18) / 2);
        int lineY = y + 17;
        drawClamped(graphics, minecraft, "效率 " + format(data.cultivationSpeedMultiplier()) + "x", left, lineY, x + width - 6, 0xFFB8F5A2);
        if (rightColumnX + 50 < x + width - 4) {
            drawClamped(graphics, minecraft, "灵气 " + data.auraConcentration(), rightColumnX, lineY, x + width - 6, 0xFFEFE4C2);
        }
        lineY += 10;
        drawClamped(graphics, minecraft, "功法 " + format(data.physiqueCultivationSpeedMultiplier()) + "x", left, lineY, x + width - 6, 0xFFEFE4C2);
        if (rightColumnX + 55 < x + width - 4) {
            drawClamped(graphics, minecraft, "灵根 " + format(data.rootCultivationSpeedCoefficient()) + "x", rightColumnX, lineY, x + width - 6, 0xFFEFE4C2);
        }

        double progress = (minecraft.player.tickCount % SETTLEMENT_TICKS) / (double) SETTLEMENT_TICKS;
        ImmortalUiSkin.drawCultivationProgressBar(graphics, left, y + HEIGHT - 11, width - 18, 8, progress);
    }

    private static void drawClamped(GuiGraphics graphics, Minecraft minecraft, String text, int x, int y, int maxX, int color) {
        int maxWidth = Math.max(0, maxX - x);
        String value = minecraft.font.width(text) <= maxWidth
                ? text
                : minecraft.font.plainSubstrByWidth(text, Math.max(0, maxWidth - minecraft.font.width("..."))) + "...";
        graphics.drawString(minecraft.font, value, x, y, color, false);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

}
