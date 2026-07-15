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

@Mod.EventBusSubscriber(modid = SeekingImmortalsMod.MODID, value = Dist.CLIENT)
public final class CultivationHudOverlay {
    private static final int MIN_PANEL_WIDTH = 136;
    private static final int MAX_PANEL_WIDTH = 184;
    private static final int RIGHT_MARGIN = 4;
    private static final int TOP_MARGIN = 4;
    private static final int PADDING_X = 7;
    private static final int PADDING_Y = 5;
    private static final int LINE_HEIGHT = 10;
    private static final int BAR_HEIGHT = 5;
    private static final int QI_DEV_WARN_THRESHOLD = 50;
    private static final int QI_DEV_DANGER_THRESHOLD = 70;

    private CultivationHudOverlay() {}

    public static void renderOverlay(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || minecraft.screen != null) return;
        if (!ClientCultivationData.isSynced()) return;

        ClientCultivationData.Snapshot data = ClientCultivationData.getSnapshot();
        List<HudLine> trailingLines = trailingLines(data);
        String realmLine = "境界 " + data.realm() + data.stage();
        String cultivationLine = "修为 " + shortNumber(data.cultivation()) + "/" + shortNumber(data.cultivationMax());
        String manaLine = "灵力 " + shortNumber(data.mana()) + "/" + shortNumber(data.manaMax());
        String coreLine = coreLine(data);

        ImmortalHudLayout.Rect panel = ImmortalHudLayout.cultivationRect(screenWidth, screenHeight);
        int paddingX = panel.width() >= 80 ? PADDING_X : Math.max(2, Math.min(4, panel.width() / 10));
        int paddingY = panel.height() >= 48 ? PADDING_Y : 3;
        int textX = panel.x() + paddingX;
        int textY = panel.y() + paddingY;
        int barWidth = Math.max(1, panel.width() - paddingX * 2);
        int contentBottom = panel.bottom() - paddingY;

        ImmortalUiSkin.drawHudPanel(graphics, panel.x(), panel.y(), panel.width(), panel.height());
        if (!canDrawText(minecraft, textY, contentBottom)) return;
        drawFit(minecraft, graphics, realmLine, textX, textY, barWidth, ImmortalUiSkin.JOURNAL_BORDER);
        textY += minecraft.font.lineHeight + 1;

        double cultivationFraction = clamp01(fraction(data.cultivation(), data.cultivationMax()));
        int barHeight = panel.height() >= 18
                ? Math.max(3, Math.min(BAR_HEIGHT, panel.height() / 8))
                : 2;
        if (!canDrawBar(textY, barHeight, contentBottom)) return;
        ImmortalUiSkin.drawSemanticStatusBar(graphics, textX, textY, barWidth, barHeight,
                cultivationFraction, ImmortalUiSkin.StatusBarStyle.CULTIVATION);
        textY += barHeight + 1;
        if (!canDrawText(minecraft, textY, contentBottom)) return;
        drawFit(minecraft, graphics, cultivationLine, textX, textY, barWidth, ImmortalUiSkin.JOURNAL_JADE_TEXT);
        textY += minecraft.font.lineHeight + 1;

        double manaFraction = clamp01(fraction(data.mana(), data.manaMax()));
        if (!canDrawBar(textY, barHeight, contentBottom)) return;
        ImmortalUiSkin.drawSemanticStatusBar(graphics, textX, textY, barWidth, barHeight,
                manaFraction, ImmortalUiSkin.StatusBarStyle.SPIRIT);
        textY += barHeight + 1;
        if (!canDrawText(minecraft, textY, contentBottom)) return;
        drawFit(minecraft, graphics, manaLine, textX, textY, barWidth, ImmortalUiSkin.JOURNAL_SPIRIT);
        textY += minecraft.font.lineHeight + 1;

