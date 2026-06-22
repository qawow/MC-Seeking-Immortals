package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.fml.common.Mod;

/**
 * Phase 7 基础修仙 HUD。
 * <p>常驻显示境界、修为进度条、灵力条、神识、走火风险；技能冷却由 {@link TechniqueSkillBarOverlay} 负责。</p>
 */
@Mod.EventBusSubscriber(modid = SeekingImmortalsMod.MODID, value = Dist.CLIENT)
public final class CultivationHudOverlay {
    private static final int PANEL_WIDTH = 122;
    private static final int PANEL_HEIGHT = 70;
    private static final int RIGHT_MARGIN = 4;
    private static final int TOP_MARGIN = 4;
    private static final int BAR_HEIGHT = 6;
    private static final int QI_DEV_WARN_THRESHOLD = 50;
    private static final int QI_DEV_DANGER_THRESHOLD = 70;

    private CultivationHudOverlay() {}

    public static void renderOverlay(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || minecraft.screen != null) return;
        if (!ClientCultivationData.isSynced()) return;

        ClientCultivationData.Snapshot data = ClientCultivationData.getSnapshot();
        int x = screenWidth - PANEL_WIDTH - RIGHT_MARGIN;
        int y = TOP_MARGIN;

        ImmortalUiSkin.drawPanel(graphics, x, y, PANEL_WIDTH, PANEL_HEIGHT);

        int textX = x + 8;
        int textY = y + 5;
        // 境界名称
        String realmText = data.realm() + " " + data.stage();
        graphics.drawString(minecraft.font, realmText, textX, textY, 0xFFE6D59A, false);
        textY += 11;

        // 修为：当前值文本 + 真实进度条（基于服务端同步的 cultivationMax）
        int cultivationCurrent = Math.max(0, data.cultivation());
        int barWidth = PANEL_WIDTH - 16;
        double cultivationFraction = clamp01(fraction(cultivationCurrent, (int)Math.max(1, data.cultivationMax())));
        ImmortalUiSkin.drawStatusBar(graphics, textX, textY, barWidth, BAR_HEIGHT, cultivationFraction);
        textY += BAR_HEIGHT + 2;
        graphics.drawString(minecraft.font, "xiu " + cultivationCurrent, textX, textY, 0xFFEFE4C2, false);
        textY += 11;

        // 灵力条
        double manaFraction = clamp01(fraction(data.mana(), data.manaMax()));
        ImmortalUiSkin.drawStatusBar(graphics, textX, textY, barWidth, BAR_HEIGHT, manaFraction);
        textY += BAR_HEIGHT + 2;
        graphics.drawString(minecraft.font, "ling " + data.mana() + "/" + data.manaMax(), textX, textY, 0xFF9AD1FF, false);
        textY += 11;

        // 神识 + 走火风险
        int qiRisk = data.qiDevRisk();
        int qiColor = qiRisk >= QI_DEV_DANGER_THRESHOLD ? 0xFFFF6B6B : qiRisk >= QI_DEV_WARN_THRESHOLD ? 0xFFFFD66B : 0xFFB8F5A2;
        String line = "shen " + data.divSense() + "  zouhuo " + qiRisk + "%";
        graphics.drawString(minecraft.font, line, textX, textY, qiColor, false);

        // 走火风险 >70% 顶部额外警示
        if (qiRisk >= QI_DEV_DANGER_THRESHOLD) {
            graphics.drawCenteredString(minecraft.font, "! zouhuo risk high !", x + PANEL_WIDTH / 2, y + PANEL_HEIGHT + 2, 0xFFFF6B6B);
        }
    }

    private static double fraction(int current, int max) {
        if (max <= 0) return 0.0;
        return (double) current / (double) max;
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}