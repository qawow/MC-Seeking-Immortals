package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.menu.AuctionHallMenu;
import com.xunxian.seekingimmortals.network.AuctionActionPacket;
import com.xunxian.seekingimmortals.network.ModNetwork;
import com.xunxian.seekingimmortals.network.SyncAuctionLadderPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** Wave491: productized auction hall with live ladder page + pagination. */
public class AuctionHallScreen extends AbstractContainerScreen<AuctionHallMenu> {
    public AuctionHallScreen(AuctionHallMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 360;
        this.imageHeight = 236;
    }

    @Override
    protected void init() {
        super.init();
        // Request current ladder on open.
        ModNetwork.CHANNEL.sendToServer(new AuctionActionPacket(AuctionActionPacket.ACTION_PAGE, "0"));
        rebuildButtons();
    }

    private void rebuildButtons() {
        clearWidgets();
        int left = leftPos;
        int top = topPos;
        int smallWidth = Math.max(54, Math.min(72, (imageWidth - 32) / 5));
        ClientAuctionLadderData.Snapshot data = ClientAuctionLadderData.get();
        int page = data.page();
        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.auction.refresh"), button ->
                        ModNetwork.CHANNEL.sendToServer(new AuctionActionPacket(
                                AuctionActionPacket.ACTION_PAGE, Integer.toString(page))))
                .bounds(left + imageWidth - smallWidth * 2 - 18, top + 8, smallWidth, 18)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.auction.close"), button -> onClose())
                .bounds(left + imageWidth - smallWidth - 12, top + 8, smallWidth, 18)
                .build());
        addRenderableWidget(Button.builder(Component.literal("<"), button ->
                        ModNetwork.CHANNEL.sendToServer(new AuctionActionPacket(
                                AuctionActionPacket.ACTION_PAGE, Integer.toString(Math.max(0, page - 1)))))
                .bounds(left + 12, top + imageHeight - 28, 24, 18)
                .build());
        addRenderableWidget(Button.builder(Component.literal(">"), button ->
                        ModNetwork.CHANNEL.sendToServer(new AuctionActionPacket(
                                AuctionActionPacket.ACTION_PAGE, Integer.toString(page + 1))))
                .bounds(left + 40, top + imageHeight - 28, 24, 18)
                .build());

        List<SyncAuctionLadderPacket.LotBid> lots = data.lots();
        int rows = Math.min(lots.size(), 6);
        int buyWidth = Math.min(54, Math.max(1, imageWidth - 24));
        int buyX = Math.max(left + 4, left + imageWidth - buyWidth - 12);
        int rowY = top + 56;
        for (int i = 0; i < rows; i++) {
            SyncAuctionLadderPacket.LotBid lot = lots.get(i);
            String lotId = lot.lotId();
            boolean settled = lot.settled();
            Button bid = Button.builder(Component.translatable("screen.seeking_immortals.auction.bid"), ignored ->
                            ModNetwork.CHANNEL.sendToServer(new AuctionActionPacket(AuctionActionPacket.ACTION_BID, lotId)))
                    .bounds(buyX, rowY + i * 24 - 4, buyWidth, 18)
                    .build();
            bid.active = !settled;
            addRenderableWidget(bid);
            Button settle = Button.builder(Component.translatable("screen.seeking_immortals.auction.settle"), ignored ->
                            ModNetwork.CHANNEL.sendToServer(new AuctionActionPacket(AuctionActionPacket.ACTION_SETTLE, lotId)))
                    .bounds(buyX - buyWidth - 4, rowY + i * 24 - 4, buyWidth, 18)
                    .build();
            settle.active = !settled;
            addRenderableWidget(settle);
            addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.auction.preview"), ignored ->
                            ModNetwork.CHANNEL.sendToServer(new AuctionActionPacket(AuctionActionPacket.ACTION_PREVIEW, lotId)))
                    .bounds(buyX - buyWidth * 2 - 8, rowY + i * 24 - 4, buyWidth, 18)
                    .build());
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // Rebuild when ladder sync arrives/changes page size.
        if (minecraft != null && minecraft.level != null && minecraft.level.getGameTime() % 20L == 0L) {
            // keep buttons in sync with latest lot ids after page change
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        ImmortalUiSkin.drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        ClientAuctionLadderData.Snapshot data = ClientAuctionLadderData.get();
        graphics.drawCenteredString(font, Component.translatable("screen.seeking_immortals.auction.title"),
                leftPos + imageWidth / 2, topPos + 12, ImmortalUiSkin.COLOR_TITLE);
        ImmortalUiSkin.drawStringFit(font, graphics,
                Component.translatable("screen.seeking_immortals.auction.page",
                        data.page() + 1, data.maxPage() + 1, data.totalLots()).getString(),
                leftPos + 14, topPos + 34, imageWidth - 28, ImmortalUiSkin.COLOR_TEXT_NORMAL, false);
        if (!data.synced()) {
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.auction.waiting").getString(),
                    leftPos + 14, topPos + 60, imageWidth - 28, ImmortalUiSkin.COLOR_TEXT_MUTED, false);
            renderTooltip(graphics, mouseX, mouseY);
            return;
        }
        List<SyncAuctionLadderPacket.LotBid> lots = data.lots();
        int rows = Math.min(lots.size(), 6);
        for (int i = 0; i < rows; i++) {
            SyncAuctionLadderPacket.LotBid lot = lots.get(i);
            String text = lot.display()
                    + "  now=" + lot.current()
                    + " next=" + lot.next()
                    + " lead=" + lot.leaderName()
                    + (lot.settled() ? " [SETTLED]" : "");
            ImmortalUiSkin.drawStringFit(font, graphics, text, leftPos + 16, topPos + 56 + i * 24,
                    imageWidth - 180, ImmortalUiSkin.COLOR_TEXT_NORMAL, false);
        }
        if (lots.isEmpty()) {
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.auction.empty").getString(),
                    leftPos + 14, topPos + 60, imageWidth - 28, ImmortalUiSkin.COLOR_TEXT_MUTED, false);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Rebuild buttons when page changes after click handlers fire next frame.
        boolean result = super.mouseClicked(mouseX, mouseY, button);
        rebuildButtons();
        return result;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }
}
