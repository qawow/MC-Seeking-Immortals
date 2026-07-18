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
        double fraction = total <= 0 ? 0.0D : (total - progress) / (double)total;
        ImmortalUiSkin.drawSemanticStatusBar(graphics, x + 87, y + 35, 28, 7, fraction,
                ImmortalUiSkin.StatusBarStyle.CULTIVATION);
    }

    @Override
    protected void renderJournalOverlays(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ImmortalUiSkin.drawStringFit(font, graphics,
                Component.translatable("screen.seeking_immortals.alchemy_menu.progress",
                        Math.max(0, menu.getTotal() - menu.getProgress()), menu.getTotal()).getString(),
                leftPos + 87, topPos + 23, 80, ImmortalUiSkin.JOURNAL_PAPER, false);
        ImmortalUiSkin.drawStringFit(font, graphics, Component.translatable(
                        menu.isFormed()
                                ? "screen.seeking_immortals.alchemy_menu.shell_ok"
                                : "screen.seeking_immortals.alchemy_menu.shell_bad").getString(),
                leftPos + 8, topPos + 69, 78,
                menu.isFormed() ? ImmortalUiSkin.JOURNAL_JADE_TEXT : ImmortalUiSkin.JOURNAL_CINNABAR_BRIGHT, false);
        ImmortalUiSkin.drawStringFit(font, graphics, Component.translatable(
                        menu.hasEarthFireRoom()
                                ? "screen.seeking_immortals.alchemy_menu.room_ok"
                                : "screen.seeking_immortals.alchemy_menu.room_none").getString(),
                leftPos + 90, topPos + 69, 78,
                menu.hasEarthFireRoom() ? ImmortalUiSkin.JOURNAL_JADE_TEXT : ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
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
