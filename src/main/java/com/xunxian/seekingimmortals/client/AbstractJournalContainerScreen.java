package com.xunxian.seekingimmortals.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Journal chrome base for {@link AbstractContainerScreen} subclasses.
 *
 * <p>Pipeline mirrors {@link AbstractJournalScreen} while respecting container rendering:
 * {@code renderBackground → (super.render → renderBg chrome/body + slots/widgets) → overlays → tooltips}.
 * Material climate defaults to warm lacquer (market/desk family) and may be overridden.</p>
 */
public abstract class AbstractJournalContainerScreen<T extends AbstractContainerMenu>
        extends AbstractContainerScreen<T> {
    protected AbstractJournalContainerScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    /** Material climate for this container family. Override per semantic routing. */
    protected UiClimate defaultClimate() {
        return UiClimate.WARM_LACQUER;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ImmortalUiSkin.pushClimate(defaultClimate());
        try {
            renderBackground(graphics);
            super.render(graphics, mouseX, mouseY, partialTick);
            renderJournalOverlays(graphics, mouseX, mouseY, partialTick);
            renderTooltip(graphics, mouseX, mouseY);
        } finally {
            ImmortalUiSkin.popClimate();
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        renderJournalChrome(graphics);
        renderJournalBody(graphics, partialTick, mouseX, mouseY);
    }

    /** Draws the layered panel and title bar for the fixed container image bounds. */
    protected void renderJournalChrome(GuiGraphics graphics) {
        int x = leftPos;
        int y = topPos;
        ImmortalUiSkin.drawLayeredPanel(graphics, x, y, imageWidth, imageHeight);
        UiRect header = journalTitleBar();
        ImmortalUiSkin.drawTitleBar(graphics, header.x(), header.y(), header.width(), header.height());
        renderJournalTitle(graphics, header);
    }

    /** Default title bar inset used by the alchemy furnace pilot and similar fixed menus. */
    protected UiRect journalTitleBar() {
        return new UiRect(leftPos + 5, topPos + 4, imageWidth - 10, 14);
    }

    protected void renderJournalTitle(GuiGraphics graphics, UiRect header) {
        String fitted = ImmortalUiSkin.fitWidth(font, title.getString(), Math.max(1, imageWidth - 24));
        graphics.drawCenteredString(font, fitted,
                leftPos + imageWidth / 2,
                header.y() + Math.max(2, (header.height() - 8) / 2),
                ImmortalUiSkin.JOURNAL_PAPER);
    }

    /** Container-specific body (slots, meters, frames) drawn after chrome inside {@link #renderBg}. */
    protected abstract void renderJournalBody(GuiGraphics graphics, float partialTick, int mouseX, int mouseY);

    /** Labels drawn after slots/widgets and before tooltips. */
    protected void renderJournalOverlays(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Title is drawn by journal chrome; keep vanilla inventory labels suppressed by default.
    }
}
