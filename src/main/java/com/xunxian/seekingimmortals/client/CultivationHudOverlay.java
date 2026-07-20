package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Cultivation content for the merged left-top status strip.
 * Does not draw outer chrome — {@link CultivationHealthOverlay} owns the jade tablet frame.
 */
@Mod.EventBusSubscriber(modid = SeekingImmortalsMod.MODID, value = Dist.CLIENT)
public final class CultivationHudOverlay {
    private static final int PADDING_X = 2;
    private static final int PADDING_Y = 2;

    private CultivationHudOverlay() {}

    public static void renderOverlay(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || minecraft.screen != null) return;
        if (!ClientCultivationData.isSynced()) return;

        ClientCultivationData.Snapshot data = ClientCultivationData.getSnapshot();
        ImmortalHudLayout.Layout layout = ImmortalHudLayout.calculate(screenWidth, screenHeight);
        ImmortalHudLayout.Rect band = layout.cultivationBand();
        if (band.width() <= 0 || band.height() <= 0) return;

        int paddingX = band.width() >= 80 ? PADDING_X : Math.max(1, Math.min(2, band.width() / 20));
        int paddingY = band.height() >= 40 ? PADDING_Y : 1;
        // Keep clear of the left cinnabar chrome edge.
        int textX = Math.max(band.x() + paddingX, layout.statusStrip().x() + 5);
        int textY = band.y() + paddingY + 1;
        int barWidth = Math.max(1, band.right() - textX - 3);
        int contentBottom = band.bottom() - paddingY;

        ImmortalUiSkin.withClimate(UiClimate.JADE_SLIP, () ->
                ImmortalUiSkin.withScissor(graphics, band.x(), band.y(), band.width(), band.height(), () ->
                        renderCultivationBand(graphics, minecraft, data, textX, textY, barWidth, contentBottom)));
    }

    private static void renderCultivationBand(GuiGraphics graphics, Minecraft minecraft,
                                              ClientCultivationData.Snapshot data,
                                              int textX, int textY, int barWidth, int contentBottom) {
        int cursor = textY;
        String realmLine = tr("realm_prefix") + data.realm() + data.stage();
        if (!canDrawText(minecraft, cursor, contentBottom)) return;
        drawFit(minecraft, graphics, realmLine, textX, cursor, barWidth, ImmortalUiSkin.JOURNAL_PAPER);
        cursor += minecraft.font.lineHeight + 1;

        if (cursor + minecraft.font.lineHeight + 6 > contentBottom) return;
        cursor = ImmortalUiSkin.drawMeterRow(minecraft.font, graphics, textX, cursor, barWidth,
                tr("cultivation"), shortNumber(data.cultivation()) + "/" + shortNumber(data.cultivationMax()),
                clamp01(fraction(data.cultivation(), data.cultivationMax())),
                ImmortalUiSkin.StatusBarStyle.CULTIVATION);

        if (cursor + minecraft.font.lineHeight + 6 > contentBottom) return;
        cursor = ImmortalUiSkin.drawMeterRow(minecraft.font, graphics, textX, cursor, barWidth,
                tr("mana"), shortNumber(data.mana()) + "/" + shortNumber(data.manaMax()),
                clamp01(fraction(data.mana(), data.manaMax())),
                ImmortalUiSkin.StatusBarStyle.SPIRIT);

        String core = coreLine(data);
        if (canDrawText(minecraft, cursor, contentBottom)) {
            drawFit(minecraft, graphics, core, textX, cursor, barWidth, coreLineColor(data));
            cursor += minecraft.font.lineHeight + 1;
        }
        for (HudLine line : trailingLines(data)) {
            if (!canDrawText(minecraft, cursor, contentBottom)) return;
            drawFit(minecraft, graphics, line.text(), textX, cursor, barWidth, line.color());
            cursor += minecraft.font.lineHeight + 1;
        }
    }

    private static List<HudLine> trailingLines(ClientCultivationData.Snapshot data) {
        List<HudLine> lines = new ArrayList<>();
        String advancementLine = advancementLine(data);
        if (!advancementLine.isBlank()) {
            lines.add(new HudLine(advancementLine, ImmortalUiSkin.JOURNAL_JADE_TEXT));
        }
        if (data.tribulationActive()) {
            int seconds = Math.max(0, (int)Math.ceil(data.tribulationNextStrikeTicks() / 20.0D));
            lines.add(new HudLine(tr("tribulation_prefix") + data.tribulationCurrentStrike() + "/" + data.tribulationTotalStrikes() + " " + seconds + "s",
                    ImmortalUiSkin.JOURNAL_CINNABAR_BRIGHT));
        }
        return lines;
    }

    private static String coreLine(ClientCultivationData.Snapshot data) {
        int qiRisk = data.qiDevRisk();
        return tr("sense_prefix") + shortNumber(data.divSense()) + tr("qi_dev_prefix") + qiRisk + "%"
                + (qiRisk >= ImmortalUiSkin.QI_DEV_DANGER_THRESHOLD ? tr("qi_dev_high") : "");
    }

    private static int coreLineColor(ClientCultivationData.Snapshot data) {
        return ImmortalUiSkin.qiDevRiskColor(data.qiDevRisk(), ImmortalUiSkin.JOURNAL_JADE_TEXT);
    }

    private static String advancementLine(ClientCultivationData.Snapshot data) {
        boolean hasGoldCore = data.goldCoreGrade() != null
                && !data.goldCoreGrade().isBlank()
                && !tr("no_core").equals(data.goldCoreGrade())
                && !tr("none").equals(data.goldCoreGrade());
        if (!hasGoldCore && !data.completeFiveElements()) return "";
        StringBuilder builder = new StringBuilder();
        if (hasGoldCore) {
            builder.append(tr("core_prefix")).append(data.goldCoreGrade());
        }
        if (data.completeFiveElements()) {
            if (!builder.isEmpty()) builder.append("  ");
            builder.append(tr("five_complete"));
        }
        return builder.toString();
    }

    static int availablePanelWidth(int screenWidth) {
        ImmortalHudLayout.Layout layout = ImmortalHudLayout.calculate(screenWidth, 480);
        return Math.max(1, layout.statusStrip().width());
    }

    static int calculatePanelX(int screenWidth, int panelWidth) {
        ImmortalHudLayout.Layout layout = ImmortalHudLayout.calculate(screenWidth, 480);
        int stripX = layout.statusStrip().x();
        int maxX = Math.max(0, screenWidth - panelWidth);
        return Math.max(0, Math.min(stripX, maxX));
    }

    private static boolean canDrawText(Minecraft minecraft, int y, int bottom) {
        return y + minecraft.font.lineHeight <= bottom;
    }

    private static void drawFit(Minecraft minecraft, GuiGraphics graphics, String text, int x, int y, int maxWidth, int color) {
        ImmortalUiSkin.drawStringFit(minecraft.font, graphics, text, x, y, maxWidth, color, false);
    }

    private static double fraction(long current, long max) {
        if (max <= 0L) return 0.0D;
        return (double) current / (double) max;
    }

    private static double clamp01(double value) {
        return com.xunxian.seekingimmortals.client.ui.NumberFmt.clamp01(value);
    }

    private static String shortNumber(long value) {
        return com.xunxian.seekingimmortals.client.ui.NumberFmt.cjk(value);
    }

    private record HudLine(String text, int color) {}
    private static String tr(String suffix) {
        return net.minecraft.network.chat.Component
                .translatable("hud.seeking_immortals.cultivation." + suffix).getString();
    }

}
