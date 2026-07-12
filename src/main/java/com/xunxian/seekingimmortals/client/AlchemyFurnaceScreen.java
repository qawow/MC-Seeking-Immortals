package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.menu.AlchemyFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class AlchemyFurnaceScreen extends AbstractContainerScreen<AlchemyFurnaceMenu> {
    private static final ResourceLocation VANILLA = new ResourceLocation("textures/gui/container/dispenser.png");

    public AlchemyFurnaceScreen(AlchemyFurnaceMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        // reuse a simple vanilla container backdrop; labels convey alchemy purpose
        graphics.blit(VANILLA, x, y, 0, 0, imageWidth, imageHeight);
        int progress = menu.getProgress();
        int total = menu.getTotal();
        int w = total <= 0 ? 0 : (int) (24.0F * (total - progress) / (float) total);
        graphics.fill(x + 88, y + 36, x + 88 + Math.max(0, Math.min(24, w)), y + 40, 0xFF55FF55);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        graphics.drawString(font, Component.translatable("screen.seeking_immortals.alchemy_menu.progress",
                Math.max(0, menu.getTotal() - menu.getProgress()), menu.getTotal()),
                leftPos + 8, topPos + 70, 0x404040, false);
    }
}
