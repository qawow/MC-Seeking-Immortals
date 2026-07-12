package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.menu.StorageBraceletMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class StorageBraceletScreenMenu extends AbstractContainerScreen<StorageBraceletMenu> {
    private static final ResourceLocation VANILLA = new ResourceLocation("textures/gui/container/generic_54.png");

    public StorageBraceletScreenMenu(StorageBraceletMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        int rows = (menu.storageSlots() + 8) / 9;
        this.imageWidth = 176;
        this.imageHeight = 114 + rows * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        int rows = (menu.storageSlots() + 8) / 9;
        graphics.blit(VANILLA, x, y, 0, 0, imageWidth, 17 + rows * 18);
        graphics.blit(VANILLA, x, y + 17 + rows * 18, 0, 126, imageWidth, 96);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
