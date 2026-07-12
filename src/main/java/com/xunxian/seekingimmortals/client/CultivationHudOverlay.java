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

        List<String> widthLines = new ArrayList<>();
        widthLines.add(realmLine);
        widthLines.add(cultivationLine);
        widthLines.add(manaLine);
        widthLines.add(coreLine);
        for (HudLine line : trailingLines) {
            widthLines.add(line.text());
        }

        int panelWidth = panelWidth(minecraft, widthLines, screenWidth);
        int panelHeight = panelHeight(trailingLines.size(), screenHeight);
        int x = calculatePanelX(screenWidth, panelWidth);
        int y = Math.max(0, Math.min(TOP_MARGIN, screenHeight - panelHeight));

        ImmortalUiSkin.drawPanel(graphics, x, y, panelWidth, panelHeight);

        int textX = x + PADDING_X;
        int textY = y + PADDING_Y;
        int barWidth = Math.max(1, panelWidth - PADDING_X * 2);
        int contentBottom = y + panelHeight - PADDING_Y;
        drawFit(minecraft, graphics, realmLine, textX, textY, barWidth, 0xFFE6D59A);
        textY += LINE_HEIGHT;

        double cultivationFraction = clamp01(fraction(data.cultivation(), data.cultivationMax()));
        ImmortalUiSkin.drawStatusBar(graphics, textX, textY, barWidth, BAR_HEIGHT, cultivationFraction);
        textY += BAR_HEIGHT + 1;
        drawFit(minecraft, graphics, cultivationLine, textX, textY, barWidth, 0xFFEFE4C2);
        textY += LINE_HEIGHT;

        double manaFraction = clamp01(fraction(data.mana(), data.manaMax()));
        ImmortalUiSkin.drawStatusBar(graphics, textX, textY, barWidth, BAR_HEIGHT, manaFraction);
        textY += BAR_HEIGHT + 1;
        drawFit(minecraft, graphics, manaLine, textX, textY, barWidth, 0xFF9AD1FF);
        textY += LINE_HEIGHT;

        if (canDraw(textY, contentBottom)) {
            drawFit(minecraft, graphics, coreLine, textX, textY, barWidth, coreLineColor(data));
            textY += LINE_HEIGHT;
        }
        for (HudLine line : trailingLines) {
            if (!canDraw(textY, contentBottom)) return;
            drawFit(minecraft, graphics, line.text(), textX, textY, barWidth, line.color());
            textY += LINE_HEIGHT;
        }
    }

    private static List<HudLine> trailingLines(ClientCultivationData.Snapshot data) {
        List<HudLine> lines = new ArrayList<>();
        String advancementLine = advancementLine(data);
        if (!advancementLine.isBlank()) {
            lines.add(new HudLine(advancementLine, 0xFFE6D59A));
        }
        if (data.tribulationActive()) {
            int seconds = Math.max(0, (int)Math.ceil(data.tribulationNextStrikeTicks() / 20.0D));
            lines.add(new HudLine("天劫 " + data.tribulationCurrentStrike() + "/" + data.tribulationTotalStrikes() + " " + seconds + "s", 0xFFFFD66B));
        }
        return lines;
    }

    private static String coreLine(ClientCultivationData.Snapshot data) {
        int qiRisk = data.qiDevRisk();
        return "神识 " + shortNumber(data.divSense()) + "  走火 " + qiRisk + "%" + (qiRisk >= QI_DEV_DANGER_THRESHOLD ? " 高危" : "");
    }

    private static int coreLineColor(ClientCultivationData.Snapshot data) {
        int qiRisk = data.qiDevRisk();
        if (qiRisk >= QI_DEV_DANGER_THRESHOLD) return 0xFFFF6B6B;
        if (qiRisk >= QI_DEV_WARN_THRESHOLD) return 0xFFFFD66B;
        return 0xFFB8F5A2;
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

    private static boolean canDraw(int y, int bottom) {
        return y + 8 <= bottom;
    }

    private static void drawFit(Minecraft minecraft, GuiGraphics graphics, String text, int x, int y, int maxWidth, int color) {
        graphics.drawString(minecraft.font, fit(minecraft, text, maxWidth), x, y, color, false);
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
