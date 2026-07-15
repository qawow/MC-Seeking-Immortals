package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.menu.StorageBraceletMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class StorageBraceletScreenMenu extends AbstractContainerScreen<StorageBraceletMenu> {
    private static final int CONTAINER_WIDTH = 176;

    public StorageBraceletScreenMenu(StorageBraceletMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        int rows = (menu.storageSlots() + 8) / 9;
        this.imageWidth = CONTAINER_WIDTH;
        this.imageHeight = 114 + rows * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        int rows = (menu.storageSlots() + 8) / 9;
        ImmortalUiSkin.drawLayeredPanel(graphics, x, y, imageWidth, imageHeight);
        ImmortalUiSkin.drawTitleBar(graphics, x + 4, y + 4, imageWidth - 8, 12);

        for (int i = 0; i < menu.storageSlots(); i++) {
            int row = i / 9;
            int col = i % 9;
            ImmortalUiSkin.drawInnerFrame(graphics,
                    x + 7 + col * 18, y + 17 + row * 18, 18, 18);
        }
        int playerInvY = 18 + rows * 18 + 14;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                ImmortalUiSkin.drawInnerFrame(graphics,
                        x + 7 + col * 18, y + playerInvY - 1 + row * 18, 18, 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            ImmortalUiSkin.drawInnerFrame(graphics,
                    x + 7 + col * 18, y + playerInvY + 57, 18, 18);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        ImmortalUiSkin.drawStringFit(font, graphics, title.getString(),
                titleLabelX, 6, Math.max(1, imageWidth - titleLabelX * 2),
                ImmortalUiSkin.JOURNAL_BORDER, false);
        ImmortalUiSkin.drawStringFit(font, graphics, playerInventoryTitle.getString(),
                inventoryLabelX, inventoryLabelY,
                Math.max(1, imageWidth - inventoryLabelX * 2),
                ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    static ContainerLayout calculateLayout(int screenWidth, int screenHeight, int storageSlots) {
        int safeWidth = Math.max(1, screenWidth);
        int safeHeight = Math.max(1, screenHeight);
        int rows = (Math.max(1, Math.min(27, storageSlots)) + 8) / 9;
        int containerHeight = 114 + rows * 18;
        int left = (safeWidth - CONTAINER_WIDTH) / 2;
        int top = (safeHeight - containerHeight) / 2;
        Rect full = new Rect(left, top, CONTAINER_WIDTH, containerHeight);
        int visibleLeft = Math.max(0, left);
        int visibleTop = Math.max(0, top);
        int visibleRight = Math.min(safeWidth, left + CONTAINER_WIDTH);
        int visibleBottom = Math.min(safeHeight, top + containerHeight);
        Rect visible = new Rect(visibleLeft, visibleTop,
                Math.max(1, visibleRight - visibleLeft), Math.max(1, visibleBottom - visibleTop));
        return new ContainerLayout(full, visible, rows,
                new Rect(left + 8, top + 18, 9 * 18, rows * 18),
                new Rect(left + 8, top + 18 + rows * 18 + 14, 9 * 18, 76));
    }

    record ContainerLayout(Rect fullFrame, Rect visibleFrame, int rows,
                           Rect storageSlotPlane, Rect playerSlotPlane) {}

    record Rect(int x, int y, int width, int height) {
        int right() { return x + width; }
        int bottom() { return y + height; }
        boolean inside(int screenWidth, int screenHeight) {
            return width > 0 && height > 0 && x >= 0 && y >= 0
                    && right() <= screenWidth && bottom() <= screenHeight;
        }
    }
}
