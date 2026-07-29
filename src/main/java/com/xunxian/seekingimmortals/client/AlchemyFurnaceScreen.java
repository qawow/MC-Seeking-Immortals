package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.menu.AlchemyFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class AlchemyFurnaceScreen extends AbstractJournalContainerScreen<AlchemyFurnaceMenu> {
    public AlchemyFurnaceScreen(AlchemyFurnaceMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderJournalBody(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        drawSlot(graphics, x, y, 26, 20);
        drawSlot(graphics, x, y, 62, 20);
        drawSlot(graphics, x, y, 26, 48);
        drawSlot(graphics, x, y, 62, 48);
        drawSlot(graphics, x, y, 116, 34);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(graphics, x, y, 8 + column * 18, 84 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(graphics, x, y, 8 + column * 18, 142);
        }
        int progress = menu.getProgress();
        int total = menu.getTotal();
        double fraction = progressFraction(progress, total, menu.isCrafting());
        ImmortalUiSkin.drawSemanticStatusBar(graphics, x + 87, y + 35, 28, 7, fraction,
                menu.isCrafting()
                        ? ImmortalUiSkin.StatusBarStyle.CULTIVATION
                        : ImmortalUiSkin.StatusBarStyle.NEUTRAL);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // AbstractContainerScreen renders labels before its final tooltip pass. Keeping
        // furnace text here ensures a carried item/slot tooltip is always drawn above it.
        renderFurnaceLabels(graphics);
    }

    private void renderFurnaceLabels(GuiGraphics graphics) {
        boolean crafting = menu.isCrafting();
        int progress = menu.getProgress();
        int total = menu.getTotal();
        if (crafting) {
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable(progressTextKey(true),
                            Math.max(0, Math.min(total, total - progress)), total).getString(),
                    87, 23, 80, ImmortalUiSkin.JOURNAL_PAPER, false);
        } else {
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable(progressTextKey(false)).getString(),
                    87, 23, 80, ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
        }
        ImmortalUiSkin.drawStringFit(font, graphics,
                Component.translatable(
                        menu.isFormed()
                                ? "screen.seeking_immortals.alchemy_menu.shell_ok"
                                : "screen.seeking_immortals.alchemy_menu.shell_bad").getString(),
                8, 69, 78,
                menu.isFormed() ? ImmortalUiSkin.JOURNAL_JADE_TEXT : ImmortalUiSkin.JOURNAL_CINNABAR_BRIGHT, false);
        ImmortalUiSkin.drawStringFit(font, graphics, Component.translatable(
                        menu.hasEarthFireRoom()
                                ? "screen.seeking_immortals.alchemy_menu.room_ok"
                                : "screen.seeking_immortals.alchemy_menu.room_none").getString(),
                90, 69, 78,
                menu.hasEarthFireRoom() ? ImmortalUiSkin.JOURNAL_JADE_TEXT : ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
    }

    static double progressFraction(int progress, int total, boolean crafting) {
        if (!crafting || total <= 0) {
            return 0.0D;
        }
        int elapsed = Math.max(0, Math.min(total, total - Math.max(0, progress)));
        return elapsed / (double) total;
    }

    static String progressTextKey(boolean crafting) {
        return crafting
                ? "screen.seeking_immortals.alchemy_menu.progress"
                : "screen.seeking_immortals.alchemy_menu.idle";
    }

    static FurnaceLayout calculateLayout(int screenWidth, int screenHeight) {
        int left = (screenWidth - 176) / 2;
        int top = (screenHeight - 166) / 2;
        int visibleX = Math.max(0, left);
        int visibleY = Math.max(0, top);
        int visibleRight = Math.min(screenWidth, left + 176);
        int visibleBottom = Math.min(screenHeight, top + 166);
        return new FurnaceLayout(left, top,
                new UiRect(visibleX, visibleY, Math.max(0, visibleRight - visibleX),
                        Math.max(0, visibleBottom - visibleY)));
    }

    private static void drawSlot(GuiGraphics graphics, int left, int top, int slotX, int slotY) {
        ImmortalUiSkin.drawJadeSlipSlot(graphics, left + slotX - 1, top + slotY - 1, 18, false);
    }

    record FurnaceLayout(int left, int top, UiRect visiblePanel) {}
}
