package com.xunxian.seekingimmortals.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.gui.overlay.ForgeGui;

import java.util.Locale;

public final class CultivationHealthOverlay {
    private static final int DEFAULT_WIDTH = 154;
    private static final int DEFAULT_HEIGHT = 40;
    private static final int LEFT_MARGIN = 6;
    private static final int TOP_MARGIN = 6;
    private static final int PADDING_X = 8;
    private static final int TEXT_LINE_HEIGHT = 9;
    private static final int BAR_HEIGHT = 8;

    private CultivationHealthOverlay() {}

    public static void renderOverlay(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!shouldReplaceVanillaPlayerHealth(gui)) return;

        renderHealthBar(graphics, minecraft, minecraft.player, screenWidth, screenHeight);
    }

    public static boolean shouldReplaceVanillaPlayerHealth(ForgeGui gui) {
        Minecraft minecraft = Minecraft.getInstance();
        return shouldReplaceVanillaPlayerHealth(
                minecraft.options.hideGui,
                minecraft.player != null,
                minecraft.screen != null,
                gui.shouldDrawSurvivalElements());
    }

    static boolean shouldReplaceVanillaPlayerHealth(boolean hideGui, boolean hasPlayer, boolean hasScreen, boolean shouldDrawSurvivalElements) {
        return !hideGui && hasPlayer && !hasScreen && shouldDrawSurvivalElements;
    }

    static void renderHealthBar(GuiGraphics graphics, Minecraft minecraft, LocalPlayer player, int screenWidth, int screenHeight) {
        int width = panelWidth(screenWidth);
        int height = panelHeight(screenHeight);
        int x = calculatePanelX(screenWidth);
        int y = calculatePanelY(screenHeight);
        float maxHealth = Math.max(1.0F, player.getMaxHealth());
        float health = Math.max(0.0F, Math.min(player.getHealth(), maxHealth));
        float absorption = Math.max(0.0F, player.getAbsorptionAmount());

        ImmortalUiSkin.drawPanel(graphics, x, y, width, height);

        int textX = x + PADDING_X;
        int contentWidth = Math.max(1, width - PADDING_X * 2);
        String title = "气血 " + formatValue(health) + "/" + formatValue(maxHealth);
        ImmortalUiSkin.drawStringFit(minecraft.font, graphics, title, textX, y + 5, contentWidth, 0xFFEFE4C2, false);
        if (absorption > 0.0F) {
            String absorptionText = "护体 +" + formatValue(absorption);
            ImmortalUiSkin.drawStringFit(minecraft.font, graphics, absorptionText,
                    textX, y + 5 + TEXT_LINE_HEIGHT, contentWidth, 0xFFE6D59A, false);
        }

        int barY = y + height - BAR_HEIGHT - 6;
        double healthFraction = health / maxHealth;
        double absorptionFraction = absorption / maxHealth;
        ImmortalUiSkin.drawHealthBar(graphics, textX, barY, contentWidth, BAR_HEIGHT, healthFraction, absorptionFraction);
    }

    static int panelWidth(int screenWidth) {
        int maxAllowed = Math.max(1, screenWidth - LEFT_MARGIN * 2);
        return Math.max(1, Math.min(DEFAULT_WIDTH, maxAllowed));
    }

    static int panelHeight(int screenHeight) {
        int maxAllowed = Math.max(1, screenHeight - TOP_MARGIN * 2);
        return Math.max(1, Math.min(DEFAULT_HEIGHT, maxAllowed));
    }

    static int calculatePanelX(int screenWidth) {
        return Math.max(0, Math.min(LEFT_MARGIN, screenWidth - panelWidth(screenWidth)));
    }

    static int calculatePanelY(int screenHeight) {
        return Math.max(0, Math.min(TOP_MARGIN, screenHeight - panelHeight(screenHeight)));
    }

    private static String formatValue(float value) {
        double abs = Math.abs(value);
        if (abs >= 100_000_000D) return unitNumber(value, 100_000_000D, "亿");
        if (abs >= 10_000D) return unitNumber(value, 10_000D, "万");
        return String.format(Locale.ROOT, value >= 100.0F ? "%.0f" : "%.1f", value).replace(".0", "");
    }

    private static String unitNumber(float value, double unit, String suffix) {
        double scaled = value / unit;
        String pattern = Math.abs(scaled) >= 100.0D ? "%.0f%s" : "%.1f%s";
        return String.format(Locale.ROOT, pattern, scaled, suffix).replace(".0", "");
    }
}