        if (canDrawText(minecraft, textY, contentBottom)) {
            drawFit(minecraft, graphics, coreLine, textX, textY, barWidth, coreLineColor(data));
            textY += minecraft.font.lineHeight + 1;
        }
        for (HudLine line : trailingLines) {
            if (!canDrawText(minecraft, textY, contentBottom)) return;
            drawFit(minecraft, graphics, line.text(), textX, textY, barWidth, line.color());
            textY += minecraft.font.lineHeight + 1;
        }
    }

    private static List<HudLine> trailingLines(ClientCultivationData.Snapshot data) {
        List<HudLine> lines = new ArrayList<>();
        String advancementLine = advancementLine(data);
        if (!advancementLine.isBlank()) {
            lines.add(new HudLine(advancementLine, ImmortalUiSkin.JOURNAL_BORDER));
        }
        if (data.tribulationActive()) {
            int seconds = Math.max(0, (int)Math.ceil(data.tribulationNextStrikeTicks() / 20.0D));
            lines.add(new HudLine("天劫 " + data.tribulationCurrentStrike() + "/" + data.tribulationTotalStrikes() + " " + seconds + "s",
                    ImmortalUiSkin.JOURNAL_CINNABAR_BRIGHT));
        }
        return lines;
    }

    private static String coreLine(ClientCultivationData.Snapshot data) {
        int qiRisk = data.qiDevRisk();
        return "神识 " + shortNumber(data.divSense()) + "  走火 " + qiRisk + "%" + (qiRisk >= QI_DEV_DANGER_THRESHOLD ? " 高危" : "");
    }

    private static int coreLineColor(ClientCultivationData.Snapshot data) {
        int qiRisk = data.qiDevRisk();
        if (qiRisk >= QI_DEV_DANGER_THRESHOLD) return ImmortalUiSkin.JOURNAL_CINNABAR_BRIGHT;
        if (qiRisk >= QI_DEV_WARN_THRESHOLD) return ImmortalUiSkin.JOURNAL_WARNING;
        return ImmortalUiSkin.JOURNAL_JADE_TEXT;
    }

    private static String advancementLine(ClientCultivationData.Snapshot data) {
        boolean hasGoldCore = data.goldCoreGrade() != null
                && !data.goldCoreGrade().isBlank()
                && !"未结丹".equals(data.goldCoreGrade())
                && !"无".equals(data.goldCoreGrade());
        if (!hasGoldCore && !data.completeFiveElements()) return "";
        StringBuilder builder = new StringBuilder();
        if (hasGoldCore) {
            builder.append("金丹 ").append(data.goldCoreGrade());
        }
        if (data.completeFiveElements()) {
            if (!builder.isEmpty()) builder.append("  ");
            builder.append("五行圆满");
        }
        return builder.toString();
    }

    private static int panelWidth(Minecraft minecraft, List<String> lines, int screenWidth) {
        int maxTextWidth = 0;
        for (String line : lines) {
            maxTextWidth = Math.max(maxTextWidth, minecraft.font.width(line));
        }
        int desired = Math.max(MIN_PANEL_WIDTH, Math.min(MAX_PANEL_WIDTH, maxTextWidth + PADDING_X * 2));
        int maxAllowed = availablePanelWidth(screenWidth);
        return Math.max(1, Math.min(desired, maxAllowed));
    }

    static int availablePanelWidth(int screenWidth) {
        return Math.max(1, screenWidth - TechniqueSkillBarOverlay.leftReservedWidth() - RIGHT_MARGIN);
    }

    static int calculatePanelX(int screenWidth, int panelWidth) {
        int maxX = Math.max(0, screenWidth - panelWidth);
        int rightAnchored = Math.max(0, screenWidth - panelWidth - RIGHT_MARGIN);
        int leftReserved = TechniqueSkillBarOverlay.leftReservedWidth();
        if (screenWidth >= leftReserved + panelWidth) {
            return Math.max(leftReserved, Math.min(rightAnchored, maxX));
        }
        return Math.max(0, Math.min(rightAnchored, maxX));
    }

    private static int panelHeight(int trailingLineCount, int screenHeight) {
        int desired = PADDING_Y * 2
                + LINE_HEIGHT
                + (BAR_HEIGHT + 1 + LINE_HEIGHT) * 2
                + LINE_HEIGHT
                + trailingLineCount * LINE_HEIGHT;
        int maxAllowed = Math.max(1, screenHeight - 4);
        return Math.max(1, Math.min(desired, maxAllowed));
    }

    private static boolean canDrawText(Minecraft minecraft, int y, int bottom) {
        return y + minecraft.font.lineHeight <= bottom;
    }

    private static boolean canDrawBar(int y, int height, int bottom) {
        return y + height <= bottom;
    }

    private static void drawFit(Minecraft minecraft, GuiGraphics graphics, String text, int x, int y, int maxWidth, int color) {
        ImmortalUiSkin.drawStringFit(minecraft.font, graphics, text, x, y, maxWidth, color, false);
    }

    private static String fit(Minecraft minecraft, String text, int maxWidth) {
        if (minecraft.font.width(text) <= maxWidth) return text;
        return minecraft.font.plainSubstrByWidth(text, Math.max(0, maxWidth - minecraft.font.width("..."))) + "...";
    }

    private static double fraction(long current, long max) {
        if (max <= 0L) return 0.0D;
        return (double) current / (double) max;
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static String shortNumber(long value) {
        double abs = Math.abs((double)value);
        if (abs >= 1_000_000_000_000D) return unitNumber(value, 1_000_000_000_000D, "兆");
        if (abs >= 100_000_000D) return unitNumber(value, 100_000_000D, "亿");
        if (abs >= 10_000D) return unitNumber(value, 10_000D, "万");
        return Long.toString(value);
    }

    private static String unitNumber(long value, double unit, String suffix) {
        double scaled = value / unit;
        String pattern = Math.abs(scaled) >= 100.0D ? "%.0f%s" : "%.1f%s";
        return String.format(Locale.ROOT, pattern, scaled, suffix).replace(".0", "");
    }

    private record HudLine(String text, int color) {}
}
