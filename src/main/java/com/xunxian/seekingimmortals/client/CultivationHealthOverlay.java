package com.xunxian.seekingimmortals.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.gui.overlay.ForgeGui;

import java.util.Locale;

/**
 * Custom 气血/护体 overlay. Owns the left-top jade-tablet chrome for the merged
 * status strip; cultivation content is painted by {@link CultivationHudOverlay}
 * into the lower band when the free HUD is visible.
 */
public final class CultivationHealthOverlay {
    private static final int DEFAULT_WIDTH = 220;
    private static final int DEFAULT_HEIGHT = 38;
    private static final int TOP_MARGIN = 6;
    private static final int PADDING_X = 8;
    private static final int TEXT_LINE_HEIGHT = 9;
    private static final int BAR_HEIGHT = 7;

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
        ImmortalUiSkin.pushClimate(UiClimate.JADE_SLIP);
        try {
            renderHealthBarUnderClimate(graphics, minecraft, player, screenWidth, screenHeight);
        } finally {
            ImmortalUiSkin.popClimate();
        }
    }

    private static void renderHealthBarUnderClimate(GuiGraphics graphics, Minecraft minecraft, LocalPlayer player,
                                                    int screenWidth, int screenHeight) {
        ImmortalHudLayout.Layout layout = ImmortalHudLayout.calculate(screenWidth, screenHeight);
        boolean fullStrip = minecraft.screen == null && ClientCultivationData.isSynced();
        ImmortalHudLayout.Rect chrome = fullStrip ? layout.statusStrip() : layout.healthOnlyStrip();
        ImmortalHudLayout.Rect band = fullStrip ? layout.healthBand() : healthBandInside(chrome);

        float maxHealth = Math.max(1.0F, player.getMaxHealth());
        float health = Math.max(0.0F, Math.min(player.getHealth(), maxHealth));
        float absorption = Math.max(0.0F, player.getAbsorptionAmount());

        ImmortalUiSkin.drawStatusStripChrome(graphics, chrome.x(), chrome.y(),
                chrome.width(), chrome.height(), fullStrip);

        int padding = band.width() >= 72 ? PADDING_X : Math.min(3, Math.max(1, (band.width() - 1) / 4));
        int textX = band.x() + Math.max(0, Math.min(padding - 2, band.width() / 8));
        // Leave room for the left cinnabar edge painted by the chrome.
        textX = Math.max(textX, chrome.x() + 5);
        int contentWidth = Math.max(1, band.right() - textX - 3);
        String title = "气血 " + formatValue(health) + "/" + formatValue(maxHealth);
        int barHeight = band.height() >= 14
                ? Math.max(4, Math.min(BAR_HEIGHT, band.height() / 3))
                : Math.max(2, Math.min(BAR_HEIGHT, Math.max(1, band.height() / 3)));
        int barY = Math.max(band.y(), band.bottom() - barHeight - 1);
        int textY = band.y() + Math.min(2, Math.max(0, band.height() / 10));
        if (textY + minecraft.font.lineHeight <= barY) {
            ImmortalUiSkin.drawStringFit(minecraft.font, graphics, title, textX, textY,
                    contentWidth, ImmortalUiSkin.JOURNAL_CINNABAR_BRIGHT, false);
            textY += TEXT_LINE_HEIGHT;
        }
        if (absorption > 0.0F && textY + minecraft.font.lineHeight <= barY) {
            String absorptionText = "护体 +" + formatValue(absorption);
            ImmortalUiSkin.drawStringFit(minecraft.font, graphics, absorptionText,
                    textX, textY, contentWidth, ImmortalUiSkin.JOURNAL_WARNING, false);
        }

        double healthFraction = health / maxHealth;
        double absorptionFraction = absorption / maxHealth;
        ImmortalUiSkin.drawHealthBar(graphics, textX, barY, contentWidth, barHeight,
                healthFraction, absorptionFraction);

        // Jade divider under the health band when the full strip is open for cultivation content.
        if (fullStrip && layout.cultivationBand().height() > 4) {
            ImmortalUiSkin.drawHudDivider(graphics, band.x(), band.bottom() - 1, band.width());
        }
    }

    private static ImmortalHudLayout.Rect healthBandInside(ImmortalHudLayout.Rect chrome) {
        int pad = chrome.width() >= 80 && chrome.height() >= 28 ? 4
                : chrome.height() >= 16 ? 3 : 2;
        int innerX = chrome.x() + pad;
        int innerY = chrome.y() + pad;
        int innerW = Math.max(1, chrome.width() - pad * 2);
        int innerH = Math.max(1, chrome.height() - pad * 2);
        return new ImmortalHudLayout.Rect(innerX, innerY, innerW, innerH);
    }

    static int panelWidth(int screenWidth) {
        ImmortalHudLayout.Rect strip = ImmortalHudLayout.healthOnlyStripRect(screenWidth, 480);
        return Math.max(1, Math.min(DEFAULT_WIDTH, strip.width()));
    }

    static int panelHeight(int screenHeight) {
        ImmortalHudLayout.Rect strip = ImmortalHudLayout.healthOnlyStripRect(854, screenHeight);
        return Math.max(1, Math.min(DEFAULT_HEIGHT, strip.height()));
    }

    /** Left-anchored X for the compact health-only strip (legacy test helper). */
    static int calculatePanelX(int screenWidth) {
        ImmortalHudLayout.Rect strip = ImmortalHudLayout.healthOnlyStripRect(screenWidth, 480);
        return strip.x();
    }

    static int calculatePanelY(int screenHeight) {
        ImmortalHudLayout.Rect strip = ImmortalHudLayout.healthOnlyStripRect(854, screenHeight);
        return Math.max(0, Math.min(TOP_MARGIN, strip.y()));
    }

    private static String formatValue(float value) {
        double abs = Math.abs(value);
        if (abs >= 10_000D) {
            return com.xunxian.seekingimmortals.client.ui.NumberFmt.cjk((long) value);
        }
        return String.format(Locale.ROOT, value >= 100.0F ? "%.0f" : "%.1f", value).replace(".0", "");
    }
}
