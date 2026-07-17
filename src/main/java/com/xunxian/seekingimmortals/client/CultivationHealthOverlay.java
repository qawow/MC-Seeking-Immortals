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
                gui.shouldDrawSurvivalElements());
    }

    /**
     * Custom 气血 owns survival player-health whenever the survival HUD would draw.
     * Open screens (inventory, pause, chat, full-screen UIs) no longer restore vanilla hearts;
     * render and PLAYER_HEALTH cancel stay on this same predicate.
     */
    static boolean shouldReplaceVanillaPlayerHealth(boolean hideGui, boolean hasPlayer,
                                                     boolean shouldDrawSurvivalElements) {
        return !hideGui && hasPlayer && shouldDrawSurvivalElements;
    }

    static void renderHealthBar(GuiGraphics graphics, Minecraft minecraft, LocalPlayer player, int screenWidth, int screenHeight) {
        ImmortalHudLayout.Rect panel = ImmortalHudLayout.healthRect(screenWidth, screenHeight);
        int width = panel.width();
        int height = panel.height();
        int x = panel.x();
        int y = panel.y();
        float maxHealth = Math.max(1.0F, player.getMaxHealth());
        float health = Math.max(0.0F, Math.min(player.getHealth(), maxHealth));
        float absorption = Math.max(0.0F, player.getAbsorptionAmount());

        ImmortalUiSkin.drawHudPanel(graphics, x, y, width, height);

        int padding = width >= 72 ? PADDING_X : Math.min(3, Math.max(1, (width - 1) / 4));
        int textX = x + padding;
        int contentWidth = Math.max(1, width - padding * 2);
        String title = "气血 " + formatValue(health) + "/" + formatValue(maxHealth);
        int barHeight = height >= 16
                ? Math.max(5, Math.min(BAR_HEIGHT, height / 4))
                : Math.max(2, Math.min(BAR_HEIGHT, height / 4));
        int barY = Math.max(y, y + height - barHeight - Math.min(5, Math.max(1, height / 8)));
        int textY = y + Math.min(5, Math.max(2, height / 7));
        if (textY + minecraft.font.lineHeight <= barY) {
            ImmortalUiSkin.drawStringFit(minecraft.font, graphics, title, textX, textY,
                    contentWidth, ImmortalUiSkin.JOURNAL_CINNABAR_BRIGHT, false);
        }
        if (absorption > 0.0F && textY + TEXT_LINE_HEIGHT + minecraft.font.lineHeight <= barY) {
            String absorptionText = "护体 +" + formatValue(absorption);
            ImmortalUiSkin.drawStringFit(minecraft.font, graphics, absorptionText,
                    textX, textY + TEXT_LINE_HEIGHT, contentWidth, ImmortalUiSkin.JOURNAL_WARNING, false);
        }

        double healthFraction = health / maxHealth;
        double absorptionFraction = absorption / maxHealth;
        ImmortalUiSkin.drawHealthBar(graphics, textX, barY, contentWidth, barHeight,
                healthFraction, absorptionFraction);
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
